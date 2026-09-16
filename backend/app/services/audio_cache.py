from __future__ import annotations

import hashlib
from pathlib import Path

from app.config import get_settings


def cache_key(text: str, language: str, voice_id: str, speed: float, provider: str) -> str:
    material = f"{text.strip()}|{language.lower()}|{voice_id}|{speed:.2f}|{provider}"
    return hashlib.sha256(material.encode("utf-8")).hexdigest()


class AudioDiskCache:
    def __init__(self) -> None:
        self.root = Path(get_settings().audio_cache_dir)
        self.root.mkdir(parents=True, exist_ok=True)

    def get(self, key: str) -> tuple[bytes, str] | None:
        for ext, mime in (("mp3", "audio/mpeg"), ("wav", "audio/wav")):
            path = self.root / f"{key}.{ext}"
            if path.exists():
                return path.read_bytes(), mime
        return None

    def put(self, key: str, data: bytes, mime: str) -> Path:
        ext = "wav" if "wav" in mime else "mp3"
        path = self.root / f"{key}.{ext}"
        path.write_bytes(data)
        return path

    def path_for(self, key: str) -> Path | None:
        for ext in ("mp3", "wav"):
            path = self.root / f"{key}.{ext}"
            if path.exists():
                return path
        return None
