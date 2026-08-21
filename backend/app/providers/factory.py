from __future__ import annotations

from app.config import get_settings
from app.db import SessionLocal
from app.providers.base import OCRProvider, TTSProvider, VoiceProvider
from app.providers.elevenlabs import ElevenLabsTTSProvider, ElevenLabsVoiceProvider
from app.providers.ocr_google import GoogleVisionOCRProvider
from app.providers.ocr_tesseract import TesseractOCRProvider
from app.providers.tts_azure import AzureTTSProvider
from app.providers.tts_espeak import EspeakTTSProvider
from app.providers.tts_google import GoogleTTSProvider
from app.providers.voice_mock import MockVoiceProvider


def ocr_provider() -> OCRProvider:
    name = get_settings().ocr_provider.lower()
    if name == "google_vision":
        return GoogleVisionOCRProvider()
    return TesseractOCRProvider()


def tts_provider() -> TTSProvider:
    name = get_settings().tts_provider.lower()
    if name == "google":
        return GoogleTTSProvider()
    if name == "azure":
        return AzureTTSProvider()
    if name == "elevenlabs":
        return ElevenLabsTTSProvider()
    return EspeakTTSProvider()


def voice_provider() -> VoiceProvider:
    name = get_settings().voice_provider.lower()
    if name == "elevenlabs":
        return ElevenLabsVoiceProvider()
    return MockVoiceProvider(SessionLocal)
