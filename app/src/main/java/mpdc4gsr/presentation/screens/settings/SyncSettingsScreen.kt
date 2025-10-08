package mpdc4gsr.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.settings.*
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.settings.presentation.SyncSettingsViewModel

@Composable
fun SyncSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: SyncSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.syncSettings.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
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
                    checked = settings.ntpSync,
                    onCheckedChange = { viewModel.updateNtpSync(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsDropdown(
                    label = "Sync Method",
                    value = settings.syncMethod,
                    options = listOf("NTP", "GPS", "Manual", "Device Clock"),
                    onValueChange = { viewModel.updateSyncMethod(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSlider(
                    label = "Sync Interval",
                    value = settings.syncInterval.toFloat(),
                    valueRange = 10f..300f,
                    onValueChange = { viewModel.updateSyncInterval(it.toInt()) },
                    unit = " sec"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Last Sync",
                    value = settings.lastSync
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
                    checked = settings.autoAlignment,
                    onCheckedChange = { viewModel.updateAutoAlignment(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Timestamp Correction",
                    description = "Apply correction to align timestamps",
                    checked = settings.timestampCorrection,
                    onCheckedChange = { viewModel.updateTimestampCorrection(it) }
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
