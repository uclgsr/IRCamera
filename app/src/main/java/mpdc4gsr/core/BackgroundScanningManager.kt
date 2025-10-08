package mpdc4gsr.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import mpdc4gsr.feature.system.service.BackgroundScanService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class BackgroundScanningManager(
    private val context: Context
) : DefaultLifecycleObserver, ServiceConnection {
    companion object {
        private const val TAG = "BackgroundScanningManager"
    }

    private var scanningService: BackgroundScanService? = null
    private var isBound = false
    private var isServiceStarted = false

    // Callback interface for service status updates
    interface ServiceStatusCallback {
        fun onServiceConnected(service: BackgroundScanService)
        fun onServiceDisconnected()
        fun onServiceStatusChanged(status: BackgroundScanService.ServiceStatus)
    }

    private var statusCallback: ServiceStatusCallback? = null
    fun setStatusCallback(callback: ServiceStatusCallback?) {
        this.statusCallback = callback
    }

    fun startBackgroundScanning() {
        try {
            AppLogger.i(TAG, "Starting background device scanning service")
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_START_SCANNING
            }
            context.startForegroundService(intent)
            isServiceStarted = true
            // Bind to the service to get status updates
            bindService()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start background scanning service", e)
        }
    }

    fun stopBackgroundScanning() {
        try {
            AppLogger.i(TAG, "Stopping background device scanning service")
            if (isBound) {
                unbindService()
            }
            if (isServiceStarted) {
                val intent = Intent(context, BackgroundScanService::class.java).apply {
                    action = BackgroundScanService.ACTION_STOP_SCANNING
                }
                context.startService(intent)
                isServiceStarted = false
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop background scanning service", e)
        }
    }

    fun pauseBackgroundScanning() {
        try {
            AppLogger.d(TAG, "Pausing background device scanning")
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_PAUSE_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to pause background scanning", e)
        }
    }

    fun resumeBackgroundScanning() {
        try {
            AppLogger.d(TAG, "Resuming background device scanning")
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_RESUME_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to resume background scanning", e)
        }
    }

    private fun bindService() {
        try {
            val intent = Intent(context, BackgroundScanService::class.java)
            context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to bind to background scanning service", e)
        }
    }

    private fun unbindService() {
        try {
            if (isBound) {
                context.unbindService(this)
                isBound = false
                scanningService = null
                statusCallback?.onServiceDisconnected()
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to unbind from background scanning service", e)
        }
    }

    fun getServiceStatus(): BackgroundScanService.ServiceStatus? {
        return scanningService?.getStatus()
    }

    fun isServiceRunning(): Boolean {
        return isServiceStarted && scanningService != null
    }

    // ServiceConnection implementation
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        AppLogger.d(TAG, "Connected to background scanning service")
        val binder = service as BackgroundScanService.LocalBinder
        scanningService = binder.getService()
        isBound = true
        statusCallback?.onServiceConnected(scanningService!!)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        AppLogger.d(TAG, "Disconnected from background scanning service")
        scanningService = null
        isBound = false
        statusCallback?.onServiceDisconnected()
    }

    // Lifecycle observer implementation
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isServiceStarted && !isBound) {
            bindService()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Keep service running in background, but unbind to avoid leaks
        if (isBound) {
            unbindService()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stopBackgroundScanning()
    }
}