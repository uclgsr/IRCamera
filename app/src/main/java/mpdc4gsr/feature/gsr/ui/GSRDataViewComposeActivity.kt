package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import dagger.hilt.android.AndroidEntryPoint
import mpdc4gsr.core.ui.AppBaseViewModel

@AndroidEntryPoint
class GSRDataViewComposeActivity : BaseComposeActivity<GSRDataViewViewModel>() {
    companion object {
        private const val EXTRA_FILE_PATH = "file_path"
        private const val EXTRA_SESSION_ID = "session_id"

        fun startActivity(
            context: Context,
            filePath: String,
            sessionId: String? = null,
        ) {
            val intent =
                Intent(context, GSRDataViewComposeActivity::class.java).apply {
                    putExtra(EXTRA_FILE_PATH, filePath)
                    sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
                }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): GSRDataViewViewModel = viewModels<GSRDataViewViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GSRDataViewViewModel) {
        val localContext = this@GSRDataViewComposeActivity
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Data Viewer",
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Implement search functionality
                                android.widget.Toast
                                    .makeText(
                                        localContext,
                                        "Search data feature coming soon",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = {
                                // TODO: Implement filter functionality
                                android.widget.Toast
                                    .makeText(
                                        localContext,
                                        "Filter data feature coming soon",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                            }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                            IconButton(onClick = {
                                // TODO: Implement export functionality
                                android.widget.Toast
                                    .makeText(
                                        localContext,
                                        "Export data feature coming soon",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast
                                    .makeText(
                                        localContext,
                                        "More options coming soon",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        },
                    )
                },
            ) { paddingValues ->
                GSRDataViewContent(
                    filePath = filePath,
                    sessionId = sessionId,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GSRDataViewContent(
    filePath: String,
    sessionId: String?,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Data Info Header
        DataInfoCard(filePath = filePath, sessionId = sessionId)
        // Tab Selection
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Raw Data") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Processed") },
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Statistics") },
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Quality") },
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Events") },
            )
        }
        // Tab Content
        when (selectedTab) {
            0 -> RawDataView()
            1 -> ProcessedDataView()
            2 -> StatisticsView()
            3 -> QualityAssessmentView()
            4 -> EventsView()
        }
    }
}

@Composable
private fun DataInfoCard(
    filePath: String,
    sessionId: String?,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Data File Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text("Loaded")
                }
            }
            HorizontalDivider()
            DataInfoRow("File Path", filePath.substringAfterLast("/"))
            DataInfoRow("Session ID", sessionId ?: "N/A")
            DataInfoRow("File Size", "2.3 MB")
            DataInfoRow("Records", "15,647")
            DataInfoRow("Duration", "25:30")
            DataInfoRow("Sample Rate", "128 Hz")
        }
    }
}

@Composable
private fun DataInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectionContainer {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun RawDataView() {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    // Generate sample GSR data
    val sampleData =
        remember {
            generateSampleGSRDataRows(1000)
        }
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Data Controls
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Showing ${sampleData.size} records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = {
                        // TODO: Scroll to top of data
                        android.widget.Toast
                            .makeText(
                                localContext,
                                "Scroll to top",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                    }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Go to top")
                    }
                    IconButton(onClick = {
                        // TODO: Scroll to bottom of data
                        android.widget.Toast
                            .makeText(
                                localContext,
                                "Scroll to bottom",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                    }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Go to bottom")
                    }
                }
            }
        }
        // Data Table Header
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Timestamp",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2f),
                )
                Text(
                    "GSR (μS)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Quality",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Flags",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Data Rows
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            items(sampleData) { dataRow ->
                GSRDataRow(dataRow)
            }
        }
    }
}

@Composable
private fun GSRDataRow(dataRow: GSRDataRowModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (dataRow.quality < 0.7f) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                dataRow.timestamp,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(2f),
            )
            Text(
                String.format("%.3f", dataRow.gsrValue),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${(dataRow.quality * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (dataRow.quality >= 0.8f) {
                        MaterialTheme.colorScheme.primary
                    } else if (dataRow.quality >= 0.6f) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                modifier = Modifier.weight(1f),
            )
            Text(
                if (dataRow.flags.isNotEmpty()) dataRow.flags else "-",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProcessedDataView() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProcessingOptionsCard()
        ProcessedDataPreviewCard()
        ProcessingResultsCard()
    }
}

@Composable
private fun ProcessingOptionsCard() {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Data Processing Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            var smoothingEnabled by remember { mutableStateOf(true) }
            var artifactRemoval by remember { mutableStateOf(false) }
            var normalizeData by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Smoothing Filter")
                Switch(
                    checked = smoothingEnabled,
                    onCheckedChange = { smoothingEnabled = it },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Artifact Removal")
                Switch(
                    checked = artifactRemoval,
                    onCheckedChange = { artifactRemoval = it },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Normalize Data")
                Switch(
                    checked = normalizeData,
                    onCheckedChange = { normalizeData = it },
                )
            }
            Button(
                onClick = {
                    // TODO: Apply data processing
                    android.widget.Toast
                        .makeText(
                            localContext,
                            "Applying processing...",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Apply Processing")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Processing")
            }
        }
    }
}

@Composable
private fun ProcessedDataPreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Processed Data Preview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            Text(
                "Processing Status: Complete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProcessingMetric("Filtered", "95%")
                ProcessingMetric("Artifacts", "23")
                ProcessingMetric("Quality", "A+")
            }
        }
    }
}

@Composable
private fun ProcessingMetric(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProcessingResultsCard() {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Processing Results",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: View processed data
                        android.widget.Toast
                            .makeText(
                                localContext,
                                "Viewing processed data...",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "View Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Data")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Export processed data
                        android.widget.Toast
                            .makeText(
                                localContext,
                                "Exporting processed data...",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
            }
        }
    }
}

@Composable
private fun StatisticsView() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DescriptiveStatisticsCard()
        DistributionAnalysisCard()
        TimeSeriesAnalysisCard()
    }
}

@Composable
private fun DescriptiveStatisticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Descriptive Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatisticItem("Mean", "12.45 μS")
                StatisticItem("Median", "11.87 μS")
                StatisticItem("Mode", "11.2 μS")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatisticItem("Std Dev", "3.21 μS")
                StatisticItem("Variance", "10.3")
                StatisticItem("Range", "12.8 μS")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatisticItem("Skewness", "0.15")
                StatisticItem("Kurtosis", "-0.23")
                StatisticItem("CV", "25.8%")
            }
        }
    }
}

@Composable
private fun DistributionAnalysisCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Distribution Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatisticItem("Q1", "9.2 μS")
                StatisticItem("Q2", "11.9 μS")
                StatisticItem("Q3", "14.8 μS")
                StatisticItem("IQR", "5.6 μS")
            }
            Text(
                "Distribution Type: Normal (p=0.023)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TimeSeriesAnalysisCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Time Series Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatisticItem("Trend", "↗ Increasing")
                StatisticItem("Seasonality", "None")
                StatisticItem("Stationarity", "Non-stationary")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatisticItem("Autocorr", "0.68")
                StatisticItem("Peaks", "47")
                StatisticItem("Outliers", "12")
            }
        }
    }
}

@Composable
private fun QualityAssessmentView() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OverallQualityCard()
        SignalQualityCard()
        DataIntegrityCard()
    }
}

@Composable
private fun OverallQualityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Overall Data Quality",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Quality Score",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "92%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Text("Excellent")
                    }
                }
            }
            LinearProgressIndicator(
                progress = { 0.92f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SignalQualityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Signal Quality Metrics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            QualityMetric("Signal-to-Noise Ratio", 0.89f, "High")
            QualityMetric("Baseline Stability", 0.95f, "Excellent")
            QualityMetric("Motion Artifacts", 0.78f, "Good")
            QualityMetric("Electrode Contact", 0.92f, "Excellent")
        }
    }
}

@Composable
private fun QualityMetric(
    name: String,
    score: Float,
    rating: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                rating,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color =
                    when {
                        score >= 0.9f -> MaterialTheme.colorScheme.primary
                        score >= 0.7f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
            )
        }
        LinearProgressIndicator(
            progress = { score },
            modifier = Modifier.fillMaxWidth(),
            color =
                when {
                    score >= 0.9f -> MaterialTheme.colorScheme.primary
                    score >= 0.7f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                },
        )
    }
}

@Composable
private fun DataIntegrityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Data Integrity",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            IntegrityCheck("Missing Data Points", false, "0.1%")
            IntegrityCheck("Timestamp Gaps", false, "None")
            IntegrityCheck("Value Range Violations", true, "3 instances")
            IntegrityCheck("Duplicate Entries", false, "None")
        }
    }
}

@Composable
private fun IntegrityCheck(
    name: String,
    hasIssues: Boolean,
    details: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (hasIssues) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = if (hasIssues) "Data Quality Warning" else "Data Quality Good",
                tint = if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            details,
            style = MaterialTheme.typography.bodySmall,
            color = if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EventsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Events & Annotations",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "24 events detected during recording session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(generateSampleEvents()) { event ->
                EventItem(event)
            }
        }
    }
}

@Composable
private fun EventItem(event: GSREventModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                when (event.type) {
                    "Peak" -> Icons.AutoMirrored.Filled.TrendingUp
                    "Artifact" -> Icons.Default.Warning
                    "Baseline" -> Icons.Default.HorizontalRule
                    else -> Icons.Default.Event
                },
                contentDescription = event.type,
                tint =
                    when (event.type) {
                        "Peak" -> MaterialTheme.colorScheme.primary
                        "Artifact" -> MaterialTheme.colorScheme.error
                        "Baseline" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    "${event.type} Event",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "At ${event.timestamp} - ${event.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                event.value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// Data Models
data class GSRDataRowModel(
    val timestamp: String,
    val gsrValue: Float,
    val quality: Float,
    val flags: String,
)

data class GSREventModel(
    val timestamp: String,
    val type: String,
    val description: String,
    val value: String,
)

// Helper functions
private fun generateSampleGSRDataRows(count: Int): List<GSRDataRowModel> =
    (0 until count).map { i ->
        GSRDataRowModel(
            timestamp = "00:${(i / 60).toString().padStart(2, '0')}:${
                (i % 60).toString().padStart(2, '0')
            }.${(i % 1000).toString().padStart(3, '0')}",
            gsrValue = 8.0f + kotlin.random.Random.nextFloat() * 12.0f,
            quality = 0.5f + kotlin.random.Random.nextFloat() * 0.5f,
            flags = if (kotlin.random.Random.nextFloat() < 0.1f) "ARTIFACT" else "",
        )
    }

private fun generateSampleEvents(): List<GSREventModel> =
    listOf(
        GSREventModel("00:02:15.123", "Peak", "High conductance detected", "18.4 μS"),
        GSREventModel("00:05:32.456", "Artifact", "Motion artifact detected", "N/A"),
        GSREventModel("00:08:07.789", "Baseline", "Baseline shift detected", "2.1 μS"),
        GSREventModel("00:11:45.234", "Peak", "Significant response peak", "19.8 μS"),
        GSREventModel("00:15:23.567", "Artifact", "Electrode contact issue", "N/A"),
        GSREventModel("00:18:12.890", "Peak", "Emotional response detected", "17.2 μS"),
    )

@Composable
private fun StatisticItem(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

class GSRDataViewViewModel : AppBaseViewModel() {
    // ViewModel implementation for managing data loading, processing, filtering, etc.
    // Future implementation would include:
    // - Data loading from files
    // - Real-time data processing
    // - Filtering and search functionality
    // - Export operations
    // - Quality assessment algorithms
}
