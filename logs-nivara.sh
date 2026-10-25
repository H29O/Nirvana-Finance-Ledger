#!/usr/bin/env bash

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$PROJECT_DIR"

SERVICE="${1:-all}"
FOLLOW="${2:-}"

# Mask sensitive patterns (passwords, jwt secret values)
mask_secrets() {
    sed -E \
        -e 's/(password[[:space:]]*[:=][[:space:]]*)[^[:space:]]+/\1*******/Ig' \
        -e 's/(PGPASSWORD[[:space:]]*=[[:space:]]*)[^[:space:]]+/\1*******/Ig' \
        -e 's/(secret[[:space:]]*[:=][[:space:]]*)[^[:space:]]+/\1*******/Ig'
}

case "$SERVICE" in
    backend)
        echo "=== Nivara Spring Boot Backend Logs ==="
        if [ "$FOLLOW" = "-f" ] || [ "$FOLLOW" = "--follow" ]; then
            tail -n 50 -f "$PROJECT_DIR/logs/backend-out.log" "$PROJECT_DIR/logs/backend-error.log" 2>/dev/null | mask_secrets
        else
            tail -n 100 "$PROJECT_DIR/logs/backend-out.log" "$PROJECT_DIR/logs/backend-error.log" 2>/dev/null | mask_secrets
        fi
        ;;
    ocr)
        echo "=== Nivara PaddleOCR Microservice Logs ==="
        if [ "$FOLLOW" = "-f" ] || [ "$FOLLOW" = "--follow" ]; then
            tail -n 50 -f "$PROJECT_DIR/logs/ocr-out.log" "$PROJECT_DIR/logs/ocr-error.log" 2>/dev/null | mask_secrets
        else
            tail -n 100 "$PROJECT_DIR/logs/ocr-out.log" "$PROJECT_DIR/logs/ocr-error.log" 2>/dev/null | mask_secrets
        fi
        ;;
    nginx)
        echo "=== Nginx Production Logs ==="
        if [ "$FOLLOW" = "-f" ] || [ "$FOLLOW" = "--follow" ]; then
            tail -n 50 -f "$PROJECT_DIR/logs/nginx-access.log" "$PROJECT_DIR/logs/nginx-error.log" 2>/dev/null
        else
            tail -n 100 "$PROJECT_DIR/logs/nginx-access.log" "$PROJECT_DIR/logs/nginx-error.log" 2>/dev/null
        fi
        ;;
    all|*)
        echo "=== All Nivara Logs (PM2 & Nginx) ==="
        pm2 logs --lines 25 --nostream 2>/dev/null | mask_secrets || true
        echo ""
        echo "=== Nginx Recent Access & Error Logs ==="
        tail -n 15 "$PROJECT_DIR/logs/nginx-access.log" "$PROJECT_DIR/logs/nginx-error.log" 2>/dev/null || true
        echo ""
        echo "Usage: ./logs-nivara.sh [backend|ocr|nginx|all] [-f]"
        ;;
esac
