package com.mpdc4gsr.component.thermal.activity

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel

class ThermalReportComposeActivity : BaseComposeActivity<ThermalReportViewModel>() {
    override fun createViewModel(): ThermalReportViewModel = ThermalReportViewModel()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalReportViewModel) {
        ThermalReportScreen(
            onBackClick = { finish() },
        )
    }
}

class ThermalReportViewModel : BaseViewModel() {
    // ViewModel implementation for report generation
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThermalReportScreen(
    viewModel: ThermalReportViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null,
) {
    var selectedTemplate by remember { mutableStateOf(ReportTemplate.STANDARD) }
    var reportTitle by remember { mutableStateOf("Thermal Analysis Report") }
    var isGenerating by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117)),
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Generate Report",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161B22),
                    titleContentColor = Color.White,
                ),
            navigationIcon = {
                IconButton(onClick = { onBackClick?.invoke() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
            },
            actions = {
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(onClick = {
                    // TODO: Preview report before generation
                    android.widget.Toast
                        .makeText(
                            context,
                            "Opening report preview...",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                }) {
                    Icon(
                        Icons.Default.Preview,
                        contentDescription = "Preview",
                        tint = Color(0xFFFF6B35),
                    )
                }
            },
        )
        // Content
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Report Configuration
            ReportConfigurationSection(
                reportTitle = reportTitle,
                onTitleChange = { reportTitle = it },
                selectedTemplate = selectedTemplate,
                onTemplateChange = { selectedTemplate = it },
            )
            // Data Selection
            DataSelectionSection()
            // Analysis Options
            AnalysisOptionsSection()
            // Export Settings
            ExportSettingsSection()
            // Generate Button
            Button(
                onClick = { isGenerating = true },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35),
                        contentColor = Color.White,
                    ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isGenerating,
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating Report...")
                } else {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "Generate",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Generate PDF Report",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportConfigurationSection(
    reportTitle: String,
    onTitleChange: (String) -> Unit,
    selectedTemplate: ReportTemplate,
    onTemplateChange: (ReportTemplate) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Report Configuration",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            // Report Title
            OutlinedTextField(
                value = reportTitle,
                onValueChange = onTitleChange,
                label = { Text("Report Title", color = Color(0xFF7D8590)) },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6B35),
                        unfocusedBorderColor = Color(0xFF7D8590),
                    ),
            )
            // Template Selection
            Text(
                "Report Template",
                color = Color(0xFF7D8590),
                fontSize = 14.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportTemplate.values().forEach { template ->
                    FilterChip(
                        onClick = { onTemplateChange(template) },
                        label = {
                            Text(
                                template.displayName,
                                fontSize = 12.sp,
                            )
                        },
                        selected = selectedTemplate == template,
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF6B35),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0D1117),
                                labelColor = Color(0xFF7D8590),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DataSelectionSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Data Selection",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            // Sample data selection items
            DataSelectionItem("Thermal Images (15)", true)
            DataSelectionItem("Temperature Measurements (45)", true)
            DataSelectionItem("Time Series Data", false)
            DataSelectionItem("Calibration Data", true)
        }
    }
}

@Composable
private fun DataSelectionItem(
    title: String,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onSelectionChange,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = Color(0xFFFF6B35),
                    uncheckedColor = Color(0xFF7D8590),
                ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun AnalysisOptionsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Analysis Options",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            AnalysisOptionItem("Statistical Summary", Icons.Default.BarChart)
            AnalysisOptionItem("Temperature Trends", Icons.AutoMirrored.Filled.TrendingUp)
            AnalysisOptionItem("Thermal Mapping", Icons.Default.Map)
            AnalysisOptionItem("Comparative Analysis", Icons.Default.Compare)
        }
    }
}

@Composable
private fun AnalysisOptionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = Color(0xFFFF6B35),
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ExportSettingsSection() {
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Export Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExportFormat.values().forEach { format ->
                    FilterChip(
                        onClick = { selectedFormat = format },
                        label = {
                            Text(
                                format.displayName,
                                fontSize = 12.sp,
                            )
                        },
                        selected = format == selectedFormat,
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF6B35),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0D1117),
                                labelColor = Color(0xFF7D8590),
                            ),
                    )
                }
            }
            Text(
                text = "Selected format: ${selectedFormat.displayName}",
                color = Color(0xFF7D8590),
                fontSize = 12.sp,
            )
        }
    }
}

private enum class ReportTemplate(
    val displayName: String,
) {
    STANDARD("Standard"),
    DETAILED("Detailed"),
    SUMMARY("Summary"),
    RESEARCH("Research"),
}

private enum class ExportFormat(
    val displayName: String,
) {
    PDF("PDF"),
    DOCX("Word"),
    HTML("HTML"),
}



