package mpdc4gsr.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.components.*
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Network Settings Screen - Device pairing and network configuration
 */
@Composable
fun NetworkSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var wifiEnabled by remember { mutableStateOf(true) }
    var bluetoothEnabled by remember { mutableStateOf(true) }
    var autoConnect by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Network Settings",
            showBackClick = true,
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
                    checked = wifiEnabled,
                    onCheckedChange = { wifiEnabled = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Network",
                    value = "UCL-WiFi"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "IP Address",
                    value = "192.168.1.100"
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
                    checked = bluetoothEnabled,
                    onCheckedChange = { bluetoothEnabled = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Auto Connect",
                    description = "Automatically connect to known devices",
                    checked = autoConnect,
                    onCheckedChange = { autoConnect = it }
                )
            }

            // Paired Devices
            SettingsCard(
                title = "Paired Devices",
                icon = Icons.Default.Devices
            ) {
                SettingsRow(
                    label = "Shimmer3 GSR",
                    value = "Connected"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "TOPDON TC001",
                    value = "Disconnected"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { /* Scan for devices */ },
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
