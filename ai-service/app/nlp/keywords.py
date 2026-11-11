from sklearn.feature_extraction.text import ENGLISH_STOP_WORDS, TfidfVectorizer

# Everyday job-posting words that say nothing about the role. Edit this list as needed.
GENERIC_TERMS = frozenset({
    "experience", "experienced", "years", "year", "work", "working", "works", "team", "teams",
    "strong", "ability", "skills", "skill", "knowledge", "required", "requirements", "requirement",
    "preferred", "responsibilities", "responsibility", "role", "candidate", "looking", "including",
    "include", "includes", "etc", "job", "company", "position", "good", "excellent", "must", "able",
    "using", "use", "used", "new", "well", "across", "within", "related", "degree", "bachelor",
    "bachelors", "master", "masters", "qualification", "qualifications", "plus", "nice",
    "opportunity", "join", "seeking", "highly", "proven", "demonstrated", "understanding",
    "familiarity", "proficiency", "environment", "business", "level",
})


def _is_keyword(term: str, excluded: set[str]) -> bool:
    return (
        term.isalpha()
        and len(term) >= 3
        and term not in ENGLISH_STOP_WORDS
        and term not in GENERIC_TERMS
        and term not in excluded
    )


def top_job_keywords(job_norm: str, excluded_tokens: set[str], limit: int = 15) -> list[str]:
    """Most heavily weighted job description words that are not skills, stop words or filler."""
    if not job_norm:
        return []
    # One document, so the weights reflect term frequency; ties are broken alphabetically
    vectorizer = TfidfVectorizer(token_pattern=r"\S+", lowercase=False, sublinear_tf=True)
    try:
        matrix = vectorizer.fit_transform([job_norm])
    except ValueError:
        return []
    terms = vectorizer.get_feature_names_out()
    weights = matrix.toarray()[0]
    candidates = [
        (float(weight), term)
        for term, weight in zip(terms, weights)
        if _is_keyword(term, excluded_tokens)
    ]
    candidates.sort(key=lambda item: (-item[0], item[1]))
    return [term for _, term in candidates[:limit]]