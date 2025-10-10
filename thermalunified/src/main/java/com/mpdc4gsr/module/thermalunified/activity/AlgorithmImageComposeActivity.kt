package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlgorithmImageComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel = viewModels<ThermalViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedAlgorithm by remember { mutableStateOf("Edge Detection") }
        var isProcessing by remember { mutableStateOf(false) }
        var processingProgress by remember { mutableFloatStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Algorithm Processing",
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
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Processed result saved to gallery")
                                }
                            }) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = Color.White,
                                )
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black,
                            ),
                    )
                },
                containerColor = Color.Black,
            ) { paddingValues ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(Color.Black),
                ) {
                    // Image processing view
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(0.7f),
                    ) {
                        // Main image display
                        AlgorithmImageView(
                            selectedAlgorithm = selectedAlgorithm,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Processing indicator
                        if (isProcessing) {
                            ProcessingOverlay(
                                progress = processingProgress,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        // Algorithm info overlay
                        AlgorithmInfoOverlay(
                            selectedAlgorithm = selectedAlgorithm,
                            modifier =
                                Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp),
                        )
                    }
                    // Control panel
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(0.3f)
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Algorithm selection
                        AlgorithmSelector(
                            selectedAlgorithm = selectedAlgorithm,
                            onAlgorithmSelected = { selectedAlgorithm = it },
                        )
                        // Processing controls
                        ProcessingControls(
                            isProcessing = isProcessing,
                            selectedAlgorithm = selectedAlgorithm,
                            coroutineScope = coroutineScope,
                            snackbarHostState = snackbarHostState,
                            onProcess = {
                                isProcessing = true
                                processingProgress = 0f
                                // Simulate processing
                                coroutineScope.launch {
                                    for (i in 1..100) {
                                        delay(50L)
                                        processingProgress = i / 100f
                                    }
                                    isProcessing = false
                                }
                            },
                            onStop = {
                                isProcessing = false
                                processingProgress = 0f
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlgorithmImageView(
    selectedAlgorithm: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF0D1117),
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Default.AutoFixHigh,
                    contentDescription = "Processing",
                    tint = Color(0xFF7D8590),
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    "Thermal Image Processing",
                    color = Color(0xFF7D8590),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Algorithm: $selectedAlgorithm",
                    color = Color(0xFFFF6B35),
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun ProcessingOverlay(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                progress = { progress },
                color = Color(0xFFFF6B35),
                modifier = Modifier.size(48.dp),
            )
            Text(
                "Processing...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${(progress * 100).toInt()}%",
                color = Color(0xFFFF6B35),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun AlgorithmInfoOverlay(
    selectedAlgorithm: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f),
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                "Active Algorithm",
                color = Color(0xFF7D8590),
                fontSize = 10.sp,
            )
            Text(
                selectedAlgorithm,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AlgorithmSelector(
    selectedAlgorithm: String,
    onAlgorithmSelected: (String) -> Unit,
) {
    val algorithms = getAlgorithmOptions()
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
                "Algorithm Selection",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(algorithms) { algorithm ->
                    AlgorithmChip(
                        algorithm = algorithm,
                        isSelected = selectedAlgorithm == algorithm.name,
                        onClick = { onAlgorithmSelected(algorithm.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlgorithmChip(
    algorithm: AlgorithmOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                algorithm.name,
                fontSize = 12.sp,
            )
        },
        selected = isSelected,
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFFF6B35),
                selectedLabelColor = Color.White,
                containerColor = Color(0xFF16131E),
                labelColor = Color(0xFF7D8590),
            ),
    )
}

@Composable
private fun ProcessingControls(
    isProcessing: Boolean,
    selectedAlgorithm: String,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onProcess: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF21262D),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isProcessing) {
                Button(
                    onClick = onProcess,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B35),
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Process",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process Image", fontSize = 14.sp)
                }
            } else {
                Button(
                    onClick = onStop,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop", fontSize = 14.sp)
                }
            }
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Image reset to original state")
                    }
                },
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset", fontSize = 14.sp)
            }
        }
    }
}

// Data classes
data class AlgorithmOption(
    val name: String,
    val description: String,
)

private fun getAlgorithmOptions(): List<AlgorithmOption> =
    listOf(
        AlgorithmOption("Edge Detection", "Detect temperature boundaries"),
        AlgorithmOption("Noise Reduction", "Smooth thermal image"),
        AlgorithmOption("Contrast Enhancement", "Improve image contrast"),
        AlgorithmOption("Temperature Mapping", "Enhanced color mapping"),
        AlgorithmOption("Object Detection", "Identify thermal objects"),
        AlgorithmOption("Pattern Analysis", "Analyze thermal patterns"),
    )
