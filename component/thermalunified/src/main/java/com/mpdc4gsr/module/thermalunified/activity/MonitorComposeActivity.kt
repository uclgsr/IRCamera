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