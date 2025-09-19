# Integration Guide: Samsung Stage3/Level3 RAW DNG Recording

This guide provides step-by-step instructions for integrating the Samsung Stage3/Level3 RAW DNG recording functionality into camera applications.

## Quick Start

### 1. Basic Integration

```kotlin
// Initialize Camera2 system
val camera2System = Camera2System(context, textureView)

// Enable Stage3/Level3 processing
camera2System.configureStage3Processing(true)

// Start recording with Stage3/Level3 DNG capture
camera2System.startRecording()
```

### 2. UI Integration

```kotlin
// Add Stage3/Level3 toggle to camera settings
cameraSettingsView.setStage3ProcessingVisible(true)

// Handle toggle changes
cameraSettingsView.onStage3ProcessingToggle = { enabled ->
    camera2System.configureStage3Processing(enabled)
    
    // Optional: Show feedback to user
    Toast.makeText(this, 
        if (enabled) "Stage3/Level3 DNG enabled" else "Standard RAW enabled",
        Toast.LENGTH_SHORT
    ).show()
}
```

## Advanced Configuration

### Device-Specific Optimization

```kotlin
// Check if device supports Stage3/Level3 processing
val deviceModel = android.os.Build.MODEL
val isStage3Compatible = deviceModel.contains("SM-S9", ignoreCase = true) || 
                         deviceModel.contains("SM-S22", ignoreCase = true)

if (isStage3Compatible) {
    cameraSettingsView.setStage3ProcessingVisible(true)
    cameraSettingsView.setStage3ProcessingEnabled(true)
} else {
    // Hide Stage3/Level3 option for unsupported devices
    cameraSettingsView.setStage3ProcessingVisible(false)
}
```

### Custom Error Handling

```kotlin
// Monitor Stage3/Level3 processing errors
camera2System.onError = { errorMessage ->
    if (errorMessage.contains("Stage3", ignoreCase = true)) {
        // Handle Stage3/Level3 specific errors
        Log.w(TAG, "Stage3/Level3 processing error: $errorMessage")
        
        // Optionally disable Stage3/Level3 and fall back to standard
        camera2System.configureStage3Processing(false)
    }
}
```

## File Management

### Stage3/Level3 File Detection

```kotlin
fun isStage3DngFile(file: File): Boolean {
    return file.name.contains("stage3", ignoreCase = true) && 
           file.extension.equals("dng", ignoreCase = true)
}

fun processRawFiles(directory: File) {
    directory.listFiles { file ->
        file.extension.equals("dng", ignoreCase = true)
    }?.forEach { dngFile ->
        if (isStage3DngFile(dngFile)) {
            // Process Stage3/Level3 DNG file
            processStage3Dng(dngFile)
        } else {
            // Process standard DNG file
            processStandardDng(dngFile)
        }
    }
}
```

### Storage Management

```kotlin
// Calculate storage requirements
fun calculateStage3StorageRequirements(captureDuration: Long, fps: Int): Long {
    // Stage3/Level3 DNG files are approximately 25-30MB each for 12MP
    val avgDngSizeMB = 28
    val totalFrames = (captureDuration / 1000) * fps
    return totalFrames * avgDngSizeMB * 1024 * 1024 // Return bytes
}

// Check available storage before starting
val requiredStorage = calculateStage3StorageRequirements(recordingDuration, 15)
val availableStorage = Environment.getExternalStorageDirectory().freeSpace

if (availableStorage < requiredStorage * 1.2) { // 20% buffer
    // Warn user about storage
    AlertDialog.Builder(this)
        .setTitle("Storage Warning")
        .setMessage("Stage3/Level3 DNG recording requires ${requiredStorage/1024/1024}MB. Available: ${availableStorage/1024/1024}MB")
        .show()
}
```

## Testing Integration

### Unit Testing

```kotlin
@Test
fun testStage3Integration() {
    // Mock camera system
    val mockCamera2System = mockk<Camera2System>()
    
    // Test Stage3/Level3 configuration
    every { mockCamera2System.configureStage3Processing(true) } returns Unit
    every { mockCamera2System.isStage3ProcessingEnabled() } returns true
    
    // Verify integration
    mockCamera2System.configureStage3Processing(true)
    assertTrue(mockCamera2System.isStage3ProcessingEnabled())
}
```

### Integration Testing

```kotlin
class Stage3IntegrationTest {
    
    @Test
    fun testEndToEndStage3Recording() {
        // Setup camera system
        val camera2System = Camera2System(context, textureView)
        
        // Configure for Stage3/Level3
        camera2System.configureStage3Processing(true)
        
        // Start recording and wait
        camera2System.startRecording()
        Thread.sleep(5000) // Record for 5 seconds
        camera2System.stopRecording()
        
        // Verify Stage3/Level3 files were created
        val outputFiles = outputDirectory.listFiles()
        assertTrue("Should have Stage3 DNG files", 
                   outputFiles.any { it.name.contains("stage3") })
    }
}
```

## Performance Optimization

### Memory Management

```kotlin
// Monitor memory usage during Stage3/Level3 recording
fun monitorStage3Memory() {
    val runtime = Runtime.getRuntime()
    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
    val maxMemory = runtime.maxMemory()
    
    if (usedMemory > maxMemory * 0.8) {
        Log.w(TAG, "High memory usage during Stage3/Level3 processing")
        // Consider reducing frame rate or switching to standard mode
    }
}
```

### Battery Optimization

```kotlin
// Stage3/Level3 processing is more CPU intensive
fun optimizeForStage3Battery() {
    // Reduce frame rate for longer recording sessions
    val isLongSession = expectedRecordingTime > 300_000 // 5 minutes
    val frameRate = if (isLongSession && camera2System.isStage3ProcessingEnabled()) {
        10 // Reduce from 15fps to 10fps for battery
    } else {
        15 // Standard frame rate
    }
    
    // Apply frame rate optimization
    updateRawCaptureFrameRate(frameRate)
}
```

## Troubleshooting Common Issues

### Issue: DNG Files Not Created
```kotlin
// Check if camera characteristics are available
val characteristics = cameraController.getCameraCharacteristics()
if (characteristics == null) {
    Log.e(TAG, "Camera characteristics not available - DNG creation will fail")
    // Fall back to raw binary
}
```

### Issue: Stage3/Level3 Toggle Not Visible
```kotlin
// Ensure the toggle is made visible for compatible devices
val isCompatible = checkStage3Compatibility()
cameraSettingsView.setStage3ProcessingVisible(isCompatible)

if (!isCompatible) {
    Log.i(TAG, "Device does not support Stage3/Level3 processing")
}
```

### Issue: High Storage Usage
```kotlin
// Monitor storage during recording
fun monitorStage3Storage() {
    if (camera2System.isStage3ProcessingEnabled()) {
        val freeSpace = Environment.getExternalStorageDirectory().freeSpace
        val minRequiredSpace = 1024L * 1024 * 100 // 100MB minimum
        
        if (freeSpace < minRequiredSpace) {
            // Stop recording or switch to JPEG mode
            camera2System.configureStage3Processing(false)
            notifyUser("Switched to standard mode due to low storage")
        }
    }
}
```

## Best Practices

1. **Always check device compatibility** before enabling Stage3/Level3
2. **Provide user feedback** when switching processing modes
3. **Monitor system resources** during recording
4. **Implement graceful fallbacks** for error conditions
5. **Test on actual Samsung devices** for optimal results
6. **Document Stage3/Level3 specific features** for users
7. **Consider storage requirements** in UI design

## Example Complete Integration

```kotlin
class CameraActivity : AppCompatActivity() {
    
    private lateinit var camera2System: Camera2System
    private lateinit var cameraSettingsView: CameraSettingsView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize camera system
        camera2System = Camera2System(this, textureView)
        
        // Setup Stage3/Level3 integration
        setupStage3Integration()
        
        // Configure UI
        setupUI()
    }
    
    private fun setupStage3Integration() {
        // Check device compatibility
        val isStage3Compatible = checkStage3Compatibility()
        
        if (isStage3Compatible) {
            // Enable Stage3/Level3 by default for compatible devices
            camera2System.configureStage3Processing(true)
            
            // Show UI controls
            cameraSettingsView.setStage3ProcessingVisible(true)
            cameraSettingsView.setStage3ProcessingEnabled(true)
            
            // Handle toggle
            cameraSettingsView.onStage3ProcessingToggle = { enabled ->
                camera2System.configureStage3Processing(enabled)
                val mode = if (enabled) "Stage3/Level3" else "Standard"
                showToast("RAW mode: $mode")
            }
        }
    }
    
    private fun checkStage3Compatibility(): Boolean {
        val model = android.os.Build.MODEL
        return model.contains("SM-S9") || model.contains("SM-S22")
    }
}
```

This integration guide provides the essential patterns for implementing Stage3/Level3 RAW DNG recording in camera applications.