from __future__ import annotations

from abc import ABC, abstractmethod

from app.schemas import OcrResult, VoiceProfile


class OCRProvider(ABC):
    name: str

    @abstractmethod
    async def process_image(self, image_bytes: bytes, hint_language: str | None = None) -> OcrResult:
        raise NotImplementedError


class TTSProvider(ABC):
    name: str

    @abstractmethod
    async def synthesize(self, text: str, language: str, voice_id: str | None, speed: float) -> tuple[bytes, str]:
        """Return audio bytes and mime type."""
        raise NotImplementedError

    def supported_languages(self) -> list[str]:
        return ["en", "hi", "te"]


class VoiceProvider(ABC):
    name: str

    @abstractmethod
    async def create_voice(self, name: str, samples: list[bytes], user_email: str) -> VoiceProfile:
        raise NotImplementedError

    @abstractmethod
    async def get_voice(self, voice_id: str) -> VoiceProfile:
        raise NotImplementedError

    @abstractmethod
    async def list_voices(self, user_email: str) -> list[VoiceProfile]:
        raise NotImplementedError

    @abstractmethod
    async def delete_voice(self, voice_id: str) -> None:
        raise NotImplementedError
