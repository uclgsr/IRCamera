package mpdc4gsr.sensors.thermal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*

/**
 * Unit tests for ThermalRecorder functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThermalRecorderTest {

    private lateinit var context: Context
    private lateinit var thermalRecorder: ThermalRecorder
    private lateinit var testSessionDir: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        thermalRecorder = ThermalRecorder(context)

        // Create test session directory
        testSessionDir = "${context.cacheDir}/test_session_${System.currentTimeMillis()}"
        File(testSessionDir).mkdirs()
    }

    @Test
    fun testStartStopRecording() = runTest {
        // Test starting recording
        val started = thermalRecorder.startRecording(testSessionDir, saveImages = false)
        assertTrue("Recording should start successfully", started)
        assertTrue("Should be recording", thermalRecorder.isRecording())

        // Test stopping recording
        val stopped = thermalRecorder.stopRecording()
        assertTrue("Recording should stop successfully", stopped)
        assertFalse("Should not be recording", thermalRecorder.isRecording())
    }

    @Test
    fun testFrameProcessing() = runTest {
        val latch = CountDownLatch(1)
        var processedStats: ThermalRecorder.ThermalFrameStats? = null

        // Set up frame listener
        thermalRecorder.setFrameListener(object : ThermalRecorder.ThermalFrameListener {
            override fun onFrameProcessed(stats: ThermalRecorder.ThermalFrameStats) {
                processedStats = stats
                latch.countDown()
            }

            override fun onError(error: String) {
                fail("Should not have error: $error")
            }
        })

        // Start recording
        thermalRecorder.startRecording(testSessionDir, saveImages = false)

        // Process a test frame
        val width = 256
        val height = 192
        val frameData = ByteArray(width * height)

        // Fill with test data (simulate temperature range 20-30°C)
        for (i in frameData.indices) {
            frameData[i] = (20 + (i % 10)).toByte() // Values 20-29
        }

        thermalRecorder.processFrameFromIntensity(
            frameData, width, height,
            minTempRange = 20f, maxTempRange = 30f
        )

        // Wait for processing
        assertTrue("Should process frame", latch.await(2, TimeUnit.SECONDS))

        // Verify results
        assertNotNull("Should have processed stats", processedStats)
        processedStats?.let { stats ->
            assertEquals("Should be first frame", 1L, stats.frameSequence)
            assertTrue("Min temp should be in range", stats.minTemp >= 20f)
            assertTrue("Max temp should be in range", stats.maxTemp <= 30f)
            assertTrue("Avg temp should be in range", stats.avgTemp >= 20f && stats.avgTemp <= 30f)
            assertTrue("Should have valid pixels", stats.pixelCount > 0)
        }
    }

    @Test
    fun testCsvOutput() = runTest {
        // Start recording
        thermalRecorder.startRecording(testSessionDir, saveImages = false)

        // Process a frame
        val frameData = ByteArray(100) // Small test frame
        for (i in frameData.indices) {
            frameData[i] = 25.toByte() // 25°C
        }

        thermalRecorder.processFrameFromIntensity(
            frameData, 10, 10,
            minTempRange = 20f, maxTempRange = 30f
        )

        // Stop recording to flush data
        thermalRecorder.stopRecording()

        // Check CSV file was created
        val sessionDir = File(testSessionDir)
        val csvFiles =
            sessionDir.listFiles { _, name -> name.startsWith("thermal_stats_") && name.endsWith(".csv") }

        assertNotNull("Should have CSV files", csvFiles)
        assertTrue("Should have at least one CSV file", csvFiles.isNotEmpty())

        // Check CSV content
        val csvFile = csvFiles.first()
        val lines = csvFile.readLines()

        assertTrue("Should have header + at least one data line", lines.size >= 2)

        val header = lines[0]
        assertTrue("Header should contain timestamp", header.contains("timestamp_ns"))
        assertTrue("Header should contain min_temp", header.contains("min_temp_c"))
        assertTrue("Header should contain avg_temp", header.contains("avg_temp_c"))
        assertTrue("Header should contain max_temp", header.contains("max_temp_c"))

        val dataLine = lines[1]
        val parts = dataLine.split(",")
        assertTrue("Data line should have required columns", parts.size >= 5)

        // Verify data format
        val timestamp = parts[0].toLongOrNull()
        assertNotNull("Timestamp should be valid long", timestamp)

        val frameSeq = parts[1].toLongOrNull()
        assertEquals("Frame sequence should be 1", 1L, frameSeq)

        val minTemp = parts[2].toFloatOrNull()
        assertNotNull("Min temp should be valid float", minTemp)
    }

    @Test
    fun testImageSaving() = runTest {
        // Start recording with image saving
        thermalRecorder.startRecording(testSessionDir, saveImages = true)

        // Process a frame
        val frameData = ByteArray(100)
        for (i in frameData.indices) {
            frameData[i] = (i % 256).toByte()
        }

        thermalRecorder.processFrameFromIntensity(
            frameData, 10, 10,
            minTempRange = 0f, maxTempRange = 255f
        )

        // Give some time for async image saving
        Thread.sleep(500)

        thermalRecorder.stopRecording()

        // Check image files were created
        val sessionDir = File(testSessionDir)
        val imageFiles = sessionDir.listFiles { _, name ->
            name.startsWith("thermal_frame_") && name.endsWith(".png")
        }

        assertNotNull("Should have image files", imageFiles)
        assertTrue("Should have at least one image file", imageFiles.isNotEmpty())

        // Verify image file is not empty
        val imageFile = imageFiles.first()
        assertTrue("Image file should not be empty", imageFile.length() > 0)
    }

    @Test
    fun testFrameCounter() = runTest {
        thermalRecorder.startRecording(testSessionDir, saveImages = false)

        assertEquals("Initial frame count should be 0", 0L, thermalRecorder.getFrameCount())

        // Process multiple frames
        val frameData = ByteArray(100)
        repeat(5) { i ->
            frameData[0] = i.toByte()
            thermalRecorder.processFrameFromIntensity(
                frameData, 10, 10,
                minTempRange = 0f, maxTempRange = 100f
            )
        }

        // Give time for processing
        Thread.sleep(100)

        assertEquals("Frame count should be 5", 5L, thermalRecorder.getFrameCount())

        thermalRecorder.stopRecording()
    }

    @Test
    fun testErrorHandling() = runTest {
        val latch = CountDownLatch(1)
        var errorReceived = false

        thermalRecorder.setFrameListener(object : ThermalRecorder.ThermalFrameListener {
            override fun onFrameProcessed(stats: ThermalRecorder.ThermalFrameStats) {}

            override fun onError(error: String) {
                errorReceived = true
                latch.countDown()
            }
        })

        // Try to process frame without starting recording
        val frameData = ByteArray(100)
        thermalRecorder.processFrameFromIntensity(
            frameData, 10, 10,
            minTempRange = 0f, maxTempRange = 100f
        )

        // Should not generate error for frame processing when not recording
        // (it just ignores the frame)
        assertFalse("Should not be recording", thermalRecorder.isRecording())
    }
}
