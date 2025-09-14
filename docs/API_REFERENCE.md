# API Reference - MPDC4GSR Platform

Complete API documentation for the Multi-Modal Physiological Sensing Platform.

## 📡 Communication Protocol

### Protocol Overview

The MPDC4GSR platform uses **JSON over TLS-secured TCP/IP** for all communication between the PC
Controller (Hub) and Android devices (Spokes).

- **Transport**: TCP/IP with TLS 1.2+ encryption
- **Serialization**: JSON with UTF-8 encoding
- **Port**: 8080 (configurable)
- **Discovery**: mDNS/Bonjour service discovery

### Base Message Format

All messages follow this structure:

```json
{
  "message_id": "uuid-v4-string",
  "timestamp": "2024-01-15T14:30:22.123456Z",
  "sender_id": "device-identifier",
  "recipient_id": "target-device-id",
  "message_type": "command|response|data|heartbeat|error",
  "sequence_number": 12345,
  "payload": {
    // Message-specific content
  },
  "checksum": "sha256-hash-optional"
}
```

#### Field Descriptions

| Field             | Type    | Required | Description                             |
| ----------------- | ------- | -------- | --------------------------------------- |
| `message_id`      | string  | ✅       | UUID v4 for message tracking            |
| `timestamp`       | string  | ✅       | ISO 8601 timestamp with microseconds    |
| `sender_id`       | string  | ✅       | Unique identifier of sending device     |
| `recipient_id`    | string  | ❌       | Target device (optional for broadcast)  |
| `message_type`    | enum    | ✅       | Message category                        |
| `sequence_number` | integer | ❌       | For ordering and duplicate detection    |
| `payload`         | object  | ✅       | Message-specific data                   |
| `checksum`        | string  | ❌       | SHA-256 hash for integrity verification |

## 🎛️ Command Messages

### Device Discovery

#### Discover Request (PC → Android)

```json
{
  "message_type": "command",
  "payload": {
    "action": "discover",
    "pc_info": {
      "hostname": "research-pc-01",
      "platform": "Windows 11",
      "software_version": "1.0.0"
    }
  }
}
```

#### Discover Response (Android → PC)

```json
{
  "message_type": "response",
  "payload": {
    "response_to": "discover",
    "device_info": {
      "device_id": "samsung_s22_001",
      "device_name": "Samsung Galaxy S22",
      "android_version": "14",
      "app_version": "1.0.0",
      "capabilities": ["rgb_camera", "gsr_sensor", "thermal_camera"],
      "battery_level": 85,
      "storage_available_gb": 64.2
    }
  }
}
```

### Authentication

#### Authentication Request

```json
{
  "message_type": "command",
  "payload": {
    "action": "authenticate",
    "auth_token": "base64-encoded-token",
    "pc_certificate": "-----BEGIN CERTIFICATE-----\n...",
    "timestamp": 1705328522123456789
  }
}
```

#### Authentication Response

```json
{
  "message_type": "response",
  "payload": {
    "response_to": "authenticate",
    "status": "success|failed",
    "android_certificate": "-----BEGIN CERTIFICATE-----\n...",
    "session_key": "aes-256-key-base64",
    "expires_at": "2024-01-15T20:30:22.123Z"
  }
}
```

### Session Management

#### Start Recording

```json
{
  "message_type": "command",
  "payload": {
    "action": "start_recording",
    "session_id": "session_20240115_143022_Study001_P001",
    "participant_id": "P001",
    "study_protocol": "StressInduction_V2",
    "sync_offset_ns": 1503425,
    "configuration": {
      "recording_duration_ms": 300000,
      "auto_stop": true,
      "rgb_video": {
        "enabled": true,
        "resolution": "4K",
        "frame_rate": 60,
        "codec": "H.264",
        "bitrate_mbps": 25,
        "stabilization": true
      },
      "raw_capture": {
        "enabled": true,
        "frame_rate": 30,
        "format": "DNG",
        "compression": "lossless"
      },
      "gsr_sampling": {
        "enabled": true,
        "rate_hz": 128,
        "filters": ["lowpass_5hz", "notch_50hz"],
        "calibration": "auto",
        "electrode_check": true
      },
      "thermal_recording": {
        "enabled": false,
        "frame_rate": 30,
        "temperature_range": "0-50C",
        "color_palette": "iron"
      }
    }
  }
}
```

#### Start Recording Response

```json
{
  "message_type": "response",
  "payload": {
    "response_to": "start_recording",
    "status": "success|error",
    "session_id": "session_20240115_143022_Study001_P001",
    "message": "Recording started successfully",
    "estimated_duration_ms": 300000,
    "active_sensors": ["rgb_camera", "gsr_sensor"],
    "inactive_sensors": ["thermal_camera"],
    "data_quality": {
      "gsr_signal_quality": 0.92,
      "camera_focus_quality": 0.88,
      "storage_write_speed_mbps": 45.2
    },
    "estimated_file_sizes": {
      "rgb_video_mb": 300,
      "raw_images_mb": 1800,
      "gsr_data_mb": 2.1,
      "total_mb": 2102.1
    }
  }
}
```

#### Stop Recording

```json
{
  "message_type": "command",
  "payload": {
    "action": "stop_recording",
    "session_id": "session_20240115_143022_Study001_P001",
    "emergency_stop": false,
    "reason": "user_initiated"
  }
}
```

#### Stop Recording Response

```json
{
  "message_type": "response",
  "payload": {
    "response_to": "stop_recording",
    "status": "success",
    "session_id": "session_20240115_143022_Study001_P001",
    "actual_duration_ms": 298750,
    "data_summary": {
      "gsr_samples_count": 38240,
      "video_frames_count": 17925,
      "raw_images_count": 8963,
      "sync_events_count": 5,
      "total_file_size_mb": 2089.7
    },
    "data_quality_summary": {
      "gsr_data_quality": 0.94,
      "video_frame_drops": 2,
      "sync_accuracy_ms": 2.3,
      "overall_quality": "excellent"
    },
    "file_locations": [
      "/storage/emulated/0/IRCamera_Sessions/session_20240115_143022_Study001_P001/"
    ]
  }
}
```

### Synchronization Commands

#### Sync Flash

```json
{
  "message_type": "command",
  "payload": {
    "action": "sync_flash",
    "flash_duration_ms": 100,
    "flash_intensity": 1.0,
    "trigger_timestamp_ns": 1705328525000000000,
    "event_metadata": {
      "event_type": "stimulus_presentation",
      "condition": "stress_task_start"
    }
  }
}
```

#### Time Sync Request

```json
{
  "message_type": "command",
  "payload": {
    "action": "time_sync",
    "pc_timestamp_ns": 1705328522123456789,
    "sync_round": 1
  }
}
```

#### Time Sync Response

```json
{
  "message_type": "response",
  "payload": {
    "response_to": "time_sync",
    "pc_timestamp_ns": 1705328522123456789,
    "android_receive_timestamp_ns": 1705328522125460214,
    "android_send_timestamp_ns": 1705328522125962183,
    "sync_round": 1
  }
}
```

## 📊 Data Messages

### Real-time GSR Data

```json
{
  "message_type": "data",
  "payload": {
    "data_type": "gsr_sample",
    "session_id": "session_20240115_143022_Study001_P001",
    "device_timestamp_ns": 1705328522123456789,
    "sync_timestamp_ns": 1705328522125960214,
    "samples": [
      {
        "conductance_us": 12.347,
        "resistance_kohms": 80.923,
        "sample_index": 12345,
        "quality_score": 0.95,
        "electrode_status": "good_contact"
      },
      {
        "conductance_us": 12.356,
        "resistance_kohms": 80.897,
        "sample_index": 12346,
        "quality_score": 0.94,
        "electrode_status": "good_contact"
      }
    ]
  }
}
```

### Video Frame Metadata

```json
{
  "message_type": "data",
  "payload": {
    "data_type": "video_metadata",
    "session_id": "session_20240115_143022_Study001_P001",
    "frame_number": 1800,
    "timestamp_ns": 1705328522123456789,
    "capture_settings": {
      "resolution": "3840x2160",
      "exposure_time_ms": 16.67,
      "iso": 100,
      "focus_distance": 0.5,
      "white_balance": 4000
    },
    "file_info": {
      "frame_file": "frame_001800.jpg",
      "file_size_bytes": 2048576
    }
  }
}
```

### Sensor Status Updates

```json
{
  "message_type": "data",
  "payload": {
    "data_type": "sensor_status",
    "session_id": "session_20240115_143022_Study001_P001",
    "timestamp_ns": 1705328522123456789,
    "sensors": {
      "gsr_sensor": {
        "status": "active",
        "sample_rate": 128.2,
        "signal_quality": 0.92,
        "battery_level": 78,
        "electrode_impedance_kohms": 5.2
      },
      "rgb_camera": {
        "status": "recording",
        "frame_rate": 59.8,
        "focus_locked": true,
        "exposure_locked": false,
        "storage_write_speed_mbps": 45.6
      },
      "thermal_camera": {
        "status": "inactive",
        "reason": "not_available"
      }
    }
  }
}
```

## 🔄 Response Messages

### Standard Response Format

```json
{
  "message_type": "response",
  "payload": {
    "response_to": "original_command_action",
    "status": "success|error|warning",
    "message": "Human readable status message",
    "error_code": "ERROR_CODE_IF_APPLICABLE",
    "data": {
      // Response-specific data
    }
  }
}
```

### Error Response Format

```json
{
  "message_type": "error",
  "payload": {
    "error_code": "SENSOR_UNAVAILABLE",
    "error_message": "Shimmer3 GSR sensor not connected",
    "error_category": "hardware|network|permission|configuration",
    "severity": "low|medium|high|critical",
    "suggested_action": "Check Bluetooth pairing and retry",
    "retry_possible": true,
    "error_details": {
      "sensor_type": "gsr",
      "last_known_status": "disconnected",
      "bluetooth_address": "00:11:22:33:44:55",
      "connection_attempts": 3
    }
  }
}
```

## 💓 Heartbeat Messages

### Heartbeat Request

```json
{
  "message_type": "heartbeat",
  "payload": {
    "sequence": 1234,
    "status": "idle|recording|processing",
    "uptime_ms": 3600000,
    "system_stats": {
      "cpu_usage": 15.2,
      "memory_usage_mb": 512.7,
      "storage_available_gb": 45.8,
      "battery_level": 85
    }
  }
}
```

### Heartbeat Response

```json
{
  "message_type": "heartbeat",
  "payload": {
    "response_to_sequence": 1234,
    "pc_status": "ready",
    "connected_devices": 2,
    "active_sessions": 0,
    "system_stats": {
      "cpu_usage": 8.5,
      "memory_usage_mb": 2048.3,
      "storage_available_gb": 500.2
    }
  }
}
```

## 🔒 Security Protocol

### TLS Configuration

#### Certificate Requirements

- **Encryption**: RSA 2048-bit or ECDSA P-256
- **Hash Algorithm**: SHA-256 or higher
- **Validity**: Maximum 1 year
- **Key Usage**: Digital Signature, Key Encipherment

#### Cipher Suites (Ordered by preference)

1. `TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384`
2. `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384`
3. `TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256`
4. `TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256`

### Authentication Flow

1. **TCP Connection**: Establish connection on port 8080
2. **TLS Handshake**: Mutual certificate verification
3. **Device Discovery**: Exchange device capabilities
4. **Authentication**: Token-based authentication with time-limited session
5. **Session Key Exchange**: Establish session-specific encryption
6. **Command Exchange**: Authenticated command/response cycle

## 📱 Android SDK API

### Core Classes

#### RecordingController

```kotlin
class RecordingController @Inject constructor(
    private val gsrRecorder: GSRRecorder,
    private val cameraRecorder: CameraRecorder,
    private val networkClient: NetworkClient
) {
    
    suspend fun startRecording(config: SessionConfig): Result<Session>
    
    
    suspend fun stopRecording(): Result<SessionData>
    
    
    suspend fun addSyncMarker(eventType: String, metadata: Map<String, Any> = emptyMap())
    
    
    fun getRecordingStatus(): RecordingStatus
}
```

#### GSRRecorder

```kotlin
interface GSRRecorder {
    
    suspend fun initialize(): Result<Unit>
    
    
    suspend fun startRecording(session: Session, syncTimeOffset: Long): Result<Unit>
    
    
    suspend fun stopRecording(): Result<GSRData>
    
    
    fun isDeviceConnected(): Boolean
    
    
    fun getCurrentSampleRate(): Float
    
    
    fun observeGSRData(): Flow<GSRSample>
}
```

#### NetworkClient

```kotlin
class NetworkClient @Inject constructor(
    private val securityManager: SecurityManager
) {
    
    suspend fun discoverPCControllers(timeoutMs: Long = 10000): List<PCController>
    
    
    suspend fun connect(controller: PCController): Result<Connection>
    
    
    suspend fun sendMessage(message: Message): Result<Message>
    
    
    fun observeMessages(): Flow<Message>
}
```

### Data Models

#### GSRSample

```kotlin
@Parcelize
data class GSRSample(
    val timestamp: Long,
    val conductanceMicrosiemens: Double,
    val resistanceKilohms: Double,
    val sampleIndex: Long,
    val qualityScore: Float,
    val sessionId: String
) : Parcelable
```

#### SessionConfig

```kotlin
@Parcelize
data class SessionConfig(
    val sessionId: String,
    val participantId: String,
    val studyProtocol: String,
    val durationMs: Long,
    val rgbVideoConfig: RGBVideoConfig,
    val rawCaptureConfig: RawCaptureConfig,
    val gsrConfig: GSRConfig,
    val thermalConfig: ThermalConfig?
) : Parcelable
```

#### SyncEvent

```kotlin
@Entity(tableName = "sync_events")
data class SyncEvent(
    @PrimaryKey val id: String,
    val sessionId: String,
    val timestamp: Long,
    val eventType: String,
    val metadata: Map<String, Any>
)
```

## 🖥️ PC Controller API

### Core Classes

#### SessionManager

```python
class SessionManager:
    """Manages recording sessions across multiple devices"""

    async def start_session(self, config: SessionConfig) -> Session:
        """Start a new recording session"""

    async def stop_session(self) -> SessionData:
        """Stop current session and collect data"""

    async def add_sync_event(self, event_type: str, metadata: dict = None):
        """Add synchronization marker to all devices"""

    def get_session_status(self) -> SessionStatus:
        """Get current session status and statistics"""
```

#### NetworkController

```python
class NetworkController(QThread):
    """Manages network communication with Android devices"""

    # Signals
    device_discovered = pyqtSignal(Device)
    device_connected = pyqtSignal(Device)
    device_disconnected = pyqtSignal(Device)
    data_received = pyqtSignal(Device, dict)

    async def discover_devices(self, timeout: float = 10.0) -> List[Device]:
        """Discover Android devices on network"""

    async def connect_device(self, device: Device) -> bool:
        """Connect to specific Android device"""

    async def send_command(self, device: Device, command: dict) -> dict:
        """Send command to device and wait for response"""
```

#### DataAggregator

```python
class DataAggregator:
    """Aggregates and synchronizes data from multiple devices"""

    def add_device_data(self, device_id: str, data: DeviceData):
        """Add data from specific device"""

    def synchronize_timestamps(self) -> TimeSyncResult:
        """Synchronize timestamps across all devices"""

    def export_session(self, format: str = "hdf5") -> str:
        """Export session data in specified format"""
```

### Data Models

#### Session

```python
@dataclass
class Session:
    id: str
    participant_id: str
    study_protocol: str
    start_time: datetime
    end_time: Optional[datetime]
    devices: List[Device]
    configuration: SessionConfig
    data_quality: DataQuality
```

#### Device

```python
@dataclass
class Device:
    id: str
    name: str
    ip_address: str
    capabilities: List[str]
    connection_status: ConnectionStatus
    last_heartbeat: datetime
    battery_level: int
    storage_available: float
```

## 🔧 Configuration API

### Android Configuration

#### Runtime Configuration

```kotlin
// Application-level configuration
object AppConfig {
    const val DEFAULT_GSR_SAMPLE_RATE = 128
    const val DEFAULT_VIDEO_BITRATE = 25_000_000
    const val MAX_SESSION_DURATION_MS = 3600_000L
    const val SYNC_TOLERANCE_MS = 5L

    // Network settings
    const val PC_DISCOVERY_PORT = 8080
    const val CONNECTION_TIMEOUT_MS = 10_000L
    const val HEARTBEAT_INTERVAL_MS = 30_000L
}

// Session-specific configuration
@Parcelize
data class SessionConfig(
    val sessionId: String,
    val participantId: String,
    val studyProtocol: String,
    val durationMs: Long = 300_000L,
    val autoStop: Boolean = true,

    // Video configuration
    val videoResolution: VideoResolution = VideoResolution.UHD_4K,
    val videoFrameRate: Int = 60,
    val videoBitrate: Int = 25_000_000,

    // RAW capture configuration
    val rawCaptureEnabled: Boolean = false,
    val rawFrameRate: Int = 30,

    // GSR configuration
    val gsrSampleRate: Int = 128,
    val gsrFilters: List<String> = listOf("lowpass_5hz"),

    // Sync configuration
    val syncFlashEnabled: Boolean = true,
    val syncEventTypes: List<String> = emptyList()
) : Parcelable
```

### PC Controller Configuration

#### settings.json

```json
{
  "network": {
    "discovery_port": 8080,
    "max_devices": 4,
    "connection_timeout_ms": 10000,
    "heartbeat_interval_ms": 30000
  },
  "synchronization": {
    "tolerance_ms": 5,
    "sync_rounds": 3,
    "max_offset_ms": 50
  },
  "data": {
    "export_format": "hdf5",
    "compression": true,
    "backup_enabled": true,
    "retention_days": 90
  },
  "gui": {
    "theme": "dark",
    "plot_update_interval_ms": 100,
    "max_plot_points": 1000
  },
  "logging": {
    "level": "INFO",
    "file_rotation": true,
    "max_file_size_mb": 10,
    "backup_count": 5
  }
}
```

## 📈 Performance Metrics

### API Performance Targets

| Operation                | Target Latency | Throughput | Notes                   |
| ------------------------ | -------------- | ---------- | ----------------------- |
| Device Discovery         | <5 seconds     | N/A        | Network dependent       |
| Connection Establishment | <2 seconds     | N/A        | Including TLS handshake |
| Command Response         | <100ms         | N/A        | Simple commands         |
| GSR Data Streaming       | <10ms          | 128 Hz     | Real-time constraint    |
| Video Metadata           | <50ms          | 60 Hz      | Frame metadata          |
| Sync Flash               | <5ms           | N/A        | Critical timing         |
| Session Start            | <3 seconds     | N/A        | All sensors initialized |
| Session Stop             | <5 seconds     | N/A        | Data finalization       |

### Error Handling

#### Retry Policies

```python
@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=4, max=10),
    retry=retry_if_exception_type(NetworkError)
)
async def send_command_with_retry(device: Device, command: dict) -> dict:
    """Send command with automatic retry logic"""
    return await device.send_command(command)
```

#### Circuit Breaker Pattern

```kotlin
class CircuitBreakerNetworkClient(
    private val failureThreshold: Int = 5,
    private val recoveryTimeout: Long = 30_000L
) {
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var state = CircuitState.CLOSED

    suspend fun sendMessage(message: Message): Result<Message> {
        when (state) {
            CircuitState.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > recoveryTimeout) {
                    state = CircuitState.HALF_OPEN
                } else {
                    return Result.failure(CircuitOpenException())
                }
            }
            CircuitState.HALF_OPEN -> {
                // Test with single request
            }
            CircuitState.CLOSED -> {
                // Normal operation
            }
        }

        return try {
            val result = actualSendMessage(message)
            onSuccess()
            Result.success(result)
        } catch (e: Exception) {
            onFailure()
            Result.failure(e)
        }
    }
}
```

---

**This API reference provides the complete interface specification for integrating with the MPDC4GSR
platform. For implementation examples and tutorials, refer to the Developer Guide.**
