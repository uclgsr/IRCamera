package com.mpdc4gsr.module.user.fragment

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.lms.feedback.activity.FeedbackActivity
import com.mpdc4gsr.module.user.viewmodel.MineFragmentViewModel
import com.mpdc4gsr.module.user.viewmodel.MineViewModel

/**
 * Compose migration of MineFragment - Minimal working version
 */
class MineFragmentCompose : BaseComposeFragment<MineFragmentViewModel>() {

    data class SettingsItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val icon: ImageVector,
        val iconTint: Color
    )

    override fun createViewModel(): MineFragmentViewModel {
        return viewModels<MineFragmentViewModel>().value
    }

    @Composable
    override fun Content(viewModel: MineFragmentViewModel) {
        val context = LocalContext.current
        val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

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
                        userProfile = userProfile,
                        onEditProfile = { },
                        onAvatarClick = { }
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
                            when (settingItem.id) {
                                "general" -> {
                                    NavigationManager.getInstance()
                                        .build(RouterConfig.VERSION)
                                        .navigation(context)
                                }
                                "thermal" -> {
                                    NavigationManager.getInstance()
                                        .build(RouterConfig.IR_SETTING)
                                        .navigation(context)
                                }
                                "network" -> {
                                    NavigationManager.getInstance()
                                        .build(RouterConfig.VERSION)
                                        .navigation(context)
                                }
                                "storage" -> {
                                    NavigationManager.getInstance()
                                        .build(RouterConfig.VERSION)
                                        .navigation(context)
                                }
                                "feedback" -> {
                                    val intent = Intent(context, FeedbackActivity::class.java)
                                    context.startActivity(intent)
                                }
                                "about" -> {
                                    NavigationManager.getInstance()
                                        .build(RouterConfig.VERSION)
                                        .navigation(context)
                                }
                            }
                        }
                    )
                }

                // App information section
                item {
                    AppInfoCard(
                        onViewLogs = { },
                        onClearCache = { viewModel.clearCache() },
                        onCheckUpdates = { }
                    )
                }
            }
        }
    }

    @Composable
    private fun UserProfileCard(
        userProfile: MineFragmentViewModel.UserProfileState,
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
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (userProfile.isLoggedIn) Icons.Default.AccountCircle else Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // User info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userProfile.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (userProfile.isLoggedIn) "Logged In" else "Guest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = onEditProfile,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text("Edit")
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            onClick = onClick
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
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
        onViewLogs: () -> Unit,
        onClearCache: () -> Unit,
        onCheckUpdates: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                AppInfoRow("Version", "1.10.000")
                AppInfoRow("Build", "1100")
                AppInfoRow("Cache Size", "0 MB")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewLogs,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Logs")
                    }
                    
                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Cache")
                    }
                    
                    Button(
                        onClick = onCheckUpdates,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update")
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
                iconTint = Color.Blue
            ),
            SettingsItem(
                id = "thermal",
                title = "Thermal Settings",
                subtitle = "Camera calibration, temperature units",
                icon = Icons.Default.Thermostat,
                iconTint = Color.Red
            ),
            SettingsItem(
                id = "network",
                title = "Network Settings",
                subtitle = "WiFi, connection preferences",
                icon = Icons.Default.NetworkWifi,
                iconTint = Color.Green
            ),
            SettingsItem(
                id = "storage",
                title = "Storage Management",
                subtitle = "File locations, auto-cleanup",
                icon = Icons.Default.Storage,
                iconTint = Color.Blue
            ),
            SettingsItem(
                id = "feedback",
                title = "Feedback & Support",
                subtitle = "Send feedback, get help",
                icon = Icons.Default.Feedback,
                iconTint = Color.Red
            ),
            SettingsItem(
                id = "about",
                title = "About",
                subtitle = "Version info, licenses",
                icon = Icons.Default.Info,
                iconTint = Color.Gray
            )
        )
    }
}
