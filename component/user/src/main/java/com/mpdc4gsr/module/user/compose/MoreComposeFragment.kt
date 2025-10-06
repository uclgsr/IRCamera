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
