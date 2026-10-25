#!/usr/bin/env bash
set -e

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$PROJECT_DIR"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}${BLUE}======================================================${NC}"
echo -e "${BOLD}${BLUE}             Restarting Nivara Application            ${NC}"
echo -e "${BOLD}${BLUE}======================================================${NC}\n"

echo -e "${BOLD}Restarting PM2 services (Backend & OCR)...${NC}"
pm2 restart "$PROJECT_DIR/ecosystem.config.cjs" >/dev/null 2>&1 || pm2 start "$PROJECT_DIR/ecosystem.config.cjs" >/dev/null

echo -e "${BOLD}Reloading Nginx configuration...${NC}"
nginx -s reload >/dev/null 2>&1 || brew services restart nginx >/dev/null 2>&1 || true

echo -e "\n${BOLD}Awaiting readiness...${NC}"
for i in {1..20}; do
    if curl -s --connect-timeout 1 "http://127.0.0.1:2906/api/health" | grep -q "Backend is running!" && \
       curl -s --connect-timeout 1 "http://127.0.0.1:5005/health" | grep -q '"status":"UP"'; then
        break
    fi
    sleep 1
done

pm2 save >/dev/null 2>&1 || true

echo -e ""
./status-nivara.sh
