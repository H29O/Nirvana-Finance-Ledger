# Nivara Receipt OCR Service (PaddleOCR)

Lightweight local microservice that provides structured OCR inference for receipt processing using PaddleOCR 3.x.

## Ports
- **OCR Service**: `http://127.0.0.1:5005`
- **Spring Boot Backend**: `http://127.0.0.1:2906`
- **React Frontend**: `http://localhost:5173`

## Endpoints
- `GET /health`: Health status check
- `POST /ocr`: Multipart receipt image upload (`file`), returns structured JSON containing bounding boxes, confidence, coordinates, ordered lines, and full text.

## Running Locally

### 1. Setup & Run OCR Service
```bash
cd ocr-service
# First time setup (if venv does not exist):
python3.12 -m venv venv
./venv/bin/pip install -r requirements.txt

# Start service (runs on port 5005):
./run.sh
```

### 2. Run Backend (Spring Boot)
```bash
cd backend
sh ./mvnw spring-boot:run
```

### 3. Run Frontend
```bash
cd frontend
npm run dev
```

## Environment Variables
- `OCR_PORT`: Port to listen on (default `5005`)
- `OCR_HOST`: Host interface to bind (default `127.0.0.1`)
- In backend `application.properties`:
  - `app.ocr.paddle.url=http://localhost:5005`
  - `app.ocr.paddle.timeout-ms=30000`
