package mpdc4gsr.feature.network.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.SettingsCard
import mpdc4gsr.core.ui.components.settings.SettingsRow
import mpdc4gsr.core.ui.components.settings.SettingsToggle
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.settings.presentation.NetworkSettingsViewModel
import mpdc4gsr.feature.settings.presentation.NetworkSettingsViewModelFactory

@Composable
fun NetworkSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: NetworkSettingsViewModel = viewModel(
        factory = NetworkSettingsViewModelFactory(
            LocalContext.current.applicationContext
        )
    ),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.networkSettings.collectAsState()
    val networkInfo by viewModel.networkInfo.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TitleBar(
            title = "Network Settings",
            showBackButton = true,
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // WiFi Settings
            SettingsCard(
                title = "WiFi",
                icon = Icons.Default.Wifi
            ) {
                SettingsToggle(
                    label = "WiFi",
                    description = "Enable WiFi connectivity",
                    checked = settings.wifiEnabled,
                    onCheckedChange = { viewModel.refreshWifiInfo() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Network",
                    value = networkInfo.wifiNetwork
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "IP Address",
                    value = networkInfo.ipAddress
                )
            }
            // Bluetooth Settings
            SettingsCard(
                title = "Bluetooth",
                icon = Icons.Default.Bluetooth
            ) {
                SettingsToggle(
                    label = "Bluetooth",
                    description = "Enable Bluetooth connectivity",
                    checked = settings.bluetoothEnabled,
                    onCheckedChange = { viewModel.refreshBluetoothInfo() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Auto Connect",
                    description = "Automatically connect to known devices",
                    checked = settings.autoConnect,
                    onCheckedChange = { viewModel.updateAutoConnect(it) }
                )
            }
            // Paired Devices
            SettingsCard(
                title = "Paired Devices",
                icon = Icons.Default.Devices
            ) {
                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "No paired devices found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    pairedDevices.forEach { device ->
                        SettingsRow(
                            label = device.name,
                            value = if (device.isConnected) "Connected" else "Disconnected"
                        )
                        if (device != pairedDevices.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.scanForDevices() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan for Devices")
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun NetworkSettingsScreenPreview() {
    IRCameraTheme {
        NetworkSettingsScreen()
    }
}
