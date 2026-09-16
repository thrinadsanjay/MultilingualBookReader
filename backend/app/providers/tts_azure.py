from __future__ import annotations

import logging

import httpx

from app.config import get_settings
from app.providers.base import TTSProvider

logger = logging.getLogger(__name__)


class AzureTTSProvider(TTSProvider):
    name = "azure"

    async def synthesize(self, text: str, language: str, voice_id: str | None, speed: float) -> tuple[bytes, str]:
        settings = get_settings()
        if not settings.azure_speech_key or not settings.azure_speech_region:
            raise RuntimeError("Azure Speech credentials are not configured.")
        lang = language.split("-")[0]
        mapping = {
            "en": ("en-IN", "en-IN-NeerjaNeural"),
            "hi": ("hi-IN", "hi-IN-SwaraNeural"),
            "te": ("te-IN", "te-IN-ShrutiNeural"),
        }
        locale, voice = mapping.get(lang, mapping["en"])
        rate = int((speed - 1.0) * 100)
        ssml = f"""<speak version='1.0' xml:lang='{locale}'>
  <voice name='{voice_id if voice_id and voice_id != "standard" else voice}'>
    <prosody rate='{rate}%'>{_escape(text)}</prosody>
  </voice>
</speak>"""
        async with httpx.AsyncClient(timeout=60) as client:
            token_resp = await client.post(
                f"https://{settings.azure_speech_region}.api.cognitive.microsoft.com/sts/v1.0/issueToken",
                headers={"Ocp-Apim-Subscription-Key": settings.azure_speech_key},
            )
            token_resp.raise_for_status()
            audio = await client.post(
                f"https://{settings.azure_speech_region}.tts.speech.microsoft.com/cognitiveservices/v1",
                headers={
                    "Authorization": f"Bearer {token_resp.text}",
                    "Content-Type": "application/ssml+xml",
                    "X-Microsoft-OutputFormat": "audio-24khz-48kbitrate-mono-mp3",
                },
                content=ssml.encode("utf-8"),
            )
            audio.raise_for_status()
        logger.info("tts_complete provider=%s language=%s", self.name, lang)
        return audio.content, "audio/mpeg"


def _escape(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "&apos;")
    )
