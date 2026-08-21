from __future__ import annotations

import logging

import httpx

from app.config import get_settings
from app.providers.base import TTSProvider

logger = logging.getLogger(__name__)

GOOGLE_VOICES = {
    "en": "en-IN-Chirp3-HD-Achernar",
    "hi": "hi-IN-Chirp3-HD-Achernar",
    "te": "te-IN-Chirp3-HD-Achernar",
}


class GoogleTTSProvider(TTSProvider):
    name = "google"

    async def synthesize(self, text: str, language: str, voice_id: str | None, speed: float) -> tuple[bytes, str]:
        settings = get_settings()
        api_key = settings.google_tts_api_key or settings.tts_api_key
        if not api_key:
            raise RuntimeError("Google TTS credentials are not configured.")
        lang = language.split("-")[0]
        locale = {"en": "en-IN", "hi": "hi-IN", "te": "te-IN"}.get(lang, "en-IN")
        voice = voice_id if voice_id and voice_id != "standard" else GOOGLE_VOICES.get(lang, GOOGLE_VOICES["en"])
        payload = {
            "input": {"text": text},
            "voice": {"languageCode": locale, "name": voice},
            "audioConfig": {"audioEncoding": "MP3", "speakingRate": max(0.5, min(speed, 2.0))},
        }
        async with httpx.AsyncClient(timeout=60) as client:
            response = await client.post(
                f"https://texttospeech.googleapis.com/v1/text:synthesize?key={api_key}",
                json=payload,
            )
            if response.status_code == 429:
                raise RuntimeError("rate_limited")
            response.raise_for_status()
            audio_b64 = response.json()["audioContent"]
        import base64

        logger.info("tts_complete provider=%s language=%s bytes=%s", self.name, lang, "omitted")
        return base64.b64decode(audio_b64), "audio/mpeg"
