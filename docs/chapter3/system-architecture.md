# System Architecture Diagram

## Figure 3.1: System Architecture - Multi-Modal Physiological Monitoring Platform

This diagram shows the complete system architecture including the Android device with internal modules for GSR, thermal, and RGB data acquisition, the PC orchestrator, and communication links.

```mermaid
graph TB
    subgraph "PC Orchestrator (Python)"
        PC_Controller[PC Controller Application<br/>Session Management<br/>Device Coordination]
        
        subgraph "Core Coordination Layer"
            SessionMgr[Session Manager<br/>Lifecycle Control<br/>Metadata Management]
            DeviceMgr[Device Manager<br/>Connection Tracking<br/>Multi-device Orchestration]
            TimeSyncSvc[Time Sync Service<br/>NTP-like Protocol<br/>Clock Synchronization]
        end
        
        subgraph "Network & Data Layer"
            NetServer[Network Server<br/>TCP Server (Port 8080)<br/>Command Dispatch]
            DataAggregator[Data Aggregator<br/>Multi-modal Fusion<br/>Session Storage]
            FileReceiver[File Transfer Manager<br/>Bulk Data Reception<br/>Integrity Validation]
        end
        
        subgraph "Sensor Integration (PC-side)"
            ShimmerMgr[Shimmer Manager<br/>Direct PC Connection<br/>BLE Communication]
            DataLogger[Data Logger<br/>Real-time CSV Writer<br/>128 Hz Logging]
        end
        
        subgraph "User Interface"
            GUI[PyQt6 GUI<br/>Control Panel<br/>Real-time Monitoring<br/>Status Display]
        end
    end
    
    subgraph "Android Sensor Node (Kotlin/Java)"
        AndroidApp[Android Application<br/>Multi-modal Capture<br/>Local Storage]
        
        subgraph "Application Control Layer"
            MainActivity[MainActivity<br/>UI Controller<br/>Session Management UI]
            RecordingService[Recording Service<br/>Background Operation<br/>Lifecycle Management]
            RecordingController[Recording Controller<br/>Sensor Coordination<br/>Command Execution]
        end
        
        subgraph "Network Communication Layer"
            NetClient[Network Client<br/>TCP Client<br/>Command Handler]
            ProtocolHandler[Protocol Handler<br/>JSON Parser<br/>Message Dispatcher]
            SyncClient[Sync Manager<br/>Clock Calibration<br/>Drift Correction]
        end
        
        subgraph "Sensor Driver Layer"
            ThermalDriver[Thermal Camera Driver<br/>TC001 SDK Integration<br/>USB/OTG Interface<br/>LibIRParse]
            GSRDriver[GSR Sensor Driver<br/>Shimmer3 BLE<br/>ShimmerAndroidAPI<br/>12-bit ADC]
            CameraDriver[RGB Camera Driver<br/>CameraX API<br/>Camera2 Backend<br/>H.264 Encoder]
        end
        
        subgraph "Data Processing Layer"
            ThermalProc[Temperature Processor<br/>Calibration (+/-2C)<br/>256x192@25Hz]
            GSRProc[GSR Processor<br/>Microsiemens Conversion<br/>128 Hz Sampling]
            VideoProc[Video Processor<br/>1920x1080@30fps<br/>MediaRecorder]
        end
        
        subgraph "Storage & Sync Layer"
            TimeManager[Time Manager<br/>Nanosecond Timestamps<br/>getCurrentTimestampNanos()]
            StorageMgr[Storage Manager<br/>Session Directory<br/>Local File System]
            DataWriter[Data Writer<br/>CSV Logger<br/>Video File Handler]
        end
        
        subgraph "Hardware Access Layer"
            USBManager[USB Manager<br/>OTG Detection<br/>VID/PID: 0x0525/0xa4a2]
            BLEManager[BLE Manager<br/>Device Discovery<br/>GATT Communication]
            PermissionMgr[Permission Manager<br/>Runtime Permissions<br/>Hardware Access]
        end
    end
    
    subgraph "Hardware Sensors"
        TC001[Topdon TC001<br/>Thermal IR Camera<br/>256x192 Resolution<br/>25 FPS<br/>USB-C/OTG]
        Shimmer3[Shimmer3 GSR+<br/>Galvanic Skin Response<br/>12-bit ADC<br/>51.2-512 Hz<br/>BLE 4.0]
        PhoneCamera[Phone RGB Camera<br/>1920x1080 or Higher<br/>30 FPS<br/>Camera2 API]
    end
    
    %% PC Internal Connections
    PC_Controller --> SessionMgr
    PC_Controller --> GUI
    SessionMgr --> DeviceMgr
    SessionMgr --> DataAggregator
    DeviceMgr --> NetServer
    DeviceMgr --> TimeSyncSvc
    NetServer --> FileReceiver
    ShimmerMgr --> DataLogger
    DataLogger --> DataAggregator
    
    %% PC to Android Communication (TCP/IP)
    NetServer -.->|"TCP Commands<br/>(START/STOP/SYNC)<br/>Port 8080"| NetClient
    NetClient -.->|"ACK/Status<br/>JSON Messages"| NetServer
    TimeSyncSvc -.->|"SYNC_REQUEST<br/>Timestamp Exchange"| SyncClient
    SyncClient -.->|"SYNC_RESPONSE<br/>Clock Offset"| TimeSyncSvc
    FileReceiver -.->|"File Transfer<br/>Bulk Data"| StorageMgr
    
    %% Android Internal Connections
    MainActivity --> RecordingService
    RecordingService --> RecordingController
    NetClient --> ProtocolHandler
    ProtocolHandler --> RecordingController
    RecordingController --> ThermalDriver
    RecordingController --> GSRDriver
    RecordingController --> CameraDriver
    
    %% Sensor to Processor Flow
    ThermalDriver --> ThermalProc
    GSRDriver --> GSRProc
    CameraDriver --> VideoProc
    
    %% Processing to Storage Flow
    ThermalProc --> TimeManager
    GSRProc --> TimeManager
    VideoProc --> TimeManager
    TimeManager --> DataWriter
    DataWriter --> StorageMgr
    SyncClient --> TimeManager
    
    %% Hardware Management
    ThermalDriver --> USBManager
    GSRDriver --> BLEManager
    CameraDriver --> PermissionMgr
    
    %% Hardware to Driver Connections
    TC001 -->|"USB/OTG<br/>UVC Protocol<br/>Raw Thermal Data"| ThermalDriver
    Shimmer3 -->|"Bluetooth LE<br/>GATT<br/>12-bit ADC Samples"| GSRDriver
    PhoneCamera -->|"Camera2 API<br/>YUV/JPEG Frames"| CameraDriver
    
    %% PC Direct Sensor Connection (Optional)
    Shimmer3 -.->|"Direct BLE<br/>(Alternative)"| ShimmerMgr
    
    %% Styling
    classDef pcClass fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef androidClass fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef hardwareClass fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef networkClass fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    classDef storageClass fill:#fce4ec,stroke:#880e4f,stroke-width:2px
    
    class PC_Controller,SessionMgr,DeviceMgr,TimeSyncSvc,NetServer,DataAggregator,FileReceiver,ShimmerMgr,DataLogger,GUI pcClass
    class AndroidApp,MainActivity,RecordingService,RecordingController,ThermalProc,GSRProc,VideoProc,TimeManager,DataWriter androidClass
    class NetClient,ProtocolHandler,SyncClient networkClass
    class ThermalDriver,GSRDriver,CameraDriver,USBManager,BLEManager,PermissionMgr,StorageMgr storageClass
    class TC001,Shimmer3,PhoneCamera hardwareClass
```

## Architecture Description

### PC Orchestrator Component

The PC acts as the central coordination hub with the following responsibilities:

1. **Session Management**: Creates and manages recording sessions with unique IDs, maintains metadata, and controls session lifecycle
2. **Device Coordination**: Tracks multiple Android devices, manages connections, monitors health status
3. **Time Synchronization**: Provides NTP-like time service to align clocks across all devices within 5ms accuracy
4. **Network Server**: Runs TCP server on port 8080, handles JSON-based command protocol
5. **Data Aggregation**: Collects and consolidates multi-modal data from all sources
6. **User Interface**: PyQt6-based GUI for real-time monitoring and control

### Android Sensor Node Component

Each Android device operates as an autonomous sensor node:

1. **Application Control**: Manages UI, background services, and recording lifecycle
2. **Network Communication**: Maintains TCP connection to PC, handles commands, sends status updates
3. **Sensor Drivers**: Integrates with hardware via USB/OTG (thermal), BLE (GSR), and Camera2 API (RGB)
4. **Data Processing**: Real-time temperature calibration, GSR conversion, video encoding
5. **Time Synchronization**: Maintains synchronized clock, applies drift correction
6. **Local Storage**: Writes CSV files and video to session directory with nanosecond-precision timestamps

### Communication Links

1. **TCP/IP (PC to Android)**: Port 8080, JSON protocol, commands (START_RECORD, STOP_RECORD, SYNC_REQUEST), acknowledgments, status updates
2. **Bluetooth LE (Shimmer to Android/PC)**: GATT protocol, 12-bit ADC data at 128 Hz
3. **USB/OTG (Topdon to Android)**: UVC protocol via TC001 SDK, 256x192 thermal frames at 25 Hz
4. **Camera2 API (Internal)**: YUV frames from phone camera, H.264 encoding at 30 fps

### Data Flow

1. **Recording Start**: PC broadcasts START_RECORD → Android devices initialize sensors → Each sensor begins streaming
2. **Continuous Capture**: Thermal (25 Hz), GSR (128 Hz), RGB (30 fps) data timestamped locally
3. **Local Storage**: Android writes CSV (thermal, GSR) and MP4 (RGB) to session directory
4. **PC Direct Capture**: If Shimmer connected to PC, data logged directly to CSV
5. **Recording Stop**: PC sends STOP_RECORD → Sensors gracefully shutdown → Files finalized
6. **Data Transfer**: Android devices transfer files to PC via TCP → PC stores in centralized session folder

### Key Design Principles

1. **Modular Architecture**: Independent components with clear interfaces, enabling sensor additions/modifications
2. **Distributed Recording**: Android devices record locally to avoid network bottlenecks
3. **Centralized Coordination**: PC manages session state, timing, and aggregation
4. **Temporal Precision**: Nanosecond timestamps with sub-5ms synchronization accuracy
5. **Fault Tolerance**: Devices continue recording if network drops, auto-reconnect and sync when restored
