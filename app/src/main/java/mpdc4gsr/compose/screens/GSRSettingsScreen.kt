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
 * GSR Settings Screen - Configure GSR sensor parameters and Shimmer3 device
 */
@Composable
fun GSRSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var samplingRate by remember { mutableIntStateOf(128) }
    var autoConnect by remember { mutableStateOf(true) }
    var dataLogging by remember { mutableStateOf(true) }
    var batteryWarning by remember { mutableStateOf(false) }
    var deviceId by remember { mutableStateOf("Shimmer3_001") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "GSR Settings",
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
            // Device Configuration
            SettingsCard(
                title = "Device Configuration",
                icon = Icons.Default.DeviceHub
            ) {
                SettingsRow(
                    label = "Device ID",
                    value = deviceId
                )
                SettingsSlider(
                    label = "Sampling Rate",
                    value = samplingRate.toFloat(),
                    valueRange = 1f..512f,
                    onValueChange = { samplingRate = it.toInt() },
                    unit = "Hz"
                )
                SettingsToggle(
                    label = "Auto Connect",
                    description = "Automatically connect to known devices",
                    checked = autoConnect,
                    onCheckedChange = { autoConnect = it }
                )
            }
            
            // Data Collection
            SettingsCard(
                title = "Data Collection",
                icon = Icons.Default.DataUsage
            ) {
                SettingsToggle(
                    label = "Data Logging",
                    description = "Enable continuous data logging",
                    checked = dataLogging,
                    onCheckedChange = { dataLogging = it }
                )
                SettingsToggle(
                    label = "Battery Warnings",
                    description = "Show low battery notifications",
                    checked = batteryWarning,
                    onCheckedChange = { batteryWarning = it }
                )
            }
            
            // Calibration
            SettingsCard(
                title = "Calibration",
                icon = Icons.Default.Tune
            ) {
                Button(
                    onClick = { /* Start calibration */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Calibration")
                }
                Button(
                    onClick = { /* Reset to defaults */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Defaults")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GSRSettingsScreenPreview() {
    IRCameraTheme {
        GSRSettingsScreen()
    }
}