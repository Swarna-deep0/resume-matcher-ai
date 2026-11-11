from dataclasses import dataclass

from app.models.analysis import (
    AnalyzeResponse,
    KeywordItem,
    MatchedSkill,
    MissingSkill,
    ScoreComponent,
    StructureCheckItem,
)
from app.nlp.keywords import top_job_keywords
from app.nlp.similarity import tfidf_cosine
from app.nlp.skill_extractor import extract_skills
from app.nlp.structure import check_structure
from app.nlp.text_normalizer import normalize_text

# Initial design values chosen by this project. They are NOT established ATS weights;
# they are meant to be validated and only changed with a documented reason.
WEIGHTS = {
    "skillCoverage": 50.0,
    "textSimilarity": 25.0,
    "keywordCoverage": 15.0,
    "structure": 10.0,
}
LABELS = {
    "skillCoverage": "Skill coverage",
    "textSimilarity": "Text similarity",
    "keywordCoverage": "Keyword coverage",
    "structure": "Resume structure",
}
# A resume/job pair with this cosine similarity or higher earns full similarity marks
SIMILARITY_FULL_SCORE = 0.40
KEYWORD_LIMIT = 15
DISCLAIMER = (
    "This score is an estimate from this project's own transparent formula. "
    "It does not guarantee any result on commercial ATS platforms."
)


@dataclass(frozen=True)
class _Part:
    key: str
    ratio: float | None  # None means "no data", so the part is left out of the score
    detail: str


def analyze_resume(resume_text: str, job_text: str) -> AnalyzeResponse:
    resume_norm = normalize_text(resume_text)
    job_norm = normalize_text(job_text)
    warnings: list[str] = []

    # --- Skills ---
    resume_skills = {m.name: m for m in extract_skills(resume_text)}
    job_skills = {m.name: m for m in extract_skills(job_text)}
    matched = sorted(set(job_skills) & set(resume_skills), key=str.lower)
    missing = sorted(set(job_skills) - set(resume_skills), key=str.lower)
    additional = sorted(set(resume_skills) - set(job_skills), key=str.lower)

    if job_skills:
        skill_ratio = len(matched) / len(job_skills)
        skill_detail = (
            f"{len(matched)} of {len(job_skills)} skills from the job description "
            f"were found in the resume"
        )
    else:
        skill_ratio = None
        skill_detail = "No known skills were found in the job description"
        warnings.append(
            "No known skills were found in the job description, "
            "so skill coverage was left out of the score."
        )
    if not resume_skills:
        warnings.append(
            "No known skills were found in the resume. Check that the text was extracted correctly."
        )

    # --- Text similarity ---
    cosine = tfidf_cosine(resume_norm, job_norm)
    similarity_ratio = min(cosine / SIMILARITY_FULL_SCORE, 1.0)
    similarity_detail = (
        f"TF-IDF cosine similarity {cosine:.2f} (full marks at {SIMILARITY_FULL_SCORE:.2f})"
    )

    # --- Keywords (job description words that are not skills) ---
    skill_tokens = {
        token
        for match in job_skills.values()
        for phrase in (match.name, *match.matched_as)
        for token in normalize_text(phrase).split()
    }
    keywords = top_job_keywords(job_norm, skill_tokens, KEYWORD_LIMIT)
    resume_tokens = set(resume_norm.split())
    keyword_items = [KeywordItem(term=k, presentInResume=k in resume_tokens) for k in keywords]
    if keyword_items:
        found = sum(1 for item in keyword_items if item.presentInResume)
        keyword_ratio = found / len(keyword_items)
        keyword_detail = (
            f"{found} of {len(keyword_items)} important job description keywords "
            f"were found in the resume"
        )
    else:
        keyword_ratio = None
        keyword_detail = "No distinctive keywords were found in the job description"
        warnings.append(
            "No distinctive keywords were found in the job description, "
            "so keyword coverage was left out of the score."
        )

    # --- Structure ---
    checks = check_structure(resume_text)
    earned = sum(c.points for c in checks)
    possible = sum(c.max_points for c in checks)
    structure_ratio = earned / possible
    structure_detail = f"{earned:g} of {possible:g} structure points"

    # --- Combine: parts without data are left out and the remaining weights are scaled to 100 ---
    parts = [
        _Part("skillCoverage", skill_ratio, skill_detail),
        _Part("textSimilarity", similarity_ratio, similarity_detail),
        _Part("keywordCoverage", keyword_ratio, keyword_detail),
        _Part("structure", structure_ratio, structure_detail),
    ]
    available_weight = sum(WEIGHTS[p.key] for p in parts if p.ratio is not None)

    components: list[ScoreComponent] = []
    for part in parts:
        if part.ratio is None:
            components.append(ScoreComponent(
                key=part.key, label=LABELS[part.key], weight=0.0, ratio=0.0,
                points=0.0, available=False, detail=part.detail,
            ))
            continue
        weight = WEIGHTS[part.key] * 100 / available_weight
        components.append(ScoreComponent(
            key=part.key, label=LABELS[part.key], weight=round(weight, 1),
            ratio=round(part.ratio, 3), points=round(part.ratio * weight, 1),
            available=True, detail=part.detail,
        ))

    score = min(max(round(sum(c.points for c in components), 1), 0.0), 100.0)

    return AnalyzeResponse(
        score=score,
        components=components,
        matchedSkills=[
            MatchedSkill(
                name=n, category=resume_skills[n].category,
                resumeCount=resume_skills[n].count, jobCount=job_skills[n].count,
            )
            for n in matched
        ],
        missingSkills=[
            MissingSkill(name=n, category=job_skills[n].category, jobCount=job_skills[n].count)
            for n in missing
        ],
        additionalSkills=additional,
        keywords=keyword_items,
        structureChecks=[
            StructureCheckItem(
                name=c.name, passed=c.passed, points=c.points,
                maxPoints=c.max_points, detail=c.detail,
            )
            for c in checks
        ],
        warnings=warnings,
        disclaimer=DISCLAIMER,
    )