# AI Multilingual Book Reader

Android app that turns printed pages and PDFs into a personal audiobook in **Telugu**, **Hindi**, and **English**. You scan or import a book, press Play, and listen. Reading position, bookmarks, notes, and cached speech stay on the phone.

The app never embeds cloud API keys. Cloud OCR, neural TTS, and voice cloning go through a FastAPI backend.

## What you can do

- Scan a physical page with the camera, edit the text, and add it to a book
- Import a PDF (text pages are extracted; scanned pages are OCR’d)
- Listen with highlighted text, speed control, and lock-screen playback
- Continue later from the last sentence
- Create a custom voice profile (only a voice you are authorized to use)
- Compare English / Hindi / Telugu speech quality before committing to a provider
- Work offline for saved books, cached audio, bookmarks, and notes

## Repository layout

```
android/     Kotlin + Jetpack Compose app (clean architecture)
backend/     FastAPI service and provider adapters
docs live at the repository root
```

## Architecture in one paragraph

The Android UI talks only to domain interfaces (`OcrEngine`, `TextToSpeechEngine`, `VoiceCloningEngine`, `LanguageDetector`, `TextProcessor`). On-device ML Kit handles English and Hindi OCR when it can. Telugu OCR, neural TTS, and voice cloning are backend calls. The backend selects providers from environment variables (`OCR_PROVIDER`, `TTS_PROVIDER`, `VOICE_PROVIDER`) so Google, Azure, ElevenLabs, or Tesseract/eSpeak can be swapped without changing the app.

## Honest capability notes

| Feature | On device | Backend (no paid keys) | Production cloud |
| --- | --- | --- | --- |
| English OCR | ML Kit Latin | Tesseract `eng` | Google Vision |
| Hindi OCR | ML Kit Devanagari | Tesseract `hin` | Google Vision |
| Telugu OCR | **Not supported by ML Kit** | Tesseract `tel` | Google Vision |
| English/Hindi/Telugu TTS | Android TTS (quality varies) | eSpeak-NG (robotic, **dev only**) | Google Chirp3-HD or Azure Neural |
| Custom cloned voice | Upload only | Mock profile (does **not** clone) | ElevenLabs Instant Voice Cloning — English supported; Hindi/Telugu experimental |

Do not treat Telugu or Hindi cloned speech as production-ready until you have listened on the Voice Test screen with your chosen provider.

## Quick start

### Backend

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
cp .env.example .env
# Install system packages: tesseract-ocr tesseract-ocr-eng tesseract-ocr-hin tesseract-ocr-tel espeak-ng
uvicorn app.main:app --host 0.0.0.0 --port 8080
```

### Android

1. Install Android Studio (Koala+), JDK 17, and Android SDK 35.
2. Open the `android/` folder.
3. Create `android/local.properties` with `sdk.dir=/path/to/Android/sdk`.
4. Point Settings → backend URL at your machine (`http://10.0.2.2:8080/` for the emulator).
5. Run the `app` configuration.

### Install on a phone (no Android Studio)

Android cannot silently replace an installed app, so how you get updates depends on what the phone allows. Settings → **Check for updates** detects this and shows only the route that can actually succeed.

| Phone state | What the app offers |
| --- | --- |
| Installed from Google Play | Play updates it in the background; the button opens the Play listing |
| Sideloading allowed | Download the APK and install it in place |
| Install permission not granted yet | One tap to Android's "allow installs" screen |
| Advanced Protection on | Explains the block and points at Play; no download that would fail |

**Advanced Protection blocks every sideload**, including Chrome, Files, and `adb install`
(`INSTALL_FAILED_USER_RESTRICTED`). No app can work around it, and this one does not pretend to.
To keep protection on, distribute through the Play internal testing track — see
[DEPLOYMENT.md](DEPLOYMENT.md#testers-with-advanced-protection) for the one-time Play Console setup
and `.github/workflows/play-internal.yml` for the upload job.

New test APKs are published as GitHub prereleases named `BookReader-{versionCode}-debug.apk` (see the rolling [`testing-latest`](https://github.com/thrinadsanjay/MultilingualBookReader/releases/tag/testing-latest) release after merges to `main`). Bump `versionCode` in `android/app/build.gradle.kts` whenever testers should receive an update. The repository must stay **public** so the app can list and download those releases without a token.

See [ARCHITECTURE.md](ARCHITECTURE.md), [API.md](API.md), [DEPLOYMENT.md](DEPLOYMENT.md), and the other docs in this folder.

## Tests

```bash
cd backend && pytest
cd android && ./gradlew :core:test :app:testDebugUnitTest
```

## License

GPL-3.0. See [LICENSE](LICENSE).
