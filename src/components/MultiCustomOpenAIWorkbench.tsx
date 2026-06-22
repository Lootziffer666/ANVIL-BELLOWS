"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";

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

// ────────────────────────────────────────────────────────────────────────────
// DATENSICHERHEIT — key-freie, niemals-zerstörende Persistenz
//
// Lehre aus dem Keystore-Vorfall: NIE einen vorhandenen Stand blind
// überschreiben. Diese WebView speichert deshalb:
//   1. NIEMALS echte API-Keys — ausschließlich ENV-Variablen-NAMEN
//      (z. B. "OPENAI_KEY"). Die Geheimnisse leben in der .env des Proxys.
//   2. Vor jedem Schreiben wird der vorherige Stand in einen Backup-Slot
//      kopiert (1 Slot, jederzeit per Button wiederherstellbar).
//   3. Beim Laden: Haupt-Slot → bei Defekt/Leere Backup-Slot → sonst Starter.
//      Ein kaputter localStorage-Eintrag löscht also nie die Konfiguration.
// ────────────────────────────────────────────────────────────────────────────
const STORAGE_KEY = "anvil-bellows.endpoints.v1";
const BACKUP_KEY = "anvil-bellows.endpoints.v1.bak";

/** Whitelist: nur diese (nicht-geheimen) Felder werden je persistiert. */
function sanitize(list: Endpoint[]): Endpoint[] {
  return list
    .filter((ep) => ep && typeof ep === "object")
    .map((ep) => ({
      id: String(ep.id ?? crypto.randomUUID()),
      alias: String(ep.alias ?? ""),
      model: String(ep.model ?? ""),
      apiBase: String(ep.apiBase ?? ""),
      // apiKeyEnv ist ein ENV-NAME, kein Geheimnis. Sicherheitshalber wird ein
      // versehentlich eingetippter Key-artiger Wert hier NICHT gespeichert.
      apiKeyEnv: looksLikeSecret(String(ep.apiKeyEnv ?? "")) ? "" : String(ep.apiKeyEnv ?? ""),
    }));
}

/** Heuristik: sieht der Wert wie ein echter Secret-Key statt eines ENV-Namens aus? */
function looksLikeSecret(v: string): boolean {
  const s = v.trim();
  if (!s) return false;
  // Typische Key-Präfixe oder lange zufällige Strings → niemals persistieren.
  if (/^(sk-|sk_|pk-|rk-|AIza|ghp_|xoxb-)/.test(s)) return true;
  if (s.length >= 32 && /[a-z]/.test(s) && /[0-9]/.test(s) && !/\s/.test(s) && !/^[A-Z0-9_]+$/.test(s)) return true;
  return false;
}

function readSlot(key: string): Endpoint[] | null {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return null;
    return sanitize(parsed as Endpoint[]);
  } catch {
    return null; // defekter Eintrag → behandeln wie "nicht vorhanden", nie löschen
  }
}

function loadEndpoints(): Endpoint[] {
  const main = readSlot(STORAGE_KEY);
  if (main && main.length) return main;
  const backup = readSlot(BACKUP_KEY);
  if (backup && backup.length) return backup;
  return starter;
}

/**
 * Schreibt die Konfiguration sicher zurück:
 *  - sichert den vorherigen (nicht-leeren) Stand in den Backup-Slot,
 *  - schreibt dann den neuen Stand (key-bereinigt) in den Haupt-Slot.
 * Gibt zurück, ob danach ein Backup zum Wiederherstellen existiert.
 */
function persist(next: Endpoint[]): boolean {
  try {
    const prev = localStorage.getItem(STORAGE_KEY);
    if (prev && prev !== "[]") localStorage.setItem(BACKUP_KEY, prev);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sanitize(next)));
    return Boolean(readSlot(BACKUP_KEY)?.length);
  } catch {
    return false; // localStorage nicht verfügbar (z. B. privater Modus)
  }
}

export default function MultiCustomOpenAIWorkbench() {
  const [endpoints, setEndpoints] = useState<Endpoint[]>(starter);
  const [alias, setAlias] = useState("");
  const [model, setModel] = useState("openai/gpt-4o-mini");
  const [apiBase, setApiBase] = useState("https://");
  const [apiKeyEnv, setApiKeyEnv] = useState("OPENAI_KEY");
  const [restorable, setRestorable] = useState(false);
  const [statusMsg, setStatusMsg] = useState<string>("");

  // Erstes Laden aus dem sicheren Speicher. Bewusst im Effect (nicht als
  // useState-Initializer), damit Server- und Client-Render identisch mit
  // `starter` starten und es keinen Hydration-Mismatch gibt. Das einmalige
  // setState hier ist der dokumentierte Ausnahmefall „aus externem Storage laden“.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- einmaliges Laden aus localStorage (Hydration-sicher)
    setEndpoints(loadEndpoints());
    setRestorable(Boolean(readSlot(BACKUP_KEY)?.length));
  }, []);

  /** Setzt neuen Stand UND persistiert ihn (Persistenz im Event-Handler, nicht im Effect). */
  function commit(next: Endpoint[]) {
    setEndpoints(next);
    setRestorable(persist(next));
  }

  function restoreBackup() {
    const backup = readSlot(BACKUP_KEY);
    if (backup && backup.length) {
      setEndpoints(backup);
      // Haupt-Slot auf den wiederhergestellten Stand setzen, Backup unangetastet lassen.
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(sanitize(backup)));
      } catch {
        /* ignore */
      }
      setStatusMsg(`Vorheriger Stand wiederhergestellt (${backup.length} Endpoints).`);
      setTimeout(() => setStatusMsg(""), 2600);
    } else {
      setStatusMsg("Kein Backup vorhanden.");
      setTimeout(() => setStatusMsg(""), 2600);
    }
  }

  function addEndpoint(e: FormEvent) {
    e.preventDefault();
    if (!alias.trim() || !model.trim() || !apiBase.trim() || !apiKeyEnv.trim()) return;

    if (looksLikeSecret(apiKeyEnv)) {
      setStatusMsg("Das sieht nach einem echten Key aus. Hier bitte nur den ENV-NAMEN (z. B. OPENAI_KEY) eintragen — der Key gehört in die .env.");
      setTimeout(() => setStatusMsg(""), 4200);
      return;
    }

    const item: Endpoint = {
      id: crypto.randomUUID(),
      alias: alias.trim(),
      model: model.trim(),
      apiBase: apiBase.trim(),
      apiKeyEnv: apiKeyEnv.trim(),
    };

    commit([item, ...endpoints]);
    setAlias("");
    setModel("openai/gpt-4o-mini");
    setApiBase("https://");
    setApiKeyEnv("OPENAI_KEY");
  }

  function removeEndpoint(id: string) {
    commit(endpoints.filter((ep) => ep.id !== id));
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
        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-sm)" }}>
          {restorable && (
            <button
              type="button"
              className="btn btn-secondary"
              onClick={restoreBackup}
              title="Vorherigen gespeicherten Stand wiederherstellen"
              style={{ minHeight: "34px", padding: "0.4rem 0.7rem", fontSize: "var(--fs-meta)" }}
            >
              ↩ Wiederherstellen
            </button>
          )}
          <span className="badge">
            <span className="seal seal-info" style={{ inlineSize: "1.4rem", blockSize: "1.4rem", fontSize: "0.5rem" }}>GUI</span>
            Prototyp · lokal
          </span>
        </div>
      </div>

      {/* Datensicherheits-Hinweis */}
      <div
        className="card"
        style={{ background: "var(--iig-bg-canvas)", marginBottom: "var(--space-lg)", borderLeft: "3px solid var(--iig-accent-success)" }}
      >
        <p className="body-copy" style={{ fontSize: "var(--fs-body-sm)", margin: 0 }}>
          <strong>Keys werden hier nie gespeichert.</strong> Diese Oberfläche merkt sich nur deine
          Endpoint-Konfiguration und die <em>ENV-Variablennamen</em> (z. B. <code style={{ fontFamily: "var(--font-mono)" }}>OPENAI_KEY</code>) —
          lokal im Browser, mit automatischem Backup. Die echten Schlüssel gehören in die
          <code style={{ fontFamily: "var(--font-mono)" }}> .env</code> des Proxys und werden von keinem Neustart überschrieben.
        </p>
        {statusMsg && (
          <p className="body-copy" style={{ fontSize: "var(--fs-body-sm)", marginTop: "var(--space-xs)", color: "var(--iig-accent-info)" }}>
            {statusMsg}
          </p>
        )}
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
            <span className="meta-label">ENV Key Name (kein Key!)</span>
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
