package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun TUTimetableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.onPrimary,
            primaryContainer = appColors.primarySoft,
            onPrimaryContainer = appColors.text,
            secondary = appColors.accent,
            onSecondary = appColors.onAccent,
            secondaryContainer = appColors.accentSoft,
            onSecondaryContainer = appColors.accentText,
            tertiary = appColors.success,
            onTertiary = appColors.surface,
            background = appColors.bg,
            onBackground = appColors.text,
            surface = appColors.surface,
            onSurface = appColors.text,
            surfaceVariant = appColors.surface2,
            onSurfaceVariant = appColors.muted,
            outline = appColors.line,
            outlineVariant = appColors.line,
            error = appColors.danger,
            onError = appColors.surface
        )
    } else {
        lightColorScheme(
            primary = appColors.primary,
            onPrimary = appColors.onPrimary,
            primaryContainer = appColors.primarySoft,
            onPrimaryContainer = appColors.text,
            secondary = appColors.accent,
            onSecondary = appColors.onAccent,
            secondaryContainer = appColors.accentSoft,
            onSecondaryContainer = appColors.accentText,
            tertiary = appColors.success,
            onTertiary = appColors.surface,
            background = appColors.bg,
            onBackground = appColors.text,
            surface = appColors.surface,
            onSurface = appColors.text,
            surfaceVariant = appColors.surface2,
            onSurfaceVariant = appColors.muted,
            outline = appColors.line,
            outlineVariant = appColors.line,
            error = appColors.danger,
            onError = appColors.surface
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = appColors.bg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = appColors.accent,
        backgroundColor = appColors.primarySoft
    )

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalTextSelectionColors provides customTextSelectionColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
