package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ReportPreviewSecondComposeActivity : BaseComposeActivity<ReportPreviewSecondViewModel>() {

    override fun createViewModel(): ReportPreviewSecondViewModel {
        return ReportPreviewSecondViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ReportPreviewSecondViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Advanced Report Preview",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1976D2)
                        )
                    )
                }
            ) { paddingValues ->
                ReportPreviewSecondContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun ReportPreviewSecondContent(
        viewModel: ReportPreviewSecondViewModel,
        modifier: Modifier = Modifier
    ) {
        val reportSections = remember {
            listOf(
                ReportSection("Header", "Report Title and Project Information", true),
                ReportSection("Executive Summary", "Key findings and recommendations", false),
                ReportSection("Thermal Analysis", "Detailed thermal imaging analysis", true),
                ReportSection("Temperature Data", "Temperature measurements and statistics", true),
                ReportSection("Visual Evidence", "Thermal images and thermal overlays", true),
                ReportSection("Conclusions", "Analysis conclusions and recommendations", false),
                ReportSection("Appendix", "Supporting data and references", false)
            )
        }

        var selectedSection by remember { mutableStateOf<String?>(null) }
        var previewMode by remember { mutableStateOf("Full") }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Preview Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("Full", "Summary", "Images Only")
                        modes.forEach { mode ->
                            FilterChip(
                                onClick = { previewMode = mode },
                                label = { Text(mode) },
                                selected = previewMode == mode,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1976D2),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Section navigation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Quick Navigation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(reportSections) { section ->
                            NavigationItem(
                                section = section,
                                isSelected = selectedSection == section.title,
                                onClick = { selectedSection = section.title }
                            )
                        }
                    }
                }
            }

            // Report preview area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedSection ?: "Full Report Preview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )

                        if (selectedSection != null) {
                            IconButton(
                                onClick = { }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Section",
                                    tint = Color(0xFF1976D2)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE0E0E0)
                    )

                    // Preview content
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            PreviewContent(
                                section = selectedSection,
                                mode = previewMode
                            )
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1976D2)
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }

                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Finalize")
                }
            }
        }
    }

    @Composable
    private fun NavigationItem(
        section: ReportSection,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF1976D2) else Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.Black
                    )
                    Text(
                        section.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF666666)
                    )
                }

                if (section.hasContent) {
                    Text(
                        "",
                        color = if (isSelected) Color.White else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    private fun PreviewContent(
        section: String?,
        mode: String
    ) {
        when (section) {
            null -> {
                // Full report preview
                Text(
                    "Thermal Analysis Report",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Project: Industrial Equipment Inspection\nDate: ${
                        java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
                    }\nOperator: Thermal Analysis Team",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            "Header" -> {
                Text(
                    "Report Header Section",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "This section contains the report title, project information, date, and operator details.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            "Thermal Analysis" -> {
                Text(
                    "Thermal Analysis Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Detailed thermal imaging analysis with temperature measurements and thermal patterns.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                Text(
                    "$section Content",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Preview content for the $section section of the thermal analysis report.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

data class ReportSection(
    val title: String,
    val description: String,
    val hasContent: Boolean
)

class ReportPreviewSecondViewModel : BaseViewModel()