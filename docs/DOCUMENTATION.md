# PathBoot PI - Technical Documentation

> Last updated: 2026-07-31

## 1. Project Goal

PathBoot PI is a multilingual assistant focused on Norwegian Tax, NAV, and Immigration.

- Input languages: English, Amharic, Norwegian
- Output language: same as user language
- Security: API key authentication for chat endpoints
- Runtime: local-first (Ollama + NLLB + SQLite)

## 2. Runtime Topology

### Local development

- `pathboot-ui` (Vite): `http://localhost:5173`
- `pathboot-pi` (Spring Boot): `http://localhost:8080`
- Ollama: `http://localhost:11434`
- NLLB Flask server: `http://localhost:5000`

### Optional frontend

- `pathboot-ui` can run separately (if present in workspace) and call backend at `http://localhost:8080`.

## 3. Architecture Overview

```
Browser (pathboot-ui)
    |
    v
Spring Boot API (pathboot-pi)
    |
    +--> ApiKeyAuthFilter (protect /api/v1/chat/**)
    +--> ChatController
           |
           v
      ChatFacadeService (orchestrator)
           |
           +--> LanguageDetectionUtil
           +--> TranslationOrchestrationService
           |      +--> NllbTranslationService (Amharic <-> English)
           |      \--> OllamaTranslationService (Norwegian handling)
           +--> DomainClassificationService
           +--> AgentFactory (TaxAgent/NavAgent/ImmigrationAgent)
           +--> RagGroundingService (SimpleVectorStore)
           +--> ChatClient (Ollama, model: mistral)
           +--> AsyncPersistenceService (SQLite)
           \--> UserSessionManager (memory + SQLite)
```

## 4. Request Processing Pipeline

1. `ApiKeyAuthFilter` validates `X-API-Key` for protected endpoints.
2. Language is detected (`ENGLISH`, `AMHARIC`, `NORWEGIAN`).
3. If needed, input is translated to English for downstream processing.
4. Domain is classified via keyword scoring (`TAX`, `NAV`, `IMMIGRATION`, `UNKNOWN`).
5. If `UNKNOWN`, pipeline short-circuits with a language-specific clarification message.
6. Matching domain agent retrieves top-k grounding chunks from vector store.
7. Agent calls Ollama (`mistral`) with domain prompt + RAG context.
8. Response is translated back to original language if needed.
9. Interaction/session are persisted and returned to client.

## 5. Service Interaction Between Applications

### UI <-> Backend

- UI sends chat messages to backend REST endpoints (`/api/v1/chat/**`).
- UI does not call Ollama or NLLB directly.

### Backend <-> Ollama

- Backend calls Ollama for:
  - response generation
  - embedding generation (`nomic-embed-text`) for RAG
  - Norwegian translation support path

### Backend <-> NLLB

- Backend calls NLLB REST endpoint:
  - `POST /translate`
- NLLB supports:
  - `amh_Ethi -> eng_Latn`
  - `eng_Latn -> amh_Ethi`
- Health endpoint:
  - `GET /health`

### Persistence

- SQLite stores interactions and sessions in `pathboot-pi/data/pathboot-data.db`.
- Vector embeddings persist in `pathboot-pi/data/vector-store.json`.

## 6. Security Model

- Public endpoint:
  - `POST /api/v1/auth/login`
- Protected endpoints:
  - `/api/v1/chat/**`

Credential and key mappings are configured in `src/main/resources/application.yml` under:

- `api.security.users`
- `api.security.api-keys`
- `api.security.user-keys`

## 7. Run Modes

### Mode A: manual local startup

Run services in order:

1. Ollama
2. NLLB
3. Backend
4. Frontend

Commands are documented in `README.md`.

### Mode B: automated startup scripts (recommended)

From this repository root (`PathBootPI/`):

- `start-local.ps1`
- `stop-local.ps1`
- `smoke-api.ps1` (login + protected chat smoke test)

### Mode C: Docker Compose (if available in your workspace)

Some environments include compose files at a higher-level workspace. This repository's
default documented and validated flow is `start-local.ps1` / `stop-local.ps1`.

## 8. Data Grounding and RAG

Grounding files:

- `src/main/resources/grounding/tax/tax-grounding.txt`
- `src/main/resources/grounding/nav/nav-grounding.txt`
- `src/main/resources/grounding/immigration/immigration-grounding.txt`

If grounding content changes and you want a clean rebuild, delete:

- `data/vector-store.json`

Then restart backend to regenerate embeddings.

## 9. Testing

Backend:

```powershell
.\mvnw.cmd test
```

Manual API smoke:

```powershell
powershell -ExecutionPolicy Bypass -File .\smoke-api.ps1
```

Frontend:

```powershell
cd ..\pathboot-ui
npm test
```

## 10. Key Configuration References

- `src/main/resources/application.yml`
- `../pathboot-ui/README.md` (if frontend repo is present)
- `start-local.ps1`
- `stop-local.ps1`
- `smoke-api.ps1`
- `../pathboot-ui/vite.config.ts`

## 11. Related Documents

- `README.md` - quick start and API usage
- `PATHBOOT_DOCS.md` - documentation index
- `TECHNOLOGY_CHOICES.md` - rationale by technology
- `ADDING_NEW_DOMAIN.md` - domain extension playbook
