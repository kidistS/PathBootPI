# PathBoot PI – Step-by-Step Technical Documentation

## 1. Project Goal

PathBoot PI is a multilingual domain Q&A system designed to help immigrants and others
access information about Norwegian Tax, NAV (labour & welfare), and Immigration procedures.
Users may ask questions in English, Amharic, or Norwegian and will receive answers in the
same language they used.

---

## 2. Architecture Overview

The application follows a **layered architecture**:

```
HTTP Request
    │
    ▼
[ChatController]          ← Layer 1: REST API (Spring MVC)
    │
    ▼
[ChatFacadeService]       ← Layer 2: Facade / Orchestrator
    │
    ├─[LanguageDetectionUtil]            detect language (Unicode heuristics)
    ├─[TranslationOrchestrationService]  route to Ollama (Norwegian) or NLLB (Amharic)
    ├─[DomainClassificationService]      classify domain via fast keyword scoring (no LLM call)
    ├─[AgentFactory]  ──────────────     get the right domain agent
    │     └─[TaxAgent / NavAgent / ImmigrationAgent]
    │           ├─[RagGroundingService]  retrieve top-3 relevant chunks (SimpleVectorStore)
    │           └─[ChatClient (Ollama)]  generate answer with injected RAG context
    ├─[AsyncPersistenceService (@Async)] persist to SQLite (non-blocking)
    └─[UserSessionManager]               update session memory + SQLite cold tier
    │
    ▼
[ChatResponse]            return answer in original language
```

---

## 3. Technology Choices & Rationale

> **Full details with rationale for every library** are in
> [`TECHNOLOGY_CHOICES.md`](./TECHNOLOGY_CHOICES.md).

### Java 17 + Spring Boot 3.4.5
Spring Boot 3.x requires Java 17+ (Jakarta EE). It provides:
- Auto-configured beans (data source, JPA, web MVC, async executor, cache)
- Spring AI integration for LLM calls and RAG vector store
- Production-grade embedded server (Tomcat)

### Spring AI 1.0.5
Provides the `ChatClient` abstraction over Ollama, the `SimpleVectorStore` for RAG, and
the `EmbeddingModel` for `nomic-embed-text`. Integrates natively with Spring `@Cacheable`
and `@Async`.

### Ollama + Mistral (local LLM)
Ollama runs LLMs locally with no API key or data leaving the machine. Mistral is chosen
because it handles both English and Norwegian, fits in 8 GB VRAM, and can be pinned
permanently in RAM (`keep-alive: "-1m"`) to eliminate cold-start latency.

### nomic-embed-text + SimpleVectorStore (RAG)
`nomic-embed-text` (274 MB, via Ollama) embeds grounding file chunks into a
`SimpleVectorStore`. On first run embeddings are computed and saved to
`./data/vector-store.json`; subsequent restarts load in milliseconds. RAG injects only
the top-3 relevant chunks per query, keeping prompts inside the 1 024-token context window.

### NLLB-200-Distilled-600M (Amharic translation)
The Meta NLLB model provides high-quality translation for low-resource languages including
Amharic (amh_Ethi). A lightweight Flask sidecar (`nllb_server.py`) wraps the HuggingFace
Transformers pipeline and exposes a simple JSON REST endpoint at `localhost:5000`.

### SQLite (persistent database)
SQLite stores all user interactions and sessions as a single file (`./data/pathboot-data.db`).
WAL journal mode allows concurrent reads; HikariCP pool size is set to 1 to serialise
writes and eliminate `SQLITE_BUSY` errors. H2 is used **only for automated tests**.

### Caffeine (in-memory cache)
LLM answers are cached for 60 minutes (max 500 entries) keyed by domain + language + question.
Identical questions get instant responses without an extra Ollama call.

### Log4j2
Log4j2 provides async appenders, rolling file policies, and finer-grained configuration
compared to Logback (Spring's default is explicitly excluded from all starters).

---

## 4. Request Processing Pipeline (Step by Step)

**Example input:** "Jak mam złożyć PIT?" → Actually: "How do I pay taxes?" in Norwegian: "Hvordan betaler jeg skatt?"

**Step 1 – Language Detection (`LanguageDetectionUtil`)**
- Scans Unicode code points.
- Ethiopic range (U+1200–U+137F) → AMHARIC
- Characters æ/ø/å → NORWEGIAN
- Otherwise → ENGLISH

**Step 2 – Translate to English (`TranslationOrchestrationService`)**
- AMHARIC → ENGLISH: HTTP POST to NLLB server (`nllb_server.py`)
- NORWEGIAN → ENGLISH: Ollama prompt `"Translate the following Norwegian text to English..."`
- ENGLISH: no-op

**Step 3 – Classify Domain (`DomainClassificationService`)**
- Fast keyword scoring — **no LLM call** needed.
- Counts keyword hits per domain (TAX / NAV / IMMIGRATION) after lowercasing the text.
- Returns the domain with the highest score; tie or zero hits → `UNKNOWN` → fallback domain.

**Step 4 – Route to Agent (`AgentFactory.getAgentForDomain()`)**
- Factory pattern: looks up the registered `DomainAgent` for the detected domain.

**Step 5 – Generate Answer (`AbstractDomainAgent.processUserQuestion()`)**
- Template Method pattern:
  1. `RagGroundingService.findRelevantContext()` — retrieves the top-3 most relevant
     chunks from the pre-embedded `SimpleVectorStore` (filtered by domain metadata).
  2. Build system prompt (domain context + RAG chunks injected).
  3. Call Ollama chat: `chatClient.prompt().system(…).user(question).call().content()`
  4. Result cached via `@Cacheable` (Caffeine, 60 min TTL).

**Step 6 – Translate Answer Back**
- Same as Step 2 but reversed direction.
- ENGLISH → AMHARIC: NLLB server
- ENGLISH → NORWEGIAN: Ollama prompt

**Step 7 – Persist (`AsyncPersistenceService`)**
- Saves `UserInteraction` entity to **SQLite** asynchronously (`@Async`).
- Non-fatal: if persistence fails, the response is still returned.
- Runs on the `pathboot-async-` thread pool (core 2, max 4, queue 500).

**Step 8 – Update Session (`UserSessionManager`)**
- Appends the turn (question + answer) to the in-memory session store.
- Oldest turns are evicted when the cap (20) is reached.

---

## 5. Data Grounding Files

Each domain agent reads a plain-text grounding file from the classpath at first use.
The content is cached forever (Flyweight pattern) so disk I/O is one-time.

| File | Covers |
|------|--------|
| `grounding/tax/tax-grounding.txt` | Income tax rates, deductions, filing deadlines, wealth tax, VAT |
| `grounding/nav/nav-grounding.txt` | Dagpenger, sykepenger, AAP, parental leave, disability benefit |
| `grounding/immigration/immigration-grounding.txt` | Work permits, residence permits, citizenship, asylum |

To update domain knowledge: edit the relevant `.txt` file and restart the application
(or clear the `DataGroundingLoader` cache if you add hot-reload support).

---

## 6. Session Management

Sessions use a **two-tier storage strategy**:

| Tier | Storage | Speed | Purpose |
|------|---------|-------|---------|
| Hot | `ConcurrentHashMap<String, UserSessionData>` | O(1) in-process | Fast per-request access |
| Cold | SQLite via JPA (`UserSessionRepository`) | Disk I/O | Persistence across restarts |

**Lookup order on every request:** memory → DB → create new session.

Each session has:
- A UUID session ID (generated server-side if the client does not provide one)
- A capped list of `SessionTurn` objects (max 20)
- Conversation history serialised as JSON in the `user_sessions` table

**Sessions survive restarts** via the cold tier. When a client reconnects with a known
session ID after a server restart, the session history is restored from SQLite into memory.

To scale horizontally, replace the hot tier with Spring Session backed by Redis:
`spring.session.store-type=redis`.

---

## 7. Design Patterns Quick Reference

### Creational
| Pattern | Class |
|---------|-------|
| Singleton | All `@Component` / `@Service` beans |
| Factory + Multiton | `AgentFactory` – registry of agents keyed by `DomainType` |
| Builder | `ChatResponse`, `UserInteraction` (`@Builder` via Lombok) |

### Structural
| Pattern | Class |
|---------|-------|
| Facade | `ChatFacadeService` |
| Adapter | `OllamaTranslationService`, `NllbTranslationService` |
| Flyweight | `DataGroundingLoader` – shared cached grounding content |

### Behavioural
| Pattern | Class |
|---------|-------|
| Template Method | `AbstractDomainAgent.processUserQuestion()` |
| Strategy | `TranslationOrchestrationService` → picks correct `TranslationService` |
| Chain of Responsibility | Full pipeline in `ChatFacadeService` |

---

## 8. Adding a New Domain

1. Create `grounding/<newdomain>/<newdomain>-grounding.txt` with relevant content.
2. Add `NEW_DOMAIN` to the `DomainType` enum.
3. Add `NEW_DOMAIN_GROUNDING_FILE` and `NEW_DOMAIN_DISPLAY_NAME` constants in `PathBootConstants`.
4. Create `agent/<newdomain>/NewDomainAgent.java` extending `AbstractDomainAgent`.
5. Create `service/domain/NewDomainService.java` implementing `DomainProcessingService`.
6. `AgentFactory.registerAgents()` will automatically pick up the new `@Component` agent if you
   add it to the constructor and `@PostConstruct` method.
7. Write unit tests for the new service.

---

## 9. Adding a New Language

1. Add the new language to the `Language` enum.
2. Add detection logic in `LanguageDetectionUtil.detectLanguage()`.
3. Add NLLB language code in `PathBootConstants` (if NLLB supports it).
4. Implement or extend a `TranslationService` for the new pair.
5. Register the new service in `TranslationOrchestrationService.resolveTranslationService()`.

---

## 10. Environment Variables & Secrets

All configuration lives in `application.yml`. No API keys are required for the default setup.

If you later integrate a cloud LLM (e.g., OpenAI), add:
```yaml
openai:
  api-key: ${OPENAI_API_KEY}   # inject via environment variable
```
And set the environment variable:
```powershell
$env:OPENAI_API_KEY = "sk-..."
```
Never hard-code API keys in source files.

---

## 11. Database Schema

The following tables are auto-created by Hibernate (`ddl-auto: update`):

### `user_interactions`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (PK) | Auto-increment (SQLite rowid) |
| session_id | VARCHAR | Client session UUID |
| original_input | TEXT | Raw user text |
| detected_language | VARCHAR | ENGLISH / AMHARIC / NORWEGIAN |
| translated_input | TEXT | English translation (null if input was already English) |
| detected_domain | VARCHAR | TAX / NAV / IMMIGRATION |
| generated_response_english | TEXT | English answer from agent |
| final_response | TEXT | Response in user's original language |
| created_at | TIMESTAMP | Auto-set by Hibernate |

### `user_sessions`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (PK) | Auto-increment |
| session_id | VARCHAR (unique) | UUID |
| conversation_history_json | TEXT | JSON array of `SessionTurn` |
| last_updated | TIMESTAMP | Updated on every turn |

**Database file:** `./data/pathboot-data.db`  
**WAL mode** is enabled for concurrent reads.  
No H2 console is available in production — use the `sqlite3` CLI or DB Browser for SQLite.

---

## 12. Known Limitations & Future Improvements

| Limitation | Suggested Improvement |
|------------|-----------------------|
| Language detection is heuristic | Integrate a proper language-ID library (e.g., `lingua`) |
| Sessions lost if cold-tier SQLite is deleted | Add Spring Session + Redis for distributed persistence |
| RAG uses `SimpleVectorStore` (in-process JSON) | Migrate to PGVector or Qdrant for large-scale deployments |
| No authentication | Add Spring Security + JWT for multi-tenant deployments |
| NLLB slow on CPU | Add GPU support or replace with a faster model |
| No rate limiting | Add Bucket4j or Spring Cloud Gateway rate limiting |
| Domain classification is keyword-only | Add an ML classifier (e.g., fine-tuned BERT) for edge cases |

