# PathBoot PI - Multilingual Domain Q&A Assistant

PathBoot PI is a Spring Boot service that answers questions about Norwegian Tax, NAV, and Immigration in English, Amharic, and Norwegian.

It runs fully local with:
- Ollama (`mistral`) for chat and classification support
- Ollama (`nomic-embed-text`) for RAG embeddings
- NLLB (`facebook/nllb-200-distilled-600M`) for Amharic <-> English translation
- SQLite for persistence

## Table of Contents

1. [What Runs Where](#what-runs-where)
2. [Prerequisites](#prerequisites)
3. [Quick Start (Manual)](#quick-start-manual)
4. [Quick Start (Automated)](#quick-start-automated)
5. [Docker Compose Run](#docker-compose-run)
6. [Service Interaction Flow](#service-interaction-flow)
7. [Authentication](#authentication)
8. [API Usage](#api-usage)
9. [Configuration](#configuration)
10. [Run Tests](#run-tests)
11. [Troubleshooting](#troubleshooting)

## What Runs Where

- Backend API: `http://localhost:8080`
- Ollama API: `http://localhost:11434`
- NLLB server: `http://localhost:5000`
- Frontend (Vite dev): `http://localhost:5173`
- Frontend (Docker Compose): `http://localhost:3000`

## Prerequisites

- Java 17+
- Node.js 20+ (for `pathboot-ui`)
- Docker Desktop
- Ollama installed locally

Check basics:

```powershell
java -version
node -v
docker --version
ollama --version
```

Pull required Ollama models:

```powershell
ollama pull mistral
ollama pull nomic-embed-text
```

## Quick Start (Manual)

Use this startup order:
1. Ollama
2. NLLB
3. Backend
4. Frontend

### 1) Start Ollama

```powershell
ollama serve
```

In another terminal, verify:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:11434/api/tags"
```

### 2) Start NLLB translation service

From `pathboot-pi`:

```powershell
docker build -f Dockerfile.nllb -t pathboot-nllb .
docker rm -f pathboot-nllb 2>$null
docker run -d --name pathboot-nllb -p 5000:5000 pathboot-nllb
```

Verify health:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:5000/health"
```

### 3) Start backend (`pathboot-pi`)

```powershell
.\mvnw.cmd spring-boot:run
```

### 4) Start frontend (`pathboot-ui`)

```powershell
cd ..\pathboot-ui
npm install
npm run dev
```

## Quick Start (Automated)

From workspace root (`pathboot/`):

```powershell
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

Stop services:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-all.ps1
```

## Docker Compose Run

### Profile Overview

PathBoot supports three profiles for different environments:

| Profile | Database | Use Case | Service URLs |
|---------|----------|----------|--------------|
| `local` (default) | SQLite | Local development | Ollama, NLLB on localhost |
| `test` | PostgreSQL | Integration testing | PostgreSQL container in Compose |
| `prod` | PostgreSQL | Production deployment | Environment-managed PostgreSQL |

### Profile Activation Precedence

1. **CLI/Environment override** (highest priority)
   ```bash
   SPRING_PROFILES_ACTIVE=prod docker compose up --build
   ```

2. **`.env` file value**
   ```bash
   # Set in .env: SPRING_PROFILES_ACTIVE=test
   docker compose up --build
   ```

3. **Compose fallback default** (lowest priority – `local`)
   ```bash
   # No override, no .env → defaults to 'local'
   docker compose up --build
   ```

### Setup

1. Copy environment template:
   ```powershell
   Copy-Item .env.example .env
   ```

2. Edit `.env` if needed (defaults work for local development).

### Run Local (Default)

```powershell
# Uses docker-compose.yml only (SQLite, localhost services)
docker compose up --build
```

Access at:
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- Swagger: `http://localhost:8080/swagger-ui.html`

### Run Test Environment

```powershell
# Uses docker-compose.yml + docker-compose.test.yml (PostgreSQL)
SPRING_PROFILES_ACTIVE=test docker compose -f docker-compose.yml -f docker-compose.test.yml up --build
```

Or set in `.env`:
```bash
SPRING_PROFILES_ACTIVE=test
```

Then:
```powershell
docker compose -f docker-compose.yml -f docker-compose.test.yml up --build
```

### Run Production

```powershell
# Uses docker-compose.yml + docker-compose.prod.yml (PostgreSQL with strict security)
SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://db.example.com:5432/pathboot_prod \
SPRING_DATASOURCE_USERNAME=prod_user \
SPRING_DATASOURCE_PASSWORD=<secure-password> \
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build
```

### Startup Sequence

All profiles enforce this health-based startup order:

1. **Ollama** starts, waits 90s for model initialization
2. **NLLB** starts, waits 180s for model load
3. **Backend** waits for both to be `healthy`, then starts (60s startup window)
4. **Frontend** waits for backend health, then starts

This prevents premature backend startup failures due to missing dependencies.

### Stop All Services

```powershell
docker compose down
```

To also remove volumes (careful – data loss):
```powershell
docker compose down -v
```

## Security Model

### Authentication and Authorization
- All protected endpoints require the `X-API-Key` header.
- Authentication is stateless; no session cookies are used.
- The API key filter uses timing-safe comparison.
- Public routes stay limited to `/api/v1/auth/**`, `/swagger-ui/**`, `/api-docs/**`, `/v3/api-docs/**`, `/actuator/health`, `/actuator/info`, and `/error`.

### Profile Security Rules
| Profile | API key enforcement | Swagger | CORS | DB |
|---|---|---|---|---|
| local | enabled | enabled | allow all | SQLite |
| test | enabled | enabled | localhost-only | PostgreSQL |
| prod | enabled | disabled | `FRONTEND_URL` only | PostgreSQL |

### Production Requirements
- Set `FRONTEND_URL` for CORS restrictions.
- Set all `PATHBOOT_*` secrets from environment variables or a secret manager.
- Do not expose Swagger/OpenAPI in production.
- Keep actuator exposure to `health` and `info` only.
- Use PostgreSQL `validate` mode in production.

## Service Interaction Flow

End-to-end request flow:

1. User sends a question in `pathboot-ui`.
2. Frontend calls backend endpoints in `pathboot-pi`.
3. Backend detects language and classifies domain.
4. If needed, backend translates via NLLB (`/translate`).
5. Backend retrieves top-k grounding chunks (RAG) and calls Ollama.
6. Backend translates back (if needed), persists session/interaction, and returns response.

`pathboot-pi` is the orchestrator between UI, Ollama, NLLB, and SQLite.

## Authentication

All `/api/v1/chat/**` routes require `X-API-Key`.

Get key:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"pathboot-admin-2026"}'
```

Use key:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/chat" `
  -Headers @{"X-API-Key"="pathboot-dev-key-2026"} `
  -ContentType "application/json" `
  -Body '{"userInput":"What is the tax filing deadline?","sessionId":null}'
```

Default credentials are configured in `src/main/resources/application.yml` under `api.security.users`.

## API Usage

- `POST /api/v1/auth/login` (public)
- `POST /api/v1/chat` (requires `X-API-Key`)
- `GET /api/v1/chat/sessions/{sessionId}/history`
- `DELETE /api/v1/chat/sessions/{sessionId}`

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

## Configuration

### Configuration Files

| File | Purpose | Profile |
|------|---------|---------|
| `src/main/resources/application.yml` | Shared base config (Ollama, NLLB, API, Swagger) | All |
| `src/main/resources/application-local.yml` | SQLite datasource, dev logging | `local` |
| `src/main/resources/application-test.yml` | PostgreSQL, test logging, in-memory RAG | `test` |
| `src/main/resources/application-prod.yml` | PostgreSQL, strict security, minimal logging | `prod` |

### Important Configuration Keys

**Common to all profiles:**
- `spring.ai.ollama.base-url` – Ollama endpoint
- `spring.ai.ollama.chat.options.model` – LLM model
- `spring.ai.ollama.embedding.options.model` – Embedding model
- `nllb.server.base-url` – NLLB translation endpoint
- `rag.top-k` – Number of RAG chunks per query
- `api.security.*` – API keys and credentials

**Database-specific (per profile):**
- `local`: SQLite via `spring.datasource.url` (SQLiteDialect)
- `test`: PostgreSQL via `SPRING_DATASOURCE_*` env vars (PostgreSQLDialect)
- `prod`: PostgreSQL via `SPRING_DATASOURCE_*` env vars (PostgreSQLDialect)

### Environment Variable Override

All secrets and runtime URLs support environment variable override:

```bash
# Backend API
PATHBOOT_API_KEY_1=<secure-key-1>
PATHBOOT_API_KEY_2=<secure-key-2>
PATHBOOT_ADMIN_PASSWORD=<admin-pass>
PATHBOOT_USER_PASSWORD=<user-pass>

# Backend datasource (test/prod)
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>

# LLM/Translation service URLs (normally auto-wired in compose)
SPRING_AI_OLLAMA_BASE_URL=http://ollama:11434
NLLB_SERVER_BASE_URL=http://nllb:5000
```

Primary config file:

- `src/main/resources/application.yml`

Important keys:

- `spring.ai.ollama.base-url`
- `spring.ai.ollama.chat.options.model`
- `spring.ai.ollama.embedding.options.model`
- `nllb.server.base-url`
- `rag.top-k`
- `api.security.*`
- `server.port`

## Run Tests

```powershell
.\mvnw.cmd test
```

## Troubleshooting

| Problem | Fix |
|---|---|
| `Connection refused` on `11434` | Start Ollama with `ollama serve` or check `docker logs pathboot-ollama` |
| `Connection refused` on `5000` | Check `docker logs pathboot-nllb` – model may still be loading (normal for first run) |
| Backend fails to start in Compose | Check `docker logs pathboot-backend` – waits for Ollama + NLLB healthy (up to 90s + 180s) |
| `401 Unauthorized` | Include `X-API-Key` from `/api/v1/auth/login` |
| Slow first Amharic response | Expected on first model warm-up (NLLB cold-load) |
| Missing embeddings model | Run `ollama pull nomic-embed-text` |
| RAG not rebuilding after grounding changes | Delete `data/vector-store.json` and restart backend |
| Compose health checks timeout | Check service logs: `docker logs <container>` – increase `start_period` if needed |
| PostgreSQL connection fails (test/prod) | Verify `docker logs pathboot-postgres-test` and `SPRING_DATASOURCE_*` env vars match |
| Frontend cannot reach backend | Verify `VITE_API_BASE_URL` matches backend container URL in Compose override |

## Profiled Startup Dependencies

When using Docker Compose, startup is health-gated to prevent cascading failures:

```
Ollama (90s start_period)
   ↓
   └─ NLLB (180s start_period)
   ↓
   └─ Backend (waits for both healthy, 60s startup)
   ↓
   └─ Frontend (waits for backend healthy)
```

Check individual service health:
```powershell
# Ollama
docker exec pathboot-ollama curl -f http://localhost:11434/api/tags

# NLLB
docker exec pathboot-nllb curl -f http://localhost:5000/health

# Backend
docker exec pathboot-backend curl -f http://localhost:8080/actuator/health

# Frontend
docker exec pathboot-frontend curl -f http://localhost:80
```

## Documentation Map

- High-level index: `PATHBOOT_DOCS.md`
- Technical deep dive: `DOCUMENTATION.md`
- Technology rationale: `TECHNOLOGY_CHOICES.md`
- Domain extension guide: `ADDING_NEW_DOMAIN.md`
