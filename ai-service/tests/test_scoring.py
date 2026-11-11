from app.services.scoring_service import analyze_resume

JOB = """Backend Developer
We need a developer with Java, Spring Boot, MySQL, Docker and REST APIs.
You will build secure services, write tests and work with CI/CD pipelines.
"""

STRONG_RESUME = """Jane Sample
jane.sample@example.com | +91 98765 43210

Skills
Java, Spring Boot, Spring Security, JWT, MySQL, REST APIs, Docker, CI/CD

Experience
Backend Developer, Example Corp (2022 - 2025)
Built REST APIs with Spring Boot and MySQL. Secured services with JWT and Spring Security.
Containerised services with Docker and automated builds with CI/CD pipelines.

Education
B.Tech in Computer Science
"""

WEAK_RESUME = """John Sample
john@example.com

Experience
Sales Associate handling customers and inventory.

Education
High school diploma
"""


def test_strong_resume_scores_higher_than_weak():
    strong = analyze_resume(STRONG_RESUME, JOB)
    weak = analyze_resume(WEAK_RESUME, JOB)
    assert strong.score > weak.score
    assert {s.name for s in strong.matchedSkills} == {
        "CI/CD", "Docker", "Java", "MySQL", "REST API", "Spring Boot",
    }
    assert strong.missingSkills == []


def test_missing_skills_are_reported():
    result = analyze_resume("Skills: Java, MySQL", JOB)
    assert {s.name for s in result.matchedSkills} == {"Java", "MySQL"}
    assert {s.name for s in result.missingSkills} == {"CI/CD", "Docker", "REST API", "Spring Boot"}


def test_adding_a_missing_skill_raises_the_score():
    before = analyze_resume("Skills: Java, MySQL", JOB)
    after = analyze_resume("Skills: Java, MySQL, Docker", JOB)
    assert after.score > before.score


def test_score_is_deterministic():
    first = analyze_resume(STRONG_RESUME, JOB)
    second = analyze_resume(STRONG_RESUME, JOB)
    assert first.model_dump() == second.model_dump()


def test_score_is_within_bounds():
    for resume in (STRONG_RESUME, WEAK_RESUME, JOB):
        assert 0 <= analyze_resume(resume, JOB).score <= 100


def test_breakdown_adds_up_to_total():
    result = analyze_resume(STRONG_RESUME, JOB)
    assert round(sum(c.points for c in result.components if c.available), 1) == result.score


def test_job_without_known_skills_redistributes_weight():
    job = "We need a motivated person who enjoys teamwork and learning new things."
    result = analyze_resume(STRONG_RESUME, job)
    by_key = {c.key: c for c in result.components}
    assert by_key["skillCoverage"].available is False
    assert result.warnings
    assert abs(sum(c.weight for c in result.components if c.available) - 100) < 0.2


def test_structure_checks():
    strong = {c.name: c.passed for c in analyze_resume(STRONG_RESUME, JOB).structureChecks}
    assert strong["Contact details"] and strong["Skills section"]
    assert strong["Experience section"] and strong["Education section"]

    weak = {c.name: c.passed for c in analyze_resume(WEAK_RESUME, JOB).structureChecks}
    assert weak["Contact details"] is True
    assert weak["Skills section"] is False

    plain = {c.name: c.passed for c in analyze_resume("Skills: Java, MySQL", JOB).structureChecks}
    assert plain["Contact details"] is False
    assert plain["Skills section"] is True