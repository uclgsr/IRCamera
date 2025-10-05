package mpdc4gsr.feature.thermal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.components.TitleBarAction
import mpdc4gsr.core.ui.theme.IRCameraTheme

/**
 * Gallery Screen - Media gallery for thermal images, recordings, and data
 * Replaces thermal gallery activities with unified Compose implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF16131e))
    ) {
        // Title bar with search
        TitleBar(
            title = "Media Gallery",
            showBackButton = true,
            onBackClick = onBackClick
        ) {
        }

        // Tab selector
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF2A2A2A),
            contentColor = Color.White
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Thermal Images") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Recordings") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Data Exports") }
            )
        }

        // Content based on selected tab
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> ThermalImagesGrid()
                1 -> RecordingsGrid()
                2 -> DataExportsGrid()
            }
        }
    }
}

/**
 * Thermal images grid
 */
@Composable
private fun ThermalImagesGrid(
    modifier: Modifier = Modifier
) {
    val mockImages = remember {
        (1..20).map { index ->
            ThermalImage(
                id = index,
                name = "Thermal_${index.toString().padStart(3, '0')}.jpg",
                timestamp = "2024-01-${
                    (index % 28 + 1).toString().padStart(2, '0')
                } 14:${(index * 3 % 60).toString().padStart(2, '0')}",
                maxTemp = 35.0f + (index * 2.5f),
                minTemp = 18.0f + (index * 1.2f)
            )
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(mockImages) { image ->
            ThermalImageCard(image = image)
        }
    }
}

/**
 * Recordings grid
 */
@Composable
private fun RecordingsGrid(
    modifier: Modifier = Modifier
) {
    val mockRecordings = remember {
        (1..12).map { index ->
            Recording(
                id = index,
                name = "Recording_${index.toString().padStart(3, '0')}.mp4",
                duration = "${(index * 45 / 60)}:${(index * 45 % 60).toString().padStart(2, '0')}",
                size = "${(index * 2.5f + 10.0f).toInt()}MB",
                timestamp = "2024-01-${(index % 28 + 1).toString().padStart(2, '0')}"
            )
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(mockRecordings) { recording ->
            RecordingCard(recording = recording)
        }
    }
}

/**
 * Data exports grid
 */
@Composable
private fun DataExportsGrid(
    modifier: Modifier = Modifier
) {
    val mockExports = remember {
        listOf(
            DataExport("GSR_Session_001.csv", "1.2MB", "GSR Data"),
            DataExport("Thermal_Analysis_002.json", "0.8MB", "Thermal Data"),
            DataExport("Multi_Modal_003.zip", "15.4MB", "Combined Data"),
            DataExport("GSR_Session_004.csv", "2.1MB", "GSR Data"),
            DataExport("Thermal_Report_005.pdf", "3.5MB", "Report")
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(mockExports) { export ->
            DataExportCard(export = export)
        }
    }
}

/**
 * Thermal image card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThermalImageCard(
    image: ThermalImage,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        onClick = {
            // TODO: Open image detail view
            android.widget.Toast.makeText(
                context,
                "Opening image: ${image.name}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        },
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thermal image preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Draw thermal pattern
                    val width = size.width
                    val height = size.height

                    // Hot spot
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.8f),
                        radius = width * 0.15f,
                        center = Offset(width * 0.6f, height * 0.4f)
                    )

                    // Cool spot
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.8f),
                        radius = width * 0.1f,
                        center = Offset(width * 0.3f, height * 0.7f)
                    )
                }

                // Temperature overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${image.maxTemp.toInt()}°C",
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${image.minTemp.toInt()}°C",
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }

            // Image info
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = image.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = image.timestamp,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Recording card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordingCard(
    recording: Recording,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        onClick = {
            // TODO: Play thermal recording
            android.widget.Toast.makeText(
                context,
                "Playing recording: ${recording.name}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recording.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = recording.timestamp,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Duration: ${recording.duration} • Size: ${recording.size}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Data export card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataExportCard(
    export: DataExport,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        onClick = {
            // TODO: Open or share data export
            android.widget.Toast.makeText(
                context,
                "Opening export: ${export.filename}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = Color.Green,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = export.filename,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = export.type,
                    color = Color.Green,
                    fontSize = 12.sp
                )
                Text(
                    text = "Size: ${export.size}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Data classes for gallery items
 */
data class ThermalImage(
    val id: Int,
    val name: String,
    val timestamp: String,
    val maxTemp: Float,
    val minTemp: Float
)

data class Recording(
    val id: Int,
    val name: String,
    val duration: String,
    val size: String,
    val timestamp: String
)

data class DataExport(
    val filename: String,
    val size: String,
    val type: String
)

@Preview(showBackground = true)
@Composable
private fun GalleryScreenPreview() {
    IRCameraTheme {
        GalleryScreen()
    }
}