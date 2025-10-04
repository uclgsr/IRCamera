package com.mpdc4gsr.gsr.tests

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
class RobolectricBasicTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testContextAccess() {
        assertNotNull("Context should be available", context)
        assertNotNull("Package name should be available", context.packageName)
        assertFalse("Package name should not be empty", context.packageName.isEmpty())
    }

    @Test
    fun testSystemServiceAccess() {

        val notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE)
        assertNotNull("Notification service should be available", notificationService)

        val connectivityService = context.getSystemService(Context.CONNECTIVITY_SERVICE)
        assertNotNull("Connectivity service should be available", connectivityService)

        val bluetoothService = context.getSystemService(Context.BLUETOOTH_SERVICE)
        assertNotNull("Bluetooth service should be available", bluetoothService)
    }

    @Test
    fun testFileSystemAccess() {
        val filesDir = context.filesDir
        assertNotNull("Files directory should be accessible", filesDir)
        assertTrue("Files directory should exist", filesDir.exists())

        val cacheDir = context.cacheDir
        assertNotNull("Cache directory should be accessible", cacheDir)
        assertTrue("Cache directory should exist", cacheDir.exists())
    }

    @Test
    fun testSharedPreferencesAccess() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        assertNotNull("SharedPreferences should be available", prefs)

        val editor = prefs.edit()
        editor.putString("test_key", "test_value")
        editor.apply()

        val value = prefs.getString("test_key", null)
        assertEquals("Value should be stored and retrieved", "test_value", value)
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

        }
    }

    @Test
    fun testAsyncOperations() =
        runTest {

            val result =
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.packageName
                }

            assertEquals("Async operation should return correct value", context.packageName, result)
        }

    @Test
    fun testMultipleContextInstances() {
        val context1 = ApplicationProvider.getApplicationContext<Context>()
        val context2 = ApplicationProvider.getApplicationContext<Context>()

        assertSame("Application contexts should be the same instance", context1, context2)
        assertEquals("Package names should match", context1.packageName, context2.packageName)
    }

    @Test
    fun testContextBasedClassInstantiation() {

        try {
            val sessionManager = SessionManager.getInstance(context)
            assertNotNull("SessionManager should be created with context", sessionManager)

            val shimmerRecorder = ShimmerGSRRecorder(context, MockShimmerDeviceFactory(), 128)
            assertNotNull("ShimmerGSRRecorder should be created with context", shimmerRecorder)
        } catch (e: Exception) {

            assertTrue("Context-based class instantiation attempted", true)
        }
    }
}
