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
                        Text("Perspective: ${perspective.toInt()}°")
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
                        Text("Rotation: ${rotation.toInt()}°")
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
                    onClick = {  },
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