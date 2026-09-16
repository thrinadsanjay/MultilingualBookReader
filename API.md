# API

Base URL: `https://<host>/api/v1`

All mutating AI endpoints require `Authorization: Bearer <access_token>` except `/status` and `/health`.

HTTPS is required in production. The Android debug build may use `http://10.0.2.2:8080/` for a local emulator.

## Auth

### `POST /auth/register`

```json
{ "email": "reader@example.com", "password": "at-least-8-chars" }
```

Response:

```json
{ "access_token": "<jwt>", "refresh_token": "<jwt>", "email": "reader@example.com" }
```

### `POST /auth/login`

Same body and response. Failed login returns `401` with a generic message.

## OCR

### `POST /ocr` (`multipart/form-data`)

| Field | Type |
| --- | --- |
| `image` | JPEG/PNG bytes |
| `hint_language` | optional `en` / `hi` / `te` |

Response: `OcrResult`

```json
{
  "text": "...",
  "language": "te",
  "confidence": 0.91,
  "engine": "tesseract",
  "blocks": [
    {
      "text": "...",
      "confidence": 0.9,
      "language": "te",
      "bounding_box": { "left": 0, "top": 0, "right": 100, "bottom": 40 },
      "lines": [{ "text": "...", "words": [{ "text": "..." }] }]
    }
  ]
}
```

The server does not log `text`.

## TTS

### `POST /tts`

```json
{ "text": "ఇది నా పుస్తకం.", "language": "te", "voice_id": "standard", "speed": 1.0 }
```

Returns audio bytes (`audio/mpeg` or `audio/wav`) with header `X-Audio-Cache: hit|miss`.

### `POST /audio/generate`

Same body. Returns `{ "id": "<cache key>", "cached": true, "mime_type": "audio/wav" }`.

### `GET /audio/{id}`

Returns previously generated audio.

## Voice

### `POST /voice/create` (`multipart/form-data`)

| Field | Type |
| --- | --- |
| `name` | string |
| `consent` | `true` |
| `samples` | ≥ 3 audio files |

Creates a provider-backed voice profile. The mock provider stores metadata only and **does not clone the voice**.

### `GET /voices` · `GET /voice/{id}` · `DELETE /voice/{id}`

Deleting a voice asks the provider to drop the model and should be paired with the app deleting local samples and cached audio.

## Status

### `GET /status`

```json
{
  "online": true,
  "ocr_provider": "tesseract",
  "tts_provider": "espeak",
  "voice_provider": "mock",
  "notes": {
    "voice": "Custom cloned Telugu/Hindi is experimental unless the selected provider lists those languages as supported."
  }
}
```

### `GET /health`

Liveness probe. No secrets.

## Errors

| Code | Meaning shown to the user |
| --- | --- |
| 401 | Sign-in required / failed |
| 400 | Empty image, missing consent, too few samples |
| 429 | Too many requests |
| 502 | OCR or speech provider failed |
| 500 | Generic failure (no stack traces) |

## Rate limiting

Default `RATE_LIMIT=30/minute` via SlowAPI. Override per environment.
