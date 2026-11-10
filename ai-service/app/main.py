import logging

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError

from app.errors import ApiError, api_error_handler, validation_error_handler
from app.nlp import skill_extractor
from app.routes import extract, internal, skills

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger = logging.getLogger(__name__)

# Load the skills dictionary once at startup, so the first request is not slower
logger.info("Loaded %d skills", skill_extractor.warm_up())

app = FastAPI(
    title="Resume Matcher AI Service",
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
)

app.add_exception_handler(ApiError, api_error_handler)
app.add_exception_handler(RequestValidationError, validation_error_handler)
app.include_router(internal.router)
app.include_router(extract.router)
app.include_router(skills.router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}