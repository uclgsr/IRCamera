package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.common.TitleBar
import mpdc4gsr.core.ui.components.common.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

data class ResearchSession(
    val id: String,
    val title: String,
    val participantId: String,
    val date: String,
    val duration: String,
    val status: SessionStatus,
    val sensorTypes: List<String>,
    val dataSize: String,
    val progress: Float = 0f // 0.0 to 1.0
)

enum class SessionStatus {
    COMPLETED,
    IN_PROGRESS,
    PAUSED,
    FAILED,
    SCHEDULED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagerScreen(
    onNavigateBack: () -> Unit = {},
    onCreateNewSession: () -> Unit = {},
    onViewSession: (ResearchSession) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val sessions = remember { getSampleSessions() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val filteredSessions = sessions.filter { session ->
        when (selectedTab) {
            0 -> true // All
            1 -> session.status == SessionStatus.COMPLETED
            2 -> session.status == SessionStatus.IN_PROGRESS
            3 -> session.status == SessionStatus.SCHEDULED
            else -> true
        }
    }.filter { session ->
        if (searchQuery.isBlank()) true
        else session.title.contains(searchQuery, ignoreCase = true) ||
                session.participantId.contains(searchQuery, ignoreCase = true)
    }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Session Manager",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "New session",
                    onClick = onCreateNewSession
                )
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search sessions...") },
                    leadingIcon = {
                        IconButton(onClick = { keyboardController?.hide() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6B73FF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6B73FF)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                        }
                    )
                )
                // Tab Row
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(selectedTab),
                            color = Color(0xFF6B73FF)
                        )
                    }
                ) {
                    val tabs = listOf("All", "Completed", "Active", "Scheduled")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) Color(0xFF6B73FF) else Color.White
                                )
                            }
                        )
                    }
                }
                // Session Statistics
                SessionStatsCard(
                    sessions = sessions,
                    modifier = Modifier.padding(16.dp)
                )
                // Sessions List
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSessions) { session ->
                        SessionItem(
                            session = session,
                            onClick = { onViewSession(session) }
                        )
                    }
                    if (filteredSessions.isEmpty()) {
                        item {
                            EmptySessionsState(
                                searchQuery = searchQuery,
                                onCreateNew = onCreateNewSession
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionStatsCard(
    sessions: List<ResearchSession>,
    modifier: Modifier = Modifier
) {
    val completedSessions = sessions.count { it.status == SessionStatus.COMPLETED }
    val activeSessions = sessions.count { it.status == SessionStatus.IN_PROGRESS }
    val totalDuration = sessions.filter { it.status == SessionStatus.COMPLETED }
        .sumOf { parseDuration(it.duration) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Completed",
                value = completedSessions.toString(),
                color = Color(0xFF4ECDC4)
            )
            StatItem(
                label = "Active",
                value = activeSessions.toString(),
                color = Color(0xFF6B73FF)
            )
            StatItem(
                label = "Total Time",
                value = formatTotalDuration(totalDuration),
                color = Color(0xFFFF6B6B)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFCCFFFFFF)
        )
    }
}

@Composable
fun SessionItem(
    session: ResearchSession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                SessionStatusBadge(status = session.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Session Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${session.participantId}",
                    fontSize = 12.sp,
                    color = Color(0xFFCCFFFFFF)
                )
                Text(
                    text = session.date,
                    fontSize = 12.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Progress Bar (for in-progress sessions)
            if (session.status == SessionStatus.IN_PROGRESS) {
                LinearProgressIndicator(
                    progress = { session.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF6B73FF),
                    trackColor = Color(0xFF404040)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Duration",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.duration,
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Data Size",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.dataSize,
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                }
                // Sensor Type Icons
                Row {
                    session.sensorTypes.forEach { sensorType ->
                        Icon(
                            imageVector = when (sensorType) {
                                "GSR" -> Icons.Default.Sensors
                                "Thermal" -> Icons.Default.Thermostat
                                "Camera" -> Icons.Default.Camera
                                else -> Icons.Default.DeviceHub
                            },
                            contentDescription = sensorType,
                            tint = when (sensorType) {
                                "GSR" -> Color(0xFF4ECDC4)
                                "Thermal" -> Color(0xFFFF6B6B)
                                "Camera" -> Color.White
                                else -> Color(0xFF6B73FF)
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionStatusBadge(status: SessionStatus) {
    val (color, text) = when (status) {
        SessionStatus.COMPLETED -> Color(0xFF4ECDC4) to "Completed"
        SessionStatus.IN_PROGRESS -> Color(0xFF6B73FF) to "Active"
        SessionStatus.PAUSED -> Color(0xFFFFB74D) to "Paused"
        SessionStatus.FAILED -> Color(0xFFFF6B6B) to "Failed"
        SessionStatus.SCHEDULED -> Color(0xFF9E9E9E) to "Scheduled"
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptySessionsState(
    searchQuery: String,
    onCreateNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (searchQuery.isBlank()) Icons.AutoMirrored.Filled.Assignment else Icons.Default.SearchOff,
            contentDescription = if (searchQuery.isBlank()) "No Sessions" else "No Search Results",
            tint = Color(0xFF6B73FF),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isBlank()) "No sessions yet" else "No sessions found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isBlank())
                "Create your first research session to get started"
            else
                "Try adjusting your search criteria",
            fontSize = 14.sp,
            color = Color(0xFFCCFFFFFF)
        )
        if (searchQuery.isBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateNew,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B73FF)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create New Session",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Session")
            }
        }
    }
}

private fun parseDuration(duration: String): Int {
    // Parse "25:42" format to minutes
    val parts = duration.split(":")
    return if (parts.size == 2) {
        parts[0].toIntOrNull()?.let { minutes ->
            parts[1].toIntOrNull()?.let { seconds ->
                minutes + (seconds / 60)
            }
        } ?: 0
    } else 0
}

private fun formatTotalDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}

private fun getSampleSessions() = listOf(
    ResearchSession(
        id = "SESSION-001",
        title = "Stress Response Study A",
        participantId = "P001-UCL-2024",
        date = "Dec 15, 2024",
        duration = "25:42",
        status = SessionStatus.COMPLETED,
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        dataSize = "2.4 MB"
    ),
    ResearchSession(
        id = "SESSION-002",
        title = "Cognitive Load Assessment",
        participantId = "P002-UCL-2024",
        date = "Dec 16, 2024",
        duration = "18:30",
        status = SessionStatus.IN_PROGRESS,
        sensorTypes = listOf("GSR", "Thermal"),
        dataSize = "1.2 MB",
        progress = 0.65f
    ),
    ResearchSession(
        id = "SESSION-003",
        title = "Emotion Recognition Task",
        participantId = "P003-UCL-2024",
        date = "Dec 17, 2024",
        duration = "32:15",
        status = SessionStatus.COMPLETED,
        sensorTypes = listOf("GSR", "Camera"),
        dataSize = "3.1 MB"
    ),
    ResearchSession(
        id = "SESSION-004",
        title = "Baseline Measurement",
        participantId = "P001-UCL-2024",
        date = "Dec 18, 2024",
        duration = "15:00",
        status = SessionStatus.SCHEDULED,
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        dataSize = "0 MB"
    ),
    ResearchSession(
        id = "SESSION-005",
        title = "Social Interaction Study",
        participantId = "P004-UCL-2024",
        date = "Dec 14, 2024",
        duration = "28:45",
        status = SessionStatus.FAILED,
        sensorTypes = listOf("GSR"),
        dataSize = "0.8 MB"
    )
)

@Preview(showBackground = true)
@Composable
fun SessionManagerScreenPreview() {
    SessionManagerScreen()
}
