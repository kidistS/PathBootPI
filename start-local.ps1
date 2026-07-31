param(
    [switch]$RebuildNllb
)

$ErrorActionPreference = "Stop"

function Wait-Http200 {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$TimeoutSeconds = 180,
        [int]$DelaySeconds = 2
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $statusCode = (Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 10).StatusCode
            if ($statusCode -eq 200) {
                return $true
            }
        }
        catch {
            Start-Sleep -Seconds $DelaySeconds
        }
    }

    return $false
}

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

$runDir = Join-Path $repoRoot ".run"
$logDir = Join-Path $repoRoot "logs"
$backendLog = Join-Path $logDir "pathboot-backend-local.log"
$backendErrLog = Join-Path $logDir "pathboot-backend-local.err.log"
$backendPidFile = Join-Path $runDir "backend.pid"

New-Item -ItemType Directory -Force -Path $runDir | Out-Null
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Write-Host "[1/4] Ensuring Ollama is running..."
if (-not (Wait-Http200 -Url "http://localhost:11434/api/tags" -TimeoutSeconds 2 -DelaySeconds 1)) {
    Start-Process -FilePath "ollama" -ArgumentList "serve" -WindowStyle Hidden | Out-Null
    if (-not (Wait-Http200 -Url "http://localhost:11434/api/tags" -TimeoutSeconds 60 -DelaySeconds 2)) {
        throw "Ollama did not become healthy on http://localhost:11434/api/tags"
    }
}

$ollamaModels = (& ollama list | Out-String)
if ($ollamaModels -notmatch "mistral") {
    Write-Host "Pulling missing model: mistral"
    & ollama pull mistral
}
if ($ollamaModels -notmatch "nomic-embed-text") {
    Write-Host "Pulling missing model: nomic-embed-text"
    & ollama pull nomic-embed-text
}

Write-Host "[2/4] Ensuring NLLB container is running..."
if ($RebuildNllb -or -not (& docker images -q pathboot-nllb)) {
    & docker build -f Dockerfile.nllb -t pathboot-nllb .
}
$existingNllb = (& docker ps -aq --filter "name=^pathboot-nllb$" 2>$null | Out-String).Trim()
if ($existingNllb) {
    & docker rm -f pathboot-nllb | Out-Null
}
& docker run -d --name pathboot-nllb -p 5000:5000 pathboot-nllb | Out-Null

if (-not (Wait-Http200 -Url "http://localhost:5000/health" -TimeoutSeconds 420 -DelaySeconds 3)) {
    throw "NLLB did not become healthy on http://localhost:5000/health"
}

Write-Host "[3/4] Starting Spring Boot backend..."
$backendAlreadyRunning = $false

if (Wait-Http200 -Url "http://localhost:8080/actuator/health" -TimeoutSeconds 2 -DelaySeconds 1) {
    $backendAlreadyRunning = $true
    Write-Host "Backend is already healthy on port 8080; reusing existing process."
}

if (Test-Path $backendPidFile) {
    $existingPid = Get-Content $backendPidFile -ErrorAction SilentlyContinue
    if ($existingPid -and (Get-Process -Id $existingPid -ErrorAction SilentlyContinue)) {
        $backendAlreadyRunning = $true
    }
}

if (-not $backendAlreadyRunning) {
    $backendCmd = '.\mvnw.cmd "-Dmaven.test.skip=true" "-Dspring-boot.run.profiles=local" spring-boot:run'
    $backendProc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $backendCmd -WorkingDirectory $repoRoot -RedirectStandardOutput $backendLog -RedirectStandardError $backendErrLog -PassThru
    Set-Content -Path $backendPidFile -Value $backendProc.Id
}

if (-not (Wait-Http200 -Url "http://localhost:8080/actuator/health" -TimeoutSeconds 240 -DelaySeconds 3)) {
    throw "Backend did not become healthy on http://localhost:8080/actuator/health. See logs/pathboot-backend-local.log"
}

Write-Host "[4/4] Local stack is ready."
Write-Host "Backend: http://localhost:8080"
Write-Host "Swagger: http://localhost:8080/swagger-ui.html"
Write-Host "Health:  http://localhost:8080/actuator/health"

