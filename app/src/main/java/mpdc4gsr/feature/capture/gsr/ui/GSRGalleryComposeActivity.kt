package mpdc4gsr.feature.capture.gsr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.designsystem.AppBaseViewModel
import mpdc4gsr.core.designsystem.theme.IRCameraTheme

@AndroidEntryPoint
class GSRGalleryComposeActivity : BaseComposeActivity<GSRGalleryViewModel>() {
    override fun createViewModel(): GSRGalleryViewModel = GSRGalleryViewModel()

    @Composable
    override fun Content(viewModel: GSRGalleryViewModel) {
        IRCameraTheme {
            GSRGalleryScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRGalleryScreen(viewModel: GSRGalleryViewModel) {
    val uiState by viewModel.galleryState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var showFilterDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadGSRSessions()
    }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Header with Search and View Toggle
        GSRGalleryHeader(
            searchQuery = searchQuery,
            onSearchChange = {
                searchQuery = it
                viewModel.filterSessions(it)
            },
            isGridView = isGridView,
            onViewToggle = { isGridView = !isGridView },
            onShowFilter = { showFilterDialog = true },
            sessionsCount = uiState.sessions.size,
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Content based on view mode
        if (isGridView) {
            GSRSessionGrid(
                sessions = uiState.filteredSessions,
                onSessionClick = { viewModel.openSession(it) },
                onSessionLongClick = { viewModel.selectSession(it) },
                selectedSessions = uiState.selectedSessions,
            )
        } else {
            GSRSessionList(
                sessions = uiState.filteredSessions,
                onSessionClick = { viewModel.openSession(it) },
                onSessionLongClick = { viewModel.selectSession(it) },
                selectedSessions = uiState.selectedSessions,
            )
        }
    }
    // Filter Dialog
    if (showFilterDialog) {
        GSRFilterDialog(
            currentFilter = uiState.currentFilter,
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { filter ->
                viewModel.applyFilter(filter)
                showFilterDialog = false
            },
        )
    }
    // Selection Actions
    if (uiState.selectedSessions.isNotEmpty()) {
        SelectionActionsBar(
            selectedCount = uiState.selectedSessions.size,
            onExportSelected = { viewModel.exportSelectedSessions() },
            onDeleteSelected = { viewModel.deleteSelectedSessions() },
            onClearSelection = { viewModel.clearSelection() },
        )
    }
}

@Composable
fun GSRGalleryHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isGridView: Boolean,
    onViewToggle: () -> Unit,
    onShowFilter: () -> Unit,
    sessionsCount: Int,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "GSR Gallery",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$sessionsCount sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search GSR sessions...") },
                leadingIcon = {
                    IconButton(onClick = { keyboardController?.hide() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                    KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                        },
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onViewToggle) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = if (isGridView) "List View" else "Grid View",
                )
            }
            IconButton(onClick = onShowFilter) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }
    }
}

@Composable
fun GSRSessionGrid(
    sessions: List<GSRSession>,
    onSessionClick: (GSRSession) -> Unit,
    onSessionLongClick: (GSRSession) -> Unit,
    selectedSessions: Set<String>,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sessions) { session ->
            GSRSessionGridItem(
                session = session,
                isSelected = selectedSessions.contains(session.id),
                onClick = { onSessionClick(session) },
                onLongClick = { onSessionLongClick(session) },
            )
        }
    }
}

@Composable
fun GSRSessionList(
    sessions: List<GSRSession>,
    onSessionClick: (GSRSession) -> Unit,
    onSessionLongClick: (GSRSession) -> Unit,
    selectedSessions: Set<String>,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sessions) { session ->
            GSRSessionListItem(
                session = session,
                isSelected = selectedSessions.contains(session.id),
                onClick = { onSessionClick(session) },
                onLongClick = { onSessionLongClick(session) },
            )
        }
    }
}

@Composable
fun GSRSessionGridItem(
    session: GSRSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
        ) {
            // Session preview visualization
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "GSR Data",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = "${formatDuration(session.duration)} • ${session.sampleCount} samples",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Quality: ${session.dataQuality}%",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        when {
                            session.dataQuality > 90 -> Color(0xFF4CAF50)
                            session.dataQuality > 70 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        },
                )
            }
        }
    }
}

@Composable
fun GSRSessionListItem(
    session: GSRSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "GSR Data",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (session.participantId.isNotEmpty()) {
                    Text(
                        text = "Participant: ${session.participantId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                Row {
                    Text(
                        text = formatDuration(session.duration),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(" • ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${session.sampleCount} samples",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(" • ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Quality: ${session.dataQuality}%",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            when {
                                session.dataQuality > 90 -> Color(0xFF4CAF50)
                                session.dataQuality > 70 -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            },
                    )
                }
            }
            Text(
                text = formatFileSize(session.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun GSRFilterDialog(
    currentFilter: GSRFilter,
    onDismiss: () -> Unit,
    onApplyFilter: (GSRFilter) -> Unit,
) {
    var filter by remember { mutableStateOf(currentFilter) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter GSR Sessions") },
        text = {
            Column {
                Text("Minimum Quality:")
                Slider(
                    value = filter.minQuality.toFloat(),
                    onValueChange = { filter = filter.copy(minQuality = it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 10,
                )
                Text("${filter.minQuality}%")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Minimum Duration (minutes):")
                Slider(
                    value = filter.minDuration.toFloat(),
                    onValueChange = { filter = filter.copy(minDuration = it.toInt()) },
                    valueRange = 0f..60f,
                    steps = 12,
                )
                Text("${filter.minDuration} min")
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = filter.hasParticipant,
                        onCheckedChange = { filter = filter.copy(hasParticipant = it) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Only sessions with participant ID")
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
        },
    )
}

@Composable
fun SelectionActionsBar(
    selectedCount: Int,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row {
                TextButton(onClick = onClearSelection) {
                    Text("Clear")
                }
                OutlinedButton(onClick = onExportSelected) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
                Button(
                    onClick = onDeleteSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

// Helper functions
private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    return String.format("%d:%02d", minutes, seconds % 60)
}

private fun formatFileSize(bytes: Long): String =
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }

// Data classes
data class GSRSession(
    val id: String,
    val name: String,
    val participantId: String = "",
    val duration: Long,
    val sampleCount: Int,
    val dataQuality: Int,
    val fileSize: Long,
    val timestamp: Long,
)

data class GSRFilter(
    val minQuality: Int = 0,
    val minDuration: Int = 0,
    val hasParticipant: Boolean = false,
)

data class GSRGalleryUiState(
    val sessions: List<GSRSession> = emptyList(),
    val filteredSessions: List<GSRSession> = emptyList(),
    val selectedSessions: Set<String> = emptySet(),
    val currentFilter: GSRFilter = GSRFilter(),
    val isLoading: Boolean = false,
)

// ViewModel
class GSRGalleryViewModel : AppBaseViewModel() {
    private val _galleryState = MutableStateFlow(GSRGalleryUiState())
    val galleryState: StateFlow<GSRGalleryUiState> = _galleryState.asStateFlow()

    fun loadGSRSessions() {
        _galleryState.value = _galleryState.value.copy(isLoading = true)
        val mockSessions =
            listOf(
                GSRSession(
                    "1",
                    "Stress Response Study",
                    "P001",
                    1800000,
                    460800,
                    95,
                    2048576,
                    System.currentTimeMillis() - 86400000,
                ),
                GSRSession(
                    "2",
                    "Cognitive Load Test",
                    "P002",
                    1200000,
                    307200,
                    87,
                    1048576,
                    System.currentTimeMillis() - 172800000,
                ),
                GSRSession(
                    "3",
                    "Emotion Recognition",
                    "",
                    2700000,
                    691200,
                    92,
                    3145728,
                    System.currentTimeMillis() - 259200000,
                ),
                GSRSession(
                    "4",
                    "Quick Recording",
                    "",
                    300000,
                    76800,
                    78,
                    262144,
                    System.currentTimeMillis() - 345600000,
                ),
                GSRSession(
                    "5",
                    "Baseline Measurement",
                    "P001",
                    600000,
                    153600,
                    98,
                    524288,
                    System.currentTimeMillis() - 432000000,
                ),
            )
        _galleryState.value =
            _galleryState.value.copy(
                sessions = mockSessions,
                filteredSessions = mockSessions,
                isLoading = false,
            )
    }

    fun filterSessions(query: String) {
        val filtered =
            _galleryState.value.sessions.filter { session ->
                query.isEmpty() ||
                    session.name.contains(query, ignoreCase = true) ||
                    session.participantId.contains(query, ignoreCase = true)
            }
        _galleryState.value = _galleryState.value.copy(filteredSessions = filtered)
    }

    fun applyFilter(filter: GSRFilter) {
        val filtered =
            _galleryState.value.sessions.filter { session ->
                session.dataQuality >= filter.minQuality &&
                    (session.duration / 60000) >= filter.minDuration &&
                    (!filter.hasParticipant || session.participantId.isNotEmpty())
            }
        _galleryState.value =
            _galleryState.value.copy(
                filteredSessions = filtered,
                currentFilter = filter,
            )
    }

    fun selectSession(session: GSRSession) {
        val currentSelection = _galleryState.value.selectedSessions
        val newSelection =
            if (currentSelection.contains(session.id)) {
                currentSelection - session.id
            } else {
                currentSelection + session.id
            }
        _galleryState.value = _galleryState.value.copy(selectedSessions = newSelection)
    }

    fun clearSelection() {
        _galleryState.value = _galleryState.value.copy(selectedSessions = emptySet())
    }

    fun openSession(session: GSRSession) {
        // Implementation for opening session details
    }

    fun exportSelectedSessions() {
        // Implementation for exporting selected sessions
    }

    fun deleteSelectedSessions() {
        val selectedIds = _galleryState.value.selectedSessions
        val updatedSessions = _galleryState.value.sessions.filter { !selectedIds.contains(it.id) }
        _galleryState.value =
            _galleryState.value.copy(
                sessions = updatedSessions,
                filteredSessions = updatedSessions,
                selectedSessions = emptySet(),
            )
    }
}
