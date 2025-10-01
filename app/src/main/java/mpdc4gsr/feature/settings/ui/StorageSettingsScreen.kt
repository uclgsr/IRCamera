package mpdc4gsr.feature.settings.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.*
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.viewmodel.StorageSettingsViewModel

/**
 * Storage Settings Screen - Configure data storage and export options
 * Integrated with StorageSettingsViewModel for MVVM architecture
 */
@Composable
fun StorageSettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: StorageSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.storageSettings.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

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
                    value = settings.storageLocation,
                    options = listOf("Internal Storage", "SD Card", "External USB"),
                    onValueChange = { viewModel.updateStorageLocation(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsRow(
                    label = "Available Space",
                    value = storageInfo.availableSpace
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsRow(
                    label = "Used Space",
                    value = storageInfo.usedSpace
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
                    checked = settings.autoExport,
                    onCheckedChange = { viewModel.updateAutoExport(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsDropdown(
                    label = "Export Format",
                    value = settings.exportFormat,
                    options = listOf("CSV", "JSON", "XML", "MATLAB"),
                    onValueChange = { viewModel.updateExportFormat(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggle(
                    label = "Compression",
                    description = "Compress exported files to save space",
                    checked = settings.compressionEnabled,
                    onCheckedChange = { viewModel.updateCompression(it) }
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
                    checked = settings.autoBackup,
                    onCheckedChange = { viewModel.updateAutoBackup(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggle(
                    label = "Delete After Export",
                    description = "Delete local data after successful export",
                    checked = settings.deleteAfterExport,
                    onCheckedChange = { viewModel.updateDeleteAfterExport(it) }
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
