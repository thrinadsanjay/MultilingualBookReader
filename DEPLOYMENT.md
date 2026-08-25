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

## Telugu OCR server

English and Hindi are recognised on the phone. Telugu has no on-device model, so those pages are
sent to this backend. It is the only part of reading that needs a server.

### Host it

```bash
cd backend
cp .env.example .env
# set at least these two:
#   API_KEY=$(openssl rand -base64 32)
#   SECRET_KEY=$(openssl rand -base64 32)
docker compose up -d --build
```

The image installs `tesseract-ocr-tel` and the build fails if the Telugu data is missing, so a
container that starts can read Telugu. Put it behind HTTPS (Caddy, nginx, or a tunnel): the phone
sends the API key on every request, and Android blocks plain `http` to anything but localhost.

### Check it

```bash
curl https://your-server/api/v1/ocr/health
```

```json
{"provider":"tesseract","languages":["eng","hin","osd","tel"],"telugu_ready":true,"requires_api_key":true}
```

`telugu_ready: false` means the container is missing `tesseract-ocr-tel`. This endpoint needs no
credentials so a deployment can be verified from a browser; it exposes nothing but capability.

### Read a page

```bash
curl -X POST https://your-server/api/v1/ocr \
  -H "X-API-Key: $API_KEY" \
  -F image=@page.jpg \
  -F hint_language=te
```

Returns `{ "text": ..., "language": "te", "confidence": 0.0-1.0, "blocks": [...], "engine": "tesseract" }`.
A wrong or missing key returns 401. If the server is running but Telugu data is absent, `/ocr`
returns **503** with an explanation rather than pretending the page was unreadable.

The same key also unlocks `/tts` and the audio endpoints, so a single-user deployment never needs
to create accounts. User accounts still work if you prefer them; leave `API_KEY` empty.

### Point the phone at it

Settings → **Reading server** → enter the address and key → **Test connection**. The reply states
whether Telugu is ready and which models are installed. Nothing is rebuilt; the app rewrites its
requests onto whatever address is saved.

### Tuning notes

Telugu accuracy depends heavily on image quality. The server converts pages to grayscale, stretches
contrast, and upscales anything narrower than 1400 px, because Tesseract is trained on roughly
300 DPI scans and Telugu conjuncts lose their shape at low resolution. It tries page segmentation
modes 6, 4, then 3, and loads `tel+eng` together since Telugu books usually contain English words.
For higher accuracy at the cost of a cloud dependency, set `OCR_PROVIDER=google_vision`.

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

- Filename: `Svara-{versionCode}-debug.apk` (example: `Svara-10-debug.apk`); the older `BookReader-{versionCode}-debug.apk` name is still parsed
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

The only distribution that works with protection left on is Google Play. After the first Play
install, `InstallChannel.PLAY` takes over: Play updates the app in the background and Settings →
Check for updates just opens the Play listing.

## Publishing to Google Play

`.github/workflows/play-internal.yml` builds a signed bundle and uploads it to the internal testing
track. Everything below the line is already done in this repo; everything above it needs a human
with a Play Console account.

### What only you can do

1. **Create a Play Console developer account** (one-time 25 USD, plus identity and, for individual
   accounts, D-U-N-S-free personal verification). CI cannot create or pay for this.
2. **Create the app** named **Svara** with package name `com.multilingualbookreader`. The package
   name is permanent and deliberately unchanged by the rename: changing it would orphan every
   existing install. Check that the name "Svara" is free on Play before committing to the listing.
3. **Generate an upload key** and keep it somewhere safe — losing it means you cannot ship updates:
   `keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
4. **Create a Google Cloud service account**, grant it release permissions in Play Console, and
   download its JSON key.
5. **Add repository secrets**: `PLAY_SERVICE_ACCOUNT_JSON`, `PLAY_KEYSTORE_BASE64`
   (`base64 -w0 upload.jks`), `PLAY_KEYSTORE_PASSWORD`, `PLAY_KEY_ALIAS`, `PLAY_KEY_PASSWORD`, and
   `BOOKREADER_API_BASE_URL` pointing at your deployed backend.
6. **Complete the Play Console forms**: store listing, content rating questionnaire, target
   audience, and the Data safety form. This app collects **camera images** (scanned pages) and
   **audio recordings** (voice samples, only when a voice profile is created); both are processed
   through your own backend, so declare them accordingly.
7. **Host a privacy policy** at a public URL and paste it into the listing. Play requires one
   because the app requests camera and microphone access.
8. **Run closed testing.** Personal developer accounts created after November 2023 must run a closed
   test with at least 12 testers for 14 continuous days before production access is granted.
   Internal testing installs immediately and is enough for your own devices.

### Already handled in this repo

- `targetSdk`/`compileSdk` are **36**, which Play requires for new apps and updates submitted from
  31 August 2026.
- `./gradlew :app:bundleRelease` produces a signed AAB when the keystore environment variables are
  set, and R8 completes (pdfbox's optional JPEG 2000 decoder is suppressed in `proguard-rules.pro`).
- Every bundled native library is 16 KB page aligned, which Play requires for apps targeting
  Android 15 and above.
- `REQUEST_INSTALL_PACKAGES` is declared only in `src/debug/AndroidManifest.xml`. Play treats it as
  a sensitive permission, and release builds do not need it because Play performs the update.
- Release builds set `SELF_INSTALL_SUPPORTED=false`, so the in-app updater always points at Play
  rather than downloading anything itself.
- The release backend URL comes from `-PreleaseApiBaseUrl=` or `BOOKREADER_API_BASE_URL`. The
  default `https://api.example.com/` is a placeholder: ship a real URL or voice cloning and cloud
  OCR will fail. Scanning, on-device OCR, and system text-to-speech work without a backend.
- Draft store screenshots for both themes are generated by `ScreenRenderTest` into
  `android/app/build/screenshots/`.

The keystore is read from `BOOKREADER_KEYSTORE_PATH` at build time and is never committed.

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
