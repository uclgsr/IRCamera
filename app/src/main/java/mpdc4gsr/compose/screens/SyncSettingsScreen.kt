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
 * Synchronization Settings Screen - Configure time sync and data alignment
 */
@Composable
fun SyncSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var ntpSync by remember { mutableStateOf(true) }
    var manualTimeSync by remember { mutableStateOf(false) }
    var syncMethod by remember { mutableStateOf("NTP") }
    var syncInterval by remember { mutableIntStateOf(60) }
    var autoAlignment by remember { mutableStateOf(true) }
    var timestampCorrection by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Synchronization Settings",
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
            // Time Synchronization Card
            SettingsCard(
                title = "Time Synchronization",
                icon = Icons.Default.Schedule
            ) {
                SettingsToggle(
                    label = "NTP Synchronization",
                    description = "Sync time with network time protocol server",
                    checked = ntpSync,
                    onCheckedChange = { ntpSync = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsDropdown(
                    label = "Sync Method",
                    value = syncMethod,
                    options = listOf("NTP", "GPS", "Manual", "Device Clock"),
                    onValueChange = { syncMethod = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSlider(
                    label = "Sync Interval",
                    value = syncInterval.toFloat(),
                    valueRange = 10f..300f,
                    onValueChange = { syncInterval = it.toInt() },
                    unit = "sec"
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsRow(
                    label = "Last Sync",
                    value = "2 minutes ago"
                )
            }

            // Data Alignment Card
            SettingsCard(
                title = "Data Alignment",
                icon = Icons.Default.AlignHorizontalCenter
            ) {
                SettingsToggle(
                    label = "Auto Alignment",
                    description = "Automatically align data from multiple sensors",
                    checked = autoAlignment,
                    onCheckedChange = { autoAlignment = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggle(
                    label = "Timestamp Correction",
                    description = "Apply correction to align timestamps",
                    checked = timestampCorrection,
                    onCheckedChange = { timestampCorrection = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsRow(
                    label = "Time Offset",
                    value = "+0.023 ms"
                )
            }

            // Sensor Synchronization Card
            SettingsCard(
                title = "Sensor Synchronization",
                icon = Icons.Default.Sync
            ) {
                SettingsRow(
                    label = "GSR Sensor",
                    value = "Synced"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Thermal Camera",
                    value = "Synced"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "RGB Camera",
                    value = "Synced"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncSettingsScreenPreview() {
    IRCameraTheme {
        SyncSettingsScreen()
    }
}
