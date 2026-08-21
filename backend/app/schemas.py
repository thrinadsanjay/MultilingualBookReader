from __future__ import annotations

from pydantic import BaseModel, EmailStr, Field


class AuthRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class AuthResponse(BaseModel):
    access_token: str
    refresh_token: str
    email: str


class BoundingBox(BaseModel):
    left: float
    top: float
    right: float
    bottom: float


class OcrWord(BaseModel):
    text: str
    confidence: float = 0
    bounding_box: BoundingBox | None = None


class OcrLine(BaseModel):
    text: str
    confidence: float = 0
    bounding_box: BoundingBox | None = None
    words: list[OcrWord] = Field(default_factory=list)


class OcrBlock(BaseModel):
    text: str
    confidence: float = 0
    language: str | None = None
    bounding_box: BoundingBox | None = None
    lines: list[OcrLine] = Field(default_factory=list)


class OcrResult(BaseModel):
    text: str
    language: str
    confidence: float
    blocks: list[OcrBlock] = Field(default_factory=list)
    engine: str


class TtsRequest(BaseModel):
    text: str = Field(min_length=1, max_length=5000)
    language: str
    voice_id: str | None = None
    speed: float = 1.0


class AudioGenerateResponse(BaseModel):
    id: str
    cached: bool
    duration_ms: int = 0
    mime_type: str = "audio/wav"


class VoiceProfile(BaseModel):
    id: str
    name: str
    provider: str
    provider_voice_id: str | None = None
    supported_languages: list[str] = Field(default_factory=list)
    experimental_languages: list[str] = Field(default_factory=list)
    status: str
    is_cloned: bool = True
    quality_note: str | None = None


class ProviderStatus(BaseModel):
    online: bool = True
    ocr_provider: str
    tts_provider: str
    voice_provider: str
    notes: dict[str, str] = Field(default_factory=dict)
