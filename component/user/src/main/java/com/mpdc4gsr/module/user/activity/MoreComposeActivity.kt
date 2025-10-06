package com.mpdc4gsr.module.user.activity
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.user.viewmodel.MoreViewModel
class MoreComposeActivity : BaseComposeActivity<MoreViewModel>() {
    override fun createViewModel(): MoreViewModel {
        return viewModels<MoreViewModel>().value
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MoreViewModel) {
        val settingsItems by viewModel.settingsItems.collectAsState()
        val isUpgradeAvailable by viewModel.isUpgradeAvailable.collectAsState()
        // Check for updates on start
        LaunchedEffect(Unit) {
            viewModel.checkForUpdates()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "More Settings",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(settingsItems) { item ->
                    SettingsMenuItem(
                        item = item,
                        hasUpgrade = isUpgradeAvailable && item.action == MoreViewModel.SettingsAction.VERSION,
                        onClick = { handleSettingsClick(item.action) }
                    )
                }
            }
        }
    }
    private fun handleSettingsClick(action: MoreViewModel.SettingsAction) {
        when (action) {
            MoreViewModel.SettingsAction.DEVICE_INFORMATION -> {
                // Navigate to device details - for now just finish, would need router setup
                finish()
            }
            MoreViewModel.SettingsAction.TISR -> {
                // Navigate to TISR Compose Activity (would need to be registered in router)
                // For now, use the original activity
                NavigationManager.getInstance()
                    .build(RouterConfig.TISR)
                    .navigation(this)
            }
            MoreViewModel.SettingsAction.STORAGE_SPACE -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.STORAGE_SPACE)
                    .navigation(this)
            }
            MoreViewModel.SettingsAction.AUTO_SAVE -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.AUTO_SAVE)
                    .navigation(this)
            }
            MoreViewModel.SettingsAction.UNIT -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.UNIT)
                    .navigation(this)
            }
            MoreViewModel.SettingsAction.VERSION -> {
                NavigationManager.getInstance()
                    .build(RouterConfig.VERSION)
                    .navigation(this)
            }
            MoreViewModel.SettingsAction.DISCONNECT -> {
                // Handle disconnect logic
                finish()
            }
            MoreViewModel.SettingsAction.RESET -> {
                // Handle reset confirmation dialog
                // Original implementation had complex reset logic
            }
        }
    }
}
@Composable
private fun SettingsMenuItem(
    item: MoreViewModel.SettingsItem,
    hasUpgrade: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
                Icon(
                    imageVector = getIconForAction(item.action),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasUpgrade) {
                    Badge(
                        modifier = Modifier.padding(end = 8.dp),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = "!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
@Composable
private fun getIconForAction(action: MoreViewModel.SettingsAction): ImageVector {
    return when (action) {
        MoreViewModel.SettingsAction.DEVICE_INFORMATION -> Icons.Default.Info
        MoreViewModel.SettingsAction.TISR -> Icons.Default.Settings
        MoreViewModel.SettingsAction.STORAGE_SPACE -> Icons.Default.Build
        MoreViewModel.SettingsAction.AUTO_SAVE -> Icons.Default.Add
        MoreViewModel.SettingsAction.UNIT -> Icons.Default.Settings
        MoreViewModel.SettingsAction.VERSION -> Icons.Default.Info
        MoreViewModel.SettingsAction.DISCONNECT -> Icons.Default.Close
        MoreViewModel.SettingsAction.RESET -> Icons.Default.Refresh
    }
}