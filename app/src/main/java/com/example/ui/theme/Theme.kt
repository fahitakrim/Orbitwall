package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AuraWallColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color(0xFF1E1035),
    primaryContainer = VioletContainer,
    onPrimaryContainer = OnVioletContainer,
    secondary = CyanAccent,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = AmberAccent,
    background = AmoledBackground,
    onBackground = TextPrimaryDark,
    surface = AmoledSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = AuraWallColorScheme,
        typography = Typography,
        content = content
    )
}
