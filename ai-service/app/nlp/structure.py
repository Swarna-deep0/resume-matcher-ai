import re
from dataclasses import dataclass

_EMAIL = re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+")
_PHONE = re.compile(r"(?<!\w)\+?\d[\d\s().-]{7,}\d(?!\w)")
_SKILLS_HEADING = re.compile(
    r"^\s*(?:technical\s+|key\s+|core\s+)?(?:skills|competencies)\b", re.IGNORECASE | re.MULTILINE
)
_EXPERIENCE_HEADING = re.compile(
    r"^\s*(?:(?:work|professional|employment)\s+)?(?:experience|history)\b|^\s*employment\b",
    re.IGNORECASE | re.MULTILINE,
)
_EDUCATION_HEADING = re.compile(
    r"^\s*(?:education|academic|qualifications)\b", re.IGNORECASE | re.MULTILINE
)

MIN_WORDS = 150
MAX_WORDS = 1500


@dataclass(frozen=True)
class StructureCheckResult:
    name: str
    passed: bool
    points: float  # points earned
    max_points: float
    detail: str


def _has_phone(text: str) -> bool:
    # At least 10 digits, so a date range like "2019 - 2023" is not mistaken for a phone number
    return any(sum(ch.isdigit() for ch in m.group(0)) >= 10 for m in _PHONE.finditer(text))


def _check(name: str, passed: bool, max_points: float, detail: str) -> StructureCheckResult:
    return StructureCheckResult(name, passed, max_points if passed else 0.0, max_points, detail)


def check_structure(resume_text: str) -> list[StructureCheckResult]:
    words = len(resume_text.split())
    has_contact = bool(_EMAIL.search(resume_text)) or _has_phone(resume_text)
    return [
        _check("Contact details", has_contact, 3, "Email address or phone number found"
               if has_contact else "No email address or phone number found"),
        _check("Skills section", bool(_SKILLS_HEADING.search(resume_text)), 2,
               "A skills heading was found" if _SKILLS_HEADING.search(resume_text)
               else "No skills heading found"),
        _check("Experience section", bool(_EXPERIENCE_HEADING.search(resume_text)), 2,
               "An experience heading was found" if _EXPERIENCE_HEADING.search(resume_text)
               else "No experience heading found"),
        _check("Education section", bool(_EDUCATION_HEADING.search(resume_text)), 2,
               "An education heading was found" if _EDUCATION_HEADING.search(resume_text)
               else "No education heading found"),
        _check("Reasonable length", MIN_WORDS <= words <= MAX_WORDS, 1,
               f"{words} words (expected {MIN_WORDS} to {MAX_WORDS})"),
    ]