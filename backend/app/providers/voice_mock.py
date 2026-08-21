from __future__ import annotations

import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db import VoiceRecord
from app.providers.base import VoiceProvider
from app.schemas import VoiceProfile


class MockVoiceProvider(VoiceProvider):
    """
    Development stand-in. It stores a profile locally but does NOT clone the user's
    voice. Speech still comes from the configured TTS_PROVIDER.
    """

    name = "mock"

    def __init__(self, session_factory):
        self._session_factory = session_factory

    async def create_voice(self, name: str, samples: list[bytes], user_email: str) -> VoiceProfile:
        del samples  # samples are discarded after validation; never logged
        voice_id = str(uuid.uuid4())
        profile = VoiceProfile(
            id=voice_id,
            name=name,
            provider=self.name,
            provider_voice_id=voice_id,
            supported_languages=[],
            experimental_languages=[],
            status="READY",
            is_cloned=False,
            quality_note=(
                "DEVELOPMENT MOCK: this does not clone your voice. "
                "Configure VOICE_PROVIDER=elevenlabs and ELEVENLABS_API_KEY for real cloning. "
                "Until then, books are read with the standard TTS provider."
            ),
        )
        async with self._session_factory() as session:
            session.add(
                VoiceRecord(
                    id=profile.id,
                    user_email=user_email,
                    name=profile.name,
                    provider=profile.provider,
                    provider_voice_id=profile.provider_voice_id,
                    supported_languages="",
                    experimental_languages="",
                    status=profile.status,
                    is_cloned=False,
                    quality_note=profile.quality_note,
                )
            )
            await session.commit()
        return profile

    async def get_voice(self, voice_id: str) -> VoiceProfile:
        async with self._session_factory() as session:
            record = await session.get(VoiceRecord, voice_id)
            if record is None:
                raise KeyError(voice_id)
            return _to_profile(record)

    async def list_voices(self, user_email: str) -> list[VoiceProfile]:
        async with self._session_factory() as session:
            rows = await session.execute(select(VoiceRecord).where(VoiceRecord.user_email == user_email))
            return [_to_profile(row) for row in rows.scalars().all()]

    async def delete_voice(self, voice_id: str) -> None:
        async with self._session_factory() as session:
            record = await session.get(VoiceRecord, voice_id)
            if record:
                await session.delete(record)
                await session.commit()


def _to_profile(record: VoiceRecord) -> VoiceProfile:
    return VoiceProfile(
        id=record.id,
        name=record.name,
        provider=record.provider,
        provider_voice_id=record.provider_voice_id,
        supported_languages=[item for item in record.supported_languages.split(",") if item],
        experimental_languages=[item for item in record.experimental_languages.split(",") if item],
        status=record.status,
        is_cloned=record.is_cloned,
        quality_note=record.quality_note,
    )
