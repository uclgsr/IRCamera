package mpdc4gsr.feature.system.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.csl.irCamera.R
import com.mpdc4gsr.module.user.ble.BleDeviceManager
import kotlinx.coroutines.*
import mpdc4gsr.feature.main.ui.MainComposeActivity
import kotlin.coroutines.CoroutineContext

class BackgroundScanService : Service(), CoroutineScope {
    private val serviceJob = SupervisorJob()

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "background_device_scanning"
        private const val CHANNEL_NAME = "Background Device Scanning"
        private const val SCAN_DURATION_MS = 30000L
        private const val SCAN_INTERVAL_MS = 120000L
        private const val IDLE_SCAN_INTERVAL_MS = 300000L

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
        fun getService(): BackgroundScanService = this@BackgroundScanService
    }

    override fun onBind(intent: Intent): IBinder = binder

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
        return START_STICKY
    }

    private fun initializeBleManager() {
        bleDeviceManager = BleDeviceManager(applicationContext)
        bleDeviceManager?.initialize(enableNordicBackend = true)
    }

    private fun startBackgroundScanning() {
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
            if (!isPaused) {
                performSingleScan()
            }
            val interval = if (isPaused) {
                IDLE_SCAN_INTERVAL_MS
            } else if (lastDeviceCount > 0) {
                SCAN_INTERVAL_MS
            } else {
                IDLE_SCAN_INTERVAL_MS
            }
            delay(interval)
        }
    }

    private suspend fun performSingleScan() {
        scanCount++
        updateNotification("Scanning for devices... (#$scanCount)")
        bleDeviceManager?.startDeviceDiscovery()
        delay(SCAN_DURATION_MS)
        bleDeviceManager?.stopDeviceDiscovery()
        val deviceCount = getCurrentDeviceCount()
        lastDeviceCount = deviceCount
        updateNotification("Found $deviceCount devices (Scan #$scanCount)")
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
        val stopIntent = Intent(this, BackgroundScanService::class.java).apply {
            action = ACTION_STOP_SCANNING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, BackgroundScanService::class.java).apply {
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
            val pauseIntent = Intent(this, BackgroundScanService::class.java).apply {
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
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BackgroundDeviceScanning::BackgroundScanWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    override fun onDestroy() {
        isScanning = false
        scanningJob?.cancel()
        bleDeviceManager?.stopDeviceDiscovery()
        bleDeviceManager?.release()
        releaseWakeLock()
        serviceJob.cancel()
        super.onDestroy()
    }

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