# PathBoot PI - Standard Setup and Operations Guidelines

PathBoot PI is a Spring Boot-based multilingual Q&A assistant designed to explore how a practical solution could help users find information about Norwegian tax, NAV, and immigration services in English, Amharic, and Norwegian.

This is primarily an experimental and exploratory project: it models a real-world problem and tests whether a local-first, grounded AI workflow can support multilingual public-service guidance. The current grounding data is synthetic and fabricated for prototype purposes, so this project should be understood as a proof of concept and research-oriented implementation rather than a production-ready public service.

This document serves as the operational guide for setup, experimentation, testing, and environment-based execution across local, test, and production-like configurations.

## Project Objective

The main objective of PathBoot PI is to model and evaluate a solution for a real-world access problem: helping people navigate complex public-service information across language barriers and fragmented knowledge sources.

Key goals:
- explore how multilingual Q&A can support users with different language needs
- model a domain-grounded AI workflow for tax, NAV, and immigration topics
- test whether retrieval, translation, and LLM orchestration can improve answer quality
- create a customizable foundation for future real-world deployment with real data sources

## Current Limitations

This project is intentionally exploratory. The grounding data used in the current implementation is fabricated and synthetic, which means the system is designed to validate architecture, workflow, and feasibility rather than provide official or production-grade answers.

Important limitations:
- the current knowledge base is not a real official source of truth
- answer quality depends on synthetic sample data rather than verified public-domain content
- the project does not yet represent a production-ready public service
- additional domain data, validation, and governance are required before it can be used in a real environment

## Scope

This guide covers:
- local development setup
- test environment execution
- production deployment configuration
- Docker-based execution by profile
- authentication and API verification
- troubleshooting and operational notes

## Runtime Components

PathBoot PI runs with the following services:
- Ollama (`mistral`) for chat and classification support
- Ollama (`nomic-embed-text`) for RAG embeddings
- NLLB (`facebook/nllb-200-distilled-600M`) for Amharic <-> English translation
- SQLite in local mode
- PostgreSQL in test and production modes

## Table of Contents

1. [What Runs Where](#what-runs-where)
2. [Prerequisites](#prerequisites)
3. [Standard Environment Guidelines](#standard-environment-guidelines)
4. [Quick Start (Standard Local Development)](#quick-start-standard-local-development)
5. [Manual API Testing](#manual-api-testing)
6. [Service Interaction Flow](#service-interaction-flow)
7. [Authentication](#authentication)
8. [API Usage](#api-usage)
9. [Configuration](#configuration)
10. [Run Tests](#run-tests)
11. [Troubleshooting](#troubleshooting)
12. [Documentation Map](#documentation-map)
13. [Beginner Guide](#beginner-guide)

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

## Standard Environment Guidelines

This project supports three Spring profiles:

- `local` – SQLite + local Ollama/NLLB for development
- `test` – PostgreSQL + QA/integration configuration
- `prod` – PostgreSQL + hardened production settings

Use the profile that matches the environment. Do not use production settings in local development and do not use local SQLite in production.

### Local environment (`local`)

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

Manual run:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd "-Dmaven.test.skip=true" spring-boot:run
```

### Test environment (`test`)

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/pathboot_test"
$env:SPRING_DATASOURCE_USERNAME = "pathboot_test"
$env:SPRING_DATASOURCE_PASSWORD = "pathboot_test_password"
.\mvnw.cmd spring-boot:run
```

### Production environment (`prod`)

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://host:5432/pathboot_prod"
$env:SPRING_DATASOURCE_USERNAME = "pathboot"
$env:SPRING_DATASOURCE_PASSWORD = "<secure-password>"
$env:PATHBOOT_API_KEY_1 = "<secure-key-1>"
$env:PATHBOOT_API_KEY_2 = "<secure-key-2>"
$env:PATHBOOT_ADMIN_PASSWORD = "<admin-password>"
$env:PATHBOOT_USER_PASSWORD = "<user-password>"

.\mvnw.cmd "-DskipTests" package
java -jar .\target\pathboot-pi-1.0.0.jar
```

Production notes:
- PostgreSQL is required
- Swagger/OpenAPI is disabled
- API keys are enforced
- schema is validated, not auto-migrated
- secrets must be loaded from environment variables or a secret manager

### Docker-based profile execution

This repo now includes one Dockerfile per profile:

- `Dockerfile.local`
- `Dockerfile.test`
- `Dockerfile.prod`

Each file sets the corresponding Spring profile as the default, so you can build and run a profile-specific image without passing the profile in every command.

#### 1) Build the image for a profile

```powershell
docker build -f Dockerfile.local -t pathboot-pi-local .
docker build -f Dockerfile.test -t pathboot-pi-test .
docker build -f Dockerfile.prod -t pathboot-pi-prod .
```

#### 2) Run in `local` profile

This profile is intended for local development using SQLite and local Ollama/NLLB services.

```powershell
docker run --rm -it \
  -p 8080:8080 \
  -e SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434 \
  -e NLLB_SERVER_BASE_URL=http://host.docker.internal:5000 \
  -v "${PWD}\data:/app/data" \
  pathboot-pi-local
```

Notes:
- `host.docker.internal` is used so the container can reach services running on the host machine.
- SQLite data is stored in the local `data` directory.

#### 3) Run in `test` profile

This profile uses PostgreSQL for QA or integration testing.

```powershell
docker run --rm -it \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/pathboot_test \
  -e SPRING_DATASOURCE_USERNAME=pathboot_test \
  -e SPRING_DATASOURCE_PASSWORD=pathboot_test_password \
  pathboot-pi-test
```

#### 4) Run in `prod` profile

This profile is for deployment and requires real secrets. Do not hardcode credentials in Docker commands.

```powershell
docker run --rm -it \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/pathboot_prod \
  -e SPRING_DATASOURCE_USERNAME=pathboot \
  -e SPRING_DATASOURCE_PASSWORD=<secure-password> \
  -e PATHBOOT_API_KEY_1=<secure-key-1> \
  -e PATHBOOT_API_KEY_2=<secure-key-2> \
  -e PATHBOOT_ADMIN_PASSWORD=<admin-password> \
  -e PATHBOOT_USER_PASSWORD=<user-password> \
  -e FRONTEND_URL=https://your-frontend.example.com \
  pathboot-pi-prod
```

Important production notes:
- Swagger is disabled in `prod`
- PostgreSQL is required
- `FRONTEND_URL` should be set for CORS
- secrets must come from environment variables or a secret manager

## Quick Start (Standard Local Development)

From the project root (`PathBootPI/`):

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

To stop services:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-local.ps1
```

Smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File .\smoke-api.ps1
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

| File                                       | Purpose                                         | Profile |
|--------------------------------------------|-------------------------------------------------|---------|
| `src/main/resources/application.yml`       | Shared base config (Ollama, NLLB, API, Swagger) | All     |
| `src/main/resources/application-local.yml` | SQLite datasource, dev logging                  | `local` |
| `src/main/resources/application-test.yml`  | PostgreSQL, test logging, in-memory RAG         | `test`  |
| `src/main/resources/application-prod.yml`  | PostgreSQL, strict security, minimal logging    | `prod`  |

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

Keep the documentation set small and focused:

- Main project guide: `README.md`
- Clone/setup/run guide: `docs/CLONE_SETUP_RUN.md`
- Beginner-friendly setup guide: `docs/GETTING_STARTED_BEGINNER.md`
- Domain extension guide: `docs/ADDING_NEW_DOMAIN.md`
- Architecture diagram: `docs/pathboot-architecture.d2` and `docs/PathBoot_Architecture.png`

## Beginner-Friendly Guide

For a simpler setup flow, see:

- `docs/GETTING_STARTED_BEGINNER.md`

## From Clone to Running

For a complete end-to-end setup from a fresh machine, see:

- `docs/CLONE_SETUP_RUN.md`
