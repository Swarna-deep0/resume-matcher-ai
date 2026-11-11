from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

# Tokens are already separated by single spaces (see text_normalizer). A token needs at least
# 2 characters and one letter, so "c++", "node.js" and ".net" are kept and bare numbers are not.
_TOKEN_PATTERN = r"(?=\S*[^\W\d_])\S{2,}"


def tfidf_cosine(resume_norm: str, job_norm: str) -> float:
    """TF-IDF cosine similarity (0 to 1) between two normalized texts."""
    if not resume_norm or not job_norm:
        return 0.0
    vectorizer = TfidfVectorizer(
        token_pattern=_TOKEN_PATTERN,
        stop_words="english",
        sublinear_tf=True,
        ngram_range=(1, 2),
        lowercase=False,
    )
    try:
        matrix = vectorizer.fit_transform([resume_norm, job_norm])
    except ValueError:
        # Empty vocabulary: the texts contain only stop words or symbols
        return 0.0
    return float(cosine_similarity(matrix[0], matrix[1])[0, 0])