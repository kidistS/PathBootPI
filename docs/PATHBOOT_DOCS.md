# PathBoot PI - Documentation Index

> Last updated: 2026-07-31

Use this file as the primary navigation page for all project documentation.

## Core documents

- `README.md`: quick start, run modes, auth, API examples, troubleshooting
- `DOCUMENTATION.md`: architecture, pipeline, service interaction, runtime topology
- `TECHNOLOGY_CHOICES.md`: why each framework/library was selected
- `ADDING_NEW_DOMAIN.md`: step-by-step domain extension workflow
- `../pathboot-ui/README.md`: frontend setup, environment, tests

## 2.1 System Architecture (Container-Aware)

Current runtime architecture is local-first and supports containerized dependencies:

- Backend (`pathboot-pi`): Spring Boot API on `http://localhost:8080`
- Ollama runtime: model server on `http://localhost:11434`
- NLLB translation service: Docker container `pathboot-nllb` on `http://localhost:5000`
- Persistence: SQLite + vector store files under `data/`

Container-aware interaction flow:

1. Client (Swagger/UI) calls backend endpoints.
2. Backend calls Ollama for chat + embeddings.
3. Backend calls NLLB container for Amharic translation.
4. Backend persists sessions/interactions in SQLite and RAG vectors on disk.

Architecture source file:

- `pathboot-architecture.d2` (D2 diagram source)

## Run modes

### Manual local run

1. Start Ollama
2. Start NLLB
3. Start backend (`pathboot-pi`)
4. Start frontend (`pathboot-ui`)

### Automated local run

From this repository root (`PathBootPI/`):

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

Stop:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-local.ps1
```

Smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File .\smoke-api.ps1
```

### Docker Compose run

If your broader workspace contains compose files, use those workspace-level docs.
The default and validated flow in this repository is script-based local startup.

## Interaction quick map

- `pathboot-ui` sends requests to `pathboot-pi`
- `pathboot-pi` calls Ollama (`11434`) for LLM and embeddings
- `pathboot-pi` calls NLLB (`5000`) for Amharic translation
- `pathboot-pi` persists data and vector store in `pathboot-pi/data/`

## Where to edit what

- Update run commands and first-time setup: `README.md`
- Update architecture flow and component behavior: `DOCUMENTATION.md`
- Update design rationale and tradeoffs: `TECHNOLOGY_CHOICES.md`
- Update extension workflow: `ADDING_NEW_DOMAIN.md`
- Update UI-only behavior and scripts: `../pathboot-ui/README.md`
