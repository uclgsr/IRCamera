package com.mpdc4gsr.module.thermalunified.activity

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.module.thermalunified.feature.device.ThermalColorPalette
import com.mpdc4gsr.module.thermalunified.feature.device.ThermalGainMode
import com.mpdc4gsr.module.thermalunified.feature.device.TopdonThermalDeviceManager
import com.mpdc4gsr.module.thermalunified.feature.ui.components.ThermalControlPanel
import com.mpdc4gsr.module.thermalunified.feature.ui.components.ThermalStatusBanner
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.launch

class BaseIRComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    private val deviceManager by lazy { TopdonThermalDeviceManager(this, lifecycleScope) }
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
        val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
        var thermalMode by remember { mutableIntStateOf(0) }
        var paletteIndex by remember { mutableIntStateOf(0) }
        val palettes = remember {
            listOf(
                ThermalColorPalette.Ironbow,
                ThermalColorPalette.Rainbow,
                ThermalColorPalette.WhiteHot,
                ThermalColorPalette.BlackHot
            )
        }
        LaunchedEffect(Unit) {
            viewModel.attachDeviceManager(deviceManager)
            viewModel.connectHardware()
            viewModel.startStream()
        }
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
                        IconButton(onClick = { viewModel.triggerManualCalibration() }) {
                            Icon(
                                Icons.Default.Widgets,
                                contentDescription = "Manual NUC",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            // Placeholder for future settings screen wiring
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
                ThermalStatusBanner(
                    status = deviceStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    ThermalCameraSurface(
                        modifier = Modifier.fillMaxSize()
                    )
                    ThermalControlPanel(
                        selectedModeIndex = thermalMode,
                        isRecording = isRecording,
                        onModeSelected = { modeIndex ->
                            thermalMode = modeIndex
                            val gainMode = when (modeIndex) {
                                0 -> ThermalGainMode.High
                                1 -> ThermalGainMode.Low
                                else -> ThermalGainMode.Auto
                            }
                            viewModel.updateGainMode(gainMode)
                        },
                        onCapture = viewModel::captureSnapshot,
                        onToggleRecording = viewModel::toggleRecording,
                        onPaletteClick = {
                            paletteIndex = (paletteIndex + 1) % palettes.size
                            viewModel.updatePalette(palettes[paletteIndex])
                        },
                        onAdjustClick = viewModel::triggerManualCalibration,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            deviceManager.stopStream()
            deviceManager.disconnect()
        }
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

