package mpdc4gsr.feature.main.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mpdc4gsr.core.ui.components.common.TitleBar
import mpdc4gsr.core.ui.components.common.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.main.domain.model.SensorState
import mpdc4gsr.feature.main.domain.model.SensorStatus
import mpdc4gsr.feature.main.presentation.MainActivityViewModel
import mpdc4gsr.feature.main.presentation.MainUiAction
import mpdc4gsr.feature.main.presentation.MainUiState
import mpdc4gsr.feature.main.presentation.SensorOverviewState
import mpdc4gsr.core.domain.model.UiEvent

@Composable
fun MainRoute(
    onNavigateToSensors: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSensor: (mpdc4gsr.core.ui.model.SensorType) -> Unit = {},
    onExitRequested: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                UiEvent.ShowExitDialog -> onExitRequested()
                is UiEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        event.message,
                        if (event.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    MainScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onNavigateToSensors = onNavigateToSensors,
        onNavigateToGallery = onNavigateToGallery,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToSensor = onNavigateToSensor,
        modifier = modifier
    )
}

@Composable
fun MainScreen(
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onNavigateToSensors: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSensor: (mpdc4gsr.core.ui.model.SensorType) -> Unit = {},
    modifier: Modifier = Modifier
) {
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
            when (state.currentPage) {
                MainUiState.PAGE_GALLERY -> GalleryTab(onNavigateToGallery = onNavigateToGallery)
                MainUiState.PAGE_PROFILE -> ProfileTab(onNavigateToProfile = onNavigateToProfile)
                else -> SensorDashboardTab(
                    state = state,
                    onAction = onAction,
                    onSensorClick = onNavigateToSensor
                )
            }
        }
        // Bottom navigation
        NavigationBar(
            containerColor = Color(0xFF2A2A2A)
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Sensors") },
                label = { Text("Sensors") },
                selected = state.currentPage == MainUiState.PAGE_MAIN,
                onClick = { onAction(MainUiAction.SelectPage(MainUiState.PAGE_MAIN)) },
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
                selected = state.currentPage == MainUiState.PAGE_GALLERY,
                onClick = { onAction(MainUiAction.SelectPage(MainUiState.PAGE_GALLERY)) },
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
                onClick = {
                    onAction(MainUiAction.SelectPage(MainUiState.PAGE_SETTINGS))
                    onNavigateToSettings()
                },
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
                selected = state.currentPage == MainUiState.PAGE_PROFILE,
                onClick = { onAction(MainUiAction.SelectPage(MainUiState.PAGE_PROFILE)) },
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
    state: MainUiState,
    onAction: (MainUiAction) -> Unit,
    onSensorClick: (mpdc4gsr.core.ui.model.SensorType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gsrSensorUiState = remember(state.sensorOverview.gsr.status) {
        mapSensorStatusToUiState(state.sensorOverview.gsr.status)
    }
    val thermalSensorUiState = remember(state.sensorOverview.thermal.status) {
        mapSensorStatusToUiState(state.sensorOverview.thermal.status)
    }
    val rgbSensorUiState = remember(state.sensorOverview.rgb.status) {
        mapSensorStatusToUiState(state.sensorOverview.rgb.status)
    }

    val gsrActionHandler = remember(onAction) {
        { action: mpdc4gsr.core.ui.model.GSRAction ->
            onAction(MainUiAction.PerformGsrAction(action))
        }
    }
    val thermalActionHandler = remember(onAction) {
        { action: mpdc4gsr.core.ui.model.ThermalAction ->
            onAction(MainUiAction.PerformThermalAction(action))
        }
    }
    val rgbActionHandler = remember(onAction) {
        { action: mpdc4gsr.core.ui.model.CameraAction ->
            onAction(MainUiAction.PerformCameraAction(action))
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
        SystemStatusOverview(sensorOverview = state.sensorOverview)
        // Sensor cards for direct access
        // Using memoized handlers to prevent unnecessary recomposition
        mpdc4gsr.core.ui.components.sensors.GSRSensorCard(
            state = gsrSensorUiState,
            onStateChange = {},
            onClick = { onSensorClick(mpdc4gsr.core.ui.model.SensorType.GSR) },
            onAction = gsrActionHandler
        )
        mpdc4gsr.core.ui.components.sensors.ThermalSensorCard(
            state = thermalSensorUiState,
            onStateChange = {},
            onClick = { onSensorClick(mpdc4gsr.core.ui.model.SensorType.ThermalIR) },
            onAction = thermalActionHandler
        )
        mpdc4gsr.core.ui.components.sensors.RGBCameraSensorCard(
            state = rgbSensorUiState,
            onStateChange = {},
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
    sensorOverview: SensorOverviewState,
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
                StatusItem("GSR", sensorOverview.gsr)
                StatusItem("Thermal", sensorOverview.thermal)
                StatusItem("RGB", sensorOverview.rgb)
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    sensorState: SensorState,
    modifier: Modifier = Modifier
) {
    val indicatorColor = sensorState.status.indicatorColor()
    val subtitle = sensorState.message ?: sensorState.status.displayLabel()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = indicatorColor
        ) {}
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

private fun mapSensorStatusToUiState(status: SensorStatus): mpdc4gsr.core.ui.model.SensorState =
    when (status) {
        SensorStatus.DISCONNECTED -> mpdc4gsr.core.ui.model.SensorState.Disconnected
        SensorStatus.CONNECTING -> mpdc4gsr.core.ui.model.SensorState.Connecting
        SensorStatus.CONNECTED -> mpdc4gsr.core.ui.model.SensorState.Connected
        SensorStatus.STREAMING -> mpdc4gsr.core.ui.model.SensorState.Streaming
        SensorStatus.ERROR -> mpdc4gsr.core.ui.model.SensorState.Error
        SensorStatus.SIMULATION -> mpdc4gsr.core.ui.model.SensorState.Simulation
    }

private fun SensorStatus.displayLabel(): String = when (this) {
    SensorStatus.DISCONNECTED -> "Disconnected"
    SensorStatus.CONNECTING -> "Connecting"
    SensorStatus.CONNECTED -> "Ready"
    SensorStatus.STREAMING -> "Streaming"
    SensorStatus.ERROR -> "Error"
    SensorStatus.SIMULATION -> "Simulation"
}

private fun SensorStatus.indicatorColor(): Color = when (this) {
    SensorStatus.DISCONNECTED -> Color.Gray
    SensorStatus.CONNECTING -> Color(0xFFFFA000) // Amber
    SensorStatus.CONNECTED -> Color(0xFF4CAF50) // Green
    SensorStatus.STREAMING -> Color(0xFF00BCD4) // Teal
    SensorStatus.ERROR -> Color(0xFFF44336) // Red
    SensorStatus.SIMULATION -> Color(0xFF9C27B0) // Purple
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    IRCameraTheme {
        MainScreen(
            state = MainUiState(),
            onAction = {}
        )
    }
}
