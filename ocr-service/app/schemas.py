from pydantic import BaseModel, Field
from typing import List, Optional

class BoundingBox(BaseModel):
    points: List[List[float]] = Field(default_factory=list, description="4 polygon points [[x1, y1], [x2, y2], [x3, y3], [x4, y4]]")
    top_left: List[float] = Field(default_factory=list, description="[min_x, min_y]")
    bottom_right: List[float] = Field(default_factory=list, description="[max_x, max_y]")
    center_x: float = 0.0
    center_y: float = 0.0
    width: float = 0.0
    height: float = 0.0

class OCRTextBlock(BaseModel):
    text: str
    confidence: float
    box: BoundingBox
    center_x: float = 0.0
    center_y: float = 0.0
    width: float = 0.0
    height: float = 0.0
    line_index: int = 0

class OcrResponse(BaseModel):
    success: bool
    raw_text: str = ""
    blocks: List[OCRTextBlock] = Field(default_factory=list)
    lines: List[str] = Field(default_factory=list)
    page_width: Optional[int] = None
    page_height: Optional[int] = None
    processing_time_ms: Optional[float] = None
    error: Optional[str] = None
