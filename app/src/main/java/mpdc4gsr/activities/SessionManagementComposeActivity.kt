package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.components.IRCameraTopAppBar
import mpdc4gsr.ui.theme.IRCameraTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class SessionInfo(
    val id: String,
    val participantId: String,
    val participantName: String,
    val sessionType: String,
    val startTime: Long,
    val duration: Long,
    val status: SessionStatus,
    val recordedSensors: List<String>
)

enum class SessionStatus {
    ACTIVE, PAUSED, COMPLETED, ERROR
}

class SessionManagementComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            SessionManagementScreen()
        }
    }

    @Composable
    private fun SessionManagementScreen() {
        var selectedTab by remember { mutableStateOf(0) }
        var sessions by remember { mutableStateOf(generateSampleSessions()) }
        var currentSessionDuration by remember { mutableStateOf(0L) }
        var showNewSessionDialog by remember { mutableStateOf(false) }

        // Update active session duration
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                val activeSession = sessions.find { it.status == SessionStatus.ACTIVE }
                if (activeSession != null) {
                    currentSessionDuration = (System.currentTimeMillis() - activeSession.startTime) / 1000
                }
            }
        }

        Scaffold(
            topBar = {
                IRCameraTopAppBar(
                    title = "Session Management",
                    onNavigationClick = { finish() },
                    actions = {
                        IconButton(onClick = { showNewSessionDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New Session")
                        }
                    }
                )
            },
            floatingActionButton = {
                val activeSession = sessions.find { it.status == SessionStatus.ACTIVE }
                if (activeSession != null) {
                    FloatingActionButton(
                        onClick = {
                            sessions = sessions.map { session ->
                                if (session.status == SessionStatus.ACTIVE) {
                                    session.copy(
                                        status = if (session.status == SessionStatus.ACTIVE) SessionStatus.PAUSED else SessionStatus.ACTIVE,
                                        duration = currentSessionDuration
                                    )
                                } else session
                            }
                        }
                    ) {
                        Icon(
                            if (activeSession.status == SessionStatus.ACTIVE) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (activeSession.status == SessionStatus.ACTIVE) "Pause Session" else "Resume Session"
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Active session status
                val activeSession = sessions.find { it.status == SessionStatus.ACTIVE }
                if (activeSession != null) {
                    ActiveSessionCard(
                        session = activeSession,
                        duration = currentSessionDuration,
                        onStopSession = {
                            sessions = sessions.map { session ->
                                if (session.id == activeSession.id) {
                                    session.copy(
                                        status = SessionStatus.COMPLETED,
                                        duration = currentSessionDuration
                                    )
                                } else session
                            }
                        }
                    )
                }

                // Tab bar
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Recent Sessions") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Templates") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Analytics") }
                    )
                }

                // Tab content
                when (selectedTab) {
                    0 -> SessionsList(sessions = sessions.filter { it.status != SessionStatus.ACTIVE })
                    1 -> SessionTemplates()
                    2 -> SessionAnalytics(sessions = sessions)
                }
            }
        }

        if (showNewSessionDialog) {
            NewSessionDialog(
                onDismiss = { showNewSessionDialog = false },
                onCreateSession = { participantId, sessionType ->
                    val newSession = SessionInfo(
                        id = "session_${System.currentTimeMillis()}",
                        participantId = participantId,
                        participantName = "Participant $participantId",
                        sessionType = sessionType,
                        startTime = System.currentTimeMillis(),
                        duration = 0,
                        status = SessionStatus.ACTIVE,
                        recordedSensors = listOf("GSR", "Camera", "Thermal")
                    )
                    sessions = sessions + newSession
                    showNewSessionDialog = false
                }
            )
        }
    }

    @Composable
    private fun ActiveSessionCard(
        session: SessionInfo,
        duration: Long,
        onStopSession: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active Session",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = session.participantName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Row {
                        IconButton(onClick = onStopSession) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop Session",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Duration: ${formatDuration(duration)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = session.sessionType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    session.recordedSensors.forEach { sensor ->
                        AssistChip(
                            onClick = { },
                            label = { Text(sensor) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SessionsList(sessions: List<SessionInfo>) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions) { session ->
                SessionCard(session = session)
            }
        }
    }

    @Composable
    private fun SessionCard(session: SessionInfo) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* View session details */ }
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = session.participantName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${session.participantId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusChip(status = session.status)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(session.startTime)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${formatDuration(session.duration)} • ${session.sessionType}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (session.recordedSensors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        session.recordedSensors.take(3).forEach { sensor ->
                            AssistChip(
                                onClick = { },
                                label = { Text(sensor) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        if (session.recordedSensors.size > 3) {
                            Text(
                                text = "+${session.recordedSensors.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusChip(status: SessionStatus) {
        val (color, text) = when (status) {
            SessionStatus.ACTIVE -> MaterialTheme.colorScheme.primary to "Active"
            SessionStatus.PAUSED -> MaterialTheme.colorScheme.secondary to "Paused"
            SessionStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to "Completed"
            SessionStatus.ERROR -> MaterialTheme.colorScheme.error to "Error"
        }

        Surface(
            color = color,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }

    @Composable
    private fun SessionTemplates() {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf(
                "Standard GSR Recording",
                "Thermal + GSR Combined",
                "Multi-Modal Session",
                "Quick Test Session"
            )) { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Use template */ }
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
                                text = template,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Template configuration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Use Template"
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SessionAnalytics(sessions: List<SessionInfo>) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Session Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val completedSessions = sessions.count { it.status == SessionStatus.COMPLETED }
                        val totalDuration = sessions.filter { it.status == SessionStatus.COMPLETED }
                            .sumOf { it.duration }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem("Total Sessions", completedSessions.toString())
                            StatItem("Total Duration", formatDuration(totalDuration))
                            StatItem("Avg Duration", 
                                if (completedSessions > 0) formatDuration(totalDuration / completedSessions) else "0:00"
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StatItem(label: String, value: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun NewSessionDialog(
        onDismiss: () -> Unit,
        onCreateSession: (String, String) -> Unit
    ) {
        var participantId by remember { mutableStateOf("") }
        var sessionType by remember { mutableStateOf("Standard GSR Recording") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("New Session") },
            text = {
                Column {
                    OutlinedTextField(
                        value = participantId,
                        onValueChange = { participantId = it },
                        label = { Text("Participant ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Session Type:", style = MaterialTheme.typography.labelMedium)
                    // Simplified dropdown
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Show dropdown */ }
                    ) {
                        Text(
                            text = sessionType,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        if (participantId.isNotBlank()) {
                            onCreateSession(participantId, sessionType)
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }

    private fun generateSampleSessions(): List<SessionInfo> {
        return listOf(
            SessionInfo(
                id = "session_1",
                participantId = "P001",
                participantName = "Participant 001",
                sessionType = "Standard GSR Recording",
                startTime = System.currentTimeMillis() - 3600000,
                duration = 1800,
                status = SessionStatus.COMPLETED,
                recordedSensors = listOf("GSR", "Accelerometer")
            ),
            SessionInfo(
                id = "session_2",
                participantId = "P002",
                participantName = "Participant 002",
                sessionType = "Thermal + GSR Combined",
                startTime = System.currentTimeMillis() - 7200000,
                duration = 2400,
                status = SessionStatus.COMPLETED,
                recordedSensors = listOf("GSR", "Thermal", "Camera")
            )
        )
    }
}