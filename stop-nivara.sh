#!/usr/bin/env bash
set -e

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$PROJECT_DIR"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

STOP_NGINX=false
if [ "$1" = "--all" ] || [ "$1" = "--nginx" ]; then
    STOP_NGINX=true
fi

echo -e "${BOLD}${BLUE}======================================================${NC}"
echo -e "${BOLD}${BLUE}              Stopping Nivara Services                ${NC}"
echo -e "${BOLD}${BLUE}======================================================${NC}\n"

echo -e "${BOLD}Stopping PM2 managed applications (Backend & OCR)...${NC}"
pm2 stop nivara-backend nivara-ocr >/dev/null 2>&1 || pm2 stop "$PROJECT_DIR/ecosystem.config.cjs" >/dev/null 2>&1 || true
pm2 save >/dev/null 2>&1 || true
echo -e "${GREEN}✔ Backend and OCR services stopped.${NC}"

if [ "$STOP_NGINX" = true ]; then
    echo -e "\n${BOLD}Stopping Nginx service...${NC}"
    brew services stop nginx >/dev/null 2>&1 || true
    echo -e "${GREEN}✔ Nginx stopped.${NC}"
else
    echo -e "\n${YELLOW}Note: Nginx and PostgreSQL left running. Use './stop-nivara.sh --all' to also stop Nginx.${NC}"
fi

echo -e "\n${BOLD}Current status:${NC}"
./status-nivara.sh || true
