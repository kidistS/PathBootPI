# PathBoot PI – Step-by-Step Technical Documentation

> Last updated: 2026-04-29

## 1. Project Goal

PathBoot PI is a multilingual domain Q&A system designed to help immigrants and others
access information about Norwegian Tax, NAV (labour & welfare), and Immigration procedures.
Users may ask questions in English, Amharic, or Norwegian and will receive answers in the
same language they used. Access is secured via API key authentication.

---

## 2. Architecture Overview

The application follows a **layered architecture**:

```
HTTP Request
    │
    ▼
[ApiKeyAuthFilter]        ← Security: validates X-API-Key header (public paths skipped)
    │
    ▼
[AuthController]          ← POST /api/v1/auth/login (always public — returns API key)
    │
    ▼
[ChatController]          ← Layer 1: REST API (Spring MVC)
    │
    ▼
[ChatFacadeService]       ← Layer 2: Facade / Orchestrator
    │
    ├─[LanguageDetectionUtil]            detect language (Unicode script + Norwegian keywords)
    ├─[TranslationOrchestrationService]  route to Ollama (Norwegian) or NLLB (Amharic)
    ├─[DomainClassificationService]      classify domain via fast keyword scoring (no LLM call)
    │     └── UNKNOWN → short-circuit: return static clarification message in user's language
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
Spring Boot 3.x requires Java 17+ (Jakarta EE). Provides auto-configured beans, Spring AI
integration, production-grade embedded Tomcat, and Spring Security.

### Spring Security 3.4.5
Provides the `SecurityFilterChain` and `OncePerRequestFilter` infrastructure for API key
enforcement. Configured as fully stateless (no sessions, no CSRF). Public paths (Swagger,
actuator, login) bypass the filter via `shouldNotFilter()`.

### Spring AI 1.0.5
Provides `ChatClient` over Ollama, `SimpleVectorStore` for RAG, and `EmbeddingModel`
for `nomic-embed-text`.

### Ollama + Mistral (local LLM)
Runs locally with no data leaving the machine. Handles English and Norwegian. Pinned
permanently in RAM (`keep-alive: "-1m"`) to eliminate cold-start latency.

### nomic-embed-text + SimpleVectorStore (RAG)
Embeds grounding file chunks; saves to `./data/vector-store.json` on first run.
RAG injects the top-3 relevant chunks per query inside the 1 024-token context window.

### NLLB-200-Distilled-600M (Amharic translation)
Meta NLLB model for Amharic (amh_Ethi). Lightweight Flask sidecar (`nllb_server.py`)
exposes a JSON REST endpoint at `localhost:5000`.

### SQLite + Caffeine
SQLite stores all interactions and sessions as a single file. WAL mode + pool-size=1
prevents `SQLITE_BUSY`. H2 is used **only for automated tests**.
Caffeine caches LLM answers for 60 minutes (max 500 entries).

---

## 4. Request Processing Pipeline (Step by Step)

**Step 1 – Language Detection (`LanguageDetectionUtil`)**
- Scans Unicode code points for Ethiopic script (U+1200–U+137F) → AMHARIC
- Checks for Norwegian special characters æ/ø/å → NORWEGIAN
- **Also checks for uniquely Norwegian indicator words** (e.g. `hva`, `ikke`, `dagpenger`,
  `innvandring`, `skatt`) so Norwegian sentences without special characters are correctly detected.
- Otherwise → ENGLISH

**Step 2 – Translate to English (`TranslationOrchestrationService`)**
- AMHARIC → ENGLISH: HTTP POST to NLLB server (`nllb_server.py`)
- NORWEGIAN → pass-through: agent receives Norwegian text directly; Mistral is prompted to answer in Norwegian
- ENGLISH: no-op

**Step 3 – Classify Domain (`DomainClassificationService`)**
- Fast keyword scoring — **no LLM call** needed.
- Counts keyword hits per domain (TAX / NAV / IMMIGRATION) after lowercasing the text.
- Returns the domain with the highest score; tie or zero hits → `UNKNOWN`.
- **If `UNKNOWN`:** short-circuit immediately — no agent is called. A static pre-written
  clarification message is returned in the user's own language (English / Norwegian / Amharic).
  This saves one full LLM call and one translation call.

**Step 4 – Route to Agent (`AgentFactory.getAgentForDomain()`)**
- `AgentFactory` injects `List<DomainAgent>` from the Spring context and auto-registers
  every `@Component` that implements `DomainAgent` — keyed by `getDomainType()`.
- No manual wiring is needed; adding a new domain only requires a new `@Component` class.
- Only reached when domain is TAX, NAV, or IMMIGRATION (never UNKNOWN).

**Step 5 – Generate Answer (`AbstractDomainAgent.processUserQuestion()`)**
- Template Method pattern:
  1. `RagGroundingService.findRelevantContext()` — top-3 most relevant chunks
  2. Build system prompt (domain + RAG chunks + language hint)
  3. Call Ollama chat
  4. Result cached via `@Cacheable` (Caffeine, 60 min TTL; key uses SHA-256 of the question
     so cache keys are always 64 chars regardless of question length)

**Step 6 – Translate Answer Back**
- ENGLISH → AMHARIC: NLLB server
- NORWEGIAN: answer already in Norwegian (see Step 2)
- UNKNOWN: static string already in the correct language — no translation call

**Step 7 – Persist (`AsyncPersistenceService`)**
- Saves `UserInteraction` entity to SQLite asynchronously (`@Async`).
- Called even for UNKNOWN domain — the interaction is recorded with `DomainType.UNKNOWN`.

**Step 8 – Update Session (`UserSessionManager`)**
- Appends the turn to the in-memory session store (cap: 20 turns).
- Called even for UNKNOWN domain.

---

## 5. Security

### Authentication Model: API Key

All `/api/v1/chat` endpoints require a valid `X-API-Key` request header.
Authentication is stateless — no cookies, no sessions, no JWT.

| Path | Protected? |
|------|-----------|
| `/swagger-ui/**`, `/webjars/**`, `/api-docs/**` | ✅ Public |
| `/actuator/health`, `/actuator/info` | ✅ Public |
| `/api/v1/auth/login` | ✅ Public |
| `/api/v1/chat/**` | 🔒 Requires X-API-Key |

### Getting a Key

```
POST /api/v1/auth/login
Body: { "username": "admin", "password": "pathboot-admin-2026" }
→ Response: { "apiKey": "pathboot-dev-key-2026", ... }
```

### Configuration (no code change needed)

```yaml
api:
  security:
    enabled: true               # set false to disable for local dev
    api-keys:
      - "pathboot-dev-key-2026"
      - "pathboot-local-key-001"
    users:
      admin: "pathboot-admin-2026"
      user: "pathboot-user-2026"
    # Each user receives their own key on login.
    # Falls back to the first api-keys entry if a username is not listed here.
    user-keys:
      admin: "pathboot-dev-key-2026"
      user: "pathboot-local-key-001"
```

### Key Classes

| Class | Role |
|-------|------|
| `ApiKeyAuthFilter` | Reads `X-API-Key` header; returns 401 JSON if missing/invalid; skips public paths via `shouldNotFilter()` |
| `SecurityConfig` | Defines `SecurityFilterChain`; registers `ApiKeyAuthFilter` before `UsernamePasswordAuthenticationFilter` |
| `ApiSecurityProperties` | `@ConfigurationProperties(prefix="api.security")` — binds YAML list and map correctly |
| `AuthController` | `POST /api/v1/auth/login` — validates credentials, returns API key + `Location: /api/v1/chat` |

---

## 6. Data Grounding Files

Each domain agent reads a plain-text grounding file from the classpath at first use.

| File | Covers |
|------|--------|
| `grounding/tax/tax-grounding.txt` | Income tax rates, deductions, filing deadlines, wealth tax, VAT |
| `grounding/nav/nav-grounding.txt` | Dagpenger, sykepenger, AAP, parental leave, disability benefit |
| `grounding/immigration/immigration-grounding.txt` | Work permits, residence permits, citizenship, asylum |

To update domain knowledge: edit the relevant `.txt` file and restart the application.

---

## 7. Session Management

Sessions use a **two-tier storage strategy**:

| Tier | Storage | Speed | Purpose |
|------|---------|-------|---------|
| Hot | `ConcurrentHashMap<String, UserSessionData>` | O(1) in-process | Fast per-request access |
| Cold | SQLite via JPA (`UserSessionRepository`) | Disk I/O | Persistence across restarts |

Each session has a UUID ID (client-supplied IDs are validated as UUID format; invalid
strings result in a new generated UUID to prevent session-ID pollution), a capped history
of `SessionTurn` objects (max 20, stored in an `ArrayDeque` for O(1) eviction), and
conversation history serialised as JSON in the `user_sessions` table. Session is updated
even on UNKNOWN domain responses.

---

## 8. Design Patterns Quick Reference

### Creational
| Pattern | Class |
|---------|-------|
| Singleton | All `@Component` / `@Service` beans |
| Factory + Auto-discovery | `AgentFactory` – injects `List<DomainAgent>`; auto-registers every `@Component` agent via `getDomainType()` key |
| Builder | `ChatResponse`, `AuthResponse`, `UserInteraction` (`@Builder` via Lombok) |

### Structural
| Pattern | Class |
|---------|-------|
| Facade | `ChatFacadeService` |
| Adapter | `OllamaTranslationService`, `NllbTranslationService` |
| Flyweight | `DataGroundingLoader` – shared cached grounding content |
| Filter | `ApiKeyAuthFilter` – `OncePerRequestFilter` |

### Behavioural
| Pattern | Class |
|---------|-------|
| Template Method | `AbstractDomainAgent.processUserQuestion()` |
| Strategy | `TranslationOrchestrationService` → picks correct `TranslationService` |
| Chain of Responsibility | Full pipeline in `ChatFacadeService` |

---

## 9. Adding a New Domain

> 📄 Full step-by-step guide: **[ADDING_NEW_DOMAIN.md](./ADDING_NEW_DOMAIN.md)**

**Summary — 6 files to change, `AgentFactory` is NOT touched:**

| Step | File |
|------|------|
| 1 | `enums/DomainType.java` — add enum constant |
| 2 | `util/PathBootConstants.java` — add keywords array + 2 constants |
| 3 | `grounding/<domain>/<domain>-grounding.txt` — new knowledge file |
| 4 | `agent/<domain>/<Domain>Agent.java` — new `@Component`, 3 method overrides |
| 5 | `service/classification/DomainClassificationService.java` — add score to classifier |
| 6 | Tests — `AgentFactoryTest` + `DomainClassificationServiceTest` |

> ✅ `AgentFactory` automatically discovers the new agent via `List<DomainAgent>` injection —
> no factory wiring is needed for new domains.

> ⚠️ Delete `data/vector-store.json` before the first restart so RAG re-embeds all domains.

---

## 10. Adding a New Language

1. Add the new language to the `Language` enum.
2. Add detection logic in `LanguageDetectionUtil.detectLanguage()` (script check or keyword set).
3. Add NLLB language code in `PathBootConstants` (if NLLB supports it).
4. Implement or extend a `TranslationService` for the new pair.
5. Register the new service in `TranslationOrchestrationService.resolveTranslationService()`.
6. Add a `CLARIFICATION_MESSAGE_<LANGUAGE>` constant in `PathBootConstants` for the UNKNOWN domain path.

---

## 11. Environment Variables & Secrets

```yaml
api:
  security:
    api-keys:
      - "${PATHBOOT_API_KEY_1:pathboot-dev-key-2026}"   # env var with fallback
    users:
      admin: "${PATHBOOT_ADMIN_PASSWORD:pathboot-admin-2026}"
```

```powershell
$env:PATHBOOT_API_KEY_1 = "my-production-key"
$env:PATHBOOT_ADMIN_PASSWORD = "strong-password"
.\mvnw.cmd spring-boot:run
```

Never hard-code credentials in source files. In production use a secrets manager.

---

## 12. Database Schema

### `user_interactions`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (PK) | Auto-increment |
| session_id | VARCHAR | Client session UUID |
| original_input | TEXT | Raw user text |
| detected_language | VARCHAR | ENGLISH / AMHARIC / NORWEGIAN |
| translated_input | TEXT | English translation (null if already English) |
| detected_domain | VARCHAR | TAX / NAV / IMMIGRATION / **UNKNOWN** |
| generated_response_english | TEXT | English agent response (UNKNOWN: static English clarification) |
| final_response | TEXT | Response in user's original language |
| created_at | TIMESTAMP | Auto-set by Hibernate |

### `user_sessions`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT (PK) | Auto-increment |
| session_id | VARCHAR (unique) | UUID |
| conversation_history_json | TEXT | JSON array of `SessionTurn` |
| last_updated | TIMESTAMP | Updated on every turn |

---

## 13. Known Limitations & Future Improvements

| Limitation | Suggested Improvement |
|------------|-----------------------|
| Language detection uses heuristics | Integrate a proper language-ID library (e.g., `lingua`) |
| Sessions lost if SQLite is deleted | Add Spring Session + Redis for distributed persistence |
| RAG uses `SimpleVectorStore` (in-process JSON) | Migrate to PGVector or Qdrant for large-scale deployments |
| API key auth (no per-user identity beyond key) | Add JWT for fine-grained roles and per-user audit trails |
| NLLB slow on CPU | Add GPU support or replace with a faster model |
| No rate limiting | Add Bucket4j or Spring Cloud Gateway rate limiting |
| Domain classification is keyword-only | Add an ML classifier (e.g., fine-tuned BERT) for edge cases |
| Credentials hardcoded in `application.yml` | Inject via `${ENV_VAR}` or use Spring Cloud Config / Vault |
