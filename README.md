# ANVIL-BELLOWS

## Purpose

Anvil Bellows is an LLM Router & Multi-CustomOpenAI Toolkit with budget guarding, quota management, and agent presets. It includes an Android app and a Next.js control surface GUI.

## Design System

The GUI uses the **Ink & Iron Glow (IIG) Design System** with two material themes:

- **Warm Paper** (Light) — Cream, sand, warm ink. Oxidrot primary, Amber focus.
- **Charcoal Room** (Dark) — Deep anthracite, glowing red, metallic amber. Non-linear material shift (not a simple inversion).

Key principles:
- Editorial grid layout with Rail, Topbar, Main Canvas, Annotation Sidebar
- Three font families: Bebas Neue (display), Source Sans 3 (body), Merriweather (annotations)
- Semantic Seal system instead of generic icons
- Permanent annotation margins instead of hidden tooltips
- Surface noise texture for tactile paper feel

## Current Status

Active development. Android app + Next.js GUI both use IIG design tokens.

## Architecture

```
Android/AnvilBellows/   — Android app (Kotlin, Compose, Material3, IIG theme)
src/                    — Next.js control surface GUI (IIG Design System)
anvil-bellows/          — Python proxy + budget guard
```

## Related Repositories

- [ANVIL](https://github.com/Lootziffer666/ANVIL) — Core framework
