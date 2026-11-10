import os

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_is_public():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}


def test_ping_requires_api_key():
    assert client.get("/internal/ping").status_code == 401


def test_ping_with_wrong_key_is_rejected():
    response = client.get("/internal/ping", headers={"X-Internal-Api-Key": "wrong"})
    assert response.status_code == 401


def test_ping_with_correct_key():
    key = os.environ["AI_SERVICE_API_KEY"]
    response = client.get("/internal/ping", headers={"X-Internal-Api-Key": key})
    assert response.status_code == 200


def test_extract_rejects_non_pdf():
    key = os.environ["AI_SERVICE_API_KEY"]
    response = client.post(
        "/extract-text",
        headers={"X-Internal-Api-Key": key},
        files={"file": ("resume.pdf", b"not a pdf", "application/pdf")},
    )
    assert response.status_code == 415
    assert response.json()["code"] == "NOT_A_PDF"


def test_extract_rejects_empty_file():
    key = os.environ["AI_SERVICE_API_KEY"]
    response = client.post(
        "/extract-text",
        headers={"X-Internal-Api-Key": key},
        files={"file": ("resume.pdf", b"", "application/pdf")},
    )
    assert response.status_code == 400
    assert response.json()["code"] == "EMPTY_FILE"