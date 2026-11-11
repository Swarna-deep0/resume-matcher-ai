import logging
import time

from fastapi import APIRouter, Depends, Header

from app.logging_utils import safe_id
from app.models.analysis import AnalyzeRequest, AnalyzeResponse
from app.security import require_api_key
from app.services.scoring_service import analyze_resume

logger = logging.getLogger(__name__)

router = APIRouter(dependencies=[Depends(require_api_key)])


@router.post("/analyze", response_model=AnalyzeResponse)
def analyze_endpoint(
    payload: AnalyzeRequest,
    x_request_id: str | None = Header(default=None),
) -> AnalyzeResponse:
    started = time.perf_counter()
    result = analyze_resume(payload.resumeText, payload.jobDescriptionText)
    millis = (time.perf_counter() - started) * 1000

    # Only numbers are logged, never the resume or job description text
    logger.info(
        "request_id=%s analysed score=%.1f matched=%d missing=%d took=%.1f ms",
        safe_id(x_request_id),
        result.score,
        len(result.matchedSkills),
        len(result.missingSkills),
        millis,
    )
    return result