package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

/**
 * SessionDetailComposeActivity - Modern Compose Implementation
 *
 * Demonstrates migration from simple BaseBindingActivity to Compose:
 * - Clean, modern Material 3 UI
 * - Better data presentation with cards and structured layout
 * - Enhanced user interactions with action buttons
 * - Consistent theming with other modernized activities
 */
class SessionDetailComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        private const val EXTRA_SESSION_ID = "session_id"

        fun startActivity(
            context: Context,
            sessionId: String,
        ) {
            val intent = Intent(context, SessionDetailComposeActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): AppBaseViewModel {
        return viewModels<AppBaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "Unknown"

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Session Details",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* TODO: Implement share session
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = { /* TODO: Implement export session
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                SessionDetailContent(
                    sessionId = sessionId,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun SessionDetailContent(
    sessionId: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Session Overview Card
        SessionOverviewCard(sessionId = sessionId)

        // Session Statistics Card
        SessionStatisticsCard()

        // Data Quality Card
        DataQualityCard()

        // Session Timeline Card
        SessionTimelineCard()

        // Actions Card
        SessionActionsCard(
            onViewData = { /* TODO: Implement navigate to data view
                     *   - Implement callback logic for onViewData
                     *   - Handle data/state updates
                     *   - Provide user feedback
                     */ },
            onExportData = { /* TODO: Implement export session data
                     *   - Implement callback logic for onExportData
                     *   - Handle data/state updates
                     *   - Provide user feedback
                     */ },
            onDeleteSession = { /* TODO: Implement delete session with confirmation
                     *   - Implement callback logic for onDeleteSession
                     *   - Handle data/state updates
                     *   - Provide user feedback
                     */ }
        )
    }
}

@Composable
private fun SessionOverviewCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Session Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            SessionInfoRow("Session ID", sessionId)
            SessionInfoRow("Date", "2024-01-15 14:30:00")
            SessionInfoRow("Duration", "45 minutes")
            SessionInfoRow("Device", "Shimmer3 GSR Unit")
            SessionInfoRow("Sample Rate", "128 Hz")
            SessionInfoRow("Status", "Completed") {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("Completed")
                }
            }
        }
    }
}

@Composable
private fun SessionInfoRow(
    label: String,
    value: String,
    valueContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (valueContent != null) {
            valueContent()
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SessionStatisticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Session Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Data Points", "345,600")
                StatisticItem("Avg GSR", "12.5 μS")
                StatisticItem("Peak GSR", "45.7 μS")
            }
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DataQualityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Data Quality",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            QualityIndicator("Signal Quality", 0.95f)
            QualityIndicator("Data Completeness", 0.98f)
            QualityIndicator("Noise Level", 0.15f, isInverse = true)
        }
    }
}

@Composable
private fun QualityIndicator(
    label: String,
    value: Float,
    isInverse: Boolean = false
) {
    val displayValue = if (isInverse) 1f - value else value
    val color = when {
        displayValue >= 0.8f -> MaterialTheme.colorScheme.primary
        displayValue >= 0.6f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(displayValue * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { displayValue },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
    }
}

@Composable
private fun SessionTimelineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Session Timeline",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Text(
                " Session started at 14:30:00\n" +
                        " Device connected at 14:30:15\n" +
                        " Data recording began at 14:30:30\n" +
                        " Peak activity detected at 14:45:12\n" +
                        " Steady state achieved at 14:50:00\n" +
                        " Recording completed at 15:15:00",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5
            )
        }
    }
}

@Composable
private fun SessionActionsCard(
    onViewData: () -> Unit,
    onExportData: () -> Unit,
    onDeleteSession: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Actions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onViewData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Data")
                }

                OutlinedButton(
                    onClick = onExportData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export")
                }
            }

            OutlinedButton(
                onClick = onDeleteSession,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Session")
            }
        }
    }
}

// Simple ViewModel for the session detail
class SessionDetailViewModel : AppBaseViewModel() {
    // Future implementation would include:
    // - Session data loading
    // - Export functionality
    // - Share functionality
    // - Delete confirmation
}