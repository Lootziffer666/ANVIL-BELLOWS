package com.anvil.bellows.presentation.theme

import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════
//  IIG Design System — Spacing Tokens
//
//  Base unit: 4dp  (one "glyph slot" on a 4-column sub-grid)
//
//  Usage:
//    Modifier.padding(IigSpacing.lg)          // 16dp card padding
//    Modifier.size(IigSpacing.xl)             // 20dp icon container
//    Arrangement.spacedBy(IigSpacing.sm)      // 8dp list gap
//
//  Avoid raw dp literals in composables — prefer these tokens
//  so spacing stays consistent across the whole app.
// ═══════════════════════════════════════════════════════════

object IigSpacing {
    /** 2dp — hairline gap; icon internal padding, thin dividers */
    val micro   = 2.dp

    /** 4dp — xs; tight chip padding, badge margin */
    val xs      = 4.dp

    /** 8dp — sm; icon-to-label gap, chip horizontal padding */
    val sm      = 8.dp

    /** 12dp — md; row gap inside cards, dense form fields */
    val md      = 12.dp

    /** 16dp — lg; standard horizontal screen margin, card padding */
    val lg      = 16.dp

    /** 20dp — xl; wide card padding, comfortable list item height padding */
    val xl      = 20.dp

    /** 24dp — 2xl; section separator, dialog content padding */
    val xxl     = 24.dp

    /** 32dp — 3xl; prominent section gap, large dialog padding */
    val xxxl    = 32.dp

    /** 48dp — section; top-of-screen section breathing room */
    val section = 48.dp

    /** 64dp — screen; hero / splash area top padding */
    val screen  = 64.dp
}

// ═══════════════════════════════════════════════════════════
//  IIG Elevation Tokens
//
//  Material 3 uses "tonal elevation" (colour overlay) rather
//  than drop shadows for most surfaces, so these values are
//  small.  The tokens name the semantic layer — use them
//  in CardDefaults.cardElevation(defaultElevation = IigElevation.card).
// ═══════════════════════════════════════════════════════════

object IigElevation {
    /** 0dp — flat surfaces: background, list separators */
    val flat    = 0.dp

    /** 1dp — subtly raised: list item hover, inline chips */
    val raised  = 1.dp

    /** 2dp — standard card elevation */
    val card    = 2.dp

    /** 3dp — navigation bars, FABs */
    val nav     = 3.dp

    /** 4dp — menus, autocomplete popups */
    val overlay = 4.dp

    /** 8dp — bottom sheets, side sheets */
    val sheet   = 8.dp

    /** 12dp — dialogs */
    val dialog  = 12.dp
}
