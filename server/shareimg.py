# shareimg.py
from io import BytesIO
from PIL import Image

def get_image_info(image_bytes: bytes):
    """
    Simple helper used by the Flask server.
    Takes raw image bytes and returns width, height, total_pixels.
    """
    with Image.open(BytesIO(image_bytes)) as img:
        width, height = img.size
        total_pixels = width * height
        return {
            "width": width,
            "height": height,
            "total_pixels": total_pixels,
        }
