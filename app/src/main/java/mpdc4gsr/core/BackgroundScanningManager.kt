package mpdc4gsr.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import mpdc4gsr.feature.system.service.BackgroundScanService

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
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_START_SCANNING
            }
            context.startForegroundService(intent)
            isServiceStarted = true
            // Bind to the service to get status updates
            bindService()
        } catch (e: Exception) {
        }
    }

    fun stopBackgroundScanning() {
        try {
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
        }
    }

    fun pauseBackgroundScanning() {
        try {
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_PAUSE_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
        }
    }

    fun resumeBackgroundScanning() {
        try {
            val intent = Intent(context, BackgroundScanService::class.java).apply {
                action = BackgroundScanService.ACTION_RESUME_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
        }
    }

    private fun bindService() {
        try {
            val intent = Intent(context, BackgroundScanService::class.java)
            context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
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
        val binder = service as BackgroundScanService.LocalBinder
        scanningService = binder.getService()
        isBound = true
        statusCallback?.onServiceConnected(scanningService!!)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
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