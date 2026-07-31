# PathBoot PI - Skill Matrix

> Last updated: 2026-07-31
> Purpose: define the core skills needed to develop, run, test, and maintain PathBoot PI.

## 1) Scope

This document is for:
- new contributors onboarding to `PathBootPI`
- maintainers reviewing readiness and growth areas
- leads assigning work by skill level

## 2) Proficiency Scale

- **L1 (Aware):** can follow guided steps
- **L2 (Working):** can complete standard tasks independently
- **L3 (Strong):** can debug non-trivial issues and improve design
- **L4 (Expert):** can lead architecture, reliability, and mentoring

## 3) Required Skill Areas

| Area | What matters in this repo | Target |
|---|---|---|
| Java / Spring Boot | Controllers, services, config, profiles, Actuator | L2-L3 |
| API Security | API key flow (`/api/v1/auth/login`, `X-API-Key`) | L2 |
| AI Integration | Ollama setup, model checks, prompt flow, RAG behavior | L2-L3 |
| Translation Service | NLLB container lifecycle and endpoint validation | L2 |
| Data Layer | SQLite + JPA/Hibernate usage and constraints | L2 |
| Caching + Async | Caffeine cache and async persistence behavior | L2 |
| Testing | Unit tests with JUnit/Mockito; smoke test usage | L2-L3 |
| Tooling | Maven wrapper, PowerShell scripts, Docker basics | L2 |
| Observability | Read logs, health checks, startup diagnostics | L2-L3 |
| Documentation | Keep README/docs aligned with actual procedures | L2 |

## 4) Role-Oriented Expectations

### Backend Developer
- Build and refactor Spring components safely.
- Debug startup/runtime issues from logs and health endpoints.
- Add/maintain tests for service and controller behavior.
- Keep API and config docs accurate.

### Platform/DevOps Contributor
- Maintain local startup scripts (`start-local.ps1`, `stop-local.ps1`).
- Keep Docker-based NLLB workflow stable.
- Improve reliability checks and troubleshooting paths.

### QA / Test Contributor
- Run and extend `smoke-api.ps1` scenarios.
- Validate auth flow, protected routes, and error handling.
- Confirm docs match real manual test procedures.

## 5) Task-to-Skill Mapping

| Typical Task | Primary Skills |
|---|---|
| Add a new endpoint | Spring Boot, validation, security, testing, docs |
| Fix auth bug | API security, controller/service debugging, tests |
| Improve answer quality | prompt/RAG understanding, grounding files, testing |
| Fix startup issue | PowerShell, Docker, logs, health checks |
| Add a domain | classification logic, agent structure, grounding, tests |
| Update docs/procedures | operational understanding, reproducible commands |

## 6) Readiness Checklist (Contributor)

- [ ] Can start local stack with `start-local.ps1`
- [ ] Can stop local stack with `stop-local.ps1`
- [ ] Can run API smoke test via `smoke-api.ps1`
- [ ] Can manually test login + protected chat endpoint
- [ ] Can run backend tests with `./mvnw.cmd test`
- [ ] Can diagnose failures using backend and container logs
- [ ] Can update docs when process or scripts change

## 7) Suggested 30-60-90 Day Growth Plan

### First 30 days
- Learn run/test flow and script behavior.
- Fix one bug with test coverage.
- Perform one docs update tied to real verification.

### By 60 days
- Deliver a feature touching controller + service + test layers.
- Debug one local runtime issue end-to-end.
- Improve or extend smoke test coverage.

### By 90 days
- Lead a medium change (domain logic, security refinement, or reliability upgrade).
- Mentor onboarding for a new contributor.
- Propose one measurable engineering/process improvement.

## 8) Evidence of Proficiency

Use this list during reviews:
- merged PRs with tests
- incident/bug fixes with clear root-cause notes
- docs updates that remove ambiguity or stale commands
- demonstrated ability to run and verify local stack independently

