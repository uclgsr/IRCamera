# Chapter 1: Example Use-Case Scenario Timeline

## Figure 1.2: Example Use-Case Scenario Timeline

A detailed timeline diagram showing a typical recording session workflow with all system interactions, state changes,
and data flow.

### Part A: Complete Interaction Sequence

```mermaid
sequenceDiagram
    participant Researcher as Researcher<br/>(Operator)
    participant UI as PC UI<br/>(Qt GUI)
    participant PC as PC Controller<br/>(Python Core)
    participant Network as Network<br/>(TCP/IP)
    participant Android as Android App<br/>(Sensor Node)
    participant RecCtrl as Recording<br/>Controller
    participant ThermalMgr as Thermal<br/>Manager
    participant GSRMgr as GSR<br/>Manager
    participant RGBMgr as RGB<br/>Manager
    participant Sensors as Hardware<br/>Sensors
    participant Data as Local<br/>Storage
    
    Note over Researcher,Data: Pre-Session Setup Phase
    Researcher->>UI: Launch Controller Application
    activate UI
    UI->>PC: Initialize Application
    activate PC
    PC->>PC: Load Configuration
    PC->>PC: Start TCP Server (port 8080)
    PC->>PC: Initialize Logger
    UI-->>Researcher: Ready - Awaiting Device
    
    Researcher->>Android: Launch Sensor App
    activate Android
    Android->>Android: Check Permissions (USB/BLE/Camera)
    Android->>RecCtrl: Initialize Recording Controller
    activate RecCtrl
    Android->>Network: Discover PC on Local Network
    activate Network
    Network->>PC: TCP Connect Request
    PC->>Network: Accept Connection
    Network->>Android: Connection Established
    
    Android->>PC: HELLO device_id=Samsung_S22 version=1.2.0
    PC->>PC: Register Device in Session Manager
    PC->>Android: HELLO_ACK capabilities_request=true
    Android->>RecCtrl: Query Sensor Availability
    RecCtrl->>ThermalMgr: Check TC001
    RecCtrl->>GSRMgr: Check Shimmer3
    RecCtrl->>RGBMgr: Check Camera
    RecCtrl-->>Android: Capabilities Ready
    Android->>PC: CAPABILITIES thermal=256x192@25Hz gsr=128Hz@16bit rgb=1080p@30fps
    PC->>UI: Device Connected (Samsung_S22)
    UI-->>Researcher: Device Ready - 3 Sensors Available
    
    Note over Researcher,Data: Time Synchronization Phase (NTP-style)
    PC->>PC: Query System Time (t0)
    PC->>Network: SYNC_REQUEST t1=1703441234567890
    Network->>Android: Forward SYNC_REQUEST
    Android->>Android: Capture t2=Android_recv_time
    Android->>Android: Capture t3=Android_send_time
    Android->>Network: SYNC_RESPONSE t1 t2 t3
    Network->>PC: Forward SYNC_RESPONSE
    PC->>PC: Capture t4=PC_recv_time
    PC->>PC: Calculate RTT=(t4-t1)-(t3-t2)
    PC->>PC: Calculate offset=(t2-t1+t3-t4)/2
    PC->>PC: offset=+2.3ms RTT=4.1ms
    PC->>Network: SYNC_RESULT offset=+2.3ms rtt=4.1ms
    Network->>Android: Forward SYNC_RESULT
    Android->>RecCtrl: Apply Time Offset
    RecCtrl->>RecCtrl: Update TimeManager with offset
    Android->>PC: SYNC_ACK status=synchronized
    PC->>UI: Time Sync Complete (offset=+2.3ms)
    UI-->>Researcher: Synchronized - Ready to Record
    
    Note over Researcher,Data: Session Initialization
    Researcher->>UI: Click "Start Recording"
    UI->>PC: Start Session Request
    PC->>PC: Generate session_id=20241215_1430
    PC->>PC: Create Session Directory
    PC->>Network: START_RECORD session_id=20241215_1430
    Network->>Android: Forward START_RECORD
    Android->>RecCtrl: Start Recording Session
    RecCtrl->>Data: Create Session Directory
    
    par Parallel Sensor Initialization
        RecCtrl->>ThermalMgr: Initialize TC001
        activate ThermalMgr
        ThermalMgr->>Sensors: Request USB Permission
        Sensors-->>ThermalMgr: Permission Granted
        ThermalMgr->>Sensors: Initialize TC001 SDK
        Sensors-->>ThermalMgr: Device Ready (50ms)
        ThermalMgr->>Data: Create thermal_frames.csv
        ThermalMgr->>Data: Write CSV Header
        ThermalMgr->>RecCtrl: Thermal Ready
    and
        RecCtrl->>GSRMgr: Initialize Shimmer3
        activate GSRMgr
        GSRMgr->>Sensors: BLE Scan for Shimmer3
        Sensors-->>GSRMgr: Device Found (MAC: XX:XX:XX)
        GSRMgr->>Sensors: BLE Connect
        Sensors-->>GSRMgr: Connected (80ms)
        GSRMgr->>Sensors: Send 0x07 (Start Streaming)
        Sensors-->>GSRMgr: ACK + Data Stream Active
        GSRMgr->>Data: Create gsr_samples.csv
        GSRMgr->>Data: Write CSV Header
        GSRMgr->>RecCtrl: GSR Ready
    and
        RecCtrl->>RGBMgr: Initialize Camera
        activate RGBMgr
        RGBMgr->>Sensors: Request Camera Permission
        Sensors-->>RGBMgr: Permission Granted
        RGBMgr->>Sensors: Initialize CameraX
        Sensors-->>RGBMgr: Camera Available (100ms)
        RGBMgr->>Sensors: Start Recording (H.264)
        Sensors-->>RGBMgr: Recording Started
        RGBMgr->>Data: Create video_recording.mp4
        RGBMgr->>RecCtrl: RGB Ready
    end
    
    RecCtrl->>Android: All Sensors Initialized
    Android->>Network: ACK status=recording sensors=3 timestamp=...
    Network->>PC: Forward ACK
    PC->>UI: Recording Started (3 sensors active)
    UI-->>Researcher: Recording... (00:00:00)
    
    Note over Researcher,Data: Active Recording Phase (Continuous Data Flow)
    
    loop Every Sample Period (Real-time Data Streaming)
        Sensors->>ThermalMgr: Frame Callback (40ms period, 25Hz)
        ThermalMgr->>ThermalMgr: Extract Temperature Matrix
        ThermalMgr->>ThermalMgr: Apply Timestamp
        ThermalMgr->>Data: Write CSV Row (timestamp, 256x192 temps)
        
        Sensors->>GSRMgr: Sample Callback (7.8ms period, 128Hz)
        GSRMgr->>GSRMgr: Parse ADC Value
        GSRMgr->>GSRMgr: Convert to Microsiemens
        GSRMgr->>GSRMgr: Apply Timestamp
        GSRMgr->>Data: Write CSV Row (timestamp, conductance)
        
        Sensors->>RGBMgr: Frame Callback (33ms period, 30fps)
        RGBMgr->>RGBMgr: Encode H.264 Frame
        RGBMgr->>RGBMgr: Apply Timestamp
        RGBMgr->>Data: Append to MP4 Stream
    end
    
    loop Every 2 seconds (Heartbeat)
        RecCtrl->>RecCtrl: Gather Statistics
        RecCtrl->>Android: Build Status Message
        Android->>Network: HEARTBEAT duration=120s thermal_frames=3000 gsr_samples=15360 rgb_frames=3600
        Network->>PC: Forward HEARTBEAT
        PC->>UI: Update Status (02:00 / 3GB)
        UI-->>Researcher: Recording... (00:02:00)
    end
    
    Note over Researcher,Data: Recording Continues (Example: 5 minute session)
    
    Note over Researcher,Data: Session Termination Phase
    Researcher->>UI: Click "Stop Recording"
    UI->>PC: Stop Session Request
    PC->>Network: STOP_RECORD session_id=20241215_1430
    Network->>Android: Forward STOP_RECORD
    Android->>RecCtrl: Stop Recording Session
    
    par Parallel Sensor Shutdown
        RecCtrl->>ThermalMgr: Stop Thermal Camera
        ThermalMgr->>Sensors: Stop TC001 Stream
        Sensors-->>ThermalMgr: Stream Stopped
        ThermalMgr->>ThermalMgr: Flush Write Buffer
        ThermalMgr->>Data: Close thermal_frames.csv
        ThermalMgr->>RecCtrl: Thermal Stopped (3000 frames)
        deactivate ThermalMgr
    and
        RecCtrl->>GSRMgr: Stop GSR Sensor
        GSRMgr->>Sensors: Send 0x20 (Stop Streaming)
        Sensors-->>GSRMgr: ACK Stream Stopped
        GSRMgr->>GSRMgr: Flush Write Buffer
        GSRMgr->>Data: Close gsr_samples.csv
        GSRMgr->>RecCtrl: GSR Stopped (15360 samples)
        deactivate GSRMgr
    and
        RecCtrl->>RGBMgr: Stop RGB Camera
        RGBMgr->>Sensors: Stop CameraX Recording
        Sensors-->>RGBMgr: Recording Stopped
        RGBMgr->>RGBMgr: Finalize H.264 Stream
        RGBMgr->>Data: Close video_recording.mp4
        RGBMgr->>RecCtrl: RGB Stopped (3600 frames)
        deactivate RGBMgr
    end
    
    RecCtrl->>Data: Generate metadata.json
    Data->>Data: Write Session Info (duration, sensors, files)
    RecCtrl->>Data: Generate timesync_log.csv
    RecCtrl->>Android: Session Complete
    deactivate RecCtrl
    Android->>Android: Calculate Total Size (1.8GB)
    Android->>Network: STOP_ACK files=5 size=1.8GB duration=300s
    Network->>PC: Forward STOP_ACK
    PC->>PC: Log Session Statistics
    PC->>UI: Recording Complete (5:00 / 1.8GB)
    UI-->>Researcher: Session Saved - Ready for Transfer
    deactivate Android
    deactivate Network
    deactivate PC
    deactivate UI
    
    Note over Researcher,Data: Post-Session Data Transfer
    Researcher->>Android: Connect USB Cable / Select WiFi Transfer
    Android->>Data: List Session Files
    Data-->>Android: thermal_frames.csv (30MB)<br/>gsr_samples.csv (0.5MB)<br/>video_recording.mp4 (1.7GB)<br/>metadata.json (2KB)<br/>timesync_log.csv (1KB)
    Android->>Researcher: Transfer Files via USB/Network
    Researcher->>Researcher: Import to Analysis Workstation
```

### Part B: System State Machine

```mermaid
stateDiagram-v2
    [*] --> Disconnected
    
    Disconnected --> Connecting : Launch App
    Connecting --> Connected : TCP Connection Established
    Connecting --> Disconnected : Connection Failed
    
    Connected --> Synchronizing : SYNC_REQUEST
    Synchronizing --> Synchronized : SYNC_ACK
    Synchronizing --> Connected : Sync Failed (retry)
    
    Synchronized --> Initializing : START_RECORD
    Initializing --> InitializingThermal : Parallel Init
    Initializing --> InitializingGSR : Parallel Init
    Initializing --> InitializingRGB : Parallel Init
    
    state Initializing {
        [*] --> InitializingThermal
        [*] --> InitializingGSR
        [*] --> InitializingRGB
        
        InitializingThermal --> ThermalReady : TC001 Init (50ms)
        InitializingGSR --> GSRReady : Shimmer Connect (80ms)
        InitializingRGB --> RGBReady : CameraX Init (100ms)
        
        ThermalReady --> AllReady
        GSRReady --> AllReady
        RGBReady --> AllReady
    }
    
    Initializing --> Recording : All Sensors Ready
    
    Recording --> Recording : Data Streaming
    Recording --> Recording : Heartbeat (2s)
    
    Recording --> Stopping : STOP_RECORD
    
    state Stopping {
        [*] --> StoppingThermal
        [*] --> StoppingGSR
        [*] --> StoppingRGB
        
        StoppingThermal --> ThermalStopped : Close TC001
        StoppingGSR --> GSRStopped : Send 0x20
        StoppingRGB --> RGBStopped : Stop CameraX
        
        ThermalStopped --> AllStopped
        GSRStopped --> AllStopped
        RGBStopped --> AllStopped
    }
    
    Stopping --> Finalizing : All Sensors Stopped
    Finalizing --> Synchronized : Write Metadata
    
    Synchronized --> Disconnected : Close Connection
    Disconnected --> [*]
    
    note right of Recording
        Continuous Data Flow:
        - Thermal: 25 fps
        - GSR: 128 Hz
        - RGB: 30 fps
    end note
    
    note right of Synchronizing
        NTP-Style Time Sync:
        - offset: +/- 5ms
        - RTT measured
    end note
```

### Part C: Data Flow Timing Diagram

```mermaid
gantt
    title Sensor Data Flow Timeline (First 200ms of Recording)
    dateFormat X
    axisFormat %L ms
    
    section Command Flow
    START_RECORD Sent       :milestone, cmd1, 0, 0ms
    Android Receives        :milestone, cmd2, 2, 2ms
    Sensor Init Begins      :milestone, cmd3, 5, 5ms
    
    section Thermal Camera (25Hz)
    TC001 Init              :active, th1, 5, 55ms
    First Frame             :milestone, thf1, 55, 55ms
    Frame 2                 :milestone, thf2, 95, 95ms
    Frame 3                 :milestone, thf3, 135, 135ms
    Frame 4                 :milestone, thf4, 175, 175ms
    
    section GSR Sensor (128Hz)
    Shimmer3 Connect        :active, gsr1, 5, 85ms
    Sample 1                :milestone, gsrs1, 85, 85ms
    Sample 2                :milestone, gsrs2, 93, 93ms
    Sample 3                :milestone, gsrs3, 101, 101ms
    Sample 4-25             :active, gsrs4, 101, 200ms
    
    section RGB Camera (30fps)
    CameraX Init            :active, rgb1, 5, 105ms
    First Frame             :milestone, rgbf1, 105, 105ms
    Frame 2                 :milestone, rgbf2, 138, 138ms
    Frame 3                 :milestone, rgbf3, 171, 171ms
    
    section Data Storage
    CSV Headers Written     :active, stor1, 55, 85ms
    Thermal Data Stream     :active, stor2, 55, 200ms
    GSR Data Stream         :active, stor3, 85, 200ms
    Video Stream            :active, stor4, 105, 200ms
    
    section Heartbeat
    First Heartbeat         :milestone, hb1, 200, 200ms
```

### Part D: Resource Utilization Flow

```mermaid
flowchart LR
    subgraph Input["Data Input Sources"]
        direction TB
        TC001["TC001 Thermal<br/>256×192 @ 25Hz<br/>~1.2 MB/s"]
        Shimmer["Shimmer3 GSR<br/>128 Hz @ 16-bit<br/>~256 B/s"]
        Camera["RGB Camera<br/>1080p @ 30fps<br/>~10 MB/s"]
    end
    
    subgraph Processing["Android Processing"]
        direction TB
        ThermalProc["Thermal Processing<br/>Temperature Extraction<br/>CSV Serialization<br/>CPU: 5-8%"]
        GSRProc["GSR Processing<br/>ADC Conversion<br/>CSV Serialization<br/>CPU: 1-2%"]
        VideoProc["Video Processing<br/>H.264 Encoding<br/>Hardware Accelerated<br/>CPU: 3-5%"]
        TimestampProc["Timestamp Sync<br/>Nanosecond Precision<br/>CPU: <1%"]
    end
    
    subgraph Storage["Data Storage"]
        direction TB
        ThermalCSV["thermal_frames.csv<br/>~6 MB/min<br/>SSD Write"]
        GSRCSV["gsr_samples.csv<br/>~15 KB/min<br/>SSD Write"]
        VideoMP4["video.mp4<br/>~600 MB/min<br/>SSD Write"]
        Metadata["metadata.json<br/>~2 KB<br/>SSD Write"]
    end
    
    subgraph Network["Network Transfer"]
        direction TB
        Heartbeat["Heartbeat<br/>~100 B / 2s<br/>TCP"]
        Commands["Commands<br/>~200 B<br/>TCP"]
    end
    
    TC001 -->|Frame Callback| ThermalProc
    Shimmer -->|BLE GATT| GSRProc
    Camera -->|CameraX API| VideoProc
    
    ThermalProc -->|Timestamped| TimestampProc
    GSRProc -->|Timestamped| TimestampProc
    VideoProc -->|Timestamped| TimestampProc
    
    TimestampProc -->|Write| ThermalCSV
    TimestampProc -->|Write| GSRCSV
    TimestampProc -->|Write| VideoMP4
    TimestampProc -->|Write| Metadata
    
    ThermalCSV -.->|Status| Heartbeat
    GSRCSV -.->|Status| Heartbeat
    VideoMP4 -.->|Status| Heartbeat
    
    Heartbeat -->|TCP Port 8080| Commands
    
    style TC001 fill:#ffccbc
    style Shimmer fill:#c5e1a5
    style Camera fill:#b3e5fc
    style ThermalProc fill:#ffe0b2
    style GSRProc fill:#dcedc8
    style VideoProc fill:#b2ebf2
    style TimestampProc fill:#f8bbd0
    style ThermalCSV fill:#fff9c4
    style GSRCSV fill:#fff9c4
    style VideoMP4 fill:#fff9c4
    style Metadata fill:#fff9c4
```

## Use-Case Context

This enhanced timeline demonstrates:

- **Simple Operation**: Researcher clicks "Start" on PC, all sensors begin recording automatically
- **Automatic Synchronization**: Clock alignment happens transparently before recording
- **Parallel Initialization**: All sensors start simultaneously within ~100ms window
- **Continuous Recording**: Data flows to local storage throughout session
- **Clean Shutdown**: Graceful termination ensures all data saved properly
- **Data Accessibility**: Files ready for analysis immediately after session

## Typical Session Characteristics

- **Setup Time**: ~10-15 seconds (connection + sync)
- **Recording Duration**: 1-30 minutes typical
- **Data Volume**: ~360MB per 5-minute session
- **Synchronization Accuracy**: <5ms timestamp alignment
- **Sensors Active**: 3 modalities recording simultaneously
- **Heartbeat Interval**: 2 seconds for status monitoring

## Motivations Addressed

This simple workflow maps the problem context to the solution:

1. **Researcher Need**: Simple interface for multi-modal data collection
2. **Technical Challenge**: Synchronizing diverse sensor streams
3. **Solution**: Automated coordination via PC controller
4. **Outcome**: Time-aligned multi-modal dataset for analysis








