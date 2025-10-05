package com.mpdc4gsr.module.thermalunified.report.activity

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalReportCreationComposeActivity : BaseComposeActivity<BaseViewModel>() {

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ThermalReportCreationComposeActivity::class.java))
        }

        fun startWithImage(context: Context, imagePath: String) {
            val intent = Intent(context, ThermalReportCreationComposeActivity::class.java).apply {
                putExtra("image_path", imagePath)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): BaseViewModel {
        return viewModels<BaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: BaseViewModel) {
        var currentStep by remember { mutableStateOf(0) }
        var selectedTemplate by remember { mutableStateOf<ReportTemplate?>(null) }
        var reportData by remember { mutableStateOf(ReportData()) }
        var showPreview by remember { mutableStateOf(false) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Create Thermal Report",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showPreview = true }) {
                                Icon(Icons.Default.Preview, contentDescription = "Preview")
                            }
                            IconButton(onClick = {
                                // TODO: Save current report as draft
                                android.widget.Toast.makeText(
                                    this@ThermalReportCreationComposeActivity,
                                    "Saving draft...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save Draft")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                ReportCreationContent(
                    currentStep = currentStep,
                    onStepChange = { currentStep = it },
                    selectedTemplate = selectedTemplate,
                    onTemplateSelect = { selectedTemplate = it },
                    reportData = reportData,
                    onReportDataChange = { reportData = it },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        if (showPreview) {
            ReportPreviewDialog(
                reportData = reportData,
                template = selectedTemplate,
                onDismiss = { showPreview = false },
                onGenerate = { format ->
                    // Generate report in specified format
                    showPreview = false
                }
            )
        }
    }
}

@Composable
private fun ReportCreationContent(
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    selectedTemplate: ReportTemplate?,
    onTemplateSelect: (ReportTemplate) -> Unit,
    reportData: ReportData,
    onReportDataChange: (ReportData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress indicator
        ReportCreationProgress(
            currentStep = currentStep,
            totalSteps = 4,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Step content
        when (currentStep) {
            0 -> TemplateSelectionStep(
                selectedTemplate = selectedTemplate,
                onTemplateSelect = onTemplateSelect,
                modifier = Modifier.weight(1f)
            )

            1 -> ReportInfoStep(
                reportData = reportData,
                onReportDataChange = onReportDataChange,
                modifier = Modifier.weight(1f)
            )

            2 -> ThermalDataStep(
                reportData = reportData,
                onReportDataChange = onReportDataChange,
                modifier = Modifier.weight(1f)
            )

            3 -> ReviewStep(
                reportData = reportData,
                template = selectedTemplate,
                modifier = Modifier.weight(1f)
            )
        }

        // Navigation buttons
        ReportNavigationButtons(
            currentStep = currentStep,
            onStepChange = onStepChange,
            canProceed = when (currentStep) {
                0 -> selectedTemplate != null
                1 -> reportData.title.isNotEmpty()
                2 -> reportData.thermalImages.isNotEmpty()
                3 -> true
                else -> false
            },
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ReportCreationProgress(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val steps = listOf("Template", "Info", "Data", "Review")
            steps.forEachIndexed { index, stepName ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (index <= currentStep)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < currentStep) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = stepName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index <= currentStep)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Composable
private fun TemplateSelectionStep(
    selectedTemplate: ReportTemplate?,
    onTemplateSelect: (ReportTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Choose Report Template",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val templates = getReportTemplates()
        templates.forEach { template ->
            ReportTemplateCard(
                template = template,
                isSelected = selectedTemplate?.id == template.id,
                onSelect = { onTemplateSelect(template) },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun ReportTemplateCard(
    template: ReportTemplate,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getTemplateIcon(template.type),
                contentDescription = template.type,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReportInfoStep(
    reportData: ReportData,
    onReportDataChange: (ReportData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Report Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = reportData.title,
            onValueChange = { onReportDataChange(reportData.copy(title = it)) },
            label = { Text("Report Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = reportData.author,
            onValueChange = { onReportDataChange(reportData.copy(author = it)) },
            label = { Text("Author") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = reportData.description,
            onValueChange = { onReportDataChange(reportData.copy(description = it)) },
            label = { Text("Description") },
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = reportData.location,
            onValueChange = { onReportDataChange(reportData.copy(location = it)) },
            label = { Text("Location") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun ThermalDataStep(
    reportData: ReportData,
    onReportDataChange: (ReportData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Thermal Data Selection",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Selected Images: ${reportData.thermalImages.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        // TODO: Open thermal image selector
                        android.widget.Toast.makeText(
                            context,
                            "Select thermal images...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Images",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Thermal Images")
                }
            }
        }

        if (reportData.thermalImages.isNotEmpty()) {
            Text(
                text = "Analysis Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = reportData.includeTemperatureAnalysis,
                            onCheckedChange = {
                                onReportDataChange(reportData.copy(includeTemperatureAnalysis = it))
                            }
                        )
                        Text("Include temperature analysis")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = reportData.includeHotspotDetection,
                            onCheckedChange = {
                                onReportDataChange(reportData.copy(includeHotspotDetection = it))
                            }
                        )
                        Text("Include hotspot detection")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = reportData.includeStatistics,
                            onCheckedChange = {
                                onReportDataChange(reportData.copy(includeStatistics = it))
                            }
                        )
                        Text("Include statistical analysis")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(
    reportData: ReportData,
    template: ReportTemplate?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Review & Generate",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Report Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ReportSummaryItem("Template", template?.name ?: "None selected")
                ReportSummaryItem("Title", reportData.title)
                ReportSummaryItem("Author", reportData.author)
                ReportSummaryItem("Images", "${reportData.thermalImages.size} thermal images")
                ReportSummaryItem("Location", reportData.location.ifEmpty { "Not specified" })
            }
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Export Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val exportFormats = listOf("PDF", "Word", "HTML")
                exportFormats.forEach { format ->
                    TextButton(
                        onClick = { /* Generate and export report */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            when (format) {
                                "PDF" -> Icons.Default.PictureAsPdf
                                "Word" -> Icons.Default.Description
                                "HTML" -> Icons.Default.Language
                                else -> Icons.Default.FileDownload
                            },
                            contentDescription = format,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate $format Report")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryItem(
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ReportNavigationButtons(
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    canProceed: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentStep > 0) {
            OutlinedButton(
                onClick = { onStepChange(currentStep - 1) }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        if (currentStep < 3) {
            Button(
                onClick = { onStepChange(currentStep + 1) },
                enabled = canProceed
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    // TODO: Generate final report
                    android.widget.Toast.makeText(
                        context,
                        "Generating report...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                enabled = canProceed
            ) {
                Text("Generate Report")
            }
        }
    }
}

@Composable
private fun ReportPreviewDialog(
    reportData: ReportData,
    template: ReportTemplate?,
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Preview") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (template != null) {
                    Text(
                        text = "Template: ${template.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = template.description,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Title: ${reportData.title}", fontWeight = FontWeight.SemiBold)
                Text("Author: ${reportData.author}")
                Text("Description: ${reportData.description}")
                Text("Location: ${reportData.location}")
                if (reportData.thermalImages.isNotEmpty()) {
                    Text("Thermal Images: ${reportData.thermalImages.size} attached")
                } else {
                    Text("No thermal images attached")
                }
                Text("Include Temperature Analysis: ${if (reportData.includeTemperatureAnalysis) "Yes" else "No"}")
                Text("Include Hotspot Detection: ${if (reportData.includeHotspotDetection) "Yes" else "No"}")
                Text("Include Statistics: ${if (reportData.includeStatistics) "Yes" else "No"}")
            }
        },
        confirmButton = {
            TextButton(onClick = { onGenerate("PDF") }) {
                Text("Generate PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun getTemplateIcon(type: String) = when (type) {
    "inspection" -> Icons.Default.Search
    "maintenance" -> Icons.Default.Build
    "research" -> Icons.Default.Science
    "compliance" -> Icons.Default.Verified
    else -> Icons.Default.Description
}

data class ReportTemplate(
    val id: String,
    val name: String,
    val description: String,
    val type: String
)

data class ReportData(
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val location: String = "",
    val thermalImages: List<String> = emptyList(),
    val includeTemperatureAnalysis: Boolean = true,
    val includeHotspotDetection: Boolean = true,
    val includeStatistics: Boolean = true
)

private fun getReportTemplates() = listOf(
    ReportTemplate(
        "inspection",
        "Building Inspection",
        "Comprehensive building thermal inspection template",
        "inspection"
    ),
    ReportTemplate(
        "maintenance",
        "Maintenance Report",
        "Equipment maintenance and thermal analysis template",
        "maintenance"
    ),
    ReportTemplate("research", "Research Study", "Academic research and analysis template", "research"),
    ReportTemplate("compliance", "Compliance Report", "Regulatory compliance and audit template", "compliance")
)