package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.theme.IRCameraTheme
import mpdc4gsr.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

// SessionManagerViewModel
class SessionManagerViewModel : BaseViewModel() {
    
    data class SessionManagerUiState(
        val sessions: List<RecordingSession> = emptyList(),
        val filteredSessions: List<RecordingSession> = emptyList(),
        val statistics: SessionStatistics? = null,
        val isLoading: Boolean = false,
        val currentFilter: SessionFilter = SessionFilter()
    )
    
    private val _sessionState = MutableStateFlow(SessionManagerUiState())
    val sessionState: StateFlow<SessionManagerUiState> = _sessionState.asStateFlow()
    
    fun loadSessions() {
        _sessionState.value = _sessionState.value.copy(
            isLoading = false,
            sessions = emptyList(),
            filteredSessions = emptyList()
        )
    }
    
    fun filterSessions(query: String, filter: SessionFilter) {
        // Stub implementation
    }
    
    fun exportSession(session: RecordingSession, format: ExportFormat) {
        // Stub implementation
    }
    
    fun deleteSession(session: RecordingSession) {
        // Stub implementation
    }
}

/**
 * SessionManagerActivityCompose - Enhanced Compose Session Management
 *
 * Comprehensive interface for managing recording sessions with:
 * - Session browsing with search and filtering capabilities
 * - Detailed session information and statistics display
 * - Export functionality with multiple format support
 * - Session comparison and analysis tools
 * - Data quality assessment and validation
 */
class SessionManagerActivityCompose : BaseComposeActivity<SessionManagerViewModel>() {

    override fun createViewModel(): SessionManagerViewModel = SessionManagerViewModel()

    @Composable
    override fun Content(viewModel: SessionManagerViewModel) {
        IRCameraTheme {
            SessionManagerScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagerScreen(viewModel: SessionManagerViewModel) {
    val uiState by viewModel.sessionState.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf<RecordingSession?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Search
        SessionManagerHeader(
            searchQuery = searchQuery,
            onSearchChange = {
                searchQuery = it
                viewModel.filterSessions(it, uiState.currentFilter)
            },
            onShowFilter = { showFilterDialog = true },
            totalSessions = uiState.sessions.size,
            filteredSessions = uiState.filteredSessions.size
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics Summary
        if (uiState.statistics != null) {
            SessionStatisticsSummary(
                statistics = uiState.statistics!!,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Sessions List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    LoadingSessionsCard()
                }
            } else if (uiState.filteredSessions.isEmpty()) {
                item {
                    EmptySessionsState(
                        hasSearch = searchQuery.isNotEmpty(),
                        onClearSearch = {
                            searchQuery = ""
                            viewModel.filterSessions("", uiState.currentFilter)
                        }
                    )
                }
            } else {
                items(uiState.filteredSessions) { session ->
                    SessionCard(
                        session = session,
                        onViewDetails = { selectedSession = session },
                        onExport = {
                            selectedSession = session
                            showExportDialog = true
                        },
                        onDelete = { viewModel.deleteSession(session) }
                    )
                }
            }
        }
    }

    // Session Details Dialog
    selectedSession?.let { session ->
        if (!showExportDialog) {
            SessionDetailsDialog(
                session = session,
                onDismiss = { selectedSession = null },
                onExport = {
                    showExportDialog = true
                }
            )
        }
    }

    // Export Dialog
    if (showExportDialog && selectedSession != null) {
        ExportSessionDialog(
            session = selectedSession!!,
            onDismiss = {
                showExportDialog = false
                selectedSession = null
            },
            onExport = { session, format ->
                viewModel.exportSession(session, format)
                showExportDialog = false
                selectedSession = null
            }
        )
    }

    // Filter Dialog
    if (showFilterDialog) {
        FilterSessionsDialog(
            currentFilter = uiState.currentFilter,
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { filter ->
                viewModel.filterSessions(searchQuery, filter)
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun SessionManagerHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onShowFilter: () -> Unit,
    totalSessions: Int,
    filteredSessions: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Session Manager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (totalSessions == filteredSessions) "$totalSessions sessions"
                else "$filteredSessions of $totalSessions sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search sessions...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onShowFilter) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }
    }
}

@Composable
fun SessionStatisticsSummary(
    statistics: SessionStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistics Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Total Sessions",
                    value = statistics.totalSessions.toString()
                )
                StatisticItem(
                    label = "Total Duration",
                    value = formatDuration(statistics.totalDuration)
                )
                StatisticItem(
                    label = "Total Data",
                    value = formatDataSize(statistics.totalDataSize)
                )
                StatisticItem(
                    label = "Avg Quality",
                    value = "${statistics.averageQuality}%"
                )
            }
        }
    }
}

@Composable
fun LoadingSessionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Loading sessions...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun EmptySessionsState(
    hasSearch: Boolean,
    onClearSearch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (hasSearch) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (hasSearch) "No sessions found" else "No recording sessions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasSearch) "Try adjusting your search criteria"
                else "Start recording to create your first session",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            if (hasSearch) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClearSearch) {
                    Text("Clear Search")
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: RecordingSession,
    onViewDetails: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewDetails
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (session.participantId.isNotEmpty()) {
                        Text(
                            text = "Participant: ${session.participantId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                            .format(Date(session.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Row {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFF44336)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionMetric(
                    label = "Duration",
                    value = formatDuration(session.duration)
                )
                SessionMetric(
                    label = "Sensors",
                    value = session.sensorCount.toString()
                )
                SessionMetric(
                    label = "Quality",
                    value = "${session.dataQuality}%",
                    isHighlighted = session.dataQuality > 90
                )
                SessionMetric(
                    label = "Size",
                    value = formatDataSize(session.dataSize)
                )
            }

            if (session.protocol.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "Protocol: ${session.protocol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session") },
            text = { Text("Are you sure you want to delete session '${session.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SessionMetric(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SessionDetailsDialog(
    session: RecordingSession,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(session.name) },
        text = {
            LazyColumn {
                item {
                    SessionDetailItem(
                        "Participant ID",
                        session.participantId.ifEmpty { "Not specified" })
                    SessionDetailItem(
                        "Start Time",
                        SimpleDateFormat("MMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault()).format(
                            Date(session.startTime)
                        )
                    )
                    SessionDetailItem("Duration", formatDuration(session.duration))
                    SessionDetailItem("Protocol", session.protocol.ifEmpty { "None" })
                    SessionDetailItem("Sensor Count", session.sensorCount.toString())
                    SessionDetailItem("Data Quality", "${session.dataQuality}%")
                    SessionDetailItem("Data Size", formatDataSize(session.dataSize))

                    if (session.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notes:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = session.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onExport) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SessionDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun ExportSessionDialog(
    session: RecordingSession,
    onDismiss: () -> Unit,
    onExport: (RecordingSession, ExportFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Session") },
        text = {
            Column {
                Text("Select export format for session '${session.name}':")

                Spacer(modifier = Modifier.height(12.dp))

                ExportFormat.values().forEach { format ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = format.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = format.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(session, selectedFormat) }) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FilterSessionsDialog(
    currentFilter: SessionFilter,
    onDismiss: () -> Unit,
    onApplyFilter: (SessionFilter) -> Unit
) {
    var filter by remember { mutableStateOf(currentFilter) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Sessions") },
        text = {
            Column {
                Text("Date Range:")
                // Date range selector would go here

                Spacer(modifier = Modifier.height(12.dp))

                Text("Minimum Quality:")
                Slider(
                    value = filter.minQuality.toFloat(),
                    onValueChange = { filter = filter.copy(minQuality = it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 10
                )
                Text("${filter.minQuality}%")

                Spacer(modifier = Modifier.height(12.dp))

                Text("Has Protocol:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = filter.hasProtocol,
                        onCheckedChange = { filter = filter.copy(hasProtocol = it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Only sessions with research protocol")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApplyFilter(filter) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%d:%02d", minutes, seconds % 60)
    }
}

private fun formatDataSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

// Data classes
data class RecordingSession(
    val id: String,
    val name: String,
    val participantId: String,
    val protocol: String,
    val startTime: Long,
    val duration: Long,
    val sensorCount: Int,
    val dataQuality: Int,
    val dataSize: Long,
    val notes: String = ""
)

data class SessionStatistics(
    val totalSessions: Int,
    val totalDuration: Long,
    val totalDataSize: Long,
    val averageQuality: Int
)

data class SessionFilter(
    val minQuality: Int = 0,
    val hasProtocol: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null
)

enum class ExportFormat(val displayName: String, val description: String) {
    CSV("CSV", "Comma-separated values for spreadsheet analysis"),
    JSON("JSON", "Structured data format for programmatic analysis"),
    MATLAB("MATLAB", "Format compatible with MATLAB analysis tools"),
    EDF("EDF+", "European Data Format for clinical applications")
}

