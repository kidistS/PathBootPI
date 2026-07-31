param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "pathboot-admin-2026",
    [string]$Question = "What services does NAV provide?"
)

$ErrorActionPreference = "Stop"

$loginUri = "$BaseUrl/api/v1/auth/login"
$chatUri = "$BaseUrl/api/v1/chat"

Write-Host "Running API smoke test against $BaseUrl"

$loginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest -UseBasicParsing -Method Post -Uri $loginUri -ContentType "application/json" -Body $loginBody
$loginPayload = $loginResponse.Content | ConvertFrom-Json

if (-not $loginPayload.apiKey) {
    throw "Login did not return apiKey. Response: $($loginResponse.Content)"
}

$apiKey = $loginPayload.apiKey

$chatBody = @{
    userInput = $Question
} | ConvertTo-Json

$chatResponse = Invoke-RestMethod -Method Post -Uri $chatUri -Headers @{ "X-API-Key" = $apiKey } -ContentType "application/json" -Body $chatBody

$result = [PSCustomObject]@{
    loginStatusCode = [int]$loginResponse.StatusCode
    apiKeyPrefix = if ($apiKey.Length -ge 8) { $apiKey.Substring(0, 8) + "..." } else { $apiKey }
    detectedLanguage = $chatResponse.detectedLanguage
    detectedDomain = $chatResponse.detectedDomain
    sessionId = $chatResponse.sessionId
    conversationEnded = $chatResponse.conversationEnded
    responsePreview = if ($chatResponse.responseText.Length -gt 140) { $chatResponse.responseText.Substring(0, 140) + "..." } else { $chatResponse.responseText }
}

$result | ConvertTo-Json -Depth 4

