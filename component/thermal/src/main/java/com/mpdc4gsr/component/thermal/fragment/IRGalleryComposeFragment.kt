package com.mpdc4gsr.component.thermal.fragment

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.mpdc4gsr.component.shared.app.bean.GalleryBean
import com.mpdc4gsr.component.shared.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.component.shared.app.compose.theme.LibSharedTheme
import com.mpdc4gsr.component.shared.app.repository.GalleryRepository.DirType
import com.mpdc4gsr.component.thermal.viewmodel.IRGalleryViewModel
import java.text.SimpleDateFormat
import java.util.*

class IRGalleryComposeFragment : BaseComposeFragment<IRGalleryViewModel>() {
    override fun createViewModel(): IRGalleryViewModel = viewModels<IRGalleryViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRGalleryViewModel) {
        val context = LocalContext.current
        // Observe ViewModel state
        val galleryItems by viewModel.galleryItems.collectAsStateWithLifecycle()
        val currentDirType by viewModel.currentDirType.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
        LibSharedTheme {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Top app bar with directory selector and controls
                IRGalleryTopBar(
                    currentDirType = currentDirType,
                    isGridView = isGridView,
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedItems.size,
                    onDirTypeChanged = { dirType ->
                        viewModel.changeDirType(dirType)
                    },
                    onToggleView = { viewModel.toggleViewMode() },
                    onCancelSelection = { viewModel.exitSelectionMode() },
                    onDeleteSelected = { viewModel.deleteSelectedItems() },
                    onShareSelected = { viewModel.shareSelectedItems() },
                )
                // Main content
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        isLoading -> {
                            LoadingState()
                        }

                        galleryItems.isEmpty() -> {
                            EmptyGalleryState(
                                dirType = currentDirType,
                                onRefresh = { viewModel.refreshGallery() },
                            )
                        }

                        isGridView -> {
                            GridGalleryView(
                                galleryItems = galleryItems,
                                selectedItems = selectedItems,
                                isSelectionMode = isSelectionMode,
                                viewModel = viewModel,
                                onItemClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item)
                                    } else {
                                        viewModel.openGalleryItem(item)
                                    }
                                },
                                onItemLongClick = { item ->
                                    if (!isSelectionMode) {
                                        viewModel.enterSelectionMode(item)
                                    }
                                },
                                onRefresh = { viewModel.refreshGallery() },
                            )
                        }

                        else -> {
                            ListGalleryView(
                                galleryItems = galleryItems,
                                selectedItems = selectedItems,
                                isSelectionMode = isSelectionMode,
                                viewModel = viewModel,
                                onItemClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item)
                                    } else {
                                        viewModel.openGalleryItem(item)
                                    }
                                },
                                onItemLongClick = { item ->
                                    if (!isSelectionMode) {
                                        viewModel.enterSelectionMode(item)
                                    }
                                },
                                onRefresh = { viewModel.refreshGallery() },
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun IRGalleryTopBar(
        currentDirType: DirType,
        isGridView: Boolean,
        isSelectionMode: Boolean,
        selectedCount: Int,
        onDirTypeChanged: (DirType) -> Unit,
        onToggleView: () -> Unit,
        onCancelSelection: () -> Unit,
        onDeleteSelected: () -> Unit,
        onShareSelected: () -> Unit,
    ) {
        TopAppBar(
            title = {
                if (isSelectionMode) {
                    Text("$selectedCount selected")
                } else {
                    // Directory selector dropdown
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(
                                text =
                                    when (currentDirType) {
                                        DirType.LINE -> "LINE Device"
                                        DirType.TS004_LOCALE -> "TS004 Device"
                                        else -> "All Devices"
                                    },
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("LINE Device") },
                                onClick = {
                                    onDirTypeChanged(DirType.LINE)
                                    expanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("TS004 Device") },
                                onClick = {
                                    onDirTypeChanged(DirType.TS004_LOCALE)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
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
                    text = "Loading gallery...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun EmptyGalleryState(
        dirType: DirType,
        onRefresh: () -> Unit,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Empty gallery",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "No Files Found",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Capture thermal images with your ${getDirTypeName(dirType)} to see them here",
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
    private fun GridGalleryView(
        galleryItems: List<GalleryBean>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        viewModel: IRGalleryViewModel,
        onItemClick: (GalleryBean) -> Unit,
        onItemLongClick: (GalleryBean) -> Unit,
        onRefresh: () -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(galleryItems) { item ->
                GridGalleryItem(
                    item = item,
                    isSelected = selectedItems.contains(item.path),
                    isSelectionMode = isSelectionMode,
                    viewModel = viewModel,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                )
            }
        }
    }

    @Composable
    private fun ListGalleryView(
        galleryItems: List<GalleryBean>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        viewModel: IRGalleryViewModel,
        onItemClick: (GalleryBean) -> Unit,
        onItemLongClick: (GalleryBean) -> Unit,
        onRefresh: () -> Unit,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(galleryItems) { item ->
                ListGalleryItem(
                    item = item,
                    isSelected = selectedItems.contains(item.path),
                    isSelectionMode = isSelectionMode,
                    viewModel = viewModel,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                )
            }
        }
    }

    @Composable
    private fun GridGalleryItem(
        item: GalleryBean,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        viewModel: IRGalleryViewModel,
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
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(item.path)
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
                if (isSelectionMode && isSelected) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                    ) {
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
                    }
                }
                // File info overlay
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
                            text = item.name ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                        )
                        Text(
                            text = formatFileSize(viewModel.getCachedFileSize(item.path)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ListGalleryItem(
        item: GalleryBean,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        viewModel: IRGalleryViewModel,
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
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(item.path)
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
                        text = item.name ?: "Unknown File",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = formatFileSize(viewModel.getCachedFileSize(item.path)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text =
                            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(item.timeMillis)),
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

    private fun getDirTypeName(dirType: DirType): String =
        when (dirType) {
            DirType.LINE -> "LINE device"
            DirType.TS004_LOCALE -> "TS004 device"
            else -> "device"
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




