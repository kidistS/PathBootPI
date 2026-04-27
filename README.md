# Motivation of the project
> Critical information about Norwegian tax, NAV, and immigration is almost exclusively available in Norwegian or English — leaving newcomers who speak neither
> language entirely dependent on third-party helpers or paid translators just to understand their own rights and obligations. PathBoot PI was built to break
> that dependency. By combining local AI models with a RAG pipeline, it delivers accurate, instant answers in English, Norwegian, or Amharic, directly from
> official grounding sources — entirely offline, with no cloud dependencies or API keys. Newcomers who speak Amharic can now get the information they need
> in their own language, without waiting for a translator or relying on someone else, making the transition to life in Norway more independent and informed.

# PathBoot PI – Multilingual Domain Q&A Assistant


> A **Spring Boot 3.4** application that answers questions about **Norwegian Tax**, **NAV**, and **Immigration** in **English**, **Amharic**, and **Norwegian
> ** using local AI models (Ollama/Mistral + NLLB-200) and a **Retrieval-Augmented Generation (RAG)** pipeline — no cloud dependency, no API keys required.

---

## Table of Contents

1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Prerequisites](#prerequisites)
4. [Project Structure](#project-structure)
5. [Setup & Run](#setup--run)
6. [API Usage](#api-usage)
7. [Configuration](#configuration)
8. [Design Patterns](#design-patterns)
9. [Running Tests](#running-tests)
10. [Troubleshooting](#troubleshooting)

---

## Overview

PathBoot PI accepts a user question in **any of three languages** and returns an answer in the **same language**:

| Input language | Translation model      | Domain model       |
|---------------|------------------------|--------------------|
| English        | none                   | Ollama / Mistral   |
| Norwegian      | Ollama / Mistral       | Ollama / Mistral   |
| Amharic        | NLLB-200-Distilled-600M (local Python server) | Ollama / Mistral |

The system automatically **detects the language**, **classifies the domain** (Tax / NAV / Immigration), routes to the matching **domain agent**, enriches the prompt with **RAG context** from a local vector store, and translates the response back if needed. Session history is persisted to **SQLite** and survives restarts.

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
    │   │   ├── config/                  # Ollama, NLLB, Swagger, Async, Cache configs
    │   │   ├── controller/              # ChatController (REST endpoints)
    │   │   ├── dto/                     # ChatRequest, ChatResponse DTOs
    │   │   ├── enums/                   # DomainType, Language
    │   │   ├── exception/               # Custom exceptions + GlobalExceptionHandler
    │   │   ├── mapper/                  # MapStruct mappers
    │   │   ├── model/                   # UserSession JPA entity
    │   │   ├── repository/              # UserSessionRepository
    │   │   ├── service/
    │   │   │   ├── ChatFacadeService.java          # Facade: detect → translate → classify → answer → translate back
    │   │   │   ├── AsyncPersistenceService.java    # @Async session persistence (separate bean)
    │   │   │   ├── classification/                 # DomainClassificationService
    │   │   │   ├── rag/                            # RagGroundingService (SmartInitializingSingleton)
    │   │   │   └── translation/                    # Ollama + NLLB + orchestration
    │   │   ├── session/                 # UserSessionManager (ConcurrentHashMap hot cache)
    │   │   └── util/                    # LanguageDetectionUtil, PathBootConstants, etc.
    │   └── resources/
    │       ├── application.yml
    │       ├── log4j2.xml
    │       └── grounding/
    │           ├── tax/tax-grounding.txt
    │           ├── nav/nav-grounding.txt
    │           └── immigration/immigration-grounding.txt
    └── test/
        └── java/com/pathboot/
            ├── service/domain/          # Tax/Nav/ImmigrationDomainServiceTest
            ├── service/                 # ChatFacadeServiceTest
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

> **First run:** The RAG vector store is built by chunking the grounding files and calling `nomic-embed-text` for each chunk. Embeddings are saved to `data/vector-store.json`. Subsequent restarts load from disk — no re-embedding needed.

### Step 4 – Open Swagger UI
Navigate to: http://localhost:8080/swagger-ui.html

---

## API Usage

### POST `/api/v1/chat` – Ask a question

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
  "timestamp": "2026-04-26T10:30:00"
}
```

> **Tip:** Save the `sessionId` and pass it in future requests to continue the conversation. Session history is persisted to SQLite and survives application restarts.

---

### Norwegian example
```json
{
  "userInput": "Hvordan søker jeg om dagpenger?",
  "sessionId": null
}
```

### Amharic example
```json
{
  "userInput": "ወደ ኖርዌይ ለመሄድ ምን ዓይነት ቪዛ ያስፈልጋል?",
  "sessionId": null
}
```

---

### GET `/api/v1/chat/sessions/{sessionId}/history` – View conversation history

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/chat/sessions/YOUR-SESSION-ID/history"
```

### DELETE `/api/v1/chat/sessions/{sessionId}` – Clear session

```powershell
Invoke-RestMethod -Method Delete -Uri "http://localhost:8080/api/v1/chat/sessions/YOUR-SESSION-ID"
```

---

## Configuration

All configuration is in `src/main/resources/application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama server URL |
| `spring.ai.ollama.chat.options.model` | `mistral` | LLM model name |
| `spring.ai.ollama.embedding.options.model` | `nomic-embed-text` | Embedding model for RAG |
| `spring.ai.ollama.chat.options.keep-alive` | `-1m` | Never unload model from RAM (eliminates cold-start delay) |
| `nllb.server.base-url` | `http://localhost:5000` | NLLB Python server URL |
| `nllb.server.read-timeout-ms` | `120000` | 2-minute timeout for NLLB CPU inference |
| `spring.datasource.url` | `jdbc:sqlite:./data/pathboot-data.db` | SQLite database file path |
| `spring.datasource.hikari.maximum-pool-size` | `1` | Single writer — prevents `SQLITE_BUSY` errors |
| `spring.datasource.hikari.connection-init-sql` | WAL + busy_timeout=30000 | WAL journal mode + 30 s lock wait |
| `spring.cache.caffeine.spec` | `maximumSize=500,expireAfterWrite=60m` | In-memory answer cache |
| `rag.top-k` | `3` | Number of RAG chunks retrieved per query |
| `rag.store-path` | `./data/vector-store.json` | Persisted embeddings file |
| `rag.enabled` | `true` | Set `false` to skip RAG and use full grounding file |
| `server.port` | `8080` | Spring Boot HTTP port |
| `management.endpoints.web.exposure.include` | `health,info` | Exposed Actuator endpoints |

---

## Design Patterns

| Pattern | Where Used |
|---------|-----------|
| **Singleton** | All Spring beans (agents, services, repositories) |
| **Factory** | `AgentFactory` – returns the correct domain agent by `DomainType` |
| **Template Method** | `AbstractDomainAgent.processUserQuestion()` – shared pipeline; subclasses provide domain-specific hooks |
| **Facade** | `ChatFacadeService` – single entry point orchestrating the entire request pipeline |
| **Strategy** | `TranslationService` – routes to Ollama or NLLB depending on language |
| **Adapter** | `OllamaTranslationService` / `NllbTranslationService` – uniform interface over different translation backends |
| **Builder** | `ChatResponse`, `UserSession` – Lombok `@Builder` |
| **Chain of Responsibility** | Request pipeline: detect language → translate → classify domain → RAG enrich → LLM answer → translate back → persist async |
| **Two-tier cache** | `UserSessionManager` (`ConcurrentHashMap` hot tier) + SQLite via JPA (cold tier, survives restart) |
| **SmartInitializingSingleton** | `RagGroundingService` pre-loads vector store before Tomcat opens its port — no race condition |

---

## Running Tests

```powershell
mvn test
```

Tests cover:
- `LanguageDetectionUtilTest` – language detection heuristics (Ethiopic script, Norwegian keywords)
- `TaxDomainServiceTest` – tax agent delegation and RAG integration
- `NavDomainServiceTest` – NAV agent delegation
- `ImmigrationDomainServiceTest` – immigration agent delegation
- `ChatFacadeServiceTest` – full pipeline orchestration (all external dependencies mocked)

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `Connection refused :11434` | Run `ollama serve` first |
| `Connection refused :5000` | Run `python nllb_server.py` first |
| NLLB model download stuck | Check internet connection; model is ~2.5 GB |
| Amharic text not detected | Ensure the text contains Ethiopic Unicode characters (U+1200–U+137F) |
| `nomic-embed-text` not found | Run `ollama pull nomic-embed-text` |
| RAG vector store not building | Verify `rag.enabled=true` and that `nomic-embed-text` is pulled and Ollama is running |
| Slow first Amharic response | Expected — NLLB-600M on CPU takes 30–90 s on the first request; subsequent calls are faster |
| `SQLITE_BUSY: database is locked` | Already mitigated: pool-size=1 + WAL mode + busy_timeout=30000. If still occurring, check for external SQLite clients holding a write lock on `data/pathboot-data.db` |
| Swagger UI blank | Navigate to http://localhost:8080/swagger-ui.html |
| Actuator health check | Navigate to http://localhost:8080/actuator/health |
