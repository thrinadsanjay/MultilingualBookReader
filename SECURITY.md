# Security

## Threats this MVP actually addresses

- Cloud vendor keys leaking from the APK
- Book contents and voice samples appearing in logs
- Casual backup extraction of tokens (`allowBackup=false`)
- Unauthenticated use of expensive OCR/TTS
- Accidental impersonation features (consent required; no “clone any audio file from the internet” flow)

## Rules

1. **No API keys in Android source, BuildConfig, or resources.** Only a backend base URL.
2. **HTTPS only** in production (`network_security_config` allows cleartext solely for emulator localhost).
3. **Passwords** hashed with bcrypt. JWT secret from `SECRET_KEY`.
4. **Tokens** on device sit in EncryptedSharedPreferences (Android Keystore).
5. **Voice samples** live in app-private storage, are uploaded over TLS, and are deleted when the user deletes the profile.
6. **Logging** records event names and metadata (`engine`, `language`, `cached`, `bytes`). It must never include page text, sample audio, passwords, or tokens. Verbose logs compile out of release (`ENABLE_VERBOSE_LOGS=false`).
7. **Rate limits** on the API default to 30 requests / minute / IP.
8. **Error bodies** are human sentences, not stack traces.
9. **Privacy screen** explains local storage, cloud use, and deletion.

## Data inventory

| Data | Stored | Leaves the device |
| --- | --- | --- |
| Book images/text | App files + Room | Only if user OCR/TTS via backend |
| Reading progress, bookmarks, notes | Room | No |
| Auth JWT | Encrypted prefs | Authorization header |
| Voice samples | Temporary files | `POST /voice/create` |
| Cached speech | App files | No (playback local) |

## Operator checklist

- Rotate `SECRET_KEY` and provider keys independently.
- Restrict CORS in production.
- Keep provider dashboards on least-privilege keys (TTS-only, Vision-only).
- Do not enable analytics unless the user opts in (defaults off).
