from pydantic import BaseModel, Field

from app.config import settings


class ExtractSkillsRequest(BaseModel):
    text: str = Field(min_length=1, max_length=settings.max_text_chars)


class SkillItem(BaseModel):
    # camelCase field names so the JSON matches what the Java side expects
    name: str
    category: str
    count: int
    matchedAs: list[str]


class ExtractSkillsResponse(BaseModel):
    skills: list[SkillItem]
    skillCount: int