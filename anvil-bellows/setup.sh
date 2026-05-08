#!/bin/bash
# Anvil-Bellows Environment Setup
# Run this once to install dependencies and configure environment

set -e

echo "=== Anvil-Bellows Environment Setup ==="

# Check Python
if ! command -v python3 &> /dev/null; then
    echo "Python3 is required but not installed."
    exit 1
fi

echo "Installing LiteLLM with proxy dependencies..."
python3 -m pip install 'litellm[proxy]' requests prometheus-client

echo ""
if [ ! -f .env ]; then
  cp .env.template .env
  echo "Created .env from .env.template"
else
  echo ".env already exists, not overwriting"
fi

echo ""
echo "=== Setup Complete ==="
echo ""
echo "Next steps:"
echo "1. Fill in your API keys in .env"
echo "2. Run: source .env"
echo "3. Run: ./run_proxy.sh"
echo ""
echo "Monitor with: python3 guard_stats.py --api-key \$LITELLM_MASTER_KEY"
