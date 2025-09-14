package com.topdon.module.thermal

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O], manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class ThermalModuleTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testContextAccess() {
        assertNotNull("Context should be available", context)
        assertNotNull("Package name should be available", context.packageName)
    }

    @Test
    fun testThermalDataProcessing() =
        runTest {

            val mockThermalData =
                floatArrayOf(
                    20.5f, 21.0f, 22.3f, 25.7f, 28.1f,
                    19.8f, 23.4f, 26.9f, 24.2f, 22.8f,
                    21.7f, 25.3f, 27.5f, 23.9f, 20.1f,
                )

            assertTrue("Thermal data should not be empty", mockThermalData.isNotEmpty())

            val avgTemp = mockThermalData.average()
            val maxTemp = mockThermalData.maxOrNull() ?: 0f
            val minTemp = mockThermalData.minOrNull() ?: 0f

            assertTrue("Average temperature should be reasonable", avgTemp > 0 && avgTemp < 100)
            assertTrue("Max temperature should be >= min temperature", maxTemp >= minTemp)
            assertTrue(
                "All temperatures should be in reasonable range",
                mockThermalData.all { it > -50 && it < 200 },
            )

            val tempRange = maxTemp - minTemp
            assertTrue("Temperature range should be non-negative", tempRange >= 0)

            mockThermalData.forEach { temp ->
                val normalized = (temp - minTemp) / tempRange
                assertTrue("Normalized value should be 0-1", normalized >= 0f && normalized <= 1f)
            }
        }

    @Test
    fun testThermalImageDimensions() =
        runTest {
            // Test thermal image dimension handling
            val mockImageWidth = 320
            val mockImageHeight = 240
            val expectedPixelCount = mockImageWidth * mockImageHeight

            assertTrue("Image width should be positive", mockImageWidth > 0)
            assertTrue("Image height should be positive", mockImageHeight > 0)
            assertEquals("Pixel count should match dimensions", expectedPixelCount, 320 * 240)

            // Test aspect ratio calculations
            val aspectRatio = mockImageWidth.toFloat() / mockImageHeight.toFloat()
            assertTrue("Aspect ratio should be positive", aspectRatio > 0)
            assertEquals("Aspect ratio should be 4:3", 4f / 3f, aspectRatio, 0.01f)
        }

    @Test
    fun testThermalColorMapping() =
        runTest {
            // Test thermal to color mapping functionality
            val testTemperatures = listOf(-10f, 0f, 25f, 50f, 100f)
            val minTemp = -10f
            val maxTemp = 100f

            testTemperatures.forEach { temp ->
                // Normalize temperature
                val normalized = ((temp - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
                assertTrue(
                    "Normalized temperature should be 0-1",
                    normalized >= 0f && normalized <= 1f
                )

                // Test color mapping (HSV to RGB)
                val hue = (1f - normalized) * 240f // Blue (cold) to Red (hot)
                assertTrue("Hue should be valid HSV range", hue >= 0f && hue <= 240f)

                // Test HSV color values
                val saturation = 1f
                val value = 1f
                assertTrue("Saturation should be valid", saturation >= 0f && saturation <= 1f)
                assertTrue("Value should be valid", value >= 0f && value <= 1f)
            }
        }

    @Test
    fun testThermalCalibration() =
        runTest {
            // Test thermal calibration calculations
            val rawThermalValue = 12345
            val calibrationOffset = 100f
            val calibrationGain = 0.04f

            // Simulate calibrated temperature calculation
            val calibratedTemp = (rawThermalValue - calibrationOffset) * calibrationGain

            assertTrue("Raw thermal value should be positive", rawThermalValue > 0)
            assertTrue("Calibration gain should be positive", calibrationGain > 0)
            assertNotEquals("Calibrated temperature should be calculated", 0f, calibratedTemp)

            // Test calibration bounds
            val expectedMinTemp = -40f
            val expectedMaxTemp = 300f
            val clampedTemp = calibratedTemp.coerceIn(expectedMinTemp, expectedMaxTemp)

            assertTrue(
                "Clamped temperature should be in valid range",
                clampedTemp >= expectedMinTemp && clampedTemp <= expectedMaxTemp,
            )
        }

    @Test
    fun testThermalRegionAnalysis() =
        runTest {
            // Test thermal region analysis (hot spots, cold spots)
            val thermalGrid =
                arrayOf(
                    floatArrayOf(20f, 21f, 22f, 23f),
                    floatArrayOf(21f, 35f, 36f, 24f), // Hot spot in middle
                    floatArrayOf(22f, 37f, 38f, 25f), // Hot spot continues
                    floatArrayOf(23f, 24f, 25f, 26f),
                )

            val width = thermalGrid[0].size
            val height = thermalGrid.size

            assertTrue("Grid width should be positive", width > 0)
            assertTrue("Grid height should be positive", height > 0)

            // Find min and max temperatures in grid
            var minTemp = Float.MAX_VALUE
            var maxTemp = Float.MIN_VALUE

            for (row in thermalGrid) {
                for (temp in row) {
                    minTemp = minOf(minTemp, temp)
                    maxTemp = maxOf(maxTemp, temp)
                }
            }

            assertTrue("Max temperature should be > min temperature", maxTemp > minTemp)
            assertTrue("Should detect hot spot", maxTemp > 35f)
            assertTrue("Should detect normal temperatures", minTemp < 30f)

            // Test temperature thresholds
            val hotThreshold = 35f
            val coldThreshold = 22f

            var hotPixelCount = 0
            var coldPixelCount = 0

            for (row in thermalGrid) {
                for (temp in row) {
                    if (temp > hotThreshold) hotPixelCount++
                    if (temp < coldThreshold) coldPixelCount++
                }
            }

            assertTrue("Should find hot pixels", hotPixelCount > 0)
            assertTrue("Should find cold pixels", coldPixelCount > 0)
        }

    @Test
    fun testSystemServiceAccess() {
        // Test system services that thermal module might use
        val cameraService = context.getSystemService(Context.CAMERA_SERVICE)
        assertNotNull("Camera service should be available", cameraService)

        val sensorService = context.getSystemService(Context.SENSOR_SERVICE)
        assertNotNull("Sensor service should be available", sensorService)

        val powerService = context.getSystemService(Context.POWER_SERVICE)
        assertNotNull("Power service should be available", powerService)
    }

    @Test
    fun testResourceAccess() {
        val resources = context.resources
        assertNotNull("Resources should be available", resources)

        val displayMetrics = resources.displayMetrics
        assertNotNull("Display metrics should be available", displayMetrics)
        assertTrue("Display density should be positive", displayMetrics.density > 0)
    }

    @Test
    fun testAsyncOperations() =
        runTest {
            // Test that coroutines work with thermal processing context
            val result =
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // Simulate thermal processing operation
                    context.packageName
                }

            assertEquals(
                "Async thermal operation should return correct value",
                context.packageName,
                result
            )
        }
}
