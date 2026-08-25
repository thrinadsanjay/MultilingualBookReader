from __future__ import annotations

import hashlib
import hmac
import os
from datetime import datetime, timedelta, timezone

from fastapi import Depends, HTTPException, status
from fastapi.security import APIKeyHeader, HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.db import SessionLocal, User

bearer = HTTPBearer(auto_error=False)
api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)
PBKDF2_ROUNDS = 390_000
API_KEY_CLIENT = "api-key-client"


def hash_password(password: str) -> str:
    salt = os.urandom(16)
    derived = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, PBKDF2_ROUNDS)
    return f"pbkdf2${salt.hex()}${derived.hex()}"


def verify_password(password: str, hashed: str) -> bool:
    try:
        scheme, salt_hex, digest_hex = hashed.split("$", 2)
    except ValueError:
        return False
    if scheme != "pbkdf2":
        return False
    derived = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), bytes.fromhex(salt_hex), PBKDF2_ROUNDS)
    return hmac.compare_digest(derived.hex(), digest_hex)


def create_token(email: str, minutes: int | None = None) -> str:
    settings = get_settings()
    expire = datetime.now(timezone.utc) + timedelta(minutes=minutes or settings.access_token_expire_minutes)
    return jwt.encode({"sub": email, "exp": expire}, settings.secret_key, algorithm="HS256")


async def get_session() -> AsyncSession:
    async with SessionLocal() as session:
        yield session


async def require_client(
    api_key: str | None = Depends(api_key_header),
    creds: HTTPAuthorizationCredentials | None = Depends(bearer),
    session: AsyncSession = Depends(get_session),
) -> str:
    """Identifies the caller by shared API key or by signed-in user.

    A single-user server can set API_KEY and skip accounts entirely, which is what the phone does
    when you give it a server URL and key.
    """
    settings = get_settings()
    if settings.api_key and api_key is not None:
        if hmac.compare_digest(api_key, settings.api_key):
            return API_KEY_CLIENT
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="That API key is not valid.")
    if creds is not None:
        user = await get_current_user(creds, session)
        return user.email
    detail = "Provide the API key in the X-API-Key header." if settings.api_key else "Sign in required."
    raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=detail)


async def get_current_user(
    creds: HTTPAuthorizationCredentials | None = Depends(bearer),
    session: AsyncSession = Depends(get_session),
) -> User:
    if creds is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Sign in required.")
    try:
        payload = jwt.decode(creds.credentials, get_settings().secret_key, algorithms=["HS256"])
        email = payload.get("sub")
    except JWTError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Sign in required.") from exc
    result = await session.execute(select(User).where(User.email == email))
    user = result.scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Sign in required.")
    return user
