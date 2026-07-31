param(
    [switch]$StopOllama
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

$backendPidFile = Join-Path $repoRoot ".run\backend.pid"

Write-Host "Stopping backend..."
if (Test-Path $backendPidFile) {
    $backendPid = Get-Content $backendPidFile -ErrorAction SilentlyContinue
    if ($backendPid) {
        $proc = Get-Process -Id $backendPid -ErrorAction SilentlyContinue
        if ($proc) {
            Stop-Process -Id $backendPid -Force
            Write-Host "Stopped backend PID $backendPid"
        }
    }
    Remove-Item $backendPidFile -Force -ErrorAction SilentlyContinue
}
else {
    Write-Host "No backend PID file found."
}

# Fallback: stop stale PathBoot backend bound to port 8080 even without PID file.
$port8080 = (& netstat -ano | Select-String ":8080" | Select-Object -First 1)
if ($port8080) {
    $pidMatch = [regex]::Match($port8080.ToString(), "\s+(\d+)\s*$")
    if ($pidMatch.Success) {
        $listeningPid = [int]$pidMatch.Groups[1].Value
        $procInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $listeningPid" -ErrorAction SilentlyContinue
        if ($procInfo -and ($procInfo.CommandLine -match "PathBootApplication" -or $procInfo.CommandLine -match "PathBootPI")) {
            Stop-Process -Id $listeningPid -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped stale backend PID $listeningPid on port 8080"
        }
    }
}

Write-Host "Stopping NLLB container..."
$containerId = (& docker ps -aq --filter "name=^pathboot-nllb$" 2>$null | Out-String).Trim()
if ($containerId) {
    & docker rm -f pathboot-nllb | Out-Null
}

if ($StopOllama) {
    Write-Host "Stopping Ollama serve processes..."
    Get-CimInstance Win32_Process -Filter "Name = 'ollama.exe'" |
        Where-Object { $_.CommandLine -match "serve" } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
}

Write-Host "Done."

