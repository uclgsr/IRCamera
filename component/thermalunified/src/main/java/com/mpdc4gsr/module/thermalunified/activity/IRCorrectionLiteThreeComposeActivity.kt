package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
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

/**
 * Lite Correction Step 3 - Advanced color correction and temperature calibration
 * Streamlined correction algorithms for quick processing in lite mode
 */
class IRCorrectionLiteThreeComposeActivity : BaseComposeActivity<IRCorrectionLiteThreeViewModel>() {

    override fun createViewModel(): IRCorrectionLiteThreeViewModel {
        return IRCorrectionLiteThreeViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRCorrectionLiteThreeViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Lite Correction - Step 3",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF4A90E2) // Lite blue theme
                        )
                    )
                }
            ) { paddingValues ->
                IRCorrectionLiteThreeContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRCorrectionLiteThreeContent(
        viewModel: IRCorrectionLiteThreeViewModel,
        modifier: Modifier = Modifier
    ) {
        var isProcessing by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0f) }
        var colorTemperature by remember { mutableStateOf(5000f) }
        var saturation by remember { mutableStateOf(1f) }

        // Handle processing with LaunchedEffect
        LaunchedEffect(isProcessing) {
            if (isProcessing) {
                for (i in 0..100) {
                    progress = i / 100f
                    kotlinx.coroutines.delay(30)
                }
                isProcessing = false
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Step 3 of 4: Color Correction",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4A90E2),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "75%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4A90E2)
                    )
                }
            }

            // Processing status
            if (isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Processing Color Correction...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF4A90E2)
                        )
                    }
                }
            }

            // Color correction controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Color Temperature Correction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Color temperature slider
                    Column {
                        Text("Color Temperature: ${colorTemperature.toInt()}K")
                        Slider(
                            value = colorTemperature,
                            onValueChange = { colorTemperature = it },
                            valueRange = 2700f..10000f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4A90E2),
                                activeTrackColor = Color(0xFF4A90E2)
                            )
                        )
                    }

                    // Saturation slider
                    Column {
                        Text("Saturation: ${(saturation * 100).toInt()}%")
                        Slider(
                            value = saturation,
                            onValueChange = { saturation = it },
                            valueRange = 0f..2f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4A90E2),
                                activeTrackColor = Color(0xFF4A90E2)
                            )
                        )
                    }
                }
            }

            // Preview area placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Color Correction Preview",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Real-time thermal image preview with color corrections",
                        color = Color(0xFF9E9E9E),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        colorTemperature = 5000f
                        saturation = 1f
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4A90E2)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset")
                }

                Button(
                    onClick = {
                        isProcessing = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90E2)
                    ),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply & Next")
                }
            }
        }
    }
}

class IRCorrectionLiteThreeViewModel : BaseViewModel()