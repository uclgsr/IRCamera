// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report' subtree
// Files: 21; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\activity\ReportCreateComposeActivity.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\activity\ReportDetailComposeActivity.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\activity\ReportPreviewFirstComposeActivity.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\activity\ThermalReportCreationComposeActivity.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ImageTempBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageTempBean(
    val full: TempBean?,
    val pointList: ArrayList<TempBean>,
    val lineList: ArrayList<TempBean>,
    val rectList: ArrayList<TempBean>,
) : Parcelable {
    @Parcelize
    data class TempBean(
        val max: String,
        val min: String? = null,
        val average: String? = null,
    ) : Parcelable
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportBean(
    val software_info: SoftwareInfo,
    val report_info: ReportInfoBean,
    val detection_condition: ReportConditionBean,
    val infrared_data: List<ReportIRBean>,
) : Parcelable


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportConditionBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportConditionBean(
    val ambient_humidity: String?,
    val is_ambient_humidity: Int,
    val ambient_temperature: String?,
    val is_ambient_temperature: Int,
    val emissivity: String?,
    val is_emissivity: Int,
    val test_distance: String?,
    val is_test_distance: Int,
) : Parcelable


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportData.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import com.google.gson.Gson

class ReportData {
    var code = 0
    var data: DataBean? = null
    var msg: String? = null
    var serverTime: String? = null

    class DataBean {
        var total = 0
        var current = 0
        var isHitCount = false
        var pages = 0
        var size = 0
        var isOptimizeCountSql = false
        var isSearchCount = false
        var records: MutableList<Records?>? = null
    }

    class Records {
        var testReportId: String? = null
        var testTime: String? = null
        var testInfo: String? = null
        var sn: String? = null
        var uploadTime: String? = null
        var status: String? = null
        var isShowTitleTime: Boolean = false
        var reportContent: ReportBean? = null
            get() {
                if (field == null) {
                    field = Gson().fromJson(testInfo, ReportBean::class.java)
                }
                return field
            }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportInfoBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import com.mpdc4gsr.libunified.app.utils.CommUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportInfoBean(
    val report_name: String?,
    val report_author: String?,
    val is_report_author: Int,
    val report_date: String?,
    val is_report_date: Int,
    val report_place: String?,
    val is_report_place: Int,
    val report_watermark: String?,
    val is_report_watermark: Int,
) : Parcelable {
    @IgnoredOnParcel
    val is_report_name: Int = 1

    @IgnoredOnParcel
    val report_type: Int = 1

    @IgnoredOnParcel
    val report_version: String = "V1.00"

    @IgnoredOnParcel
    val report_number: String =
        "${CommUtils.getAppName()}${System.currentTimeMillis()}"
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportIRBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportIRBean(
    var picture_id: String,
    var picture_url: String,
    val full_graph_data: ReportTempBean?,
    val point_data: List<ReportTempBean>,
    val line_data: List<ReportTempBean>,
    val surface_data: List<ReportTempBean>,
) : Parcelable


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportItemBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportItemBean(
    val testReportId: String?,
    val testInfo: String?,
    val testTime: String?,
    val uploadTime: String?,
    val sn: String?,
    val url: String?,
    val status: Int?,
) : Parcelable {
    @IgnoredOnParcel
    var reportBean: ReportBean? = null
        get() {
            if (field == null) {
                field = Gson().fromJson(testInfo, ReportBean::class.java)
            }
            return field
        }

    @IgnoredOnParcel
    var isFirst: Boolean = false

    @IgnoredOnParcel
    var isTitle: Boolean = false
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportPageBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportPageBean(
    val total: Int = 0,
    var current: Int = 0,
    var pages: Int = 0,
    var size: Int = 0,
    var isHitCount: Boolean = false,
    var isOptimizeCountSql: Boolean = false,
    var isSearchCount: Boolean = false,
    var records: MutableList<ReportItemBean>? = null,
) : Parcelable


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\ReportTempBean.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportTempBean(
    val max_temperature: String?,
    val is_max_temperature: Int,
    val min_temperature: String?,
    val is_min_temperature: Int,
    val comment: String?,
    val is_comment: Int,
    val mean_temperature: String? = null,
    val is_mean_temperature: Int = 0,
    val temperature: String? = null,
    val is_temperature: Int = 0,
) : Parcelable {
    constructor(
        temperature: String?,
        is_temperature: Int,
        comment: String?,
        is_comment: Int
    ) : this(
        null,
        0,
        null,
        0,
        comment,
        is_comment,
        null,
        0,
        temperature,
        is_temperature,
    )

    fun isMaxOpen() = is_max_temperature == 1
    fun isMinOpen() = is_min_temperature == 1
    fun isAverageOpen() = is_mean_temperature == 1
    fun isExplainOpen() = is_comment == 1
    fun isTempOpen() = is_temperature == 1
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\bean\SoftwareInfo.kt =====

package com.mpdc4gsr.module.thermalunified.report.bean

import android.os.Build
import android.os.Parcelable
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SoftwareInfo(
    val app_language: String,
    val sdk_version: String,
) : Parcelable {
    @IgnoredOnParcel
    val software_code = BaseApplication.instance.getSoftWareCode()

    @IgnoredOnParcel
    val system_language = AppLanguageUtils.getSystemLanguage()

    @IgnoredOnParcel
    val app_version = "1.10.000"

    @IgnoredOnParcel
    val hardware_version = ""

    @IgnoredOnParcel
    val app_sn = ""

    @IgnoredOnParcel
    val mobile_phone_model = Build.BRAND

    @IgnoredOnParcel
    val system_version = Build.VERSION.RELEASE
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\view\ReportInfoView.kt =====

package com.mpdc4gsr.module.thermalunified.report.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.report.bean.ReportConditionBean
import com.mpdc4gsr.module.thermalunified.report.bean.ReportInfoBean

class ReportInfoView : LinearLayout {
    private lateinit var tvReportName: android.widget.TextView
    private lateinit var tvReportAuthor: android.widget.TextView
    private lateinit var groupReportPlace: androidx.constraintlayout.widget.Group
    private lateinit var tvReportPlace: android.widget.TextView
    private lateinit var tvReportDate: android.widget.TextView
    private lateinit var clReportCondition: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var groupAmbientTemperature: androidx.constraintlayout.widget.Group
    private lateinit var tvAmbientTemperature: android.widget.TextView
    private lateinit var viewLine1: android.view.View
    private lateinit var groupAmbientHumidity: androidx.constraintlayout.widget.Group
    private lateinit var tvAmbientHumidity: android.widget.TextView
    private lateinit var viewLine2: android.view.View
    private lateinit var groupTestDistance: androidx.constraintlayout.widget.Group
    private lateinit var tvTestDistance: android.widget.TextView
    private lateinit var viewLine3: android.view.View
    private lateinit var groupEmissivity: androidx.constraintlayout.widget.Group
    private lateinit var tvEmissivity: android.widget.TextView
    private lateinit var clTop: androidx.constraintlayout.widget.ConstraintLayout

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        LayoutInflater.from(context).inflate(R.layout.view_report_info, this, true)
        initViews()
    }

    private fun initViews() {
        tvReportName = findViewById(R.id.tv_report_name)
        tvReportAuthor = findViewById(R.id.tv_report_author)
        groupReportPlace = findViewById(R.id.group_report_place)
        tvReportPlace = findViewById(R.id.tv_report_place)
        tvReportDate = findViewById(R.id.tv_report_date)
        clReportCondition = findViewById(R.id.cl_report_condition)
        groupAmbientTemperature = findViewById(R.id.group_ambient_temperature)
        tvAmbientTemperature = findViewById(R.id.tv_ambient_temperature)
        viewLine1 = findViewById(R.id.view_line_1)
        groupAmbientHumidity = findViewById(R.id.group_ambient_humidity)
        tvAmbientHumidity = findViewById(R.id.tv_ambient_humidity)
        viewLine2 = findViewById(R.id.view_line_2)
        groupTestDistance = findViewById(R.id.group_test_distance)
        tvTestDistance = findViewById(R.id.tv_test_distance)
        viewLine3 = findViewById(R.id.view_line_3)
        groupEmissivity = findViewById(R.id.group_emissivity)
        tvEmissivity = findViewById(R.id.tv_emissivity)
        clTop = findViewById(R.id.cl_top)
    }

    fun refreshInfo(reportInfoBean: ReportInfoBean?) {
        tvReportName.text = reportInfoBean?.report_name
        tvReportAuthor.isVisible = reportInfoBean?.is_report_author == 1
        tvReportAuthor.text = reportInfoBean?.report_author
        groupReportPlace.isVisible = reportInfoBean?.is_report_place == 1
        tvReportPlace.text = reportInfoBean?.report_place
        tvReportDate.isVisible = reportInfoBean?.is_report_date == 1
        tvReportDate.text = reportInfoBean?.report_date
    }

    fun refreshCondition(conditionBean: ReportConditionBean?) {
        clReportCondition.isVisible = conditionBean?.is_ambient_humidity == 1 ||
                conditionBean?.is_ambient_temperature == 1 ||
                conditionBean?.is_test_distance == 1 ||
                conditionBean?.is_emissivity == 1
        groupAmbientTemperature.isVisible = conditionBean?.is_ambient_temperature == 1
        tvAmbientTemperature.text = conditionBean?.ambient_temperature
        viewLine1.isVisible = conditionBean?.is_ambient_temperature == 1 &&
                (conditionBean.is_ambient_humidity == 1 || conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)
        groupAmbientHumidity.isVisible = conditionBean?.is_ambient_humidity == 1
        tvAmbientHumidity.text = conditionBean?.ambient_humidity
        viewLine2.isVisible =
            conditionBean?.is_ambient_humidity == 1 && (conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)
        groupTestDistance.isVisible = conditionBean?.is_test_distance == 1
        tvTestDistance.text = conditionBean?.test_distance
        viewLine3.isVisible =
            conditionBean?.is_test_distance == 1 && conditionBean.is_emissivity == 1
        groupEmissivity.isVisible = conditionBean?.is_emissivity == 1
        tvEmissivity.text = conditionBean?.emissivity
    }

    fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        result.add(clTop)
        result.add(clReportCondition)
        return result
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\view\ReportIRInputView.kt =====

package com.mpdc4gsr.module.thermalunified.report.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.report.bean.ImageTempBean
import com.mpdc4gsr.libunified.R as LibR

class ReportIRInputView : LinearLayout {
    companion object {
        private const val TYPE_FULL = 0
        private const val TYPE_POINT = 1
        private const val TYPE_LINE = 2
        private const val TYPE_RECT = 3
    }

    private lateinit var clTitle: View
    private lateinit var viewLine: View
    private lateinit var tvTitle: TextView
    private lateinit var clMax: View
    private lateinit var clMin: View
    private lateinit var clAverage: View
    private lateinit var clExplain: View

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("SetTextI18n")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        inflate(context, R.layout.view_report_ir_input, this)
        initViews()
        val etExplain = clExplain.findViewById<EditText>(R.id.et_item)
        etExplain.inputType = InputType.TYPE_CLASS_TEXT
        etExplain.filters = arrayOf(LengthFilter(150))
        val switchMax = clMax.findViewById<SwitchCompat>(R.id.switch_item)
        val etMax = clMax.findViewById<EditText>(R.id.et_item)
        setSwitchListener(switchMax, etMax)
        val switchMin = clMin.findViewById<SwitchCompat>(R.id.switch_item)
        val etMin = clMin.findViewById<EditText>(R.id.et_item)
        setSwitchListener(switchMin, etMin)
        val switchAverage = clAverage.findViewById<SwitchCompat>(R.id.switch_item)
        val etAverage = clAverage.findViewById<EditText>(R.id.et_item)
        setSwitchListener(switchAverage, etAverage)
        val switchExplain = clExplain.findViewById<SwitchCompat>(R.id.switch_item)
        setSwitchListener(switchExplain, etExplain)
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.ReportIRInputView)
        val type = typeArray.getInt(R.styleable.ReportIRInputView_type, TYPE_FULL)
        val index = typeArray.getInt(R.styleable.ReportIRInputView_index, 0)
        typeArray.recycle()
        clTitle.isVisible = index == 0
        viewLine.isVisible = index > 0
        setupTypeSpecificViews(type, index)
    }

    private fun initViews() {
        clTitle = findViewById(R.id.cl_title)
        viewLine = findViewById(R.id.view_line)
        tvTitle = findViewById(R.id.tv_title)
        clMax = findViewById(R.id.cl_max)
        clMin = findViewById(R.id.cl_min)
        clAverage = findViewById(R.id.cl_average)
        clExplain = findViewById(R.id.cl_explain)
    }

    private fun setupTypeSpecificViews(
        type: Int,
        index: Int,
    ) {
        val tvMaxName = clMax.findViewById<TextView>(R.id.tv_item_name)
        val tvMinName = clMin.findViewById<TextView>(R.id.tv_item_name)
        val tvAverageName = clAverage.findViewById<TextView>(R.id.tv_item_name)
        val tvExplainName = clExplain.findViewById<TextView>(R.id.tv_item_name)
        when (type) {
            TYPE_FULL -> {
                tvTitle.setText(LibR.string.thermal_full_rect)
                clMin.isVisible = true
                clAverage.isVisible = false
                tvMaxName.text =
                    context.getString(LibR.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                tvMinName.text =
                    context.getString(LibR.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                tvExplainName.text = context.getString(LibR.string.album_report_comment)
            }

            TYPE_POINT -> {
                tvTitle.text = context.getString(LibR.string.thermal_point) + "(P)"
                clMin.isVisible = false
                clAverage.isVisible = false
                tvMaxName.text =
                    "P${index + 1} " + context.getString(LibR.string.chart_temperature) + " (${UnitTools.showUnit()})"
                tvExplainName.text =
                    "P${index + 1} " + context.getString(LibR.string.album_report_comment)
            }

            TYPE_LINE -> {
                tvTitle.text = context.getString(LibR.string.thermal_line) + "(L)"
                clMin.isVisible = true
                clAverage.isVisible = true
                tvMaxName.text =
                    "L${index + 1} " + context.getString(LibR.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                tvMinName.text =
                    "L${index + 1} " + context.getString(LibR.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                tvAverageName.text =
                    "L${index + 1} " + context.getString(LibR.string.album_report_mean_temperature) + " (${UnitTools.showUnit()})"
                tvExplainName.text =
                    "L${index + 1} " + context.getString(LibR.string.album_report_comment)
            }

            TYPE_RECT -> {
                tvTitle.text = context.getString(LibR.string.thermal_rect) + "(R)"
                clMin.isVisible = true
                clAverage.isVisible = true
                tvMaxName.text =
                    "R${index + 1} " + context.getString(LibR.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                tvMinName.text =
                    "R${index + 1} " + context.getString(LibR.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                tvAverageName.text =
                    "R${index + 1} " + context.getString(LibR.string.album_report_mean_temperature) + " (${UnitTools.showUnit()})"
                tvExplainName.text =
                    "R${index + 1} " + context.getString(LibR.string.album_report_comment)
            }
        }
    }

    fun isSwitchMaxCheck(): Boolean {
        val switchMax = clMax.findViewById<SwitchCompat>(R.id.switch_item)
        return switchMax.isChecked
    }

    fun isSwitchMinCheck(): Boolean {
        val switchMin = clMin.findViewById<SwitchCompat>(R.id.switch_item)
        return switchMin.isChecked
    }

    fun isSwitchAverageCheck(): Boolean {
        val switchAverage = clAverage.findViewById<SwitchCompat>(R.id.switch_item)
        return switchAverage.isChecked
    }

    fun isSwitchExplainCheck(): Boolean {
        val switchExplain = clExplain.findViewById<SwitchCompat>(R.id.switch_item)
        return switchExplain.isChecked
    }

    fun getMaxInput(): String {
        val etMax = clMax.findViewById<EditText>(R.id.et_item)
        return etMax.text.toString()
    }

    fun getMinInput(): String {
        val etMin = clMin.findViewById<EditText>(R.id.et_item)
        return etMin.text.toString()
    }

    fun getAverageInput(): String {
        val etAverage = clAverage.findViewById<EditText>(R.id.et_item)
        return etAverage.text.toString()
    }

    fun getExplainInput(): String {
        val etExplain = clExplain.findViewById<EditText>(R.id.et_item)
        return etExplain.text.toString()
    }

    fun refreshData(tempBean: ImageTempBean.TempBean?) {
        val etMax = clMax.findViewById<EditText>(R.id.et_item)
        val etMin = clMin.findViewById<EditText>(R.id.et_item)
        val etAverage = clAverage.findViewById<EditText>(R.id.et_item)
        val etExplain = clExplain.findViewById<EditText>(R.id.et_item)
        tempBean?.max?.let {
            etMax.setText(UnitTools.showUnitValue(it.toFloat())?.toString())
        }
        tempBean?.min?.let {
            etMin.setText(UnitTools.showUnitValue(it.toFloat())?.toString())
        }
        tempBean?.average?.let {
            etAverage.setText(UnitTools.showUnitValue(it.toFloat())?.toString())
        }
        etExplain.setText("")
    }

    private fun setSwitchListener(
        switchCompat: SwitchCompat,
        editText: EditText,
    ) {
        switchCompat.setOnCheckedChangeListener { _, isChecked ->
            editText.isVisible = isChecked
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\view\ReportIRShowView.kt =====

package com.mpdc4gsr.module.thermalunified.report.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.mpdc4gsr.libunified.app.utils.ScreenUtils
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.report.bean.ReportIRBean
import com.mpdc4gsr.module.thermalunified.report.bean.ReportTempBean
import com.mpdc4gsr.libunified.R as LibR

class ReportIRShowView : LinearLayout {
    companion object {
        private const val TYPE_FULL = 0
        private const val TYPE_POINT = 1
        private const val TYPE_LINE = 2
        private const val TYPE_RECT = 3
    }

    private lateinit var clImage: View
    private lateinit var clFull: View
    private lateinit var clPoint1: View
    private lateinit var clPoint2: View
    private lateinit var clPoint3: View
    private lateinit var clPoint4: View
    private lateinit var clPoint5: View
    private lateinit var clLine1: View
    private lateinit var clLine2: View
    private lateinit var clLine3: View
    private lateinit var clLine4: View
    private lateinit var clLine5: View
    private lateinit var clRect1: View
    private lateinit var clRect2: View
    private lateinit var clRect3: View
    private lateinit var clRect4: View
    private lateinit var clRect5: View

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        inflate(context, R.layout.view_report_ir_show, this)
        initViews()
        initTitleTexts()
    }

    private fun initViews() {
        clImage = findViewById(R.id.cl_image)
        clFull = findViewById(R.id.cl_full)
        clPoint1 = findViewById(R.id.cl_point1)
        clPoint2 = findViewById(R.id.cl_point2)
        clPoint3 = findViewById(R.id.cl_point3)
        clPoint4 = findViewById(R.id.cl_point4)
        clPoint5 = findViewById(R.id.cl_point5)
        clLine1 = findViewById(R.id.cl_line1)
        clLine2 = findViewById(R.id.cl_line2)
        clLine3 = findViewById(R.id.cl_line3)
        clLine4 = findViewById(R.id.cl_line4)
        clLine5 = findViewById(R.id.cl_line5)
        clRect1 = findViewById(R.id.cl_rect1)
        clRect2 = findViewById(R.id.cl_rect2)
        clRect3 = findViewById(R.id.cl_rect3)
        clRect4 = findViewById(R.id.cl_rect4)
        clRect5 = findViewById(R.id.cl_rect5)
    }

    private fun initTitleTexts() {
        initTitleText(clFull, TYPE_FULL, 0)
        initTitleText(clPoint1, TYPE_POINT, 0)
        initTitleText(clPoint2, TYPE_POINT, 1)
        initTitleText(clPoint3, TYPE_POINT, 2)
        initTitleText(clPoint4, TYPE_POINT, 3)
        initTitleText(clPoint5, TYPE_POINT, 4)
        initTitleText(clLine1, TYPE_LINE, 0)
        initTitleText(clLine2, TYPE_LINE, 1)
        initTitleText(clLine3, TYPE_LINE, 2)
        initTitleText(clLine4, TYPE_LINE, 3)
        initTitleText(clLine5, TYPE_LINE, 4)
        initTitleText(clRect1, TYPE_RECT, 0)
        initTitleText(clRect2, TYPE_RECT, 1)
        initTitleText(clRect3, TYPE_RECT, 2)
        initTitleText(clRect4, TYPE_RECT, 3)
        initTitleText(clRect5, TYPE_RECT, 4)
    }

    private fun initTitleText(
        itemRoot: View,
        type: Int,
        index: Int,
    ) {
        val tvTitle = itemRoot.findViewById<TextView>(R.id.tv_title)
        val tvAverageTitle = itemRoot.findViewById<TextView>(R.id.tv_average_title)
        val tvExplainTitle = itemRoot.findViewById<TextView>(R.id.tv_explain_title)
        tvTitle.isVisible = index == 0
        tvTitle.text =
            when (type) {
                TYPE_FULL -> context.getString(LibR.string.thermal_full_rect)
                TYPE_POINT -> context.getString(LibR.string.thermal_point) + "(P)"
                TYPE_LINE -> context.getString(LibR.string.thermal_line) + "(L)"
                else -> context.getString(LibR.string.thermal_rect) + "(R)"
            }
        tvAverageTitle.text =
            when (type) {
                TYPE_FULL, TYPE_POINT -> ""
                TYPE_LINE -> "L${index + 1} " + context.getString(LibR.string.album_report_mean_temperature)
                else -> "R${index + 1} " + context.getString(LibR.string.album_report_mean_temperature)
            }
        tvExplainTitle.text =
            when (type) {
                TYPE_FULL -> context.getString(LibR.string.album_report_comment)
                TYPE_POINT -> "P${index + 1} " + context.getString(LibR.string.album_report_comment)
                TYPE_LINE -> "L${index + 1} " + context.getString(LibR.string.album_report_comment)
                else -> "R${index + 1} " + context.getString(LibR.string.album_report_comment)
            }
    }

    fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        result.add(clImage)
        getItemChild(clFull, result)
        getItemChild(clPoint1, result)
        getItemChild(clPoint2, result)
        getItemChild(clPoint3, result)
        getItemChild(clPoint4, result)
        getItemChild(clPoint5, result)
        getItemChild(clLine1, result)
        getItemChild(clLine2, result)
        getItemChild(clLine3, result)
        getItemChild(clLine4, result)
        getItemChild(clLine5, result)
        getItemChild(clRect1, result)
        getItemChild(clRect2, result)
        getItemChild(clRect3, result)
        getItemChild(clRect4, result)
        getItemChild(clRect5, result)
        return result
    }

    private fun getItemChild(
        itemRoot: View,
        resultList: ArrayList<View>,
    ) {
        if (itemRoot.isVisible) {
            val clRange = itemRoot.findViewById<View>(R.id.cl_range)
            val clAverage = itemRoot.findViewById<View>(R.id.cl_average)
            val clExplain = itemRoot.findViewById<View>(R.id.cl_explain)
            if (clRange.isVisible) {
                resultList.add(clRange)
            }
            if (clAverage.isVisible) {
                resultList.add(clAverage)
            }
            if (clExplain.isVisible) {
                resultList.add(clExplain)
            }
        }
    }

    fun setImageDrawable(drawable: Drawable?) {
        val ivImage = findViewById<View>(R.id.iv_image)
        val isLand = (drawable?.intrinsicWidth ?: 0) > (drawable?.intrinsicHeight ?: 0)
        val width = (ScreenUtils.getScreenWidth(context) * (if (isLand) 234 else 175) / 375f).toInt()
        val height = (width * (drawable?.intrinsicHeight ?: 0).toFloat() / (drawable?.intrinsicWidth
            ?: 1)).toInt()
        val layoutParams = ivImage.layoutParams
        layoutParams.width = width
        layoutParams.height = height
        ivImage.layoutParams = layoutParams
        (ivImage as? android.widget.ImageView)?.setImageDrawable(drawable)
    }

    fun refreshData(
        isFirst: Boolean,
        isLast: Boolean,
        reportIRBean: ReportIRBean,
    ) {
        val tvHead = findViewById<TextView>(R.id.tv_head)
        val viewNotHead = findViewById<View>(R.id.view_not_head)
        val viewImageBg = findViewById<View>(R.id.view_image_bg)
        tvHead.isVisible = isFirst
        viewNotHead.isVisible = !isFirst
        viewImageBg.setBackgroundResource(if (isFirst) R.drawable.layer_report_ir_show_top_bg else R.drawable.layer_report_ir_show_item_bg)
        clImage.setPadding(0, if (isFirst) 20f.dpToPx(context).toInt() else 0, 0, 0)
        refreshItem(clFull, reportIRBean.full_graph_data, TYPE_FULL, 0)
        val pointList = reportIRBean.point_data
        for (i in pointList.indices) {
            when (i) {
                0 -> refreshItem(clPoint1, pointList[i], TYPE_POINT, i)
                1 -> refreshItem(clPoint2, pointList[i], TYPE_POINT, i)
                2 -> refreshItem(clPoint3, pointList[i], TYPE_POINT, i)
                3 -> refreshItem(clPoint4, pointList[i], TYPE_POINT, i)
                4 -> refreshItem(clPoint5, pointList[i], TYPE_POINT, i)
            }
        }
        val tvTitlePoint2 = clPoint2.findViewById<TextView>(R.id.tv_title)
        val tvTitlePoint3 = clPoint3.findViewById<TextView>(R.id.tv_title)
        val tvTitlePoint4 = clPoint4.findViewById<TextView>(R.id.tv_title)
        val tvTitlePoint5 = clPoint5.findViewById<TextView>(R.id.tv_title)
        tvTitlePoint2.isVisible = !clPoint1.isVisible
        tvTitlePoint3.isVisible = !clPoint1.isVisible && !clPoint2.isVisible
        tvTitlePoint4.isVisible = !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible
        tvTitlePoint5.isVisible =
            !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible && !clPoint4.isVisible
        val lineList = reportIRBean.line_data
        for (i in lineList.indices) {
            when (i) {
                0 -> refreshItem(clLine1, lineList[i], TYPE_LINE, i)
                1 -> refreshItem(clLine2, lineList[i], TYPE_LINE, i)
                2 -> refreshItem(clLine3, lineList[i], TYPE_LINE, i)
                3 -> refreshItem(clLine4, lineList[i], TYPE_LINE, i)
                4 -> refreshItem(clLine5, lineList[i], TYPE_LINE, i)
            }
        }
        val tvTitleLine2 = clLine2.findViewById<TextView>(R.id.tv_title)
        val tvTitleLine3 = clLine3.findViewById<TextView>(R.id.tv_title)
        val tvTitleLine4 = clLine4.findViewById<TextView>(R.id.tv_title)
        val tvTitleLine5 = clLine5.findViewById<TextView>(R.id.tv_title)
        tvTitleLine2.isVisible = !clLine1.isVisible
        tvTitleLine3.isVisible = !clLine1.isVisible && !clLine2.isVisible
        tvTitleLine4.isVisible = !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible
        tvTitleLine5.isVisible =
            !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible && !clLine4.isVisible
        val rectList = reportIRBean.surface_data
        for (i in rectList.indices) {
            when (i) {
                0 -> refreshItem(clRect1, rectList[i], TYPE_RECT, i)
                1 -> refreshItem(clRect2, rectList[i], TYPE_RECT, i)
                2 -> refreshItem(clRect3, rectList[i], TYPE_RECT, i)
                3 -> refreshItem(clRect4, rectList[i], TYPE_RECT, i)
                4 -> refreshItem(clRect5, rectList[i], TYPE_RECT, i)
            }
        }
        val tvTitleRect2 = clRect2.findViewById<TextView>(R.id.tv_title)
        val tvTitleRect3 = clRect3.findViewById<TextView>(R.id.tv_title)
        val tvTitleRect4 = clRect4.findViewById<TextView>(R.id.tv_title)
        val tvTitleRect5 = clRect5.findViewById<TextView>(R.id.tv_title)
        tvTitleRect2.isVisible = !clRect1.isVisible
        tvTitleRect3.isVisible = !clRect1.isVisible && !clRect2.isVisible
        tvTitleRect4.isVisible = !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible
        tvTitleRect5.isVisible =
            !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible && !clRect4.isVisible
        if (rectList.isNotEmpty()) {
            when (rectList.size) {
                1 -> hideLastLine(isLast, clRect1, rectList[0], TYPE_RECT)
                2 -> hideLastLine(isLast, clRect2, rectList[1], TYPE_RECT)
                3 -> hideLastLine(isLast, clRect3, rectList[2], TYPE_RECT)
                4 -> hideLastLine(isLast, clRect4, rectList[3], TYPE_RECT)
                5 -> hideLastLine(isLast, clRect5, rectList[4], TYPE_RECT)
            }
            return
        }
        if (lineList.isNotEmpty()) {
            when (lineList.size) {
                1 -> hideLastLine(isLast, clLine1, lineList[0], TYPE_LINE)
                2 -> hideLastLine(isLast, clLine2, lineList[1], TYPE_LINE)
                3 -> hideLastLine(isLast, clLine3, lineList[2], TYPE_LINE)
                4 -> hideLastLine(isLast, clLine4, lineList[3], TYPE_LINE)
                5 -> hideLastLine(isLast, clLine5, lineList[4], TYPE_LINE)
            }
            return
        }
        if (pointList.isNotEmpty()) {
            when (pointList.size) {
                1 -> hideLastLine(isLast, clPoint1, pointList[0], TYPE_POINT)
                2 -> hideLastLine(isLast, clPoint2, pointList[1], TYPE_POINT)
                3 -> hideLastLine(isLast, clPoint3, pointList[2], TYPE_POINT)
                4 -> hideLastLine(isLast, clPoint4, pointList[3], TYPE_POINT)
                5 -> hideLastLine(isLast, clPoint5, pointList[4], TYPE_POINT)
            }
            return
        }
        hideLastLine(isLast, clFull, reportIRBean.full_graph_data, TYPE_FULL)
    }

    private fun hideLastLine(
        isLast: Boolean,
        itemRoot: View,
        tempBean: ReportTempBean?,
        type: Int,
    ) {
        if (tempBean == null) {
            return
        }
        val viewLineExplain = itemRoot.findViewById<View>(R.id.view_line_explain)
        val viewLineAverage = itemRoot.findViewById<View>(R.id.view_line_average)
        val viewLineRange = itemRoot.findViewById<View>(R.id.view_line_range)
        if (tempBean.isExplainOpen()) {
            viewLineExplain.isVisible = !isLast
        } else if ((type == TYPE_LINE || type == TYPE_RECT) && tempBean.isAverageOpen()) {
            viewLineAverage.isVisible = !isLast
        } else {
            viewLineRange.isVisible = !isLast
        }
    }

    private fun refreshItem(
        itemRoot: View,
        tempBean: ReportTempBean?,
        type: Int,
        index: Int,
    ) {
        if (tempBean == null) {
            itemRoot.isVisible = false
            return
        }
        itemRoot.isVisible =
            when (type) {
                TYPE_FULL -> tempBean.isMaxOpen() || tempBean.isMinOpen() || tempBean.isExplainOpen()
                TYPE_POINT -> tempBean.isTempOpen() || tempBean.isExplainOpen()
                else -> tempBean.isMaxOpen() || tempBean.isMinOpen() || tempBean.isAverageOpen() || tempBean.isExplainOpen()
            }
        if (!itemRoot.isVisible) {
            return
        }
        val rangeTitle =
            if (type == TYPE_POINT) {
                "P${index + 1} " + context.getString(LibR.string.chart_temperature)
            } else {
                val prefix =
                    when (type) {
                        TYPE_LINE -> "L${index + 1} "
                        TYPE_RECT -> "R${index + 1} "
                        else -> ""
                    }
                prefix +
                        if (tempBean.isMinOpen() && tempBean.isMaxOpen()) {
                            context.getString(LibR.string.chart_temperature_low) + "-" + context.getString(
                                LibR.string.chart_temperature_high
                            )
                        } else if (tempBean.isMinOpen()) {
                            context.getString(LibR.string.chart_temperature_low)
                        } else {
                            context.getString(LibR.string.chart_temperature_high)
                        }
            }
        val rangeValue =
            if (type == TYPE_POINT) {
                tempBean.temperature
            } else {
                if (tempBean.isMinOpen() && tempBean.isMaxOpen()) {
                    tempBean.min_temperature + "~" + tempBean.max_temperature
                } else if (tempBean.isMinOpen()) {
                    tempBean.min_temperature
                } else {
                    tempBean.max_temperature
                }
            }
        val tvRangeTitle = itemRoot.findViewById<TextView>(R.id.tv_range_title)
        val tvRangeValue = itemRoot.findViewById<TextView>(R.id.tv_range_value)
        val viewLineRange = itemRoot.findViewById<View>(R.id.view_line_range)
        val clAverage = itemRoot.findViewById<View>(R.id.cl_average)
        val clExplain = itemRoot.findViewById<View>(R.id.cl_explain)
        val tvAverageValue = itemRoot.findViewById<TextView>(R.id.tv_average_value)
        val tvExplainValue = itemRoot.findViewById<TextView>(R.id.tv_explain_value)
        tvRangeTitle.isVisible =
            if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
        tvRangeValue.isVisible =
            if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
        viewLineRange.isVisible =
            if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
        clAverage.isVisible = (type == TYPE_LINE || type == TYPE_RECT) && tempBean.isAverageOpen()
        clExplain.isVisible = tempBean.isExplainOpen()
        tvRangeTitle.text = rangeTitle
        tvRangeValue.text = rangeValue
        tvAverageValue.text = tempBean.mean_temperature
        tvExplainValue.text = tempBean.comment
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\view\WatermarkView.kt =====

package com.mpdc4gsr.module.thermalunified.report.view

import android.content.Context
import android.graphics.Canvas
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.compat.spToPx

class WatermarkView : View {
    var watermarkText: String? = null
        set(value) {
            field = value
            invalidate()
        }
    private var marginTop: Float = 0f
    private val textPaint: TextPaint = TextPaint()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        marginTop = 220f.dpToPx(context)
        textPaint.isFakeBoldText = true
        textPaint.isAntiAlias = true
        textPaint.color = 0x082b79d8
        textPaint.textSize = 80f.spToPx(context)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        watermarkText?.let {
            var hasAddCount = 0
            var hasUseHeight = 0f
            while (hasUseHeight < height + marginTop) {
                canvas?.save()
                canvas?.rotate(15f)
                val translateX =
                    (width - textPaint.measureText(it)).coerceAtLeast(0f) / 2f + if (hasAddCount % 2 == 0) 100f else 0f
                canvas?.translate(translateX, 0f)
                canvas?.drawText(it, 0f, 0f, textPaint)
                canvas?.restore()
                canvas?.translate(0f, marginTop)
                hasUseHeight += marginTop
                hasAddCount++
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\viewmodel\ModernPdfViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.report.viewmodel

import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtils
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.lms.utils.TLog
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import com.mpdc4gsr.libunified.app.utils.HttpHelp
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import com.mpdc4gsr.module.thermalunified.report.bean.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ModernPdfViewModel : BaseViewModel() {
    // Modern StateFlow-based state management
    private val _reportDataState = MutableStateFlow<ReportDataState>(ReportDataState.Idle)
    val reportDataState: StateFlow<ReportDataState> = _reportDataState.asStateFlow()
    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()

    // One-time events using SharedFlow
    private val _events = MutableSharedFlow<PdfEvent>()
    val events: SharedFlow<PdfEvent> = _events.asSharedFlow()

    // Data classes for type-safe state management
    sealed class ReportDataState {
        object Idle : ReportDataState()
        object Loading : ReportDataState()
        data class Success(val data: ReportData, val isLoadMore: Boolean = false) :
            ReportDataState()

        data class Error(val message: String, val code: Int = -1) : ReportDataState()
        object NoNetwork : ReportDataState()
    }

    data class PaginationState(
        val currentPage: Int = 1,
        val hasMorePages: Boolean = true,
        val totalPages: Int = 0,
        val isLoadingMore: Boolean = false
    )

    sealed class PdfEvent {
        data class ShowToast(val message: String) : PdfEvent()
        data class ShowError(val message: String) : PdfEvent()
        data class NavigateToReport(val reportId: String) : PdfEvent()
        object RefreshCompleted : PdfEvent()
        data class ShareReport(val reportData: ReportData) : PdfEvent()
    }

    // Repository instance
    private val reportRepository = ReportRepository()
    fun getReportData(
        isTC007: Boolean,
        page: Int = 1,
        forceRefresh: Boolean = false
    ) {
        launchWithLoading {
            try {
                // Check network connectivity first
                if (!NetworkUtils.isConnected(ContextProvider.getContext())) {
                    _reportDataState.value = ReportDataState.NoNetwork
                    _events.emit(PdfEvent.ShowError("No network connection available"))
                    return@launchWithLoading
                }
                // Update loading states
                if (page == 1) {
                    _reportDataState.value = ReportDataState.Loading
                } else {
                    _paginationState.value = _paginationState.value.copy(isLoadingMore = true)
                }
                // Fetch data through repository
                val result = reportRepository.getReportData(isTC007, page, forceRefresh)
                when (result) {
                    is BaseRepository.Result.Success -> {
                        val reportData = result.data
                        _reportDataState.value = ReportDataState.Success(
                            data = reportData,
                            isLoadMore = page > 1
                        )
                        // Update pagination state
                        _paginationState.value = _paginationState.value.copy(
                            currentPage = page,
                            hasMorePages = reportData.hasMoreData(),
                            isLoadingMore = false
                        )
                        if (page == 1) {
                            _events.emit(PdfEvent.RefreshCompleted)
                        }
                    }

                    is BaseRepository.Result.Error -> {
                        val errorMessage = result.exception.message ?: "Unknown error occurred"
                        _reportDataState.value = ReportDataState.Error(errorMessage)
                        _paginationState.value = _paginationState.value.copy(isLoadingMore = false)
                        _events.emit(PdfEvent.ShowError(errorMessage))
                    }

                    is BaseRepository.Result.Loading -> {
                        // Already handled above
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to load report data"
                _reportDataState.value = ReportDataState.Error(errorMessage)
                _paginationState.value = _paginationState.value.copy(isLoadingMore = false)
                _events.emit(PdfEvent.ShowError(errorMessage))
            }
        }
    }

    fun loadNextPage(isTC007: Boolean) {
        val currentState = _paginationState.value
        if (currentState.hasMorePages && !currentState.isLoadingMore) {
            getReportData(isTC007, currentState.currentPage + 1)
        }
    }

    fun refreshData(isTC007: Boolean) {
        getReportData(isTC007, 1, forceRefresh = true)
    }

    fun navigateToReport(reportId: String) {
        launchWithErrorHandling {
            _events.emit(PdfEvent.NavigateToReport(reportId))
        }
    }

    fun shareReport(reportData: ReportData) {
        launchWithErrorHandling {
            _events.emit(PdfEvent.ShareReport(reportData))
        }
    }

    fun clearErrorState() {
        super.clearError()
        if (_reportDataState.value is ReportDataState.Error) {
            _reportDataState.value = ReportDataState.Idle
        }
    }

    fun resetStates() {
        _reportDataState.value = ReportDataState.Idle
        _paginationState.value = PaginationState()
    }

    private inner class ReportRepository : BaseRepository() {
        private val cacheKey = "report_data"
        suspend fun getReportData(
            isTC007: Boolean,
            page: Int,
            forceRefresh: Boolean = false
        ): BaseRepository.Result<ReportData> = safeCall {
            val key = "${cacheKey}_${isTC007}_$page"
            if (!forceRefresh) {
                // Try cached data first (5 minute cache)
                getCachedOrExecute(key, 5 * 60 * 1000L) {
                    fetchReportDataFromNetwork(isTC007, page)
                }
            } else {
                // Force refresh - clear cache and fetch
                clearCache(key)
                fetchReportDataFromNetwork(isTC007, page)
            }
        }

        private suspend fun fetchReportDataFromNetwork(
            isTC007: Boolean,
            page: Int
        ): ReportData = suspendCancellableCoroutine { continuation ->
            val downLatch = CountDownLatch(1)
            var result: ReportData? = null
            var error: Exception? = null
            HttpHelp.getFirstReportData(
                isTC007,
                page,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        try {
                            result = if (!response.isNullOrEmpty()) {
                                Gson().fromJson(response, ReportData::class.java)
                            } else {
                                ReportData().apply {
                                    code = -1
                                    msg = "Empty response from server"
                                }
                            }
                        } catch (e: Exception) {
                            error = Exception("JSON parsing error: ${e.message}")
                        } finally {
                            downLatch.countDown()
                        }
                    }

                    override fun onFail(exception: Exception?) {
                        error = exception ?: Exception("Network request failed")
                        result = ReportData().apply {
                            msg = exception?.message ?: "Network error"
                            code = -1
                        }
                        downLatch.countDown()
                        TLog.e("ModernPdfViewModel", "Network error: ${exception?.message}")
                    }

                    override fun onFail(failMsg: String?, errorCode: String) {
                        super.onFail(failMsg, errorCode)
                        // Handle localized error messages
                        try {
                            val localizedMessage = StringUtils.getResString(
                                LMS.mContext,
                                if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt()
                            )
                            // Emit toast event on main thread
                            viewModelScope.launch {
                                _events.emit(PdfEvent.ShowToast(localizedMessage))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        error = Exception(failMsg ?: "Server error")
                        result = ReportData().apply {
                            msg = failMsg
                            code = if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt()
                        }
                        downLatch.countDown()
                    }
                }
            )
            continuation.invokeOnCancellation {
                downLatch.countDown()
            }
            // Wait for network response in IO dispatcher
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    downLatch.await()
                    when {
                        error != null -> continuation.resumeWithException(error!!)
                        result != null -> continuation.resume(result!!)
                        else -> continuation.resumeWithException(Exception("Unknown error occurred"))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    // Extension functions for ReportData
    private fun ReportData.hasMoreData(): Boolean {
        return code == 200 && data?.records?.isNotEmpty() == true && data!!.records!!.size >= 20
    }

    companion object {
        private const val TAG = "ModernPdfViewModel"
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\viewmodel\PdfViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.report.viewmodel

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtils
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.lms.utils.TLog
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.utils.HttpHelp
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import com.mpdc4gsr.module.thermalunified.report.bean.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import com.mpdc4gsr.libunified.R as LibR

class PdfViewModel : BaseViewModel() {
    val listData = MutableLiveData<ReportData?>()
    fun getReportData(
        isTC007: Boolean,
        page: Int,
    ) {
        if (!NetworkUtils.isConnected(ContextProvider.getContext())) {
            TToast.shortToast(ContextProvider.getContext(), LibR.string.http_code_z5004)
            listData.postValue(null)
            return
        }
        viewModelScope.launch {
            val data = getReportDataRepository(isTC007, page)
            listData.postValue(data)
        }
    }

    private suspend fun getReportDataRepository(
        isTC007: Boolean,
        page: Int,
    ): ReportData? {
        var result: ReportData? = null
        val downLatch = CountDownLatch(1)
        HttpHelp.getFirstReportData(
            isTC007,
            page,
            object : IResponseCallback {
                override fun onResponse(p0: String?) {
                    result = Gson().fromJson(p0, ReportData::class.java)
                    downLatch.countDown()
                }

                override fun onFail(p0: Exception?) {
                    result = ReportData()
                    result?.msg = p0?.message
                    result?.code = -1
                    downLatch.countDown()
                    TLog.e("bcf", "ï¼š" + p0?.message)
                }

                override fun onFail(
                    failMsg: String?,
                    errorCode: String,
                ) {
                    super.onFail(failMsg, errorCode)
                    try {
                        StringUtils.getResString(
                            LMS.mContext,
                            if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt(),
                        ).let {
                            TToast.shortToast(LMS.mContext, it)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        )
        withContext(Dispatchers.IO) {
            downLatch.await()
        }
        return result
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\viewmodel\UpReportViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.report.viewmodel

import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.UrlConstants
import com.mpdc4gsr.libunified.app.lms.bean.CommonBean
import com.mpdc4gsr.libunified.app.lms.network.HttpProxy
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.network.ResponseBean
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.mpdc4gsr.module.thermalunified.report.bean.ReportBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch

class UpReportViewModel : BaseViewModel() {
    val commonBeanLD = SingleLiveEvent<CommonBean>()
    val exceptionLD = SingleLiveEvent<Exception?>()
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    fun upload(
        isTC007: Boolean,
        reportBean: ReportBean?,
    ) {
        viewModelScope.launch {
            uploadImages(reportBean)
            uploadJSON(isTC007, reportBean)
        }
    }

    private suspend fun uploadImages(reportBean: ReportBean?) {
        withContext(Dispatchers.IO) {
            val irList = reportBean?.infrared_data
            if (irList != null) {
                val downLatch = CountDownLatch(irList.size)
                for (reportIrBean in irList) {
                    if (reportIrBean.picture_id.isNotEmpty()) {
                        downLatch.countDown()
                        continue
                    }
                    val file = File(reportIrBean.picture_url)
                    LMS.getInstance().uploadFile(file, 0, 13, 20) { response ->
                        try {
                            if (response != null) {
                                val jsonObject = JSONObject(response)
                                val code = jsonObject.optString("code", "")
                                if (code == LMS.SUCCESS) {
                                    file.delete()
                                    val dataObject = jsonObject.optJSONObject("data")
                                    if (dataObject != null) {
                                        reportIrBean.picture_id =
                                            dataObject.optString("fileSecret", "")
                                        reportIrBean.picture_url = dataObject.optString("url", "")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            XLog.e("Error parsing upload response", e)
                        }
                        XLog.i("Upload")
                        downLatch.countDown()
                    }
                }
                downLatch.await()
                XLog.i("${irList.size} Upload")
            }
        }
    }

    private suspend fun uploadJSON(
        isTC007: Boolean,
        reportBean: ReportBean?,
    ) {
        withContext(Dispatchers.IO) {
            val url = UrlConstants.BASE_URL + "api/v1/outProduce/testReport/addTestReport"
            val params = RequestParams()
            params.addBodyParameter("reportType", 2)
            params.addBodyParameter(
                "modelId",
                if (isTC007) 1783 else 950
            )
            params.addBodyParameter("testTime", dateFormat.format(Date()))
            params.addBodyParameter("testInfo", gson.toJson(reportBean))
            params.addBodyParameter("sn", "")
            HttpProxy.getInstant().post(
                url,
                params,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        commonBeanLD.postValue(ResponseBean.convertCommonBean(response, null))
                    }

                    override fun onFail(exception: Exception?) {
                        exceptionLD.postValue(exception)
                    }
                },
            )
        }
    }
}


