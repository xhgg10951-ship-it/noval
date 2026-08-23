# RUN.md — v0.1.1 Local Runbook

Verified baseline: Java 17, Maven 3.9+, Node 22, Python 3.11+, MySQL 8.

## 1. Database

Create the local database once:

```sql
CREATE DATABASE story_ai CHARACTER SET utf8mb4;
CREATE USER 'story_dev'@'localhost' IDENTIFIED BY 'storypass';
GRANT ALL PRIVILEGES ON story_ai.* TO 'story_dev'@'localhost';
FLUSH PRIVILEGES;
```

v0.1.1 uses the additive migrations `V1` through `V15`. The project does not
embed Flyway/Liquibase, so apply each migration exactly once in numeric order.
Existing v0.1 data must be preserved.

PowerShell:

```powershell
1..15 | ForEach-Object {
  $migration = Get-ChildItem "backend/src/main/resources/db/V$($_)__*.sql"
  Get-Content -Raw -LiteralPath $migration.FullName |
    mysql.exe -h 127.0.0.1 -u story_dev -pstorypass story_ai
}
```

Bash:

```bash
for version in $(seq 1 15); do
  migration=$(find backend/src/main/resources/db -name "V${version}__*.sql" -print -quit)
  mysql -h 127.0.0.1 -u story_dev -pstorypass story_ai < "$migration"
done
```

## 2. Python AI Service (port 8000)

```powershell
cd ai-service
python -m pip install -r requirements.txt
$env:LLM_API_KEY = "<provider-key>"
$env:LLM_BASE_URL = "<OpenAI-compatible-base-url>"
$env:LLM_MODEL = "qwen3.7-plus"
python -m uvicorn app.main:app --port 8000
```

`API_KEY`, `API_URL`, and `MODEL` remain compatibility aliases. The v0.1.1
release model is `qwen3.7-plus`; without an API key the service deliberately
uses its deterministic mock provider. Health: `GET http://localhost:8000/health`.

## 3. Spring Boot Backend (port 8080)

```powershell
cd backend
$env:DB_USERNAME = "story_dev"
$env:DB_PASSWORD = "storypass"
mvn spring-boot:run
```

Optional overrides: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and
`AI_SERVICE_URL` (default `http://localhost:8000`). A versioned library jar can
be produced with `mvn clean package`; the supported local startup is the Maven
Spring Boot goal above.

## 4. Vue Frontend (port 5173)

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`; Vite proxies `/api` to the backend.

## 5. Current v0.1.1 Workflows

1. Create a Story and Stage, generate a plan, review it, and confirm it.
2. Generate in STEP or CONTINUOUS mode; the UI polls progress. CONTINUOUS jobs
   can be paused at the next safe checkpoint or stopped.
3. Revise a draft through Manual Edit, Regenerate, or AI Polish. Each path
   updates the current chapter view and refreshes chapter-derived Memory.
4. Approve the selected revision when it is author-accepted.
5. Use Replan Remaining to supersede only unfinished plans while preserving
   completed Chapter history and logical chapter order.

## 6. Tests

```powershell
# Backend (requires MySQL with V1..V15)
cd backend
$env:DB_USERNAME = "story_dev"
$env:DB_PASSWORD = "storypass"
mvn test

# Python deterministic/mock suite (tests isolate ambient LLM credentials)
cd ../ai-service
python -m pytest tests -q

# Frontend
cd ../frontend
npm ci
npm test
npm run build
```

The real-LLM acceptance suite is intentionally separate from ordinary pytest
and CI. It must run explicitly with `LLM_MODEL=qwen3.7-plus` for RH-10.
