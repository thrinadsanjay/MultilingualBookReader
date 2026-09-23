# Architecture

## Layers

```
Presentation (Compose + ViewModels)
        ↓
Domain (models, use cases, engine interfaces)
        ↓
Data / AI adapters (Room, Retrofit, ML Kit, Media3, WorkManager)
        ↓ HTTPS
FastAPI
        ↓
OCR / TTS / Voice providers (env-selected)
```

The Android app is split into two Gradle modules:

- `:core` — pure Kotlin JVM. Language detection, OCR cleanup, sentence segmentation, cache keys, domain models. No Android imports.
- `:app` — Compose UI, Room, CameraX, Media3, Hilt, ML Kit, PDF import, WorkManager.

Packages under `app/src/main/java/com/multilingualbookreader/` follow the requested structure: `presentation`, `domain` (via `:core`), `data`, `ocr`, `tts`, `voice`, `audio`, `camera`, `pdf`, `storage`, `database`, `network`, `worker`, `common`.

## Why not one cloud vendor in the app

Google, Azure, and ElevenLabs keys must never ship in the APK. The only credential a build may
carry is the shared Svara `API_KEY`, so phones can call your nginx front-end without opening
Settings. Cloud vendor keys stay in GitHub secrets and the server `.env`. The app has:

- `OcrEngine` / `TextToSpeechEngine` / `VoiceCloningEngine`
- A composite implementation that prefers on-device work, then the backend
- Retrofit DTOs that match backend Pydantic models

The backend has:

- `OCRProvider`, `TTSProvider`, `VoiceProvider`
- `app/providers/factory.py` reading `OCR_PROVIDER`, `TTS_PROVIDER`, `VOICE_PROVIDER`

## Local vs cloud

| Work | Where | Notes |
| --- | --- | --- |
| PDF text extraction | Device, page by page | PdfBox + `PdfRenderer`. Never loads a 500-page file as bitmaps at once. |
| Scanned PDF OCR | Device ML Kit, else backend | Telugu requires backend Tesseract/Vision. |
| Camera capture, crop, blur score | Device | CameraX + overlay frame. |
| Language detection | Device + backend, script-based | Deterministic for `en` / `hi` / `te` / mixed. |
| OCR cleanup | Device `:core` | Conservative; user can always edit. |
| Standard neural TTS | Backend | Google Chirp3-HD or Azure Neural recommended. |
| Offline speech fallback | Device `TextToSpeech` | Quality depends on installed system voices. |
| Audio cache | Device files + backend disk | Key = SHA-256(text + language + voice + speed + provider). |
| Voice cloning | Backend only | Samples uploaded over HTTPS; deleted with the profile. |
| Reading progress, bookmarks, notes | Room | Offline. |
| Auth tokens | EncryptedSharedPreferences | Android Keystore. |

## Reading pipeline

```
Page text
  → TextProcessor.clean
  → SentenceSegmenter (language per sentence)
  → TextToSpeechEngine.synthesize (cache first)
  → Media3 playlist
  → highlight current sentence + persist ReadingProgress
```

Mixed lines such as `రాముడు went to the market.` are marked `MIXED`. The backend TTS provider still receives the original sentence; higher-quality providers can switch languages internally. Fine-grained per-span synthesis is a follow-up behind the same `LanguageDetector.detectSpans` API.

## Large books

`PdfImportProcessor` walks `PdfRenderer.pageCount` one index at a time. Each page is stored with `PENDING → PROCESSING → COMPLETED | FAILED`. WorkManager runs the import off the UI thread. Failed scanned pages can be retried later without re-importing the whole file.

## Audio in the background

`BookPlaybackService` is a Media3 `MediaSessionService` (`foregroundServiceType=mediaPlayback`). That gives lock-screen controls, Bluetooth, and interruption handling. The reader UI is a `MediaController`.

## Security boundaries

- No provider keys in Android BuildConfig besides a backend base URL.
- Backend secrets only in environment variables.
- Logs record engine name, language code, byte sizes, cache hit/miss — never book text, samples, tokens, or keys.
- `allowBackup=false`. Voice samples are deleted with the profile.

## Future extension points

EPUB, extra Indian languages, summaries, and iOS can attach new `OcrEngine` / importer implementations without changing Compose screens. Cloud sync would be a new repository implementation beside Room, not a UI rewrite.
