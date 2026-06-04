# PathBoot PI - Documentation Index

> Last updated: 2026-05-29

Use this file as the primary navigation page for all project documentation.

## Core documents

- `README.md`: quick start, run modes, auth, API examples, troubleshooting
- `DOCUMENTATION.md`: architecture, pipeline, service interaction, runtime topology
- `TECHNOLOGY_CHOICES.md`: why each framework/library was selected
- `ADDING_NEW_DOMAIN.md`: step-by-step domain extension workflow
- `../pathboot-ui/README.md`: frontend setup, environment, tests

## Run modes

### Manual local run

1. Start Ollama
2. Start NLLB
3. Start backend (`pathboot-pi`)
4. Start frontend (`pathboot-ui`)

### Automated local run

From workspace root (`pathboot/`):

```powershell
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

Stop:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-all.ps1
```

### Docker Compose run

From workspace root (`pathboot/`):

```powershell
docker compose up --build
```

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
