from pydantic import BaseModel


class ExtractResponse(BaseModel):
    # camelCase field names so the JSON matches what the Java side expects
    text: str
    pageCount: int
    characterCount: int