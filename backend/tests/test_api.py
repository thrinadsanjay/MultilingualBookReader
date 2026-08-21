from fastapi.testclient import TestClient

from app.main import app


def test_health():
    with TestClient(app) as client:
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "ok"


def test_auth_and_status():
    with TestClient(app) as client:
        register = client.post(
            "/api/v1/auth/register",
            json={"email": "reader-ci@example.com", "password": "password1"},
        )
        assert register.status_code in {200, 409}
        if register.status_code == 200:
            token = register.json()["access_token"]
        else:
            login = client.post(
                "/api/v1/auth/login",
                json={"email": "reader-ci@example.com", "password": "password1"},
            )
            token = login.json()["access_token"]
        public = client.get("/api/v1/status")
        assert public.status_code == 200
        assert "ocr_provider" in public.json()
        tts = client.post(
            "/api/v1/tts",
            headers={"Authorization": f"Bearer {token}"},
            json={"text": "Hello", "language": "en", "speed": 1.0},
        )
        assert tts.status_code in {200, 502}
