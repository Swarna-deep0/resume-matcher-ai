import logging

from fastapi import APIRouter, Depends, File, Header, UploadFile

from app.config import settings
from app.logging_utils import safe_id
from app.models.extraction import ExtractResponse
from app.security import require_api_key
from app.services.pdf_service import extract_text_from_pdf

logger = logging.getLogger(__name__)

router = APIRouter(dependencies=[Depends(require_api_key)])


@router.post("/extract-text", response_model=ExtractResponse)
def extract_text(
    file: UploadFile = File(...),
    x_request_id: str | None = Header(default=None),
) -> ExtractResponse:
    # Read at most one byte over the limit, so oversized files are rejected without full processing
    data = file.file.read(settings.max_pdf_bytes + 1)

    text, page_count = extract_text_from_pdf(data)

    logger.info(
        "request_id=%s extracted pages=%d chars=%d bytes=%d",
        safe_id(x_request_id),
        page_count,
        len(text),
        len(data),
    )
    return ExtractResponse(text=text, pageCount=page_count, characterCount=len(text))