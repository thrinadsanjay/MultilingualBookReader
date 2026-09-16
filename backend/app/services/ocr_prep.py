"""Pure helpers for preparing a page image for Tesseract.

Telugu is the reason this exists: ML Kit has no on-device Telugu model, so those pages come here,
and Tesseract's Telugu accuracy collapses on small or low-contrast images.
"""

from __future__ import annotations

# Tesseract is trained on roughly 300 DPI scans. Anything narrower than this is upscaled first.
MIN_OCR_WIDTH = 1400
MAX_OCR_WIDTH = 3500

# Telugu books routinely mix English words, so the English model is loaded alongside.
LANGUAGE_CHAINS: dict[str, tuple[str, ...]] = {
    "te": ("tel", "eng"),
    "hi": ("hin", "eng"),
    "en": ("eng",),
}
DEFAULT_CHAIN: tuple[str, ...] = ("eng", "hin", "tel")


def normalise_hint(hint_language: str | None) -> str:
    """Turns 'te-IN', 'TE', or None into a bare lowercase language subtag."""
    return (hint_language or "").strip().lower().split("-")[0].split("_")[0]


def tesseract_language(hint_language: str | None, available: set[str] | None = None) -> str:
    """Builds the Tesseract `-l` argument, dropping models the server does not have installed."""
    chain = LANGUAGE_CHAINS.get(normalise_hint(hint_language), DEFAULT_CHAIN)
    if available is not None:
        installed = tuple(code for code in chain if code in available)
        if not installed:
            installed = tuple(code for code in DEFAULT_CHAIN if code in available)
        chain = installed or ("eng",)
    return "+".join(chain)


def missing_languages(hint_language: str | None, available: set[str]) -> list[str]:
    """Models this request wanted but the server cannot load, so the caller can say so plainly."""
    chain = LANGUAGE_CHAINS.get(normalise_hint(hint_language), DEFAULT_CHAIN)
    return [code for code in chain if code not in available]


def upscale_factor(width: int, min_width: int = MIN_OCR_WIDTH, max_width: int = MAX_OCR_WIDTH) -> float:
    """How much to enlarge a page so small text is legible, without exploding huge scans."""
    if width <= 0:
        return 1.0
    if width >= min_width:
        return 1.0
    factor = min_width / width
    if width * factor > max_width:
        factor = max_width / width
    return round(factor, 3)


def page_segmentation_modes(hint_language: str | None) -> tuple[int, ...]:
    """PSM values to try in order. 6 suits a single column of body text; 4 and 3 are fallbacks."""
    del hint_language
    return (6, 4, 3)
