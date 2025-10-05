package com.mpdc4gsr.module.thermalunified.fragment

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.module.thermalunified.viewmodel.IRPlushViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.IRPlushViewModel.*

class IRPlushComposeFragment : BaseComposeFragment<IRPlushViewModel>() {

    override fun createViewModel(): IRPlushViewModel {
        return viewModels<IRPlushViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRPlushViewModel) {
        // Observe ViewModel state
        val dualViewState by viewModel.dualViewState.collectAsStateWithLifecycle()
        val temperatureData by viewModel.temperatureData.collectAsStateWithLifecycle()
        val processingMode by viewModel.processingMode.collectAsStateWithLifecycle()
        val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()

        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Status bar with IR Plus features
                IRPlusStatusBar(
                    dualViewState = dualViewState,
                    processingMode = processingMode,
                    isRecording = isRecording,
                    onToggleRecording = { viewModel.toggleRecording() }
                )

                // Main dual-view interface
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dual camera view
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        DualCameraView(
                            dualViewState = dualViewState,
                            onSurfaceReady = { surfaceView ->
                                viewModel.initializeDualView(surfaceView)
                            }
                        )

                        // Temperature overlays
                        TemperatureOverlays(
                            temperatureData = temperatureData,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )

                        // Plus features overlay
                        PlusFeatureOverlay(
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }

                    // Controls panel
                    IRPlusControlsPanel(
                        dualViewState = dualViewState,
                        processingMode = processingMode,
                        onModeChange = { mode ->
                            viewModel.changeProcessingMode(mode)
                        },
                        onCalibrate = { viewModel.calibrateDualView() },
                        onResetSettings = { viewModel.resetSettings() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun IRPlusStatusBar(
        dualViewState: DualViewState,
        processingMode: ProcessingMode,
        isRecording: Boolean,
        onToggleRecording: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (dualViewState) {
                    DualViewState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    DualViewState.CALIBRATING -> MaterialTheme.colorScheme.secondaryContainer
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
                        text = "IR Plus - Dual Camera Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusChip(
                            text = getDualViewStatusText(dualViewState),
                            color = getDualViewStatusColor(dualViewState)
                        )
                        StatusChip(
                            text = processingMode.name,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (isRecording) {
                            StatusChip(
                                text = "RECORDING",
                                color = Color.Red
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Recording button
                    IconButton(
                        onClick = onToggleRecording,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = "Toggle Recording",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun DualCameraView(
        dualViewState: DualViewState,
        onSurfaceReady: (SurfaceView) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dual surface view for native camera
                AndroidView(
                    factory = { context ->
                        SurfaceView(context).apply {
                            onSurfaceReady(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Temperature view overlay
                AndroidView(
                    factory = { context ->
                        TemperatureView(context).apply {
                            // Configure for dual IR mode
                            productType = com.mpdc4gsr.libunified.ir.usbdual.Const.TYPE_IR_DUAL
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Processing status overlay
                if (dualViewState == DualViewState.CALIBRATING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Calibrating Dual View...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Please keep the camera steady",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TemperatureOverlays(
        temperatureData: TemperatureData?,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            temperatureData?.let { data ->
                TemperatureCard(
                    label = "IR Center",
                    temperature = "${data.irCenterTemp}°C",
                    isMain = true
                )

                TemperatureCard(
                    label = "IR Max",
                    temperature = "${data.irMaxTemp}°C",
                    color = Color.Red
                )

                TemperatureCard(
                    label = "IR Min",
                    temperature = "${data.irMinTemp}°C",
                    color = MaterialTheme.colorScheme.primary
                )

                TemperatureCard(
                    label = "Ambient",
                    temperature = "${data.ambientTemp}°C",
                    color = Color.Green
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
    private fun PlusFeatureOverlay(
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Plus",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "PLUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    @Composable
    private fun IRPlusControlsPanel(
        dualViewState: DualViewState,
        processingMode: ProcessingMode,
        onModeChange: (ProcessingMode) -> Unit,
        onCalibrate: () -> Unit,
        onResetSettings: () -> Unit,
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
                    text = "IR Plus Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Processing mode selector
                ProcessingModeSelector(
                    currentMode = processingMode,
                    onModeChange = onModeChange,
                    enabled = dualViewState == DualViewState.ACTIVE
                )

                HorizontalDivider()

                // Calibration section
                CalibrationSection(
                    onCalibrate = onCalibrate,
                    onReset = onResetSettings,
                    enabled = dualViewState != DualViewState.CALIBRATING
                )

                HorizontalDivider()

                // Plus features
                PlusFeaturesSection()

                Spacer(modifier = Modifier.weight(1f))

                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Advanced Settings")
                    }
                }
            }
        }
    }

    @Composable
    private fun ProcessingModeSelector(
        currentMode: ProcessingMode,
        onModeChange: (ProcessingMode) -> Unit,
        enabled: Boolean
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Processing Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            ProcessingMode.values().forEach { mode ->
                FilterChip(
                    onClick = { if (enabled) onModeChange(mode) },
                    label = { Text(mode.displayName) },
                    selected = currentMode == mode,
                    enabled = enabled,
                    leadingIcon = {
                        Icon(
                            mode.icon,
                            contentDescription = mode.displayName,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
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
    private fun PlusFeaturesSection() {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Plus Features",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            val features = listOf(
                "Dual-IR Processing" to true,
                "Advanced Fusion" to true,
                "Professional Analysis" to false,
                "Enhanced Calibration" to true
            )

            features.forEach { (feature, enabled) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        if (enabled) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = if (enabled) "Enabled" else "Locked",
                        modifier = Modifier.size(16.dp),
                        tint = if (enabled) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Helper functions
    private fun getDualViewStatusText(state: DualViewState): String = when (state) {
        DualViewState.INACTIVE -> "Inactive"
        DualViewState.ACTIVE -> "Active"
        DualViewState.CALIBRATING -> "Calibrating"
        DualViewState.ERROR -> "Error"
    }

    private fun getDualViewStatusColor(state: DualViewState): Color = when (state) {
        DualViewState.INACTIVE -> Color.Gray
        DualViewState.ACTIVE -> Color.Green
        DualViewState.CALIBRATING -> Color(0xFFFFA500)
        DualViewState.ERROR -> Color.Red
    }

}