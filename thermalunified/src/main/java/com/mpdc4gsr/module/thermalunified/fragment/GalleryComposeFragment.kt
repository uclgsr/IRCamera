package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.GalleryViewModel
import java.text.SimpleDateFormat
import java.util.*

class GalleryComposeFragment : BaseComposeFragment<GalleryViewModel>() {
    override fun createViewModel(): GalleryViewModel = viewModels<GalleryViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GalleryViewModel) {
        LibUnifiedTheme {
            val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
            val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
            val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
            val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Top bar with controls
                GalleryTopBar(
                    isGridView = isGridView,
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedItems.size,
                    onToggleView = { viewModel.toggleViewMode() },
                    onCancelSelection = { viewModel.exitSelectionMode() },
                    onDeleteSelected = { viewModel.deleteSelectedItems() },
                    onShareSelected = { viewModel.shareSelectedItems() },
                )
                // Media content
                when {
                    mediaItems.isEmpty() -> {
                        EmptyGalleryState(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    isGridView -> {
                        GridGalleryView(
                            mediaItems = mediaItems,
                            selectedItems = selectedItems,
                            isSelectionMode = isSelectionMode,
                            onItemClick = { item ->
                                if (isSelectionMode) {
                                    viewModel.toggleItemSelection(item)
                                } else {
                                    viewModel.openMediaItem(item)
                                }
                            },
                            onItemLongClick = { item ->
                                if (!isSelectionMode) {
                                    viewModel.enterSelectionMode(item)
                                }
                            },
                        )
                    }

                    else -> {
                        ListGalleryView(
                            mediaItems = mediaItems,
                            selectedItems = selectedItems,
                            isSelectionMode = isSelectionMode,
                            onItemClick = { item ->
                                if (isSelectionMode) {
                                    viewModel.toggleItemSelection(item)
                                } else {
                                    viewModel.openMediaItem(item)
                                }
                            },
                            onItemLongClick = { item ->
                                if (!isSelectionMode) {
                                    viewModel.enterSelectionMode(item)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GalleryTopBar(
        isGridView: Boolean,
        isSelectionMode: Boolean,
        selectedCount: Int,
        onToggleView: () -> Unit,
        onCancelSelection: () -> Unit,
        onDeleteSelected: () -> Unit,
        onShareSelected: () -> Unit,
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (isSelectionMode) "$selectedCount selected" else "Gallery",
                    fontWeight = FontWeight.Bold,
                )
            },
            actions = {
                if (isSelectionMode) {
                    IconButton(onClick = onShareSelected) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onCancelSelection) {
                        Text("Cancel")
                    }
                } else {
                    IconButton(onClick = onToggleView) {
                        Icon(
                            if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "List View" else "Grid View",
                        )
                    }
                }
            },
        )
    }

    @Composable
    private fun GridGalleryView(
        mediaItems: List<GalleryViewModel.MediaItem>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        onItemClick: (GalleryViewModel.MediaItem) -> Unit,
        onItemLongClick: (GalleryViewModel.MediaItem) -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(mediaItems) { item ->
                GridMediaItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                )
            }
        }
    }

    @Composable
    private fun ListGalleryView(
        mediaItems: List<GalleryViewModel.MediaItem>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        onItemClick: (GalleryViewModel.MediaItem) -> Unit,
        onItemLongClick: (GalleryViewModel.MediaItem) -> Unit,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(mediaItems) { item ->
                ListMediaItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                )
            }
        }
    }

    @Composable
    private fun GridMediaItem(
        item: GalleryViewModel.MediaItem,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    ) {
        Card(
            onClick = onClick,
            modifier =
                Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                ),
            border =
                if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Thumbnail image
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(item.thumbnailPath)
                            .crossfade(true)
                            .build(),
                    contentDescription = item.name,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                // Selection indicator
                if (isSelectionMode) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Delete, // Using as checkmark substitute
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier =
                                    Modifier
                                        .background(
                                            color = Color.White,
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                        ).padding(4.dp),
                            )
                        }
                    }
                }
                // Media info overlay
                Card(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                        )
                        Text(
                            text = formatFileSize(item.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ListMediaItem(
        item: GalleryViewModel.MediaItem,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Thumbnail
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(item.thumbnailPath)
                            .crossfade(true)
                            .build(),
                    contentDescription = item.name,
                    modifier =
                        Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                // File info
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = formatFileSize(item.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text =
                            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(item.dateModified)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Selection indicator
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null, // Handled by card click
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyGalleryState(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Default.GridView,
                    contentDescription = "Empty gallery",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "No Media Files",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Capture photos or videos with the thermal camera to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return "%.1f %s".format(size, units[unitIndex])
    }
}
