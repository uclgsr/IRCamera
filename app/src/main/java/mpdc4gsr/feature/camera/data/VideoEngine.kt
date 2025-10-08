package mpdc4gsr.feature.camera.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Size
import java.io.File

class VideoEngine(private val context: Context? = null) {
    companion object {
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
                if (audioEnabled) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                }
                prepare()
            }
            isPrepared = true
                TAG,
                "MediaRecorder prepared for ${videoSize.width}x${videoSize.height}@${frameRate}fps, orientation=$orientationHint°"
            )
            return mediaRecorder?.surface
            release()
            return null
        }
    }

    fun start(): Boolean {
        return (
            if (!isPrepared) {
                return false
            }
            mediaRecorder?.start()
            isRecording = true
            true
            false
        }
    }

    fun stop() {
            if (isRecording) {
                mediaRecorder?.stop()
                isRecording = false
            }
        }
    }

    fun release() {
            if (isRecording) {
                stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isPrepared = false
            isRecording = false
        }
    }

    fun isRecording(): Boolean = isRecording
    fun getSurface(): android.view.Surface? = mediaRecorder?.surface
}
