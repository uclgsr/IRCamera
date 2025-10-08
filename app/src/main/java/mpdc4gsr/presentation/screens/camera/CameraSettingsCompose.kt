package mpdc4gsr.presentation.screens.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CameraSettingsPanel(
    isAutoExposure: Boolean = true,
    exposureCompensation: Float = 0f,
    isAeLocked: Boolean = false,
    isAutoFocus: Boolean = true,
    focusDistance: Float = 0f,
    isAfLocked: Boolean = false,
    isFlashEnabled: Boolean = false,
    isStage3ProcessingEnabled: Boolean = false,
    onExposureModeToggle: (Boolean) -> Unit = {},
    onExposureCompensationChanged: (Float) -> Unit = {},
    onAeLockToggle: (Boolean) -> Unit = {},
    onFocusModeToggle: (Boolean) -> Unit = {},
    onFocusDistanceChanged: (Float) -> Unit = {},
    onAfLockToggle: (Boolean) -> Unit = {},
    onCameraToggle: () -> Unit = {},
    onRecordingToggle: (Boolean) -> Unit = {},
    onFlashToggle: (Boolean) -> Unit = {},
    onStage3ProcessingToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Camera Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        // Exposure Controls
        CameraSettingsSection(title = "Exposure") {
            SwitchSettingItem(
                label = "Auto Exposure",
                checked = isAutoExposure,
                onCheckedChange = onExposureModeToggle,
                icon = Icons.Default.WbSunny
            )
            SliderSettingItem(
                label = "Exposure Compensation",
                value = exposureCompensation,
                onValueChange = onExposureCompensationChanged,
                valueRange = -2f..2f,
                enabled = !isAutoExposure
            )
            SwitchSettingItem(
                label = "AE Lock",
                checked = isAeLocked,
                onCheckedChange = onAeLockToggle,
                icon = Icons.Default.Lock
            )
        }
        // Focus Controls
        CameraSettingsSection(title = "Focus") {
            SwitchSettingItem(
                label = "Auto Focus",
                checked = isAutoFocus,
                onCheckedChange = onFocusModeToggle,
                icon = Icons.Default.CenterFocusStrong
            )
            SliderSettingItem(
                label = "Focus Distance",
                value = focusDistance,
                onValueChange = onFocusDistanceChanged,
                valueRange = 0f..1f,
                enabled = !isAutoFocus
            )
            SwitchSettingItem(
                label = "AF Lock",
                checked = isAfLocked,
                onCheckedChange = onAfLockToggle,
                icon = Icons.Default.Lock
            )
        }
        // Basic Controls
        CameraSettingsSection(title = "Controls") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCameraToggle,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flip")
                }
                FilledTonalButton(
                    onClick = { onFlashToggle(!isFlashEnabled) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flash")
                }
            }
            SwitchSettingItem(
                label = "Stage 3 Processing",
                checked = isStage3ProcessingEnabled,
                onCheckedChange = onStage3ProcessingToggle,
                icon = Icons.Default.AutoFixHigh
            )
        }
    }
}

@Composable
private fun CameraSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SwitchSettingItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSettingItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
