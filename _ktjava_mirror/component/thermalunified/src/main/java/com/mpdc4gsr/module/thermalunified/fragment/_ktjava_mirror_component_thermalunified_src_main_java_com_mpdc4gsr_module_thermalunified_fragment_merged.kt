// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:43


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_fragment_all.kt =====

// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment' subtree
// Files: 15; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\AbilityComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.activity.IRThermalPlusComposeActivity
import com.mpdc4gsr.module.thermalunified.activity.MonitoryHomeComposeActivity
import com.mpdc4gsr.module.thermalunified.activity.ThermalIrNightComposeActivity

class AbilityComposeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Content()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content() {
        val context = LocalContext.current
        var isTC007 by remember { mutableStateOf(false) }
        // Get TC007 status from arguments
        LaunchedEffect(Unit) {
            isTC007 = arguments?.getBoolean("IS_TC007", false) ?: false
        }
        LibUnifiedTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Thermal Imaging Abilities",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Explore advanced thermal imaging capabilities and specialized modes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                // Abilities grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(getAbilityItems(isTC007)) { ability ->
                        AbilityCard(
                            ability = ability,
                            onClick = {
                                handleAbilityClick(context, ability, isTC007)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AbilityCard(
        ability: AbilityItem,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            colors = CardDefaults.cardColors(
                containerColor = ability.containerColor
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (ability.iconRes != null) {
                    Icon(
                        painter = painterResource(id = ability.iconRes),
                        contentDescription = ability.title,
                        modifier = Modifier.size(48.dp),
                        tint = ability.iconTint
                    )
                } else {
                    Icon(
                        imageVector = ability.icon ?: Icons.Default.Settings,
                        contentDescription = ability.title,
                        modifier = Modifier.size(48.dp),
                        tint = ability.iconTint
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = ability.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ability.textColor
                )
                if (ability.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ability.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ability.textColor.copy(alpha = 0.8f)
                    )
                }
                if (ability.badge.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = ability.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    private fun getAbilityItems(isTC007: Boolean): List<AbilityItem> {
        return listOf(
            AbilityItem(
                id = "winter",
                title = "Winter Mode",
                description = "Enhanced cold weather detection",
                iconRes = R.drawable.ic_ir_winter_bg,
                containerColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "SPECIALTY"
            ),
            AbilityItem(
                id = "monitoring",
                title = "Monitoring",
                description = "Advanced thermal monitoring",
                icon = Icons.Default.Monitor,
                containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "CORE"
            ),
            AbilityItem(
                id = "residential",
                title = "Residential",
                description = "Home energy audit mode",
                icon = Icons.Default.Home,
                containerColor = androidx.compose.ui.graphics.Color(0xFFFF9800),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "AUDIT"
            ),
            AbilityItem(
                id = "automotive",
                title = "Automotive",
                description = "Vehicle thermal analysis",
                icon = Icons.Default.DirectionsCar,
                containerColor = androidx.compose.ui.graphics.Color(0xFFF44336),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "AUTO"
            ),
            AbilityItem(
                id = "night_vision",
                title = "Night Vision",
                description = "Enhanced night thermal",
                icon = Icons.Default.NightsStay,
                containerColor = androidx.compose.ui.graphics.Color(0xFF9C27B0),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = if (isTC007) "TC007" else "ENHANCED"
            ),
            AbilityItem(
                id = "thermal_plus",
                title = "Thermal Plus",
                description = "Advanced thermal features",
                icon = Icons.Default.Add,
                containerColor = androidx.compose.ui.graphics.Color(0xFF607D8B),
                iconTint = androidx.compose.ui.graphics.Color.White,
                textColor = androidx.compose.ui.graphics.Color.White,
                badge = "PLUS"
            )
        )
    }

    private fun handleAbilityClick(
        context: android.content.Context,
        ability: AbilityItem,
        isTC007: Boolean
    ) {
        when (ability.id) {
            "winter" -> {
                // Handle winter mode - winter mode tracking
            }

            "monitoring" -> {
                // Navigate to monitoring home
                val intent = Intent(context, MonitoryHomeComposeActivity::class.java)
                context.startActivity(intent)
            }

            "residential" -> {
                // Navigate to residential thermal analysis
                NavigationManager.getInstance()
                    .build("IR_RESIDENTIAL")
                    .navigation(context)
            }

            "automotive" -> {
                // Navigate to automotive thermal analysis
                NavigationManager.getInstance()
                    .build("IR_AUTOMOTIVE")
                    .navigation(context)
            }

            "night_vision" -> {
                // Navigate to night vision thermal mode
                val intent = Intent(context, ThermalIrNightComposeActivity::class.java)
                context.startActivity(intent)
            }

            "thermal_plus" -> {
                // Navigate to thermal plus features
                val intent = Intent(context, IRThermalPlusComposeActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    data class AbilityItem(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector? = null,
        val iconRes: Int? = null,
        val containerColor: androidx.compose.ui.graphics.Color,
        val iconTint: androidx.compose.ui.graphics.Color,
        val textColor: androidx.compose.ui.graphics.Color,
        val badge: String = ""
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\GalleryComposeFragment.kt =====

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
    override fun createViewModel(): GalleryViewModel {
        return viewModels<GalleryViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GalleryViewModel) {
        LibUnifiedTheme {
            val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
            val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
            val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
            val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar with controls
                GalleryTopBar(
                    isGridView = isGridView,
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedItems.size,
                    onToggleView = { viewModel.toggleViewMode() },
                    onCancelSelection = { viewModel.exitSelectionMode() },
                    onDeleteSelected = { viewModel.deleteSelectedItems() },
                    onShareSelected = { viewModel.shareSelectedItems() }
                )
                // Media content
                when {
                    mediaItems.isEmpty() -> {
                        EmptyGalleryState(
                            modifier = Modifier.fillMaxSize()
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
                            }
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
                            }
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
        onShareSelected: () -> Unit
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (isSelectionMode) "$selectedCount selected" else "Gallery",
                    fontWeight = FontWeight.Bold
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
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = onCancelSelection) {
                        Text("Cancel")
                    }
                } else {
                    IconButton(onClick = onToggleView) {
                        Icon(
                            if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "List View" else "Grid View"
                        )
                    }
                }
            }
        )
    }

    @Composable
    private fun GridGalleryView(
        mediaItems: List<GalleryViewModel.MediaItem>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        onItemClick: (GalleryViewModel.MediaItem) -> Unit,
        onItemLongClick: (GalleryViewModel.MediaItem) -> Unit
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mediaItems) { item ->
                GridMediaItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
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
        onItemLongClick: (GalleryViewModel.MediaItem) -> Unit
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mediaItems) { item ->
                ListMediaItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
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
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Thumbnail image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.thumbnailPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                // Selection indicator
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Delete, // Using as checkmark substitute
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        color = Color.White,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .padding(4.dp)
                            )
                        }
                    }
                }
                // Media info overlay
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = formatFileSize(item.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
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
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.thumbnailPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                // File info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatFileSize(item.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(Date(item.dateModified)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Selection indicator
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null // Handled by card click
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyGalleryState(
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.GridView,
                    contentDescription = "Empty gallery",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No Media Files",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Capture photos or videos with the thermal camera to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\GalleryPictureComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import android.content.Intent
import android.net.Uri
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
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.GalleryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GalleryPictureComposeFragment : BaseComposeFragment<GalleryViewModel>() {
    override fun createViewModel(): GalleryViewModel {
        return viewModels<GalleryViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GalleryViewModel) {
        val context = LocalContext.current
        // Observe ViewModel state
        val galleryItems by viewModel.galleryItems.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Selection toolbar
                if (isSelectionMode) {
                    SelectionToolbar(
                        selectedCount = selectedItems.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onShareSelected = {
                            shareSelectedImages(context, selectedItems.toList())
                        },
                        onDeleteSelected = { viewModel.deleteSelectedItems() },
                        onExportSelected = {
                            exportSelectedImages(context, selectedItems.toList())
                        }
                    )
                }
                // Gallery content
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            LoadingState()
                        }

                        galleryItems.isEmpty() -> {
                            EmptyGalleryState(
                                onRefresh = { viewModel.refreshGallery() }
                            )
                        }

                        else -> {
                            PictureGrid(
                                pictures = galleryItems,
                                selectedItems = selectedItems,
                                isSelectionMode = isSelectionMode,
                                onItemClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item)
                                    } else {
                                        previewPicture(context, item.path)
                                    }
                                },
                                onItemLongClick = { item ->
                                    if (!isSelectionMode) {
                                        viewModel.enterSelectionMode()
                                        viewModel.toggleItemSelection(item)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SelectionToolbar(
        selectedCount: Int,
        onClearSelection: () -> Unit,
        onShareSelected: () -> Unit,
        onDeleteSelected: () -> Unit,
        onExportSelected: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            tint = MaterialTheme.colorScheme.error
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
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading images...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun EmptyGalleryState(
        onRefresh: () -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "No pictures",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No Pictures Found",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Capture thermal images to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    private fun PictureGrid(
        pictures: List<GalleryViewModel.MediaItem>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        onItemClick: (GalleryViewModel.MediaItem) -> Unit,
        onItemLongClick: (GalleryViewModel.MediaItem) -> Unit
    ) {
        // Adaptive grid columns based on screen size
        val columns = remember { mutableIntStateOf(3) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns.intValue),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pictures) { item ->
                PictureGridItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }

    @Composable
    private fun PictureGridItem(
        item: GalleryViewModel.MediaItem,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Image thumbnail
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.path)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                // Thermal image overlay indicator
                if (!item.isVideo) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "THERMAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                // Selection indicator
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        Color.White,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .padding(2.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Not Selected",
                                tint = Color.White,
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .padding(2.dp)
                            )
                        }
                    }
                }
                // Image info overlay
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = formatFileSize(item.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(item.dateModified)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // Helper functions
    private fun previewPicture(context: android.content.Context, path: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(File(path)), "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - maybe show a toast or use internal image viewer
        }
    }

    private fun shareSelectedImages(context: android.content.Context, selectedPaths: List<String>) {
        try {
            val uris = selectedPaths.map { path ->
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(path)
                )
            }
            val intent = Intent().apply {
                if (uris.size == 1) {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                } else {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                }
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Images"))
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun exportSelectedImages(context: android.content.Context, selectedPaths: List<String>) {
        // Implementation for exporting images to external storage
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\GalleryVideoComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

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
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.GalleryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GalleryVideoComposeFragment : BaseComposeFragment<GalleryViewModel>() {
    override fun createViewModel(): GalleryViewModel {
        return viewModels<GalleryViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GalleryViewModel) {
        val context = LocalContext.current
        // Observe ViewModel state
        val videoItems by viewModel.videoItems.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
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
                        }
                    )
                }
                // Video gallery content
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            LoadingState()
                        }

                        videoItems.isEmpty() -> {
                            EmptyVideoGalleryState(
                                onRefresh = { viewModel.refreshVideoGallery() }
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
                                }
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
        onExportSelected: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount video${if (selectedCount != 1) "s" else ""} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            tint = MaterialTheme.colorScheme.error
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
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading videos...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun EmptyVideoGalleryState(
        onRefresh: () -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = "No videos",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No Videos Found",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Record thermal videos to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        onItemLongClick: (GalleryViewModel.MediaItem) -> Unit
    ) {
        // Adaptive grid columns based on screen size
        val columns = remember { mutableIntStateOf(3) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns.intValue),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(videos) { item ->
                VideoGridItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
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
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .aspectRatio(16f / 9f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Video thumbnail
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.thumbnailPath ?: item.path)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                // Video play indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                // Thermal video indicator
                if (item.isVideo) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "THERMAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                // Selection indicator
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        Color.White,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .padding(2.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Not Selected",
                                tint = Color.White,
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .padding(2.dp)
                            )
                        }
                    }
                }
                // Video info overlay
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatFileSize(item.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(item.dateModified)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // Helper functions
    private fun playVideo(context: android.content.Context, path: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path)
                    ), "video/*"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - maybe show a toast or use internal video player
        }
    }

    private fun shareSelectedVideos(context: android.content.Context, selectedPaths: List<String>) {
        try {
            val uris = selectedPaths.map { path ->
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(path)
                )
            }
            val intent = Intent().apply {
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

    private fun exportSelectedVideos(context: android.content.Context, selectedPaths: List<String>) {
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\IRCorrectionComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infisense.usbir.view.CameraView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.module.thermalunified.viewmodel.CalibrationStatus
import com.mpdc4gsr.module.thermalunified.viewmodel.CorrectionState
import com.mpdc4gsr.module.thermalunified.viewmodel.IRCorrectionViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.TemperatureData

class IRCorrectionComposeFragment : BaseComposeFragment<IRCorrectionViewModel>() {
    // Compatibility property for legacy code that checks frameReady
    val frameReady: Boolean get() = createViewModel().correctionState.value == CorrectionState.ACTIVE
    override fun createViewModel(): IRCorrectionViewModel {
        return viewModels<IRCorrectionViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRCorrectionViewModel) {
        // Observe ViewModel state
        val correctionState by viewModel.correctionState.collectAsStateWithLifecycle()
        val temperatureData by viewModel.temperatureData.collectAsStateWithLifecycle()
        val calibrationStatus by viewModel.calibrationStatus.collectAsStateWithLifecycle()
        val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar with correction status
                CorrectionStatusBar(
                    correctionState = correctionState,
                    calibrationStatus = calibrationStatus,
                    isProcessing = isProcessing,
                    onToggleCorrection = { viewModel.toggleCorrection() }
                )
                // Main content area
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thermal view area
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        ThermalCorrectionView(
                            temperatureData = temperatureData,
                            onTemperatureUpdate = { temp, x, y ->
                                viewModel.updateTemperaturePoint(temp, x, y)
                            }
                        )
                        // Temperature overlay
                        TemperatureOverlay(
                            temperatureData = temperatureData,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                    // Correction controls panel
                    CorrectionControlsPanel(
                        correctionState = correctionState,
                        onCorrectionValueChange = { value ->
                            viewModel.updateCorrectionValue(value)
                        },
                        onCalibrate = { viewModel.startCalibration() },
                        onReset = { viewModel.resetCorrection() },
                        onSaveSettings = { viewModel.saveSettings() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun CorrectionStatusBar(
        correctionState: CorrectionState,
        calibrationStatus: CalibrationStatus,
        isProcessing: Boolean,
        onToggleCorrection: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (correctionState) {
                    CorrectionState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    CorrectionState.CALIBRATING -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Temperature Correction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getStatusText(correctionState, calibrationStatus),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Switch(
                        checked = correctionState == CorrectionState.ACTIVE,
                        onCheckedChange = { onToggleCorrection() },
                        enabled = !isProcessing
                    )
                }
            }
        }
    }

    @Composable
    private fun ThermalCorrectionView(
        temperatureData: TemperatureData?,
        onTemperatureUpdate: (Float, Int, Int) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Thermal camera view integration
                AndroidView(
                    factory = { context ->
                        TemperatureView(context).apply {
                            // Configure the temperature view
                            setListener(object : TemperatureView.TempListener {
                                override fun getTemp(max: Float, min: Float, tempData: ByteArray) {
                                    // Use max temperature as the representative temperature
                                    // For demo purposes, use center coordinates
                                    onTemperatureUpdate(max, 0, 0)
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Camera view overlay
                AndroidView(
                    factory = { context ->
                        CameraView(context)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )
            }
        }
    }

    @Composable
    private fun TemperatureOverlay(
        temperatureData: TemperatureData?,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            temperatureData?.let { data ->
                TemperatureCard(
                    label = "Current",
                    temperature = "${data.currentTemp}Â°C",
                    isMain = true
                )
                TemperatureCard(
                    label = "Corrected",
                    temperature = "${data.correctedTemp}Â°C",
                    color = Color.Green
                )
                TemperatureCard(
                    label = "Offset",
                    temperature = "${data.offsetValue}Â°C",
                    color = if (data.offsetValue >= 0) MaterialTheme.colorScheme.primary else Color.Red
                )
            }
        }
    }

    @Composable
    private fun TemperatureCard(
        label: String,
        temperature: String,
        isMain: Boolean = false,
        color: Color = Color.White
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMain)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Text(
                    text = temperature,
                    style = if (isMain)
                        MaterialTheme.typography.titleSmall
                    else
                        MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }

    @Composable
    private fun CorrectionControlsPanel(
        correctionState: CorrectionState,
        onCorrectionValueChange: (Float) -> Unit,
        onCalibrate: () -> Unit,
        onReset: () -> Unit,
        onSaveSettings: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Correction Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Correction value slider
                CorrectionValueSlider(
                    onValueChange = onCorrectionValueChange,
                    enabled = correctionState == CorrectionState.ACTIVE
                )
                HorizontalDivider()
                // Calibration section
                CalibrationSection(
                    onCalibrate = onCalibrate,
                    onReset = onReset,
                    enabled = correctionState != CorrectionState.CALIBRATING
                )
                HorizontalDivider()
                // Temperature regions
                TemperatureRegionsSection()
                Spacer(modifier = Modifier.weight(1f))
                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Settings")
                    }
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All")
                    }
                }
            }
        }
    }

    @Composable
    private fun CorrectionValueSlider(
        onValueChange: (Float) -> Unit,
        enabled: Boolean
    ) {
        var sliderValue by remember { mutableFloatStateOf(0f) }
        Column {
            Text(
                text = "Correction Offset: ${String.format("%.1f", sliderValue)}Â°C",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onValueChange(it)
                },
                valueRange = -10f..10f,
                steps = 39, // 0.5 degree steps
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun CalibrationSection(
        onCalibrate: () -> Unit,
        onReset: () -> Unit,
        enabled: Boolean
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Calibration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCalibrate,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }

    @Composable
    private fun TemperatureRegionsSection() {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Temperature Regions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RegionButton("Point", Icons.Default.Place)
                RegionButton("Line", Icons.Default.Timeline)
                RegionButton("Area", Icons.Default.CropFree)
            }
        }
    }

    @Composable
    private fun RegionButton(
        text: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector
    ) {
        var isSelected by remember { mutableStateOf(false) }
        FilterChip(
            onClick = { isSelected = !isSelected },
            label = { Text(text, style = MaterialTheme.typography.labelSmall) },
            selected = isSelected,
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = text,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }

    private fun getStatusText(
        correctionState: CorrectionState,
        calibrationStatus: CalibrationStatus
    ): String = when {
        correctionState == CorrectionState.CALIBRATING -> "Calibrating..."
        correctionState == CorrectionState.ACTIVE -> "Active - ${calibrationStatus.name.lowercase()}"
        else -> "Inactive"
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\IRGalleryComposeFragment.kt =====

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
import com.mpdc4gsr.libunified.app.bean.GalleryBean
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.repository.GalleryRepository.DirType
import com.mpdc4gsr.module.thermalunified.viewmodel.IRGalleryViewModel
import java.text.SimpleDateFormat
import java.util.*

class IRGalleryComposeFragment : BaseComposeFragment<IRGalleryViewModel>() {
    override fun createViewModel(): IRGalleryViewModel {
        return viewModels<IRGalleryViewModel>().value
    }

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
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
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
                    onShareSelected = { viewModel.shareSelectedItems() }
                )
                // Main content
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            LoadingState()
                        }

                        galleryItems.isEmpty() -> {
                            EmptyGalleryState(
                                dirType = currentDirType,
                                onRefresh = { viewModel.refreshGallery() }
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
                                onRefresh = { viewModel.refreshGallery() }
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
                                onRefresh = { viewModel.refreshGallery() }
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
        onShareSelected: () -> Unit
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
                                text = when (currentDirType) {
                                    DirType.LINE -> "LINE Device"
                                    DirType.TS004_LOCALE -> "TS004 Device"
                                    else -> "All Devices"
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("LINE Device") },
                                onClick = {
                                    onDirTypeChanged(DirType.LINE)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("TS004 Device") },
                                onClick = {
                                    onDirTypeChanged(DirType.TS004_LOCALE)
                                    expanded = false
                                }
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
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = onCancelSelection) {
                        Text("Cancel")
                    }
                } else {
                    IconButton(onClick = onToggleView) {
                        Icon(
                            if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "List View" else "Grid View"
                        )
                    }
                }
            }
        )
    }

    @Composable
    private fun LoadingState() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading gallery...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun EmptyGalleryState(
        dirType: DirType,
        onRefresh: () -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Empty gallery",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No Files Found",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Capture thermal images with your ${getDirTypeName(dirType)} to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        onRefresh: () -> Unit
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(galleryItems) { item ->
                GridGalleryItem(
                    item = item,
                    isSelected = selectedItems.contains(item.path),
                    isSelectionMode = isSelectionMode,
                    viewModel = viewModel,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
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
        onRefresh: () -> Unit
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(galleryItems) { item ->
                ListGalleryItem(
                    item = item,
                    isSelected = selectedItems.contains(item.path),
                    isSelectionMode = isSelectionMode,
                    viewModel = viewModel,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
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
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.path)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                // Selection indicator
                if (isSelectionMode && isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    Color.White,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .padding(2.dp)
                        )
                    }
                }
                // File info overlay
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = item.name ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = formatFileSize(viewModel.getCachedFileSize(item.path)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
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
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.path)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                // File info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name ?: "Unknown File",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatFileSize(viewModel.getCachedFileSize(item.path)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(Date(item.timeMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Selection indicator
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null // Handled by card click
                    )
                }
            }
        }
    }

    private fun getDirTypeName(dirType: DirType): String = when (dirType) {
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\IRGalleryTabComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mpdc4gsr.libunified.app.repository.GalleryRepository.DirType
import com.mpdc4gsr.module.thermalunified.viewmodel.IRGalleryTabViewModel
import kotlinx.coroutines.launch

class IRGalleryTabComposeFragment : BaseComposeFragment<IRGalleryTabViewModel>() {
    override fun createViewModel(): IRGalleryTabViewModel {
        return viewModels<IRGalleryTabViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRGalleryTabViewModel) {
        // Observe ViewModel state
        val currentDirType by viewModel.currentDirType.collectAsStateWithLifecycle()
        val canSwitchDir by viewModel.canSwitchDir.collectAsStateWithLifecycle()
        val hasBackIcon by viewModel.hasBackIcon.collectAsStateWithLifecycle()
        // Handle UI events from ViewModel
        LaunchedEffect(Unit) {
            viewModel.uiEvents.collect { event ->
                when (event) {
                    is com.mpdc4gsr.libunified.app.ktbase.BaseViewModel.UiEvent.NavigateBack -> {
                        requireActivity().finish()
                    }

                    else -> {} // Handle other events if needed
                }
            }
        }
        // Tab configuration
        val tabTitles = listOf("Pictures", "Videos", "Reports")
        val pagerState = rememberPagerState(pageCount = { tabTitles.size })
        val coroutineScope = rememberCoroutineScope()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top app bar with directory switcher
                GalleryTopBar(
                    viewModel = viewModel,
                    currentDirType = currentDirType,
                    canSwitchDir = canSwitchDir,
                    hasBackIcon = hasBackIcon,
                    onDirectoryChange = { dirType ->
                        viewModel.changeDirType(dirType)
                    },
                    onBackClick = { viewModel.navigateBack() }
                )
                // Tab row
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (pagerState.currentPage == index)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
                // Tab content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> GalleryPictureTab(
                            dirType = currentDirType,
                            modifier = Modifier.fillMaxSize()
                        )

                        1 -> GalleryVideoTab(
                            dirType = currentDirType,
                            modifier = Modifier.fillMaxSize()
                        )

                        2 -> GalleryReportsTab(
                            dirType = currentDirType,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GalleryTopBar(
        viewModel: IRGalleryTabViewModel,
        currentDirType: DirType,
        canSwitchDir: Boolean,
        hasBackIcon: Boolean,
        onDirectoryChange: (DirType) -> Unit,
        onBackClick: () -> Unit
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Gallery",
                        fontWeight = FontWeight.Bold
                    )
                    if (canSwitchDir) {
                        DirectorySwitcher(
                            currentDirType = currentDirType,
                            onDirectoryChange = onDirectoryChange
                        )
                    }
                }
            },
            navigationIcon = {
                if (hasBackIcon) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                IconButton(onClick = { viewModel.showSearch() }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { viewModel.showMoreOptions() }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        )
    }

    @Composable
    private fun DirectorySwitcher(
        currentDirType: DirType,
        onDirectoryChange: (DirType) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                onClick = { expanded = true },
                label = {
                    Text(
                        text = getDirTypeDisplayName(currentDirType),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = false,
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Directory"
                    )
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DirType.values().forEach { dirType ->
                    DropdownMenuItem(
                        text = {
                            Text(getDirTypeDisplayName(dirType))
                        },
                        onClick = {
                            onDirectoryChange(dirType)
                            expanded = false
                        },
                        leadingIcon = {
                            if (dirType == currentDirType) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun GalleryPictureTab(
        dirType: DirType,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            // This would embed the actual GalleryPictureComposeFragment
            // For now, showing a placeholder that will be replaced with the actual implementation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = "Pictures",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Picture Gallery",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Directory: ${getDirTypeDisplayName(dirType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Integration with GalleryPictureComposeFragment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun GalleryVideoTab(
        dirType: DirType,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = "Videos",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Video Gallery",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Directory: ${getDirTypeDisplayName(dirType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Thermal video recordings and playback",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun GalleryReportsTab(
        dirType: DirType,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = "Reports",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Analysis Reports",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Directory: ${getDirTypeDisplayName(dirType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "PDF reports and thermal analysis data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    private fun getDirTypeDisplayName(dirType: DirType): String = when (dirType) {
        DirType.LINE -> "LINE Device"
        DirType.TS004_LOCALE -> "TS004 Local"
        DirType.TS004_REMOTE -> "TS004 Remote"
        DirType.TC007 -> "TC007 Device"
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\IRMonitorCaptureComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorCaptureViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorCaptureViewModel.*
import java.text.SimpleDateFormat
import java.util.*

class IRMonitorCaptureComposeFragment : BaseComposeFragment<IRMonitorCaptureViewModel>() {
    override fun createViewModel(): IRMonitorCaptureViewModel {
        return viewModels<IRMonitorCaptureViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRMonitorCaptureViewModel) {
        // Observe ViewModel state
        val captureState by viewModel.captureState.collectAsStateWithLifecycle()
        val temperatureData by viewModel.temperatureData.collectAsStateWithLifecycle()
        val captureHistory by viewModel.captureHistory.collectAsStateWithLifecycle()
        val deviceConnectionState by viewModel.deviceConnectionState.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Capture status bar
                CaptureStatusBar(
                    captureState = captureState,
                    deviceConnectionState = deviceConnectionState,
                    onToggleCapture = { viewModel.toggleCapture() }
                )
                // Main capture interface
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thermal capture view
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        ThermalCaptureView(
                            temperatureData = temperatureData,
                            deviceConnectionState = deviceConnectionState
                        )
                        // Temperature overlay
                        TemperatureCaptureOverlay(
                            temperatureData = temperatureData,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                        // Capture controls overlay
                        CaptureControlsOverlay(
                            captureState = captureState,
                            onCapture = { viewModel.captureFrame() },
                            onContinuousToggle = { viewModel.toggleContinuousCapture() },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                    // Capture history and controls panel
                    CaptureHistoryPanel(
                        captureState = captureState,
                        captureHistory = captureHistory,
                        onClearHistory = { viewModel.clearCaptureHistory() },
                        onExportCaptures = { viewModel.exportCaptures() },
                        onDeleteCapture = { capture ->
                            viewModel.deleteCapture(capture)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun CaptureStatusBar(
        captureState: CaptureState,
        deviceConnectionState: DeviceConnectionState,
        onToggleCapture: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (captureState) {
                    CaptureState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    CaptureState.CONTINUOUS -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IR Monitor Capture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusChip(
                            text = getConnectionStatusText(deviceConnectionState),
                            color = getConnectionStatusColor(deviceConnectionState)
                        )
                        StatusChip(
                            text = getCaptureStatusText(captureState),
                            color = getCaptureStatusColor(captureState)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = captureState != CaptureState.INACTIVE,
                        onCheckedChange = { onToggleCapture() },
                        enabled = deviceConnectionState == DeviceConnectionState.CONNECTED
                    )
                }
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun ThermalCaptureView(
        temperatureData: TemperatureData?,
        deviceConnectionState: DeviceConnectionState
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (deviceConnectionState == DeviceConnectionState.CONNECTED) {
                    // Temperature view integration for capture
                    AndroidView(
                        factory = { context ->
                            TemperatureView(context).apply {
                                // Configure for capture mode
                                temperatureRegionMode = TemperatureView.REGION_MODE_RECTANGLE
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Connection placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "No connection",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when (deviceConnectionState) {
                                    DeviceConnectionState.DISCONNECTED -> "Device Not Connected"
                                    DeviceConnectionState.CONNECTING -> "Connecting..."
                                    DeviceConnectionState.ERROR -> "Connection Error"
                                    else -> "No Signal"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (deviceConnectionState == DeviceConnectionState.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TemperatureCaptureOverlay(
        temperatureData: TemperatureData?,
        modifier: Modifier = Modifier
    ) {
        temperatureData?.let { data ->
            Column(
                modifier = modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CaptureTemperatureCard(
                    label = "Center",
                    temperature = "${data.centerTemp}Â°C",
                    isMain = true
                )
                CaptureTemperatureCard(
                    label = "Max",
                    temperature = "${data.maxTemp}Â°C",
                    color = Color.Red
                )
                CaptureTemperatureCard(
                    label = "Min",
                    temperature = "${data.minTemp}Â°C",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    @Composable
    private fun CaptureTemperatureCard(
        label: String,
        temperature: String,
        isMain: Boolean = false,
        color: Color = Color.White
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMain)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Text(
                    text = temperature,
                    style = if (isMain)
                        MaterialTheme.typography.titleSmall
                    else
                        MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }

    @Composable
    private fun CaptureControlsOverlay(
        captureState: CaptureState,
        onCapture: () -> Unit,
        onContinuousToggle: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Single capture button
                FloatingActionButton(
                    onClick = onCapture,
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Continuous capture toggle
                FilterChip(
                    onClick = onContinuousToggle,
                    label = {
                        Text(
                            if (captureState == CaptureState.CONTINUOUS) "Stop Auto" else "Auto Capture",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    selected = captureState == CaptureState.CONTINUOUS,
                    leadingIcon = {
                        Icon(
                            if (captureState == CaptureState.CONTINUOUS) Icons.Default.Stop else Icons.Default.Timer,
                            contentDescription = "Continuous Capture",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun CaptureHistoryPanel(
        captureState: CaptureState,
        captureHistory: List<CaptureData>,
        onClearHistory: () -> Unit,
        onExportCaptures: () -> Unit,
        onDeleteCapture: (CaptureData) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Capture History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${captureHistory.size} captures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                // Capture list
                if (captureHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "No captures",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No captures yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(captureHistory) { capture ->
                            CaptureHistoryItem(
                                capture = capture,
                                onDeleteCapture = { onDeleteCapture(capture) }
                            )
                        }
                    }
                }
                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportCaptures,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = captureHistory.isNotEmpty()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export All")
                    }
                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = captureHistory.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear History")
                    }
                }
            }
        }
    }

    @Composable
    private fun CaptureHistoryItem(
        capture: CaptureData,
        onDeleteCapture: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Capture ${capture.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(capture.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${capture.temperature}Â°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDeleteCapture,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Helper functions
    private fun getConnectionStatusText(state: DeviceConnectionState): String = when (state) {
        DeviceConnectionState.CONNECTED -> "Connected"
        DeviceConnectionState.CONNECTING -> "Connecting"
        DeviceConnectionState.DISCONNECTED -> "Disconnected"
        DeviceConnectionState.ERROR -> "Error"
    }

    private fun getConnectionStatusColor(state: DeviceConnectionState): Color = when (state) {
        DeviceConnectionState.CONNECTED -> Color.Green
        DeviceConnectionState.CONNECTING -> Color(0xFFFFA500)
        DeviceConnectionState.DISCONNECTED -> Color.Gray
        DeviceConnectionState.ERROR -> Color.Red
    }

    private fun getCaptureStatusText(state: CaptureState): String = when (state) {
        CaptureState.INACTIVE -> "Inactive"
        CaptureState.ACTIVE -> "Ready"
        CaptureState.CONTINUOUS -> "Auto Capture"
        CaptureState.CAPTURING -> "Capturing"
    }

    @Composable
    private fun getCaptureStatusColor(state: CaptureState): Color = when (state) {
        CaptureState.INACTIVE -> Color.Gray
        CaptureState.ACTIVE -> Color.Green
        CaptureState.CONTINUOUS -> MaterialTheme.colorScheme.primary
        CaptureState.CAPTURING -> Color(0xFFFFA500)
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\IRMonitorHistoryComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorHistoryViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.IRMonitorHistoryViewModel.*
import java.text.SimpleDateFormat
import java.util.*

class IRMonitorHistoryComposeFragment : BaseComposeFragment<IRMonitorHistoryViewModel>() {
    override fun createViewModel(): IRMonitorHistoryViewModel {
        return viewModels<IRMonitorHistoryViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRMonitorHistoryViewModel) {
        // Observe ViewModel state
        val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()
        val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // History header with filter controls
                HistoryHeader(
                    selectedFilter = selectedFilter,
                    totalItems = historyItems.size,
                    onFilterChange = { filter ->
                        viewModel.changeFilter(filter)
                    }
                )
                // Selection toolbar
                if (isSelectionMode) {
                    HistorySelectionToolbar(
                        selectedCount = selectedItems.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onExportSelected = { viewModel.exportSelectedItems() },
                        onDeleteSelected = { viewModel.deleteSelectedItems() }
                    )
                }
                // History content
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            LoadingState()
                        }

                        historyItems.isEmpty() -> {
                            EmptyHistoryState(
                                filter = selectedFilter,
                                onRefresh = { viewModel.refreshHistory() }
                            )
                        }

                        else -> {
                            HistoryList(
                                viewModel = viewModel,
                                historyItems = historyItems,
                                selectedItems = selectedItems,
                                isSelectionMode = isSelectionMode,
                                onItemClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item.id)
                                    } else {
                                        viewModel.viewHistoryDetails(item)
                                    }
                                },
                                onItemLongClick = { item ->
                                    if (!isSelectionMode) {
                                        viewModel.enterSelectionMode()
                                        viewModel.toggleItemSelection(item.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HistoryHeader(
        selectedFilter: HistoryFilter,
        totalItems: Int,
        onFilterChange: (HistoryFilter) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monitor History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalItems sessions recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(
                        Icons.Default.History,
                        contentDescription = "History",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                // Filter chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryFilter.values().forEach { filter ->
                        FilterChip(
                            onClick = { onFilterChange(filter) },
                            label = {
                                Text(
                                    text = filter.displayName,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = selectedFilter == filter,
                            leadingIcon = {
                                Icon(
                                    filter.icon,
                                    contentDescription = filter.displayName,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun HistorySelectionToolbar(
        selectedCount: Int,
        onClearSelection: () -> Unit,
        onExportSelected: () -> Unit,
        onDeleteSelected: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount session${if (selectedCount != 1) "s" else ""} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onExportSelected) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
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
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading history...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun EmptyHistoryState(
        filter: HistoryFilter,
        onRefresh: () -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = "No history",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (filter) {
                        HistoryFilter.ALL -> "No History Found"
                        HistoryFilter.TODAY -> "No Sessions Today"
                        HistoryFilter.WEEK -> "No Sessions This Week"
                        HistoryFilter.MONTH -> "No Sessions This Month"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Monitor thermal sessions to see history here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    private fun HistoryList(
        viewModel: IRMonitorHistoryViewModel,
        historyItems: List<HistoryItem>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        onItemClick: (HistoryItem) -> Unit,
        onItemLongClick: (HistoryItem) -> Unit
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(historyItems) { item ->
                HistoryListItem(
                    viewModel = viewModel,
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }

    @Composable
    private fun HistoryListItem(
        viewModel: IRMonitorHistoryViewModel,
        item: HistoryItem,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Session icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            getSessionTypeColor(item.sessionType).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getSessionTypeIcon(item.sessionType),
                        contentDescription = item.sessionType.name,
                        tint = getSessionTypeColor(item.sessionType),
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Session info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.sessionName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(item.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.sampleCount} samples",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusChip(
                            text = item.sessionType.displayName,
                            color = getSessionTypeColor(item.sessionType)
                        )
                    }
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Temperature summary
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${item.avgTemperature}Â°C",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Average",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Selection indicator or actions
                if (isSelectionMode) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Not Selected",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.viewHistoryDetails(item) }
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "View details"
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }

    // Helper functions
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    private fun getSessionTypeIcon(type: SessionType): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
        SessionType.MONITORING -> Icons.Default.Monitor
        SessionType.CAPTURE -> Icons.Default.CameraAlt
        SessionType.ANALYSIS -> Icons.Default.Analytics
        SessionType.CALIBRATION -> Icons.Default.Tune
    }

    @Composable
    private fun getSessionTypeColor(type: SessionType): Color = when (type) {
        SessionType.MONITORING -> MaterialTheme.colorScheme.primary
        SessionType.CAPTURE -> Color.Green
        SessionType.ANALYSIS -> Color(0xFFFFA500)
        SessionType.CALIBRATION -> Color.Red
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\IRMonitorThermalComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThermostatAuto
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalFragmentViewModel

class IRMonitorThermalComposeFragment : BaseComposeFragment<ThermalFragmentViewModel>() {
    override fun createViewModel(): ThermalFragmentViewModel {
        return viewModels<ThermalFragmentViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalFragmentViewModel) {
        val context = LocalContext.current
        val uiState by viewModel.thermalUiState.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "IR Thermal Monitor",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            IconButton(onClick = { viewModel.showSettings() }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                IRMonitorThermalContent(
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    private fun IRMonitorThermalContent(
        viewModel: ThermalFragmentViewModel,
        uiState: ThermalFragmentViewModel.ThermalMonitoringUiState,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thermal camera view integration
            ThermalCameraSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            // Monitoring controls
            MonitoringControlsSection(
                onStartMonitoring = { viewModel.startMonitoring() },
                onStopMonitoring = { viewModel.stopMonitoring() },
                onConfigureRegions = { viewModel.configureRegions() },
                isMonitoring = uiState.isMonitoring
            )
            // Temperature data display
            TemperatureDataSection(
                currentTemp = uiState.currentTemperature,
                minTemp = uiState.minTemperature,
                maxTemp = uiState.maxTemperature,
                avgTemp = uiState.averageTemperature
            )
            // Monitoring status and alerts
            MonitoringStatusSection(
                isConnected = uiState.isDeviceConnected,
                isRecording = uiState.isRecording,
                alertCount = uiState.alertCount
            )
        }
    }

    @Composable
    private fun ThermalCameraSection(
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Integration with native thermal camera views
                AndroidView(
                    factory = { context: android.content.Context ->
                        // This would integrate with the actual CameraView and TemperatureView
                        // from the legacy implementation
                        FrameLayout(context).apply {
                            // Add CameraView and TemperatureView here
                            // For now, placeholder that shows integration point
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Overlay for camera integration status
                if (true) { // Replace with actual camera status
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ThermostatAuto,
                                contentDescription = "Thermal Camera",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "IR Thermal Monitor View",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                "Integration with CameraView & TemperatureView",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MonitoringControlsSection(
        onStartMonitoring: () -> Unit,
        onStopMonitoring: () -> Unit,
        onConfigureRegions: () -> Unit,
        isMonitoring: Boolean
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Monitoring Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = if (isMonitoring) onStopMonitoring else onStartMonitoring,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonitoring)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (isMonitoring) Icons.Default.RecordVoiceOver else Icons.Default.MonitorHeart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMonitoring) "Stop" else "Start")
                    }
                    OutlinedButton(
                        onClick = onConfigureRegions,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Configure Regions")
                    }
                }
            }
        }
    }

    @Composable
    private fun TemperatureDataSection(
        currentTemp: Float?,
        minTemp: Float?,
        maxTemp: Float?,
        avgTemp: Float?
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Temperature Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TemperatureCard("Current", currentTemp, MaterialTheme.colorScheme.primary)
                    TemperatureCard("Min", minTemp, MaterialTheme.colorScheme.secondary)
                    TemperatureCard("Max", maxTemp, MaterialTheme.colorScheme.error)
                    TemperatureCard("Avg", avgTemp, MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }

    @Composable
    private fun TemperatureCard(
        label: String,
        temperature: Float?,
        color: Color
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
                Text(
                    temperature?.let { "%.1fÂ°C".format(it) } ?: "--",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }

    @Composable
    private fun MonitoringStatusSection(
        isConnected: Boolean,
        isRecording: Boolean,
        alertCount: Int
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusRow(
                    "Device Connected",
                    isConnected,
                    if (isConnected) Color.Green else Color.Red
                )
                StatusRow(
                    "Recording",
                    isRecording,
                    if (isRecording) Color.Red else Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alerts", style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (alertCount > 0)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            alertCount.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (alertCount > 0)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusRow(
        label: String,
        status: Boolean,
        color: Color
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = color.copy(alpha = 0.2f)
            ) {
                Text(
                    if (status) "Active" else "Inactive",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\IRPlushComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.ir.view.TemperatureView
import com.mpdc4gsr.module.thermalunified.viewmodel.IRPlushViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.IRPlushViewModel.*

class IRPlushComposeFragment : BaseComposeFragment<IRPlushViewModel>() {
    override fun createViewModel(): IRPlushViewModel {
        return viewModels<IRPlushViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRPlushViewModel) {
        // Observe ViewModel state
        val dualViewState by viewModel.dualViewState.collectAsStateWithLifecycle()
        val temperatureData by viewModel.temperatureData.collectAsStateWithLifecycle()
        val processingMode by viewModel.processingMode.collectAsStateWithLifecycle()
        val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Status bar with IR Plus features
                IRPlusStatusBar(
                    dualViewState = dualViewState,
                    processingMode = processingMode,
                    isRecording = isRecording,
                    onToggleRecording = { viewModel.toggleRecording() }
                )
                // Main dual-view interface
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dual camera view
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        DualCameraView(
                            dualViewState = dualViewState,
                            onSurfaceReady = { surfaceView ->
                                viewModel.initializeDualView(surfaceView)
                            }
                        )
                        // Temperature overlays
                        TemperatureOverlays(
                            temperatureData = temperatureData,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                        // Plus features overlay
                        PlusFeatureOverlay(
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                    // Controls panel
                    IRPlusControlsPanel(
                        viewModel = viewModel,
                        dualViewState = dualViewState,
                        processingMode = processingMode,
                        onModeChange = { mode ->
                            viewModel.changeProcessingMode(mode)
                        },
                        onCalibrate = { viewModel.calibrateDualView() },
                        onResetSettings = { viewModel.resetSettings() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun IRPlusStatusBar(
        dualViewState: DualViewState,
        processingMode: ProcessingMode,
        isRecording: Boolean,
        onToggleRecording: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (dualViewState) {
                    DualViewState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    DualViewState.CALIBRATING -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IR Plus - Dual Camera Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusChip(
                            text = getDualViewStatusText(dualViewState),
                            color = getDualViewStatusColor(dualViewState)
                        )
                        StatusChip(
                            text = processingMode.name,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (isRecording) {
                            StatusChip(
                                text = "RECORDING",
                                color = Color.Red
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Recording button
                    IconButton(
                        onClick = onToggleRecording,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = "Toggle Recording",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun DualCameraView(
        dualViewState: DualViewState,
        onSurfaceReady: (SurfaceView) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dual surface view for native camera
                AndroidView(
                    factory = { context ->
                        SurfaceView(context).apply {
                            onSurfaceReady(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Temperature view overlay
                AndroidView(
                    factory = { context ->
                        TemperatureView(context).apply {
                            // Configure for dual IR mode
                            productType = com.mpdc4gsr.libunified.ir.usbdual.Const.TYPE_IR_DUAL
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Processing status overlay
                if (dualViewState == DualViewState.CALIBRATING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Calibrating Dual View...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Please keep the camera steady",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TemperatureOverlays(
        temperatureData: TemperatureData?,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            temperatureData?.let { data ->
                TemperatureCard(
                    label = "IR Center",
                    temperature = "${data.irCenterTemp}Â°C",
                    isMain = true
                )
                TemperatureCard(
                    label = "IR Max",
                    temperature = "${data.irMaxTemp}Â°C",
                    color = Color.Red
                )
                TemperatureCard(
                    label = "IR Min",
                    temperature = "${data.irMinTemp}Â°C",
                    color = MaterialTheme.colorScheme.primary
                )
                TemperatureCard(
                    label = "Ambient",
                    temperature = "${data.ambientTemp}Â°C",
                    color = Color.Green
                )
            }
        }
    }

    @Composable
    private fun TemperatureCard(
        label: String,
        temperature: String,
        isMain: Boolean = false,
        color: Color = Color.White
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMain)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Text(
                    text = temperature,
                    style = if (isMain)
                        MaterialTheme.typography.titleSmall
                    else
                        MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }

    @Composable
    private fun PlusFeatureOverlay(
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Plus",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "PLUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    @Composable
    private fun IRPlusControlsPanel(
        viewModel: IRPlushViewModel,
        dualViewState: DualViewState,
        processingMode: ProcessingMode,
        onModeChange: (ProcessingMode) -> Unit,
        onCalibrate: () -> Unit,
        onResetSettings: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "IR Plus Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Processing mode selector
                ProcessingModeSelector(
                    currentMode = processingMode,
                    onModeChange = onModeChange,
                    enabled = dualViewState == DualViewState.ACTIVE
                )
                HorizontalDivider()
                // Calibration section
                CalibrationSection(
                    onCalibrate = onCalibrate,
                    onReset = onResetSettings,
                    enabled = dualViewState != DualViewState.CALIBRATING
                )
                HorizontalDivider()
                // Plus features
                PlusFeaturesSection()
                Spacer(modifier = Modifier.weight(1f))
                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.showAdvancedSettings() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Advanced Settings")
                    }
                }
            }
        }
    }

    @Composable
    private fun ProcessingModeSelector(
        currentMode: ProcessingMode,
        onModeChange: (ProcessingMode) -> Unit,
        enabled: Boolean
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Processing Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            ProcessingMode.values().forEach { mode ->
                FilterChip(
                    onClick = { if (enabled) onModeChange(mode) },
                    label = { Text(mode.displayName) },
                    selected = currentMode == mode,
                    enabled = enabled,
                    leadingIcon = {
                        Icon(
                            mode.icon,
                            contentDescription = mode.displayName,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun CalibrationSection(
        onCalibrate: () -> Unit,
        onReset: () -> Unit,
        enabled: Boolean
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Calibration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCalibrate,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }

    @Composable
    private fun PlusFeaturesSection() {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Plus Features",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            val features = listOf(
                "Dual-IR Processing" to true,
                "Advanced Fusion" to true,
                "Professional Analysis" to false,
                "Enhanced Calibration" to true
            )
            features.forEach { (feature, enabled) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        if (enabled) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = if (enabled) "Enabled" else "Locked",
                        modifier = Modifier.size(16.dp),
                        tint = if (enabled) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Helper functions
    private fun getDualViewStatusText(state: DualViewState): String = when (state) {
        DualViewState.INACTIVE -> "Inactive"
        DualViewState.ACTIVE -> "Active"
        DualViewState.CALIBRATING -> "Calibrating"
        DualViewState.ERROR -> "Error"
    }

    private fun getDualViewStatusColor(state: DualViewState): Color = when (state) {
        DualViewState.INACTIVE -> Color.Gray
        DualViewState.ACTIVE -> Color.Green
        DualViewState.CALIBRATING -> Color(0xFFFFA500)
        DualViewState.ERROR -> Color.Red
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\IRThermalComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.module.thermalunified.activity.IRThermalPlusComposeActivity
import com.mpdc4gsr.module.thermalunified.activity.ThermalIrNightComposeActivity
import com.mpdc4gsr.module.thermalunified.viewmodel.IRThermalFragmentViewModel

class IRThermalComposeFragment : BaseComposeFragment<IRThermalFragmentViewModel>() {
    override fun createViewModel(): IRThermalFragmentViewModel {
        return viewModels<IRThermalFragmentViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: IRThermalFragmentViewModel) {
        val context = LocalContext.current
        // Observe ViewModel state
        val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
        val isTC007 by viewModel.isTC007.collectAsStateWithLifecycle()
        val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header section
                Text(
                    text = "IR Thermal Imaging",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Access advanced thermal imaging capabilities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Connection status card
                ConnectionStatusCard(
                    connectionStatus = connectionStatus,
                    isTC007 = isTC007,
                    deviceInfo = deviceInfo,
                    onRetryConnection = { viewModel.retryConnection() }
                )
                // Main thermal entry point
                if (connectionStatus == IRThermalFragmentViewModel.ConnectionStatus.CONNECTED) {
                    ThermalEntryCard(
                        onOpenThermal = { viewModel.openMainThermal() },
                        onOpenNightVision = {
                            val intent = Intent(context, ThermalIrNightComposeActivity::class.java)
                            context.startActivity(intent)
                        },
                        onOpenThermalPlus = {
                            val intent = Intent(context, IRThermalPlusComposeActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                } else {
                    ConnectionGuideCard(
                        connectionStatus = connectionStatus,
                        isTC007 = isTC007,
                        onConnectDevice = { viewModel.connectDevice() },
                        onOpenSettings = { viewModel.openDeviceSettings() }
                    )
                }
                // Advanced features section
                AdvancedFeaturesSection(
                    onNavigateToFeature = { route ->
                        NavigationManager.getInstance()
                            .build(route)
                            .navigation(context)
                    }
                )
            }
        }
    }

    @Composable
    private fun ConnectionStatusCard(
        connectionStatus: IRThermalFragmentViewModel.ConnectionStatus,
        isTC007: Boolean,
        deviceInfo: String?,
        onRetryConnection: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (connectionStatus) {
                    IRThermalFragmentViewModel.ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                    IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Device Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTC007) "TC007 Device" else "Standard Device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (connectionStatus) {
                                    IRThermalFragmentViewModel.ConnectionStatus.CONNECTED -> Color.Green
                                    IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> Color(0xFFFFA500)
                                    else -> Color.Red
                                },
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
                Text(
                    text = getStatusText(connectionStatus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (connectionStatus) {
                        IRThermalFragmentViewModel.ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                        IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                deviceInfo?.let { info ->
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (connectionStatus != IRThermalFragmentViewModel.ConnectionStatus.CONNECTED) {
                    Button(
                        onClick = onRetryConnection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry Connection")
                    }
                }
            }
        }
    }

    @Composable
    private fun ThermalEntryCard(
        onOpenThermal: () -> Unit,
        onOpenNightVision: () -> Unit,
        onOpenThermalPlus: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Thermal Imaging Modes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                // Main thermal button
                Button(
                    onClick = onOpenThermal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Thermal Camera")
                }
                // Additional mode buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenNightVision,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NightsStay, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Night Vision")
                    }
                    OutlinedButton(
                        onClick = onOpenThermalPlus,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thermal Plus")
                    }
                }
            }
        }
    }

    @Composable
    private fun ConnectionGuideCard(
        connectionStatus: IRThermalFragmentViewModel.ConnectionStatus,
        isTC007: Boolean,
        onConnectDevice: () -> Unit,
        onOpenSettings: () -> Unit
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Connection Required",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (connectionStatus) {
                        IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> "Connecting to Device..."
                        else -> "Device Not Connected"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isTC007) {
                        "Connect your TC007 thermal imaging device to access advanced thermal features"
                    } else {
                        "Connect your thermal imaging device to start capturing thermal data"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (connectionStatus != IRThermalFragmentViewModel.ConnectionStatus.CONNECTING) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConnectDevice,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Cable, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Device")
                        }
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Device Settings")
                        }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }

    @Composable
    private fun AdvancedFeaturesSection(
        onNavigateToFeature: (String) -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Advanced Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureCard(
                    title = "Gallery",
                    description = "View thermal images",
                    icon = Icons.Default.PhotoLibrary,
                    onClick = { onNavigateToFeature(RouterConfig.IR_GALLERY_HOME) },
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    title = "Settings",
                    description = "Configure thermal",
                    icon = Icons.Default.Settings,
                    onClick = { onNavigateToFeature(RouterConfig.THERMAL_SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    private fun FeatureCard(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    private fun getStatusText(status: IRThermalFragmentViewModel.ConnectionStatus): String = when (status) {
        IRThermalFragmentViewModel.ConnectionStatus.CONNECTED -> "Device connected and ready"
        IRThermalFragmentViewModel.ConnectionStatus.CONNECTING -> "Connecting to device..."
        IRThermalFragmentViewModel.ConnectionStatus.DISCONNECTED -> "Device not connected"
        IRThermalFragmentViewModel.ConnectionStatus.ERROR -> "Connection error - check device"
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\MonitorThermalComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.stubs.FenceLineView
import com.mpdc4gsr.module.thermalunified.stubs.FencePointView
import com.mpdc4gsr.module.thermalunified.stubs.FenceView
import com.mpdc4gsr.module.thermalunified.stubs.IrSurfaceView
import com.mpdc4gsr.module.thermalunified.viewmodel.MonitorThermalViewModel
import com.mpdc4gsr.module.thermalunified.viewmodel.MonitorThermalViewModel.*
import java.text.SimpleDateFormat
import java.util.*

class MonitorThermalComposeFragment : BaseComposeFragment<MonitorThermalViewModel>() {
    override fun createViewModel(): MonitorThermalViewModel {
        return viewModels<MonitorThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: MonitorThermalViewModel) {
        // Observe ViewModel state
        val monitoringState by viewModel.monitoringState.collectAsStateWithLifecycle()
        val thermalData by viewModel.thermalData.collectAsStateWithLifecycle()
        val recordingStatus by viewModel.recordingStatus.collectAsStateWithLifecycle()
        val monitoringAlerts by viewModel.monitoringAlerts.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Monitoring status bar
                MonitoringStatusBar(
                    monitoringState = monitoringState,
                    recordingStatus = recordingStatus,
                    onToggleMonitoring = { viewModel.toggleMonitoring() },
                    onToggleRecording = { viewModel.toggleRecording() }
                )
                // Main monitoring interface
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thermal monitoring view
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    ) {
                        ThermalMonitorView(
                            thermalData = thermalData,
                            onFenceUpdate = { fence ->
                                viewModel.updateMonitoringFence(fence)
                            }
                        )
                        // Monitoring overlays
                        MonitoringOverlays(
                            thermalData = thermalData,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                        // Alert notifications
                        AlertNotifications(
                            alerts = monitoringAlerts,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                    // Monitoring controls and data panel
                    MonitoringControlsPanel(
                        monitoringState = monitoringState,
                        thermalData = thermalData,
                        onThresholdChange = { threshold ->
                            viewModel.updateTemperatureThreshold(threshold)
                        },
                        onAlertSettingsChange = { settings ->
                            viewModel.updateAlertSettings(settings)
                        },
                        onExportData = { viewModel.exportMonitoringData() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun MonitoringStatusBar(
        monitoringState: MonitoringState,
        recordingStatus: RecordingStatus,
        onToggleMonitoring: () -> Unit,
        onToggleRecording: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (monitoringState) {
                    MonitoringState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    MonitoringState.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Thermal Monitoring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusChip(
                            text = getMonitoringStatusText(monitoringState),
                            color = getMonitoringStatusColor(monitoringState)
                        )
                        if (recordingStatus == RecordingStatus.RECORDING) {
                            StatusChip(
                                text = "Recording",
                                color = Color.Red
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Recording toggle
                    IconButton(
                        onClick = onToggleRecording,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (recordingStatus == RecordingStatus.RECORDING)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (recordingStatus == RecordingStatus.RECORDING)
                                Icons.Default.Stop
                            else
                                Icons.Default.FiberManualRecord,
                            contentDescription = "Toggle Recording",
                            tint = Color.White
                        )
                    }
                    // Monitoring toggle
                    Switch(
                        checked = monitoringState == MonitoringState.ACTIVE,
                        onCheckedChange = { onToggleMonitoring() }
                    )
                }
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun ThermalMonitorView(
        thermalData: ThermalData?,
        onFenceUpdate: (FenceData) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main thermal surface view
                AndroidView(
                    factory = { context ->
                        IrSurfaceView(context).apply {
                            // Configure surface view for monitoring
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Fence overlays for monitoring regions
                AndroidView(
                    factory = { context ->
                        FenceView(context).apply {
                            listener = object : FenceView.CallBack {
                                override fun callback(
                                    startPoint: IntArray,
                                    endPoint: IntArray,
                                    srcRect: IntArray
                                ) {
                                    // Convert fence callback data to FenceData and notify viewmodel
                                    val fenceData =
                                        "start:${startPoint.contentToString()},end:${endPoint.contentToString()},rect:${srcRect.contentToString()}"
                                    onFenceUpdate(FenceData(fenceData))
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { context ->
                        FencePointView(context)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { context ->
                        FenceLineView(context)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    @Composable
    private fun MonitoringOverlays(
        thermalData: ThermalData?,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            thermalData?.let { data ->
                TemperatureMonitorCard(
                    label = "Current",
                    temperature = "${data.currentTemp}Â°C",
                    isAlarm = data.isAlarmTriggered,
                    isMain = true
                )
                TemperatureMonitorCard(
                    label = "Max",
                    temperature = "${data.maxTemp}Â°C",
                    color = Color.Red
                )
                TemperatureMonitorCard(
                    label = "Min",
                    temperature = "${data.minTemp}Â°C",
                    color = MaterialTheme.colorScheme.primary
                )
                TemperatureMonitorCard(
                    label = "Avg",
                    temperature = "${data.avgTemp}Â°C",
                    color = Color.Gray
                )
            }
        }
    }

    @Composable
    private fun TemperatureMonitorCard(
        label: String,
        temperature: String,
        isAlarm: Boolean = false,
        isMain: Boolean = false,
        color: Color = Color.White
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isAlarm -> MaterialTheme.colorScheme.errorContainer
                    isMain -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                }
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAlarm) MaterialTheme.colorScheme.onErrorContainer else color
                )
                Text(
                    text = temperature,
                    style = if (isMain)
                        MaterialTheme.typography.titleSmall
                    else
                        MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlarm) MaterialTheme.colorScheme.onErrorContainer else color
                )
            }
        }
    }

    @Composable
    private fun AlertNotifications(
        alerts: List<MonitoringAlert>,
        modifier: Modifier = Modifier
    ) {
        if (alerts.isNotEmpty()) {
            LazyColumn(
                modifier = modifier
                    .width(300.dp)
                    .heightIn(max = 200.dp)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(alerts) { alert ->
                    AlertCard(alert = alert)
                }
            }
        }
    }

    @Composable
    private fun AlertCard(alert: MonitoringAlert) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (alert.severity) {
                    AlertSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                    AlertSeverity.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                    AlertSeverity.LOW -> MaterialTheme.colorScheme.tertiaryContainer
                }
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (alert.severity) {
                        AlertSeverity.HIGH -> Icons.Default.Error
                        AlertSeverity.MEDIUM -> Icons.Default.Warning
                        AlertSeverity.LOW -> Icons.Default.Info
                    },
                    contentDescription = "Alert",
                    modifier = Modifier.size(16.dp),
                    tint = when (alert.severity) {
                        AlertSeverity.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                        AlertSeverity.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
                        AlertSeverity.LOW -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(alert.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun MonitoringControlsPanel(
        monitoringState: MonitoringState,
        thermalData: ThermalData?,
        onThresholdChange: (TemperatureThreshold) -> Unit,
        onAlertSettingsChange: (AlertSettings) -> Unit,
        onExportData: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Monitoring Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    ThresholdControls(
                        onThresholdChange = onThresholdChange,
                        enabled = monitoringState == MonitoringState.ACTIVE
                    )
                }
                item {
                    HorizontalDivider()
                }
                item {
                    AlertSettingsSection(
                        onAlertSettingsChange = onAlertSettingsChange
                    )
                }
                item {
                    HorizontalDivider()
                }
                item {
                    MonitoringDataSection(
                        thermalData = thermalData
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Button(
                        onClick = onExportData,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Data")
                    }
                }
            }
        }
    }

    @Composable
    private fun ThresholdControls(
        onThresholdChange: (TemperatureThreshold) -> Unit,
        enabled: Boolean
    ) {
        var highThreshold by remember { mutableFloatStateOf(50f) }
        var lowThreshold by remember { mutableFloatStateOf(10f) }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Temperature Thresholds",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            // High threshold slider
            Text(
                text = "High Alert: ${String.format("%.1f", highThreshold)}Â°C",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = highThreshold,
                onValueChange = {
                    highThreshold = it
                    onThresholdChange(TemperatureThreshold(it, lowThreshold))
                },
                valueRange = 20f..100f,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
            // Low threshold slider
            Text(
                text = "Low Alert: ${String.format("%.1f", lowThreshold)}Â°C",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = lowThreshold,
                onValueChange = {
                    lowThreshold = it
                    onThresholdChange(TemperatureThreshold(highThreshold, it))
                },
                valueRange = -20f..40f,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun AlertSettingsSection(
        onAlertSettingsChange: (AlertSettings) -> Unit
    ) {
        var enableSound by remember { mutableStateOf(true) }
        var enableVibration by remember { mutableStateOf(true) }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Alert Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound Alerts", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = enableSound,
                    onCheckedChange = {
                        enableSound = it
                        onAlertSettingsChange(AlertSettings(it, enableVibration))
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibration", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = enableVibration,
                    onCheckedChange = {
                        enableVibration = it
                        onAlertSettingsChange(AlertSettings(enableSound, it))
                    }
                )
            }
        }
    }

    @Composable
    private fun MonitoringDataSection(
        thermalData: ThermalData?
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Session Data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            thermalData?.let { data ->
                DataRow("Duration", data.sessionDuration)
                DataRow("Samples", "${data.sampleCount}")
                DataRow("Alerts", "${data.alertCount}")
                DataRow("Data Size", data.dataSize)
            }
        }
    }

    @Composable
    private fun DataRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // Helper functions
    private fun getMonitoringStatusText(state: MonitoringState): String = when (state) {
        MonitoringState.ACTIVE -> "Active"
        MonitoringState.PAUSED -> "Paused"
        MonitoringState.STOPPED -> "Stopped"
    }

    private fun getMonitoringStatusColor(state: MonitoringState): Color = when (state) {
        MonitoringState.ACTIVE -> Color.Green
        MonitoringState.PAUSED -> Color(0xFFFFA500)
        MonitoringState.STOPPED -> Color.Gray
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\PDFListComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.PDFListViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Type alias for cleaner code
typealias PDFItem = PDFListViewModel.PDFItem

class PDFListComposeFragment : BaseComposeFragment<PDFListViewModel>() {
    private var isTC007 by mutableStateOf(false)
    override fun createViewModel(): PDFListViewModel {
        return viewModels<PDFListViewModel>().value
    }

    companion object {
        fun newInstance(isTC007: Boolean): PDFListComposeFragment {
            return PDFListComposeFragment().apply {
                this.isTC007 = isTC007
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: PDFListViewModel) {
        val context = LocalContext.current
        // Observe ViewModel state
        val pdfItems by viewModel.pdfItems.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
        val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
        LibUnifiedTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with device type indicator
                PDFListHeader(
                    isTC007 = isTC007,
                    totalReports = pdfItems.size
                )
                // Selection toolbar
                if (isSelectionMode) {
                    PDFSelectionToolbar(
                        selectedCount = selectedItems.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onShareSelected = {
                            shareSelectedPDFs(context, selectedItems.toList())
                        },
                        onDeleteSelected = { viewModel.deleteSelectedItems() },
                        onExportSelected = {
                            exportSelectedPDFs(context, selectedItems.toList())
                        }
                    )
                }
                // PDF list content
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            LoadingState()
                        }

                        pdfItems.isEmpty() -> {
                            EmptyPDFState(
                                onRefresh = { viewModel.refreshPDFList() }
                            )
                        }

                        else -> {
                            PDFList(
                                viewModel = viewModel,
                                pdfs = pdfItems,
                                selectedItems = selectedItems,
                                isSelectionMode = isSelectionMode,
                                onItemClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item.path)
                                    } else {
                                        openPDF(context, item.path)
                                    }
                                },
                                onItemLongClick = { item ->
                                    if (!isSelectionMode) {
                                        viewModel.enterSelectionMode()
                                        viewModel.toggleItemSelection(item.path)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PDFListHeader(
        isTC007: Boolean,
        totalReports: Int
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Analysis Reports",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(
                            text = if (isTC007) "TC007 Device" else "Standard Device",
                            color = if (isTC007) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$totalReports reports",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = "PDF Reports",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    @Composable
    private fun StatusChip(
        text: String,
        color: androidx.compose.ui.graphics.Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun PDFSelectionToolbar(
        selectedCount: Int,
        onClearSelection: () -> Unit,
        onShareSelected: () -> Unit,
        onDeleteSelected: () -> Unit,
        onExportSelected: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount report${if (selectedCount != 1) "s" else ""} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            tint = MaterialTheme.colorScheme.error
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
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading reports...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun EmptyPDFState(
        onRefresh: () -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = "No reports",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No Reports Found",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Generate thermal analysis reports to see them here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    private fun PDFList(
        viewModel: PDFListViewModel,
        pdfs: List<PDFItem>,
        selectedItems: Set<String>,
        isSelectionMode: Boolean,
        onItemClick: (PDFItem) -> Unit,
        onItemLongClick: (PDFItem) -> Unit
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pdfs) { item ->
                PDFListItem(
                    viewModel = viewModel,
                    item = item,
                    isSelected = selectedItems.contains(item.path),
                    isSelectionMode = isSelectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }

    @Composable
    private fun PDFListItem(
        viewModel: PDFListViewModel,
        item: PDFItem,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PDF icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // PDF info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatFileSize(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.isAnalysisReport) {
                            StatusChip(
                                text = "ANALYSIS",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Text(
                        text = SimpleDateFormat(
                            "MMM dd, yyyy HH:mm",
                            Locale.getDefault()
                        ).format(Date(item.dateModified)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Selection indicator or actions
                if (isSelectionMode) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Not Selected",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.showMoreActions(item) }
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More actions"
                        )
                    }
                }
            }
        }
    }

    // Helper functions
    private fun openPDF(context: android.content.Context, path: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path)
                    ),
                    "application/pdf"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - maybe show a toast or use internal PDF viewer
        }
    }

    private fun shareSelectedPDFs(context: android.content.Context, selectedPaths: List<String>) {
        try {
            val uris = selectedPaths.map { path ->
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(path)
                )
            }
            val intent = Intent().apply {
                if (uris.size == 1) {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                } else {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                }
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Reports"))
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun exportSelectedPDFs(context: android.content.Context, selectedPaths: List<String>) {
        // Implementation for exporting PDFs to external storage
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\fragment\ThermalComposeFragment.kt =====

package com.mpdc4gsr.module.thermalunified.fragment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeFragment
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.matrix.IrSurfaceView
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalFragmentViewModel

class ThermalComposeFragment : BaseComposeFragment<ThermalFragmentViewModel>() {
    override fun createViewModel(): ThermalFragmentViewModel {
        return viewModels<ThermalFragmentViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalFragmentViewModel) {
        LibUnifiedTheme {
            // Observe thermal data from ViewModel
            val temperatureData by viewModel.temperatureData.collectAsStateWithLifecycle()
            val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
            val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
            val processingMode by viewModel.processingMode.collectAsStateWithLifecycle()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Status bar with connection and recording status
                StatusBar(
                    connectionStatus = connectionStatus,
                    isRecording = isRecording,
                    processingMode = processingMode
                )
                // Main thermal camera view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    ThermalCameraView(
                        modifier = Modifier.fillMaxSize(),
                        onSurfaceReady = { surfaceView ->
                            viewModel.initializeThermalCamera(surfaceView)
                        }
                    )
                    // Temperature overlays
                    TemperatureOverlays(
                        temperatureData = temperatureData,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
                // Control panel
                ControlPanel(
                    isRecording = isRecording,
                    onCapturePhoto = { viewModel.capturePhoto() },
                    onToggleRecording = { viewModel.toggleRecording() },
                    onOpenSettings = { viewModel.openSettings() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }

    @Composable
    private fun StatusBar(
        connectionStatus: String,
        isRecording: Boolean,
        processingMode: String
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isRecording -> MaterialTheme.colorScheme.errorContainer
                    connectionStatus == "Connected" -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TC001 Thermal Camera",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (connectionStatus) {
                            "Connected" -> Color.Green
                            "Connecting" -> Color(0xFFFFA500)
                            else -> Color.Red
                        }
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (isRecording) {
                        Text(
                            text = "RECORDING",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Mode: $processingMode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun ThermalCameraView(
        modifier: Modifier = Modifier,
        onSurfaceReady: (IrSurfaceView) -> Unit
    ) {
        AndroidView(
            factory = { context ->
                IrSurfaceView(context).apply {
                    onSurfaceReady(this)
                }
            },
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        )
    }

    @Composable
    private fun TemperatureOverlays(
        temperatureData: ThermalFragmentViewModel.TemperatureData?,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            temperatureData?.let { data ->
                // Center temperature
                TemperatureCard(
                    label = "Center",
                    temperature = data.centerTemp,
                    isMain = true
                )
                // Max temperature
                TemperatureCard(
                    label = "Max",
                    temperature = data.maxTemp,
                    color = Color.Red
                )
                // Min temperature
                TemperatureCard(
                    label = "Min",
                    temperature = data.minTemp,
                    color = MaterialTheme.colorScheme.primary
                )
            } ?: run {
                // Placeholder when no data
                TemperatureCard(
                    label = "Center",
                    temperature = "--Â°C",
                    isMain = true
                )
            }
        }
    }

    @Composable
    private fun TemperatureCard(
        label: String,
        temperature: String,
        isMain: Boolean = false,
        color: Color = Color.White
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMain)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = if (isMain)
                        MaterialTheme.typography.labelMedium
                    else
                        MaterialTheme.typography.labelSmall,
                    color = color
                )
                Text(
                    text = temperature,
                    style = if (isMain)
                        MaterialTheme.typography.titleMedium
                    else
                        MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }

    @Composable
    private fun ControlPanel(
        isRecording: Boolean,
        onCapturePhoto: () -> Unit,
        onToggleRecording: () -> Unit,
        onOpenSettings: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Capture photo button
                Button(
                    onClick = onCapturePhoto,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Capture")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Capture")
                }
                // Recording toggle button
                Button(
                    onClick = onToggleRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Record")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRecording) "Stop" else "Record")
                }
                // Settings button
                OutlinedButton(
                    onClick = onOpenSettings
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings")
                }
            }
        }
    }
}