#!/usr/bin/env python3
"""
generate_config.py — erzeugt eine breite LiteLLM-`model_list` aus
`providers.catalog.json`.

Designprinzipien:
  * Keys NIEMALS im Klartext: jeder API-Key wird als `os.environ/<ENV>`
    referenziert (LiteLLM löst das zur Laufzeit auf). Anonyme Endpunkte
    (auth=none) bekommen einen unschädlichen Dummy-Key.
  * Native LiteLLM-Präfixe (groq/, gemini/, cerebras/, …) wo bekannt —
    sonst generisch `openai/<model>` + `api_base` (OpenAI-kompatibel).
  * `model_info.free`/`model_info.provider` werden mitgeschrieben, damit die
    Policy-Schicht (BellowsHttpServer) gezielt Free-Tier-Modelle wählen kann.
  * Reines Standard-Python (kein PyYAML nötig) — läuft auch im Termux-Minimalsetup.

Aufruf:
    python generate_config.py [--catalog providers.catalog.json]
                              [--out config.generated.yaml]
                              [--only-with-env]   # nur Provider, deren ENV gesetzt ist
"""
import argparse
import json
import os
import sys

# Dummy-Key für anonyme (key-freie) Endpunkte — LiteLLM erwartet beim
# openai-Provider einen api_key-Parameter, der Endpunkt ignoriert ihn.
NO_AUTH_DUMMY = "anonymous-no-auth"


def yq(value: str) -> str:
    """YAML-sicheres doppelt gequotetes Skalar."""
    return '"' + str(value).replace("\\", "\\\\").replace('"', '\\"') + '"'


def build_entries(providers, only_with_env):
    lines = []
    stats = {"providers": 0, "models": 0, "skipped_env": 0}
    for p in providers:
        env_key = p.get("env_key", "")
        auth = p.get("auth", "api_key")
        if only_with_env and auth == "api_key" and not os.environ.get(env_key):
            stats["skipped_env"] += 1
            continue
        stats["providers"] += 1
        prefix = p.get("litellm_prefix", "openai")
        is_openai_compatible = prefix == "openai"
        api_key_ref = NO_AUTH_DUMMY if auth == "none" else f"os.environ/{env_key}"

        for model in p.get("models", []):
            litellm_model = f"openai/{model}" if is_openai_compatible else f"{prefix}/{model}"
            lines.append(f"  - model_name: {yq(p['id'] + '/' + model)}")
            lines.append("    litellm_params:")
            lines.append(f"      model: {yq(litellm_model)}")
            if is_openai_compatible:
                lines.append(f"      api_base: {yq(p['base_url'])}")
            lines.append(f"      api_key: {yq(api_key_ref)}")
            lines.append("    model_info:")
            lines.append(f"      free: {str(bool(p.get('free', False))).lower()}")
            lines.append(f"      provider: {yq(p['id'])}")
            if p.get("limits"):
                lines.append(f"      limits_hint: {yq(p['limits'])}")
            stats["models"] += 1
    return lines, stats


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    ap = argparse.ArgumentParser()
    ap.add_argument("--catalog", default=os.path.join(here, "providers.catalog.json"))
    ap.add_argument("--out", default=os.path.join(here, "config.generated.yaml"))
    ap.add_argument("--only-with-env", action="store_true",
                    help="nur Provider aufnehmen, deren API-Key-ENV gesetzt ist")
    args = ap.parse_args()

    with open(args.catalog, encoding="utf-8") as f:
        catalog = json.load(f)
    providers = catalog.get("providers", [])

    entries, stats = build_entries(providers, args.only_with_env)

    header = [
        "# ============================================================",
        "# AUTOGENERIERT von generate_config.py — NICHT von Hand editieren.",
        f"# Quelle: {os.path.basename(args.catalog)} (Katalog-Version "
        f"{catalog.get('version', '?')})",
        "# Keys werden ausschließlich via os.environ/<ENV> aufgelöst.",
        "# ============================================================",
        "",
        "model_list:",
    ]
    footer = [
        "",
        "general_settings:",
        '  master_key: "os.environ/LITELLM_MASTER_KEY"',
        "",
        "litellm_settings:",
        "  drop_params: true",
        "  # request_timeout konservativ; Limits lernt ANVIL-BELLOWS zur Laufzeit.",
        "  request_timeout: 120",
    ]
    out = "\n".join(header + entries + footer) + "\n"
    with open(args.out, "w", encoding="utf-8") as f:
        f.write(out)

    print(f"✓ {args.out}")
    print(f"  Provider: {stats['providers']}  Modelle: {stats['models']}"
          + (f"  (übersprungen ohne ENV: {stats['skipped_env']})" if args.only_with_env else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
