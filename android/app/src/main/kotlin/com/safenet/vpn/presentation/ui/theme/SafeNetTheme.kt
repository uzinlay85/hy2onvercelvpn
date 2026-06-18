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

private val VibrantTeal = Color(0xFF14B8A6)
private val VibrantTealDark = Color(0xFF0F766E)
private val VibrantTealBright = Color(0xFF5EEAD4)

private val CalmBlue = Color(0xFF3B82F6)
private val DeepSpace = Color(0xFF0B1120)
private val SurfaceDark = Color(0xFF111827)
private val SurfaceVariantDark = Color(0xFF1F2937)

private val SafeNetGreen = Color(0xFF10B981)
private val SafeNetRed = Color(0xFFF43F5E)
private val SafeNetAmber = Color(0xFFF59E0B)

// Dark theme (primary)
private val DarkColorScheme = darkColorScheme(
    primary = VibrantTeal,
    onPrimary = Color.White,
    primaryContainer = VibrantTealDark,
    onPrimaryContainer = VibrantTealBright,

    secondary = CalmBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFBFDBFE),

    tertiary = SafeNetGreen,
    onTertiary = Color.White,

    background = DeepSpace,
    onBackground = Color(0xFFF8FAFC),

    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF9CA3AF),

    error = SafeNetRed,
    onError = Color.White,

    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF1F2937),
)

// Light theme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = VibrantTealDark,

    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,

    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),

    surface = Color.White,
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
