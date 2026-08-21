from pathlib import Path

from app.config import get_settings
from app.services.audio_cache import AudioDiskCache, cache_key


def test_cache_key_is_stable():
    a = cache_key("Hello", "en", "v1", 1.0, "espeak")
    b = cache_key("Hello", "en", "v1", 1.0, "espeak")
    assert a == b
    assert len(a) == 64
    assert "Hello" not in a


def test_disk_cache_roundtrip(tmp_path, monkeypatch):
    monkeypatch.setenv("AUDIO_CACHE_DIR", str(tmp_path))
    get_settings.cache_clear()
    cache = AudioDiskCache()
    key = cache_key("ఇది నా పుస్తకం.", "te", "standard", 1.0, "espeak")
    cache.put(key, b"RIFF", "audio/wav")
    found = cache.get(key)
    assert found is not None
    assert found[0] == b"RIFF"
    assert cache.path_for(key).exists()
    get_settings.cache_clear()
