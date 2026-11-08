import logging

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError

from app.errors import ApiError, api_error_handler, validation_error_handler
from app.routes import extract, internal

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)

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


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}