"use client";

import { FormEvent, useMemo, useState } from "react";

type Endpoint = {
  id: string;
  alias: string;
  model: string;
  apiBase: string;
  apiKeyEnv: string;
};

const starter: Endpoint[] = [
  {
    id: "1",
    alias: "custom-gpt",
    model: "openai/gpt-4o-mini",
    apiBase: "https://my-endpoint.example.com/v1",
    apiKeyEnv: "OPENAI_KEY",
  },
  {
    id: "2",
    alias: "support-bot",
    model: "openai/gpt-4.1-mini",
    apiBase: "https://support-llm.example.com/v1",
    apiKeyEnv: "SUPPORT_LLM_KEY",
  },
];

export default function MultiCustomOpenAIWorkbench() {
  const [endpoints, setEndpoints] = useState<Endpoint[]>(starter);
  const [alias, setAlias] = useState("");
  const [model, setModel] = useState("openai/gpt-4o-mini");
  const [apiBase, setApiBase] = useState("https://");
  const [apiKeyEnv, setApiKeyEnv] = useState("OPENAI_KEY");

  function addEndpoint(e: FormEvent) {
    e.preventDefault();
    if (!alias.trim() || !model.trim() || !apiBase.trim() || !apiKeyEnv.trim()) return;

    const item: Endpoint = {
      id: crypto.randomUUID(),
      alias: alias.trim(),
      model: model.trim(),
      apiBase: apiBase.trim(),
      apiKeyEnv: apiKeyEnv.trim(),
    };

    setEndpoints((prev) => [item, ...prev]);
    setAlias("");
    setModel("openai/gpt-4o-mini");
    setApiBase("https://");
    setApiKeyEnv("OPENAI_KEY");
  }

  function removeEndpoint(id: string) {
    setEndpoints((prev) => prev.filter((ep) => ep.id !== id));
  }

  const yamlPreview = useMemo(() => {
    return endpoints
      .map(
        (ep) =>
          `  - model_name: ${ep.alias}\n    litellm_params:\n      model: ${ep.model}\n      api_base: ${ep.apiBase}\n      api_key: "\${${ep.apiKeyEnv}}"`,
      )
      .join("\n");
  }, [endpoints]);

  return (
    <section className="card">
      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "var(--space-lg)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-sm)" }}>
          <span className="seal seal-draft">END</span>
          <h2 style={{ fontFamily: "var(--font-display)", fontSize: "var(--fs-h2)", color: "var(--iig-text-strong)", lineHeight: "var(--lh-tight)" }}>
            Multi-CustomOpenAI Bastelkasten
          </h2>
        </div>
        <span className="badge">
          <span className="seal seal-info" style={{ inlineSize: "1.4rem", blockSize: "1.4rem", fontSize: "0.5rem" }}>GUI</span>
          Prototyp · lokal
        </span>
      </div>

      {/* Add Endpoint Form */}
      <form
        onSubmit={addEndpoint}
        className="card"
        style={{ background: "var(--iig-bg-canvas)", marginBottom: "var(--space-xl)" }}
      >
        <p className="meta-label" style={{ marginBottom: "var(--space-md)" }}>Neuen Endpoint hinzufügen</p>
        <div style={{ display: "grid", gap: "var(--space-md)", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
          <label style={{ display: "grid", gap: "var(--space-2xs)" }}>
            <span className="meta-label">Alias</span>
            <input
              value={alias}
              onChange={(e) => setAlias(e.target.value)}
              className="field"
              placeholder="team-assistant"
            />
          </label>

          <label style={{ display: "grid", gap: "var(--space-2xs)" }}>
            <span className="meta-label">Model</span>
            <input
              value={model}
              onChange={(e) => setModel(e.target.value)}
              className="field"
              placeholder="openai/gpt-4o-mini"
            />
          </label>

          <label style={{ display: "grid", gap: "var(--space-2xs)" }}>
            <span className="meta-label">API Base URL</span>
            <input
              value={apiBase}
              onChange={(e) => setApiBase(e.target.value)}
              className="field"
              placeholder="https://my-endpoint/v1"
            />
          </label>

          <label style={{ display: "grid", gap: "var(--space-2xs)" }}>
            <span className="meta-label">ENV Key Name</span>
            <input
              value={apiKeyEnv}
              onChange={(e) => setApiKeyEnv(e.target.value)}
              className="field"
              placeholder="OPENAI_KEY"
            />
          </label>
        </div>
        <div style={{ marginTop: "var(--space-lg)" }}>
          <button type="submit" className="btn btn-primary" style={{ width: "100%" }}>
            Endpoint hinzufügen
          </button>
        </div>
      </form>

      {/* Two-column: endpoints list + YAML preview */}
      <div style={{ display: "grid", gap: "var(--space-xl)", gridTemplateColumns: "1fr 1fr" }}>
        {/* Active Endpoints */}
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "var(--space-xs)", marginBottom: "var(--space-md)" }}>
            <span className="seal seal-proof" style={{ inlineSize: "1.6rem", blockSize: "1.6rem", fontSize: "0.55rem" }}>ACT</span>
            <h3 style={{ fontFamily: "var(--font-display)", fontSize: "var(--fs-h3)", color: "var(--iig-text-strong)" }}>
              Aktive Custom Endpoints
            </h3>
          </div>
          <div style={{ display: "grid", gap: "var(--space-sm)" }}>
            {endpoints.map((ep) => (
              <div key={ep.id} className="card" style={{ padding: "var(--space-md)", background: "var(--iig-bg-canvas)" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start" }}>
                  <div>
                    <p style={{ fontWeight: "var(--fw-body-strong)", color: "var(--iig-text-strong)" } as React.CSSProperties}>
                      {ep.alias}
                    </p>
                    <p className="body-copy" style={{ fontSize: "var(--fs-body-sm)" }}>{ep.model}</p>
                    <p style={{ fontSize: "var(--fs-meta)", color: "var(--iig-text-faint)", fontFamily: "var(--font-mono)" }}>
                      {ep.apiBase}
                    </p>
                  </div>
                  <button
                    onClick={() => removeEndpoint(ep.id)}
                    className="seal seal-risk"
                    style={{ inlineSize: "1.6rem", blockSize: "1.6rem", fontSize: "0.55rem", cursor: "pointer" }}
                    title="Endpoint entfernen"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
            {endpoints.length === 0 && (
              <p style={{ color: "var(--iig-text-faint)", fontStyle: "italic" }}>Keine Endpoints konfiguriert.</p>
            )}
          </div>
        </div>

        {/* YAML Preview */}
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "var(--space-xs)", marginBottom: "var(--space-md)" }}>
            <span className="seal seal-note" style={{ inlineSize: "1.6rem", blockSize: "1.6rem", fontSize: "0.55rem" }}>YML</span>
            <h3 style={{ fontFamily: "var(--font-display)", fontSize: "var(--fs-h3)", color: "var(--iig-text-strong)" }}>
              config.yaml Vorschau
            </h3>
          </div>
          <pre className="code-block" style={{ maxHeight: "320px", overflowY: "auto" }}>
{`model_list:\n${yamlPreview}`}
          </pre>
        </div>
      </div>
    </section>
  );
}
