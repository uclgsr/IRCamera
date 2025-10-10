package mpdc4gsr.feature.capture.thermal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.designsystem.components.common.NavigationBreadcrumb
import mpdc4gsr.core.designsystem.components.common.TitleBar
import mpdc4gsr.core.designsystem.components.common.TitleBarAction
import mpdc4gsr.core.designsystem.theme.IRCameraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalGalleryScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    val tabs = listOf("Images", "Videos", "Reports")
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0xFF16131e)),
    ) {
        TitleBar(
            title = "Gallery",
            showBackButton = true,
            onBackClick = onBackClick,
        ) {
            TitleBarAction(
                icon = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                contentDescription = "Toggle View Mode",
                onClick = {
                    viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                },
            )
            val context = androidx.compose.ui.platform.LocalContext.current
            TitleBarAction(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                onClick = {
                    // TODO: Implement search functionality
                    android.widget.Toast
                        .makeText(
                            context,
                            "Search feature coming soon",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                },
            )
        }
        NavigationBreadcrumb(
            currentScreen = "Gallery",
            previousScreen = "Home",
        )
        // Tab Row
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF2A2A2A),
            contentColor = Color.White,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Gray,
                        )
                    },
                )
            }
        }
        // Content based on selected tab
        when (selectedTab) {
            0 -> ThermalImagesContent(viewMode = viewMode)
            1 -> ThermalVideosContent(viewMode = viewMode)
            2 -> ThermalReportsContent(viewMode = viewMode)
        }
    }
}

@Composable
private fun ThermalImagesContent(viewMode: ViewMode) {
    val sampleImages = remember { generateSampleThermalImages() }
    if (viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sampleImages) { item ->
                ThermalImageGridItem(item)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sampleImages) { item ->
                ThermalImageListItem(item)
            }
        }
    }
}

@Composable
private fun ThermalVideosContent(viewMode: ViewMode) {
    val sampleVideos = remember { generateSampleThermalVideos() }
    if (viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sampleVideos) { item ->
                ThermalVideoGridItem(item)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sampleVideos) { item ->
                ThermalVideoListItem(item)
            }
        }
    }
}

@Composable
private fun ThermalReportsContent(viewMode: ViewMode) {
    val sampleReports = remember { generateSampleThermalReports() }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sampleReports) { item ->
            ThermalReportItem(item)
        }
    }
}

@Composable
private fun ThermalImageGridItem(item: ThermalMediaItem) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Box {
            // Thermal image with realistic thermal gradient
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors =
                                    listOf(
                                        Color.Yellow,
                                        Color.Red,
                                        Color(0xFF8B0000),
                                        MaterialTheme.colorScheme.primary,
                                    ),
                                radius = 200f,
                            ),
                        ),
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Thermal Image",
                    tint = Color.Red,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                )
            }
            // Temperature overlay
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp),
                        ).padding(4.dp),
            ) {
                Text(
                    text = item.temperature,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            // File info
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.date,
                    color = Color.Gray,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ThermalImageListItem(item: ThermalMediaItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            Box(
                modifier =
                    Modifier
                        .size(60.dp)
                        .background(Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Thermal Image",
                    tint = Color.Red,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // File info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${item.date} • ${item.size}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
                Text(
                    text = "Max: ${item.temperature}",
                    color = Color.Red,
                    fontSize = 12.sp,
                )
            }
            // Actions
            val context = androidx.compose.ui.platform.LocalContext.current
            IconButton(onClick = {
                // TODO: Share thermal image
                android.widget.Toast
                    .makeText(
                        context,
                        "Share image feature coming soon",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
            }
        }
    }
}

@Composable
private fun ThermalVideoGridItem(item: ThermalMediaItem) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Box {
            // Video thumbnail with thermal pattern
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color(0xFF4A0080),
                                        MaterialTheme.colorScheme.primary,
                                        Color.Cyan,
                                        Color.Green,
                                        Color.Yellow,
                                        Color.Red,
                                    ),
                            ),
                        ),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                )
            }
            // Duration
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp),
                        ).padding(4.dp),
            ) {
                Text(
                    text = item.duration ?: "0:00",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            // File info
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.date,
                    color = Color.Gray,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ThermalVideoListItem(item: ThermalMediaItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            Box(
                modifier =
                    Modifier
                        .width(80.dp)
                        .height(45.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Video info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${item.date} • ${item.duration}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
                Text(
                    text = item.size,
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
            }
            // Actions
            val context = androidx.compose.ui.platform.LocalContext.current
            IconButton(onClick = {
                // TODO: Play thermal video
                android.widget.Toast
                    .makeText(
                        context,
                        "Play video feature coming soon",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
            }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ThermalReportItem(item: ThermalMediaItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = "Report",
                tint = Color.Green,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Created: ${item.date}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
                Text(
                    text = "${item.size} • PDF Report",
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
            }
            val context = androidx.compose.ui.platform.LocalContext.current
            Row {
                IconButton(onClick = {
                    // TODO: View report details
                    android.widget.Toast
                        .makeText(
                            context,
                            "View report feature coming soon",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                }) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = {
                    // TODO: Share report
                    android.widget.Toast
                        .makeText(
                            context,
                            "Share report feature coming soon",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
                }
            }
        }
    }
}

enum class ViewMode {
    GRID,
    LIST,
}

data class ThermalMediaItem(
    val name: String,
    val date: String,
    val size: String,
    val temperature: String,
    val duration: String? = null,
)

private fun generateSampleThermalImages(): List<ThermalMediaItem> =
    listOf(
        ThermalMediaItem("IMG_001.thermal", "2024-01-15", "2.3 MB", "45.2°C"),
        ThermalMediaItem("IMG_002.thermal", "2024-01-15", "2.1 MB", "38.7°C"),
        ThermalMediaItem("IMG_003.thermal", "2024-01-14", "2.5 MB", "52.1°C"),
        ThermalMediaItem("IMG_004.thermal", "2024-01-14", "2.2 MB", "41.3°C"),
        ThermalMediaItem("IMG_005.thermal", "2024-01-13", "2.4 MB", "47.8°C"),
        ThermalMediaItem("IMG_006.thermal", "2024-01-13", "2.0 MB", "36.9°C"),
    )

private fun generateSampleThermalVideos(): List<ThermalMediaItem> =
    listOf(
        ThermalMediaItem("VID_001.mp4", "2024-01-15", "15.2 MB", "48.5°C", "2:34"),
        ThermalMediaItem("VID_002.mp4", "2024-01-14", "22.1 MB", "42.1°C", "3:47"),
        ThermalMediaItem("VID_003.mp4", "2024-01-13", "18.7 MB", "39.8°C", "3:12"),
        ThermalMediaItem("VID_004.mp4", "2024-01-12", "12.3 MB", "44.2°C", "2:01"),
    )

private fun generateSampleThermalReports(): List<ThermalMediaItem> =
    listOf(
        ThermalMediaItem("Thermal_Report_001.pdf", "2024-01-15", "1.2 MB", ""),
        ThermalMediaItem("Thermal_Report_002.pdf", "2024-01-14", "980 KB", ""),
        ThermalMediaItem("Thermal_Report_003.pdf", "2024-01-13", "1.5 MB", ""),
        ThermalMediaItem("Analysis_Summary.pdf", "2024-01-12", "2.1 MB", ""),
    )

@Preview(showBackground = true)
@Composable
private fun ThermalGalleryScreenPreview() {
    IRCameraTheme {
        ThermalGalleryScreen()
    }
}
