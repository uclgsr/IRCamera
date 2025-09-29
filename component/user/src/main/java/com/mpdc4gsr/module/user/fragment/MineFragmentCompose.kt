package com.mpdc4gsr.module.user.fragment

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.user.viewmodel.MineFragmentViewModel

/**
 * Compose migration of MineFragment - Minimal working version
 */
class MineFragmentCompose : BaseComposeFragment<MineFragmentViewModel>() {

    override fun createViewModel(): MineFragmentViewModel {
        return viewModels<MineFragmentViewModel>().value
    }

    @Composable
    override fun Content(viewModel: MineFragmentViewModel) {
        val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

        LibUnifiedTheme {
            Column(
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
                    modifier = Modifier.fillMaxWidth()
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
                            text = userProfile.username,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (userProfile.isLoggedIn) "Logged In" else "Guest",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Button(
                    onClick = { viewModel.refreshUserProfile() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh Profile")
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
