import os

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
HEADERS = {"X-Internal-Api-Key": os.environ["AI_SERVICE_API_KEY"]}
PAYLOAD = {
    "resumeText": "Skills: Java, MySQL",
    "jobDescriptionText": "We need Java, MySQL and Docker.",
}


def test_requires_api_key():
    assert client.post("/analyze", json=PAYLOAD).status_code == 401


def test_returns_analysis():
    response = client.post("/analyze", headers=HEADERS, json=PAYLOAD)
    assert response.status_code == 200
    body = response.json()
    assert 0 <= body["score"] <= 100
    assert {s["name"] for s in body["matchedSkills"]} == {"Java", "MySQL"}
    assert [s["name"] for s in body["missingSkills"]] == ["Docker"]
    assert len(body["components"]) == 4
    assert "guarantee" in body["disclaimer"]


def test_rejects_missing_field():
    response = client.post("/analyze", headers=HEADERS, json={"resumeText": "Java"})
    assert response.status_code == 400