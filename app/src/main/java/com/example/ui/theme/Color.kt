package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Centralized Design Tokens for Thal University Bhakkar App.
 * Light and dark palette values live exclusively in this file.
 */
data class AppColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val infoLab: Color,
    val infoLabSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warning: Color,
    val warningSoft: Color
)

// STEP 2: LIGHT THEME TOKENS
val LightAppColors = AppColors(
    bg = Color(0xFFF4F5F2),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF1F3EE),
    line = Color(0xFFE5E7EB),
    text = Color(0xFF111827),
    muted = Color(0xFF6B7280),
    primary = Color(0xFF8CC63F),
    onPrimary = Color(0xFF111827),
    primarySoft = Color(0xFFE6F4CC),
    accent = Color(0xFF8CC63F),
    onAccent = Color(0xFF111827),
    accentText = Color(0xFF3F6A12),
    accentSoft = Color(0xFFE6F4CC),
    success = Color(0xFF3F6A12),
    successSoft = Color(0xFFE6F4CC),
    infoLab = Color(0xFF0F766E),
    infoLabSoft = Color(0xFFE0F4F1),
    danger = Color(0xFFE5484D),
    dangerSoft = Color(0xFFFDECEC),
    warning = Color(0xFFB45309),
    warningSoft = Color(0xFFFEF3C7)
)

// STEP 3: DARK THEME TOKENS (follow system setting)
val DarkAppColors = AppColors(
    bg = Color(0xFF0E110C),
    surface = Color(0xFF171B14),
    surface2 = Color(0xFF1F241B),
    line = Color(0xFF2B3126),
    text = Color(0xFFF3F4F1),
    muted = Color(0xFF9CA3AF),
    primary = Color(0xFF8CC63F),
    onPrimary = Color(0xFF0E110C),
    primarySoft = Color(0xFF2A3A17),
    accent = Color(0xFF8CC63F),
    onAccent = Color(0xFF0E110C),
    accentText = Color(0xFFB7E37A),
    accentSoft = Color(0xFF2A3A17),
    success = Color(0xFFB7E37A),
    successSoft = Color(0xFF1B2B11),
    infoLab = Color(0xFF2DD4BF),
    infoLabSoft = Color(0xFF0F2B2A),
    danger = Color(0xFFE5484D),
    dangerSoft = Color(0xFF2E1618),
    warning = Color(0xFFFBBF24),
    warningSoft = Color(0xFF2B2410)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

// Backward-compatible semantic getters mapping legacy names to central design tokens
val ColorBackgroundLight: Color @Composable get() = AppTheme.colors.bg
val ColorTextPrimary: Color @Composable get() = AppTheme.colors.text
val ColorTextSecondary: Color @Composable get() = AppTheme.colors.muted
val ColorPrimary: Color @Composable get() = AppTheme.colors.primary
val ColorPrimaryDark: Color @Composable get() = AppTheme.colors.accentText
val ColorButtonText: Color @Composable get() = AppTheme.colors.onPrimary
val PaletteShade1: Color @Composable get() = AppTheme.colors.surface2
val PaletteShade2: Color @Composable get() = AppTheme.colors.primarySoft
val PaletteShade3: Color @Composable get() = AppTheme.colors.surface2
val PaletteShade4: Color @Composable get() = AppTheme.colors.line
val PaletteShade5: Color @Composable get() = AppTheme.colors.muted
val PaletteShade6: Color @Composable get() = AppTheme.colors.primary
val PaletteShade7: Color @Composable get() = AppTheme.colors.primary
val ColorSuccess: Color @Composable get() = AppTheme.colors.success
