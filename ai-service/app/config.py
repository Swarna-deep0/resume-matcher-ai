import os


def _int_env(name: str, default: int) -> int:
    value = os.environ.get(name)
    if value is None or value.strip() == "":
        return default
    try:
        return int(value)
    except ValueError:
        raise RuntimeError(f"{name} must be an integer")


class Settings:
    def __init__(self) -> None:
        self.api_key = os.environ.get("AI_SERVICE_API_KEY", "")
        if len(self.api_key) < 32:
            raise RuntimeError("AI_SERVICE_API_KEY must be set and at least 32 characters long")
        self.max_pdf_bytes = _int_env("MAX_PDF_BYTES", 5 * 1024 * 1024)
        self.max_pdf_pages = _int_env("MAX_PDF_PAGES", 10)
        self.max_text_chars = _int_env("MAX_TEXT_CHARS", 100_000)


settings = Settings()