package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorHistoryViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorHistoryViewModel.*
import java.text.SimpleDateFormat
import java.util.*

class IRMonitorHistoryComposeFragment : BaseComposeFragment<IRMonitorHistoryViewModel>() {

    override fun createViewModel(): IRMonitorHistoryViewModel {
        return viewModels<IRMonitorHistoryViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRMonitorHistoryViewModel) {
        // Observe ViewModel state
        val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()
        val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()

        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // History header with filter controls
                HistoryHeader(
                    selectedFilter = selectedFilter,
                    totalItems = historyItems.size,
                    onFilterChange = { filter ->
                        viewModel.changeFilter(filter)
                    }
                )

                // Selection toolbar
                if (isSelectionMode) {
                    HistorySelectionToolbar(
                        selectedCount = selectedItems.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onExportSelected = { viewModel.exportSelectedItems() },
                        onDeleteSelected = { viewModel.deleteSelectedItems() }
                    )
                }

                // History content
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            LoadingState()
                        }

                        historyItems.isEmpty() -> {
                            EmptyHistoryState(
                                filter = selectedFilter,
                                onRefresh = { viewModel.refreshHistory() }
                            )
                        }

                        else -> {
                            HistoryList(
                                historyItems = historyItems,
                                selectedItems = selectedItems,
                                isSelectionMode = isSelectionMode,
                                onItemClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item.id)
                                    } else {
                                        viewModel.viewHistoryDetails(item)
                                    }
                                },
                                onItemLongClick = { item ->
                                    if (!isSelectionMode) {
                                        viewModel.enterSelectionMode()
                                        viewModel.toggleItemSelection(item.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HistoryHeader(
        selectedFilter: HistoryFilter,
        totalItems: Int,
        onFilterChange: (HistoryFilter) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monitor History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalItems sessions recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Icon(
                        Icons.Default.History,
                        contentDescription = "History",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Filter chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryFilter.values().forEach { filter ->
                        FilterChip(
                            onClick = { onFilterChange(filter) },
                            label = {
                                Text(
                                    text = filter.displayName,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = selectedFilter == filter,
                            leadingIcon = {
                                Icon(
                                    filter.icon,
                                    contentDescription = filter.displayName,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun HistorySelectionToolbar(
        selectedCount: Int,
        onClearSelection: () -> Unit,
        onExportSelected: () -> Unit,
        onDeleteSelected: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount session${if (selectedCount != 1) "s" else ""} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onExportSelected) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = onClearSelection) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingState() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading history...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun EmptyHistoryState(
        filter: HistoryFilter,
        onRefresh: () -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = "No history",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = when (filter) {
                        HistoryFilter.ALL -> "No History Found"
                        HistoryFilter.TODAY -> "No Sessions Today"
                        HistoryFilter.WEEK -> "No Sessions This Week"
                        HistoryFilter.MONTH -> "No Sessions This Month"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Monitor thermal sessions to see history here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
        }
    }

    @Composable
    private fun HistoryList(
        historyItems: List<HistoryItem>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        onItemClick: (HistoryItem) -> Unit,
        onItemLongClick: (HistoryItem) -> Unit
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(historyItems) { item ->
                HistoryListItem(
                    viewModel = viewModel,
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }

    @Composable
    private fun HistoryListItem(
        viewModel: IRMonitorHistoryViewModel,
        item: HistoryItem,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Session icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            getSessionTypeColor(item.sessionType).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getSessionTypeIcon(item.sessionType),
                        contentDescription = item.sessionType.name,
                        tint = getSessionTypeColor(item.sessionType),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Session info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.sessionName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(item.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.sampleCount} samples",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusChip(
                            text = item.sessionType.displayName,
                            color = getSessionTypeColor(item.sessionType)
                        )
                    }

                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Temperature summary
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${item.avgTemperature}°C",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Average",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Selection indicator or actions
                if (isSelectionMode) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Not Selected",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.viewHistoryDetails(item) }
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "View details"
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }

    // Helper functions
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    private fun getSessionTypeIcon(type: SessionType): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
        SessionType.MONITORING -> Icons.Default.Monitor
        SessionType.CAPTURE -> Icons.Default.CameraAlt
        SessionType.ANALYSIS -> Icons.Default.Analytics
        SessionType.CALIBRATION -> Icons.Default.Tune
    }

    @Composable
    private fun getSessionTypeColor(type: SessionType): Color = when (type) {
        SessionType.MONITORING -> MaterialTheme.colorScheme.primary
        SessionType.CAPTURE -> Color.Green
        SessionType.ANALYSIS -> Color(0xFFFFA500)
        SessionType.CALIBRATION -> Color.Red
    }
}