# PathBoot PI – Documentation Index

> Last updated: 2026-04-27  
> This file is the **single entry point** for all PathBoot PI documentation.  
> Each document below is self-contained and covers a distinct concern.

---

## Documents

### 1. [`DOCUMENTATION.md`](./DOCUMENTATION.md) — Technical Documentation
Step-by-step guide to how the system is built and how it works.

| Section | What it covers |
|---------|---------------|
| Architecture Overview | Layered call tree from HTTP request to response |
| Request Processing Pipeline | All 8 steps: detect → translate → classify → RAG → LLM → back-translate → persist → session |
| Data Grounding Files | Tax, NAV, immigration knowledge base files |
| Session Management | Two-tier hot/cold session strategy |
| Design Patterns | Creational, structural, and behavioural patterns used |
| Adding a New Domain | Step-by-step extension guide |
| Adding a New Language | Step-by-step extension guide |
| Database Schema | `user_interactions` and `user_sessions` table definitions |
| Known Limitations | Current constraints and suggested future improvements |

---

### 2. [`TECHNOLOGY_CHOICES.md`](./TECHNOLOGY_CHOICES.md) — Technology Choices & Rationale
Explains every library, framework, and tool used — and **why** each was chosen over alternatives.

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
| §9 | Caffeine in-memory cache |
| §10 | Spring @Async + ThreadPoolTaskExecutor |
| §11 | ConcurrentHashMap + SQLite two-tier session |
| §12 | Keyword scoring domain classifier (no LLM) |
| §13–20 | MapStruct, Lombok, Jackson, SpringDoc, Log4j2, Actuator, JUnit 5, Maven |
| §21 | Design patterns summary |

---

### 3. [`diagrams/pathboot-architecture.d2`](./diagrams/pathboot-architecture.d2) — Architecture Diagram (D2)
Visual architecture diagram showing all components and their connections.

- **Render online:** paste the file content at https://play.d2lang.com
- **Render locally:** `d2 diagrams/pathboot-architecture.d2 diagrams/pathboot-architecture.svg`
- **Sequence diagram:** [`diagrams/pathboot-sequence.d2`](./diagrams/pathboot-sequence.d2)
- **ASCII sequence diagram:** [`diagrams/pathboot-sequence.txt`](./diagrams/pathboot-sequence.txt)

---

## Quick Reference

| I want to… | Go to |
|-----------|-------|
| Understand the request flow end-to-end | `DOCUMENTATION.md` §4 |
| Understand why a technology was chosen | `TECHNOLOGY_CHOICES.md` |
| See the architecture visually | `diagrams/pathboot-architecture.d2` |
| Add a new domain (e.g., Housing) | `DOCUMENTATION.md` §8 |
| Add a new language | `DOCUMENTATION.md` §9 |
| Check the database schema | `DOCUMENTATION.md` §11 |
| Read the API reference | `README.md` – API Usage section |
| Run the application | `README.md` – Setup & Run section |
