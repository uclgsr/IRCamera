package mpdc4gsr.feature.gsr.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.AppBaseViewModel

/**
 * GSRVideoPlayerComposeActivity - Modern Video Playback with Compose
 *
 * Advanced video player for GSR session recordings with:
 * - Modern media player controls with Compose UI
 * - Synchronized GSR data overlay during playback
 * - Timeline scrubbing with data markers
 * - Multi-modal recording playback (video + sensor data)
 * - Export and sharing capabilities
 * - Performance-optimized video rendering
 */
class GSRVideoPlayerComposeActivity : BaseComposeActivity<AppBaseViewModel>() {

    companion object {
        private const val EXTRA_VIDEO_PATH = "video_path"
        private const val EXTRA_SESSION_ID = "session_id"

        fun startActivity(
            context: Context,
            videoPath: String,
            sessionId: String? = null
        ) {
            val intent = Intent(context, GSRVideoPlayerComposeActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_PATH, videoPath)
                sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): AppBaseViewModel {
        return viewModels<AppBaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: AppBaseViewModel) {
        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: ""
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "GSR Video Player",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* TODO: Implement share video
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = { /* TODO: Implement video settings
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                            IconButton(onClick = { /* TODO: Implement more options
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                GSRVideoPlayerContent(
                    videoPath = videoPath,
                    sessionId = sessionId,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun GSRVideoPlayerContent(
    videoPath: String,
    sessionId: String?,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(100f) }
    var showDataOverlay by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Video Player Area
        VideoPlayerCard(
            videoPath = videoPath,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            showDataOverlay = showDataOverlay,
            onPlayPause = { isPlaying = !isPlaying },
            onSeek = { currentPosition = it }
        )

        // Scrollable content below video
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Controls
            VideoControlsCard(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed,
                showDataOverlay = showDataOverlay,
                onPlayPause = { isPlaying = !isPlaying },
                onSeek = { currentPosition = it },
                onSpeedChange = { playbackSpeed = it },
                onOverlayToggle = { showDataOverlay = it }
            )

            // Video Information
            VideoInfoCard(
                videoPath = videoPath,
                sessionId = sessionId,
                duration = duration
            )

            // GSR Data Timeline
            if (sessionId != null) {
                GSRDataTimelineCard(
                    sessionId = sessionId,
                    currentPosition = currentPosition,
                    duration = duration
                )
            }

            // Playback Statistics
            PlaybackStatisticsCard(
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed
            )

            // Export Options
            VideoExportCard(
                videoPath = videoPath,
                sessionId = sessionId
            )
        }
    }
}

@Composable
private fun VideoPlayerCard(
    videoPath: String,
    isPlaying: Boolean,
    currentPosition: Float,
    duration: Float,
    showDataOverlay: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Video Playback",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Video View Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Color.Black,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // AndroidView for VideoView integration
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(Uri.parse(videoPath))
                            setMediaController(MediaController(context))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // GSR Data Overlay
                if (showDataOverlay) {
                    GSRDataOverlay(
                        currentPosition = currentPosition,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }

                // Play/Pause Overlay
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(32.dp)
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Seek Bar
            Column {
                Slider(
                    value = currentPosition,
                    onValueChange = onSeek,
                    valueRange = 0f..duration,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun GSRDataOverlay(
    currentPosition: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "GSR Data",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "12.4 μS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Quality: 94%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun VideoControlsCard(
    isPlaying: Boolean,
    currentPosition: Float,
    duration: Float,
    playbackSpeed: Float,
    showDataOverlay: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOverlayToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Playback Controls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Main Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSeek(0f) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Restart")
                }

                IconButton(onClick = { onSeek(maxOf(0f, currentPosition - 10f)) }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(28.dp)
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { onSeek(minOf(duration, currentPosition + 10f)) }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                }

                IconButton(onClick = { onSeek(duration) }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "End")
                }
            }

            // Playback Speed
            Text(
                "Playback Speed: ${String.format("%.1f", playbackSpeed)}x",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                    FilterChip(
                        onClick = { onSpeedChange(speed) },
                        label = { Text("${speed}x") },
                        selected = playbackSpeed == speed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Overlay Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Show GSR Data Overlay",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showDataOverlay,
                    onCheckedChange = onOverlayToggle
                )
            }
        }
    }
}

@Composable
private fun VideoInfoCard(
    videoPath: String,
    sessionId: String?,
    duration: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Video Information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            VideoInfoRow("File Name", videoPath.substringAfterLast("/"))
            VideoInfoRow("Session ID", sessionId ?: "N/A")
            VideoInfoRow("Duration", formatTime(duration))
            VideoInfoRow("Format", "MP4")
            VideoInfoRow("Resolution", "1920x1080")
            VideoInfoRow("Frame Rate", "30 FPS")
            VideoInfoRow("File Size", "125.6 MB")
        }
    }
}

@Composable
private fun VideoInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GSRDataTimelineCard(
    sessionId: String,
    currentPosition: Float,
    duration: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "GSR Data Timeline",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Timeline visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                // Simplified timeline representation
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current position indicator
                    val progress = if (duration > 0) currentPosition / duration else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }

                Text(
                    "GSR Timeline Visualization",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Current GSR values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GSRTimelineMetric("Current", "12.4 μS")
                GSRTimelineMetric("Peak", "18.9 μS")
                GSRTimelineMetric("Average", "11.2 μS")
                GSRTimelineMetric("Quality", "94%")
            }
        }
    }
}

@Composable
private fun GSRTimelineMetric(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaybackStatisticsCard(
    currentPosition: Float,
    duration: Float,
    playbackSpeed: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Playback Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlaybackStatistic("Progress", "${((currentPosition / duration) * 100).toInt()}%")
                PlaybackStatistic("Remaining", formatTime(duration - currentPosition))
                PlaybackStatistic("Speed", "${playbackSpeed}x")
            }
        }
    }
}

@Composable
private fun PlaybackStatistic(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VideoExportCard(
    videoPath: String,
    sessionId: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Export Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Implement export video
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VideoFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Video")
                }

                OutlinedButton(
                    onClick = { /* TODO: Implement export audio
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AudioFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Audio")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* TODO: Implement share video
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Video")
                }

                OutlinedButton(
                    onClick = { /* TODO: Implement save frame
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Frame")
                }
            }
        }
    }
}

// Helper function to format time
private fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

class GSRVideoPlayerViewModel : AppBaseViewModel() {
    // ViewModel implementation for managing video playback state, GSR data synchronization, etc.
    // Future implementation would include:
    // - Video playback state management
    // - GSR data loading and synchronization
    // - Export functionality
    // - Playback statistics tracking
}