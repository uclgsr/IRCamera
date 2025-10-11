package com.mpdc4gsr.component.thermal.feature.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ThermalControlPanel(
    selectedModeIndex: Int,
    isRecording: Boolean,
    onModeSelected: (Int) -> Unit,
    onCapture: () -> Unit,
    onToggleRecording: () -> Unit,
    onPaletteClick: () -> Unit,
    onAdjustClick: () -> Unit,
    modifier: Modifier = Modifier,
    modeLabels: List<String> = defaultThermalModes(),
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThermalQuickActionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Capture",
                    onClick = onCapture,
                    modifier = Modifier.weight(1f),
                )
                ThermalQuickActionButton(
                    icon = Icons.Default.Videocam,
                    label = if (isRecording) "Stop" else "Record",
                    onClick = onToggleRecording,
                    modifier = Modifier.weight(1f),
                    isPrimary = isRecording,
                )
                ThermalQuickActionButton(
                    icon = Icons.Default.Palette,
                    label = "Palette",
                    onClick = onPaletteClick,
                    modifier = Modifier.weight(1f),
                )
                ThermalQuickActionButton(
                    icon = Icons.Default.Tune,
                    label = "Adjust",
                    onClick = onAdjustClick,
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Gain Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    modeLabels.forEachIndexed { index, label ->
                        FilterChip(
                            onClick = { onModeSelected(index) },
                            label = { Text(label) },
                            selected = selectedModeIndex == index,
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B35),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1F2933),
                                    labelColor = Color(0xFF94A3B8),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalQuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = if (isPrimary) Color(0x33FF6B35) else Color.Transparent,
                contentColor = Color.White,
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = if (isPrimary) Color(0xFFFF6B35) else Color(0xFF475569),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

fun defaultThermalModes(): List<String> =
    listOf(
        "High Gain",
        "Low Gain",
        "Auto Gain",
        "Manual",
    )

