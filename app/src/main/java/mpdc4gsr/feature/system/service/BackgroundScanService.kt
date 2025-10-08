package mpdc4gsr.feature.system.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.csl.irCamera.R
import com.mpdc4gsr.module.user.ble.BleDeviceManager
import kotlinx.coroutines.*
import mpdc4gsr.feature.main.ui.MainComposeActivity
import kotlin.coroutines.CoroutineContext

class BackgroundScanService : Service(), CoroutineScope {
    private val serviceJob = SupervisorJob()

    companion object {
        private const val TAG = "BackgroundDeviceScanning"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "background_device_scanning"
        private const val CHANNEL_NAME = "Background Device Scanning"

        // Scanning configuration
        private const val SCAN_DURATION_MS = 30000L // 30 seconds
        private const val SCAN_INTERVAL_MS = 120000L // 2 minutes between scans
        private const val IDLE_SCAN_INTERVAL_MS = 300000L // 5 minutes when idle

        // Actions
        const val ACTION_START_SCANNING = "mpdc4gsr.action.START_BACKGROUND_SCANNING"
        const val ACTION_STOP_SCANNING = "mpdc4gsr.action.STOP_BACKGROUND_SCANNING"
        const val ACTION_PAUSE_SCANNING = "mpdc4gsr.action.PAUSE_BACKGROUND_SCANNING"
        const val ACTION_RESUME_SCANNING = "mpdc4gsr.action.RESUME_BACKGROUND_SCANNING"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.IO + serviceJob
    private var bleDeviceManager: BleDeviceManager? = null
    private var scanningJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isScanning = false
    private var isPaused = false
    private var scanCount = 0
    private var lastDeviceCount = 0
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundDeviceScanningService = this@BackgroundDeviceScanningService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        initializeBleManager()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_SCANNING -> startBackgroundScanning()
            ACTION_STOP_SCANNING -> stopBackgroundScanning()
            ACTION_PAUSE_SCANNING -> pauseBackgroundScanning()
            ACTION_RESUME_SCANNING -> resumeBackgroundScanning()
            else -> startBackgroundScanning()
        }
        return START_STICKY // Restart if killed by system
    }

    private fun initializeBleManager() {
        try {
            bleDeviceManager = BleDeviceManager(applicationContext)
            bleDeviceManager?.initialize(enableNordicBackend = true)
        } catch (e: Exception) {
        }
    }

    private fun startBackgroundScanning() {
        if (isScanning) {
            return
        }
        isScanning = true
        isPaused = false
        scanCount = 0
        val notification = createOngoingNotification()
        startForeground(NOTIFICATION_ID, notification)
        scanningJob = launch {
            performBackgroundScanning()
        }
    }

    private fun stopBackgroundScanning() {
        isScanning = false
        isPaused = false
        scanningJob?.cancel()
        scanningJob = null
        bleDeviceManager?.stopDeviceDiscovery()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun pauseBackgroundScanning() {
        isPaused = true
        bleDeviceManager?.stopDeviceDiscovery()
        updateNotification("Background scanning paused")
    }

    private fun resumeBackgroundScanning() {
        isPaused = false
        updateNotification("Background scanning active")
    }

    private suspend fun performBackgroundScanning() = withContext(Dispatchers.IO) {
        while (isActive && isScanning) {
            try {
                if (!isPaused) {
                    performSingleScan()
                }
                // Use different intervals based on activity and results
                val interval = if (isPaused) {
                    IDLE_SCAN_INTERVAL_MS
                } else if (lastDeviceCount > 0) {
                    SCAN_INTERVAL_MS // More frequent when devices are found
                } else {
                    IDLE_SCAN_INTERVAL_MS // Less frequent when no devices
                }
                delay(interval)
            } catch (e: Exception) {
                delay(SCAN_INTERVAL_MS) // Standard interval on error
            }
        }
    }

    private suspend fun performSingleScan() {
        try {
            scanCount++
            val scanStartTime = System.currentTimeMillis()
            updateNotification("Scanning for devices... (#$scanCount)")
            // Start scanning
            bleDeviceManager?.startDeviceDiscovery()
            // Let scan run for configured duration
            delay(SCAN_DURATION_MS)
            // Stop scanning
            bleDeviceManager?.stopDeviceDiscovery()
            // Get current device count (this would need to be exposed by BleDeviceManager)
            val deviceCount = getCurrentDeviceCount()
            lastDeviceCount = deviceCount
            val scanDuration = System.currentTimeMillis() - scanStartTime
            Log.i(
                TAG,
                "Background scan #$scanCount completed in ${scanDuration}ms, found $deviceCount devices"
            )
            updateNotification("Found $deviceCount devices (Scan #$scanCount)")
        } catch (e: Exception) {
            updateNotification("Scan error occurred")
        }
    }

    private fun getCurrentDeviceCount(): Int {
        return bleDeviceManager?.getDiscoveredDeviceCount() ?: 0
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous background scanning for BLE devices"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createOngoingNotification(): Notification {
        val intent = Intent(this, MainComposeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, BackgroundDeviceScanningService::class.java).apply {
            action = ACTION_STOP_SCANNING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, BackgroundDeviceScanningService::class.java).apply {
                action = ACTION_RESUME_SCANNING
            }
            val resumePendingIntent = PendingIntent.getService(
                this, 2, resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action(
                R.drawable.ic_play,
                "Resume",
                resumePendingIntent
            )
        } else {
            val pauseIntent = Intent(this, BackgroundDeviceScanningService::class.java).apply {
                action = ACTION_PAUSE_SCANNING
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 3, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Pause",
                pausePendingIntent
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Background Device Scanning")
            .setContentText("Continuously scanning for BLE devices")
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(pauseResumeAction)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_close,
                    "Stop",
                    stopPendingIntent
                )
            )
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = createOngoingNotification().apply {
            // Update content text
        }
        val updatedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Background Device Scanning")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, updatedNotification)
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$TAG::BackgroundScanWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L)
            }
        } catch (e: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        isScanning = false
        scanningJob?.cancel()
        bleDeviceManager?.stopDeviceDiscovery()
        bleDeviceManager?.release()
        releaseWakeLock()
        // Cancel all coroutines launched in this service's scope
        serviceJob.cancel()
        super.onDestroy()
    }

    // Public interface for controlling the service
    fun getStatus(): ServiceStatus {
        return ServiceStatus(
            isScanning = isScanning,
            isPaused = isPaused,
            scanCount = scanCount,
            lastDeviceCount = lastDeviceCount
        )
    }

    data class ServiceStatus(
        val isScanning: Boolean,
        val isPaused: Boolean,
        val scanCount: Int,
        val lastDeviceCount: Int
    )
}