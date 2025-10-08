package mpdc4gsr.feature.thermal.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.thermal.data.MeasurementMode
import mpdc4gsr.feature.thermal.data.TemperatureUnit
import mpdc4gsr.feature.thermal.data.ThermalPalette
import mpdc4gsr.feature.thermal.presentation.ThermalCameraViewModel
import mpdc4gsr.feature.thermal.presentation.ThermalUiState
import mpdc4gsr.feature.thermal.presentation.ThermalUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalCameraScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThermalCameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
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
            when (uiState) {
                is ThermalUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ThermalUiState.Error -> {
                    ThermalErrorContent(
                        error = uiState as ThermalUiState.Error,
                        onRetry = { viewModel.onEvent(ThermalUiEvent.ClearError) },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                is ThermalUiState.Success -> {
                    ThermalCameraContent(
                        uiState = uiState as ThermalUiState.Success,
                        onEvent = viewModel::onEvent,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalErrorContent(
    error: ThermalUiState.Error,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error.message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (error.isRecoverable) {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ThermalCameraContent(
    uiState: ThermalUiState.Success,
    onEvent: (ThermalUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPalette by remember { mutableStateOf(ThermalPalette.IRON) }
    var temperatureUnit by remember { mutableStateOf(TemperatureUnit.CELSIUS) }
    var measurementMode by remember { mutableStateOf(MeasurementMode.SPOT) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Thermal Preview Area
        ThermalPreviewCard(
            uiState = uiState,
            selectedPalette = selectedPalette,
            measurementMode = measurementMode,
            temperatureUnit = temperatureUnit
        )
        // Temperature Measurements
        TemperatureMeasurementsCard(
            uiState = uiState,
            temperatureUnit = temperatureUnit
        )
        // Camera Controls
        ThermalCameraControlsCard(
            uiState = uiState,
            selectedPalette = selectedPalette,
            temperatureUnit = temperatureUnit,
            measurementMode = measurementMode,
            onPaletteChange = { selectedPalette = it },
            onTemperatureUnitChange = { temperatureUnit = it },
            onStartRecording = { onEvent(ThermalUiEvent.StartRecording) },
            onStopRecording = { onEvent(ThermalUiEvent.StopRecording) },
            onCaptureSnapshot = { onEvent(ThermalUiEvent.CaptureSnapshot) },
            onMeasurementModeChange = { measurementMode = it }
        )
        // Analysis Tools
        ThermalAnalysisToolsCard()
        // Camera Status
        ThermalCameraStatusCard(
            uiState = uiState,
            onConnectCamera = { onEvent(ThermalUiEvent.ConnectCamera) },
            onDisconnectCamera = { onEvent(ThermalUiEvent.DisconnectCamera) }
        )
    }
}

// ThermalPalette enum is defined in IRGalleryEditComposeActivity.kt
// TemperatureUnit and MeasurementMode are imported from mpdc4gsr.feature.thermal.data.ThermalModels.kt
@Composable
private fun ThermalPreviewCard(
    uiState: ThermalUiState.Success,
    selectedPalette: ThermalPalette,
    measurementMode: MeasurementMode,
    temperatureUnit: TemperatureUnit
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
                    var showCrosshair by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        showCrosshair = !showCrosshair
                        // TODO: Toggle crosshair overlay on thermal image
                    }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Crosshair")
                    }
                    var isFullscreen by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        isFullscreen = !isFullscreen
                        // TODO: Toggle fullscreen mode
                    }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                    }
                }
            }
            // Thermal Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        getThermalPreviewColor(selectedPalette),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Thermostat,
                        contentDescription = "Thermal Camera",
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
                        "Palette: ${selectedPalette.name}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    // Temperature overlay
                    when (measurementMode) {
                        MeasurementMode.SPOT -> {
                            Text(
                                "Center Point: ${formatTemperature(25.6f, temperatureUnit)}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        MeasurementMode.LINE -> {
                            Text(
                                "Line Profile: Max ${formatTemperature(31.2f, temperatureUnit)}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        MeasurementMode.AREA -> {
                            Text(
                                "Area Avg: ${formatTemperature(27.8f, temperatureUnit)}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        MeasurementMode.CONTINUOUS -> {
                            Text(
                                "Continuous: ${formatTemperature(30.0f, temperatureUnit)}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                // Temperature scale indicator
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
    uiState: ThermalUiState.Success,
    temperatureUnit: TemperatureUnit
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
            // Current measurements
            MeasurementRow("Hot Spot", uiState.maxTemperature, temperatureUnit, Icons.Default.LocalFireDepartment)
            MeasurementRow("Cold Spot", uiState.minTemperature, temperatureUnit, Icons.Default.AcUnit)
            MeasurementRow("Center Point", uiState.centerTemperature, temperatureUnit, Icons.Default.CenterFocusStrong)
            MeasurementRow("Average", uiState.avgTemperature, temperatureUnit, Icons.Default.Analytics)
            HorizontalDivider()
            // Measurement controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        // TODO: Add measurement point on thermal image
                        android.widget.Toast.makeText(
                            context,
                            "Add measurement feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Measurement")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Clear all measurements
                        android.widget.Toast.makeText(
                            context,
                            "Clear measurements feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Measurements")
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
                contentDescription = label,
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
    uiState: ThermalUiState.Success,
    selectedPalette: ThermalPalette,
    temperatureUnit: TemperatureUnit,
    measurementMode: MeasurementMode,
    onPaletteChange: (ThermalPalette) -> Unit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCaptureSnapshot: () -> Unit,
    onMeasurementModeChange: (MeasurementMode) -> Unit
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
                if (uiState.isRecording) {
                    Button(
                        onClick = onStopRecording,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Recording")
                    }
                } else {
                    Button(
                        onClick = onStartRecording,
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isConnected
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = "Start Recording")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record")
                    }
                }
                OutlinedButton(
                    onClick = onCaptureSnapshot,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.isConnected
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture Snapshot")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snapshot")
                }
            }
        }
    }
}

@Composable
private fun ThermalAnalysisToolsCard() {
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
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Show temperature profile analysis
                        android.widget.Toast.makeText(
                            context,
                            "Temperature profile feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Temperature Profile")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Profile")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Show histogram analysis
                        android.widget.Toast.makeText(
                            context,
                            "Histogram analysis feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = "Histogram Analysis")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Histogram")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Compare thermal images
                        android.widget.Toast.makeText(
                            context,
                            "Thermal comparison feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Compare, contentDescription = "Compare Images")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compare")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Generate thermal report
                        android.widget.Toast.makeText(
                            context,
                            "Generate report feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = "Generate Report")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report")
                }
            }
        }
    }
}

@Composable
private fun ThermalCameraStatusCard(
    uiState: ThermalUiState.Success,
    onConnectCamera: () -> Unit,
    onDisconnectCamera: () -> Unit
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
                if (uiState.isConnected) "Connected" else "Disconnected", 
                Icons.Default.CheckCircle, 
                uiState.isConnected
            )
            StatusRow(
                "Mode", 
                if (uiState.isSimulationMode) "Simulation" else "Hardware", 
                Icons.Default.Thermostat, 
                !uiState.isSimulationMode
            )
            StatusRow(
                "Recording", 
                if (uiState.isRecording) "Active" else "Inactive", 
                Icons.Default.FiberManualRecord, 
                uiState.isRecording
            )
            StatusRow(
                "Frames", 
                "${uiState.frameCount}", 
                Icons.Default.PhotoLibrary, 
                true
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isConnected) {
                    OutlinedButton(
                        onClick = onDisconnectCamera,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = "Disconnect Camera")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = onConnectCamera,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = "Connect Camera")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                }
                OutlinedButton(
                    onClick = {
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = "Run Diagnostic")
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
                contentDescription = label,
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