from __future__ import annotations

import asyncio
from io import BytesIO

import pytesseract
from PIL import Image

from app.providers.base import OCRProvider
from app.schemas import OcrBlock, OcrLine, OcrResult, OcrWord
from app.services.language import detect_language


class TesseractOCRProvider(OCRProvider):
    name = "tesseract"

    async def process_image(self, image_bytes: bytes, hint_language: str | None = None) -> OcrResult:
        return await asyncio.to_thread(self._run, image_bytes, hint_language)

    def _run(self, image_bytes: bytes, hint_language: str | None) -> OcrResult:
        image = Image.open(BytesIO(image_bytes)).convert("RGB")
        lang = {
            "te": "tel",
            "hi": "hin+eng",
            "en": "eng",
        }.get((hint_language or "").split("-")[0], "eng+hin+tel")
        data = pytesseract.image_to_data(image, lang=lang, output_type=pytesseract.Output.DICT)
        words: list[OcrWord] = []
        texts: list[str] = []
        confidences: list[float] = []
        n = len(data["text"])
        for i in range(n):
            raw = (data["text"][i] or "").strip()
            if not raw:
                continue
            conf = float(data["conf"][i]) if str(data["conf"][i]).replace(".", "", 1).isdigit() else 0.0
            texts.append(raw)
            confidences.append(max(conf, 0) / 100.0)
            words.append(
                OcrWord(
                    text=raw,
                    confidence=max(conf, 0) / 100.0,
                    bounding_box=None,
                )
            )
        text = " ".join(texts)
        language = detect_language(text)
        line = OcrLine(text=text, confidence=_avg(confidences), words=words)
        block = OcrBlock(text=text, confidence=_avg(confidences), language=language, lines=[line])
        return OcrResult(
            text=text,
            language=language,
            confidence=_avg(confidences),
            blocks=[block] if text else [],
            engine=self.name,
        )


def _avg(values: list[float]) -> float:
    return sum(values) / len(values) if values else 0.0
