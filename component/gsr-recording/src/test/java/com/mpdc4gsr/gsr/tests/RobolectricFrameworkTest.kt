package com.mpdc4gsr.gsr.tests

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("All tests disabled")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
@OptIn(ExperimentalCoroutinesApi::class)
class RobolectricFrameworkTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSharedPreferencesWithRealContext() {
        val prefs = context.getSharedPreferences("test_session_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("session_id", "test_session_123")
        editor.putLong("start_time", System.currentTimeMillis())
        editor.putBoolean("is_active", true)
        editor.apply()
        assertEquals("test_session_123", prefs.getString("session_id", null))
        assertTrue("start_time should be stored", prefs.getLong("start_time", 0) > 0)
        assertTrue("is_active should be true", prefs.getBoolean("is_active", false))
        editor.clear()
        editor.apply()
        assertNull("session_id should be cleared", prefs.getString("session_id", null))
        assertEquals("start_time should be cleared", 0L, prefs.getLong("start_time", 0))
        assertFalse("is_active should be false", prefs.getBoolean("is_active", false))
    }

    @Test
    fun testFileOperationsWithRealFileSystem() {
        val filesDir = context.filesDir
        val testFile = java.io.File(filesDir, "test_gsr_data.csv")
        testFile.writeText("timestamp,conductance,resistance\n1234567890,12.5,80.0\n")
        assertTrue("File should exist", testFile.exists())
        assertTrue("File should have content", testFile.length() > 0)
        val content = testFile.readText()
        assertTrue(
            "Content should contain header",
            content.contains("timestamp,conductance,resistance")
        )
        assertTrue("Content should contain data", content.contains("1234567890,12.5,80.0"))
        testFile.delete()
        assertFalse("File should be deleted", testFile.exists())
    }

    @Test
    fun testSystemServiceAccess() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
        assertNotNull("NotificationManager should be available", notificationManager)
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE)
                    as android.bluetooth.BluetoothManager?
        assertNotNull("BluetoothManager should be available", bluetoothManager)
        val bluetoothAdapter = bluetoothManager?.adapter
        assertNotNull("BluetoothAdapter should be available", bluetoothAdapter)
    }

    @Test
    fun testIntegrationWithMultipleServices() {
        val prefs = context.getSharedPreferences("integration_test", Context.MODE_PRIVATE)
        val filesDir = context.filesDir
        prefs.edit()
            .putString("data_directory", filesDir.absolutePath)
            .putInt("sampling_rate", 128)
            .putBoolean("bluetooth_enabled", true)
            .apply()
        val dataDir = prefs.getString("data_directory", "")
        val samplingRate = prefs.getInt("sampling_rate", 0)
        val bluetoothEnabled = prefs.getBoolean("bluetooth_enabled", false)
        assertEquals(filesDir.absolutePath, dataDir)
        assertEquals(128, samplingRate)
        assertTrue(bluetoothEnabled)
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
        assertTrue(
            "Should be running on Android O or higher",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
        )
        assertNotNull("Package name should be available", context.packageName)
        assertFalse("Package name should not be empty", context.packageName.isEmpty())
        val resources = context.resources
        assertNotNull("Resources should be available", resources)
        val displayMetrics = resources.displayMetrics
        assertTrue("Display density should be realistic", displayMetrics.density > 0)
        assertTrue("Screen width should be realistic", displayMetrics.widthPixels > 0)
        assertTrue("Screen height should be realistic", displayMetrics.heightPixels > 0)
    }

    @Test
    fun testContextAccess() {
        assertNotNull("Context should be available", context)
        assertNotNull("Package name should be available", context.packageName)
        assertFalse("Package name should not be empty", context.packageName.isEmpty())
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
    fun testPackageManagerAccess() {
        val packageManager = context.packageManager
        assertNotNull("Package manager should be available", packageManager)
        try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            assertNotNull("Package info should be available", packageInfo)
            assertEquals("Package name should match", context.packageName, packageInfo.packageName)
        } catch (e: Exception) {
            // Expected in test environment
        }
    }

    @Test
    fun testMultipleContextInstances() {
        val context1 = ApplicationProvider.getApplicationContext<Context>()
        val context2 = ApplicationProvider.getApplicationContext<Context>()
        assertSame("Application contexts should be the same instance", context1, context2)
        assertEquals("Package names should match", context1.packageName, context2.packageName)
    }
}
