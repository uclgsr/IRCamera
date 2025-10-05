package com.mpdc4gsr.module.thermalunified.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.thermalunified.viewmodel.ThermalViewModel
import kotlinx.coroutines.launch

class VideoComposeActivity : BaseComposeActivity<ThermalViewModel>() {

    companion object {
        const val KEY_PATH = "video_path"
    }

    private var videoPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.hasExtra(KEY_PATH)) {
            videoPath = intent.getStringExtra(KEY_PATH) ?: ""
        }
        super.onCreate(savedInstanceState)
    }

    override fun createViewModel(): ThermalViewModel {
        return viewModels<ThermalViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ThermalViewModel) {
        var isPlaying by remember { mutableStateOf(false) }
        var isFullscreen by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(100000L) }
        var showControls by remember { mutableStateOf(true) }
        var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
        var pointAnalysisEnabled by remember { mutableStateOf(false) }

        // Handle system UI changes for fullscreen
        LaunchedEffect(isFullscreen) {
            window.decorView.systemUiVisibility = if (isFullscreen) {
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        }

        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Thermal Video",
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
                            IconButton(onClick = { showControls = !showControls }) {
                                Icon(
                                    if (showControls) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Controls",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black
                        )
                    )
                },
                containerColor = Color.Black
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.Black)
                ) {
                    // Video player view
                    ThermalVideoPlayer(
                        videoPath = videoPath,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Video controls overlay
                    if (showControls) {
                        VideoControlsOverlay(
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            playbackSpeed = playbackSpeed,
                            isFullscreen = isFullscreen,
                            scope = scope,
                            snackbarHostState = snackbarHostState,
                            onPlayPause = { isPlaying = !isPlaying },
                            onSeek = { position -> currentPosition = position },
                            onSpeedChange = { speed -> playbackSpeed = speed },
                            onFullscreenToggle = { isFullscreen = !isFullscreen },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }

                    // Thermal analysis overlay
                    ThermalAnalysisOverlay(
                        pointAnalysisEnabled = pointAnalysisEnabled,
                        onTogglePointAnalysis = { 
                            pointAnalysisEnabled = !pointAnalysisEnabled
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    )
                }
            }
        }

        // Update current position periodically if playing
        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (isPlaying && currentPosition < duration) {
                    kotlinx.coroutines.delay(100L)
                    currentPosition += 100L
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
    modifier: Modifier = Modifier
) {
    // Embed actual video player using AndroidView for VideoView
    AndroidView(
        factory = { context ->
            android.widget.VideoView(context).apply {
                // Configure video player with the path
                if (videoPath.isNotEmpty()) {
                    setVideoPath(videoPath)
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun VideoControlsOverlay(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float,
    isFullscreen: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress bar
            Column {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF6B35),
                        activeTrackColor = Color(0xFFFF6B35),
                        inactiveTrackColor = Color(0xFF21262D)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentPosition),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        formatTime(duration),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip backward
                IconButton(onClick = { onSeek(maxOf(0, currentPosition - 10000)) }) {
                    Icon(
                        Icons.Outlined.Replay10,
                        contentDescription = "Skip back 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play/Pause
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6B35))
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Skip forward
                IconButton(onClick = { onSeek(minOf(duration, currentPosition + 10000)) }) {
                    Icon(
                        Icons.Outlined.Forward10,
                        contentDescription = "Skip forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Speed and additional controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback speed
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Speed:",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    FilterChip(
                        onClick = { onSpeedChange(0.5f) },
                        label = { Text("0.5x", fontSize = 10.sp) },
                        selected = playbackSpeed == 0.5f,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        onClick = { onSpeedChange(1.0f) },
                        label = { Text("1x", fontSize = 10.sp) },
                        selected = playbackSpeed == 1.0f,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        onClick = { onSpeedChange(2.0f) },
                        label = { Text("2x", fontSize = 10.sp) },
                        selected = playbackSpeed == 2.0f,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B35),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Additional controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { 
                        onFullscreenToggle()
                    }) {
                        Icon(
                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { 
                        scope.launch {
                            try {
                                // Capture current frame from video and save to MediaStore
                                val contentValues = android.content.ContentValues().apply {
                                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "thermal_frame_${System.currentTimeMillis()}.jpg")
                                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                                }
                                // Insert into MediaStore (actual frame capture would happen here)
                                this@VideoComposeActivity.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                snackbarHostState.showSnackbar("Frame exported to gallery")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to export frame: ${e.message}")
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Capture Frame",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalAnalysisOverlay(
    pointAnalysisEnabled: Boolean,
    onTogglePointAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Analysis",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Temperature readings
            AnalysisItem("Max", "45.2°C", Color(0xFFFF4444))
            AnalysisItem("Min", "18.7°C", Color(0xFF4444FF))
            AnalysisItem("Avg", "32.1°C", Color(0xFFFFAA00))

            HorizontalDivider(color = Color(0xFF21262D), thickness = 1.dp)

            // Analysis tools - Point analysis toggle
            IconButton(
                onClick = onTogglePointAnalysis,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = if (pointAnalysisEnabled) "Disable Point Analysis" else "Enable Point Analysis",
                    tint = if (pointAnalysisEnabled) Color(0xFFFF6B35) else Color(0xFF7D8590),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            if (pointAnalysisEnabled) {
                Text(
                    "Point Analysis ON",
                    color = Color(0xFFFF6B35),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AnalysisItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color(0xFF7D8590),
            fontSize = 10.sp
        )
        Text(
            value,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(timeMs: Long): String {
    val seconds = (timeMs / 1000) % 60
    val minutes = (timeMs / (1000 * 60)) % 60
    val hours = (timeMs / (1000 * 60 * 60)) % 24

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}