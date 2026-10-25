const fs = require('fs');
const path = require('path');

// Helper to safely load environment variables from local .env
function loadLocalEnv() {
  const env = {};
  const envPath = path.resolve(__dirname, '.env');
  if (fs.existsSync(envPath)) {
    const lines = fs.readFileSync(envPath, 'utf8').split('\n');
    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#') || !trimmed.includes('=')) continue;
      const eqIdx = trimmed.indexOf('=');
      const key = trimmed.slice(0, eqIdx).trim();
      let val = trimmed.slice(eqIdx + 1).trim();
      if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
        val = val.slice(1, -1);
      }
      env[key] = val;
    }
  }
  return env;
}

const localEnv = loadLocalEnv();

// Discover Java 21 Home
let javaBinary = '/usr/bin/java';
try {
  const { execSync } = require('child_process');
  const javaHome = execSync('/usr/libexec/java_home -v 21', { encoding: 'utf8' }).trim();
  if (javaHome) {
    javaBinary = path.join(javaHome, 'bin', 'java');
  }
} catch (e) {
  // Fallback to Homebrew openjdk@21 path if available
  const brewJava = '/opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home/bin/java';
  if (fs.existsSync(brewJava)) {
    javaBinary = brewJava;
  }
}

const jarPath = path.resolve(__dirname, 'backend/target/backend-0.0.1-SNAPSHOT.jar');
const pythonBinary = path.resolve(__dirname, 'ocr-service/venv/bin/python');
const logDir = path.resolve(__dirname, 'logs');

module.exports = {
  apps: [
    {
      name: 'nivara-ocr',
      cwd: path.resolve(__dirname, 'ocr-service'),
      script: pythonBinary,
      args: ['-m', 'uvicorn', 'app.main:app', '--host', '127.0.0.1', '--port', '5005'],
      exec_interpreter: 'none',
      exec_mode: 'fork',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      min_uptime: '10s',
      restart_delay: 3000,
      kill_timeout: 8000,
      env: {
        OCR_HOST: '127.0.0.1',
        OCR_PORT: '5005',
        PYTHONUNBUFFERED: '1',
        PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK: 'True',
        ...localEnv
      },
      out_file: path.join(logDir, 'ocr-out.log'),
      error_file: path.join(logDir, 'ocr-error.log'),
      merge_logs: true,
      time: true
    },
    {
      name: 'nivara-backend',
      cwd: __dirname,
      script: javaBinary,
      args: ['-jar', jarPath],
      exec_interpreter: 'none',
      exec_mode: 'fork',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      min_uptime: '15s',
      restart_delay: 3000,
      kill_timeout: 10000,
      env: {
        PORT: '2906',
        DB_HOST: 'localhost',
        DB_PORT: '5432',
        DB_NAME: 'finance_db',
        DB_USERNAME: 'postgres',
        PADDLE_OCR_URL: 'http://localhost:5005',
        OCR_AUTO_START_ENABLED: 'false',
        ...localEnv
      },
      out_file: path.join(logDir, 'backend-out.log'),
      error_file: path.join(logDir, 'backend-error.log'),
      merge_logs: true,
      time: true
    }
  ]
};
