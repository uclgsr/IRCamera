package com.mpdc4gsr.module.user.activity

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.component.shared.app.permissions.FeaturePermissionArea
import com.mpdc4gsr.component.shared.app.compose.theme.Spacing
import com.mpdc4gsr.module.user.viewmodel.StorageSpaceViewModel
import com.mpdc4gsr.component.shared.R as RCore

class StorageSpaceComposeActivity : BaseComposeActivity<StorageSpaceViewModel>() {
    override val requiredPermissionAreas: Set<FeaturePermissionArea> = emptySet()

    override fun createViewModel(): StorageSpaceViewModel = viewModels<StorageSpaceViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: StorageSpaceViewModel) {
        val storageInfo by viewModel.storageInfo.collectAsState()
        val usagePercentage = viewModel.getUsagePercentage()
        // Load storage info on start
        LaunchedEffect(Unit) {
            viewModel.loadStorageInfo()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(RCore.string.ts004_storage_space),
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(Spacing.normal)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.normal),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.medium),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall),
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.normal),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        Text(
                            text = stringResource(RCore.string.ts004_storage_space),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Storage Usage",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LinearProgressIndicator(
                            progress = { usagePercentage },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(Spacing.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Used: ${viewModel.formatFileSize(storageInfo.usedSpace)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            Text(
                                text = "Total: ${viewModel.formatFileSize(storageInfo.totalSpace)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.medium),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall),
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.normal),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        Text(
                            text = "Storage Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        StorageItem(
                            icon = Icons.Default.Add,
                            title = "Photos",
                            size = viewModel.formatFileSize(storageInfo.photoSpace),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        StorageItem(
                            icon = Icons.Default.Info,
                            title = "Videos",
                            size = viewModel.formatFileSize(storageInfo.videoSpace),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        StorageItem(
                            icon = Icons.Default.Settings,
                            title = "System",
                            size = viewModel.formatFileSize(storageInfo.systemSpace),
                            color = MaterialTheme.colorScheme.outline,
                        )
                        StorageItem(
                            icon = Icons.Default.Build,
                            title = "Free Space",
                            size = viewModel.formatFileSize(storageInfo.freeSpace),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.medium),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Spacing.extraSmall),
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.normal),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "Format Storage",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = "This will delete all data and free up space",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = Spacing.extraSmall),
                                )
                            }
                            Button(
                                onClick = { viewModel.formatStorage() },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                    ),
                                modifier = Modifier.heightIn(min = Spacing.touchTarget),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(Spacing.large),
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text("Format")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageItem(
    icon: ImageVector,
    title: String,
    size: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(Spacing.large),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = size,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

