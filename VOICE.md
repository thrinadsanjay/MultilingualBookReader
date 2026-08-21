# Voice

## Product rule

Users may only clone a voice they are authorized to use. The create-voice screen requires an explicit consent checkbox. There is no “clone from a file you found” shortcut.

## Profile fields

`id`, `name`, `provider`, `providerVoiceId`, `supportedLanguages`, `experimentalLanguages`, `status`, `isCloned`, `qualityNote`, timestamps.

`supportedLanguages` is a claim we are willing to show as Ready. `experimentalLanguages` means “the vendor might produce audio; listen before you trust it.”

## Recording guidance (shown in the app)

- Quiet room, no music
- Phone 15–20 cm away
- Speak naturally, avoid clipping
- At least three samples, eight seconds or more each

## Lifecycle

1. Record samples on device (held in app-private storage).
2. Upload over HTTPS to `POST /api/v1/voice/create`.
3. Provider returns a profile. Samples are deleted locally after a successful create.
4. Reader uses `providerVoiceId` for TTS.
5. Delete: `DELETE /api/v1/voice/{id}` + wipe local samples + wipe audio cache rows for that voice.

## Multilingual custom voice — current truth

If the user records mostly English:

- **ElevenLabs multilingual v2** can attempt Hindi and Telugu. Quality is not guaranteed and is **not** marked supported in this repo.
- **eSpeak / mock** cannot speak in the user’s voice at all.
- **Google / Azure standard voices** are high quality in all three languages but are **not** the user’s clone.

The Voice Test screen exists so this decision is made with ears, not marketing copy.

## Offline

Creating or refreshing a cloned voice needs the network. Reading with an already-cached clone works offline. If cache misses while offline, the app falls back to the device speech engine and says so.
