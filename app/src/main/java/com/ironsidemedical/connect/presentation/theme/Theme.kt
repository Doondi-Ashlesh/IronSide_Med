package com.ironsidemedical.connect.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// IronSide brand palette
private val IronBlue = Color(0xFF0A2540)
private val IronTeal = Color(0xFF00B4D8)
private val IronLightBlue = Color(0xFF90E0EF)
private val AlertRed = Color(0xFFD62828)
private val AlertAmber = Color(0xFFF4A261)
private val SafeGreen = Color(0xFF2DC653)

val IronSideLightColors = lightColorScheme(
    primary = IronBlue,
    onPrimary = Color.White,
    primaryContainer = IronLightBlue,
    secondary = IronTeal,
    onSecondary = Color.White,
    tertiary = SafeGreen,
    error = AlertRed,
    surface = Color(0xFFF8FAFC),
    onSurface = IronBlue,
    surfaceVariant = Color(0xFFE8F4FD),
    outline = Color(0xFFB0BEC5),
)

val IronSideDarkColors = darkColorScheme(
    primary = IronTeal,
    onPrimary = IronBlue,
    primaryContainer = IronBlue,
    secondary = IronLightBlue,
    tertiary = SafeGreen,
    error = AlertRed,
    surface = Color(0xFF0D1B2A),
    onSurface = Color(0xFFE8F4FD),
)

val AlertRedColor = AlertRed
val AlertAmberColor = AlertAmber
val SafeGreenColor = SafeGreen

@Composable
fun IronSideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) IronSideDarkColors else IronSideLightColors,
        typography = IronSideTypography,
        content = content,
    )
}
