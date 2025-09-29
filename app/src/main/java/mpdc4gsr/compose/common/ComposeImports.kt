package mpdc4gsr.compose.common

/**
 * ComposeImports - Centralized imports for consistent Compose usage
 * 
 * This file consolidates the most commonly used Compose imports to reduce
 * duplication across 132+ Compose files in the codebase.
 * 
 * Usage: Import this file and use the standard imports provided
 */

// Standard Compose imports used across 132+ files
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Re-export commonly used types for convenience
typealias ComposeScope = @Composable () -> Unit
typealias ComposeContent = @Composable ColumnScope.() -> Unit
typealias ComposeRowContent = @Composable RowScope.() -> Unit

/**
 * Standard dimensions used across the app
 */
object ComposeDimens {
    val paddingSmall = 8.dp
    val paddingMedium = 16.dp  
    val paddingLarge = 24.dp
    val cornerRadius = 12.dp
    val spacingSmall = 8.dp
    val spacingMedium = 12.dp
    val spacingLarge = 16.dp
}

/**
 * Standard colors used in Compose components
 */
object ComposeColors {
    val primaryVariant = Color(0xFFFF6B35)
    val surfaceVariant = Color(0xFF2D2D2D)
    val backgroundLight = Color(0xFFF5F5F5)
    val textSecondary = Color(0xFF666666)
}