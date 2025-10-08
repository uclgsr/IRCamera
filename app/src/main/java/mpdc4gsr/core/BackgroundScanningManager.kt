package mpdc4gsr.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import mpdc4gsr.feature.system.service.BackgroundScanService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class BackgroundScanningManager(
    private val context: Context
) : DefaultLifecycleObserver, ServiceConnection {

    private var scanningService: BackgroundScanService? = null
    private var isBound = false
    private var isServiceStarted = false

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
        val intent = Intent(context, BackgroundScanService::class.java).apply {
            action = BackgroundScanService.ACTION_START_SCANNING
        }
        context.startForegroundService(intent)
        isServiceStarted = true
        bindService()
    }

    fun stopBackgroundScanning() {
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
    }

    fun pauseBackgroundScanning() {
        val intent = Intent(context, BackgroundScanService::class.java).apply {
            action = BackgroundScanService.ACTION_PAUSE_SCANNING
        }
        context.startService(intent)
    }

    fun resumeBackgroundScanning() {
        val intent = Intent(context, BackgroundScanService::class.java).apply {
            action = BackgroundScanService.ACTION_RESUME_SCANNING
        }
        context.startService(intent)
    }

    private fun bindService() {
        val intent = Intent(context, BackgroundScanService::class.java)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        if (isBound) {
            context.unbindService(this)
            isBound = false
            scanningService = null
            statusCallback?.onServiceDisconnected()
        }
    }

    fun getServiceStatus(): BackgroundScanService.ServiceStatus? {
        return scanningService?.getStatus()
    }

    fun isServiceRunning(): Boolean {
        return isServiceStarted && scanningService != null
    }

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

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isServiceStarted && !isBound) {
            bindService()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (isBound) {
            unbindService()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stopBackgroundScanning()
    }
}