package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMainActivityViewModel

/**
 * Modern Compose implementation of the thermal main activity
 * Demonstrates successful cross-module integration using shared BaseComposeActivity
 */
class IRMainComposeActivity : BaseComposeActivity<IRMainActivityViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibUnifiedTheme {
                ThermalMainScreen()
            }
        }
    }
}

@Composable
private fun ThermalMainScreen(
    viewModel: IRMainActivityViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Thermal Camera",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF161B22),
                titleContentColor = Color.White
            ),
            actions = {
                IconButton(onClick = { /* Settings */ }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFFF6B35)
                    )
                }
            }
        )

        // Main Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> ThermalCameraTab()
                1 -> ThermalGalleryTab()
                2 -> ThermalAnalysisTab()
                3 -> ThermalReportsTab()
            }
        }

        // Bottom Navigation
        ThermalBottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}

@Composable
private fun ThermalBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        ThermalTab("Camera", Icons.Default.Camera),
        ThermalTab("Gallery", Icons.Default.Photo),
        ThermalTab("Analysis", Icons.Default.Analytics),
        ThermalTab("Reports", Icons.Default.Description)
    )

    NavigationBar(
        containerColor = Color(0xFF161B22),
        contentColor = Color.White
    ) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = tab.title,
                        tint = if (selectedTab == index) Color(0xFFFF6B35) else Color(0xFF7D8590)
                    )
                },
                label = {
                    Text(
                        tab.title,
                        color = if (selectedTab == index) Color(0xFFFF6B35) else Color(0xFF7D8590),
                        fontSize = 12.sp
                    )
                },
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFFF6B35),
                    selectedTextColor = Color(0xFFFF6B35),
                    indicatorColor = Color(0xFF21262D),
                    unselectedIconColor = Color(0xFF7D8590),
                    unselectedTextColor = Color(0xFF7D8590)
                )
            )
        }
    }
}

@Composable
private fun ThermalCameraTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Camera Preview Placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Camera Preview",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Thermal Camera Preview",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Connect thermal camera to start",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Control Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThermalActionButton(
                icon = Icons.Default.PhotoCamera,
                text = "Capture",
                onClick = { /* Capture */ }
            )
            ThermalActionButton(
                icon = Icons.Default.VideoCall,
                text = "Record",
                onClick = { /* Record */ }
            )
            ThermalActionButton(
                icon = Icons.Default.Tune,
                text = "Settings",
                onClick = { /* Settings */ }
            )
        }
    }
}

@Composable
private fun ThermalGalleryTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Photo,
            contentDescription = "Gallery",
            tint = Color(0xFFFF6B35),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Thermal Gallery",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "View and manage thermal images",
            color = Color(0xFF7D8590),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ThermalAnalysisTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Analytics,
            contentDescription = "Analysis",
            tint = Color(0xFFFF6B35),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Thermal Analysis",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Advanced temperature analysis tools",
            color = Color(0xFF7D8590),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ThermalReportsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = "Reports",
            tint = Color(0xFFFF6B35),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Thermal Reports",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Generate professional thermal reports",
            color = Color(0xFF7D8590),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ThermalActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF6B35),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class ThermalTab(
    val title: String,
    val icon: ImageVector
)