// Merged ALL .kt and .java files from the 'component\user\src\main\java\com\mpdc4gsr\module\user\compose' directory and its subdirectories.
// Total files: 5 | Generated on: 2025-10-08 01:42:36


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\compose\DownloadProgressDialog.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.DecimalFormat
import com.mpdc4gsr.libunified.R as RCore

@Composable
fun DownloadProgressDialog(
    isVisible: Boolean,
    currentBytes: Long,
    totalBytes: Long,
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = stringResource(RCore.string.ts004_download_doing),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    // File size info
                    val sizeText =
                        "${stringResource(RCore.string.detail_len)}: ${formatFileSize(currentBytes)}/${
                            formatFileSize(totalBytes)
                        }"
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    // Progress bar
                    val progress =
                        if (totalBytes > 0) (currentBytes.toFloat() / totalBytes.toFloat()) else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "${size}B"
        size < 1024 * 1024 -> DecimalFormat("#.0").format(size.toDouble() / 1024) + "KB"
        size < 1024 * 1024 * 1024 -> DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024) + "MB"
        else -> DecimalFormat("#.0").format(size.toDouble() / 1024 / 1024 / 1024) + "GB"
    }
}


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\compose\FirmwareInstallDialog.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FirmwareInstallDialog(
    isVisible: Boolean,
    message: String = "Installing firmware...",
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .size(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\compose\ListItemComponent.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ListItemComponent(
    leftText: String,
    modifier: Modifier = Modifier,
    leftIconRes: Int? = null,
    leftIcon: ImageVector? = null,
    rightText: String? = null,
    showLine: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        val content: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left icon
                    when {
                        leftIcon != null -> {
                            Icon(
                                imageVector = leftIcon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        leftIconRes != null -> {
                            Icon(
                                painter = painterResource(id = leftIconRes),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                    // Left text
                    Text(
                        text = leftText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Right text
                rightText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        if (onClick != null) {
            Card(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                content()
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                content()
            }
        }
        // Line separator
        if (showLine) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\compose\MineComposeFragment.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.utils.Constants
import com.mpdc4gsr.module.user.viewmodel.MineFragmentViewModel
import com.mpdc4gsr.libunified.R as RCore

@Composable
fun MineComposeFragment(
    viewModel: MineFragmentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val showWinterPoint by viewModel.showWinterPoint.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "User Avatar",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                // User Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userProfile.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (userProfile.isLoggedIn) "Logged In" else "Guest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                // Winter Easter Egg
                if (showWinterPoint) {
                    IconButton(
                        onClick = { viewModel.onWinterEggClick() }
                    ) {
                        Icon(
                            Icons.Default.AcUnit,
                            contentDescription = "Winter",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        // Settings Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_version),
                    leftIcon = Icons.Default.Info,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.VERSION)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.set_clear_cache),
                    leftIcon = Icons.Default.Delete,
                    showLine = true,
                    onClick = { viewModel.clearCache() }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.electronic_manual),
                    leftIcon = Icons.Default.Book,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.ELECTRONIC_MANUAL)
                            .withInt(Constants.SETTING_TYPE, Constants.SETTING_BOOK)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.app_question),
                    leftIcon = Icons.AutoMirrored.Filled.Help,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.ELECTRONIC_MANUAL)
                            .withInt(Constants.SETTING_TYPE, Constants.SETTING_FAQ)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_feedback),
                    leftIcon = Icons.Default.Feedback,
                    showLine = true,
                    onClick = {
                        // Feedback navigation
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_unit),
                    leftIcon = Icons.Default.Settings,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.UNIT)
                            .navigation(context)
                    }
                )
            }
        }
    }
}


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\compose\MoreComposeFragment.kt =====

package com.mpdc4gsr.module.user.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.user.viewmodel.MoreComposeFragmentViewModel
import com.mpdc4gsr.libunified.R as RCore

@Composable
fun MoreComposeFragment(
    viewModel: MoreComposeFragmentViewModel,
    isTC007: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceSettings by viewModel.deviceSettings.collectAsState()
    // Initialize ViewModel with device type
    LaunchedEffect(isTC007) {
        viewModel.initialize(isTC007)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Device Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Save Setting Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Save Settings",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatically save device configuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = deviceSettings.isSaveSettingEnabled,
                        onCheckedChange = { viewModel.updateSaveSetting(it) }
                    )
                }
            }
        }
        // Settings Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                ListItemComponent(
                    leftText = "Model Settings",
                    leftIcon = Icons.Default.Settings,
                    showLine = true,
                    onClick = {
                        // Model settings navigation
                    }
                )
                ListItemComponent(
                    leftText = "Correction Settings",
                    leftIcon = Icons.Default.Tune,
                    showLine = true,
                    onClick = {
                        // Correction settings navigation
                    }
                )
                ListItemComponent(
                    leftText = "Dual Mode",
                    leftIcon = Icons.Default.Apps,
                    showLine = true,
                    onClick = {
                        // Dual mode navigation
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_unit),
                    leftIcon = Icons.Default.Speed,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.UNIT)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = stringResource(RCore.string.setting_version),
                    leftIcon = Icons.Default.Info,
                    rightText = if (deviceSettings.hasUpgrade) "Update Available" else deviceSettings.versionText,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.VERSION)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = "Device Information",
                    leftIcon = Icons.Default.Devices,
                    showLine = true,
                    onClick = {
                        NavigationManager.getInstance()
                            .build(RouterConfig.DEVICE_INFORMATION)
                            .navigation(context)
                    }
                )
                ListItemComponent(
                    leftText = "Factory Reset",
                    leftIcon = Icons.Default.Refresh,
                    onClick = { viewModel.performFactoryReset() }
                )
            }
        }
    }
}