package com.anvil.bellows.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════
//  IIG Design System — Material3 Theme Mapping
//  Light = "Warm Paper"   |   Dark = "Charcoal Room"
// ═══════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    primary              = OxidRedHover,          // Glowing on Anthracite
    onPrimary            = CharcoalTextStrong,
    primaryContainer     = OxidRedSoftDark,
    onPrimaryContainer   = CharcoalTextStrong,
    secondary            = AmberDark,
    onSecondary          = CharcoalCanvas,
    secondaryContainer   = AmberSoftDark,
    onSecondaryContainer = CharcoalTextStrong,
    tertiary             = ClayDark,
    onTertiary           = CharcoalCanvas,
    background           = CharcoalCanvas,
    onBackground         = CharcoalTextBody,
    surface              = CharcoalSurface,
    onSurface            = CharcoalTextBody,
    surfaceVariant       = CharcoalSurfaceRaised,
    onSurfaceVariant     = CharcoalTextSoft,
    outline              = BorderMediumDark,
    outlineVariant       = BorderSubtleDark,
    error                = OxidRedHover,
    onError              = CharcoalTextStrong,
    inverseSurface       = WarmPaperCanvas,
    inverseOnSurface     = WarmPaperTextStrong,
)

private val LightColorScheme = lightColorScheme(
    primary              = OxidRed,
    onPrimary            = WarmPaperCanvas,
    primaryContainer     = OxidRedSoft,
    onPrimaryContainer   = WarmPaperTextStrong,
    secondary            = Amber,
    onSecondary          = WarmPaperCanvas,
    secondaryContainer   = AmberSoft,
    onSecondaryContainer = WarmPaperTextStrong,
    tertiary             = Clay,
    onTertiary           = WarmPaperCanvas,
    background           = WarmPaperCanvas,
    onBackground         = WarmPaperTextBody,
    surface              = WarmPaperSurface,
    onSurface            = WarmPaperTextBody,
    surfaceVariant       = WarmPaperSurfaceRaised,
    onSurfaceVariant     = WarmPaperTextSoft,
    outline              = BorderMediumLight,
    outlineVariant       = BorderSubtleLight,
    error                = OxidRed,
    onError              = WarmPaperCanvas,
    inverseSurface       = CharcoalCanvas,
    inverseOnSurface     = CharcoalTextStrong,
)

// ═══════════════════════════════════════════════════════════
//  IIG Shape System
//  small  8dp  — chips, badges, small buttons, text fields
//  medium 14dp — cards, containers, list tiles
//  large  22dp — bottom sheets, dialogs, large cards
//  (extra-large = full circle is handled per-component with CircleShape)
// ═══════════════════════════════════════════════════════════

val IigShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // --radius-xs  (tooltip, snackbar)
    small      = RoundedCornerShape(8.dp),   // --radius-sm  (chip, badge, FAB)
    medium     = RoundedCornerShape(14.dp),  // --radius-md  (card, container)
    large      = RoundedCornerShape(22.dp),  // --radius-lg  (bottom sheet, dialog)
    extraLarge = RoundedCornerShape(28.dp),  // --radius-xl  (full-bleed sheet)
)

@Composable
fun AnvilBellowsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = IigTypography,    // IIG type scale (Type.kt)
        shapes      = IigShapes,        // IIG corner radii (above)
        content     = content
    )
}
