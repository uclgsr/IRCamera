package com.mpdc4gsr.component.thermal.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.thermal.viewmodel.ThermalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IRCorrectionComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel = viewModels<ThermalViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedCorrection by remember { mutableIntStateOf(0) }
        var correctionProgress by remember { mutableFloatStateOf(0f) }
        var isProcessing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        LibSharedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Correction",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                selectedCorrection = 0
                                correctionProgress = 0f
                                isProcessing = false
                            }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = Color.White,
                                )
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF16131E),
                            ),
                    )
                },
                containerColor = Color(0xFF16131E),
            ) { paddingValues ->
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(Color(0xFF16131E)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Correction type selector
                    item {
                        CorrectionTypeSelector(
                            selectedCorrection = selectedCorrection,
                            onCorrectionSelected = { selectedCorrection = it },
                        )
                    }
                    // Processing status
                    item {
                        ProcessingStatusCard(
                            isProcessing = isProcessing,
                            progress = correctionProgress,
                            correctionType = getCorrectionTypes()[selectedCorrection],
                        )
                    }
                    // Correction parameters
                    item {
                        CorrectionParametersCard(
                            correctionType = selectedCorrection,
                        )
                    }
                    // Comparison view
                    item {
                        CorrectionComparisonCard()
                    }
                    // Action buttons
                    item {
                        CorrectionActionButtons(
                            isProcessing = isProcessing,
                            onStartCorrection = {
                                isProcessing = true
                                correctionProgress = 0f
                                // Simulate correction process
                                coroutineScope.launch {
                                    for (i in 1..100) {
                                        delay(50L)
                                        correctionProgress = i / 100f
                                    }
                                    isProcessing = false
                                }
                            },
                            onSaveCorrection = { },
                            onDiscardChanges = {
                                correctionProgress = 0f
                                isProcessing = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CorrectionTypeSelector(
    selectedCorrection: Int,
    onCorrectionSelected: (Int) -> Unit,
) {
    val correctionTypes = getCorrectionTypes()
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF21262D),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                "Correction Type",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            correctionTypes.forEachIndexed { index, correction ->
                CorrectionTypeCard(
                    correction = correction,
                    isSelected = selectedCorrection == index,
                    onClick = { onCorrectionSelected(index) },
                )
                if (index < correctionTypes.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CorrectionTypeCard(
    correction: CorrectionType,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFFFF6B35) else Color(0xFF16131E),
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                correction.icon,
                contentDescription = correction.name,
                tint = if (isSelected) Color.White else Color(0xFFFF6B35),
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    correction.name,
                    color = if (isSelected) Color.White else Color(0xFF7D8590),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    correction.description,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF7D8590),
                    fontSize = 12.sp,
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ProcessingStatusCard(
    isProcessing: Boolean,
    progress: Float,
    correctionType: CorrectionType,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF21262D),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Processing Status",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isProcessing) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (isProcessing) "Applying ${correctionType.name}..." else "Ready for correction",
                color = Color(0xFF7D8590),
                fontSize = 14.sp,
            )
            if (isProcessing) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF6B35),
                    trackColor = Color(0xFF16131E),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${(progress * 100).toInt()}% Complete",
                    color = Color(0xFFFF6B35),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun CorrectionParametersCard(correctionType: Int) {
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var gamma by remember { mutableFloatStateOf(1f) }
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF21262D),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                "Correction Parameters",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Brightness adjustment
            ParameterSlider(
                label = "Brightness",
                value = brightness,
                onValueChange = { brightness = it },
                valueRange = -100f..100f,
                unit = "%",
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Contrast adjustment
            ParameterSlider(
                label = "Contrast",
                value = contrast,
                onValueChange = { contrast = it },
                valueRange = -100f..100f,
                unit = "%",
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Gamma adjustment
            ParameterSlider(
                label = "Gamma",
                value = gamma,
                onValueChange = { gamma = it },
                valueRange = 0.1f..3f,
                unit = "",
            )
        }
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = Color.White,
                fontSize = 14.sp,
            )
            Text(
                "${value.toInt()}$unit",
                color = Color(0xFFFF6B35),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors =
                SliderDefaults.colors(
                    thumbColor = Color(0xFFFF6B35),
                    activeTrackColor = Color(0xFFFF6B35),
                    inactiveTrackColor = Color(0xFF16131E),
                ),
        )
    }
}

@Composable
private fun CorrectionComparisonCard() {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF21262D),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                "Before / After Comparison",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Before image
                ComparisonImageCard(
                    title = "Before",
                    description = "Original thermal image",
                    modifier = Modifier.weight(1f),
                )
                // After image
                ComparisonImageCard(
                    title = "After",
                    description = "Corrected thermal image",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ComparisonImageCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF16131E),
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = title,
                        tint = Color(0xFF7D8590),
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        description,
                        color = Color(0xFF7D8590),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CorrectionActionButtons(
    isProcessing: Boolean,
    onStartCorrection: () -> Unit,
    onSaveCorrection: () -> Unit,
    onDiscardChanges: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isProcessing) {
            Button(
                onClick = onStartCorrection,
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35),
                    ),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply Correction")
            }
        }
        Button(
            onClick = onSaveCorrection,
            modifier = Modifier.weight(1f),
            enabled = !isProcessing,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00AA00),
                ),
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = "Save",
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save")
        }
        OutlinedButton(
            onClick = onDiscardChanges,
            modifier = Modifier.weight(1f),
            enabled = !isProcessing,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF7D8590),
                ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590)),
        ) {
            Icon(
                Icons.Default.Cancel,
                contentDescription = "Discard",
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Discard")
        }
    }
}

// Data classes
data class CorrectionType(
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private fun getCorrectionTypes(): List<CorrectionType> =
    listOf(
        CorrectionType(
            "Auto Correct",
            "Automatic thermal image enhancement",
            Icons.Default.AutoFixHigh,
        ),
        CorrectionType(
            "Manual Adjust",
            "Fine-tune brightness, contrast, and gamma",
            Icons.Default.Tune,
        ),
        CorrectionType(
            "Noise Reduction",
            "Remove thermal noise and artifacts",
            Icons.Default.FilterAlt,
        ),
        CorrectionType(
            "Temperature Range",
            "Optimize temperature range display",
            Icons.Default.Straighten,
        ),
    )




