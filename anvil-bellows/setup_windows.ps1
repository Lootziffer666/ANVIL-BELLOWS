# Anvil-Bellows Environment Setup (Windows PowerShell)
# Run this once to install dependencies and prepare .env

$ErrorActionPreference = "Stop"

Write-Host "=== Anvil-Bellows Environment Setup (Windows) ===" -ForegroundColor Green

$pythonCmd = $null
if (Get-Command py -ErrorAction SilentlyContinue) {
    $pythonCmd = "py"
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    $pythonCmd = "python"
} else {
    Write-Host "Python is required but not installed. Install Python 3.10+ first." -ForegroundColor Red
    exit 1
}

Write-Host "Using Python command: $pythonCmd"
Write-Host "Installing LiteLLM + proxy dependencies..."
& $pythonCmd -m pip install "litellm[proxy]" requests prometheus-client

if (-not (Test-Path ".env")) {
    Copy-Item ".env.template" ".env"
    Write-Host "Created .env from .env.template" -ForegroundColor Yellow
} else {
    Write-Host ".env already exists, not overwriting" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Setup Complete ===" -ForegroundColor Green
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1) Open .env and set your API keys"
Write-Host "2) Run proxy with: .\\run_proxy_windows.ps1"
Write-Host "3) Optional monitoring: $pythonCmd guard_stats.py --api-key sk-anvil-safe-key"
