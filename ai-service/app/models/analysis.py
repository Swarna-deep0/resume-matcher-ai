from pydantic import BaseModel, Field

from app.config import settings


class AnalyzeRequest(BaseModel):
    resumeText: str = Field(min_length=1, max_length=settings.max_text_chars)
    jobDescriptionText: str = Field(min_length=1, max_length=settings.max_text_chars)


class ScoreComponent(BaseModel):
    key: str
    label: str
    weight: float
    ratio: float
    points: float
    available: bool
    detail: str


class MatchedSkill(BaseModel):
    name: str
    category: str
    resumeCount: int
    jobCount: int


class MissingSkill(BaseModel):
    name: str
    category: str
    jobCount: int


class KeywordItem(BaseModel):
    term: str
    presentInResume: bool


class StructureCheckItem(BaseModel):
    name: str
    passed: bool
    points: float
    maxPoints: float
    detail: str


class AnalyzeResponse(BaseModel):
    score: float
    components: list[ScoreComponent]
    matchedSkills: list[MatchedSkill]
    missingSkills: list[MissingSkill]
    additionalSkills: list[str]
    keywords: list[KeywordItem]
    structureChecks: list[StructureCheckItem]
    warnings: list[str]
    disclaimer: str