# Anvil-Bellows LiteLLM proxy — containerized so it can run as a persistent,
# network-reachable BELLOWS_BASE_URL instead of only "while a phone app or
# PowerShell window happens to be open". Mirrors setup.sh's dependency set
# and run_proxy.sh's startup command exactly; see docker-compose.yml for the
# env/credential wiring.
FROM python:3.11-slim

WORKDIR /app

RUN pip install --no-cache-dir 'litellm[proxy]' requests prometheus-client

COPY config.yaml ./
COPY model_manager.py guard_stats.py ./

EXPOSE 4000

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD python3 -c "import urllib.request; urllib.request.urlopen('http://localhost:4000/health/liveliness', timeout=4)" || exit 1

CMD ["litellm", "--config", "config.yaml", "--port", "4000", "--host", "0.0.0.0"]
