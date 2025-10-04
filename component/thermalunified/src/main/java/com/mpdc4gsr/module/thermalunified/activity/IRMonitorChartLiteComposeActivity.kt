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
                                onClick = { /* TODO: Implement finish monitoring
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }
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
                                                "Current: %.1f°C".format(currentTemp),
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                "Low: %.1f°C".format(lowTemp),
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
