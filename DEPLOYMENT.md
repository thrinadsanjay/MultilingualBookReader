# Deployment

## Development machine

### Required

- JDK 17+ (Android Studio bundled JDK is fine)
- Android Studio with SDK 35, Build-Tools 35.0.0
- Python 3.12
- `tesseract-ocr`, `tesseract-ocr-eng`, `tesseract-ocr-hin`, `tesseract-ocr-tel`
- `espeak-ng` (only if `TTS_PROVIDER=espeak`)

Ubuntu:

```bash
sudo apt-get install openjdk-17-jdk python3-venv python3-pip \
  tesseract-ocr tesseract-ocr-eng tesseract-ocr-hin tesseract-ocr-tel espeak-ng
```

macOS (Homebrew):

```bash
brew install tesseract tesseract-lang espeak
```

Windows: install Android Studio, Python 3.12, Tesseract from UB Mannheim, and add it to `PATH`.

### Backend

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements-dev.txt
cp .env.example .env
# edit SECRET_KEY even in development
uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

### Android

```bash
cd android
# create local.properties: sdk.dir=...
./gradlew :app:assembleDebug
```

Emulator backend URL is already `http://10.0.2.2:8080/` in the debug BuildConfig. A physical phone needs your computer’s LAN IP and HTTP cleartext only for that IP (update `network_security_config.xml`).

## Production backend

1. Set `DEBUG=false`.
2. Generate a long `SECRET_KEY`.
3. Put the API behind HTTPS (Caddy, nginx, or a cloud load balancer). Disable cleartext.
4. Choose production providers:

```
OCR_PROVIDER=google_vision
TTS_PROVIDER=google
VOICE_PROVIDER=elevenlabs
```

5. Inject secrets from your host (Cloud Run secrets, Kubernetes secrets, systemd EnvironmentFile). Never bake them into the image.
6. Persist `AUDIO_CACHE_DIR` and `VOICE_SAMPLE_DIR` on a volume. Voice samples should be deleted after the provider accepts them if you do not need them for support.
7. Run database backups if you move off SQLite.

Docker:

```bash
cd backend
docker build -t book-reader-api .
docker run --env-file .env -p 8080:8080 book-reader-api
```

## Sideload test updates

The debug app checks public GitHub Releases and can install a newer APK from Home or Settings.

- Filename: `BookReader-{versionCode}-debug.apk` (example: `BookReader-3-debug.apk`)
- Or put `versionCode=3` in the release body if the filename has no integer code
- Bump `versionCode` in `android/app/build.gradle.kts` for every build testers should receive
- CI workflow `.github/workflows/publish-debug-apk.yml` publishes a rolling `testing-latest` prerelease on pushes to `main`
- The user still confirms the Android package installer; the app cannot replace itself silently
- If Advanced Protection or a work policy blocks “Install unknown apps” for Book Reader, the app opens the APK in Chrome or Files instead. It cannot whitelist itself against that policy.
- In-app checks use the unauthenticated GitHub API, so the repository must be public

## Production Android

- `release` BuildConfig uses `https://api.example.com/` — replace with your host before shipping.
- Enable R8 (already on for `release`).
- Upload the Play signing key yourself; this repo contains none.
- Request camera, microphone, and notification permissions only when the user taps Scan / Voice / Play.

## Configuration matrix

| Env | OCR | TTS | Voice |
| --- | --- | --- | --- |
| Laptop, no cloud keys | tesseract | espeak | mock |
| Staging | tesseract or vision | google | elevenlabs (test voices) |
| Production | google_vision | google or azure | elevenlabs (after Voice Test) |
