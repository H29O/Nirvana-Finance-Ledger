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

echo -e "${BOLD}${BLUE}======================================================${NC}"
echo -e "${BOLD}${BLUE}              Starting Nivara Application             ${NC}"
echo -e "${BOLD}${BLUE}======================================================${NC}\n"

# Ensure log directory exists
mkdir -p "$PROJECT_DIR/logs"

# 1. PostgreSQL Check
echo -e "${BOLD}[1/4] Verifying PostgreSQL...${NC}"
PG_OK=false
if [ -x "/Library/PostgreSQL/18/bin/pg_isready" ]; then
    /Library/PostgreSQL/18/bin/pg_isready -h localhost -p 5432 >/dev/null 2>&1 && PG_OK=true
elif pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
    pg_isready -h localhost -p 5432 >/dev/null 2>&1 && PG_OK=true
fi

if [ "$PG_OK" != true ]; then
    echo -e "${RED}PostgreSQL is not running on port 5432!${NC}"
    echo -e "Please ensure PostgreSQL service is running or check /Library/LaunchDaemons/postgresql-18.plist"
    exit 1
fi
echo -e "${GREEN}✔ PostgreSQL is online.${NC}"

# 2. Build artifacts check
JAR_PATH="$PROJECT_DIR/backend/target/backend-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${YELLOW}Backend executable JAR not found. Building now...${NC}"
    JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "")
    if [ -n "$JAVA_HOME" ]; then
        export JAVA_HOME
    fi
    (cd backend && ./mvnw clean package -DskipTests)
fi

if [ ! -d "$PROJECT_DIR/frontend/dist" ] || [ ! -f "$PROJECT_DIR/frontend/dist/index.html" ]; then
    echo -e "${YELLOW}Frontend production build not found. Building now...${NC}"
    (cd frontend && npm run build)
fi

# 3. Start OCR & Spring Boot via PM2
echo -e "\n${BOLD}[2/4] Starting OCR & Backend services via PM2...${NC}"
pm2 start "$PROJECT_DIR/ecosystem.config.cjs" >/dev/null

# 4. Start Nginx
echo -e "\n${BOLD}[3/4] Ensuring Nginx reverse proxy is running...${NC}"
if ! brew services list | grep -E "^nginx\s+started" >/dev/null 2>&1; then
    brew services start nginx >/dev/null
else
    # Reload in case configuration was updated
    nginx -s reload >/dev/null 2>&1 || brew services restart nginx >/dev/null
fi
echo -e "${GREEN}✔ Nginx service active.${NC}"

# 5. Wait for service readiness
echo -e "\n${BOLD}[4/4] Awaiting service health verification...${NC}"

# Await OCR
OCR_UP=false
for i in {1..30}; do
    if curl -s --connect-timeout 1 "http://127.0.0.1:5005/health" | grep -q '"status":"UP"'; then
        OCR_UP=true
        break
    fi
    sleep 1
done

if [ "$OCR_UP" = true ]; then
    echo -e "  PaddleOCR Service: ${GREEN}READY${NC}"
else
    echo -e "  PaddleOCR Service: ${RED}FAILED TO BECOME READY IN 30s${NC}"
fi

# Await Backend
BACKEND_UP=false
for i in {1..40}; do
    if curl -s --connect-timeout 1 "http://127.0.0.1:2906/api/health" | grep -q "Backend is running!"; then
        BACKEND_UP=true
        break
    fi
    sleep 1
done

if [ "$BACKEND_UP" = true ]; then
    echo -e "  Spring Boot API:   ${GREEN}READY${NC}"
else
    echo -e "  Spring Boot API:   ${RED}FAILED TO BECOME READY IN 40s${NC}"
fi

# Save PM2 state for automatic resurrection
pm2 save >/dev/null 2>&1 || true

echo -e ""
./status-nivara.sh
