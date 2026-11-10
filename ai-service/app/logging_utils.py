import re
def safe_id(value: str | None) -> str:
    """Keep only safe characters so a request ID cannot inject fake log lines."""
    return re.sub(r"[^A-Za-z0-9\-]", "", value or "")[:64] or "-"