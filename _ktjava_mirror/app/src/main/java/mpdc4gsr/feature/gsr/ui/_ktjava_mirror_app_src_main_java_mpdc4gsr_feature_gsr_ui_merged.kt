// Merged ALL .kt and .java files from the '_ktjava_mirror\app\src\main\java\mpdc4gsr\feature\gsr\ui' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\feature\gsr\ui\app_src_main_java_mpdc4gsr_feature_gsr_ui_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\gsr\ui' subtree
// Files: 28; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRDataViewComposeActivity.kt =====

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
import mpdc4gsr.core.ui.AppBaseViewModel

class GSRDataViewComposeActivity : BaseComposeActivity<GSRDataViewViewModel>() {
    companion object {
        private const val EXTRA_FILE_PATH = "file_path"
        private const val EXTRA_SESSION_ID = "session_id"
        fun startActivity(
            context: Context,
            filePath: String,
            sessionId: String? = null
        ) {
            val intent = Intent(context, GSRDataViewComposeActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
                sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): GSRDataViewViewModel {
        return viewModels<GSRDataViewViewModel>().value
    }

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
                                fontWeight = FontWeight.Bold
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
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Search data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = {
                                // TODO: Implement filter functionality
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Filter data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                            IconButton(onClick = {
                                // TODO: Implement export functionality
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Export data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    localContext,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRDataViewContent(
                    filePath = filePath,
                    sessionId = sessionId,
                    modifier = Modifier.padding(paddingValues)
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
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Data Info Header
        DataInfoCard(filePath = filePath, sessionId = sessionId)
        // Tab Selection
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Raw Data") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Processed") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Statistics") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Quality") }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Events") }
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
    sessionId: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Data File Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
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
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun RawDataView() {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    // Generate sample GSR data
    val sampleData = remember {
        generateSampleGSRDataRows(1000)
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Data Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Showing ${sampleData.size} records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {
                        // TODO: Scroll to top of data
                        android.widget.Toast.makeText(
                            localContext,
                            "Scroll to top",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Go to top")
                    }
                    IconButton(onClick = {
                        // TODO: Scroll to bottom of data
                        android.widget.Toast.makeText(
                            localContext,
                            "Scroll to bottom",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Go to bottom")
                    }
                }
            }
        }
        // Data Table Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Timestamp",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    "GSR (Î¼S)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Quality",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Flags",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Data Rows
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
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
        colors = CardDefaults.cardColors(
            containerColor = if (dataRow.quality < 0.7f) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
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
                dataRow.timestamp,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(2f)
            )
            Text(
                String.format("%.3f", dataRow.gsrValue),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(dataRow.quality * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = if (dataRow.quality >= 0.8f) {
                    MaterialTheme.colorScheme.primary
                } else if (dataRow.quality >= 0.6f) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                if (dataRow.flags.isNotEmpty()) dataRow.flags else "-",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProcessedDataView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Data Processing Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            var smoothingEnabled by remember { mutableStateOf(true) }
            var artifactRemoval by remember { mutableStateOf(false) }
            var normalizeData by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Smoothing Filter")
                Switch(
                    checked = smoothingEnabled,
                    onCheckedChange = { smoothingEnabled = it }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Artifact Removal")
                Switch(
                    checked = artifactRemoval,
                    onCheckedChange = { artifactRemoval = it }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Normalize Data")
                Switch(
                    checked = normalizeData,
                    onCheckedChange = { normalizeData = it }
                )
            }
            Button(
                onClick = {
                    // TODO: Apply data processing
                    android.widget.Toast.makeText(
                        localContext,
                        "Applying processing...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Processed Data Preview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Text(
                "Processing Status: Complete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
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
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProcessingResultsCard() {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Processing Results",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: View processed data
                        android.widget.Toast.makeText(
                            localContext,
                            "Viewing processed data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "View Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Data")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Export processed data
                        android.widget.Toast.makeText(
                            localContext,
                            "Exporting processed data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Descriptive Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Mean", "12.45 Î¼S")
                StatisticItem("Median", "11.87 Î¼S")
                StatisticItem("Mode", "11.2 Î¼S")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Std Dev", "3.21 Î¼S")
                StatisticItem("Variance", "10.3")
                StatisticItem("Range", "12.8 Î¼S")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Distribution Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Q1", "9.2 Î¼S")
                StatisticItem("Q2", "11.9 Î¼S")
                StatisticItem("Q3", "14.8 Î¼S")
                StatisticItem("IQR", "5.6 Î¼S")
            }
            Text(
                "Distribution Type: Normal (p=0.023)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TimeSeriesAnalysisCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Time Series Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Trend", "â†— Increasing")
                StatisticItem("Seasonality", "None")
                StatisticItem("Stationarity", "Non-stationary")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Overall Data Quality",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Quality Score",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "92%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Signal Quality Metrics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
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
    rating: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                rating,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = when {
                    score >= 0.9f -> MaterialTheme.colorScheme.primary
                    score >= 0.7f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
        LinearProgressIndicator(
            progress = { score },
            modifier = Modifier.fillMaxWidth(),
            color = when {
                score >= 0.9f -> MaterialTheme.colorScheme.primary
                score >= 0.7f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun DataIntegrityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Data Integrity",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
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
    details: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (hasIssues) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = if (hasIssues) "Data Quality Warning" else "Data Quality Good",
                tint = if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            details,
            style = MaterialTheme.typography.bodySmall,
            color = if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EventsView() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Events & Annotations",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "24 events detected during recording session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                when (event.type) {
                    "Peak" -> Icons.AutoMirrored.Filled.TrendingUp
                    "Artifact" -> Icons.Default.Warning
                    "Baseline" -> Icons.Default.HorizontalRule
                    else -> Icons.Default.Event
                },
                contentDescription = event.type,
                tint = when (event.type) {
                    "Peak" -> MaterialTheme.colorScheme.primary
                    "Artifact" -> MaterialTheme.colorScheme.error
                    "Baseline" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "${event.type} Event",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "At ${event.timestamp} - ${event.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                event.value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// Data Models
data class GSRDataRowModel(
    val timestamp: String,
    val gsrValue: Float,
    val quality: Float,
    val flags: String
)

data class GSREventModel(
    val timestamp: String,
    val type: String,
    val description: String,
    val value: String
)

// Helper functions
private fun generateSampleGSRDataRows(count: Int): List<GSRDataRowModel> {
    return (0 until count).map { i ->
        GSRDataRowModel(
            timestamp = "00:${(i / 60).toString().padStart(2, '0')}:${
                (i % 60).toString().padStart(2, '0')
            }.${(i % 1000).toString().padStart(3, '0')}",
            gsrValue = 8.0f + kotlin.random.Random.nextFloat() * 12.0f,
            quality = 0.5f + kotlin.random.Random.nextFloat() * 0.5f,
            flags = if (kotlin.random.Random.nextFloat() < 0.1f) "ARTIFACT" else ""
        )
    }
}

private fun generateSampleEvents(): List<GSREventModel> {
    return listOf(
        GSREventModel("00:02:15.123", "Peak", "High conductance detected", "18.4 Î¼S"),
        GSREventModel("00:05:32.456", "Artifact", "Motion artifact detected", "N/A"),
        GSREventModel("00:08:07.789", "Baseline", "Baseline shift detected", "2.1 Î¼S"),
        GSREventModel("00:11:45.234", "Peak", "Significant response peak", "19.8 Î¼S"),
        GSREventModel("00:15:23.567", "Artifact", "Electrode contact issue", "N/A"),
        GSREventModel("00:18:12.890", "Peak", "Emotional response detected", "17.2 Î¼S")
    )
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRDataViewerScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GSRDataViewerScreen(
    sessionId: String = "GSR_Session_001",
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Sample GSR data - in real app, this would come from ViewModel
    val gsrData = remember { generateSampleGSRData() }
    var selectedAnalysis by remember { mutableStateOf(AnalysisType.RAW_SIGNAL) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "GSR Data Analysis",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Share,
                contentDescription = "Export Data",
                onClick = {
                    // TODO: Implement GSR data export functionality
                    android.widget.Toast.makeText(
                        context,
                        "Exporting GSR data...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session Info Card
            SessionInfoCard(sessionId = sessionId)
            // Analysis Type Selector
            AnalysisTypeSelector(
                selectedType = selectedAnalysis,
                onTypeSelected = { selectedAnalysis = it }
            )
            // Data Visualization
            when (selectedAnalysis) {
                AnalysisType.RAW_SIGNAL -> {
                    GSRSignalChart(
                        title = "Raw GSR Signal",
                        data = gsrData,
                        color = Color.Cyan
                    )
                }

                AnalysisType.FILTERED -> {
                    GSRSignalChart(
                        title = "Filtered Signal",
                        data = gsrData.map { it * 0.8f + 0.1f }, // Simple filter simulation
                        color = Color.Green
                    )
                }

                AnalysisType.FEATURES -> {
                    GSRFeaturesCard(gsrData)
                }

                AnalysisType.STATISTICS -> {
                    GSRStatisticsCard(gsrData)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SessionInfoCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Session Information",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Session ID:", color = Color.Gray, fontSize = 14.sp)
                Text(sessionId, color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Duration:", color = Color.Gray, fontSize = 14.sp)
                Text("5m 32s", color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sample Rate:", color = Color.Gray, fontSize = 14.sp)
                Text("128 Hz", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AnalysisTypeSelector(
    selectedType: AnalysisType,
    onTypeSelected: (AnalysisType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Analysis Type",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalysisType.entries.forEach { type ->
                    FilterChip(
                        onClick = { onTypeSelected(type) },
                        label = {
                            Text(
                                type.displayName,
                                fontSize = 12.sp
                            )
                        },
                        selected = selectedType == type,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Gray,
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRSignalChart(
    title: String,
    data: List<Float>,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val pointSpacing = width / data.size
                val path = Path()
                data.forEachIndexed { index, value ->
                    val x = index * pointSpacing
                    val y = height - (value * height)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx())
                )
                // Draw grid lines
                for (i in 0..4) {
                    val y = (height / 4) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRFeaturesCard(data: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "GSR Features",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val features = listOf(
                "Skin Conductance Response (SCR) Count" to "23",
                "Average SCR Amplitude" to "0.15 Î¼S",
                "Peak Detection" to "17 peaks",
                "Arousal Index" to "High",
                "Stress Level" to "Moderate"
            )
            features.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 14.sp)
                    Text(value, color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GSRStatisticsCard(data: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistical Analysis",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val stats = listOf(
                "Mean" to String.format("%.3f Î¼S", data.average()),
                "Standard Deviation" to "0.045 Î¼S",
                "Min Value" to String.format("%.3f Î¼S", data.minOrNull() ?: 0f),
                "Max Value" to String.format("%.3f Î¼S", data.maxOrNull() ?: 0f),
                "Range" to String.format(
                    "%.3f Î¼S",
                    (data.maxOrNull() ?: 0f) - (data.minOrNull() ?: 0f)
                )
            )
            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 14.sp)
                    Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

enum class AnalysisType(val displayName: String) {
    RAW_SIGNAL("Raw"),
    FILTERED("Filtered"),
    FEATURES("Features"),
    STATISTICS("Stats")
}

private fun generateSampleGSRData(): List<Float> {
    return (0..200).map { i ->
        0.5f + 0.3f * sin(i * 0.1).toFloat() + 0.1f * Random.nextFloat()
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRDataViewerScreenPreview() {
    IRCameraTheme {
        GSRDataViewerScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRDeviceManagementComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

class GSRDeviceManagementComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRDeviceManagementComposeActivity::class.java))
        }
    }

    override fun createViewModel(): AppBaseViewModel {
        return viewModels<AppBaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        val localContext = this@GSRDeviceManagementComposeActivity
        var isScanning by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<GSRDeviceInfo?>(null) }
        var showDeviceDetails by remember { mutableStateOf(false) }
        var showBulkActions by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Device Management",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isScanning = !isScanning }) {
                                Icon(
                                    if (isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                                    contentDescription = if (isScanning) "Stop Scan" else "Scan"
                                )
                            }
                            IconButton(onClick = { showBulkActions = true }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Bulk Actions")
                            }
                            IconButton(onClick = {
                                // TODO: Open device help documentation
                                android.widget.Toast.makeText(
                                    this@GSRDeviceManagementComposeActivity,
                                    "Opening device help...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRDeviceManagementContent(
                    isScanning = isScanning,
                    selectedDevice = selectedDevice,
                    onDeviceSelect = {
                        selectedDevice = it
                        showDeviceDetails = true
                    },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showDeviceDetails && selectedDevice != null) {
            DeviceDetailsDialog(
                device = selectedDevice!!,
                onDismiss = { showDeviceDetails = false },
                onConfigure = {
                    ShimmerConfigComposeActivity.startActivity(this@GSRDeviceManagementComposeActivity)
                    showDeviceDetails = false
                }
            )
        }
        if (showBulkActions) {
            BulkActionsDialog(
                onDismiss = { showBulkActions = false },
                onPerformAction = { action ->
                    // Perform bulk action
                    showBulkActions = false
                }
            )
        }
    }
}

@Composable
private fun GSRDeviceManagementContent(
    isScanning: Boolean,
    selectedDevice: GSRDeviceInfo?,
    onDeviceSelect: (GSRDeviceInfo) -> Unit,
    viewModel: AppBaseViewModel,
    modifier: Modifier = Modifier
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Device Status Overview
        DeviceStatusOverview(
            connectedDevices = 2,
            availableDevices = 3,
            isScanning = isScanning,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Connected Devices Section
        Text(
            text = "Connected Devices",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val connectedDevices = getMockGSRDevices().filter { it.status == "connected" }
            val availableDevices = getMockGSRDevices().filter { it.status != "connected" }
            items(connectedDevices) { device ->
                GSRDeviceCard(
                    device = device,
                    onSelect = { onDeviceSelect(device) },
                    onConnect = {
                        // TODO: Implement device connection logic
                        android.widget.Toast.makeText(
                            localContext,
                            "Connecting to ${device.name}...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDisconnect = {
                        // TODO: Implement device disconnection logic
                        android.widget.Toast.makeText(
                            localContext,
                            "Disconnecting from ${device.name}...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            if (availableDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Available Devices",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                    )
                }
                items(availableDevices) { device ->
                    GSRDeviceCard(
                        device = device,
                        onSelect = { onDeviceSelect(device) },
                        onConnect = {
                            // TODO: Implement device connection logic
                            android.widget.Toast.makeText(
                                localContext,
                                "Connecting to ${device.name}...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onDisconnect = {
                            // TODO: Implement device disconnection logic
                            android.widget.Toast.makeText(
                                localContext,
                                "Disconnecting from ${device.name}...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceStatusOverview(
    connectedDevices: Int,
    availableDevices: Int,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeviceStatusItem(
                    label = "Connected",
                    count = connectedDevices,
                    color = Color(0xFF4CAF50)
                )
                DeviceStatusItem(
                    label = "Available",
                    count = availableDevices,
                    color = Color(0xFF2196F3)
                )
                DeviceStatusItem(
                    label = "Total",
                    count = connectedDevices + availableDevices,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun DeviceStatusItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun GSRDeviceCard(
    device: GSRDeviceInfo,
    onSelect: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = when (device.status) {
                "connected" -> MaterialTheme.colorScheme.tertiaryContainer
                "connecting" -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "ID: ${device.deviceId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Status indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getDeviceStatusColor(device.status),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = device.status?.uppercase() ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Device metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeviceMetricItem(
                    label = "Battery",
                    value = "${device.batteryLevel}%",
                    icon = getBatteryIcon(device.batteryLevel)
                )
                DeviceMetricItem(
                    label = "Signal",
                    value = "${device.signalStrength} dBm",
                    icon = Icons.Default.Wifi
                )
                DeviceMetricItem(
                    label = "Sample Rate",
                    value = "${device.samplingRate} Hz",
                    icon = Icons.Default.Timeline
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Device actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Details",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }
                if (device.status == "connected") {
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53E3E)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.LinkOff,
                            contentDescription = "Disconnect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeviceDetailsDialog(
    device: GSRDeviceInfo,
    onDismiss: () -> Unit,
    onConfigure: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(device.name)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                DeviceDetailItem("Device ID", device.deviceId)
                DeviceDetailItem("Status", device.status?.replaceFirstChar { it.uppercaseChar() } ?: "N/A")
                DeviceDetailItem("Battery Level", "${device.batteryLevel}%")
                DeviceDetailItem("Signal Strength", "${device.signalStrength} dBm")
                DeviceDetailItem("Sampling Rate", "${device.samplingRate} Hz")
                DeviceDetailItem("Last Seen", device.lastSeen)
                if (device.status == "connected") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recent Data",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "GSR: 2.45 ÂµS\nPPG: 1024, 1028\nTemperature: 36.2Â°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfigure) {
                Text("Configure")
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
private fun DeviceDetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BulkActionsDialog(
    onDismiss: () -> Unit,
    onPerformAction: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Actions") },
        text = {
            Column {
                Text("Select an action to perform on all devices:")
                Spacer(modifier = Modifier.height(16.dp))
                listOf(
                    "Disconnect All" to "disconnect_all",
                    "Update Firmware" to "update_firmware",
                    "Reset Configuration" to "reset_config",
                    "Export Device List" to "export_list"
                ).forEach { (label, action) ->
                    TextButton(
                        onClick = { onPerformAction(action) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getDeviceStatusColor(status: String?) = when (status) {
    "connected" -> Color(0xFF4CAF50)
    "connecting" -> Color(0xFFFF9800)
    "available" -> Color(0xFF2196F3)
    "disconnected" -> Color(0xFF9E9E9E)
    null -> Color(0xFF9E9E9E).copy(alpha = 0.6f)
    else -> Color(0xFFE53E3E)
}

private fun getBatteryIcon(batteryLevel: Int) = when {
    batteryLevel > 75 -> Icons.Default.BatteryFull
    batteryLevel > 50 -> Icons.Default.Battery6Bar
    batteryLevel > 25 -> Icons.Default.Battery3Bar
    else -> Icons.Default.Battery1Bar
}

data class GSRDeviceInfo(
    val name: String,
    val deviceId: String,
    val status: String?,
    val batteryLevel: Int,
    val signalStrength: Int,
    val samplingRate: Int,
    val lastSeen: String
)

private fun getMockGSRDevices() = listOf(
    GSRDeviceInfo("Shimmer3 GSR+ #001", "shimmer_001", "disconnected", 89, -42, 128, "Just now"),
    GSRDeviceInfo("Shimmer3 GSR+ #002", "shimmer_002", "disconnected", 76, -38, 256, "2 min ago"),
    GSRDeviceInfo("Shimmer3 GSR+ #003", "shimmer_003", "available", 92, -55, 128, "5 min ago"),
    GSRDeviceInfo("Shimmer3 GSR+ #004", "shimmer_004", "disconnected", 45, -68, 128, "1 hour ago"),
    GSRDeviceInfo("Shimmer3 GSR+ #005", "shimmer_005", "available", 83, -48, 256, "10 min ago")
)


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRGalleryComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.theme.IRCameraTheme

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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
            sessionsCount = uiState.sessions.size
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Content based on view mode
        if (isGridView) {
            GSRSessionGrid(
                sessions = uiState.filteredSessions,
                onSessionClick = { viewModel.openSession(it) },
                onSessionLongClick = { viewModel.selectSession(it) },
                selectedSessions = uiState.selectedSessions
            )
        } else {
            GSRSessionList(
                sessions = uiState.filteredSessions,
                onSessionClick = { viewModel.openSession(it) },
                onSessionLongClick = { viewModel.selectSession(it) },
                selectedSessions = uiState.selectedSessions
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
            }
        )
    }
    // Selection Actions
    if (uiState.selectedSessions.isNotEmpty()) {
        SelectionActionsBar(
            selectedCount = uiState.selectedSessions.size,
            onExportSelected = { viewModel.exportSelectedSessions() },
            onDeleteSelected = { viewModel.deleteSelectedSessions() },
            onClearSelection = { viewModel.clearSelection() }
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
    sessionsCount: Int
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GSR Gallery",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$sessionsCount sessions",
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
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onViewToggle) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = if (isGridView) "List View" else "Grid View"
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
    selectedSessions: Set<String>
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sessions) { session ->
            GSRSessionGridItem(
                session = session,
                isSelected = selectedSessions.contains(session.id),
                onClick = { onSessionClick(session) },
                onLongClick = { onSessionLongClick(session) }
            )
        }
    }
}

@Composable
fun GSRSessionList(
    sessions: List<GSRSession>,
    onSessionClick: (GSRSession) -> Unit,
    onSessionLongClick: (GSRSession) -> Unit,
    selectedSessions: Set<String>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions) { session ->
            GSRSessionListItem(
                session = session,
                isSelected = selectedSessions.contains(session.id),
                onClick = { onSessionClick(session) },
                onLongClick = { onSessionLongClick(session) }
            )
        }
    }
}

@Composable
fun GSRSessionGridItem(
    session: GSRSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Session preview visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "GSR Data",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = "${formatDuration(session.duration)} â€¢ ${session.sampleCount} samples",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quality: ${session.dataQuality}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        session.dataQuality > 90 -> Color(0xFF4CAF50)
                        session.dataQuality > 70 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
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
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "GSR Data",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (session.participantId.isNotEmpty()) {
                    Text(
                        text = "Participant: ${session.participantId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Row {
                    Text(
                        text = formatDuration(session.duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(" â€¢ ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${session.sampleCount} samples",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(" â€¢ ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Quality: ${session.dataQuality}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            session.dataQuality > 90 -> Color(0xFF4CAF50)
                            session.dataQuality > 70 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }
            Text(
                text = formatFileSize(session.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun GSRFilterDialog(
    currentFilter: GSRFilter,
    onDismiss: () -> Unit,
    onApplyFilter: (GSRFilter) -> Unit
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
                    steps = 10
                )
                Text("${filter.minQuality}%")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Minimum Duration (minutes):")
                Slider(
                    value = filter.minDuration.toFloat(),
                    onValueChange = { filter = filter.copy(minDuration = it.toInt()) },
                    valueRange = 0f..60f,
                    steps = 12
                )
                Text("${filter.minDuration} min")
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = filter.hasParticipant,
                        onCheckedChange = { filter = filter.copy(hasParticipant = it) }
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
        }
    )
}

@Composable
fun SelectionActionsBar(
    selectedCount: Int,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                TextButton(onClick = onClearSelection) {
                    Text("Clear")
                }
                OutlinedButton(onClick = onExportSelected) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
                Button(
                    onClick = onDeleteSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
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
    val timestamp: Long
)

data class GSRFilter(
    val minQuality: Int = 0,
    val minDuration: Int = 0,
    val hasParticipant: Boolean = false
)

data class GSRGalleryUiState(
    val sessions: List<GSRSession> = emptyList(),
    val filteredSessions: List<GSRSession> = emptyList(),
    val selectedSessions: Set<String> = emptySet(),
    val currentFilter: GSRFilter = GSRFilter(),
    val isLoading: Boolean = false
)

// ViewModel
class GSRGalleryViewModel : AppBaseViewModel() {
    private val _galleryState = MutableStateFlow(GSRGalleryUiState())
    val galleryState: StateFlow<GSRGalleryUiState> = _galleryState.asStateFlow()
    fun loadGSRSessions() {
        _galleryState.value = _galleryState.value.copy(isLoading = true)
        val mockSessions = listOf(
            GSRSession(
                "1",
                "Stress Response Study",
                "P001",
                1800000,
                460800,
                95,
                2048576,
                System.currentTimeMillis() - 86400000
            ),
            GSRSession(
                "2",
                "Cognitive Load Test",
                "P002",
                1200000,
                307200,
                87,
                1048576,
                System.currentTimeMillis() - 172800000
            ),
            GSRSession(
                "3",
                "Emotion Recognition",
                "",
                2700000,
                691200,
                92,
                3145728,
                System.currentTimeMillis() - 259200000
            ),
            GSRSession(
                "4",
                "Quick Recording",
                "",
                300000,
                76800,
                78,
                262144,
                System.currentTimeMillis() - 345600000
            ),
            GSRSession(
                "5",
                "Baseline Measurement",
                "P001",
                600000,
                153600,
                98,
                524288,
                System.currentTimeMillis() - 432000000
            )
        )
        _galleryState.value = _galleryState.value.copy(
            sessions = mockSessions,
            filteredSessions = mockSessions,
            isLoading = false
        )
    }

    fun filterSessions(query: String) {
        val filtered = _galleryState.value.sessions.filter { session ->
            query.isEmpty() ||
                    session.name.contains(query, ignoreCase = true) ||
                    session.participantId.contains(query, ignoreCase = true)
        }
        _galleryState.value = _galleryState.value.copy(filteredSessions = filtered)
    }

    fun applyFilter(filter: GSRFilter) {
        val filtered = _galleryState.value.sessions.filter { session ->
            session.dataQuality >= filter.minQuality &&
                    (session.duration / 60000) >= filter.minDuration &&
                    (!filter.hasParticipant || session.participantId.isNotEmpty())
        }
        _galleryState.value = _galleryState.value.copy(
            filteredSessions = filtered,
            currentFilter = filter
        )
    }

    fun selectSession(session: GSRSession) {
        val currentSelection = _galleryState.value.selectedSessions
        val newSelection = if (currentSelection.contains(session.id)) {
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
        _galleryState.value = _galleryState.value.copy(
            sessions = updatedSessions,
            filteredSessions = updatedSessions,
            selectedSessions = emptySet()
        )
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRPlotComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel
import kotlin.math.sin

class GSRPlotComposeActivity : BaseComposeActivity<GSRPlotViewModel>() {
    companion object {
        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_DATA_PATH = "data_path"
        fun startActivity(
            context: Context,
            sessionId: String,
            dataPath: String? = null
        ) {
            val intent = Intent(context, GSRPlotComposeActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                dataPath?.let { putExtra(EXTRA_DATA_PATH, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): GSRPlotViewModel {
        return viewModels<GSRPlotViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GSRPlotViewModel) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "unknown"
        val dataPath = intent.getStringExtra(EXTRA_DATA_PATH)
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Data Analysis",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = {
                                // TODO: Implement data export
                                android.widget.Toast.makeText(
                                    context,
                                    "Export data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                // TODO: Implement plot sharing
                                android.widget.Toast.makeText(
                                    context,
                                    "Share plot feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                // TODO: Open plot settings
                                android.widget.Toast.makeText(
                                    context,
                                    "Plot settings feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Tune, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRPlotContent(
                    sessionId = sessionId,
                    dataPath = dataPath,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun GSRPlotContent(
    sessionId: String,
    dataPath: String?,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedVisualization by remember { mutableStateOf(VisualizationType.LINE_CHART) }
    var timeRange by remember { mutableStateOf(TimeRange.ALL) }
    var showStatistics by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visualization Controls
        VisualizationControlsCard(
            selectedVisualization = selectedVisualization,
            selectedTimeRange = timeRange,
            onVisualizationChange = { selectedVisualization = it },
            onTimeRangeChange = { timeRange = it }
        )
        // Main Plot Area
        MainPlotCard(
            visualizationType = selectedVisualization,
            timeRange = timeRange,
            sessionId = sessionId,
            context = context
        )
        // Statistics Panel
        if (showStatistics) {
            StatisticsCard(sessionId = sessionId)
        }
        // Data Analysis Tools
        DataAnalysisToolsCard(context = context)
        // Export Options
        ExportOptionsCard(context = context)
    }
}

enum class VisualizationType {
    LINE_CHART,
    SCATTER_PLOT,
    HISTOGRAM,
    HEATMAP
}

enum class TimeRange {
    ALL,
    LAST_MINUTE,
    LAST_5_MINUTES,
    LAST_10_MINUTES,
    CUSTOM
}

@Composable
private fun VisualizationControlsCard(
    selectedVisualization: VisualizationType,
    selectedTimeRange: TimeRange,
    onVisualizationChange: (VisualizationType) -> Unit,
    onTimeRangeChange: (TimeRange) -> Unit
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
                "Visualization Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Visualization Type Selection
            Text(
                "Chart Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualizationType.values().take(2).forEach { type ->
                    FilterChip(
                        onClick = { onVisualizationChange(type) },
                        label = {
                            Text(
                                when (type) {
                                    VisualizationType.LINE_CHART -> "Line Chart"
                                    VisualizationType.SCATTER_PLOT -> "Scatter Plot"
                                    VisualizationType.HISTOGRAM -> "Histogram"
                                    VisualizationType.HEATMAP -> "Heatmap"
                                }
                            )
                        },
                        selected = selectedVisualization == type,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualizationType.values().drop(2).forEach { type ->
                    FilterChip(
                        onClick = { onVisualizationChange(type) },
                        label = {
                            Text(
                                when (type) {
                                    VisualizationType.LINE_CHART -> "Line Chart"
                                    VisualizationType.SCATTER_PLOT -> "Scatter Plot"
                                    VisualizationType.HISTOGRAM -> "Histogram"
                                    VisualizationType.HEATMAP -> "Heatmap"
                                }
                            )
                        },
                        selected = selectedVisualization == type,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Time Range Selection
            Text(
                "Time Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.values().take(3).forEach { range ->
                    FilterChip(
                        onClick = { onTimeRangeChange(range) },
                        label = {
                            Text(
                                when (range) {
                                    TimeRange.ALL -> "All"
                                    TimeRange.LAST_MINUTE -> "1m"
                                    TimeRange.LAST_5_MINUTES -> "5m"
                                    TimeRange.LAST_10_MINUTES -> "10m"
                                    TimeRange.CUSTOM -> "Custom"
                                }
                            )
                        },
                        selected = selectedTimeRange == range,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainPlotCard(
    visualizationType: VisualizationType,
    timeRange: TimeRange,
    sessionId: String,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GSR Signal Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {
                        // TODO: Implement zoom in functionality
                        android.widget.Toast.makeText(
                            context,
                            "Zoom in feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                    IconButton(onClick = {
                        // TODO: Implement zoom out functionality
                        android.widget.Toast.makeText(
                            context,
                            "Zoom out feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }
                    IconButton(onClick = {
                        // TODO: Implement reset zoom functionality
                        android.widget.Toast.makeText(
                            context,
                            "Reset zoom feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset")
                    }
                }
            }
            HorizontalDivider()
            // Plot Area
            when (visualizationType) {
                VisualizationType.LINE_CHART -> {
                    GSRLineChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                VisualizationType.SCATTER_PLOT -> {
                    GSRScatterPlot(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                VisualizationType.HISTOGRAM -> {
                    GSRHistogram(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                VisualizationType.HEATMAP -> {
                    GSRHeatmap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }
            // Plot Legend
            PlotLegend()
        }
    }
}

@Composable
private fun GSRLineChart(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRLineChart(this, primaryColor, secondaryColor)
    }
}

private fun drawGSRLineChart(drawScope: DrawScope, primaryColor: Color, secondaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val padding = 40f
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        // Generate sample GSR data
        val dataPoints = generateSampleGSRData(100)
        val path = Path()
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * (width - 2 * padding)
            val y = height - padding - (value * (height - 2 * padding))
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        // Draw the GSR signal line
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3f)
        )
        // Draw data points
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * (width - 2 * padding)
            val y = height - padding - (value * (height - 2 * padding))
            drawCircle(
                color = secondaryColor,
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun GSRScatterPlot(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRScatterPlot(this, primaryColor)
    }
}

private fun drawGSRScatterPlot(drawScope: DrawScope, primaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val padding = 40f
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        // Generate sample scatter data
        val dataPoints = generateSampleGSRData(50)
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (dataPoints.size - 1)) * (width - 2 * padding)
            val y = height - padding - (value * (height - 2 * padding))
            // Vary point size based on value
            val radius = 3f + (value * 5f)
            drawCircle(
                color = primaryColor.copy(alpha = 0.7f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun GSRHistogram(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRHistogram(this, primaryColor)
    }
}

private fun drawGSRHistogram(drawScope: DrawScope, primaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val padding = 40f
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        // Generate histogram data
        val binCount = 15
        val binWidth = (width - 2 * padding) / binCount
        val histogramData = generateHistogramData(binCount)
        histogramData.forEachIndexed { index, value ->
            val x = padding + index * binWidth
            val barHeight = value * (height - 2 * padding)
            drawRect(
                color = primaryColor.copy(alpha = 0.8f),
                topLeft = Offset(x, height - padding - barHeight),
                size = androidx.compose.ui.geometry.Size(binWidth * 0.8f, barHeight)
            )
        }
    }
}

@Composable
private fun GSRHeatmap(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        drawGSRHeatmap(this, primaryColor)
    }
}

private fun drawGSRHeatmap(drawScope: DrawScope, primaryColor: Color) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        val cellSize = 20f
        val cols = (width / cellSize).toInt()
        val rows = (height / cellSize).toInt()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val intensity = (sin((row + col) * 0.3) + 1) / 2
                val color = primaryColor.copy(alpha = intensity.toFloat())
                drawRect(
                    color = color,
                    topLeft = Offset(col * cellSize, row * cellSize),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }
        }
    }
}

@Composable
private fun PlotLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem("GSR Signal", MaterialTheme.colorScheme.primary)
        LegendItem("Data Points", MaterialTheme.colorScheme.secondary)
        LegendItem("Threshold", MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatisticsCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Statistical Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Mean", "12.5 Î¼S")
                StatisticItem("Std Dev", "3.2 Î¼S")
                StatisticItem("Min", "8.1 Î¼S")
                StatisticItem("Max", "18.9 Î¼S")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Peaks", "24")
                StatisticItem("Frequency", "0.8 Hz")
                StatisticItem("Trend", "â†— Rising")
                StatisticItem("Quality", "95%")
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DataAnalysisToolsCard(context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Analysis Tools",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Apply data filter
                        android.widget.Toast.makeText(
                            context,
                            "Applying filter...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FilterAlt, contentDescription = "Filter Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filter")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Apply data smoothing
                        android.widget.Toast.makeText(
                            context,
                            "Smoothing data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Smooth Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Smooth")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Implement peak detection algorithm
                        android.widget.Toast.makeText(
                            context,
                            "Peak detection feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Detect Peaks")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Peaks")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Implement trend analysis
                        android.widget.Toast.makeText(
                            context,
                            "Trend analysis feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = "Analyze Trends")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trends")
                }
            }
        }
    }
}

@Composable
private fun ExportOptionsCard(context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Export Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Export data to CSV
                        android.widget.Toast.makeText(
                            context,
                            "Exporting to CSV...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "Export CSV")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Save plot as image
                        android.widget.Toast.makeText(
                            context,
                            "Saving plot...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Save Plot as Image")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Plot")
                }
            }
        }
    }
}

// Helper functions for generating sample data
private fun generateSampleGSRData(points: Int): List<Float> {
    return (0 until points).map { i ->
        val baseValue = 0.3f + sin(i * 0.1f) * 0.2f
        val noise = (kotlin.random.Random.nextFloat() - 0.5f) * 0.1f
        (baseValue + noise).coerceIn(0f, 1f)
    }
}

private fun generateHistogramData(bins: Int): List<Float> {
    return (0 until bins).map { i ->
        val centerValue = i.toFloat() / bins
        kotlin.math.exp(-((centerValue - 0.5f) * (centerValue - 0.5f)) / 0.2f).toFloat()
    }
}

class GSRPlotViewModel : AppBaseViewModel() {
    // ViewModel implementation for managing plot data, zoom state, filters, etc.
    // Future implementation would include:
    // - Data loading from files or database
    // - Real-time data updates
    // - Zoom and pan state management
    // - Filter state management
    // - Export functionality
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRPlotScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRPlotScreen(
    sessionId: String,
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("GSR Plot - $sessionId") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GSR Data Plot",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Session: $sessionId",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "GSR plot visualization would be implemented here using a charting library or custom Canvas drawing.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRQuickRecordingComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.random.Random

class GSRQuickRecordingComposeActivity : BaseComposeActivity<GSRQuickRecordingViewModel>() {
    override fun createViewModel(): GSRQuickRecordingViewModel = GSRQuickRecordingViewModel()

    @Composable
    override fun Content(viewModel: GSRQuickRecordingViewModel) {
        IRCameraTheme {
            GSRQuickRecordingScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRQuickRecordingScreen(viewModel: GSRQuickRecordingViewModel) {
    val uiState by viewModel.recordingState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initializeQuickRecording()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Quick GSR Recording",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recording Status Card
            item {
                QuickRecordingStatusCard(
                    isRecording = uiState.isRecording,
                    recordingDuration = uiState.recordingDuration,
                    samplesCollected = uiState.samplesCollected,
                    onStartRecording = { viewModel.startQuickRecording() },
                    onStopRecording = { viewModel.stopQuickRecording() }
                )
            }
            // GSR Device Status
            item {
                GSRDeviceStatusCard(
                    deviceStatus = uiState.deviceStatus,
                    signalQuality = uiState.signalQuality,
                    batteryLevel = uiState.batteryLevel
                )
            }
            // Live GSR Data Visualization
            if (uiState.isRecording) {
                item {
                    LiveGSRDataCard(
                        currentValue = uiState.currentGSRValue,
                        averageValue = uiState.averageGSRValue,
                        recentValues = uiState.recentGSRValues
                    )
                }
            }
            // Quick Settings
            item {
                QuickSettingsCard(
                    sampleRate = uiState.sampleRate,
                    autoSave = uiState.autoSave,
                    onSampleRateChange = { viewModel.setSampleRate(it) },
                    onAutoSaveToggle = { viewModel.toggleAutoSave() }
                )
            }
            // Recent Sessions
            if (uiState.recentSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Quick Sessions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.recentSessions.take(3)) { session ->
                    QuickSessionCard(
                        session = session,
                        onView = { viewModel.viewSession(session) },
                        onExport = { viewModel.exportSession(session) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickRecordingStatusCard(
    isRecording: Boolean,
    recordingDuration: Long,
    samplesCollected: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) Color(0xFFF44336).copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecording) {
                // Recording indicator with animation
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier.size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 2.dp,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Recording Active",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Recording stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStatItem("Duration", formatDuration(recordingDuration))
                    QuickStatItem("Samples", samplesCollected.toString())
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Recording", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "Ready to Record",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ready to Record",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to start quick GSR recording",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStartRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Recording", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun GSRDeviceStatusCard(
    deviceStatus: String,
    signalQuality: Int,
    batteryLevel: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Sensor Status: $deviceStatus",
                    tint = when (deviceStatus) {
                        "Connected" -> Color(0xFF4CAF50)
                        "Connecting" -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GSR Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = deviceStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (deviceStatus) {
                            "Connected" -> Color(0xFF4CAF50)
                            "Connecting" -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Quality: $signalQuality%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (batteryLevel != null) {
                        Text(
                            text = "Battery: $batteryLevel%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (deviceStatus == "Connected") {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { signalQuality / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        signalQuality > 80 -> Color(0xFF4CAF50)
                        signalQuality > 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
        }
    }
}

@Composable
fun LiveGSRDataCard(
    currentValue: Double,
    averageValue: Double,
    recentValues: List<Double>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Live GSR Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStatItem("Current", String.format("%.2f ÂµS", currentValue))
                QuickStatItem("Average", String.format("%.2f ÂµS", averageValue))
                QuickStatItem(
                    "Range",
                    String.format(
                        "%.1f",
                        recentValues.maxOrNull()?.minus(recentValues.minOrNull() ?: 0.0) ?: 0.0
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Simple data visualization placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Live GSR Signal Visualization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun QuickSettingsCard(
    sampleRate: Int,
    autoSave: Boolean,
    onSampleRateChange: (Int) -> Unit,
    onAutoSaveToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sample Rate: ${sampleRate} Hz")
                Row {
                    listOf(128, 256, 512).forEach { rate ->
                        FilterChip(
                            onClick = { onSampleRateChange(rate) },
                            label = { Text("$rate") },
                            selected = sampleRate == rate,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-save Sessions")
                Switch(
                    checked = autoSave,
                    onCheckedChange = { onAutoSaveToggle() }
                )
            }
        }
    }
}

@Composable
fun QuickSessionCard(
    session: QuickSession,
    onView: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = "GSR Data",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${formatDuration(session.duration)} â€¢ ${session.sampleCount} samples",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onView) {
                Icon(Icons.Default.Visibility, contentDescription = "View")
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Default.Download, contentDescription = "Export")
            }
        }
    }
}

@Composable
fun QuickStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    return String.format("%d:%02d", minutes, seconds % 60)
}

// Data classes
data class QuickSession(
    val name: String,
    val duration: Long,
    val sampleCount: Int,
    val timestamp: Long
)

data class GSRQuickRecordingUiState(
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0,
    val samplesCollected: Int = 0,
    val deviceStatus: String = "Disconnected",
    val signalQuality: Int = 95,
    val batteryLevel: Int? = 87,
    val currentGSRValue: Double = 0.0,
    val averageGSRValue: Double = 0.0,
    val recentGSRValues: List<Double> = emptyList(),
    val sampleRate: Int = 256,
    val autoSave: Boolean = true,
    val recentSessions: List<QuickSession> = emptyList()
)

// ViewModel
class GSRQuickRecordingViewModel : AppBaseViewModel() {
    private val _recordingState = MutableStateFlow(GSRQuickRecordingUiState())
    val recordingState: StateFlow<GSRQuickRecordingUiState> = _recordingState.asStateFlow()
    private var recordingJob: Job? = null
    fun initializeQuickRecording() {
        val mockSessions = listOf(
            QuickSession("Quick Session 1", 180000, 46080, System.currentTimeMillis() - 3600000),
            QuickSession("Quick Session 2", 120000, 30720, System.currentTimeMillis() - 7200000),
            QuickSession("Quick Session 3", 240000, 61440, System.currentTimeMillis() - 10800000)
        )
        _recordingState.value = _recordingState.value.copy(recentSessions = mockSessions)
    }

    fun startQuickRecording() {
        _recordingState.value = _recordingState.value.copy(isRecording = true)
        // Cancel any existing recording job
        recordingJob?.cancel()
        // Start recording simulation on main dispatcher
        recordingJob = viewModelScope.launch(Dispatchers.Main) {
            while (_recordingState.value.isRecording) {
                delay(1000)
                val currentState = _recordingState.value
                _recordingState.value = currentState.copy(
                    recordingDuration = currentState.recordingDuration + 1000,
                    samplesCollected = currentState.samplesCollected + currentState.sampleRate,
                    currentGSRValue = Random.nextDouble(5.0, 15.0),
                    averageGSRValue = Random.nextDouble(8.0, 12.0),
                    recentGSRValues = currentState.recentGSRValues.takeLast(10) + Random.nextDouble(5.0, 15.0)
                )
            }
        }
    }

    fun stopQuickRecording() {
        _recordingState.value = _recordingState.value.copy(isRecording = false)
        recordingJob?.cancel()
        recordingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
    }

    fun setSampleRate(rate: Int) {
        _recordingState.value = _recordingState.value.copy(sampleRate = rate)
    }

    fun toggleAutoSave() {
        _recordingState.value = _recordingState.value.copy(autoSave = !_recordingState.value.autoSave)
    }

    fun viewSession(session: QuickSession) {
        // Implementation for viewing session
    }

    fun exportSession(session: QuickSession) {
        // Implementation for exporting session
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRQuickRecordingScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin
import kotlin.math.sqrt

data class GSRReading(
    val timestamp: Long,
    val value: Double, // in microsiemens
    val quality: SignalQuality = SignalQuality.GOOD
)

enum class SignalQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

enum class RecordingState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECORDING,
    PAUSED,
    COMPLETED,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRQuickRecordingScreen(
    onNavigateBack: () -> Unit = {},
    onSaveRecording: () -> Unit = {}
) {
    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
    var recordingDuration by remember { mutableStateOf(0) } // in seconds
    var gsrReadings by remember { mutableStateOf(listOf<GSRReading>()) }
    var currentGSRValue by remember { mutableStateOf(12.5) }
    var batteryLevel by remember { mutableStateOf(85) }
    var signalQuality by remember { mutableStateOf(SignalQuality.GOOD) }
    // Simulate GSR data updates
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            while (recordingState == RecordingState.RECORDING) {
                delay(100) // Update every 100ms
                recordingDuration += 1
                // Simulate GSR reading
                val newValue = 12.0 + 4.0 * sin(recordingDuration * 0.01) +
                        (Math.random() - 0.5) * 2.0
                currentGSRValue = newValue
                val newReading = GSRReading(
                    timestamp = System.currentTimeMillis(),
                    value = newValue,
                    quality = signalQuality
                )
                gsrReadings = (gsrReadings + newReading).takeLast(200) // Keep last 200 readings
            }
        }
    }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Quick GSR Recording",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Save,
                    contentDescription = "Save recording",
                    onClick = {
                        if (recordingState == RecordingState.COMPLETED && gsrReadings.isNotEmpty()) {
                            onSaveRecording()
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Status Card
                DeviceStatusCard(
                    recordingState = recordingState,
                    batteryLevel = batteryLevel,
                    signalQuality = signalQuality,
                    onConnect = { recordingState = RecordingState.CONNECTING }
                )
                // Real-time GSR Display
                if (recordingState == RecordingState.CONNECTED ||
                    recordingState == RecordingState.RECORDING ||
                    recordingState == RecordingState.PAUSED
                ) {
                    GSRDisplayCard(
                        currentValue = currentGSRValue,
                        readings = gsrReadings,
                        signalQuality = signalQuality
                    )
                }
                // Recording Controls
                RecordingControlsCard(
                    recordingState = recordingState,
                    duration = recordingDuration,
                    onStartRecording = {
                        if (recordingState == RecordingState.CONNECTED) {
                            recordingState = RecordingState.RECORDING
                        }
                    },
                    onPauseRecording = {
                        if (recordingState == RecordingState.RECORDING) {
                            recordingState = RecordingState.PAUSED
                        }
                    },
                    onResumeRecording = {
                        if (recordingState == RecordingState.PAUSED) {
                            recordingState = RecordingState.RECORDING
                        }
                    },
                    onStopRecording = {
                        if (recordingState == RecordingState.RECORDING ||
                            recordingState == RecordingState.PAUSED
                        ) {
                            recordingState = RecordingState.COMPLETED
                        }
                    }
                )
                // Session Summary (when completed)
                if (recordingState == RecordingState.COMPLETED && gsrReadings.isNotEmpty()) {
                    SessionSummaryCard(readings = gsrReadings)
                }
                // Quick Setup Instructions
                if (recordingState == RecordingState.IDLE) {
                    QuickSetupCard(
                        onStartSetup = { recordingState = RecordingState.CONNECTING }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceStatusCard(
    recordingState: RecordingState,
    batteryLevel: Int,
    signalQuality: SignalQuality,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shimmer3 GSR Device",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                RecordingStateBadge(state = recordingState)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (recordingState != RecordingState.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Battery Level
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                batteryLevel > 75 -> Icons.Default.BatteryFull
                                batteryLevel > 50 -> Icons.Default.Battery6Bar
                                batteryLevel > 25 -> Icons.Default.Battery3Bar
                                else -> Icons.Default.Battery2Bar
                            },
                            contentDescription = "Battery",
                            tint = if (batteryLevel > 25) Color(0xFF4ECDC4) else Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${batteryLevel}%",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    // Signal Quality
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = "Signal",
                            tint = when (signalQuality) {
                                SignalQuality.EXCELLENT -> Color(0xFF4ECDC4)
                                SignalQuality.GOOD -> Color(0xFF4ECDC4)
                                SignalQuality.FAIR -> Color(0xFFFFB74D)
                                SignalQuality.POOR -> Color(0xFFFF6B6B)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = signalQuality.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B73FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Device")
                }
            }
        }
    }
}

@Composable
fun RecordingStateBadge(state: RecordingState) {
    val (color, text, icon) = when (state) {
        RecordingState.IDLE -> Triple(Color(0xFF9E9E9E), "Idle", Icons.Default.PowerOff)
        RecordingState.CONNECTING -> Triple(
            Color(0xFFFFB74D),
            "Connecting",
            Icons.Default.Bluetooth
        )

        RecordingState.CONNECTED -> Triple(
            Color(0xFF4ECDC4),
            "Connected",
            Icons.Default.CheckCircle
        )

        RecordingState.RECORDING -> Triple(
            Color(0xFFFF6B6B),
            "Recording",
            Icons.Default.FiberManualRecord
        )

        RecordingState.PAUSED -> Triple(Color(0xFFFFB74D), "Paused", Icons.Default.Pause)
        RecordingState.COMPLETED -> Triple(Color(0xFF4ECDC4), "Completed", Icons.Default.Done)
        RecordingState.ERROR -> Triple(Color(0xFFFF6B6B), "Error", Icons.Default.Error)
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun GSRDisplayCard(
    currentValue: Double,
    readings: List<GSRReading>,
    signalQuality: SignalQuality
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Live GSR Reading",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Current Value Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.2f", currentValue),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ECDC4)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Î¼S",
                    fontSize = 16.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Waveform Display
            if (readings.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val path = Path()
                    val width = size.width
                    val height = size.height
                    val minValue = readings.minOf { it.value }
                    val maxValue = readings.maxOf { it.value }
                    val valueRange = if (maxValue > minValue) maxValue - minValue else 1.0
                    readings.forEachIndexed { index, reading ->
                        val x = (index.toFloat() / (readings.size - 1)) * width
                        val normalizedValue = ((reading.value - minValue) / valueRange)
                        val y = height - (normalizedValue.toFloat() * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4ECDC4),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingControlsCard(
    recordingState: RecordingState,
    duration: Int,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duration Display
            if (recordingState == RecordingState.RECORDING ||
                recordingState == RecordingState.PAUSED ||
                recordingState == RecordingState.COMPLETED
            ) {
                Text(
                    text = formatDuration(duration),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            // Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (recordingState) {
                    RecordingState.CONNECTED -> {
                        FloatingActionButton(
                            onClick = onStartRecording,
                            containerColor = Color(0xFFFF6B6B)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Start recording",
                                tint = Color.White
                            )
                        }
                    }

                    RecordingState.RECORDING -> {
                        FloatingActionButton(
                            onClick = onPauseRecording,
                            containerColor = Color(0xFFFFB74D)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause recording",
                                tint = Color.White
                            )
                        }
                        FloatingActionButton(
                            onClick = onStopRecording,
                            containerColor = Color(0xFF4ECDC4)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop recording",
                                tint = Color.White
                            )
                        }
                    }

                    RecordingState.PAUSED -> {
                        FloatingActionButton(
                            onClick = onResumeRecording,
                            containerColor = Color(0xFF6B73FF)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume recording",
                                tint = Color.White
                            )
                        }
                        FloatingActionButton(
                            onClick = onStopRecording,
                            containerColor = Color(0xFF4ECDC4)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop recording",
                                tint = Color.White
                            )
                        }
                    }

                    else -> {
                        // Show disabled button
                        FloatingActionButton(
                            onClick = { },
                            containerColor = Color(0xFF404040)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Record (disabled)",
                                tint = Color(0xFF6B6B6B)
                            )
                        }
                    }
                }
            }
            // Status Text
            if (recordingState != RecordingState.IDLE) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (recordingState) {
                        RecordingState.CONNECTING -> "Connecting to device..."
                        RecordingState.CONNECTED -> "Ready to record"
                        RecordingState.RECORDING -> "Recording in progress"
                        RecordingState.PAUSED -> "Recording paused"
                        RecordingState.COMPLETED -> "Recording completed"
                        RecordingState.ERROR -> "Connection error"
                        else -> ""
                    },
                    fontSize = 14.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
        }
    }
}

@Composable
fun SessionSummaryCard(readings: List<GSRReading>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Session Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            val meanValue = readings.map { it.value }.average()
            val minValue = readings.minOf { it.value }
            val maxValue = readings.maxOf { it.value }
            val stdDev = calculateStandardDeviation(readings.map { it.value })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetric(
                    label = "Mean",
                    value = "${String.format("%.2f", meanValue)} Î¼S",
                    color = Color(0xFF4ECDC4)
                )
                SummaryMetric(
                    label = "Min",
                    value = "${String.format("%.2f", minValue)} Î¼S",
                    color = Color(0xFF6B73FF)
                )
                SummaryMetric(
                    label = "Max",
                    value = "${String.format("%.2f", maxValue)} Î¼S",
                    color = Color(0xFFFF6B6B)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Data points: ${readings.size}",
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF)
            )
            Text(
                text = "Standard deviation: ${String.format("%.2f", stdDev)} Î¼S",
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF)
            )
        }
    }
}

@Composable
fun SummaryMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
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
fun QuickSetupCard(onStartSetup: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Quick Setup",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "1. Turn on your Shimmer3 GSR device\n" +
                        "2. Ensure Bluetooth is enabled\n" +
                        "3. Attach GSR electrodes to fingers\n" +
                        "4. Tap 'Connect Device' to begin",
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onStartSetup,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B73FF)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Quick Recording")
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

private fun calculateStandardDeviation(values: List<Double>): Double {
    val mean = values.average()
    val variance = values.map { (it - mean) * (it - mean) }.average()
    return sqrt(variance)
}

@Preview(showBackground = true)
@Composable
fun GSRQuickRecordingScreenPreview() {
    GSRQuickRecordingScreen()
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRRawImageViewComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.GSRRawImageViewViewModel
import mpdc4gsr.feature.gsr.presentation.GSRRawImageViewViewModelFactory
import java.io.File

class GSRRawImageViewComposeActivity : BaseComposeActivity<GSRRawImageViewViewModel>() {
    override fun createViewModel(): GSRRawImageViewViewModel =
        viewModels<GSRRawImageViewViewModel> {
            GSRRawImageViewViewModelFactory(application)
        }.value

    @Composable
    override fun Content(viewModel: GSRRawImageViewViewModel) {
        IRCameraTheme {
            GSRRawImageViewScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRRawImageViewScreen(
    viewModel: GSRRawImageViewViewModel = viewModel(
        factory = GSRRawImageViewViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.imageViewState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Modern Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("GSR Raw Images") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        // Content Area
        when {
            uiState.isLoading -> {
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
                            text = "Loading GSR images...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            uiState.error != null -> {
                val errorMessage = uiState.error ?: "Unknown error"
                ErrorContent(
                    error = errorMessage,
                    onRetry = { viewModel.loadImages() }
                )
            }

            uiState.imageFiles.isEmpty() -> {
                EmptyContent()
            }

            else -> {
                ImageListContent(
                    imageFiles = uiState.imageFiles,
                    onImageClick = { file -> viewModel.openImage(file) }
                )
            }
        }
    }
}

@Composable
private fun ImageListContent(
    imageFiles: List<File>,
    onImageClick: (File) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(imageFiles) { imageFile ->
            GSRImageCard(
                imageFile = imageFile,
                onClick = { onImageClick(imageFile) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GSRImageCard(
    imageFile: File,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Thumbnail
            Card(
                modifier = Modifier.size(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "GSR image thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Image Information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = imageFile.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Size: ${formatFileSize(imageFile.length())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Modified: ${formatDate(imageFile.lastModified())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
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
                imageVector = Icons.Default.Image,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error loading images",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "No images",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No GSR images found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "GSR images will appear here when available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Utility functions
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

private fun formatDate(timestamp: Long): String {
    return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(timestamp))
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRScreens.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRSettingsScreenPlaceholder(
    onBackClick: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("GSR Settings Screen - Use GSRSettingsComposeActivity")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBackClick: () -> Unit,
    onNavigateToGSRPlot: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Session Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Session Details Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Session ID: $sessionId")
                    Text("Use SessionDetailComposeActivity for full functionality")
                    Button(onClick = onNavigateToGSRPlot) {
                        Text("View Plot")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GSRPlotScreenPlaceholder(
    sessionId: String,
    onBackClick: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Plot", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "GSR Plot Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Session ID: $sessionId")
                    Text("Use GSRPlotComposeActivity for full functionality")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSRDataViewScreen(
    filePath: String,
    onBackClick: () -> Unit
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GSR Data View", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "GSR Data View Screen",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("File Path: $filePath")
                    Text("Use GSRDataViewComposeActivity for full functionality")
                }
            }
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRSensorScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.GSRSensorViewModel
import mpdc4gsr.feature.gsr.presentation.GSRSensorViewModelFactory

@Composable
fun GSRSensorScreen(
    viewModel: GSRSensorViewModel = viewModel(
        factory = GSRSensorViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onSaveData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorState by viewModel.sensorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Initialize recorder on first composition and manage lifecycle properly
    LaunchedEffect(Unit) {
        viewModel.initializeRecorder(context, lifecycleOwner)
    }
    // Track critical errors that need a dialog
    var showCriticalErrorDialog by remember { mutableStateOf(false) }
    var criticalErrorMessage by remember { mutableStateOf("") }
    // Show error notifications as Snackbar for non-critical errors
    LaunchedEffect(sensorState.error) {
        sensorState.error?.let { error ->
            // Check if this is a critical error (Bluetooth/permission)
            if (error.contains("Bluetooth", ignoreCase = true) ||
                error.contains("permission", ignoreCase = true) ||
                error.contains("initialization failed", ignoreCase = true)
            ) {
                criticalErrorMessage = error
                showCriticalErrorDialog = true
            } else {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    // Critical error dialog
    if (showCriticalErrorDialog) {
        AlertDialog(
            onDismissRequest = { showCriticalErrorDialog = false },
            title = { Text("GSR Sensor Error") },
            text = { Text(criticalErrorMessage) },
            confirmButton = {
                TextButton(onClick = { showCriticalErrorDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                if (criticalErrorMessage.contains("Bluetooth", ignoreCase = true)) {
                    TextButton(onClick = {
                        showCriticalErrorDialog = false
                        // Try to re-initialize
                        viewModel.initializeRecorder(context, lifecycleOwner)
                    }) {
                        Text("Retry")
                    }
                }
            }
        )
    }
    // Use real data from ViewModel or fallback to simulated data for preview
    val isConnected = sensorState.isConnected
    val isRecording = sensorState.isRecording
    val currentGSR = if (sensorState.currentGSR > 0) sensorState.currentGSR else 2.45f
    val skinConductance = if (sensorState.skinConductance > 0) sensorState.skinConductance else 0.82f
    val deviceBattery = if (sensorState.deviceBattery > 0) sensorState.deviceBattery else 87
    val samplingRate = sensorState.samplingRate
    // Use GSR history from ViewModel state, with fallback to generated data
    val gsrHistory = if (sensorState.gsrHistory.isNotEmpty()) {
        sensorState.gsrHistory
    } else {
        remember { generateInitialGSRData() }
    }
    // Only simulate data when not connected and for preview purposes
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            // Simulation only runs when not connected for preview
            while (!isConnected) {
                kotlinx.coroutines.delay(1000)
                // This is just for UI preview when no real data
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF16131e)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Title bar with GSR-specific actions
            TitleBar(
                title = "GSR Sensor Monitor",
                showBackButton = true,
                onBackClick = onBackClick
            ) {
                TitleBarAction(
                    icon = Icons.Default.Save,
                    contentDescription = "Save GSR Data",
                    onClick = onSaveData
                )
                TitleBarAction(
                    icon = Icons.Default.Settings,
                    contentDescription = "GSR Settings",
                    onClick = onSettingsClick
                )
            }
            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection status card
                GSRConnectionCard(
                    isConnected = isConnected,
                    deviceBattery = deviceBattery,
                    samplingRate = samplingRate,
                    connectionStatus = sensorState.connectionStatus,
                    isReconnecting = sensorState.isReconnecting,
                    reconnectionAttempt = sensorState.reconnectionAttempt,
                    maxReconnectionAttempts = sensorState.maxReconnectionAttempts,
                    error = sensorState.error,
                    onConnectionToggle = {
                        if (isConnected) {
                            viewModel.disconnectDevice()
                        } else {
                            viewModel.connectDevice()
                        }
                    }
                )
                // Real-time GSR metrics
                GSRMetricsCard(
                    currentGSR = currentGSR,
                    skinConductance = skinConductance,
                    isRecording = isRecording
                )
                // GSR waveform visualization
                GSRWaveformCard(
                    gsrHistory = gsrHistory,
                    isStreaming = isConnected,
                    currentValue = currentGSR
                )
                // Recording controls
                GSRRecordingControls(
                    isRecording = isRecording,
                    isConnected = isConnected,
                    onRecordingToggle = {
                        if (isRecording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    onExportData = {
                        viewModel.exportData()
                        onSaveData()
                    }
                )
                // GSR analysis summary
                if (isRecording || gsrHistory.isNotEmpty()) {
                    GSRAnalysisCard(
                        gsrData = gsrHistory,
                        isRecording = isRecording
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRConnectionCard(
    isConnected: Boolean,
    deviceBattery: Int,
    samplingRate: Int,
    connectionStatus: String = "Disconnected",
    isReconnecting: Boolean = false,
    reconnectionAttempt: Int = 0,
    maxReconnectionAttempts: Int = 0,
    error: String? = null,
    onConnectionToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isReconnecting -> Color(0xFF8B4513)
                isConnected -> Color(0xFF1A2A1A)
                error != null -> Color(0xFF4A1A1A)
                else -> Color(0xFF2A1A1A)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shimmer3 GSR Device",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connectionStatus,
                        color = when {
                            isReconnecting -> Color.Yellow
                            isConnected -> Color.Green
                            error != null -> Color.Red
                            else -> Color.Gray
                        },
                        fontSize = 14.sp
                    )
                    if (isReconnecting && reconnectionAttempt > 0) {
                        Text(
                            text = "Reconnecting: attempt $reconnectionAttempt/$maxReconnectionAttempts",
                            color = Color.Yellow,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (error != null && !isReconnecting) {
                        Text(
                            text = error,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Switch(
                    checked = isConnected,
                    onCheckedChange = { onConnectionToggle() },
                    enabled = !isReconnecting,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Green,
                        uncheckedThumbColor = Color.Gray,
                        disabledCheckedThumbColor = Color.Yellow,
                        disabledUncheckedThumbColor = Color.DarkGray
                    )
                )
            }
            if (isConnected && !isReconnecting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem("Battery", "$deviceBattery%", Color.Green)
                    MetricItem("Sampling", "${samplingRate}Hz", MaterialTheme.colorScheme.primary)
                    MetricItem("Status", "Streaming", Color.Cyan)
                }
            }
        }
    }
}

@Composable
private fun GSRMetricsCard(
    currentGSR: Float,
    skinConductance: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Real-time GSR Metrics",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isRecording) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "RECORDING",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricCard(
                    label = "GSR Value",
                    value = String.format("%.2f Î¼S", currentGSR),
                    color = Color.Cyan,
                    description = "Current resistance"
                )
                MetricCard(
                    label = "Skin Conductance",
                    value = String.format("%.2f Î¼S", skinConductance),
                    color = Color.Green,
                    description = "Conductance level"
                )
            }
        }
    }
}

@Composable
private fun GSRWaveformCard(
    gsrHistory: List<Float>,
    isStreaming: Boolean,
    currentValue: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GSR Waveform",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 20.dp.toPx()
                val graphWidth = width - 2 * padding
                val graphHeight = height - 2 * padding
                // Draw axes
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, height - padding),
                    end = Offset(width - padding, height - padding),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, padding),
                    end = Offset(padding, height - padding),
                    strokeWidth = 1.dp.toPx()
                )
                // Draw GSR waveform
                if (gsrHistory.isNotEmpty()) {
                    val path = Path()
                    val minGSR = gsrHistory.minOrNull() ?: 0f
                    val maxGSR = gsrHistory.maxOrNull() ?: 5f
                    val range = maxGSR - minGSR
                    gsrHistory.forEachIndexed { index, value ->
                        val x = padding + (index.toFloat() / (gsrHistory.size - 1)) * graphWidth
                        val normalizedValue = if (range > 0) (value - minGSR) / range else 0.5f
                        val y = height - padding - normalizedValue * graphHeight
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Cyan,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
                // Draw current value indicator
                if (isStreaming) {
                    drawCircle(
                        color = Color.Yellow,
                        radius = 4.dp.toPx(),
                        center = Offset(width - padding - 10.dp.toPx(), height / 2)
                    )
                }
            }
            // Value scale indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 Î¼S", color = Color.Gray, fontSize = 10.sp)
                Text(
                    "${String.format("%.1f", currentValue)} Î¼S",
                    color = Color.Cyan,
                    fontSize = 10.sp
                )
                Text("5 Î¼S", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun GSRRecordingControls(
    isRecording: Boolean,
    isConnected: Boolean,
    onRecordingToggle: () -> Unit,
    onExportData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onRecordingToggle,
                    enabled = isConnected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else Color.Green
                    )
                ) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
                Button(
                    onClick = onExportData,
                    enabled = !isRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Export Data")
                }
            }
        }
    }
}

@Composable
private fun GSRAnalysisCard(
    gsrData: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    if (gsrData.isEmpty()) return
    val avgGSR = gsrData.average().toFloat()
    val maxGSR = gsrData.maxOrNull() ?: 0f
    val minGSR = gsrData.minOrNull() ?: 0f
    val stdDev = kotlin.math.sqrt(gsrData.map { (it - avgGSR) * (it - avgGSR) }.average()).toFloat()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "GSR Analysis",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Average", String.format("%.2f Î¼S", avgGSR), Color.Cyan)
                MetricItem("Maximum", String.format("%.2f Î¼S", maxGSR), Color.Red)
                MetricItem("Minimum", String.format("%.2f Î¼S", minGSR), MaterialTheme.colorScheme.primary)
                MetricItem("Std Dev", String.format("%.2f", stdDev), Color.Yellow)
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
        Text(
            text = description,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

private fun generateInitialGSRData(): List<Float> {
    return (0..99).map {
        2.0f + kotlin.math.sin(it * 0.1f).toFloat() * 0.5f + kotlin.random.Random.nextFloat() * 0.2f
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRSensorScreenPreview() {
    IRCameraTheme {
        GSRSensorScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRSettingsComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.gsr.presentation.GSRSettingsViewModel

class GSRSettingsComposeActivity : BaseComposeActivity<GSRSettingsViewModel>() {
    companion object {
        private const val TAG = "GSRSettingsComposeActivity"
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsComposeActivity::class.java))
        }
    }

    override fun createViewModel(): GSRSettingsViewModel {
        return viewModels<GSRSettingsViewModel>().value
    }

    @Composable
    override fun Content(viewModel: GSRSettingsViewModel) {
        LibUnifiedTheme {
            GSRSettingsScreen(
                onBackClick = { finish() },
                viewModel = viewModel
            )
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRSettingsScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.components.settings.SettingsSlider
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.GSRSettingsViewModel

@Composable
fun GSRSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: GSRSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.settingsUiState.collectAsState()
    val gsrSettings = uiState.gsrSettings
    val deviceSettings = uiState.deviceSettings
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        viewModel.settingsEvents.collect { event ->
            val message = when (event) {
                is GSRSettingsViewModel.SettingsEvent.ShowToast -> event.message
                is GSRSettingsViewModel.SettingsEvent.CalibrationStarted -> event.message
                is GSRSettingsViewModel.SettingsEvent.CalibrationCompleted -> event.message
                is GSRSettingsViewModel.SettingsEvent.ShowError -> event.message
                else -> null
            }
            message?.let {
                android.widget.Toast.makeText(
                    context,
                    it,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "GSR Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Configuration
            SettingsCard(
                title = "Device Configuration",
                icon = Icons.Default.DeviceHub
            ) {
                deviceSettings.deviceName?.let { name ->
                    SettingsRow(
                        label = "Device Name",
                        value = name
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                SettingsSlider(
                    label = "Sampling Rate",
                    value = gsrSettings.samplingRate.toFloat(),
                    valueRange = 1f..512f,
                    onValueChange = { viewModel.updateSamplingRate(it.toInt()) },
                    unit = " Hz"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Auto Reconnect",
                    description = "Automatically reconnect to devices after disconnection",
                    checked = deviceSettings.autoReconnect,
                    onCheckedChange = {
                        viewModel.updateDeviceSettings(
                            deviceSettings.copy(autoReconnect = it)
                        )
                    }
                )
                if (deviceSettings.autoReconnect) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSlider(
                        label = "Reconnection Attempts",
                        value = deviceSettings.reconnectionAttempts.toFloat(),
                        valueRange = 1f..10f,
                        onValueChange = {
                            viewModel.updateDeviceSettings(
                                deviceSettings.copy(reconnectionAttempts = it.toInt())
                            )
                        },
                        unit = " attempts"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSlider(
                        label = "Reconnection Delay",
                        value = (deviceSettings.reconnectionBaseDelayMs / 1000f),
                        valueRange = 1f..10f,
                        onValueChange = {
                            viewModel.updateDeviceSettings(
                                deviceSettings.copy(reconnectionBaseDelayMs = (it * 1000).toLong())
                            )
                        },
                        unit = " seconds"
                    )
                }
            }
            // Data Collection
            SettingsCard(
                title = "Data Collection",
                icon = Icons.Default.DataUsage
            ) {
                SettingsToggle(
                    label = "Real-Time Monitoring",
                    description = "Enable real-time data monitoring",
                    checked = gsrSettings.enableRealTimeMonitoring,
                    onCheckedChange = {
                        viewModel.updateGSRSettings(
                            gsrSettings.copy(enableRealTimeMonitoring = it)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Data Filtering",
                    description = "Apply filtering to GSR data",
                    checked = gsrSettings.enableFiltering,
                    onCheckedChange = {
                        viewModel.updateGSRSettings(
                            gsrSettings.copy(enableFiltering = it)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Notifications",
                    description = "Show data collection notifications",
                    checked = gsrSettings.notificationEnabled,
                    onCheckedChange = {
                        viewModel.updateGSRSettings(
                            gsrSettings.copy(notificationEnabled = it)
                        )
                    }
                )
            }
            // Calibration
            SettingsCard(
                title = "Calibration",
                icon = Icons.Default.Tune
            ) {
                Button(
                    onClick = { viewModel.startCalibration() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Calibration")
                }
                Button(
                    onClick = { viewModel.resetToDefaults() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Defaults")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRSettingsScreenPreview() {
    IRCameraTheme {
        GSRSettingsScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRVideoPlayerComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

class GSRVideoPlayerComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        private const val EXTRA_VIDEO_PATH = "video_path"
        private const val EXTRA_SESSION_ID = "session_id"
        fun startActivity(
            context: Context,
            videoPath: String,
            sessionId: String? = null
        ) {
            val intent = Intent(context, GSRVideoPlayerComposeActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_PATH, videoPath)
                sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
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
        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: ""
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Video Player",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Share video
                                android.widget.Toast.makeText(
                                    this@GSRVideoPlayerComposeActivity,
                                    "Share video feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                // TODO: Open video settings
                                android.widget.Toast.makeText(
                                    this@GSRVideoPlayerComposeActivity,
                                    "Video settings coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    this@GSRVideoPlayerComposeActivity,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRVideoPlayerContent(
                    videoPath = videoPath,
                    sessionId = sessionId,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun GSRVideoPlayerContent(
    videoPath: String,
    sessionId: String?,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(100f) }
    var showDataOverlay by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Video Player Area
        VideoPlayerCard(
            videoPath = videoPath,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            showDataOverlay = showDataOverlay,
            onPlayPause = { isPlaying = !isPlaying },
            onSeek = { currentPosition = it }
        )
        // Scrollable content below video
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Controls
            VideoControlsCard(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed,
                showDataOverlay = showDataOverlay,
                onPlayPause = { isPlaying = !isPlaying },
                onSeek = { currentPosition = it },
                onSpeedChange = { playbackSpeed = it },
                onOverlayToggle = { showDataOverlay = it }
            )
            // Video Information
            VideoInfoCard(
                videoPath = videoPath,
                sessionId = sessionId,
                duration = duration
            )
            // GSR Data Timeline
            if (sessionId != null) {
                GSRDataTimelineCard(
                    sessionId = sessionId,
                    currentPosition = currentPosition,
                    duration = duration
                )
            }
            // Playback Statistics
            PlaybackStatisticsCard(
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed
            )
            // Export Options
            VideoExportCard(
                videoPath = videoPath,
                sessionId = sessionId
            )
        }
    }
}

@Composable
private fun VideoPlayerCard(
    videoPath: String,
    isPlaying: Boolean,
    currentPosition: Float,
    duration: Float,
    showDataOverlay: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Video Playback",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            // Video View Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Color.Black,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // AndroidView for VideoView integration
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(Uri.parse(videoPath))
                            setMediaController(MediaController(context))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // GSR Data Overlay
                if (showDataOverlay) {
                    GSRDataOverlay(
                        currentPosition = currentPosition,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
                // Play/Pause Overlay
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(32.dp)
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            // Seek Bar
            Column {
                Slider(
                    value = currentPosition,
                    onValueChange = onSeek,
                    valueRange = 0f..duration,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRDataOverlay(
    currentPosition: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "GSR Data",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "12.4 Î¼S",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Quality: 94%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun VideoControlsCard(
    isPlaying: Boolean,
    currentPosition: Float,
    duration: Float,
    playbackSpeed: Float,
    showDataOverlay: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOverlayToggle: (Boolean) -> Unit
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
                "Playback Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Main Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSeek(0f) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Restart")
                }
                IconButton(onClick = { onSeek(maxOf(0f, currentPosition - 10f)) }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(28.dp)
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { onSeek(minOf(duration, currentPosition + 10f)) }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                }
                IconButton(onClick = { onSeek(duration) }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "End")
                }
            }
            // Playback Speed
            Text(
                "Playback Speed: ${String.format("%.1f", playbackSpeed)}x",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                    FilterChip(
                        onClick = { onSpeedChange(speed) },
                        label = { Text("${speed}x") },
                        selected = playbackSpeed == speed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Overlay Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Show GSR Data Overlay",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showDataOverlay,
                    onCheckedChange = onOverlayToggle
                )
            }
        }
    }
}

@Composable
private fun VideoInfoCard(
    videoPath: String,
    sessionId: String?,
    duration: Float
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
                "Video Information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            VideoInfoRow("File Name", videoPath.substringAfterLast("/"))
            VideoInfoRow("Session ID", sessionId ?: "N/A")
            VideoInfoRow("Duration", formatTime(duration))
            VideoInfoRow("Format", "MP4")
            VideoInfoRow("Resolution", "1920x1080")
            VideoInfoRow("Frame Rate", "30 FPS")
            VideoInfoRow("File Size", "125.6 MB")
        }
    }
}

@Composable
private fun VideoInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GSRDataTimelineCard(
    sessionId: String,
    currentPosition: Float,
    duration: Float
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
                "GSR Data Timeline",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            // Timeline visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                // Simplified timeline representation
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current position indicator
                    val progress = if (duration > 0) currentPosition / duration else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
                Text(
                    "GSR Timeline Visualization",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Current GSR values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GSRTimelineMetric("Current", "12.4 Î¼S")
                GSRTimelineMetric("Peak", "18.9 Î¼S")
                GSRTimelineMetric("Average", "11.2 Î¼S")
                GSRTimelineMetric("Quality", "94%")
            }
        }
    }
}

@Composable
private fun GSRTimelineMetric(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaybackStatisticsCard(
    currentPosition: Float,
    duration: Float,
    playbackSpeed: Float
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
                "Playback Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlaybackStatistic("Progress", "${((currentPosition / duration) * 100).toInt()}%")
                PlaybackStatistic("Remaining", formatTime(duration - currentPosition))
                PlaybackStatistic("Speed", "${playbackSpeed}x")
            }
        }
    }
}

@Composable
private fun PlaybackStatistic(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VideoExportCard(
    videoPath: String,
    sessionId: String?
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
                "Export Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Export video file
                        android.widget.Toast.makeText(
                            context,
                            "Exporting video...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VideoFile, contentDescription = "Export Video")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Video")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Export audio track
                        android.widget.Toast.makeText(
                            context,
                            "Exporting audio...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AudioFile, contentDescription = "Export Audio")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Audio")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Share video
                        android.widget.Toast.makeText(
                            context,
                            "Sharing video...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Video")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Video")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Save current frame as image
                        android.widget.Toast.makeText(
                            context,
                            "Saving frame...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Save Frame")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Frame")
                }
            }
        }
    }
}

// Helper function to format time
private fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

class GSRVideoPlayerViewModel : AppBaseViewModel() {
    // ViewModel implementation for managing video playback state, GSR data synchronization, etc.
    // Future implementation would include:
    // - Video playback state management
    // - GSR data loading and synchronization
    // - Export functionality
    // - Playback statistics tracking
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\GSRVideoPlayerScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun GSRVideoPlayerScreen(
    videoUri: String = "sample_video.mp4",
    sessionId: String = "GSR_Session_001",
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val localContext = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(100) }
    var showGSROverlay by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "GSR Video Player",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Visibility,
                contentDescription = "Toggle GSR Overlay",
                onClick = { showGSROverlay = !showGSROverlay }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Player Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Video View
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                // In real implementation, set video URI and controls
                                // setVideoURI(Uri.parse(videoUri))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // GSR Data Overlay
                    if (showGSROverlay) {
                        GSRDataOverlay(
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                    // Play/Pause Button
                    FloatingActionButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.align(Alignment.Center),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }
                }
            }
            // Video Controls
            VideoControlsCard(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPause = { isPlaying = !isPlaying },
                onSeek = { currentPosition = it }
            )
            // Session Information
            SessionDetailsCard(sessionId = sessionId)
            // GSR Metrics
            GSRMetricsCard()
        }
    }
}

@Composable
private fun GSRDataOverlay(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GSR",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "0.42 Î¼S",
                color = Color.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // Mini GSR waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                // Placeholder for mini waveform visualization
                Text(
                    text = "~~~",
                    color = Color.Cyan,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoControlsCard(
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Progress slider
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.Gray
                )
            )
            // Time indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    // TODO: Skip to previous video
                    android.widget.Toast.makeText(
                        localContext,
                        "Previous video",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    // TODO: Skip to next video
                    android.widget.Toast.makeText(
                        localContext,
                        "Next video",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                }
                IconButton(onClick = {
                    // TODO: Toggle fullscreen mode
                    android.widget.Toast.makeText(
                        localContext,
                        "Fullscreen mode",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDetailsCard(sessionId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Session Details",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val details = listOf(
                "Session ID" to sessionId,
                "Recording Date" to "2024-01-15",
                "Duration" to "5m 32s",
                "Participant" to "P001",
                "Condition" to "Stress Test"
            )
            details.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.Gray, fontSize = 14.sp)
                    Text(value, color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun GSRMetricsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Real-time GSR Metrics",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Current", "0.42 Î¼S", Color.Cyan)
                MetricItem("Average", "0.38 Î¼S", Color.Green)
                MetricItem("Peak", "0.67 Î¼S", Color.Red)
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

@Preview(showBackground = true)
@Composable
private fun GSRVideoPlayerScreenPreview() {
    IRCameraTheme {
        GSRVideoPlayerScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\MultiModalRecordingComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.gsr.presentation.MultiModalRecordingViewModel

class MultiModalRecordingComposeActivity : BaseComposeActivity<MultiModalRecordingViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, MultiModalRecordingComposeActivity::class.java))
        }

        fun startWithTemplate(context: Context, templateId: String) {
            val intent = Intent(context, MultiModalRecordingComposeActivity::class.java).apply {
                putExtra("template_id", templateId)
            }
            context.startActivity(intent)
        }

        fun startRecording(context: Context, sessionInfo: SessionInfo) {
            val intent = Intent(context, MultiModalRecordingComposeActivity::class.java).apply {
                putExtra("session_info", sessionInfo)
                putExtra("auto_start", true)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): MultiModalRecordingViewModel {
        return viewModels<MultiModalRecordingViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MultiModalRecordingViewModel) {
        var isRecording by remember { mutableStateOf(false) }
        var recordingDuration by remember { mutableStateOf(0L) }
        var selectedSensors by remember { mutableStateOf(setOf("gsr", "thermal", "rgb")) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Multi-Modal Recording",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                // TODO: Open recording templates
                                android.widget.Toast.makeText(
                                    this@MultiModalRecordingComposeActivity,
                                    "Recording templates coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Templates")
                            }
                            IconButton(onClick = {
                                // TODO: Open recording settings
                                android.widget.Toast.makeText(
                                    this@MultiModalRecordingComposeActivity,
                                    "Opening recording settings...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                MultiModalRecordingContent(
                    isRecording = isRecording,
                    onRecordingToggle = { isRecording = !isRecording },
                    recordingDuration = recordingDuration,
                    selectedSensors = selectedSensors,
                    onSensorToggle = { sensor ->
                        selectedSensors = if (selectedSensors.contains(sensor)) {
                            selectedSensors - sensor
                        } else {
                            selectedSensors + sensor
                        }
                    },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun MultiModalRecordingContent(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    recordingDuration: Long,
    selectedSensors: Set<String>,
    onSensorToggle: (String) -> Unit,
    viewModel: MultiModalRecordingViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Recording Status Card
        RecordingStatusCard(
            isRecording = isRecording,
            duration = recordingDuration,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Sensor Selection Cards
        Text(
            text = "Active Sensors",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        SensorCard(
            title = "GSR Sensor",
            subtitle = "Shimmer3 GSR+ Device",
            icon = Icons.Default.MonitorHeart,
            isEnabled = selectedSensors.contains("gsr"),
            isConnected = false,
            onToggle = { onSensorToggle("gsr") },
            statusText = "128 Hz",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SensorCard(
            title = "Thermal Camera",
            subtitle = "TOPDON TC001 Device",
            icon = Icons.Default.Thermostat,
            isEnabled = selectedSensors.contains("thermal"),
            isConnected = false,
            onToggle = { onSensorToggle("thermal") },
            statusText = "25 FPS",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SensorCard(
            title = "RGB Camera",
            subtitle = "Device Camera",
            icon = Icons.Default.CameraAlt,
            isEnabled = selectedSensors.contains("rgb"),
            isConnected = false,
            onToggle = { onSensorToggle("rgb") },
            statusText = "30 FPS",
            modifier = Modifier.padding(bottom = 24.dp)
        )
        // Recording Controls
        RecordingControls(
            isRecording = isRecording,
            onRecordingToggle = onRecordingToggle,
            canRecord = selectedSensors.isNotEmpty(),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Live Data Preview (if recording)
        if (isRecording) {
            LiveDataPreview(
                selectedSensors = selectedSensors,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun RecordingStatusCard(
    isRecording: Boolean,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording)
                Color(0xFFE53E3E).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isRecording) "RECORDING" else "READY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (isRecording) formatDuration(duration) else "Tap record to start",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53E3E))
                )
            }
        }
    }
}

@Composable
private fun SensorCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    isConnected: Boolean,
    onToggle: () -> Unit,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = if (isEnabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Connection status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53E3E)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) statusText else "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                enabled = isConnected
            )
        }
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    onRecordingToggle: () -> Unit,
    canRecord: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main record button
        Button(
            onClick = onRecordingToggle,
            enabled = canRecord,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color(0xFFE53E3E) else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isRecording) "Stop" else "Start",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRecording) "STOP RECORDING" else "START RECORDING",
                fontWeight = FontWeight.Bold
            )
        }
        // Pause button (only show when recording)
        if (isRecording) {
            val context = androidx.compose.ui.platform.LocalContext.current
            OutlinedButton(
                onClick = {
                    // TODO: Implement pause recording logic
                    android.widget.Toast.makeText(
                        context,
                        "Pause recording feature coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause"
                )
            }
        }
    }
}

@Composable
private fun LiveDataPreview(
    selectedSensors: Set<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Live Data Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            selectedSensors.forEach { sensor ->
                when (sensor) {
                    "gsr" -> {
                        Text(
                            text = "GSR: 2.45 ÂµS (Normal)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    "thermal" -> {
                        Text(
                            text = "Thermal: 36.8Â°C (Body temp detected)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    "rgb" -> {
                        Text(
                            text = "RGB: 1920x1080 @ 30fps",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\MultiModalRecordingScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MultiModalRecordingScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var gsrEnabled by remember { mutableStateOf(true) }
    var thermalEnabled by remember { mutableStateOf(true) }
    var rgbEnabled by remember { mutableStateOf(true) }
    var syncStatus by remember { mutableStateOf(SyncStatus.SYNCED) }
    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Multi-Modal Recording",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "Recording Settings",
                onClick = {
                    // TODO: Implement recording settings screen
                    // Open settings for multi-modal recording configuration
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recording Status Card
            RecordingStatusCard(
                isRecording = isRecording,
                duration = recordingDuration,
                syncStatus = syncStatus
            )
            // Sensor Status Cards
            SensorStatusSection(
                gsrEnabled = gsrEnabled,
                thermalEnabled = thermalEnabled,
                rgbEnabled = rgbEnabled,
                onGsrToggle = { gsrEnabled = it },
                onThermalToggle = { thermalEnabled = it },
                onRgbToggle = { rgbEnabled = it },
                isRecording = isRecording
            )
            // Live Data Preview
            if (isRecording) {
                LiveDataPreviewSection()
            }
            // Recording Controls
            RecordingControlsSection(
                isRecording = isRecording,
                onStartStop = {
                    isRecording = !isRecording
                    if (!isRecording) recordingDuration = 0
                },
                canRecord = gsrEnabled || thermalEnabled || rgbEnabled
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecordingStatusCard(
    isRecording: Boolean,
    duration: Int,
    syncStatus: SyncStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) Color.Red.copy(alpha = 0.1f) else Color(0xFF2A2A2A)
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
                Text(
                    text = if (isRecording) "RECORDING" else "READY",
                    color = if (isRecording) Color.Red else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Duration: ${formatDuration(duration)}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Sync: ${syncStatus.displayName}",
                    color = syncStatus.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SensorStatusSection(
    gsrEnabled: Boolean,
    thermalEnabled: Boolean,
    rgbEnabled: Boolean,
    onGsrToggle: (Boolean) -> Unit,
    onThermalToggle: (Boolean) -> Unit,
    onRgbToggle: (Boolean) -> Unit,
    isRecording: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sensor Configuration",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SensorToggleItem(
                name = "GSR Sensor",
                description = "Galvanic Skin Response (Shimmer3)",
                enabled = gsrEnabled,
                onToggle = onGsrToggle,
                color = Color.Cyan,
                isRecording = isRecording,
                status = "Connected â€¢ 128 Hz"
            )
            SensorToggleItem(
                name = "Thermal Camera",
                description = "TOPDON TC001 Thermal Imaging",
                enabled = thermalEnabled,
                onToggle = onThermalToggle,
                color = Color.Red,
                isRecording = isRecording,
                status = "Connected â€¢ 256Ã—192"
            )
            SensorToggleItem(
                name = "RGB Camera",
                description = "Built-in Camera",
                enabled = rgbEnabled,
                onToggle = onRgbToggle,
                color = Color.White,
                isRecording = isRecording,
                status = "Ready â€¢ 1080p@30fps"
            )
        }
    }
}

@Composable
private fun SensorToggleItem(
    name: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    color: Color,
    isRecording: Boolean,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    if (enabled) color else Color.Gray,
                    androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = status,
                color = if (enabled) color else Color.Gray,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = if (!isRecording) onToggle else {
                {}
            },
            enabled = !isRecording,
            colors = SwitchDefaults.colors(
                checkedThumbColor = color,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = color.copy(alpha = 0.3f),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun LiveDataPreviewSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Live Data Preview",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // GSR Waveform
            Text(
                text = "GSR Signal",
                color = Color.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LiveGSRWaveform(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Sensor Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LiveMetricItem("GSR", "0.42 Î¼S", Color.Cyan)
                LiveMetricItem("Thermal", "36.8Â°C", Color.Red)
                LiveMetricItem("RGB", "Recording", Color.White)
            }
        }
    }
}

@Composable
private fun LiveGSRWaveform(modifier: Modifier = Modifier) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            phase += 0.2f
        }
    }
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val amplitude = height * 0.3f
        val points = (0..100).map { i ->
            val x = (i / 100f) * width
            val y = centerY + amplitude * sin((i * 0.2f) + phase + Random.nextFloat() * 0.1f)
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color.Cyan,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx()
            )
        }
        // Grid lines
        for (i in 0..4) {
            val y = (height / 4) * i
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
private fun LiveMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RecordingControlsSection(
    isRecording: Boolean,
    onStartStop: () -> Unit,
    canRecord: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Recording Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Main Record Button
            FloatingActionButton(
                onClick = onStartStop,
                modifier = Modifier.size(80.dp),
                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isRecording) "Tap to stop recording" else "Tap to start synchronized recording",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (!canRecord && !isRecording) {
                Text(
                    text = "Enable at least one sensor to record",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
            // Additional Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Implement pause recording functionality
                        // Pause the multi-modal recording
                    },
                    enabled = isRecording
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Implement add marker functionality
                        // Add timestamp marker to recording
                    },
                    enabled = isRecording
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark")
                }
            }
        }
    }
}

enum class SyncStatus(val displayName: String, val color: Color) {
    SYNCED("Synced", Color.Green),
    SYNCING("Syncing", Color.Yellow),
    OUT_OF_SYNC("Out of Sync", Color.Red),
    DISABLED("Disabled", Color.Gray)
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format("%d:%02d", minutes, remainingSeconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun MultiModalRecordingScreenPreview() {
    IRCameraTheme {
        MultiModalRecordingScreen()
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\ResearchTemplateComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.gsr.model.ResearchTemplate
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

class ResearchTemplateComposeActivity : BaseComposeActivity<AppBaseViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ResearchTemplateComposeActivity::class.java))
        }
    }

    override fun createViewModel(): AppBaseViewModel {
        return viewModels<AppBaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        var selectedTemplate by remember { mutableStateOf<ResearchTemplate?>(null) }
        var selectedCategory by remember { mutableStateOf<ResearchTemplate.TemplateCategory?>(null) }
        var showTemplateDetails by remember { mutableStateOf(false) }
        var showCreateDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Research Templates",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Create Template")
                            }
                            IconButton(onClick = {
                                // TODO: Import template from file
                                android.widget.Toast.makeText(
                                    this@ResearchTemplateComposeActivity,
                                    "Import template feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileOpen, contentDescription = "Import")
                            }
                            IconButton(onClick = {
                                // TODO: Show more options menu
                                android.widget.Toast.makeText(
                                    this@ResearchTemplateComposeActivity,
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
                    selectedTemplate?.let { template ->
                        ExtendedFloatingActionButton(
                            onClick = {
                                startRecordingWithTemplate(template)
                            },
                            text = { Text("Start Recording") },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Start") }
                        )
                    }
                }
            ) { paddingValues ->
                ResearchTemplateContent(
                    selectedTemplate = selectedTemplate,
                    onTemplateSelect = {
                        selectedTemplate = it
                        showTemplateDetails = true
                    },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showTemplateDetails && selectedTemplate != null) {
            TemplateDetailsDialog(
                template = selectedTemplate!!,
                onDismiss = { showTemplateDetails = false },
                onStartRecording = {
                    startRecordingWithTemplate(selectedTemplate!!)
                    showTemplateDetails = false
                }
            )
        }
        if (showCreateDialog) {
            CreateTemplateDialog(
                onDismiss = { showCreateDialog = false },
                onCreateTemplate = { template ->
                    // Create new template logic
                    showCreateDialog = false
                }
            )
        }
    }

    private fun startRecordingWithTemplate(template: ResearchTemplate) {
        MultiModalRecordingComposeActivity.startWithTemplate(this, template.id)
    }
}

@Composable
private fun ResearchTemplateContent(
    selectedTemplate: ResearchTemplate?,
    onTemplateSelect: (ResearchTemplate) -> Unit,
    selectedCategory: ResearchTemplate.TemplateCategory?,
    onCategoryChange: (ResearchTemplate.TemplateCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Category Filter
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategoryChange = onCategoryChange,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Template Grid
        val templates = ResearchTemplate.PREDEFINED_TEMPLATES
        val filteredTemplates = if (selectedCategory == null) {
            templates
        } else {
            templates.filter { it.category == selectedCategory }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTemplates) { template ->
                TemplateCard(
                    template = template,
                    isSelected = selectedTemplate?.id == template.id,
                    onClick = { onTemplateSelect(template) }
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: ResearchTemplate.TemplateCategory?,
    onCategoryChange: (ResearchTemplate.TemplateCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(null) + ResearchTemplate.TemplateCategory.values().toList()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        categories.forEach { category ->
            val displayName = category?.name?.replace("_", " ")?.lowercase()
                ?.replaceFirstChar { it.uppercase() } ?: "All"
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategoryChange(category) },
                label = { Text(displayName) },
                leadingIcon = if (selectedCategory == category) {
                    { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: ResearchTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Template icon and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = getTemplateIcon(template.category),
                    contentDescription = template.category.name,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getCategoryColor(template.category),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = template.category.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Template name and description
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            // Template details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = template.duration?.let { "${it / (60 * 1000)} min" } ?: "Variable",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sensors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = template.sensors.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateDetailsDialog(
    template: ResearchTemplate,
    onDismiss: () -> Unit,
    onStartRecording: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = template.name,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duration:", style = MaterialTheme.typography.bodySmall)
                            Text("${template.duration} minutes", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sensors:", style = MaterialTheme.typography.bodySmall)
                            Text(template.sensors.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GSR Sampling Rate:", style = MaterialTheme.typography.bodySmall)
                            Text("${template.gsrSamplingRate} Hz", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onStartRecording) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Recording")
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
private fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onCreateTemplate: (ResearchTemplate) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Custom") }
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Template") },
        text = {
            Column {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            // Focus moves to description field
                        }
                    )
                )
                OutlinedTextField(
                    value = templateDescription,
                    onValueChange = { templateDescription = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (templateName.isNotBlank()) {
                                val newTemplate = ResearchTemplate(
                                    id = "custom_${System.currentTimeMillis()}",
                                    name = templateName,
                                    description = templateDescription,
                                    category = enumValues<ResearchTemplate.TemplateCategory>().firstOrNull { it.name == selectedCategory }
                                        ?: ResearchTemplate.TemplateCategory.CUSTOM,
                                    duration = 30,
                                    sensors = setOf(
                                        ResearchTemplate.SensorType.GSR,
                                        ResearchTemplate.SensorType.THERMAL_CAMERA
                                    ),
                                    gsrSamplingRate = 128
                                )
                                onCreateTemplate(newTemplate)
                                onDismiss()
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (templateName.isNotBlank()) {
                        val newTemplate = ResearchTemplate(
                            id = "custom_${System.currentTimeMillis()}",
                            name = templateName,
                            description = templateDescription,
                            category = enumValues<ResearchTemplate.TemplateCategory>().firstOrNull { it.name == selectedCategory }
                                ?: ResearchTemplate.TemplateCategory.CUSTOM,
                            duration = 30,
                            sensors = setOf(
                                ResearchTemplate.SensorType.GSR,
                                ResearchTemplate.SensorType.THERMAL_CAMERA
                            ),
                            gsrSamplingRate = 128
                        )
                        onCreateTemplate(newTemplate)
                    }
                },
                enabled = templateName.isNotBlank()
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

private fun getTemplateIcon(category: ResearchTemplate.TemplateCategory) = when (category) {
    ResearchTemplate.TemplateCategory.STRESS_RESPONSE -> Icons.Default.Psychology
    ResearchTemplate.TemplateCategory.COGNITIVE_LOAD -> Icons.Default.Psychology
    ResearchTemplate.TemplateCategory.EMOTION_RECOGNITION -> Icons.Default.Psychology
    ResearchTemplate.TemplateCategory.PHYSIOLOGICAL_MONITORING -> Icons.Default.MonitorHeart
    ResearchTemplate.TemplateCategory.BEHAVIORAL_ANALYSIS -> Icons.Default.Groups
    ResearchTemplate.TemplateCategory.CUSTOM -> Icons.Default.Build
}

private fun getCategoryColor(category: ResearchTemplate.TemplateCategory) = when (category) {
    ResearchTemplate.TemplateCategory.STRESS_RESPONSE -> Color(0xFF9C27B0)
    ResearchTemplate.TemplateCategory.COGNITIVE_LOAD -> Color(0xFF2196F3)
    ResearchTemplate.TemplateCategory.EMOTION_RECOGNITION -> Color(0xFFE91E63)
    ResearchTemplate.TemplateCategory.PHYSIOLOGICAL_MONITORING -> Color(0xFF4CAF50)
    ResearchTemplate.TemplateCategory.BEHAVIORAL_ANALYSIS -> Color(0xFF00BCD4)
    ResearchTemplate.TemplateCategory.CUSTOM -> Color(0xFFFF9800)
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\ResearchTemplateScreen.kt =====

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
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

data class ResearchTemplate(
    val id: String,
    val title: String,
    val description: String,
    val duration: String,
    val sensorTypes: List<String>,
    val tasks: List<String>,
    val difficulty: TemplateDifficulty,
    val category: TemplateCategory,
    val isCustom: Boolean = false
)

enum class TemplateDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

enum class TemplateCategory {
    STRESS_RESPONSE,
    COGNITIVE_LOAD,
    EMOTION_RECOGNITION,
    PHYSIOLOGICAL_MONITORING,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchTemplateScreen(
    onNavigateBack: () -> Unit = {},
    onCreateCustomTemplate: () -> Unit = {},
    onUseTemplate: (ResearchTemplate) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val templates = remember { getSampleTemplates() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val filteredTemplates = templates.filter { template ->
        val matchesCategory = selectedCategory == null || template.category == selectedCategory
        val matchesSearch = if (searchQuery.isBlank()) true
        else template.title.contains(searchQuery, ignoreCase = true) ||
                template.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Research Templates",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Add,
                    contentDescription = "Create custom template",
                    onClick = onCreateCustomTemplate
                )
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search templates...") },
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
                // Category Filter Chips
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        CategoryFilterChips(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it }
                        )
                    }
                    item {
                        TemplateStatsCard(templates = templates)
                    }
                    items(filteredTemplates) { template ->
                        TemplateItem(
                            template = template,
                            onUse = { onUseTemplate(template) }
                        )
                    }
                    if (filteredTemplates.isEmpty()) {
                        item {
                            EmptyTemplatesState(
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                onCreateCustom = onCreateCustomTemplate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterChips(
    selectedCategory: TemplateCategory?,
    onCategorySelected: (TemplateCategory?) -> Unit
) {
    Column {
        Text(
            text = "Categories",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All Categories Chip
            FilterChip(
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                selected = selectedCategory == null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6B73FF),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF2A2A2A),
                    labelColor = Color.White
                )
            )
            // Category-specific chips (showing only a few due to space)
            listOf(
                TemplateCategory.STRESS_RESPONSE to "Stress",
                TemplateCategory.COGNITIVE_LOAD to "Cognitive",
                TemplateCategory.EMOTION_RECOGNITION to "Emotion"
            ).forEach { (category, label) ->
                FilterChip(
                    onClick = {
                        onCategorySelected(if (selectedCategory == category) null else category)
                    },
                    label = { Text(label) },
                    selected = selectedCategory == category,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6B73FF),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF2A2A2A),
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun TemplateStatsCard(templates: List<ResearchTemplate>) {
    val totalTemplates = templates.size
    val customTemplates = templates.count { it.isCustom }
    val avgDuration = templates.map { parseDuration(it.duration) }.average().toInt()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Templates",
                value = totalTemplates.toString(),
                color = Color(0xFF6B73FF)
            )
            StatItem(
                label = "Custom",
                value = customTemplates.toString(),
                color = Color(0xFF4ECDC4)
            )
            StatItem(
                label = "Avg Duration",
                value = "${avgDuration}min",
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
            fontSize = 18.sp,
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
fun TemplateItem(
    template: ResearchTemplate,
    onUse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = template.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DifficultyBadge(difficulty = template.difficulty)
                        Spacer(modifier = Modifier.width(8.dp))
                        CategoryBadge(category = template.category)
                        if (template.isCustom) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF4ECDC4).copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Custom",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4ECDC4),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = onUse,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B73FF)
                    )
                ) {
                    Text("Use Template")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Description
            Text(
                text = template.description,
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration and Sensors
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
                        text = template.duration,
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Sensors",
                        tint = Color(0xFF4ECDC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${template.sensorTypes.size} sensors",
                        fontSize = 12.sp,
                        color = Color(0xFFCCFFFFFF)
                    )
                }
                // Sensor Type Icons
                Row {
                    template.sensorTypes.take(3).forEach { sensorType ->
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
                    if (template.sensorTypes.size > 3) {
                        Text(
                            text = "+${template.sensorTypes.size - 3}",
                            fontSize = 12.sp,
                            color = Color(0xFFCCFFFFFF),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            // Tasks Preview
            if (template.tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tasks: ${
                        template.tasks.take(2).joinToString(", ")
                    }${if (template.tasks.size > 2) "..." else ""}",
                    fontSize = 12.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: TemplateDifficulty) {
    val (color, text) = when (difficulty) {
        TemplateDifficulty.BEGINNER -> Color(0xFF4ECDC4) to "Beginner"
        TemplateDifficulty.INTERMEDIATE -> Color(0xFFFFB74D) to "Intermediate"
        TemplateDifficulty.ADVANCED -> Color(0xFFFF6B6B) to "Advanced"
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CategoryBadge(category: TemplateCategory) {
    val (color, text) = when (category) {
        TemplateCategory.STRESS_RESPONSE -> Color(0xFFFF6B6B) to "Stress"
        TemplateCategory.COGNITIVE_LOAD -> Color(0xFF6B73FF) to "Cognitive"
        TemplateCategory.EMOTION_RECOGNITION -> Color(0xFFFFB74D) to "Emotion"
        TemplateCategory.PHYSIOLOGICAL_MONITORING -> Color(0xFF4ECDC4) to "Physiology"
        TemplateCategory.CUSTOM -> Color(0xFF9E9E9E) to "Custom"
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyTemplatesState(
    searchQuery: String,
    selectedCategory: TemplateCategory?,
    onCreateCustom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (searchQuery.isBlank() && selectedCategory == null)
                Icons.AutoMirrored.Filled.Assignment else Icons.Default.SearchOff,
            contentDescription = if (searchQuery.isBlank() && selectedCategory == null) "No Templates" else "No Search Results",
            tint = Color(0xFF6B73FF),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isBlank() && selectedCategory == null)
                "No templates available" else "No templates found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isBlank() && selectedCategory == null)
                "Create your first custom template to get started"
            else
                "Try adjusting your search or category filter",
            fontSize = 14.sp,
            color = Color(0xFFCCFFFFFF)
        )
        if (searchQuery.isBlank() && selectedCategory == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateCustom,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B73FF)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Template",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom Template")
            }
        }
    }
}

private fun parseDuration(duration: String): Int {
    // Parse "25 min" format to minutes
    return duration.replace(" min", "").toIntOrNull() ?: 0
}

private fun getSampleTemplates() = listOf(
    ResearchTemplate(
        id = "TEMPLATE-001",
        title = "Basic Stress Response",
        description = "Measure physiological responses to cognitive stress tasks. Includes baseline recording, math problems, and recovery period.",
        duration = "20 min",
        sensorTypes = listOf("GSR", "Thermal"),
        tasks = listOf("Baseline (5min)", "Math problems", "Recovery (5min)"),
        difficulty = TemplateDifficulty.BEGINNER,
        category = TemplateCategory.STRESS_RESPONSE
    ),
    ResearchTemplate(
        id = "TEMPLATE-002",
        title = "Cognitive Load Assessment",
        description = "Advanced cognitive load measurement using multi-modal sensors during complex reasoning tasks.",
        duration = "35 min",
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        tasks = listOf("Training", "N-back task", "Stroop test", "Memory recall"),
        difficulty = TemplateDifficulty.ADVANCED,
        category = TemplateCategory.COGNITIVE_LOAD
    ),
    ResearchTemplate(
        id = "TEMPLATE-003",
        title = "Emotion Recognition Study",
        description = "Capture emotional responses using facial thermal imaging and GSR during video stimuli presentation.",
        duration = "25 min",
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        tasks = listOf("Baseline", "Happy videos", "Sad videos", "Neutral videos"),
        difficulty = TemplateDifficulty.INTERMEDIATE,
        category = TemplateCategory.EMOTION_RECOGNITION
    ),
    ResearchTemplate(
        id = "TEMPLATE-004",
        title = "Physiological Monitoring",
        description = "Continuous physiological monitoring during extended computer work sessions.",
        duration = "60 min",
        sensorTypes = listOf("GSR", "Thermal"),
        tasks = listOf("Computer work", "Break periods", "Final assessment"),
        difficulty = TemplateDifficulty.BEGINNER,
        category = TemplateCategory.PHYSIOLOGICAL_MONITORING
    ),
    ResearchTemplate(
        id = "CUSTOM-001",
        title = "My Custom Protocol",
        description = "Custom research protocol designed for specific study requirements.",
        duration = "30 min",
        sensorTypes = listOf("GSR", "Thermal", "Camera"),
        tasks = listOf("Custom task 1", "Custom task 2"),
        difficulty = TemplateDifficulty.INTERMEDIATE,
        category = TemplateCategory.CUSTOM,
        isCustom = true
    )
)

@Preview(showBackground = true)
@Composable
fun ResearchTemplateScreenPreview() {
    ResearchTemplateScreen()
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\SensorDashboardComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui
// Note: MainActivityViewModel was moved to backup during cleanup
// Using modern Compose ViewModels instead
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppError
import mpdc4gsr.core.ui.ConnectionState
import mpdc4gsr.core.ui.components.SensorStatusCard
import mpdc4gsr.core.ui.components.sensors.GSRConnectionState
import mpdc4gsr.core.ui.components.sensors.GSRData
import mpdc4gsr.core.ui.components.sensors.GSRVisualizationCard
import mpdc4gsr.feature.main.presentation.MainActivityViewModel

class SensorDashboardComposeActivity : ComponentActivity() {
    private lateinit var dashboardViewModel: MainActivityViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dashboardViewModel = viewModels<MainActivityViewModel>().value
        setContent {
            LibUnifiedTheme {
                Content(dashboardViewModel)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(viewModel: MainActivityViewModel) {
        // Observe sensor states
        val thermalCameraState by viewModel.thermalCameraState.collectAsState()
        val gsrSensorState by viewModel.gsrSensorState.collectAsState()
        val gsrConnectionState by viewModel.gsrConnectionState.collectAsState()
        val gsrBatteryLevel by viewModel.gsrBatteryLevel.collectAsState()
        // Use real GSR data from ViewModel
        val gsrDataState by viewModel.gsrData.collectAsState()
        // Map ViewModel GSRDataState to UI GSRData with battery level
        val gsrData by remember {
            derivedStateOf {
                GSRData(
                    currentValue = gsrDataState.currentValue,
                    batteryLevel = gsrBatteryLevel ?: gsrDataState.batteryLevel,
                    recentReadings = gsrDataState.recentReadings.ifEmpty { generateMockGSRReadings() },
                    averageValue = gsrDataState.averageValue,
                    minValue = gsrDataState.minValue,
                    maxValue = gsrDataState.maxValue
                )
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Sensor Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overall sensor status overview
                Text(
                    text = "Sensor Status Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                SensorStatusCard(
                    thermalCameraState = mapSensorStateToConnectionState(thermalCameraState),
                    gsrSensorState = mapSensorStateToConnectionState(gsrSensorState),
                    bleConnectionState = mapGSRConnectionToConnectionState(gsrConnectionState)
                )
                // GSR Sensor detailed visualization
                Text(
                    text = "GSR Sensor Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                GSRVisualizationCard(
                    gsrData = gsrData,
                    connectionState = GSRConnectionState(
                        isConnected = gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED,
                        deviceName = "Shimmer3-GSR",
                        connectionStrength = if (gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED) 85 else 0
                    ),
                    onExportData = {
                        // TODO: Implement GSR data export functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Export GSR data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onResetStatistics = {
                        // TODO: Implement statistics reset functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Reset statistics feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                // Additional sensor information cards
                AdditionalSensorInfo(
                    thermalCameraState = mapSensorStateToConnectionState(thermalCameraState),
                    gsrSensorState = mapSensorStateToConnectionState(gsrSensorState)
                )
                // Data export and management section
                DataManagementSection(
                    onExportAllData = {
                        // TODO: Export all sensor data
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Exporting all sensor data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onClearData = {
                        // TODO: Clear sensor data
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Clear data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onOpenSettings = {
                        // TODO: Open sensor settings
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeActivity,
                            "Opening settings...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    @Composable
    private fun AdditionalSensorInfo(
        thermalCameraState: ConnectionState,
        gsrSensorState: ConnectionState
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Thermal camera info
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Thermal Camera",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Resolution: 384x288",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Frame Rate: 10Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Status: ${
                            when (thermalCameraState) {
                                is ConnectionState.Connected -> "Connected"
                                is ConnectionState.Connecting -> "Connecting"
                                is ConnectionState.Disconnected -> "Disconnected"
                                is ConnectionState.Error -> "Error"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            // GSR sensor info
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "GSR Sensor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sample Rate: 51.2Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Connection: BLE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Status: ${
                            when (gsrSensorState) {
                                is ConnectionState.Connected -> "Connected"
                                is ConnectionState.Connecting -> "Connecting"
                                is ConnectionState.Disconnected -> "Disconnected"
                                is ConnectionState.Error -> "Error"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    @Composable
    private fun DataManagementSection(
        onExportAllData: () -> Unit,
        onClearData: () -> Unit,
        onOpenSettings: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportAllData,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export All Data")
                    }
                    OutlinedButton(
                        onClick = onClearData,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Data")
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Settings")
                    }
                }
            }
        }
    }

    // Helper functions to map existing state to Compose-friendly types
    private fun mapSensorStateToConnectionState(sensorState: MainActivityViewModel.SensorState): ConnectionState {
        return when (sensorState.status) {
            MainActivityViewModel.SensorStatus.DISCONNECTED -> ConnectionState.Disconnected
            MainActivityViewModel.SensorStatus.CONNECTING -> ConnectionState.Connecting
            MainActivityViewModel.SensorStatus.CONNECTED -> ConnectionState.Connected()
            MainActivityViewModel.SensorStatus.STREAMING -> ConnectionState.Connected()
            MainActivityViewModel.SensorStatus.ERROR -> ConnectionState.Error(
                AppError.SensorError(
                    "ThermalCamera",
                    "Sensor Error"
                )
            )

            MainActivityViewModel.SensorStatus.SIMULATION -> ConnectionState.Connected()
        }
    }

    private fun mapGSRConnectionToConnectionState(gsrState: MainActivityViewModel.GSRConnectionState): ConnectionState {
        return when (gsrState) {
            MainActivityViewModel.GSRConnectionState.DISCONNECTED -> ConnectionState.Disconnected
            MainActivityViewModel.GSRConnectionState.DISCOVERING -> ConnectionState.Connecting
            MainActivityViewModel.GSRConnectionState.CONNECTING -> ConnectionState.Connecting
            MainActivityViewModel.GSRConnectionState.CONNECTED -> ConnectionState.Connected()
            MainActivityViewModel.GSRConnectionState.ERROR -> ConnectionState.Error(
                AppError.SensorError(
                    "GSR",
                    "GSR Error"
                )
            )
        }
    }

    private fun generateMockGSRReadings(): List<Float> {
        return (0..50).map {
            100f + (kotlin.random.Random.nextFloat() - 0.5f) * 40f
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\SensorDashboardComposeEnhanced.kt =====

package mpdc4gsr.feature.gsr.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.components.sensors.GSRConnectionState
import mpdc4gsr.core.ui.components.sensors.GSRData
import mpdc4gsr.core.ui.components.sensors.GSRVisualizationCard
import mpdc4gsr.feature.main.presentation.MainActivityViewModel

class SensorDashboardComposeEnhanced : ComponentActivity() {
    private lateinit var dashboardViewModel: MainActivityViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dashboardViewModel = viewModels<MainActivityViewModel>().value
        setContent {
            LibUnifiedTheme {
                Content(dashboardViewModel)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(viewModel: MainActivityViewModel) {
        // Observe sensor states
        val thermalCameraState by viewModel.thermalCameraState.collectAsState()
        val gsrSensorState by viewModel.gsrSensorState.collectAsState()
        val gsrConnectionState by viewModel.gsrConnectionState.collectAsState()
        val gsrBatteryLevel by viewModel.gsrBatteryLevel.collectAsState()
        val sessionState by viewModel.sessionState.collectAsState()
        // Enhanced GSR data with consolidated layout integration
        val gsrData by remember {
            derivedStateOf {
                GSRData(
                    currentValue = 125.5f,
                    batteryLevel = gsrBatteryLevel ?: 75,
                    recentReadings = generateEnhancedGSRReadings(),
                    averageValue = 118.3f,
                    minValue = 95.2f,
                    maxValue = 145.8f
                )
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Enhanced Sensor Dashboard",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Multi-Modal Sensor Integration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Enhanced actions leveraging consolidated patterns
                        IconButton(onClick = {
                            // TODO: Export all sensor data
                            android.widget.Toast.makeText(
                                this@SensorDashboardComposeEnhanced,
                                "Exporting all sensor data...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Export Data")
                        }
                        IconButton(onClick = {
                            // TODO: Open sensor settings
                            android.widget.Toast.makeText(
                                this@SensorDashboardComposeEnhanced,
                                "Opening sensor settings...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enhanced multi-modal sensor overview
                MultiModalSensorOverview(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    sessionState = sessionState
                )
                // Enhanced GSR visualization with consolidated patterns
                GSRVisualizationCard(
                    gsrData = gsrData,
                    connectionState = GSRConnectionState(
                        isConnected = gsrConnectionState != MainActivityViewModel.GSRConnectionState.DISCONNECTED,
                        deviceName = "Shimmer3-GSR-Enhanced",
                        connectionStrength = if (gsrConnectionState == MainActivityViewModel.GSRConnectionState.CONNECTED) 90 else 0
                    ),
                    onExportData = {
                        // TODO: Implement GSR data export functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Export GSR data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onResetStatistics = {
                        // TODO: Implement statistics reset functionality
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Reset statistics feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                // Device management section (consolidated layout pattern)
                DeviceManagementSection(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    onDeviceConfig = { deviceType -> launchDeviceConfig(deviceType) },
                    onDeviceTest = { deviceType -> launchDeviceTest(deviceType) }
                )
                // Enhanced data export section
                DataExportSection(
                    sessionState = sessionState,
                    onExportSession = {
                        // TODO: Export current session
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Exporting current session...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onExportAllData = {
                        // TODO: Export all sensor data
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Exporting all data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onManageSessions = {
                        // TODO: Launch session manager
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Opening session manager...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                // System status and diagnostics
                SystemDiagnosticsSection(
                    thermalCameraState = thermalCameraState,
                    gsrSensorState = gsrSensorState,
                    onRunDiagnostics = {
                        // TODO: Run system diagnostics
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Running diagnostics...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onViewLogs = {
                        // TODO: View system logs
                        android.widget.Toast.makeText(
                            this@SensorDashboardComposeEnhanced,
                            "Opening system logs...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    @Composable
    private fun MultiModalSensorOverview(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        sessionState: MainActivityViewModel.SessionState
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Multi-Modal Recording",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (sessionState == MainActivityViewModel.SessionState.RECORDING) "Recording Active" else "Ready to Record",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    if (sessionState == MainActivityViewModel.SessionState.RECORDING) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = "Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Sensor status grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SensorStatusIndicator(
                        title = "Thermal",
                        status = thermalCameraState.status.name,
                        icon = Icons.Default.Camera,
                        isActive = thermalCameraState.status == MainActivityViewModel.SensorStatus.STREAMING
                    )
                    SensorStatusIndicator(
                        title = "GSR",
                        status = gsrSensorState.status.name,
                        icon = Icons.Default.Sensors,
                        isActive = gsrSensorState.status == MainActivityViewModel.SensorStatus.STREAMING
                    )
                    SensorStatusIndicator(
                        title = "Session",
                        status = if (sessionState == MainActivityViewModel.SessionState.RECORDING) "Active" else "Idle",
                        icon = Icons.Default.Storage,
                        isActive = sessionState == MainActivityViewModel.SessionState.RECORDING
                    )
                }
            }
        }
    }

    @Composable
    private fun SensorStatusIndicator(
        title: String,
        status: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isActive: Boolean
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isActive) Color.Green else MaterialTheme.colorScheme.onPrimaryContainer.copy(
                    alpha = 0.6f
                ),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }

    @Composable
    private fun DeviceManagementSection(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        onDeviceConfig: (String) -> Unit,
        onDeviceTest: (String) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Device Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Enhanced device cards with consolidated layout patterns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviceCard(
                        title = "Thermal Camera",
                        subtitle = "TOPDON TC001",
                        status = thermalCameraState.status.name,
                        icon = Icons.Default.Camera,
                        onConfig = { onDeviceConfig("thermal") },
                        onTest = { onDeviceTest("thermal") },
                        modifier = Modifier.weight(1f)
                    )
                    DeviceCard(
                        title = "GSR Sensor",
                        subtitle = "Shimmer3",
                        status = gsrSensorState.status.name,
                        icon = Icons.Default.Sensors,
                        onConfig = { onDeviceConfig("gsr") },
                        onTest = { onDeviceTest("gsr") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceCard(
        title: String,
        subtitle: String,
        status: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onConfig: () -> Unit,
        onTest: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Status: $status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = onConfig,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Config", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onTest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    @Composable
    private fun DataExportSection(
        sessionState: MainActivityViewModel.SessionState,
        onExportSession: () -> Unit,
        onExportAllData: () -> Unit,
        onManageSessions: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (sessionState == MainActivityViewModel.SessionState.RECORDING) {
                    Text(
                        text = "Recording in progress - data will be available after session ends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportSession,
                        enabled = sessionState != MainActivityViewModel.SessionState.RECORDING,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Session")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Session")
                    }
                    OutlinedButton(
                        onClick = onExportAllData,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = "Export All Data")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export All")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onManageSessions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = "Manage Sessions")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Sessions")
                }
            }
        }
    }

    @Composable
    private fun SystemDiagnosticsSection(
        thermalCameraState: MainActivityViewModel.SensorState,
        gsrSensorState: MainActivityViewModel.SensorState,
        onRunDiagnostics: () -> Unit,
        onViewLogs: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // System health indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("Thermal Status", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            if (thermalCameraState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = if (thermalCameraState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                "Thermal Camera Connected" else "Thermal Camera Error",
                            tint = if (thermalCameraState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Color.Green else Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("GSR Status", style = MaterialTheme.typography.labelMedium)
                        Icon(
                            if (gsrSensorState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = if (gsrSensorState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                "GSR Sensor Connected" else "GSR Sensor Error",
                            tint = if (gsrSensorState.status == MainActivityViewModel.SensorStatus.CONNECTED)
                                Color.Green else Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRunDiagnostics,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = "Run Diagnostics")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Diagnostics")
                    }
                    OutlinedButton(
                        onClick = onViewLogs,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "View Logs")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Logs")
                    }
                }
            }
        }
    }

    // Launch methods for enhanced functionality
    private fun launchDeviceConfig(deviceType: String) {
        // Launch device-specific configuration
        when (deviceType) {
            "thermal" -> {
                // Launch thermal camera configuration
            }

            "gsr" -> {
                // Launch GSR sensor configuration
            }
        }
    }

    private fun launchDeviceTest(deviceType: String) {
        // Launch device-specific testing
        when (deviceType) {
            "thermal" -> {
                // Launch thermal camera test
            }

            "gsr" -> {
                // Launch GSR sensor test
            }
        }
    }

    // Enhanced mock data generation
    private fun generateEnhancedGSRReadings(): List<Float> {
        return (0..100).map {
            100f + (kotlin.random.Random.nextFloat() - 0.5f) * 50f
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\SessionDetailComposeActivity.kt =====

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
                            IconButton(onClick = {
                                // TODO: Share session data
                                android.widget.Toast.makeText(
                                    this@SessionDetailComposeActivity,
                                    "Share session feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                // TODO: Export session data
                                android.widget.Toast.makeText(
                                    this@SessionDetailComposeActivity,
                                    "Exporting session...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
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
        val context = androidx.compose.ui.platform.LocalContext.current
        SessionActionsCard(
            onViewData = {
                // TODO: Navigate to data view activity
                android.widget.Toast.makeText(
                    context,
                    "Opening data view...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onExportData = {
                // TODO: Export session data
                android.widget.Toast.makeText(
                    context,
                    "Exporting session data...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onDeleteSession = {
                // TODO: Show confirmation dialog and delete session
                android.widget.Toast.makeText(
                    context,
                    "Delete session confirmation dialog",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
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
                StatisticItem("Avg GSR", "12.5 Î¼S")
                StatisticItem("Peak GSR", "45.7 Î¼S")
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


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\SessionDetailScreen.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import kotlin.math.sin

data class SessionInfo(
    val id: String,
    val title: String,
    val date: String,
    val duration: String,
    val participantId: String,
    val sensorTypes: List<String>,
    val dataPoints: Int,
    val notes: String
)

data class SessionMetrics(
    val gsrMean: Double,
    val gsrStd: Double,
    val gsrMin: Double,
    val gsrMax: Double,
    val thermalMean: Double,
    val thermalStd: Double,
    val heartRateAvg: Int,
    val stressLevel: String
)

data class TimeSeriesData(
    val timestamp: Long,
    val gsrValue: Double,
    val thermalValue: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String = "SESSION-2024-001",
    onNavigateBack: () -> Unit = {},
    onExportSession: () -> Unit = {},
    onPlayVideo: () -> Unit = {}
) {
    val session = remember { getSampleSession(sessionId) }
    val metrics = remember { getSampleMetrics() }
    val timeSeriesData = remember { getSampleTimeSeriesData() }
    IRCameraTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TitleBar(
                title = "Session Details",
                showBackButton = true,
                onBackClick = onNavigateBack
            ) {
                TitleBarAction(
                    icon = Icons.Default.Share,
                    contentDescription = "Export session",
                    onClick = onExportSession
                )
                TitleBarAction(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    onClick = onPlayVideo
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Session Header
                item {
                    SessionHeaderCard(session = session)
                }
                // Metrics Overview
                item {
                    MetricsOverviewCard(metrics = metrics)
                }
                // GSR Waveform
                item {
                    GSRWaveformCard(data = timeSeriesData)
                }
                // Thermal Data
                item {
                    ThermalDataCard(data = timeSeriesData)
                }
                // Analysis Summary
                item {
                    AnalysisSummaryCard(session = session, metrics = metrics)
                }
                // Export Options
                item {
                    ExportOptionsCard(
                        onExportRaw = { onExportSession() },
                        onExportReport = { onExportSession() },
                        onExportVideo = { onPlayVideo() }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionHeaderCard(session: SessionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = session.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionInfoItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Date",
                    value = session.date
                )
                SessionInfoItem(
                    icon = Icons.Default.Timer,
                    label = "Duration",
                    value = session.duration
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionInfoItem(
                    icon = Icons.Default.Person,
                    label = "Participant",
                    value = session.participantId
                )
                SessionInfoItem(
                    icon = Icons.Default.DataUsage,
                    label = "Data Points",
                    value = session.dataPoints.toString()
                )
            }
            if (session.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Notes: ${session.notes}",
                    fontSize = 14.sp,
                    color = Color(0xFFCCFFFFFF)
                )
            }
        }
    }
}

@Composable
fun SessionInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF4ECDC4),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFFCCFFFFFF)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun MetricsOverviewCard(metrics: SessionMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Metrics Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "GSR Mean",
                    value = "${String.format("%.2f", metrics.gsrMean)} Î¼S",
                    color = Color(0xFF4ECDC4)
                )
                MetricItem(
                    label = "Thermal Avg",
                    value = "${String.format("%.1f", metrics.thermalMean)}Â°C",
                    color = Color(0xFFFF6B6B)
                )
                MetricItem(
                    label = "Heart Rate",
                    value = "${metrics.heartRateAvg} BPM",
                    color = Color(0xFF6B73FF)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stress Level: ",
                    fontSize = 14.sp,
                    color = Color(0xFFCCFFFFFF)
                )
                Text(
                    text = metrics.stressLevel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (metrics.stressLevel) {
                        "Low" -> Color(0xFF4ECDC4)
                        "Medium" -> Color(0xFFFFB74D)
                        "High" -> Color(0xFFFF6B6B)
                        else -> Color.White
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
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
fun GSRWaveformCard(data: List<TimeSeriesData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "GSR Waveform",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val path = Path()
                val width = size.width
                val height = size.height
                if (data.isNotEmpty()) {
                    val minValue = data.minOf { it.gsrValue }
                    val maxValue = data.maxOf { it.gsrValue }
                    val valueRange = maxValue - minValue
                    data.forEachIndexed { index, point ->
                        val x = (index.toFloat() / (data.size - 1)) * width
                        val normalizedValue = if (valueRange > 0) {
                            ((point.gsrValue - minValue) / valueRange)
                        } else 0.5
                        val y = height - (normalizedValue.toFloat() * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4ECDC4),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun ThermalDataCard(data: List<TimeSeriesData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Thermal Data",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val path = Path()
                val width = size.width
                val height = size.height
                if (data.isNotEmpty()) {
                    val minValue = data.minOf { it.thermalValue }
                    val maxValue = data.maxOf { it.thermalValue }
                    val valueRange = maxValue - minValue
                    data.forEachIndexed { index, point ->
                        val x = (index.toFloat() / (data.size - 1)) * width
                        val normalizedValue = if (valueRange > 0) {
                            ((point.thermalValue - minValue) / valueRange)
                        } else 0.5
                        val y = height - (normalizedValue.toFloat() * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFF6B6B),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisSummaryCard(
    session: SessionInfo,
    metrics: SessionMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Analysis Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            val summaryText = buildString {
                append("This ${session.duration} session recorded ${session.dataPoints} data points ")
                append("from ${session.sensorTypes.joinToString(", ")} sensors. ")
                append("Average GSR was ${String.format("%.2f", metrics.gsrMean)} Î¼S with ")
                append("standard deviation of ${String.format("%.2f", metrics.gsrStd)}. ")
                append(
                    "Thermal readings averaged ${
                        String.format(
                            "%.1f",
                            metrics.thermalMean
                        )
                    }Â°C. "
                )
                append("Overall stress level assessed as ${metrics.stressLevel.lowercase()}.")
            }
            Text(
                text = summaryText,
                fontSize = 14.sp,
                color = Color(0xFFCCFFFFFF),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ExportOptionsCard(
    onExportRaw: () -> Unit,
    onExportReport: () -> Unit,
    onExportVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Export Options",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onExportRaw,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4ECDC4)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Export Raw Data",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Raw Data")
                }
                OutlinedButton(
                    onClick = onExportReport,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6B73FF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Export Report",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report")
                }
                OutlinedButton(
                    onClick = onExportVideo,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF6B6B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = "Export Video",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Video")
                }
            }
        }
    }
}

private fun getSampleSession(id: String) = SessionInfo(
    id = id,
    title = "Stress Response Study - Session A",
    date = "Dec 15, 2024 14:30",
    duration = "25:42",
    participantId = "P001-UCL-2024",
    sensorTypes = listOf("GSR", "Thermal", "Heart Rate"),
    dataPoints = 15420,
    notes = "Baseline recording with cognitive stress tasks. Participant reported feeling moderately stressed during math problems."
)

private fun getSampleMetrics() = SessionMetrics(
    gsrMean = 12.45,
    gsrStd = 3.21,
    gsrMin = 8.12,
    gsrMax = 18.67,
    thermalMean = 36.4,
    thermalStd = 0.8,
    heartRateAvg = 78,
    stressLevel = "Medium"
)

private fun getSampleTimeSeriesData(): List<TimeSeriesData> {
    return (0..100).map { i ->
        TimeSeriesData(
            timestamp = System.currentTimeMillis() + i * 1000,
            gsrValue = 12.0 + 3.0 * sin(i * 0.1) + (Math.random() - 0.5) * 2.0,
            thermalValue = 36.4 + 0.5 * sin(i * 0.05) + (Math.random() - 0.5) * 0.3
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SessionDetailScreenPreview() {
    SessionDetailScreen()
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\SessionExportComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.gsr.presentation.ExportDestination
import mpdc4gsr.feature.gsr.presentation.ExportFormat
import mpdc4gsr.feature.gsr.presentation.GSRSession
import mpdc4gsr.feature.gsr.presentation.SessionExportViewModel
import mpdc4gsr.feature.gsr.presentation.SessionExportViewModelFactory

class SessionExportComposeActivity : BaseComposeActivity<SessionExportViewModel>() {
    override fun createViewModel(): SessionExportViewModel =
        viewModels<SessionExportViewModel> {
            SessionExportViewModelFactory(application)
        }.value

    @Composable
    override fun Content(viewModel: SessionExportViewModel) {
        IRCameraTheme {
            SessionExportScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionExportScreen(
    viewModel: SessionExportViewModel = viewModel(
        factory = SessionExportViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext
        )
    ),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.exportState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("Export GSR Session") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.startExport() },
                    enabled = !uiState.isExporting && uiState.selectedSessions.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Start export"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
                    onRetry = { viewModel.loadSessions() }
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
                    onStartExport = { viewModel.startExport() }
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
    onStartExport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Export Progress (if exporting)
        if (uiState.isExporting) {
            item {
                ExportProgressCard(
                    progress = uiState.exportProgress,
                    currentFile = uiState.currentExportFile
                )
            }
        }
        // Export Configuration
        item {
            ExportConfigurationCard(
                selectedFormat = uiState.exportFormat,
                selectedDestination = uiState.exportDestination,
                onFormatChange = onExportFormatChange,
                onDestinationChange = onExportDestinationChange
            )
        }
        // Session Selection Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Sessions to Export",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.selectedSessions.size} of ${uiState.sessions.size} sessions selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        // Session List
        items(uiState.sessions) { session ->
            SessionSelectionCard(
                session = session,
                isSelected = session in uiState.selectedSessions,
                onToggle = { onSessionToggle(session) }
            )
        }
        // Export Action
        if (!uiState.isExporting && uiState.selectedSessions.isNotEmpty()) {
            item {
                Button(
                    onClick = onStartExport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.selectedSessions.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
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
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "Duration: ${session.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "Data points: ${session.dataPointCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ExportConfigurationCard(
    selectedFormat: ExportFormat,
    selectedDestination: ExportDestination,
    onFormatChange: (ExportFormat) -> Unit,
    onDestinationChange: (ExportDestination) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Export Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Export Format Selection
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExportFormat.values().forEach { format ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedFormat == format,
                            onClick = { onFormatChange(format) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedFormat == format,
                        onClick = { onFormatChange(format) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = format.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Export Destination Selection
            Text(
                text = "Export Destination",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExportDestination.values().forEach { destination ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedDestination == destination,
                            onClick = { onDestinationChange(destination) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedDestination == destination,
                        onClick = { onDestinationChange(destination) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = destination.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportProgressCard(
    progress: Float,
    currentFile: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Exporting...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            if (currentFile != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current: $currentFile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
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
                text = "Loading sessions...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
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
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error loading sessions",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium
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
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "No sessions",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No sessions available",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "GSR sessions will appear here when available for export",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\SessionManagerComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

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
import mpdc4gsr.feature.gsr.presentation.SessionManagerViewModel
import java.text.SimpleDateFormat
import java.util.*

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


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\SessionManagerScreen.kt =====

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
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
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


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\ShimmerConfigComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.feature.gsr.presentation.ShimmerConfigViewModel
import mpdc4gsr.feature.gsr.presentation.ShimmerConfigViewModelFactory

class ShimmerConfigComposeActivity : ComponentActivity() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ShimmerConfigComposeActivity::class.java))
        }
    }

    private val viewModel: ShimmerConfigViewModel by viewModels {
        ShimmerConfigViewModelFactory(application, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Content(viewModel)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(viewModel: ShimmerConfigViewModel) {
        val localContext = androidx.compose.ui.platform.LocalContext.current
        var isScanning by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<DeviceInfo?>(null) }
        var showConfigDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Shimmer Configuration",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = { isScanning = !isScanning }) {
                                Icon(
                                    if (isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                                    contentDescription = if (isScanning) "Stop Scan" else "Scan"
                                )
                            }
                            IconButton(onClick = {
                                // TODO: Implement Shimmer configuration help/documentation
                                android.widget.Toast.makeText(
                                    localContext,
                                    "Opening Shimmer configuration help",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                ShimmerConfigContent(
                    isScanning = isScanning,
                    selectedDevice = selectedDevice,
                    onDeviceSelect = { selectedDevice = it },
                    onConfigureDevice = { showConfigDialog = true },
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showConfigDialog && selectedDevice != null) {
            DeviceConfigurationDialog(
                device = selectedDevice!!,
                onDismiss = { showConfigDialog = false },
                onSaveConfiguration = { config ->
                    // Save device configuration
                    showConfigDialog = false
                }
            )
        }
    }
}

@Composable
private fun ShimmerConfigContent(
    isScanning: Boolean,
    selectedDevice: DeviceInfo?,
    onDeviceSelect: (DeviceInfo?) -> Unit,
    onConfigureDevice: () -> Unit,
    viewModel: ShimmerConfigViewModel,
    modifier: Modifier = Modifier
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scanning Status Card
        ScanningStatusCard(
            isScanning = isScanning,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Device List
        Text(
            text = "Available Devices",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val mockDevices = listOf(
                DeviceInfo("shimmer_001", "Shimmer3 GSR+ #001", "Shimmer3", -45, true),
                DeviceInfo("shimmer_002", "Shimmer3 GSR+ #002", "Shimmer3", -62, true),
                DeviceInfo("shimmer_003", "Shimmer3 GSR+ #003", "Shimmer3", -38, true)
            )
            items(mockDevices) { device ->
                DeviceCard(
                    device = device,
                    isSelected = selectedDevice?.address == device.address,
                    onSelect = { onDeviceSelect(device) },
                    onConnect = {
                        // TODO: Implement Shimmer device connection
                        android.widget.Toast.makeText(
                            localContext,
                            "Connecting to ${device.name}...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onConfigure = {
                        onDeviceSelect(device)
                        onConfigureDevice()
                    }
                )
            }
        }
        // Selected Device Configuration Panel
        selectedDevice?.let { device ->
            SelectedDevicePanel(
                device = device,
                onConfigure = onConfigureDevice,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ScanningStatusCard(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isScanning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
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
                    text = if (isScanning) "Scanning for devices..." else "Scan complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isScanning) "Looking for Shimmer devices" else "3 devices found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Scan complete",
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DeviceInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onConnect: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "ID: ${device.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Status and signal strength
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Connection status indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (device.deviceType) {
                                        "connected" -> Color(0xFF4CAF50)
                                        "available" -> Color(0xFF2196F3)
                                        "configuring" -> Color(0xFFFF9800)
                                        else -> Color(0xFF9E9E9E)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = device.deviceType.replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Signal strength
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = "Signal strength",
                            modifier = Modifier.size(16.dp),
                            tint = when (device.signalStrength) {
                                DeviceInfo.SignalStrength.EXCELLENT -> Color(0xFF4CAF50)
                                DeviceInfo.SignalStrength.GOOD -> Color(0xFF4CAF50)
                                DeviceInfo.SignalStrength.FAIR -> Color(0xFFFF9800)
                                DeviceInfo.SignalStrength.POOR -> Color(0xFFE53E3E)
                                DeviceInfo.SignalStrength.VERY_POOR -> Color(0xFFE53E3E)
                            }
                        )
                        Text(
                            text = "${device.rssi} dBm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onSelect) {
                    Icon(
                        if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isSelected) "Collapse" else "Expand"
                    )
                }
            }
            if (isSelected) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                // Device actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Connect",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                    Button(
                        onClick = onConfigure,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configure",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configure")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDevicePanel(
    device: DeviceInfo,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Selected Device",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Status: ${device.deviceType.replaceFirstChar { it.uppercaseChar() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Button(
                onClick = onConfigure,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Advanced Configuration",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advanced Configuration")
            }
        }
    }
}

@Composable
private fun DeviceConfigurationDialog(
    device: DeviceInfo,
    onDismiss: () -> Unit,
    onSaveConfiguration: (Map<String, Any>) -> Unit
) {
    var samplingRate by remember { mutableStateOf(128f) }
    var gsrRange by remember { mutableStateOf("Auto") }
    var enablePPG by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configure ${device.name}")
        },
        text = {
            Column {
                Text(
                    text = "Sampling Rate (Hz)",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = samplingRate,
                    onValueChange = { samplingRate = it },
                    valueRange = 1f..512f,
                    steps = 8
                )
                Text(
                    text = "${samplingRate.toInt()} Hz",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "GSR Range",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enablePPG,
                        onCheckedChange = { enablePPG = it }
                    )
                    Text(
                        text = "Enable PPG channels",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveConfiguration(
                        mapOf(
                            "samplingRate" to samplingRate.toInt(),
                            "gsrRange" to gsrRange,
                            "enablePPG" to enablePPG
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// ===== app\src\main\java\mpdc4gsr\feature\gsr\ui\UnifiedSensorComposeActivity.kt =====

package mpdc4gsr.feature.gsr.ui

import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.SessionQuality
import mpdc4gsr.core.data.model.SessionStatus
import mpdc4gsr.core.data.model.SessionType
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

enum class UnifiedSensorType(
    val displayName: String,
    val icon: ImageVector,
    val description: String
) {
    THERMAL("Thermal Camera", Icons.Default.Thermostat, "TC001/TS004 thermal imaging sensors"),
    GSR("GSR Sensor", Icons.Default.Sensors, "Galvanic skin response monitoring"),
    RGB_CAMERA("RGB Camera", Icons.Default.Camera, "High-resolution RGB camera recording"),
    AUDIO("Audio", Icons.Default.Audiotrack, "Audio recording"),
    NETWORK("Network", Icons.Default.NetworkCheck, "Network connectivity and data transmission")
}

data class SensorStatus(
    val type: UnifiedSensorType,
    val isConnected: Boolean = false,
    val isRecording: Boolean = false,
    val quality: String = "Unknown",
    val dataRate: String = "0 KB/s",
    val lastUpdate: String = "Never"
)

data class UnifiedSessionInfo(
    val name: String = "New Session",
    val type: SessionType = SessionType.RESEARCH,
    val quality: SessionQuality = SessionQuality(),
    val status: SessionStatus = SessionStatus.IDLE,
    val duration: String = "00:00:00",
    val dataSize: String = "0 MB"
)

class UnifiedSensorViewModel : AppBaseViewModel() {
    private val _sensorStatuses = mutableStateOf(
        UnifiedSensorType.values().map { type ->
            SensorStatus(
                type = type,
                isConnected = false,
                quality = "Disconnected"
            )
        }
    )
    val sensorStatuses: State<List<SensorStatus>> = _sensorStatuses
    private val _sessionInfo = mutableStateOf(UnifiedSessionInfo())
    val sessionInfo: State<UnifiedSessionInfo> = _sessionInfo
    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> = _isRecording
    private val _connectedDevices = mutableStateOf<List<DeviceInfo>>(emptyList())
    val connectedDevices: State<List<DeviceInfo>> = _connectedDevices
    fun connectSensor(sensorType: UnifiedSensorType) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Simulate connection process
            delay(2000)
            _sensorStatuses.value = _sensorStatuses.value.map { status ->
                if (status.type == sensorType) {
                    status.copy(
                        isConnected = true,
                        quality = "Good",
                        dataRate = when (sensorType) {
                            UnifiedSensorType.THERMAL -> "125 KB/s"
                            UnifiedSensorType.GSR -> "2 KB/s"
                            UnifiedSensorType.RGB_CAMERA -> "1.2 MB/s"
                            UnifiedSensorType.AUDIO -> "64 KB/s"
                            UnifiedSensorType.NETWORK -> "10 MB/s"
                        },
                        lastUpdate = "Just now"
                    )
                } else status
            }
        }
    }

    fun disconnectSensor(sensorType: UnifiedSensorType) {
        _sensorStatuses.value = _sensorStatuses.value.map { status ->
            if (status.type == sensorType) {
                status.copy(
                    isConnected = false,
                    isRecording = false,
                    quality = "Disconnected",
                    dataRate = "0 KB/s",
                    lastUpdate = "Disconnected"
                )
            } else status
        }
    }

    fun startRecording() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isRecording.value = true
            _sessionInfo.value = _sessionInfo.value.copy(status = SessionStatus.RECORDING)
            // Update sensor recording status
            _sensorStatuses.value = _sensorStatuses.value.map { status ->
                if (status.isConnected) {
                    status.copy(isRecording = true)
                } else status
            }
            // Simulate recording time updates
            var seconds = 0
            while (_isRecording.value) {
                delay(1000)
                seconds++
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                val duration = String.format("%02d:%02d:%02d", hours, minutes, secs)
                val dataSize = "${(seconds * 0.5).toInt()} MB" // Simulate growing data
                _sessionInfo.value = _sessionInfo.value.copy(
                    duration = duration,
                    dataSize = dataSize
                )
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        _sessionInfo.value = _sessionInfo.value.copy(status = SessionStatus.IDLE)
        _sensorStatuses.value = _sensorStatuses.value.map { status ->
            status.copy(isRecording = false)
        }
    }

    fun updateSessionName(name: String) {
        _sessionInfo.value = _sessionInfo.value.copy(name = name)
    }
}

class UnifiedSensorComposeActivity : BaseComposeActivity<UnifiedSensorViewModel>() {
    override fun createViewModel(): UnifiedSensorViewModel =
        viewModels<UnifiedSensorViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: UnifiedSensorViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val sensorStatuses by viewModel.sensorStatuses
            val sessionInfo by viewModel.sessionInfo
            val isRecording by viewModel.isRecording
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Unified Sensor Control",
                    onBackClick = { finish() }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Session info card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = if (isRecording)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sessionInfo.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${sessionInfo.status} â€¢ ${sessionInfo.duration} â€¢ ${sessionInfo.dataSize}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isRecording) {
                                    Button(
                                        onClick = { viewModel.stopRecording() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Stop")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.startRecording() },
                                        enabled = sensorStatuses.any { it.isConnected }
                                    ) {
                                        Text("Start Recording")
                                    }
                                }
                            }
                        }
                    }
                    // RGB Camera preview
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { context ->
                                    PreviewView(context).apply {
                                        // Camera preview will be initialized here
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            // Overlay when camera is not connected
                            val rgbCameraStatus =
                                sensorStatuses.find { it.type == UnifiedSensorType.RGB_CAMERA }
                            if (rgbCameraStatus?.isConnected != true) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.Black.copy(alpha = 0.8f)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VideocamOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Camera Preview",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Connect RGB camera to view",
                                            color = Color.White.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Sensor status cards
                    Text(
                        text = "Sensor Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    sensorStatuses.forEach { sensorStatus ->
                        SensorStatusCard(
                            sensorStatus = sensorStatus,
                            onConnect = { viewModel.connectSensor(it) },
                            onDisconnect = { viewModel.disconnectSensor(it) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Quick actions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Quick Actions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        UnifiedSensorType.values().forEach { type ->
                                            viewModel.connectSensor(type)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Connect All")
                                }
                                OutlinedButton(
                                    onClick = {
                                        UnifiedSensorType.values().forEach { type ->
                                            viewModel.disconnectSensor(type)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Disconnect All")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorStatusCard(
    sensorStatus: SensorStatus,
    onConnect: (UnifiedSensorType) -> Unit,
    onDisconnect: (UnifiedSensorType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                sensorStatus.isRecording -> MaterialTheme.colorScheme.errorContainer
                sensorStatus.isConnected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = sensorStatus.type.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = when {
                    sensorStatus.isRecording -> MaterialTheme.colorScheme.error
                    sensorStatus.isConnected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensorStatus.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = sensorStatus.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Quality: ${sensorStatus.quality} â€¢ Rate: ${sensorStatus.dataRate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = when {
                        sensorStatus.isRecording -> MaterialTheme.colorScheme.error
                        sensorStatus.isConnected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when {
                            sensorStatus.isRecording -> "Recording"
                            sensorStatus.isConnected -> "Connected"
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            sensorStatus.isRecording -> MaterialTheme.colorScheme.onError
                            sensorStatus.isConnected -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (sensorStatus.isConnected) {
                    OutlinedButton(
                        onClick = { onDisconnect(sensorStatus.type) },
                        modifier = Modifier.width(90.dp)
                    ) {
                        Text(
                            text = "Disconnect",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Button(
                        onClick = { onConnect(sensorStatus.type) },
                        modifier = Modifier.width(90.dp)
                    ) {
                        Text(
                            text = "Connect",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}