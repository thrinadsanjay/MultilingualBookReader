"""The server can be run with a shared API key instead of user accounts.

That is the mode the phone uses: paste a URL and a key, no sign-up.
"""

from __future__ import annotations

from io import BytesIO

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.config import get_settings
from app.main import app

API_KEY = "test-key-please-change"


@pytest.fixture
def keyed_server(monkeypatch):
    get_settings.cache_clear()
    monkeypatch.setenv("API_KEY", API_KEY)
    get_settings.cache_clear()
    with TestClient(app) as client:
        yield client
    get_settings.cache_clear()


def _page() -> bytes:
    buffer = BytesIO()
    Image.new("RGB", (900, 300), "white").save(buffer, format="PNG")
    return buffer.getvalue()


def _post_ocr(client: TestClient, headers: dict[str, str]) -> object:
    return client.post(
        "/api/v1/ocr",
        headers=headers,
        files={"image": ("page.png", _page(), "image/png")},
        data={"hint_language": "te"},
    )


def test_the_right_key_is_accepted(keyed_server):
    response = _post_ocr(keyed_server, {"X-API-Key": API_KEY})
    assert response.status_code in {200, 503}, response.text


def test_a_wrong_key_is_rejected(keyed_server):
    response = _post_ocr(keyed_server, {"X-API-Key": "not-the-key"})
    assert response.status_code == 401
    assert "API key" in response.json()["detail"]


def test_no_credentials_are_rejected_and_the_message_says_what_to_send(keyed_server):
    response = _post_ocr(keyed_server, {})
    assert response.status_code == 401
    assert "X-API-Key" in response.json()["detail"]


def test_health_reports_whether_telugu_is_installed(keyed_server):
    response = keyed_server.get("/api/v1/ocr/health")
    assert response.status_code == 200
    body = response.json()
    assert set(body) >= {"provider", "languages", "telugu_ready", "requires_api_key"}
    assert body["requires_api_key"] is True
    assert isinstance(body["languages"], list)


def test_health_needs_no_credentials_so_a_deployment_can_be_checked():
    get_settings.cache_clear()
    with TestClient(app) as client:
        assert client.get("/api/v1/ocr/health").status_code == 200
