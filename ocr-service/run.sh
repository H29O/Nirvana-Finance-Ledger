#!/usr/bin/env bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

# Use python in venv
if [ ! -f "venv/bin/python" ]; then
    echo "Virtual environment not found at $DIR/venv. Please set up the venv first."
    exit 1
fi

PORT="${OCR_PORT:-5005}"
HOST="${OCR_HOST:-127.0.0.1}"

echo "Starting Nivara PaddleOCR Service on http://${HOST}:${PORT}..."
exec ./venv/bin/python -m uvicorn app.main:app --host "$HOST" --port "$PORT"
