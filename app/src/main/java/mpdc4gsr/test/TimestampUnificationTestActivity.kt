package mpdc4gsr.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.R
import mpdc4gsr.sensors.TimestampManager
import mpdc4gsr.sensors.TimeSynchronizationService
import kotlin.math.abs

class TimestampUnificationTestActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "TimestampUnificationTest"
    }

    private lateinit var resultTextView: TextView
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timestamp_sync_verification)

        resultTextView = findViewById(R.id.result_text_view)
        testButton = findViewById(R.id.start_test_button)

        testButton.text = "Test Unified Timestamps"

        testButton.setOnClickListener {
            lifecycleScope.launch {
                runUnificationTest()
            }
        }

        updateResultText("Timestamp Unification Test\nReady to validate unified timestamp system...")
    }

    private suspend fun runUnificationTest() {
        updateResultText("Running unified timestamp validation...\n")

        try {
            val tempDir = cacheDir.resolve("unification_test_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val timeSyncService = TimeSynchronizationService()
            val sessionRef = timeSyncService.initializeSession(tempDir.absolutePath)

            appendResultText("✓ Session initialized\n")
            appendResultText("Session start: ${sessionRef.sessionStartSystemMs}ms\n\n")

            val testResults = mutableListOf<String>()

            for (i in 1..10) {
                val timestampRecord = TimestampManager.createTimestampRecord()

                val wallClockFromManager = TimestampManager.convertMonotonicToWallClock(timestampRecord.systemNanos)

                val difference = abs(wallClockFromManager - timestampRecord.systemTimeMs)

                testResults.add("Test $i: diff=${difference}ms")

                if (difference > 10) {
                    testResults.add("  ⚠️ WARNING: Difference exceeds 10ms threshold")
                }

                delay(100)
            }

            appendResultText("Timestamp Consistency Results:\n")
            testResults.forEach { result ->
                appendResultText("$result\n")
            }

            val differences = testResults.mapNotNull {
                it.substringAfter("diff=", "").substringBefore("ms", "").toLongOrNull()
            }
            val maxDifference = differences.maxOrNull() ?: 0L

            val finalResult = if (maxDifference <= 10) {
                "✅ PASS: All timestamps consistent within 10ms tolerance"
            } else {
                "❌ FAIL: Timestamp inconsistency detected (max: ${maxDifference}ms)"
            }

            appendResultText("\n$finalResult\n")

            tempDir.deleteRecursively()

        } catch (e: Exception) {
            appendResultText("❌ Test error: ${e.message}\n")
            Log.e(TAG, "Unification test failed", e)
        }
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