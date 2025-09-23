package mpdc4gsr.test

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kotlinx.coroutines.delay
import com.kotlinx.coroutines.launch
import com.mpdc4gsr.controller.RecordingController
import com.mpdc4gsr.sensors.TimeSynchronizationService
import com.mpdc4gsr.sensors.TimestampManager
import com.mpdc4gsr.sensors.TimestampManager.TimestampRecord


class TimeSynchronizationTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TimeSyncTest"
    }

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var testButton: Button
    private lateinit var clearLogsButton: Button
    private lateinit var syncTestButton: Button

    private lateinit var timeSyncService: TimeSynchronizationService
    private var recordingController: RecordingController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            text = "Time Synchronization Test Ready"
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }

        testButton = Button(this).apply {
            text = "Test Unified Timestamp System"
            setOnClickListener { testUnifiedTimestampSystem() }
        }

        syncTestButton = Button(this).apply {
            text = "Test Cross-Sensor Sync Events"
            setOnClickListener { testCrossSensorSyncEvents() }
        }

        clearLogsButton = Button(this).apply {
            text = "Clear Logs"
            setOnClickListener { clearLogs() }
        }

        logText = TextView(this).apply {
            text = ""
            textSize = 12f
            setPadding(0, 16, 0, 0)
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.LTGRAY)
            setPadding(16, 16, 16, 16)
        }

        val scrollView = ScrollView(this).apply {
            addView(logText)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        layout.addView(statusText)
        layout.addView(testButton)
        layout.addView(syncTestButton)
        layout.addView(clearLogsButton)
        layout.addView(scrollView)

        setContentView(layout)


        timeSyncService = TimeSynchronizationService()
        recordingController = RecordingController(this, this)

        addLog("Time Synchronization Test Activity initialized")
    }

    private fun testUnifiedTimestampSystem() {
        lifecycleScope.launch {
            try {
                addLog("=== Testing Unified Timestamp System ===")


                addLog("1. Testing TimestampManager basic functionality...")
                val timestamp1 = TimestampManager.createTimestampRecord()
                delay(100)
                val timestamp2 = TimestampManager.createTimestampRecord()

                addLog("Timestamp 1: system_nanos=${timestamp1.systemNanos}")
                addLog("Timestamp 1: system_time_ms=${timestamp1.systemTimeMs}")
                addLog("Timestamp 1: session_relative_ms=${timestamp1.sessionRelativeMs}")

                addLog("Timestamp 2: system_nanos=${timestamp2.systemNanos}")
                val timeDiff = (timestamp2.systemNanos - timestamp1.systemNanos) / 1_000_000
                addLog("Time difference: ${timeDiff}ms (should be ~100ms)")


                addLog("\n2. Testing session initialization...")
                val testSessionDir = "/tmp/test_session"
                val sessionRef = timeSyncService.initializeSession(testSessionDir)

                addLog("Session reference initialized:")
                addLog("  - session_start_system_ms: ${sessionRef.sessionStartSystemMs}")
                addLog("  - session_start_monotonic_ns: ${sessionRef.sessionStartMonotonicNs}")
                addLog("  - boot_time_reference_ms: ${sessionRef.bootTimeReferenceMs}")


                addLog("\n3. Testing synchronized timestamps...")
                val syncTimestamp1 = timeSyncService.createSynchronizedTimestamp()
                delay(50)
                val syncTimestamp2 = timeSyncService.createSynchronizedTimestamp()

                addLog("Sync timestamp 1: ${syncTimestamp1.toCsvFormat()}")
                addLog("Sync timestamp 2: ${syncTimestamp2.toCsvFormat()}")

                val syncDiff = (syncTimestamp2.systemNanos - syncTimestamp1.systemNanos) / 1_000_000
                addLog("Sync time difference: ${syncDiff}ms (should be ~50ms)")


                addLog("\n4. Testing CSV header format...")
                addLog("CSV Header: ${TimestampRecord.getCsvHeader()}")

                statusText.text = "Unified Timestamp System Test Completed ✓"

            } catch (e: Exception) {
                addLog("ERROR during timestamp test: ${e.message}")
                statusText.text = "Timestamp Test Failed ✗"
            }
        }
    }

    private fun testCrossSensorSyncEvents() {
        lifecycleScope.launch {
            try {
                addLog("=== Testing Cross-Sensor Sync Events ===")


                addLog("1. Testing sync event emission...")
                timeSyncService.emitSyncEvent(
                    "test_sync_event", mapOf(
                        "test_type" to "integration_test",
                        "sensor_count" to "3"
                    )
                )

                delay(100)


                addLog("2. Testing multiple sync events...")
                val syncEvents = listOf("session_start", "flash_sync", "manual_marker", "session_end")

                syncEvents.forEach { eventType ->
                    timeSyncService.emitSyncEvent(
                        eventType, mapOf(
                            "sequence" to eventType,
                            "timestamp_test" to "true"
                        )
                    )
                    delay(25)
                }


                addLog("3. Testing RecordingController integration...")
                recordingController?.let { controller ->
                    val syncTimestamp = controller.createSynchronizedTimestamp()
                    addLog("RecordingController sync timestamp: ${syncTimestamp.systemTimeMs}ms")

                    val sessionRef = controller.getSessionTimestampReference()
                    if (sessionRef != null) {
                        addLog("RecordingController has session reference: ✓")
                    } else {
                        addLog("RecordingController session reference: null (no active session)")
                    }
                }

                addLog("4. Testing timestamp consistency validation...")
                recordingController?.let { controller ->
                    val timestamps = controller.validateTimestampConsistency()
                    addLog("Timestamp consistency check - sensor count: ${timestamps.size}")
                    timestamps.forEach { (sensor, timestamp) ->
                        addLog("  $sensor: $timestamp")
                    }
                }

                statusText.text = "Cross-Sensor Sync Event Test Completed ✓"

            } catch (e: Exception) {
                addLog("ERROR during sync event test: ${e.message}")
                statusText.text = "Sync Event Test Failed ✗"
            }
        }
    }

    private fun addLog(message: String) {
        Log.i(TAG, message)
        runOnUiThread {
            val currentText = logText.text.toString()
            logText.text = if (currentText.isEmpty()) {
                message
            } else {
                "$currentText\n$message"
            }


            (logText.parent as? ScrollView)?.post {
                (logText.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun clearLogs() {
        logText.text = ""
        statusText.text = "Logs cleared - Ready for testing"
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            timeSyncService.finalizeSession()
        } catch (e: Exception) {
            Log.w(TAG, "Error finalizing session on destroy", e)
        }
    }
}