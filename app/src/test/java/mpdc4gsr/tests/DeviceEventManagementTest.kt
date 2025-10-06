package mpdc4gsr.tests

import android.hardware.usb.UsbDevice
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceEventManagementTest {
    private lateinit var mockDevice: UsbDevice

    @Before
    fun setUp() {
        mockDevice = mockk(relaxed = true)
    }

    @Test
    fun `deviceConnectionState initial value is null`() = runTest {
        val initialState = DeviceEventManager.deviceConnectionState.value
        assertNull("Initial device connection state should be null", initialState)
    }

    @Test
    fun `socketConnectionState initial value is null`() = runTest {
        val initialState = DeviceEventManager.socketConnectionState.value
        assertNull("Initial socket connection state should be null", initialState)
    }

    @Test
    fun `emitDeviceConnection updates deviceConnectionState`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val job = launch(testDispatcher) {
            DeviceEventManager.deviceConnectionState.collect { state ->
                if (state != null) {
                    assertTrue("Device should be connected", state.isConnected)
                    assertEquals("Device should match mock device", mockDevice, state.device)
                }
            }
        }
        DeviceEventManager.emitDeviceConnection(true, mockDevice)
        val state = DeviceEventManager.deviceConnectionState.value
        assertNotNull("State should not be null", state)
        assertTrue("Device should be connected", state!!.isConnected)
        assertEquals("Device should match mock device", mockDevice, state.device)
        job.cancel()
    }

    @Test
    fun `emitDeviceConnection with null device`() = runTest {
        DeviceEventManager.emitDeviceConnection(false, null)
        val state = DeviceEventManager.deviceConnectionState.value
        assertNotNull("State should not be null", state)
        assertFalse("Device should be disconnected", state!!.isConnected)
        assertNull("Device should be null", state.device)
    }

    @Test
    fun `emitSocketConnection updates socketConnectionState`() = runTest {
        DeviceEventManager.emitSocketConnection(true, false)
        val state = DeviceEventManager.socketConnectionState.value
        assertNotNull("State should not be null", state)
        assertTrue("Socket should be connected", state!!.isConnected)
        assertFalse("Should not be TS004", state.isTS004)
    }

    @Test
    fun `emitSocketConnection with TS004 flag`() = runTest {
        DeviceEventManager.emitSocketConnection(true, true)
        val state = DeviceEventManager.socketConnectionState.value
        assertNotNull("State should not be null", state)
        assertTrue("Socket should be connected", state!!.isConnected)
        assertTrue("Should be TS004", state.isTS004)
    }

    @Test
    fun `emitDeviceConnectionSync updates state synchronously`() = runTest {
        DeviceEventManager.emitDeviceConnectionSync(true, mockDevice)
        val state = DeviceEventManager.deviceConnectionState.value
        assertNotNull("State should not be null", state)
        assertTrue("Device should be connected", state!!.isConnected)
        assertEquals("Device should match mock device", mockDevice, state.device)
    }

    @Test
    fun `emitSocketConnectionSync updates state synchronously`() = runTest {
        DeviceEventManager.emitSocketConnectionSync(false, false)
        val state = DeviceEventManager.socketConnectionState.value
        assertNotNull("State should not be null", state)
        assertFalse("Socket should be disconnected", state!!.isConnected)
        assertFalse("Should not be TS004", state.isTS004)
    }

    @Test
    fun `emitDevicePermissionRequest emits to SharedFlow`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val collectedDevices = mutableListOf<UsbDevice>()
        val job = launch(testDispatcher) {
            DeviceEventManager.devicePermissionRequested.collect { device ->
                collectedDevices.add(device)
            }
        }
        DeviceEventManager.emitDevicePermissionRequest(mockDevice)
        assertEquals("Should have collected one device", 1, collectedDevices.size)
        assertEquals("Collected device should match mock", mockDevice, collectedDevices[0])
        job.cancel()
    }

    @Test
    fun `emitDevicePermissionRequestSync emits to SharedFlow synchronously`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val collectedDevices = mutableListOf<UsbDevice>()
        val job = launch(testDispatcher) {
            DeviceEventManager.devicePermissionRequested.collect { device ->
                collectedDevices.add(device)
            }
        }
        val result = DeviceEventManager.emitDevicePermissionRequestSync(mockDevice)
        assertTrue("tryEmit should succeed with active collector", result)
        assertEquals("Should have collected one device", 1, collectedDevices.size)
        assertEquals("Collected device should match mock", mockDevice, collectedDevices[0])
        job.cancel()
    }

    @Test
    fun `multiple emissions update state correctly`() = runTest {
        val device1 = mockk<UsbDevice>(relaxed = true)
        val device2 = mockk<UsbDevice>(relaxed = true)
        DeviceEventManager.emitDeviceConnection(true, device1)
        var state = DeviceEventManager.deviceConnectionState.value
        assertEquals("First device should be set", device1, state?.device)
        DeviceEventManager.emitDeviceConnection(false, null)
        state = DeviceEventManager.deviceConnectionState.value
        assertNull("Device should be null after disconnect", state?.device)
        assertFalse("Should be disconnected", state?.isConnected ?: true)
        DeviceEventManager.emitDeviceConnection(true, device2)
        state = DeviceEventManager.deviceConnectionState.value
        assertEquals("Second device should be set", device2, state?.device)
        assertTrue("Should be connected", state?.isConnected ?: false)
    }

    @Test
    fun `DeviceConnectionState data class equality`() {
        val state1 = DeviceEventManager.DeviceConnectionState(true, mockDevice)
        val state2 = DeviceEventManager.DeviceConnectionState(true, mockDevice)
        val state3 = DeviceEventManager.DeviceConnectionState(false, null)
        assertEquals("Same states should be equal", state1, state2)
        assertNotEquals("Different states should not be equal", state1, state3)
    }

    @Test
    fun `SocketConnectionState data class equality`() {
        val state1 = DeviceEventManager.SocketConnectionState(true, false)
        val state2 = DeviceEventManager.SocketConnectionState(true, false)
        val state3 = DeviceEventManager.SocketConnectionState(false, true)
        assertEquals("Same states should be equal", state1, state2)
        assertNotEquals("Different states should not be equal", state1, state3)
    }

    @Test
    fun `deviceConnectionState is a cold StateFlow`() = runTest {
        DeviceEventManager.emitDeviceConnection(true, mockDevice)
        val state1 = DeviceEventManager.deviceConnectionState.first()
        val state2 = DeviceEventManager.deviceConnectionState.first()
        assertEquals("StateFlow should provide same value on multiple collections", state1, state2)
    }

    @Test
    fun `socket connection state transitions correctly`() = runTest {
        DeviceEventManager.emitSocketConnection(false, false)
        var state = DeviceEventManager.socketConnectionState.value
        assertFalse("Should start disconnected", state?.isConnected ?: true)
        DeviceEventManager.emitSocketConnection(true, false)
        state = DeviceEventManager.socketConnectionState.value
        assertTrue("Should be connected", state?.isConnected ?: false)
        DeviceEventManager.emitSocketConnection(false, false)
        state = DeviceEventManager.socketConnectionState.value
        assertFalse("Should be disconnected again", state?.isConnected ?: true)
    }
}
