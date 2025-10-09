package mpdc4gsr.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class BackgroundScanningManager(
    private val context: Context
) : DefaultLifecycleObserver, ServiceConnection {
    companion object {
        private const val TAG = "BackgroundScanningManager"
    }

    private var scanningService: BackgroundDeviceScanningService? = null
    private var isBound = false
    private var isServiceStarted = false

    // Callback interface for service status updates
    interface ServiceStatusCallback {
        fun onServiceConnected(service: BackgroundDeviceScanningService)
        fun onServiceDisconnected()
        fun onServiceStatusChanged(status: BackgroundDeviceScanningService.ServiceStatus)
    }

    private var statusCallback: ServiceStatusCallback? = null
    fun setStatusCallback(callback: ServiceStatusCallback?) {
        this.statusCallback = callback
    }

    fun startBackgroundScanning() {
        try {
            val intent = Intent(context, BackgroundDeviceScanningService::class.java).apply {
                action = BackgroundDeviceScanningService.ACTION_START_SCANNING
            }
            context.startForegroundService(intent)
            isServiceStarted = true
            // Bind to the service to get status updates
            bindService()
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("BackgroundScanningManager", "Unexpected Exception in BackgroundScanningManager catch block", e)
        }
    }

    fun stopBackgroundScanning() {
        try {
            if (isBound) {
                unbindService()
            }
            if (isServiceStarted) {
                val intent = Intent(context, BackgroundDeviceScanningService::class.java).apply {
                    action = BackgroundDeviceScanningService.ACTION_STOP_SCANNING
                }
                context.startService(intent)
                isServiceStarted = false
            }
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("BackgroundScanningManager", "Unexpected Exception in BackgroundScanningManager catch block", e)
        }
    }

    fun pauseBackgroundScanning() {
        try {
            val intent = Intent(context, BackgroundDeviceScanningService::class.java).apply {
                action = BackgroundDeviceScanningService.ACTION_PAUSE_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("BackgroundScanningManager", "Unexpected Exception in BackgroundScanningManager catch block", e)
        }
    }

    fun resumeBackgroundScanning() {
        try {
            val intent = Intent(context, BackgroundDeviceScanningService::class.java).apply {
                action = BackgroundDeviceScanningService.ACTION_RESUME_SCANNING
            }
            context.startService(intent)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("BackgroundScanningManager", "Unexpected Exception in BackgroundScanningManager catch block", e)
        }
    }

    private fun bindService() {
        try {
            val intent = Intent(context, BackgroundDeviceScanningService::class.java)
            context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger.e("BackgroundScanningManager", "Unexpected Exception in BackgroundScanningManager catch block", e)
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
            mpdc4gsr.core.utils.AppLogger.e("BackgroundScanningManager", "Unexpected Exception in BackgroundScanningManager catch block", e)
        }
    }

    fun getServiceStatus(): BackgroundDeviceScanningService.ServiceStatus? {
        return scanningService?.getStatus()
    }

    fun isServiceRunning(): Boolean {
        return isServiceStarted && scanningService != null
    }

    // ServiceConnection implementation
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as BackgroundDeviceScanningService.LocalBinder
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