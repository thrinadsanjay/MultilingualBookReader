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

The debug app checks public GitHub Releases and can install a newer APK from Settings → Check for updates.

- Filename: `BookReader-{versionCode}-debug.apk` (example: `BookReader-3-debug.apk`)
- Or put `versionCode=3` in the release body if the filename has no integer code
- Bump `versionCode` in `android/app/build.gradle.kts` for every build testers should receive
- CI workflow `.github/workflows/publish-debug-apk.yml` publishes a rolling `testing-latest` prerelease on pushes to `main`
- The user still confirms the Android package installer; the app cannot replace itself silently
- In-app checks use the unauthenticated GitHub API, so the repository must be public

## Testers with Advanced Protection

Android 16's Advanced Protection permanently revokes `REQUEST_INSTALL_PACKAGES` for every app and
rejects `adb install` with `INSTALL_FAILED_USER_RESTRICTED`. Chrome, Files, a second phone, and a
laptop are all blocked the same way, so **no sideload path exists** while it is on. The app detects
this (`InstallChannel.BLOCKED`) and stops offering a download it could never install.

The only distribution that works with protection left on is Google Play. `.github/workflows/play-internal.yml`
builds a signed bundle and uploads it to the internal testing track. One-time setup:

1. Create a Play Console developer account and an app with package `com.multilingualbookreader`.
2. Generate an upload key: `keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`.
3. Create a Google Cloud service account, grant it release permissions in Play Console, and download its JSON key.
4. Add repository secrets: `PLAY_SERVICE_ACCOUNT_JSON`, `PLAY_KEYSTORE_BASE64` (`base64 -w0 upload.jks`), `PLAY_KEYSTORE_PASSWORD`, `PLAY_KEY_ALIAS`, `PLAY_KEY_PASSWORD`.
5. Add the tester's Google account to the internal testing track and have them install from the opt-in link once.

After that first Play install, `InstallChannel.PLAY` takes over: Play updates the app in the
background and Settings → Check for updates just opens the Play listing. The keystore is read from
`BOOKREADER_KEYSTORE_PATH` at build time and is never committed.

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
