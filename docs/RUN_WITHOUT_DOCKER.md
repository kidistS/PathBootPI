# PathBoot PI - Run Without Docker

This guide explains how to run the application locally on your PC without using Docker.

## What you need

Install the following:

- Java 17+
- Ollama
- Docker Desktop is not required for this flow

## 1) Clone the project

```powershell
git clone https://github.com/kidistS/PathBootPI.git
cd PathBootPI
```

## 2) Pull required Ollama models

```powershell
ollama pull mistral
ollama pull nomic-embed-text
```

## 3) Start Ollama locally

```powershell
ollama serve
```

Verify it is working:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:11434/api/tags"
```

## 4) Start the NLLB translation service without Docker

The NLLB service is a Python Flask app and can also run directly on your PC without Docker.

First, install the required Python packages:

```powershell
pip install flask transformers torch sentencepiece sacremoses
```

Then start the translation service:

```powershell
python .\nllb_server.py
```

This starts the service on:

- http://localhost:5000
- health endpoint: http://localhost:5000/health
- translation endpoint: http://localhost:5000/translate

Verify health:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:5000/health"
```

## 5) Start the backend without Docker

From the project root:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:NLLB_SERVER_BASE_URL = "http://localhost:5000"
.\mvnw.cmd "-Dmaven.test.skip=true" spring-boot:run
```

This runs the Spring Boot application directly on your local machine.

## 6) Verify the app is running

Open these in the browser:

- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## 7) Test login and chat

```powershell
$loginBody = @{ username = "admin"; password = "pathboot-admin-2026" } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
$apiKey = $login.apiKey

$chatBody = @{ userInput = "What services does NAV provide?"; sessionId = $null } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/chat" -Headers @{ "X-API-Key" = $apiKey } -ContentType "application/json" -Body $chatBody
```

## 8) Stop the app

Stop the backend by closing the terminal window used for Maven, or stop the relevant Java process manually.

Stop the NLLB Python service by closing the terminal window where `python .\nllb_server.py` is running.

## 9) Important note

This setup runs both the application and the `pathboot-nllb` translation service without Docker.

The local profile uses SQLite and is designed for development and experimentation.

## 10) Related docs

- `README.md`
- `docs/CLONE_SETUP_RUN.md`
- `docs/GETTING_STARTED_BEGINNER.md`
