package mpdc4gsr.feature.settings.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.NavigationBreadcrumb
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

@Composable
fun SettingsScreen(
    onBackClick: (() -> Unit)? = null,
    onNavigateToGSRSettings: (() -> Unit)? = null,
    onNavigateToThermalSettings: (() -> Unit)? = null,
    onNavigateToCameraSettings: (() -> Unit)? = null,
    onNavigateToRecordingSettings: (() -> Unit)? = null,
    onNavigateToStorageSettings: (() -> Unit)? = null,
    onNavigateToSyncSettings: (() -> Unit)? = null,
    onNavigateToCalibration: (() -> Unit)? = null,
    onNavigateToNetworkSettings: (() -> Unit)? = null,
    onNavigateToDiagnostics: (() -> Unit)? = null,
    onNavigateToAppInfo: (() -> Unit)? = null,
    onNavigateToPrivacyPolicy: (() -> Unit)? = null,
    onNavigateToHelp: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        NavigationBreadcrumb(
            currentScreen = "Settings",
            previousScreen = "Home"
        )
        // Settings content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sensor Settings Section
            SettingsSection(
                title = "Sensor Configuration"
            ) {
                SettingsItem(
                    icon = Icons.Default.Sensors,
                    title = "GSR Sensor Settings",
                    subtitle = "Configure Shimmer3 device and sampling rate",
                    onClick = { onNavigateToGSRSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Thermostat,
                    title = "Thermal Camera Settings",
                    subtitle = "Temperature calibration and palette options",
                    onClick = { onNavigateToThermalSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Camera,
                    title = "RGB Camera Settings",
                    subtitle = "Resolution, frame rate, and quality settings",
                    onClick = { onNavigateToCameraSettings?.invoke() }
                )
            }
            // Recording Settings Section
            SettingsSection(
                title = "Recording & Data"
            ) {
                SettingsItem(
                    icon = Icons.Default.VideoCall,
                    title = "Recording Settings",
                    subtitle = "Multi-modal recording preferences",
                    onClick = { onNavigateToRecordingSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Data Storage",
                    subtitle = "Export location and file formats",
                    onClick = { onNavigateToStorageSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "Synchronization",
                    subtitle = "Time sync and data alignment settings",
                    onClick = { onNavigateToSyncSettings?.invoke() }
                )
            }
            // Application Settings Section
            SettingsSection(
                title = "Application"
            ) {
                var darkMode by remember { mutableStateOf(true) }
                var notifications by remember { mutableStateOf(true) }
                var autoConnect by remember { mutableStateOf(false) }
                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme interface",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Enable system notifications",
                    checked = notifications,
                    onCheckedChange = { notifications = it }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Bluetooth,
                    title = "Auto Connect",
                    subtitle = "Automatically connect to known devices",
                    checked = autoConnect,
                    onCheckedChange = { autoConnect = it }
                )
            }
            // Advanced Settings Section
            SettingsSection(
                title = "Advanced"
            ) {
                SettingsItem(
                    icon = Icons.Default.Tune,
                    title = "Calibration",
                    subtitle = "System calibration and alignment tools",
                    onClick = { onNavigateToCalibration?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.NetworkCheck,
                    title = "Network Settings",
                    subtitle = "Device pairing and network configuration",
                    onClick = { onNavigateToNetworkSettings?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Diagnostics",
                    subtitle = "System diagnostics and troubleshooting",
                    onClick = { onNavigateToDiagnostics?.invoke() }
                )
            }
            // About Section
            SettingsSection(
                title = "About"
            ) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "App Information",
                    subtitle = "Version 1.0.0 - Multi-Modal Sensor Platform",
                    onClick = { onNavigateToAppInfo?.invoke() }
                )
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Privacy Policy",
                    subtitle = "Data privacy and usage policy",
                    onClick = { onNavigateToPrivacyPolicy?.invoke() }
                )
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Help & Support",
                    subtitle = "User guide and technical support",
                    onClick = { onNavigateToHelp?.invoke() }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}
@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    IRCameraTheme {
        SettingsScreen()
    }
}