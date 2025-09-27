package mpdc4gsr.sensors.gsr

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.BackgroundDeviceScanningService
import mpdc4gsr.core.BackgroundScanningManager

class BackgroundScanningDemoActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "BackgroundScanningDemo"
    }
    
    private lateinit var backgroundScanningManager: BackgroundScanningManager
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resumeButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple layout programmatically
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        statusTextView = TextView(this).apply {
            text = "Background scanning status: Not started"
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(statusTextView)
        
        startButton = Button(this).apply {
            text = "Start Background Scanning"
            setOnClickListener { startBackgroundScanning() }
        }
        layout.addView(startButton)
        
        stopButton = Button(this).apply {
            text = "Stop Background Scanning"
            setOnClickListener { stopBackgroundScanning() }
            isEnabled = false
        }
        layout.addView(stopButton)
        
        pauseButton = Button(this).apply {
            text = "Pause Scanning"
            setOnClickListener { pauseBackgroundScanning() }
            isEnabled = false
        }
        layout.addView(pauseButton)
        
        resumeButton = Button(this).apply {
            text = "Resume Scanning"
            setOnClickListener { resumeBackgroundScanning() }
            isEnabled = false
        }
        layout.addView(resumeButton)
        
        setContentView(layout)
        
        // Initialize background scanning manager
        backgroundScanningManager = BackgroundScanningManager(this)
        lifecycle.addObserver(backgroundScanningManager)
        
        // Set up status callback
        backgroundScanningManager.setStatusCallback(object : BackgroundScanningManager.ServiceStatusCallback {
            override fun onServiceConnected(service: BackgroundDeviceScanningService) {
                Log.d(TAG, "Background scanning service connected")
                updateUI()
                startStatusUpdates()
            }
            
            override fun onServiceDisconnected() {
                Log.d(TAG, "Background scanning service disconnected")
                updateUI()
            }
            
            override fun onServiceStatusChanged(status: BackgroundDeviceScanningService.ServiceStatus) {
                Log.d(TAG, "Service status changed: $status")
                updateUI()
            }
        })
        
        Log.i(TAG, "Background Scanning Demo Activity created")
    }
    
    private fun startBackgroundScanning() {
        Log.i(TAG, "User requested to start background scanning")
        backgroundScanningManager.startBackgroundScanning()
        updateUI()
    }
    
    private fun stopBackgroundScanning() {
        Log.i(TAG, "User requested to stop background scanning")
        backgroundScanningManager.stopBackgroundScanning()
        updateUI()
    }
    
    private fun pauseBackgroundScanning() {
        Log.i(TAG, "User requested to pause background scanning")
        backgroundScanningManager.pauseBackgroundScanning()
        updateUI()
    }
    
    private fun resumeBackgroundScanning() {
        Log.i(TAG, "User requested to resume background scanning")
        backgroundScanningManager.resumeBackgroundScanning()
        updateUI()
    }
    
    private fun updateUI() {
        runOnUiThread {
            val status = backgroundScanningManager.getServiceStatus()
            val isRunning = backgroundScanningManager.isServiceRunning()
            
            if (status != null && isRunning) {
                val statusText = buildString {
                    append("Background scanning: ")
                    when {
                        status.isPaused -> append("PAUSED")
                        status.isScanning -> append("ACTIVE")
                        else -> append("STOPPED")
                    }
                    append("\n")
                    append("Scan count: ${status.scanCount}\n")
                    append("Last devices found: ${status.lastDeviceCount}")
                }
                
                statusTextView.text = statusText
                
                startButton.isEnabled = !status.isScanning
                stopButton.isEnabled = status.isScanning
                pauseButton.isEnabled = status.isScanning && !status.isPaused
                resumeButton.isEnabled = status.isScanning && status.isPaused
                
            } else {
                statusTextView.text = "Background scanning: Not started"
                startButton.isEnabled = true
                stopButton.isEnabled = false
                pauseButton.isEnabled = false
                resumeButton.isEnabled = false
            }
        }
    }
    
    private fun startStatusUpdates() {
        lifecycleScope.launch {
            while (backgroundScanningManager.isServiceRunning()) {
                updateUI()
                delay(2000) // Update every 2 seconds
            }
        }
    }
    
    override fun onDestroy() {
        Log.i(TAG, "Background Scanning Demo Activity destroyed")
        super.onDestroy()
    }
}