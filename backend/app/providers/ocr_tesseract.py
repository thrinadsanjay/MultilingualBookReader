from __future__ import annotations

import asyncio
import logging
from functools import lru_cache
from io import BytesIO

import pytesseract
from PIL import Image, ImageOps

from app.providers.base import OCRProvider
from app.schemas import OcrBlock, OcrLine, OcrResult, OcrWord
from app.services.language import detect_language
from app.services.ocr_prep import (
    missing_languages,
    normalise_hint,
    page_segmentation_modes,
    tesseract_language,
    upscale_factor,
)

logger = logging.getLogger(__name__)


class MissingLanguageError(RuntimeError):
    """Raised when the server has no trained data for the requested script."""


@lru_cache(maxsize=1)
def installed_languages() -> set[str]:
    try:
        return set(pytesseract.get_languages(config=""))
    except Exception:  # noqa: BLE001 - older tesseract builds do not expose this
        logger.warning("could not list tesseract languages")
        return set()


def tesseract_version() -> str:
    try:
        return str(pytesseract.get_tesseract_version())
    except Exception:  # noqa: BLE001
        return "unavailable"


class TesseractOCRProvider(OCRProvider):
    name = "tesseract"

    async def process_image(self, image_bytes: bytes, hint_language: str | None = None) -> OcrResult:
        return await asyncio.to_thread(self._run, image_bytes, hint_language)

    def _run(self, image_bytes: bytes, hint_language: str | None) -> OcrResult:
        available = installed_languages()
        if available:
            absent = missing_languages(hint_language, available)
            if absent and normalise_hint(hint_language) == "te":
                raise MissingLanguageError(
                    "Telugu trained data is not installed on this server. "
                    "Install tesseract-ocr-tel and restart."
                )

        image = self._prepare(Image.open(BytesIO(image_bytes)))
        lang = tesseract_language(hint_language, available or None)

        # Page segmentation guesses the layout; a single text block is right for most book pages,
        # so the looser modes are only tried when it finds nothing.
        data = None
        for psm in page_segmentation_modes(hint_language):
            data = pytesseract.image_to_data(
                image,
                lang=lang,
                config=f"--oem 1 --psm {psm}",
                output_type=pytesseract.Output.DICT,
            )
            if any((word or "").strip() for word in data["text"]):
                break

        return self._to_result(data, lang)

    def _prepare(self, image: Image.Image) -> Image.Image:
        """Grayscale, stretch contrast, and upscale small pages; Telugu conjuncts need the pixels."""
        gray = ImageOps.exif_transpose(image).convert("L")
        gray = ImageOps.autocontrast(gray)
        factor = upscale_factor(gray.width)
        if factor > 1.0:
            gray = gray.resize((int(gray.width * factor), int(gray.height * factor)), Image.LANCZOS)
        return gray

    def _to_result(self, data: dict | None, lang: str) -> OcrResult:
        words: list[OcrWord] = []
        texts: list[str] = []
        confidences: list[float] = []
        for index in range(len(data["text"]) if data else 0):
            raw = (data["text"][index] or "").strip()
            if not raw:
                continue
            conf = _confidence(data["conf"][index])
            texts.append(raw)
            confidences.append(conf)
            words.append(OcrWord(text=raw, confidence=conf, bounding_box=None))

        text = " ".join(texts)
        language = detect_language(text)
        line = OcrLine(text=text, confidence=_avg(confidences), words=words)
        block = OcrBlock(text=text, confidence=_avg(confidences), language=language, lines=[line])
        logger.info("tesseract_ocr", extra={"languages": lang, "words": len(words)})
        return OcrResult(
            text=text,
            language=language,
            confidence=_avg(confidences),
            blocks=[block] if text else [],
            engine=self.name,
        )


def _confidence(raw: object) -> float:
    try:
        value = float(str(raw))
    except (TypeError, ValueError):
        return 0.0
    return max(value, 0.0) / 100.0


def _avg(values: list[float]) -> float:
    return sum(values) / len(values) if values else 0.0
