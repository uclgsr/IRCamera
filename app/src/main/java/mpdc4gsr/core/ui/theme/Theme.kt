package mpdc4gsr.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Standard color constants for buttons and UI elements
val Orange = Color(0xFFFF9800)
val Green = Color(0xFF4CAF50)
val Purple = Color(0xFF9C27B0)

// Color scheme for IRCamera app - based on thermal imaging colors
private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF6B73FF), // Blue-purple for thermal UI
        onPrimary = Color.White,
        primaryContainer = Color(0xFF1E1B3C),
        onPrimaryContainer = Color(0xFFE0E0FF),
        secondary = Color(0xFFFF6B6B), // Red-orange for thermal highlights
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF3C1E1E),
        onSecondaryContainer = Color(0xFFFFE0E0),
        tertiary = Color(0xFF4ECDC4), // Cyan for GSR/sensor data
        onTertiary = Color.White,
        error = Color(0xFFFFB4AB),
        errorContainer = Color(0xFF93000A),
        onError = Color(0xFF690005),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF16131e),
        onBackground = Color(0xFFE6E6E6),
        surface = Color(0xFF1E1E1E),
        onSurface = Color(0xFFE6E6E6),
        surfaceVariant = Color(0xFF2D2D2D),
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99),
        inverseOnSurface = Color(0xFF121212),
        inverseSurface = Color(0xFFE6E6E6),
        inversePrimary = Color(0xFF415FDF),
        scrim = Color(0xFF000000),
    )
private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF415FDF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDFE0FF),
        onPrimaryContainer = Color(0xFF000F5C),
        secondary = Color(0xFFD32F2F), // Red for thermal highlights
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDAD6),
        onSecondaryContainer = Color(0xFF410002),
        tertiary = Color(0xFF26A69A), // Teal for GSR/sensor data
        onTertiary = Color.White,
        error = Color(0xFFBA1A1A),
        errorContainer = Color(0xFFFFDAD6),
        onError = Color.White,
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFE),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFFFBFE),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E),
        inverseOnSurface = Color(0xFFF4EFF4),
        inverseSurface = Color(0xFF313033),
        inversePrimary = Color(0xFFBEC2FF),
    )

@Composable
fun IRCameraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent thermal imaging theme
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
