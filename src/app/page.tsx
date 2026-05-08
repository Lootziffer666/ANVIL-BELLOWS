"use client";

import { useState } from "react";
import MultiCustomOpenAIWorkbench from "@/components/MultiCustomOpenAIWorkbench";

const kpis = [
  { label: "Daily Spend",   value: "$1.84", detail: "37 % von $5.00 Limit", seal: "BDG", sealClass: "seal-note" },
  { label: "Requests/min",  value: "27",    detail: "Peak 41 @ 14:03 UTC",  seal: "REQ", sealClass: "seal-info" },
  { label: "Blocked",       value: "3",     detail: "Token cap > 8 192",    seal: "RSK", sealClass: "seal-risk" },
  { label: "Models",        value: "10",    detail: "Vertex + OpenRouter",  seal: "PRF", sealClass: "seal-proof" },
];

type NavItem = { id: string; seal: string; label: string; sealClass: string };
const navItems: NavItem[] = [
  { id: "dashboard", seal: "DSH", label: "Dashboard", sealClass: "" },
  { id: "endpoints", seal: "END", label: "Endpoints", sealClass: "" },
  { id: "quota",     seal: "QTA", label: "Quota",     sealClass: "seal-note" },
  { id: "agents",    seal: "AGT", label: "Agents",    sealClass: "seal-info" },
  { id: "settings",  seal: "CFG", label: "Settings",  sealClass: "" },
];

export default function Home() {
  const [theme, setTheme] = useState<"light" | "charcoal-room">("light");
  const [activeNav, setActiveNav] = useState("dashboard");

  function toggleTheme() {
    const next = theme === "light" ? "charcoal-room" : "light";
    setTheme(next);
    document.documentElement.setAttribute("data-theme", next);
  }

  return (
    <div className="app-shell surface-noise">
      {/* ── Topbar ──────────────────────────────────────────────── */}
      <header className="topbar">
        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-md)" }}>
          <span className="display-hero" style={{ fontSize: "var(--fs-h2)", lineHeight: 1 }}>
            ANVIL BELLOWS
          </span>
          <span className="meta-label" style={{ color: "var(--iig-accent-primary)" }}>
            CONTROL SURFACE
          </span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-md)" }}>
          <div className="badge">
            <span className="seal seal-proof" style={{ inlineSize: "1.6rem", blockSize: "1.6rem", fontSize: "0.6rem" }}>ON</span>
            <span style={{ color: "var(--iig-accent-success)", fontWeight: "var(--fw-body-strong)" } as React.CSSProperties}>Proxy online</span>
          </div>
          <button onClick={toggleTheme} className="btn btn-secondary" style={{ minHeight: "36px", padding: "0.5rem 0.85rem", fontSize: "var(--fs-meta)" }}>
            {theme === "light" ? "◐ Charcoal Room" : "◑ Warm Paper"}
          </button>
        </div>
      </header>

      {/* ── Rail ────────────────────────────────────────────────── */}
      <nav className="rail">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => setActiveNav(item.id)}
            className={`rail-item ${activeNav === item.id ? "rail-item--active" : ""}`}
          >
            <span className={`seal ${item.sealClass}`}>{item.seal}</span>
            <span className="rail-label">{item.label}</span>
          </button>
        ))}
      </nav>

      {/* ── Main Canvas ─────────────────────────────────────────── */}
      <main className="main-canvas">
        {/* Hero Section */}
        <section className="card card--raised" style={{ display: "grid", gap: "var(--space-lg)", gridTemplateColumns: "1.3fr 1fr" }}>
          <div>
            <p className="meta-label" style={{ marginBottom: "var(--space-sm)" }}>
              ANVIL BELLOWS / RUNTIME GUI
            </p>
            <h1 className="section-title" style={{ marginBottom: "var(--space-md)" }}>
              Multi-CustomOpenAI Toolkit
            </h1>
            <p className="body-copy" style={{ maxWidth: "var(--content-narrow)" }}>
              Nicht nur Mockup: unten findest du den Bastelkasten, um mehrere Custom-OpenAI-Endpunkte zusammenzustellen
              und direkt als <code style={{ fontFamily: "var(--font-mono)", background: "var(--iig-bg-surface)", padding: "0.15em 0.35em", borderRadius: "var(--radius-sm)" }}>config.yaml</code>-Block vorzubereiten.
            </p>
          </div>
          <div className="card" style={{ background: "var(--iig-bg-canvas)" }}>
            <p className="meta-label" style={{ marginBottom: "var(--space-sm)" }}>System State</p>
            <div style={{ display: "grid", gap: "var(--space-xs)" }}>
              <p className="body-copy">
                Proxy: <span className="state-success" style={{ fontWeight: "var(--fw-body-strong)" } as React.CSSProperties}>online</span>
              </p>
              <p className="body-copy">
                Mode: <span style={{ color: "var(--iig-accent-info)", fontWeight: "var(--fw-body-strong)" } as React.CSSProperties}>budget-guarded</span>
              </p>
              <p className="body-copy">
                Policy: <span className="state-warning" style={{ fontWeight: "var(--fw-body-strong)" } as React.CSSProperties}>80 % warn / 100 % block</span>
              </p>
            </div>
          </div>
        </section>

        {/* KPI Grid */}
        <div className="kpi-grid">
          {kpis.map((kpi) => (
            <article key={kpi.label} className="kpi-box">
              <div style={{ display: "flex", alignItems: "center", gap: "var(--space-xs)", marginBottom: "var(--space-xs)" }}>
                <span className={`seal ${kpi.sealClass}`} style={{ inlineSize: "1.8rem", blockSize: "1.8rem", fontSize: "0.6rem" }}>
                  {kpi.seal}
                </span>
                <span className="meta-label">{kpi.label}</span>
              </div>
              <p className="kpi-value">{kpi.value}</p>
              <p className="kpi-detail">{kpi.detail}</p>
            </article>
          ))}
        </div>

        {/* Workbench */}
        <MultiCustomOpenAIWorkbench />
      </main>

      {/* ── Annotation Sidebar ──────────────────────────────────── */}
      <aside className="annotation-sidebar">
        <div>
          <p className="meta-label" style={{ marginBottom: "var(--space-sm)" }}>Hinweise</p>
        </div>

        <div className="annotation">
          <div style={{ display: "flex", alignItems: "center", gap: "var(--space-xs)", marginBottom: "var(--space-xs)" }}>
            <span className="seal seal-note" style={{ inlineSize: "1.6rem", blockSize: "1.6rem", fontSize: "0.55rem" }}>NTE</span>
          </div>
          <p>Diese Oberfläche ist ein lokales Werkzeug. API-Keys werden niemals an Dritte übertragen.</p>
        </div>

        <div className="annotation">
          <div style={{ display: "flex", alignItems: "center", gap: "var(--space-xs)", marginBottom: "var(--space-xs)" }}>
            <span className="seal seal-info" style={{ inlineSize: "1.6rem", blockSize: "1.6rem", fontSize: "0.55rem" }}>INF</span>
          </div>
          <p>Der Proxy läuft auf Port 4141 und ist standardmäßig nur lokal erreichbar.</p>
        </div>

        <div className="annotation">
          <div style={{ display: "flex", alignItems: "center", gap: "var(--space-xs)", marginBottom: "var(--space-xs)" }}>
            <span className="seal seal-risk" style={{ inlineSize: "1.6rem", blockSize: "1.6rem", fontSize: "0.55rem" }}>RSK</span>
          </div>
          <p>Budget-Limiter greift bei 100 % Verbrauch. Blockierte Requests werden geloggt.</p>
        </div>

        <div className="annotation">
          <div style={{ display: "flex", alignItems: "center", gap: "var(--space-xs)", marginBottom: "var(--space-xs)" }}>
            <span className="seal seal-proof" style={{ inlineSize: "1.6rem", blockSize: "1.6rem", fontSize: "0.55rem" }}>PRF</span>
          </div>
          <p>Alle Provider-Konfigurationen lassen sich als config.yaml exportieren und versionieren.</p>
        </div>
      </aside>
    </div>
  );
}
