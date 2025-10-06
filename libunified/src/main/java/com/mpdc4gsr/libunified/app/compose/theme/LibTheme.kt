package com.mpdc4gsr.libunified.app.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Shared theme colors that can be used across modules
// These colors are now public for use in Compose components
val ThermalOrange = Color(0xFFFF6B35)
val ThermalRed = Color(0xFFE63946)
val ThermalBlue = Color(0xFF457B9D)
val ThermalDark = Color(0xFF1D3557)
private val LightColorScheme = lightColorScheme(
    primary = ThermalBlue,
    secondary = ThermalOrange,
    tertiary = ThermalRed,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE)
)
private val DarkColorScheme = darkColorScheme(
    primary = ThermalBlue,
    secondary = ThermalOrange,
    tertiary = ThermalRed,
    background = ThermalDark,
    surface = ThermalDark
)

@Composable
fun LibUnifiedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}