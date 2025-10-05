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
                            listOf("Auto Enhance", "Noise Reduction", "Detail Enhancement", "Color Correction").forEach { mode ->
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
                        "• Best quality images detected automatically",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                    Text(
                        "• Optimal thermal range suggestions",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                    Text(
                        "• Smart batch processing available",
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