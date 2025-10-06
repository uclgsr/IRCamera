package com.mpdc4gsr.gsr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mpdc4gsr.gsr.R
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MultiModalRecordingService : Service() {
    companion object {
        private const val TAG = "MultiModalService"
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "gsr_recording_channel"
        private const val ACTION_START_RECORDING = "action_start_recording"
        private const val ACTION_STOP_RECORDING = "action_stop_recording"
        private const val ACTION_SYNC_EVENT = "action_sync_event"
        private const val EXTRA_SESSION_ID = "extra_session_id"
        private const val EXTRA_PARTICIPANT_ID = "extra_participant_id"
        private const val EXTRA_STUDY_NAME = "extra_study_name"
        private const val EXTRA_EVENT_TYPE = "extra_event_type"
        fun startRecording(
            context: Context,
            sessionId: String,
            participantId: String? = null,
            studyName: String? = null,
        ) {
            val intent =
                Intent(context, MultiModalRecordingService::class.java).apply {
                    action = ACTION_START_RECORDING
                    putExtra(EXTRA_SESSION_ID, sessionId)
                    putExtra(EXTRA_PARTICIPANT_ID, participantId)
                    putExtra(EXTRA_STUDY_NAME, studyName)
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRecording(context: Context) {
            val intent =
                Intent(context, MultiModalRecordingService::class.java).apply {
                    action = ACTION_STOP_RECORDING
                }
            context.startService(intent)
        }

        fun triggerSyncEvent(
            context: Context,
            eventType: String,
        ) {
            val intent =
                Intent(context, MultiModalRecordingService::class.java).apply {
                    action = ACTION_SYNC_EVENT
                    putExtra(EXTRA_EVENT_TYPE, eventType)
                }
            context.startService(intent)
        }
    }

    private lateinit var gsrRecorder: GSRRecorder
    private lateinit var sessionManager: SessionManager
    private var isRecording = false
    private var currentSessionId: String? = null
    private val gsrListener =
        object : GSRRecorder.GSRRecordingListener {
            override fun onRecordingStarted(sessionInfo: SessionInfo) {
                Log.i(TAG, "GSR recording started: ${sessionInfo.sessionId}")
                updateNotification("Recording GSR data...")
            }

            override fun onRecordingStopped(sessionInfo: SessionInfo) {
                Log.i(TAG, "GSR recording stopped: ${sessionInfo.sessionId}")
                isRecording = false
                currentSessionId = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            override fun onSampleRecorded(sample: GSRSample) {
                if (sample.sampleIndex % 1280 == 0L) {
                    updateNotification("Recording... ${sample.sampleIndex} samples")
                }
            }

            override fun onSyncMarkAdded(syncMark: SyncMark) {
                Log.d(TAG, "Sync mark added: ${syncMark.eventType}")
            }

            override fun onError(error: String) {
                Log.e(TAG, "GSR recording error: $error")
            }
        }

    override fun onCreate() {
        super.onCreate()
        gsrRecorder = GSRRecorder(this, ShimmerDeviceFactoryResolver.createFactory(this))
        sessionManager = SessionManager.getInstance(this)
        gsrRecorder.addListener(gsrListener)
        createNotificationChannel()
        Log.d(TAG, "MultiModalRecordingService created")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_NOT_STICKY
                val participantId = intent.getStringExtra(EXTRA_PARTICIPANT_ID)
                val studyName = intent.getStringExtra(EXTRA_STUDY_NAME)
                startRecording(sessionId, participantId, studyName)
            }

            ACTION_STOP_RECORDING -> {
                stopRecording()
            }

            ACTION_SYNC_EVENT -> {
                val eventType = intent.getStringExtra(EXTRA_EVENT_TYPE) ?: "UNKNOWN"
                triggerSyncEvent(eventType)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    private fun startRecording(
        sessionId: String,
        participantId: String?,
        studyName: String?,
    ) {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress")
            return
        }
        sessionManager.createSession(sessionId, participantId, studyName)
        startForeground(NOTIFICATION_ID, createNotification("Starting recording..."))
        CoroutineScope(Dispatchers.IO).launch {
            if (gsrRecorder.startRecording(sessionId, participantId, studyName)) {
                isRecording = true
                currentSessionId = sessionId
                Log.i(TAG, "Multi-modal recording started: $sessionId")
            } else {
                Log.e(TAG, "Failed to start GSR recording")
                stopSelf()
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "No recording in progress")
            return
        }
        val session = gsrRecorder.stopRecording()
        session?.let {
            sessionManager.completeSession(it.sessionId)
        }
        Log.i(TAG, "Multi-modal recording stopped")
    }

    private fun triggerSyncEvent(eventType: String) {
        if (isRecording) {
            gsrRecorder.triggerSyncEvent(eventType)
            Log.d(TAG, "Sync event triggered: $eventType")
        } else {
            Log.w(TAG, "Cannot trigger sync event - not recording")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "GSR Recording",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Multi-modal physiological data recording"
                    setSound(null, null)
                }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Multi-Modal Recording")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_fast_forward)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        gsrRecorder.removeListener(gsrListener)
        if (isRecording) {
            stopRecording()
        }
        Log.d(TAG, "MultiModalRecordingService destroyed")
    }
}
