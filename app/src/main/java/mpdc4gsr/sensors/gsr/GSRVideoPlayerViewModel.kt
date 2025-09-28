package mpdc4gsr.sensors.gsr

import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern GSR Video Player ViewModel - MVVM StateFlow Implementation
 * Manages video playback state and metadata with reactive patterns
 */
class GSRVideoPlayerViewModel : BaseViewModel() {

    // StateFlow for video state management
    private val _videoState = MutableStateFlow(VideoState(null, ""))
    val videoState: StateFlow<VideoState> = _videoState.asStateFlow()

    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()

    // SharedFlow for one-time events
    private val _videoEvents = MutableSharedFlow<VideoEvent>()
    val videoEvents: SharedFlow<VideoEvent> = _videoEvents.asSharedFlow()

    // Combined UI State
    val videoUiState: StateFlow<VideoPlayerUiState> = combine(
        _videoState,
        _videoInfo
    ) { videoState, videoInfo ->
        VideoPlayerUiState(
            videoState = videoState,
            videoInfo = videoInfo,
            hasVideo = videoState.uri != null,
            canPlay = videoState.uri != null && videoState.isReady
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, VideoPlayerUiState())

    data class VideoPlayerUiState(
        val videoState: VideoState = VideoState(null, ""),
        val videoInfo: VideoInfo? = null,
        val hasVideo: Boolean = false,
        val canPlay: Boolean = false,
        val isLoading: Boolean = false
    )

    data class VideoState(
        val uri: Uri?,
        val title: String,
        val isReady: Boolean = false,
        val isPlaying: Boolean = false
    )

    data class VideoInfo(
        val fileName: String,
        val fileSize: String,
        val createdDate: String,
        val filePath: String
    )

    sealed class VideoEvent {
        data class ShowError(val message: String) : VideoEvent()
        data class ShowToast(val message: String) : VideoEvent()
        object VideoReady : VideoEvent()
        data class PlaybackError(val what: Int, val extra: Int) : VideoEvent()
        data class ShareVideo(val uri: Uri) : VideoEvent()
    }

    fun loadVideo(
        videoPath: String,
        packageName: String,
        context: android.content.Context? = null
    ) {
        launchWithErrorHandling {
            val videoFile = File(videoPath)

            if (!videoFile.exists()) {
                _videoEvents.emit(VideoEvent.ShowError("Video file does not exist"))
                return@launchWithErrorHandling
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val authority = "$packageName.fileprovider"
                try {
                    if (context != null) {
                        FileProvider.getUriForFile(context, authority, videoFile)
                    } else {
                        _videoEvents.emit(VideoEvent.ShowError("Context is required to generate a content URI on API 24+"))
                        return@launchWithErrorHandling
                    }
                } catch (e: Exception) {
                    _videoEvents.emit(VideoEvent.ShowError("Failed to create file URI: ${e.message}"))
                    return@launchWithErrorHandling
                }
            } else {
                Uri.fromFile(videoFile)
            }

            _videoState.value = VideoState(
                uri = uri,
                title = videoFile.name
            )

            generateVideoInfo(videoFile)
        }
    }

    private fun generateVideoInfo(videoFile: File) {
        val fileSize = when {
            videoFile.length() >= 1024 * 1024 * 1024 -> {
                "%.1f GB".format(videoFile.length() / (1024.0 * 1024.0 * 1024.0))
            }

            videoFile.length() >= 1024 * 1024 -> {
                "%.1f MB".format(videoFile.length() / (1024.0 * 1024.0))
            }

            else -> {
                "%.1f KB".format(videoFile.length() / 1024.0)
            }
        }

        val createdDate = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date(videoFile.lastModified()))

        _videoInfo.value = VideoInfo(
            fileName = videoFile.name,
            fileSize = fileSize,
            createdDate = createdDate,
            filePath = videoFile.absolutePath
        )
    }

    fun onVideoReady() {
        val currentState = _videoState.value
        _videoState.value = currentState.copy(isReady = true)
        launchWithErrorHandling {
            _videoEvents.emit(VideoEvent.VideoReady)
        }
    }

    fun onVideoError(what: Int, extra: Int) {
        launchWithErrorHandling {
            _videoEvents.emit(VideoEvent.PlaybackError(what, extra))
            _videoEvents.emit(VideoEvent.ShowError("Video playback error: what=$what, extra=$extra"))
        }
    }

    fun onPlayStateChanged(isPlaying: Boolean) {
        val currentState = _videoState.value
        _videoState.value = currentState.copy(isPlaying = isPlaying)
    }

    fun createShareUri(
        videoPath: String,
        packageName: String,
        context: android.content.Context? = null
    ): Uri? {
        return try {
            val videoFile = File(videoPath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context?.let { ctx ->
                    FileProvider.getUriForFile(ctx, "$packageName.fileprovider", videoFile)
                } ?: Uri.fromFile(videoFile)
            } else {
                Uri.fromFile(videoFile)
            }
        } catch (e: Exception) {
            launchWithErrorHandling {
                _videoEvents.emit(VideoEvent.ShowError("Failed to create share URI: ${e.message}"))
            }
            null
        }
    }

    fun shareVideo(videoPath: String, packageName: String, context: android.content.Context? = null) {
        launchWithErrorHandling {
            createShareUri(videoPath, packageName, context)?.let { uri ->
                _videoEvents.emit(VideoEvent.ShareVideo(uri))
            }
        }
    }
}