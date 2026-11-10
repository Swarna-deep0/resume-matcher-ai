import logging
import time

from fastapi import APIRouter, Depends, Header

from app.logging_utils import safe_id
from app.models.skills import ExtractSkillsRequest, ExtractSkillsResponse, SkillItem
from app.nlp.skill_extractor import extract_skills
from app.security import require_api_key

logger = logging.getLogger(__name__)

router = APIRouter(dependencies=[Depends(require_api_key)])


@router.post("/extract-skills", response_model=ExtractSkillsResponse)
def extract_skills_endpoint(
    payload: ExtractSkillsRequest,
    x_request_id: str | None = Header(default=None),
) -> ExtractSkillsResponse:
    started = time.perf_counter()
    matches = extract_skills(payload.text)
    millis = (time.perf_counter() - started) * 1000

    logger.info(
        "request_id=%s skills=%d chars=%d took=%.1f ms",
        safe_id(x_request_id),
        len(matches),
        len(payload.text),
        millis,
    )
    items = [
        SkillItem(name=m.name, category=m.category, count=m.count, matchedAs=list(m.matched_as))
        for m in matches
    ]
    return ExtractSkillsResponse(skills=items, skillCount=len(items))