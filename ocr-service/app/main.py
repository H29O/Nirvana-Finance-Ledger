import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.schemas import OcrResponse
from app.ocr_engine import PaddleOcrEngine

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("ocr_service")

# Global engine reference
ocr_engine: PaddleOcrEngine = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global ocr_engine
    logger.info("Initializing PaddleOCR service on startup...")
    try:
        ocr_engine = PaddleOcrEngine()
        logger.info("PaddleOCR engine loaded and ready.")
    except Exception as e:
        logger.error("Failed to initialize PaddleOCR engine on startup: %s", str(e), exc_info=True)
    yield
    logger.info("Shutting down PaddleOCR service...")

app = FastAPI(
    title="Nivara Receipt OCR Service",
    description="Local PaddleOCR microservice for Nivara receipt processing",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health():
    return {
        "status": "UP",
        "service": "Nivara Receipt OCR Service",
        "engine": "PaddleOCR 3.x",
        "engine_ready": ocr_engine is not None
    }

@app.post("/ocr", response_model=OcrResponse)
async def process_ocr(file: UploadFile = File(...)):
    global ocr_engine
    if ocr_engine is None:
        try:
            logger.info("Lazy initializing PaddleOCR engine...")
            ocr_engine = PaddleOcrEngine()
        except Exception as e:
            logger.error("Could not initialize PaddleOCR engine: %s", str(e))
            raise HTTPException(status_code=503, detail=f"PaddleOCR engine unavailable: {str(e)}")

    if not file:
        raise HTTPException(status_code=400, detail="No file uploaded")

    content_type = file.content_type or ""
    if content_type.lower() == "application/pdf":
        return OcrResponse(
            success=False,
            raw_text="",
            error="PDF OCR is currently not supported. Please upload JPEG/PNG."
        )

    try:
        image_bytes = await file.read()
        if not image_bytes:
            raise HTTPException(status_code=400, detail="Uploaded file is empty")

        response = ocr_engine.process_image_bytes(image_bytes)
        return response
    except Exception as e:
        logger.exception("OCR request failed: %s", str(e))
        return OcrResponse(
            success=False,
            raw_text="",
            error=str(e)
        )
