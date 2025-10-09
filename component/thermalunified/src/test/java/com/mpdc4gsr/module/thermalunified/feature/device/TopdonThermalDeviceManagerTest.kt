package com.mpdc4gsr.module.thermalunified.feature.device

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TopdonThermalDeviceManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun statusUpdatesWhenProbeReportsConnection() = runTest {
        val probe = FakeHardwareProbe()
        val manager = TopdonThermalDeviceManager(context, this, probe)
        try {
            probe.topdonConnected = true
            probe.usbAttached = true

            testScheduler.advanceTimeBy(800)
            testScheduler.runCurrent()

            val status = manager.status.value
            assertTrue(status.isConnected)
            assertEquals("Topdon TC001 - Connected", status.deviceLabel)
        } finally {
            manager.cancelMonitoringForTests()
        }
    }

    @Test
    fun manualCalibrationWithoutCommandRecordsError() = runTest {
        val manager = TopdonThermalDeviceManager(context, this, FakeHardwareProbe())
        try {
            val result = manager.triggerManualCalibration()

            assertTrue(result.isFailure)
            assertNotNull(manager.status.value.lastError)
        } finally {
            manager.cancelMonitoringForTests()
        }
    }

    private class FakeHardwareProbe : TopdonThermalDeviceManager.HardwareProbe {
        @Volatile
        var topdonConnected: Boolean = false

        @Volatile
        var usbAttached: Boolean = false

        override fun isTopdonConnected(): Boolean = topdonConnected

        override fun isUsbAttached(): Boolean = usbAttached
    }
}
