# Functional Requirements and Design Criteria

## Table 3.1: System Requirements Matrix

This table enumerates the key functional requirements, design criteria, and how the system design addresses each
requirement.

| Requirement ID | Functional Requirement                           | Design Criteria / Constraint                                                                                 | Design Solution                                                                                                                                                                              | Verification Method                                                                  | Priority  |
|----------------|--------------------------------------------------|--------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|-----------|
| **FR-001**     | Record GSR, thermal, and RGB data simultaneously | - Multi-sensor coordination<br/>- Temporal alignment within 5ms<br/>- No data loss during concurrent capture | - SensorCoordinator class for unified control<br/>- TimeManager with nanosecond precision<br/>- Parallel data capture pipelines<br/>- Independent I/O threads per sensor                     | Integration test with all sensors active, verify timestamps align within tolerance   | Essential |
| **FR-002**     | Remote start/stop control via PC                 | - Network latency < 50ms<br/>- Reliable command delivery<br/>- Synchronous start across devices              | - TCP/IP protocol over Wi-Fi<br/>- JSON command format<br/>- ACK/NACK confirmation<br/>- START_RECORD broadcasts to all devices                                                              | Protocol test: measure command propagation time, verify simultaneous start           | Essential |
| **FR-003**     | Timestamp synchronization within 5ms accuracy    | - Clock drift compensation<br/>- Sub-millisecond precision<br/>- Periodic resync during long sessions        | - NTP-like sync protocol (t1-t4 exchange)<br/>- SYNC_REQUEST/SYNC_RESPONSE messages<br/>- TimeManager.getCurrentTimestampNanos()<br/>- Chrony NTP for PC clock stability                     | Sync test: compare timestamps across devices after 30min session, verify < 5ms drift | Essential |
| **FR-004**     | Multi-device support (8+ Android devices)        | - Scalable architecture<br/>- Independent device management<br/>- Per-device status tracking                 | - DeviceManager maintains device registry<br/>- Individual TCP connections per device<br/>- Concurrent session handling<br/>- Device heartbeat monitoring (2s intervals)                     | Load test with 8 devices recording simultaneously for 60 minutes                     | Important |
| **FR-005**     | Local data storage on Android devices            | - Battery efficiency (avoid network saturation)<br/>- Storage management<br/>- File integrity                | - Session-based directory structure<br/>- CSV for sensor data (thermal, GSR)<br/>- MP4 for video (H.264 encoding)<br/>- metadata.json for session info<br/>- Write buffering to minimize I/O | Storage test: verify all files created, check integrity after 2-hour recording       | Essential |
| **FR-006**     | Automatic data transfer to PC after session      | - Reliable bulk transfer<br/>- Resume capability<br/>- Integrity verification                                | - File manifest exchange<br/>- Chunked transfer (1MB chunks)<br/>- Checksum validation<br/>- Retry on failure with exponential backoff                                                       | Transfer test: interrupt network mid-transfer, verify automatic resume               | Essential |
| **FR-007**     | Real-time monitoring and status display          | - Low-latency status updates<br/>- Battery level warnings<br/>- Storage capacity alerts                      | - Heartbeat messages every 2 seconds<br/>- STATUS_UPDATE messages<br/>- PyQt6 GUI with live widgets<br/>- Color-coded device status (green/yellow/red)                                       | UI test: verify status updates within 3 seconds of state change                      | Important |
| **FR-008**     | Fault tolerance and graceful degradation         | - Continue operation if one sensor fails<br/>- Auto-reconnection<br/>- Data preservation                     | - Per-sensor error handling<br/>- Error state with recovery attempts<br/>- Local recording continues during network loss<br/>- Emergency data save on critical failure                       | Fault injection test: disconnect sensors/network during recording, verify recovery   | Essential |
| **FR-009**     | Session management with unique identifiers       | - No session collision<br/>- Organized storage<br/>- Complete metadata                                       | - Session ID format: session_YYYYMMDD_HHMM<br/>- Unique directory per session<br/>- metadata.json with full session details<br/>- SessionManager tracks lifecycle                            | Session test: create 10 sessions, verify unique IDs and directories                  | Essential |
| **FR-010**     | Thermal camera integration (TC001)               | - USB/OTG connection<br/>- 25 Hz frame rate<br/>- 256x192 resolution<br/>- Temperature calibration           | - TC001 SDK (LibIRParse)<br/>- VID/PID detection (0x0525/0xa4a2)<br/>- UVC protocol over USB<br/>- Temperature range: -20C to +550C<br/>- Accuracy: +/-2C                                    | Hardware test: verify frame rate, resolution, temperature accuracy with reference    | Essential |
| **FR-011**     | GSR sensor integration (Shimmer3)                | - BLE 4.0 connection<br/>- 128 Hz sampling rate<br/>- 12-bit ADC resolution<br/>- Microsiemens conversion    | - ShimmerAndroidAPI<br/>- EasyBLE wrapper for connection<br/>- GATT protocol<br/>- Formula: μS = (ADC/4095) * Vref / Rskin<br/>- Range: 0-50 μS                                              | Sensor test: verify sampling rate, check ADC resolution, validate conversion         | Essential |
| **FR-012**     | RGB video recording                              | - 1920x1080 minimum resolution<br/>- 30 fps minimum<br/>- H.264 encoding<br/>- Low latency preview           | - CameraX API<br/>- Camera2 backend<br/>- MediaRecorder for encoding<br/>- YUV_420_888 format<br/>- MP4 container                                                                            | Video test: verify resolution, frame rate, codec, file playback                      | Essential |
| **FR-013**     | Camera calibration support                       | - Thermal-RGB alignment<br/>- Intrinsic parameters<br/>- Extrinsic parameters                                | - Checkerboard pattern detection<br/>- Zhang's method<br/>- Calibration parameter storage<br/>- Reprojection error calculation                                                               | Calibration test: capture 10 image pairs, verify error < 1 pixel                     | Important |
| **FR-014**     | Configuration management                         | - Sensor parameters configurable<br/>- Network settings<br/>- Storage location                               | - config.json for PC settings<br/>- Android shared preferences<br/>- Runtime parameter validation<br/>- Sensible defaults                                                                    | Config test: modify all parameters, verify applied correctly                         | Important |
| **FR-015**     | Permission management                            | - Runtime permissions on Android<br/>- Hardware access control<br/>- User consent                            | - PermissionManager class<br/>- Camera, Storage, Bluetooth permissions<br/>- USB host feature requirement<br/>- Graceful failure if denied                                                   | Permission test: deny each permission, verify app handles gracefully                 | Essential |

## Design Constraints

| Constraint Category | Constraint                                        | Impact on Design                            | Mitigation Strategy                                                               |
|---------------------|---------------------------------------------------|---------------------------------------------|-----------------------------------------------------------------------------------|
| **Hardware**        | TC001 thermal camera requires USB/OTG             | Android device must support USB host mode   | Require android.hardware.usb.host feature, check at runtime                       |
| **Hardware**        | Shimmer3 requires BLE 4.0+                        | Device must have Bluetooth LE hardware      | Require android.hardware.bluetooth_le, check at runtime                           |
| **Hardware**        | Battery consumption during multi-sensor recording | Limited recording duration on battery power | Optimize sensor polling, use efficient encoding, display battery warnings         |
| **Network**         | Wi-Fi range and stability                         | Connection may drop in large spaces         | Implement auto-reconnection, continue local recording during outages              |
| **Network**         | Network bandwidth for multiple devices            | May saturate network with many devices      | Store data locally, transfer after session, use compression                       |
| **Storage**         | Limited internal storage on Android devices       | Long sessions may fill storage              | Pre-check available space, chunk large files, display storage warnings            |
| **Storage**         | SD card I/O performance                           | May not keep up with high-resolution video  | Use internal storage for video, implement write buffering, monitor I/O latency    |
| **Performance**     | Multi-threaded data capture                       | CPU and memory overhead                     | Use efficient data structures, minimize allocations, profile performance          |
| **Performance**     | Real-time H.264 encoding                          | High CPU usage                              | Use hardware encoder (MediaCodec), configure bitrate appropriately                |
| **Performance**     | Timestamp precision                               | System clock jitter                         | Use CLOCK_MONOTONIC, nanosecond resolution, periodic NTP sync                     |
| **Security**        | Network communication security                    | Data interception risk                      | TLS encryption option, authentication tokens, local network recommendation        |
| **Security**        | Data privacy                                      | Sensitive participant data                  | Local storage only, secure file permissions, user-controlled transfer             |
| **Usability**       | Researcher expertise level                        | Non-experts may struggle with setup         | Provide GUI with clear controls, sensible defaults, in-app help, documentation    |
| **Usability**       | Setup time per experiment                         | Researchers need quick turnaround           | Auto-discovery, minimal configuration, one-click start, session templates         |
| **Reliability**     | Long-duration sessions (2+ hours)                 | Increased risk of failures                  | Health monitoring, automatic recovery, periodic sync, incremental saves           |
| **Reliability**     | Sensor disconnection                              | Data loss risk                              | Graceful degradation, continue with available sensors, auto-reconnection          |
| **Maintainability** | Adding new sensor types                           | Code changes required                       | Modular architecture (RecorderInterface), plugin-style design, external config    |
| **Maintainability** | SDK updates                                       | Breaking changes risk                       | Version pinning, compatibility layer, extensive testing, fallback implementations |
| **Portability**     | Android version compatibility                     | Fragmentation across devices                | Target API 29+, runtime feature checks, conditional code paths                    |
| **Portability**     | PC operating system                               | Cross-platform requirement                  | Use Python with PyQt6, avoid OS-specific code, test on Windows/macOS/Linux        |

## Design Decisions Rationale

### Why TCP instead of UDP?

- **Requirement**: Reliable command delivery (FR-002)
- **Decision**: Use TCP for all PC-Android communication
- **Rationale**:
    - Commands must be delivered reliably (START_RECORD cannot be lost)
    - TCP provides automatic retransmission and ordering
    - Control messages are small (< 1KB), so TCP overhead is negligible
    - File transfer benefits from TCP's flow control and congestion avoidance
- **Alternative Considered**: UDP with custom reliability layer
- **Why Not Chosen**: Unnecessary complexity, TCP is proven and widely available

### Why local storage on Android instead of streaming to PC?

- **Requirement**: Multi-device support with high-quality video (FR-004, FR-012)
- **Decision**: Record data locally on Android, transfer after session
- **Rationale**:
    - Network bandwidth insufficient for 8 devices streaming 1080p30 video simultaneously
    - Local storage avoids network saturation and latency issues
    - Enables offline recording if network temporarily unavailable
    - Battery efficient (Wi-Fi transmit power > storage write power)
- **Alternative Considered**: Real-time streaming to PC
- **Why Not Chosen**: Cannot support 8 devices with video quality requirements

### Why nanosecond timestamps instead of millisecond?

- **Requirement**: Timestamp synchronization within 5ms (FR-003)
- **Decision**: Use System.nanoTime() on Android, time.time_ns() on PC
- **Rationale**:
    - Nanosecond precision provides 6 orders of magnitude more resolution than required
    - Enables future high-speed sensor integration (> 1000 Hz)
    - Eliminates rounding errors in timestamp arithmetic
    - Negligible storage overhead (8 bytes per timestamp)
- **Alternative Considered**: Millisecond timestamps (long int)
- **Why Not Chosen**: Insufficient precision for future requirements

### Why NTP-like sync instead of modifying system clock?

- **Requirement**: Timestamp synchronization without system-level changes (FR-003)
- **Decision**: Implement custom sync protocol, apply offset in software
- **Rationale**:
    - Changing system clock requires root access (not available on all devices)
    - System clock changes affect all apps (unintended consequences)
    - Software offset preserves original timestamps (can recalculate if needed)
    - NTP algorithm is well-established and accurate
- **Alternative Considered**: PTP (Precision Time Protocol)
- **Why Not Chosen**: Requires hardware support, overkill for 5ms requirement

### Why Shimmer3 instead of other GSR sensors?

- **Requirement**: High-quality GSR data for research (FR-011)
- **Decision**: Use Shimmer3 GSR+ sensor
- **Rationale**:
    - Research-grade accuracy (12-bit ADC, 0.1 μS resolution)
    - High sampling rate (51.2-512 Hz configurable)
    - Proven in academic research (many publications)
    - Excellent Android SDK (ShimmerAndroidAPI)
    - BLE connectivity (wireless, low power)
- **Alternative Considered**: Empatica E4
- **Why Not Chosen**: Low sampling rate (4 Hz), cloud-only data, no real-time API

### Why TC001 thermal camera instead of FLIR?

- **Requirement**: Affordable thermal imaging with good SDK (FR-010)
- **Decision**: Use Topdon TC001 thermal camera
- **Rationale**:
    - Android-compatible via USB/OTG (no external hardware)
    - Good resolution (256x192) for facial thermal imaging
    - Reasonable price point (~$300 vs. $400+ for FLIR)
    - InfiSense SDK provides full control and streaming API
    - No licensing restrictions
- **Alternative Considered**: FLIR One Pro
- **Why Not Chosen**: Restrictive SDK licensing, higher cost, lower resolution (160x120 in some models)

### Why PyQt6 for PC GUI instead of web-based?

- **Requirement**: Professional research tool with responsive UI (FR-007)
- **Decision**: Use PyQt6 for PC controller interface
- **Rationale**:
    - Native desktop application (better performance than web)
    - Rich widget library (progress bars, device tables, charts)
    - Cross-platform (Windows, macOS, Linux)
    - Excellent Python integration (backend in Python)
    - Mature and stable (Qt has 25+ years history)
- **Alternative Considered**: Web-based UI (Flask + React)
- **Why Not Chosen**: Network overhead, browser compatibility issues, more complex deployment

This requirements and design criteria matrix ensures all system objectives are met with well-justified design decisions
and comprehensive verification methods.








