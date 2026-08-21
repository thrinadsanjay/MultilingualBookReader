from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from fastapi.responses import Response
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.db import User
from app.providers.factory import ocr_provider, tts_provider, voice_provider
from app.schemas import (
    AudioGenerateResponse,
    AuthRequest,
    AuthResponse,
    OcrResult,
    ProviderStatus,
    TtsRequest,
    VoiceProfile,
)
from app.security import create_token, get_current_user, get_session, hash_password, verify_password
from app.services.audio_cache import AudioDiskCache, cache_key

router = APIRouter()
audio_cache = AudioDiskCache()


@router.post("/auth/register", response_model=AuthResponse)
async def register(body: AuthRequest, session: AsyncSession = Depends(get_session)) -> AuthResponse:
    existing = await session.execute(select(User).where(User.email == body.email))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=409, detail="An account with this email already exists.")
    user = User(email=body.email, password_hash=hash_password(body.password))
    session.add(user)
    await session.commit()
    token = create_token(user.email)
    return AuthResponse(access_token=token, refresh_token=token, email=user.email)


@router.post("/auth/login", response_model=AuthResponse)
async def login(body: AuthRequest, session: AsyncSession = Depends(get_session)) -> AuthResponse:
    result = await session.execute(select(User).where(User.email == body.email))
    user = result.scalar_one_or_none()
    if user is None or not verify_password(body.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Sign-in failed. Check your email and password.")
    token = create_token(user.email)
    return AuthResponse(access_token=token, refresh_token=token, email=user.email)


@router.post("/ocr", response_model=OcrResult)
async def ocr(
    image: UploadFile = File(...),
    hint_language: str | None = Form(default=None),
    user: User = Depends(get_current_user),
) -> OcrResult:
    del user
    data = await image.read()
    if not data:
        raise HTTPException(status_code=400, detail="The image was empty.")
    try:
        return await ocr_provider().process_image(data, hint_language)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=502, detail="We could not read this page.") from exc


@router.post("/tts")
async def tts(body: TtsRequest, user: User = Depends(get_current_user)) -> Response:
    del user
    provider = tts_provider()
    key = cache_key(body.text, body.language, body.voice_id or "standard", body.speed, provider.name)
    cached = audio_cache.get(key)
    if cached:
        data, mime = cached
        return Response(content=data, media_type=mime, headers={"X-Audio-Cache": "hit"})
    try:
        data, mime = await provider.synthesize(body.text, body.language, body.voice_id, body.speed)
    except RuntimeError as exc:
        if str(exc) == "rate_limited":
            raise HTTPException(status_code=429, detail="Too many requests. Please wait a little and try again.") from exc
        raise HTTPException(status_code=502, detail="Voice playback could not be generated.") from exc
    audio_cache.put(key, data, mime)
    return Response(content=data, media_type=mime, headers={"X-Audio-Cache": "miss"})


@router.post("/audio/generate", response_model=AudioGenerateResponse)
async def generate_audio(body: TtsRequest, user: User = Depends(get_current_user)) -> AudioGenerateResponse:
    response = await tts(body, user)
    provider = tts_provider()
    key = cache_key(body.text, body.language, body.voice_id or "standard", body.speed, provider.name)
    return AudioGenerateResponse(id=key, cached=response.headers.get("X-Audio-Cache") == "hit", mime_type=response.media_type or "audio/wav")


@router.get("/audio/{audio_id}")
async def get_audio(audio_id: str, user: User = Depends(get_current_user)) -> Response:
    del user
    path = audio_cache.path_for(audio_id)
    if path is None:
        raise HTTPException(status_code=404, detail="That audio is not cached.")
    mime = "audio/mpeg" if path.suffix == ".mp3" else "audio/wav"
    return Response(content=path.read_bytes(), media_type=mime)


@router.post("/voice/create", response_model=VoiceProfile)
async def create_voice(
    name: str = Form(...),
    consent: str = Form(...),
    samples: list[UploadFile] = File(...),
    user: User = Depends(get_current_user),
) -> VoiceProfile:
    if consent.lower() not in {"true", "1", "yes"}:
        raise HTTPException(status_code=400, detail="Please confirm you are allowed to use this voice.")
    if len(samples) < 3:
        raise HTTPException(status_code=400, detail="Record at least three samples in a quiet room.")
    blobs = [await item.read() for item in samples]
    try:
        return await voice_provider().create_voice(name, blobs, user.email)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=502, detail="Your voice profile could not be created.") from exc


@router.get("/voices", response_model=list[VoiceProfile])
async def list_voices(user: User = Depends(get_current_user)) -> list[VoiceProfile]:
    return await voice_provider().list_voices(user.email)


@router.get("/voice/{voice_id}", response_model=VoiceProfile)
async def get_voice(voice_id: str, user: User = Depends(get_current_user)) -> VoiceProfile:
    del user
    try:
        return await voice_provider().get_voice(voice_id)
    except KeyError as exc:
        raise HTTPException(status_code=404, detail="Voice not found.") from exc


@router.delete("/voice/{voice_id}")
async def delete_voice(voice_id: str, user: User = Depends(get_current_user)) -> dict[str, str]:
    del user
    await voice_provider().delete_voice(voice_id)
    return {"status": "deleted"}


@router.get("/status", response_model=ProviderStatus)
async def status() -> ProviderStatus:
    settings = get_settings()
    notes = {
        "ocr": "Tesseract supports English, Hindi, and Telugu locally. Google Vision is optional.",
        "tts": "Google Chirp3-HD / Azure Neural are the production-quality standard voices. espeak is development-only.",
        "voice": "Custom cloned Telugu/Hindi is experimental unless the selected provider lists those languages as supported.",
    }
    return ProviderStatus(
        ocr_provider=settings.ocr_provider,
        tts_provider=settings.tts_provider,
        voice_provider=settings.voice_provider,
        notes=notes,
    )
