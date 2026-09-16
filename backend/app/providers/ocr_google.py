from __future__ import annotations

import logging

import httpx

from app.config import get_settings
from app.providers.base import OCRProvider
from app.schemas import OcrResult
from app.services.language import detect_language

logger = logging.getLogger(__name__)


class GoogleVisionOCRProvider(OCRProvider):
    name = "google_vision"

    async def process_image(self, image_bytes: bytes, hint_language: str | None = None) -> OcrResult:
        settings = get_settings()
        if not settings.ocr_api_key and not settings.google_tts_api_key:
            raise RuntimeError("GOOGLE Vision credentials are not configured.")
        api_key = settings.ocr_api_key
        import base64

        payload = {
            "requests": [
                {
                    "image": {"content": base64.b64encode(image_bytes).decode("ascii")},
                    "features": [{"type": "DOCUMENT_TEXT_DETECTION"}],
                    "imageContext": {"languageHints": [hint_language] if hint_language else ["en", "hi", "te"]},
                }
            ]
        }
        async with httpx.AsyncClient(timeout=60) as client:
            response = await client.post(
                f"https://vision.googleapis.com/v1/images:annotate?key={api_key}",
                json=payload,
            )
            response.raise_for_status()
            data = response.json()
        annotation = data["responses"][0].get("fullTextAnnotation", {})
        text = annotation.get("text", "")
        logger.info("ocr_complete engine=%s language=%s", self.name, detect_language(text))
        return OcrResult(text=text, language=detect_language(text), confidence=0.9, blocks=[], engine=self.name)
