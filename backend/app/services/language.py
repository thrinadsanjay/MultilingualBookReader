from __future__ import annotations

TELUGU = range(0x0C00, 0x0C80)
DEVANAGARI = range(0x0900, 0x0980)


def detect_language(text: str) -> str:
    counts = {"te": 0, "hi": 0, "en": 0}
    for char in text:
        code = ord(char)
        if code in TELUGU:
            counts["te"] += 1
        elif code in DEVANAGARI:
            counts["hi"] += 1
        elif char.isascii() and char.isalpha():
            counts["en"] += 1
    used = {key: value for key, value in counts.items() if value > 0}
    if not used:
        return "und"
    if len(used) > 1:
        return "mul"
    return max(used, key=used.get)


def detect_spans(text: str) -> list[tuple[str, str]]:
    if not text:
        return []
    spans: list[tuple[str, str]] = []
    current = _lang(text[0])
    start = 0
    for index, char in enumerate(text[1:], start=1):
        nxt = _lang(char)
        if nxt != "und" and current != "und" and nxt != current:
            spans.append((current, text[start:index]))
            start = index
            current = nxt
        elif current == "und":
            current = nxt
    spans.append((current, text[start:]))
    return [(lang, chunk) for lang, chunk in spans if chunk.strip()]


def _lang(char: str) -> str:
    code = ord(char)
    if char.isspace() or char in ".,;:!?\"'()[]-—–/।॥":
        return "und"
    if code in TELUGU:
        return "te"
    if code in DEVANAGARI:
        return "hi"
    if char.isascii() and char.isalpha():
        return "en"
    return "und"
