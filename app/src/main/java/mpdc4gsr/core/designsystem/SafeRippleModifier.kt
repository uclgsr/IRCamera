package mpdc4gsr.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.mpdc4gsr.component.shared.app.compose.utils.deferAction as libDeferAction
import com.mpdc4gsr.component.shared.app.compose.utils.safeClickable as libSafeClickable
import com.mpdc4gsr.component.shared.app.compose.utils.safeClickableDeferred as libSafeClickableDeferred
import com.mpdc4gsr.component.shared.app.compose.utils.safeClickableNoRipple as libSafeClickableNoRipple
import com.mpdc4gsr.component.shared.app.compose.utils.safeClickableWithRipple as libSafeClickableWithRipple

@Composable
fun Modifier.safeClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = libSafeClickable(enabled, onClickLabel, role, onClick)

fun Modifier.safeClickableWithRipple(
    enabled: Boolean = true,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = libSafeClickableWithRipple(enabled, bounded, radius, color, onClickLabel, role, onClick)

@Composable
fun Modifier.safeClickableNoRipple(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = libSafeClickableNoRipple(enabled, onClickLabel, role, onClick)

@Composable
fun Modifier.safeClickableDeferred(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = libSafeClickableDeferred(enabled, onClickLabel, role, onClick)

@Composable
fun deferAction(action: () -> Unit): () -> Unit = libDeferAction(action)


