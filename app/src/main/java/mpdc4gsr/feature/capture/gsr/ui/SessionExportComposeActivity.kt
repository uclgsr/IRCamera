package mpdc4gsr.feature.capture.gsr.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint
import mpdc4gsr.core.designsystem.theme.IRCameraTheme
import mpdc4gsr.feature.capture.gsr.presentation.ExportDestination
import mpdc4gsr.feature.capture.gsr.presentation.ExportFormat
import mpdc4gsr.feature.capture.gsr.presentation.GSRSession
import mpdc4gsr.feature.capture.gsr.presentation.SessionExportViewModel

@AndroidEntryPoint
class SessionExportComposeActivity : BaseComposeActivity<SessionExportViewModel>() {
    override fun createViewModel(): SessionExportViewModel = viewModels<SessionExportViewModel>().value

    @Composable
    override fun Content(viewModel: SessionExportViewModel) {
        IRCameraTheme {
            SessionExportScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionExportScreen(
    viewModel: SessionExportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.exportState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("Export GSR Session") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.startExport() },
                    enabled = !uiState.isExporting && uiState.selectedSessions.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Start export",
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
        )
        // Content
        when {
            uiState.isLoading -> {
                LoadingContent()
            }

            uiState.error != null -> {
                val errorMessage = uiState.error ?: "Unknown error"
                ErrorContent(
                    error = errorMessage,
                    onRetry = { viewModel.loadSessions() },
                )
            }

            uiState.sessions.isEmpty() -> {
                EmptyContent()
            }

            else -> {
                ExportContent(
                    uiState = uiState,
                    onSessionToggle = { session -> viewModel.toggleSessionSelection(session) },
                    onExportFormatChange = { format -> viewModel.setExportFormat(format) },
                    onExportDestinationChange = { destination -> viewModel.setExportDestination(destination) },
                    onStartExport = { viewModel.startExport() },
                )
            }
        }
    }
}

@Composable
private fun ExportContent(
    uiState: SessionExportViewModel.SessionExportState,
    onSessionToggle: (GSRSession) -> Unit,
    onExportFormatChange: (ExportFormat) -> Unit,
    onExportDestinationChange: (ExportDestination) -> Unit,
    onStartExport: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Export Progress (if exporting)
        if (uiState.isExporting) {
            item {
                ExportProgressCard(
                    progress = uiState.exportProgress,
                    currentFile = uiState.currentExportFile,
                )
            }
        }
        // Export Configuration
        item {
            ExportConfigurationCard(
                selectedFormat = uiState.exportFormat,
                selectedDestination = uiState.exportDestination,
                onFormatChange = onExportFormatChange,
                onDestinationChange = onExportDestinationChange,
            )
        }
        // Session Selection Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Select Sessions to Export",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.selectedSessions.size} of ${uiState.sessions.size} sessions selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        // Session List
        items(uiState.sessions) { session ->
            SessionSelectionCard(
                session = session,
                isSelected = session in uiState.selectedSessions,
                onToggle = { onSessionToggle(session) },
            )
        }
        // Export Action
        if (!uiState.isExporting && uiState.selectedSessions.isNotEmpty()) {
            item {
                Button(
                    onClick = onStartExport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.selectedSessions.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Selected Sessions")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionSelectionCard(
    session: GSRSession,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Text(
                    text = "Duration: ${session.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                Text(
                    text = "Data points: ${session.dataPointCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun ExportConfigurationCard(
    selectedFormat: ExportFormat,
    selectedDestination: ExportDestination,
    onFormatChange: (ExportFormat) -> Unit,
    onDestinationChange: (ExportDestination) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Export Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Export Format Selection
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExportFormat.values().forEach { format ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedFormat == format,
                                onClick = { onFormatChange(format) },
                            ).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedFormat == format,
                        onClick = { onFormatChange(format) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = format.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Export Destination Selection
            Text(
                text = "Export Destination",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExportDestination.values().forEach { destination ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedDestination == destination,
                                onClick = { onDestinationChange(destination) },
                            ).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedDestination == destination,
                        onClick = { onDestinationChange(destination) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = destination.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportProgressCard(
    progress: Float,
    currentFile: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Exporting...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            if (currentFile != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current: $currentFile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading sessions...",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Error loading sessions",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "No sessions",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No sessions available",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "GSR sessions will appear here when available for export",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
