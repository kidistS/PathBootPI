# Motivation of the project
> Critical information about Norwegian tax, NAV, and immigration is almost exclusively available in Norwegian or English,
> leaving newcomers who speak neither language entirely dependent on third-party helpers just to understand their own 
> rights and obligations. 

> As an immigrant myself, I know how overwhelming it can be to navigate a new country's bureaucracy without clear information
> about where to find answers in your native language. Many newcomers end up relying on friends, family, or paid services just
> to understand basic information about taxes, social benefits, or visa requirements, etc.

> No waiting for a translator or relying on someone else, making the transition to life in Norway more independent and empowering
> for everyone.

> This project is a labor of love to empower newcomers with the knowledge they need to thrive in Norway, regardless of their 
> language background. Newcomers (Ethiopian) who speak Amharic can now get the information they need in their own language.

> PathBoot PI was built to break that dependency. By combining local AI models with a RAG pipeline, it delivers accurate, 
> instant answers in Amharic, English or Norwegian, directly from official grounding documents.

> No cloud, no API keys, no waiting for a translator or relying on someone else, making the transition to life 
> in Norway more independent and empowering for everyone.
 


# PathBoot PI – Multilingual Domain Q&A Assistant

> A **Spring Boot 3.4** application that answers questions about **Norwegian Tax**, **NAV**, and **Immigration** in **English**,
> **Amharic**, and **Norwegian** using local AI models (Ollama/Mistral + NLLB-200) and a **Retrieval-Augmented Generation (RAG)**
> pipeline — no cloud dependency required.
>
> Access is protected by **API key authentication**. Call `POST /api/v1/auth/login` with your credentials to obtain
> your key, then include it as the `X-API-Key` header on every chat request.

---

## Table of Contents

1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Prerequisites](#prerequisites)
4. [Project Structure](#project-structure)
5. [Setup & Run](#setup--run)
6. [Authentication](#authentication)
7. [API Usage](#api-usage)
8. [Configuration](#configuration)
9. [Design Patterns](#design-patterns)
10. [Running Tests](#running-tests)
11. [Troubleshooting](#troubleshooting)

---

## Overview

PathBoot PI accepts a user question in **any of three languages** and returns an answer in the **same language**:

| Input language | Translation model      | Domain model       |
|---------------|------------------------|--------------------|
| English        | none                   | Ollama / Mistral   |
| Norwegian      | Ollama / Mistral       | Ollama / Mistral   |
| Amharic        | NLLB-200-Distilled-600M (local Python server) | Ollama / Mistral |

The system automatically **detects the language** (via Unicode script + Norwegian keyword heuristics), **classifies the domain**
(Tax / NAV / Immigration), routes to the matching **domain agent**, enriches the prompt with **RAG context** from a local vector store,
and translates the response back if needed. Session history is persisted to **SQLite** and survives restarts.

When the domain cannot be determined (**UNKNOWN**), the service short-circuits immediately — no agent or LLM is called —
and returns a static clarification message in the user's own language.

---

## Tech Stack

| Component                | Technology                                          | Version      |
|--------------------------|-----------------------------------------------------|--------------|
| Language                 | Java                                                | 17           |
| Framework                | Spring Boot                                         | 3.4.5        |
| AI / LLM integration     | Spring AI                                           | 1.0.5        |
| LLM model                | Ollama + Mistral (local)                            | latest       |
| Embedding model (RAG)    | Ollama + nomic-embed-text (local)                   | latest       |
| RAG vector store         | Spring AI `SimpleVectorStore` + JSON persistence    | —            |
| Translation (Amharic)    | NLLB-200-Distilled-600M via Python/Flask server     | —            |
| Security                 | Spring Security (API key via `X-API-Key` header)    | 3.4.5        |
| Build                    | Maven                                               | 3.8+         |
| Database                 | SQLite (persistent file, WAL mode)                  | 3.49.1.0     |
| ORM                      | Spring Data JPA + Hibernate Community Dialects      | —            |
| Connection pool          | HikariCP (pool-size=1 for SQLite single-writer)     | —            |
| In-memory cache          | Caffeine (max 500 entries, 60-minute TTL)           | —            |
| DTO mapping              | MapStruct                                           | 1.5.5.Final  |
| Logging                  | Log4j2 (async, rolling file appenders)              | —            |
| API documentation        | Springdoc OpenAPI / Swagger UI                      | 2.8.8        |
| Boilerplate reduction    | Lombok                                              | —            |
| Testing                  | JUnit 5 + Mockito                                   | —            |

---

## Prerequisites

### 1. Java 17+
```powershell
java -version   # must show 17 or higher
```

### 2. Maven 3.8+
```powershell
mvn -version
```

### 3. Ollama with required models
Install Ollama from https://ollama.ai, then pull both models:
```powershell
ollama pull mistral          # LLM for Q&A, classification, Norwegian translation
ollama pull nomic-embed-text # Embedding model for the RAG vector store
ollama serve                 # keep this running (default port 11434)
```

### 4. Python 3.9+ with NLLB translation server dependencies
```powershell
pip install flask transformers torch sentencepiece sacremoses
```

### 5. (Optional) GPU support
- A CUDA-enabled NVIDIA GPU significantly speeds up NLLB inference.
- CPU-only works but NLLB-600M may take 30–90 s on the first call.

---

## Project Structure

```
pathBootPI-1/
├── pom.xml
├── nllb_server.py                       # NLLB Python translation server (Flask)
├── README.md
├── DOCUMENTATION.md
├── ADDING_NEW_DOMAIN.md
├── TECHNOLOGY_CHOICES.md
├── PATHBOOT_DOCS.md                     # Documentation index
├── data/
│   ├── pathboot-data.db                 # SQLite persistent database (WAL mode)
│   └── vector-store.json                # Pre-computed RAG embeddings (fast restart)
├── logs/                                # Log4j2 rolling logs
└── src/
    ├── main/
    │   ├── java/com/pathboot/
    │   │   ├── PathBootApplication.java
    │   │   ├── agent/                   # DomainAgent interface + AbstractDomainAgent
    │   │   │   ├── tax/TaxAgent.java
    │   │   │   ├── nav/NavAgent.java
    │   │   │   ├── immigration/ImmigrationAgent.java
    │   │   │   └── factory/AgentFactory.java
    │   │   ├── config/                  # Ollama, NLLB, Swagger, Async, Cache, Security configs
    │   │   │   ├── SecurityConfig.java         # Spring Security filter chain
    │   │   │   └── ApiSecurityProperties.java  # Binds api.security.* from application.yml
    │   │   ├── controller/
    │   │   │   ├── ChatController.java          # Chat Q&A endpoints
    │   │   │   └── AuthController.java          # POST /api/v1/auth/login
    │   │   ├── security/
    │   │   │   └── ApiKeyAuthFilter.java        # OncePerRequestFilter — validates X-API-Key
    │   │   ├── dto/                     # ChatRequest, ChatResponse, AuthRequest, AuthResponse
    │   │   ├── enums/                   # DomainType, Language
    │   │   ├── exception/               # Custom exceptions + GlobalExceptionHandler
    │   │   ├── mapper/                  # MapStruct mappers
    │   │   ├── model/                   # UserSession JPA entity
    │   │   ├── repository/              # UserSessionRepository
    │   │   ├── service/
    │   │   │   ├── ChatFacadeService.java
    │   │   │   ├── AsyncPersistenceService.java
    │   │   │   ├── classification/DomainClassificationService.java
    │   │   │   ├── rag/RagGroundingService.java
    │   │   │   └── translation/
    │   │   ├── session/                 # UserSessionManager
                    └── util/                    # LanguageDetectionUtil, PathBootConstants, CacheKeyUtil
    │   └── resources/
    │       ├── application.yml
    │       ├── log4j2.xml
    │       └── grounding/
    │           ├── tax/tax-grounding.txt
    │           ├── nav/nav-grounding.txt
    │           └── immigration/immigration-grounding.txt
    └── test/
        └── java/com/pathboot/
            ├── agent/                   # AbstractDomainAgentTest, AgentFactoryTest
            ├── security/                # ApiKeyAuthFilterTest
            ├── service/                 # ChatFacadeServiceTest, DomainClassificationServiceTest
            └── util/                    # LanguageDetectionUtilTest
```

---

## Setup & Run

### Step 1 – Start Ollama
```powershell
ollama serve
```

### Step 2 – Start the NLLB server (required for Amharic support)
```powershell
cd C:\Users\Kidist\IdeaProjects\pathBootPI-1
python nllb_server.py
```
> The first run downloads the NLLB model (~2.5 GB) automatically.
> Subsequent starts are fast (model is cached by Hugging Face).

### Step 3 – Build and run the Spring Boot app
```powershell
cd C:\Users\Kidist\IdeaProjects\pathBootPI-1
mvn clean install -DskipTests
mvn spring-boot:run
```

> **First run:** The RAG vector store is built by chunking the grounding files and calling `nomic-embed-text` for each chunk. Embeddings are saved to
> `data/vector-store.json`. Subsequent restarts load from disk — no re-embedding needed.

### Step 4 – Open Swagger UI
Navigate to: http://localhost:8080/swagger-ui.html

---

## Authentication

All `/api/v1/chat` endpoints require an `X-API-Key` header. The login endpoint is always public.

### Step 1 — Login to get your API key

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"pathboot-admin-2026"}'
```

Response:
```json
{
  "apiKey": "pathboot-dev-key-2026",
  "instructions": "Click the Authorize button in Swagger and paste the apiKey value."
}
```

### Step 2 — Include the key in every chat request

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/chat" `
  -Headers @{"X-API-Key"="pathboot-dev-key-2026"} `
  -ContentType "application/json" `
  -Body '{"userInput":"What is the tax filing deadline?","sessionId":null}'
```

### Step 3 — Via Swagger UI

1. Open http://localhost:8080/swagger-ui.html
2. Go to **Authentication → POST /api/v1/auth/login** → Try it out → Execute with your credentials
3. Copy the `apiKey` from the response
4. Click the **Authorize 🔓** button (top right) → paste the key → **Authorize** → **Close**
5. All subsequent requests in Swagger include the header automatically

### Default credentials (from `application.yml`)

| Username | Password | API Key returned on login |
|----------|----------|--------------------------|
| `admin` | `pathboot-admin-2026` | `pathboot-dev-key-2026` |
| `user` | `pathboot-user-2026` | `pathboot-local-key-001` |

Each user receives their **own dedicated API key** — configured via `api.security.user-keys` in `application.yml`.

> To add credentials, rotate keys, or map users to keys — edit `api.security.users`, `api.security.api-keys`,
> and `api.security.user-keys` in `application.yml` and restart.
> To disable authentication for local dev — set `api.security.enabled: false`.

---

## API Usage

### POST `/api/v1/auth/login` – Obtain API key *(public — no key needed)*

```json
{ "username": "admin", "password": "pathboot-admin-2026" }
```

### POST `/api/v1/chat` – Ask a question *(requires X-API-Key)*

**Request body:**
```json
{
  "userInput": "What is the income tax rate in Norway?",
  "sessionId": null
}
```

**Response:**
```json
{
  "responseText": "The general income tax rate in Norway is 22% ...",
  "detectedLanguage": "ENGLISH",
  "detectedDomain": "TAX",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-04-28T10:30:00"
}
```

> **Tip:** Save the `sessionId` and pass it in future requests to continue the conversation.

---

### Norwegian example
```json
{ "userInput": "Hva er trinnskatt?", "sessionId": null }
```

### Amharic example
```json
{ "userInput": "ወደ ኖርዌይ ለመሄድ ምን ዓይነት ቪዛ ያስፈልጋል?", "sessionId": null }
```

### UNKNOWN domain — clarification response
If the question cannot be classified, a static clarification message is returned in the user's own language (no LLM call made):
```json
{
  "responseText": "I'm sorry, I couldn't determine which topic your question relates to. I can help with Norwegian Tax (skatt), NAV (welfare and benefits), or Immigration (UDI)...",
  "detectedLanguage": "ENGLISH",
  "detectedDomain": "UNKNOWN"
}
```

---

### GET `/api/v1/chat/sessions/{sessionId}/history`

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/chat/sessions/YOUR-SESSION-ID/history" `
  -Headers @{"X-API-Key"="pathboot-dev-key-2026"}
```

### DELETE `/api/v1/chat/sessions/{sessionId}`

```powershell
Invoke-RestMethod -Method Delete -Uri "http://localhost:8080/api/v1/chat/sessions/YOUR-SESSION-ID" `
  -Headers @{"X-API-Key"="pathboot-dev-key-2026"}
```

---

## Configuration

All configuration is in `src/main/resources/application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `api.security.enabled` | `true` | Set `false` to disable API key enforcement |
| `api.security.api-keys` | see yml | List of valid API keys (shared pool) |
| `api.security.users.admin` | `pathboot-admin-2026` | Login password for `admin` user |
| `api.security.users.user` | `pathboot-user-2026` | Login password for `user` user |
| `api.security.user-keys.admin` | `pathboot-dev-key-2026` | Per-user key returned to `admin` on login |
| `api.security.user-keys.user` | `pathboot-local-key-001` | Per-user key returned to `user` on login |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama server URL |
| `spring.ai.ollama.chat.options.model` | `mistral` | LLM model name |
| `spring.ai.ollama.embedding.options.model` | `nomic-embed-text` | Embedding model for RAG |
| `spring.ai.ollama.chat.options.keep-alive` | `-1m` | Never unload model from RAM |
| `nllb.server.base-url` | `http://localhost:5000` | NLLB Python server URL |
| `nllb.server.read-timeout-ms` | `120000` | 2-minute timeout for NLLB CPU inference |
| `spring.datasource.url` | `jdbc:sqlite:./data/pathboot-data.db` | SQLite database file path |
| `spring.datasource.hikari.maximum-pool-size` | `1` | Single writer — prevents `SQLITE_BUSY` |
| `spring.cache.caffeine.spec` | *(removed from YAML)* | Caffeine is configured programmatically in `CacheConfig.java` (max 500 entries, 60 min TTL) |
| `rag.top-k` | `3` | Number of RAG chunks retrieved per query |
| `rag.store-path` | `./data/vector-store.json` | Persisted embeddings file |
| `rag.enabled` | `true` | Set `false` to skip RAG |
| `server.port` | `8080` | Spring Boot HTTP port |

---

## Design Patterns

| Pattern | Where Used |
|---------|-----------|
| **Singleton** | All Spring beans |
| **Factory (auto-discovery)** | `AgentFactory` – injects `List<DomainAgent>` and auto-registers every `@Component` agent; no wiring needed for new domains |
| **Template Method** | `AbstractDomainAgent.processUserQuestion()` |
| **Facade** | `ChatFacadeService` – single entry point for the entire pipeline |
| **Strategy** | `TranslationService` – routes to Ollama or NLLB depending on language |
| **Adapter** | `OllamaTranslationService` / `NllbTranslationService` |
| **Builder** | `ChatResponse`, `AuthResponse`, `UserSession` – Lombok `@Builder` |
| **Chain of Responsibility** | `ChatFacadeService` pipeline: detect → translate → classify → (short-circuit UNKNOWN) → RAG → LLM → back-translate → persist |
| **Filter** | `ApiKeyAuthFilter` – `OncePerRequestFilter` validating `X-API-Key` header |
| **Two-tier cache** | `UserSessionManager` (`ConcurrentHashMap`) + SQLite (cold tier) |

---

## Running Tests

```powershell
mvn test
```

**154 tests, 0 failures.** Tests cover:
- `ApiKeyAuthFilterTest` – valid key pass-through, missing/invalid key → 401
- `LanguageDetectionUtilTest` – Ethiopic script, Norwegian special chars, Norwegian keywords without æ/ø/å
- `ChatFacadeServiceTest` – full pipeline: English, Norwegian, Amharic, UNKNOWN domain (3 language variants)
- `NllbTranslationServiceTest` – NLLB translation happy path + error cases
- `DomainClassificationServiceTest` – TAX / NAV / IMMIGRATION keyword scoring + UNKNOWN edge cases
- `AgentFactoryTest` – domain agent registration and lookup
- `AbstractDomainAgentTest` – Template Method pipeline (mocked LLM + RAG)

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `Connection refused :11434` | Run `ollama serve` first |
| `Connection refused :5000` | Run `python nllb_server.py` first |
| `401 Unauthorized` on API call | Include `X-API-Key: pathboot-dev-key-2026` header |
| Swagger UI shows 401 | The page itself is public — open http://localhost:8080/swagger-ui.html directly |
| Norwegian detected as English | Fixed: keyword detection now covers sentences without æ/ø/å |
| UNKNOWN domain response | Question did not match any domain keywords — rephrase mentioning tax, NAV, or immigration |
| NLLB model download stuck | Check internet connection; model is ~2.5 GB |
| `nomic-embed-text` not found | Run `ollama pull nomic-embed-text` |
| RAG vector store not building | Verify `rag.enabled=true` and Ollama is running |
| Slow first Amharic response | Expected — NLLB-600M on CPU takes 30–90 s on first call |
| `SQLITE_BUSY` | Already mitigated: pool-size=1 + WAL + busy_timeout=30000 |
