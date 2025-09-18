package com.topdon.tc001.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.topdon.tc001.viewmodel.MainActivityViewModel
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MainActivityViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var mockObserver: Observer<Boolean>

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        viewModel = MainActivityViewModel()
        mockObserver = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `should initialize with default state`() {
        // Verify initial state
        assertNotNull("ViewModel should be initialized", viewModel)
        assertFalse("Recording should not be active initially", 
            viewModel.isRecordingActive.value ?: false)
    }

    @Test
    fun `should update recording state when startRecording is called`() = runTest {
        // Setup observer
        viewModel.isRecordingActive.observeForever(mockObserver)
        
        // Execute
        viewModel.startRecording()
        
        // Verify
        verify { mockObserver.onChanged(true) }
        assertTrue("Recording should be active", 
            viewModel.isRecordingActive.value ?: false)
        
        // Cleanup
        viewModel.isRecordingActive.removeObserver(mockObserver)
    }

    @Test
    fun `should update recording state when stopRecording is called`() = runTest {
        // Setup - start recording first
        viewModel.startRecording()
        viewModel.isRecordingActive.observeForever(mockObserver)
        
        // Execute
        viewModel.stopRecording()
        
        // Verify
        verify { mockObserver.onChanged(false) }
        assertFalse("Recording should not be active", 
            viewModel.isRecordingActive.value ?: true)
        
        // Cleanup
        viewModel.isRecordingActive.removeObserver(mockObserver)
    }

    @Test
    fun `should handle permission state changes`() = runTest {
        // Setup observer
        viewModel.allPermissionsGranted.observeForever(mockObserver)
        
        // Execute - update permission state
        viewModel.updatePermissionState(true)
        
        // Verify
        verify { mockObserver.onChanged(true) }
        assertTrue("All permissions should be granted", 
            viewModel.allPermissionsGranted.value ?: false)
        
        // Test false case
        viewModel.updatePermissionState(false)
        verify { mockObserver.onChanged(false) }
        
        // Cleanup
        viewModel.allPermissionsGranted.removeObserver(mockObserver)
    }

    @Test
    fun `should track sensor connection states`() = runTest {
        // Execute - update sensor states
        viewModel.updateSensorState("GSR", true)
        viewModel.updateSensorState("Thermal", false)
        viewModel.updateSensorState("RGB", true)
        
        // Verify
        assertTrue("GSR sensor should be connected", 
            viewModel.getSensorState("GSR"))
        assertFalse("Thermal sensor should not be connected", 
            viewModel.getSensorState("Thermal"))
        assertTrue("RGB sensor should be connected", 
            viewModel.getSensorState("RGB"))
    }

    @Test
    fun `should validate recording prerequisites`() = runTest {
        // Setup - no permissions, no sensors
        viewModel.updatePermissionState(false)
        
        // Execute
        val canRecord = viewModel.canStartRecording()
        
        // Verify
        assertFalse("Should not be able to record without permissions", canRecord)
        
        // Setup - with permissions but no sensors
        viewModel.updatePermissionState(true)
        val canRecordWithPermissions = viewModel.canStartRecording()
        
        // Should be able to record with permissions (sensors optional)
        assertTrue("Should be able to record with permissions", canRecordWithPermissions)
    }

    @Test
    fun `should handle error states appropriately`() = runTest {
        // Setup observer for error state
        val errorObserver = mockk<Observer<String?>>(relaxed = true)
        viewModel.errorMessage.observeForever(errorObserver)
        
        // Execute - simulate error
        val errorMessage = "Test error message"
        viewModel.reportError(errorMessage)
        
        // Verify
        verify { errorObserver.onChanged(errorMessage) }
        assertEquals("Error message should be set", errorMessage, 
            viewModel.errorMessage.value)
        
        // Clear error
        viewModel.clearError()
        verify { errorObserver.onChanged(null) }
        
        // Cleanup
        viewModel.errorMessage.removeObserver(errorObserver)
    }

    @Test
    fun `should manage session lifecycle correctly`() = runTest {
        // Verify initial state
        assertFalse("Session should not be active initially", viewModel.isSessionActive())
        
        // Start session
        val sessionId = "test_session_123"
        viewModel.startSession(sessionId)
        
        assertTrue("Session should be active", viewModel.isSessionActive())
        assertEquals("Session ID should match", sessionId, viewModel.getCurrentSessionId())
        
        // Stop session
        viewModel.stopSession()
        
        assertFalse("Session should not be active after stop", viewModel.isSessionActive())
        assertNull("Session ID should be null after stop", viewModel.getCurrentSessionId())
    }

    @Test
    fun `should track recording statistics`() = runTest {
        // Setup
        val initialDuration = viewModel.getRecordingDuration()
        
        // Execute - simulate recording progress
        viewModel.updateRecordingDuration(5000L) // 5 seconds
        
        // Verify
        assertEquals("Recording duration should be updated", 5000L, 
            viewModel.getRecordingDuration())
        assertTrue("Duration should have increased", 
            viewModel.getRecordingDuration() > initialDuration)
        
        // Test data size tracking
        viewModel.updateDataSize("GSR", 1024L)
        viewModel.updateDataSize("RGB", 2048L)
        
        assertEquals("GSR data size should be tracked", 1024L, 
            viewModel.getDataSize("GSR"))
        assertEquals("RGB data size should be tracked", 2048L, 
            viewModel.getDataSize("RGB"))
    }
}