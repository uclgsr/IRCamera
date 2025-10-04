# Communication Sequence Diagram (PC-Device Interaction)

## Figure 3.3: Protocol Sequence Diagram - PC to Android Communication

This sequence diagram shows the step-by-step messaging between the PC orchestrator and the Android device, tracing how commands travel, how the device acknowledges and initializes sensors, how data recording occurs, and how STOP/SYNC commands are handled.

```mermaid
sequenceDiagram
    autonumber
    participant User as 👤 Researcher
    participant PC as 🖥️ PC Orchestrator<br/>(Python/PyQt6)
    participant NetServer as 🌐 Network Server<br/>(TCP :8080)
    participant Android as 📱 Android Device<br/>(Recording App)
    participant Thermal as 🌡️ TC001 Thermal<br/>(USB/OTG)
    participant GSR as ⚡ Shimmer3 GSR<br/>(Bluetooth LE)
    participant Camera as 📹 RGB Camera<br/>(CameraX)
    participant Storage as 💾 Local Storage<br/>(Session Directory)
    participant TimeSync as ⏱️ Time Manager<br/>(Sync Service)

    rect rgb(230, 245, 255)
        Note over User,TimeSync: ═══ Phase 1: Connection and Discovery ═══
        
        User->>PC: Launch Application
        activate PC
        PC->>PC: Initialize SessionManager<br/>Load configuration
        PC->>NetServer: Start TCP Server (Port 8080)
        activate NetServer
        NetServer->>NetServer: bind(0.0.0.0:8080)<br/>listen(backlog=5)<br/>Set SO_REUSEADDR
        NetServer-->>PC: Server ready
        PC->>User: Display "Waiting for devices..."
        deactivate PC
        
        User->>Android: Launch Recording App
        activate Android
        Android->>Android: Check prerequisites:<br/>- Permissions<br/>- Storage space<br/>- Battery level
        
        alt mDNS Auto-Discovery
            Android->>Android: Start mDNS scan<br/>Service: _ircamera._tcp
            Android->>NetServer: mDNS query
            NetServer-->>Android: mDNS response<br/>IP: 192.168.1.100
            Android->>Android: Parse PC IP address
        else Manual Connection
            User->>Android: Enter PC IP manually
            Android->>Android: Validate IP format
        end
        
        critical Establish TCP Connection
            Android->>NetServer: SYN (TCP handshake)
            NetServer->>Android: SYN-ACK
            Android->>NetServer: ACK
            Note right of Android: Connection timeout: 30s
        option Connection Failed
            Android->>User: Display "Connection Failed"
            Android->>Android: Retry with backoff
        end
        
        Android->>NetServer: HELLO {<br/>  device_name: "Samsung_S22_001",<br/>  device_id: "uuid-abc123",<br/>  app_version: "1.2.3",<br/>  capabilities: ["thermal","gsr","rgb"],<br/>  timestamp: 1703441234567<br/>}
        
        NetServer->>PC: Forward HELLO
        activate PC
        PC->>PC: Validate protocol version<br/>Create DeviceManager entry<br/>Allocate device slot
        
        opt Device Already Connected
            PC->>PC: Check existing connections<br/>Handle reconnection
            PC->>NetServer: Send RECONNECT notification
        end
        
        PC->>NetServer: ACK_HELLO {<br/>  pc_timestamp: 1703441234568,<br/>  session_ready: true,<br/>  server_version: "2.0.1",<br/>  supported_features: ["sync","transfer"]<br/>}
        deactivate PC
        
        NetServer->>Android: Forward ACK_HELLO
        Android->>Android: Store PC info<br/>Transition to IDLE state
        deactivate Android
        
        NetServer->>PC: Connection registered
        PC->>User: 🟢 Display "Samsung_S22_001 Connected"
        deactivate NetServer
    end
    
    rect rgb(232, 245, 233)
        Note over User,TimeSync: ═══ Phase 2: Time Synchronization (NTP-like Protocol) ═══
        
        activate PC
        PC->>TimeSync: Initiate sync protocol
        activate TimeSync
        TimeSync->>TimeSync: Record t1 = time.time_ns()<br/>t1 = 1703441234567000000
        TimeSync->>NetServer: SYNC_REQUEST {<br/>  t1: 1703441234567000000,<br/>  sequence: 1<br/>}
        deactivate TimeSync
        deactivate PC
        
        activate NetServer
        NetServer->>Android: Forward SYNC_REQUEST
        Note right of NetServer: Network latency ~1-2ms
        deactivate NetServer
        
        activate Android
        Android->>TimeSync: Process sync request
        activate TimeSync
        TimeSync->>TimeSync: Record t2 = System.nanoTime()<br/>t2 = 1703441234569000000<br/>(Receive time)
        TimeSync->>TimeSync: Prepare t3 = System.nanoTime()<br/>t3 = 1703441234570000000<br/>(Send time)
        TimeSync->>Android: Sync data ready
        deactivate TimeSync
        
        Android->>NetServer: SYNC_RESPONSE {<br/>  t2: 1703441234569000000,<br/>  t3: 1703441234570000000,<br/>  sequence: 1<br/>}
        deactivate Android
        
        activate NetServer
        NetServer->>PC: Forward SYNC_RESPONSE
        deactivate NetServer
        
        activate PC
        PC->>TimeSync: Process sync response
        activate TimeSync
        TimeSync->>TimeSync: Record t4 = time.time_ns()<br/>t4 = 1703441234572000000<br/>(Receive time)
        
        critical Calculate Clock Offset
            TimeSync->>TimeSync: Calculate RTT:<br/>rtt = (t4 - t1) - (t3 - t2)<br/>rtt = 5000000000 - 1000000000<br/>rtt = 4ms
            
            TimeSync->>TimeSync: Calculate offset:<br/>offset = ((t2 - t1) + (t3 - t4)) / 2<br/>offset = (2ms + (-2ms)) / 2<br/>offset = +2ms
            
            TimeSync->>TimeSync: Store offset and RTT<br/>Drift monitoring enabled
        end
        
        TimeSync->>PC: Sync complete<br/>Offset: +2ms, RTT: 4ms
        deactivate TimeSync
        
        PC->>NetServer: ACK_SYNC {<br/>  offset_ms: 2.0,<br/>  rtt_ms: 4.0,<br/>  status: "synced"<br/>}
        deactivate PC
        
        activate NetServer
        NetServer->>Android: Forward ACK_SYNC
        deactivate NetServer
        
        activate Android
        Android->>TimeSync: Apply offset
        activate TimeSync
        TimeSync->>TimeSync: clock_offset = +2ms<br/>Update timestamp function<br/>Enable periodic sync (30s)
        deactivate TimeSync
        Android->>Android: Sync status: ✓ Synchronized
        deactivate Android
        
        Note over PC,Android: Time sync accuracy: ±2ms<br/>Periodic re-sync: Every 30 seconds
    end
    
    rect rgb(255, 243, 224)
        Note over User,TimeSync: ═══ Phase 3: Capability Exchange & Hardware Detection ═══
        
        User->>PC: Click "Refresh Devices"
        activate PC
        PC->>NetServer: GET_CAPABILITIES {<br/>  device_id: "uuid-abc123"<br/>}
        deactivate PC
        
        activate NetServer
        NetServer->>Android: Forward GET_CAPABILITIES
        deactivate NetServer
        
        activate Android
        Android->>Android: Start hardware scan
        
        par Parallel Hardware Detection
            Android->>Thermal: Query USB devices
            activate Thermal
            Thermal->>Thermal: Enumerate USB<br/>Check VID:PID = 0x0525:0xa4a2
            
            alt TC001 Device Found
                Thermal->>Thermal: Initialize TC001 SDK<br/>Query capabilities
                Thermal->>Android: {<br/>  available: true,<br/>  model: "TC001",<br/>  resolution: "256x192",<br/>  fps: 25,<br/>  temp_range: "-20 to 550°C",<br/>  interface: "USB-C/OTG"<br/>}
            else No Device
                Thermal->>Android: {<br/>  available: false,<br/>  error: "Device not found"<br/>}
            end
            deactivate Thermal
        and
            Android->>GSR: Scan BLE devices
            activate GSR
            GSR->>GSR: Start BLE scan<br/>Filter: Shimmer3 signature
            
            opt Shimmer3 Found
                GSR->>GSR: Read device characteristics<br/>Query sampling rates
                GSR->>Android: {<br/>  available: true,<br/>  model: "Shimmer3 GSR+",<br/>  sampling_rates: [51.2,128,256,512],<br/>  sensors: ["GSR","PPG"],<br/>  connection: "BLE 4.0",<br/>  battery: "87%"<br/>}
            end
            
            opt No Device Found
                GSR->>Android: {<br/>  available: false,<br/>  error: "No Shimmer3 detected"<br/>}
            end
            deactivate GSR
        and
            Android->>Camera: Query camera system
            activate Camera
            Camera->>Camera: Enumerate cameras<br/>Check permissions
            
            alt Permission Granted
                Camera->>Camera: Query supported formats<br/>Query supported resolutions
                Camera->>Android: {<br/>  available: true,<br/>  cameras: [{<br/>    id: "0",<br/>    facing: "BACK",<br/>    resolutions: ["1920x1080","3840x2160"],<br/>    fps_ranges: ["15-30","30-30"],<br/>    formats: ["YUV_420_888","JPEG"]<br/>  }]<br/>}
            else Permission Denied
                Camera->>Android: {<br/>  available: false,<br/>  error: "Camera permission required"<br/>}
            end
            deactivate Camera
        end
        
        Android->>Android: Aggregate capabilities<br/>Check storage space<br/>Check battery level
        
        Android->>NetServer: CAPABILITIES {<br/>  device_info: {<br/>    manufacturer: "Samsung",<br/>    model: "Galaxy S22",<br/>    android_version: "13",<br/>    storage_free_gb: 45.2,<br/>    battery_level: 87<br/>  },<br/>  sensors: {<br/>    thermal: {...},<br/>    gsr: {...},<br/>    rgb: {...}<br/>  },<br/>  status: "ready"<br/>}
        deactivate Android
        
        activate NetServer
        NetServer->>PC: Forward CAPABILITIES
        deactivate NetServer
        
        activate PC
        PC->>PC: Validate capabilities<br/>Check compatibility<br/>Update device registry
        PC->>User: 📊 Display:<br/>━━━━━━━━━━━━━━<br/>🌡️ Thermal: ✓ TC001 (256x192@25Hz)<br/>⚡ GSR: ✓ Shimmer3 (128Hz)<br/>📹 RGB: ✓ Camera (1920x1080@30fps)<br/>💾 Storage: 45GB free<br/>🔋 Battery: 87%
        deactivate PC
    end
    
    rect rgb(252, 228, 236)
        Note over User,TimeSync: ═══ Phase 4: Session Initiation & Validation ═══
        
        User->>PC: Click "Start Recording"<br/>Enter session name
        activate PC
        PC->>PC: Generate session_id:<br/>session_20241215_1430
        
        critical Validate Session Start
            PC->>PC: Check preconditions:<br/>✓ Device connected<br/>✓ Sensors available<br/>✓ No active session<br/>✓ Storage space sufficient
        option Validation Failed
            PC->>User: ❌ Display error<br/>Cannot start recording
        end
        
        PC->>PC: Create session directory:<br/>/data/sessions/session_20241215_1430/
        
        PC->>PC: Initialize metadata.json:<br/>{<br/>  session_id: "session_20241215_1430",<br/>  created_at: ISO8601_timestamp,<br/>  devices: [...],<br/>  status: "initializing"<br/>}
        
        PC->>NetServer: START_RECORD {<br/>  session_id: "session_20241215_1430",<br/>  pc_timestamp: 1703441234567,<br/>  config: {<br/>    thermal_enabled: true,<br/>    gsr_enabled: true,<br/>    rgb_enabled: true,<br/>    thermal_fps: 25,<br/>    gsr_rate: 128,<br/>    rgb_resolution: "1920x1080",<br/>    rgb_fps: 30<br/>  }<br/>}
        deactivate PC
        
        activate NetServer
        NetServer->>Android: Forward START_RECORD
        Note right of NetServer: Command timeout: 30s
        deactivate NetServer
        
        activate Android
        Android->>Android: State validation:<br/>Current: IDLE<br/>Expected: IDLE<br/>✓ Valid transition
        
        alt State Check Failed
            Android->>NetServer: ERROR {<br/>  code: "INVALID_STATE",<br/>  message: "Already recording"<br/>}
            NetServer->>PC: Forward ERROR
            PC->>User: ❌ Cannot start: Device busy
        end
        
        Android->>Storage: Create session directory:<br/>/sdcard/IRCamera/sessions/<br/>session_20241215_1430/
        activate Storage
        Storage->>Storage: mkdir -p<br/>Check write permissions<br/>Verify space (>1GB required)
        Storage-->>Android: Directory created
        deactivate Storage
        
        Android->>Android: Transition to INITIALIZING state
        deactivate Android
    end
    
    rect rgb(255, 248, 225)
        Note over User,TimeSync: ═══ Phase 5: Parallel Sensor Initialization ═══
        
        critical Initialize All Sensors (Timeout: 10s)
            par Thermal Camera Init
                activate Android
                Android->>Thermal: Initialize TC001
                activate Thermal
                
                Thermal->>Thermal: 1. Request USB permission
                opt Permission Required
                    Thermal->>User: Show permission dialog
                    User-->>Thermal: Grant permission
                end
                
                Thermal->>Thermal: 2. Open USB device<br/>VID:PID = 0x0525:0xa4a2
                Thermal->>Thermal: 3. Initialize SDK:<br/>IRCMD.init()<br/>Set emissivity: 0.95<br/>Set temp range: 0-100°C
                Thermal->>Thermal: 4. Register frame callback:<br/>IFrameCallback.onFrame()
                Thermal->>Thermal: 5. Configure output:<br/>Format: 16-bit temperature<br/>Resolution: 256x192
                
                alt Init Success
                    Thermal->>Android: ✓ Init complete<br/>First frame received
                    Android->>NetServer: ACK {<br/>  sensor: "thermal",<br/>  status: "ready",<br/>  config: {resolution: "256x192", fps: 25}<br/>}
                else Init Failed
                    Thermal->>Android: ❌ Init failed<br/>Error details
                    Android->>NetServer: ERROR {<br/>  sensor: "thermal",<br/>  error: "USB device not found"<br/>}
                end
                deactivate Thermal
                deactivate Android
            and GSR Sensor Init
                activate Android
                Android->>GSR: Initialize Shimmer3
                activate GSR
                
                GSR->>GSR: 1. Start BLE scan<br/>Scan duration: 10s
                GSR->>GSR: 2. Filter devices:<br/>Name matches "Shimmer*"
                GSR->>GSR: 3. Connect to GATT:<br/>MAC: AA:BB:CC:DD:EE:FF
                GSR->>GSR: 4. Discover services:<br/>Find GSR service UUID
                GSR->>GSR: 5. Enable notifications:<br/>GSR characteristic
                GSR->>GSR: 6. Configure sampling:<br/>Send command 0x07<br/>Rate: 128 Hz
                GSR->>GSR: 7. Wait first sample:<br/>Timeout: 3s
                
                opt First Sample Received
                    GSR->>Android: ✓ Streaming active<br/>Sample rate: 128Hz
                    Android->>NetServer: ACK {<br/>  sensor: "gsr",<br/>  status: "ready",<br/>  config: {rate: 128, units: "microsiemens"}<br/>}
                end
                
                opt Connection Failed
                    GSR->>Android: ❌ Cannot connect<br/>Device not found
                    Android->>NetServer: WARNING {<br/>  sensor: "gsr",<br/>  warning: "Optional sensor unavailable"<br/>}
                end
                deactivate GSR
                deactivate Android
            and RGB Camera Init
                activate Android
                Android->>Camera: Initialize CameraX
                activate Camera
                
                Camera->>Camera: 1. Request CAMERA permission
                opt Permission Required
                    Camera->>User: Show permission dialog
                    User-->>Camera: Grant permission
                end
                
                Camera->>Camera: 2. Select camera:<br/>cameraSelector = BACK
                Camera->>Camera: 3. Create use cases:<br/>- Preview<br/>- VideoCapture
                Camera->>Camera: 4. Configure video:<br/>Quality: HD (1920x1080)<br/>Frame rate: 30 fps<br/>Codec: H.264<br/>Container: MP4
                Camera->>Camera: 5. Setup MediaRecorder:<br/>Bitrate: 15 Mbps<br/>Audio: AAC @ 128kbps
                Camera->>Camera: 6. Bind to lifecycle:<br/>ProcessCameraProvider
                Camera->>Camera: 7. Start preview
                
                alt Camera Ready
                    Camera->>Android: ✓ Camera configured<br/>Preview active
                    Android->>NetServer: ACK {<br/>  sensor: "rgb",<br/>  status: "ready",<br/>  config: {<br/>    resolution: "1920x1080",<br/>    fps: 30,<br/>    codec: "H.264"<br/>  }<br/>}
                else Camera Error
                    Camera->>Android: ❌ Camera error<br/>Details
                    Android->>NetServer: ERROR {<br/>  sensor: "rgb",<br/>  error: "Camera unavailable"<br/>}
                end
                deactivate Camera
                deactivate Android
            end
        option Initialization Timeout
            Android->>NetServer: ERROR {<br/>  code: "INIT_TIMEOUT",<br/>  message: "Sensor init exceeded 10s"<br/>}
        end
        
        activate NetServer
        NetServer->>PC: Collect all sensor ACKs
        deactivate NetServer
        
        activate PC
        PC->>PC: Validate sensor status:<br/>✓ Thermal: Ready<br/>✓ GSR: Ready<br/>✓ RGB: Ready
        PC->>User: 🟢 Display "All Sensors Ready"
        deactivate PC
        
        activate Android
        Android->>Android: Join point reached<br/>All sensors initialized<br/>Transition to READY state
        
        Android->>NetServer: READY_TO_RECORD {<br/>  session_id: "session_20241215_1430",<br/>  sensors_ready: ["thermal","gsr","rgb"],<br/>  timestamp: 1703441235000<br/>}
        deactivate Android
        
        NetServer->>PC: Device READY
        activate PC
        PC->>User: Display "Device Ready - Click to begin"
        deactivate PC
    end
        Android->>Thermal: Initialize TC001 SDK
        activate Thermal
        Thermal->>Thermal: IRCMD.init()<br/>Set emissivity, temp range<br/>Register frame callback
        Thermal->>Android: Init success
        deactivate Thermal
        Android->>NetServer: ACK START_RECORD {sensor=thermal, status=initializing}
        
        Android->>GSR: Start BLE discovery
        activate GSR
        GSR->>GSR: Scan for Shimmer3<br/>MAC address known
        GSR->>Android: Device found
        Android->>GSR: Connect GATT
        GSR->>Android: Connected
        Android->>GSR: Send 0x07 (start streaming)
        GSR->>Android: Streaming started (128Hz)
        deactivate GSR
        Android->>NetServer: ACK START_RECORD {sensor=gsr, status=initializing}
        
        Android->>Camera: Initialize CameraX
        activate Camera
        Camera->>Camera: Request permissions<br/>Configure 1920x1080@30fps<br/>Setup H.264 encoder
        Camera->>Android: Ready for recording
        deactivate Camera
        Android->>NetServer: ACK START_RECORD {sensor=rgb, status=initializing}
    end
    
    NetServer->>PC: All sensor ACKs received
    PC->>User: Display "Sensors Initializing..."
    
    Android->>Android: Wait for all sensors ready<br/>(Join point)
    Android->>Android: Transition to READY state
    
    Note over User,Storage: Phase 5: Recording Start
    
    Android->>NetServer: READY_TO_RECORD {all_sensors_ready}
    NetServer->>PC: Device ready
    PC->>User: Display "Device Ready"
    
    PC->>NetServer: BEGIN_RECORDING {final_sync_timestamp}
    NetServer->>Android: BEGIN_RECORDING
    
    activate Android
    par Start All Sensors
        Android->>Thermal: Start capture
        activate Thermal
        Thermal->>Thermal: LibIRParse.startStream()<br/>Frame callback active
        Thermal-->>Android: Frame callback (256x192 data)
        Android->>Android: TimeManager.getCurrentTimestampNanos()<br/>ts=1703441234570123456
        Android->>Android: Temperature calibration<br/>(+/-2 degrees C)
        Android->>Storage: Write CSV: ts,w,h,t0,t1,...,t49151
        deactivate Thermal
        
        Android->>GSR: Confirm streaming
        activate GSR
        GSR-->>Android: ADC data @128Hz<br/>(12-bit raw value)
        Android->>Android: Convert to microsiemens<br/>μS = (ADC/4095) * Vref / Rskin
        Android->>Storage: Write CSV: ts,gsr_microsiemens,ppg
        deactivate GSR
        
        Android->>Camera: Start recording
        activate Camera
        Camera-->>Android: Frame callbacks @30fps<br/>(YUV_420_888)
        Android->>Android: H.264 encoding<br/>MediaRecorder
        Android->>Storage: Write MP4: rgb_video.mp4
        deactivate Camera
    end
    deactivate Android
    
    Android->>NetServer: STATUS_UPDATE {state=RECORDING, time_elapsed=0}
    NetServer->>PC: Recording started
    PC->>User: Display "Recording Active" (00:00)
    
    Note over User,Storage: Phase 6: Active Recording (Continuous Data Flow)
    
    loop Every 2 seconds (Heartbeat)
        Android->>NetServer: HEARTBEAT {status=recording, duration=elapsed}
        NetServer->>PC: Update device status
        PC->>User: Update timer display
    end
    
    loop Continuous Data Capture
        GSR-->>Android: ADC sample (128Hz)
        Android->>Android: Timestamp + Convert
        Android->>Storage: Append to gsr_data.csv
        
        Thermal-->>Android: Thermal frame (25Hz)
        Android->>Android: Timestamp + Calibrate
        Android->>Storage: Append to thermal_data.csv
        
        Camera-->>Android: Video frame (30fps)
        Android->>Android: Timestamp + Encode
        Android->>Storage: Write to rgb_video.mp4
    end
    
    Note over User,Storage: Phase 7: Mid-Session Time Sync
    
    PC->>NetServer: SYNC_REQUEST {t1=current_pc_time}
    NetServer->>Android: SYNC_REQUEST (does not stop recording)
    activate Android
    Android->>Android: Record t2, t3<br/>(recording continues)
    Android->>NetServer: SYNC_RESPONSE {t2, t3}
    deactivate Android
    NetServer->>PC: Calculate new offset
    
    activate PC
    PC->>PC: Check drift:<br/>new_offset = +2.7ms<br/>drift = 0.7ms (acceptable)
    deactivate PC
    
    PC->>NetServer: ACK SYNC {drift_ok}
    NetServer->>Android: ACK SYNC
    Android->>Android: Apply minor correction<br/>(if needed)
    
    Note over User,Storage: Phase 8: Session Termination
    
    User->>PC: Click "Stop Recording"<br/>(after 15 minutes)
    activate PC
    PC->>PC: Validate session state
    PC->>NetServer: STOP_RECORD {session_id, final_timestamp}
    deactivate PC
    NetServer->>Android: STOP_RECORD
    
    activate Android
    par Graceful Sensor Shutdown
        Android->>Thermal: Stop capture
        activate Thermal
        Thermal->>Thermal: LibIRProcess.stopStream()<br/>Release resources
        Thermal->>Android: Stopped
        deactivate Thermal
        
        Android->>GSR: Stop streaming
        activate GSR
        Android->>GSR: Send 0x20 (stop command)
        GSR->>GSR: Halt ADC sampling
        GSR->>Android: Stopped
        deactivate GSR
        
        Android->>Camera: Stop recording
        activate Camera
        Camera->>Camera: CameraX.stopRecording()<br/>Finalize MP4 file
        Camera->>Android: Video saved
        deactivate Camera
    end
    
    Android->>Storage: Close CSV files<br/>Flush buffers
    Android->>Storage: Generate metadata.json<br/>{session_id, start_time,<br/>end_time, duration, files[]}
    deactivate Android
    
    Android->>NetServer: ACK STOP_RECORD {files_ready, file_count=5}
    NetServer->>PC: Stop acknowledged
    PC->>User: Display "Finalizing..."
    
    Note over User,Storage: Phase 9: File Transfer
    
    PC->>NetServer: REQUEST_FILES {session_id}
    NetServer->>Android: REQUEST_FILES
    
    activate Android
    Android->>Storage: Read file list<br/>thermal_data.csv<br/>gsr_data.csv<br/>rgb_video.mp4<br/>metadata.json
    Android->>Android: Calculate total size: 2.3 GB
    Android->>NetServer: FILE_MANIFEST {file_list, total_size}
    deactivate Android
    
    NetServer->>PC: File manifest received
    PC->>User: Display "Transferring files (2.3 GB)..."
    
    loop For each file
        PC->>NetServer: REQUEST_FILE {filename}
        NetServer->>Android: REQUEST_FILE
        Android->>Storage: Read file (chunked)
        
        loop Until file complete
            Android->>NetServer: FILE_CHUNK {data, offset, chunk_size}
            NetServer->>PC: Write chunk to disk
            PC->>User: Update progress (e.g., 45%)
        end
        
        Android->>NetServer: FILE_COMPLETE {filename, checksum}
        NetServer->>PC: Verify checksum
        PC->>NetServer: ACK_FILE {filename}
        NetServer->>Android: ACK_FILE
    end
    
    Android->>NetServer: ALL_FILES_SENT
    NetServer->>PC: Transfer complete
    
    activate PC
    PC->>PC: Update session metadata<br/>Mark session complete
    PC->>User: Display "Session Complete"<br/>"5 files saved (2.3 GB)"
    deactivate PC
    
    Note over User,Storage: Phase 10: Connection Cleanup
    
    PC->>NetServer: SESSION_COMPLETE {session_id}
    NetServer->>Android: SESSION_COMPLETE
    Android->>Android: Clean up session resources<br/>Transition to IDLE
    Android->>NetServer: READY_FOR_NEW_SESSION
    NetServer->>PC: Device ready
    PC->>User: Display "Ready for new session"
```

## Protocol Message Specifications

### Connection Phase Messages

**HELLO** (Android → PC)
```json
{
  "command": "HELLO",
  "device_id": "Samsung_S22_001",
  "device_name": "Samsung Galaxy S22",
  "app_version": "1.2.3",
  "capabilities": ["thermal", "gsr", "rgb"],
  "timestamp": 1703441234567
}
```

**ACK HELLO** (PC → Android)
```json
{
  "command": "ACK",
  "ack_for": "HELLO",
  "status": "connected",
  "pc_timestamp": 1703441234568,
  "session_ready": true
}
```

### Time Synchronization Messages

**SYNC_REQUEST** (PC → Android)
```json
{
  "command": "SYNC_REQUEST",
  "t1": 1703441234567,
  "sequence_number": 42
}
```

**SYNC_RESPONSE** (Android → PC)
```json
{
  "command": "SYNC_RESPONSE",
  "t2": 1703441234569,
  "t3": 1703441234570,
  "sequence_number": 42
}
```

### Recording Control Messages

**START_RECORD** (PC → Android)
```json
{
  "command": "START_RECORD",
  "session_id": "session_20241215_1430",
  "pc_timestamp": 1703441234567,
  "config": {
    "thermal_enabled": true,
    "gsr_enabled": true,
    "rgb_enabled": true,
    "thermal_fps": 25,
    "gsr_rate": 128,
    "rgb_resolution": "1920x1080",
    "rgb_fps": 30
  }
}
```

**ACK START_RECORD** (Android → PC)
```json
{
  "command": "ACK",
  "ack_for": "START_RECORD",
  "sensor": "thermal",
  "status": "initializing",
  "timestamp": 1703441234570
}
```

**STOP_RECORD** (PC → Android)
```json
{
  "command": "STOP_RECORD",
  "session_id": "session_20241215_1430",
  "final_timestamp": 1703442134567
}
```

### Status and Monitoring Messages

**HEARTBEAT** (Android → PC)
```json
{
  "command": "HEARTBEAT",
  "status": "recording",
  "duration": 123.45,
  "thermal_frames": 3086,
  "gsr_samples": 15802,
  "rgb_frames": 3704,
  "battery_level": 87,
  "storage_free_mb": 45230,
  "timestamp": 1703441357012
}
```

**STATUS_UPDATE** (Android → PC)
```json
{
  "command": "STATUS_UPDATE",
  "state": "RECORDING",
  "time_elapsed": 123.45,
  "thermal_status": "active",
  "gsr_status": "active",
  "rgb_status": "active",
  "warnings": [],
  "timestamp": 1703441357012
}
```

### File Transfer Messages

**FILE_MANIFEST** (Android → PC)
```json
{
  "command": "FILE_MANIFEST",
  "session_id": "session_20241215_1430",
  "files": [
    {"name": "thermal_data.csv", "size": 125000000},
    {"name": "gsr_data.csv", "size": 8500000},
    {"name": "rgb_video.mp4", "size": 2150000000},
    {"name": "metadata.json", "size": 4096}
  ],
  "total_size": 2283504096
}
```

**FILE_CHUNK** (Android → PC)
```json
{
  "command": "FILE_CHUNK",
  "filename": "rgb_video.mp4",
  "offset": 104857600,
  "chunk_size": 1048576,
  "data": "<base64_encoded_chunk>"
}
```

## Timing Characteristics

### Latency Requirements
- **Command Propagation**: < 50ms (PC to Android)
- **ACK Response**: < 100ms (Android to PC)
- **Heartbeat Interval**: 2 seconds
- **Sync Exchange**: < 10ms round-trip

### Timeout Values
- **Connection Establishment**: 30 seconds
- **Sensor Initialization**: 10 seconds per sensor
- **Command Acknowledgment**: 5 seconds
- **File Transfer Chunk**: 60 seconds
- **Heartbeat Timeout**: 6 seconds (3 missed beats)

### Bandwidth Usage
- **Control Messages**: ~1 KB/s average
- **Heartbeat Traffic**: 0.5 KB/s
- **File Transfer**: Up to 10 MB/s (limited by network)

## Error Handling

### Network Errors
- **Connection Lost During Recording**: Android continues local recording, queues status updates, attempts reconnection
- **Packet Loss**: TCP ensures reliable delivery, retransmission handled by protocol stack
- **Timeout**: Commands have retry logic with exponential backoff (500ms, 1s, 2s, 4s, 8s max)

### Sensor Errors
- **Initialization Failure**: Send ERROR message to PC with details, mark sensor unavailable
- **Mid-Recording Failure**: Log warning, continue with remaining sensors, notify PC via STATUS_UPDATE
- **Recovery**: Attempt auto-reconnection, send recovery status when restored

### File Transfer Errors
- **Partial Transfer**: Resume from last successful chunk using offset
- **Checksum Mismatch**: Request file retransmission
- **Storage Full on PC**: Abort transfer, send ERROR, require user intervention

This communication protocol ensures reliable, efficient coordination between the PC orchestrator and Android sensor nodes with comprehensive error handling and recovery mechanisms.
