# Planterbox (APK Demo Instructions — Professor)

This document explains how to **install and demo the Planterbox Android app from an APK**, without Android Studio.

Planterbox does:
- **On-device plant classification** using our TensorFlow Lite model (works offline)
- **AI care tips (LLM)** through our hosted backend (requires internet)

---

## What you need

### ✅ Option A (recommended): Android phone or tablet
- Any Android device (Android 7.0+ recommended)
- Wi-Fi or cellular data (for AI care tips)

### ❌ iPhone / iPad (iOS)
The APK **cannot** be installed on iOS.  
If you only have iOS, please use:
- an **Android device** (borrowed is fine), or
- watch the provided demo recording / live demo from us.

---

## Install the APK (Android)

1. Download the APK file:
   - `app-release.apk` (provided by us)

2. On your Android device, open the downloaded file.
   - If it downloads through Gmail/Drive/Chrome, open it from there
   - Or open it from **Files / Downloads**

3. If you see “For your security, your phone isn’t allowed to install unknown apps…”
   - Tap **Settings**
   - Enable **Allow from this source** (Chrome / Gmail / Files)
   - Go back and tap the APK again

4. Tap **Install**

5. After installation, open the app:
   - App name: **Planterbox**
   - Icon: 🌱 (Planterbox logo)

---

## Demo checklist (what to test)

### 1) Launch + navigation
1. Open Planterbox
2. Tap **Get Started**
3. You should see the **Plant Identifier** page

### 2) Upload an image (recommended for consistent demo)
1. Tap **Upload Image**
2. Pick a photo of a plant from your gallery
3. Tap **Analyze Plant**
4. Expected result:
   - Plant name + confidence
   - Care tips text (AI)

### 3) Scan using camera (optional)
1. Tap **Scan Plant**
2. Take a photo of a plant
3. Tap **Analyze Plant**
4. Expected result:
   - Plant name + confidence
   - Care tips text (AI)

> Tip: Upload works best for demo reliability because camera lighting can affect detection.

---

## Important: Internet required for AI care tips

- The plant label/confidence comes from the on-device model ✅
- The care tips come from the backend + LLM ✅ (requires internet)

If the device has no internet, the app can still classify the plant, but AI care tips may fail.

---

## If something goes wrong

### “AI analysis failed: Failed to connect …”
This means the app could not reach the backend (usually internet or backend asleep).

**Fix:**
1. Confirm Wi-Fi/cellular is ON
2. Try again after ~10 seconds
3. If it still fails, tell us and we will restart/wake the backend service

---

### The result shows an error message (429 / quota, 503 / unavailable)
This is an LLM/provider temporary limitation.

**Fix:**
- Wait 30–60 seconds and try again
- If needed, try with a different image once more

---

### App won’t install
Common causes:
- APK blocked by security settings  
  ✅ Enable “Install unknown apps” for the app you used to open the APK (Chrome/Files/Gmail).
- Very old Android version  
  ✅ Prefer Android 7.0+.

---

## What a successful demo looks like

After **Upload Image → Analyze Plant**, you should see:
- **Plant name** (top)
- **Confidence %**
- **Care Tips** paragraph (AI-generated)

That confirms:
- Model inference is running on the phone
- Backend + LLM pipeline is functioning

---

## Quick support info

If you run into any issues during the demo:
- Take a screenshot of the error screen
- Send it to us immediately so we can help
