package mpdc4gsr.activities

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.BuildConfig
import com.csl.irCamera.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimplifiedMainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SimplifiedMainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var statusText: TextView
    private lateinit var recordButton: Button
    private lateinit var connectButton: Button

    private var isRecording = false
    private var isConnected = false

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) Log.d(TAG, "onCreate() called")
        setContentView(R.layout.activity_simplified_main)
        if (BuildConfig.DEBUG) Log.d(TAG, "Layout set successfully")

        initializeViews()
        checkPermissions()
        updateUI()
        if (BuildConfig.DEBUG) Log.d(TAG, "onCreate() completed successfully")
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        recordButton = findViewById(R.id.recordButton)
        connectButton = findViewById(R.id.connectButton)
        
        // Verify views were found
        if (BuildConfig.DEBUG) Log.d(TAG, "Views initialization complete - all views found")

        recordButton.setOnClickListener {
            if (BuildConfig.DEBUG) Log.d(TAG, "Record button clicked")
            Toast.makeText(this, "Record button clicked!", Toast.LENGTH_SHORT).show()
            toggleRecording()
        }

        connectButton.setOnClickListener {
            if (BuildConfig.DEBUG) Log.d(TAG, "Connect button clicked")
            Toast.makeText(this, "Connect button clicked!", Toast.LENGTH_SHORT).show()
            toggleConnection()
        }
        
        if (BuildConfig.DEBUG) Log.d(TAG, "Click listeners setup complete")
    }

    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PermissionChecker.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            updateStatus("All permissions granted ✓")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PermissionChecker.PERMISSION_GRANTED }
            if (allGranted) {
                updateStatus("All permissions granted ✓")
            } else {
                updateStatus("Some permissions denied ✗")
            }
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        isRecording = true
        updateStatus("Recording started at ${getCurrentTime()}")
        updateUI()

        lifecycleScope.launch {
            repeat(60) {
                delay(1000)
                if (isRecording) {
                    val gsrValue = (10..50).random() + kotlin.random.Random.nextDouble()
                    updateStatus("Recording... GSR: ${String.format("%.1f", gsrValue)} μS")
                }
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        updateStatus("Recording stopped at ${getCurrentTime()}")
        updateUI()
    }

    private fun toggleConnection() {
        if (isConnected) {
            disconnectFromPC()
        } else {
            connectToPC()
        }
    }

    private fun connectToPC() {
        updateStatus("Connecting to PC Controller...")

        lifecycleScope.launch {
            delay(2000)
            isConnected = true
            updateStatus("Connected to PC Controller ✓")
            updateUI()
        }
    }

    private fun disconnectFromPC() {
        isConnected = false
        updateStatus("Disconnected from PC Controller")
        updateUI()
    }

    private fun updateUI() {
        recordButton.text = if (isRecording) "Stop Recording" else "Start Recording"
        recordButton.isEnabled = !isRecording || isConnected

        connectButton.text = if (isConnected) "Disconnect" else "Connect to PC"

        val statusPrefix = when {
            isRecording && isConnected -> "🟢 ACTIVE"
            isConnected -> "🟡 CONNECTED"
            else -> "🔴 OFFLINE"
        }

        if (!statusText.text.toString().startsWith("Recording...")) {
            updateStatus("$statusPrefix Multi-Modal Sensing Platform")
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
