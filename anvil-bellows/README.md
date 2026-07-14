# Anvil-Bellows - V1.0 Infrastruktur & Kosten-Schild

Eigenständiges Repo/Deployment, organisatorisch IIG-Infrastruktur — kein Teil des
`ANVIL`-Kotlin-Monorepos, auch wenn beide "Bellows" heißen (Schmiede-Metapher, siehe
`ANVIL/docs/ANVIL_CONCEPT_CONTRACT.md`, "Anvil × Anvil-Bellows"). `ANVIL` hat einen
eigenen, Kotlin-nativen Router (`anvil-kmp/modules/bellows`, Port 8765) für seine eigene
Modul-Orchestrierung — dieser Proxy hier kann darin einfach als ein weiterer
OpenAI-kompatibler `ProviderAdapter` eingetragen werden (Rezept:
`anvil-kmp/modules/bellows/README.md`, "Anvil-Bellows als Upstream-Provider"), statt
Cloud-Credentials zweimal zu verwalten. Die beiden Implementierungen bleiben getrennte
Prozesse, keine Konkurrenz.

## Docker (empfohlen für einen dauerhaft erreichbaren Gateway)

Android-App und Windows-WebView-Client sind Clients/Konfigurationsoberflächen,
kein Server, der dauerhaft läuft. Für einen stabilen `BELLOWS_BASE_URL`, den
andere Repos (DECOMPILE, SHADED, LAB, CUE-AGENT) zuverlässig ansprechen
können, läuft der Proxy als eigener Container statt nur "solange ein Telefon
oder PowerShell-Fenster offen ist":

```bash
cp .env.template .env
# .env ausfüllen (GOOGLE_PROJECT_ID, GOOGLE_APPLICATION_CREDENTIALS als
# ABSOLUTER Host-Pfad zur service-account.json, OPENROUTER_API_KEY,
# LITELLM_MASTER_KEY)
docker compose up -d --build
```

Danach ist der Proxy unter `http://<host>:4000/v1` erreichbar (OpenAI-
kompatibel, `Authorization: Bearer <LITELLM_MASTER_KEY>`), persistiert über
Neustarts (`restart: unless-stopped`) und cached lokal über einen benannten
Volume. `docker-compose.yml` mountet die GCP-Credential-Datei read-only und
überschreibt `GOOGLE_APPLICATION_CREDENTIALS` intern auf einen festen
Container-Pfad — die `.env` bleibt dieselbe wie beim lokalen Lauf.

**Bugfix (2026-07-13):** `config.yaml` verwendete durchgängig
`"${VAR_NAME}"` (Bash/Compose-Stil) für Secrets/Parameter. LiteLLM löst
Environment-Variablen aber ausschließlich über die eigene
`os.environ/VAR_NAME`-Syntax auf (bereits korrekt verwendet in
`config.generated.yaml`/`generate_config.py`) — mit der alten Syntax wurde
z. B. `master_key` nie durch den echten Wert ersetzt, sondern blieb
buchstäblich der String `${LITELLM_MASTER_KEY}`, wodurch **jede** Anfrage
(auch mit korrektem Master-Key) mit `"No connected db."` fehlschlug, weil
der Key nie als Master-Key erkannt wurde. Verifiziert: vor dem Fix schlug
`/v1/chat/completions` mit korrektem Key fehl, nach dem Fix (16 Stellen in
`config.yaml` auf `os.environ/...` umgestellt) funktioniert Auth und
Modell-Listing wie dokumentiert. Betraf jede Laufzeitumgebung gleichermaßen
(Docker, `run_proxy.sh`, `run_proxy_windows.ps1`) — keine Docker-spezifische
Einschränkung.

## Quick Start (Windows, ohne WSL)

> Alle Schritte in **PowerShell** ausführen.

```powershell
# 1) In den Projektordner wechseln
cd anvil-bellows

# 2) Dependencies installieren + .env anlegen
.\setup_windows.ps1

# 3) .env bearbeiten (API Keys + Credentials)
notepad .env

# 4) Proxy starten
.\run_proxy_windows.ps1
```

## Portable (Windows, ohne globale Dependency-Installation)

Ja, geht: Du kannst ein **portables Bundle** bauen, das eine lokale Runtime inkl. Dependencies enthält. Auf dem Zielrechner brauchst du dann weder WSL noch eine globale Python-Installation.

> Build-Maschine: Python **3.11 oder 3.12 (64-bit)** empfohlen (verhindert `orjson` Build-Fehler).

```powershell
cd anvil-bellows\portable
.\build_portable.ps1

# optional: als ZIP verteilen
Compress-Archive -Path ..\dist\AnvilBellowsPortable\* -DestinationPath ..\dist\AnvilBellowsPortable.zip -Force
```

Danach reicht auf dem Zielsystem:

```powershell
cd AnvilBellowsPortable
.\start_portable.bat
```

Beim ersten Start wird `.env` aus `.env.template` angelegt.

Hinweis: Wenn du zuvor den Fehler `No module named litellm.__main__` gesehen hast, bitte das Portable Bundle mit der aktuellen Version von `build_portable.ps1` neu erzeugen.

## Alternative: Linux/macOS

```bash
cd anvil-bellows
chmod +x setup.sh run_proxy.sh
./setup.sh
cp .env.template .env
# .env bearbeiten
source .env
./run_proxy.sh
```

## Structure

| File | Purpose |
|------|---------|
| `config.yaml` | Main LiteLLM configuration |
| `run_proxy_windows.ps1` | Start proxy on Windows |
| `run_proxy.sh` | Start proxy on Linux/macOS |
| `guard_stats.py` | Real-time budget monitoring |
| `model_manager.py` | Add/remove models dynamically |
| `setup_windows.ps1` | Windows setup |
| `setup.sh` | Linux/macOS setup |
| `.env.template` | Environment template for all platforms |

## Models (Pre-configured)

### Vertex AI (your $250 credit)
- `gpt-4o` → Gemini 1.5 Pro-002 (Mayor)
- `gpt-4.5` → Gemini 2.0 Flash
- `gpt-4o-mini` → Gemini 1.5 Flash-002 (Polecat)
- `gpt-4o-mini-2` → Gemini 2.0 Flash-exp

### OpenRouter (Free Tier)
- `refinery` → nomic-ai/mimo-v2-pro
- `refinery-deepseek` → deepseek/deepseek-chat
- `qwen-coder` → qwen/qwen-2.5-coder-32b-instruct
- `phi-mini` → microsoft/phi-4-mini
- `nous-hermes` → nousresearch/nous-hermes-2-mistral-7b-dpo
- `claude-sonnet` → anthropic/claude-3-sonnet

## Adding New Models

### Method 1: Edit config.yaml (requires restart)

```yaml
model_list:
  - model_name: my-new-model
    litellm_params:
      model: provider/model-name
      api_key: "${MY_API_KEY}"
      # ... other params
```

Then restart proxy (`.\run_proxy_windows.ps1` on Windows / `./run_proxy.sh` on Linux).

### Method 2: Dynamic (no restart)

Create a model config file `my_model.json`:
```json
{
  "model_name": "my-model",
  "litellm_params": {
    "model": "openai/gpt-4o",
    "api_key": "sk-..."
  }
}
```

Add it:
```bash
python model_manager.py add my_model.json
```

### Method 3: Via API

```bash
curl -X POST http://localhost:4000/model/add \
  -H "Authorization: Bearer sk-anvil-safe-key" \
  -H "Content-Type: application/json" \
  -d '{"model_name":"new-model","litellm_params":{"model":"..."}}'
```

## Adding Custom Providers

### Ollama (Local)
```yaml
- model_name: ollama-llama3
  litellm_params:
    model: ollama/llama3
    api_base: http://localhost:11434
```

### OpenAI Compatible
```yaml
- model_name: custom-gpt
  litellm_params:
    model: openai/custom-model
    api_key: "${OPENAI_KEY}"
    api_base: https://your-endpoint.com/v1
```

### Anthropic
```yaml
- model_name: claude-opus
  litellm_params:
    model: anthropic/claude-3-opus
    api_key: "${ANTHROPIC_KEY}"
```

### Azure OpenAI
```yaml
- model_name: azure-gpt4
  litellm_params:
    model: azure/gpt-4
    api_key: "${AZURE_KEY}"
    api_base: https://your-resource.openai.azure.com
```

## API Usage

```python
import openai
client = openai.OpenAI(
    api_key="sk-anvil-safe-key",
    base_url="http://localhost:4000/v1"
)

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Hello!"}]
)
```

## Budget Protection

- **Hard limit**: $5/day (configurable in config.yaml)
- **Max tokens/request**: 8,192 (prevents runaway loops)
- **80% warning**: Console alert when budget reaches 80%
- **100% block**: Returns 429 Error - all requests blocked

## Monitoring

```bash
# Real-time dashboard
python guard_stats.py --api-key sk-anvil-safe-key

# LiteLLM Admin UI
# Open: http://localhost:4000/ui
```

## Troubleshooting

| Error | Solution |
|-------|----------|
| Connection refused | Proxy not running. Run `.\\run_proxy_windows.ps1` (Windows) or `./run_proxy.sh` (Linux/macOS) |
| 401 Unauthorized | Check `LITELLM_MASTER_KEY` in `.env` |
| 429 Budget exceeded | Wait for reset or increase limit in `config.yaml` |
| Vertex error | Check `GOOGLE_APPLICATION_CREDENTIALS` path |
| OpenRouter error | Verify `OPENROUTER_API_KEY` in `.env` |
| `No module named 'prometheus_client'` | Setup erneut ausführen (`.\setup_windows.ps1` oder `./setup.sh`) und Portable Bundle neu bauen |
