package mpdc4gsr.core.ui.components.sensors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * High level selection model bundling availability and option metadata.
 * This remains stable across recompositions to avoid unnecessary recomputation.
 */
@Immutable
data class SensorSelectionState(
    val entries: List<SensorAvailability>,
    val selected: Set<SensorOption>
)

@Composable
fun SensorSelectionDialog(
    state: SensorSelectionState,
    onSelectionChanged: (Set<SensorOption>) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Sensors",
    subtitle: String = "Choose sensors for your research session"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                SensorSelectionHeader(
                    title = title,
                    subtitle = subtitle,
                    selectedCount = state.selected.size,
                    totalCount = state.entries.count { it.isAvailable }
                )
                Spacer(modifier = Modifier.height(16.dp))
                SensorList(
                    entries = state.entries,
                    selected = state.selected,
                    onSelectionChanged = onSelectionChanged
                )
                Spacer(modifier = Modifier.height(20.dp))
                SelectionActions(
                    selectedCount = state.selected.size,
                    onClear = {
                        onSelectionChanged(emptySet())
                        onDismiss()
                    },
                    onConfirm = onDismiss
                )
                if (state.selected.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BatteryImpactSummary(
                        selected = state.selected,
                        entries = state.entries
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorList(
    entries: List<SensorAvailability>,
    selected: Set<SensorOption>,
    onSelectionChanged: (Set<SensorOption>) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries) { availability ->
            val option = availability.option
            val isSelected = selected.contains(option)
            SensorSelectionItem(
                availability = availability,
                isSelected = isSelected,
                onToggle = {
                    val next = if (isSelected) {
                        selected - option
                    } else {
                        selected + option
                    }
                    onSelectionChanged(next)
                }
            )
        }
    }
}

@Composable
private fun SensorSelectionHeader(
    title: String,
    subtitle: String,
    selectedCount: Int,
    totalCount: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$selectedCount/$totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SensorSelectionItem(
    availability: SensorAvailability,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val option = availability.option
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                enabled = availability.isAvailable,
                onClick = {
                    if (availability.isAvailable) {
                        onToggle()
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.displayName,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SelectionStatusTag(
                    available = availability.isAvailable,
                    isSelected = isSelected
                )
            }
            if (availability.availabilityReason.isNotBlank()) {
                Text(
                    text = availability.availabilityReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Battery: ${availability.batteryImpact}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Data: ${availability.dataRate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SelectionStatusTag(
    available: Boolean,
    isSelected: Boolean
) {
    val (label, container, content) = when {
        !available -> Triple("Unavailable", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.error)
        isSelected -> Triple("Selected", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        else -> Triple(
            "Available",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = container
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SelectionActions(
    selectedCount: Int,
    onClear: () -> Unit,
    onConfirm: () -> Unit
) {
    val hasSelection = selectedCount > 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }
        TextButton(
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            enabled = hasSelection
        ) {
            val label = if (hasSelection) {
                "Confirm ($selectedCount)"
            } else {
                "Confirm"
            }
            Text(label)
        }
    }
}

@Composable
private fun BatteryImpactSummary(
    selected: Set<SensorOption>,
    entries: List<SensorAvailability>
) {
    val impact = remember(selected, entries) {
        entries.filter { selected.contains(it.option) }
            .groupingBy { it.batteryImpact }
            .eachCount()
            .map { (impactLabel, count) -> impactLabel to count }
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Estimated battery impact",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            if (impact.isEmpty()) {
                Text(
                    text = "Minimal impact",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                impact.forEach { (label, count) ->
                    Text(
                        text = "$label x$count",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun SensorSelectionPreview(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val entries = SensorOption.values().map {
        SensorAvailability(
            option = it,
            isAvailable = it.isAvailableByDefault,
            isSelected = false
        )
    }
    SensorSelectionDialog(
        state = SensorSelectionState(
            entries = entries,
            selected = entries.filter { it.isAvailable }.take(2).map { it.option }.toSet()
        ),
        onSelectionChanged = {},
        onDismiss = onDismiss
    )
}
