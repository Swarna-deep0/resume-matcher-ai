import re
import unicodedata

# Anything except letters, digits, "+", "#" and "." becomes a space; underscores too
_NON_TOKEN = re.compile(r"[^\w+#.]+|_+")
_DOT_RUNS = re.compile(r"\.{2,}")
_TRAILING_DOT = re.compile(r"\.(?=\s|$)")
_WHITESPACE = re.compile(r"\s+")


def normalize_text(text: str) -> str:
    """Return lowercase text with single spaces, keeping characters used in skill names."""
    text = unicodedata.normalize("NFKC", text).lower()
    text = _NON_TOKEN.sub(" ", text)
    text = _DOT_RUNS.sub(" ", text)
    text = _TRAILING_DOT.sub("", text)
    return _WHITESPACE.sub(" ", text).strip()