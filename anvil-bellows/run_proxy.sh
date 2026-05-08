#!/bin/bash
# Anvil-Bellows Start Script
# Starts LiteLLM Proxy with all configurations

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Anvil-Bellows Proxy Startup ===${NC}"

cd "$(dirname "$0")"

# Auto-load .env if present (supports KEY=VALUE and optional export KEY=VALUE)
if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

# Check for required environment variables
if [ -z "$GOOGLE_PROJECT_ID" ]; then
    echo -e "${RED}ERROR: GOOGLE_PROJECT_ID not set${NC}"
    echo "Set it in .env as GOOGLE_PROJECT_ID=your-project-id"
    exit 1
fi

if [ -z "$GOOGLE_LOCATION" ]; then
    echo -e "${YELLOW}WARNING: GOOGLE_LOCATION not set, using us-central1${NC}"
    export GOOGLE_LOCATION="us-central1"
fi

if [ -z "$OPENROUTER_API_KEY" ]; then
    echo -e "${YELLOW}WARNING: OPENROUTER_API_KEY not set - OpenRouter models will fail${NC}"
fi

if [ -z "$LITELLM_MASTER_KEY" ]; then
    echo -e "${YELLOW}WARNING: LITELLM_MASTER_KEY not set - using default${NC}"
    export LITELLM_MASTER_KEY="sk-anvil-safe-key"
fi

if [ -z "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
    echo -e "${YELLOW}WARNING: GOOGLE_APPLICATION_CREDENTIALS not set${NC}"
    echo "Set in .env as GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json"
fi

echo -e "${GREEN}Environment check passed${NC}"
echo "Project: $GOOGLE_PROJECT_ID"
echo "Location: $GOOGLE_LOCATION"

# Check if config exists
if [ ! -f "config.yaml" ]; then
    echo -e "${RED}ERROR: config.yaml not found${NC}"
    exit 1
fi

echo -e "${GREEN}Starting LiteLLM Proxy on port 4000...${NC}"
echo -e "Dashboard: ${YELLOW}http://localhost:4000/ui${NC}"
echo -e "API:       ${YELLOW}http://localhost:4000/v1${NC}"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Start the proxy
litellm --config config.yaml --port 4000 --debug
