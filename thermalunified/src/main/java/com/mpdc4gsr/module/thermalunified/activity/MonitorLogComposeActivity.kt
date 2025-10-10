package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonitorLogComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel = viewModels<ThermalViewModel>().value

    data class LogEntry(
        val timestamp: String,
        val temperature: Float,
        val location: String,
        val notes: String = "",
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        // Sample log data
        val logEntries =
            remember {
                mutableStateListOf(
                    LogEntry("2024-10-01 10:30:00", 25.5f, "Location A", "Normal reading"),
                    LogEntry("2024-10-01 10:25:00", 27.2f, "Location B", "Elevated temperature"),
                    LogEntry("2024-10-01 10:20:00", 24.8f, "Location C", ""),
                    LogEntry("2024-10-01 10:15:00", 26.1f, "Location A", "Follow-up check"),
                )
            }
        var showFilterDialog by remember { mutableStateOf(false) }
        var showAddLogDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Monitor Log",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        // Perform heavy IO operations on background thread
                                        withContext(Dispatchers.IO) {
                                            // Export logs to CSV file
                                            val csv =
                                                buildString {
                                                    appendLine("Timestamp,Temperature,Location,Notes")
                                                    logEntries.forEach { entry ->
                                                        appendLine(
                                                            "${entry.timestamp},${entry.temperature},${entry.location},${entry.notes}",
                                                        )
                                                    }
                                                }
                                            // Create file in Downloads directory
                                            val contentValues =
                                                android.content.ContentValues().apply {
                                                    put(
                                                        android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME,
                                                        "monitor_log_${System.currentTimeMillis()}.csv",
                                                    )
                                                    put(
                                                        android.provider.MediaStore.Files.FileColumns.MIME_TYPE,
                                                        "text/csv"
                                                    )
                                                    put(
                                                        android.provider.MediaStore.Files.FileColumns.RELATIVE_PATH,
                                                        android.os.Environment.DIRECTORY_DOWNLOADS,
                                                    )
                                                }
                                            val uri =
                                                context.contentResolver.insert(
                                                    android.provider.MediaStore.Files
                                                        .getContentUri("external"),
                                                    contentValues,
                                                )
                                            uri?.let {
                                                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                                    outputStream.write(csv.toByteArray())
                                                }
                                            }
                                        }
                                        // Show success message on main thread
                                        snackbarHostState.showSnackbar("Logs exported to Downloads")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Export failed: ${e.message}")
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Export", tint = Color.White)
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black,
                            ),
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddLogDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Log")
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color(0xFF16131E),
            ) { paddingValues ->
                if (logEntries.isEmpty()) {
                    // Empty state
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "No log entries",
                                modifier = Modifier.size(64.dp),
                                tint = Color.White.copy(alpha = 0.3f),
                            )
                            Text(
                                "No log entries",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                            )
                            Text(
                                "Start monitoring to create logs",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp,
                            )
                        }
                    }
                } else {
                    // Log list
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(logEntries) { entry ->
                            LogEntryCard(entry)
                        }
                    }
                }
            }
            // Add Log Entry Dialog
            if (showAddLogDialog) {
                var newTemp by remember { mutableStateOf("25.0") }
                var newLocation by remember { mutableStateOf("") }
                var newNotes by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showAddLogDialog = false },
                    title = { Text("Add Log Entry") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newTemp,
                                onValueChange = { newTemp = it },
                                label = { Text("Temperature (°C)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = newLocation,
                                onValueChange = { newLocation = it },
                                label = { Text("Location") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = newNotes,
                                onValueChange = { newNotes = it },
                                label = { Text("Notes (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val temp = newTemp.toFloatOrNull() ?: 25.0f
                            val timestamp =
                                java.text
                                    .SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    .format(java.util.Date())
                            logEntries.add(0, LogEntry(timestamp, temp, newLocation, newNotes))
                            showAddLogDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Log entry added")
                            }
                        }) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddLogDialog = false }) {
                            Text("Cancel")
                        }
                    },
                )
            }
        }
    }

    @Composable
    fun LogEntryCard(entry: LogEntry) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A1A),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                // Header row with timestamp and temperature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.timestamp,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Location",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.6f),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                entry.location,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                    // Temperature badge
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    when {
                                        entry.temperature > 30f -> Color(0xFFFF4747)
                                        entry.temperature > 26f -> Color(0xFFFFA500)
                                        else -> Color(0xFF06AAFF)
                                    },
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            "%.1f°C".format(entry.temperature),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                // Notes section
                if (entry.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        entry.notes,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
