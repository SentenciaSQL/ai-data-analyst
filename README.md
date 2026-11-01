# AI Data Analyst

Multi-tenant SaaS for asking business questions in natural language. Each company keeps its own database. The platform stores tenant metadata, approved semantic definitions, and conversation history. It does not copy customer rows.

This repository contains the Phase 1 foundation: the Angular workspace, the Spring Boot API, and the Python AI engine, wired together with PostgreSQL (pgvector) and Redis.

## Applications

| Service | Path | Local port |
| --- | --- | --- |
| Angular workspace | `frontend/` | 4200 |
| Spring Boot API | `backend/` | 8080, management 9091 |
| AI engine | `ai-engine/` | 8090 |
| PostgreSQL 17 + pgvector | `compose.yaml` | 5432 |
| Redis | `compose.yaml` | 6379 |
| Prometheus | `compose.yaml` | 9090 |
| Grafana | `compose.yaml` | 3000 |
| OpenTelemetry collector | `compose.yaml` | 4318 |

Architecture details are in [docs/architecture.md](docs/architecture.md).

## What this release does

- Company registration, sign-in, refresh-token rotation, and logout.
- Roles: owner, admin, analyst, and viewer, with permission checks.
- Team management, company settings, and a read-only trial plan.
- Tenant isolation in the API and with PostgreSQL row-level security.
- A streaming analyst conversation. Without a provider key, the engine runs the LangGraph workflow and says it cannot answer from company data. It does not invent metrics.
- SQL validation for a single read-only statement, available to the API through the engine. Query execution against customer databases is not in this release.

Connections, schema discovery, the semantic catalog, dashboards, and query history have navigation and honest empty states. They are later phases.

## Run with Docker Compose

Docker Compose v2 is required. Copy the environment file and start the stack:

```bash
cp .env.example .env
docker compose up --build
```

Open `http://127.0.0.1:4200`. API docs are at `http://127.0.0.1:8080/api/docs`. Grafana is at `http://127.0.0.1:3000` (user `admin`, password from `GRAFANA_ADMIN_PASSWORD`).

`compose.override.yaml` publishes the API debugger on `127.0.0.1:5005`. Skip it with:

```bash
docker compose -f compose.yaml up --build
```

The values in `.env.example` are for local development. Set `ADA_ENV=production` only with real secrets. The API and the engine refuse development secrets in that mode.

The Postgres init script creates the `ada_app` role the first time the data volume is empty. Flyway then runs as the owner role `ada`. The API connects as `ada_app`, which is subject to row-level security.

## Run on the host

Use this when you want to rebuild one application without Compose.

1. PostgreSQL 17 with the `vector` extension, database `ai_data_analyst`, owner role `ada`, and login role `ada_app`.
2. Redis on `127.0.0.1:6379`.
3. API:

```bash
mvn -B -f backend/pom.xml -DskipTests package
java -jar backend/target/ai-data-analyst-api.jar
```

4. AI engine, from `ai-engine/` with Python 3.12:

```bash
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8090
```

5. Frontend:

```bash
cd frontend
npm ci
npm start
```

The dev server proxies `/api` to `http://127.0.0.1:8080`.

## Configuration

See `.env.example`. Important variables:

- `POSTGRES_PASSWORD`, `APP_DB_PASSWORD` — owner and application database passwords.
- `JWT_SECRET` — at least 32 characters.
- `INTERNAL_API_KEY` — shared by the API and the AI engine. The engine does not accept browser traffic on `/internal`.
- `AI_PROVIDER` — `openai` or `anthropic`. Leave the matching API key empty to keep the engine offline from a model provider.
- `CORS_ORIGINS` — browser origins allowed to call the API directly.
- `PRIMENG_LICENSE` — optional PrimeNG community or commercial key, applied when the frontend image is built. Without it, the workspace shows PrimeNG's license notice. Do not commit a key.

Do not commit `.env`.

## Build check

```bash
mvn -B -f backend/pom.xml -DskipTests package
cd frontend && npm ci && npm run build
python -m compileall -q ai-engine/app
```

The same steps run in `.github/workflows/build.yml`.
