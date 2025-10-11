package com.mpdc4gsr.component.thermal.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ThermalCameraFragment(
    onCapturePhoto: () -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    isRecording: Boolean = false,
    temperatureData: List<Float> = emptyList(),
    currentTemperature: Float = 0f,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Main camera view area
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black, RoundedCornerShape(8.dp)),
        ) {
            // Placeholder for thermal camera surface
            // In real implementation, this would be AndroidView with thermal surface
            AndroidViewPlaceholder(
                viewType = "ThermalCameraSurface",
                modifier = Modifier.fillMaxSize(),
            )
            // Temperature overlay
            if (currentTemperature > 0f) {
                Card(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f),
                        ),
                ) {
                    Text(
                        text = "${String.format("%.1f", currentTemperature)}°C",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Camera controls
        ThermalCameraControls(
            onCapturePhoto = onCapturePhoto,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            isRecording = isRecording,
        )
        // Temperature trend
        if (temperatureData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TrendViewStatefulCompose(
                temperatureData = temperatureData,
                initiallyExpanded = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ThermalCameraControls(
    onCapturePhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Capture photo button
        FloatingActionButton(
            onClick = onCapturePhoto,
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture Photo",
            )
        }
        // Record video button
        FloatingActionButton(
            onClick = if (isRecording) onStopRecording else onStartRecording,
            containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.secondary,
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
            )
        }
        // Settings button
        val context = androidx.compose.ui.platform.LocalContext.current
        FloatingActionButton(
            onClick = {
                // TODO: Implement thermal camera settings navigation
                // Should open settings screen for temperature range, palette, etc.
                android.widget.Toast
                    .makeText(
                        context,
                        "Opening thermal camera settings...",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
            },
            containerColor = MaterialTheme.colorScheme.tertiary,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
            )
        }
    }
}

@Composable
fun ThermalGalleryFragment(
    images: List<ThermalGalleryItem>,
    onImageClick: (ThermalGalleryItem) -> Unit = {},
    onDeleteImage: (ThermalGalleryItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) {
        EmptyStateComponent(
            title = "No Images",
            description = "Take some thermal photos to see them here",
            icon = Icons.Default.PhotoLibrary,
            actionText = "Take Photo",
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(images.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { item ->
                        ThermalGalleryItemCard(
                            item = item,
                            onClick = { onImageClick(item) },
                            onDelete = { onDeleteImage(item) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Fill empty space for odd number of items
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalGalleryItemCard(
    item: ThermalGalleryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box {
            Column {
                AsyncImage(
                    model = item.imagePath,
                    contentDescription = item.title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                    contentScale = ContentScale.Crop,
                )
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.temperature != null) {
                        Text(
                            text = "${String.format("%.1f", item.temperature)}°C",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red,
                )
            }
        }
    }
}

@Composable
fun MonitorCaptureFragment(
    isMonitoring: Boolean,
    onStartMonitoring: () -> Unit = {},
    onStopMonitoring: () -> Unit = {},
    onCaptureFrame: () -> Unit = {},
    capturedFrames: List<MonitorFrame> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Monitor status
        MonitorStatusCard(
            isMonitoring = isMonitoring,
            frameCount = capturedFrames.size,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = if (isMonitoring) onStopMonitoring else onStartMonitoring,
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = if (isMonitoring) Color.Red else MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isMonitoring) "Stop Monitor" else "Start Monitor",
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isMonitoring) "Stop Monitor" else "Start Monitor")
            }
            Button(
                onClick = onCaptureFrame,
                enabled = isMonitoring,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture Frame",
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Capture")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Captured frames
        if (capturedFrames.isNotEmpty()) {
            Text(
                text = "Captured Frames",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(capturedFrames) { frame ->
                    MonitorFrameCard(frame = frame)
                }
            }
        }
    }
}

@Composable
private fun MonitorStatusCard(
    isMonitoring: Boolean,
    frameCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isMonitoring) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isMonitoring) Color.Green else Color.Gray),
                )
                Text(
                    text = if (isMonitoring) "Monitoring Active" else "Monitoring Inactive",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = "$frameCount frames",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonitorFrameCard(
    frame: MonitorFrame,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            AsyncImage(
                model = frame.imagePath,
                contentDescription = "Monitor Frame",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(8.dp),
            ) {
                Text(
                    text = frame.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (frame.temperature != null) {
                    Text(
                        text = "${String.format("%.1f", frame.temperature)}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AndroidViewPlaceholder(
    viewType: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.1f))
                .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Preview",
                modifier = Modifier.size(48.dp),
                tint = Color.Gray,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewType,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Data classes for fragment patterns
data class ThermalGalleryItem(
    val id: String,
    val imagePath: String,
    val title: String,
    val timestamp: String,
    val temperature: Float? = null,
    val type: String = "thermal",
)

data class MonitorFrame(
    val id: String,
    val imagePath: String,
    val timestamp: String,
    val temperature: Float? = null,
)

@Composable
fun ThermalFragmentPatternsPreview() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Thermal Fragment Patterns",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        // Monitor status example
        MonitorStatusCard(
            isMonitoring = true,
            frameCount = 15,
        )
        // Camera controls example
        ThermalCameraControls(
            onCapturePhoto = {},
            onStartRecording = {},
            onStopRecording = {},
            isRecording = false,
        )
    }
}

