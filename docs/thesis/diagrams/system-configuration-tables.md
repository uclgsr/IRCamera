# System Configuration and Protocol Tables

## Table 4.1: System Configuration and Sensor Specification

| Component                       | Specification                                      | Parameters                        | Communication             | Notes                                            |
|---------------------------------|----------------------------------------------------|-----------------------------------|---------------------------|--------------------------------------------------|
| **Topdon TC001 Thermal Camera** | Resolution: 256x192                                | Frame Rate: 25 Hz                 | USB/OTG                   | Hardware VID/PID: 0x0525/0xa4a2, 0x0525/0xa4a5   |
|                                 | Temperature Range: -20 degrees C to +550 degrees C | Accuracy: +/-2 degrees C          | Data Format: CSV          | SDK Integration: IRCMD, LibIRParse, LibIRProcess |
|                                 | Calibration: Hardware emissivity correction        | Emissivity Range: 0.1-1.0         | File Size: ~0.53 GB/30min | Color Palettes: Iron, Rainbow, Grayscale         |
| **Shimmer3 GSR+ Sensor**        | Sampling Rate: 1-1024 Hz (128 Hz default)          | Range: 0-4 microS                 | Bluetooth 2.1+EDR         | IEEE 802.15.1 compliant                          |
|                                 | ADC Resolution: 12-bit (0-4095)                    | Battery: 12+ hours                | Data Format: CSV          | ShimmerAndroidAPI integration                    |
|                                 | Internal Clock: 32 kHz crystal                     | Accuracy: +/-20 ppm               | File Size: ~0.09 GB/30min | Scientific-grade precision                       |
| **RGB Camera (CameraX)**        | Resolution: 1920x1080                              | Frame Rate: 30 fps                | Internal camera API       | H.264 encoding                                   |
|                                 | Encoding: H.264/AVC                                | Quality: High profile             | File Format: MP4          | File Size: ~1.56 GB/30min                        |
|                                 | Focus: Continuous autofocus                        | Stabilization: Available          | Storage: Local device     | CameraX Jetpack library                          |
| **Android Device**              | Min SDK: 26 (Android 8.0)                          | Target SDK: 34 (Android 14)       | Wi-Fi 802.11n/ac          | Samsung Galaxy validated                         |
|                                 | RAM: 6GB+ recommended                              | Storage: 64GB+                    | USB: OTG support required | Multiple device support                          |
|                                 | Bluetooth: BLE 4.0+                                | Permissions: Camera, Storage, BLE | Network: TCP/IP local     | Thread pool management                           |
| **PC Controller**               | Python 3.8+                                        | Qt6/PyQt6                         | TCP Server Port 8080      | Cross-platform (Windows/Linux/macOS)             |
|                                 | RAM: 8GB+ recommended                              | Network: Gigabit Ethernet         | Protocol: JSON/TCP        | Master clock coordination                        |
|                                 | Storage: SSD recommended                           | Sync: Chrony NTP server           | Session management        | Multi-device orchestration                       |

## Table 4.2: Custom Protocol Command Specification

| Command          | Purpose                             | Format                                                  | Parameters                           | Response                                           | Notes                                |
|------------------|-------------------------------------|---------------------------------------------------------|--------------------------------------|----------------------------------------------------|--------------------------------------|
| **HELLO**        | Device identification on connection | `HELLO device_name=<ID> sensors=[<list>]`               | device_name: string, sensors: array  | `ACK cmd=HELLO status=connected`                   | Sent automatically on TCP connection |
| **SYNC_REQUEST** | PC-initiated clock synchronization  | `SYNC_REQUEST t_pc=<timestamp>`                         | t_pc: Unix timestamp (ms)            | `SYNC_RESPONSE t_phone=<ts> offset=<ms>`           | Network latency compensation         |
| **START_RECORD** | Begin recording session             | `START_RECORD session_id=<id>`                          | session_id: unique string            | `ACK cmd=START_RECORD status=<sensor>_ready`       | Multi-phase sensor initialization    |
| **STOP_RECORD**  | End recording session               | `STOP_RECORD session_id=<id>`                           | session_id: matching start command   | `ACK cmd=STOP_RECORD files_saved=<n> size=<bytes>` | Graceful sensor shutdown             |
| **DATA_GSR**     | Live GSR data streaming             | `DATA_GSR ts=<timestamp> value=<microsiemens>`          | ts: nanoseconds, value: double       | N/A (streaming only)                               | 128 Hz streaming rate                |
| **FRAME**        | Thermal/RGB frame data              | `FRAME type=<thermal\|rgb> ts=<timestamp> size=<bytes>` | Binary payload follows               | N/A (data only)                                    | Large binary transfers               |
| **HEARTBEAT**    | Connection keepalive                | `HEARTBEAT status=<state> duration=<seconds>`           | status: enum, duration: integer      | `ACK cmd=HEARTBEAT`                                | 2-second intervals                   |
| **ERROR**        | Error condition reporting           | `ERROR cmd=<original> code=<ERR_CODE> msg="<message>"`  | cmd: string, code: enum, msg: string | `ACK cmd=ERROR`                                    | Standardized error codes             |
| **DISCONNECT**   | Clean connection termination        | `DISCONNECT device_name=<ID>`                           | device_name: string                  | Connection close                                   | Resource cleanup                     |

### Protocol Error Codes

| Code                    | Meaning                 | Recovery Action                |
|-------------------------|-------------------------|--------------------------------|
| `ERR_FAIL`              | General command failure | Retry with exponential backoff |
| `ERR_BUSY`              | Device/sensor busy      | Wait and retry                 |
| `ERR_SENSOR_FAIL`       | Hardware sensor failure | Switch to simulation mode      |
| `ERR_THERMAL_NOT_FOUND` | TC001 not detected      | Check USB connection           |
| `ERR_GSR_NOT_FOUND`     | Shimmer3 not paired     | Initiate BLE discovery         |
| `ERR_INVALID_SESSION`   | Session ID mismatch     | Force session restart          |

## Table 4.3: Hardware Integration Specifications

| Integration Point        | Technology               | Implementation                  | Validation                         | Performance                  |
|--------------------------|--------------------------|---------------------------------|------------------------------------|------------------------------|
| **TC001 Thermal SDK**    | Native Android SDK       | `com.energy.iruvc.*` classes    | Hardware device detection          | 256x192@25Hz sustained       |
|                          | Temperature calibration  | Hardware emissivity correction  | +/-2 degrees C accuracy validation | Production-grade accuracy    |
|                          | Color palette processing | Iron/Rainbow/Grayscale          | Visual thermal rendering           | Professional imaging quality |
| **Shimmer3 Integration** | ShimmerAndroidAPI        | `com.shimmerresearch.android.*` | 12-bit ADC validation              | 128 Hz @ <3ms latency        |
|                          | BLE protocol handling    | Nordic BLE library backend      | Connection reliability testing     | >95% uptime typical          |
|                          | GSR signal processing    | Microsiemens conversion         | Scientific accuracy validation     | Research-grade precision     |
| **CameraX Integration**  | Android Jetpack          | `androidx.camera.*` libraries   | H.264 encoding validation          | 1080p@30fps sustained        |
|                          | Lifecycle management     | Activity/service integration    | Background recording support       | Battery-efficient operation  |
|                          | Permission handling      | Runtime permission system       | User consent flow                  | Privacy compliance           |

## Table 4.4: Network Architecture Configuration

| Layer               | Component             | Configuration              | Performance               | Security               |
|---------------------|-----------------------|----------------------------|---------------------------|------------------------|
| **Transport Layer** | TCP/IP Sockets        | Port 8080, SO_REUSEADDR    | <50ms RTT local network   | Optional TLS 1.3       |
| **Protocol Layer**  | JSON Message Format   | UTF-8 encoding, structured | <23ms avg message latency | Message authentication |
| **Session Layer**   | Connection Management | Persistent connections     | Exponential backoff retry | Device authentication  |
| **Discovery Layer** | Device Detection      | mDNS/manual IP entry       | 78% first-attempt success | Network isolation      |

This comprehensive specification enables reproducible implementation and provides clear technical
documentation for the multi-sensor recording system architecture.







