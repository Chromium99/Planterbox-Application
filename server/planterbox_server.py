"""
Planterbox backend server

Combines:
- Flask image receiver (saves images + basic metadata)
- Gemini-based plant diagnosis & care recommendations
  using structured info from plants.csv

Endpoints
---------
GET  /health
POST /process-image   (multipart form-data)
    fields:
        image        -> uploaded image file
        species_name -> string, e.g. "Orchid"
"""

import os
import re
import json
from datetime import datetime
from io import BytesIO

from flask import Flask, request, jsonify
from PIL import Image
import pandas as pd
from google import genai

from shareimg import get_image_info   # your teammate's helper

# ------------------------------------------------------------------
# CONFIG
# ------------------------------------------------------------------

# Where to save incoming images
UPLOAD_FOLDER = "received_images"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# CSV with care info (same idea as in the notebook)
CARE_CSV_PATH = "plants.csv"   # adjust path if needed
# ---- Load CSV keywords & care data ----
df = pd.read_csv(CARE_CSV_PATH, encoding="utf-8")

# Normalize plant names: strip spaces + lowercase
df["Plant Name"] = df["Plant Name"].astype(str).str.strip()
df["plant_name_norm"] = df["Plant Name"].str.lower()


def normalize_name(name: str) -> str:
    """Helper: normalize a species name for matching."""
    return str(name).strip().lower()

print("Loaded columns:", df.columns.tolist())
print(df.head())


# Gemini client – use env var, NOT hard-coded keys
API_KEY = os.environ.get("GOOGLE_API_KEY")
if not API_KEY:
    raise RuntimeError(
        "GOOGLE_API_KEY environment variable not set. "
        "Export it before running the server."
    )

client = genai.Client(api_key=API_KEY)

# ------------------------------------------------------------------
# CARE / LLM HELPERS (adapted from the notebook)
# ------------------------------------------------------------------


def get_care_data(species_name: str):
    """
    Look up a row in plants.csv matching the species name.
    Returns a dict of column -> value, or None if not found.
    Mirrors the notebook's get_care_data().
    """
    norm = normalize_name(species_name)
    row = df[df["plant_name_norm"] == norm]
    if row.empty:
        print(f"[care] No CSV row matched species_name='{species_name}' (norm='{norm}')")
        print("[care] Available normalized names:", df["plant_name_norm"].tolist())
        return None

    return row.iloc[0].to_dict()


def build_model_prompt(species_name: str, care_info: dict) -> str:
    """
    Prompt for Gemini. Uses explicit fields from plants.csv
    to force the model to ground its answer in YOUR data.
    """

    light = care_info.get("Light", "")
    watering = care_info.get("Watering", "")
    soil = care_info.get("Soil", "")
    fertilizer = care_info.get("Fertilizer", "")
    toxicity = care_info.get("Toxicity", "")
    notes = care_info.get("Notes", "")

    return f"""
You are a plant diagnosis and care assistant for a mobile app called Planterbox.

The app is telling you this plant species:
- species_name: "{species_name}"

You are also given a trusted care profile from our internal CSV table:

- Light: {light}
- Watering: {watering}
- Soil: {soil}
- Fertilizer: {fertilizer}
- Toxicity: {toxicity}
- Notes: {notes}

You will also see a PHOTO of the plant from the user.
Use BOTH the photo and the care profile above.

Your job is to return ONE JSON object only, with this exact structure:

{{
  "validated_species": "string",          // usually the same as species_name
  "visible_symptoms": [                   // short bullet phrases, can be empty if healthy
    "string"
  ],
  "likely_causes": [                      // 1–4 concrete, non-generic causes
    "string"
  ],
  "care_recommendations": "string",       // 3–6 sentences, practical and specific
  "urgency_level": "low"                  // one of: low, medium, high
}}

Guidelines:
- ALWAYS ground recommendations in the care profile above (light/water/soil/fertilizer/toxicity/notes).
- If the plant looks healthy, describe that and give preventative care suggestions using the profile.
- If there are problems (yellowing, spots, rot, wilting, pests, etc.), name them in visible_symptoms and explain likely_causes.
- In care_recommendations, give concrete actions (e.g. "water when top 2 inches are dry" not "water properly").
- urgency_level:
  - "low"  if plant looks generally fine or minor cosmetic issues
  - "medium" if moderate stress that should be fixed soon
  - "high" if the plant appears in serious danger or rapid decline

Output rules:
- Output ONLY valid JSON. No backticks, no markdown, no explanation text.
- Do not wrap the JSON in ```json``` fences.
    """


def run_llm_diagnostic(image_bytes: bytes, species_name: str) -> dict:
    """
    Core pipeline used by /process-image:

    1. Convert bytes -> PIL image
    2. Look up care info in CSV
    3. Call Gemini model with [image, prompt]
    4. Parse JSON from the model output

    Returns a dict like:
      {
        "validated_species": "...",
        "visible_symptoms": [...],
        "likely_causes": [...],
        "care_recommendations": "...",
        "urgency_level": "low"
      }
    or {"error": "..."} on failure.
    """
    # 1) PIL image
    try:
        pil_img = Image.open(BytesIO(image_bytes)).convert("RGB")
    except Exception as e:
        return {"error": f"Could not decode image: {e}"}

    # 2) Care info from CSV
    care_info = get_care_data(species_name)
    if care_info is None:
        return {"error": f"Species '{species_name}' not found in plants.csv"}

    # 3) Build prompt
    prompt = build_model_prompt(species_name, care_info)

    # 4) Model call (very close to notebook)
    try:
        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=[pil_img, prompt],
        )
    except Exception as e:
        return {"error": f"Model call failed: {e}"}

    raw = response.text.strip()
    # Debug log to your terminal
    print("\n===== GEMINI RAW OUTPUT =====")
    print(raw)
    print("===== END RAW OUTPUT =====\n")

    # Strip ```json ... ``` wrappers if the model adds them
    raw = re.sub(r"```json|```", "", raw).strip()

    try:
        diagnostic = json.loads(raw)
    except Exception:
        diagnostic = {"error": "Invalid JSON returned by model", "raw_output": raw}

    # Optional: pretty-print parsed diagnostic as well
    print("\n===== PARSED DIAGNOSTIC =====")
    print(json.dumps(diagnostic, indent=2, ensure_ascii=False))
    print("===== END DIAGNOSTIC =====\n")

    return diagnostic



# ------------------------------------------------------------------
# FLASK APP (based on your teammate's server)
# ------------------------------------------------------------------

app = Flask(__name__)


@app.route("/process-image", methods=["POST"])
def process_image():
    """
    Accepts:
      - form field 'species_name' (string)
      - file 'image' (multipart)

    Returns JSON:
      {
        "status": "success",
        "message": "...",
        "data": {
          "filename": "...",
          "size_bytes": ...,
          "width": ...,
          "height": ...,
          "total_pixels": ...,
          "received_at": "...",
          "saved_path": "...",
          "species_name": "...",
          "diagnostic": { ... LLM JSON ... }
        }
      }
    """
    try:
        # --- species name (from app's classifier) ---
        species_name = request.form.get("species_name")
        if not species_name:
            return jsonify(
                {
                    "status": "error",
                    "message": "Missing species_name in form data",
                }
            ), 400

        # --- image file ---
        if "image" not in request.files:
            return jsonify(
                {
                    "status": "error",
                    "message": "No image file provided",
                }
            ), 400

        image_file = request.files["image"]

        # Read bytes once
        image_bytes = image_file.read()
        if not image_bytes:
            return jsonify(
                {
                    "status": "error",
                    "message": "Empty image payload",
                }
            ), 400

        # --- basic metadata (reuse shareimg.get_image_info) ---
        image_info = get_image_info(image_bytes)

        # --- save image to disk (as original code did) ---
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"image_{timestamp}.jpg"
        filepath = os.path.join(UPLOAD_FOLDER, filename)
        with open(filepath, "wb") as f:
            f.write(image_bytes)

        file_size = len(image_bytes)

        # --- call LLM diagnostic pipeline ---
        diagnostic = run_llm_diagnostic(image_bytes, species_name)

        # --- build response ---
        response_data = {
            "status": "success",
            "message": "Image processed successfully",
            "data": {
                "filename": filename,
                "size_bytes": file_size,
                "width": image_info["width"],
                "height": image_info["height"],
                "total_pixels": image_info["total_pixels"],
                "received_at": datetime.now().isoformat(),
                "saved_path": filepath,
                "species_name": species_name,
                "diagnostic": diagnostic,
            },
        }

        return jsonify(response_data), 200

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    """Simple health check, kept from original server."""
    return jsonify({"status": "healthy", "server": "Planterbox Python Image Server"}), 200


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8080))
    print(f"Starting Planterbox backend on port {port}...")
    app.run(host="0.0.0.0", port=port, debug=False)

