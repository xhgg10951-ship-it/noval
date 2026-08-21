# RUN.md — Local Startup (AI Story Co-Author v0.1)

Verified on Windows 11, Java 17 (Corretto), Node 22, Python 3.13 (venv), MySQL 8.4.

## 0. Prerequisites

- MySQL 8.4 running locally, database `story_ai`, app user `story_dev`.
- Python venv with FastAPI / Pydantic / LangChain / pytest.
- Maven 3.9+ (use the wrapper or `/c/tools/mvn.sh`).
- Node 22 + npm.

### MySQL setup (one time)

```sql
CREATE DATABASE story_ai CHARACTER SET utf8mb4;
CREATE USER 'story_dev'@'localhost' IDENTIFIED BY 'storypass';
GRANT ALL PRIVILEGES ON story_ai.* TO 'story_dev'@'localhost';
FLUSH PRIVILEGES;
```

The schema migrations live in `backend/src/main/resources/db/V1__...sql` .. `V5__...sql`
and are applied manually (no Flyway/Liquibase in v0.1):

```bash
mysql -u story_dev -pstorypass story_ai < backend/src/main/resources/db/V1__story_schema.sql
mysql -u story_dev -pstorypass story_ai < backend/src/main/resources/db/V2__stage_schema.sql
mysql -u story_dev -pstorypass story_ai < backend/src/main/resources/db/V3__chapter_schema.sql
mysql -u story_dev -pstorypass story_ai < backend/src/main/resources/db/V4__memory_schema.sql
mysql -u story_dev -pstorypass story_ai < backend/src/main/resources/db/V5__generation_job.sql
```

## 1. Python AI Service (port 8000)

```bash
cd ai-service
<venv>/Scripts/python.exe -m uvicorn app.main:app --port 8000
```

Health: `GET http://localhost:8000/health` → `{"status":"ok","mock_llm":true}`.

- No `LLM_API_KEY` → deterministic `MockProvider` (offline, testable).
- Set `LLM_API_KEY` to enable the real LangChain provider.

## 2. Spring Boot Backend (port 8080)

```bash
cd backend
# build
/c/tools/mvn.sh clean package -DskipTests
# run (explicit port required — sandbox otherwise injects a random port)
java -jar target/story-ai-backend-0.1.0.jar --server.port=8080 --spring.profiles.active=local
```

DB credentials come from the `local` profile (`application-local.yml`); override via
`DB_USERNAME` / `DB_PASSWORD` env if needed. The backend talks to Python at
`http://localhost:8000` (override with `AI_SERVICE_BASE_URL`).

> **Dev alt (no jar):** `mvn spring-boot:run -Dspring-boot.run.profiles=local` also works.

## 3. Vue Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` → `http://localhost:8080`.

> Build for production: `npm run build` (output `dist/`). If the sandbox's safe-delete
> shim blocks emptying `dist/`, build to a temp dir (`npx vite build --outDir dist2`)
> or remove `dist/` manually first.

## 4. Tests

```bash
# Backend (needs DB_USERNAME/DB_PASSWORD=story_dev/storypass)
DB_USERNAME=story_dev DB_PASSWORD=storypass /c/tools/mvn.sh test

# Python
<venv>/Scripts/python.exe -m pytest

# Frontend
cd frontend && npm run build
```

## 5. Typical flow (UI)

1. Create Story (Core Idea + Constraints + Initial Stage Direction).
2. Enter a Stage Direction → Planner returns a chapter plan → Confirm.
3. Generate chapters (CONTINUOUS auto, or STEP-by-step with Continue).
4. Memory candidates extracted per chapter; author Applies / Ignores REVIEW items.
5. Story Query answers location/inventory/relationship from structured state.
6. Planner Suggestions offers 3 next-direction options.

## 6. Known environment quirks (see `.agent/STATE.md` → Known Blockers)

- Backend jar must launch with explicit `--server.port=8080`.
- Java 17 (spec says 21) — no behavior impact for v0.1.
- `vite build` may need a manual `dist/` clear before rebuild.
- All Java→Python calls use HTTP/1.1 (`SimpleClientHttpRequestFactory`) — do NOT switch
  the AI client back to the default `RestClient.Builder` bean (JDK h2c upgrade breaks FastAPI).
