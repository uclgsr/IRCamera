package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class BaseIRPlushComposeActivity : BaseComposeActivity<ThermalViewModel>() {

    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var plusMode by remember { mutableStateOf("enhanced") }
        var isPlushActive by remember { mutableStateOf(false) }
        var advancedSettings by remember { mutableStateOf(false) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Plus Control",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { advancedSettings = !advancedSettings }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black
                        )
                    )
                },
                containerColor = Color.Black
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.Black)
                ) {
                    // Plus mode status
                    PlusStatusCard(
                        isPlushActive = isPlushActive,
                        plusMode = plusMode,
                        onTogglePlus = { isPlushActive = !isPlushActive },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Main thermal view with plus features
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Enhanced thermal surface
                        PlusThermalSurface(
                            isPlushActive = isPlushActive,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Plus feature overlay
                        if (isPlushActive) {
                            PlusFeatureOverlay(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            )
                        }

                        // Advanced controls
                        PlusControlsOverlay(
                            plusMode = plusMode,
                            onModeChange = { plusMode = it },
                            advancedVisible = advancedSettings,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlusStatusCard(
    isPlushActive: Boolean,
    plusMode: String,
    onTogglePlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plus status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Plus",
                    tint = if (isPlushActive) Color(0xFFFFD700) else Color(0xFF7D8590),
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        "Thermal Plus Mode",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isPlushActive) "Active - $plusMode" else "Inactive",
                        color = if (isPlushActive) Color(0xFFFFD700) else Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }
            }

            // Toggle switch
            Switch(
                checked = isPlushActive,
                onCheckedChange = { onTogglePlus() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFFFD700),
                    uncheckedThumbColor = Color(0xFF7D8590),
                    uncheckedTrackColor = Color(0xFF16131E)
                )
            )
        }
    }
}

@Composable
private fun PlusThermalSurface(
    isPlushActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1117)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    if (isPlushActive) Icons.Default.Star else Icons.Default.Videocam,
                    contentDescription = "Camera",
                    tint = if (isPlushActive) Color(0xFFFFD700) else Color(0xFF7D8590),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    if (isPlushActive) "Enhanced Thermal Plus Feed" else "Standard Thermal Feed",
                    color = if (isPlushActive) Color.White else Color(0xFF7D8590),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isPlushActive) {
                    Text(
                        "Advanced Processing Active",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp
                    )
                }
            }

            // Plus enhancement indicators
            if (isPlushActive) {
                PlusEnhancementIndicators(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PlusEnhancementIndicators(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "PLUS ACTIVE",
                color = Color(0xFFFFD700),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            PlusIndicatorItem("AI Enhancement", true)
            PlusIndicatorItem("Noise Reduction", true)
            PlusIndicatorItem("Edge Detection", true)
            PlusIndicatorItem("Super Resolution", false)
        }
    }
}

@Composable
private fun PlusIndicatorItem(
    feature: String,
    active: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (active) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = feature,
            tint = if (active) Color(0xFF00FF00) else Color(0xFF7D8590),
            modifier = Modifier.size(10.dp)
        )
        Text(
            feature,
            color = if (active) Color.White else Color(0xFF7D8590),
            fontSize = 9.sp
        )
    }
}

@Composable
private fun PlusFeatureOverlay(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Plus Features",
                color = Color(0xFFFFD700),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            PlusFeatureButton(
                icon = Icons.Default.AutoFixHigh,
                text = "Auto Enhance",
                onClick = {  }
            )

            PlusFeatureButton(
                icon = Icons.Default.Tune,
                text = "Manual Tune",
                onClick = {  }
            )

            PlusFeatureButton(
                icon = Icons.Default.Analytics,
                text = "AI Analysis",
                onClick = {  }
            )
        }
    }
}

@Composable
private fun PlusFeatureButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD700).copy(alpha = 0.2f),
            contentColor = Color(0xFFFFD700)
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PlusControlsOverlay(
    plusMode: String,
    onModeChange: (String) -> Unit,
    advancedVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plus mode selector
            PlusModeSelector(
                selectedMode = plusMode,
                onModeSelected = onModeChange
            )

            // Advanced controls (when visible)
            if (advancedVisible) {
                AdvancedPlusControls()
            }

            // Quick actions
            PlusQuickActions()
        }
    }
}

@Composable
private fun PlusModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    val modes = getPlusModes()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Plus Mode",
            color = Color(0xFFFFD700),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { mode ->
                FilterChip(
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                    selected = selectedMode == mode,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFD700),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF21262D),
                        labelColor = Color(0xFF7D8590)
                    )
                )
            }
        }
    }
}

@Composable
private fun AdvancedPlusControls() {
    var aiStrength by remember { mutableFloatStateOf(75f) }
    var noiseReduction by remember { mutableFloatStateOf(50f) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Advanced Controls",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // AI Enhancement Strength
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "AI Enhancement",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    "${aiStrength.toInt()}%",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = aiStrength,
                onValueChange = { aiStrength = it },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFD700),
                    activeTrackColor = Color(0xFFFFD700),
                    inactiveTrackColor = Color(0xFF21262D)
                )
            )
        }

        // Noise Reduction
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Noise Reduction",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    "${noiseReduction.toInt()}%",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = noiseReduction,
                onValueChange = { noiseReduction = it },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFD700),
                    activeTrackColor = Color(0xFFFFD700),
                    inactiveTrackColor = Color(0xFF21262D)
                )
            )
        }
    }
}

@Composable
private fun PlusQuickActions() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {  },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFFD700)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Capture Plus",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Plus Capture", fontSize = 11.sp)
        }

        OutlinedButton(
            onClick = {  },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFFD700)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
        ) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = "Record Plus",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Plus Record", fontSize = 11.sp)
        }

        OutlinedButton(
            onClick = {  },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFFD700)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
        ) {
            Icon(
                Icons.Default.AutoFixHigh,
                contentDescription = "Process Plus",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Process", fontSize = 11.sp)
        }
    }
}

private fun getPlusModes(): List<String> {
    return listOf("enhanced", "precision", "speed", "balanced")
}