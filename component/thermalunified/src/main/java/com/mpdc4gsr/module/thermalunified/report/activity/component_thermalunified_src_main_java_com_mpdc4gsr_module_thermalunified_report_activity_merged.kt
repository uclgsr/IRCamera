// Merged ALL .kt and .java files from the 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\activity' directory and its subdirectories.
// Total files: 4 | Generated on: 2025-10-08 01:42:36


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\activity\ReportCreateComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.report.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportCreateComposeActivity : ComponentActivity() {
    private lateinit var viewModel: ReportCreateViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ReportCreateViewModel::class.java]
        setContent {
            LibUnifiedTheme {
                ReportCreateScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onReportCreated = { reportId ->
                        val resultIntent = Intent().apply {
                            putExtra("report_id", reportId)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCreateScreen(
    viewModel: ReportCreateViewModel,
    onNavigateBack: () -> Unit,
    onReportCreated: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Thermal Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.currentStep > 0) {
                        TextButton(
                            onClick = { viewModel.previousStep() }
                        ) {
                            Text("Previous")
                        }
                    }
                }
            )
        },
        bottomBar = {
            ReportNavigationBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                canProceed = uiState.canProceedToNext,
                onNext = {
                    if (uiState.currentStep == uiState.totalSteps - 1) {
                        viewModel.createReport { reportId ->
                            onReportCreated(reportId)
                        }
                    } else {
                        viewModel.nextStep()
                    }
                },
                onPrevious = { viewModel.previousStep() },
                isCreating = uiState.isCreatingReport
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1).toFloat() / uiState.totalSteps },
                modifier = Modifier.fillMaxWidth()
            )
            // Step content
            when (uiState.currentStep) {
                0 -> ReportBasicInfoStep(
                    reportInfo = uiState.reportInfo,
                    onUpdateInfo = { viewModel.updateReportInfo(it) }
                )

                1 -> ThermalDataStep(
                    thermalData = uiState.thermalData,
                    onUpdateData = { viewModel.updateThermalData(it) },
                    onAddImage = { viewModel.addThermalImage(it) },
                    onRemoveImage = { viewModel.removeThermalImage(it) }
                )

                2 -> AnalysisResultsStep(
                    analysisResults = uiState.analysisResults,
                    onUpdateResults = { viewModel.updateAnalysisResults(it) }
                )

                3 -> ReportPreviewStep(
                    reportPreview = uiState.reportPreview,
                    onUpdateSettings = { viewModel.updateReportSettings(it) }
                )
            }
        }
    }
}

@Composable
fun ReportBasicInfoStep(
    reportInfo: ReportBasicInfo,
    onUpdateInfo: (ReportBasicInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = reportInfo.title,
            onValueChange = { onUpdateInfo(reportInfo.copy(title = it)) },
            label = { Text("Report Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = reportInfo.description,
            onValueChange = { onUpdateInfo(reportInfo.copy(description = it)) },
            label = { Text("Description") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = reportInfo.inspector,
                onValueChange = { onUpdateInfo(reportInfo.copy(inspector = it)) },
                label = { Text("Inspector Name") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = reportInfo.location,
                onValueChange = { onUpdateInfo(reportInfo.copy(location = it)) },
                label = { Text("Location") },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = reportInfo.equipment,
            onValueChange = { onUpdateInfo(reportInfo.copy(equipment = it)) },
            label = { Text("Equipment Used") },
            modifier = Modifier.fillMaxWidth()
        )
        // Report type selection
        Text(
            text = "Report Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Column {
            ReportType.values().forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = reportInfo.type == type,
                        onClick = { onUpdateInfo(reportInfo.copy(type = type)) }
                    )
                    Text(
                        text = type.displayName,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        // Environmental conditions
        Text(
            text = "Environmental Conditions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = reportInfo.ambientTemp,
                        onValueChange = { onUpdateInfo(reportInfo.copy(ambientTemp = it)) },
                        label = { Text("Ambient Temp (Â°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reportInfo.humidity,
                        onValueChange = { onUpdateInfo(reportInfo.copy(humidity = it)) },
                        label = { Text("Humidity (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = reportInfo.windSpeed,
                    onValueChange = { onUpdateInfo(reportInfo.copy(windSpeed = it)) },
                    label = { Text("Wind Speed (m/s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ThermalDataStep(
    thermalData: ThermalDataInfo,
    onUpdateData: (ThermalDataInfo) -> Unit,
    onAddImage: (Uri) -> Unit,
    onRemoveImage: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Thermal Data",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        // Temperature measurements
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Temperature Measurements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = thermalData.maxTemp.toString(),
                        onValueChange = {
                            it.toFloatOrNull()?.let { temp ->
                                onUpdateData(thermalData.copy(maxTemp = temp))
                            }
                        },
                        label = { Text("Max Temp (Â°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thermalData.minTemp.toString(),
                        onValueChange = {
                            it.toFloatOrNull()?.let { temp ->
                                onUpdateData(thermalData.copy(minTemp = temp))
                            }
                        },
                        label = { Text("Min Temp (Â°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thermalData.avgTemp.toString(),
                        onValueChange = {
                            it.toFloatOrNull()?.let { temp ->
                                onUpdateData(thermalData.copy(avgTemp = temp))
                            }
                        },
                        label = { Text("Avg Temp (Â°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                // Temperature distribution chart
                Text(
                    text = "Temperature Distribution",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Card {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(16.dp)
                    ) {
                        drawTemperatureDistribution(thermalData.temperatureDistribution)
                    }
                }
            }
        }
        // Thermal images section
        ThermalImagesSection(
            images = thermalData.images,
            onAddImage = onAddImage,
            onRemoveImage = onRemoveImage
        )
        // Analysis parameters
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Analysis Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = thermalData.emissivity.toString(),
                        onValueChange = {
                            it.toFloatOrNull()?.let { emissivity ->
                                onUpdateData(thermalData.copy(emissivity = emissivity))
                            }
                        },
                        label = { Text("Emissivity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thermalData.distance.toString(),
                        onValueChange = {
                            it.toFloatOrNull()?.let { distance ->
                                onUpdateData(thermalData.copy(distance = distance))
                            }
                        },
                        label = { Text("Distance (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ThermalImagesSection(
    images: List<Uri>,
    onAddImage: (Uri) -> Unit,
    onRemoveImage: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Thermal Images (${images.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Button(
                    onClick = {
                        val mockUri = Uri.parse("content://media/external/images/media/${System.currentTimeMillis()}")
                        onAddImage(mockUri)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Image")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Image")
                }
            }
            if (images.isEmpty()) {
                Text(
                    text = "No thermal images added yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(images) { imageUri ->
                        ThermalImageItem(
                            imageUri = imageUri,
                            onRemove = { onRemoveImage(imageUri) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThermalImageItem(
    imageUri: Uri,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Thermal Image",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Thermal Image",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = imageUri.lastPathSegment ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun AnalysisResultsStep(
    analysisResults: AnalysisResults,
    onUpdateResults: (AnalysisResults) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analysis Results",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = analysisResults.findings,
            onValueChange = { onUpdateResults(analysisResults.copy(findings = it)) },
            label = { Text("Key Findings") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = analysisResults.recommendations,
            onValueChange = { onUpdateResults(analysisResults.copy(recommendations = it)) },
            label = { Text("Recommendations") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = analysisResults.conclusions,
            onValueChange = { onUpdateResults(analysisResults.copy(conclusions = it)) },
            label = { Text("Conclusions") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        // Severity assessment
        Text(
            text = "Issue Severity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Column {
            IssueSeverity.values().forEach { severity ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = analysisResults.severity == severity,
                        onClick = { onUpdateResults(analysisResults.copy(severity = severity)) }
                    )
                    Text(
                        text = severity.displayName,
                        color = severity.color,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReportPreviewStep(
    reportPreview: ReportPreview,
    onUpdateSettings: (ReportSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Report Preview",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        // Report settings
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Export Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = reportPreview.settings.includeThermalImages,
                        onCheckedChange = {
                            onUpdateSettings(reportPreview.settings.copy(includeThermalImages = it))
                        }
                    )
                    Text("Include thermal images")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = reportPreview.settings.includeCharts,
                        onCheckedChange = {
                            onUpdateSettings(reportPreview.settings.copy(includeCharts = it))
                        }
                    )
                    Text("Include temperature charts")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = reportPreview.settings.includeRawData,
                        onCheckedChange = {
                            onUpdateSettings(reportPreview.settings.copy(includeRawData = it))
                        }
                    )
                    Text("Include raw temperature data")
                }
            }
        }
        // Report summary
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Report Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                ReportSummaryItem("Title", reportPreview.title)
                ReportSummaryItem("Inspector", reportPreview.inspector)
                ReportSummaryItem("Location", reportPreview.location)
                ReportSummaryItem("Thermal Images", "${reportPreview.imageCount} images")
                ReportSummaryItem(
                    "Temperature Range",
                    "${reportPreview.tempRange.first}Â°C - ${reportPreview.tempRange.second}Â°C"
                )
                ReportSummaryItem("Issue Severity", reportPreview.severity)
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
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
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
fun ReportNavigationBar(
    currentStep: Int,
    totalSteps: Int,
    canProceed: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    isCreating: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                OutlinedButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onNext,
                enabled = canProceed && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (currentStep == totalSteps - 1) "Create Report" else "Next")
                    if (currentStep < totalSteps - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

fun DrawScope.drawTemperatureDistribution(distribution: List<Float>) {
    if (distribution.isEmpty()) return
    val width = size.width
    val height = size.height
    val barWidth = width / distribution.size
    val maxValue = distribution.maxOrNull() ?: 1f
    distribution.forEachIndexed { index, value ->
        val barHeight = (value / maxValue) * height
        val x = index * barWidth
        val y = height - barHeight
        drawRect(
            color = Color(0xFF2196F3),
            topLeft = androidx.compose.ui.geometry.Offset(x, y),
            size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, barHeight)
        )
    }
}

class ReportCreateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReportCreateUiState())
    val uiState: StateFlow<ReportCreateUiState> = _uiState.asStateFlow()
    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep < _uiState.value.totalSteps - 1) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep + 1)
        }
    }

    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep - 1)
        }
    }

    fun updateReportInfo(info: ReportBasicInfo) {
        _uiState.value = _uiState.value.copy(
            reportInfo = info,
            canProceedToNext = info.title.isNotBlank() && info.inspector.isNotBlank()
        )
    }

    fun updateThermalData(data: ThermalDataInfo) {
        _uiState.value = _uiState.value.copy(
            thermalData = data,
            canProceedToNext = data.images.isNotEmpty()
        )
    }

    fun addThermalImage(uri: Uri) {
        val currentImages = _uiState.value.thermalData.images
        _uiState.value = _uiState.value.copy(
            thermalData = _uiState.value.thermalData.copy(images = currentImages + uri)
        )
    }

    fun removeThermalImage(uri: Uri) {
        val currentImages = _uiState.value.thermalData.images
        _uiState.value = _uiState.value.copy(
            thermalData = _uiState.value.thermalData.copy(images = currentImages - uri)
        )
    }

    fun updateAnalysisResults(results: AnalysisResults) {
        _uiState.value = _uiState.value.copy(
            analysisResults = results,
            canProceedToNext = results.findings.isNotBlank()
        )
    }

    fun updateReportSettings(settings: ReportSettings) {
        _uiState.value = _uiState.value.copy(
            reportPreview = _uiState.value.reportPreview.copy(settings = settings)
        )
    }

    fun createReport(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingReport = true)
            // Simulate report creation
            kotlinx.coroutines.delay(2000)
            val reportId = "TR_${System.currentTimeMillis()}"
            onComplete(reportId)
        }
    }
}

data class ReportCreateUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 4,
    val canProceedToNext: Boolean = false,
    val isCreatingReport: Boolean = false,
    val reportInfo: ReportBasicInfo = ReportBasicInfo(),
    val thermalData: ThermalDataInfo = ThermalDataInfo(),
    val analysisResults: AnalysisResults = AnalysisResults(),
    val reportPreview: ReportPreview = ReportPreview()
)

data class ReportBasicInfo(
    val title: String = "",
    val description: String = "",
    val inspector: String = "",
    val location: String = "",
    val equipment: String = "",
    val type: ReportType = ReportType.INSPECTION,
    val ambientTemp: String = "",
    val humidity: String = "",
    val windSpeed: String = ""
)

data class ThermalDataInfo(
    val maxTemp: Float = 0f,
    val minTemp: Float = 0f,
    val avgTemp: Float = 0f,
    val emissivity: Float = 0.95f,
    val distance: Float = 1.0f,
    val images: List<Uri> = emptyList(),
    val temperatureDistribution: List<Float> = listOf(12f, 15f, 22f, 18f, 25f, 30f, 28f, 20f)
)

data class AnalysisResults(
    val findings: String = "",
    val recommendations: String = "",
    val conclusions: String = "",
    val severity: IssueSeverity = IssueSeverity.LOW
)

data class ReportPreview(
    val title: String = "Thermal Inspection Report",
    val inspector: String = "",
    val location: String = "",
    val imageCount: Int = 0,
    val tempRange: Pair<Float, Float> = 0f to 0f,
    val severity: String = "",
    val settings: ReportSettings = ReportSettings()
)

data class ReportSettings(
    val includeThermalImages: Boolean = true,
    val includeCharts: Boolean = true,
    val includeRawData: Boolean = false
)

enum class ReportType(val displayName: String) {
    INSPECTION("Thermal Inspection"),
    MAINTENANCE("Maintenance Report"),
    COMPLIANCE("Compliance Check"),
    RESEARCH("Research Analysis")
}

enum class IssueSeverity(val displayName: String, val color: Color) {
    LOW("Low", Color(0xFF4CAF50)),
    MEDIUM("Medium", Color(0xFFFF9800)),
    HIGH("High", Color(0xFFF44336)),
    CRITICAL("Critical", Color(0xFF9C27B0))
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\activity\ReportDetailComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.report.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
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
import com.mpdc4gsr.module.thermalunified.viewmodel.ReportDetailViewModel

class ReportDetailComposeActivity : BaseComposeActivity<ReportDetailViewModel>() {
    override fun createViewModel(): ReportDetailViewModel {
        return viewModels<ReportDetailViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ReportDetailViewModel) {
        var showShareDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Report Details",
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
                            IconButton(onClick = { showShareDialog = true }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            IconButton(onClick = {
                                // TODO: Edit report details
                                android.widget.Toast.makeText(
                                    this@ReportDetailComposeActivity,
                                    "Opening report editor...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Report info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Report Information",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            ReportInfoRow("Date", "2024-10-01")
                            ReportInfoRow("Time", "14:30:00")
                            ReportInfoRow("Location", "Building A - Room 101")
                            ReportInfoRow("Inspector", "John Doe")
                            ReportInfoRow("Equipment", "TC001 Thermal Camera")
                        }
                    }
                    // Thermal image section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Thermal Images",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // Image placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.White.copy(alpha = 0.3f)
                                    )
                                    Text(
                                        "Thermal Image",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    // Measurement data
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Measurements",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            MeasurementRow("Max Temperature", "28.5Â°C", Color(0xFFFF4747))
                            MeasurementRow("Min Temperature", "22.1Â°C", Color(0xFF06AAFF))
                            MeasurementRow("Avg Temperature", "25.3Â°C", Color(0xFFFFA500))
                            MeasurementRow("Emissivity", "0.95", Color.White)
                        }
                    }
                    // Notes section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Notes",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Regular thermal inspection conducted. All readings within normal parameters. Minor hotspot detected in corner area, requires follow-up.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    // Watermark notice
                    Text(
                        "Generated by IRCamera Thermal Imaging System",
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    @Composable
    fun ReportInfoRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Text(
                value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    fun MeasurementRow(label: String, value: String, valueColor: Color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = valueColor.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    value,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = valueColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\activity\ReportPreviewFirstComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.report.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
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
import com.mpdc4gsr.module.thermalunified.viewmodel.ReportPreviewViewModel

class ReportPreviewFirstComposeActivity : BaseComposeActivity<ReportPreviewViewModel>() {
    override fun createViewModel(): ReportPreviewViewModel {
        return viewModels<ReportPreviewViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ReportPreviewViewModel) {
        var selectedLayout by remember { mutableIntStateOf(0) }
        var showConfirmDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Report Preview",
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
                            TextButton(
                                onClick = {
                                    // TODO: Proceed to next report creation step
                                    android.widget.Toast.makeText(
                                        this@ReportPreviewFirstComposeActivity,
                                        "Proceeding to next step...",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Text("Next", color = Color.White)
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Preview area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = "Report Preview",
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.White.copy(alpha = 0.3f)
                                )
                                Text(
                                    "Report Preview",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Layout ${selectedLayout + 1}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .height(16.dp)
                                            .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(16.dp)
                                            .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                    // Layout selection
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Select Layout",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(3) { index ->
                                    LayoutOption(
                                        index = index,
                                        selected = selectedLayout == index,
                                        onClick = { selectedLayout = index },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    // Options
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Preview Options",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            var showImages by remember { mutableStateOf(true) }
                            var showMetadata by remember { mutableStateOf(true) }
                            var showWatermark by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show Images", color = Color.White, fontSize = 14.sp)
                                Switch(
                                    checked = showImages,
                                    onCheckedChange = { showImages = it }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show Metadata", color = Color.White, fontSize = 14.sp)
                                Switch(
                                    checked = showMetadata,
                                    onCheckedChange = { showMetadata = it }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Add Watermark", color = Color.White, fontSize = 14.sp)
                                Switch(
                                    checked = showWatermark,
                                    onCheckedChange = { showWatermark = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LayoutOption(
        index: Int,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A)
            ),
            border = if (selected) null else androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = "Layout ${index + 1}",
                    tint = if (selected) Color.White else Color.White.copy(alpha = 0.5f)
                )
                Text(
                    "Layout ${index + 1}",
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\activity\ThermalReportCreationComposeActivity.kt =====

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
                        onClick = { },
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