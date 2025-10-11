package com.mpdc4gsr.component.thermal.fragment

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.thermal.viewmodel.GalleryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GalleryVideoComposeFragment : BaseComposeFragment<GalleryViewModel>() {
    override fun createViewModel(): GalleryViewModel = viewModels<GalleryViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GalleryViewModel) {
        val context = LocalContext.current
        // Observe ViewModel state
        val videoItems by viewModel.videoItems.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        LibSharedTheme {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Selection toolbar
                if (isSelectionMode) {
                    VideoSelectionToolbar(
                        selectedCount = selectedItems.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onShareSelected = {
                            shareSelectedVideos(context, selectedItems.toList())
                        },
                        onDeleteSelected = { viewModel.deleteSelectedItems() },
                        onExportSelected = {
                            exportSelectedVideos(context, selectedItems.toList())
                        },
                    )
                }
                // Video gallery content
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        isLoading -> {
                            LoadingState()
                        }

                        videoItems.isEmpty() -> {
                            EmptyVideoGalleryState(
                                onRefresh = { viewModel.refreshVideoGallery() },
                            )
                        }

                        else -> {
                            VideoGrid(
                                videos = videoItems,
                                selectedItems = selectedItems,
                                isSelectionMode = isSelectionMode,
                                onItemClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item)
                                    } else {
                                        playVideo(context, item.path)
                                    }
                                },
                                onItemLongClick = { item ->
                                    if (!isSelectionMode) {
                                        viewModel.enterSelectionMode()
                                        viewModel.toggleItemSelection(item)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun VideoSelectionToolbar(
        selectedCount: Int,
        onClearSelection: () -> Unit,
        onShareSelected: () -> Unit,
        onDeleteSelected: () -> Unit,
        onExportSelected: () -> Unit,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$selectedCount video${if (selectedCount != 1) "s" else ""} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onShareSelected) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onExportSelected) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onClearSelection) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingState() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading videos...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun EmptyVideoGalleryState(onRefresh: () -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = "No videos",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "No Videos Found",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Record thermal videos to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
        }
    }

    @Composable
    private fun VideoGrid(
        videos: List<GalleryViewModel.MediaItem>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        onItemClick: (GalleryViewModel.MediaItem) -> Unit,
        onItemLongClick: (GalleryViewModel.MediaItem) -> Unit,
    ) {
        // Adaptive grid columns based on screen size
        val columns = remember { mutableIntStateOf(3) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns.intValue),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(videos) { item ->
                VideoGridItem(
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
    private fun VideoGridItem(
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
                    .aspectRatio(16f / 9f)
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
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Video thumbnail
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(item.thumbnailPath ?: item.path)
                            .crossfade(true)
                            .build(),
                    contentDescription = item.name,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                // Video play indicator
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                // Thermal video indicator
                if (item.isVideo) {
                    Card(
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                            ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "THERMAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
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
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier =
                                    Modifier
                                        .background(
                                            Color.White,
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                        ).padding(2.dp),
                            )
                        } else {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Not Selected",
                                tint = Color.White,
                                modifier =
                                    Modifier
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                        ).padding(2.dp),
                            )
                        }
                    }
                }
                // Video info overlay
                Card(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f),
                        ),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = formatFileSize(item.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        Text(
                            text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(item.dateModified)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }

    // Helper functions
    private fun playVideo(
        context: android.content.Context,
        path: String,
    ) {
        try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            File(path),
                        ),
                        "video/*",
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - maybe show a toast or use internal video player
        }
    }

    private fun shareSelectedVideos(
        context: android.content.Context,
        selectedPaths: List<String>,
    ) {
        try {
            val uris =
                selectedPaths.map { path ->
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path),
                    )
                }
            val intent =
                Intent().apply {
                    if (uris.size == 1) {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uris.first())
                    } else {
                        action = Intent.ACTION_SEND_MULTIPLE
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    }
                    type = "video/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(intent, "Share Videos"))
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun exportSelectedVideos(
        context: android.content.Context,
        selectedPaths: List<String>,
    ) {
        // Implementation for exporting videos to external storage
        // This would typically involve copying files to a user-accessible location
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




