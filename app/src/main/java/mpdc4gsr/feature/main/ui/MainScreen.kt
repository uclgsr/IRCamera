package mpdc4gsr.feature.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

/**
 * Main Screen - Replaces MainActivity with Compose implementation
 * Primary entry point for the IR Camera application
 */
@Composable
fun MainScreen(
    onNavigateToSensors: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        // Title bar
        TitleBar(
            title = "IR Camera",
            showBackButton = false
        ) {
            TitleBarAction(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onNavigateToSettings
            )
        }

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> SensorDashboardTab()
                1 -> GalleryTab(onNavigateToGallery = onNavigateToGallery)
                2 -> ProfileTab(onNavigateToProfile = onNavigateToProfile)
            }
        }

        // Bottom navigation
        NavigationBar(
            containerColor = Color(0xFF2A2A2A)
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Sensors") },
                label = { Text("Sensors") },
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Photo, contentDescription = "Gallery") },
                label = { Text("Gallery") },
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = false,
                onClick = { onNavigateToSettings() },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = Color.Gray
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

/**
 * Sensor Dashboard Tab - Main sensor interface
 */
@Composable
private fun SensorDashboardTab(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Welcome to IR Camera",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Multi-modal sensor platform for thermal imaging, GSR monitoring, and RGB camera recording",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // System status overview
        SystemStatusOverview()
    }
}

/**
 * Gallery Tab - Image and video gallery
 */
@Composable
private fun GalleryTab(
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Media Gallery",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "View thermal images, recordings, and data exports",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToGallery,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Open Gallery")
                }
            }
        }
    }
}

/**
 * Profile Tab - User profile and account
 */
@Composable
private fun ProfileTab(
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Profile",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "User account, research templates, and data management",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("View Profile")
                }
            }
        }
    }
}

/**
 * System status overview component
 */
@Composable
private fun SystemStatusOverview(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Status",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem("GSR", "Connected", Color.Green)
                StatusItem("Thermal", "Ready", MaterialTheme.colorScheme.primary)
                StatusItem("RGB", "Active", Color.Green)
            }
        }
    }
}

/**
 * Status item component
 */
@Composable
private fun StatusItem(
    label: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = status,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    IRCameraTheme {
        MainScreen()
    }
}