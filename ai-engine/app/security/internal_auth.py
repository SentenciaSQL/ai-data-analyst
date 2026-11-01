import hmac

from fastapi import Header, HTTPException

from app.configuration.settings import get_settings


async def require_internal_key(x_internal_api_key: str = Header(default="")) -> None:
    expected = get_settings().internal_api_key
    provided = x_internal_api_key.encode()
    if not expected or not hmac.compare_digest(provided, expected.encode()):
        raise HTTPException(status_code=401, detail="Invalid internal API key.")
