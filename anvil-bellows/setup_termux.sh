#!/data/data/com.termux/files/usr/bin/bash
# ============================================================================
# ANVIL-BELLOWS — LiteLLM-Proxy-Setup für Termux (Android, on-device)
#
# Löst die bekannte Termux-Hürde: pydantic-core & tokenizers sind Rust-
# Extensions ohne fertiges aarch64-Wheel und müssen lokal kompiliert werden.
# Darum wird VOR pip die Rust-Toolchain installiert.
#
# Nutzung in Termux (aus dem Play Store):
#   pkg update && pkg install -y git
#   git clone <repo> && cd <repo>/anvil-bellows
#   bash setup_termux.sh
# ============================================================================
set -e

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
echo -e "${GREEN}=== ANVIL-BELLOWS LiteLLM Setup (Termux) ===${NC}"

cd "$(dirname "$0")"

# 1) Systempakete: Python + Rust-Toolchain (für pydantic-core/tokenizers) -----
echo -e "${GREEN}[1/4] Pakete installieren (python, rust, build-tools)...${NC}"
pkg update -y
# rust + binutils + clang: Cargo-Builds; openssl/libffi: TLS & cffi
pkg install -y python rust binutils clang openssl libffi
# uv/maturin werden von pip bei Bedarf gezogen; rustc muss auffindbar sein
export CARGO_BUILD_JOBS="${CARGO_BUILD_JOBS:-2}"   # RAM-schonend auf Phones

# 2) LiteLLM-Proxy installieren ----------------------------------------------
echo -e "${GREEN}[2/4] LiteLLM[proxy] installieren (kompiliert pydantic-core, dauert)...${NC}"
python -m pip install --upgrade pip wheel
# Falls ein Wheel für aarch64 existiert, wird es genutzt; sonst Rust-Build.
python -m pip install "litellm[proxy]"

# 3) .env anlegen — NIEMALS eine vorhandene überschreiben --------------------
echo -e "${GREEN}[3/4] .env vorbereiten...${NC}"
if [ ! -f .env ]; then
  if [ -f .env.template ]; then
    cp .env.template .env
    echo "  .env aus .env.template erstellt — bitte deine Keys eintragen."
  else
    echo -e "${YELLOW}  Keine .env.template gefunden; .env bitte manuell anlegen.${NC}"
  fi
else
  echo "  .env existiert bereits — wird NICHT überschrieben (Datensicherheit)."
fi

# 4) Breite LiteLLM-Config aus dem Provider-Katalog erzeugen -----------------
echo -e "${GREEN}[4/4] config.generated.yaml aus providers.catalog.json erzeugen...${NC}"
# --only-with-env: nur Provider aufnehmen, deren Key gesetzt ist (sauberer)
set -a; [ -f .env ] && . ./.env; set +a
python generate_config.py --only-with-env || python generate_config.py

cat <<EOF

${GREEN}=== Setup abgeschlossen ===${NC}
Starten (key-frei testbar über OVHcloud-Anonymous-Modelle):

  export LITELLM_MASTER_KEY=\${LITELLM_MASTER_KEY:-sk-anvil-local}
  litellm --config config.generated.yaml --port 4000

Danach:
  Proxy:  http://localhost:4000/v1   (OpenAI-kompatibel)
  Modelle: curl -H "Authorization: Bearer \$LITELLM_MASTER_KEY" http://localhost:4000/v1/models

Die ANVIL-BELLOWS-App nutzt diesen lokalen Proxy bevorzugt (localhost:4000)
und fällt sonst auf ihren eingebauten Router zurück.
EOF
