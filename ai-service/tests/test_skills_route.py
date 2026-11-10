import os

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
HEADERS = {"X-Internal-Api-Key": os.environ["AI_SERVICE_API_KEY"]}


def test_requires_api_key():
    assert client.post("/extract-skills", json={"text": "Java"}).status_code == 401


def test_returns_skills():
    response = client.post("/extract-skills", headers=HEADERS, json={"text": "Java, Spring Boot and MySQL"})
    assert response.status_code == 200
    body = response.json()
    assert body["skillCount"] == 3
    assert {s["name"] for s in body["skills"]} == {"Java", "Spring Boot", "MySQL"}


def test_rejects_empty_text():
    response = client.post("/extract-skills", headers=HEADERS, json={"text": ""})
    assert response.status_code == 400
    assert response.json()["code"] == "INVALID_REQUEST"


def test_rejects_missing_text():
    response = client.post("/extract-skills", headers=HEADERS, json={})
    assert response.status_code == 400