# 🚪 GATES — ANVIL-BELLOWS

---

## 🔜 Nächste Gates

### Gate AB-011: Provider Health Dashboard
- **Branch:** `gate/ab-011-health-dashboard`
- **To-Dos:**
  - [ ] Echtzeit-Latenz pro Provider
  - [ ] Error-Rate-Tracking
  - [ ] Cooldown-Visualisierung
  - [ ] Auto-Failover-Logs
- **Akzeptanz:** Dashboard zeigt alle Provider-Metriken
- **Kill:** Dashboard ohne Echtzeit-Update

### Gate AB-012: Budget Alert System
- **Branch:** `gate/ab-012-budget-alerts`
- **To-Dos:**
  - [ ] Konfigurierbare Budget-Schwellen (50%, 80%, 100%)
  - [ ] Android-Notification bei Schwelle
  - [ ] Tages-/Wochen-/Monats-Budgets
  - [ ] Spend-Forecast
- **Akzeptanz:** Alerts bei konfigurierten Schwellen
- **Kill:** Alert erst bei 100%

### Gate AB-013: Agent Preset Library
- **Branch:** `gate/ab-013-agent-presets`
- **To-Dos:**
  - [ ] Preset-Format: System-Prompt + Model + Budget
  - [ ] Import/Export als JSON
  - [ ] Quick-Switch zwischen Presets
  - [ ] Preset-spezifisches Spend-Tracking
- **Akzeptanz:** Presets erstellbar und wechselbar
- **Kill:** Presets ohne Budget-Zuordnung

### Gate AB-014: Token Usage Analytics
- **Branch:** `gate/ab-014-analytics`
- **To-Dos:**
  - [ ] Token-Usage pro Model/Tag/Agent
  - [ ] Cost-per-Request Breakdown
  - [ ] Grafische Darstellung (Charts)
  - [ ] CSV-Export
- **Akzeptanz:** Analytics über 30 Tage korrekt
- **Kill:** Analytics ohne Export
