package com.mpdc4gsr.module.thermalunified.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.db.AppDatabase
import com.mpdc4gsr.libunified.app.db.entity.HouseReport
import com.mpdc4gsr.libunified.app.tools.TimeTools
import com.mpdc4gsr.module.thermalunified.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Compose implementation of ReportPreviewActivity
 * Modern thermal report preview with Material 3 design
 */
class ReportPreviewComposeActivity : ComponentActivity() {

    private var reportId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reportId = intent.getLongExtra(ExtraKeyConfig.reportId, 0L)

        setContent {
            LibUnifiedTheme {
                ReportPreviewScreen(
                    reportId = reportId,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewScreen(
    reportId: Long,
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current as ComponentActivity
    var reportData by remember { mutableStateOf<HouseReportPreviewData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load report data
    LaunchedEffect(reportId) {
        try {
            val report = withContext(Dispatchers.IO) {
                AppDatabase.getInstance().houseReportDao().queryById(reportId)
            }
            report?.let {
                val previewData = convertToPreviewData(it)
                reportData = previewData
                isLoading = false
            } ?: run {
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Report Preview",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share report */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { /* Print report */ }) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                reportData != null -> {
                    ReportPreviewContent(
                        reportData = reportData!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    EmptyStateComponent(
                        title = "Report Not Found",
                        description = "The requested report could not be loaded",
                        icon = Icons.Default.ErrorOutline,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportPreviewContent(
    reportData: HouseReportPreviewData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report header
        item {
            ReportHeaderCard(reportData = reportData)
        }

        // Report sections
        items(reportData.sections) { section ->
            ReportSectionCard(section = section)
        }
    }
}

@Composable
private fun ReportHeaderCard(
    reportData: HouseReportPreviewData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // House photo
            if (reportData.housePhoto.isNotEmpty()) {
                AsyncImage(
                    model = reportData.housePhoto,
                    contentDescription = "House Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Report title
            Text(
                text = reportData.houseName.ifEmpty { "Thermal Inspection Report" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Report details
            ReportInfoRow("Address", reportData.houseAddress)
            ReportInfoRow("Detection Time", reportData.detectTime)
            ReportInfoRow("Inspector", reportData.inspectorName)
            ReportInfoRow("House Year", reportData.houseYear)
            ReportInfoRow("Area", reportData.houseArea)
            ReportInfoRow("Expenses", reportData.expenses)
        }
    }
}

@Composable
private fun ReportSectionCard(
    section: ReportSection,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Project items
            section.projectItems.forEach { projectItem ->
                ProjectItemRow(projectItem = projectItem)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Album items (thermal images)
            if (section.albumItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ThermalImageGrid(albumItems = section.albumItems)
            }
        }
    }
}

@Composable
private fun ProjectItemRow(
    projectItem: ProjectItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = projectItem.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = projectItem.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = when (projectItem.status) {
                ProjectStatus.NORMAL -> Color.Green
                ProjectStatus.WARNING -> Color(0xFFFF9800)
                ProjectStatus.CRITICAL -> Color.Red
            }
        )
    }
}

@Composable
private fun ThermalImageGrid(
    albumItems: List<AlbumItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Thermal Images",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Grid layout for thermal images
        val chunkedItems = albumItems.chunked(2)
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    Card(
                        modifier = Modifier.weight(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            AsyncImage(
                                model = item.imagePath,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentScale = ContentScale.Crop
                            )

                            if (item.title.isNotEmpty()) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                // Fill empty space if odd number of items
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Data classes for report preview
data class HouseReportPreviewData(
    val housePhoto: String = "",
    val houseAddress: String = "",
    val houseName: String = "",
    val detectTime: String = "",
    val inspectorName: String = "",
    val houseYear: String = "",
    val houseArea: String = "",
    val expenses: String = "",
    val sections: List<ReportSection> = emptyList()
)

data class ReportSection(
    val title: String,
    val projectItems: List<ProjectItem> = emptyList(),
    val albumItems: List<AlbumItem> = emptyList()
)

data class ProjectItem(
    val name: String,
    val value: String,
    val status: ProjectStatus = ProjectStatus.NORMAL
)

data class AlbumItem(
    val imagePath: String,
    val title: String = "",
    val description: String = "",
    val imageCount: Int = 0
)

enum class ProjectStatus {
    NORMAL, WARNING, CRITICAL
}

// Helper function to convert HouseReport to preview data
private fun convertToPreviewData(report: HouseReport): HouseReportPreviewData {
    return HouseReportPreviewData(
        housePhoto = report.imagePath,
        houseAddress = report.address,
        houseName = report.name,
        detectTime = TimeTools.formatDetectTime(report.detectTime),
        inspectorName = report.inspectorName,
        houseYear = if (report.year == null) "--" else "${report.year}",
        houseArea = if (report.houseSpace.isEmpty()) "--" else "${report.houseSpace} ${report.getSpaceUnitStr()}",
        expenses = if (report.cost.isEmpty()) "--" else "${report.getCostUnitStr()} ${report.cost}",
        sections = emptyList() // Would be populated from actual report data
    )
}

@Composable
private fun ReportInfoRow(
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
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End
        )
    }
}