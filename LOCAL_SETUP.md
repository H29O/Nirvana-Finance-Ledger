# Nivara — Local Operations & Startup Guide

This document details the complete local architecture, operations, background process management, and macOS automatic startup configuration for **Nivara**.

---

## 1. Architecture & Component Overview

Nivara runs 100% locally on macOS with zero external cloud dependencies (no Render, Supabase, Vercel, or cloud OCR).

```
                      Browser (http://localhost:8080)
                                    │
                                    ▼
       ┌────────────────────────────────────────────────────────┐
       │                Nginx Reverse Proxy (8080)              │
       │  - Serves React/Vite SPA static build (frontend/dist)  │
       │  - Proxies /api/* to Spring Boot (port 2906)           │
       └────────────────────────────┬───────────────────────────┘
                                    │
                                    ▼
       ┌────────────────────────────────────────────────────────┐
       │          Spring Boot Backend (Port 2906, Java 21)       │
       │  - Supervised by PM2 (nivara-backend)                  │
       │  - REST API, Spring Security, JWT, JPA / Flyway        │
       └──────────────┬──────────────────────────┬──────────────┘
                      │                          │
                      ▼                          ▼
       ┌────────────────────────┐      ┌────────────────────────┐
       │   PostgreSQL 18 DB     │      │   PaddleOCR Service    │
       │   Port 5432 (finance_db│      │   Port 5005 (Python 3.12   │
       │   macOS LaunchDaemon   │      │   Supervised by PM2    │
       └────────────────────────┘      └────────────────────────┘
```

### Component Details & Ports

| Component | Port | Management / Host | Binary / Interpreter |
| :--- | :--- | :--- | :--- |
| **Nivara Web Entrypoint** | `8080` | Nginx (Homebrew LaunchAgent) | `/opt/homebrew/bin/nginx` |
| **Spring Boot Backend** | `2906` | PM2 (`nivara-backend`) | OpenJDK 21 (`/opt/homebrew/Cellar/openjdk@21/...`) |
| **PaddleOCR Service** | `5005` | PM2 (`nivara-ocr`) | Python 3.12 (`ocr-service/venv/bin/python`) |
| **PostgreSQL Database** | `5432` | macOS LaunchDaemon | PostgreSQL 18 (`/Library/PostgreSQL/18/bin/postgres`) |

---

## 2. Prerequisites & Installed Software

All required software is installed and configured on this macOS machine:
- **macOS**: Apple Silicon Darwin (aarch64)
- **Java**: OpenJDK 21 (`21.0.12.1`)
- **Node.js & PM2**: Node `v26.9.0`, npm `11.19.1`, PM2 `7.0.4`
- **Nginx**: `1.31.6` (Homebrew)
- **PostgreSQL**: `18.6` (Cluster data in `/Library/PostgreSQL/18/data`, database `finance_db`)
- **Python**: `3.12.15` in `ocr-service/venv` with PaddleOCR 3.x and pre-cached models in `~/.paddlex`

---

## 3. Application URLs

- **Normal Production Use (Recommended)**:  
  **`http://localhost:8080`**  
  Direct SPA navigation, bookmarks, and page refreshes work out of the box via Nginx routing fallback.
- **Backend Direct Health**:  
  `http://localhost:2906/api/health`
- **OCR Service Direct Health**:  
  `http://localhost:5005/health`

---

## 4. Simple Management Commands

Management shell scripts are available in the project root:

### Check Status
Inspect the health of PostgreSQL, PaddleOCR, Spring Boot, and Nginx:
```bash
./status-nivara.sh
```

### Start Application
Start PM2 services, verify PostgreSQL, ensure Nginx is running, and wait for health readiness:
```bash
./start-nivara.sh
```

### Stop Application
Safely stop the backend and OCR background processes:
```bash
./stop-nivara.sh
```
To also stop Nginx:
```bash
./stop-nivara.sh --all
```
*(Note: PostgreSQL remains running by default to safeguard your financial ledger data.)*

### Restart Application
Restart PM2 processes and reload Nginx configuration:
```bash
./restart-nivara.sh
```

### View Application Logs
Tail logs with automatic masking of sensitive credentials:
```bash
./logs-nivara.sh backend       # Spring Boot output and error logs
./logs-nivara.sh ocr           # PaddleOCR microservice logs
./logs-nivara.sh nginx         # Nginx access and error logs
./logs-nivara.sh all           # Combined PM2 and Nginx logs
./logs-nivara.sh backend -f    # Follow/stream Spring Boot logs in real time
```

---

## 5. macOS Automatic Startup

The application is configured to start automatically on system boot and user login without opening terminal windows.

### Startup Configuration Components
1. **PostgreSQL**: Managed by `/Library/LaunchDaemons/postgresql-18.plist`. Automatically launches at system startup under the `postgres` user.
2. **Nginx**: Managed by `~/Library/LaunchAgents/sh.brew.nginx.plist`. Automatically starts at user login.
3. **PM2 Backend & OCR**:
   - PM2 state is saved in `~/.pm2/dump.pm2`.
   - Managed by `~/Library/LaunchAgents/com.nivara.pm2.resurrect.plist`, which triggers `pm2 resurrect` automatically upon user login.

### Managing Automatic Startup
- **To update the auto-start process snapshot**:
  ```bash
  pm2 save
  ```
- **To disable automatic resurrection at login**:
  ```bash
  launchctl unload ~/Library/LaunchAgents/com.nivara.pm2.resurrect.plist
  ```
- **To re-enable automatic resurrection**:
  ```bash
  launchctl load ~/Library/LaunchAgents/com.nivara.pm2.resurrect.plist
  ```

---

## 6. Developer Workflow

You can seamlessly switch between **Normal-use mode** and **Development mode**.

### Switching to Development Mode
To develop the backend in IntelliJ IDEA or run the Vite dev server with Hot Module Replacement (HMR):

1. **Stop the background services that conflict with dev ports**:
   ```bash
   ./stop-nivara.sh
   ```
2. **Run the Frontend Development Server (Port 5173)**:
   ```bash
   cd frontend
   npm run dev
   ```
   Access dev frontend with HMR at `http://localhost:5173`.
3. **Run the Backend in IntelliJ or Maven Wrapper (Port 2906)**:
   ```bash
   cd backend
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)
   ./mvnw spring-boot:run
   ```
4. **Run Backend Tests**:
   ```bash
   cd backend
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)
   ./mvnw test
   ```
5. **Switch Back to Normal Background Mode**:
   ```bash
   ./start-nivara.sh
   ```

---

## 7. Rebuilding Artifacts

If you modify frontend or backend source code and want to update the production background services:

### Rebuilding Frontend
```bash
cd frontend
npm run build
cd ..
./restart-nivara.sh
```

### Rebuilding Backend
```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean package -DskipTests
cd ..
./restart-nivara.sh
```

---

## 8. File Locations & Configuration

- **PM2 Ecosystem File**: `ecosystem.config.cjs` (Project root)
- **Nginx Configuration**: `nginx/nivara.conf` (Symlinked/copied to `/opt/homebrew/etc/nginx/servers/nivara.conf`)
- **Backend Configuration**: `backend/src/main/resources/application.properties`
- **Environment Secrets**: `.env` (Project root — not tracked in Git)
- **Application Logs Directory**: `logs/`
  - `logs/backend-out.log`
  - `logs/backend-error.log`
  - `logs/ocr-out.log`
  - `logs/ocr-error.log`
  - `logs/nginx-access.log`
  - `logs/nginx-error.log`
  - `logs/pm2-startup.log`

---

## 9. Environment Variables Reference

Nivara reads configuration dynamically from the local `.env` file. Never commit actual secret values to version control.

| Variable | Description | Default / Local Value |
| :--- | :--- | :--- |
| `DB_HOST` | PostgreSQL hostname | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | PostgreSQL database name | `finance_db` |
| `DB_USERNAME` | PostgreSQL user | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | *(local password)* |
| `PORT` | Spring Boot port | `2906` |
| `JWT_SECRET` | Secret key for signing JWT tokens | *(secure hex string)* |
| `PADDLE_OCR_URL` | Local OCR service URL | `http://localhost:5005` |
| `OCR_AUTO_START_ENABLED` | Handled by PM2 in background mode | `false` (when PM2 active) |
| `SPRING_MAIL_HOST` | Gmail SMTP host | `smtp.gmail.com` |
| `SPRING_MAIL_PORT` | Gmail SMTP SSL port | `465` |
| `GMAIL_USERNAME` | SMTP account email | *(your email address)* |
| `GMAIL_APP_PASSWORD` | Google 16-character App Password | *(your app password)* |
| `APP_FRONTEND_URL` | Frontend URL for email verification | `http://localhost:8080` |

---

## 10. Database Backup and Restore

To protect your personal financial records:

### Safe Backup
```bash
mkdir -p backups
export PGPASSWORD=$(grep -E '^DB_PASSWORD=' .env | cut -d '=' -f2- | tr -d '"' | tr -d "'")
/Library/PostgreSQL/18/bin/pg_dump -h localhost -p 5432 -U postgres -d finance_db -F c -b -v -f "backups/nivara_backup_$(date +%Y%m%d_%H%M%S).dump"
```

### Safe Restore
```bash
export PGPASSWORD=$(grep -E '^DB_PASSWORD=' .env | cut -d '=' -f2- | tr -d '"' | tr -d "'")
/Library/PostgreSQL/18/bin/pg_restore -h localhost -p 5432 -U postgres -d finance_db -v --clean --if-exists "backups/<backup_file>.dump"
```

---

## 11. Troubleshooting

### Port Conflicts
If a port is unexpectedly in use:
```bash
lsof -i :8080   # Check Nginx port
lsof -i :2906   # Check Spring Boot port
lsof -i :5005   # Check PaddleOCR port
lsof -i :5432   # Check PostgreSQL port
```

### PostgreSQL Inaccessible
Verify service status with `pg_isready`:
```bash
/Library/PostgreSQL/18/bin/pg_isready -h localhost -p 5432
```
If PostgreSQL is stopped, inspect `/Library/LaunchDaemons/postgresql-18.plist`.

### OCR Service Fails to Process
1. Verify the Python virtual environment:
   ```bash
   ./ocr-service/venv/bin/python -m uvicorn app.main:app --host 127.0.0.1 --port 5005
   ```
2. Check `logs/ocr-error.log`.
3. Confirm cached model files exist in `~/.paddlex/official_models/`.
