package mpdc4gsr.feature.camera.data
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import android.util.Size
import java.io.File
class VideoEngine(private val context: Context? = null) {
    companion object {
        private const val TAG = "VideoEngine"
    }
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var isPrepared = false
    fun prepare(
        outputFile: File,
        videoSize: Size,
        frameRate: Int,
        bitRate: Int,
        audioEnabled: Boolean,
        orientationHint: Int = 0,
        enableStabilization: Boolean = true
    ): android.view.Surface? {
        try {
            release()
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                if (audioEnabled) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile.absolutePath)
                setVideoEncodingBitRate(bitRate)
                setVideoFrameRate(frameRate)
                setVideoSize(videoSize.width, videoSize.height)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setOrientationHint(orientationHint)
                AppLogger.d(TAG, "Video orientation hint set to: $orientationHint degrees")
                if (audioEnabled) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                }
                prepare()
            }
            isPrepared = true
            Log.i(
                TAG,
                "MediaRecorder prepared for ${videoSize.width}x${videoSize.height}@${frameRate}fps, orientation=$orientationHint°"
            )
            return mediaRecorder?.surface
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to prepare MediaRecorder", e)
            release()
            return null
        }
    }
    fun start(): Boolean {
        return try {
            if (!isPrepared) {
                AppLogger.e(TAG, "MediaRecorder not prepared")
                return false
            }
            mediaRecorder?.start()
            isRecording = true
            AppLogger.i(TAG, "Video recording started")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start video recording", e)
            false
        }
    }
    fun stop() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
                isRecording = false
                AppLogger.i(TAG, "Video recording stopped")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop video recording", e)
        }
    }
    fun release() {
        try {
            if (isRecording) {
                stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isPrepared = false
            isRecording = false
            AppLogger.d(TAG, "MediaRecorder released")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error releasing MediaRecorder", e)
        }
    }
    fun isRecording(): Boolean = isRecording
    fun getSurface(): android.view.Surface? = mediaRecorder?.surface
}
