# Testing

## JVM (`:core`)

No Android SDK required:

- Language detection (English, Hindi, Telugu, mixed)
- OCR cleanup (hyphen joins, Indic preservation)
- Sentence segmentation
- Audio cache key stability (content never appears in the hash)

```bash
cd android && ./gradlew :core:test
```

## Android unit tests

- Room in-memory insert of a Telugu title
- OCR cleanup pipeline
- Robolectric SDK 34

```bash
./gradlew :app:testDebugUnitTest
```

## Instrumented / Compose UI

`androidTest` includes a Home screen smoke test. Add device tests for Scan, PDF import, Library, Reader, Voice, and Settings as hardware becomes available. CameraX and Media3 need a device or emulator with Play services for ML Kit.

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Backend

```bash
cd backend
pytest
```

Coverage:

- Script language detection matching the Android tests
- Audio cache key + disk round-trip (Telugu text hashed, not stored as the filename plaintext beyond the hash)
- `/health`, registration, `/status`, TTS (200 if eSpeak is installed, 502 if not — both acceptable)

## Manual multilingual checklist

1. Import an English digital PDF → Play.
2. Import a Hindi digital PDF → Play.
3. Import a Telugu digital PDF → Play.
4. Import a scanned PDF and confirm page progress does not freeze the UI.
5. Scan a physical page or pick a photo from the gallery, edit OCR, save, Play.
6. Highlighting follows the spoken sentence.
7. Pause / speed / lock screen.
8. Kill the app, reopen, Continue.
9. Voice Test: English, Hindi, Telugu.
10. Create and delete a voice profile; confirm cached audio for that voice is gone.
11. Airplane mode: open a previously read book and play cached speech. New OCR/TTS must show Offline, not a silent failure.

## Fixture strings

```
Today we will read a good story.
आज हम एक अच्छी कहानी पढ़ेंगे।
ఈ రోజు మనం ఒక మంచి కథ చదువుదాం.
రాముడు went to the market.
```
