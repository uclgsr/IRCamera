package com.mpdc4gsr.libunified.app.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Shared theme colors that can be used across modules
private val thermalOrange = Color(0xFFFF6B35)
private val thermalRed = Color(0xFFE63946)
private val thermalBlue = Color(0xFF457B9D)
private val thermalDark = Color(0xFF1D3557)

private val LightColorScheme = lightColorScheme(
    primary = thermalBlue,
    secondary = thermalOrange,
    tertiary = thermalRed,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE)
)

private val DarkColorScheme = darkColorScheme(
    primary = thermalBlue,
    secondary = thermalOrange,
    tertiary = thermalRed,
    background = thermalDark,
    surface = thermalDark
)

/**
 * Basic theme that can be used across modules
 * Individual modules can wrap this with their own theme customizations
 */
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