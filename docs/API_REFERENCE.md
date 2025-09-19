# IRCamera API Reference

## 📋 Overview

This document provides comprehensive API reference documentation for all major components and modules of the IRCamera Multi-Modal Thermal Sensing Platform. The API is organized by component type and provides detailed information about classes, methods, and interfaces.

## 🏗️ Architecture Components

### Core Components

| Component | Purpose | Technology | Status |
|-----------|---------|------------|--------|
| **PC Controller Hub** | Central coordination and device management | Python + PyQt6 | ✅ MVP Complete |
| **Android Sensor Node** | Mobile sensor node with multi-modal capabilities | Kotlin + Android | ⚠️ Build Issues |
| **Communication Layer** | JSON-based TCP protocol with mDNS discovery | Cross-platform | ✅ Working |
| **Sensor Integration** | Hardware abstraction for multiple sensor types | Multi-platform | ✅ Working |

### Library Components

| Library | Purpose | Dependencies | Status |
|---------|---------|--------------|--------|
| **libir** | Core infrared camera processing | OpenCV, native code | ✅ Working |
| **libcom** | Communication and networking | TCP, JSON, mDNS | ✅ Working |
| **libapp** | Application framework | Android SDK | ✅ Working |
| **libui** | User interface components | Android UI, Material Design | ✅ Working |
| **libmatrix** | Matrix operations for image processing | Native math libraries | ✅ Working |

## 🖥️ PC Controller Hub API

### Core Classes

#### DeviceManager
**Location**: `pc-controller/src/ircamera_pc/core/device_manager.py`

```python
class DeviceManager:
    """Central device registry and management system."""
    
    def __init__(self, discovery_service: DeviceDiscoveryService):
        """Initialize device manager with discovery service."""
    
    def register_device(self, device_info: Dict) -> str:
        """Register a new device in the system."""
    
    def get_device_status(self, device_id: str) -> DeviceStatus:
        """Get current status of a registered device."""
    
    def get_online_devices(self) -> List[str]:
        """Get list of currently online device IDs."""
    
    def remove_device(self, device_id: str) -> bool:
        """Remove device from registry."""
```

#### SessionManager
**Location**: `pc-controller/src/ircamera_pc/core/session_manager.py`

```python
class SessionManager:
    """Advanced session lifecycle management."""
    
    def create_session(self, session_name: str, config: SessionConfiguration) -> str:
        """Create new recording session."""
    
    def start_recording(self, session_id: str, devices: List[str]) -> bool:
        """Start synchronized recording across devices."""
    
    def stop_recording(self, session_id: str) -> bool:
        """Stop recording for all devices in session."""
    
    def finalize_session(self, session_id: str) -> SessionSummary:
        """Complete session and generate metadata."""
    
    def get_session_status(self, session_id: str) -> SessionStatus:
        """Get current session status and progress."""
```

### Network Communication

#### DeviceDiscoveryService
**Location**: `pc-controller/src/ircamera_pc/network/discovery.py`

```python
class DeviceDiscoveryService:
    """mDNS-based device discovery service."""
    
    def start_discovery(self) -> None:
        """Start scanning for Android sensor nodes."""
    
    def stop_discovery(self) -> None:
        """Stop device discovery scanning."""
    
    def get_discovered_devices(self) -> List[DeviceInfo]:
        """Get list of discovered devices."""
    
    def add_manual_device(self, ip_address: str, port: int) -> bool:
        """Manually add device if mDNS fails."""
```

#### TCPServer
**Location**: `pc-controller/src/ircamera_pc/network/server.py`

```python
class TCPServer:
    """JSON-based TCP communication server."""
    
    def start_server(self, port: int = 8080) -> None:
        """Start TCP server for device communication."""
    
    def send_command(self, device_id: str, command: Command) -> bool:
        """Send command to specific device."""
    
    def broadcast_command(self, command: Command) -> Dict[str, bool]:
        """Broadcast command to all connected devices."""
    
    def get_connected_devices(self) -> List[str]:
        """Get list of currently connected device IDs."""
```

### Data Structures

#### SessionConfiguration
```python
@dataclass
class SessionConfiguration:
    """Session configuration parameters."""
    session_name: str
    duration_seconds: Optional[int]
    sampling_rate: int
    enable_thermal: bool = True
    enable_gsr: bool = True
    enable_rgb: bool = True
    thermal_range: Tuple[float, float] = (-20.0, 80.0)
    gsr_range: Tuple[float, float] = (0.0, 100.0)
    sync_flash_enabled: bool = True
```

#### DeviceInfo
```python
@dataclass
class DeviceInfo:
    """Device information and capabilities."""
    device_id: str
    device_type: DeviceType
    ip_address: str
    port: int
    capabilities: List[str]
    max_resolution: str
    sampling_rates: List[int]
    last_seen: datetime
    status: DeviceStatus
```

## 📱 Android Sensor Node API

### Core Activities

#### MainActivity
**Location**: `app/src/main/java/mpdc4gsr/MainActivity.kt`

```kotlin
class MainActivity : AppCompatActivity() {
    /**
     * Main application entry point with sensor coordination.
     */
    
    fun initializeSensors(): Boolean
    /**
     * Initialize all available sensor modules.
     * @return true if at least one sensor initialized successfully
     */
    
    fun startRecording(sessionConfig: SessionConfiguration): Boolean
    /**
     * Start synchronized recording across all sensors.
     * @param sessionConfig Session configuration from PC Hub
     * @return true if recording started successfully
     */
    
    fun stopRecording(): Boolean
    /**
     * Stop recording and finalize session data.
     * @return true if stopped successfully
     */
}
```

### Sensor Modules

#### Thermal Camera Integration
**Location**: `component/thermal-ir/`

```kotlin
class ThermalCameraController {
    /**
     * Topdon TC001 thermal camera integration.
     */
    
    fun initialize(): Boolean
    /**
     * Initialize thermal camera hardware.
     * @return true if initialization successful
     */
    
    fun startCapture(settings: ThermalSettings): Boolean
    /**
     * Start thermal imaging capture.
     * @param settings Thermal capture configuration
     * @return true if capture started
     */
    
    fun captureFrame(): ThermalFrame?
    /**
     * Capture single thermal frame.
     * @return ThermalFrame data or null if failed
     */
    
    fun stopCapture(): Boolean
    /**
     * Stop thermal capture and release resources.
     * @return true if stopped successfully
     */
}
```

#### GSR Sensor Integration  
**Location**: `component/gsr-recording/`

```kotlin
class GSRRecordingController {
    /**
     * Shimmer3 GSR+ sensor integration via BLE.
     */
    
    fun discoverDevices(): List<ShimmerDevice>
    /**
     * Discover available Shimmer3 GSR devices.
     * @return List of discovered devices
     */
    
    fun connectDevice(device: ShimmerDevice): Boolean
    /**
     * Connect to specific GSR device.
     * @param device Shimmer device to connect
     * @return true if connection successful
     */
    
    fun startStreaming(): Boolean
    /**
     * Start GSR data streaming.
     * @return true if streaming started
     */
    
    fun getLatestGSRData(): GSRDataPoint?
    /**
     * Get most recent GSR measurement.
     * @return Latest GSR data point
     */
}
```

#### RGB Camera Integration
**Location**: Uses CameraX API integration

```kotlin
class RGBCameraController {
    /**
     * CameraX-based RGB video recording.
     */
    
    fun initializeCamera(): Boolean
    /**
     * Initialize CameraX with optimal settings.
     * @return true if camera ready
     */
    
    fun startVideoRecording(outputFile: File): Boolean
    /**
     * Start high-resolution video recording.
     * @param outputFile Output file for video
     * @return true if recording started
     */
    
    fun captureFrame(outputFile: File): Boolean
    /**
     * Capture individual frame for analysis.
     * @param outputFile Output file for frame
     * @return true if frame captured
     */
}
```

### Data Models

#### ThermalFrame
```kotlin
data class ThermalFrame(
    val timestamp: Long,
    val temperatureMatrix: Array<FloatArray>,
    val width: Int,
    val height: Int,
    val metadata: ThermalMetadata
)
```

#### GSRDataPoint
```kotlin
data class GSRDataPoint(
    val timestamp: Long,
    val conductance: Double,
    val resistance: Double,
    val ppgValue: Double?,
    val quality: DataQuality
)
```

## 🔧 Core Libraries API

### libir - Infrared Processing Library
**Location**: `libir/`

#### ThermalProcessor
```kotlin
class ThermalProcessor {
    /**
     * Core thermal image processing capabilities.
     */
    
    external fun processFrame(
        data: ByteArray,
        width: Int,
        height: Int
    ): ThermalFrame
    /**
     * Process raw thermal camera data.
     * @param data Raw thermal sensor data
     * @param width Frame width
     * @param height Frame height
     * @return Processed thermal frame
     */
    
    external fun applyColorMap(
        frame: ThermalFrame,
        colorMap: ColorMap
    ): Bitmap
    /**
     * Apply color mapping to thermal data.
     * @param frame Thermal frame data
     * @param colorMap Color mapping scheme
     * @return Rendered thermal image
     */
}
```

### libcom - Communication Library
**Location**: `libcom/`

#### NetworkManager  
```kotlin
class NetworkManager {
    /**
     * Cross-platform networking capabilities.
     */
    
    fun advertiseService(
        serviceType: String,
        port: Int,
        properties: Map<String, String>
    ): Boolean
    /**
     * Advertise mDNS service for PC discovery.
     * @param serviceType mDNS service type
     * @param port TCP port for communication
     * @param properties Device capability properties
     * @return true if service advertised successfully
     */
    
    fun sendData(
        targetAddress: String,
        port: Int,
        data: ByteArray
    ): Boolean
    /**
     * Send data to specific network endpoint.
     * @param targetAddress Target IP address
     * @param port Target port
     * @param data Data to send
     * @return true if data sent successfully
     */
}
```

### libui - UI Components Library
**Location**: `libui/`

#### StatusIndicator
```kotlin
class StatusIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    /**
     * Custom UI component for device status display.
     */
    
    fun setStatus(status: DeviceStatus)
    /**
     * Update status indicator appearance.
     * @param status New device status
     */
    
    fun setLabel(label: String)
    /**
     * Set status indicator label text.
     * @param label Label text
     */
}
```

## 🌐 Communication Protocol API

### Message Format Specification

#### Command Messages (Hub → Spoke)
```json
{
  "message_id": "uuid-string",
  "timestamp": "ISO-8601-datetime",
  "sender_id": "pc_hub",
  "message_type": "command",
  "payload": {
    "action": "start_recording|stop_recording|sync_flash|get_status",
    "session_id": "session-identifier",
    "configuration": {
      "sampling_rate": 128,
      "duration_seconds": 300,
      "sensors": ["thermal", "gsr", "rgb"]
    }
  }
}
```

#### Response Messages (Spoke → Hub)
```json
{
  "message_id": "uuid-string",
  "timestamp": "ISO-8601-datetime",
  "sender_id": "android-device-id",
  "message_type": "ack|error|status|data",
  "payload": {
    "status": "success|error|recording|idle",
    "original_message_id": "command-message-id",
    "device_ready": true,
    "error_message": "Optional error description"
  }
}
```

### Protocol Methods

#### MessageHandler Interface
```python
class MessageHandler:
    """Base interface for protocol message handling."""
    
    def handle_message(self, message: Dict) -> Optional[Dict]:
        """Process incoming message and generate response."""
    
    def send_command(self, command: Command) -> bool:
        """Send command message to target."""
    
    def send_response(self, response: Response) -> bool:
        """Send response message to sender."""
```

## 🔍 Error Handling API

### Exception Classes

#### IRCameraException
```python
class IRCameraException(Exception):
    """Base exception for IRCamera platform errors."""
    
    def __init__(self, message: str, error_code: str = None):
        self.message = message
        self.error_code = error_code
        super().__init__(self.message)
```

#### DeviceConnectionException
```python
class DeviceConnectionException(IRCameraException):
    """Exception raised for device connection issues."""
    
    def __init__(self, device_id: str, reason: str):
        self.device_id = device_id
        super().__init__(f"Device {device_id} connection failed: {reason}")
```

#### SessionManagementException
```python
class SessionManagementException(IRCameraException):
    """Exception raised for session management issues."""
    
    def __init__(self, session_id: str, operation: str, reason: str):
        self.session_id = session_id
        self.operation = operation
        super().__init__(f"Session {session_id} {operation} failed: {reason}")
```

## 📊 Data Processing API

### Data Pipeline Components

#### DataProcessor Interface
```python
class DataProcessor:
    """Base interface for sensor data processing."""
    
    def process(self, raw_data: bytes) -> ProcessedData:
        """Process raw sensor data."""
    
    def validate(self, data: ProcessedData) -> bool:
        """Validate processed data quality."""
    
    def export(self, data: ProcessedData, format: str) -> bytes:
        """Export data in specified format."""
```

#### SynchronizationEngine
```python
class SynchronizationEngine:
    """Multi-modal data synchronization."""
    
    def align_timestamps(self, datasets: List[DataSet]) -> List[DataSet]:
        """Align timestamps across multiple data sources."""
    
    def interpolate_missing(self, dataset: DataSet) -> DataSet:
        """Interpolate missing data points."""
    
    def validate_sync_quality(self, datasets: List[DataSet]) -> SyncQuality:
        """Validate synchronization quality metrics."""
```

## 🧪 Testing API

### Test Utilities

#### MockDevice
```python
class MockDevice:
    """Mock device for testing device communication."""
    
    def __init__(self, device_id: str, capabilities: List[str]):
        self.device_id = device_id
        self.capabilities = capabilities
    
    def simulate_response(self, command: Command) -> Response:
        """Simulate device response to command."""
    
    def generate_sample_data(self, sensor_type: str) -> bytes:
        """Generate sample sensor data for testing."""
```

#### TestSession
```python
class TestSession:
    """Test session management utilities."""
    
    def create_test_session(self) -> str:
        """Create session for testing purposes."""
    
    def validate_session_data(self, session_id: str) -> bool:
        """Validate session data integrity."""
    
    def cleanup_test_data(self) -> None:
        """Clean up test session data."""
```

## 📋 Configuration API

### Configuration Management

#### ConfigurationManager
```python
class ConfigurationManager:
    """System configuration management."""
    
    def load_config(self, config_file: str) -> Configuration:
        """Load configuration from file."""
    
    def save_config(self, config: Configuration, config_file: str) -> bool:
        """Save configuration to file."""
    
    def get_default_config(self) -> Configuration:
        """Get default system configuration."""
    
    def validate_config(self, config: Configuration) -> List[str]:
        """Validate configuration and return errors."""
```

## 🔧 Build and Deployment API

### Build Tools

#### BuildHelper (dev.sh interface)
```bash
# Available build and validation commands
./dev.sh help              # Show all available commands
./dev.sh lint              # Run code linting and style checks
./dev.sh static            # Run static analysis
./dev.sh build-check       # Quick build validation
./dev.sh validate          # Comprehensive validation suite
./dev.sh diagram           # Generate architecture diagrams
```

### Deployment Utilities

#### PackageManager
```python
class PackageManager:
    """Application packaging and distribution."""
    
    def create_distribution(self, target: str) -> str:
        """Create distribution package."""
    
    def validate_package(self, package_path: str) -> bool:
        """Validate package integrity."""
    
    def install_package(self, package_path: str) -> bool:
        """Install package to target system."""
```

---

**Status**: ✅ Complete API Reference Documentation  
**Last Updated**: Documentation Consolidation v1.0  
**Coverage**: All major components and interfaces documented  
**Maintenance**: Update when adding new components or major API changes