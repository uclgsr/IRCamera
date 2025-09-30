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
 * Recording Settings Screen - Configure multi-modal recording parameters
 */
@Composable
fun RecordingSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var autoRecording by remember { mutableStateOf(false) }
    var recordingQuality by remember { mutableStateOf("High") }
    var videoFrameRate by remember { mutableIntStateOf(30) }
    var audioEnabled by remember { mutableStateOf(true) }
    var simultaneousRecording by remember { mutableStateOf(true) }
    var timestampSync by remember { mutableStateOf(true) }

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
                    checked = autoRecording,
                    onCheckedChange = { autoRecording = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsDropdown(
                    label = "Recording Quality",
                    value = recordingQuality,
                    options = listOf("Low", "Medium", "High", "Ultra"),
                    onValueChange = { recordingQuality = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSlider(
                    label = "Video Frame Rate",
                    value = videoFrameRate.toFloat(),
                    valueRange = 15f..60f,
                    onValueChange = { videoFrameRate = it.toInt() },
                    unit = "fps"
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
                    checked = audioEnabled,
                    onCheckedChange = { audioEnabled = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggle(
                    label = "Simultaneous Recording",
                    description = "Record all sensors at the same time",
                    checked = simultaneousRecording,
                    onCheckedChange = { simultaneousRecording = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggle(
                    label = "Timestamp Synchronization",
                    description = "Synchronize timestamps across all recordings",
                    checked = timestampSync,
                    onCheckedChange = { timestampSync = it }
                )
            }

            // Recording Format Card
            SettingsCard(
                title = "Recording Format",
                icon = Icons.Default.VideoLibrary
            ) {
                SettingsRow(
                    label = "Video Format",
                    value = "MP4 (H.264)"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Audio Format",
                    value = "AAC"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(
                    label = "Sensor Data Format",
                    value = "CSV"
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
