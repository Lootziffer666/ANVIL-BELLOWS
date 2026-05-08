package com.anvil.bellows.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════
//  IIG Design System — Material3 Theme Mapping
//  Light = "Warm Paper"   |   Dark = "Charcoal Room"
// ═══════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    primary            = OxidRedHover,        // Glühender auf Anthrazit
    onPrimary          = CharcoalTextStrong,
    primaryContainer   = OxidRedSoftDark,
    onPrimaryContainer = CharcoalTextStrong,
    secondary          = AmberDark,
    onSecondary        = CharcoalCanvas,
    secondaryContainer = AmberSoftDark,
    onSecondaryContainer = CharcoalTextStrong,
    tertiary           = ClayDark,
    onTertiary         = CharcoalCanvas,
    background         = CharcoalCanvas,
    onBackground       = CharcoalTextBody,
    surface            = CharcoalSurface,
    onSurface          = CharcoalTextBody,
    surfaceVariant     = CharcoalSurfaceRaised,
    onSurfaceVariant   = CharcoalTextSoft,
    outline            = BorderMediumDark,
    outlineVariant     = BorderSubtleDark,
    error              = OxidRedHover,
    onError            = CharcoalTextStrong,
    inverseSurface     = WarmPaperCanvas,
    inverseOnSurface   = WarmPaperTextStrong,
)

private val LightColorScheme = lightColorScheme(
    primary            = OxidRed,
    onPrimary          = WarmPaperCanvas,
    primaryContainer   = OxidRedSoft,
    onPrimaryContainer = WarmPaperTextStrong,
    secondary          = Amber,
    onSecondary        = WarmPaperCanvas,
    secondaryContainer = AmberSoft,
    onSecondaryContainer = WarmPaperTextStrong,
    tertiary           = Clay,
    onTertiary         = WarmPaperCanvas,
    background         = WarmPaperCanvas,
    onBackground       = WarmPaperTextBody,
    surface            = WarmPaperSurface,
    onSurface          = WarmPaperTextBody,
    surfaceVariant     = WarmPaperSurfaceRaised,
    onSurfaceVariant   = WarmPaperTextSoft,
    outline            = BorderMediumLight,
    outlineVariant     = BorderSubtleLight,
    error              = OxidRed,
    onError            = WarmPaperCanvas,
    inverseSurface     = CharcoalCanvas,
    inverseOnSurface   = CharcoalTextStrong,
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
        shapes = Shapes(
            small  = RoundedCornerShape(8.dp),   // --radius-sm
            medium = RoundedCornerShape(14.dp),   // --radius-md
            large  = RoundedCornerShape(22.dp),   // --radius-lg
        ),
        content = content
    )
}
