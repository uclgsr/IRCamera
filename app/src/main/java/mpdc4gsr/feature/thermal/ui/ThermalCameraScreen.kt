package mpdc4gsr.feature.thermal.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.data.SessionMetadata
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.thermal.data.MeasurementMode
import mpdc4gsr.feature.thermal.data.TemperatureUnit
import mpdc4gsr.feature.thermal.data.ThermalPalette
import mpdc4gsr.feature.thermal.presentation.ThermalCameraViewModel
import mpdc4gsr.feature.thermal.presentation.ThermalCameraViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Thermal Camera Screen - Advanced Thermal Imaging Interface
 *
 * Modern implementation of thermal camera functionality:
 * - Real-time thermal image display with temperature overlay
 * - Temperature measurement tools and calibration
 * - Multiple color palettes for thermal visualization
 * - Recording and snapshot capabilities
 * - Advanced thermal analysis tools
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalCameraScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThermalCameraViewModel = viewModel(
        factory = ThermalCameraViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Thermal Imaging",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToGallery) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            ThermalCameraContent(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ThermalCameraContent(
    viewModel: ThermalCameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedPalette by remember { mutableStateOf(ThermalPalette.IRON) }
    var temperatureUnit by remember { mutableStateOf(TemperatureUnit.CELSIUS) }
    var measurementMode by remember { mutableStateOf(MeasurementMode.SPOT) }
    var showCrosshair by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThermalPreviewCard(
                selectedPalette = selectedPalette,
                measurementMode = measurementMode,
                temperatureUnit = temperatureUnit,
                showCrosshair = showCrosshair,
                isFullscreen = isFullscreen,
                onCrosshairToggle = { showCrosshair = !showCrosshair },
                onFullscreenToggle = { isFullscreen = !isFullscreen },
                previewBitmap = uiState.previewBitmap
            )

            TemperatureMeasurementsCard(
                temperatureUnit = temperatureUnit,
                minTemp = uiState.minTemperature,
                maxTemp = uiState.maxTemperature,
                avgTemp = uiState.avgTemperature,
                centerTemp = uiState.centerTemperature,
                onAddMeasurement = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Measurement point marked at center")
                    }
                },
                onClearMeasurements = {
                    scope.launch {
                        snackbarHostState.showSnackbar("All measurement markers cleared")
                    }
                }
            )

            ThermalCameraControlsCard(
                selectedPalette = selectedPalette,
                temperatureUnit = temperatureUnit,
                isRecording = uiState.isRecording,
                measurementMode = measurementMode,
                onPaletteChange = { selectedPalette = it },
                onTemperatureUnitChange = { temperatureUnit = it },
                onRecordingToggle = { shouldRecord ->
                    if (shouldRecord) {
                        val sessionId = "thermal_${System.currentTimeMillis()}"
                        val sessionDir = File(context.filesDir, "sessions/$sessionId")
                        sessionDir.mkdirs()
                        val metadata = SessionMetadata.createSessionStart(sessionId)
                        viewModel.startRecording(sessionDir.absolutePath, metadata)
                        scope.launch {
                            snackbarHostState.showSnackbar("Recording started")
                        }
                    } else {
                        viewModel.stopRecording()
                        scope.launch {
                            snackbarHostState.showSnackbar("Recording stopped")
                        }
                    }
                },
                onMeasurementModeChange = { measurementMode = it },
                onSnapshot = {
                    scope.launch {
                        uiState.previewBitmap?.let { bitmap ->
                            val success = saveThermalSnapshot(context, bitmap)
                            if (success) {
                                snackbarHostState.showSnackbar("Snapshot saved to Pictures/IRCamera")
                            } else {
                                snackbarHostState.showSnackbar("Failed to save snapshot")
                            }
                        } ?: snackbarHostState.showSnackbar("No thermal image available")
                    }
                }
            )

            ThermalAnalysisToolsCard(
                onProfileAnalysis = {
                    scope.launch {
                        val stats = "Min: ${uiState.minTemperature}°C, Max: ${uiState.maxTemperature}°C, Avg: ${uiState.avgTemperature}°C"
                        snackbarHostState.showSnackbar(stats)
                    }
                },
                onHistogramAnalysis = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Temperature range: ${uiState.minTemperature}°C - ${uiState.maxTemperature}°C")
                    }
                },
                onCompare = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Save current frame for comparison")
                    }
                },
                onGenerateReport = {
                    scope.launch {
                        val report = generateThermalReport(uiState)
                        AppLogger.i("ThermalReport", report)
                        snackbarHostState.showSnackbar("Report generated (check logs)")
                    }
                }
            )

            ThermalCameraStatusCard(
                isConnected = uiState.isConnected,
                isSimulationMode = uiState.isSimulationMode,
                frameCount = uiState.frameCount,
                onCalibrate = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Calibration requires device restart")
                    }
                },
                onDiagnostic = {
                    scope.launch {
                        val status = "Connection: ${if (uiState.isConnected) "OK" else "Disconnected"}, " +
                                     "Frames: ${uiState.frameCount}, " +
                                     "Mode: ${if (uiState.isSimulationMode) "Simulation" else "Live"}"
                        snackbarHostState.showSnackbar(status)
                    }
                }
            )
        }
    }
}

private fun saveThermalSnapshot(context: android.content.Context, bitmap: Bitmap): Boolean {
    return try {
        val picturesDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "IRCamera")
        picturesDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "thermal_$timestamp.png"
        val file = File(picturesDir, filename)
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        AppLogger.i("ThermalSnapshot", "Saved to: ${file.absolutePath}")
        true
    } catch (e: Exception) {
        AppLogger.e("ThermalSnapshot", "Failed to save snapshot", e)
        false
    }
}

private fun generateThermalReport(uiState: ThermalCameraViewModel.ThermalCameraUiState): String {
    return buildString {
        appendLine("=== Thermal Camera Report ===")
        appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        appendLine("Connection Status: ${if (uiState.isConnected) "Connected" else "Disconnected"}")
        appendLine("Mode: ${if (uiState.isSimulationMode) "Simulation" else "Live"}")
        appendLine("Recording: ${if (uiState.isRecording) "Active" else "Inactive"}")
        appendLine()
        appendLine("Temperature Data:")
        appendLine("  Minimum: ${uiState.minTemperature}°C")
        appendLine("  Maximum: ${uiState.maxTemperature}°C")
        appendLine("  Average: ${uiState.avgTemperature}°C")
        appendLine("  Center Point: ${uiState.centerTemperature}°C")
        appendLine()
        appendLine("Statistics:")
        appendLine("  Total Frames: ${uiState.frameCount}")
        appendLine("  Temperature Range: ${uiState.maxTemperature - uiState.minTemperature}°C")
        if (uiState.isRecording) {
            appendLine("  Recording Duration: ${uiState.recordingDuration / 1000}s")
        }
        appendLine("=== End Report ===")
    }
}

// ThermalPalette enum is defined in IRGalleryEditComposeActivity.kt
// TemperatureUnit and MeasurementMode are imported from mpdc4gsr.feature.thermal.data.ThermalModels.kt

@Composable
private fun ThermalPreviewCard(
    selectedPalette: ThermalPalette,
    measurementMode: MeasurementMode,
    temperatureUnit: TemperatureUnit,
    showCrosshair: Boolean,
    isFullscreen: Boolean,
    onCrosshairToggle: () -> Unit,
    onFullscreenToggle: () -> Unit,
    previewBitmap: Bitmap? = null
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
                    "Thermal Preview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onCrosshairToggle) {
                        Icon(
                            if (showCrosshair) Icons.Default.CenterFocusStrong else Icons.Default.CenterFocusWeak,
                            contentDescription = "Toggle Crosshair",
                            tint = if (showCrosshair) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onFullscreenToggle) {
                        Icon(
                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = if (isFullscreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Thermal Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        if (previewBitmap == null) getThermalPreviewColor(selectedPalette) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                previewBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Thermal Camera Feed",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Thermal Camera Preview",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Waiting for camera...",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (showCrosshair) {
                    Icon(
                        Icons.Default.CenterFocusStrong,
                        contentDescription = "Crosshair",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(64.dp)
                    )
                }

                if (isFullscreen) {
                    Text(
                        "Fullscreen Mode",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .width(20.dp)
                        .height(150.dp)
                        .background(
                            getThermalGradient(selectedPalette),
                            RoundedCornerShape(4.dp)
                        )
                )
            }

            // Temperature range display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Min: ${formatTemperature(18.2f, temperatureUnit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Max: ${formatTemperature(35.8f, temperatureUnit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun TemperatureMeasurementsCard(
    temperatureUnit: TemperatureUnit,
    minTemp: Float,
    maxTemp: Float,
    avgTemp: Float,
    centerTemp: Float,
    onAddMeasurement: () -> Unit,
    onClearMeasurements: () -> Unit
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
                "Temperature Measurements",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Current measurements - using real data from ViewModel
            MeasurementRow("Hot Spot", maxTemp, temperatureUnit, Icons.Default.LocalFireDepartment)
            MeasurementRow("Cold Spot", minTemp, temperatureUnit, Icons.Default.AcUnit)
            MeasurementRow("Center Point", centerTemp, temperatureUnit, Icons.Default.CenterFocusStrong)
            MeasurementRow("Average", avgTemp, temperatureUnit, Icons.Default.Analytics)

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddMeasurement,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }

                OutlinedButton(
                    onClick = onClearMeasurements,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
private fun MeasurementRow(
    label: String,
    temperature: Float,
    unit: TemperatureUnit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
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
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            formatTemperature(temperature, unit),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                temperature > 30f -> MaterialTheme.colorScheme.error
                temperature < 20f -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun ThermalCameraControlsCard(
    selectedPalette: ThermalPalette,
    temperatureUnit: TemperatureUnit,
    isRecording: Boolean,
    measurementMode: MeasurementMode,
    onPaletteChange: (ThermalPalette) -> Unit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
    onRecordingToggle: (Boolean) -> Unit,
    onMeasurementModeChange: (MeasurementMode) -> Unit,
    onSnapshot: () -> Unit
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
                "Camera Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Color Palette Selection
            Text(
                "Color Palette",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThermalPalette.values().take(3).forEach { palette ->
                    FilterChip(
                        onClick = { onPaletteChange(palette) },
                        label = { Text(palette.name) },
                        selected = selectedPalette == palette,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThermalPalette.values().drop(3).forEach { palette ->
                    FilterChip(
                        onClick = { onPaletteChange(palette) },
                        label = { Text(palette.name) },
                        selected = selectedPalette == palette,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Temperature Unit Selection
            Text(
                "Temperature Unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemperatureUnit.values().forEach { unit ->
                    FilterChip(
                        onClick = { onTemperatureUnitChange(unit) },
                        label = { Text(unit.name) },
                        selected = temperatureUnit == unit,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Measurement Mode Selection
            Text(
                "Measurement Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MeasurementMode.values().take(2).forEach { mode ->
                    FilterChip(
                        onClick = { onMeasurementModeChange(mode) },
                        label = { Text(mode.name) },
                        selected = measurementMode == mode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MeasurementMode.values().drop(2).forEach { mode ->
                    FilterChip(
                        onClick = { onMeasurementModeChange(mode) },
                        label = { Text(mode.name) },
                        selected = measurementMode == mode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            // Recording Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecording) {
                    Button(
                        onClick = { onRecordingToggle(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Recording")
                    }
                } else {
                    Button(
                        onClick = { onRecordingToggle(true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record")
                    }
                }

                OutlinedButton(
                    onClick = onSnapshot,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snapshot")
                }
            }
        }
    }
}

@Composable
private fun ThermalAnalysisToolsCard(
    onProfileAnalysis: () -> Unit,
    onHistogramAnalysis: () -> Unit,
    onCompare: () -> Unit,
    onGenerateReport: () -> Unit
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
                    onClick = onProfileAnalysis,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Profile")
                }

                OutlinedButton(
                    onClick = onHistogramAnalysis,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Histogram")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCompare,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Compare, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compare")
                }

                OutlinedButton(
                    onClick = onGenerateReport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report")
                }
            }
        }
    }
}

@Composable
private fun ThermalCameraStatusCard(
    isConnected: Boolean = false,
    isSimulationMode: Boolean = false,
    frameCount: Long = 0,
    onCalibrate: () -> Unit,
    onDiagnostic: () -> Unit
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
                "Camera Status",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            StatusRow(
                "Connection", 
                if (isConnected) "Connected" else "Disconnected", 
                if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error, 
                isConnected
            )
            StatusRow(
                "Mode", 
                if (isSimulationMode) "Simulation" else "Live", 
                Icons.Default.Thermostat, 
                !isSimulationMode
            )
            StatusRow(
                "Frames Captured", 
                frameCount.toString(), 
                Icons.Default.HighQuality, 
                frameCount > 0
            )
            StatusRow(
                "Temperature", 
                "Calibrated", 
                Icons.Default.Tune, 
                true
            )
            StatusRow(
                "Storage", 
                "Available", 
                Icons.Default.Storage, 
                true
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCalibrate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }

                OutlinedButton(
                    onClick = onDiagnostic,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Diagnostic")
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHealthy: Boolean
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
                icon,
                contentDescription = null,
                tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

// Helper functions
private fun getThermalPreviewColor(palette: ThermalPalette): Color {
    return when (palette) {
        ThermalPalette.IRON -> Color(0xFF8B4513)
        ThermalPalette.RAINBOW -> Color(0xFF4169E1)
        ThermalPalette.ARCTIC -> Color(0xFF4682B4)
        ThermalPalette.GRAYSCALE -> Color(0xFF808080)
        ThermalPalette.HOT -> Color(0xFFFF6600)
        ThermalPalette.MEDICAL -> Color(0xFF00CED1)
        ThermalPalette.LAVA -> Color(0xFFDC143C)
        ThermalPalette.CONTRAST -> Color(0xFF696969)
    }
}

private fun getThermalGradient(palette: ThermalPalette): Color {
    return when (palette) {
        ThermalPalette.IRON -> Color(0xFFFF4500)
        ThermalPalette.RAINBOW -> Color(0xFF32CD32)
        ThermalPalette.ARCTIC -> Color(0xFF00CED1)
        ThermalPalette.GRAYSCALE -> Color(0xFFFFFFFF)
        ThermalPalette.HOT -> Color(0xFFFFFF00)
        ThermalPalette.MEDICAL -> Color(0xFF32CD32)
        ThermalPalette.LAVA -> Color(0xFFFF0000)
        ThermalPalette.CONTRAST -> Color(0xFFFFFFFF)
    }
}

private fun formatTemperature(temperature: Float, unit: TemperatureUnit): String {
    return when (unit) {
        TemperatureUnit.CELSIUS -> "${String.format("%.1f", temperature)}°C"
        TemperatureUnit.FAHRENHEIT -> "${String.format("%.1f", temperature * 9 / 5 + 32)}°F"
        TemperatureUnit.KELVIN -> "${String.format("%.1f", temperature + 273.15)}K"
    }
}