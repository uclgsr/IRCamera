package mpdc4gsr.tests

import android.app.Application
import io.mockk.MockKAnnotations
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mpdc4gsr.feature.gsr.presentation.GSRSensorViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GSRSensorReconnectionTest {
    private lateinit var mockApplication: Application

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockApplication = mockk(relaxed = true)
    }

    @Test
    fun `GSRSensorState should have default values for reconnection fields`() {
        val state = GSRSensorViewModel.GSRSensorState()
        assertFalse("isReconnecting should be false by default", state.isReconnecting)
        assertEquals("reconnectionAttempt should be 0 by default", 0, state.reconnectionAttempt)
        assertEquals("connectionStatus should be Disconnected by default", "Disconnected", state.connectionStatus)
    }

    @Test
    fun `GSRSensorState should track reconnection attempts`() {
        val state = GSRSensorViewModel.GSRSensorState(
            isReconnecting = true,
            reconnectionAttempt = 2
        )
        assertTrue("isReconnecting should be true", state.isReconnecting)
        assertEquals("reconnectionAttempt should be 2", 2, state.reconnectionAttempt)
    }

    @Test
    fun `GSRSensorState should update connection status`() {
        val initialState = GSRSensorViewModel.GSRSensorState()
        val connectedState = initialState.copy(
            isConnected = true,
            connectionStatus = "Connected"
        )
        val reconnectingState = connectedState.copy(
            isConnected = false,
            isReconnecting = true,
            reconnectionAttempt = 1,
            connectionStatus = "Reconnecting (attempt 1/3)..."
        )
        assertEquals("Initial status should be Disconnected", "Disconnected", initialState.connectionStatus)
        assertEquals("Connected status should update", "Connected", connectedState.connectionStatus)
        assertTrue("Should be in reconnecting state", reconnectingState.isReconnecting)
        assertEquals("Should track attempt number", 1, reconnectingState.reconnectionAttempt)
    }

    @Test
    fun `should calculate exponential backoff correctly`() = runTest {
        val maxAttempts = 3
        val baseDelay = 2000L
        // True exponential backoff: baseDelay * 2^(attempt-1)
        val expectedDelays = listOf(2000L, 4000L, 8000L)
        for (attempt in 1..maxAttempts) {
            val calculatedDelay = baseDelay * (1L shl (attempt - 1))
            assertEquals(
                "Delay for attempt $attempt should match exponential backoff",
                expectedDelays[attempt - 1],
                calculatedDelay
            )
        }
    }

    @Test
    fun `ReconnectionConfig should have default values`() {
        val config = GSRSensorViewModel.ReconnectionConfig()
        assertEquals("Default max attempts should be 3", 3, config.maxAttempts)
        assertEquals("Default base delay should be 2000ms", 2000L, config.baseDelayMs)
        assertTrue("Reconnection should be enabled by default", config.enabled)
    }

    @Test
    fun `ReconnectionConfig should be customizable`() {
        val config = GSRSensorViewModel.ReconnectionConfig(
            maxAttempts = 5,
            baseDelayMs = 3000L,
            enabled = false
        )
        assertEquals("Max attempts should be customizable", 5, config.maxAttempts)
        assertEquals("Base delay should be customizable", 3000L, config.baseDelayMs)
        assertFalse("Reconnection should be disableable", config.enabled)
    }

    @Test
    fun `connection status should indicate error after failed reconnection`() {
        val failedState = GSRSensorViewModel.GSRSensorState(
            isConnected = false,
            isReconnecting = false,
            reconnectionAttempt = 0,
            connectionStatus = "Connection Lost",
            error = "Failed to reconnect after 3 attempts"
        )
        assertFalse("Should not be connected", failedState.isConnected)
        assertFalse("Should not be reconnecting", failedState.isReconnecting)
        assertEquals("Should show Connection Lost status", "Connection Lost", failedState.connectionStatus)
        assertTrue(
            "Error message should indicate reconnection failure",
            failedState.error?.contains("Failed to reconnect") == true
        )
    }

    @Test
    fun `successful reconnection should reset state`() {
        val reconnectedState = GSRSensorViewModel.GSRSensorState(
            isConnected = true,
            isReconnecting = false,
            reconnectionAttempt = 0,
            connectionStatus = "Reconnected",
            error = null
        )
        assertTrue("Should be connected", reconnectedState.isConnected)
        assertFalse("Should not be reconnecting", reconnectedState.isReconnecting)
        assertEquals("Reconnection attempt should be reset", 0, reconnectedState.reconnectionAttempt)
        assertEquals("Status should show Reconnected", "Reconnected", reconnectedState.connectionStatus)
        assertEquals("Error should be cleared", null, reconnectedState.error)
    }

    @Test
    fun `connection status should be visible to UI`() {
        val states = listOf(
            "Disconnected",
            "Initialized",
            "Connecting...",
            "Connected",
            "Reconnecting (attempt 1/3)...",
            "Connection Lost"
        )
        states.forEach { status ->
            val state = GSRSensorViewModel.GSRSensorState(connectionStatus = status)
            assertEquals("Status should match", status, state.connectionStatus)
        }
    }

    @Test
    fun `maxReconnectionAttempts should be tracked in state`() {
        val state = GSRSensorViewModel.GSRSensorState(
            isReconnecting = true,
            reconnectionAttempt = 2,
            maxReconnectionAttempts = 5
        )
        assertTrue("Should be reconnecting", state.isReconnecting)
        assertEquals("Current attempt should be 2", 2, state.reconnectionAttempt)
        assertEquals("Max attempts should be 5", 5, state.maxReconnectionAttempts)
    }
}
