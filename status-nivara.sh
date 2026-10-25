#!/usr/bin/env bash
set -e

# Resolve repository directory
PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$PROJECT_DIR"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

echo -e "${BOLD}${BLUE}======================================================${NC}"
echo -e "${BOLD}${BLUE}          Nivara Local Service Status Check          ${NC}"
echo -e "${BOLD}${BLUE}======================================================${NC}\n"

ALL_HEALTHY=true

# 1. PostgreSQL Check
echo -e "${BOLD}[1/4] PostgreSQL Database (Port 5432)${NC}"
PG_READY=false
if [ -x "/Library/PostgreSQL/18/bin/pg_isready" ]; then
    if /Library/PostgreSQL/18/bin/pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
        PG_READY=true
    fi
elif pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
    PG_READY=true
fi

if [ "$PG_READY" = true ]; then
    DB_USER="postgres"
    DB_NAME="finance_db"
    if [ -f ".env" ]; then
        ENV_USER=$(grep -E '^DB_USERNAME=' .env | cut -d '=' -f2- | tr -d '"' | tr -d "'" || true)
        ENV_NAME=$(grep -E '^DB_NAME=' .env | cut -d '=' -f2- | tr -d '"' | tr -d "'" || true)
        ENV_PASS=$(grep -E '^DB_PASSWORD=' .env | cut -d '=' -f2- | tr -d '"' | tr -d "'" || true)
        [ -n "$ENV_USER" ] && DB_USER="$ENV_USER"
        [ -n "$ENV_NAME" ] && DB_NAME="$ENV_NAME"
        export PGPASSWORD="$ENV_PASS"
    fi
    
    # Try non-interactive query (-w disables password prompt)
    if /Library/PostgreSQL/18/bin/psql -h localhost -p 5432 -U "$DB_USER" -d "$DB_NAME" -w -c "SELECT 1;" >/dev/null 2>&1; then
        echo -e "  Status:     ${GREEN}● ONLINE & CONNECTED${NC}"
        echo -e "  Database:   $DB_NAME (User: $DB_USER)"
    else
        echo -e "  Status:     ${GREEN}● ONLINE & ACCEPTING CONNECTIONS${NC}"
        echo -e "  Note:       Socket active on port 5432"
    fi
else
    echo -e "  Status:     ${RED}✖ OFFLINE${NC}"
    ALL_HEALTHY=false
fi

# 2. PaddleOCR Service Check
echo -e "\n${BOLD}[2/4] Local PaddleOCR Microservice (Port 5005)${NC}"
OCR_HEALTH=$(curl -s --connect-timeout 2 "http://127.0.0.1:5005/health" || true)
if echo "$OCR_HEALTH" | grep -q '"status":"UP"'; then
    echo -e "  Status:     ${GREEN}● ONLINE & HEALTHY${NC}"
    echo -e "  Response:   $OCR_HEALTH"
else
    echo -e "  Status:     ${RED}✖ OFFLINE / UNHEALTHY${NC}"
    ALL_HEALTHY=false
fi

# 3. Spring Boot Backend Check
echo -e "\n${BOLD}[3/4] Spring Boot Backend (Port 2906)${NC}"
BACKEND_HEALTH=$(curl -s --connect-timeout 2 "http://127.0.0.1:2906/api/health" || true)
if [ "$BACKEND_HEALTH" = "Backend is running!" ]; then
    echo -e "  Status:     ${GREEN}● ONLINE & RESPONSIVE${NC}"
    echo -e "  Endpoint:   http://127.0.0.1:2906/api/health"
else
    echo -e "  Status:     ${RED}✖ OFFLINE / UNHEALTHY${NC}"
    ALL_HEALTHY=false
fi

# 4. Nginx Production Server Check
echo -e "\n${BOLD}[4/4] Nginx Production Web Server (Port 8080)${NC}"
NGINX_FRONTEND_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 "http://localhost:8080/" || true)
NGINX_API_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 "http://localhost:8080/api/health" || true)

if [ "$NGINX_FRONTEND_CODE" = "200" ] && [ "$NGINX_API_CODE" = "200" ]; then
    echo -e "  Frontend:   ${GREEN}● ONLINE (HTTP $NGINX_FRONTEND_CODE)${NC}"
    echo -e "  API Proxy:  ${GREEN}● WORKING (HTTP $NGINX_API_CODE)${NC}"
    echo -e "  Entrypoint: ${BOLD}http://localhost:8080${NC}"
else
    echo -e "  Frontend:   HTTP $NGINX_FRONTEND_CODE"
    echo -e "  API Proxy:  HTTP $NGINX_API_CODE"
    echo -e "  Status:     ${RED}✖ NGINX OR REVERSE PROXY ISSUE${NC}"
    ALL_HEALTHY=false
fi

# Summary
echo -e "\n${BOLD}${BLUE}------------------------------------------------------${NC}"
if [ "$ALL_HEALTHY" = true ]; then
    echo -e "${GREEN}${BOLD}✔ ALL NIVARA SERVICES ARE RUNNING AND READY!${NC}"
    echo -e "Open your browser at: ${BOLD}${GREEN}http://localhost:8080${NC}\n"
    exit 0
else
    echo -e "${YELLOW}${BOLD}⚠ ONE OR MORE SERVICES REQUIRE ATTENTION.${NC}"
    echo -e "Run ${BOLD}./start-nivara.sh${NC} to start missing services or ${BOLD}./logs-nivara.sh${NC} to inspect errors.\n"
    exit 1
fi
