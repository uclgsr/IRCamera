package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infisense.usbir.view.CameraView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.module.thermalunified.viewmodel.CalibrationStatus
import com.mpdc4gsr.module.thermalunified.viewmodel.CorrectionState
import com.mpdc4gsr.module.thermalunified.viewmodel.IRCorrectionViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.TemperatureData

class IRCorrectionComposeFragment : BaseComposeFragment<IRCorrectionViewModel>() {
    // Compatibility property for legacy code that checks frameReady
    val frameReady: Boolean get() = createViewModel().correctionState.value == CorrectionState.ACTIVE
    override fun createViewModel(): IRCorrectionViewModel {
        return viewModels<IRCorrectionViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRCorrectionViewModel) {
        // Observe ViewModel state
        val correctionState by viewModel.correctionState.collectAsStateWithLifecycle()
        val temperatureData by viewModel.temperatureData.collectAsStateWithLifecycle()
        val calibrationStatus by viewModel.calibrationStatus.collectAsStateWithLifecycle()
        val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar with correction status
                CorrectionStatusBar(
                    correctionState = correctionState,
                    calibrationStatus = calibrationStatus,
                    isProcessing = isProcessing,
                    onToggleCorrection = { viewModel.toggleCorrection() }
                )
                // Main content area
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thermal view area
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        ThermalCorrectionView(
                            temperatureData = temperatureData,
                            onTemperatureUpdate = { temp, x, y ->
                                viewModel.updateTemperaturePoint(temp, x, y)
                            }
                        )
                        // Temperature overlay
                        TemperatureOverlay(
                            temperatureData = temperatureData,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                    // Correction controls panel
                    CorrectionControlsPanel(
                        correctionState = correctionState,
                        onCorrectionValueChange = { value ->
                            viewModel.updateCorrectionValue(value)
                        },
                        onCalibrate = { viewModel.startCalibration() },
                        onReset = { viewModel.resetCorrection() },
                        onSaveSettings = { viewModel.saveSettings() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun CorrectionStatusBar(
        correctionState: CorrectionState,
        calibrationStatus: CalibrationStatus,
        isProcessing: Boolean,
        onToggleCorrection: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (correctionState) {
                    CorrectionState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    CorrectionState.CALIBRATING -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Temperature Correction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getStatusText(correctionState, calibrationStatus),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Switch(
                        checked = correctionState == CorrectionState.ACTIVE,
                        onCheckedChange = { onToggleCorrection() },
                        enabled = !isProcessing
                    )
                }
            }
        }
    }

    @Composable
    private fun ThermalCorrectionView(
        temperatureData: TemperatureData?,
        onTemperatureUpdate: (Float, Int, Int) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Thermal camera view integration
                AndroidView(
                    factory = { context ->
                        TemperatureView(context).apply {
                            // Configure the temperature view
                            setListener(object : TemperatureView.TempListener {
                                override fun getTemp(max: Float, min: Float, tempData: ByteArray) {
                                    // Use max temperature as the representative temperature
                                    // For demo purposes, use center coordinates
                                    onTemperatureUpdate(max, 0, 0)
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Camera view overlay
                AndroidView(
                    factory = { context ->
                        CameraView(context)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )
            }
        }
    }

    @Composable
    private fun TemperatureOverlay(
        temperatureData: TemperatureData?,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            temperatureData?.let { data ->
                TemperatureCard(
                    label = "Current",
                    temperature = "${data.currentTemp}°C",
                    isMain = true
                )
                TemperatureCard(
                    label = "Corrected",
                    temperature = "${data.correctedTemp}°C",
                    color = Color.Green
                )
                TemperatureCard(
                    label = "Offset",
                    temperature = "${data.offsetValue}°C",
                    color = if (data.offsetValue >= 0) MaterialTheme.colorScheme.primary else Color.Red
                )
            }
        }
    }

    @Composable
    private fun TemperatureCard(
        label: String,
        temperature: String,
        isMain: Boolean = false,
        color: Color = Color.White
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMain)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Text(
                    text = temperature,
                    style = if (isMain)
                        MaterialTheme.typography.titleSmall
                    else
                        MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }

    @Composable
    private fun CorrectionControlsPanel(
        correctionState: CorrectionState,
        onCorrectionValueChange: (Float) -> Unit,
        onCalibrate: () -> Unit,
        onReset: () -> Unit,
        onSaveSettings: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Correction Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Correction value slider
                CorrectionValueSlider(
                    onValueChange = onCorrectionValueChange,
                    enabled = correctionState == CorrectionState.ACTIVE
                )
                HorizontalDivider()
                // Calibration section
                CalibrationSection(
                    onCalibrate = onCalibrate,
                    onReset = onReset,
                    enabled = correctionState != CorrectionState.CALIBRATING
                )
                HorizontalDivider()
                // Temperature regions
                TemperatureRegionsSection()
                Spacer(modifier = Modifier.weight(1f))
                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Settings")
                    }
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All")
                    }
                }
            }
        }
    }

    @Composable
    private fun CorrectionValueSlider(
        onValueChange: (Float) -> Unit,
        enabled: Boolean
    ) {
        var sliderValue by remember { mutableFloatStateOf(0f) }
        Column {
            Text(
                text = "Correction Offset: ${String.format("%.1f", sliderValue)}°C",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onValueChange(it)
                },
                valueRange = -10f..10f,
                steps = 39, // 0.5 degree steps
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun CalibrationSection(
        onCalibrate: () -> Unit,
        onReset: () -> Unit,
        enabled: Boolean
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Calibration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCalibrate,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }

    @Composable
    private fun TemperatureRegionsSection() {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Temperature Regions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RegionButton("Point", Icons.Default.Place)
                RegionButton("Line", Icons.Default.Timeline)
                RegionButton("Area", Icons.Default.CropFree)
            }
        }
    }

    @Composable
    private fun RegionButton(
        text: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector
    ) {
        var isSelected by remember { mutableStateOf(false) }
        FilterChip(
            onClick = { isSelected = !isSelected },
            label = { Text(text, style = MaterialTheme.typography.labelSmall) },
            selected = isSelected,
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = text,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }

    private fun getStatusText(
        correctionState: CorrectionState,
        calibrationStatus: CalibrationStatus
    ): String = when {
        correctionState == CorrectionState.CALIBRATING -> "Calibrating..."
        correctionState == CorrectionState.ACTIVE -> "Active - ${calibrationStatus.name.lowercase()}"
        else -> "Inactive"
    }
}