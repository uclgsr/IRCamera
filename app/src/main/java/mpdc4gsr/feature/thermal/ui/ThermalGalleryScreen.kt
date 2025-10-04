package mpdc4gsr.feature.thermal.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import mpdc4gsr.core.ui.components.NavigationBreadcrumb
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme
import java.io.File

/**
 * Thermal Gallery Screen - Browse and manage thermal images and recordings
 * Replaces thermal GalleryActivity with Compose implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalGalleryScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf("Images", "Videos", "Reports")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        TitleBar(
            title = "Gallery",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
            TitleBarAction(
                icon = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                contentDescription = "Toggle View Mode",
                onClick = {
                    viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                }
            )
            TitleBarAction(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                onClick = { showSearchDialog = true }
            )
        }

        NavigationBreadcrumb(
            currentScreen = "Gallery",
            previousScreen = "Home"
        )

        // Tab Row
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF2A2A2A),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                )
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> ThermalImagesContent(viewMode = viewMode, searchQuery = searchQuery)
            1 -> ThermalVideosContent(viewMode = viewMode, searchQuery = searchQuery)
            2 -> ThermalReportsContent(viewMode = viewMode, searchQuery = searchQuery)
        }
    }

    if (showSearchDialog) {
        SearchDialog(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onDismiss = { showSearchDialog = false }
        )
    }
}

@Composable
private fun SearchDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Gallery") },
        text = {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search by name or date") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onDismiss()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                keyboardController?.hide()
                onDismiss()
            }) {
                Text("Search")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onSearchQueryChange("")
                onDismiss()
            }) {
                Text("Clear")
            }
        }
    )
}

@Composable
private fun ThermalImagesContent(viewMode: ViewMode, searchQuery: String = "") {
    val allImages = remember { generateSampleThermalImages() }
    val sampleImages = remember(searchQuery, allImages) {
        if (searchQuery.isBlank()) {
            allImages
        } else {
            allImages.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.date.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleImages) { item ->
                ThermalImageGridItem(item)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleImages) { item ->
                ThermalImageListItem(item)
            }
        }
    }
}

@Composable
private fun ThermalVideosContent(viewMode: ViewMode, searchQuery: String = "") {
    val allVideos = remember { generateSampleThermalVideos() }
    val sampleVideos = remember(searchQuery, allVideos) {
        if (searchQuery.isBlank()) {
            allVideos
        } else {
            allVideos.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.date.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleVideos) { item ->
                ThermalVideoGridItem(item)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleVideos) { item ->
                ThermalVideoListItem(item)
            }
        }
    }
}

@Composable
private fun ThermalReportsContent(viewMode: ViewMode, searchQuery: String = "") {
    val allReports = remember { generateSampleThermalReports() }
    val sampleReports = remember(searchQuery, allReports) {
        if (searchQuery.isBlank()) {
            allReports
        } else {
            allReports.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.date.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sampleReports) { item ->
            ThermalReportItem(item)
        }
    }
}

@Composable
private fun ThermalImageGridItem(item: ThermalMediaItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Box {
            // Thermal image with realistic thermal gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color.Yellow,
                                Color.Red,
                                Color(0xFF8B0000),
                                MaterialTheme.colorScheme.primary
                            ),
                            radius = 200f
                        )
                    )
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Temperature overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(4.dp)
            ) {
                Text(
                    text = item.temperature,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // File info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.date,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ThermalImageListItem(item: ThermalMediaItem) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.date} • ${item.size}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Max: ${item.temperature}",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }

            // Actions
            IconButton(onClick = {
                shareFile(context, item.name, "image/thermal")
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
            }
        }
    }
}

@Composable
private fun ThermalVideoGridItem(item: ThermalMediaItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Box {
            // Video thumbnail with thermal pattern
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4A0080),
                                MaterialTheme.colorScheme.primary,
                                Color.Cyan,
                                Color.Green,
                                Color.Yellow,
                                Color.Red
                            )
                        )
                    )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Duration
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(4.dp)
            ) {
                Text(
                    text = item.duration ?: "0:00",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // File info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.date,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ThermalVideoListItem(item: ThermalMediaItem) {
    val context = LocalContext.current
    var showPlayMessage by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(45.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Video info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.date} • ${item.duration}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = item.size,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // Actions
            IconButton(onClick = {
                playVideo(context, item.name)
            }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showPlayMessage) {
        AlertDialog(
            onDismissRequest = { showPlayMessage = false },
            title = { Text("Playing Video") },
            text = { Text("Opening ${item.name} in video player...") },
            confirmButton = {
                TextButton(onClick = { showPlayMessage = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ThermalReportItem(item: ThermalMediaItem) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = Color.Green,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Created: ${item.date}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "${item.size} • PDF Report",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Row {
                IconButton(onClick = {
                    viewReport(context, item.name)
                }) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    shareFile(context, item.name, "application/pdf")
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
                }
            }
        }
    }
}

enum class ViewMode {
    GRID, LIST
}

data class ThermalMediaItem(
    val name: String,
    val date: String,
    val size: String,
    val temperature: String,
    val duration: String? = null
)

private fun generateSampleThermalImages(): List<ThermalMediaItem> {
    return listOf(
        ThermalMediaItem("IMG_001.thermal", "2024-01-15", "2.3 MB", "45.2°C"),
        ThermalMediaItem("IMG_002.thermal", "2024-01-15", "2.1 MB", "38.7°C"),
        ThermalMediaItem("IMG_003.thermal", "2024-01-14", "2.5 MB", "52.1°C"),
        ThermalMediaItem("IMG_004.thermal", "2024-01-14", "2.2 MB", "41.3°C"),
        ThermalMediaItem("IMG_005.thermal", "2024-01-13", "2.4 MB", "47.8°C"),
        ThermalMediaItem("IMG_006.thermal", "2024-01-13", "2.0 MB", "36.9°C")
    )
}

private fun generateSampleThermalVideos(): List<ThermalMediaItem> {
    return listOf(
        ThermalMediaItem("VID_001.mp4", "2024-01-15", "15.2 MB", "48.5°C", "2:34"),
        ThermalMediaItem("VID_002.mp4", "2024-01-14", "22.1 MB", "42.1°C", "3:47"),
        ThermalMediaItem("VID_003.mp4", "2024-01-13", "18.7 MB", "39.8°C", "3:12"),
        ThermalMediaItem("VID_004.mp4", "2024-01-12", "12.3 MB", "44.2°C", "2:01")
    )
}

private fun generateSampleThermalReports(): List<ThermalMediaItem> {
    return listOf(
        ThermalMediaItem("Thermal_Report_001.pdf", "2024-01-15", "1.2 MB", ""),
        ThermalMediaItem("Thermal_Report_002.pdf", "2024-01-14", "980 KB", ""),
        ThermalMediaItem("Thermal_Report_003.pdf", "2024-01-13", "1.5 MB", ""),
        ThermalMediaItem("Analysis_Summary.pdf", "2024-01-12", "2.1 MB", "")
    )
}

private fun shareFile(context: Context, fileName: String, mimeType: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, "Thermal Data: $fileName")
        putExtra(Intent.EXTRA_TEXT, "Sharing thermal data file: $fileName")
    }
    
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Share $fileName"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun playVideo(context: Context, fileName: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(android.net.Uri.parse("file://$fileName"), "video/*")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun viewReport(context: Context, fileName: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(android.net.Uri.parse("file://$fileName"), "application/pdf")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Preview(showBackground = true)
@Composable
private fun ThermalGalleryScreenPreview() {
    IRCameraTheme {
        ThermalGalleryScreen()
    }
}