# PathBoot PI – Technology Choices & Rationale

> **Audience:** developers joining or maintaining the project.  
> **Last updated:** 2026-04-26

This document explains **every technology, library, and framework** used in PathBoot PI,
and—critically—**why** each was chosen over the alternatives.

---

## Table of Contents

1. [Runtime Platform – Java 17](#1-runtime-platform--java-17)
2. [Framework – Spring Boot 3.4.5](#2-framework--spring-boot-345)
3. [LLM Runtime – Ollama + Mistral](#3-llm-runtime--ollama--mistral)
4. [LLM Integration – Spring AI 1.0.5](#4-llm-integration--spring-ai-105)
5. [Embedding & RAG – nomic-embed-text + SimpleVectorStore](#5-embedding--rag--nomic-embed-text--simplevectorstore)
6. [Amharic Translation – NLLB-200-Distilled-600M (Python / Flask)](#6-amharic-translation--nllb-200-distilled-600m-python--flask)
7. [Database – SQLite + Hibernate Community Dialects](#7-database--sqlite--hibernate-community-dialects)
8. [Connection Pool – HikariCP](#8-connection-pool--hikaricp)
9. [In-Memory Cache – Caffeine](#9-in-memory-cache--caffeine)
10. [Async Execution – Spring @Async + ThreadPoolTaskExecutor](#10-async-execution--spring-async--threadpooltaskexecutor)
11. [Session Management – ConcurrentHashMap + SQLite cold tier](#11-session-management--concurrenthashmap--sqlite-cold-tier)
12. [Domain Classification – Keyword Scoring (no LLM)](#12-domain-classification--keyword-scoring-no-llm)
13. [Object Mapping – MapStruct](#13-object-mapping--mapstruct)
14. [Boilerplate Reduction – Lombok](#14-boilerplate-reduction--lombok)
15. [JSON – Jackson](#15-json--jackson)
16. [API Documentation – SpringDoc / Swagger UI](#16-api-documentation--springdoc--swagger-ui)
17. [Logging – Log4j2](#17-logging--log4j2)
18. [Observability – Spring Boot Actuator](#18-observability--spring-boot-actuator)
19. [Testing – JUnit 5 + Mockito + H2](#19-testing--junit-5--mockito--h2)
20. [Build – Maven (Maven Wrapper)](#20-build--maven-maven-wrapper)
21. [Design Patterns Summary](#21-design-patterns-summary)

---

## 1. Runtime Platform – Java 17

| Attribute | Value |
|-----------|-------|
| Version   | Java 17 (LTS) |
| Spring Boot requirement | Spring Boot 3.x requires Java 17+ (Jakarta EE namespace) |

**Why Java 17?**

- **Long-Term Support (LTS):** Java 17 is an LTS release (supported until 2029), making it
  safe for production systems.
- **Modern language features:** Records, sealed classes, pattern matching, and text blocks
  simplify domain models and prompt templates.
- **Spring Boot 3 requirement:** Spring Boot 3.x dropped support for Java 8/11 and mandated
  the Jakarta EE namespace migration, which only works on Java 17+.
- **Performance:** Virtual-thread readiness (Project Loom, available in Java 21) can be
  adopted with a simple flag change when needed.

**Alternatives considered:**
- *Java 21* – would add virtual threads but is not yet LTS-stable in all deployment targets.
- *Kotlin* – excellent Spring support but adds a second language for the team.

---

## 2. Framework – Spring Boot 3.4.5

| Attribute | Value |
|-----------|-------|
| Version   | 3.4.5 |
| Parent POM | `spring-boot-starter-parent` |

**What it provides:**
- Auto-configured beans: embedded Tomcat, DataSource, JPA EntityManagerFactory,
  CacheManager, AsyncTaskExecutor, and more — all driven by `application.yml`.
- Starter POMs that pull in battle-tested dependency stacks and resolve compatible versions.
- Spring AI integration (see §4) that fits naturally into the Spring ecosystem.
- Production-grade embedded web server with Actuator endpoints.

**Why Spring Boot (not Jakarta EE / Quarkus / Micronaut)?**

| Framework | Reason not chosen |
|-----------|-------------------|
| Jakarta EE bare | No auto-configuration; requires manual beans wiring |
| Quarkus | Spring AI does not offer a Quarkus extension |
| Micronaut | Spring AI's `ChatClient` and `VectorStore` abstractions are Spring-native |

Spring Boot was the only framework with first-class Spring AI support, making it the
natural choice once the LLM integration decision was made.

---

## 3. LLM Runtime – Ollama + Mistral

| Component | Details |
|-----------|---------|
| Ollama    | Local model server — `http://localhost:11434` |
| Chat model | `mistral` (7B instruction-tuned) |
| Temperature | `0.1` (deterministic / factual) |
| Context window | `1024` tokens |
| Max output | `200` tokens |
| Keep-alive | `-1m` (never unload from RAM) |

**Why Ollama?**

- **Privacy:** No data leaves the machine. This is critical for immigration and welfare
  (NAV) queries that may contain personal details.
- **No API costs:** All inference is free; no rate limits.
- **Zero-config model management:** `ollama pull mistral` is the only setup step.
- **Model flexibility:** Swap to `llama3`, `gemma`, or any GGUF model with one line change
  in `application.yml`.

**Why Mistral 7B?**

- Multilingual: handles both English and Norwegian in a single model.
- Strong instruction following — the system prompt for domain-constrained answers works
  reliably.
- Fits in 8 GB VRAM / 16 GB RAM, making it viable on developer hardware.
- `keep-alive: "-1m"` pins the model permanently in RAM, eliminating the 5–15 second
  cold start that would otherwise hit every idle request.

**Configuration choices explained:**

| Setting | Value | Reason |
|---------|-------|--------|
| `temperature: 0.1` | Near-zero | Factual Q&A needs no creativity; lower temp = fewer hallucinations |
| `num-predict: 200` | 200 tokens | "2–3 sentences" never exceeds 150 tokens; cap prevents runaway output |
| `num-ctx: 1024` | 1024 | RAG top-3 chunks + question comfortably fits; halves attention cost vs 2048 |
| `keep-alive: "-1m"` | Never unload | Eliminates cold-start latency on every idle-period request |

---

## 4. LLM Integration – Spring AI 1.0.5

| Attribute | Value |
|-----------|-------|
| BOM version | `1.0.5` |
| Dependencies | `spring-ai-starter-model-ollama`, `spring-ai-vector-store` |

**What Spring AI provides:**

- `ChatClient` — a fluent, model-agnostic API:  
  `chatClient.prompt().system(…).user(…).call().content()`
- `OllamaChatModel` — auto-configured from `application.yml`; no boilerplate.
- `SimpleVectorStore` — an in-process vector store with similarity search and metadata
  filtering (used for RAG — see §5).
- `EmbeddingModel` — auto-configured for `nomic-embed-text` via Ollama.

**Why Spring AI instead of raw HTTP / LangChain4j?**

| Library | Issue |
|---------|-------|
| Raw RestTemplate | No abstraction — switching models requires rewriting call code |
| LangChain4j | Does not integrate with Spring's `@Cacheable`, `@Async`, or DI |
| Spring AI | Native Spring beans; seamlessly composes with `@Cacheable`, `@Async`, DI |

---

## 5. Embedding & RAG – nomic-embed-text + SimpleVectorStore

| Component | Details |
|-----------|---------|
| Embedding model | `nomic-embed-text` (via Ollama) |
| Vector store | `SimpleVectorStore` (in-process, JSON-persisted) |
| Persistence | `./data/vector-store.json` |
| Top-K per query | `3` chunks |
| Chunk delimiter | Blank lines / `---` separators |

**What RAG does:**

RAG (Retrieval-Augmented Generation) injects only the *most relevant* paragraphs from
the domain grounding file into each LLM prompt, rather than the entire file. This:

1. Keeps the prompt well within the 1 024-token context window.
2. Reduces hallucination by focusing the LLM's attention on the right facts.
3. Improves answer quality for detailed follow-up questions.

**Two-path initialisation:**

| Path | When | How |
|------|------|-----|
| **Fast (every restart after the first)** | `vector-store.json` exists | Load pre-computed embeddings in milliseconds |
| **Slow (first run only)** | No store file | Embed all grounding files via `nomic-embed-text`, save to disk |

Spring's `SmartInitializingSingleton` guarantees the vector store is populated **before**
Tomcat opens its port — eliminating any race condition.

**Why `SimpleVectorStore` instead of Chroma / PGVector / Qdrant?**

- No external service to install or maintain.
- Persisted to a single JSON file — works out of the box.
- Sufficient for the data volume (< 1 000 chunks across 3 domains).
- Metadata filtering (`domain == 'TAX'`) is supported natively.

**Why `nomic-embed-text`?**

- Tiny model (274 MB); pulled automatically by Ollama on first run.
- High quality on semantic search benchmarks (comparable to OpenAI `text-embedding-ada-002`).
- Runs on the same Ollama daemon already serving Mistral — no extra process.

---

## 6. Amharic Translation – NLLB-200-Distilled-600M (Python / Flask)

| Component | Details |
|-----------|---------|
| Model | `facebook/nllb-200-distilled-600M` (HuggingFace Transformers) |
| Server | Python Flask — `nllb_server.py` |
| Endpoint | `POST http://localhost:5000/translate` |
| Language codes | `amh_Ethi` (Amharic) ↔ `eng_Latn` (English) |
| Norwegian translation | Handled by Ollama (Mistral) — not NLLB |

**Why NLLB for Amharic?**

Amharic is a **low-resource language** that mainstream LLMs (including Mistral 7B) handle
poorly. Meta's NLLB-200 model is specifically trained on 200 languages including Amharic
and produces significantly higher-quality translations.

| Approach | Quality | Why not chosen |
|----------|---------|----------------|
| Mistral 7B for Amharic | Poor | Training data for Amharic is minimal |
| Google Translate API | Good | External API; data privacy concern; API cost |
| NLLB-200-Distilled-600M | Good | Local; purpose-built for low-resource languages |

**Why the distilled 600M variant?**

- The full NLLB-200-3.3B model requires ~7 GB RAM just for the model.
- The 600M distilled variant fits in ~2 GB and is fast even on CPU.
- Quality for the Amharic–English pair is comparable to the larger model.

**Architecture decision — separate Python microservice:**

Calling HuggingFace Transformers from a JVM process would require JNI bridging
(DJL or similar). A lightweight Flask sidecar is simpler, testable independently,
and allows the translation model to be upgraded without touching the Spring Boot build.

The Flask server does a **warm-up inference** at startup so the first real translation
request is not penalised by model JIT compilation.

---

## 7. Database – SQLite + Hibernate Community Dialects

| Attribute | Value |
|-----------|-------|
| Database | SQLite (file: `./data/pathboot-data.db`) |
| JDBC driver | `org.xerial:sqlite-jdbc:3.49.1.0` |
| Hibernate dialect | `org.hibernate.community.dialect.SQLiteDialect` |
| Hibernate DDL | `update` — auto-creates tables on first run |
| Journal mode | WAL (Write-Ahead Log) |
| Busy timeout | 30 000 ms |

**Why SQLite?**

- **Zero administration:** a single file — no server process, no installation.
- **Sufficient concurrency for this workload:** PathBoot PI has low write volume
  (one row per chat turn). SQLite in WAL mode supports concurrent readers with one writer.
- **Portability:** the entire database is one `.db` file; easy to back up or move.
- **Persistent across restarts:** unlike H2 in-memory mode, data survives application restarts.

**Why not PostgreSQL / MySQL?**

This is a local personal-information assistant. There is no need for multi-node replication,
complex transactions, or thousands of concurrent connections. Adding a separate database
server process would increase operational complexity with no benefit.

**WAL mode + busy timeout explained:**

```yaml
connection-init-sql: "PRAGMA journal_mode=WAL; PRAGMA busy_timeout=30000; PRAGMA synchronous=NORMAL;"
```

| pragma | Effect |
|--------|--------|
| `journal_mode=WAL` | Readers never block writers; writers never block readers |
| `busy_timeout=30000` | Wait up to 30 s for the write lock instead of failing with `SQLITE_BUSY` |
| `synchronous=NORMAL` | Good durability / performance balance (not as safe as FULL but far faster) |

**H2 (test scope only):**  
H2 is used only in unit/integration tests. It starts in-memory, giving tests a clean slate
without touching the production SQLite file and without requiring Ollama or the NLLB server.

---

## 8. Connection Pool – HikariCP

| Setting | Value | Reason |
|---------|-------|--------|
| `maximum-pool-size` | `1` | SQLite allows only one writer at a time |
| `minimum-idle` | `1` | Keep one connection warm |

**Why pool size = 1 for SQLite?**

SQLite is a file-level locking database. Only one writer can hold the lock at a time.
Allowing multiple pool connections would cause threads to queue at the SQLite lock level,
producing `SQLITE_BUSY` errors. A pool of 1 serialises all writes through HikariCP's own
queue — which is designed for exactly this pattern.

The `@Async` persistence service (see §10) further ensures that write threads do not
block the HTTP request thread.

---

## 9. In-Memory Cache – Caffeine

| Setting | Value |
|---------|-------|
| Library | `com.github.ben-manes.caffeine:caffeine` |
| Cache name | `domainAnswers` |
| Max entries | 500 |
| TTL | 60 minutes (expire-after-write) |
| Key | `domainType + "_" + language + "_" + question` |

**Why cache LLM answers?**

LLM inference is slow (1–10 seconds). Many users ask the same common questions
(e.g., "What is the income tax rate?"). Caching the answer for 60 minutes means
the second requester gets an instant response.

**Why Caffeine?**

- Fastest in-process cache available for the JVM (outperforms Guava Cache on benchmarks).
- `recordStats()` enables JMX / Actuator metrics with no extra code.
- Integrates with Spring's `@Cacheable` annotation — no changes to business logic.
- TTL-based expiry prevents stale answers from accumulating indefinitely.

**Why not Redis?**

Redis is an excellent choice for distributed caching, but this application runs as a
single process. A network round-trip to Redis would add latency and a new infrastructure
dependency for no benefit.

---

## 10. Async Execution – Spring @Async + ThreadPoolTaskExecutor

| Setting | Value | Reason |
|---------|-------|--------|
| Core pool size | 2 | SQLite one-writer model |
| Max pool size | 4 | Handles burst traffic |
| Queue capacity | 500 | Absorbs spikes without dropping tasks |
| Thread name prefix | `pathboot-async-` | Identifies threads in logs |
| Shutdown grace period | 30 s | Completes in-flight writes on JVM exit |

**Why async persistence?**

Database writes (user interaction logs, session updates) are non-critical to the HTTP
response. Making them asynchronous means:

1. The HTTP response is returned to the user **immediately** after the LLM call completes.
2. Database latency (WAL sync, lock contention) does not add to response time.
3. Burst traffic is absorbed by the queue (500 capacity) without dropping data.

**Why a dedicated `AsyncPersistenceService` bean?**

Spring's `@Async` works through AOP proxies. If `ChatFacadeService` called its own
`@Async` method directly (*self-invocation*), the proxy would be bypassed and the method
would execute synchronously. Delegating to a **separate Spring bean** ensures the proxy
intercepts the call correctly.

---

## 11. Session Management – ConcurrentHashMap + SQLite Cold Tier

**Two-tier strategy:**

| Tier | Storage | Speed | Purpose |
|------|---------|-------|---------|
| Hot | `ConcurrentHashMap<String, UserSessionData>` | O(1) in-process | Fast per-request access |
| Cold | SQLite via JPA (`UserSessionRepository`) | Disk I/O | Persistence across restarts |

**Lookup order on every request:**  
Memory → DB → create new session

**Why not Spring Session (JDBC / Redis)?**

Spring Session is the correct solution for horizontally scaled deployments. For a
single-node local assistant the two-tier approach is simpler, avoids a Redis dependency,
and still survives restarts (cold tier persists to SQLite).

**Thread safety:**

`ConcurrentHashMap` is used instead of `HashMap` so concurrent requests for the same
session do not produce race conditions.

---

## 12. Domain Classification – Keyword Scoring (no LLM)

**Algorithm:**

1. Lowercase the input text.
2. Count keyword hits for each domain (TAX, NAV, IMMIGRATION).
3. Return domain with the highest hit count.
4. Tie or zero hits → `UNKNOWN` (falls back to `DEFAULT_FALLBACK_DOMAIN`).

**Why keyword scoring instead of an LLM call?**

An earlier version used Ollama for classification (one extra round-trip ~1–2 seconds).
Keyword matching achieves similar accuracy at **zero latency** and zero token cost
because the domain vocabulary (tax, income, dagpenger, asylum, etc.) is small and
non-ambiguous in practice.

**Why this matters for performance:**  
The optimised pipeline makes exactly **1 LLM call** per request (the domain answer).
Previously there were 3 calls (classification + optional translation + answer).

---

## 13. Object Mapping – MapStruct

| Attribute | Value |
|-----------|-------|
| Version | 1.5.5.Final |
| Annotation processor order | Lombok first, then MapStruct |

**Why MapStruct?**

- Generates type-safe mapping code at **compile time** — no runtime reflection.
- Mapping errors are caught by the compiler, not at runtime.
- Orders of magnitude faster than ModelMapper or Dozer.
- Integrates cleanly with Lombok: the `lombok-mapstruct-binding` annotation processor
  ensures Lombok generates getters/setters before MapStruct reads them.

---

## 14. Boilerplate Reduction – Lombok

| Annotations used | Purpose |
|-----------------|---------|
| `@Builder` | Fluent builder for `ChatResponse`, `UserInteraction` |
| `@Data` / `@Getter` / `@Setter` | Entity and DTO field accessors |
| `@NoArgsConstructor` / `@AllArgsConstructor` | JPA and Lombok builder requirements |

**Why Lombok?**

Reduces hundreds of lines of repetitive getter/setter/equals/hashCode boilerplate,
keeping model classes focused on structure rather than accessors.

**Note:** Lombok is excluded from the final fat JAR (`spring-boot-maven-plugin` exclude) —
it runs only at compile time, so no runtime dependency is added.

---

## 15. JSON – Jackson

**Provided automatically** by `spring-boot-starter-web`.

Used for:
- REST API request/response serialisation (`ChatRequest`, `ChatResponse`).
- Session history serialisation to JSON strings stored in SQLite
  (`UserSessionManager` → `ObjectMapper.readValue(…, new TypeReference<List<SessionTurn>>() {})`).

The explicit `TypeReference<List<SessionTurn>>()` (not a diamond `<>`) is intentional:
it captures the generic type at runtime and prevents Jackson type-erasure issues on
deserialization.

---

## 16. API Documentation – SpringDoc / Swagger UI

| Attribute | Value |
|-----------|-------|
| Library | `springdoc-openapi-starter-webmvc-ui:2.8.8` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |

**Why SpringDoc?**

- Automatically generates OpenAPI 3.0 specs from Spring MVC annotations — no separate
  YAML file to maintain.
- The Swagger UI allows interactive testing without a separate API client.
- `display-request-duration: true` shows latency per call — useful during development
  to observe LLM response times.

---

## 17. Logging – Log4j2

| Feature | Configuration |
|---------|--------------|
| Config file | `log4j2.xml` (classpath) |
| Rolling policy | Daily + size-based; `.gz` compression |
| Async appenders | Separate file for error logs |
| MDC | Session IDs threaded through log statements |

**Why Log4j2 over Logback (Spring Boot default)?**

| Feature | Log4j2 | Logback |
|---------|--------|---------|
| Async appenders | Built-in `AsyncAppender` | Requires extra brige |
| Rolling policy | Powerful size + time | Basic |
| Performance | Disruptor-based; near-zero GC pressure | Higher allocation rate |
| Config | XML or YAML | XML only |

Log4j2's asynchronous appenders mean logging never blocks the request thread, which is
important when LLM calls are already slow.

**Spring Boot default Logback is explicitly excluded** from all starters
(`spring-boot-starter-logging`) to prevent classpath conflicts with Log4j2.

---

## 18. Observability – Spring Boot Actuator

| Endpoint | Exposed | Purpose |
|----------|---------|---------|
| `/actuator/health` | YES | Liveness / readiness probes |
| `/actuator/info` | YES | Application metadata |
| Others | NO | Not needed for a local assistant |

`show-details: when-authorized` prevents leaking database file paths or disk stats to
unauthenticated callers.

---

## 19. Testing – JUnit 5 + Mockito + H2

| Tool | Scope | Purpose |
|------|-------|---------|
| JUnit 5 (Jupiter) | Test | Test runner |
| Mockito (`mockito-core`, `mockito-junit-jupiter`) | Test | Mock LLM calls, DB calls, translation services |
| H2 in-memory | Test | Replace SQLite; database is wiped between test runs |
| Spring Boot Test | Test | Application context slices (`@SpringBootTest`, `@DataJpaTest`) |

**Why mock the LLM?**

Ollama may not be running in CI. Mocking `ChatClient` allows domain service tests to
verify prompt construction and response handling without a live model server.

**Why H2 for tests (not SQLite)?**

H2 starts entirely in-memory with no file I/O. Tests are:
- Faster (no disk access).
- Isolated (each test run gets a blank schema).
- CI-friendly (no external files required).

---

## 20. Build – Maven (Maven Wrapper)

| Tool | File |
|------|------|
| Build system | Apache Maven |
| Wrapper | `mvnw.cmd` |
| Spring Boot plugin | `spring-boot-maven-plugin` |

The Maven Wrapper ensures every developer and CI agent uses the **same Maven version**
without requiring a local Maven installation. The Spring Boot plugin creates an executable
fat JAR with embedded Tomcat.

**Annotation processor order (critical):**

```xml
<!-- Lombok MUST run before MapStruct -->
<annotationProcessorPaths>
    <path>org.projectlombok:lombok</path>
    <path>org.projectlombok:lombok-mapstruct-binding</path>  <!-- binding glue -->
    <path>org.mapstruct:mapstruct-processor</path>
</annotationProcessorPaths>
```

If this order is wrong, MapStruct cannot see Lombok-generated accessors and produces
`No property named 'xxx' found` errors at compile time.

---

## 21. Design Patterns Summary

### Creational

| Pattern | Where |
|---------|-------|
| **Singleton** | Every `@Component` / `@Service` — Spring manages lifecycle |
| **Factory + Multiton** | `AgentFactory` — registry of `DomainAgent` instances keyed by `DomainType` |
| **Builder** | `ChatResponse`, `UserInteraction` — Lombok `@Builder` |

### Structural

| Pattern | Where |
|---------|-------|
| **Facade** | `ChatFacadeService` — single entry point hiding the 5-step pipeline |
| **Adapter** | `OllamaTranslationService`, `NllbTranslationService` — uniform `TranslationService` interface over different backends |
| **Flyweight** | `DataGroundingLoader` — grounding file content cached once, shared across all agent calls |

### Behavioural

| Pattern | Where |
|---------|-------|
| **Template Method** | `AbstractDomainAgent.processUserQuestion()` — skeleton algorithm, concrete agents fill in domain-specific hooks |
| **Strategy** | `TranslationOrchestrationService` — selects NLLB or Ollama at runtime based on language pair |
| **Chain of Responsibility** | Full request pipeline in `ChatFacadeService` (detect → translate → classify → answer → translate back → persist) |

---

## Quick Technology Map

```
Client (HTTP)
    │
    ▼
Spring Boot 3.4.5 / Tomcat (embedded)
    │
    ▼
ChatFacadeService  ──(Caffeine cache)──▶ domainAnswers (60 min TTL)
    │
    ├── LanguageDetectionUtil      (Unicode heuristics, zero dependencies)
    │
    ├── TranslationOrchestrationService
    │      ├── OllamaTranslationService  ──▶ Ollama / Mistral (Norwegian)
    │      └── NllbTranslationService    ──▶ Flask / NLLB-200  (Amharic)
    │
    ├── DomainClassificationService    (keyword scoring, no LLM call)
    │
    ├── AgentFactory
    │      └── TaxAgent / NavAgent / ImmigrationAgent
    │             ├── RagGroundingService  ──▶ SimpleVectorStore (nomic-embed-text)
    │             └── ChatClient (Spring AI) ──▶ Ollama / Mistral
    │
    ├── AsyncPersistenceService  (@Async, pool 2–4 threads)
    │      └── UserInteractionRepository  ──▶ SQLite (HikariCP pool=1, WAL)
    │
    └── UserSessionManager
           ├── Hot tier: ConcurrentHashMap
           └── Cold tier: UserSessionRepository ──▶ SQLite
```

---

*End of TECHNOLOGY_CHOICES.md*

