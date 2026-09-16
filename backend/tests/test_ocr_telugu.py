"""End-to-end checks that the Telugu OCR path actually reads Telugu.

The page image is rendered here rather than committed as a fixture, so the test proves the whole
chain: preprocessing, language selection, Tesseract, and the response shape.
"""

from __future__ import annotations

import asyncio
from io import BytesIO
from pathlib import Path

import pytest
from PIL import Image, ImageDraw, ImageFont

from app.providers.ocr_tesseract import TesseractOCRProvider, installed_languages

TELUGU_FONTS = [
    "/usr/share/fonts/truetype/noto/NotoSansTelugu-Regular.ttf",
    "/usr/share/fonts/truetype/noto/NotoSerifTelugu-Regular.ttf",
]
LATIN_FONTS = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
]
TELUGU_LINES = ["తెలుగు పుస్తకం", "చదవడం సులభం"]
ENGLISH_LINES = ["Chapter One", "The quiet morning"]

pytestmark = pytest.mark.skipif(
    "tel" not in installed_languages(),
    reason="tesseract-ocr-tel is not installed on this machine",
)


def _font(candidates: list[str], size: int, script: str) -> ImageFont.FreeTypeFont:
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    pytest.skip(f"no {script} font available to render a test page")


def _render(lines: list[str], size: int = 64, width: int = 1400) -> bytes:
    """Renders each line with a font that actually covers its script, so tofu boxes never reach OCR."""
    telugu = _font(TELUGU_FONTS, size, "Telugu")
    latin = _font(LATIN_FONTS, size, "Latin")
    height = 80 + len(lines) * (size + 30)
    image = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(image)
    y = 40
    for line in lines:
        is_telugu = any("\u0c00" <= c <= "\u0c7f" for c in line)
        draw.text((60, y), line, fill="black", font=telugu if is_telugu else latin)
        y += size + 30
    buffer = BytesIO()
    image.save(buffer, format="PNG")
    return buffer.getvalue()


def _recognise(image_bytes: bytes, hint: str | None) -> str:
    provider = TesseractOCRProvider()
    result = asyncio.run(provider.process_image(image_bytes, hint))
    return result.text


def test_reads_telugu_from_a_rendered_page():
    text = _recognise(_render(TELUGU_LINES), "te")
    assert text.strip(), "Telugu page produced no text at all"
    # Telugu code points, rather than exact words: OCR output is never character perfect.
    telugu_chars = [c for c in text if "\u0c00" <= c <= "\u0c7f"]
    assert len(telugu_chars) >= 10, f"expected Telugu characters, got {text!r}"


def test_reads_telugu_from_a_small_page_that_needs_upscaling():
    text = _recognise(_render(TELUGU_LINES, size=22, width=520), "te")
    telugu_chars = [c for c in text if "\u0c00" <= c <= "\u0c7f"]
    assert len(telugu_chars) >= 6, f"small Telugu page produced {text!r}"


def test_telugu_hint_still_reads_the_english_on_a_mixed_page():
    text = _recognise(_render(TELUGU_LINES + ENGLISH_LINES), "te")
    assert "Chapter" in text or "One" in text, f"English words were lost: {text!r}"


def test_language_is_reported_as_telugu():
    provider = TesseractOCRProvider()
    result = asyncio.run(provider.process_image(_render(TELUGU_LINES), "te"))
    assert result.language == "te", f"detected {result.language}"
    assert result.engine == "tesseract"
    assert result.blocks, "expected at least one block for a page with text"


def test_a_blank_page_returns_empty_text_without_failing():
    blank = Image.new("RGB", (1200, 600), "white")
    buffer = BytesIO()
    blank.save(buffer, format="PNG")
    assert _recognise(buffer.getvalue(), "te").strip() == ""
