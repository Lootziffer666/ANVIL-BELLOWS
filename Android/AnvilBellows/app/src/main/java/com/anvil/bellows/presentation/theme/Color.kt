package com.anvil.bellows.presentation.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════
//  IIG Design System — Ink & Iron Glow
//  "Warm Paper" (Light) + "Charcoal Room" (Dark)
// ═══════════════════════════════════════════════════════════

// ── Warm Paper (Light Mode) ────────────────────────────────
val WarmPaperCanvas         = Color(0xFFF6F2EB)
val WarmPaperPaper          = Color(0xFFF4F0EA)
val WarmPaperSurface        = Color(0xFFE8DFD0)
val WarmPaperSurfaceRaised  = Color(0xFFF8F4EE)
val WarmPaperPanelTint      = Color(0xFFF1E8DC)

val WarmPaperTextStrong     = Color(0xFF2A2A2A)
val WarmPaperTextBody       = Color(0xFF3B3B3B)
val WarmPaperTextSoft       = Color(0xFF6B6258)
val WarmPaperTextFaint      = Color(0xFF8A7D70)

// ── Charcoal Room (Dark Mode) ──────────────────────────────
val CharcoalCanvas          = Color(0xFF1A1D21)
val CharcoalPaper           = Color(0xFF15171A)
val CharcoalSurface         = Color(0xFF24272B)
val CharcoalSurfaceRaised   = Color(0xFF2C3035)
val CharcoalPanelTint       = Color(0xFF211D1A)

val CharcoalTextStrong      = Color(0xFFF2E9DC)
val CharcoalTextBody        = Color(0xFFD8CCBB)
val CharcoalTextSoft        = Color(0xFFB7AA98)
val CharcoalTextFaint       = Color(0xFF988C7D)

// ── Accent Colors ──────────────────────────────────────────
val OxidRed                 = Color(0xFF8F1D1D)  // Primary action
val OxidRedHover            = Color(0xFFB4362E)  // Primary hover / Dark primary
val OxidRedSoft             = Color(0xFFE8C9C5)  // Primary container (light)
val OxidRedSoftDark         = Color(0x2EB4362E)  // Primary container (dark, 18% alpha)

val Amber                   = Color(0xFFC89A3C)  // Focus / Highlight
val AmberSoft               = Color(0xFFE9D5A6)
val AmberDark               = Color(0xFFD2A650)
val AmberSoftDark           = Color(0x2ED2A650)

val Honey                   = Color(0xFFA8741F)  // Warning
val HoneyDark               = Color(0xFFB9852D)

val Clay                    = Color(0xFFB35A3C)  // Draft / Tertiary
val ClayDark                = Color(0xFFC07555)

val IigInfo                 = Color(0xFF365F73)  // Info
val IigInfoDark             = Color(0xFF6D93A3)

val IigSuccess              = Color(0xFF3F6B42)  // Success
val IigSuccessDark          = Color(0xFF6D916F)

// ── Borders ────────────────────────────────────────────────
val BorderSubtleLight       = Color(0xFFD7CAB8)
val BorderMediumLight       = Color(0xFFBBAE9E)
val BorderStrongLight       = Color(0xFF6E6255)

val BorderSubtleDark        = Color(0x2EF6F2EB)  // 18% alpha
val BorderMediumDark        = Color(0x4DF6F2EB)  // 30% alpha
val BorderStrongDark        = Color(0x7AF6F2EB)  // 48% alpha

// ── Seal Colors ────────────────────────────────────────────
val SealBgLight             = Color(0xFFF4F0EA)
val SealLineLight           = Color(0xFF8F1D1D)

val SealBgDark              = Color(0xFF201C19)
val SealLineDark            = Color(0xFFD2A650)

// ── Tier color indicators (preserved from original) ────────
val TierOneColor   = IigSuccess
val TierTwoColor   = IigInfo
val TierThreeColor = Honey
val TierFourColor  = WarmPaperTextSoft

fun tierColor(tier: Int) = when (tier) {
    1 -> TierOneColor
    2 -> TierTwoColor
    3 -> TierThreeColor
    else -> TierFourColor
}
