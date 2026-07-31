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
3. [Quick Start (Automated - Recommended)](#quick-start-automated---recommended)
4. [Quick Start (Manual)](#quick-start-manual)
5. [Manual API Testing](#manual-api-testing)
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
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Frontend: optional sibling repo (`pathboot-ui`) if present in your workspace

## Prerequisites

- Java 17+ (Java 17 recommended)
- Docker Desktop
- Ollama installed locally

Check basics:

```powershell
java -version
docker --version
ollama --version
```

Pull required Ollama models:

```powershell
ollama pull mistral
ollama pull nomic-embed-text
```

## Quick Start (Automated - Recommended)

From the project root (`PathBootPI/`):

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

Optional: force NLLB image rebuild:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1 -RebuildNllb
```

Stop services:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-local.ps1
```

Run API smoke test (login + protected chat call):

```powershell
powershell -ExecutionPolicy Bypass -File .\smoke-api.ps1
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

If your local JDK is newer and Maven fails in `testCompile`, run:

```powershell
.\mvnw.cmd "-Dmaven.test.skip=true" "-Dspring-boot.run.profiles=local" spring-boot:run
```

### 4) Start frontend (`pathboot-ui`)

```powershell
cd ..\pathboot-ui
npm install
npm run dev
```

## Manual API Testing

### Option A: Swagger UI

1. Open `http://localhost:8080/swagger-ui.html`
2. Call `POST /api/v1/auth/login` with:
   - `username`: `admin`
   - `password`: `pathboot-admin-2026`
3. Copy `apiKey` from response and click **Authorize**
4. Call `POST /api/v1/chat` with a test question

### Option B: PowerShell calls

```powershell
$loginBody = @{ username = "admin"; password = "pathboot-admin-2026" } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
$apiKey = $login.apiKey

$chatBody = @{ userInput = "What services does NAV provide?"; sessionId = $null } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/chat" -Headers @{ "X-API-Key" = $apiKey } -ContentType "application/json" -Body $chatBody
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
| `Connection refused` on `11434` | Start Ollama with `ollama serve` |
| `Connection refused` on `5000` | Check `docker logs pathboot-nllb` - first NLLB model load can take several minutes |
| Backend fails during `testCompile` | Use `start-local.ps1` or run Maven with `-Dmaven.test.skip=true` for local startup |
| `401 Unauthorized` | Include `X-API-Key` from `/api/v1/auth/login` |
| Slow first Amharic response | Expected on first model warm-up (NLLB cold-load) |
| Missing embeddings model | Run `ollama pull nomic-embed-text` |
| RAG not rebuilding after grounding changes | Delete `data/vector-store.json` and restart backend |

## Documentation Map

- High-level index: `PATHBOOT_DOCS.md`
- Technical deep dive: `DOCUMENTATION.md`
- Technology rationale: `TECHNOLOGY_CHOICES.md`
- Domain extension guide: `ADDING_NEW_DOMAIN.md`
