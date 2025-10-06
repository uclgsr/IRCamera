package com.mpdc4gsr.module.thermalunified.activity
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
class IRGalleryHomeComposeActivity : BaseComposeActivity<ThermalViewModel>() {
    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var selectedView by remember { mutableStateOf("grid") }
        var sortBy by remember { mutableStateOf("date") }
        var filterBy by remember { mutableStateOf("all") }
        val galleryItems = remember { getGalleryItems() }
        var showSearchDialog by remember { mutableStateOf(false) }
        var showMoreOptionsDialog by remember { mutableStateOf(false) }
        var selectedItemForOptions by remember { mutableStateOf<GalleryItem?>(null) }
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Gallery",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSearchDialog = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                selectedView = if (selectedView == "grid") "list" else "grid"
                            }) {
                                Icon(
                                    if (selectedView == "grid") Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF16131E)
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            startActivity(Intent(this, ImagePickIRComposeActivity::class.java))
                        },
                        containerColor = Color(0xFFFF6B35)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Capture",
                            tint = Color.White
                        )
                    }
                },
                containerColor = Color(0xFF16131E)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF16131E))
                ) {
                    // Filter and sort controls
                    GalleryControls(
                        sortBy = sortBy,
                        filterBy = filterBy,
                        onSortChange = { sortBy = it },
                        onFilterChange = { filterBy = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Gallery grid/list
                    if (selectedView == "grid") {
                        GalleryGrid(
                            items = galleryItems,
                            modifier = Modifier.fillMaxSize(),
                            onItemClick = { item ->
                                val intent = Intent(
                                    this@IRGalleryHomeComposeActivity,
                                    IRGalleryDetail01ComposeActivity::class.java
                                )
                                intent.putExtra("item_id", item.id)
                                startActivity(intent)
                            },
                            onItemMoreClick = { item ->
                                selectedItemForOptions = item
                                showMoreOptionsDialog = true
                            }
                        )
                    } else {
                        GalleryList(
                            items = galleryItems,
                            modifier = Modifier.fillMaxSize(),
                            onItemClick = { item ->
                                val intent = Intent(
                                    this@IRGalleryHomeComposeActivity,
                                    IRGalleryDetail01ComposeActivity::class.java
                                )
                                intent.putExtra("item_id", item.id)
                                startActivity(intent)
                            },
                            onItemMoreClick = { item ->
                                selectedItemForOptions = item
                                showMoreOptionsDialog = true
                            }
                        )
                    }
                }
            }
            // Search Dialog
            if (showSearchDialog) {
                GallerySearchDialog(
                    onDismiss = { showSearchDialog = false },
                    onSearch = { query ->
                        // TODO: Apply search filter
                        showSearchDialog = false
                    }
                )
            }
            // More Options Dialog
            if (showMoreOptionsDialog && selectedItemForOptions != null) {
                GalleryItemOptionsDialog(
                    item = selectedItemForOptions!!,
                    onDismiss = {
                        showMoreOptionsDialog = false
                        selectedItemForOptions = null
                    },
                    onShare = {
                        // TODO: Share item
                        showMoreOptionsDialog = false
                    },
                    onDelete = {
                        // TODO: Delete item
                        showMoreOptionsDialog = false
                    },
                    onExport = {
                        // TODO: Export item
                        showMoreOptionsDialog = false
                    }
                )
            }
        }
    }
}
@Composable
private fun GalleryControls(
    sortBy: String,
    filterBy: String,
    onSortChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sort options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sort:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                val sortOptions = listOf("date", "name", "size", "temperature")
                sortOptions.forEach { option ->
                    FilterChip(
                        onClick = { onSortChange(option) },
                        label = { Text(option.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                        selected = sortBy == option,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF16131E),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
            // Filter options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filter:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                val filterOptions = listOf("all", "images", "videos", "recent")
                filterOptions.forEach { option ->
                    FilterChip(
                        onClick = { onFilterChange(option) },
                        label = { Text(option.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                        selected = filterBy == option,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF16131E),
                            labelColor = Color(0xFF7D8590)
                        )
                    )
                }
            }
        }
    }
}
@Composable
private fun GalleryGrid(
    items: List<GalleryItem>,
    modifier: Modifier = Modifier,
    onItemClick: (GalleryItem) -> Unit = {},
    onItemMoreClick: (GalleryItem) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            GalleryGridItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}
@Composable
private fun GalleryGridItem(
    item: GalleryItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Color(0xFF16131E),
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.isVideo) Icons.Default.PlayCircle else Icons.Default.Image,
                    contentDescription = if (item.isVideo) "Video" else "Image",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(32.dp)
                )
                // Temperature overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${item.maxTemp}°C",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Item info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    item.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    item.date,
                    color = Color(0xFF7D8590),
                    fontSize = 10.sp
                )
                Text(
                    item.size,
                    color = Color(0xFF7D8590),
                    fontSize = 10.sp
                )
            }
        }
    }
}
@Composable
private fun GalleryList(
    items: List<GalleryItem>,
    modifier: Modifier = Modifier,
    onItemClick: (GalleryItem) -> Unit = {},
    onItemMoreClick: (GalleryItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            GalleryListItem(
                item = item,
                onClick = { onItemClick(item) },
                onMoreClick = { onItemMoreClick(item) }
            )
        }
    }
}
@Composable
private fun GalleryListItem(
    item: GalleryItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Color(0xFF16131E),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.isVideo) Icons.Default.PlayCircle else Icons.Default.Image,
                    contentDescription = if (item.isVideo) "Video" else "Image",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(24.dp)
                )
            }
            // Item details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    item.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    item.date,
                    color = Color(0xFF7D8590),
                    fontSize = 12.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        item.size,
                        color = Color(0xFF7D8590),
                        fontSize = 11.sp
                    )
                    Text(
                        "Max: ${item.maxTemp}°C",
                        color = Color(0xFFFF6B35),
                        fontSize = 11.sp
                    )
                }
            }
            // Actions
            IconButton(
                onClick = onMoreClick
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color(0xFF7D8590)
                )
            }
        }
    }
}
// Data classes
data class GalleryItem(
    val id: String,
    val name: String,
    val date: String,
    val size: String,
    val maxTemp: Float,
    val isVideo: Boolean
)
@Composable
private fun GallerySearchDialog(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Gallery", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search by name or date") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6B35),
                        unfocusedBorderColor = Color(0xFF7D8590)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSearch(searchText.text) }
            ) {
                Text("Search", color = Color(0xFFFF6B35))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF7D8590))
            }
        },
        containerColor = Color(0xFF21262D)
    )
}
@Composable
private fun GalleryItemOptionsDialog(
    item: GalleryItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Image Options", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${item.name}", color = Color(0xFF7D8590), fontSize = 14.sp)
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, "Share", tint = Color.White)
                        Text("Share", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, "Export", tint = Color.White)
                        Text("Export", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF6B35))
                        Text("Delete", color = Color(0xFFFF6B35))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF7D8590))
            }
        },
        containerColor = Color(0xFF21262D)
    )
}
private fun getGalleryItems(): List<GalleryItem> {
    return listOf(
        GalleryItem("1", "thermal_001.jpg", "2024-01-15 14:30", "2.1 MB", 45.2f, false),
        GalleryItem("2", "thermal_002.mp4", "2024-01-15 14:25", "15.3 MB", 38.7f, true),
        GalleryItem("3", "thermal_003.jpg", "2024-01-15 14:20", "1.8 MB", 52.1f, false),
        GalleryItem("4", "thermal_004.jpg", "2024-01-15 14:15", "2.3 MB", 41.5f, false),
        GalleryItem("5", "thermal_005.mp4", "2024-01-15 14:10", "22.1 MB", 47.8f, true),
        GalleryItem("6", "thermal_006.jpg", "2024-01-15 14:05", "1.9 MB", 35.2f, false),
        GalleryItem("7", "thermal_007.jpg", "2024-01-15 14:00", "2.0 MB", 49.3f, false),
        GalleryItem("8", "thermal_008.mp4", "2024-01-15 13:55", "18.7 MB", 43.6f, true)
    )
}