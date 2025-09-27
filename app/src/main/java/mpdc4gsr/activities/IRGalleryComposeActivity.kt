package mpdc4gsr.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.ui.components.CommonComponents
import mpdc4gsr.ui.theme.IRCameraTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compose version of IRGalleryEditActivity demonstrating media gallery with thermal images.
 * Shows how to handle image display, filtering, and media management in Compose.
 */
class IRGalleryComposeActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "IRGalleryComposeActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, IRGalleryComposeActivity::class.java))
        }
    }

    @Composable
    override fun Content() {
        IRCameraTheme {
            IRGalleryScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun IRGalleryScreen() {
        var isGridView by remember { mutableStateOf(true) }
        var selectedFilter by remember { mutableStateOf("All") }
        var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
        var searchQuery by remember { mutableStateOf("") }

        // Load media items
        LaunchedEffect(Unit) {
            loadMediaItems { items ->
                mediaItems = items
                isLoading = false
            }
        }

        // Filter items based on search and filter
        val filteredItems = mediaItems.filter { item ->
            val matchesSearch = searchQuery.isEmpty() || 
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.location.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Thermal" -> item.type == MediaType.THERMAL
                "RGB" -> item.type == MediaType.RGB
                "GSR" -> item.type == MediaType.GSR_DATA
                "Recent" -> (System.currentTimeMillis() - item.timestamp) < 24 * 60 * 60 * 1000 // Last 24 hours
                else -> true
            }
            
            matchesSearch && matchesFilter
        }

        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "IR Gallery",
                    onNavigationClick = { finish() },
                    actions = {
                        IconButton(
                            onClick = { isGridView = !isGridView }
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = if (isGridView) "List View" else "Grid View",
                                tint = Color.White
                            )
                        }
                        
                        if (selectedItems.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    deleteSelectedItems(selectedItems.toList()) {
                                        mediaItems = mediaItems.filter { it.id !in selectedItems }
                                        selectedItems = emptySet()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
                    .padding(paddingValues)
            ) {
                // Search and Filter Bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A2A2A)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search media...") },
                            placeholder = { Text("Enter filename or location") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF6B35FF),
                                unfocusedLabelColor = Color(0x80FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filter Chips
                        val filters = listOf("All", "Thermal", "RGB", "GSR", "Recent")
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            items(filters) { filter ->
                                FilterChip(
                                    onClick = { selectedFilter = filter },
                                    label = { 
                                        Text(
                                            text = filter,
                                            fontSize = 12.sp
                                        ) 
                                    },
                                    selected = selectedFilter == filter,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF6B35FF),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Media Count and Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredItems.size} items${if (selectedItems.isNotEmpty()) " (${selectedItems.size} selected)" else ""}",
                        color = Color(0x80FFFFFF),
                        fontSize = 14.sp
                    )
                    
                    if (selectedItems.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedItems = emptySet() }
                        ) {
                            Text(
                                text = "Clear Selection",
                                color = Color(0xFF6B35FF),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Content
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CommonComponents.LoadingIndicator()
                    }
                } else if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ImageNotSupported,
                                contentDescription = "No Media",
                                tint = Color(0x80FFFFFF),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No media found",
                                color = Color(0x80FFFFFF),
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try adjusting your search" else "Start recording to see media here",
                                color = Color(0x60FFFFFF),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    if (isGridView) {
                        MediaGridView(
                            items = filteredItems,
                            selectedItems = selectedItems,
                            onItemClick = { item -> openMediaItem(item) },
                            onItemLongClick = { item ->
                                selectedItems = if (item.id in selectedItems) {
                                    selectedItems - item.id
                                } else {
                                    selectedItems + item.id
                                }
                            }
                        )
                    } else {
                        MediaListView(
                            items = filteredItems,
                            selectedItems = selectedItems,
                            onItemClick = { item -> openMediaItem(item) },
                            onItemLongClick = { item ->
                                selectedItems = if (item.id in selectedItems) {
                                    selectedItems - item.id
                                } else {
                                    selectedItems + item.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MediaGridView(
        items: List<MediaItem>,
        selectedItems: Set<String>,
        onItemClick: (MediaItem) -> Unit,
        onItemLongClick: (MediaItem) -> Unit
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                MediaGridItem(
                    item = item,
                    isSelected = item.id in selectedItems,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }

    @Composable
    private fun MediaListView(
        items: List<MediaItem>,
        selectedItems: Set<String>,
        onItemClick: (MediaItem) -> Unit,
        onItemLongClick: (MediaItem) -> Unit
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                MediaListItem(
                    item = item,
                    isSelected = item.id in selectedItems,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }

    @Composable
    private fun MediaGridItem(
        item: MediaItem,
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .clickable { onClick() }
                .clickable { onLongClick() }, // Long click simulation
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF6B35FF) else Color(0xFF2A2A2A)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = getMediaIcon(item.type),
                        contentDescription = item.type.name,
                        tint = if (isSelected) Color.White else Color(0xFF6B35FF),
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = item.name,
                        color = if (isSelected) Color.White else Color(0xCCFFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = formatFileSize(item.sizeBytes),
                        color = if (isSelected) Color(0xCCFFFFFF) else Color(0x80FFFFFF),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun MediaListItem(
        item: MediaItem,
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .clickable { onLongClick() }, // Long click simulation
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF6B35FF) else Color(0xFF2A2A2A)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getMediaIcon(item.type),
                    contentDescription = item.type.name,
                    tint = if (isSelected) Color.White else Color(0xFF6B35FF),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = if (isSelected) Color.White else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "${item.location} • ${formatFileSize(item.sizeBytes)}",
                        color = if (isSelected) Color(0xCCFFFFFF) else Color(0x80FFFFFF),
                        fontSize = 12.sp
                    )
                    
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(Date(item.timestamp)),
                        color = if (isSelected) Color(0x80FFFFFF) else Color(0x60FFFFFF),
                        fontSize = 10.sp
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = Color(0x80FFFFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    private fun getMediaIcon(type: MediaType): ImageVector {
        return when (type) {
            MediaType.THERMAL -> Icons.Default.Thermostat
            MediaType.RGB -> Icons.Default.Camera
            MediaType.GSR_DATA -> Icons.Default.ShowChart
            MediaType.SESSION -> Icons.Default.Folder
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun loadMediaItems(onResult: (List<MediaItem>) -> Unit) {
        lifecycleScope.launch {
            try {
                delay(1000) // Simulate loading
                
                val mockItems = listOf(
                    MediaItem(
                        id = "thermal_001",
                        name = "thermal_session_001.jpg",
                        type = MediaType.THERMAL,
                        location = "/thermal/2024/01/",
                        sizeBytes = 2458240, // 2.4 MB
                        timestamp = System.currentTimeMillis() - 3600000 // 1 hour ago
                    ),
                    MediaItem(
                        id = "rgb_001",
                        name = "rgb_capture_001.mp4",
                        type = MediaType.RGB,
                        location = "/rgb/2024/01/",
                        sizeBytes = 15728640, // 15 MB
                        timestamp = System.currentTimeMillis() - 7200000 // 2 hours ago
                    ),
                    MediaItem(
                        id = "gsr_001",
                        name = "gsr_data_001.csv",
                        type = MediaType.GSR_DATA,
                        location = "/gsr/2024/01/",
                        sizeBytes = 524288, // 512 KB
                        timestamp = System.currentTimeMillis() - 1800000 // 30 min ago
                    ),
                    MediaItem(
                        id = "session_001",
                        name = "session_001",
                        type = MediaType.SESSION,
                        location = "/sessions/2024/01/",
                        sizeBytes = 52428800, // 50 MB
                        timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
                    )
                )
                
                onResult(mockItems)
            } catch (e: Exception) {
                onResult(emptyList())
                showToast("Failed to load media: ${e.message}")
            }
        }
    }

    private fun openMediaItem(item: MediaItem) {
        showToast("Opening ${item.name}")
        // Navigate to appropriate viewer based on media type
    }

    private fun deleteSelectedItems(itemIds: List<String>, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                // Delete logic would go here
                delay(500)
                onComplete()
                showToast("${itemIds.size} item(s) deleted")
            } catch (e: Exception) {
                showToast("Failed to delete items: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private data class MediaItem(
        val id: String,
        val name: String,
        val type: MediaType,
        val location: String,
        val sizeBytes: Long,
        val timestamp: Long
    )

    private enum class MediaType {
        THERMAL, RGB, GSR_DATA, SESSION
    }
}