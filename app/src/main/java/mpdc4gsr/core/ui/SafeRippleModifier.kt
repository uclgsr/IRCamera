package mpdc4gsr.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos

/**
 * Safe clickable modifier that prevents ANR issues with ripple animations.
 * 
 * This modifier wraps the standard clickable modifier with additional safety checks
 * to prevent IllegalStateException when animations try to start on detached views.
 * 
 * The approach:
 * - Uses composed() to ensure the modifier is lifecycle-aware
 * - Creates interaction source within the composition scope
 * - Tracks press interactions and cancels them on dispose
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
    var press: PressInteraction.Press? by remember { mutableStateOf(null) }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> press = interaction
                is PressInteraction.Release, is PressInteraction.Cancel -> press = null
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            press?.let { interactionSource.tryEmit(PressInteraction.Cancel(it)) }
        }
    }
    
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
    var press: PressInteraction.Press? by remember { mutableStateOf(null) }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> press = interaction
                is PressInteraction.Release, is PressInteraction.Cancel -> press = null
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            press?.let { interactionSource.tryEmit(PressInteraction.Cancel(it)) }
        }
    }
    
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = ripple(bounded = bounded, radius = radius, color = color),
        onClick = onClick
    )
}

/**
 * Safe clickable modifier without ripple indication.
 * Use for destructive or instant-navigation clicks where the host detaches immediately.
 */
@Composable
fun Modifier.safeClickableNoRipple(
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
        indication = null,
        onClick = onClick
    )
}

/**
 * Safe clickable modifier with deferred navigation.
 * Waits one frame before executing the onClick callback,
 * allowing ripple animation to settle before navigation.
 */
@Composable
fun Modifier.safeClickableDeferred(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    var press: PressInteraction.Press? by remember { mutableStateOf(null) }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> press = interaction
                is PressInteraction.Release, is PressInteraction.Cancel -> press = null
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            press?.let { interactionSource.tryEmit(PressInteraction.Cancel(it)) }
        }
    }
    
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = {
            scope.launch {
                withFrameNanos { }
                onClick()
            }
        }
    )
}
