package com.safenet.vpn.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── SafeNet Color Palette ─────────────────────────────────────────────────────

private val SafeNetBlue = Color(0xFF0EA5E9)       // Primary
private val SafeNetBlueDeep = Color(0xFF0369A1)
private val SafeNetBlueDark = Color(0xFF0C4A6E)
private val SafeNetBlueBright = Color(0xFF38BDF8)

private val SafeNetGreen = Color(0xFF22C55E)
private val SafeNetRed = Color(0xFFEF4444)
private val SafeNetAmber = Color(0xFFF59E0B)

// Dark theme (primary)
private val DarkColorScheme = darkColorScheme(
    primary = SafeNetBlue,
    onPrimary = Color.White,
    primaryContainer = SafeNetBlueDark,
    onPrimaryContainer = SafeNetBlueBright,

    secondary = Color(0xFF1E3A5F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0D2137),
    onSecondaryContainer = SafeNetBlueBright,

    tertiary = SafeNetGreen,
    onTertiary = Color.White,

    background = Color(0xFF060C14),
    onBackground = Color(0xFFF1F5F9),

    surface = Color(0xFF0B1522),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF132030),
    onSurfaceVariant = Color(0xFF94A3B8),

    error = SafeNetRed,
    onError = Color.White,

    outline = Color(0xFF1E3A5F),
    outlineVariant = Color(0xFF0F1F30),
)

// Light theme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = SafeNetBlueDark,

    secondary = Color(0xFF0369A1),
    onSecondary = Color.White,

    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),

    error = SafeNetRed,
    outline = Color(0xFFCBD5E1),
)

@Composable
fun SafeNetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable Material You dynamic colors for brand consistency
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SafeNetTypography,
        content = content,
    )
}
