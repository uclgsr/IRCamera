package mpdc4gsr.camera.core

import android.media.MediaRecorder
import android.util.Size
import java.io.File

class VideoEngine {
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

            mediaRecorder =
                MediaRecorder().apply {
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


                    setOrientationHint(orientationHint)                    if (audioEnabled) {
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioEncodingBitRate(128000)
                        setAudioSamplingRate(44100)
                    }

                    prepare()
                }

            isPrepared = true            return mediaRecorder?.surface
        } catch (e: Exception) {            release()
            return null
        }
    }

    fun start(): Boolean {
        return try {
            if (!isPrepared) {                return false
            }

            mediaRecorder?.start()
            isRecording = true            true
        } catch (e: Exception) {            false
        }
    }

    fun stop() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
                isRecording = false            }
        } catch (e: Exception) {        }
    }

    fun release() {
        try {
            if (isRecording) {
                stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isPrepared = false
            isRecording = false        } catch (e: Exception) {        }
    }

    fun isRecording(): Boolean = isRecording

    fun getSurface(): android.view.Surface? = mediaRecorder?.surface
}
