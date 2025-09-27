package mpdc4gsr.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.TimeSynchronizationService
import mpdc4gsr.sensors.TimestampManager
import java.io.File
import kotlin.math.abs

class TimestampSyncVerificationActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "TimestampSyncVerification"
        private const val SYNC_TOLERANCE_MS = 5L
    }

    private lateinit var resultTextView: TextView
    private lateinit var startTestButton: Button
    private lateinit var timeSyncService: TimeSynchronizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timestamp_sync_verification)

        resultTextView = findViewById(R.id.result_text_view)
        startTestButton = findViewById(R.id.start_test_button)

        timeSyncService = TimeSynchronizationService()

        startTestButton.setOnClickListener {
            lifecycleScope.launch {
                runTimestampSyncVerificationTest()
            }
        }

        updateResultText("Timestamp Synchronization Verification\nReady to test...")
    }

    private suspend fun runTimestampSyncVerificationTest() {
        updateResultText("Starting timestamp synchronization verification test...\n")

        try {
            val tempDir = File(cacheDir, "timestamp_test_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val sessionRef = timeSyncService.initializeSession(tempDir.absolutePath)
            appendResultText("✓ Session initialized with reference: ${sessionRef.sessionStartSystemMs}ms\n")

            appendResultText("Creating SessionSync markers for multi-modal alignment verification...\n")

            val syncEvents = mutableListOf<SyncTestEvent>()

            syncEvents.add(simulateRGBFrameCapture())
            delay(2)
            syncEvents.add(simulateGSRSample())
            delay(1)
            syncEvents.add(simulateThermalFrame())
            delay(3)
            syncEvents.add(simulateSharpEvent("hand_clap"))

            appendResultText("\nAnalyzing timestamp alignment:\n")

            val alignmentResults = analyzeTimestampAlignment(syncEvents)
            appendResultText(alignmentResults)

            val finalResult = if (isTimestampAlignmentValid(syncEvents)) {
                "✅ PASS: All sensor timestamps are synchronized within ${SYNC_TOLERANCE_MS}ms tolerance"
            } else {
                "❌ FAIL: Timestamp synchronization exceeds tolerance"
            }

            appendResultText("\n$finalResult\n")

            tempDir.deleteRecursively()

        } catch (e: Exception) {
            appendResultText("❌ Test error: ${e.message}\n")
            Log.e(TAG, "Timestamp sync verification test failed", e)
        }
    }

    private suspend fun simulateRGBFrameCapture(): SyncTestEvent {
        val timestamp = TimestampManager.createTimestampRecord()
        timeSyncService.logSyncEvent(
            "RGB_FRAME_TEST", mapOf(
                "sensor" to "rgb_camera",
                "test_event" to "frame_capture",
                "expected_sync" to "true"
            )
        )

        appendResultText("RGB: Frame captured at ${timestamp.systemTimeMs}ms\n")
        return SyncTestEvent("RGB_CAMERA", timestamp.systemNanos, timestamp.systemTimeMs)
    }

    private suspend fun simulateGSRSample(): SyncTestEvent {
        val timestamp = TimestampManager.createTimestampRecord()
        timeSyncService.logSyncEvent(
            "GSR_SAMPLE_TEST", mapOf(
                "sensor" to "gsr_shimmer",
                "test_event" to "conductance_reading",
                "expected_sync" to "true"
            )
        )

        appendResultText("GSR: Sample recorded at ${timestamp.systemTimeMs}ms\n")
        return SyncTestEvent("GSR_SHIMMER", timestamp.systemNanos, timestamp.systemTimeMs)
    }

    private suspend fun simulateThermalFrame(): SyncTestEvent {
        val timestamp = TimestampManager.createTimestampRecord()
        timeSyncService.logSyncEvent(
            "THERMAL_FRAME_TEST", mapOf(
                "sensor" to "thermal_topdon",
                "test_event" to "temperature_frame",
                "expected_sync" to "true"
            )
        )

        appendResultText("THERMAL: Frame processed at ${timestamp.systemTimeMs}ms\n")
        return SyncTestEvent("THERMAL_TOPDON", timestamp.systemNanos, timestamp.systemTimeMs)
    }

    private suspend fun simulateSharpEvent(eventType: String): SyncTestEvent {
        val timestamp = TimestampManager.createTimestampRecord()
        timeSyncService.logSyncEvent(
            "SHARP_EVENT_TEST", mapOf(
                "event_type" to eventType,
                "test_event" to "multi_modal_trigger",
                "expected_sync" to "true"
            )
        )

        appendResultText("EVENT: $eventType detected at ${timestamp.systemTimeMs}ms\n")
        return SyncTestEvent("SHARP_EVENT", timestamp.systemNanos, timestamp.systemTimeMs)
    }

    private fun analyzeTimestampAlignment(events: List<SyncTestEvent>): String {
        val result = StringBuilder()
        result.append("Timestamp Alignment Analysis:\n")

        for (i in events.indices) {
            for (j in i + 1 until events.size) {
                val event1 = events[i]
                val event2 = events[j]

                val diffMs = abs(event1.wallClockMs - event2.wallClockMs)
                val diffNs = abs(event1.timestampNs - event2.timestampNs) / 1_000_000

                result.append("${event1.sensorType} ↔ ${event2.sensorType}: ")
                result.append("${diffMs}ms wall-clock, ${diffNs}ms monotonic\n")
            }
        }

        return result.toString()
    }

    private fun isTimestampAlignmentValid(events: List<SyncTestEvent>): Boolean {
        for (i in events.indices) {
            for (j in i + 1 until events.size) {
                val diffMs = abs(events[i].wallClockMs - events[j].wallClockMs)
                if (diffMs > SYNC_TOLERANCE_MS) {
                    return false
                }
            }
        }
        return true
    }

    private fun updateResultText(text: String) {
        runOnUiThread {
            resultTextView.text = text
        }
    }

    private fun appendResultText(text: String) {
        runOnUiThread {
            resultTextView.append(text)
        }
    }
}

private data class SyncTestEvent(
    val sensorType: String,
    val timestampNs: Long,
    val wallClockMs: Long
)