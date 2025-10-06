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