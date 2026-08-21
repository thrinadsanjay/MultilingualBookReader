# OCR

## Engines

`interface OcrEngine { suspend fun recognize(imageBytes: ByteArray, hintLanguage): OcrResult }`

`OcrResult` includes full text, detected language, confidence, blocks, lines, words, and bounding boxes when the engine provides them.

Implementations:

1. `MlKitOcrEngine` — on-device Latin + Devanagari. Offline. No Telugu.
2. `BackendOcrEngine` — Tesseract or Google Vision via the API. Required for Telugu.
3. `CompositeOcrEngine` — respects Settings (`ON_DEVICE` / `CLOUD` / `AUTO`). AUTO uses ML Kit unless the hint is Telugu, confidence is low, or the device is online and local text is empty.

## Camera path

1. CameraX preview with a page frame overlay.
2. Manual capture (auto-capture is gated on a blur score; blurry shots can still be used).
3. Crop to the overlay, light contrast enhancement.
4. OCR → editor → **Use Page** / **Retake**.
5. Repeat; pages append to one book. **Read book** opens the reader.

Google ML Kit Document Scanner can be added later as an alternative capture UX without changing `OcrEngine`.

## PDF path

For each page:

- If PdfBox extracts ≥ 40 characters, treat as selectable text. **Do not OCR.**
- Otherwise render that page with `PdfRenderer` (bounded size) and OCR the JPEG.

Pages keep their PDF page numbers.

## Cleanup pipeline (`DefaultTextProcessor`)

```
raw OCR
 → strip NULs
 → drop obvious repeated headers/footers
 → join hyphenated line breaks (`informa-\ntion` → `information`)
 → join wrapped lines
 → normalize whitespace / light punctuation
 → language detect
 → prepareForSpeech (collapse bullets)
 → TTS
```

Indic words are not spell-corrected. Users always see an editor for camera pages and can edit saved page text in the database.

## Languages

Script ranges:

- Telugu `U+0C00–U+0C7F`
- Devanagari (Hindi) `U+0900–U+097F`
- Latin letters → English

Mixed content returns `MIXED` / `mul`.
