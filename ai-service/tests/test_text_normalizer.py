from app.nlp.text_normalizer import normalize_text


def test_lowercases_and_collapses_whitespace():
    assert normalize_text("Python,  Java\n\nSQL") == "python java sql"


def test_keeps_plus_and_hash_in_skill_names():
    assert normalize_text("C++ and C#") == "c++ and c#"


def test_keeps_dotted_names():
    assert normalize_text("Node.js, .NET Core") == "node.js .net core"


def test_slash_and_hyphen_become_spaces():
    assert normalize_text("CI/CD and scikit-learn") == "ci cd and scikit learn"


def test_strips_trailing_periods():
    assert normalize_text("Built APIs in Python.") == "built apis in python"
    assert normalize_text("Used Node.js.") == "used node.js"


def test_removes_bullets_and_symbols():
    assert normalize_text("• Java\n• Python") == "java python"


def test_keeps_accented_letters():
    assert normalize_text("Résumé") == "résumé"


def test_ellipsis_and_underscores():
    assert normalize_text("Java... Python") == "java python"
    assert normalize_text("snake_case_name") == "snake case name"


def test_empty_and_blank():
    assert normalize_text("") == ""
    assert normalize_text("   \n ") == ""


def test_keeps_version_numbers():
    assert normalize_text("Spring Boot 3.2.") == "spring boot 3.2"