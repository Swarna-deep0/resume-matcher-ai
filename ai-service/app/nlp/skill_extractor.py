import json
import re
import unicodedata
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from app.nlp.text_normalizer import normalize_text

DATA_FILE = Path(__file__).with_name("skills_data.json")

# A skill must not touch other letters, "+", "#" or a ".word" suffix. This keeps "java"
# out of "javascript" and ".net" out of "asp.net". Digits after a name are allowed,
# so "python3" and "java 17" still match.
_LEFT = r"(?<![\w+#.])"
_RIGHT = r"(?![^\W\d]|_|\+|#|\.\w)"


@dataclass(frozen=True)
class SkillMatch:
    name: str
    category: str
    count: int
    matched_as: tuple[str, ...]


@dataclass(frozen=True)
class _Skill:
    name: str
    category: str
    pattern: re.Pattern[str] | None  # aliases, run on normalized (lowercase) text
    exact_pattern: re.Pattern[str] | None  # exact capitalisation, run on the original text


def _build_alias_pattern(aliases: list[str]) -> re.Pattern[str] | None:
    unique = sorted({a for a in aliases if a}, key=len, reverse=True)
    if not unique:
        return None
    body = "|".join(re.escape(a) for a in unique)
    return re.compile(_LEFT + "(?:" + body + ")" + _RIGHT)


@lru_cache(maxsize=1)
def _load_skills() -> tuple[_Skill, ...]:
    with DATA_FILE.open(encoding="utf-8") as f:
        entries = json.load(f)["skills"]

    skills = []
    for entry in entries:
        name = entry["name"]
        aliases = [normalize_text(a) for a in entry["aliases"]]
        exact = None
        if entry.get("exact_only", False):
            exact = re.compile(_LEFT + re.escape(unicodedata.normalize("NFKC", name)) + _RIGHT)
        else:
            aliases.append(normalize_text(name))
        skills.append(_Skill(name, entry["category"], _build_alias_pattern(aliases), exact))
    return tuple(skills)


def warm_up() -> int:
    """Load the dictionary and compile the patterns once; returns the number of skills."""
    return len(_load_skills())


def extract_skills(text: str) -> list[SkillMatch]:
    """Return every dictionary skill that the text literally mentions, sorted by name."""
    normalized = normalize_text(text)
    original = unicodedata.normalize("NFKC", text)

    found = []
    for skill in _load_skills():
        phrases = []
        if skill.pattern:
            phrases.extend(m.group(0) for m in skill.pattern.finditer(normalized))
        if skill.exact_pattern:
            phrases.extend(m.group(0) for m in skill.exact_pattern.finditer(original))
        if phrases:
            found.append(SkillMatch(skill.name, skill.category, len(phrases), tuple(sorted(set(phrases)))))
    return sorted(found, key=lambda m: m.name.lower())