# AI providers

Providers are selected with environment variables. Credentials never belong in the Android app.

```
OCR_PROVIDER=tesseract|google_vision
TTS_PROVIDER=espeak|google|azure|elevenlabs
VOICE_PROVIDER=mock|elevenlabs
```

## OCR

### Tesseract (default, local to the server)

- Languages: `eng`, `hin`, `tel`
- Good enough to unblock Telugu (ML Kit cannot OCR Telugu on device)
- Install: `tesseract-ocr tesseract-ocr-eng tesseract-ocr-hin tesseract-ocr-tel`

### Google Cloud Vision (`OCR_PROVIDER=google_vision`)

- Set `OCR_API_KEY` (API key) or a service account via `GOOGLE_APPLICATION_CREDENTIALS`
- Stronger layout/handwriting; required for production Telugu scan quality if Tesseract is insufficient

### On-device ML Kit (Android, not a backend provider)

- Latin → English
- Devanagari → Hindi
- **No Telugu model.** Composite OCR sends Telugu (or low-confidence) pages to the backend when online.

## Standard TTS (choose one for production)

| Provider | English | Hindi | Telugu | Notes |
| --- | --- | --- | --- | --- |
| Google Cloud TTS Chirp3-HD | Excellent | Chirp3-HD + Neural2 | Chirp3-HD | Recommended default once billed |
| Azure Speech Neural | Excellent | `hi-IN-SwaraNeural` | `te-IN-ShrutiNeural` | Strong Indic alternative |
| ElevenLabs multilingual v2 | Excellent | Variable | Unverified | Better as a *cloned* voice engine |
| eSpeak-NG | Intelligible | Intelligible | Intelligible | **Development only.** Robotic. Ships so the pipeline works without keys. |
| Android `TextToSpeech` | Device-dependent | Device-dependent | Device-dependent | Offline fallback |

Set `TTS_PROVIDER=google` and `GOOGLE_TTS_API_KEY=...` or `TTS_PROVIDER=azure` with `AZURE_SPEECH_KEY` + `AZURE_SPEECH_REGION`.

Use the in-app **Voice test** screen with:

- English: `Welcome to my book reader.`
- Hindi: `यह मेरी किताब है।`
- Telugu: `ఇది నా పుస్తకం.`

Do not freeze the production provider until those three sound acceptable.

## Voice cloning

| Provider | What it actually does |
| --- | --- |
| `mock` | Stores a profile. Does **not** clone. Speech still uses `TTS_PROVIDER`. Clearly labeled in the UI. |
| `elevenlabs` | Instant Voice Cloning. **Supported: English.** Hindi and Telugu are flagged `experimentalLanguages`. |

Azure Custom Neural Voice and similar studio products need a separate approval process; add a new `VoiceProvider` class when you have access. Do not pretend a model speaks Telugu in the user’s voice if the vendor does not list it.

## Switching providers

1. Change env vars.
2. Restart the API.
3. Keep the same REST contract.
4. Re-run the Voice test screen.
5. Only then set `supportedLanguages` on cloned profiles to include `hi` / `te`.
