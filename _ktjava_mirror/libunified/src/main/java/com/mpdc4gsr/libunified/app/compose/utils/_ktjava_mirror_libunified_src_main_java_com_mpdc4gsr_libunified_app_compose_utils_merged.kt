// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\utils' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\utils\libunified_src_main_java_com_mpdc4gsr_libunified_app_compose_utils_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\utils' subtree
// Files: 1; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\compose\utils\SafeRippleModifier.kt =====

package com.mpdc4gsr.libunified.app.compose.utils

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

@Composable
private fun rememberCancellableInteractionSource(): MutableInteractionSource {
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
    return interactionSource
}

@Composable
fun Modifier.safeClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = rememberCancellableInteractionSource()
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = onClick
    )
}

fun Modifier.safeClickableWithRipple(
    enabled: Boolean = true,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = rememberCancellableInteractionSource()
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = ripple(bounded = bounded, radius = radius, color = color),
        onClick = onClick
    )
}

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

@Composable
fun Modifier.safeClickableDeferred(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val interactionSource = rememberCancellableInteractionSource()
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = {
            scope.launch {
                withFrameNanos { }
                onClick()
            }
        }
    )
}

@Composable
fun deferAction(action: () -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            withFrameNanos { }
            action()
        }
    }
}