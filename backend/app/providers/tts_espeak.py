from __future__ import annotations

import asyncio
import shutil
import tempfile
from pathlib import Path

from app.providers.base import TTSProvider


class EspeakTTSProvider(TTSProvider):
    """Development TTS. Robotic, but works offline for en/hi/te without API keys."""

    name = "espeak"

    async def synthesize(self, text: str, language: str, voice_id: str | None, speed: float) -> tuple[bytes, str]:
        if shutil.which("espeak-ng") is None:
            raise RuntimeError("espeak-ng is not installed on this server.")
        voice = {
            "en": "en",
            "hi": "hi",
            "te": "te",
        }.get(language.split("-")[0], "en")
        wpm = int(160 * max(0.5, min(speed, 2.0)))
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "out.wav"
            proc = await asyncio.create_subprocess_exec(
                "espeak-ng",
                "-v",
                voice,
                "-s",
                str(wpm),
                "-w",
                str(path),
                text,
                stdout=asyncio.subprocess.DEVNULL,
                stderr=asyncio.subprocess.DEVNULL,
            )
            await proc.communicate()
            if proc.returncode != 0 or not path.exists():
                raise RuntimeError("Development speech engine failed.")
            return path.read_bytes(), "audio/wav"
