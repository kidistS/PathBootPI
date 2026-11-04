# PathBoot PI - Clone, Setup, and Run Guide

This is the final end-to-end setup guide for getting the project running from a fresh machine.

## 1) Clone the project

Open PowerShell and run:

```powershell
git clone https://github.com/kidistS/PathBootPI.git
cd PathBootPI
```

If you already have the repo locally, just go to the project folder:

```powershell
cd C:\Users\Kidist\Desktop\PathBootPI
```

## 2) Install required tools

Make sure the following are installed:

- Java 17+
- Docker Desktop
- Ollama

Check them with:

```powershell
java -version
docker --version
ollama --version
```

## 3) Install the required Ollama models

Run:

```powershell
ollama pull mistral
ollama pull nomic-embed-text
```

## 4) Start the local app

From the project root, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

This script starts the full local stack:

- Ollama if needed
- required models if missing
- NLLB container
- backend application on port 8080

## 5) Verify the app is running

Open these URLs in your browser:

- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

If the app is healthy, you should see a normal Spring Boot health response.

## 6) Test the API

You can use Swagger UI or a PowerShell command.

### Swagger UI

1. Open http://localhost:8080/swagger-ui.html
2. Call `POST /api/v1/auth/login`
3. Use:
   - username: `admin`
   - password: `pathboot-admin-2026`
4. Copy the returned `apiKey`
5. Use it in the `X-API-Key` header for chat requests

### PowerShell example:

```powershell
$loginBody = @{ username = "admin"; password = "pathboot-admin-2026" } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
$apiKey = $login.apiKey

$chatBody = @{ userInput = "What services does NAV provide?"; sessionId = $null } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/chat" -Headers @{ "X-API-Key" = $apiKey } -ContentType "application/json" -Body $chatBody
```

## 7) Stop the app

When you are done, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-local.ps1
```

## 8) Common issues

### Ollama not starting

```powershell
ollama serve
```

### NLLB not responding

```powershell
docker logs pathboot-nllb
```

### Backend not starting

Check Java and run the local startup script again.

### 401 Unauthorized

Make sure you are sending the `X-API-Key` header from the login response.

## 9) Profiles

This project supports multiple runtime profiles:

- `local` – SQLite + local development setup
- `test` – PostgreSQL for testing
- `prod` – PostgreSQL + hardened production settings

For a first setup, use `local`.

## 10) Important note

This project is experimental and prototype-oriented. The current grounding data is synthetic and intended for research and proof-of-concept validation rather than official production use.

## 11) Recommended next reading

- `README.md` for the main technical guide
- `docs/GETTING_STARTED_BEGINNER.md` for a simpler beginner follow-up
- `docs/ADDING_NEW_DOMAIN.md` if you want to add a new domain or knowledge area
