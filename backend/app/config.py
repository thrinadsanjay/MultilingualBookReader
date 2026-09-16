from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    debug: bool = True
    app_name: str = "Multilingual Book Reader API"
    secret_key: str = "change-me-in-production"
    database_url: str = "sqlite+aiosqlite:///./bookreader.db"
    audio_cache_dir: str = "./storage/audio"
    voice_sample_dir: str = "./storage/voice"
    cors_origins: str = "*"

    # Set this to password the server: callers then send X-API-Key instead of signing in.
    api_key: str = ""

    ocr_provider: str = "tesseract"
    tts_provider: str = "espeak"
    voice_provider: str = "mock"

    ocr_api_key: str = ""
    google_application_credentials: str = ""
    tts_api_key: str = ""
    google_tts_api_key: str = ""
    azure_speech_key: str = ""
    azure_speech_region: str = ""
    elevenlabs_api_key: str = ""
    voice_api_key: str = ""

    rate_limit: str = "30/minute"
    access_token_expire_minutes: int = 1440

    @property
    def cors_origin_list(self) -> list[str]:
        if self.cors_origins.strip() == "*":
            return ["*"]
        return [item.strip() for item in self.cors_origins.split(",") if item.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
