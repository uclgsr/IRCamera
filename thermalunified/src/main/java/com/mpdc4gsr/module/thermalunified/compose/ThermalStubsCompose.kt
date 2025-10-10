package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

// Data classes for component state
data class MonitorOption(
    val id: Int,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean = true
)

data class TipDialogData(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val type: TipType = TipType.INFO
)

enum class TipType {
    INFO, WARNING, ERROR, SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorSelectDialogCompose(
    showDialog: Boolean,
    options: List<MonitorOption>,
    selectedOption: MonitorOption?,
    onOptionSelected: (MonitorOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Dialog title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Monitor",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Options list
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options) { option ->
                            MonitorOptionItem(
                                option = option,
                                isSelected = option.id == selectedOption?.id,
                                onSelected = { onOptionSelected(option) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                selectedOption?.let { onOptionSelected(it) }
                                onDismiss()
                            },
                            enabled = selectedOption != null
                        ) {
                            Text("Select")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorOptionItem(
    option: MonitorOption,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = option.isEnabled) { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.name,
                tint = if (option.isEnabled) {
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (option.isEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                if (option.description.isNotEmpty()) {
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TipDialogCompose(
    showDialog: Boolean,
    tipData: TipDialogData,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = tipData.icon,
                    contentDescription = tipData.type.name,
                    tint = when (tipData.type) {
                        TipType.INFO -> MaterialTheme.colorScheme.primary
                        TipType.WARNING -> MaterialTheme.colorScheme.error
                        TipType.ERROR -> MaterialTheme.colorScheme.error
                        TipType.SUCCESS -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = tipData.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = tipData.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm?.invoke()
                        onDismiss()
                    }
                ) {
                    Text(if (onConfirm != null) "OK" else "Dismiss")
                }
            },
            dismissButton = if (onConfirm != null) {
                {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            } else null,
            modifier = modifier
        )
    }
}

@Composable
fun FenceViewsCompose(
    fences: List<FenceData>,
    onFenceSelected: (FenceData) -> Unit,
    onFenceDeleted: (FenceData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(fences) { fence ->
            FenceItemCompose(
                fence = fence,
                onSelected = { onFenceSelected(fence) },
                onDeleted = { onFenceDeleted(fence) }
            )
        }
    }
}

@Composable
private fun FenceItemCompose(
    fence: FenceData,
    onSelected: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CropFree,
                contentDescription = "Fence",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fence.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Points: ${fence.points.size} | Temp: ${fence.temperature}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDeleted) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete fence",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun GuideStubsCompose(
    guideSteps: List<GuideStep>,
    currentStep: Int,
    onStepChanged: (Int) -> Unit,
    onGuideCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1f) / guideSteps.size },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Current step content
            if (currentStep < guideSteps.size) {
                val step = guideSteps[currentStep]
                Text(
                    text = "Step ${currentStep + 1} of ${guideSteps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (currentStep > 0) onStepChanged(currentStep - 1)
                        },
                        enabled = currentStep > 0
                    ) {
                        Text("Previous")
                    }
                    Button(
                        onClick = {
                            if (currentStep < guideSteps.size - 1) {
                                onStepChanged(currentStep + 1)
                            } else {
                                onGuideCompleted()
                            }
                        }
                    ) {
                        Text(if (currentStep < guideSteps.size - 1) "Next" else "Complete")
                    }
                }
            }
        }
    }
}

@Composable
fun UIWidgetsCompose(
    widgets: List<WidgetData>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(widgets) { widget ->
            WidgetItemCompose(widget = widget)
        }
    }
}

@Composable
private fun WidgetItemCompose(
    widget: WidgetData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = widget.icon,
                    contentDescription = widget.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = widget.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            if (widget.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = widget.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Data classes for components
data class FenceData(
    val id: Int,
    val name: String,
    val points: List<Pair<Float, Float>>,
    val temperature: Float
)

data class GuideStep(
    val title: String,
    val description: String,
    val imageRes: Int? = null
)

data class WidgetData(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector
)

// Preview functions
@Preview(showBackground = true)
@Composable
private fun MonitorSelectDialogPreview() {
    LibUnifiedTheme {
        val sampleOptions = listOf(
            MonitorOption(1, "Temperature Monitor", "Real-time temperature tracking", Icons.Default.Thermostat),
            MonitorOption(2, "Pressure Monitor", "Pressure level monitoring", Icons.Default.Speed),
            MonitorOption(3, "Humidity Monitor", "Humidity level tracking", Icons.Default.WaterDrop, false)
        )
        MonitorSelectDialogCompose(
            showDialog = true,
            options = sampleOptions,
            selectedOption = sampleOptions[0],
            onOptionSelected = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TipDialogPreview() {
    LibUnifiedTheme {
        TipDialogCompose(
            showDialog = true,
            tipData = TipDialogData(
                title = "Calibration Required",
                message = "Please calibrate the thermal sensor before proceeding with measurements.",
                icon = Icons.Default.Warning,
                type = TipType.WARNING
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FenceViewsPreview() {
    LibUnifiedTheme {
        val sampleFences = listOf(
            FenceData(1, "Temperature Zone 1", listOf(0f to 0f, 100f to 100f), 25.5f),
            FenceData(2, "Critical Area", listOf(50f to 50f, 150f to 150f), 85.2f)
        )
        FenceViewsCompose(
            fences = sampleFences,
            onFenceSelected = {},
            onFenceDeleted = {}
        )
    }
}