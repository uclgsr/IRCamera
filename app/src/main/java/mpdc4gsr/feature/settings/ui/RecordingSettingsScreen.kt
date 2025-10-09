package mpdc4gsr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.common.TitleBar
import mpdc4gsr.core.ui.components.settings.*
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.settings.presentation.RecordingSettingsViewModel

@Composable
fun RecordingSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: RecordingSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Recording Settings",
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
            // Recording Preferences Card
            SettingsCard(
                title = "Recording Preferences",
                icon = Icons.Default.VideoCall
            ) {
                SettingsToggle(
                    label = "Auto Recording",
                    description = "Start recording automatically when all devices are connected",
                    checked = settings.autoRecording,
                    onCheckedChange = { viewModel.updateAutoRecording(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsDropdown(
                    label = "Recording Quality",
                    value = settings.recordingQuality.displayName,
                    options = uiState.qualityOptions.map { it.displayName },
                    onValueChange = viewModel::updateRecordingQuality
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSlider(
                    label = "Video Frame Rate",
                    value = settings.videoFrameRate.toFloat(),
                    valueRange = uiState.frameRateRange,
                    onValueChange = { viewModel.updateVideoFrameRate(it.toInt()) },
                    unit = " fps"
                )
            }
            // Multi-Modal Recording Card
            SettingsCard(
                title = "Multi-Modal Recording",
                icon = Icons.Default.Sync
            ) {
                SettingsToggle(
                    label = "Audio Recording",
                    description = "Record audio along with video and sensor data",
                    checked = settings.audioEnabled,
                    onCheckedChange = { viewModel.updateAudioEnabled(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Simultaneous Recording",
                    description = "Record all sensors at the same time",
                    checked = settings.simultaneousRecording,
                    onCheckedChange = { viewModel.updateSimultaneousRecording(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggle(
                    label = "Timestamp Synchronization",
                    description = "Synchronize timestamps across all recordings",
                    checked = settings.timestampSync,
                    onCheckedChange = { viewModel.updateTimestampSync(it) }
                )
            }
            // Recording Format Card
            SettingsCard(
                title = "Recording Format",
                icon = Icons.Default.VideoLibrary
            ) {
                SettingsRow(
                    label = "Video Format",
                    value = settings.videoFormat
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Audio Format",
                    value = settings.audioFormat
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Sensor Data Format",
                    value = settings.sensorDataFormat
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordingSettingsScreenPreview() {
    IRCameraTheme {
        RecordingSettingsScreen()
    }
}
