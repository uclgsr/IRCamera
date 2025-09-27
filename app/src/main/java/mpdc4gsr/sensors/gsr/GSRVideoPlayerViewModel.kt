package mpdc4gsr.sensors.gsr

import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GSRVideoPlayerViewModel : BaseViewModel() {

    private val _videoState = MutableLiveData<VideoState>()
    val videoState: LiveData<VideoState> = _videoState

    private val _videoInfo = MutableLiveData<VideoInfo>()
    val videoInfo: LiveData<VideoInfo> = _videoInfo

    private val _errorState = MutableLiveData<String?>()
    val errorState: LiveData<String?> = _errorState

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

    fun loadVideo(videoPath: String, packageName: String, context: android.content.Context? = null) {
        val videoFile = File(videoPath)
        
        if (!videoFile.exists()) {
            _errorState.value = "Video file does not exist"
            return
        }

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val authority = "$packageName.fileprovider"
            try {
                // Use context if provided, otherwise create URI differently
                context?.let { ctx ->
                    FileProvider.getUriForFile(ctx, authority, videoFile)
                } ?: Uri.fromFile(videoFile) // Fallback for older versions
            } catch (e: Exception) {
                _errorState.value = "Failed to create file URI: ${e.message}"
                return
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
        if (currentState != null) {
            _videoState.value = currentState.copy(isReady = true)
        }
    }

    fun onVideoError(what: Int, extra: Int) {
        _errorState.value = "Video playback error: what=$what, extra=$extra"
    }

    fun onPlayStateChanged(isPlaying: Boolean) {
        val currentState = _videoState.value
        if (currentState != null) {
            _videoState.value = currentState.copy(isPlaying = isPlaying)
        }
    }

    fun createShareUri(videoPath: String, packageName: String, context: android.content.Context? = null): Uri? {
        val videoFile = File(videoPath)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                // Use context if provided, otherwise fall back to Uri.fromFile
                context?.let { ctx ->
                    FileProvider.getUriForFile(ctx, "$packageName.fileprovider", videoFile)
                } ?: Uri.fromFile(videoFile)
            } catch (e: Exception) {
                null
            }
        } else {
            Uri.fromFile(videoFile)
        }
    }

    fun clearError() {
        _errorState.value = null
    }
}