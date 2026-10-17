package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode {
    DARK,
    LIGHT
}

val NothingDarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    secondary = NothingBlue,
    onSecondary = Color.White,
    background = NothingDarkBackground,
    onBackground = NothingDarkTextPrimary,
    surface = NothingDarkSurface,
    onSurface = NothingDarkTextPrimary,
    surfaceVariant = NothingDarkPill,
    onSurfaceVariant = NothingDarkTextSecondary,
    outline = NothingDarkBorder
)

val NothingLightColorScheme = lightColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    secondary = NothingBlue,
    onSecondary = Color.White,
    background = NothingLightBackground,
    onBackground = NothingLightTextPrimary,
    surface = NothingLightSurface,
    onSurface = NothingLightTextPrimary,
    surfaceVariant = NothingLightPill,
    onSurfaceVariant = NothingLightTextSecondary,
    outline = NothingLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NothingDarkColorScheme else NothingLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                // In dark theme, status bar icons should be light (isAppearanceLightStatusBars = false)
                // In light theme, status bar icons should be dark (isAppearanceLightStatusBars = true)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
