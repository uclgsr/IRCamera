package com.mpdc4gsr.component.thermal.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.thermal.feature.device.ThermalColorPalette
import com.mpdc4gsr.component.thermal.feature.device.ThermalGainMode
import com.mpdc4gsr.component.thermal.feature.device.TopdonThermalDeviceManager
import com.mpdc4gsr.component.thermal.feature.navigation.ThermalFeatureContent
import com.mpdc4gsr.component.thermal.feature.navigation.ThermalFeatureDescriptor
import com.mpdc4gsr.component.thermal.feature.navigation.ThermalFeatureRegistry
import com.mpdc4gsr.component.thermal.feature.presentation.ThermalFeatureCoordinator
import com.mpdc4gsr.component.thermal.feature.presentation.ThermalPresentationState
import com.mpdc4gsr.component.thermal.feature.ui.ThermalFeatureScaffoldContent
import com.mpdc4gsr.component.thermal.feature.ui.components.ThermalControlPanel
import com.mpdc4gsr.component.thermal.fragment.ThermalComposeFragment
import com.mpdc4gsr.component.thermal.viewmodel.ThermalViewModel
import kotlinx.coroutines.launch

class ThermalComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    private val deviceManager by lazy { TopdonThermalDeviceManager(this, lifecycleScope) }

    override fun createViewModel(): ThermalViewModel = viewModels<ThermalViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        val selectedTool by viewModel.selectedToolAction.collectAsStateWithLifecycle()
        val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
        val registry = remember { ThermalFeatureRegistry() }
        val coordinator = remember { ThermalFeatureCoordinator(registry, deviceManager, lifecycleScope) }
        val state by coordinator.state.collectAsStateWithLifecycle()
        var thermalMode by remember { mutableStateOf(0) }
        var paletteIndex by remember { mutableStateOf(0) }
        val palettes =
            remember {
                listOf(
                    ThermalColorPalette.Ironbow,
                    ThermalColorPalette.Rainbow,
                    ThermalColorPalette.WhiteHot,
                    ThermalColorPalette.BlackHot,
                )
            }

        val features =
            remember(selectedTool) {
                buildFeatureDescriptors(
                    selectedTool = selectedTool,
                    onToolSelected = viewModel::onToolActionSelected,
                )
            }

        LaunchedEffect(features) {
            registry.registerAll(features)
            coordinator.refreshRegistry()
        }

        LaunchedEffect(Unit) {
            viewModel.attachDeviceManager(deviceManager)
            viewModel.connectHardware()
            viewModel.startStream()
        }

        FeatureStreamGuard(
            state = state,
            onStartStream = viewModel::startStream,
            onStopStream = viewModel::stopStream,
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Thermal Processing", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                )
            },
            containerColor = Color.Black,
        ) { paddingValues ->
            ThermalFeatureScaffoldContent(
                state = state,
                onTabSelected = coordinator::selectFeature,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                topContent = {
                    ThermalProcessingHeader()
                    if (state.activeFeatureId == FeatureIds.CAMERA) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ThermalControlPanel(
                            selectedModeIndex = thermalMode,
                            isRecording = isRecording,
                            onModeSelected = { index ->
                                thermalMode = index
                                val gainMode =
                                    when (index) {
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
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                        )
                    }
                },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            deviceManager.stopStream()
            deviceManager.disconnect()
        }
    }

    @Composable
    private fun FeatureStreamGuard(
        state: ThermalPresentationState,
        onStartStream: () -> Unit,
        onStopStream: () -> Unit,
    ) {
        val activeId = state.activeFeatureId
        LaunchedEffect(activeId) {
            if (activeId == FeatureIds.CAMERA) {
                onStartStream()
            } else {
                onStopStream()
            }
        }
    }

    private fun buildFeatureDescriptors(
        selectedTool: Int?,
        onToolSelected: (Int) -> Unit,
    ): List<ThermalFeatureDescriptor> =
        listOf(
            ThermalFeatureDescriptor(
                id = FeatureIds.CAMERA,
                title = "Camera",
                iconId = "camera",
                priority = 100,
                content = ThermalFeatureContent.FragmentHost { ThermalComposeFragment() },
            ),
            ThermalFeatureDescriptor(
                id = FeatureIds.MEASURE,
                title = "Measure",
                iconId = "measure",
                priority = 80,
                content =
                    ThermalFeatureContent.Compose {
                        ThermalToolsetScreen(
                            title = "Measurement Overlays",
                            subtitle = "Define points and regions to analyse live Topdon data.",
                            tools = measurementTools(),
                            selectedAction = selectedTool,
                            onToolSelected = onToolSelected,
                        )
                    },
            ),
            ThermalFeatureDescriptor(
                id = FeatureIds.ANALYSIS,
                title = "Analysis",
                iconId = "analysis",
                priority = 60,
                content =
                    ThermalFeatureContent.Compose {
                        ThermalToolsetScreen(
                            title = "Analysis Toolkit",
                            subtitle = "Launch advanced analytics across histograms, profiles, and reports.",
                            tools = analysisTools(),
                            selectedAction = selectedTool,
                            onToolSelected = onToolSelected,
                        )
                    },
            ),
            ThermalFeatureDescriptor(
                id = FeatureIds.PALETTE,
                title = "Palette",
                iconId = "palette",
                priority = 40,
                content =
                    ThermalFeatureContent.Compose {
                        ThermalToolsetScreen(
                            title = "Palette Options",
                            subtitle = "Switch Topdon colour palettes on the fly.",
                            tools = paletteTools(),
                            selectedAction = selectedTool,
                            onToolSelected = onToolSelected,
                        )
                    },
            ),
            ThermalFeatureDescriptor(
                id = FeatureIds.SETTINGS,
                title = "Settings",
                iconId = "settings",
                priority = 20,
                content =
                    ThermalFeatureContent.Compose {
                        ThermalToolsetScreen(
                            title = "Thermal Settings",
                            subtitle = "Access emissivity, reference temperature, and distance controls.",
                            tools = settingsTools(),
                            selectedAction = selectedTool,
                            onToolSelected = onToolSelected,
                        )
                    },
            ),
        )

    @Composable
    private fun ThermalProcessingHeader() {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Processing Suite",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "Coordinate capture, measurement, and analysis across reusable thermal modules.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
            )
        }
    }

    @Composable
    private fun ThermalToolsetScreen(
        title: String,
        subtitle: String,
        tools: List<ThermalTool>,
        selectedAction: Int?,
        onToolSelected: (Int) -> Unit,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2933)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (tools.isEmpty()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No tools available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF),
                        )
                    }
                } else {
                    LazyRow(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(tools) { tool ->
                            ThermalToolButton(
                                tool = tool,
                                isSelected = selectedAction == tool.actionCode,
                                onClick = { onToolSelected(tool.actionCode) },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun measurementTools(): List<ThermalTool> =
        listOf(
            ThermalTool("Point", Icons.Default.Place, 1001),
            ThermalTool("Line", Icons.Default.Timeline, 1002),
            ThermalTool("Rectangle", Icons.Default.CropFree, 1003),
            ThermalTool("Circle", Icons.Default.RadioButtonUnchecked, 1004),
        )

    private fun analysisTools(): List<ThermalTool> =
        listOf(
            ThermalTool("Histogram", Icons.Default.BarChart, 2001),
            ThermalTool("Profile", Icons.AutoMirrored.Filled.ShowChart, 2002),
            ThermalTool("Report", Icons.Default.Description, 2003),
        )

    private fun paletteTools(): List<ThermalTool> =
        listOf(
            ThermalTool("Iron", Icons.Default.Palette, 3001),
            ThermalTool("Rainbow", Icons.Default.ColorLens, 3002),
            ThermalTool("Grayscale", Icons.Default.InvertColors, 3003),
            ThermalTool("High Heat", Icons.Default.LocalFireDepartment, 3004),
        )

    private fun settingsTools(): List<ThermalTool> =
        listOf(
            ThermalTool("Emissivity", Icons.Default.Tune, 4001),
            ThermalTool("Temperature", Icons.Default.Thermostat, 4002),
            ThermalTool("Distance", Icons.Outlined.Straighten, 4003),
        )

    private object FeatureIds {
        const val CAMERA = "thermal-compose-camera"
        const val MEASURE = "thermal-compose-measure"
        const val ANALYSIS = "thermal-compose-analysis"
        const val PALETTE = "thermal-compose-palette"
        const val SETTINGS = "thermal-compose-settings"
    }
}

data class ThermalTool(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val actionCode: Int,
)

@Composable
private fun ThermalToolButton(
    tool: ThermalTool,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) Color(0xFFFF6B35) else Color.Transparent,
                contentColor = if (isSelected) Color.White else Color(0xFF94A3B8),
            ),
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) Color(0xFFFF6B35) else Color(0xFF334155),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                tool.icon,
                contentDescription = tool.name,
                tint = Color.White,
            )
            Text(
                text = tool.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}



