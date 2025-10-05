package com.mpdc4gsr.module.thermalunified.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class ThermalVideoComposeActivity : BaseComposeActivity<BaseViewModel>() {

    companion object {
        private const val KEY_PATH = "video_path"
        private const val KEY_TITLE = "video_title"

        fun startWithPath(context: Context, videoPath: String, title: String = "Thermal Video") {
            val intent = Intent(context, ThermalVideoComposeActivity::class.java).apply {
                putExtra(KEY_PATH, videoPath)
                putExtra(KEY_TITLE, title)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewModel(): BaseViewModel {
        return viewModels<BaseViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: BaseViewModel) {
        val videoPath = intent.getStringExtra(KEY_PATH) ?: ""
        val videoTitle = intent.getStringExtra(KEY_TITLE) ?: "Thermal Video"

        var isPlaying by remember { mutableStateOf(false) }
        var currentPosition by remember { mutableStateOf(0L) }
        var videoDuration by remember { mutableStateOf(0L) }
        var showControls by remember { mutableStateOf(true) }
        var showThermalData by remember { mutableStateOf(true) }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                videoTitle,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showThermalData = !showThermalData }) {
                                Icon(
                                    if (showThermalData) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle thermal data"
                                )
                            }
                            IconButton(onClick = { /* TODO: Implement share video
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
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
                ThermalVideoContent(
                    videoPath = videoPath,
                    isPlaying = isPlaying,
                    onPlayingChange = { isPlaying = it },
                    currentPosition = currentPosition,
                    videoDuration = videoDuration,
                    onPositionChange = { currentPosition = it },
                    showControls = showControls,
                    onControlsToggle = { showControls = !showControls },
                    showThermalData = showThermalData,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ThermalVideoContent(
    videoPath: String,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    currentPosition: Long,
    videoDuration: Long,
    onPositionChange: (Long) -> Unit,
    showControls: Boolean,
    onControlsToggle: () -> Unit,
    showThermalData: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video player
        ThermalVideoPlayer(
            videoPath = videoPath,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            onPositionChange = onPositionChange,
            onClick = onControlsToggle,
            modifier = Modifier.fillMaxSize()
        )

        // Thermal data overlay
        if (showThermalData) {
            ThermalDataOverlay(
                currentTemp = 36.8f,
                maxTemp = 42.1f,
                minTemp = 28.3f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }

        // Video controls overlay
        if (showControls) {
            ThermalVideoControls(
                isPlaying = isPlaying,
                onPlayingChange = onPlayingChange,
                currentPosition = currentPosition,
                videoDuration = videoDuration,
                onPositionChange = onPositionChange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        // Playback indicator
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalVideoPlayer(
    videoPath: String,
    isPlaying: Boolean,
    currentPosition: Long,
    onPositionChange: (Long) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            android.widget.VideoView(ctx).apply {
                val uri = Uri.parse(videoPath)
                setVideoURI(uri)

                // Set up media controller
                val mediaController = android.widget.MediaController(ctx)
                setMediaController(mediaController)
                mediaController.setAnchorView(this)

                setOnClickListener { onClick() }

                // Set up completion listener
                setOnCompletionListener {
                    onPositionChange(0L)
                }
            }
        },
        update = { videoView ->
            if (isPlaying && !videoView.isPlaying) {
                videoView.start()
            } else if (!isPlaying && videoView.isPlaying) {
                videoView.pause()
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ThermalDataOverlay(
    currentTemp: Float,
    maxTemp: Float,
    minTemp: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "THERMAL DATA",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${currentTemp}°C",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "H:${maxTemp}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = "L:${minTemp}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ThermalVideoControls(
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    currentPosition: Long,
    videoDuration: Long,
    onPositionChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Progress bar
            if (videoDuration > 0) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onPositionChange(it.toLong()) },
                    valueRange = 0f..videoDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF6B35),
                        activeTrackColor = Color(0xFFFF6B35)
                    )
                )

                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = formatTime(videoDuration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* TODO: Implement previous frame
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }

                // Play/Pause button
                IconButton(
                    onClick = { onPlayingChange(!isPlaying) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color(0xFFFF6B35),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { /* TODO: Implement next frame
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Additional controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Implement export frame
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Export Frame",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Frame")
                }

                OutlinedButton(
                    onClick = { /* TODO: Implement analyze
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = "Analyze",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Analyze")
                }

                OutlinedButton(
                    onClick = { /* TODO: Implement settings
                     *   - Determine required implementation
                     *   - Add necessary state management
                     *   - Update UI accordingly
                     */ },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settings")
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}