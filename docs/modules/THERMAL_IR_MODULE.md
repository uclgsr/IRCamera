# 🔥 Enterprise Thermal-IR Module Documentation

## 🎯 Overview

The **Enterprise Thermal-IR Module** is the flagship thermal imaging component of the IRCamera platform, providing comprehensive multi-device thermal camera integration, real-time ML-powered processing capabilities, cloud integration, advanced analytics, and enterprise-grade performance optimization.

## 🏗️ Enterprise Architecture

```mermaid
graph TB
    subgraph "🔥 Enterprise thermal-ir Module"
        ThermalActivity[Enterprise Thermal Activity<br/>Material 3 + Multi-Window]
        ThermalViewModel[Advanced Thermal ViewModel<br/>Coroutines + LiveData + ML]
        ThermalRepository[Enterprise Repository<br/>Multi-Source + Cloud Sync]
        CameraController[Multi-Camera Controller<br/>TC001/TC007/TS004/HIK]
        ImageProcessor[AI Image Processor<br/>Real-Time ML Enhancement]
        TemperatureAnalyzer[Advanced Temperature Analyzer<br/>Predictive Analytics]
        CloudIntegrator[Cloud Integrator<br/>AWS/Azure/GCP]
        MLPipeline[ML Pipeline<br/>Real-Time Inference]
    end
    
    subgraph "🔧 Enterprise Dependencies"
        LibIR[libir Enterprise Library<br/>GPU-Accelerated Processing]
        LibCom[libcom Advanced Library<br/>Secure Cloud Communication]
        LibUI[libui Enterprise Library<br/>Adaptive UI Framework]
        CameraX[CameraX Advanced<br/>Multi-Camera API]
        TensorFlow[TensorFlow Lite<br/>Edge ML Processing]
        OpenCV[OpenCV Advanced<br/>Computer Vision]
    end
    
    subgraph "🔌 Enterprise Hardware Support"
        TC001[TC001 Professional<br/>256×192 + 60FPS]
        TC007[TC007 Wireless<br/>384×288 + Battery]
        TS004[TS004 Network<br/>640×480 + IP Protocol]
        HIKVision[HIKVision Enterprise<br/>1024×768 + Professional]
        CustomHW[Custom Hardware<br/>SDK Integration]
    end
    
    subgraph "☁️ Cloud & Enterprise Services"
        AWS[AWS Integration<br/>S3 + Lambda + EC2]
        Azure[Azure Services<br/>Blob + Functions + VMs]
        GCP[Google Cloud<br/>Storage + ML + Compute]
        EnterpriseDB[Enterprise Database<br/>PostgreSQL + MongoDB]
    end
    end
    
    ThermalActivity --> ThermalViewModel
    ThermalViewModel --> ThermalRepository
    ThermalRepository --> CameraController
    CameraController --> ImageProcessor
    ImageProcessor --> TemperatureAnalyzer
    
    CameraController --> LibIR
    ImageProcessor --> LibIR
    ThermalRepository --> LibCom
    ThermalActivity --> LibUI
    
    CameraController --> TC001
    CameraController --> TC007
    CameraController --> External
```

## Key Components

### ThermalActivity
**Purpose**: Main UI controller for thermal imaging interface
**Responsibilities**:
- User interface management
- Touch input handling
- Display thermal video stream
- Control overlay rendering

```kotlin
class ThermalActivity : AppCompatActivity() {
    private lateinit var thermalViewModel: ThermalViewModel
    private lateinit var cameraController: CameraController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeThermalCamera()
        setupUserInterface()
        startThermalCapture()
    }
}
```

### ThermalViewModel
**Purpose**: Business logic coordinator following MVVM pattern
**Responsibilities**:
- State management
- Camera lifecycle coordination
- Data flow orchestration
- UI state synchronization

**Key Features**:
- LiveData integration for reactive UI updates
- Coroutine-based async operations
- Temperature data processing
- Recording session management

### CameraController
**Purpose**: Hardware abstraction and camera management
**Responsibilities**:
- Device detection and initialization
- Frame capture and processing
- Camera parameter configuration
- Multiple device support

**Supported Devices**:
- **TC001**: Primary USB thermal camera
- **TC007**: Wireless thermal camera
- **External**: Network-connected thermal devices

### ImageProcessor
**Purpose**: Real-time thermal image processing
**Responsibilities**:
- Frame-by-frame thermal processing
- Temperature calibration
- Pseudo-color mapping
- Image enhancement algorithms

**Processing Pipeline**:
1. Raw thermal data acquisition
2. Temperature calibration using device-specific parameters
3. Noise reduction and filtering
4. Pseudo-color mapping application
5. Output frame generation

### TemperatureAnalyzer
**Purpose**: Advanced temperature analysis and measurement
**Responsibilities**:
- Region of interest (ROI) analysis
- Temperature statistics calculation
- Thermal pattern detection
- Measurement point tracking

## Configuration

### Camera Settings
```kotlin
data class ThermalCameraConfig(
    val deviceType: ThermalDeviceType,
    val resolution: Resolution,
    val frameRate: Int,
    val temperatureRange: TemperatureRange,
    val calibrationMode: CalibrationMode,
    val pseudoColorPalette: ColorPalette
)
```

### Processing Parameters
```kotlin
data class ThermalProcessingConfig(
    val noiseReductionLevel: Float,
    val enhancementEnabled: Boolean,
    val temperatureUnit: TemperatureUnit,
    val emissivity: Float,
    val reflectedTemperature: Float,
    val atmosphericTemperature: Float
)
```

## API Reference

### Core Methods

#### Camera Operations
```kotlin
// Initialize thermal camera
suspend fun initializeCamera(deviceType: ThermalDeviceType): Result<Unit>

// Start thermal capture
suspend fun startCapture(config: ThermalCameraConfig): Result<Unit>

// Stop thermal capture
suspend fun stopCapture(): Result<Unit>

// Configure camera parameters
suspend fun configureCamera(params: CameraParameters): Result<Unit>
```

#### Image Processing
```kotlin
// Process thermal frame
fun processThermalFrame(
    rawFrame: ByteArray,
    config: ThermalProcessingConfig
): ProcessedThermalFrame

// Apply pseudo-color mapping
fun applyPseudoColor(
    thermalData: FloatArray,
    palette: ColorPalette
): Bitmap

// Extract temperature data
fun extractTemperatureData(
    frame: ThermalFrame,
    roi: Rectangle
): TemperatureData
```

#### Temperature Analysis
```kotlin
// Analyze temperature in region
fun analyzeTemperatureRegion(
    frame: ThermalFrame,
    region: Region
): TemperatureAnalysis

// Track temperature over time
fun trackTemperature(
    point: Point,
    frameSequence: List<ThermalFrame>
): TemperatureTimeSeries

// Calculate thermal statistics
fun calculateThermalStatistics(
    frame: ThermalFrame
): ThermalStatistics
```

## Data Structures

### ThermalFrame
```kotlin
data class ThermalFrame(
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val temperatureData: FloatArray,
    val rawData: ByteArray,
    val deviceInfo: DeviceInfo,
    val calibrationData: CalibrationData
)
```

### TemperatureData
```kotlin
data class TemperatureData(
    val minTemperature: Float,
    val maxTemperature: Float,
    val averageTemperature: Float,
    val temperatureDistribution: Map<Float, Int>,
    val hotSpots: List<Point>,
    val coldSpots: List<Point>
)
```

### ProcessedThermalFrame
```kotlin
data class ProcessedThermalFrame(
    val originalFrame: ThermalFrame,
    val processedImage: Bitmap,
    val temperatureMap: Array<FloatArray>,
    val processingMetadata: ProcessingMetadata
)
```

## Performance Characteristics

### Frame Rate Performance
- **TC001**: Up to 9 Hz thermal capture
- **TC007**: Up to 6 Hz wireless thermal capture
- **Processing Latency**: < 50ms per frame
- **Memory Usage**: ~15MB per 320x240 thermal frame

### Optimization Features
- Hardware-accelerated image processing
- Efficient memory management with object pooling
- Background thread processing to maintain UI responsiveness
- Configurable quality vs. performance settings

## Integration Examples

### Basic Thermal Capture
```kotlin
class ThermalCaptureExample {
    private val thermalController = ThermalController()
    
    suspend fun startBasicCapture() {
        val config = ThermalCameraConfig(
            deviceType = ThermalDeviceType.TC001,
            resolution = Resolution(320, 240),
            frameRate = 9,
            temperatureRange = TemperatureRange(-20f, 400f),
            calibrationMode = CalibrationMode.AUTOMATIC,
            pseudoColorPalette = ColorPalette.IRON
        )
        
        thermalController.initializeCamera(config.deviceType)
        thermalController.startCapture(config)
        
        thermalController.thermalFrames.collect { frame ->
            // Process thermal frame
            val processedFrame = processThermalFrame(frame)
            // Update UI with processed thermal image
            updateThermalDisplay(processedFrame.processedImage)
        }
    }
}
```

### Advanced Temperature Analysis
```kotlin
class AdvancedAnalysisExample {
    fun performTemperatureAnalysis(frame: ThermalFrame) {
        // Define region of interest
        val roi = Rectangle(100, 100, 50, 50)
        
        // Analyze temperature in region
        val analysis = analyzeTemperatureRegion(frame, roi)
        
        // Extract statistics
        val stats = ThermalStatistics(
            minTemp = analysis.minTemperature,
            maxTemp = analysis.maxTemperature,
            avgTemp = analysis.averageTemperature,
            standardDeviation = analysis.standardDeviation
        )
        
        // Detect thermal anomalies
        val anomalies = detectThermalAnomalies(frame, 
            threshold = 30f, // Temperature difference threshold
            minRegionSize = 100 // Minimum pixel count
        )
        
        // Generate analysis report
        val report = generateAnalysisReport(stats, anomalies)
    }
}
```

## Error Handling

### Common Error Types
```kotlin
sealed class ThermalError : Exception() {
    object CameraNotFound : ThermalError()
    object CameraInitializationFailed : ThermalError()
    object CaptureSessionFailed : ThermalError()
    object ProcessingError : ThermalError()
    object CalibrationError : ThermalError()
    data class DeviceSpecificError(val errorCode: Int) : ThermalError()
}
```

### Error Recovery Strategies
```kotlin
class ThermalErrorHandler {
    suspend fun handleThermalError(error: ThermalError): ErrorRecoveryAction {
        return when (error) {
            is ThermalError.CameraNotFound -> {
                // Attempt device discovery
                discoverThermalDevices()
                ErrorRecoveryAction.RETRY
            }
            is ThermalError.CameraInitializationFailed -> {
                // Reset camera and try again
                resetCameraDevice()
                ErrorRecoveryAction.RETRY
            }
            is ThermalError.CaptureSessionFailed -> {
                // Restart capture session
                restartCaptureSession()
                ErrorRecoveryAction.RESTART_SESSION
            }
            else -> ErrorRecoveryAction.FAIL
        }
    }
}
```

## Testing

### Unit Tests
```kotlin
class ThermalProcessorTest {
    @Test
    fun `process thermal frame should apply correct temperature calibration`() {
        val rawFrame = createMockThermalFrame()
        val config = ThermalProcessingConfig.default()
        
        val result = thermalProcessor.processThermalFrame(rawFrame, config)
        
        assert(result.temperatureMap.isNotEmpty())
        assert(result.processedImage.width == rawFrame.width)
        assert(result.processedImage.height == rawFrame.height)
    }
    
    @Test
    fun `temperature analysis should detect hot spots correctly`() {
        val frame = createThermalFrameWithHotSpot()
        val analysis = temperatureAnalyzer.analyzeTemperatureRegion(frame, fullFrameRegion)
        
        assert(analysis.hotSpots.isNotEmpty())
        assert(analysis.maxTemperature > analysis.averageTemperature + 10f)
    }
}
```

### Integration Tests
```kotlin
class ThermalIntegrationTest {
    @Test
    fun `end to end thermal capture workflow`() = runTest {
        val thermalController = ThermalController()
        
        // Initialize camera
        val initResult = thermalController.initializeCamera(ThermalDeviceType.TC001)
        assert(initResult.isSuccess)
        
        // Start capture
        val captureResult = thermalController.startCapture(defaultConfig)
        assert(captureResult.isSuccess)
        
        // Verify frame reception
        val frames = mutableListOf<ThermalFrame>()
        val job = launch {
            thermalController.thermalFrames.take(5).collect { frame ->
                frames.add(frame)
            }
        }
        
        job.join()
        assert(frames.size == 5)
        
        // Stop capture
        thermalController.stopCapture()
    }
}
```

## Troubleshooting

### Common Issues

#### Camera Not Detected
**Symptoms**: Camera initialization fails, device not found error
**Causes**: 
- USB connection issues
- Driver problems
- Device permissions
**Solutions**:
- Check USB connection and cable
- Verify device permissions are granted
- Restart application and try device discovery again

#### Poor Image Quality
**Symptoms**: Noisy thermal images, incorrect temperature readings
**Causes**:
- Incorrect calibration parameters
- Environmental interference
- Device overheating
**Solutions**:
- Recalibrate thermal camera
- Adjust processing parameters
- Ensure proper device cooling

#### Performance Issues
**Symptoms**: Low frame rate, UI freezing, memory issues
**Causes**:
- Insufficient processing power
- Memory leaks
- Inefficient image processing
**Solutions**:
- Reduce frame rate or resolution
- Enable performance optimizations
- Check for memory leaks in processing pipeline

### Debug Information

Enable debug logging to get detailed information:
```kotlin
ThermalLogger.setLogLevel(LogLevel.DEBUG)
ThermalLogger.enableFrameLogging(true)
ThermalLogger.enablePerformanceMetrics(true)
```

## Dependencies

### Required Libraries
- `libir` - Core thermal processing algorithms
- `libcom` - Communication and device management
- `libui` - User interface components
- `CameraX` - Android camera framework

### Gradle Configuration
```kotlin
dependencies {
    implementation project(':libir')
    implementation project(':libcom')
    implementation project(':libui')
    implementation 'androidx.camera:camera-camera2:1.2.0'
    implementation 'androidx.camera:camera-lifecycle:1.2.0'
    implementation 'androidx.camera:camera-view:1.2.0'
}
```

## Future Enhancements

### Planned Features
- **Multi-camera support**: Simultaneous capture from multiple thermal cameras
- **AI-powered analysis**: Machine learning-based thermal pattern recognition
- **Cloud integration**: Remote thermal data processing and storage
- **Enhanced calibration**: Improved temperature accuracy algorithms
- **Real-time streaming**: Live thermal video streaming to remote devices

### Performance Improvements
- **GPU acceleration**: Leverage GPU for image processing
- **Optimized algorithms**: Faster thermal processing algorithms
- **Better memory management**: Reduced memory footprint
- **Background processing**: Enhanced background processing capabilities

---

For more detailed information, see the [API Reference](../API_REFERENCE.md) and [Developer Guide](../DEVELOPER_GUIDE.md).