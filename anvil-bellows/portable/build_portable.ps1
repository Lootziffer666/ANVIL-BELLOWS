# Build a portable Windows bundle for Anvil-Bellows (no WSL, no global Python needed on target machine)
# IMPORTANT: Build machine should have Python 3.11 or 3.12 installed.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$distRoot = Join-Path $root "dist"
$bundle = Join-Path $distRoot "AnvilBellowsPortable"
$runtime = Join-Path $bundle "runtime"

function Resolve-BuildPython {
    # Prefer versions with broad binary wheel support (avoids building orjson from source)
    $candidates = @(
        @{ Cmd = "py"; Args = @("-3.12") },
        @{ Cmd = "py"; Args = @("-3.11") },
        @{ Cmd = "python"; Args = @() }
    )

    foreach ($candidate in $candidates) {
        if (-not (Get-Command $candidate.Cmd -ErrorAction SilentlyContinue)) {
            continue
        }

        try {
            $version = & $candidate.Cmd @($candidate.Args + @("-c", "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')"))
            $version = $version.Trim()

            if ($version -match '^3\.(1[12])$') {
                return $candidate
            }
        } catch {
            continue
        }
    }

    throw "No suitable Python found. Please install Python 3.11 or 3.12 (64-bit) and ensure 'py -3.12' or 'py -3.11' works."
}

Write-Host "=== Building Anvil-Bellows Portable Bundle ===" -ForegroundColor Green

if (Test-Path $bundle) {
    Remove-Item $bundle -Recurse -Force
}

New-Item -ItemType Directory -Path $bundle | Out-Null

$python = Resolve-BuildPython
Write-Host "Using Python launcher: $($python.Cmd) $($python.Args -join ' ')" -ForegroundColor Cyan
& $python.Cmd @($python.Args + @("-m", "venv", $runtime))

$pyExe = Join-Path $runtime "Scripts\python.exe"
& $pyExe -m pip install --upgrade pip setuptools wheel

# Force binary wheels for orjson to avoid Rust toolchain/source builds.
& $pyExe -m pip install --only-binary=:all: "orjson==3.10.16"
& $pyExe -m pip install --prefer-binary --only-binary=orjson "litellm[proxy]" requests prometheus-client

Copy-Item (Join-Path $root "config.yaml") (Join-Path $bundle "config.yaml")
Copy-Item (Join-Path $root ".env.template") (Join-Path $bundle ".env.template")
Copy-Item (Join-Path $root "model_manager.py") (Join-Path $bundle "model_manager.py")
Copy-Item (Join-Path $root "guard_stats.py") (Join-Path $bundle "guard_stats.py")

$startBat = @"
@echo off
setlocal enabledelayedexpansion
cd /d %~dp0
if not exist .env (
  copy .env.template .env >nul
  echo Created .env from .env.template
)
for /f "usebackq tokens=* delims=" %%A in (".env") do (
  set "line=%%A"
  call :setLine
)
if "%GOOGLE_PROJECT_ID%"=="" (
  echo ERROR: GOOGLE_PROJECT_ID missing in .env
  pause
  exit /b 1
)
if "%GOOGLE_LOCATION%"=="" set GOOGLE_LOCATION=us-central1
if "%LITELLM_MASTER_KEY%"=="" set LITELLM_MASTER_KEY=sk-anvil-safe-key

echo Starting LiteLLM Proxy...
echo Dashboard: http://localhost:4000/ui
echo API:       http://localhost:4000/v1
if not exist runtime\Scripts\litellm.exe (
  echo ERROR: runtime\Scripts\litellm.exe not found. Portable build incomplete.
  pause
  exit /b 1
)
runtime\Scripts\litellm.exe --config config.yaml --port 4000 --debug
exit /b %errorlevel%

:setLine
if "%line%"=="" goto :eof
if "%line:~0,1%"=="#" goto :eof
for /f "tokens=1,* delims==" %%K in ("%line%") do (
  set "key=%%K"
  set "val=%%L"
)
if defined key set "%key%=%val%"
set "key="
set "val="
goto :eof
"@

Set-Content -Path (Join-Path $bundle "start_portable.bat") -Value $startBat -Encoding ASCII

$readme = @"
Anvil-Bellows Portable (Windows)
================================

1) start_portable.bat starten
2) Beim ersten Start wird .env aus .env.template erzeugt
3) .env ausfüllen (API Keys)
4) start_portable.bat erneut starten

Hinweis:
- Dieses Bundle enthält eine eigene Python-Runtime + Abhängigkeiten lokal im Ordner ./runtime.
- Auf dem Zielsystem ist keine globale Python-Installation nötig.
"@
Set-Content -Path (Join-Path $bundle "README_PORTABLE.txt") -Value $readme -Encoding UTF8

Write-Host "Portable bundle created: $bundle" -ForegroundColor Green
Write-Host "Zip for distribution:" -ForegroundColor Cyan
Write-Host "  Compress-Archive -Path '$bundle\\*' -DestinationPath '$distRoot\\AnvilBellowsPortable.zip' -Force"
