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

        LibUnifiedTheme {
            Scaffold(
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
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Export",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { }) {
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
                            1 -> MonitorRealTimeTab(isRecording = isRecording)
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
    isRecording: Boolean
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
                "Live: 35.2°C",
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FloatingActionButton(
            onClick = { },
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
            onClick = { },
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
            onClick = { },
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