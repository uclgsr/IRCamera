package mpdc4gsr.presentation.screens.gsr

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import dagger.hilt.android.AndroidEntryPoint
import mpdc4gsr.presentation.screens.gsr.SessionManagerViewModel
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SessionManagerComposeActivity : BaseComposeActivity<SessionManagerViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SessionManagerComposeActivity::class.java))
        }
    }

    override fun createViewModel(): SessionManagerViewModel {
        return viewModels<SessionManagerViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: SessionManagerViewModel) {
        var searchQuery by remember { mutableStateOf("") }
        var selectedSessions by remember { mutableStateOf(setOf<String>()) }
        var showFilterDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Session Manager",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                            IconButton(onClick = {
                                // TODO: Export all sessions
                                android.widget.Toast.makeText(
                                    this@SessionManagerComposeActivity,
                                    "Exporting all sessions...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    this@SessionManagerComposeActivity,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            // Start new recording session
                            MultiModalRecordingComposeActivity.startActivity(this@SessionManagerComposeActivity)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Session")
                    }
                }
            ) { paddingValues ->
                val context = LocalContext.current
                SessionManagerContent(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedSessions = selectedSessions,
                    onSessionSelectionChange = { selectedSessions = it },
                    viewModel = viewModel,
                    context = context,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showFilterDialog) {
            SessionFilterDialog(
                onDismiss = { showFilterDialog = false },
                onApplyFilter = {
                    // TODO: Apply filter logic to session list
                    android.widget.Toast.makeText(
                        this@SessionManagerComposeActivity,
                        "Applying filters...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    showFilterDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionManagerContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSessions: Set<String>,
    onSessionSelectionChange: (Set<String>) -> Unit,
    viewModel: SessionManagerViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search Sessions") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                }
            )
        )
        // Session Statistics Card
        SessionStatisticsCard(
            selectedCount = selectedSessions.size,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val mockSessions = listOf(
                com.mpdc4gsr.gsr.model.SessionInfo(
                    sessionId = "session_1",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis(),
                    participantId = "P001",
                    studyName = "GSR Study Session"
                ),
                com.mpdc4gsr.gsr.model.SessionInfo(
                    sessionId = "session_2",
                    startTime = System.currentTimeMillis() - 86400000,
                    endTime = System.currentTimeMillis() - 82800000,
                    participantId = "P002",
                    studyName = "Thermal Analysis"
                ),
                com.mpdc4gsr.gsr.model.SessionInfo(
                    sessionId = "session_3",
                    startTime = System.currentTimeMillis() - 172800000,
                    endTime = null,
                    participantId = "P003",
                    studyName = "Multi-modal Recording"
                )
            )
            items(mockSessions.filter {
                (it.studyName ?: "").contains(searchQuery, ignoreCase = true)
            }) { session ->
                SessionCard(
                    session = session,
                    isSelected = selectedSessions.contains(session.sessionId),
                    onSelectionChange = { isSelected ->
                        if (isSelected) {
                            onSessionSelectionChange(selectedSessions + session.sessionId)
                        } else {
                            onSessionSelectionChange(selectedSessions - session.sessionId)
                        }
                    },
                    onClick = {
                        SessionDetailComposeActivity.startActivity(
                            context = context,
                            sessionId = session.sessionId
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionStatisticsCard(
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Sessions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "12", // Replace with actual count
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (selectedCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = selectedCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionInfo,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.studyName ?: session.sessionId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Started: ${dateFormatter.format(session.startTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Status chip - determine status from endTime
                val status = if (session.endTime == null) "active" else "completed"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (status) {
                        "active" -> Color(0xFF4CAF50)
                        "completed" -> Color(0xFF2196F3)
                        else -> Color(0xFF9E9E9E)
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            val context = androidx.compose.ui.platform.LocalContext.current
            IconButton(onClick = {
                // TODO: Show session options menu
                android.widget.Toast.makeText(
                    context,
                    "Session options coming soon",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Session options")
            }
        }
    }
}

@Composable
private fun SessionFilterDialog(
    onDismiss: () -> Unit,
    onApplyFilter: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Sessions") },
        text = {
            Column {
                Text("Select filter criteria:")
                // Add filter options here
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApplyFilter()
                onDismiss()
            }) {
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