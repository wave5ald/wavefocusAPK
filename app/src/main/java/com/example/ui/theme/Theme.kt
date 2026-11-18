package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/* ==========================================================================
   Wave Focus theme — mirrors pwa/css/app.css design tokens.
   ========================================================================== */

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimarySoft,
    onPrimaryContainer = NightOnPrimaryContainer,
    inversePrimary = DayPrimary,
    secondary = NightSecondary,
    onSecondary = Color(0xFF11162E),
    secondaryContainer = NightSecondarySoft,
    onSecondaryContainer = NightOnSecondaryContainer,
    tertiary = NightTertiary,
    onTertiary = Color(0xFF2A1E02),
    tertiaryContainer = NightTertiarySoft,
    onTertiaryContainer = NightOnTertiaryContainer,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    surfaceTint = NightPrimary,
    inverseSurface = NightInverseSurface,
    inverseOnSurface = NightInverseOnSurface,
    error = NightError,
    onError = NightOnError,
    errorContainer = NightErrorSoft,
    onErrorContainer = Color(0xFFFFC9D2),
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
    scrim = Color(0xFF020617),
    surfaceBright = NightSurfaceHighest,
    surfaceDim = NightBackground,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHighest,
    surfaceContainerLow = NightBackgroundElev,
    surfaceContainerLowest = Color(0xFF070B13)
)

private val LightColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = DayOnPrimary,
    primaryContainer = DayPrimarySoft,
    onPrimaryContainer = DayOnPrimaryContainer,
    inversePrimary = NightPrimary,
    secondary = DaySecondary,
    onSecondary = Color.White,
    secondaryContainer = DaySecondarySoft,
    onSecondaryContainer = DayOnSecondaryContainer,
    tertiary = DayTertiary,
    onTertiary = Color.White,
    tertiaryContainer = DayTertiarySoft,
    onTertiaryContainer = DayOnTertiaryContainer,
    background = DayBackground,
    onBackground = DayOnSurface,
    surface = DaySurface,
    onSurface = DayOnSurface,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayOnSurfaceVariant,
    surfaceTint = DayPrimary,
    inverseSurface = DayInverseSurface,
    inverseOnSurface = DayInverseOnSurface,
    error = DayError,
    onError = DayOnError,
    errorContainer = DayErrorSoft,
    onErrorContainer = Color(0xFF881337),
    outline = DayOutline,
    outlineVariant = DayOutlineVariant,
    scrim = Color(0xFF0F172A),
    surfaceBright = Color.White,
    surfaceDim = DaySurfaceVariant,
    surfaceContainer = DaySurface,
    surfaceContainerHigh = DaySurfaceHigh,
    surfaceContainerHighest = DaySurfaceHighest,
    surfaceContainerLow = DaySurfaceHigh,
    surfaceContainerLowest = Color.White
)

/** PWA radii: xl 26, lg 20, md 14, sm 10. */
val WaveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/**
 * Extra Wave-only tokens that don't map cleanly onto MaterialTheme.
 * Provided once per theme so screens can use `WaveTheme.colors.*`.
 */
data class WaveColors(
    val isDark: Boolean,
    val backgroundElevated: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val primarySoft: Color,
    val secondarySoft: Color,
    val tertiarySoft: Color,
    val success: Color,
    val flame: Color,
    val coin: Color,
    val danger: Color,
    val heroGlow: Color,
    val navBackground: Color,
    val onSurfaceMuted: Color,
    val outlineSoft: Color
)

private val DarkWaveColors = WaveColors(
    isDark = true,
    backgroundElevated = NightBackgroundElev,
    surfaceHigh = NightSurfaceHigh,
    surfaceHighest = NightSurfaceHighest,
    primarySoft = WavePrimarySoftDark,
    secondarySoft = NightSecondarySoft,
    tertiarySoft = NightTertiarySoft,
    success = SuccessGreen,
    flame = FlameOrange,
    coin = CoinGold,
    danger = NightError,
    heroGlow = WaveHeroTopDark,
    navBackground = NightSurface.copy(alpha = 0.92f),
    onSurfaceMuted = NightOnSurfaceVariant,
    outlineSoft = NightOutlineVariant
)

private val LightWaveColors = WaveColors(
    isDark = false,
    backgroundElevated = DayBackgroundElev,
    surfaceHigh = DaySurfaceHigh,
    surfaceHighest = DaySurfaceHighest,
    primarySoft = WavePrimarySoftLight,
    secondarySoft = DaySecondarySoft,
    tertiarySoft = DayTertiarySoft,
    success = Color(0xFF059669),
    flame = Color(0xFFEA580C),
    coin = Color(0xFFD97706),
    danger = DayError,
    heroGlow = WaveHeroTopLight,
    navBackground = DaySurface.copy(alpha = 0.94f),
    onSurfaceMuted = DayOnSurfaceVariant,
    outlineSoft = DayOutlineVariant
)

/** Rotate the accent tokens that live outside [MaterialTheme]. */
fun WaveColors.shiftHue(degrees: Float): WaveColors {
    if (abs(degrees % 360f) < 0.01f) return this
    return copy(
        primarySoft = primarySoft.shiftHue(degrees),
        secondarySoft = secondarySoft.shiftHue(degrees),
        tertiarySoft = tertiarySoft.shiftHue(degrees),
        heroGlow = heroGlow.shiftHue(degrees)
    )
}

val LocalWaveColors = staticCompositionLocalOf { DarkWaveColors }

object WaveTheme {
    val colors: WaveColors
        @Composable get() = LocalWaveColors.current
}

@Composable
fun MyApplicationTheme(
    themeMode: String = "system",
    hueShift: Float = 0f,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    // The settings slider rotates every accent colour of the palette.
    val colorScheme = (if (isDark) DarkColorScheme else LightColorScheme).shiftHue(hueShift)
    val waveColors = (if (isDark) DarkWaveColors else LightWaveColors).shiftHue(hueShift)

    CompositionLocalProvider(LocalWaveColors provides waveColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = WaveShapes,
            content = content
        )
    }
}
