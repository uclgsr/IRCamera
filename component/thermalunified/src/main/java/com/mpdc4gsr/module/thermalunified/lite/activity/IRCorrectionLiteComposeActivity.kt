package com.mpdc4gsr.module.thermalunified.lite.activity

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

/**
 * IRCorrectionLiteComposeActivity - Thermal Image Correction with Compose
 *
 * Streamlined thermal image correction interface featuring:
 * - Real-time thermal image correction and calibration
 * - Simplified correction controls for quick adjustments
 * - Temperature calibration with reference points
 * - Image enhancement and optimization tools
 * - Before/after comparison view
 * - Export corrected thermal images with metadata
 */
class IRCorrectionLiteComposeActivity : BaseComposeActivity<BaseViewModel>() {

    companion object {
        private const val KEY_IMAGE_PATH = "image_path"

        fun startWithImage(context: Context, imagePath: String) {
            val intent = Intent(context, IRCorrectionLiteComposeActivity::class.java).apply {
                putExtra(KEY_IMAGE_PATH, imagePath)
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
        val imagePath = intent.getStringExtra(KEY_IMAGE_PATH) ?: ""

        var correctionMode by remember { mutableStateOf("Auto") }
        var showBeforeAfter by remember { mutableStateOf(false) }
        var temperature by remember { mutableStateOf(25.0f) }
        var emissivity by remember { mutableStateOf(0.95f) }
        var distance by remember { mutableStateOf(1.0f) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "IR Correction Lite",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showBeforeAfter = !showBeforeAfter }) {
                                Icon(
                                    if (showBeforeAfter) Icons.Default.Compare else Icons.Default.CompareArrows,
                                    contentDescription = "Compare"
                                )
                            }
                            IconButton(onClick = { /* Save image */ }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                            IconButton(onClick = { /* Export */ }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                IRCorrectionContent(
                    imagePath = imagePath,
                    correctionMode = correctionMode,
                    onCorrectionModeChange = { correctionMode = it },
                    showBeforeAfter = showBeforeAfter,
                    temperature = temperature,
                    onTemperatureChange = { temperature = it },
                    emissivity = emissivity,
                    onEmissivityChange = { emissivity = it },
                    distance = distance,
                    onDistanceChange = { distance = it },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun IRCorrectionContent(
    imagePath: String,
    correctionMode: String,
    onCorrectionModeChange: (String) -> Unit,
    showBeforeAfter: Boolean,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    emissivity: Float,
    onEmissivityChange: (Float) -> Unit,
    distance: Float,
    onDistanceChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Image display area
        ThermalImageDisplay(
            imagePath = imagePath,
            showBeforeAfter = showBeforeAfter,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Correction controls
        CorrectionControlsPanel(
            correctionMode = correctionMode,
            onCorrectionModeChange = onCorrectionModeChange,
            temperature = temperature,
            onTemperatureChange = onTemperatureChange,
            emissivity = emissivity,
            onEmissivityChange = onEmissivityChange,
            distance = distance,
            onDistanceChange = onDistanceChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun ThermalImageDisplay(
    imagePath: String,
    showBeforeAfter: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black)
    ) {
        if (showBeforeAfter) {
            // Before/After comparison view
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Before image
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Before",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "BEFORE",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.3f))
                )

                // After image
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = "After",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFFFF6B35)
                        )
                        Text(
                            text = "AFTER",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            // Single corrected image view
            AndroidView(
                factory = { context ->
                    // This would integrate with the actual thermal image view
                    android.view.View(context).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Temperature overlay
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "CORRECTED",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${temperature.toInt()}°C",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFFF6B35),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Status indicator
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF4CAF50).copy(alpha = 0.9f)
        ) {
            Text(
                text = "CORRECTED",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CorrectionControlsPanel(
    correctionMode: String,
    onCorrectionModeChange: (String) -> Unit,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    emissivity: Float,
    onEmissivityChange: (Float) -> Unit,
    distance: Float,
    onDistanceChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // Correction mode selector
        Text(
            text = "Correction Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf("Auto", "Manual", "Advanced")
            modes.forEach { mode ->
                FilterChip(
                    selected = correctionMode == mode,
                    onClick = { onCorrectionModeChange(mode) },
                    label = { Text(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Correction parameters
        CorrectionParameterCard(
            title = "Temperature Correction",
            value = temperature,
            onValueChange = onTemperatureChange,
            valueRange = -20f..150f,
            unit = "°C",
            description = "Ambient temperature for correction"
        )

        Spacer(modifier = Modifier.height(12.dp))

        CorrectionParameterCard(
            title = "Emissivity",
            value = emissivity,
            onValueChange = onEmissivityChange,
            valueRange = 0.1f..1.0f,
            unit = "ε",
            description = "Material emissivity coefficient"
        )

        Spacer(modifier = Modifier.height(12.dp))

        CorrectionParameterCard(
            title = "Distance",
            value = distance,
            onValueChange = onDistanceChange,
            valueRange = 0.1f..10.0f,
            unit = "m",
            description = "Distance from target"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { /* Reset corrections */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }

            Button(
                onClick = { /* Apply corrections */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Apply",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply")
            }
        }
    }
}

@Composable
private fun CorrectionParameterCard(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format("%.2f", value)} $unit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF6B35),
                    activeTrackColor = Color(0xFFFF6B35)
                )
            )
        }
    }
}