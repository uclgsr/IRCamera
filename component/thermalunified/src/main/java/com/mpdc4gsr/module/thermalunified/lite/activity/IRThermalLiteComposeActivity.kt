package com.mpdc4gsr.module.thermalunified.lite.activity

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

/**
 * IRThermalLiteComposeActivity - Lightweight Thermal Camera Interface with Compose
 *
 * Streamlined thermal camera interface featuring:
 * - Optimized thermal image capture with minimal resource usage
 * - Simplified controls for quick thermal imaging operations
 * - Real-time temperature monitoring with basic analysis
 * - Lightweight recording and capture capabilities
 * - Fast thermal image processing and display
 * - Essential thermal measurement tools
 */
class IRThermalLiteComposeActivity : BaseComposeActivity<BaseViewModel>() {

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, IRThermalLiteComposeActivity::class.java))
        }
    }

    override fun createViewModel(): BaseViewModel {
        return viewModels<BaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: BaseViewModel) {
        var isCapturing by remember { mutableStateOf(false) }
        var currentTemp by remember { mutableStateOf(25.6f) }
        var showSettings by remember { mutableStateOf(false) }
        var captureMode by remember { mutableStateOf("Auto") }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "IR Thermal Lite",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = { /* Switch mode */ }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Mode")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                ThermalLiteContent(
                    isCapturing = isCapturing,
                    onCaptureToggle = { isCapturing = !isCapturing },
                    currentTemp = currentTemp,
                    captureMode = captureMode,
                    onCaptureModeChange = { captureMode = it },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        if (showSettings) {
            ThermalLiteSettingsDialog(
                onDismiss = { showSettings = false },
                onSaveSettings = { settings ->
                    // Apply settings
                    showSettings = false
                }
            )
        }
    }
}

@Composable
private fun ThermalLiteContent(
    isCapturing: Boolean,
    onCaptureToggle: () -> Unit,
    currentTemp: Float,
    captureMode: String,
    onCaptureModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Thermal camera preview
        ThermalLitePreview(
            isCapturing = isCapturing,
            currentTemp = currentTemp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Quick controls
        ThermalLiteControls(
            isCapturing = isCapturing,
            onCaptureToggle = onCaptureToggle,
            captureMode = captureMode,
            onCaptureModeChange = onCaptureModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun ThermalLitePreview(
    isCapturing: Boolean,
    currentTemp: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black)
    ) {
        // Thermal surface view placeholder
        AndroidView(
            factory = { context ->
                // This would integrate with the actual thermal camera surface
                android.view.View(context).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Minimal temperature display
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Text(
                text = "${currentTemp}°C",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp)
            )
        }

        // Capture indicator
        if (isCapturing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color(0xFFE53E3E),
                        CircleShape
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = "●",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Center crosshair
        Icon(
            Icons.Default.MyLocation,
            contentDescription = "Center point",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
        )

        // Quick temperature range
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "RANGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "H: 42.1°C",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = "L: 18.3°C",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ThermalLiteControls(
    isCapturing: Boolean,
    onCaptureToggle: () -> Unit,
    captureMode: String,
    onCaptureModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Mode selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf("Auto", "Manual", "Continuous")
            modes.forEach { mode ->
                FilterChip(
                    selected = captureMode == mode,
                    onClick = { onCaptureModeChange(mode) },
                    label = { Text(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Main controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery shortcut
            OutlinedButton(
                onClick = { /* Open gallery */ },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery"
                )
            }

            // Main capture button
            Button(
                onClick = onCaptureToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing) Color(0xFFE53E3E) else Color(0xFFFF6B35)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = if (isCapturing) Icons.Default.Stop else Icons.Default.Camera,
                    contentDescription = if (isCapturing) "Stop" else "Capture",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCapturing) "STOP" else "CAPTURE",
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick settings
            OutlinedButton(
                onClick = { /* Quick settings */ },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Quick Settings"
                )
            }
        }

        // Quick info bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickInfoItem(
                    label = "Mode",
                    value = captureMode,
                    icon = Icons.Default.CameraAlt
                )
                QuickInfoItem(
                    label = "Status",
                    value = if (isCapturing) "Active" else "Ready",
                    icon = Icons.Default.Sensors
                )
                QuickInfoItem(
                    label = "Quality",
                    value = "High",
                    icon = Icons.Default.HighQuality
                )
            }
        }
    }
}

@Composable
private fun QuickInfoItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThermalLiteSettingsDialog(
    onDismiss: () -> Unit,
    onSaveSettings: (Map<String, Any>) -> Unit
) {
    var imageQuality by remember { mutableStateOf("High") }
    var autoSave by remember { mutableStateOf(true) }
    var temperatureUnit by remember { mutableStateOf("Celsius") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thermal Lite Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Image Quality",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Standard", "High", "Maximum").forEach { quality ->
                        FilterChip(
                            selected = imageQuality == quality,
                            onClick = { imageQuality = quality },
                            label = { Text(quality) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoSave,
                        onCheckedChange = { autoSave = it }
                    )
                    Text(
                        text = "Auto-save captures",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Temperature Unit",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Celsius", "Fahrenheit").forEach { unit ->
                        FilterChip(
                            selected = temperatureUnit == unit,
                            onClick = { temperatureUnit = unit },
                            label = { Text(unit) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveSettings(
                        mapOf(
                            "imageQuality" to imageQuality,
                            "autoSave" to autoSave,
                            "temperatureUnit" to temperatureUnit
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}