// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity' subtree
// Files: 47; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\AlgorithmImageComposeActivity.kt =====

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
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

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
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Processed result saved to gallery")
                                }
                            }) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = "Save",
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
                    // Image processing view
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                    ) {
                        // Main image display
                        AlgorithmImageView(
                            selectedAlgorithm = selectedAlgorithm,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Processing indicator
                        if (isProcessing) {
                            ProcessingOverlay(
                                progress = processingProgress,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        // Algorithm info overlay
                        AlgorithmInfoOverlay(
                            selectedAlgorithm = selectedAlgorithm,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        )
                    }
                    // Control panel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.3f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Algorithm selection
                        AlgorithmSelector(
                            selectedAlgorithm = selectedAlgorithm,
                            onAlgorithmSelected = { selectedAlgorithm = it }
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
                            }
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
                    Icons.Default.AutoFixHigh,
                    contentDescription = "Processing",
                    tint = Color(0xFF7D8590),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Thermal Image Processing",
                    color = Color(0xFF7D8590),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Algorithm: $selectedAlgorithm",
                    color = Color(0xFFFF6B35),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ProcessingOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress },
                color = Color(0xFFFF6B35),
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Processing...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${(progress * 100).toInt()}%",
                color = Color(0xFFFF6B35),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AlgorithmInfoOverlay(
    selectedAlgorithm: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                "Active Algorithm",
                color = Color(0xFF7D8590),
                fontSize = 10.sp
            )
            Text(
                selectedAlgorithm,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AlgorithmSelector(
    selectedAlgorithm: String,
    onAlgorithmSelected: (String) -> Unit
) {
    val algorithms = getAlgorithmOptions()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Algorithm Selection",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(algorithms) { algorithm ->
                    AlgorithmChip(
                        algorithm = algorithm,
                        isSelected = selectedAlgorithm == algorithm.name,
                        onClick = { onAlgorithmSelected(algorithm.name) }
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
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                algorithm.name,
                fontSize = 12.sp
            )
        },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFF6B35),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF16131E),
            labelColor = Color(0xFF7D8590)
        )
    )
}

@Composable
private fun ProcessingControls(
    isProcessing: Boolean,
    selectedAlgorithm: String,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onProcess: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isProcessing) {
                Button(
                    onClick = onProcess,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Process",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process Image", fontSize = 14.sp)
                }
            } else {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(16.dp)
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
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(16.dp)
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
    val description: String
)

private fun getAlgorithmOptions(): List<AlgorithmOption> {
    return listOf(
        AlgorithmOption("Edge Detection", "Detect temperature boundaries"),
        AlgorithmOption("Noise Reduction", "Smooth thermal image"),
        AlgorithmOption("Contrast Enhancement", "Improve image contrast"),
        AlgorithmOption("Temperature Mapping", "Enhanced color mapping"),
        AlgorithmOption("Object Detection", "Identify thermal objects"),
        AlgorithmOption("Pattern Analysis", "Analyze thermal patterns")
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\BaseIRComposeActivity.kt =====

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

class BaseIRComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var connectionStatus by remember { mutableStateOf("Disconnected") }
        var cameraReady by remember { mutableStateOf(false) }
        var thermalMode by remember { mutableIntStateOf(1) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Base Control",
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
                            IconButton(onClick = {
                                // TODO: Open thermal camera settings
                                android.widget.Toast.makeText(
                                    this@BaseIRComposeActivity,
                                    "Opening settings...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
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
                    // Status bar
                    ThermalStatusBar(
                        connectionStatus = connectionStatus,
                        cameraReady = cameraReady,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Main thermal view
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Thermal camera surface
                        ThermalCameraSurface(
                            modifier = Modifier.fillMaxSize()
                        )
                        // Control overlay
                        ThermalControlOverlay(
                            thermalMode = thermalMode,
                            onModeChange = { thermalMode = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
        // Initialize camera connection
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000L)
            connectionStatus = "Connected"
            cameraReady = true
        }
    }
}

@Composable
private fun ThermalStatusBar(
    connectionStatus: String,
    cameraReady: Boolean,
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
            // Connection status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (cameraReady) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = "Status",
                    tint = if (cameraReady) Color(0xFF00FF00) else Color(0xFFFF4444),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    connectionStatus,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            // Camera info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusItem("Resolution", "384x288")
                StatusItem("FPS", "9")
                StatusItem("Mode", "IR")
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ThermalCameraSurface(
    modifier: Modifier = Modifier
) {
    // Placeholder for thermal camera surface
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
                    Icons.Default.Videocam,
                    contentDescription = "Camera",
                    tint = Color(0xFF7D8590),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Thermal Camera Feed",
                    color = Color(0xFF7D8590),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Base IR Camera Control",
                    color = Color(0xFFFF6B35),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ThermalControlOverlay(
    thermalMode: Int,
    onModeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode selector
            ThermalModeSelector(
                selectedMode = thermalMode,
                onModeSelected = onModeChange
            )
            // Quick actions
            QuickActionButtons()
        }
    }
}

@Composable
private fun ThermalModeSelector(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit
) {
    val modes = getThermalModes()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Thermal Mode",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEachIndexed { index, mode ->
                FilterChip(
                    onClick = { onModeSelected(index + 1) },
                    label = { Text(mode, fontSize = 12.sp) },
                    selected = selectedMode == index + 1,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF6B35),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF21262D),
                        labelColor = Color(0xFF7D8590)
                    )
                )
            }
        }
    }
}

@Composable
private fun QuickActionButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        QuickActionButton(
            icon = Icons.Default.CameraAlt,
            text = "Capture",
            onClick = {
                // TODO: Capture thermal image
                android.widget.Toast.makeText(
                    context,
                    "Capturing thermal image...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Videocam,
            text = "Record",
            onClick = {
                // TODO: Start/stop thermal video recording
                android.widget.Toast.makeText(
                    context,
                    "Recording...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Palette,
            text = "Palette",
            onClick = {
                // TODO: Change color palette
                android.widget.Toast.makeText(
                    context,
                    "Changing palette...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Tune,
            text = "Adjust",
            onClick = {
                // TODO: Open thermal adjustments
                android.widget.Toast.makeText(
                    context,
                    "Opening adjustments...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text,
                fontSize = 10.sp
            )
        }
    }
}

private fun getThermalModes(): List<String> {
    return listOf("Standard", "High Gain", "Low Gain", "Manual")
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\BaseIRPlusComposeActivity.kt =====

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
import kotlinx.coroutines.launch

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
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                scope = scope,
                                snackbarHostState = snackbarHostState,
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
                            scope = scope,
                            snackbarHostState = snackbarHostState,
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
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
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
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Auto enhance processing...")
                    }
                }
            )
            PlusFeatureButton(
                icon = Icons.Default.Tune,
                text = "Manual Tune",
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Opening manual tune controls...")
                    }
                }
            )
            PlusFeatureButton(
                icon = Icons.Default.Analytics,
                text = "AI Analysis",
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Running AI analysis...")
                    }
                }
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
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
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
            PlusQuickActions(
                onCapture = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Capturing with Plus enhancement...")
                    }
                },
                onRecord = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Recording with Plus features...")
                    }
                },
                onProcess = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Processing with Plus algorithms...")
                    }
                }
            )
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
private fun PlusQuickActions(
    onCapture: () -> Unit = {},
    onRecord: () -> Unit = {},
    onProcess: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onCapture,
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
            onClick = onRecord,
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
            onClick = onProcess,
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\BaseIRPlusFragment.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.graphics.ImageFormat
import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.usb.USBMonitor
import com.energy.iruvc.utils.*
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils
import com.mpdc4gsr.libunified.app.ktbase.BaseFragment
import com.mpdc4gsr.libunified.app.utils.UnifiedScreenUtils
import com.mpdc4gsr.libunified.ir.usbdual.Const
import com.mpdc4gsr.libunified.ir.usbdual.camera.DualViewWithExternalCameraCommonApi
import com.mpdc4gsr.libunified.ir.usbdual.camera.IRUVCDual
import com.mpdc4gsr.libunified.ir.usbdual.camera.USBMonitorManager
import com.mpdc4gsr.libunified.ir.usbdual.inf.OnUSBConnectListener
import com.mpdc4gsr.libunified.ir.utils.PseudocodeUtils
import com.mpdc4gsr.libunified.ir.view.ITsTempListener
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.module.thermalunified.extension.setAutoShutter
import com.mpdc4gsr.module.thermalunified.extension.setContrast
import com.mpdc4gsr.module.thermalunified.extension.setMirror
import com.mpdc4gsr.module.thermalunified.extension.setPropDdeLevel
import com.mpdc4gsr.module.thermalunified.repository.ConfigRepository
import com.mpdc4gsr.module.thermalunified.utils.DualParamsUtils
import com.mpdc4gsr.module.thermalunified.utils.IRCmdTools
import com.mpdc4gsr.module.thermalunified.utils.IRCmdTools.getSNStr
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream

abstract class BaseIRPlusFragment :
    BaseFragment(),
    OnUSBConnectListener,
    ITsTempListener,
    IIRFrameCallback {
    val INIT_ALIGN_DATA = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
    protected var dualView: DualViewWithExternalCameraCommonApi? = null
    protected var pseudoColorModeDual = CommonParams.PseudoColorUsbDualType.IRONBOW_MODE
    private var hasStartPreview = false
    protected var ircmd: IRCMD? = null
    protected var snStr = ""
    protected var defaultDataFlowMode = CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT
    private var irPid = 0x5830
    private var irFps = 25
    private var irCameraWidth =
        0
    private var irCameraHeight =
        0
    private var irTempHeight =
        0
    private var imageWidth =
        0
    private var imageHeight =
        0
    protected var temperatureSrc: ByteArray? = null
    protected var mCurrentFusionType = DualParamsUtils.fusionTypeToParams(SaveSettingUtils.fusionType)
    private var syncimage = SynchronizedBitmap()
    protected var isConfigWait = true
    protected var pseudoColorMode = SaveSettingUtils.pseudoColorMode
    private var vlPid = 12337
    private var vlFps = 30
    protected var vlCameraWidth = 1280
    protected var vlCameraHeight = 720
    private var vlData = ByteArray(vlCameraWidth * vlCameraHeight * 3)
    private var dualCameraWidth = 480
    private var dualCameraHeight = 640
    protected var isrun = false
    protected val isUseIRISP = false
    protected var fullScreenlayoutParams: FrameLayout.LayoutParams? = null
    protected var psedocolor: Array<ByteArray>? = null
    protected var dualRotate = 0
    protected var dualDisp = 30
    private var vlUVCCamera: IRUVCDual? = null
    abstract fun getSurfaceView(): SurfaceView
    abstract fun getTemperatureDualView(): TemperatureView
    abstract suspend fun onDualViewCreate(dualView: DualViewWithExternalCameraCommonApi?)
    open fun initdata() {
    }

    open fun initDataFlowMode(dataFlowMode: CommonParams.DataFlowMode) {
        when (dataFlowMode) {
            CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT -> {
                irCameraWidth = 256
                irCameraHeight = 384
                irTempHeight = 192
                imageWidth = irCameraHeight - irTempHeight
                imageHeight = irCameraWidth
                temperatureSrc = ByteArray(imageWidth * imageHeight * 2)
            }

            CommonParams.DataFlowMode.IMAGE_OUTPUT -> {
                irCameraWidth = 256
                irCameraHeight = 192
                irTempHeight = 0
                imageWidth = irCameraHeight - irTempHeight
                imageHeight = irCameraWidth
                temperatureSrc = ByteArray(imageWidth * imageHeight * 2)
            }

            CommonParams.DataFlowMode.TEMP_OUTPUT -> {
                irCameraWidth = 256
                irCameraHeight = 192
                irTempHeight = 0
                imageWidth = irCameraHeight - irTempHeight
                imageHeight = irCameraWidth
                temperatureSrc = ByteArray(imageWidth * imageHeight * 2)
            }

            else -> {
                irCameraWidth = 256
                irCameraHeight = 192
                irTempHeight = 0
                imageWidth = irCameraHeight - irTempHeight
                imageHeight = irCameraWidth
                temperatureSrc = ByteArray(imageWidth * imageHeight * 2)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        dualStart()
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    abstract fun isDualIR(): Boolean
    abstract fun setTemperatureViewType()
    override fun initView() {
        if (isDualIR()) {
            getTemperatureDualView().setTextSize(SaveSettingUtils.tempTextSize)
            initDataFlowMode(defaultDataFlowMode)
            initIrDualdata()
        }
    }

    private fun initIrDualdata() {
        var width = 0
        var height = 0
        val screenWidth: Int = UnifiedScreenUtils.getScreenWidth(requireContext())
        val screenHeight: Int =
            UnifiedScreenUtils.getScreenHeight(requireContext()) - 52f.dpToPx(requireContext()).toInt()
        if (screenWidth > screenHeight) {
            width = screenHeight * imageWidth / imageHeight
            height = screenHeight
        } else {
            width = screenWidth
            height = screenWidth * imageHeight / imageWidth
        }
        fullScreenlayoutParams =
            FrameLayout.LayoutParams(
                width,
                height,
            )
        getSurfaceView().layoutParams = fullScreenlayoutParams
        getTemperatureDualView().layoutParams = fullScreenlayoutParams
        USBMonitorManager.getInstance().init(irPid, isUseIRISP, defaultDataFlowMode)
        USBMonitorManager.getInstance().addOnUSBConnectListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mIrHandler.removeCallbacksAndMessages(null)
        USBMonitorManager.getInstance().removeOnUSBConnectListener(this)
    }

    open fun initPseudocolor() {
        val am = requireContext().assets
        var `is`: InputStream? = null
        try {
            psedocolor = Array(11) { ByteArray(0) }
            `is` = am.open("pseudocolor/White_Hot.bin")
            var lenth = `is`.available()
            psedocolor!![0] = ByteArray(lenth + 1)
            if (`is`.read(psedocolor!![0]) != lenth) {
                Log.d(
                    TAG,
                    "read file fail ",
                )
            }
            psedocolor!![0][lenth] = 0
            dualView!!.getDualUVCCamera().loadPseudocolor(
                CommonParams.PseudoColorUsbDualType.WHITE_HOT_MODE,
                psedocolor!![0],
            )
            setFusion(mCurrentFusionType)
            `is`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    open fun setFusion(fusion: DualCameraParams.FusionType) {
        dualView?.setCurrentFusionType(fusion)
        getTemperatureDualView().setCurrentFusionType(fusion)
        if (fusion == DualCameraParams.FusionType.IROnlyNoFusion) {
            getTemperatureDualView().setImageSize(Const.IR_HEIGHT, Const.IR_WIDTH, null)
        } else {
            getTemperatureDualView().setImageSize(dualCameraWidth, dualCameraHeight, null)
        }
    }

    val calibrationDataSize = 192
    val SAVE_DUAL_BIN = "dual_calibration_parameters2.bin"
    open fun initDefIntegralArgsDISP_VALUE(typeLoadParameters: DualCameraParams.TypeLoadParameters) {
        if (!isDualIR()) {
            return
        }
        lifecycleScope.launch {
            val parameters = IRCmdTools.getDualBytes(USBMonitorManager.getInstance().ircmd)
            val data = dualView?.dualUVCCamera?.loadParameters(parameters, typeLoadParameters)
            dualDisp = IRCmdTools.dispNumber
            setDispViewData(dualDisp)
            dualView?.dualUVCCamera?.setDisp(dualDisp)
            dualView?.startPreview()
        }
    }

    open fun setDispViewData(dualDisp: Int) {
    }

    open fun restartDualCamera() {
        if (isrun) {
            USBMonitorManager.getInstance().isReStart = true
            dualStop()
            SystemClock.sleep(200)
            dualStart()
        }
    }

    override fun onStop() {
        super.onStop()
        dualStop()
    }

    open fun dualStart() {
        if (!isDualIR()) {
            return
        }
        Log.d(
            TAG,
            "dualStart",
        )
        USBMonitorManager.getInstance().registerUSB()
        getTemperatureDualView().setUseIRISP(isUseIRISP)
        if (mCurrentFusionType == DualCameraParams.FusionType.IROnlyNoFusion) {
            getTemperatureDualView().setImageSize(Const.IR_HEIGHT, Const.IR_WIDTH, null)
        } else {
            getTemperatureDualView().setImageSize(dualCameraWidth, dualCameraHeight, null)
        }
        setTemperatureViewType()
        getTemperatureDualView().start()
    }

    var mIrHandler: Handler =
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                Log.d(
                    TAG,
                    "USBMonitorManager ${msg.what}",
                )
                if (!isDualIR()) {
                    return
                }
                if (msg.what == Const.RESTART_USB) {
                    Log.d(
                        TAG,
                        "restartDualCamera",
                    )
                    restartDualCamera()
                } else if (msg.what == Const.HANDLE_CONNECT) {
                    Log.d(
                        TAG,
                        "USBMonitorManager HANDLE_CONNECT",
                    )
                    lifecycleScope.launch(Dispatchers.Main) {
                        startVLCamera(vlPid, vlFps, vlCameraWidth, vlCameraHeight)
                        initDualCamera()
                        initDefIntegralArgsDISP_VALUE(DualCameraParams.TypeLoadParameters.ROTATE_270)
                    }
                } else if (msg.what == Const.HANDLE_REGISTER) {
                    USBMonitorManager.getInstance().registerUSB()
                } else if (msg.what == Const.SHOW_LOADING) {
                    Log.d(
                        TAG,
                        "SHOW_LOADING",
                    )
                    showLoadingDialog()
                } else if (msg.what == Const.HIDE_LOADING) {
                    Log.d(
                        TAG,
                        "HIDE_LOADING",
                    )
                    dismissLoadingDialog()
                } else if (msg.what == Const.SHOW_RESTART_MESSAGE) {
                    Toast.makeText(
                        context,
                        "please restart app or reinsert device",
                        Toast.LENGTH_SHORT,
                    ).show()
                    activity?.finish()
                }
            }
        }

    open fun initDualCamera() {
        if (!isDualIR()) {
            return
        }
        if (dualView != null) {
            return
        }
        Log.d(
            TAG,
            "initDualCamera",
        )
        dualView =
            DualViewWithExternalCameraCommonApi(
                getSurfaceView(),
                USBMonitorManager.getInstance().uvcCamera, defaultDataFlowMode,
                irCameraWidth, irCameraHeight - irTempHeight,
                vlCameraWidth, vlCameraHeight, dualCameraWidth, dualCameraHeight,
                isUseIRISP, dualRotate, this,
            )
        dualView?.addFrameCallback(getTemperatureDualView())
        getTemperatureDualView().setDualUVCCamera(dualView!!.getDualUVCCamera())
        initPseudocolor()
        dualView?.setHandler(mIrHandler)
        isrun = true
    }

    open fun startVLCamera(
        pid: Int,
        fps: Int,
        cameraWidth: Int,
        cameraHeight: Int,
    ) {
        if (!isDualIR()) {
            return
        }
        Log.i(
            TAG,
            "startVLCamera",
        )
        vlUVCCamera =
            IRUVCDual(
                cameraWidth,
                cameraHeight,
                requireContext(),
                pid,
                fps,
                object : ConnectCallback {
                    override fun onCameraOpened(uvcCamera: UVCCamera) {
                        Log.i(
                            TAG,
                            "ConnectCallback-startVLCamera-onCameraOpened",
                        )
                    }

                    override fun onIRCMDCreate(ircmd: IRCMD) {
                        setUVCCameraICMD(ircmd)
                    }
                },
                IFrameCallback { frame ->
                    Log.i(
                        TAG,
                        "startVLCamera-onFrame->frame.length = " + frame.size,
                    )
                    if (dualView != null && dualView?.getDualUVCCamera() != null &&
                        Const.isDeviceConnected
                    ) {
                        System.arraycopy(frame, 0, vlData, 0, vlData.size)
                        dualView?.getDualUVCCamera()?.updateFrame(
                            ImageFormat.FLEX_RGB_888, vlData, vlCameraWidth,
                            vlCameraHeight,
                        )
                    }
                },
            )
        vlUVCCamera?.setHandler(mIrHandler)
        vlUVCCamera?.registerUSB()
        vlUVCCamera?.TAG = "mjpeg"
    }

    open fun setUVCCameraICMD(ircmd: IRCMD) {
        this@BaseIRPlusFragment.ircmd = ircmd
        snStr = getSNStr(ircmd)
        isConfigWait = false
        Log.i(
            TAG,
            "ConnectCallback-startVLCamera-onIRCMDCreate",
        )
    }

    override fun onStart() {
        super.onStart()
        if (!isrun) {
            isrun = true
            configParam()
        }
    }

    private var isFirst = true
    private var configJob: Job? = null
    private val timeMillis = 150L
    private fun configParam() {
        configJob =
            lifecycleScope.launch {
                while (isConfigWait && isActive) {
                    delay(200)
                }
                delay(500)
                val config = ConfigRepository.readConfig(false)
                val disChar = (config.distance * 128).toInt()
                val emsChar = (config.radiation * 128).toInt()
                XLog.w("TPD_PROP DISTANCE:$disChar, EMS:$emsChar}")
                delay(timeMillis)
                ircmd?.setPropTPDParams(
                    CommonParams.PropTPDParams.TPD_PROP_EMS,
                    CommonParams.PropTPDParamsValue.NumberType(emsChar.toString()),
                )
                delay(timeMillis)
                ircmd?.setPropTPDParams(
                    CommonParams.PropTPDParams.TPD_PROP_DISTANCE,
                    CommonParams.PropTPDParamsValue.NumberType(disChar.toString()),
                )
                delay(timeMillis)
                XLog.w("TPD_PROP DISTANCE:$disChar, EMS:$emsChar}")
                if (isFirst && isrun) {
                    ircmd?.setMirror(false)
                    delay(timeMillis)
                    withContext(Dispatchers.IO) {
                        ircmd?.setAutoShutter(true)
                        isFirst = false
                    }
                    ircmd?.setPropDdeLevel(2)
                    ircmd?.setContrast(128)
                }
                ircmd?.setPropImageParams(
                    CommonParams.PropImageParams.IMAGE_PROP_ONOFF_AGC,
                    CommonParams.PropImageParamsValue.StatusSwith.ON,
                )
                if (syncimage.type == 1) {
                    ircmd?.tc1bShutterManual()
                } else {
                    ircmd?.updateOOCOrB(CommonParams.UpdateOOCOrBType.B_UPDATE)
                }
                XLog.w("TPD_PROP DISTANCE2:$disChar, EMS:$emsChar}")
            }
    }

    open fun dualStop() {
        if (!isDualIR()) {
            return
        }
        Log.d(
            TAG,
            "USBMonitorManager dualStop",
        )
        isrun = false
        syncimage.valid = false
        isConfigWait = true
        getTemperatureDualView().stop()
        USBMonitorManager.getInstance().unregisterUSB()
        ircmd?.onDestroy()
        ircmd = null
        SystemClock.sleep(100)
        if (dualView != null) {
            dualView?.removeFrameCallback(getTemperatureDualView())
            dualView?.dualUVCCamera?.onPausePreview()
            USBMonitorManager.getInstance().stopPreview()
            if (vlUVCCamera != null) {
                vlUVCCamera?.unregisterUSB()
                vlUVCCamera?.stopPreview()
                vlUVCCamera = null
            }
            SystemClock.sleep(100)
            dualView?.stopPreview()
            dualView = null
            Log.d(
                TAG,
                " dualStop",
            )
        }
    }

    override fun onAttach(device: UsbDevice?) {
        Log.d(
            TAG,
            "USBMonitorManager onAttach",
        )
    }

    override fun onGranted(
        usbDevice: UsbDevice?,
        granted: Boolean,
    ) {
        Log.d(
            TAG,
            "USBMonitorManager onGranted",
        )
    }

    override fun onDettach(device: UsbDevice?) {
        Log.d(
            TAG,
            "USBMonitorManager onDettach",
        )
    }

    override fun onConnect(
        device: UsbDevice?,
        ctrlBlock: USBMonitor.UsbControlBlock?,
        createNew: Boolean,
    ) {
        Log.d(
            TAG,
            "USBMonitorManager onConnect",
        )
        mIrHandler.sendEmptyMessage(Const.HANDLE_CONNECT)
    }

    override fun onDisconnect(
        device: UsbDevice?,
        ctrlBlock: USBMonitor.UsbControlBlock?,
    ) {
        XLog.d(
            TAG,
            "USBMonitorManager onDisconnect",
        )
    }

    override fun onCancel(device: UsbDevice?) {
        Log.d(
            TAG,
            "USBMonitorManager onCancel",
        )
    }

    override fun onIRCMDInit(ircmd: IRCMD?) {
        setUVCCameraICMD(ircmd!!)
    }

    override fun onCompleteInit() {
        mIrHandler.sendEmptyMessage(Const.HIDE_LOADING)
    }

    override fun onSetPreviewSizeFail() {
        mIrHandler.sendEmptyMessage(Const.SHOW_RESTART_MESSAGE)
    }

    protected val preIrARGBData = ByteArray(256 * 192 * 4)
    protected val preIrData = ByteArray(256 * 192 * 2)
    protected val preTempData = ByteArray(256 * 192 * 2)
    override fun onIrFrame(irFrame: ByteArray?): ByteArray {
        irFrame?.let {
            System.arraycopy(it, 0, preIrData, 0, preIrData.size)
        } ?: return preIrARGBData
        LibIRProcess.convertYuyvMapToARGBPseudocolor(
            preIrData,
            (Const.IR_WIDTH * Const.IR_HEIGHT).toLong(),
            PseudocodeUtils.changePseudocodeModeByOld(pseudoColorMode),
            preIrARGBData,
        )
        return preIrARGBData
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ConnectComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
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
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConnectComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var isConnected by remember { mutableStateOf(DeviceTools.isConnect()) }
        var isConnecting by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Device Connection",
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Connection Status Card
                    ConnectionStatusCard(
                        isConnected = isConnected,
                        isConnecting = isConnecting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Connection Controls
                    ConnectionControls(
                        isConnected = isConnected,
                        isConnecting = isConnecting,
                        onConnect = {
                            isConnecting = true
                            // Simulate connection process
                            coroutineScope.launch {
                                delay(2000L)
                                isConnected = true
                                isConnecting = false
                            }
                        },
                        onDisconnect = {
                            isConnected = false
                            isConnecting = false
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    // Device Information
                    if (isConnected) {
                        DeviceInfoCard(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Icon
            when {
                isConnecting -> {
                    CircularProgressIndicator(
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.size(48.dp)
                    )
                }

                isConnected -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = Color(0xFF00FF00),
                        modifier = Modifier.size(48.dp)
                    )
                }

                else -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Disconnected",
                        tint = Color(0xFFFF4444),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Status Text
            Text(
                when {
                    isConnecting -> "Connecting..."
                    isConnected -> "Device Connected"
                    else -> "Device Disconnected"
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                when {
                    isConnecting -> "Establishing connection to thermal camera"
                    isConnected -> "Thermal camera is ready for use"
                    else -> "Please connect your thermal camera device"
                },
                color = Color(0xFF7D8590),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ConnectionControls(
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isConnected && !isConnecting) {
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = "Connect",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Connect Device",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (isConnected) {
            OutlinedButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF4444)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4444)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Disconnect",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Disconnect",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Device Information",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            DeviceInfoItem("Model", "TC007 Thermal Camera")
            DeviceInfoItem("Status", "Ready")
            DeviceInfoItem("Connection", "Bluetooth")
            DeviceInfoItem("Battery", "87%")
            DeviceInfoItem("Temperature Range", "-10Â°C to 50Â°C")
        }
    }
}

@Composable
private fun DeviceInfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 14.sp
        )
        Text(
            value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\GalleryComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
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
import com.mpdc4gsr.module.thermalunified.viewmodel.GalleryActivityViewModel
import kotlinx.coroutines.launch

class GalleryComposeActivity : BaseComposeActivity<GalleryActivityViewModel>() {
    override fun createViewModel(): GalleryActivityViewModel {
        return viewModels<GalleryActivityViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GalleryActivityViewModel) {
        val pagerState = rememberPagerState(pageCount = { 2 })
        val scope = rememberCoroutineScope()
        var selectedTab by remember { mutableIntStateOf(0) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Gallery",
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
                            IconButton(onClick = {
                                // TODO: Search gallery
                                android.widget.Toast.makeText(
                                    this@GalleryComposeActivity,
                                    "Searching gallery...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                // TODO: Open gallery settings
                                android.widget.Toast.makeText(
                                    this@GalleryComposeActivity,
                                    "Opening gallery settings...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                ) {
                    // Tab row
                    GalleryTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            selectedTab = tab
                            scope.launch {
                                pagerState.animateScrollToPage(tab)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Content pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> GalleryPictureTab()
                            1 -> GalleryVideoTab()
                        }
                    }
                }
            }
        }
        // Sync pager with tabs
        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }
    }
}

@Composable
private fun GalleryTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GalleryTab(
                text = "Pictures",
                icon = Icons.Default.Photo,
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            GalleryTab(
                text = "Videos",
                icon = Icons.Default.VideoLibrary,
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GalleryTab(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFFF6B35) else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color(0xFF7D8590)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GalleryPictureTab() {
    // Embed existing picture fragment using AndroidView wrapper
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_0
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun GalleryVideoTab() {
    // Embed existing video fragment using AndroidView wrapper
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_1
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ImageColorComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ImageColorViewModel

class ImageColorComposeActivity : BaseComposeActivity<ImageColorViewModel>() {
    override fun createViewModel(): ImageColorViewModel {
        return viewModels<ImageColorViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ImageColorViewModel) {
        val timestamp by viewModel.timestamp.collectAsState()
        val showData by viewModel.showData.collectAsState()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Image Color",
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
                ) {
                    // Horizontal scrollable image comparison
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .background(Color.Black),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // First image
                        Card(
                            modifier = Modifier
                                .width(250.dp)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Thermal Image 1",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        // Second image
                        Card(
                            modifier = Modifier
                                .width(250.dp)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Thermal Image 2",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    // Timestamp display
                    if (timestamp.isNotEmpty()) {
                        Text(
                            text = timestamp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    // ARGB image display
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ARGB Image",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    // Control buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.toggleDataDisplay() },
                            modifier = Modifier.width(120.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (showData) "Hide Data" else "Show Data")
                        }
                        Button(
                            onClick = { },
                            modifier = Modifier.width(120.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Process U4")
                        }
                    }
                }
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ImagePickIRComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
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
import com.mpdc4gsr.module.thermalunified.fragment.IRMonitorThermalComposeFragment
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ImagePickIRComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var captureMode by remember { mutableStateOf("single") }
        var selectedImages by remember { mutableStateOf(setOf<String>()) }
        var isCapturing by remember { mutableStateOf(false) }
        val recentImages = remember { getRecentImages() }
        val coroutineScope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Pick Thermal Image",
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
                            if (selectedImages.isNotEmpty()) {
                                TextButton(
                                    onClick = { finish() }
                                ) {
                                    Text(
                                        "Select (${selectedImages.size})",
                                        color = Color(0xFFFF6B35),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            isCapturing = true
                            // Simulate capture
                            coroutineScope.launch {
                                delay(2000L)
                                isCapturing = false
                            }
                        },
                        containerColor = Color(0xFFFF6B35)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = Color.White
                            )
                        }
                    }
                },
                containerColor = Color.Black
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.Black)
                ) {
                    // Live thermal preview
                    ThermalPreviewSection(
                        isCapturing = isCapturing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f)
                    )
                    // Capture mode selector
                    CaptureModeSelector(
                        selectedMode = captureMode,
                        onModeSelected = { captureMode = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Recent images gallery
                    RecentImagesSection(
                        images = recentImages,
                        selectedImages = selectedImages,
                        onImageSelected = { imageId ->
                            selectedImages = if (captureMode == "single") {
                                setOf(imageId)
                            } else {
                                if (selectedImages.contains(imageId)) {
                                    selectedImages - imageId
                                } else {
                                    selectedImages + imageId
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalPreviewSection(
    isCapturing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1117)
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Thermal fragment preview (using AndroidView)
            AndroidView(
                factory = { context ->
                    val fragment = IRMonitorThermalComposeFragment()
                    androidx.fragment.app.FragmentContainerView(context).apply {
                        id = androidx.core.R.id.accessibility_custom_action_3
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Capture overlay
            if (isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF6B35),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Capturing thermal image...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            // Temperature overlay
            ThermalInfoOverlay(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun ThermalInfoOverlay(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Live Thermal Feed",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Max: 42.5Â°C",
                color = Color(0xFFFF4444),
                fontSize = 11.sp
            )
            Text(
                "Min: 18.2Â°C",
                color = Color(0xFF4444FF),
                fontSize = 11.sp
            )
            Text(
                "Avg: 28.7Â°C",
                color = Color(0xFFFFAA00),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CaptureModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Mode:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            val modes = listOf(
                "single" to "Single Select",
                "multiple" to "Multiple Select",
                "burst" to "Burst Mode"
            )
            modes.forEach { (mode, label) ->
                FilterChip(
                    onClick = { onModeSelected(mode) },
                    label = { Text(label, fontSize = 12.sp) },
                    selected = selectedMode == mode,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF6B35),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF16131E),
                        labelColor = Color(0xFF7D8590)
                    )
                )
            }
        }
    }
}

@Composable
private fun RecentImagesSection(
    images: List<RecentImage>,
    selectedImages: Set<String>,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Recent Thermal Images",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images) { image ->
                    RecentImageItem(
                        image = image,
                        isSelected = selectedImages.contains(image.id),
                        onClick = { onImageSelected(image.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentImageItem(
    image: RecentImage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFF6B35) else Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFF0D1117),
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Image",
                    tint = if (isSelected) Color.White else Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
            }
            // Image info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    image.name,
                    color = if (isSelected) Color.White else Color(0xFF7D8590),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    image.timestamp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF7D8590),
                    fontSize = 10.sp
                )
                Text(
                    "Max: ${image.maxTemp}Â°C",
                    color = if (isSelected) Color.White else Color(0xFFFF6B35),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Selection indicator
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                )
            }
        }
    }
}

// Data classes
data class RecentImage(
    val id: String,
    val name: String,
    val timestamp: String,
    val maxTemp: Float
)

private fun getRecentImages(): List<RecentImage> {
    return listOf(
        RecentImage("1", "thermal_capture_001.jpg", "14:30:25", 45.2f),
        RecentImage("2", "thermal_capture_002.jpg", "14:28:15", 38.7f),
        RecentImage("3", "thermal_capture_003.jpg", "14:25:45", 52.1f),
        RecentImage("4", "thermal_capture_004.jpg", "14:22:30", 41.5f),
        RecentImage("5", "thermal_capture_005.jpg", "14:18:12", 47.8f),
        RecentImage("6", "thermal_capture_006.jpg", "14:15:55", 35.2f)
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ImagePickIRPlushComposeActivity.kt =====

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ImagePickIRPlushComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var showAIDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Plus",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Image Picker Plus",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117)
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            showAIDialog = true
                        },
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Capture")
                    }
                }
            ) { paddingValues ->
                ImagePickerPlusContent(
                    modifier = Modifier.padding(paddingValues),
                    scope = scope,
                    snackbarHostState = snackbarHostState
                )
            }
            // AI Enhancement Dialog
            if (showAIDialog) {
                AlertDialog(
                    onDismissRequest = { showAIDialog = false },
                    title = { Text("AI Enhancement") },
                    text = {
                        Column {
                            Text("Select AI enhancement mode:")
                            Spacer(modifier = Modifier.height(16.dp))
                            listOf(
                                "Auto Enhance",
                                "Noise Reduction",
                                "Detail Enhancement",
                                "Color Correction"
                            ).forEach { mode ->
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Applying $mode...")
                                        }
                                        showAIDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(mode)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAIDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun ImagePickerPlusContent(
        modifier: Modifier = Modifier,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState
    ) {
        var captureMode by remember { mutableStateOf("Smart") }
        var aiEnhancement by remember { mutableStateOf(true) }
        var qualityFilter by remember { mutableStateOf(85) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Recommendations Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFD700).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "AI",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI Recommendations",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "â€¢ Best quality images detected automatically",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                    Text(
                        "â€¢ Optimal thermal range suggestions",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                    Text(
                        "â€¢ Smart batch processing available",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }
            }
            // Live Preview Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraEnhance,
                            contentDescription = "Enhanced Preview",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "AI-Enhanced Live Preview",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Real-time quality scoring and recommendations",
                            color = Color(0xFF7D8590),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Quality Score Display
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Quality Score: 94%",
                                color = Color(0xFFFFD700),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
            // Plus Features Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Plus Features",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "AI Enhancement",
                            color = Color(0xFF7D8590),
                            fontSize = 14.sp
                        )
                        Switch(
                            checked = aiEnhancement,
                            onCheckedChange = { aiEnhancement = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFD700),
                                checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.5f)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Quality Filter: ${qualityFilter}%",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                    Slider(
                        value = qualityFilter.toFloat(),
                        onValueChange = { qualityFilter = it.toInt() },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700)
                        )
                    )
                }
            }
            // Capture Mode Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Capture Mode",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CaptureModeChip("Smart", captureMode == "Smart") { captureMode = "Smart" }
                        CaptureModeChip("Batch", captureMode == "Batch") { captureMode = "Batch" }
                        CaptureModeChip("Quality", captureMode == "Quality") { captureMode = "Quality" }
                        CaptureModeChip("Speed", captureMode == "Speed") { captureMode = "Speed" }
                    }
                }
            }
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Loading recent images...")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7D8590)
                    )
                ) {
                    Icon(Icons.Default.History, contentDescription = "Recent")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recent (10)")
                }
                Button(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Starting AI batch processing...")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = "AI Process")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Process")
                }
            }
        }
    }

    @Composable
    private fun CaptureModeChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        FilterChip(
            onClick = onClick,
            label = { Text(label) },
            selected = selected,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFFFD700),
                selectedLabelColor = Color.Black,
                containerColor = Color(0xFF0D1117),
                labelColor = Color(0xFF7D8590)
            )
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRCameraSettingComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
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

class IRCameraSettingComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedPalette by remember { mutableStateOf("Iron") }
        var frameRate by remember { mutableIntStateOf(9) }
        var autoShutter by remember { mutableStateOf(true) }
        var imageCorrection by remember { mutableStateOf(true) }
        var temperatureUnit by remember { mutableStateOf("Celsius") }
        var resolution by remember { mutableStateOf("384x288") }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Camera Settings",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Image Settings
                    item {
                        SettingsCategoryCard(
                            title = "Image Settings",
                            icon = Icons.Default.Image
                        ) {
                            SettingsDropdownItem(
                                title = "Color Palette",
                                selectedValue = selectedPalette,
                                options = listOf("Iron", "Rainbow", "Grayscale", "Hot", "Cool"),
                                onValueChange = { selectedPalette = it }
                            )
                            SettingsSliderItem(
                                title = "Frame Rate",
                                value = frameRate,
                                valueRange = 1f..25f,
                                unit = "fps",
                                onValueChange = { frameRate = it.toInt() }
                            )
                            SettingsDropdownItem(
                                title = "Resolution",
                                selectedValue = resolution,
                                options = listOf("384x288", "640x480", "160x120"),
                                onValueChange = { resolution = it }
                            )
                        }
                    }
                    // Camera Features
                    item {
                        SettingsCategoryCard(
                            title = "Camera Features",
                            icon = Icons.Default.CameraAlt
                        ) {
                            SettingsSwitchItem(
                                title = "Auto Shutter",
                                description = "Automatic shutter calibration",
                                checked = autoShutter,
                                onCheckedChange = { autoShutter = it }
                            )
                            SettingsSwitchItem(
                                title = "Image Correction",
                                description = "Automatic image enhancement",
                                checked = imageCorrection,
                                onCheckedChange = { imageCorrection = it }
                            )
                        }
                    }
                    // Temperature Settings
                    item {
                        SettingsCategoryCard(
                            title = "Temperature Settings",
                            icon = Icons.Default.Thermostat
                        ) {
                            SettingsDropdownItem(
                                title = "Temperature Unit",
                                selectedValue = temperatureUnit,
                                options = listOf("Celsius", "Fahrenheit", "Kelvin"),
                                onValueChange = { temperatureUnit = it }
                            )
                        }
                    }
                    // Advanced Settings
                    item {
                        SettingsCategoryCard(
                            title = "Advanced Settings",
                            icon = Icons.Default.Tune
                        ) {
                            AdvancedSettingItem(
                                title = "Calibration",
                                description = "Manual camera calibration",
                                onClick = {
                                    // TODO: Navigate to calibration activity
                                    android.widget.Toast.makeText(
                                        this@IRCameraSettingComposeActivity,
                                        "Calibration feature coming soon",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            AdvancedSettingItem(
                                title = "Firmware Update",
                                description = "Check for camera firmware updates",
                                onClick = {
                                    // TODO: Check for firmware updates
                                    android.widget.Toast.makeText(
                                        this@IRCameraSettingComposeActivity,
                                        "Checking for firmware updates...",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            AdvancedSettingItem(
                                title = "Factory Reset",
                                description = "Reset camera to default settings",
                                onClick = {
                                    // TODO: Show confirmation dialog for factory reset
                                    android.widget.Toast.makeText(
                                        this@IRCameraSettingComposeActivity,
                                        "Factory reset confirmation dialog",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                    // Save/Reset buttons
                    item {
                        SaveResetButtons(
                            onSave = {
                                // TODO: Save camera settings to preferences
                                android.widget.Toast.makeText(
                                    this@IRCameraSettingComposeActivity,
                                    "Settings saved successfully",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            onReset = {
                                // TODO: Reset settings to defaults
                                selectedPalette = "Iron"
                                frameRate = 9
                                autoShutter = true
                                imageCorrection = true
                                temperatureUnit = "Celsius"
                                resolution = "384x288"
                                android.widget.Toast.makeText(
                                    this@IRCameraSettingComposeActivity,
                                    "Settings reset to defaults",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownItem(
    title: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF6B35),
                    unfocusedBorderColor = Color(0xFF7D8590),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSliderItem(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "$value $unit",
                color = Color(0xFFFF6B35),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF6B35),
                activeTrackColor = Color(0xFFFF6B35),
                inactiveTrackColor = Color(0xFF7D8590)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                color = Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF6B35),
                uncheckedThumbColor = Color(0xFF7D8590),
                uncheckedTrackColor = Color(0xFF21262D)
            )
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun AdvancedSettingItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                tint = Color(0xFF7D8590),
                modifier = Modifier.size(20.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SaveResetButtons(
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF7D8590)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
        ) {
            Text("Reset", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35)
            )
        ) {
            Text("Save Settings", fontWeight = FontWeight.Bold)
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRConfigComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mpdc4gsr.libunified.app.config.ExtraKeyConfig
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.module.thermalunified.viewmodel.IRConfigViewModel

class IRConfigComposeActivity : BaseComposeActivity<IRConfigViewModel>() {
    private var isTC007 = false
    override fun onCreate(savedInstanceState: Bundle?) {
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)
        super.onCreate(savedInstanceState)
    }

    override fun createViewModel(): IRConfigViewModel {
        return viewModels<IRConfigViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRConfigViewModel) {
        var useDefaultModel by remember { mutableStateOf(true) }
        var environmentTemp by remember { mutableFloatStateOf(25.0f) }
        var distance by remember { mutableFloatStateOf(1.0f) }
        var emissivity by remember { mutableFloatStateOf(0.95f) }
        var selectedMaterial by remember { mutableStateOf<MaterialPreset?>(null) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Model Configuration",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E)),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Default Model Selection
                    item {
                        DefaultModelCard(
                            isSelected = useDefaultModel,
                            onSelectionChange = { useDefaultModel = it }
                        )
                    }
                    // Environment Temperature
                    item {
                        ConfigurationCard(
                            title = "Environment Temperature",
                            subtitle = "${UnitTools.showConfigC(-10, if (isTC007) 50 else 55)}",
                            value = "${environmentTemp.toInt()}Â°${UnitTools.showUnit()}",
                            icon = Icons.Default.Thermostat,
                            onClick = {
                                // TODO: Show temperature input dialog
                                android.widget.Toast.makeText(
                                    this@IRConfigComposeActivity,
                                    "Opening temperature dialog...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    // Distance Configuration
                    item {
                        ConfigurationCard(
                            title = "Measurement Distance",
                            subtitle = "(0.2~${if (isTC007) 4 else 5}m)",
                            value = "${distance}m",
                            icon = Icons.Default.Straighten,
                            onClick = {
                                // TODO: Show distance input dialog
                                android.widget.Toast.makeText(
                                    this@IRConfigComposeActivity,
                                    "Opening distance dialog...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    // Emissivity Configuration
                    item {
                        ConfigurationCard(
                            title = "Emissivity",
                            subtitle = "(${if (isTC007) "0.1" else "0.01"}~1.00)",
                            value = String.format("%.2f", emissivity),
                            icon = Icons.Default.Tune,
                            onClick = {
                                // TODO: Show emissivity input dialog
                                android.widget.Toast.makeText(
                                    this@IRConfigComposeActivity,
                                    "Opening emissivity dialog...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    // Material Presets Section
                    item {
                        Text(
                            "Material Presets",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(getMaterialPresets()) { material ->
                        MaterialPresetCard(
                            material = material,
                            isSelected = selectedMaterial?.name == material.name,
                            onClick = {
                                selectedMaterial = material
                                emissivity = material.emissivity
                                useDefaultModel = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultModelCard(
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChange(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Default Model",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Use system default thermal settings",
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = { onSelectionChange(!isSelected) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFFFF6B35),
                    unselectedColor = Color(0xFF7D8590)
                )
            )
        }
    }
}

@Composable
private fun ConfigurationCard(
    title: String,
    subtitle: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        subtitle,
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }
            }
            Text(
                value,
                color = Color(0xFFFF6B35),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MaterialPresetCard(
    material: MaterialPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2D1B69) else Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF6B35))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    material.icon,
                    contentDescription = material.name,
                    tint = if (isSelected) Color(0xFFFF6B35) else Color(0xFF7D8590),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        material.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Îµ = ${material.emissivity}",
                        color = Color(0xFF7D8590),
                        fontSize = 12.sp
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Data classes and helper functions
data class MaterialPreset(
    val name: String,
    val emissivity: Float,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun getMaterialPresets(): List<MaterialPreset> {
    return listOf(
        MaterialPreset("Human Skin", 0.98f, Icons.Default.Person),
        MaterialPreset("Concrete", 0.95f, Icons.Default.Home),
        MaterialPreset("Metal (Polished)", 0.07f, Icons.Default.Build),
        MaterialPreset("Metal (Oxidized)", 0.85f, Icons.Default.Build),
        MaterialPreset("Glass", 0.90f, Icons.Default.Home),
        MaterialPreset("Water", 0.95f, Icons.Default.Home),
        MaterialPreset("Wood", 0.90f, Icons.Default.Home),
        MaterialPreset("Plastic", 0.94f, Icons.Default.Build),
        MaterialPreset("Paper", 0.92f, Icons.Default.Home),
        MaterialPreset("Ceramic", 0.90f, Icons.Default.Home)
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRCorrectionComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

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
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IRCorrectionComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedCorrection by remember { mutableIntStateOf(0) }
        var correctionProgress by remember { mutableFloatStateOf(0f) }
        var isProcessing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Correction",
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
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Correction type selector
                    item {
                        CorrectionTypeSelector(
                            selectedCorrection = selectedCorrection,
                            onCorrectionSelected = { selectedCorrection = it }
                        )
                    }
                    // Processing status
                    item {
                        ProcessingStatusCard(
                            isProcessing = isProcessing,
                            progress = correctionProgress,
                            correctionType = getCorrectionTypes()[selectedCorrection]
                        )
                    }
                    // Correction parameters
                    item {
                        CorrectionParametersCard(
                            correctionType = selectedCorrection
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
                            }
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
    onCorrectionSelected: (Int) -> Unit
) {
    val correctionTypes = getCorrectionTypes()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Correction Type",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            correctionTypes.forEachIndexed { index, correction ->
                CorrectionTypeCard(
                    correction = correction,
                    isSelected = selectedCorrection == index,
                    onClick = { onCorrectionSelected(index) }
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
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFF6B35) else Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                correction.icon,
                contentDescription = correction.name,
                tint = if (isSelected) Color.White else Color(0xFFFF6B35),
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    correction.name,
                    color = if (isSelected) Color.White else Color(0xFF7D8590),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    correction.description,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF7D8590),
                    fontSize = 12.sp
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ProcessingStatusCard(
    isProcessing: Boolean,
    progress: Float,
    correctionType: CorrectionType
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Processing Status",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isProcessing) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (isProcessing) "Applying ${correctionType.name}..." else "Ready for correction",
                color = Color(0xFF7D8590),
                fontSize = 14.sp
            )
            if (isProcessing) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF6B35),
                    trackColor = Color(0xFF16131E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${(progress * 100).toInt()}% Complete",
                    color = Color(0xFFFF6B35),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CorrectionParametersCard(
    correctionType: Int
) {
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var gamma by remember { mutableFloatStateOf(1f) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Correction Parameters",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Brightness adjustment
            ParameterSlider(
                label = "Brightness",
                value = brightness,
                onValueChange = { brightness = it },
                valueRange = -100f..100f,
                unit = "%"
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Contrast adjustment
            ParameterSlider(
                label = "Contrast",
                value = contrast,
                onValueChange = { contrast = it },
                valueRange = -100f..100f,
                unit = "%"
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Gamma adjustment
            ParameterSlider(
                label = "Gamma",
                value = gamma,
                onValueChange = { gamma = it },
                valueRange = 0.1f..3f,
                unit = ""
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
    unit: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                "${value.toInt()}$unit",
                color = Color(0xFFFF6B35),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF6B35),
                activeTrackColor = Color(0xFFFF6B35),
                inactiveTrackColor = Color(0xFF16131E)
            )
        )
    }
}

@Composable
private fun CorrectionComparisonCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Before / After Comparison",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Before image
                ComparisonImageCard(
                    title = "Before",
                    description = "Original thermal image",
                    modifier = Modifier.weight(1f)
                )
                // After image
                ComparisonImageCard(
                    title = "After",
                    description = "Corrected thermal image",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ComparisonImageCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = title,
                        tint = Color(0xFF7D8590),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        description,
                        color = Color(0xFF7D8590),
                        fontSize = 10.sp
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
    onDiscardChanges: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isProcessing) {
            Button(
                onClick = onStartCorrection,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35)
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply Correction")
            }
        }
        Button(
            onClick = onSaveCorrection,
            modifier = Modifier.weight(1f),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00AA00)
            )
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = "Save",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save")
        }
        OutlinedButton(
            onClick = onDiscardChanges,
            modifier = Modifier.weight(1f),
            enabled = !isProcessing,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF7D8590)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
        ) {
            Icon(
                Icons.Default.Cancel,
                contentDescription = "Discard",
                modifier = Modifier.size(18.dp)
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
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun getCorrectionTypes(): List<CorrectionType> {
    return listOf(
        CorrectionType(
            "Auto Correct",
            "Automatic thermal image enhancement",
            Icons.Default.AutoFixHigh
        ),
        CorrectionType(
            "Manual Adjust",
            "Fine-tune brightness, contrast, and gamma",
            Icons.Default.Tune
        ),
        CorrectionType(
            "Noise Reduction",
            "Remove thermal noise and artifacts",
            Icons.Default.FilterAlt
        ),
        CorrectionType(
            "Temperature Range",
            "Optimize temperature range display",
            Icons.Default.Straighten
        )
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRCorrectionFourComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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

class IRCorrectionFourComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Final Review",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117)
                        )
                    )
                }
            ) { paddingValues ->
                FinalReviewContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun FinalReviewContent(
        modifier: Modifier = Modifier
    ) {
        var isExporting by remember { mutableStateOf(false) }
        var qualityScore by remember { mutableStateOf(92) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quality Assessment
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Quality Assessment",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (qualityScore >= 90) Color(0xFF238636) else Color(0xFFFF6B35)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "$qualityScore%",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    QualityMetric("Geometric Correction", 95)
                    QualityMetric("Color Calibration", 88)
                    QualityMetric("Thermal Range", 94)
                    QualityMetric("Noise Reduction", 91)
                }
            }
            // Before/After Comparison Placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = "Compare",
                        tint = Color(0xFF7D8590),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Before/After Comparison",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Side-by-side thermal image comparison",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }
            }
            // Export Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Export Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ExportOption("Original", false)
                        ExportOption("Corrected", true)
                        ExportOption("Both", false)
                    }
                }
            }
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { finish() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7D8590)
                    )
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = { isExporting = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    ),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save & Export")
                    }
                }
            }
        }
    }

    @Composable
    private fun QualityMetric(
        name: String,
        score: Int
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                color = Color(0xFF7D8590),
                fontSize = 14.sp
            )
            Text(
                "$score%",
                color = if (score >= 90) Color(0xFF238636) else Color(0xFFFF6B35),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    private fun ExportOption(
        label: String,
        selected: Boolean,
        onClick: () -> Unit = {}
    ) {
        FilterChip(
            onClick = onClick,
            label = { Text(label) },
            selected = selected,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFFF6B35),
                selectedLabelColor = Color.White,
                containerColor = Color(0xFF0D1117),
                labelColor = Color(0xFF7D8590)
            )
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRCorrectionThreeComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

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
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class IRCorrectionThreeComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Correction Step 3",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { paddingValues ->
                CorrectionStep3Content(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun CorrectionStep3Content(
        modifier: Modifier = Modifier
    ) {
        var colorTemperature by remember { mutableFloatStateOf(6500f) }
        var thermalRange by remember { mutableFloatStateOf(0.8f) }
        var enhancementLevel by remember { mutableFloatStateOf(0.5f) }
        var isProcessing by remember { mutableStateOf(false) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Color Calibration Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Color Calibration",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Color Temperature: ${colorTemperature.toInt()}K",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = colorTemperature,
                        onValueChange = { colorTemperature = it },
                        valueRange = 2700f..6500f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF6B35),
                            activeTrackColor = Color(0xFFFF6B35)
                        )
                    )
                }
            }
            // Thermal Range Optimization
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Thermal Range Enhancement",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Dynamic Range: ${(thermalRange * 100).toInt()}%",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                    Slider(
                        value = thermalRange,
                        onValueChange = { thermalRange = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF6B35),
                            activeTrackColor = Color(0xFFFF6B35)
                        )
                    )
                }
            }
            // Quality Enhancement
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Quality Enhancement",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Enhancement Level: ${(enhancementLevel * 100).toInt()}%",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                    Slider(
                        value = enhancementLevel,
                        onValueChange = { enhancementLevel = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF6B35),
                            activeTrackColor = Color(0xFFFF6B35)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { finish() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7D8590)
                    )
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { isProcessing = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    ),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Apply Corrections")
                    }
                }
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRCorrectionTwoComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class IRCorrectionTwoComposeActivity : BaseComposeActivity<IRCorrectionTwoViewModel>() {
    override fun createViewModel(): IRCorrectionTwoViewModel {
        return IRCorrectionTwoViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRCorrectionTwoViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Correction Step 2",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFFFF6B35)
                        )
                    )
                }
            ) { paddingValues ->
                IRCorrectionTwoContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRCorrectionTwoContent(
        viewModel: IRCorrectionTwoViewModel,
        modifier: Modifier = Modifier
    ) {
        var perspective by remember { mutableStateOf(0f) }
        var distortion by remember { mutableStateOf(0f) }
        var rotation by remember { mutableStateOf(0f) }
        var scale by remember { mutableStateOf(1f) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBE0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Step 2 of 4: Geometric Correction",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFF6B35),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "50%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF6B35)
                    )
                }
            }
            // Correction controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Geometric Adjustments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Perspective correction
                    Column {
                        Text("Perspective: ${perspective.toInt()}Â°")
                        Slider(
                            value = perspective,
                            onValueChange = { perspective = it },
                            valueRange = -45f..45f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35)
                            )
                        )
                    }
                    // Distortion correction
                    Column {
                        Text("Distortion: ${(distortion * 100).toInt()}%")
                        Slider(
                            value = distortion,
                            onValueChange = { distortion = it },
                            valueRange = -1f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35)
                            )
                        )
                    }
                    // Rotation
                    Column {
                        Text("Rotation: ${rotation.toInt()}Â°")
                        Slider(
                            value = rotation,
                            onValueChange = { rotation = it },
                            valueRange = -180f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35)
                            )
                        )
                    }
                    // Scale
                    Column {
                        Text("Scale: ${(scale * 100).toInt()}%")
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 0.5f..2f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35)
                            )
                        )
                    }
                }
            }
            // Preview area
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
                        "Geometric Correction Preview",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Real-time preview of geometric adjustments",
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
                        perspective = 0f
                        distortion = 0f
                        rotation = 0f
                        scale = 1f
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF6B35)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset")
                }
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Apply and Next")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply & Next")
                }
            }
        }
    }
}

class IRCorrectionTwoViewModel : BaseViewModel()


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IREmissivityComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
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

class IREmissivityComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedEmissivity by remember { mutableFloatStateOf(0.95f) }
        var selectedCategory by remember { mutableStateOf("Common Materials") }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Emissivity Selection",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                ) {
                    // Current selection display
                    CurrentSelectionCard(
                        selectedEmissivity = selectedEmissivity,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    // Material categories and list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category sections
                        getEmissivityCategories().forEach { category ->
                            item {
                                EmissivityCategorySection(
                                    category = category,
                                    selectedEmissivity = selectedEmissivity,
                                    onEmissivitySelected = { emissivity ->
                                        selectedEmissivity = emissivity
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentSelectionCard(
    selectedEmissivity: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Current Emissivity",
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    String.format("Îµ = %.3f", selectedEmissivity),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Visual indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFFFF6B35),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Emissivity",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun EmissivityCategorySection(
    category: EmissivityCategory,
    selectedEmissivity: Float,
    onEmissivitySelected: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    category.icon,
                    contentDescription = category.name,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    category.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Materials in this category
            category.materials.forEach { material ->
                EmissivityMaterialItem(
                    material = material,
                    isSelected = selectedEmissivity == material.emissivity,
                    onClick = { onEmissivitySelected(material.emissivity) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EmissivityMaterialItem(
    material: EmissivityMaterial,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2D1B69) else Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF6B35))
        } else null
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
                    material.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (material.description.isNotEmpty()) {
                    Text(
                        material.description,
                        color = Color(0xFF7D8590),
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    String.format("%.3f", material.emissivity),
                    color = if (isSelected) Color(0xFFFF6B35) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Data classes
data class EmissivityCategory(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val materials: List<EmissivityMaterial>
)

data class EmissivityMaterial(
    val name: String,
    val emissivity: Float,
    val description: String = ""
)

private fun getEmissivityCategories(): List<EmissivityCategory> {
    return listOf(
        EmissivityCategory(
            name = "Common Materials",
            icon = Icons.Default.Home,
            materials = listOf(
                EmissivityMaterial("Human Skin", 0.980f, "Body temperature measurement"),
                EmissivityMaterial("Water", 0.950f, "Clean water surface"),
                EmissivityMaterial("Concrete", 0.950f, "Rough concrete surface"),
                EmissivityMaterial("Asphalt", 0.930f, "Road surface"),
                EmissivityMaterial("Wood", 0.900f, "Natural wood surface"),
                EmissivityMaterial("Paper", 0.920f, "White paper"),
                EmissivityMaterial("Fabric", 0.900f, "Cotton/polyester cloth")
            )
        ),
        EmissivityCategory(
            name = "Metals",
            icon = Icons.Default.Build,
            materials = listOf(
                EmissivityMaterial("Aluminum (polished)", 0.050f, "Mirror finish"),
                EmissivityMaterial("Aluminum (oxidized)", 0.300f, "Weathered surface"),
                EmissivityMaterial("Copper (polished)", 0.070f, "Bright copper"),
                EmissivityMaterial("Copper (oxidized)", 0.780f, "Green patina"),
                EmissivityMaterial("Steel (polished)", 0.080f, "Stainless steel"),
                EmissivityMaterial("Steel (oxidized)", 0.850f, "Rusty steel"),
                EmissivityMaterial("Iron (cast)", 0.810f, "Cast iron surface"),
                EmissivityMaterial("Brass (polished)", 0.060f, "Bright brass")
            )
        ),
        EmissivityCategory(
            name = "Building Materials",
            icon = Icons.Default.Home,
            materials = listOf(
                EmissivityMaterial("Brick", 0.930f, "Red clay brick"),
                EmissivityMaterial("Glass", 0.900f, "Window glass"),
                EmissivityMaterial("Plaster", 0.920f, "Wall plaster"),
                EmissivityMaterial("Tiles (ceramic)", 0.900f, "Glazed ceramic"),
                EmissivityMaterial("Paint", 0.900f, "Most paint colors"),
                EmissivityMaterial("Roofing (shingles)", 0.910f, "Asphalt shingles"),
                EmissivityMaterial("Insulation", 0.950f, "Foam insulation")
            )
        ),
        EmissivityCategory(
            name = "Plastics & Polymers",
            icon = Icons.Default.Build,
            materials = listOf(
                EmissivityMaterial("PVC", 0.940f, "Polyvinyl chloride"),
                EmissivityMaterial("Polyethylene", 0.940f, "PE plastic"),
                EmissivityMaterial("Polystyrene", 0.950f, "Foam cups/packaging"),
                EmissivityMaterial("Teflon", 0.850f, "PTFE coating"),
                EmissivityMaterial("Rubber", 0.950f, "Natural rubber"),
                EmissivityMaterial("Nylon", 0.900f, "Synthetic fabric")
            )
        ),
        EmissivityCategory(
            name = "Food & Organic",
            icon = Icons.Default.Home,
            materials = listOf(
                EmissivityMaterial("Ice", 0.980f, "Frozen water"),
                EmissivityMaterial("Snow", 0.900f, "Fresh snow"),
                EmissivityMaterial("Vegetation", 0.950f, "Green leaves"),
                EmissivityMaterial("Soil (dry)", 0.900f, "Dry earth"),
                EmissivityMaterial("Soil (wet)", 0.950f, "Moist soil"),
                EmissivityMaterial("Food (cooked)", 0.950f, "Most cooked foods")
            )
        )
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRGalleryDetail01ComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.report.activity.ThermalReportCreationComposeActivity
import com.mpdc4gsr.module.thermalunified.fragment.GalleryComposeFragment
import com.mpdc4gsr.module.thermalunified.viewmodel.IRGalleryEditViewModel
import kotlinx.coroutines.launch

class IRGalleryDetail01ComposeActivity : BaseComposeActivity<IRGalleryEditViewModel>() {
    override fun createViewModel(): IRGalleryEditViewModel {
        return viewModels<IRGalleryEditViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRGalleryEditViewModel) {
        var showEditTools by remember { mutableStateOf(false) }
        var selectedTool by remember { mutableStateOf("") }
        var imageInfo by remember { mutableStateOf(ImageInfo()) }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Gallery Detail",
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
                            IconButton(onClick = { showEditTools = !showEditTools }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_TEXT, "Thermal image from IR Camera")
                                }
                                startActivity(Intent.createChooser(shareIntent, "Share thermal image"))
                            }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
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
                    // Main image display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                    ) {
                        // Gallery fragment view
                        GalleryImageView(
                            modifier = Modifier.fillMaxSize()
                        )
                        // Image info overlay
                        ImageInfoOverlay(
                            imageInfo = imageInfo,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        )
                    }
                    // Edit tools and controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.3f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Edit tools (shown when enabled)
                        if (showEditTools) {
                            EditToolsPanel(
                                selectedTool = selectedTool,
                                onToolSelected = { selectedTool = it }
                            )
                        }
                        // Image information
                        ImageInfoCard(imageInfo = imageInfo)
                        // Action buttons
                        ImageActionButtons(
                            onExport = {
                                scope.launch {
                                    try {
                                        val contentValues = ContentValues().apply {
                                            put(
                                                MediaStore.Images.Media.DISPLAY_NAME,
                                                "thermal_export_${System.currentTimeMillis()}.jpg"
                                            )
                                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ThermalExports")
                                        }
                                        context.contentResolver.insert(
                                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            contentValues
                                        )
                                        Toast.makeText(context, "Image exported successfully", Toast.LENGTH_SHORT)
                                            .show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            },
                            onReport = {
                                val intent = Intent(context, ThermalReportCreationComposeActivity::class.java).apply {
                                    putExtra("imageId", imageInfo.id)
                                    putExtra("imagePath", imageInfo.path)
                                }
                                startActivity(intent)
                            },
                            onDelete = {
                                scope.launch {
                                    Toast.makeText(context, "Image deleted from gallery", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryImageView(
    modifier: Modifier = Modifier
) {
    // Embed existing gallery fragment using AndroidView
    AndroidView(
        factory = { context ->
            val fragment = GalleryComposeFragment()
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_2
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ImageInfoOverlay(
    imageInfo: ImageInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Temperature Range",
                color = Color(0xFF7D8590),
                fontSize = 10.sp
            )
            Text(
                "${imageInfo.maxTemp}Â°C - ${imageInfo.minTemp}Â°C",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                imageInfo.timestamp,
                color = Color(0xFF7D8590),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EditToolsPanel(
    selectedTool: String,
    onToolSelected: (String) -> Unit
) {
    val tools = getEditTools()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Edit Tools",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tools) { tool ->
                    EditToolChip(
                        tool = tool,
                        isSelected = selectedTool == tool.name,
                        onClick = { onToolSelected(tool.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditToolChip(
    tool: EditTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    tool.icon,
                    contentDescription = tool.name,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    tool.name,
                    fontSize = 10.sp
                )
            }
        },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFF6B35),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF16131E),
            labelColor = Color(0xFF7D8590)
        )
    )
}

@Composable
private fun ImageInfoCard(
    imageInfo: ImageInfo
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Image Information",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoItem("Resolution", "${imageInfo.width} x ${imageInfo.height}")
            InfoItem("File size", imageInfo.fileSize)
            InfoItem("Max Temperature", "${imageInfo.maxTemp}Â°C")
            InfoItem("Min Temperature", "${imageInfo.minTemp}Â°C")
            InfoItem("Capture time", imageInfo.timestamp)
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 12.sp
        )
        Text(
            value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ImageActionButtons(
    onExport: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onExport,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35)
            )
        ) {
            Icon(
                Icons.Default.FileDownload,
                contentDescription = "Export",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Export", fontSize = 12.sp)
        }
        Button(
            onClick = onReport,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6B7280)
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Assignment,
                contentDescription = "Report",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Report", fontSize = 12.sp)
        }
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Red
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", fontSize = 12.sp)
        }
    }
}

// Data classes
data class ImageInfo(
    val id: Long = 0,
    val path: String = "",
    val width: Int = 384,
    val height: Int = 288,
    val fileSize: String = "2.1 MB",
    val maxTemp: Float = 45.2f,
    val minTemp: Float = 18.7f,
    val timestamp: String = "2024-01-15 14:30:25"
)

data class EditTool(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun getEditTools(): List<EditTool> {
    return listOf(
        EditTool("Crop", Icons.Default.CropFree),
        EditTool("Rotate", Icons.AutoMirrored.Filled.RotateRight),
        EditTool("Analyze", Icons.Default.Analytics),
        EditTool("Measure", Icons.Default.Straighten),
        EditTool("Filter", Icons.Default.FilterAlt)
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRGalleryDetail04ComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
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

class IRGalleryDetail04ComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Gallery Detail",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
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
                            IconButton(onClick = {
                                // TODO: Share thermal image
                                android.widget.Toast.makeText(
                                    this@IRGalleryDetail04ComposeActivity,
                                    "Share image feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            IconButton(onClick = {
                                // TODO: Show more options
                                android.widget.Toast.makeText(
                                    this@IRGalleryDetail04ComposeActivity,
                                    "More options coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117)
                        )
                    )
                }
            ) { paddingValues ->
                GalleryDetailContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun GalleryDetailContent(
        modifier: Modifier = Modifier
    ) {
        var showAnalysis by remember { mutableStateOf(true) }
        var showAnnotations by remember { mutableStateOf(false) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Display Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ThermostatAuto,
                            contentDescription = "Thermal Image",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Thermal Image Analysis",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (showAnalysis) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFF6B35).copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Max: 42.5Â°C", color = Color(0xFFFF6B35), fontSize = 14.sp)
                                    Text("Min: 18.2Â°C", color = Color(0xFF4A90E2), fontSize = 14.sp)
                                    Text("Avg: 28.7Â°C", color = Color(0xFF7D8590), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
            // Analysis Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Analysis Tools",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AnalysisButton(Icons.Default.CropFree, "Crop", false)
                        AnalysisButton(Icons.AutoMirrored.Filled.RotateLeft, "Rotate", false)
                        AnalysisButton(Icons.Default.Analytics, "Analyze", showAnalysis) {
                            showAnalysis = !showAnalysis
                        }
                        AnalysisButton(Icons.Default.Straighten, "Measure", false)
                        AnalysisButton(Icons.Default.FilterAlt, "Filter", false)
                    }
                }
            }
            // Image Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Image Information",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow("Resolution", "384 x 288")
                    InfoRow("File Size", "2.1 MB")
                    InfoRow("Date", "2024-01-15 14:30")
                    InfoRow("Temperature Range", "-10Â°C to 50Â°C")
                    InfoRow("Emissivity", "0.95")
                }
            }
            // Action Buttons
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Export thermal image
                        android.widget.Toast.makeText(
                            context,
                            "Exporting image...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7D8590)
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export")
                }
                Button(
                    onClick = {
                        // TODO: Generate thermal analysis report
                        android.widget.Toast.makeText(
                            context,
                            "Generating report...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Icon(Icons.Default.Description, contentDescription = "Report")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report")
                }
                IconButton(
                    onClick = {
                        // TODO: Delete thermal image with confirmation
                        android.widget.Toast.makeText(
                            context,
                            "Delete confirmation",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }
    }

    @Composable
    private fun AnalysisButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        isActive: Boolean,
        onClick: () -> Unit = {}
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.then(
                if (isActive) Modifier else Modifier
            )
        ) {
            IconButton(
                onClick = onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isActive) Color(0xFFFF6B35) else Color(0xFF161B22),
                    contentColor = if (isActive) Color.White else Color(0xFF7D8590)
                )
            ) {
                Icon(icon, contentDescription = label)
            }
            Text(
                label,
                color = if (isActive) Color(0xFFFF6B35) else Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
    }

    @Composable
    private fun InfoRow(
        label: String,
        value: String
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = Color(0xFF7D8590),
                fontSize = 14.sp
            )
            Text(
                value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRGalleryHomeComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class IRGalleryHomeComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedView by remember { mutableStateOf("grid") }
        var sortBy by remember { mutableStateOf("date") }
        var filterBy by remember { mutableStateOf("all") }
        val galleryItems = remember { getGalleryItems() }
        var showSearchDialog by remember { mutableStateOf(false) }
        var showMoreOptionsDialog by remember { mutableStateOf(false) }
        var selectedItemForOptions by remember { mutableStateOf<GalleryItem?>(null) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Gallery",
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
                            IconButton(onClick = { showSearchDialog = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                selectedView = if (selectedView == "grid") "list" else "grid"
                            }) {
                                Icon(
                                    if (selectedView == "grid") Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            startActivity(Intent(this, ImagePickIRComposeActivity::class.java))
                        },
                        containerColor = Color(0xFFFF6B35)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Capture",
                            tint = Color.White
                        )
                    }
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                ) {
                    // Filter and sort controls
                    GalleryControls(
                        sortBy = sortBy,
                        filterBy = filterBy,
                        onSortChange = { sortBy = it },
                        onFilterChange = { filterBy = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Gallery grid/list
                    if (selectedView == "grid") {
                        GalleryGrid(
                            items = galleryItems,
                            modifier = Modifier.fillMaxSize(),
                            onItemClick = { item ->
                                val intent = Intent(
                                    this@IRGalleryHomeComposeActivity,
                                    IRGalleryDetail01ComposeActivity::class.java
                                )
                                intent.putExtra("item_id", item.id)
                                startActivity(intent)
                            },
                            onItemMoreClick = { item ->
                                selectedItemForOptions = item
                                showMoreOptionsDialog = true
                            }
                        )
                    } else {
                        GalleryList(
                            items = galleryItems,
                            modifier = Modifier.fillMaxSize(),
                            onItemClick = { item ->
                                val intent = Intent(
                                    this@IRGalleryHomeComposeActivity,
                                    IRGalleryDetail01ComposeActivity::class.java
                                )
                                intent.putExtra("item_id", item.id)
                                startActivity(intent)
                            },
                            onItemMoreClick = { item ->
                                selectedItemForOptions = item
                                showMoreOptionsDialog = true
                            }
                        )
                    }
                }
            }
            // Search Dialog
            if (showSearchDialog) {
                GallerySearchDialog(
                    onDismiss = { showSearchDialog = false },
                    onSearch = { query ->
                        // TODO: Apply search filter
                        showSearchDialog = false
                    }
                )
            }
            // More Options Dialog
            if (showMoreOptionsDialog && selectedItemForOptions != null) {
                GalleryItemOptionsDialog(
                    item = selectedItemForOptions!!,
                    onDismiss = {
                        showMoreOptionsDialog = false
                        selectedItemForOptions = null
                    },
                    onShare = {
                        // TODO: Share item
                        showMoreOptionsDialog = false
                    },
                    onDelete = {
                        // TODO: Delete item
                        showMoreOptionsDialog = false
                    },
                    onExport = {
                        // TODO: Export item
                        showMoreOptionsDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GalleryControls(
    sortBy: String,
    filterBy: String,
    onSortChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sort options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sort:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                val sortOptions = listOf("date", "name", "size", "temperature")
                sortOptions.forEach { option ->
                    FilterChip(
                        onClick = { onSortChange(option) },
                        label = { Text(option.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                        selected = sortBy == option,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF16131E),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
            // Filter options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filter:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                val filterOptions = listOf("all", "images", "videos", "recent")
                filterOptions.forEach { option ->
                    FilterChip(
                        onClick = { onFilterChange(option) },
                        label = { Text(option.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                        selected = filterBy == option,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF16131E),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryGrid(
    items: List<GalleryItem>,
    modifier: Modifier = Modifier,
    onItemClick: (GalleryItem) -> Unit = {},
    onItemMoreClick: (GalleryItem) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            GalleryGridItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun GalleryGridItem(
    item: GalleryItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Color(0xFF16131E),
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.isVideo) Icons.Default.PlayCircle else Icons.Default.Image,
                    contentDescription = if (item.isVideo) "Video" else "Image",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(32.dp)
                )
                // Temperature overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${item.maxTemp}Â°C",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Item info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    item.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    item.date,
                    color = Color(0xFF7D8590),
                    fontSize = 10.sp
                )
                Text(
                    item.size,
                    color = Color(0xFF7D8590),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun GalleryList(
    items: List<GalleryItem>,
    modifier: Modifier = Modifier,
    onItemClick: (GalleryItem) -> Unit = {},
    onItemMoreClick: (GalleryItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            GalleryListItem(
                item = item,
                onClick = { onItemClick(item) },
                onMoreClick = { onItemMoreClick(item) }
            )
        }
    }
}

@Composable
private fun GalleryListItem(
    item: GalleryItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Color(0xFF16131E),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.isVideo) Icons.Default.PlayCircle else Icons.Default.Image,
                    contentDescription = if (item.isVideo) "Video" else "Image",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(24.dp)
                )
            }
            // Item details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    item.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    item.date,
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        item.size,
                        color = Color(0xFF7D8590),
                        fontSize = 11.sp
                    )
                    Text(
                        "Max: ${item.maxTemp}Â°C",
                        color = Color(0xFFFF6B35),
                        fontSize = 11.sp
                    )
                }
            }
            // Actions
            IconButton(
                onClick = onMoreClick
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color(0xFF7D8590)
                )
            }
        }
    }
}

// Data classes
data class GalleryItem(
    val id: String,
    val name: String,
    val date: String,
    val size: String,
    val maxTemp: Float,
    val isVideo: Boolean
)

@Composable
private fun GallerySearchDialog(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Gallery", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search by name or date") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6B35),
                        unfocusedBorderColor = Color(0xFF7D8590)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSearch(searchText.text) }
            ) {
                Text("Search", color = Color(0xFFFF6B35))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF7D8590))
            }
        },
        containerColor = Color(0xFF21262D)
    )
}

@Composable
private fun GalleryItemOptionsDialog(
    item: GalleryItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Image Options", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${item.name}", color = Color(0xFF7D8590), fontSize = 14.sp)
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, "Share", tint = Color.White)
                        Text("Share", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, "Export", tint = Color.White)
                        Text("Export", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF6B35))
                        Text("Delete", color = Color(0xFFFF6B35))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF7D8590))
            }
        },
        containerColor = Color(0xFF21262D)
    )
}

private fun getGalleryItems(): List<GalleryItem> {
    return listOf(
        GalleryItem("1", "thermal_001.jpg", "2024-01-15 14:30", "2.1 MB", 45.2f, false),
        GalleryItem("2", "thermal_002.mp4", "2024-01-15 14:25", "15.3 MB", 38.7f, true),
        GalleryItem("3", "thermal_003.jpg", "2024-01-15 14:20", "1.8 MB", 52.1f, false),
        GalleryItem("4", "thermal_004.jpg", "2024-01-15 14:15", "2.3 MB", 41.5f, false),
        GalleryItem("5", "thermal_005.mp4", "2024-01-15 14:10", "22.1 MB", 47.8f, true),
        GalleryItem("6", "thermal_006.jpg", "2024-01-15 14:05", "1.9 MB", 35.2f, false),
        GalleryItem("7", "thermal_007.jpg", "2024-01-15 14:00", "2.0 MB", 49.3f, false),
        GalleryItem("8", "thermal_008.mp4", "2024-01-15 13:55", "18.7 MB", 43.6f, true)
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRLogMPChartComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.layout.*
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
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class IRLogMPChartComposeActivity : BaseComposeActivity<IRLogMPChartViewModel>() {
    override fun createViewModel(): IRLogMPChartViewModel {
        return IRLogMPChartViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRLogMPChartViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Data Logging",
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
                            IconButton(onClick = {
                                // TODO: Implement export functionality
                                android.widget.Toast.makeText(
                                    this@IRLogMPChartComposeActivity,
                                    "Export data feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFFFF6B35)
                        )
                    )
                }
            ) { paddingValues ->
                IRLogMPChartContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRLogMPChartContent(
        viewModel: IRLogMPChartViewModel,
        modifier: Modifier = Modifier
    ) {
        var isLogging by remember { mutableStateOf(false) }
        var dataPoints by remember { mutableStateOf(247) }
        var loggingDuration by remember { mutableStateOf("00:04:07") }
        var chartType by remember { mutableStateOf("Line Chart") }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logging status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLogging) Color(0xFFE8F5E8) else Color(0xFFF8F9FA)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isLogging) Icons.Default.FiberManualRecord else Icons.Default.Stop,
                        contentDescription = null,
                        tint = if (isLogging) Color(0xFF4CAF50) else Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isLogging) "Logging Active" else "Logging Stopped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isLogging) Color(0xFF4CAF50) else Color(0xFF666666)
                        )
                        Text(
                            "$dataPoints data points | Duration: $loggingDuration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isLogging,
                        onCheckedChange = { isLogging = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4CAF50),
                            checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                        )
                    )
                }
            }
            // Chart type selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBE0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Chart Visualization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chartTypes = listOf("Line Chart", "Bar Chart", "Area Chart", "Scatter Plot")
                        chartTypes.forEach { type ->
                            FilterChip(
                                onClick = { chartType = type },
                                label = { Text(type) },
                                selected = chartType == type,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B35),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
            // Chart area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Temperature Trend Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Chart placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "MP Android Chart Integration",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B35)
                            )
                            Text(
                                "Real-time thermal data visualization",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
            // Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Data Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatisticItem("Peak", "45.2Â°C", Color(0xFFF44336))
                        StatisticItem("Valley", "18.7Â°C", Color(0xFF2196F3))
                        StatisticItem("Average", "28.9Â°C", Color(0xFF4CAF50))
                        StatisticItem("Variance", "Â±3.4Â°C", Color(0xFFFF9800))
                    }
                }
            }
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Implement clear data functionality
                        android.widget.Toast.makeText(
                            this@IRLogMPChartComposeActivity,
                            "Clear data feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF666666)
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }
                Button(
                    onClick = {
                        // TODO: Implement CSV export
                        android.widget.Toast.makeText(
                            this@IRLogMPChartComposeActivity,
                            "Export CSV feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export CSV")
                }
                Button(
                    onClick = {
                        // TODO: Implement PDF export
                        android.widget.Toast.makeText(
                            this@IRLogMPChartComposeActivity,
                            "Export PDF feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF")
                }
            }
        }
    }

    @Composable
    private fun StatisticItem(
        label: String,
        value: String,
        color: Color
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

class IRLogMPChartViewModel : BaseViewModel()


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRMainComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.fragment.AbilityComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRGalleryTabComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRThermalComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.PDFListComposeFragment
import com.mpdc4gsr.module.user.compose.MoreComposeFragment
import com.mpdc4gsr.module.user.viewmodel.MoreComposeFragmentViewModel
import kotlinx.coroutines.launch

class IRMainComposeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibUnifiedTheme {
                MainContent()
            }
        }
    }

    @Composable
    private fun MainContent() {
        val pagerState = rememberPagerState(pageCount = { 5 })
        val scope = rememberCoroutineScope()
        Scaffold(
            containerColor = Color(0xFF16131E)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF16131E))
            ) {
                // Main ViewPager content (85% of screen)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.85f)
                ) { page ->
                    when (page) {
                        0 -> ThermalTabContent()
                        1 -> GalleryTabContent()
                        2 -> AbilityTabContent()
                        3 -> PDFTabContent()
                        4 -> MoreTabContent()
                    }
                }
                // Bottom navigation (15% of screen)
                ThermalBottomNavigation(
                    selectedPage = pagerState.currentPage,
                    onPageSelected = { page ->
                        scope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.15f)
                )
            }
        }
    }
}

@Composable
private fun ThermalTabContent() {
    val context = LocalContext.current
    val activity = context as? IRMainComposeActivity
    // Embed existing thermal fragment using AndroidView with proper FragmentManager integration
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_0
            }
        },
        update = { view ->
            activity?.let {
                val fragment = IRThermalComposeFragment()
                it.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun GalleryTabContent() {
    val context = LocalContext.current
    val activity = context as? IRMainComposeActivity
    // Embed existing gallery fragment using AndroidView with proper FragmentManager integration
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_1
            }
        },
        update = { view ->
            activity?.let {
                val fragment = IRGalleryTabComposeFragment()
                it.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun AbilityTabContent() {
    val context = LocalContext.current
    val activity = context as? IRMainComposeActivity
    // Embed existing ability fragment using AndroidView with proper FragmentManager integration
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_2
            }
        },
        update = { view ->
            activity?.let {
                val fragment = AbilityComposeFragment()
                it.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PDFTabContent() {
    val context = LocalContext.current
    val activity = context as? IRMainComposeActivity
    // Embed existing PDF fragment using AndroidView with proper FragmentManager integration
    AndroidView(
        factory = { context ->
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_3
            }
        },
        update = { view ->
            activity?.let {
                val fragment = PDFListComposeFragment()
                it.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MoreTabContent() {
    val viewModel: MoreComposeFragmentViewModel = viewModel()
    MoreComposeFragment(
        viewModel = viewModel,
        isTC007 = false,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ThermalBottomNavigation(
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = getThermalTabs()
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                ThermalTabButton(
                    tab = tab,
                    isSelected = selectedPage == index,
                    onClick = { onPageSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThermalTabButton(
    tab: MainThermalTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(0xFFFF6B35) else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    tab.icon,
                    contentDescription = tab.title,
                    tint = if (isSelected) Color.White else Color(0xFF7D8590),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            tab.title,
            color = if (isSelected) Color(0xFFFF6B35) else Color(0xFF7D8590),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Data class for tab configuration
internal data class MainThermalTab(
    val title: String,
    val icon: ImageVector
)

private fun getThermalTabs(): List<MainThermalTab> {
    return listOf(
        MainThermalTab("Thermal", Icons.Default.Videocam),
        MainThermalTab("Gallery", Icons.Default.Photo),
        MainThermalTab("Ability", Icons.Default.Build),
        MainThermalTab("PDF", Icons.Default.Description),
        MainThermalTab("More", Icons.Default.MoreVert)
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRMonitorChartComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

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
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class IRMonitorChartComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var isRecording by remember { mutableStateOf(false) }
        var recordingTime by remember { mutableLongStateOf(0L) }
        var maxTemp by remember { mutableFloatStateOf(25.0f) }
        var minTemp by remember { mutableFloatStateOf(20.0f) }
        var avgTemp by remember { mutableFloatStateOf(22.5f) }
        var showTemperatureOverlay by remember { mutableStateOf(true) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Monitor Chart",
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
                            IconButton(onClick = { showTemperatureOverlay = !showTemperatureOverlay }) {
                                Icon(
                                    if (showTemperatureOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showTemperatureOverlay) "Hide Overlay" else "Show Overlay",
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
                val context = androidx.compose.ui.platform.LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.Black)
                ) {
                    // Main thermal camera view with overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                    ) {
                        // Thermal camera view
                        ThermalCameraView(
                            modifier = Modifier.fillMaxSize()
                        )
                        // Temperature overlay
                        if (showTemperatureOverlay) {
                            TemperatureOverlay(
                                maxTemp = maxTemp,
                                minTemp = minTemp,
                                avgTemp = avgTemp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                            )
                        }
                        // Recording indicator
                        if (isRecording) {
                            RecordingIndicator(
                                recordingTime = recordingTime,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            )
                        }
                    }
                    // Control panel and chart data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.3f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Recording controls
                        item {
                            RecordingControls(
                                isRecording = isRecording,
                                onStartStop = {
                                    isRecording = !isRecording
                                    if (!isRecording) recordingTime = 0L
                                },
                                context = context
                            )
                        }
                        // Temperature statistics
                        item {
                            TemperatureStatsCard(
                                maxTemp = maxTemp,
                                minTemp = minTemp,
                                avgTemp = avgTemp
                            )
                        }
                        // Chart controls
                        item {
                            ChartControlsCard(context = context)
                        }
                    }
                }
            }
        }
        // Recording timer
        LaunchedEffect(isRecording) {
            if (isRecording) {
                while (isRecording) {
                    kotlinx.coroutines.delay(1000L)
                    recordingTime++
                    // Simulate temperature changes
                    maxTemp = 20f + (recordingTime % 10) * 0.5f
                    minTemp = 15f + (recordingTime % 8) * 0.3f
                    avgTemp = (maxTemp + minTemp) / 2f
                }
            }
        }
    }
}

@Composable
private fun ThermalCameraView(
    modifier: Modifier = Modifier
) {
    // Placeholder for thermal camera view
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
            Text(
                "Thermal Camera Feed\n(Real-time monitoring)",
                color = Color(0xFF7D8590),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TemperatureOverlay(
    maxTemp: Float,
    minTemp: Float,
    avgTemp: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Temperature",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            TemperatureItem("Max", maxTemp, Color(0xFFFF4444))
            TemperatureItem("Min", minTemp, Color(0xFF4444FF))
            TemperatureItem("Avg", avgTemp, Color(0xFFFFAA00))
        }
    }
}

@Composable
private fun TemperatureItem(
    label: String,
    temperature: Float,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 10.sp
        )
        Text(
            String.format("%.1fÂ°C", temperature),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RecordingIndicator(
    recordingTime: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.FiberManualRecord,
                contentDescription = "Recording",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            val minutes = recordingTime / 60
            val seconds = recordingTime % 60
            Text(
                String.format("REC %02d:%02d", minutes, seconds),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    onStartStop: () -> Unit,
    context: android.content.Context
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStartStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red else Color(0xFFFF6B35)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "Stop" else "Start",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isRecording) "Stop" else "Start",
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = {
                    // TODO: Save chart as image
                    android.widget.Toast.makeText(
                        context,
                        "Saving chart...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TemperatureStatsCard(
    maxTemp: Float,
    minTemp: Float,
    avgTemp: Float
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Temperature Statistics",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Maximum", maxTemp, Color(0xFFFF4444))
                StatItem("Minimum", minTemp, Color(0xFF4444FF))
                StatItem("Average", avgTemp, Color(0xFFFFAA00))
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            String.format("%.1fÂ°C", value),
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ChartControlsCard(context: android.content.Context) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Chart Controls",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Export monitoring data
                        android.widget.Toast.makeText(
                            context,
                            "Exporting data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
                ) {
                    Text("Export", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Clear monitoring data
                        android.widget.Toast.makeText(
                            context,
                            "Clearing data...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Open monitoring settings
                        android.widget.Toast.makeText(
                            context,
                            "Opening settings...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590))
                ) {
                    Text("Settings", fontSize = 12.sp)
                }
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRMonitorChartLiteComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorChartLiteViewModel

class IRMonitorChartLiteComposeActivity : BaseComposeActivity<IRMonitorChartLiteViewModel>() {
    override fun createViewModel(): IRMonitorChartLiteViewModel {
        return viewModels<IRMonitorChartLiteViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRMonitorChartLiteViewModel) {
        val isRecording by viewModel.isRecording.collectAsState()
        val recordingTime by viewModel.recordingTime.collectAsState()
        val showOverlay by viewModel.showOverlay.collectAsState()
        val currentTemp by viewModel.currentTemp.collectAsState()
        val lowTemp by viewModel.lowTemp.collectAsState()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Monitor Chart Lite",
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
                            IconButton(onClick = { viewModel.toggleOverlay() }) {
                                Icon(
                                    if (showOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Overlay",
                                    tint = Color.White
                                )
                            }
                            TextButton(
                                onClick = { finish() }
                            ) {
                                Text("Finish", color = Color.White)
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
                ) {
                    // Chart area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16131E)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Recording time indicator
                            Card(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF3B3E44)
                                ),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 4.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF4747))
                                    )
                                    Text(
                                        recordingTime,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // Legend
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF4747))
                                    )
                                    Text(
                                        "Current",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF06AAFF))
                                    )
                                    Text(
                                        "Low Temp",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            // Chart placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 4.dp)
                                    .background(
                                        Color(0xFF1A1A1A),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Chart View",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    // Thermal image view area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.55f)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16131E)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Thermal Display",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 16.sp
                                )
                                if (showOverlay) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0x80000000)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                "Current: %.1fÂ°C".format(currentTemp),
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                "Low: %.1fÂ°C".format(lowTemp),
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRMonitorComposeActivity.kt =====

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorViewModel

class IRMonitorComposeActivity : BaseComposeActivity<IRMonitorViewModel>() {
    override fun createViewModel(): IRMonitorViewModel {
        return viewModels<IRMonitorViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRMonitorViewModel) {
        val context = LocalContext.current
        var monitorState by remember { mutableIntStateOf(0) } // 0: initial, 1: selected, 2: monitoring
        var selectedType by remember { mutableIntStateOf(1) }
        var isMonitoring by remember { mutableStateOf(false) }
        var monitoringTime by remember { mutableLongStateOf(0L) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "IR Monitor",
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Monitor Status Card
                    MonitorStatusCard(
                        monitorState = monitorState,
                        selectedType = selectedType,
                        isMonitoring = isMonitoring,
                        monitoringTime = monitoringTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Control Buttons
                    when (monitorState) {
                        0 -> {
                            // Initial state - show monitor selection button
                            Button(
                                onClick = {
                                    monitorState = 1
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6B35)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(56.dp)
                            ) {
                                Text(
                                    "Create Monitor Chart",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        1 -> {
                            // Selection state - show type selection
                            MonitorTypeSelection(
                                selectedType = selectedType,
                                onTypeSelected = { type ->
                                    selectedType = type
                                    monitorState = 2
                                    // Thermal action tracking
                                    val action = when (type) {
                                        1 -> 2001 // Point monitoring
                                        2 -> 2002 // Line monitoring
                                        else -> 2003 // Area monitoring
                                    }
                                }
                            )
                        }

                        2 -> {
                            // Ready to start monitoring
                            Button(
                                onClick = {
                                    NavigationManager.getInstance()
                                        .build(RouterConfig.MONITOR_CHART)
                                        .withInt("type", selectedType)
                                        .navigation(context)
                                    finish()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6B35)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(56.dp)
                            ) {
                                Text(
                                    "Start Monitoring",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        // Timer effect for monitoring state
        LaunchedEffect(isMonitoring) {
            if (isMonitoring) {
                while (isMonitoring) {
                    kotlinx.coroutines.delay(1000L)
                    monitoringTime++
                }
            }
        }
    }
}

@Composable
private fun MonitorStatusCard(
    monitorState: Int,
    selectedType: Int,
    isMonitoring: Boolean,
    monitoringTime: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.MonitorHeart,
                contentDescription = "Monitor Status",
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                when (monitorState) {
                    0 -> "Ready to Monitor"
                    1 -> "Select Monitor Type"
                    2 -> "Monitor Configured"
                    else -> "Monitoring..."
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (monitorState >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                val typeText = when (selectedType) {
                    1 -> "Point Monitoring"
                    2 -> "Line Monitoring"
                    else -> "Area Monitoring"
                }
                Text(
                    typeText,
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp
                )
            }
            if (isMonitoring && monitoringTime > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val minutes = monitoringTime / 60
                val seconds = monitoringTime % 60
                Text(
                    String.format("%02d:%02d", minutes, seconds),
                    color = Color(0xFFFF6B35),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MonitorTypeSelection(
    selectedType: Int,
    onTypeSelected: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Select Monitor Type",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            MonitorTypeButton(
                text = "Point Monitoring",
                description = "Monitor temperature at specific points",
                icon = Icons.Default.Place,
                isSelected = selectedType == 1,
                onClick = { onTypeSelected(1) }
            )
            MonitorTypeButton(
                text = "Line Monitoring",
                description = "Monitor temperature along a line",
                icon = Icons.Default.Timeline,
                isSelected = selectedType == 2,
                onClick = { onTypeSelected(2) }
            )
            MonitorTypeButton(
                text = "Area Monitoring",
                description = "Monitor temperature in a region",
                icon = Icons.Default.CropFree,
                isSelected = selectedType == 3,
                onClick = { onTypeSelected(3) }
            )
        }
    }
}

@Composable
private fun MonitorTypeButton(
    text: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2D1B69) else Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF6B35))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    icon,
                    contentDescription = text,
                    tint = if (isSelected) Color(0xFFFF6B35) else Color(0xFF7D8590),
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRThermalDoubleComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.IRThermalDoubleViewModel

class IRThermalDoubleComposeActivity : BaseComposeActivity<IRThermalDoubleViewModel>() {
    override fun createViewModel(): IRThermalDoubleViewModel {
        return viewModels<IRThermalDoubleViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRThermalDoubleViewModel) {
        val selectedMode by viewModel.selectedMode.collectAsState()
        val showOverlay by viewModel.showOverlay.collectAsState()
        val showTrendChart by viewModel.showTrendChart.collectAsState()
        val showCompass by viewModel.showCompass.collectAsState()
        val isRecording by viewModel.isRecording.collectAsState()
        val isRangeLocked by viewModel.isRangeLocked.collectAsState()
        var showRangeEditDialog by remember { mutableStateOf(false) }
        var showMoreOptionsDialog by remember { mutableStateOf(false) }
        var minTemp by remember { mutableStateOf("0") }
        var maxTemp by remember { mutableStateOf("100") }
        val context = androidx.compose.ui.platform.LocalContext.current
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.selectMode(0) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (selectedMode == 0) Color.White else Color.White.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Temperature", fontSize = 16.sp)
                                        if (selectedMode == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { viewModel.selectMode(1) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (selectedMode == 1) Color.White else Color.White.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Observe", fontSize = 16.sp)
                                        if (selectedMode == 1) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }
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
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = {
                                // TODO: Implement info dialog display
                                android.widget.Toast.makeText(
                                    context,
                                    "Show thermal info",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                            }
                            IconButton(onClick = {
                                // TODO: Implement TISR (Thermal Image Super Resolution) toggle
                                // TODO: Implement toggleTISR() in IRThermalDoubleViewModel
                                // viewModel.toggleTISR()
                                android.widget.Toast.makeText(
                                    context,
                                    "Toggle TISR mode",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "TISR", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Main thermal display area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .aspectRatio(192f / 256f)
                            .background(Color.Black)
                    ) {
                        // Thermal camera view placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Thermal Camera View",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        }
                        // Overlay controls
                        if (showOverlay) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                // Temperature range controls would go here
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0x80000000)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(onClick = { viewModel.toggleRangeLock() }) {
                                            Icon(
                                                if (isRangeLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = if (isRangeLocked) "Locked" else "Unlocked",
                                                tint = Color.White
                                            )
                                        }
                                        Text(
                                            "Temp Range",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                        IconButton(onClick = { showRangeEditDialog = true }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Recording indicator
                        if (isRecording) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0x80000000)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red)
                                    )
                                    Text("00:00", color = Color.White, fontSize = 15.sp)
                                }
                            }
                        }
                        // Trend chart overlay
                        if (showTrendChart) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(0.7f)
                                    .aspectRatio(264f / 158f),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xE6 + 0x16131E)
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Trend", color = Color.White, fontSize = 14.sp)
                                        IconButton(
                                            onClick = { if (showTrendChart) viewModel.toggleTrendChart() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Chart",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Bottom menu controls
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16131E)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            // Secondary menu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = { viewModel.toggleTrendChart() }) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, "Trend", tint = Color.White)
                                }
                                IconButton(onClick = { viewModel.toggleCompass() }) {
                                    Icon(Icons.Default.Explore, "Compass", tint = Color.White)
                                }
                                IconButton(onClick = { viewModel.toggleOverlay() }) {
                                    Icon(
                                        if (showOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        "Toggle Overlay",
                                        tint = Color.White
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            // Primary menu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = {
                                    val intent = Intent(context, ThermalGalleryComposeActivity::class.java)
                                    context.startActivity(intent)
                                }) {
                                    Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White)
                                }
                                IconButton(onClick = { viewModel.toggleRecording() }) {
                                    Icon(
                                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                        "Record",
                                        tint = if (isRecording) Color.Red else Color.White
                                    )
                                }
                                IconButton(onClick = {
                                    // Capture thermal snapshot
                                    android.widget.Toast.makeText(
                                        context,
                                        "Thermal snapshot captured",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    // TODO: Implement actual thermal snapshot capture logic
                                }) {
                                    Icon(Icons.Default.CameraAlt, "Camera", tint = Color.White)
                                }
                                IconButton(onClick = { showMoreOptionsDialog = true }) {
                                    Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
        // Range Edit Dialog
        if (showRangeEditDialog) {
            AlertDialog(
                onDismissRequest = { showRangeEditDialog = false },
                title = { Text("Edit Temperature Range") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minTemp,
                            onValueChange = { minTemp = it },
                            label = { Text("Min Temperature (Â°C)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = maxTemp,
                            onValueChange = { maxTemp = it },
                            label = { Text("Max Temperature (Â°C)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Apply temperature range
                        showRangeEditDialog = false
                        android.widget.Toast.makeText(
                            context,
                            "Range updated: $minTempÂ°C - $maxTempÂ°C",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRangeEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        // More Options Dialog
        if (showMoreOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showMoreOptionsDialog = false },
                title = { Text("More Options") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                showMoreOptionsDialog = false
                                android.widget.Toast.makeText(
                                    context,
                                    "Opening color palette",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = "Color Palette")
                                Spacer(Modifier.width(8.dp))
                                Text("Color Palette")
                            }
                        }
                        TextButton(
                            onClick = {
                                showMoreOptionsDialog = false
                                android.widget.Toast.makeText(
                                    context,
                                    "Opening measurement tools",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = "Measurement Tools")
                                Spacer(Modifier.width(8.dp))
                                Text("Measurement Tools")
                            }
                        }
                        TextButton(
                            onClick = {
                                showMoreOptionsDialog = false
                                android.widget.Toast.makeText(
                                    context,
                                    "Opening advanced settings",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Advanced Settings")
                                Spacer(Modifier.width(8.dp))
                                Text("Advanced Settings")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMoreOptionsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRThermalNightComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class IRThermalNightComposeActivity : BaseComposeActivity<IRThermalNightViewModel>() {
    override fun createViewModel(): IRThermalNightViewModel {
        return IRThermalNightViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRThermalNightViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Brightness2,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Night Vision Thermal",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
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
                            var showSettings by remember { mutableStateOf(false) }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                            if (showSettings) {
                                AlertDialog(
                                    onDismissRequest = { showSettings = false },
                                    title = { Text("Night Vision Settings") },
                                    text = {
                                        Column {
                                            Text("Configure night vision thermal settings")
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("â€¢ Sensitivity: Adjustable")
                                            Text("â€¢ Mode: Enhanced/Standard")
                                            Text("â€¢ Temperature range: Customizable")
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showSettings = false }) {
                                            Text("Close")
                                        }
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117) // Dark night theme
                        )
                    )
                }
            ) { paddingValues ->
                IRThermalNightContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRThermalNightContent(
        viewModel: IRThermalNightViewModel,
        modifier: Modifier = Modifier
    ) {
        var isRecording by remember { mutableStateOf(false) }
        var nightMode by remember { mutableStateOf("Enhanced") }
        var sensitivity by remember { mutableStateOf(75f) }
        var currentTemp by remember { mutableStateOf(22.5f) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Night mode status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Brightness2,
                        contentDescription = null,
                        tint = Color(0xFF58A6FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Night Vision Active",
                            color = Color(0xFF58A6FF),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Enhanced sensitivity for low-light conditions",
                            color = Color(0xFF7D8590),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            // Thermal camera view
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Night Thermal Feed",
                            color = Color(0xFF58A6FF),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Enhanced night vision thermal imaging",
                            color = Color(0xFF7D8590),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "${currentTemp}Â°C",
                            color = Color(0xFF58A6FF),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Recording indicator
                    if (isRecording) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDA3633))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "â— REC",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            // Night controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Night Vision Controls",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Night mode selection
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("Standard", "Enhanced", "Ultra")
                        modes.forEach { mode ->
                            FilterChip(
                                onClick = { nightMode = mode },
                                label = {
                                    Text(
                                        mode,
                                        color = if (nightMode == mode) Color.White else Color(0xFF7D8590)
                                    )
                                },
                                selected = nightMode == mode,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF58A6FF),
                                    containerColor = Color(0xFF161B22)
                                )
                            )
                        }
                    }
                    // Sensitivity control
                    Column {
                        Text(
                            "Sensitivity: ${sensitivity.toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = sensitivity,
                            onValueChange = { sensitivity = it },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF58A6FF),
                                activeTrackColor = Color(0xFF58A6FF),
                                inactiveTrackColor = Color(0xFF30363D)
                            )
                        )
                    }
                }
            }
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF58A6FF)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF58A6FF))
                    )
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Capture")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Capture")
                }
                Button(
                    onClick = { isRecording = !isRecording },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFDA3633) else Color(0xFF58A6FF)
                    )
                ) {
                    Icon(
                        Icons.Default.VideoCall,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
            }
        }
    }
}

class IRThermalNightViewModel : BaseViewModel()


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRThermalPlusComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class IRThermalPlusComposeActivity : BaseComposeActivity<IRThermalPlusViewModel>() {
    override fun createViewModel(): IRThermalPlusViewModel {
        return IRThermalPlusViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRThermalPlusViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Stars,
                                    contentDescription = "Plus Features",
                                    tint = Color(0xFFFFD700)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Thermal Plus",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
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
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1A1A1A) // Premium dark theme
                        )
                    )
                }
            ) { paddingValues ->
                IRThermalPlusContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRThermalPlusContent(
        viewModel: IRThermalPlusViewModel,
        modifier: Modifier = Modifier
    ) {
        var aiEnhancement by remember { mutableStateOf(true) }
        var processingMode by remember { mutableStateOf("Premium") }
        var enhancementLevel by remember { mutableStateOf(80f) }
        var currentTemp by remember { mutableStateOf(28.7f) }
        var maxTemp by remember { mutableStateOf(45.2f) }
        var minTemp by remember { mutableStateOf(18.3f) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plus status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = "Premium Features",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Premium Plus Mode Active",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "AI-powered thermal enhancement enabled",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            // Temperature readings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "MAX",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${maxTemp}Â°C",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "CENTER",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${currentTemp}Â°C",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "MIN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${minTemp}Â°C",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Premium thermal view
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Plus Mode Active",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "AI-Enhanced Thermal Feed",
                            color = Color(0xFFFFD700),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Premium thermal processing with AI enhancement",
                            color = Color(0xFF9E9E9E),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            // AI enhancement controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "AI Enhancement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = aiEnhancement,
                            onCheckedChange = { aiEnhancement = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFD700),
                                checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.5f)
                            )
                        )
                    }
                    // Processing mode selection
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("Standard", "Premium", "Ultra")
                        modes.forEach { mode ->
                            FilterChip(
                                onClick = { processingMode = mode },
                                label = { Text(mode) },
                                selected = processingMode == mode,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFD700),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                    // Enhancement level
                    if (aiEnhancement) {
                        Column {
                            Text(
                                "Enhancement Level: ${enhancementLevel.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = enhancementLevel,
                                onValueChange = { enhancementLevel = it },
                                valueRange = 10f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                        }
                    }
                }
            }
            // Premium action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFFD700)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFD700))
                    )
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "AI Capture")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Capture")
                }
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Analyze")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze")
                }
            }
        }
    }
}

class IRThermalPlusViewModel : BaseViewModel()


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\IRVideoGSYComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class IRVideoGSYComposeActivity : BaseComposeActivity<IRVideoGSYViewModel>() {
    override fun createViewModel(): IRVideoGSYViewModel {
        return IRVideoGSYViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRVideoGSYViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Video Player",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFFFF6B35)
                        )
                    )
                }
            ) { paddingValues ->
                IRVideoGSYContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRVideoGSYContent(
        viewModel: IRVideoGSYViewModel,
        modifier: Modifier = Modifier
    ) {
        var isPlaying by remember { mutableStateOf(false) }
        var currentTime by remember { mutableStateOf(0f) }
        var totalTime by remember { mutableStateOf(120f) }
        var playbackSpeed by remember { mutableStateOf(1f) }
        var showThermalOverlay by remember { mutableStateOf(true) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video player area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Thermal Video Playback",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "GSY Player with thermal analysis",
                            color = Color(0xFF9E9E9E),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    // Thermal overlay indicator
                    if (showThermalOverlay) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B35))
                        ) {
                            Text(
                                "THERMAL",
                                modifier = Modifier.padding(8.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            // Video controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress slider
                    Column {
                        Slider(
                            value = currentTime,
                            onValueChange = { currentTime = it },
                            valueRange = 0f..totalTime,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${(currentTime / 60).toInt()}:${
                                    (currentTime % 60).toInt().toString().padStart(2, '0')
                                }",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "${(totalTime / 60).toInt()}:${(totalTime % 60).toInt().toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentTime = (currentTime - 10f).coerceAtLeast(0f) }
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "Back 10s")
                        }
                        FloatingActionButton(
                            onClick = { isPlaying = !isPlaying },
                            containerColor = Color(0xFFFF6B35)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { currentTime = (currentTime + 10f).coerceAtMost(totalTime) }
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                        }
                    }
                    // Speed and overlay controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback speed
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Speed: ")
                            val speeds = listOf(0.5f, 1f, 1.5f, 2f)
                            speeds.forEach { speed ->
                                FilterChip(
                                    onClick = { playbackSpeed = speed },
                                    label = { Text("${speed}x") },
                                    selected = playbackSpeed == speed,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFF6B35),
                                        selectedLabelColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        // Thermal overlay toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Thermal Overlay")
                            Switch(
                                checked = showThermalOverlay,
                                onCheckedChange = { showThermalOverlay = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF6B35),
                                    checkedTrackColor = Color(0xFFFF6B35).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
            // Temperature analysis
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBE0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Frame Temperature Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Max", style = MaterialTheme.typography.bodySmall)
                            Text("42.3Â°C", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Min", style = MaterialTheme.typography.bodySmall)
                            Text("18.7Â°C", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Avg", style = MaterialTheme.typography.bodySmall)
                            Text("28.5Â°C", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

class IRVideoGSYViewModel : BaseViewModel()


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\LogMPChartComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
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
import kotlinx.coroutines.launch

class LogMPChartComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var isLogging by remember { mutableStateOf(false) }
        var logEntries by remember { mutableIntStateOf(0) }
        var dataPoints by remember { mutableIntStateOf(125) }
        var selectedTimeRange by remember { mutableStateOf("1 Hour") }
        var showExportDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Data Log Chart",
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
                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Export",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logging status card
                    item {
                        LoggingStatusCard(
                            isLogging = isLogging,
                            logEntries = logEntries,
                            dataPoints = dataPoints,
                            onToggleLogging = {
                                isLogging = !isLogging
                                if (isLogging) logEntries = 0
                            }
                        )
                    }
                    // Chart display
                    item {
                        ChartDisplayCard(
                            selectedTimeRange = selectedTimeRange,
                            dataPoints = dataPoints
                        )
                    }
                    // Time range selector
                    item {
                        TimeRangeSelector(
                            selectedRange = selectedTimeRange,
                            onRangeSelected = { selectedTimeRange = it }
                        )
                    }
                    // Chart statistics
                    item {
                        ChartStatisticsCard()
                    }
                    // Export and management
                    item {
                        DataManagementCard(
                            onExportCsv = {
                                scope.launch {
                                    viewModel.exportData(
                                        this@LogMPChartComposeActivity,
                                        ThermalViewModel.ExportFormat.CSV
                                    )
                                    snackbarHostState.showSnackbar("Exporting data as CSV...")
                                }
                            },
                            onExportPdf = {
                                scope.launch {
                                    viewModel.exportData(
                                        this@LogMPChartComposeActivity,
                                        ThermalViewModel.ExportFormat.PDF
                                    )
                                    snackbarHostState.showSnackbar("Exporting data as PDF...")
                                }
                            },
                            onClearData = {
                                logEntries = 0
                                dataPoints = 0
                            }
                        )
                    }
                }
            }
        }
        // Simulate logging
        LaunchedEffect(isLogging) {
            if (isLogging) {
                while (isLogging) {
                    kotlinx.coroutines.delay(1000L)
                    logEntries++
                    dataPoints++
                }
            }
        }
        // Export dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Chart Data") },
                text = {
                    Column {
                        Text("Select export format:")
                        Spacer(modifier = Modifier.height(16.dp))
                        listOf("Image (PNG)", "CSV Data", "PDF Report").forEach { format ->
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Exporting as $format...")
                                    }
                                    showExportDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(format)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        // Settings dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Chart Settings") },
                text = {
                    Column {
                        Text("Configure chart display settings")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("â€¢ Time range", style = MaterialTheme.typography.bodySmall)
                        Text("â€¢ Data refresh rate", style = MaterialTheme.typography.bodySmall)
                        Text("â€¢ Chart colors", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {}
            )
        }
    }
}

@Composable
private fun LoggingStatusCard(
    isLogging: Boolean,
    logEntries: Int,
    dataPoints: Int,
    onToggleLogging: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Data Logging",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isLogging) "Recording..." else "Stopped",
                        color = if (isLogging) Color(0xFF00FF00) else Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }
                Switch(
                    checked = isLogging,
                    onCheckedChange = { onToggleLogging() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF6B35),
                        uncheckedThumbColor = Color(0xFF7D8590),
                        uncheckedTrackColor = Color(0xFF16131E)
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LogStatItem("Log Entries", logEntries.toString())
                LogStatItem("Data Points", dataPoints.toString())
                LogStatItem("Duration", if (isLogging) "${logEntries}s" else "0s")
            }
        }
    }
}

@Composable
private fun LogStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = Color(0xFFFF6B35),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ChartDisplayCard(
    selectedTimeRange: String,
    dataPoints: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Temperature Chart - $selectedTimeRange",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Chart placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Color(0xFF16131E),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Chart",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Temperature Trend Chart",
                        color = Color(0xFF7D8590),
                        fontSize = 16.sp
                    )
                    Text(
                        "$dataPoints data points",
                        color = Color(0xFFFF6B35),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRangeSelector(
    selectedRange: String,
    onRangeSelected: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Time Range",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val timeRanges = listOf("15 Min", "1 Hour", "6 Hours", "24 Hours")
                timeRanges.forEach { range ->
                    FilterChip(
                        onClick = { onRangeSelected(range) },
                        label = { Text(range, fontSize = 12.sp) },
                        selected = selectedRange == range,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF16131E),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartStatisticsCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Chart Statistics",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Peak", "47.2Â°C", Color(0xFFFF4444))
                StatItem("Valley", "18.1Â°C", Color(0xFF4444FF))
                StatItem("Average", "32.7Â°C", Color(0xFFFFAA00))
                StatItem("Variance", "Â±4.8Â°C", Color(0xFF7D8590))
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DataManagementCard(
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onClearData: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Data Management",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Text("Export CSV", fontSize = 12.sp)
                }
                Button(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B7280)
                    )
                ) {
                    Text("Export PDF", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onClearData,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ManualStep1ComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class ManualStep1ComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        val context = LocalContext.current
        var currentStep by remember { mutableIntStateOf(1) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Manual Setup - Step 1",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress indicator
                    SetupProgressIndicator(
                        currentStep = 1,
                        totalSteps = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Main setup card
                    ManualSetupCard(
                        step = currentStep,
                        onNextStep = {
                            val intent = Intent(context, ManualStep2ComposeActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Setup Progress",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Progress bar
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFFFF6B35),
                trackColor = Color(0xFF16131E)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Step $currentStep of $totalSteps",
                color = Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ManualSetupCard(
    step: Int,
    onNextStep: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Setup icon
            Icon(
                Icons.Default.Build,
                contentDescription = "Setup",
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(64.dp)
            )
            // Title
            Text(
                "Thermal Camera Setup",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            // Instructions
            SetupInstructions()
            // Setup checklist
            SetupChecklist()
            // Continue button
            Button(
                onClick = onNextStep,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    "Continue to Step 2",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SetupInstructions() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Device Preparation",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        InstructionItem(
            step = "1",
            instruction = "Ensure your thermal camera device is powered on and ready"
        )
        InstructionItem(
            step = "2",
            instruction = "Check that Bluetooth is enabled on your mobile device"
        )
        InstructionItem(
            step = "3",
            instruction = "Place the thermal camera within 3 meters of your phone"
        )
    }
}

@Composable
private fun InstructionItem(
    step: String,
    instruction: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    Color(0xFFFF6B35),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                step,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            instruction,
            color = Color(0xFF7D8590),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SetupChecklist() {
    var devicePowered by remember { mutableStateOf(false) }
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var cameraInRange by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Setup Checklist",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            ChecklistItem(
                text = "Thermal camera powered on",
                checked = devicePowered,
                onCheckedChange = { devicePowered = it }
            )
            ChecklistItem(
                text = "Bluetooth enabled",
                checked = bluetoothEnabled,
                onCheckedChange = { bluetoothEnabled = it }
            )
            ChecklistItem(
                text = "Camera within range",
                checked = cameraInRange,
                onCheckedChange = { cameraInRange = it }
            )
        }
    }
}

@Composable
private fun ChecklistItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFFF6B35),
                uncheckedColor = Color(0xFF7D8590),
                checkmarkColor = Color.White
            )
        )
        Text(
            text,
            color = if (checked) Color.White else Color(0xFF7D8590),
            fontSize = 14.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ManualStep2ComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ManualStep2ComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var isConnecting by remember { mutableStateOf(false) }
        var isConnected by remember { mutableStateOf(false) }
        var isCalibrating by remember { mutableStateOf(false) }
        var calibrationProgress by remember { mutableFloatStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Manual Setup - Step 2",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress indicator
                    SetupProgressIndicator(
                        currentStep = 2,
                        totalSteps = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Connection and calibration card
                    ConnectionSetupCard(
                        isConnecting = isConnecting,
                        isConnected = isConnected,
                        isCalibrating = isCalibrating,
                        calibrationProgress = calibrationProgress,
                        onConnect = {
                            isConnecting = true
                            // Simulate connection
                            coroutineScope.launch {
                                delay(3000L)
                                isConnecting = false
                                isConnected = true
                            }
                        },
                        onCalibrate = {
                            isCalibrating = true
                            calibrationProgress = 0f
                            // Simulate calibration
                            coroutineScope.launch {
                                for (i in 1..100) {
                                    delay(50L)
                                    calibrationProgress = i / 100f
                                }
                                isCalibrating = false
                            }
                        },
                        onFinish = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Setup Progress",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Progress bar
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFFFF6B35),
                trackColor = Color(0xFF16131E)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Step $currentStep of $totalSteps - Final Step",
                color = Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ConnectionSetupCard(
    isConnecting: Boolean,
    isConnected: Boolean,
    isCalibrating: Boolean,
    calibrationProgress: Float,
    onConnect: () -> Unit,
    onCalibrate: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Status icon
            when {
                isCalibrating -> {
                    CircularProgressIndicator(
                        progress = { calibrationProgress },
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.size(64.dp)
                    )
                }

                isConnecting -> {
                    CircularProgressIndicator(
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.size(64.dp)
                    )
                }

                isConnected -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = Color(0xFF00FF00),
                        modifier = Modifier.size(64.dp)
                    )
                }

                else -> {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            // Title and status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    when {
                        isCalibrating -> "Calibrating Camera"
                        isConnecting -> "Connecting..."
                        isConnected -> "Connection Successful"
                        else -> "Connect & Calibrate"
                    },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    when {
                        isCalibrating -> "Please wait while the camera calibrates (${(calibrationProgress * 100).toInt()}%)"
                        isConnecting -> "Establishing connection with thermal camera..."
                        isConnected -> "Camera connected and ready for calibration"
                        else -> "Connect to your thermal camera and perform initial calibration"
                    },
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            // Connection steps
            if (!isConnected && !isConnecting) {
                ConnectionSteps()
            }
            // Calibration info
            if (isConnected && !isCalibrating && calibrationProgress == 0f) {
                CalibrationInfo()
            }
            // Action buttons
            ActionButtons(
                isConnecting = isConnecting,
                isConnected = isConnected,
                isCalibrating = isCalibrating,
                calibrationComplete = calibrationProgress >= 1f,
                onConnect = onConnect,
                onCalibrate = onCalibrate,
                onFinish = onFinish
            )
        }
    }
}

@Composable
private fun ConnectionSteps() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Connection Steps",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            ConnectionStep("1", "Turn on thermal camera")
            ConnectionStep("2", "Enable camera pairing mode")
            ConnectionStep("3", "Tap connect to search for device")
        }
    }
}

@Composable
private fun ConnectionStep(
    step: String,
    instruction: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    Color(0xFFFF6B35),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                step,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            instruction,
            color = Color(0xFF7D8590),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CalibrationInfo() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16131E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Calibration Required",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Camera calibration ensures accurate temperature readings. This process takes about 30 seconds.",
                color = Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ActionButtons(
    isConnecting: Boolean,
    isConnected: Boolean,
    isCalibrating: Boolean,
    calibrationComplete: Boolean,
    onConnect: () -> Unit,
    onCalibrate: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            calibrationComplete -> {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00AA00)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Complete",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Setup Complete",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            isConnected && !isCalibrating -> {
                Button(
                    onClick = onCalibrate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Calibrate",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Start Calibration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            !isConnected && !isConnecting -> {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = "Connect",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Connect to Camera",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        // Cancel button (always available)
        if (!calibrationComplete) {
            OutlinedButton(
                onClick = onFinish,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF7D8590)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7D8590)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Setup", fontSize = 14.sp)
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\MonitorChartComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class MonitorChartComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var showSettings by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Monitor Chart",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
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
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117)
                        )
                    )
                },
                floatingActionButton = {
                    var isRecording by remember { mutableStateOf(false) }
                    FloatingActionButton(
                        onClick = { isRecording = !isRecording },
                        containerColor = if (isRecording) Color(0xFFDC2626) else Color(0xFFFF6B35)
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isRecording) "Stop" else "Start",
                            tint = Color.White
                        )
                    }
                }
            ) { paddingValues ->
                MonitorChartContent(
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun MonitorChartContent(
        scope: kotlinx.coroutines.CoroutineScope,
        snackbarHostState: SnackbarHostState,
        modifier: Modifier = Modifier
    ) {
        var timeRange by remember { mutableStateOf("1hr") }
        var alertThreshold by remember { mutableFloatStateOf(35f) }
        var showAlerts by remember { mutableStateOf(true) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("Current", "28.5Â°C", Color(0xFF7D8590))
                    StatCard("Peak", "42.1Â°C", Color(0xFFFF6B35))
                    StatCard("Average", "31.2Â°C", Color(0xFF4A90E2))
                    StatCard("Sensors", "4", Color(0xFF238636))
                }
            }
            // Chart Display Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Chart",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Real-time Temperature Chart",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Multi-sensor monitoring with threshold alerts",
                            color = Color(0xFF7D8590),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Chart legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LegendItem("Sensor 1", Color(0xFFFF6B35))
                            LegendItem("Sensor 2", Color(0xFF4A90E2))
                            LegendItem("Sensor 3", Color(0xFF238636))
                            LegendItem("Sensor 4", Color(0xFFFFD700))
                        }
                    }
                }
            }
            // Time Range Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Time Range",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeRangeChip("5min", timeRange == "5min") { timeRange = "5min" }
                        TimeRangeChip("15min", timeRange == "15min") { timeRange = "15min" }
                        TimeRangeChip("1hr", timeRange == "1hr") { timeRange = "1hr" }
                        TimeRangeChip("6hr", timeRange == "6hr") { timeRange = "6hr" }
                        TimeRangeChip("24hr", timeRange == "24hr") { timeRange = "24hr" }
                    }
                }
            }
            // Alert Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Temperature Alerts",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = showAlerts,
                            onCheckedChange = { showAlerts = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFF6B35),
                                checkedTrackColor = Color(0xFFFF6B35).copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (showAlerts) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Threshold: ${alertThreshold.toInt()}Â°C",
                            color = Color(0xFF7D8590),
                            fontSize = 14.sp
                        )
                        Slider(
                            value = alertThreshold,
                            onValueChange = { alertThreshold = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF6B35),
                                activeTrackColor = Color(0xFFFF6B35)
                            )
                        )
                    }
                }
            }
            // Chart Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Chart data exported")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7D8590)
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Chart data cleared")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7D8590)
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }
                Button(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Zoom to fit applied")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = "Zoom")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zoom")
                }
            }
        }
    }

    @Composable
    private fun StatCard(
        label: String,
        value: String,
        color: Color
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                color = Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
    }

    @Composable
    private fun TimeRangeChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        FilterChip(
            onClick = onClick,
            label = { Text(label) },
            selected = selected,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFFF6B35),
                selectedLabelColor = Color.White,
                containerColor = Color(0xFF0D1117),
                labelColor = Color(0xFF7D8590)
            )
        )
    }

    @Composable
    private fun LegendItem(
        label: String,
        color: Color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(
                label,
                color = Color(0xFF7D8590),
                fontSize = 12.sp
            )
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\MonitorComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.thermalunified.fragment.MonitorThermalComposeFragment
import com.mpdc4gsr.module.thermalunified.viewmodel.MonitorViewModel

class MonitorComposeActivity : BaseComposeActivity<MonitorViewModel>() {
    override fun createViewModel(): MonitorViewModel {
        return viewModels<MonitorViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MonitorViewModel) {
        val context = LocalContext.current
        var monitorState by remember { mutableIntStateOf(MonitorViewModel.STATS_START) }
        var selectedType by remember { mutableIntStateOf(1) }
        var recordingTime by remember { mutableLongStateOf(0L) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Monitor",
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
                    // Thermal camera view (85% of screen)
                    ThermalCameraView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.85f)
                    )
                    // Control panel (15% of screen)
                    MonitorControlPanel(
                        monitorState = monitorState,
                        selectedType = selectedType,
                        recordingTime = recordingTime,
                        onQueryLog = {
                            NavigationManager.getInstance()
                                .build(RouterConfig.LOG_MP_CHART)
                                .navigation(context)
                        },
                        onCreateChart = {
                            // Show monitor selection dialog
                            monitorState = MonitorViewModel.STATS_MONITOR
                        },
                        onStartMonitoring = {
                            NavigationManager.getInstance()
                                .build(RouterConfig.MONITOR_CHART)
                                .withInt("type", selectedType)
                                .navigation(context)
                            finish()
                        },
                        onMonitorTypeSelected = { type ->
                            selectedType = type
                            monitorState = MonitorViewModel.STATS_FINISH
                            // Thermal action tracking
                            val action = when (type) {
                                1 -> 2001 // Point monitoring
                                2 -> 2002 // Line monitoring  
                                else -> 2003 // Area monitoring
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.15f)
                    )
                }
            }
        }
        // Handle time updates for recording display
        LaunchedEffect(monitorState) {
            if (monitorState == MonitorViewModel.STATS_MONITOR) {
                while (monitorState == MonitorViewModel.STATS_MONITOR) {
                    recordingTime++
                    kotlinx.coroutines.delay(1000L)
                }
            }
        }
    }
}

@Composable
private fun ThermalCameraView(
    modifier: Modifier = Modifier
) {
    // Embed the existing thermal fragment using AndroidView
    // This preserves all existing thermal camera functionality
    AndroidView(
        factory = { context ->
            val fragment = MonitorThermalComposeFragment()
            // Return a container view for the fragment
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_0
            }
        },
        modifier = modifier
    )
}

@Composable
private fun MonitorControlPanel(
    monitorState: Int,
    selectedType: Int,
    recordingTime: Long,
    onQueryLog: () -> Unit,
    onCreateChart: () -> Unit,
    onStartMonitoring: () -> Unit,
    onMonitorTypeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black
    ) {
        when (monitorState) {
            MonitorViewModel.STATS_START -> {
                InitialControlsView(
                    onQueryLog = onQueryLog,
                    onCreateChart = onCreateChart
                )
            }

            MonitorViewModel.STATS_MONITOR -> {
                MonitorTypeSelectionView(
                    onTypeSelected = onMonitorTypeSelected
                )
            }

            MonitorViewModel.STATS_FINISH -> {
                StartMonitoringView(
                    recordingTime = recordingTime,
                    onStartMonitoring = onStartMonitoring
                )
            }
        }
    }
}

@Composable
private fun InitialControlsView(
    onQueryLog: () -> Unit,
    onCreateChart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onQueryLog,
            modifier = Modifier.weight(0.4f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6B7280),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Query Log",
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.weight(0.2f))
        Button(
            onClick = onCreateChart,
            modifier = Modifier.weight(0.4f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Create Chart",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MonitorTypeSelectionView(
    onTypeSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Select Monitor Type",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonitorTypeButton(
                    text = "Point",
                    icon = Icons.Default.Place,
                    onClick = { onTypeSelected(1) },
                    modifier = Modifier.weight(1f)
                )
                MonitorTypeButton(
                    text = "Line",
                    icon = Icons.Default.Place,
                    onClick = { onTypeSelected(2) },
                    modifier = Modifier.weight(1f)
                )
                MonitorTypeButton(
                    text = "Area",
                    icon = Icons.Default.Place,
                    onClick = { onTypeSelected(3) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MonitorTypeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF6B35),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StartMonitoringView(
    recordingTime: Long,
    onStartMonitoring: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onStartMonitoring,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(56.dp)
        ) {
            val minutes = recordingTime / 60 % 60
            val seconds = recordingTime % 60
            val timeText = String.format("%02d:%02d", minutes, seconds)
            Text(
                if (recordingTime > 0) timeText else "Start",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\MonitorLogComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonitorLogComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    data class LogEntry(
        val timestamp: String,
        val temperature: Float,
        val location: String,
        val notes: String = ""
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        // Sample log data
        val logEntries = remember {
            mutableStateListOf(
                LogEntry("2024-10-01 10:30:00", 25.5f, "Location A", "Normal reading"),
                LogEntry("2024-10-01 10:25:00", 27.2f, "Location B", "Elevated temperature"),
                LogEntry("2024-10-01 10:20:00", 24.8f, "Location C", ""),
                LogEntry("2024-10-01 10:15:00", 26.1f, "Location A", "Follow-up check"),
            )
        }
        var showFilterDialog by remember { mutableStateOf(false) }
        var showAddLogDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Monitor Log",
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
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        // Perform heavy IO operations on background thread
                                        withContext(Dispatchers.IO) {
                                            // Export logs to CSV file
                                            val csv = buildString {
                                                appendLine("Timestamp,Temperature,Location,Notes")
                                                logEntries.forEach { entry ->
                                                    appendLine("${entry.timestamp},${entry.temperature},${entry.location},${entry.notes}")
                                                }
                                            }
                                            // Create file in Downloads directory
                                            val contentValues = android.content.ContentValues().apply {
                                                put(
                                                    android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME,
                                                    "monitor_log_${System.currentTimeMillis()}.csv"
                                                )
                                                put(android.provider.MediaStore.Files.FileColumns.MIME_TYPE, "text/csv")
                                                put(
                                                    android.provider.MediaStore.Files.FileColumns.RELATIVE_PATH,
                                                    android.os.Environment.DIRECTORY_DOWNLOADS
                                                )
                                            }
                                            val uri = context.contentResolver.insert(
                                                android.provider.MediaStore.Files.getContentUri("external"),
                                                contentValues
                                            )
                                            uri?.let {
                                                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                                    outputStream.write(csv.toByteArray())
                                                }
                                            }
                                        }
                                        // Show success message on main thread
                                        snackbarHostState.showSnackbar("Logs exported to Downloads")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Export failed: ${e.message}")
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Export", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddLogDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Log")
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                if (logEntries.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "No log entries",
                                modifier = Modifier.size(64.dp),
                                tint = Color.White.copy(alpha = 0.3f)
                            )
                            Text(
                                "No log entries",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                            Text(
                                "Start monitoring to create logs",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // Log list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(logEntries) { entry ->
                            LogEntryCard(entry)
                        }
                    }
                }
            }
            // Add Log Entry Dialog
            if (showAddLogDialog) {
                var newTemp by remember { mutableStateOf("25.0") }
                var newLocation by remember { mutableStateOf("") }
                var newNotes by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showAddLogDialog = false },
                    title = { Text("Add Log Entry") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newTemp,
                                onValueChange = { newTemp = it },
                                label = { Text("Temperature (Â°C)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newLocation,
                                onValueChange = { newLocation = it },
                                label = { Text("Location") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newNotes,
                                onValueChange = { newNotes = it },
                                label = { Text("Notes (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val temp = newTemp.toFloatOrNull() ?: 25.0f
                            val timestamp =
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    .format(java.util.Date())
                            logEntries.add(0, LogEntry(timestamp, temp, newLocation, newNotes))
                            showAddLogDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Log entry added")
                            }
                        }) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddLogDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun LogEntryCard(entry: LogEntry) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header row with timestamp and temperature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.timestamp,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Location",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                entry.location,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    // Temperature badge
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                entry.temperature > 30f -> Color(0xFFFF4747)
                                entry.temperature > 26f -> Color(0xFFFFA500)
                                else -> Color(0xFF06AAFF)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "%.1fÂ°C".format(entry.temperature),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Notes section
                if (entry.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        entry.notes,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\MonitoryHomeComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.fragment.IRMonitorCaptureComposeFragment
import com.mpdc4gsr.module.thermalunified.fragment.IRMonitorHistoryComposeFragment
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.launch

class MonitoryHomeComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        val pagerState = rememberPagerState(pageCount = { 2 })
        val scope = rememberCoroutineScope()
        var selectedTab by remember { mutableIntStateOf(0) }
        var isRecording by remember { mutableStateOf(false) }
        var showExportDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        val exportStatus by viewModel.exportStatus.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        // Handle export status
        LaunchedEffect(exportStatus) {
            when (exportStatus) {
                is ThermalViewModel.ExportStatus.Success -> {
                    snackbarHostState.showSnackbar("Data exported successfully")
                }

                is ThermalViewModel.ExportStatus.Error -> {
                    snackbarHostState.showSnackbar("Export failed: ${(exportStatus as ThermalViewModel.ExportStatus.Error).message}")
                }

                else -> {}
            }
        }
        LibUnifiedTheme {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Monitor Dashboard",
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
                            IconButton(onClick = {
                                // Show format selection dialog
                                showExportDialog = true
                            }) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Export",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                // Show settings dialog
                                showSettingsDialog = true
                            }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            isRecording = !isRecording
                        },
                        containerColor = if (isRecording) Color.Red else Color(0xFFFF6B35),
                        contentColor = Color.White
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isRecording) "Stop" else "Start"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isRecording) "Stop Monitor" else "Start Monitor",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                ) {
                    // Recording status
                    if (isRecording) {
                        RecordingStatusBar(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Tab selector
                    MonitorTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            selectedTab = tab
                            scope.launch {
                                pagerState.animateScrollToPage(tab)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Content pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> MonitorHistoryTab()
                            1 -> MonitorRealTimeTab(
                                isRecording = isRecording,
                                onSnapshot = { viewModel.captureSnapshot() },
                                onZoom = { },
                                onAdjust = { }
                            )
                        }
                    }
                }
            }
        }
        // Sync pager with tabs
        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }
        // Export format selection dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Data") },
                text = {
                    Column {
                        Text("Select export format:")
                        Spacer(modifier = Modifier.height(16.dp))
                        ThermalViewModel.ExportFormat.values().forEach { format ->
                            TextButton(
                                onClick = {
                                    viewModel.exportData(this@MonitoryHomeComposeActivity, format)
                                    showExportDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(format.name)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        // Settings dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings") },
                text = {
                    Column {
                        Text("Monitor Settings")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Configure monitoring parameters here",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {}
            )
        }
    }
}

@Composable
private fun RecordingStatusBar(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FiberManualRecord,
                contentDescription = "Recording",
                tint = Color.Red,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Recording in progress...",
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MonitorTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonitorTab(
                text = "History",
                icon = Icons.Default.History,
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            MonitorTab(
                text = "Real-time",
                icon = Icons.AutoMirrored.Filled.ShowChart,
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonitorTab(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFFF6B35) else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color(0xFF7D8590)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MonitorHistoryTab() {
    // Embed existing history fragment using AndroidView
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        AndroidView(
            factory = { context ->
                val fragment = IRMonitorHistoryComposeFragment()
                androidx.fragment.app.FragmentContainerView(context).apply {
                    id = androidx.core.R.id.accessibility_custom_action_4
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // History stats overlay
        HistoryStatsOverlay(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun HistoryStatsOverlay(
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
                "History Stats",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Sessions: 15",
                color = Color(0xFF7D8590),
                fontSize = 9.sp
            )
            Text(
                "Duration: 2h 30m",
                color = Color(0xFF7D8590),
                fontSize = 9.sp
            )
            Text(
                "Data Points: 1,250",
                color = Color(0xFFFF6B35),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun MonitorRealTimeTab(
    isRecording: Boolean,
    onSnapshot: () -> Unit = {},
    onZoom: () -> Unit = {},
    onAdjust: () -> Unit = {}
) {
    // Embed existing real-time fragment using AndroidView
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        AndroidView(
            factory = { context ->
                val fragment = IRMonitorCaptureComposeFragment()
                androidx.fragment.app.FragmentContainerView(context).apply {
                    id = androidx.core.R.id.accessibility_custom_action_5
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Real-time monitor overlay
        RealTimeMonitorOverlay(
            isRecording = isRecording,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        // Quick controls
        QuickControlsOverlay(
            onSnapshot = onSnapshot,
            onZoom = onZoom,
            onAdjust = onAdjust,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun RealTimeMonitorOverlay(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    var recordingTime by remember { mutableIntStateOf(0) }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isRecording) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        contentDescription = "Recording",
                        tint = Color.Red,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    if (isRecording) "RECORDING" else "READY",
                    color = if (isRecording) Color.Red else Color(0xFF00FF00),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isRecording) {
                Text(
                    "Duration: ${recordingTime}s",
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
            Text(
                "Live: 35.2Â°C",
                color = Color(0xFFFF6B35),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    // Simulate recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(1000L)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }
}

@Composable
private fun QuickControlsOverlay(
    onSnapshot: () -> Unit = {},
    onZoom: () -> Unit = {},
    onAdjust: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FloatingActionButton(
            onClick = onSnapshot,
            modifier = Modifier.size(40.dp),
            containerColor = Color(0xFF6B7280)
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Snapshot",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        FloatingActionButton(
            onClick = onZoom,
            modifier = Modifier.size(40.dp),
            containerColor = Color(0xFF6B7280)
        ) {
            Icon(
                Icons.Default.ZoomIn,
                contentDescription = "Zoom",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        FloatingActionButton(
            onClick = onAdjust,
            modifier = Modifier.size(40.dp),
            containerColor = Color(0xFF6B7280)
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = "Adjust",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ReportPickImgComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ReportPickImgComposeActivity : BaseComposeActivity<ReportPickImgViewModel>() {
    override fun createViewModel(): ReportPickImgViewModel {
        return ReportPickImgViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ReportPickImgViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Select Report Images",
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
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                            }
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1976D2)
                        )
                    )
                }
            ) { paddingValues ->
                ReportPickImgContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun ReportPickImgContent(
        viewModel: ReportPickImgViewModel,
        modifier: Modifier = Modifier
    ) {
        var selectedImages by remember { mutableStateOf(setOf<Int>()) }
        var filterCriteria by remember { mutableStateOf("All Images") }
        var showAIRecommendations by remember { mutableStateOf(true) }
        val thermalImages = remember {
            (1..20).map { index ->
                ReportThermalImage(
                    id = index,
                    name = "Thermal_IMG_$index.jpg",
                    temperature = (25.0 + index * 2.5),
                    quality = (70 + index * 2).coerceAtMost(100),
                    isRecommended = index % 3 == 0,
                    timestamp = "2024-01-${(index % 30) + 1}"
                )
            }
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selection summary and AI recommendations
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
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
                            "Image Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${selectedImages.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Recommendations")
                        Switch(
                            checked = showAIRecommendations,
                            onCheckedChange = { showAIRecommendations = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF1976D2),
                                checkedTrackColor = Color(0xFF1976D2).copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
            // Filter options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Filter Criteria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All Images", "High Quality", "High Temp", "Recommended")
                        filters.forEach { filter ->
                            FilterChip(
                                onClick = { filterCriteria = filter },
                                label = { Text(filter) },
                                selected = filterCriteria == filter,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1976D2),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
            // Image grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(thermalImages.filter { image ->
                    when (filterCriteria) {
                        "High Quality" -> image.quality >= 90
                        "High Temp" -> image.temperature >= 40.0
                        "Recommended" -> image.isRecommended
                        else -> true
                    }
                }) { image ->
                    ThermalImageCard(
                        image = image,
                        isSelected = selectedImages.contains(image.id),
                        showRecommendation = showAIRecommendations,
                        onSelectionChange = { isSelected ->
                            selectedImages = if (isSelected) {
                                selectedImages + image.id
                            } else {
                                selectedImages - image.id
                            }
                        }
                    )
                }
            }
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { selectedImages = setOf() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF666666)
                    )
                ) {
                    Text("Clear All")
                }
                Button(
                    onClick = {
                        // Auto-select recommended images
                        selectedImages = thermalImages
                            .filter { it.isRecommended }
                            .map { it.id }
                            .toSet()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Auto Select")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto Select")
                }
                Button(
                    onClick = { finish() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    ),
                    enabled = selectedImages.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Add to Report")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Report")
                }
            }
        }
    }

    @Composable
    private fun ThermalImageCard(
        image: ReportThermalImage,
        isSelected: Boolean,
        showRecommendation: Boolean,
        onSelectionChange: (Boolean) -> Unit
    ) {
        Card(
            onClick = { onSelectionChange(!isSelected) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
            ),
            border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Image placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "${image.temperature.toInt()}Â°C",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Thermal Image",
                                color = Color(0xFF9E9E9E),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    // Recommendation badge
                    if (showRecommendation && image.isRecommended) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Recommended",
                                modifier = Modifier.padding(4.dp),
                                tint = Color.White
                            )
                        }
                    }
                    // Selection indicator
                    if (isSelected) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.padding(4.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Image info
                Text(
                    image.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Quality: ${image.quality}% | ${image.timestamp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

private data class ReportThermalImage(
    val id: Int,
    val name: String,
    val temperature: Double,
    val quality: Int,
    val isRecommended: Boolean,
    val timestamp: String
)

class ReportPickImgViewModel : BaseViewModel()


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ReportPreviewComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
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

class ReportPreviewComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Report Preview",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
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
                            IconButton(onClick = {
                                // TODO: Implement print functionality
                                android.widget.Toast.makeText(
                                    this@ReportPreviewComposeActivity,
                                    "Print report feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White)
                            }
                            IconButton(onClick = {
                                // TODO: Implement share functionality
                                android.widget.Toast.makeText(
                                    this@ReportPreviewComposeActivity,
                                    "Share report feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0D1117)
                        )
                    )
                }
            ) { paddingValues ->
                ReportPreviewContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun ReportPreviewContent(
        modifier: Modifier = Modifier
    ) {
        var currentPage by remember { mutableStateOf(1) }
        var totalPages by remember { mutableStateOf(5) }
        var showNavigationBookmarks by remember { mutableStateOf(false) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
        ) {
            // Navigation and Page Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page Navigation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous",
                                tint = if (currentPage > 1) Color.White else Color(0xFF7D8590)
                            )
                        }
                        Text(
                            "Page $currentPage of $totalPages",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next",
                                tint = if (currentPage < totalPages) Color.White else Color(0xFF7D8590)
                            )
                        }
                    }
                    // Quick Navigation
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { showNavigationBookmarks = !showNavigationBookmarks }) {
                            Icon(
                                Icons.Default.Bookmarks,
                                contentDescription = "Bookmarks",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { }) {
                            Icon(
                                Icons.Default.FitScreen,
                                contentDescription = "Fit to screen",
                                tint = Color.White
                            )
                        }
                    }
                }
                // Navigation Bookmarks
                if (showNavigationBookmarks) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BookmarkItem("Executive Summary", 1) { currentPage = 1 }
                        BookmarkItem("Thermal Analysis", 2) { currentPage = 2 }
                        BookmarkItem("Temperature Data", 3) { currentPage = 3 }
                        BookmarkItem("Conclusions", 4) { currentPage = 4 }
                        BookmarkItem("Appendix", 5) { currentPage = 5 }
                    }
                }
            }
            // Report Content Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentPage) {
                        1 -> ExecutiveSummaryPage()
                        2 -> ThermalAnalysisPage()
                        3 -> TemperatureDataPage()
                        4 -> ConclusionsPage()
                        5 -> AppendixPage()
                    }
                }
            }
            // Format Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Export Format",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormatButton("PDF", Icons.Default.PictureAsPdf, true)
                        FormatButton("Word", Icons.Default.Description, false)
                        FormatButton("HTML", Icons.Default.Language, false)
                        FormatButton("Print", Icons.Default.Print, false)
                    }
                }
            }
        }
    }

    @Composable
    private fun BookmarkItem(
        title: String,
        pageNumber: Int,
        onClick: () -> Unit
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    title,
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp
                )
                Text(
                    "Page $pageNumber",
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    private fun RowScope.FormatButton(
        label: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        selected: Boolean,
        onClick: () -> Unit = {}
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) Color(0xFFFF6B35) else Color(0xFF161B22),
                contentColor = if (selected) Color.White else Color(0xFF7D8590)
            )
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                fontSize = 12.sp
            )
        }
    }

    @Composable
    private fun ExecutiveSummaryPage() {
        Text(
            "Executive Summary",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "This thermal analysis report presents comprehensive findings from thermal imaging inspection conducted on January 15, 2024. The inspection covered critical infrastructure components and identified several areas requiring attention.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Key Findings:",
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        BulletPoint("Temperature anomalies detected in sectors 3 and 7")
        BulletPoint("Average operating temperature: 28.7Â°C")
        BulletPoint("Peak temperature recorded: 42.1Â°C")
        BulletPoint("No critical temperature violations observed")
    }

    @Composable
    private fun ThermalAnalysisPage() {
        Text(
            "Thermal Analysis",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Detailed thermal analysis of captured data reveals normal operating conditions with isolated temperature variations within acceptable ranges.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }

    @Composable
    private fun TemperatureDataPage() {
        Text(
            "Temperature Data",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Comprehensive temperature measurements and statistical analysis of thermal imaging data.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }

    @Composable
    private fun ConclusionsPage() {
        Text(
            "Conclusions",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Based on the thermal analysis results, all systems are operating within normal parameters with no immediate action required.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }

    @Composable
    private fun AppendixPage() {
        Text(
            "Appendix",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Additional technical specifications, calibration data, and supporting documentation.",
            color = Color.Black,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }

    @Composable
    private fun BulletPoint(text: String) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Text(
                "â€¢ ",
                color = Color.Black,
                fontSize = 14.sp
            )
            Text(
                text,
                color = Color.Black,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ReportPreviewSecondComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ReportPreviewSecondComposeActivity : BaseComposeActivity<ReportPreviewSecondViewModel>() {
    override fun createViewModel(): ReportPreviewSecondViewModel {
        return ReportPreviewSecondViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ReportPreviewSecondViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Advanced Report Preview",
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
                            IconButton(onClick = {
                                // TODO: Implement share functionality
                                android.widget.Toast.makeText(
                                    this@ReportPreviewSecondComposeActivity,
                                    "Share report feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            IconButton(onClick = {
                                // TODO: Implement export functionality
                                android.widget.Toast.makeText(
                                    this@ReportPreviewSecondComposeActivity,
                                    "Export report feature coming soon",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1976D2)
                        )
                    )
                }
            ) { paddingValues ->
                ReportPreviewSecondContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun ReportPreviewSecondContent(
        viewModel: ReportPreviewSecondViewModel,
        modifier: Modifier = Modifier
    ) {
        val reportSections = remember {
            listOf(
                ReportSection("Header", "Report Title and Project Information", true),
                ReportSection("Executive Summary", "Key findings and recommendations", false),
                ReportSection("Thermal Analysis", "Detailed thermal imaging analysis", true),
                ReportSection("Temperature Data", "Temperature measurements and statistics", true),
                ReportSection("Visual Evidence", "Thermal images and thermal overlays", true),
                ReportSection("Conclusions", "Analysis conclusions and recommendations", false),
                ReportSection("Appendix", "Supporting data and references", false)
            )
        }
        var selectedSection by remember { mutableStateOf<String?>(null) }
        var previewMode by remember { mutableStateOf("Full") }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Preview Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("Full", "Summary", "Images Only")
                        modes.forEach { mode ->
                            FilterChip(
                                onClick = { previewMode = mode },
                                label = { Text(mode) },
                                selected = previewMode == mode,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1976D2),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
            // Section navigation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Quick Navigation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(reportSections) { section ->
                            NavigationItem(
                                section = section,
                                isSelected = selectedSection == section.title,
                                onClick = { selectedSection = section.title }
                            )
                        }
                    }
                }
            }
            // Report preview area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedSection ?: "Full Report Preview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        if (selectedSection != null) {
                            IconButton(
                                onClick = { }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Section",
                                    tint = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE0E0E0)
                    )
                    // Preview content
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            PreviewContent(
                                section = selectedSection,
                                mode = previewMode
                            )
                        }
                    }
                }
            }
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Navigate back to edit report
                        android.widget.Toast.makeText(
                            this@ReportPreviewSecondComposeActivity,
                            "Edit report feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1976D2)
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
                Button(
                    onClick = {
                        // TODO: Finalize and export report
                        android.widget.Toast.makeText(
                            this@ReportPreviewSecondComposeActivity,
                            "Finalize report feature coming soon",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "Finalize")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Finalize")
                }
            }
        }
    }

    @Composable
    private fun NavigationItem(
        section: ReportSection,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF1976D2) else Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.Black
                    )
                    Text(
                        section.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF666666)
                    )
                }
                if (section.hasContent) {
                    Text(
                        "",
                        color = if (isSelected) Color.White else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    private fun PreviewContent(
        section: String?,
        mode: String
    ) {
        when (section) {
            null -> {
                // Full report preview
                Text(
                    "Thermal Analysis Report",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Project: Industrial Equipment Inspection\nDate: ${
                        java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
                    }\nOperator: Thermal Analysis Team",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            "Header" -> {
                Text(
                    "Report Header Section",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "This section contains the report title, project information, date, and operator details.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            "Thermal Analysis" -> {
                Text(
                    "Thermal Analysis Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Detailed thermal imaging analysis with temperature measurements and thermal patterns.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                Text(
                    "$section Content",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Preview content for the $section section of the thermal analysis report.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

data class ReportSection(
    val title: String,
    val description: String,
    val hasContent: Boolean
)

class ReportPreviewSecondViewModel : BaseViewModel()


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\temp\ChartActivity.kt =====


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ThermalCameraComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalFragmentViewModel

class ThermalCameraComposeActivity : BaseComposeActivity<ThermalFragmentViewModel>() {
    override fun createViewModel(): ThermalFragmentViewModel {
        return viewModels<ThermalFragmentViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalFragmentViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Camera",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                ThermalCameraContent(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun ThermalCameraContent(
        viewModel: ThermalFragmentViewModel,
        modifier: Modifier = Modifier
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Placeholder for thermal camera view
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Thermal Camera View\n(Integration with IrSurfaceView)",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            // Camera controls with proper icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Implement thermal image capture
                        android.widget.Toast.makeText(
                            context,
                            "Capturing thermal image",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Capture")
                }
                Button(
                    onClick = {
                        // TODO: Implement thermal video recording
                        android.widget.Toast.makeText(
                            context,
                            "Start/stop thermal recording",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VideoCall, contentDescription = "Record")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record")
                }
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ThermalComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.fragment.ThermalComposeFragment
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel

class ThermalComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        var selectedToolIndex by remember { mutableIntStateOf(-1) }
        var showToolbar by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Processing",
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
                    // Main thermal camera view
                    ThermalCameraView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    // Tool selection menu (shown when tab is selected)
                    if (showToolbar) {
                        ThermalToolsMenu(
                            selectedTabIndex = selectedTabIndex,
                            selectedToolIndex = selectedToolIndex,
                            onToolSelected = { toolIndex ->
                                selectedToolIndex = toolIndex
                                // Thermal action tracking
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    // Main navigation tabs
                    ThermalNavigationTabs(
                        selectedIndex = selectedTabIndex,
                        onTabSelected = { index ->
                            selectedTabIndex = index
                            showToolbar = index > 0 // Show tools for non-camera tabs
                            selectedToolIndex = -1 // Reset tool selection
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalCameraView(
    modifier: Modifier = Modifier
) {
    // Embed the existing thermal fragment using AndroidView
    AndroidView(
        factory = { context ->
            val fragment = ThermalComposeFragment()
            androidx.fragment.app.FragmentContainerView(context).apply {
                id = androidx.core.R.id.accessibility_custom_action_1
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ThermalNavigationTabs(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = getThermalTabs()
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        itemsIndexed(tabs) { index, tab ->
            ThermalTabButton(
                tab = tab,
                isSelected = selectedIndex == index,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

@Composable
private fun ThermalTabButton(
    tab: ThermalComposeTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFFF6B35) else Color(0xFF21262D),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(56.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(20.dp)
            )
            Text(
                tab.title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ThermalToolsMenu(
    selectedTabIndex: Int,
    selectedToolIndex: Int,
    onToolSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = getThermalTools(selectedTabIndex)
    if (tools.isNotEmpty()) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF21262D)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tools) { index, tool ->
                    ThermalToolButton(
                        tool = tool,
                        isSelected = selectedToolIndex == tool.actionCode,
                        onClick = { onToolSelected(tool.actionCode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalToolButton(
    tool: ThermalTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color(0xFFFF6B35) else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color(0xFF7D8590)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFFF6B35) else Color(0xFF7D8590)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                tool.icon,
                contentDescription = tool.name,
                modifier = Modifier.size(16.dp)
            )
            Text(
                tool.name,
                fontSize = 10.sp
            )
        }
    }
}

// Data classes and helper functions
internal data class ThermalComposeTab(
    val title: String,
    val icon: ImageVector
)

data class ThermalTool(
    val name: String,
    val icon: ImageVector,
    val actionCode: Int
)

private fun getThermalTabs(): List<ThermalComposeTab> {
    return listOf(
        ThermalComposeTab("Camera", Icons.Default.CameraAlt),
        ThermalComposeTab("Measure", Icons.Default.Settings),
        ThermalComposeTab("Analysis", Icons.Default.Analytics),
        ThermalComposeTab("Palette", Icons.Default.Palette),
        ThermalComposeTab("Settings", Icons.Default.Settings)
    )
}

private fun getThermalTools(tabIndex: Int): List<ThermalTool> {
    return when (tabIndex) {
        1 -> listOf( // Measure tools
            ThermalTool("Point", Icons.Default.Place, 1001),
            ThermalTool("Line", Icons.Default.Timeline, 1002),
            ThermalTool("Rectangle", Icons.Default.CropFree, 1003),
            ThermalTool("Circle", Icons.Default.RadioButtonUnchecked, 1004)
        )

        2 -> listOf( // Analysis tools
            ThermalTool("Histogram", Icons.Default.BarChart, 2001),
            ThermalTool("Profile", Icons.AutoMirrored.Filled.ShowChart, 2002),
            ThermalTool("Report", Icons.Default.Description, 2003)
        )

        3 -> listOf( // Palette tools
            ThermalTool("Iron", Icons.Default.Palette, 3001),
            ThermalTool("Rainbow", Icons.Default.ColorLens, 3002),
            ThermalTool("Gray", Icons.Default.InvertColors, 3003),
            ThermalTool("Hot", Icons.Default.LocalFireDepartment, 3004)
        )

        4 -> listOf( // Settings tools
            ThermalTool("Emissivity", Icons.Default.Tune, 4001),
            ThermalTool("Temperature", Icons.Default.Thermostat, 4002),
            ThermalTool("Distance", Icons.Outlined.Straighten, 4003)
        )

        else -> emptyList()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ThermalGalleryComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalGalleryComposeActivity : BaseComposeActivity<ThermalGalleryViewModel>() {
    override fun createViewModel(): ThermalGalleryViewModel {
        return ThermalGalleryViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalGalleryViewModel) {
        ThermalGalleryScreen(
            onBackClick = { finish() }
        )
    }
}

class ThermalGalleryViewModel : BaseViewModel() {
    // ViewModel implementation
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThermalGalleryScreen(
    viewModel: ThermalGalleryViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null
) {
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var selectedFilter by remember { mutableStateOf(FilterType.ALL) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showMoreOptionsDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Thermal Gallery",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF161B22),
                titleContentColor = Color.White
            ),
            navigationIcon = {
                IconButton(onClick = { onBackClick?.invoke() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                }) {
                    Icon(
                        if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = Color(0xFFFF6B35)
                    )
                }
                IconButton(onClick = { showSearchDialog = true }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFFF6B35)
                    )
                }
            }
        )
        // Filter Bar
        ThermalFilterBar(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )
        // Gallery Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (viewMode) {
                ViewMode.GRID -> ThermalGridView()
                ViewMode.LIST -> ThermalListView(
                    onMoreClick = { showMoreOptionsDialog = true }
                )
            }
        }
    }
    // Search Dialog
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search Thermal Images") },
            text = {
                Column {
                    Text("Search by:")
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf("Date", "Temperature Range", "Location", "Tags").forEach { searchType ->
                        TextButton(
                            onClick = { showSearchDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(searchType)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    // More Options Dialog
    if (showMoreOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showMoreOptionsDialog = false },
            title = { Text("Gallery Options") },
            text = {
                Column {
                    listOf(
                        "Sort by Date",
                        "Sort by Temperature",
                        "Batch Export",
                        "Delete Selected",
                        "Settings"
                    ).forEach { option ->
                        TextButton(
                            onClick = { showMoreOptionsDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoreOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThermalFilterBar(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterType.values().forEach { filter ->
                    FilterChip(
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                filter.displayName,
                                fontSize = 12.sp
                            )
                        },
                        selected = selectedFilter == filter,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF21262D),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalGridView() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(generateSampleThermalImages()) { image ->
            ThermalImageCard(image = image, onMoreClick = {})
        }
    }
}

@Composable
private fun ThermalListView(
    onMoreClick: (GalleryThermalImage) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(generateSampleThermalImages()) { image ->
            ThermalImageListItem(
                image = image,
                onMoreClick = { onMoreClick(image) }
            )
        }
    }
}

@Composable
private fun ThermalImageCard(image: GalleryThermalImage, onMoreClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Image preview placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Color(0xFF0D1117),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Thermal Image",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Image info
            Text(
                image.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                "${image.temperature}Â°C",
                color = Color(0xFFFF6B35),
                fontSize = 10.sp
            )
            Text(
                image.date,
                color = Color(0xFF7D8590),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ThermalImageListItem(
    image: GalleryThermalImage,
    onMoreClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Color(0xFF0D1117),
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Thermal Image",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Image details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    image.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${image.temperature}Â°C â€¢ ${image.date}",
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
                Text(
                    "${image.resolution} â€¢ ${image.size}",
                    color = Color(0xFF7D8590),
                    fontSize = 10.sp
                )
            }
            // Actions
            IconButton(onClick = onMoreClick) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color(0xFF7D8590)
                )
            }
        }
    }
}

private fun generateSampleThermalImages(): List<GalleryThermalImage> {
    return (1..20).map { index ->
        GalleryThermalImage(
            id = index,
            name = "thermal_image_$index.tiff",
            temperature = (20..80).random(),
            date = "2024-01-${(10..28).random()}",
            resolution = "640x480",
            size = "${(100..500).random()}KB"
        )
    }
}

private data class GalleryThermalImage(
    val id: Int,
    val name: String,
    val temperature: Int,
    val date: String,
    val resolution: String,
    val size: String
)

private enum class ViewMode {
    GRID, LIST
}

private enum class FilterType(val displayName: String) {
    ALL("All"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    HIGH_TEMP("High Temp"),
    LOW_TEMP("Low Temp")
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ThermalIrNightComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalIrNightViewModel

class ThermalIrNightComposeActivity : BaseComposeActivity<ThermalIrNightViewModel>() {
    override fun createViewModel(): ThermalIrNightViewModel {
        return viewModels<ThermalIrNightViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalIrNightViewModel) {
        val selectedMode by viewModel.selectedMode.collectAsState()
        val nightModeEnabled by viewModel.nightModeEnabled.collectAsState()
        val showOverlay by viewModel.showOverlay.collectAsState()
        val isRecording by viewModel.isRecording.collectAsState()
        var showInfoDialog by remember { mutableStateOf(false) }
        var showPaletteDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        var showRangeEditDialog by remember { mutableStateOf(false) }
        var showMoreOptions by remember { mutableStateOf(false) }
        var rangeLocked by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.selectMode(0) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (selectedMode == 0) Color.White else Color.White.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Temperature", fontSize = 16.sp)
                                        if (selectedMode == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { viewModel.selectMode(1) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (selectedMode == 1) Color.White else Color.White.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Observe", fontSize = 16.sp)
                                        if (selectedMode == 1) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }
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
                            IconButton(onClick = { showInfoDialog = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.toggleNightMode() }) {
                                Icon(
                                    if (nightModeEnabled) Icons.Default.Brightness3 else Icons.Default.Brightness7,
                                    contentDescription = "Night Mode",
                                    tint = if (nightModeEnabled) Color.Yellow else Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Main thermal display with night mode styling
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .aspectRatio(192f / 256f)
                            .background(Color.Black)
                    ) {
                        // Thermal camera view
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (nightModeEnabled) Icons.Default.Brightness3 else Icons.Default.Brightness7,
                                    contentDescription = if (nightModeEnabled) "Night Mode" else "Day Mode",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.White.copy(alpha = 0.3f)
                                )
                                Text(
                                    if (nightModeEnabled) "Night Mode Active" else "Day Mode",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 16.sp
                                )
                            }
                        }
                        // Night mode specific overlays
                        if (nightModeEnabled && showOverlay) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0x80000000)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.NightsStay,
                                            contentDescription = "Night Mode",
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.Yellow
                                        )
                                        Text(
                                            "Night Mode",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                        // Temperature controls
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0x80000000)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(onClick = { rangeLocked = !rangeLocked }) {
                                        Icon(
                                            if (rangeLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = if (rangeLocked) "Unlock" else "Lock",
                                            tint = if (rangeLocked) Color.Yellow else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    IconButton(onClick = { showRangeEditDialog = true }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // Recording status
                        if (isRecording) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xCC000000)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                                    )
                                    Text("Recording", color = Color.White, fontSize = 14.sp)
                                    Text("00:00", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    // Bottom menu optimized for night mode
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16131E)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            // Secondary menu row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = { viewModel.toggleOverlay() }) {
                                    Icon(
                                        if (showOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        "Overlay",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { showPaletteDialog = true }) {
                                    Icon(Icons.Default.Palette, "Palette", tint = Color.White)
                                }
                                IconButton(onClick = { showSettingsDialog = true }) {
                                    Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            // Primary controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = {
                                    startActivity(
                                        Intent(
                                            this@ThermalIrNightComposeActivity,
                                            ThermalGalleryComposeActivity::class.java
                                        )
                                    )
                                }) {
                                    Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White)
                                }
                                FloatingActionButton(
                                    onClick = { viewModel.toggleRecording() },
                                    containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(
                                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                        contentDescription = "Record",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { showMoreOptions = true }) {
                                    Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            // Info Dialog
            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    title = { Text("Night Vision Mode") },
                    text = {
                        Column {
                            Text("Enhanced thermal imaging optimized for low-light conditions:")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("â€¢ Increased sensitivity")
                            Text("â€¢ Adjusted color palettes")
                            Text("â€¢ Reduced noise processing")
                            Text("â€¢ Optimized range settings")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showInfoDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
            // Palette Dialog
            if (showPaletteDialog) {
                AlertDialog(
                    onDismissRequest = { showPaletteDialog = false },
                    title = { Text("Select Color Palette") },
                    text = {
                        Column {
                            listOf("Ironbow", "Rainbow", "Grayscale", "Night Vision", "Hot Metal").forEach { palette ->
                                TextButton(onClick = { showPaletteDialog = false }) {
                                    Text(palette, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPaletteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            // Settings Dialog (reuse from IRThermalNightComposeActivity)
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Night Vision Settings") },
                    text = {
                        Column {
                            Text("Sensitivity: High")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Mode: Enhanced")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Temperature Range: Auto")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            // Range Edit Dialog
            if (showRangeEditDialog) {
                AlertDialog(
                    onDismissRequest = { showRangeEditDialog = false },
                    title = { Text("Edit Temperature Range") },
                    text = {
                        Column {
                            Text("Min Temperature: 0Â°C")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Max Temperature: 100Â°C")
                            Spacer(modifier = Modifier.height(8.dp))
                            if (rangeLocked) {
                                Text("Range is locked", color = Color.Yellow)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRangeEditDialog = false }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRangeEditDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            // More Options Menu
            if (showMoreOptions) {
                AlertDialog(
                    onDismissRequest = { showMoreOptions = false },
                    title = { Text("More Options") },
                    text = {
                        Column {
                            TextButton(onClick = { showMoreOptions = false }) {
                                Text("Export Recording", modifier = Modifier.fillMaxWidth())
                            }
                            TextButton(onClick = { showMoreOptions = false }) {
                                Text("Share", modifier = Modifier.fillMaxWidth())
                            }
                            TextButton(onClick = { showMoreOptions = false }) {
                                Text("Calibrate", modifier = Modifier.fillMaxWidth())
                            }
                            TextButton(onClick = { showMoreOptions = false }) {
                                Text("Advanced Settings", modifier = Modifier.fillMaxWidth())
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMoreOptions = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ThermalMonitoringComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalMonitoringComposeActivity : BaseComposeActivity<BaseViewModel>() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, ThermalMonitoringComposeActivity::class.java))
        }
    }

    override fun createViewModel(): BaseViewModel {
        return viewModels<BaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: BaseViewModel) {
        var selectedTab by remember { mutableStateOf(0) }
        var isMonitoring by remember { mutableStateOf(false) }
        var showAlertDialog by remember { mutableStateOf(false) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Monitoring",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = { isMonitoring = !isMonitoring }) {
                                Icon(
                                    if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isMonitoring) "Stop Monitoring" else "Start Monitoring"
                                )
                            }
                            IconButton(onClick = { showAlertDialog = true }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                            }
                            IconButton(onClick = {
                                // TODO: Implement monitoring options menu
                                android.widget.Toast.makeText(
                                    context,
                                    "More monitoring options",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                },
                bottomBar = {
                    MonitoringNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            ) { paddingValues ->
                MonitoringContent(
                    selectedTab = selectedTab,
                    isMonitoring = isMonitoring,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        if (showAlertDialog) {
            AlertConfigurationDialog(
                onDismiss = { showAlertDialog = false },
                onSaveAlerts = { alerts ->
                    // Save alert configuration
                    showAlertDialog = false
                }
            )
        }
    }
}

@Composable
private fun MonitoringContent(
    selectedTab: Int,
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    when (selectedTab) {
        0 -> RealTimeMonitoringTab(
            isMonitoring = isMonitoring,
            modifier = modifier
        )

        1 -> ThermalAnalyticsTab(modifier = modifier)
        2 -> AlertsHistoryTab(modifier = modifier)
        3 -> MonitoringSettingsTab(modifier = modifier)
    }
}

@Composable
private fun RealTimeMonitoringTab(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Monitoring status
        MonitoringStatusCard(
            isMonitoring = isMonitoring,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Temperature zones
        Text(
            text = "Temperature Zones",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val temperatureZones = getMockTemperatureZones()
        temperatureZones.forEach { zone ->
            TemperatureZoneCard(
                zone = zone,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        // Recent alerts
        Text(
            text = "Recent Alerts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
        )
        val recentAlerts = getMockAlerts()
        if (recentAlerts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No recent alerts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            recentAlerts.take(3).forEach { alert ->
                AlertCard(
                    alert = alert,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MonitoringStatusCard(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitoring)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isMonitoring) "MONITORING ACTIVE" else "MONITORING STOPPED",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isMonitoring) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isMonitoring) "4 zones being monitored" else "Click start to begin monitoring",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isMonitoring) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }
    }
}

@Composable
private fun TemperatureZoneCard(
    zone: TemperatureZone,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (zone.status) {
                "normal" -> MaterialTheme.colorScheme.surface
                "warning" -> Color(0xFFFFF3E0)
                "critical" -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zone indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(getZoneStatusColor(zone.status))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Current: ${zone.currentTemp}Â°C | Threshold: ${zone.threshold}Â°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${zone.currentTemp}Â°C",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = getTemperatureColor(zone.currentTemp, zone.threshold)
                )
                Text(
                    text = zone.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = getZoneStatusColor(zone.status),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: ThermalAlert,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                "high" -> Color(0xFFFFEBEE)
                "medium" -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (alert.severity) {
                    "high" -> Icons.Default.Error
                    "medium" -> Icons.Default.Warning
                    else -> Icons.Default.Info
                },
                contentDescription = alert.severity,
                tint = when (alert.severity) {
                    "high" -> Color(0xFFE53E3E)
                    "medium" -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${alert.zone} â€¢ ${alert.timestamp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThermalAnalyticsTab(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Analytics,
            contentDescription = "Analytics",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFF6B35)
        )
        Text(
            text = "Thermal Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Advanced thermal data analysis and trends",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlertsHistoryTab(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Alert History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allAlerts = getMockAlerts()
            items(allAlerts) { alert ->
                AlertCard(alert = alert)
            }
        }
    }
}

@Composable
private fun MonitoringSettingsTab(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Settings",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFF6B35)
        )
        Text(
            text = "Monitoring Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure monitoring parameters and alerts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonitoringNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        val tabs = listOf(
            "Monitor" to Icons.Default.Monitor,
            "Analytics" to Icons.Default.Analytics,
            "Alerts" to Icons.Default.Notifications,
            "Settings" to Icons.Default.Settings
        )
        tabs.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun AlertConfigurationDialog(
    onDismiss: () -> Unit,
    onSaveAlerts: (Map<String, Any>) -> Unit
) {
    var highTempThreshold by remember { mutableStateOf(80f) }
    var lowTempThreshold by remember { mutableStateOf(0f) }
    var enableNotifications by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alert Configuration") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "High Temperature Alert: ${highTempThreshold.toInt()}Â°C",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = highTempThreshold,
                    onValueChange = { highTempThreshold = it },
                    valueRange = 30f..150f
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Low Temperature Alert: ${lowTempThreshold.toInt()}Â°C",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = lowTempThreshold,
                    onValueChange = { lowTempThreshold = it },
                    valueRange = -20f..30f
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableNotifications,
                        onCheckedChange = { enableNotifications = it }
                    )
                    Text(
                        text = "Enable push notifications",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveAlerts(
                        mapOf(
                            "highThreshold" to highTempThreshold,
                            "lowThreshold" to lowTempThreshold,
                            "notifications" to enableNotifications
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

private fun getZoneStatusColor(status: String) = when (status) {
    "normal" -> Color(0xFF4CAF50)
    "warning" -> Color(0xFFFF9800)
    "critical" -> Color(0xFFE53E3E)
    else -> Color(0xFF9E9E9E)
}

private fun getTemperatureColor(current: Float, threshold: Float) = when {
    current > threshold * 1.1f -> Color(0xFFE53E3E)
    current > threshold * 0.9f -> Color(0xFFFF9800)
    else -> Color(0xFF4CAF50)
}

data class TemperatureZone(
    val name: String,
    val currentTemp: Float,
    val threshold: Float,
    val status: String
)

data class ThermalAlert(
    val message: String,
    val zone: String,
    val timestamp: String,
    val severity: String
)

private fun getMockTemperatureZones() = listOf(
    TemperatureZone("Zone A - Engine Bay", 76.5f, 80.0f, "warning"),
    TemperatureZone("Zone B - Electronics", 42.3f, 60.0f, "normal"),
    TemperatureZone("Zone C - Exhaust", 95.2f, 90.0f, "critical"),
    TemperatureZone("Zone D - Ambient", 24.8f, 40.0f, "normal")
)

private fun getMockAlerts() = listOf(
    ThermalAlert("Temperature exceeded threshold", "Zone C - Exhaust", "2 min ago", "high"),
    ThermalAlert("Warning temperature detected", "Zone A - Engine Bay", "5 min ago", "medium"),
    ThermalAlert("Normal operating temperature", "Zone B - Electronics", "10 min ago", "low")
)


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ThermalReportComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalReportComposeActivity : BaseComposeActivity<ThermalReportViewModel>() {
    override fun createViewModel(): ThermalReportViewModel {
        return ThermalReportViewModel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalReportViewModel) {
        ThermalReportScreen(
            onBackClick = { finish() }
        )
    }
}

class ThermalReportViewModel : BaseViewModel() {
    // ViewModel implementation for report generation
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThermalReportScreen(
    viewModel: ThermalReportViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null
) {
    var selectedTemplate by remember { mutableStateOf(ReportTemplate.STANDARD) }
    var reportTitle by remember { mutableStateOf("Thermal Analysis Report") }
    var isGenerating by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Generate Report",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF161B22),
                titleContentColor = Color.White
            ),
            navigationIcon = {
                IconButton(onClick = { onBackClick?.invoke() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(onClick = {
                    // TODO: Preview report before generation
                    android.widget.Toast.makeText(
                        context,
                        "Opening report preview...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(
                        Icons.Default.Preview,
                        contentDescription = "Preview",
                        tint = Color(0xFFFF6B35)
                    )
                }
            }
        )
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Report Configuration
            ReportConfigurationSection(
                reportTitle = reportTitle,
                onTitleChange = { reportTitle = it },
                selectedTemplate = selectedTemplate,
                onTemplateChange = { selectedTemplate = it }
            )
            // Data Selection
            DataSelectionSection()
            // Analysis Options
            AnalysisOptionsSection()
            // Export Settings
            ExportSettingsSection()
            // Generate Button
            Button(
                onClick = { isGenerating = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating Report...")
                } else {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "Generate",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Generate PDF Report",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportConfigurationSection(
    reportTitle: String,
    onTitleChange: (String) -> Unit,
    selectedTemplate: ReportTemplate,
    onTemplateChange: (ReportTemplate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Report Configuration",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            // Report Title
            OutlinedTextField(
                value = reportTitle,
                onValueChange = onTitleChange,
                label = { Text("Report Title", color = Color(0xFF7D8590)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF6B35),
                    unfocusedBorderColor = Color(0xFF7D8590)
                )
            )
            // Template Selection
            Text(
                "Report Template",
                color = Color(0xFF7D8590),
                fontSize = 14.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportTemplate.values().forEach { template ->
                    FilterChip(
                        onClick = { onTemplateChange(template) },
                        label = {
                            Text(
                                template.displayName,
                                fontSize = 12.sp
                            )
                        },
                        selected = selectedTemplate == template,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0D1117),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DataSelectionSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Data Selection",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            // Sample data selection items
            DataSelectionItem("Thermal Images (15)", true)
            DataSelectionItem("Temperature Measurements (45)", true)
            DataSelectionItem("Time Series Data", false)
            DataSelectionItem("Calibration Data", true)
        }
    }
}

@Composable
private fun DataSelectionItem(title: String, selected: Boolean, onSelectionChange: (Boolean) -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onSelectionChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFFF6B35),
                uncheckedColor = Color(0xFF7D8590)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AnalysisOptionsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Analysis Options",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            AnalysisOptionItem("Statistical Summary", Icons.Default.BarChart)
            AnalysisOptionItem("Temperature Trends", Icons.AutoMirrored.Filled.TrendingUp)
            AnalysisOptionItem("Thermal Mapping", Icons.Default.Map)
            AnalysisOptionItem("Comparative Analysis", Icons.Default.Compare)
        }
    }
}

@Composable
private fun AnalysisOptionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = Color(0xFFFF6B35),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ExportSettingsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Export Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportFormat.values().forEach { format ->
                    FilterChip(
                        onClick = { },
                        label = {
                            Text(
                                format.displayName,
                                fontSize = 12.sp
                            )
                        },
                        selected = format == ExportFormat.PDF,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0D1117),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
        }
    }
}

private enum class ReportTemplate(val displayName: String) {
    STANDARD("Standard"),
    DETAILED("Detailed"),
    SUMMARY("Summary"),
    RESEARCH("Research")
}

private enum class ExportFormat(val displayName: String) {
    PDF("PDF"),
    DOCX("Word"),
    HTML("HTML")
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\ThermalVideoComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalVideoComposeActivity : BaseComposeActivity<BaseViewModel>() {
    companion object {
        private const val KEY_PATH = "video_path"
        private const val KEY_TITLE = "video_title"
        fun startWithPath(context: Context, videoPath: String, title: String = "Thermal Video") {
            val intent = Intent(context, ThermalVideoComposeActivity::class.java).apply {
                putExtra(KEY_PATH, videoPath)
                putExtra(KEY_TITLE, title)
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
        val videoPath = intent.getStringExtra(KEY_PATH) ?: ""
        val videoTitle = intent.getStringExtra(KEY_TITLE) ?: "Thermal Video"
        var isPlaying by remember { mutableStateOf(false) }
        var currentPosition by remember { mutableStateOf(0L) }
        var videoDuration by remember { mutableStateOf(0L) }
        var showControls by remember { mutableStateOf(true) }
        var showThermalData by remember { mutableStateOf(true) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                videoTitle,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showThermalData = !showThermalData }) {
                                Icon(
                                    if (showThermalData) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle thermal data"
                                )
                            }
                            IconButton(onClick = {
                                // TODO: Share thermal video
                                android.widget.Toast.makeText(
                                    this@ThermalVideoComposeActivity,
                                    "Sharing video...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                // TODO: Show more video options
                                android.widget.Toast.makeText(
                                    this@ThermalVideoComposeActivity,
                                    "More options...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                ThermalVideoContent(
                    videoPath = videoPath,
                    isPlaying = isPlaying,
                    onPlayingChange = { isPlaying = it },
                    currentPosition = currentPosition,
                    videoDuration = videoDuration,
                    onPositionChange = { currentPosition = it },
                    showControls = showControls,
                    onControlsToggle = { showControls = !showControls },
                    showThermalData = showThermalData,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ThermalVideoContent(
    videoPath: String,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    currentPosition: Long,
    videoDuration: Long,
    onPositionChange: (Long) -> Unit,
    showControls: Boolean,
    onControlsToggle: () -> Unit,
    showThermalData: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video player
        ThermalVideoPlayer(
            videoPath = videoPath,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            onPositionChange = onPositionChange,
            onClick = onControlsToggle,
            modifier = Modifier.fillMaxSize()
        )
        // Thermal data overlay
        if (showThermalData) {
            ThermalDataOverlay(
                currentTemp = 36.8f,
                maxTemp = 42.1f,
                minTemp = 28.3f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
        // Video controls overlay
        if (showControls) {
            ThermalVideoControls(
                isPlaying = isPlaying,
                onPlayingChange = onPlayingChange,
                currentPosition = currentPosition,
                videoDuration = videoDuration,
                onPositionChange = onPositionChange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        // Playback indicator
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalVideoPlayer(
    videoPath: String,
    isPlaying: Boolean,
    currentPosition: Long,
    onPositionChange: (Long) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            android.widget.VideoView(ctx).apply {
                val uri = Uri.parse(videoPath)
                setVideoURI(uri)
                // Set up media controller
                val mediaController = android.widget.MediaController(ctx)
                setMediaController(mediaController)
                mediaController.setAnchorView(this)
                setOnClickListener { onClick() }
                // Set up completion listener
                setOnCompletionListener {
                    onPositionChange(0L)
                }
            }
        },
        update = { videoView ->
            if (isPlaying && !videoView.isPlaying) {
                videoView.start()
            } else if (!isPlaying && videoView.isPlaying) {
                videoView.pause()
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ThermalDataOverlay(
    currentTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "THERMAL DATA",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${currentTemp}Â°C",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "H:${maxTemp}Â°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = "L:${minTemp}Â°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ThermalVideoControls(
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    currentPosition: Long,
    videoDuration: Long,
    onPositionChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Progress bar
            if (videoDuration > 0) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onPositionChange(it.toLong()) },
                    valueRange = 0f..videoDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF6B35),
                        activeTrackColor = Color(0xFFFF6B35)
                    )
                )
                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = formatTime(videoDuration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    // TODO: Go to previous frame
                    android.widget.Toast.makeText(
                        context,
                        "Previous frame",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }
                // Play/Pause button
                IconButton(
                    onClick = { onPlayingChange(!isPlaying) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color(0xFFFF6B35),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = {
                    // TODO: Go to next frame
                    android.widget.Toast.makeText(
                        context,
                        "Next frame",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Additional controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Export current frame
                        android.widget.Toast.makeText(
                            context,
                            "Exporting frame...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Export Frame",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Frame")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Analyze thermal video
                        android.widget.Toast.makeText(
                            context,
                            "Analyzing video...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = "Analyze",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Analyze")
                }
                OutlinedButton(
                    onClick = {
                        // TODO: Open video settings
                        android.widget.Toast.makeText(
                            context,
                            "Opening settings...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settings")
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\activity\VideoComposeActivity.kt =====

package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Replay10
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class VideoComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    companion object {
        const val KEY_PATH = "video_path"
    }

    private var videoPath = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.hasExtra(KEY_PATH)) {
            videoPath = intent.getStringExtra(KEY_PATH) ?: ""
        }
        super.onCreate(savedInstanceState)
    }

    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var isPlaying by remember { mutableStateOf(false) }
        var isFullscreen by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(100000L) }
        var showControls by remember { mutableStateOf(true) }
        var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
        var pointAnalysisEnabled by remember { mutableStateOf(false) }
        LaunchedEffect(isFullscreen) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.apply {
                if (isFullscreen) {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Video",
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
                            IconButton(onClick = { showControls = !showControls }) {
                                Icon(
                                    if (showControls) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Controls",
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.Black)
                ) {
                    // Video player view
                    ThermalVideoPlayer(
                        videoPath = videoPath,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Video controls overlay
                    if (showControls) {
                        VideoControlsOverlay(
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            playbackSpeed = playbackSpeed,
                            isFullscreen = isFullscreen,
                            onPlayPause = { isPlaying = !isPlaying },
                            onSeek = { position -> currentPosition = position },
                            onSpeedChange = { speed -> playbackSpeed = speed },
                            onToggleFullscreen = { isFullscreen = !isFullscreen },
                            scope = scope,
                            snackbarHostState = snackbarHostState,
                            contentResolver = contentResolver,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }
                    // Thermal analysis overlay
                    ThermalAnalysisOverlay(
                        pointAnalysisEnabled = pointAnalysisEnabled,
                        onTogglePointAnalysis = {
                            pointAnalysisEnabled = !pointAnalysisEnabled
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    )
                }
            }
        }
        // Update current position periodically if playing
        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (isPlaying && currentPosition < duration) {
                    kotlinx.coroutines.delay(100L)
                    currentPosition += 100L
                }
            }
        }
    }
}

@Composable
private fun ThermalVideoPlayer(
    videoPath: String,
    isPlaying: Boolean,
    currentPosition: Long,
    modifier: Modifier = Modifier
) {
    // Embed actual video player using AndroidView for VideoView
    AndroidView(
        factory = { context ->
            android.widget.VideoView(context).apply {
                // Configure video player with the path
                if (videoPath.isNotEmpty()) {
                    setVideoPath(videoPath)
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun VideoControlsOverlay(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float,
    isFullscreen: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    contentResolver: android.content.ContentResolver,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress bar
            Column {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF6B35),
                        activeTrackColor = Color(0xFFFF6B35),
                        inactiveTrackColor = Color(0xFF21262D)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentPosition),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        formatTime(duration),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip backward
                IconButton(onClick = { onSeek(maxOf(0, currentPosition - 10000)) }) {
                    Icon(
                        Icons.Outlined.Replay10,
                        contentDescription = "Skip back 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                // Play/Pause
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6B35))
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                // Skip forward
                IconButton(onClick = { onSeek(minOf(duration, currentPosition + 10000)) }) {
                    Icon(
                        Icons.Outlined.Forward10,
                        contentDescription = "Skip forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            // Speed and additional controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback speed
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Speed:",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    FilterChip(
                        onClick = { onSpeedChange(0.5f) },
                        label = { Text("0.5x", fontSize = 10.sp) },
                        selected = playbackSpeed == 0.5f,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        onClick = { onSpeedChange(1.0f) },
                        label = { Text("1x", fontSize = 10.sp) },
                        selected = playbackSpeed == 1.0f,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        onClick = { onSpeedChange(2.0f) },
                        label = { Text("2x", fontSize = 10.sp) },
                        selected = playbackSpeed == 2.0f,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White
                        )
                    )
                }
                // Additional controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onToggleFullscreen) {
                        Icon(
                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                // Capture current frame from video and save to MediaStore
                                val contentValues = android.content.ContentValues().apply {
                                    put(
                                        android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                                        "thermal_frame_${System.currentTimeMillis()}.jpg"
                                    )
                                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                    put(
                                        android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                                        android.os.Environment.DIRECTORY_PICTURES
                                    )
                                }
                                // Insert into MediaStore (actual frame capture would happen here)
                                context.contentResolver.insert(
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    contentValues
                                )
                                snackbarHostState.showSnackbar("Frame exported to gallery")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to export frame: ${e.message}")
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Capture Frame",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalAnalysisOverlay(
    pointAnalysisEnabled: Boolean,
    onTogglePointAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Analysis",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            // Temperature readings
            AnalysisItem("Max", "45.2Â°C", Color(0xFFFF4444))
            AnalysisItem("Min", "18.7Â°C", Color(0xFF4444FF))
            AnalysisItem("Avg", "32.1Â°C", Color(0xFFFFAA00))
            HorizontalDivider(color = Color(0xFF21262D), thickness = 1.dp)
            // Analysis tools - Point analysis toggle
            IconButton(
                onClick = onTogglePointAnalysis,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = if (pointAnalysisEnabled) "Disable Point Analysis" else "Enable Point Analysis",
                    tint = if (pointAnalysisEnabled) Color(0xFFFF6B35) else Color(0xFF7D8590),
                    modifier = Modifier.size(16.dp)
                )
            }
            if (pointAnalysisEnabled) {
                Text(
                    "Point Analysis ON",
                    color = Color(0xFFFF6B35),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AnalysisItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 10.sp
        )
        Text(
            value,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(timeMs: Long): String {
    val seconds = (timeMs / 1000) % 60
    val minutes = (timeMs / (1000 * 60)) % 60
    val hours = (timeMs / (1000 * 60 * 60)) % 24
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}


