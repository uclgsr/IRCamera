package com.topdon.tc001.camera

import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.camera.core.ModeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DemoActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "DemoActivity"
    }

    private lateinit var textureView: TextureView
    private lateinit var camera2System: Camera2System
    private lateinit var buttonRaw: Button
    private lateinit var buttonVideo: Button
    private lateinit var buttonPreview: Button
    private lateinit var buttonRecord: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textureView = TextureView(this)
        setContentView(textureView)

        camera2System = Camera2System(this, textureView)
        setupCallbacks()

        lifecycleScope.launch {
            if (camera2System.initialize()) {
                Log.i(TAG, "Camera2System initialized successfully")
                val caps = camera2System.getDeviceCaps()
                Log.i(
                    TAG,
                    "Device capabilities: RAW=${caps?.supportsRaw}, 4K60=${caps?.supports4k60}"
                )
            } else {
                Log.e(TAG, "Failed to initialize Camera2System")
            }
        }
    }

    private fun setupCallbacks() {
        camera2System.onError = { error ->
            Log.e(TAG, "Camera error: $error")
            runOnUiThread {
                Toast.makeText(this, "Camera error: $error", Toast.LENGTH_SHORT).show()
            }
        }

        camera2System.onProgress = { message ->
            Log.i(TAG, "Progress: $message")
        }

        camera2System.onModeChanged = { mode ->
            Log.i(TAG, "Mode changed to: $mode")
            runOnUiThread {
                Toast.makeText(this, "Mode: $mode", Toast.LENGTH_SHORT).show()
            }
        }

        camera2System.onRecordingStarted = {
            Log.i(TAG, "Recording started")
            runOnUiThread {
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            }
        }

        camera2System.onRecordingStopped = {
            Log.i(TAG, "Recording stopped")
            runOnUiThread {
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testModeSwitch() {
        lifecycleScope.launch {

            if (camera2System.switchMode(ModeManager.CameraMode.RAW_50MP)) {
                Log.i(TAG, "Switched to RAW mode")
                delay(2000)

                if (camera2System.startRecording("demo_session_raw")) {
                    delay(5000) // Record for 5 seconds
                    camera2System.stopRecording()
                }
            }

            delay(1000)

            if (camera2System.switchMode(ModeManager.CameraMode.VIDEO_4K)) {
                Log.i(TAG, "Switched to Video mode")
                delay(2000)

                if (camera2System.startRecording("demo_session_video")) {
                    delay(5000) // Record for 5 seconds
                    camera2System.stopRecording()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            delay(3000) // Wait for camera to initialize
            testModeSwitch()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        camera2System.release()
    }
}
