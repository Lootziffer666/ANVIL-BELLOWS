package com.anvil.bellows.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.anvil.bellows.R

// ═══════════════════════════════════════════════════════════
//  IIG Design System — Typography
//
//  Typeface stack:
//  · Libre Baskerville — Display / Headline  (editorial serif, "Ink on Paper")
//  · DM Sans           — Body / UI           (clean warm sans, everyday legibility)
//  · JetBrains Mono    — Technical           (API keys, tokens, model IDs, code)
//
//  Fonts delivered via Google Fonts Compose API (downloaded on first use,
//  cached by the system; no bundled binaries required).
// ═══════════════════════════════════════════════════════════

private val fontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs   // provided by ui-text-google-fonts
)

// ── Font Families ──────────────────────────────────────────

/** Editorial serif — display headings, screen titles, section leaders */
val IigSerif: FontFamily = FontFamily(
    Font(GoogleFont("Libre Baskerville"), fontsProvider, weight = FontWeight.Normal),
    Font(GoogleFont("Libre Baskerville"), fontsProvider, weight = FontWeight.Bold)
)

/** Warm sans-serif — all UI text, body copy, labels */
val IigSans: FontFamily = FontFamily(
    Font(GoogleFont("DM Sans"), fontsProvider, weight = FontWeight.Normal),
    Font(GoogleFont("DM Sans"), fontsProvider, weight = FontWeight.Medium),
    Font(GoogleFont("DM Sans"), fontsProvider, weight = FontWeight.SemiBold),
    Font(GoogleFont("DM Sans"), fontsProvider, weight = FontWeight.Bold)
)

/** Developer monospace — API keys, bearer tokens, model identifiers */
val IigMono: FontFamily = FontFamily(
    Font(GoogleFont("JetBrains Mono"), fontsProvider, weight = FontWeight.Normal),
    Font(GoogleFont("JetBrains Mono"), fontsProvider, weight = FontWeight.Medium)
)

// ═══════════════════════════════════════════════════════════
//  Material 3 Type Scale
//
//  Roles:
//  displayLarge  / displayMedium  — hero text, onboarding, splash
//  displaySmall  / headlineLarge  — screen-level titles
//  headlineMedium / headlineSmall — section headings
//  titleLarge    / titleMedium    — card headings, top-bar titles
//  titleSmall                     — sub-headings, dense lists
//  bodyLarge     / bodyMedium     — primary reading content
//  bodySmall                      — secondary info, helper text
//  labelLarge    / labelMedium    — button text, chips
//  labelSmall                     — captions, metadata, badge text
// ═══════════════════════════════════════════════════════════

val IigTypography = Typography(

    // ── Display (large hero text) ─────────────────────────
    displayLarge = TextStyle(
        fontFamily    = IigSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 57.sp,
        lineHeight    = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily    = IigSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 45.sp,
        lineHeight    = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily    = IigSerif,
        fontWeight    = FontWeight.Normal,
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = 0.sp
    ),

    // ── Headline (section / screen titles) ────────────────
    headlineLarge = TextStyle(
        fontFamily    = IigSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = IigSerif,
        fontWeight    = FontWeight.Normal,
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = 0.sp
    ),

    // ── Title (card headings, top-bar) ────────────────────
    titleLarge = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ── Body (reading content) ────────────────────────────
    bodyLarge = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ── Label (buttons, chips, captions) ─────────────────
    labelLarge = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = IigSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    ),
)
