from fastapi import APIRouter, Depends

from app.security import require_api_key

router = APIRouter(prefix="/internal", dependencies=[Depends(require_api_key)])


@router.get("/ping")
def ping() -> dict[str, str]:
    return {"status": "ok"}