package com.mpdc4gsr.libunified.app.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch

/**
 * Helper function to defer an action by one frame.
 * Use for navigation actions that need to let ripple animations settle.
 * 
 * Example:
 * ```
 * IconButton(onClick = deferAction { finish() }) {
 *     Icon(Icons.Default.ArrowBack, contentDescription = "Back")
 * }
 * ```
 */
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
