package mpdc4gsr.camera

import android.os.Bundle
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.camera.core.ModeManager
import mpdc4gsr.camera.core.SamsungDeviceCompatibility

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
            if (camera2System.initialize()) {                val caps = camera2System.getDeviceCaps()                // Enable Samsung Stage3/Level3 processing for supported devices
                if (SamsungDeviceCompatibility.isStage3Compatible() && caps?.supportsRaw == true) {
                    camera2System.configureStage3Processing(true)}"
                    )
                    runOnUiThread {
                        Toast.makeText(this@DemoActivity, "Stage3/Level3 DNG Recording Enabled", Toast.LENGTH_LONG)
                            .show()
                    }
                } else {} (Stage3/Level3 not available)"
                    )
                }
            } else {            }
        }
    }

    private fun setupCallbacks() {
        camera2System.onError = { error ->            runOnUiThread {
                Toast.makeText(this, "Camera error: $error", Toast.LENGTH_SHORT).show()
            }
        }

        camera2System.onProgress = { message ->        }

        camera2System.onModeChanged = { mode ->            runOnUiThread {
                Toast.makeText(this, "Mode: $mode", Toast.LENGTH_SHORT).show()
            }
        }

        camera2System.onRecordingStarted = {            runOnUiThread {
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            }
        }

        camera2System.onRecordingStopped = {            runOnUiThread {
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testModeSwitch() {
        lifecycleScope.launch {

            if (camera2System.switchMode(ModeManager.CameraMode.RAW_50MP)) {                delay(2000)

                if (camera2System.startRecording("demo_session_raw")) {
                    delay(5000)
                    camera2System.stopRecording()
                }
            }

            delay(1000)

            if (camera2System.switchMode(ModeManager.CameraMode.VIDEO_4K)) {                delay(2000)

                if (camera2System.startRecording("demo_session_video")) {
                    delay(5000)
                    camera2System.stopRecording()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            delay(3000)
            testModeSwitch()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        camera2System.release()
    }
}
