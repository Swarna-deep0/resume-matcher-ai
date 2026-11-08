import secrets

from fastapi import Header

from app.config import settings
from app.errors import ApiError


def require_api_key(x_internal_api_key: str | None = Header(default=None)) -> None:
    """Reject any request that does not carry the shared internal key."""
    supplied = (x_internal_api_key or "").encode("utf-8")
    expected = settings.api_key.encode("utf-8")
    if not secrets.compare_digest(supplied, expected):
        raise ApiError(401, "UNAUTHORIZED", "Missing or invalid API key")