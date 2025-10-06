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

@Composable
fun MainScreen(
    onNavigateToSensors: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSensor: (mpdc4gsr.core.ui.model.SensorType) -> Unit = {},
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
                0 -> SensorDashboardTab(
                    onSensorClick = onNavigateToSensor
                )
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

@Composable
private fun SensorDashboardTab(
    onSensorClick: (mpdc4gsr.core.ui.model.SensorType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Use remember for stable state holders to prevent unnecessary recomposition
    var gsrState by remember { mutableStateOf(mpdc4gsr.core.ui.model.SensorState.Connected) }
    var thermalState by remember { mutableStateOf(mpdc4gsr.core.ui.model.SensorState.Connected) }
    var rgbState by remember { mutableStateOf(mpdc4gsr.core.ui.model.SensorState.Connected) }
    // Memoize action handlers to prevent recreating lambdas on every recomposition
    val gsrActionHandler = remember {
        { action: mpdc4gsr.core.ui.model.GSRAction ->
            when (action) {
                is mpdc4gsr.core.ui.model.GSRAction.Connect ->
                    gsrState = mpdc4gsr.core.ui.model.SensorState.Connecting
                is mpdc4gsr.core.ui.model.GSRAction.Disconnect ->
                    gsrState = mpdc4gsr.core.ui.model.SensorState.Disconnected
                is mpdc4gsr.core.ui.model.GSRAction.StartStream ->
                    gsrState = mpdc4gsr.core.ui.model.SensorState.Streaming
                is mpdc4gsr.core.ui.model.GSRAction.StopStream ->
                    gsrState = mpdc4gsr.core.ui.model.SensorState.Connected
                is mpdc4gsr.core.ui.model.GSRAction.ConfigureDevice -> {}
            }
        }
    }
    val thermalActionHandler = remember {
        { action: mpdc4gsr.core.ui.model.ThermalAction ->
            when (action) {
                is mpdc4gsr.core.ui.model.ThermalAction.Connect ->
                    thermalState = mpdc4gsr.core.ui.model.SensorState.Connecting
                is mpdc4gsr.core.ui.model.ThermalAction.Disconnect ->
                    thermalState = mpdc4gsr.core.ui.model.SensorState.Disconnected
                is mpdc4gsr.core.ui.model.ThermalAction.StartPreview ->
                    thermalState = mpdc4gsr.core.ui.model.SensorState.Streaming
                is mpdc4gsr.core.ui.model.ThermalAction.StopPreview ->
                    thermalState = mpdc4gsr.core.ui.model.SensorState.Connected
                is mpdc4gsr.core.ui.model.ThermalAction.Calibrate -> {}
                is mpdc4gsr.core.ui.model.ThermalAction.OpenSettings -> {}
            }
        }
    }
    val rgbActionHandler = remember {
        { action: mpdc4gsr.core.ui.model.CameraAction ->
            when (action) {
                is mpdc4gsr.core.ui.model.CameraAction.Connect ->
                    rgbState = mpdc4gsr.core.ui.model.SensorState.Connecting
                is mpdc4gsr.core.ui.model.CameraAction.Disconnect ->
                    rgbState = mpdc4gsr.core.ui.model.SensorState.Disconnected
                is mpdc4gsr.core.ui.model.CameraAction.StartPreview ->
                    rgbState = mpdc4gsr.core.ui.model.SensorState.Streaming
                is mpdc4gsr.core.ui.model.CameraAction.StopPreview ->
                    rgbState = mpdc4gsr.core.ui.model.SensorState.Connected
                is mpdc4gsr.core.ui.model.CameraAction.SetResolution -> {}
            }
        }
    }
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
                    text = "MPDC4GSR",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Multi-sensor data collection platform for GSR",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
        // System status overview
        SystemStatusOverview()
        // Sensor cards for direct access
        // Using memoized handlers to prevent unnecessary recomposition
        mpdc4gsr.core.ui.components.sensors.GSRSensorCard(
            state = gsrState,
            onStateChange = { gsrState = it },
            onClick = { onSensorClick(mpdc4gsr.core.ui.model.SensorType.GSR) },
            onAction = gsrActionHandler
        )
        mpdc4gsr.core.ui.components.sensors.ThermalSensorCard(
            state = thermalState,
            onStateChange = { thermalState = it },
            onClick = { onSensorClick(mpdc4gsr.core.ui.model.SensorType.ThermalIR) },
            onAction = thermalActionHandler
        )
        mpdc4gsr.core.ui.components.sensors.RGBCameraSensorCard(
            state = rgbState,
            onStateChange = { rgbState = it },
            onClick = { onSensorClick(mpdc4gsr.core.ui.model.SensorType.RGBCamera) },
            onAction = rgbActionHandler
        )
    }
}

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

@Composable
private fun SystemStatusOverview(
    modifier: Modifier = Modifier
) {
    // Memoize colors to prevent recomposition when theme changes
    val connectedColor = Color.Green
    val primaryColor = MaterialTheme.colorScheme.primary
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
                StatusItem("GSR", "Connected", connectedColor)
                StatusItem("Thermal", "Ready", primaryColor)
                StatusItem("RGB", "Active", connectedColor)
            }
        }
    }
}

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