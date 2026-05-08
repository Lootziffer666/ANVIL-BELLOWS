# Anvil-Bellows Start Script (Windows PowerShell)
# Starts LiteLLM Proxy with all configurations

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return
    }

    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) { return }

        if ($line.StartsWith("export ")) {
            $line = $line.Substring(7)
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) { return }

        $key = $parts[0].Trim()
        $value = $parts[1].Trim().Trim('"').Trim("'")
        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
}

Write-Host "=== Anvil-Bellows Proxy Startup (Windows) ===" -ForegroundColor Green
Import-DotEnv ".env"

if (-not $env:GOOGLE_PROJECT_ID) {
    Write-Host "ERROR: GOOGLE_PROJECT_ID not set (.env)" -ForegroundColor Red
    exit 1
}

if (-not $env:GOOGLE_LOCATION) {
    Write-Host "WARNING: GOOGLE_LOCATION not set, using us-central1" -ForegroundColor Yellow
    $env:GOOGLE_LOCATION = "us-central1"
}

if (-not $env:OPENROUTER_API_KEY) {
    Write-Host "WARNING: OPENROUTER_API_KEY not set - OpenRouter models will fail" -ForegroundColor Yellow
}

if (-not $env:LITELLM_MASTER_KEY) {
    Write-Host "WARNING: LITELLM_MASTER_KEY not set - using default" -ForegroundColor Yellow
    $env:LITELLM_MASTER_KEY = "sk-anvil-safe-key"
}

if (-not $env:GOOGLE_APPLICATION_CREDENTIALS) {
    Write-Host "WARNING: GOOGLE_APPLICATION_CREDENTIALS not set" -ForegroundColor Yellow
}

Write-Host "Environment check passed" -ForegroundColor Green
Write-Host "Project: $($env:GOOGLE_PROJECT_ID)"
Write-Host "Location: $($env:GOOGLE_LOCATION)"
Write-Host "Dashboard: http://localhost:4000/ui"
Write-Host "API:       http://localhost:4000/v1"
Write-Host ""
Write-Host "Press Ctrl+C to stop"
Write-Host ""

litellm --config config.yaml --port 4000 --debug
