// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme\libunified_src_main_java_com_mpdc4gsr_libunified_app_compose_theme_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme' subtree
// Files: 2; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme\LibTheme.kt =====

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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\theme\Spacing.kt =====

package com.mpdc4gsr.libunified.app.compose.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Spacing {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val normal: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 32.dp
    val touchTarget: Dp = 48.dp
}