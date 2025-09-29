package com.mpdc4gsr.module.user.fragment

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.lms.feedback.activity.FeedbackActivity
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.module.user.R
import com.mpdc4gsr.module.user.viewmodel.MineViewModel

/**
 * Compose migration of MineFragment
 *
 * This fragment demonstrates:
 * - Complete migration of user profile UI to Compose
 * - Modern Material 3 design with user-friendly layout
 * - Enhanced settings and preferences management
 * - Integrated user info management
 * - Improved navigation and accessibility
 */
class MineFragmentCompose : BaseComposeFragment<MineViewModel>() {

    override fun createViewModel(): MineViewModel {
        return viewModels<MineViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MineViewModel) {
        val context = LocalContext.current

        // Observe ViewModel state
        val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
        val appInfo by viewModel.appInfo.collectAsStateWithLifecycle()
        val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()

        LibUnifiedTheme {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User profile section
                item {
                    UserProfileCard(
                        userInfo = userInfo,
                        onEditProfile = { viewModel.editUserProfile() },
                        onAvatarClick = { viewModel.changeAvatar() }
                    )
                }

                // Device information section
                item {
                    DeviceInfoCard(
                        deviceInfo = deviceInfo,
                        onDeviceSettings = { viewModel.openDeviceSettings() }
                    )
                }

                // Settings sections
                item {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Settings options
                val settingsItems = getSettingsItems()
                items(settingsItems) { settingItem ->
                    SettingsCard(
                        item = settingItem,
                        onClick = {
                            handleSettingsClick(context, settingItem, viewModel)
                        }
                    )
                }

                // App information section
                item {
                    AppInfoCard(
                        appInfo = appInfo,
                        onViewLogs = { viewModel.viewAppLogs() },
                        onClearCache = { viewModel.clearAppCache() },
                        onCheckUpdates = { viewModel.checkForUpdates() }
                    )
                }
            }
        }
    }

    @Composable
    private fun UserProfileCard(
        userInfo: MineViewModel.UserInfo?,
        onEditProfile: () -> Unit,
        onAvatarClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Card(
                    onClick = onAvatarClick,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape
                ) {
                    if (userInfo?.avatarUrl?.isNotEmpty() == true) {
                        // TODO: Replace with AsyncImage when coil dependency is available
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // User info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userInfo?.name ?: "User",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userInfo?.email ?: "user@example.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (userInfo?.isLoggedIn == true) {
                        Text(
                            text = "Logged In",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Edit button
                IconButton(onClick = onEditProfile) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Profile"
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceInfoCard(
        deviceInfo: MineViewModel.DeviceInfo?,
        onDeviceSettings: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connected Devices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDeviceSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Device Settings"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Device status indicators
                deviceInfo?.let { info ->
                    DeviceStatusIndicator(
                        deviceName = "TC Line Device",
                        isConnected = info.hasLineConnection,
                        batteryLevel = null
                    )

                    if (info.hasTC007) {
                        DeviceStatusIndicator(
                            deviceName = "TC007",
                            isConnected = info.hasTC007Connection,
                            batteryLevel = info.tc007Battery
                        )
                    }

                    if (info.hasTS004) {
                        DeviceStatusIndicator(
                            deviceName = "TS004",
                            isConnected = info.hasTS004Connection,
                            batteryLevel = null
                        )
                    }
                } ?: run {
                    Text(
                        text = "No devices connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun DeviceStatusIndicator(
        deviceName: String,
        isConnected: Boolean,
        batteryLevel: Int?
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected)
                            androidx.compose.ui.graphics.Color.Green
                        else
                            androidx.compose.ui.graphics.Color.Gray
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            // Battery level if available
            batteryLevel?.let { level ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BatteryStd,
                        contentDescription = "Battery",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$level%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsCard(
        item: SettingsItem,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp),
                    tint = item.iconTint
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.subtitle.isNotEmpty()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun AppInfoCard(
        appInfo: MineViewModel.AppInfo?,
        onViewLogs: () -> Unit,
        onClearCache: () -> Unit,
        onCheckUpdates: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                appInfo?.let { info ->
                    AppInfoRow("Version", info.version)
                    AppInfoRow("Build", info.buildNumber)
                    AppInfoRow("Cache Size", info.cacheSize)
                    AppInfoRow("Last Updated", info.lastUpdated)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewLogs,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Logs")
                        }

                        OutlinedButton(
                            onClick = onClearCache,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Cache")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onCheckUpdates,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Check for Updates")
                    }
                }
            }
        }
    }

    @Composable
    private fun AppInfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }

    private fun getSettingsItems(): List<SettingsItem> {
        return listOf(
            SettingsItem(
                id = "general",
                title = "General Settings",
                subtitle = "Language, theme, notifications",
                icon = Icons.Default.Settings,
                iconTint = androidx.compose.ui.graphics.Color.Blue
            ),
            SettingsItem(
                id = "thermal",
                title = "Thermal Settings",
                subtitle = "Camera calibration, temperature units",
                icon = Icons.Default.Thermostat,
                iconTint = androidx.compose.ui.graphics.Color.Red
            ),
            SettingsItem(
                id = "network",
                title = "Network Settings",
                subtitle = "WiFi, connection preferences",
                icon = Icons.Default.NetworkWifi,
                iconTint = androidx.compose.ui.graphics.Color.Green
            ),
            SettingsItem(
                id = "storage",
                title = "Storage Management",
                subtitle = "File locations, auto-cleanup",
                icon = Icons.Default.Storage,
                iconTint = androidx.compose.ui.graphics.Color.Blue
            ),
            SettingsItem(
                id = "feedback",
                title = "Feedback & Support",
                subtitle = "Send feedback, get help",
                icon = Icons.Default.Feedback,
                iconTint = androidx.compose.ui.graphics.Color.Red
            ),
            SettingsItem(
                id = "about",
                title = "About",
                subtitle = "Version info, licenses",
                icon = Icons.Default.Info,
                iconTint = androidx.compose.ui.graphics.Color.Gray
            )
        )
    }

    private fun handleSettingsClick(
        context: android.content.Context,
        item: SettingsItem,
        viewModel: MineViewModel
    ) {
        when (item.id) {
            "general" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.GENERAL_SETTINGS)
                    .navigation(context)
            }

            "thermal" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.THERMAL_SETTINGS)
                    .navigation(context)
            }

            "network" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.NETWORK_SETTINGS)
                    .navigation(context)
            }

            "storage" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.STORAGE_SETTINGS)
                    .navigation(context)
            }

            "feedback" -> {
                val intent = Intent(context, FeedbackActivity::class.java)
                context.startActivity(intent)
            }

            "about" -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.ABOUT)
                    .navigation(context)
            }
        }
    }

    data class SettingsItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val icon: ImageVector,
        val iconTint: androidx.compose.ui.graphics.Color
    )
}