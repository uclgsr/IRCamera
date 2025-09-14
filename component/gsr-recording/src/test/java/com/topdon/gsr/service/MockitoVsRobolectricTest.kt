package com.topdon.gsr.service

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
@OptIn(ExperimentalCoroutinesApi::class)
class MockitoVsRobolectricTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }


    @Test
    fun testSharedPreferencesWithRealContext() {
        // Instead of mocking Context and SharedPreferences, we use real ones
        val prefs = context.getSharedPreferences("test_session_prefs", Context.MODE_PRIVATE)

        // Test actual SharedPreferences behavior
        val editor = prefs.edit()
        editor.putString("session_id", "test_session_123")
        editor.putLong("start_time", System.currentTimeMillis())
        editor.putBoolean("is_active", true)
        editor.apply()

        // Verify actual persistence behavior
        assertEquals("test_session_123", prefs.getString("session_id", null))
        assertTrue("start_time should be stored", prefs.getLong("start_time", 0) > 0)
        assertTrue("is_active should be true", prefs.getBoolean("is_active", false))

        // Test clearing data
        editor.clear()
        editor.apply()

        assertNull("session_id should be cleared", prefs.getString("session_id", null))
        assertEquals("start_time should be cleared", 0L, prefs.getLong("start_time", 0))
        assertFalse("is_active should be false", prefs.getBoolean("is_active", false))
    }


    @Test
    fun testFileOperationsWithRealFileSystem() {
        // Use real file operations instead of mocking File, FileWriter, etc.
        val filesDir = context.filesDir
        val testFile = java.io.File(filesDir, "test_gsr_data.csv")

        // Test actual file writing
        testFile.writeText("timestamp,conductance,resistance\n1234567890,12.5,80.0\n")

        assertTrue("File should exist", testFile.exists())
        assertTrue("File should have content", testFile.length() > 0)

        // Test actual file reading
        val content = testFile.readText()
        assertTrue("Content should contain header", content.contains("timestamp,conductance,resistance"))
        assertTrue("Content should contain data", content.contains("1234567890,12.5,80.0"))

        // Cleanup
        testFile.delete()
        assertFalse("File should be deleted", testFile.exists())
    }


    @Test
    fun testSystemServiceAccess() {
        // No mocking needed - Robolectric provides real service implementations
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        assertNotNull("NotificationManager should be available", notificationManager)

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE)
                as android.bluetooth.BluetoothManager?
        assertNotNull("BluetoothManager should be available", bluetoothManager)

        // Test actual service behavior (not mocked responses)
        val bluetoothAdapter = bluetoothManager?.adapter
        assertNotNull("BluetoothAdapter should be available", bluetoothAdapter)
    }


    @Test
    fun testIntegrationWithMultipleServices() {
        // Real integration between multiple Android components
        val prefs = context.getSharedPreferences("integration_test", Context.MODE_PRIVATE)
        val filesDir = context.filesDir

        // Store configuration in SharedPreferences
        prefs.edit()
            .putString("data_directory", filesDir.absolutePath)
            .putInt("sampling_rate", 128)
            .putBoolean("bluetooth_enabled", true)
            .apply()

        // Use configuration to create file
        val dataDir = prefs.getString("data_directory", "")
        val samplingRate = prefs.getInt("sampling_rate", 0)
        val bluetoothEnabled = prefs.getBoolean("bluetooth_enabled", false)

        assertEquals(filesDir.absolutePath, dataDir)
        assertEquals(128, samplingRate)
        assertTrue(bluetoothEnabled)

        // Create actual file based on configuration
        val configFile = java.io.File(dataDir, "config.json")
        configFile.writeText(
            """
            {
                "sampling_rate": $samplingRate,
                "bluetooth_enabled": $bluetoothEnabled,
                "data_directory": "$dataDir"
            }
            """.trimIndent(),
        )

        assertTrue("Config file should exist", configFile.exists())
        val configContent = configFile.readText()
        assertTrue("Config should contain sampling rate", configContent.contains("128"))
        assertTrue("Config should contain bluetooth setting", configContent.contains("true"))

        configFile.delete()
    }


    @Test
    fun testAndroidSpecificBehavior() {
        // Test Android version-specific behavior
        assertTrue(
            "Should be running on Android O or higher",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
        )

        // Test package information
        assertNotNull("Package name should be available", context.packageName)
        assertFalse("Package name should not be empty", context.packageName.isEmpty())

        // Test resource access
        val resources = context.resources
        assertNotNull("Resources should be available", resources)

        val displayMetrics = resources.displayMetrics
        assertTrue("Display density should be realistic", displayMetrics.density > 0)
        assertTrue("Screen width should be realistic", displayMetrics.widthPixels > 0)
        assertTrue("Screen height should be realistic", displayMetrics.heightPixels > 0)
    }
}
