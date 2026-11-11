import pytest

from app.nlp.keywords import top_job_keywords
from app.nlp.similarity import tfidf_cosine


def test_identical_texts_have_cosine_one():
    assert tfidf_cosine("java spring boot mysql", "java spring boot mysql") == pytest.approx(1.0)


def test_unrelated_texts_have_low_cosine():
    assert tfidf_cosine("java spring boot mysql", "cooking recipes pasta tomato") < 0.1


def test_empty_text_has_zero_cosine():
    assert tfidf_cosine("", "java") == 0.0
    assert tfidf_cosine("java", "") == 0.0


def test_top_keywords_rank_by_frequency():
    text = "build secure scalable services secure services"
    assert top_job_keywords(text, set(), 3) == ["secure", "services", "build"]


def test_excluded_and_generic_terms_are_skipped():
    text = "secure services experience experience"
    assert top_job_keywords(text, {"secure"}, 5) == ["services"]