package mpdc4gsr.activities

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme

/**
 * Compose version of a Settings Activity demonstrating configuration and preferences UI.
 * Shows how to handle various settings categories and options in Compose.
 */
class SettingsComposeActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        IRCameraTheme {
            SettingsScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SettingsScreen() {
        var notificationsEnabled by remember { mutableStateOf(true) }
        var autoSyncEnabled by remember { mutableStateOf(false) }
        var highQualityRecording by remember { mutableStateOf(true) }
        var selectedLanguage by remember { mutableStateOf("English") }
        var selectedTheme by remember { mutableStateOf("Dark") }
        var cacheSize by remember { mutableStateOf("2.3 MB") }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Settings",
                    onNavigationClick = { finish() }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Recording Settings Section
                item {
                    SettingsSectionHeader("Recording Settings")
                }

                item {
                    SettingsToggleItem(
                        icon = Icons.Default.HighQuality,
                        title = "High Quality Recording",
                        subtitle = "Use maximum resolution and frame rate",
                        checked = highQualityRecording,
                        onCheckedChange = { highQualityRecording = it }
                    )
                }

                item {
                    SettingsToggleItem(
                        icon = Icons.Default.Sync,
                        title = "Auto Sync",
                        subtitle = "Automatically sync recordings to cloud",
                        checked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it }
                    )
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Storage,
                        title = "Storage Location",
                        subtitle = "Internal Storage > IRCamera",
                        onClick = { showStorageOptions() }
                    )
                }

                // Application Settings Section
                item {
                    SettingsSectionHeader("Application Settings")
                }

                item {
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Show recording status and alerts",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = selectedLanguage,
                        onClick = { showLanguageOptions { selectedLanguage = it } }
                    )
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = selectedTheme,
                        onClick = { showThemeOptions { selectedTheme = it } }
                    )
                }

                // Device Settings Section
                item {
                    SettingsSectionHeader("Device Settings")
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Bluetooth,
                        title = "Bluetooth Devices",
                        subtitle = "Manage paired devices",
                        onClick = { showBluetoothSettings() }
                    )
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Wifi,
                        title = "Network Settings",
                        subtitle = "Configure WiFi and connections",
                        onClick = { showNetworkSettings() }
                    )
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Camera,
                        title = "Camera Settings",
                        subtitle = "Configure camera preferences",
                        onClick = { showCameraSettings() }
                    )
                }

                // Data & Privacy Section
                item {
                    SettingsSectionHeader("Data & Privacy")
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Delete,
                        title = "Clear Cache",
                        subtitle = "Cache size: $cacheSize",
                        onClick = { 
                            clearCache { newSize ->
                                cacheSize = newSize
                            }
                        }
                    )
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        subtitle = "View privacy policy and terms",
                        onClick = { showPrivacyPolicy() }
                    )
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.ImportExport,
                        title = "Export Data",
                        subtitle = "Export recordings and settings",
                        onClick = { showExportOptions() }
                    )
                }

                // About Section
                item {
                    SettingsSectionHeader("About")
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "Version 1.0.0 (Build 123)",
                        onClick = { showVersionInfo() }
                    )
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Help,
                        title = "Help & Support",
                        subtitle = "Get help and contact support",
                        onClick = { showHelpSupport() }
                    )
                }

                item {
                    SettingsClickableItem(
                        icon = Icons.Default.Update,
                        title = "Check for Updates",
                        subtitle = "Check for app updates",
                        onClick = { checkForUpdates() }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun SettingsSectionHeader(title: String) {
        Text(
            text = title,
            color = Color(0xFF6B35FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )
    }

    @Composable
    private fun SettingsToggleItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        CommonComponents.IRCameraCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF6B35FF),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        color = Color(0x80FFFFFF),
                        fontSize = 14.sp
                    )
                }

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6B35FF),
                        checkedTrackColor = Color(0x556B35FF)
                    )
                )
            }
        }
    }

    @Composable
    private fun SettingsClickableItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ) {
        CommonComponents.IRCameraCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF6B35FF),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        color = Color(0x80FFFFFF),
                        fontSize = 14.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = Color(0x80FFFFFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Settings action methods
    private fun showStorageOptions() {
        Toast.makeText(this, "Storage options would open here", Toast.LENGTH_SHORT).show()
    }

    private fun showLanguageOptions(onLanguageSelected: (String) -> Unit) {
        // In a real app, this would show a language selection dialog
        val languages = listOf("English", "Spanish", "French", "German", "Chinese")
        onLanguageSelected(languages.random())
        Toast.makeText(this, "Language selection would open here", Toast.LENGTH_SHORT).show()
    }

    private fun showThemeOptions(onThemeSelected: (String) -> Unit) {
        val themes = listOf("Dark", "Light", "Auto")
        onThemeSelected(themes.random())
        Toast.makeText(this, "Theme selection would open here", Toast.LENGTH_SHORT).show()
    }

    private fun showBluetoothSettings() {
        Toast.makeText(this, "Bluetooth settings would open here", Toast.LENGTH_SHORT).show()
    }

    private fun showNetworkSettings() {
        Toast.makeText(this, "Network settings would open here", Toast.LENGTH_SHORT).show()
    }

    private fun showCameraSettings() {
        Toast.makeText(this, "Camera settings would open here", Toast.LENGTH_SHORT).show()
    }

    private fun clearCache(onCacheCleared: (String) -> Unit) {
        onCacheCleared("0 MB")
        Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showPrivacyPolicy() {
        Toast.makeText(this, "Privacy policy would open here", Toast.LENGTH_SHORT).show()
    }

    private fun showExportOptions() {
        Toast.makeText(this, "Export options would open here", Toast.LENGTH_SHORT).show()
    }

    private fun showVersionInfo() {
        Toast.makeText(this, "Version info would display here", Toast.LENGTH_SHORT).show()
    }

    private fun showHelpSupport() {
        Toast.makeText(this, "Help & support would open here", Toast.LENGTH_SHORT).show()
    }

    private fun checkForUpdates() {
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
    }
}