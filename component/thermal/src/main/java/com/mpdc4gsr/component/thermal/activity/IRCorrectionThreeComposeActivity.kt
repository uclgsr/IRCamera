package com.mpdc4gsr.component.thermal.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.thermal.viewmodel.ThermalViewModel

class IRCorrectionThreeComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel = viewModels<ThermalViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        LibSharedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Correction Step 3",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                    )
                },
            ) { paddingValues ->
                CorrectionStep3Content(
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }

    @Composable
    private fun CorrectionStep3Content(modifier: Modifier = Modifier) {
        var colorTemperature by remember { mutableFloatStateOf(6500f) }
        var thermalRange by remember { mutableFloatStateOf(0.8f) }
        var enhancementLevel by remember { mutableFloatStateOf(0.5f) }
        var isProcessing by remember { mutableStateOf(false) }
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Color Calibration Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Color Calibration",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Color Temperature: ${colorTemperature.toInt()}K",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                    Slider(
                        value = colorTemperature,
                        onValueChange = { colorTemperature = it },
                        valueRange = 2700f..6500f,
                        colors =
                            SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35),
                            ),
                    )
                }
            }
            // Thermal Range Optimization
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Thermal Range Enhancement",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Dynamic Range: ${(thermalRange * 100).toInt()}%",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp,
                    )
                    Slider(
                        value = thermalRange,
                        onValueChange = { thermalRange = it },
                        colors =
                            SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35),
                            ),
                    )
                }
            }
            // Quality Enhancement
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Quality Enhancement",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Enhancement Level: ${(enhancementLevel * 100).toInt()}%",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp,
                    )
                    Slider(
                        value = enhancementLevel,
                        onValueChange = { enhancementLevel = it },
                        colors =
                            SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35),
                            ),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { finish() },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF7D8590),
                        ),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { isProcessing = true },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B35),
                        ),
                    enabled = !isProcessing,
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Apply Corrections")
                    }
                }
            }
        }
    }
}




