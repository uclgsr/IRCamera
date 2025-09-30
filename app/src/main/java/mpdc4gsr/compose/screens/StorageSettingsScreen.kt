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
 * Storage Settings Screen - Configure data storage and export options
 */
@Composable
fun StorageSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var autoExport by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("CSV") }
    var storageLocation by remember { mutableStateOf("Internal Storage") }
    var autoBackup by remember { mutableStateOf(false) }
    var compressionEnabled by remember { mutableStateOf(true) }
    var deleteAfterExport by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Storage Settings",
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
            // Storage Location Card
            SettingsCard(
                title = "Storage Location",
                icon = Icons.Default.Storage
            ) {
                SettingsDropdown(
                    label = "Default Storage",
                    value = storageLocation,
                    options = listOf("Internal Storage", "SD Card", "External USB"),
                    onValueChange = { storageLocation = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsRow(
                    label = "Available Space",
                    value = "45.2 GB"
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsRow(
                    label = "Used Space",
                    value = "12.8 GB"
                )
            }

            // Export Settings Card
            SettingsCard(
                title = "Export Settings",
                icon = Icons.Default.FileUpload
            ) {
                SettingsToggle(
                    label = "Auto Export",
                    description = "Automatically export data after recording",
                    checked = autoExport,
                    onCheckedChange = { autoExport = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsDropdown(
                    label = "Export Format",
                    value = exportFormat,
                    options = listOf("CSV", "JSON", "XML", "MATLAB"),
                    onValueChange = { exportFormat = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggle(
                    label = "Compression",
                    description = "Compress exported files to save space",
                    checked = compressionEnabled,
                    onCheckedChange = { compressionEnabled = it }
                )
            }

            // Backup Settings Card
            SettingsCard(
                title = "Backup Settings",
                icon = Icons.Default.Backup
            ) {
                SettingsToggle(
                    label = "Auto Backup",
                    description = "Automatically backup data to cloud storage",
                    checked = autoBackup,
                    onCheckedChange = { autoBackup = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggle(
                    label = "Delete After Export",
                    description = "Delete local data after successful export",
                    checked = deleteAfterExport,
                    onCheckedChange = { deleteAfterExport = it }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageSettingsScreenPreview() {
    IRCameraTheme {
        StorageSettingsScreen()
    }
}
