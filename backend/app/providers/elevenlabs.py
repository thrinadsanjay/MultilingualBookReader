from __future__ import annotations

import logging

import httpx

from app.config import get_settings
from app.providers.base import TTSProvider, VoiceProvider
from app.schemas import VoiceProfile

logger = logging.getLogger(__name__)

ELEVEN_TTS_URL = "https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"
ELEVEN_VOICES_URL = "https://api.elevenlabs.io/v1/voices/add"


class ElevenLabsTTSProvider(TTSProvider):
    name = "elevenlabs"

    def supported_languages(self) -> list[str]:
        # Instant Voice Cloning is strongest in English. Hindi/Telugu must be tested.
        return ["en"]

    async def synthesize(self, text: str, language: str, voice_id: str | None, speed: float) -> tuple[bytes, str]:
        settings = get_settings()
        api_key = settings.elevenlabs_api_key or settings.tts_api_key
        if not api_key:
            raise RuntimeError("ElevenLabs credentials are not configured.")
        voice = voice_id or "JBFqnCBsd6RMkjVDRZzb"
        async with httpx.AsyncClient(timeout=90) as client:
            response = await client.post(
                ELEVEN_TTS_URL.format(voice_id=voice),
                headers={"xi-api-key": api_key, "accept": "audio/mpeg"},
                json={
                    "text": text,
                    "model_id": "eleven_multilingual_v2",
                    "voice_settings": {"stability": 0.4, "similarity_boost": 0.8, "speed": max(0.7, min(speed, 1.2))},
                },
            )
            if response.status_code == 429:
                raise RuntimeError("rate_limited")
            response.raise_for_status()
        logger.info("tts_complete provider=%s language=%s", self.name, language.split("-")[0])
        return response.content, "audio/mpeg"


class ElevenLabsVoiceProvider(VoiceProvider):
    name = "elevenlabs"

    async def create_voice(self, name: str, samples: list[bytes], user_email: str) -> VoiceProfile:
        settings = get_settings()
        api_key = settings.elevenlabs_api_key or settings.voice_api_key
        if not api_key:
            raise RuntimeError("ElevenLabs credentials are not configured.")
        files = [("files", (f"sample-{i}.wav", sample, "audio/wav")) for i, sample in enumerate(samples)]
        async with httpx.AsyncClient(timeout=120) as client:
            response = await client.post(
                ELEVEN_VOICES_URL,
                headers={"xi-api-key": api_key},
                data={"name": name},
                files=files,
            )
            response.raise_for_status()
            data = response.json()
        voice_id = data.get("voice_id") or data.get("voiceId")
        return VoiceProfile(
            id=voice_id,
            name=name,
            provider=self.name,
            provider_voice_id=voice_id,
            supported_languages=["en"],
            experimental_languages=["hi", "te"],
            status="READY",
            is_cloned=True,
            quality_note=(
                "English cloned speech is the only quality we treat as supported. "
                "Hindi and Telugu are experimental on this provider — use the Voice Test screen before relying on them."
            ),
        )

    async def get_voice(self, voice_id: str) -> VoiceProfile:
        return VoiceProfile(
            id=voice_id,
            name="Custom voice",
            provider=self.name,
            provider_voice_id=voice_id,
            supported_languages=["en"],
            experimental_languages=["hi", "te"],
            status="READY",
        )

    async def list_voices(self, user_email: str) -> list[VoiceProfile]:
        return []

    async def delete_voice(self, voice_id: str) -> None:
        settings = get_settings()
        api_key = settings.elevenlabs_api_key or settings.voice_api_key
        if not api_key:
            return
        async with httpx.AsyncClient(timeout=30) as client:
            await client.delete(
                f"https://api.elevenlabs.io/v1/voices/{voice_id}",
                headers={"xi-api-key": api_key},
            )
