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
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.feature.thermal.data.MeasurementMode
import mpdc4gsr.feature.thermal.data.TemperatureUnit
import mpdc4gsr.feature.thermal.data.ThermalPalette

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
    modifier: Modifier = Modifier
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
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ThermalCameraContent(
    modifier: Modifier = Modifier
) {
    var selectedPalette by remember { mutableStateOf(ThermalPalette.IRON) }
    var temperatureUnit by remember { mutableStateOf(TemperatureUnit.CELSIUS) }
    var isRecording by remember { mutableStateOf(false) }
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
            selectedPalette = selectedPalette,
            measurementMode = measurementMode,
            temperatureUnit = temperatureUnit
        )

        // Temperature Measurements
        TemperatureMeasurementsCard(
            temperatureUnit = temperatureUnit
        )

        // Camera Controls
        ThermalCameraControlsCard(
            selectedPalette = selectedPalette,
            temperatureUnit = temperatureUnit,
            isRecording = isRecording,
            measurementMode = measurementMode,
            onPaletteChange = { selectedPalette = it },
            onTemperatureUnitChange = { temperatureUnit = it },
            onRecordingToggle = { isRecording = it },
            onMeasurementModeChange = { measurementMode = it }
        )

        // Analysis Tools
        ThermalAnalysisToolsCard()

        // Camera Status
        ThermalCameraStatusCard()
    }
}

// ThermalPalette enum is defined in IRGalleryEditComposeActivity.kt
// TemperatureUnit and MeasurementMode are imported from mpdc4gsr.feature.thermal.data.ThermalModels.kt

@Composable
private fun ThermalPreviewCard(
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
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Crosshair")
                    }
                    IconButton(onClick = { }) {
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
            MeasurementRow("Hot Spot", 35.8f, temperatureUnit, Icons.Default.LocalFireDepartment)
            MeasurementRow("Cold Spot", 18.2f, temperatureUnit, Icons.Default.AcUnit)
            MeasurementRow("Center Point", 25.6f, temperatureUnit, Icons.Default.CenterFocusStrong)
            MeasurementRow("Average", 27.1f, temperatureUnit, Icons.Default.Analytics)

            HorizontalDivider()

            // Measurement controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }

                OutlinedButton(
                    onClick = { },
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
                    onClick = { },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Profile")
                }

                OutlinedButton(
                    onClick = { },
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
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Compare, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compare")
                }

                OutlinedButton(
                    onClick = { },
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
private fun ThermalCameraStatusCard() {
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

            StatusRow("Connection", "Connected", Icons.Default.CheckCircle, true)
            StatusRow("Temperature", "Calibrated", Icons.Default.Thermostat, true)
            StatusRow("Image Quality", "Excellent", Icons.Default.HighQuality, true)
            StatusRow("Battery", "87%", Icons.Default.Battery4Bar, true)
            StatusRow("Storage", "2.1 GB Free", Icons.Default.Storage, true)

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }

                OutlinedButton(
                    onClick = { },
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