import io
import logging
import re

from pypdf import PdfReader

from app.config import settings
from app.errors import ApiError

logger = logging.getLogger(__name__)

PDF_SIGNATURE = b"%PDF-"


def _normalize(text: str) -> str:
    text = text.replace("\x00", "")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def extract_text_from_pdf(data: bytes) -> tuple[str, int]:
    """Validate the PDF bytes and return (text, page_count). Raises ApiError on any problem."""
    if len(data) == 0:
        raise ApiError(400, "EMPTY_FILE", "The uploaded file is empty")

    if len(data) > settings.max_pdf_bytes:
        max_mb = round(settings.max_pdf_bytes / (1024 * 1024), 1)
        raise ApiError(413, "FILE_TOO_LARGE", f"The PDF must be at most {max_mb} MB")

    if not data.startswith(PDF_SIGNATURE):
        raise ApiError(415, "NOT_A_PDF", "The uploaded file is not a PDF")

    try:
        reader = PdfReader(io.BytesIO(data), strict=False)

        if reader.is_encrypted:
            raise ApiError(422, "PDF_ENCRYPTED", "Password-protected PDFs are not supported")

        page_count = len(reader.pages)
        if page_count == 0:
            raise ApiError(422, "PDF_UNREADABLE", "The PDF has no pages")
        if page_count > settings.max_pdf_pages:
            raise ApiError(
                422,
                "TOO_MANY_PAGES",
                f"The PDF has {page_count} pages; the maximum is {settings.max_pdf_pages}",
            )

        raw_text = "\n".join((page.extract_text() or "") for page in reader.pages)
    except ApiError:
        raise
    except Exception as exc:
        logger.warning("PDF parsing failed: %s", type(exc).__name__)
        raise ApiError(422, "PDF_UNREADABLE", "The PDF could not be read; it may be corrupt")

    text = _normalize(raw_text)

    if not text:
        raise ApiError(
            422,
            "NO_TEXT_FOUND",
            "No extractable text found. Scanned or image-only PDFs are not supported",
        )
    if len(text) > settings.max_text_chars:
        raise ApiError(422, "TEXT_TOO_LONG", "The PDF contains too much text to analyse")

    return text, page_count