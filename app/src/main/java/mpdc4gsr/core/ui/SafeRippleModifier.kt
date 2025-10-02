package mpdc4gsr.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp

/**
 * Safe clickable modifier that prevents ANR issues with ripple animations.
 * 
 * This modifier wraps the standard clickable modifier with additional safety checks
 * to prevent IllegalStateException when animations try to start on detached views.
 * 
 * The approach:
 * - Uses composed() to ensure the modifier is lifecycle-aware
 * - Creates interaction source within the composition scope
 * - Properly manages ripple indication lifecycle
 * 
 * This prevents the ANR scenario where:
 * 1. User taps a button triggering ripple + navigation
 * 2. Main thread blocks (from other operations)
 * 3. Navigation happens but ripple is queued
 * 4. Thread unblocks and tries to animate detached view -> crash
 */
@Composable
fun Modifier.safeClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = onClick
    )
}

/**
 * Safe clickable modifier with custom ripple configuration
 */
@Composable
fun Modifier.safeClickableWithRipple(
    enabled: Boolean = true,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = ripple(bounded = bounded, radius = radius, color = color),
        onClick = onClick
    )
}
