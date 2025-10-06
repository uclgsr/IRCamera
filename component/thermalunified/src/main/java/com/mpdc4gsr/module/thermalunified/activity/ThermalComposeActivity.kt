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