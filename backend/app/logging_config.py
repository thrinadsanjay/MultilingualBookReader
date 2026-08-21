from __future__ import annotations

import logging
import sys

from app.config import get_settings


def configure_logging() -> None:
    settings = get_settings()
    level = logging.DEBUG if settings.debug else logging.INFO
    logging.basicConfig(
        level=level,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
        stream=sys.stdout,
        force=True,
    )
    # Never attach filters that could print request bodies containing book/voice data.
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
