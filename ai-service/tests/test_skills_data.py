import json
from pathlib import Path

from app.nlp.text_normalizer import normalize_text

DATA_FILE = Path(__file__).resolve().parent.parent / "app" / "nlp" / "skills_data.json"
ALLOWED_CATEGORIES = {
    "language", "framework", "library", "database", "cloud",
    "devops", "tool", "testing", "concept", "data",
}
ALLOWED_FIELDS = {"name", "category", "aliases", "exact_only"}


def load_skills():
    with DATA_FILE.open(encoding="utf-8") as f:
        return json.load(f)["skills"]


def test_file_has_enough_skills():
    assert len(load_skills()) >= 100


def test_each_entry_is_well_formed():
    for skill in load_skills():
        name = skill.get("name")
        assert isinstance(name, str) and name.strip(), skill
        assert skill.get("category") in ALLOWED_CATEGORIES, name
        assert isinstance(skill.get("aliases"), list), name
        assert all(isinstance(a, str) and a.strip() for a in skill["aliases"]), name
        assert isinstance(skill.get("exact_only", False), bool), name
        assert set(skill) <= ALLOWED_FIELDS, f"{name} has unknown fields"


def test_names_are_unique():
    names = [skill["name"].lower() for skill in load_skills()]
    assert len(names) == len(set(names))


def test_no_alias_is_shared_between_skills():
    owner = {}
    for skill in load_skills():
        for text in [skill["name"]] + skill["aliases"]:
            key = normalize_text(text)
            previous = owner.setdefault(key, skill["name"])
            assert previous == skill["name"], f"'{key}' belongs to both {previous} and {skill['name']}"


def test_no_single_character_aliases():
    for skill in load_skills():
        for text in [skill["name"]] + skill["aliases"]:
            assert len(normalize_text(text)) >= 2, f"{skill['name']}: '{text}' is too short to match safely"