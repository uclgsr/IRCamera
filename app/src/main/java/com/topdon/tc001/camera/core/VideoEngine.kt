package com.topdon.tc001.camera.core

import android.media.MediaRecorder
import android.util.Log
import android.util.Size
import java.io.File

/**
 * VideoEngine wraps MediaRecorder for 4K video recording
 * Clean architecture component for Samsung S22 4K@60fps support
 */
class VideoEngine {
    
    companion object {
        private const val TAG = "VideoEngine"
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var isPrepared = false
    
    /**
     * Prepare MediaRecorder for 4K recording
     * Returns surface for camera session
     */
    fun prepare(
        outputFile: File,
        videoSize: Size,
        frameRate: Int,
        bitRate: Int,
        audioEnabled: Boolean
    ): android.view.Surface? {
        try {
            release() // Clean up any existing recorder
            
            mediaRecorder = MediaRecorder().apply {
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
                
                if (audioEnabled) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                }
                
                prepare()
            }
            
            isPrepared = true
            Log.i(TAG, "MediaRecorder prepared for ${videoSize.width}x${videoSize.height}@${frameRate}fps")
            
            return mediaRecorder?.surface
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare MediaRecorder", e)
            release()
            return null
        }
    }
    
    /**
     * Start video recording
     */
    fun start(): Boolean {
        return try {
            if (!isPrepared) {
                Log.e(TAG, "MediaRecorder not prepared")
                return false
            }
            
            mediaRecorder?.start()
            isRecording = true
            Log.i(TAG, "Video recording started")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video recording", e)
            false
        }
    }
    
    /**
     * Stop video recording
     */
    fun stop() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
                isRecording = false
                Log.i(TAG, "Video recording stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop video recording", e)
        }
    }
    
    /**
     * Release MediaRecorder resources
     */
    fun release() {
        try {
            if (isRecording) {
                stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isPrepared = false
            isRecording = false
            Log.d(TAG, "MediaRecorder released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Get recorder surface (must call prepare() first)
     */
    fun getSurface(): android.view.Surface? = mediaRecorder?.surface
}