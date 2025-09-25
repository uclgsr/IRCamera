package mpdc4gsr.sensors.camera

import mpdc4gsr.sensors.ErrorType
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CameraErrorMessageProvider
 * Tests error message generation and user guidance
 */
class CameraErrorMessageProviderTest {
    
    @Test
    fun `test permission denied error message`() {
        val message = CameraErrorMessageProvider.getUserFriendlyErrorMessage(
            ErrorType.PERMISSION_DENIED, 
            "Camera permission required"
        )
        
        assertTrue("Should mention camera permission", message.contains("Camera permission"))
        assertTrue("Should mention Settings", message.contains("Settings"))
        assertTrue("Should mention restart", message.contains("restart"))
    }
    
    @Test
    fun `test hardware unavailable error message`() {
        val message = CameraErrorMessageProvider.getUserFriendlyErrorMessage(
            ErrorType.HARDWARE_UNAVAILABLE,
            "Camera not available"
        )
        
        assertTrue("Should mention closing other apps", message.contains("close other camera apps"))
        assertTrue("Should mention restart device", message.contains("restart"))
    }
    
    @Test
    fun `test recording failed with storage error`() {
        val message = CameraErrorMessageProvider.getUserFriendlyErrorMessage(
            ErrorType.RECORDING_FAILED,
            "Not enough storage space"
        )
        
        assertTrue("Should mention storage", message.contains("storage"))
        assertTrue("Should mention GB", message.contains("GB"))
        assertTrue("Should mention free up space", message.contains("free up") || message.contains("Free up"))
    }
    
    @Test
    fun `test 4K not supported error`() {
        val message = CameraErrorMessageProvider.getUserFriendlyErrorMessage(
            ErrorType.FEATURE_NOT_SUPPORTED,
            "4K recording not supported"
        )
        
        assertTrue("Should mention 4K", message.contains("4K"))
        assertTrue("Should mention 1080p alternative", message.contains("1080p"))
        assertTrue("Should mention device specifications", message.contains("device specifications"))
    }
    
    @Test 
    fun `test RAW not supported error`() {
        val message = CameraErrorMessageProvider.getUserFriendlyErrorMessage(
            ErrorType.FEATURE_NOT_SUPPORTED,
            "RAW capture not supported"
        )
        
        assertTrue("Should mention RAW", message.contains("RAW"))
        assertTrue("Should mention JPEG alternative", message.contains("JPEG"))
        assertTrue("Should mention HDR", message.contains("HDR"))
    }
    
    @Test
    fun `test short error messages`() {
        val permissionMessage = CameraErrorMessageProvider.getShortErrorMessage(ErrorType.PERMISSION_DENIED)
        val hardwareMessage = CameraErrorMessageProvider.getShortErrorMessage(ErrorType.HARDWARE_UNAVAILABLE)
        val recordingMessage = CameraErrorMessageProvider.getShortErrorMessage(ErrorType.RECORDING_FAILED)
        
        assertTrue("Permission message should be concise", permissionMessage.length < 100)
        assertTrue("Hardware message should be concise", hardwareMessage.length < 100)
        assertTrue("Recording message should be concise", recordingMessage.length < 100)
        
        assertTrue("Should mention permission", permissionMessage.contains("permission"))
        assertTrue("Should mention camera", hardwareMessage.contains("camera"))
        assertTrue("Should mention storage", recordingMessage.contains("storage"))
    }
    
    @Test
    fun `test performance suggestions for different device capabilities`() {
        // Test for high-end device
        val highEndSuggestions = CameraErrorMessageProvider.getPerformanceSuggestions(
            deviceSupports4K = true,
            supportsRAW = true,
            supports60fps = true
        )
        
        assertTrue("Should have general suggestions", highEndSuggestions.isNotEmpty())
        assertTrue("Should mention battery", highEndSuggestions.any { it.contains("battery") })
        assertTrue("Should mention storage", highEndSuggestions.any { it.contains("storage") })
        
        // Test for basic device
        val basicDeviceSuggestions = CameraErrorMessageProvider.getPerformanceSuggestions(
            deviceSupports4K = false,
            supportsRAW = false,
            supports60fps = false
        )
        
        assertTrue("Should suggest 1080p", basicDeviceSuggestions.any { it.contains("1080p") })
        assertTrue("Should suggest 30fps", basicDeviceSuggestions.any { it.contains("30fps") })
        assertTrue("Should suggest JPEG", basicDeviceSuggestions.any { it.contains("JPEG") })
    }
    
    @Test
    fun `test error message specificity`() {
        // Test that different error types produce different messages
        val permissionError = CameraErrorMessageProvider.getUserFriendlyErrorMessage(
            ErrorType.PERMISSION_DENIED, "Permission denied"
        )
        val hardwareError = CameraErrorMessageProvider.getUserFriendlyErrorMessage(
            ErrorType.HARDWARE_UNAVAILABLE, "Hardware unavailable"  
        )
        
        assertNotEquals("Different error types should produce different messages", 
            permissionError, hardwareError)
    }
}