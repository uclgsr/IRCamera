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
                        label = { Text("Ambient Temp (°C)") },
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
                        label = { Text("Max Temp (°C)") },
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
                        label = { Text("Min Temp (°C)") },
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
                        label = { Text("Avg Temp (°C)") },
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
                    "${reportPreview.tempRange.first}°C - ${reportPreview.tempRange.second}°C"
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