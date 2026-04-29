# PathBoot PI – Documentation Index

> Last updated: 2026-04-29
> This file is the **single entry point** for all PathBoot PI documentation.
> Each document below is self-contained and covers a distinct concern.

---

## Documents

### 1. [`README.md`](./README.md) — Quick Start & API Reference
Getting started guide, tech stack, setup instructions, authentication flow, and API usage examples.

| Section | What it covers |
|---------|---------------|
| Overview | Language/domain matrix |
| Tech Stack | All libraries + versions including Spring Security |
| Setup & Run | 4-step startup (Ollama → NLLB → Spring Boot → Swagger) |
| **Authentication** | Login endpoint, per-user API key mapping, Swagger authorize flow, default credentials |
| API Usage | All endpoints with curl/PowerShell examples including `X-API-Key` header |
| Configuration | Full `application.yml` reference table (incl. `api.security.user-keys`) |
| Design Patterns | Updated: Factory now uses auto-discovery via `List<DomainAgent>` |
| Running Tests | Test suite summary (154 tests, 0 failures) |
| Troubleshooting | Common errors and fixes |

---

### 2. [`DOCUMENTATION.md`](./DOCUMENTATION.md) — Step-by-Step Technical Documentation
How the system is built and how every component works.

| Section | What it covers |
|---------|---------------|
| Architecture Overview | Layered call tree: `ApiKeyAuthFilter` → `AuthController` → `ChatController` → pipeline |
| Request Processing Pipeline | All 8 steps; Step 4 updated: AgentFactory auto-discovers agents via `List<DomainAgent>` |
| **Security (§5)** | API key model, public vs protected paths, per-user key mapping (`user-keys`), key classes |
| Data Grounding Files | Tax, NAV, immigration knowledge base files |
| Session Management | Two-tier hot/cold; UUID validation; `ArrayDeque` history eviction (O(1)) |
| Design Patterns | Creational, structural, and behavioural patterns — factory updated |
| Adding a New Domain | Summary (→ `ADDING_NEW_DOMAIN.md`) — now **6 steps**, AgentFactory not touched |
| Adding a New Language | 6-step guide including `CLARIFICATION_MESSAGE_*` constant |
| Database Schema | `user_interactions` and `user_sessions` tables |
| Known Limitations | Added: credential injection via env vars, rate limiting |

---

### 3. [`TECHNOLOGY_CHOICES.md`](./TECHNOLOGY_CHOICES.md) — Technology Choices & Rationale
Every library and framework — why it was chosen over the alternatives.

| Section | Technology covered |
|---------|--------------------|
| §1 | Java 17 |
| §2 | Spring Boot 3.4.5 |
| §3 | Ollama + Mistral (local LLM) |
| §4 | Spring AI 1.0.5 |
| §5 | nomic-embed-text + SimpleVectorStore (RAG) |
| §6 | NLLB-200-Distilled-600M (Amharic translation) |
| §7 | SQLite + Hibernate Community Dialects |
| §8 | HikariCP (pool-size=1) |
| **§9** | **Caffeine — SHA-256 cache keys; programmatic config (no YAML spec)** |
| **§10** | **Spring @Async — CallerRunsPolicy added to prevent silent task drops** |
| **§11** | **ConcurrentHashMap + SQLite — UUID validation; ArrayDeque eviction (O(1))** |
| §12 | Keyword scoring domain classifier (no LLM) |
| §13–20 | MapStruct, Lombok, Jackson, SpringDoc, Log4j2, Actuator, JUnit 5, Maven |
| §21 | Spring Security – API key authentication |
| **§22** | **Design patterns — Factory updated: auto-discovery via `List<DomainAgent>`** |

---

### 4. [`ADDING_NEW_DOMAIN.md`](./ADDING_NEW_DOMAIN.md) — How to Add a New Domain
Complete guide for extending PathBoot PI with a new domain.

| Section | What it covers |
|---------|---------------|
| **Overview** | **6 steps** — AgentFactory is NOT touched (auto-discovery) |
| Step 1–2 | `DomainType` enum + `PathBootConstants` keywords and constants |
| Step 3 | Knowledge grounding file format and RAG re-embed instruction |
| Step 4 | New agent class — only 3 method overrides; `@Component` is enough for auto-registration |
| ~~Step 5~~ | ~~AgentFactory wiring~~ — **eliminated by refactoring** |
| Step 5 | Extending `DomainClassificationService` keyword scorer |
| Step 6 | Unit tests for `AgentFactoryTest` and `DomainClassificationServiceTest` |
| Files NOT Touched | `AgentFactory` added to the list |
| Checklist | 7-item completion checklist (step 5 = classifier, not factory) |

---

## Quick Reference

| I want to… | Go to |
|-----------|-------|
| Start the application | `README.md` – Setup & Run |
| Authenticate and get an API key | `README.md` – Authentication |
| Use the API with curl/PowerShell | `README.md` – API Usage |
| Understand the request flow end-to-end | `DOCUMENTATION.md` §4 |
| Understand the security model | `DOCUMENTATION.md` §5 |
| Understand why a technology was chosen | `TECHNOLOGY_CHOICES.md` |
| Add a new domain (e.g., Education) | `ADDING_NEW_DOMAIN.md` |
| Add a new language | `DOCUMENTATION.md` §10 |
| Check the database schema | `DOCUMENTATION.md` §12 |
| See current test coverage | `README.md` – Running Tests |
