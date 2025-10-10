package com.mpdc4gsr.module.thermalunified.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun VideoPlayerCompose(
    url: String?,
    title: String? = null,
    isAutoRotate: Boolean = true,
    isLockLandscape: Boolean = false,
    showFullAnimation: Boolean = true,
    needLockFull: Boolean = true,
    cacheWithPlay: Boolean = false,
    isTouchWidget: Boolean = true,
    onVideoCallback: VideoPlayerCallback? = null,
    modifier: Modifier = Modifier,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var volume by remember { mutableStateOf(1f) }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        // Video surface placeholder (would integrate with actual video player)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (url.isNullOrEmpty()) {
                EmptyVideoStateCompose()
            } else {
                // In actual implementation, this would be the video surface
                VideoSurfaceCompose(
                    url = url,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                )
            }
        }
        // Video controls overlay
        if (isTouchWidget) {
            VideoControlsOverlayCompose(
                isPlaying = isPlaying,
                isLoading = isLoading,
                isFullscreen = isFullscreen,
                currentPosition = currentPosition,
                duration = duration,
                volume = volume,
                title = title,
                onPlayPause = {
                    isPlaying = !isPlaying
                    onVideoCallback?.onPlayStateChanged(isPlaying)
                },
                onSeek = { position ->
                    currentPosition = position
                    onVideoCallback?.onSeekTo(position)
                },
                onVolumeChange = { newVolume ->
                    volume = newVolume
                    onVideoCallback?.onVolumeChanged(newVolume)
                },
                onFullscreenToggle = {
                    if (!isLockLandscape || !needLockFull) {
                        isFullscreen = !isFullscreen
                        onVideoCallback?.onFullscreenChanged(isFullscreen)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyVideoStateCompose() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.VideocamOff,
            contentDescription = "No video",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No video source",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun VideoSurfaceCompose(
    url: String,
    isPlaying: Boolean,
    isLoading: Boolean,
) {
    // Placeholder for video surface - in actual implementation would use ExoPlayer or similar
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (!isLoading) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPlaying) "Playing" else "Paused",
                modifier = Modifier.size(80.dp),
                tint = Color.White.copy(alpha = 0.8f),
            )
        }
        // URL display for preview
        Text(
            text = "Video: ${url.takeLast(30)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
        )
    }
}

@Composable
private fun VideoControlsOverlayCompose(
    isPlaying: Boolean,
    isLoading: Boolean,
    isFullscreen: Boolean,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    title: String?,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showControls by remember { mutableStateOf(true) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            // Auto-hide controls after 3 seconds when playing
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
    Box(
        modifier = modifier.clickable { showControls = !showControls },
    ) {
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
        ) {
            // Top bar with title and fullscreen
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                        ).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title ?: "Video",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onFullscreenToggle,
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        tint = Color.White,
                    )
                }
            }
        }
        // Center play/pause button
        AnimatedVisibility(
            visible = showControls && !isLoading,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = scaleOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            FloatingActionButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White,
                )
            }
        }
        // Bottom controls
        AnimatedVisibility(
            visible = showControls,
            enter =
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300),
                ) + fadeIn(),
            exit =
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300),
                ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            VideoBottomControlsCompose(
                currentPosition = currentPosition,
                duration = duration,
                volume = volume,
                showVolumeSlider = showVolumeSlider,
                onSeek = onSeek,
                onVolumeChange = onVolumeChange,
                onVolumeToggle = { showVolumeSlider = !showVolumeSlider },
            )
        }
    }
}

@Composable
private fun VideoBottomControlsCompose(
    currentPosition: Long,
    duration: Long,
    volume: Float,
    showVolumeSlider: Boolean,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeToggle: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.6f),
                ).padding(16.dp),
    ) {
        // Progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.width(48.dp),
            )
            Slider(
                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                onValueChange = { progress ->
                    onSeek((progress * duration).toLong())
                },
                modifier = Modifier.weight(1f),
                colors =
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.End,
            )
        }
        // Volume controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = showVolumeSlider,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.width(100.dp),
                        colors =
                            SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            IconButton(
                onClick = onVolumeToggle,
            ) {
                Icon(
                    imageVector =
                        when {
                            volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff
                            volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        },
                    contentDescription = "Volume",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
fun ThermalImageLoaderCompose(
    url: String?,
    contentDescription: String? = null,
    placeholderIcon: ImageVector = Icons.Default.Image,
    errorIcon: ImageVector = Icons.Default.BrokenImage,
    modifier: Modifier = Modifier,
    onImageLoad: (() -> Unit)? = null,
    onImageError: (() -> Unit)? = null,
) {
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        onSuccess = { onImageLoad?.invoke() },
        onError = { onImageError?.invoke() },
    )
}

@Composable
fun MediaUtilsCompose(
    mediaFiles: List<MediaFile>,
    selectedFile: MediaFile? = null,
    onFileSelected: (MediaFile) -> Unit = {},
    onFileDelete: (MediaFile) -> Unit = {},
    onFileShare: (MediaFile) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(mediaFiles) { file ->
            MediaFileItemCompose(
                file = file,
                isSelected = file == selectedFile,
                onSelect = { onFileSelected(file) },
                onDelete = { onFileDelete(file) },
                onShare = { onFileShare(file) },
            )
        }
    }
}

@Composable
private fun MediaFileItemCompose(
    file: MediaFile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onSelect() },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector =
                    when (file.type) {
                        MediaFileType.VIDEO -> Icons.Default.VideoFile
                        MediaFileType.IMAGE -> Icons.Default.Image
                        MediaFileType.AUDIO -> Icons.Default.AudioFile
                    },
                contentDescription = file.type.name,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${formatFileSize(file.size)} • ${formatTime(file.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// Helper functions
private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
    } else {
        "%02d:%02d".format(minutes, seconds % 60)
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.1f KB".format(kb)
        else -> "$bytes B"
    }
}

// Data classes and interfaces
interface VideoPlayerCallback {
    fun onPlayStateChanged(isPlaying: Boolean)

    fun onSeekTo(position: Long)

    fun onVolumeChanged(volume: Float)

    fun onFullscreenChanged(isFullscreen: Boolean)
}

data class MediaFile(
    val id: String,
    val name: String,
    val path: String,
    val type: MediaFileType,
    val size: Long,
    val duration: Long,
    val thumbnailPath: String? = null,
)

enum class MediaFileType {
    VIDEO,
    IMAGE,
    AUDIO,
}

// Preview functions
@Preview(showBackground = true)
@Composable
private fun VideoPlayerComposePreview() {
    MaterialTheme {
        VideoPlayerCompose(
            url = "https://example.com/video.mp4",
            title = "Thermal Video Recording",
            modifier = Modifier.size(400.dp, 300.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaUtilsComposePreview() {
    MaterialTheme {
        MediaUtilsCompose(
            mediaFiles =
                listOf(
                    MediaFile("1", "thermal_video_1.mp4", "/path/1", MediaFileType.VIDEO, 1024000, 30000),
                    MediaFile("2", "thermal_image_1.jpg", "/path/2", MediaFileType.IMAGE, 512000, 0),
                    MediaFile("3", "thermal_audio_1.mp3", "/path/3", MediaFileType.AUDIO, 256000, 45000),
                ),
            selectedFile = null,
            modifier = Modifier.height(300.dp),
        )
    }
}
