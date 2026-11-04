# PathBoot PI - Beginner-Friendly Guide

This is the simple version of the setup guide. It is written for people who want to run the project without deep technical knowledge.

## What this project is

PathBoot PI is an experimental project that explores how an AI assistant could help people find information about Norwegian tax, NAV, and immigration services in different languages.

It is not yet a production public service. It is a prototype and research project.

## What you need before starting

Install the following on your computer:

- Java 17 or newer
- Docker Desktop
- Ollama

You also need to download these models in Ollama:

```powershell
ollama pull mistral
ollama pull nomic-embed-text
```

## Step 1: Open PowerShell in the project folder

Go to the folder where the project is stored, for example:

```powershell
cd C:\Users\Kidist\Desktop\PathBootPI
```

## Step 2: Start the app with the easy script

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

This script does the following automatically:

- starts Ollama if needed
- downloads required models if missing
- builds the NLLB translation container
- starts the backend application

## Step 3: Wait for the app to start

After a short time, the application should be available at:

- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

## Step 4: Test the app

A simple smoke test is already included:

```powershell
powershell -ExecutionPolicy Bypass -File .\smoke-api.ps1
```

You can also try logging in manually in Swagger.

Default test credentials:

- username: `admin`
- password: `pathboot-admin-2026`

## Step 5: Stop the app

When you are done, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-local.ps1
```

## Common problems

### Ollama not responding

Run:

```powershell
ollama serve
```

Then check:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:11434/api/tags"
```

### Docker container not starting

Check whether Docker is running and then try again.

### Backend not starting

Make sure Java 17 is installed and run the local script again.

## Important note

This project is an experimental prototype. The grounding data is fabricated for testing the solution and should not be treated as official public information.

The purpose is to explore whether such an AI-powered assistant can solve a real-world problem and how the system should be structured.

## Recommended next step

If you are a developer, read the main `README.md` for the full technical guide. If you only want to run the prototype, use this beginner guide.
