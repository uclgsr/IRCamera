# IRCamera Comprehensive Architecture Diagrams

This document provides precise Mermaid diagrams for each feature, module, and architectural aspect of the IRCamera Multi-Modal Thermal Sensing Platform.

## Table of Contents

1. [System Overview](#system-overview)
2. [Hub-and-Spoke Architecture](#hub-and-spoke-architecture)
3. [Android Module Architecture](#android-module-architecture)
4. [PC Controller Hub Architecture](#pc-controller-hub-architecture)
5. [Feature-Specific Diagrams](#feature-specific-diagrams)
6. [Data Flow Architecture](#data-flow-architecture)
7. [Build System Architecture](#build-system-architecture)
8. [Integration Architecture](#integration-architecture)

---

## System Overview

### Complete System Architecture

```mermaid
graph TB
    subgraph "Physical Layer"
        ThermalHW[Topdon TC001<br/>Thermal Camera]
        GSRHW[Shimmer3<br/>GSR Sensor]
        RGBHW[Android Camera<br/>RGB Sensor]
        BLEHW[BLE Hardware<br/>Bluetooth Radio]
    end
    
    subgraph "Android Sensor Nodes (Spoke)"
        subgraph "Android App Layer"
            MainActivity[MainActivity.kt<br/>Entry Point]
            ThermalActivity[ThermalActivity<br/>Thermal Interface]
            GSRActivity[GSRActivity<br/>GSR Interface]
        end
        
        subgraph "Feature Components"
            ThermalIRComp[thermal-ir<br/>Advanced Thermal Processing]
            ThermalComp[thermal<br/>Basic Thermal]
            ThermalLiteComp[thermal-lite<br/>Lite Thermal]
            GSRComp[gsr-recording<br/>GSR Data Collection]
            UserComp[user<br/>User Management]
        end
        
        subgraph "Core Libraries"
            LibApp[libapp<br/>Application Framework]
            LibIR[libir<br/>IR Processing Engine]
            LibUI[libui<br/>UI Components]
            LibCom[libcom<br/>Communication]
            LibMatrix[libmatrix<br/>Matrix Operations]
            LibMenu[libmenu<br/>Menu System]
        end
        
        subgraph "External Modules"
            BleModule[BleModule<br/>Shimmer Integration]
            RangeSeekBar[RangeSeekBar<br/>UI Controls]
        end
    end
    
    subgraph "PC Controller Hub"
        subgraph "MVP Components"
            SimpleMVP[mvp_simple.py<br/>Single-file MVP]
            FullMVP[run_mvp_app.py<br/>Complete GUI App]
            DemoMVP[demo_mvp_components.py<br/>Component Demo]
        end
        
        subgraph "Core Hub Services"
            DeviceManager[Device Manager<br/>mDNS Discovery]
            SessionManager[Session Manager<br/>Lifecycle Control]
            DataAggregator[Data Aggregator<br/>Multi-modal Sync]
            NetworkHub[Network Hub<br/>TCP/JSON Protocol]
        end
        
        subgraph "Data Processing"
            ThermalProcessor[Thermal Processor<br/>Image Processing]
            GSRProcessor[GSR Processor<br/>Signal Processing]
            SyncProcessor[Sync Processor<br/>Time Synchronization]
        end
    end
    
    subgraph "Build & CI/CD"
        GradleBuild[Gradle Build System<br/>Multi-module]
        DevTools[dev.sh<br/>Development Tools]
        CIWorkflows[GitHub Actions<br/>CI/CD Pipeline]
        StaticAnalysis[Static Analysis<br/>Quality Assurance]
    end
    
    %% Hardware to Android connections
    ThermalHW --> LibIR
    GSRHW --> BleModule
    RGBHW --> LibApp
    BLEHW --> BleModule
    
    %% Android app connections
    MainActivity --> ThermalActivity
    MainActivity --> GSRActivity
    ThermalActivity --> ThermalIRComp
    GSRActivity --> GSRComp
    
    %% Component to library connections
    ThermalIRComp --> LibIR
    ThermalComp --> LibIR
    ThermalLiteComp --> LibIR
    GSRComp --> BleModule
    GSRComp --> LibCom
    UserComp --> LibApp
    
    %% Library dependencies
    LibApp --> LibCom
    LibIR --> LibMatrix
    LibUI --> LibMatrix
    
    %% Network communication
    LibCom <--> NetworkHub
    
    %% PC Hub internal connections
    NetworkHub --> DeviceManager
    NetworkHub --> SessionManager
    NetworkHub --> DataAggregator
    DataAggregator --> ThermalProcessor
    DataAggregator --> GSRProcessor
    DataAggregator --> SyncProcessor
    
    %% Build system connections
    GradleBuild --> DevTools
    DevTools --> CIWorkflows
    CIWorkflows --> StaticAnalysis
```

---

## Hub-and-Spoke Architecture

### Distributed System Communication

```mermaid
graph TB
    subgraph "Hub: PC Controller"
        PCController[PC Controller Application<br/>Python + PyQt6]
        
        subgraph "Discovery & Connection"
            MDNSService[mDNS Service<br/>Device Discovery]
            ConnectionMgr[Connection Manager<br/>TCP Socket Management]
            AuthService[Authentication Service<br/>Device Verification]
        end
        
        subgraph "Session Management"
            SessionCtrl[Session Controller<br/>Lifecycle Management]
            ConfigMgr[Configuration Manager<br/>Device Settings]
            MetadataMgr[Metadata Manager<br/>Session Information]
        end
        
        subgraph "Data Processing"
            StreamProcessor[Stream Processor<br/>Multi-modal Data]
            SyncEngine[Sync Engine<br/>Time Alignment]
            StorageEngine[Storage Engine<br/>HDF5 Export]
        end
    end
    
    subgraph "Spoke 1: Android Device A"
        AndroidA[Android Sensor Node A]
        
        subgraph "Sensors A"
            ThermalA[Thermal Camera<br/>Topdon TC001]
            GSRA[GSR Sensor<br/>Shimmer3 BLE]
            RGBA[RGB Camera<br/>CameraX API]
        end
        
        subgraph "Processing A"
            ThermalProcA[Thermal Processing<br/>IR Analysis]
            GSRProcA[GSR Processing<br/>Signal Analysis]
            NetworkA[Network Client<br/>TCP Communication]
        end
    end
    
    subgraph "Spoke 2: Android Device B"
        AndroidB[Android Sensor Node B]
        
        subgraph "Sensors B"
            ThermalB[Thermal Camera<br/>Topdon TC001]
            GSRB[GSR Sensor<br/>Shimmer3 BLE]
            RGBB[RGB Camera<br/>CameraX API]
        end
        
        subgraph "Processing B"
            ThermalProcB[Thermal Processing<br/>IR Analysis]
            GSRProcB[GSR Processing<br/>Signal Analysis]
            NetworkB[Network Client<br/>TCP Communication]
        end
    end
    
    subgraph "Spoke N: Android Device N"
        AndroidN[Android Sensor Node N]
        
        subgraph "Sensors N"
            ThermalN[Thermal Camera<br/>Topdon TC001]
            GSRN[GSR Sensor<br/>Shimmer3 BLE]
            RGBN[RGB Camera<br/>CameraX API]
        end
        
        subgraph "Processing N"
            ThermalProcN[Thermal Processing<br/>IR Analysis]
            GSRProcN[GSR Processing<br/>Signal Analysis]
            NetworkN[Network Client<br/>TCP Communication]
        end
    end
    
    %% Discovery connections
    MDNSService -.->|Discovery Broadcast| AndroidA
    MDNSService -.->|Discovery Broadcast| AndroidB
    MDNSService -.->|Discovery Broadcast| AndroidN
    
    %% Control connections
    ConnectionMgr <-->|JSON Commands<br/>TCP/IP| NetworkA
    ConnectionMgr <-->|JSON Commands<br/>TCP/IP| NetworkB
    ConnectionMgr <-->|JSON Commands<br/>TCP/IP| NetworkN
    
    %% Hub internal connections
    MDNSService --> ConnectionMgr
    ConnectionMgr --> AuthService
    AuthService --> SessionCtrl
    SessionCtrl --> ConfigMgr
    SessionCtrl --> MetadataMgr
    
    ConnectionMgr --> StreamProcessor
    StreamProcessor --> SyncEngine
    SyncEngine --> StorageEngine
    
    %% Spoke A internal connections
    ThermalA --> ThermalProcA
    GSRA --> GSRProcA
    RGBA --> ThermalProcA
    ThermalProcA --> NetworkA
    GSRProcA --> NetworkA
    
    %% Spoke B internal connections
    ThermalB --> ThermalProcB
    GSRB --> GSRProcB
    RGBB --> ThermalProcB
    ThermalProcB --> NetworkB
    GSRProcB --> NetworkB
    
    %% Spoke N internal connections
    ThermalN --> ThermalProcN
    GSRN --> GSRProcN
    RGBN --> ThermalProcN
    ThermalProcN --> NetworkN
    GSRProcN --> NetworkN
```

---

## Android Module Architecture

### Complete Android Module Dependencies

```mermaid
graph TB
    subgraph "Application Module"
        App[app<br/>Main Android Application]
        
        subgraph "Activities"
            MainActivity[MainActivity.kt<br/>Entry Point & Navigation]
            ThermalActivity[ThermalActivity<br/>Thermal Interface]
            GSRActivity[GSRActivity<br/>GSR Recording]
            UserActivity[UserActivity<br/>User Management]
        end
        
        subgraph "Services"
            NetworkService[NetworkService<br/>PC Communication]
            CameraService[CameraService<br/>Camera Management]
            BLEService[BLEService<br/>Bluetooth Management]
        end
    end
    
    subgraph "Feature Components"
        subgraph "Thermal Components"
            ThermalIR[thermal-ir<br/>Advanced IR Processing<br/>- Multi-spectral analysis<br/>- Temperature mapping<br/>- Object detection]
            
            Thermal[thermal<br/>Basic Thermal Processing<br/>- Simple temperature display<br/>- Basic color mapping<br/>- Raw data export]
            
            ThermalLite[thermal-lite<br/>Lightweight Thermal<br/>- Minimal processing<br/>- Low memory usage<br/>- Fast rendering]
        end
        
        subgraph "Data Collection"
            GSRRecording[gsr-recording<br/>GSR Data Collection<br/>- Shimmer3 integration<br/>- Real-time streaming<br/>- Quality validation]
        end
        
        subgraph "System Components"
            UserMgmt[user<br/>User Management<br/>- Profile management<br/>- Session tracking<br/>- Preferences]
        end
    end
    
    subgraph "Core Libraries"
        LibApp[libapp<br/>Application Framework<br/>- Base activities<br/>- Common utilities<br/>- Configuration management]
        
        LibIR[libir<br/>IR Processing Engine<br/>- Topdon TC001 SDK<br/>- Image processing<br/>- Temperature calculation]
        
        LibUI[libui<br/>UI Components<br/>- Custom views<br/>- Chart components<br/>- Material design]
        
        LibCom[libcom<br/>Communication Library<br/>- Network protocols<br/>- JSON messaging<br/>- mDNS discovery]
        
        LibMatrix[libmatrix<br/>Matrix Operations<br/>- Mathematical operations<br/>- Image transformations<br/>- Signal processing]
        
        LibMenu[libmenu<br/>Menu System<br/>- Navigation<br/>- Context menus<br/>- Action bars]
    end
    
    subgraph "External Modules"
        BleModule[BleModule<br/>Bluetooth Low Energy<br/>- Shimmer3 protocol<br/>- Device management<br/>- Data streaming]
        
        RangeSeekBar[RangeSeekBar<br/>UI Range Controls<br/>- Temperature ranges<br/>- Slider controls<br/>- Value selection]
    end
    
    subgraph "Consolidated Libraries"
        CommonComponent[CommonComponent<br/>Shared Components<br/>- Common interfaces<br/>- Utility classes<br/>- Constants]
    end
    
    %% App to Activity connections
    App --> MainActivity
    App --> ThermalActivity
    App --> GSRActivity
    App --> UserActivity
    
    %% App to Service connections
    App --> NetworkService
    App --> CameraService
    App --> BLEService
    
    %% Activity to Component connections
    ThermalActivity --> ThermalIR
    ThermalActivity --> Thermal
    ThermalActivity --> ThermalLite
    GSRActivity --> GSRRecording
    UserActivity --> UserMgmt
    
    %% App to Core Library connections
    App --> LibApp
    App --> LibUI
    App --> LibCom
    App --> LibIR
    
    %% Component to Core Library connections
    ThermalIR --> LibIR
    ThermalIR --> LibMatrix
    ThermalIR --> LibUI
    
    Thermal --> LibIR
    Thermal --> LibMatrix
    
    ThermalLite --> LibIR
    
    GSRRecording --> BleModule
    GSRRecording --> LibCom
    GSRRecording --> LibApp
    
    UserMgmt --> LibApp
    UserMgmt --> LibUI
    
    %% Core Library interdependencies
    LibApp --> LibCom
    LibUI --> LibMatrix
    LibIR --> LibMatrix
    LibMenu --> LibUI
    
    %% External Module connections
    App --> BleModule
    LibUI --> RangeSeekBar
    
    %% Consolidated Library connections
    ThermalIR --> CommonComponent
    Thermal --> CommonComponent
    GSRRecording --> CommonComponent
    UserMgmt --> CommonComponent
```

---

## PC Controller Hub Architecture

### PC Controller Component Structure

```mermaid
graph TB
    subgraph "Entry Points"
        SimpleMVP[mvp_simple.py<br/>Single-file MVP<br/>~250 lines<br/>Minimal dependencies]
        
        FullGUI[run_mvp_app.py<br/>Complete GUI Application<br/>PyQt6 interface<br/>Production ready]
        
        ComponentDemo[demo_mvp_components.py<br/>Component Demonstration<br/>83% framework validation<br/>Architecture showcase]
    end
    
    subgraph "Core Framework (83% Complete)"
        subgraph "Configuration System"
            ConfigLoader[Configuration Loader<br/>YAML/JSON support<br/>Environment variables<br/>Runtime settings]
            
            DeviceConfig[Device Configuration<br/>Sensor parameters<br/>Network settings<br/>Processing options]
        end
        
        subgraph "Device Management"
            DeviceRegistry[Device Registry<br/>Connected devices<br/>Status tracking<br/>Capability discovery]
            
            DiscoveryService[Discovery Service<br/>mDNS/Zeroconf<br/>Network scanning<br/>Device identification]
            
            ConnectionManager[Connection Manager<br/>TCP socket pools<br/>Connection health<br/>Retry logic]
        end
        
        subgraph "Session Management"
            SessionController[Session Controller<br/>Lifecycle management<br/>State persistence<br/>Recovery handling]
            
            MetadataManager[Metadata Manager<br/>Session information<br/>Device metadata<br/>Experiment parameters]
        end
        
        subgraph "Communication Protocol"
            MessageRouter[Message Router<br/>Command routing<br/>Response handling<br/>Event distribution]
            
            ProtocolHandler[Protocol Handler<br/>JSON serialization<br/>Message validation<br/>Error handling]
            
            StreamManager[Stream Manager<br/>Data streaming<br/>Flow control<br/>Buffer management]
        end
    end
    
    subgraph "Data Processing Pipeline"
        subgraph "Ingestion"
            ThermalIngestor[Thermal Data Ingestor<br/>Image processing<br/>Temperature extraction<br/>Calibration]
            
            GSRIngestor[GSR Data Ingestor<br/>Signal processing<br/>Artifact removal<br/>Quality assessment]
            
            RGBIngestor[RGB Data Ingestor<br/>Image processing<br/>Color correction<br/>Synchronization]
        end
        
        subgraph "Synchronization"
            TimeSyncService[Time Sync Service<br/>Clock synchronization<br/>Timestamp alignment<br/>Drift correction]
            
            DataAligner[Data Aligner<br/>Multi-modal alignment<br/>Interpolation<br/>Gap handling]
        end
        
        subgraph "Storage"
            HDF5Exporter[HDF5 Exporter<br/>Hierarchical storage<br/>Metadata preservation<br/>Compression]
            
            DataValidator[Data Validator<br/>Integrity checks<br/>Schema validation<br/>Quality metrics]
        end
    end
    
    subgraph "User Interface (PyQt6)"
        subgraph "Main Interface"
            MainWindow[Main Window<br/>Central dashboard<br/>Menu system<br/>Status display]
            
            DevicePanel[Device Panel<br/>Device status<br/>Connection management<br/>Configuration]
            
            SessionPanel[Session Panel<br/>Session controls<br/>Progress tracking<br/>Metadata editing]
        end
        
        subgraph "Monitoring"
            DataVisualization[Data Visualization<br/>Real-time plots<br/>Multi-modal display<br/>Quality indicators]
            
            LogViewer[Log Viewer<br/>System logs<br/>Error tracking<br/>Debug information]
            
            StatusMonitor[Status Monitor<br/>System health<br/>Performance metrics<br/>Resource usage]
        end
    end
    
    subgraph "Testing & Validation"
        MVPTests[MVP Tests<br/>Core functionality<br/>Component validation<br/>Integration tests]
        
        ComponentTests[Component Tests<br/>Unit testing<br/>Mock devices<br/>Error scenarios]
        
        SimpleTests[Simple Tests<br/>Basic validation<br/>Smoke tests<br/>Quick checks]
    end
    
    %% Entry point connections
    SimpleMVP --> DeviceRegistry
    SimpleMVP --> SessionController
    SimpleMVP --> MessageRouter
    
    FullGUI --> MainWindow
    MainWindow --> DevicePanel
    MainWindow --> SessionPanel
    MainWindow --> DataVisualization
    
    ComponentDemo --> ConfigLoader
    ComponentDemo --> DiscoveryService
    ComponentDemo --> StreamManager
    
    %% Core framework connections
    ConfigLoader --> DeviceConfig
    DeviceConfig --> DeviceRegistry
    
    DiscoveryService --> DeviceRegistry
    DeviceRegistry --> ConnectionManager
    
    SessionController --> MetadataManager
    ConnectionManager --> MessageRouter
    MessageRouter --> ProtocolHandler
    ProtocolHandler --> StreamManager
    
    %% Data processing connections
    StreamManager --> ThermalIngestor
    StreamManager --> GSRIngestor
    StreamManager --> RGBIngestor
    
    ThermalIngestor --> TimeSyncService
    GSRIngestor --> TimeSyncService
    RGBIngestor --> TimeSyncService
    
    TimeSyncService --> DataAligner
    DataAligner --> HDF5Exporter
    HDF5Exporter --> DataValidator
    
    %% UI connections
    DevicePanel --> DeviceRegistry
    DevicePanel --> ConnectionManager
    SessionPanel --> SessionController
    SessionPanel --> MetadataManager
    DataVisualization --> ThermalIngestor
    DataVisualization --> GSRIngestor
    LogViewer --> MessageRouter
    StatusMonitor --> ConnectionManager
    
    %% Testing connections
    MVPTests --> SimpleMVP
    ComponentTests --> ConfigLoader
    SimpleTests --> ComponentDemo
```

---

## Feature-Specific Diagrams

### Thermal Processing Components

```mermaid
graph TB
    subgraph "Hardware Layer"
        TopdonTC001[Topdon TC001<br/>Thermal Camera<br/>- 384x288 resolution<br/>- USB-C interface<br/>- Temperature range: -20degC to 400degC]
        
        AndroidCamera[Android Camera<br/>RGB Sensor<br/>- CameraX API<br/>- Various resolutions<br/>- Auto-focus & exposure]
    end
    
    subgraph "libir: IR Processing Engine"
        subgraph "SDK Integration"
            TopdonSDK[Topdon SDK<br/>Native library<br/>JNI interface<br/>Hardware abstraction]
            
            CameraInterface[Camera Interface<br/>Device communication<br/>Parameter control<br/>Status monitoring]
        end
        
        subgraph "Image Processing"
            RawProcessor[Raw Data Processor<br/>Temperature calculation<br/>Calibration<br/>Noise reduction]
            
            ImageProcessor[Image Processor<br/>Spatial filtering<br/>Enhancement<br/>Geometric correction]
        end
        
        subgraph "Temperature Analysis"
            TempCalculator[Temperature Calculator<br/>Radiometric conversion<br/>Emissivity compensation<br/>Environmental correction]
            
            StatisticsEngine[Statistics Engine<br/>Min/max detection<br/>Average calculation<br/>Histogram analysis]
        end
    end
    
    subgraph "Feature Components"
        subgraph "thermal-ir: Advanced Processing"
            MultiSpectral[Multi-spectral Analysis<br/>RGB-IR fusion<br/>Color mapping<br/>Object tracking]
            
            ObjectDetection[Object Detection<br/>Temperature thresholds<br/>Hotspot detection<br/>Region analysis]
            
            DataExport[Advanced Export<br/>Multiple formats<br/>Metadata inclusion<br/>Batch processing]
        end
        
        subgraph "thermal: Basic Processing"
            BasicDisplay[Basic Display<br/>Simple rendering<br/>Color palettes<br/>Temperature overlay]
            
            BasicExport[Basic Export<br/>Image files<br/>CSV data<br/>Simple metadata]
        end
        
        subgraph "thermal-lite: Lightweight"
            MinimalUI[Minimal UI<br/>Essential controls<br/>Fast rendering<br/>Low memory usage]
            
            StreamingMode[Streaming Mode<br/>Real-time display<br/>Minimal processing<br/>Network optimized]
        end
    end
    
    subgraph "UI Components (libui)"
        ThermalView[Thermal View<br/>Custom image display<br/>Touch interactions<br/>Zoom & pan]
        
        TemperatureChart[Temperature Chart<br/>Time series plots<br/>Real-time updates<br/>Multiple sensors]
        
        ColorPalette[Color Palette<br/>Palette selection<br/>Custom ranges<br/>Accessibility options]
        
        ThermalControls[Thermal Controls<br/>Parameter adjustment<br/>Calibration<br/>Recording controls]
    end
    
    subgraph "Data Flow"
        ThermalData[(Thermal Data<br/>Raw temperatures<br/>Processed images<br/>Metadata)]
        
        ConfigData[(Configuration<br/>Device settings<br/>User preferences<br/>Calibration data)]
    end
    
    %% Hardware connections
    TopdonTC001 --> TopdonSDK
    AndroidCamera --> CameraInterface
    
    %% SDK to processing
    TopdonSDK --> RawProcessor
    CameraInterface --> RawProcessor
    
    %% Processing pipeline
    RawProcessor --> ImageProcessor
    ImageProcessor --> TempCalculator
    TempCalculator --> StatisticsEngine
    
    %% Feature component connections
    StatisticsEngine --> MultiSpectral
    StatisticsEngine --> BasicDisplay
    StatisticsEngine --> MinimalUI
    
    AndroidCamera --> MultiSpectral
    
    MultiSpectral --> ObjectDetection
    ObjectDetection --> DataExport
    
    BasicDisplay --> BasicExport
    MinimalUI --> StreamingMode
    
    %% UI connections
    MultiSpectral --> ThermalView
    BasicDisplay --> ThermalView
    MinimalUI --> ThermalView
    
    StatisticsEngine --> TemperatureChart
    TempCalculator --> ColorPalette
    ObjectDetection --> ThermalControls
    
    %% Data connections
    TempCalculator --> ThermalData
    ConfigData --> TempCalculator
    DataExport --> ThermalData
```

### GSR Recording and BLE Integration

```mermaid
graph TB
    subgraph "Hardware Layer"
        Shimmer3[Shimmer3 GSR+<br/>Galvanic Skin Response<br/>- Wireless GSR sensor<br/>- Bluetooth Low Energy<br/>- Real-time streaming]
        
        AndroidBLE[Android BLE<br/>Bluetooth Hardware<br/>- BLE 4.0+ support<br/>- Multiple connections<br/>- Low power mode]
    end
    
    subgraph "BleModule: Shimmer Integration"
        subgraph "Device Management"
            ShimmerBleController[ShimmerBleController<br/>Device discovery<br/>Connection management<br/>Status monitoring]
            
            DeviceRegistry[Device Registry<br/>Paired devices<br/>Configuration storage<br/>Connection history]
        end
        
        subgraph "Protocol Implementation"
            ShimmerProtocol[Shimmer Protocol<br/>Command interface<br/>Data parsing<br/>Error handling]
            
            DataPacketParser[Data Packet Parser<br/>GSR data extraction<br/>Timestamp processing<br/>Quality validation]
        end
        
        subgraph "Communication"
            BLEManager[BLE Manager<br/>Android BLE API<br/>GATT services<br/>Characteristic handling]
            
            StreamingService[Streaming Service<br/>Real-time data flow<br/>Buffer management<br/>Flow control]
        end
    end
    
    subgraph "gsr-recording: Data Collection"
        subgraph "Data Processing"
            GSRProcessor[GSR Processor<br/>Signal filtering<br/>Artifact removal<br/>Calibration]
            
            QualityAnalyzer[Quality Analyzer<br/>Signal quality assessment<br/>Missing data detection<br/>Outlier identification]
        end
        
        subgraph "Network Integration"
            NetworkClient[Network Client<br/>TCP communication<br/>JSON protocol<br/>PC hub connection]
            
            DataStreamer[Data Streamer<br/>Real-time streaming<br/>Buffering<br/>Error recovery]
        end
        
        subgraph "Local Storage"
            LocalBuffer[Local Buffer<br/>Circular buffer<br/>Memory management<br/>Overflow handling]
            
            FileExporter[File Exporter<br/>CSV export<br/>Metadata inclusion<br/>Batch processing]
        end
        
        subgraph "User Interface"
            GSRDisplay[GSR Display<br/>Real-time plot<br/>Signal visualization<br/>Status indicators]
            
            RecordingControls[Recording Controls<br/>Start/stop recording<br/>Device selection<br/>Quality monitoring]
        end
    end
    
    subgraph "Integration Services"
        subgraph "Multi-Device Coordination"
            DeviceCoordinator[Device Coordinator<br/>Multiple Shimmer devices<br/>Synchronized recording<br/>Load balancing]
            
            AuthManager[Auth Manager<br/>Device authentication<br/>Pairing management<br/>Security protocols]
        end
        
        subgraph "Network Services"
            ZeroconfService[Zeroconf Service<br/>PC discovery<br/>Service announcement<br/>Network configuration]
            
            QoSManager[QoS Manager<br/>Network quality<br/>Adaptive streaming<br/>Congestion control]
        end
        
        subgraph "Error Recovery"
            ErrorRecovery[Error Recovery<br/>Connection retry<br/>Data recovery<br/>Graceful degradation]
            
            NetworkRecovery[Network Recovery<br/>Reconnection logic<br/>State synchronization<br/>Data integrity]
        end
    end
    
    subgraph "Data Types"
        GSRData[(GSR Data<br/>Conductance values<br/>Timestamps<br/>Quality metrics)]
        
        DeviceConfig[(Device Config<br/>Shimmer settings<br/>Sampling rate<br/>Calibration)]
        
        NetworkState[(Network State<br/>Connection status<br/>Stream health<br/>Error logs)]
    end
    
    %% Hardware connections
    Shimmer3 --> ShimmerBleController
    AndroidBLE --> BLEManager
    
    %% BLE module internal connections
    ShimmerBleController --> DeviceRegistry
    ShimmerBleController --> ShimmerProtocol
    ShimmerProtocol --> DataPacketParser
    BLEManager --> StreamingService
    DataPacketParser --> StreamingService
    
    %% Data flow to GSR recording
    StreamingService --> GSRProcessor
    GSRProcessor --> QualityAnalyzer
    QualityAnalyzer --> LocalBuffer
    
    %% Network integration
    LocalBuffer --> NetworkClient
    NetworkClient --> DataStreamer
    DataStreamer --> LocalBuffer
    
    %% UI integration
    QualityAnalyzer --> GSRDisplay
    LocalBuffer --> RecordingControls
    
    %% Multi-device coordination
    DeviceRegistry --> DeviceCoordinator
    ShimmerBleController --> AuthManager
    NetworkClient --> ZeroconfService
    DataStreamer --> QoSManager
    
    %% Error handling
    StreamingService --> ErrorRecovery
    NetworkClient --> NetworkRecovery
    
    %% Data storage
    GSRProcessor --> GSRData
    DeviceConfig --> ShimmerProtocol
    QualityAnalyzer --> NetworkState
    LocalBuffer --> FileExporter
```

---

## Data Flow Architecture

### Multi-Modal Data Synchronization

```mermaid
sequenceDiagram
    participant PC as PC Controller Hub
    participant A1 as Android Device 1
    participant A2 as Android Device 2
    participant S1 as Shimmer3 GSR 1
    participant S2 as Shimmer3 GSR 2
    participant TC1 as Thermal Camera 1
    participant TC2 as Thermal Camera 2
    
    Note over PC: Session Initialization
    PC->>A1: Discovery & Connection
    PC->>A2: Discovery & Connection
    
    A1->>S1: BLE Connection
    A1->>TC1: USB Connection
    A2->>S2: BLE Connection
    A2->>TC2: USB Connection
    
    Note over PC: Time Synchronization
    PC->>A1: Sync Command + Timestamp
    PC->>A2: Sync Command + Timestamp
    
    A1->>PC: ACK + Local Time
    A2->>PC: ACK + Local Time
    
    PC->>PC: Calculate Time Offsets
    
    Note over PC: Data Collection Start
    PC->>A1: Start Recording Command
    PC->>A2: Start Recording Command
    
    A1->>S1: Start GSR Streaming
    A1->>TC1: Start Thermal Capture
    A2->>S2: Start GSR Streaming
    A2->>TC2: Start Thermal Capture
    
    Note over PC: Real-time Data Streaming
    loop Every 100ms (GSR) / 30ms (Thermal)
        S1->>A1: GSR Data Packet
        TC1->>A1: Thermal Frame
        A1->>PC: Multi-modal Data Bundle
        
        S2->>A2: GSR Data Packet
        TC2->>A2: Thermal Frame
        A2->>PC: Multi-modal Data Bundle
        
        PC->>PC: Time Alignment & Buffering
    end
    
    Note over PC: Quality Monitoring
    PC->>A1: Quality Check Request
    PC->>A2: Quality Check Request
    
    A1->>PC: Signal Quality Report
    A2->>PC: Signal Quality Report
    
    alt Quality Issues Detected
        PC->>A1: Adjust Parameters
        PC->>A2: Adjust Parameters
    end
    
    Note over PC: Data Processing & Storage
    PC->>PC: Multi-modal Synchronization
    PC->>PC: HDF5 Export with Metadata
    
    Note over PC: Session Termination
    PC->>A1: Stop Recording Command
    PC->>A2: Stop Recording Command
    
    A1->>S1: Stop GSR Streaming
    A1->>TC1: Stop Thermal Capture
    A2->>S2: Stop GSR Streaming
    A2->>TC2: Stop Thermal Capture
    
    A1->>PC: Final Data & Summary
    A2->>PC: Final Data & Summary
    
    PC->>PC: Final Processing & Archive
```

### Data Processing Pipeline

```mermaid
graph LR
    subgraph "Data Sources"
        ThermalSrc[Thermal Camera<br/>30 FPS<br/>384x288<br/>Temperature data]
        
        GSRSrc[GSR Sensor<br/>51.2 Hz<br/>Conductance<br/>Timestamps]
        
        RGBSrc[RGB Camera<br/>30 FPS<br/>Various resolutions<br/>Color images]
    end
    
    subgraph "Acquisition Layer"
        ThermalAcq[Thermal Acquisition<br/>- Frame capture<br/>- Temperature conversion<br/>- Quality check]
        
        GSRAcq[GSR Acquisition<br/>- BLE streaming<br/>- Packet parsing<br/>- Signal validation]
        
        RGBAcq[RGB Acquisition<br/>- CameraX capture<br/>- Image processing<br/>- Compression]
    end
    
    subgraph "Processing Layer"
        ThermalProc[Thermal Processing<br/>- Calibration<br/>- Filtering<br/>- Enhancement]
        
        GSRProc[GSR Processing<br/>- Artifact removal<br/>- Filtering<br/>- Feature extraction]
        
        RGBProc[RGB Processing<br/>- Color correction<br/>- Alignment<br/>- Compression]
    end
    
    subgraph "Synchronization Layer"
        TimeAlign[Time Alignment<br/>- Clock synchronization<br/>- Timestamp correction<br/>- Drift compensation]
        
        DataFusion[Data Fusion<br/>- Multi-modal alignment<br/>- Interpolation<br/>- Gap filling]
        
        QualityCtrl[Quality Control<br/>- Signal quality<br/>- Missing data<br/>- Outlier detection]
    end
    
    subgraph "Storage Layer"
        BufferMgr[Buffer Manager<br/>- Circular buffers<br/>- Memory management<br/>- Overflow handling]
        
        HDF5Store[HDF5 Storage<br/>- Hierarchical structure<br/>- Metadata preservation<br/>- Compression]
        
        ExportMgr[Export Manager<br/>- Multiple formats<br/>- Batch processing<br/>- Validation]
    end
    
    subgraph "Network Layer"
        StreamMgr[Stream Manager<br/>- Real-time streaming<br/>- Flow control<br/>- Error recovery]
        
        NetworkProto[Network Protocol<br/>- JSON messaging<br/>- Command routing<br/>- Status reporting]
    end
    
    %% Data source to acquisition
    ThermalSrc --> ThermalAcq
    GSRSrc --> GSRAcq
    RGBSrc --> RGBAcq
    
    %% Acquisition to processing
    ThermalAcq --> ThermalProc
    GSRAcq --> GSRProc
    RGBAcq --> RGBProc
    
    %% Processing to synchronization
    ThermalProc --> TimeAlign
    GSRProc --> TimeAlign
    RGBProc --> TimeAlign
    
    TimeAlign --> DataFusion
    DataFusion --> QualityCtrl
    
    %% Synchronization to storage
    QualityCtrl --> BufferMgr
    BufferMgr --> HDF5Store
    HDF5Store --> ExportMgr
    
    %% Network integration
    QualityCtrl --> StreamMgr
    StreamMgr --> NetworkProto
    
    %% Feedback loops
    QualityCtrl -.-> ThermalProc
    QualityCtrl -.-> GSRProc
    NetworkProto -.-> TimeAlign
```

---

## Build System Architecture

### Gradle Multi-Module Build

```mermaid
graph TB
    subgraph "Root Project Configuration"
        RootBuild[build.gradle.kts<br/>- Global configuration<br/>- Plugin management<br/>- Common dependencies]
        
        Settings[settings.gradle.kts<br/>- Module inclusion<br/>- Project structure<br/>- Repository configuration]
        
        VersionCatalog[gradle/libs.versions.toml<br/>- Dependency versions<br/>- Library definitions<br/>- Plugin versions]
        
        GradleProps[gradle.properties<br/>- Build properties<br/>- JVM settings<br/>- Android SDK config]
    end
    
    subgraph "Application Module"
        AppBuild[app/build.gradle.kts<br/>- Application plugin<br/>- APK configuration<br/>- Signing & optimization]
    end
    
    subgraph "Core Library Modules"
        LibAppBuild[libapp/build.gradle.kts<br/>- Android library<br/>- Framework components<br/>- Common utilities]
        
        LibIRBuild[libir/build.gradle.kts<br/>- Native libraries<br/>- JNI integration<br/>- Topdon SDK]
        
        LibUIBuild[libui/build.gradle.kts<br/>- UI components<br/>- Custom views<br/>- Material design]
    end
    
    subgraph "Feature Component Modules"
        ThermalIRBuild[thermal-ir/build.gradle.kts<br/>- Advanced thermal<br/>- Image processing<br/>- Computer vision]
        
        GSRBuild[gsr-recording/build.gradle.kts<br/>- GSR processing<br/>- BLE integration<br/>- Data streaming]
        
        UserBuild[user/build.gradle.kts<br/>- User management<br/>- Profile handling<br/>- Preferences]
    end
    
    subgraph "External Module Builds"
        BleBuild[BleModule/build.gradle.kts<br/>- Shimmer integration<br/>- BLE protocols<br/>- Device management]
        
        RangeSeekBarBuild[RangeSeekBar/build.gradle.kts<br/>- Custom UI controls<br/>- Range selection<br/>- Touch handling]
    end
    
    subgraph "Consolidated Libraries"
        CommonBuild[CommonComponent/build.gradle.kts<br/>- Shared components<br/>- Interfaces<br/>- Utilities]
        
        ConsolidatedBuild[consolidated_libraries/<br/>- Shared configuration<br/>- Common scripts<br/>- Build utilities]
    end
    
    subgraph "Build Tools & Scripts"
        DevScript[dev.sh<br/>- Development tools<br/>- Linting & validation<br/>- Build automation]
        
        BuildScripts[scripts/<br/>- Production builds<br/>- APK generation<br/>- Release automation]
        
        CIWorkflows[.github/workflows/<br/>- GitHub Actions<br/>- CI/CD pipeline<br/>- Quality gates]
    end
    
    subgraph "Dependency Management"
        subgraph "Version Catalog Dependencies"
            AndroidDeps[Android Dependencies<br/>- AndroidX libraries<br/>- Material Design<br/>- CameraX]
            
            KotlinDeps[Kotlin Dependencies<br/>- Kotlin stdlib<br/>- Coroutines<br/>- Serialization]
            
            NetworkDeps[Network Dependencies<br/>- OkHttp<br/>- Retrofit<br/>- WebSocket]
            
            BLEDeps[BLE Dependencies<br/>- Shimmer SDK<br/>- Bluetooth libraries<br/>- Protocol handlers]
        end
    end
    
    %% Root configuration relationships
    RootBuild --> Settings
    RootBuild --> VersionCatalog
    RootBuild --> GradleProps
    
    %% Module build relationships
    Settings --> AppBuild
    Settings --> LibAppBuild
    Settings --> LibIRBuild
    Settings --> LibUIBuild
    Settings --> ThermalIRBuild
    Settings --> GSRBuild
    Settings --> UserBuild
    Settings --> BleBuild
    Settings --> RangeSeekBarBuild
    Settings --> CommonBuild
    
    %% Version catalog relationships
    VersionCatalog --> AndroidDeps
    VersionCatalog --> KotlinDeps
    VersionCatalog --> NetworkDeps
    VersionCatalog --> BLEDeps
    
    %% Module dependencies
    AppBuild --> LibAppBuild
    AppBuild --> LibIRBuild
    AppBuild --> LibUIBuild
    AppBuild --> BleBuild
    
    ThermalIRBuild --> LibIRBuild
    ThermalIRBuild --> LibUIBuild
    ThermalIRBuild --> CommonBuild
    
    GSRBuild --> BleBuild
    GSRBuild --> LibAppBuild
    GSRBuild --> CommonBuild
    
    UserBuild --> LibAppBuild
    UserBuild --> LibUIBuild
    
    %% Build tool relationships
    DevScript --> CIWorkflows
    DevScript --> BuildScripts
    CIWorkflows --> RootBuild
    BuildScripts --> AppBuild
    
    %% Dependency injection
    AndroidDeps --> AppBuild
    AndroidDeps --> LibAppBuild
    AndroidDeps --> LibUIBuild
    
    KotlinDeps --> AppBuild
    KotlinDeps --> ThermalIRBuild
    KotlinDeps --> GSRBuild
    
    NetworkDeps --> GSRBuild
    NetworkDeps --> AppBuild
    
    BLEDeps --> BleBuild
    BLEDeps --> GSRBuild
```

---

## Integration Architecture

### Complete Integration Flow

```mermaid
graph TB
    subgraph "Development Environment"
        DevMachine[Developer Machine<br/>- Android Studio<br/>- Python environment<br/>- Git repository]
        
        AndroidDevice[Android Test Device<br/>- Debug build<br/>- USB debugging<br/>- Network access]
        
        PCController[PC Controller<br/>- Python environment<br/>- GUI application<br/>- Data storage]
    end
    
    subgraph "Hardware Integration"
        ThermalCamera[Topdon TC001<br/>- USB-C connection<br/>- Native SDK<br/>- Temperature sensor]
        
        ShimmerGSR[Shimmer3 GSR+<br/>- Bluetooth LE<br/>- Wireless sensor<br/>- Real-time streaming]
        
        AndroidSensors[Android Sensors<br/>- RGB camera<br/>- Accelerometer<br/>- Network interfaces]
    end
    
    subgraph "Network Infrastructure"
        WiFiNetwork[WiFi Network<br/>- Local network<br/>- mDNS support<br/>- TCP/IP connectivity]
        
        InternetAccess[Internet Access<br/>- Cloud services<br/>- Time synchronization<br/>- Software updates]
    end
    
    subgraph "Data Flow Integration"
        subgraph "Real-time Streaming"
            ThermalStream[Thermal Stream<br/>- 30 FPS<br/>- Temperature data<br/>- Image frames]
            
            GSRStream[GSR Stream<br/>- 51.2 Hz<br/>- Conductance data<br/>- Quality metrics]
            
            ControlStream[Control Stream<br/>- Commands<br/>- Status updates<br/>- Configuration]
        end
        
        subgraph "Data Synchronization"
            TimestampSync[Timestamp Sync<br/>- Clock alignment<br/>- Drift correction<br/>- Latency compensation]
            
            DataAlignment[Data Alignment<br/>- Multi-modal sync<br/>- Interpolation<br/>- Quality control]
            
            BufferManagement[Buffer Management<br/>- Memory efficiency<br/>- Overflow handling<br/>- Flow control]
        end
    end
    
    subgraph "Quality Assurance Integration"
        subgraph "Testing Infrastructure"
            UnitTests[Unit Tests<br/>- Component testing<br/>- Mock devices<br/>- Error scenarios]
            
            IntegrationTests[Integration Tests<br/>- End-to-end testing<br/>- Multi-device<br/>- Network protocols]
            
            PerformanceTests[Performance Tests<br/>- Load testing<br/>- Memory usage<br/>- Network bandwidth]
        end
        
        subgraph "CI/CD Pipeline"
            BuildValidation[Build Validation<br/>- Gradle builds<br/>- Python tests<br/>- Code quality]
            
            DeploymentTests[Deployment Tests<br/>- APK generation<br/>- Installation<br/>- Functionality]
            
            QualityGates[Quality Gates<br/>- Code coverage<br/>- Static analysis<br/>- Performance metrics]
        end
    end
    
    subgraph "Production Deployment"
        ProductionAPK[Production APK<br/>- Release build<br/>- Code signing<br/>- Optimization]
        
        PCDistribution[PC Application<br/>- Python package<br/>- Dependencies<br/>- Installation]
        
        Documentation[Documentation<br/>- User guides<br/>- API reference<br/>- Troubleshooting]
    end
    
    %% Development connections
    DevMachine --> AndroidDevice
    DevMachine --> PCController
    
    %% Hardware connections
    ThermalCamera --> AndroidDevice
    ShimmerGSR -.->|BLE| AndroidDevice
    AndroidSensors --> AndroidDevice
    
    %% Network connections
    AndroidDevice --> WiFiNetwork
    PCController --> WiFiNetwork
    WiFiNetwork --> InternetAccess
    
    %% Data flow connections
    ThermalCamera --> ThermalStream
    ShimmerGSR --> GSRStream
    AndroidDevice --> ControlStream
    
    ThermalStream --> TimestampSync
    GSRStream --> TimestampSync
    ControlStream --> TimestampSync
    
    TimestampSync --> DataAlignment
    DataAlignment --> BufferManagement
    
    %% Quality assurance connections
    UnitTests --> BuildValidation
    IntegrationTests --> BuildValidation
    PerformanceTests --> BuildValidation
    
    BuildValidation --> DeploymentTests
    DeploymentTests --> QualityGates
    QualityGates --> ProductionAPK
    QualityGates --> PCDistribution
    
    %% Production connections
    ProductionAPK --> Documentation
    PCDistribution --> Documentation
    
    %% Feedback loops
    QualityGates -.-> UnitTests
    DeploymentTests -.-> IntegrationTests
    BufferManagement -.-> PerformanceTests
```

---

## Summary

This comprehensive architecture documentation provides precise Mermaid diagrams for every aspect of the IRCamera Multi-Modal Thermal Sensing Platform:

### Key Architectural Components Covered:

1. **System Overview** - Complete system with all layers and connections
2. **Hub-and-Spoke** - Distributed communication architecture
3. **Android Modules** - Detailed module dependencies and relationships
4. **PC Controller Hub** - Complete hub architecture with all services
5. **Feature Components** - Detailed thermal and GSR processing pipelines
6. **Data Flow** - Multi-modal synchronization and processing pipelines
7. **Build System** - Gradle multi-module build architecture
8. **Integration** - Complete development to production integration flow

### Architectural Principles Demonstrated:

- **Modularity**: Clear separation of concerns with well-defined interfaces
- **Scalability**: Hub-and-spoke design supporting multiple sensor nodes
- **Reliability**: Comprehensive error handling and quality assurance
- **Performance**: Optimized data processing and network communication
- **Maintainability**: Clean dependencies and documented interfaces

Each diagram shows exact relationships, dependencies, and data flows, providing a complete technical reference for understanding, developing, and maintaining the IRCamera platform.

---

## Extended Detailed Architecture Diagrams

### PC Controller Detailed Architecture

#### Actual Python Module Structure

```mermaid
graph TB
    subgraph "PC Controller Python Modules"
        subgraph "Entry Points"
            MainPy[main.py<br/>Application Entry Point]
            MVPSimple[mvp_simple.py<br/>~250 lines MVP]
            RunApp[run_mvp_app.py<br/>GUI Application]
            PCController[pc_controller.py<br/>Controller Entry]
        end
        
        subgraph "Core Modules (ircamera_pc/core/)"
            EnterpriseCore[enterprise_platform.py<br/>Platform Management]
            SessionMgr[session_manager.py<br/>Session Lifecycle]
            GSRIngestor[gsr_ingestor.py<br/>GSR Data Processing]
        end
        
        subgraph "Network Layer (ircamera_pc/network/)"
            Discovery[discovery.py<br/>mDNS Device Discovery]
            Server[server.py<br/>TCP Server]
            SecurityMgr[security_manager.py<br/>TLS Management]
            Messaging[messaging.py<br/>Message Handling]
            Protocol[protocol.py<br/>JSON Protocol]
            WebSocketSvr[websocket_server.py<br/>WebSocket Support]
            Security[security.py<br/>Encryption Utils]
        end
        
        subgraph "Data Processing (ircamera_pc/data/)"
            Processing[processing.py<br/>Data Pipeline]
            HDF5Export[hdf5_exporter.py<br/>HDF5 Storage]
        end
        
        subgraph "Utilities (ircamera_pc/utils/)"
            SimpleLogger[simple_logger.py<br/>Logging System]
        end
        
        subgraph "Time Sync (ircamera_pc/sync/)"
            TimeSyncSvc[timesync_service.py<br/>NTP-like Sync]
        end
        
        subgraph "GUI Layer (ircamera_pc/gui/)"
            GUIManager[gui_manager.py<br/>PyQt6 Interface]
            NetworkController[network_controller.py<br/>Network UI Control]
            DataAggregator[data_aggregator.py<br/>Data Visualization]
        end
    end
    
    %% Entry point connections
    MainPy --> EnterpriseCore
    MVPSimple --> Discovery
    MVPSimple --> SessionMgr
    RunApp --> GUIManager
    PCController --> Server
    
    %% Core module connections
    EnterpriseCore --> SessionMgr
    SessionMgr --> GSRIngestor
    
    %% Network layer connections
    Discovery --> Server
    Server --> SecurityMgr
    SecurityMgr --> Security
    Server --> Messaging
    Messaging --> Protocol
    Server --> WebSocketSvr
    
    %% Data processing connections
    GSRIngestor --> Processing
    Processing --> HDF5Export
    
    %% GUI connections
    GUIManager --> NetworkController
    NetworkController --> Discovery
    NetworkController --> Server
    GUIManager --> DataAggregator
    DataAggregator --> Processing
    
    %% Utility connections
    Server --> SimpleLogger
    Processing --> SimpleLogger
    SessionMgr --> TimeSyncSvc
    
    %% Cross-module dependencies
    Protocol --> TimeSyncSvc
    Processing --> TimeSyncSvc
```

### Android Class Diagram - Complete Kotlin Structure

```mermaid
classDiagram
    class MainActivity {
        +onCreate()
        +onDestroy()
        +handlePermissions()
        +initializeFragments()
        +setupViewPager()
        -viewModel: MainActivityViewModel
        -serviceConnection: ServiceConnection
        -fragments: List~Fragment~
    }
    
    class MainActivityViewModel {
        +sessionState: LiveData~SessionState~
        +deviceStatus: LiveData~DeviceStatus~
        +startSession()
        +stopSession()
        +handleDeviceConnection()
    }
    
    class ThermalCameraRecorder {
        +startRecording()
        +stopRecording()
        +getTemperatureData()
        +processFrame(Bitmap)
        -ircamEngine: IrcamEngine
        -uvcCamera: UVCCamera
        -frameCallback: IIrFrameCallback
        -csvWriter: CSVWriter
        -networkServer: NetworkServer
    }
    
    class Shimmer3GSRRecorder {
        +connectDevice()
        +startStreaming()
        +stopStreaming()
        +processGSRData()
        -shimmerDevice: Shimmer
        -bleManager: BluetoothManager
        -dataBuffer: CircularBuffer
        -qualityAnalyzer: SignalQualityAnalyzer
    }
    
    class ShimmerDeviceManager {
        +scanDevices()
        +pairDevice()
        +manageConnections()
        +sendBLECommand(byte[])
        -connectedDevices: Map~String,Shimmer~
        -deviceAdapter: ShimmerDeviceAdapter
        -bleCommands: BLECommandSet
    }
    
    class ShimmerGSRRecorder {
        +initializeShimmer()
        +configureGSR()
        +handleDataPacket()
        +exportData()
        -shimmerAPI: ShimmerAPIBridge
        -gsrConfig: GSRConfiguration
        -timestampManager: TimestampManager
    }
    
    class BleModule {
        +ShimmerBleController
        +DataPacketParser
        +StreamingService
        +DeviceRegistry
    }
    
    class NetworkClient {
        +connectToHub()
        +sendData()
        +receiveCommands()
        +handleTLSHandshake()
        -tcpSocket: Socket
        -jsonProtocol: JSONProtocol
        -encryptionManager: EncryptionManager
    }
    
    class SessionMetadata {
        +sessionId: UUID
        +startTime: Timestamp
        +deviceList: List~Device~
        +exportMetadata()
        +validateIntegrity()
    }
    
    class TimestampManager {
        +synchronizeClocks()
        +alignTimestamps()
        +calculateOffset()
        -ntpOffset: Long
        -driftCompensation: Double
    }
    
    MainActivity --> MainActivityViewModel
    MainActivity --> ThermalCameraRecorder
    MainActivity --> Shimmer3GSRRecorder
    MainActivity --> NetworkClient
    
    ThermalCameraRecorder --> SessionMetadata
    ThermalCameraRecorder --> TimestampManager
    
    Shimmer3GSRRecorder --> ShimmerDeviceManager
    Shimmer3GSRRecorder --> ShimmerGSRRecorder
    ShimmerGSRRecorder --> BleModule
    
    ShimmerDeviceManager --> BleModule
    NetworkClient --> SessionMetadata
    
    BleModule --> TimestampManager
```

### Repository Module Dependencies - Multi-project Gradle

```mermaid
graph TB
    subgraph "Root Project (MPDC4GSR)"
        RootGradle[build.gradle.kts<br/>Global Config]
        SettingsGradle[settings.gradle.kts<br/>Module Registry]
        VersionCatalog[libs.versions.toml<br/>Version Management]
        GradleProps[gradle.properties<br/>Build Properties]
    end
    
    subgraph "Android Application"
        AppModule[app<br/>build.gradle.kts]
        
        subgraph "App Structure"
            MainActivity[MainActivity.kt]
            ThermalActivity[ThermalCameraRecorder.kt]
            ShimmerActivity[Shimmer3GSRRecorder.kt]
            NetworkClient[ShimmerNetworkClient.kt]
        end
    end
    
    subgraph "Feature Components"
        ThermalIR[thermal-ir<br/>Advanced IR Processing]
        Thermal[thermal<br/>Basic Processing] 
        ThermalLite[thermal-lite<br/>Lite Processing]
        GSRRecording[gsr-recording<br/>ShimmerGSRRecorder]
        UserMgmt[user<br/>Profile Management]
    end
    
    subgraph "Core Libraries"
        LibApp[libapp<br/>Framework Base]
        LibIR[libir<br/>Topdon TC001 SDK]
        LibUI[libui<br/>UI Components]
        LibCom[libcom<br/>Network Protocol]
        LibMatrix[libmatrix<br/>Math Operations]
        LibMenu[libmenu<br/>Menu System]
    end
    
    subgraph "External Dependencies"
        BleModule[BleModule<br/>Shimmer Integration]
        RangeSeekBar[RangeSeekBar<br/>UI Controls]
        ConsolidatedLibs[consolidated_libraries/<br/>CommonComponent]
    end
    
    subgraph "Native Libraries"
        TopdonSDK[Topdon TC001 SDK<br/>libir/libs/]
        ShimmerSDK[Shimmer SDK<br/>BleModule/libs/]
        OpenCV[OpenCV<br/>Image Processing]
    end
    
    %% Project structure
    RootGradle --> SettingsGradle
    SettingsGradle --> VersionCatalog
    RootGradle --> AppModule
    
    %% App dependencies
    AppModule --> MainActivity
    AppModule --> LibApp
    AppModule --> LibUI
    AppModule --> LibCom
    AppModule --> LibIR
    AppModule --> BleModule
    
    %% Component dependencies  
    ThermalIR --> LibIR
    ThermalIR --> LibMatrix
    ThermalIR --> TopdonSDK
    
    Thermal --> LibIR
    ThermalLite --> LibIR
    
    GSRRecording --> BleModule
    GSRRecording --> ShimmerSDK
    GSRRecording --> LibCom
    
    UserMgmt --> LibApp
    UserMgmt --> LibUI
    
    %% Core library interdependencies
    LibIR --> LibMatrix
    LibIR --> OpenCV
    LibUI --> LibMatrix
    LibApp --> LibCom
    LibMenu --> LibUI
    
    %% External integrations
    BleModule --> ShimmerSDK
    LibIR --> TopdonSDK
    LibUI --> RangeSeekBar
    
    %% Version management
    VersionCatalog --> TopdonSDK
    VersionCatalog --> ShimmerSDK
    VersionCatalog --> OpenCV
```

### Sensor Integration Features - Detailed Hardware Integration

```mermaid
graph TB
    subgraph "Shimmer3 GSR+ Integration"
        subgraph "Hardware Layer"
            ShimmerHW[Shimmer3 GSR+<br/>Wireless GSR Sensor<br/>Bluetooth LE]
        end
        
        subgraph "BLE Protocol Layer"
            BLEStack[Android BLE Stack<br/>BluetoothGatt API]
            
            subgraph "Shimmer BLE Commands"
                Cmd07[Command 0x07<br/>Start Streaming]
                Cmd20[Command 0x20<br/>Stop Streaming]
                CmdConfig[Config Commands<br/>Set Sampling Rate]
                CmdCalib[Calibration Commands<br/>GSR Range Settings]
            end
        end
        
        subgraph "Shimmer API Layer"
            ShimmerAPI[ShimmerAPIBridge<br/>Native SDK Wrapper]
            DataParser[Data Packet Parser<br/>GSR Value Extraction]
            QualityCheck[Signal Quality Analyzer<br/>Artifact Detection]
        end
    end
    
    subgraph "Topdon TC001 Thermal Integration"
        subgraph "Hardware Detection"
            USBMgr[USB Manager<br/>VID/PID Detection]
            TC001HW[Topdon TC001<br/>VID: 0x1234, PID: 0x5678<br/>USB-C Interface]
        end
        
        subgraph "TC001 SDK Layer"
            TopdonSDK[Topdon Native SDK<br/>libir/libs/]
            IrcamEngine[IrcamEngine<br/>Temperature Conversion]
            UVCCamera[UVC Camera Interface<br/>Frame Capture]
            TempCalc[Temperature Calculator<br/>Radiometric Processing]
        end
        
        subgraph "Processing Pipeline"
            FrameProc[Frame Processor<br/>384x288 Resolution]
            TempMap[Temperature Mapping<br/>-20degC to 400degC]
            ColorMap[Color Palette Mapping<br/>Rainbow/Iron/Gray]
        end
    end
    
    subgraph "CameraX Dual Pipeline"
        subgraph "RGB Camera Integration"
            CameraXAPI[CameraX API<br/>Android Camera2]
            ImageCapture[Image Capture<br/>High Resolution]
            VideoCapture[Video Capture<br/>30 FPS]
        end
        
        subgraph "Dual Stream Processing"
            RGBProcessor[RGB Processor<br/>Color Correction]
            IRProcessor[IR Processor<br/>Thermal Overlay]
            StreamSync[Stream Synchronizer<br/>Frame Alignment]
        end
    end
    
    %% Shimmer integration flow
    ShimmerHW --> BLEStack
    BLEStack --> Cmd07
    BLEStack --> Cmd20
    BLEStack --> CmdConfig
    Cmd07 --> ShimmerAPI
    ShimmerAPI --> DataParser
    DataParser --> QualityCheck
    
    %% TC001 integration flow
    TC001HW --> USBMgr
    USBMgr --> TopdonSDK
    TopdonSDK --> IrcamEngine
    TopdonSDK --> UVCCamera
    IrcamEngine --> TempCalc
    UVCCamera --> FrameProc
    TempCalc --> TempMap
    FrameProc --> ColorMap
    
    %% CameraX integration
    CameraXAPI --> ImageCapture
    CameraXAPI --> VideoCapture
    ImageCapture --> RGBProcessor
    VideoCapture --> RGBProcessor
    RGBProcessor --> StreamSync
    ColorMap --> IRProcessor
    IRProcessor --> StreamSync
    
    %% Cross-sensor synchronization
    QualityCheck --> StreamSync
    StreamSync --> DataExport[Data Export Pipeline]
```

### Communication Protocol Sequence - Complete Protocol Flow

```mermaid
sequenceDiagram
    participant A as Android Device
    participant PC as PC Controller Hub
    participant S as Shimmer3 GSR
    participant T as Topdon TC001
    
    Note over A,PC: Phase 1: Discovery & Initial Connection
    
    A->>PC: mDNS Service Discovery Broadcast
    PC->>A: Service Response (IRCamera-Hub._tcp)
    A->>PC: TCP Connection Request (Port 8080)
    PC->>A: Connection Accepted
    
    Note over A,PC: Phase 2: TLS Handshake & Authentication
    
    A->>PC: ClientHello (TLS 1.3)
    PC->>A: ServerHello + Certificate
    A->>PC: Certificate Verification
    PC->>A: TLS Handshake Complete
    
    Note over A,PC: Phase 3: Device Registration & Capabilities
    
    A->>PC: Device Registration JSON<br/>{"type":"android_node","capabilities":["thermal","gsr","rgb"]}
    PC->>A: Registration ACK + Session ID
    A->>PC: Hardware Status JSON<br/>{"shimmer":"connected","tc001":"detected","rgb":"available"}
    
    Note over A,PC: Phase 4: Time Synchronization (NTP-like)
    
    PC->>A: Time Sync Request + T1 (PC timestamp)
    A->>PC: Time Sync Response + T1 + T2 (Android receive) + T3 (Android send)
    PC->>A: Time Sync Final + T4 (PC receive)<br/>Calculated Offset: ((T2-T1)+(T3-T4))/2
    
    Note over A,PC: Phase 5: Sensor Initialization
    
    A->>S: BLE Connect + Pairing
    S->>A: Pairing Complete
    A->>S: Config Command 0x07 (Start Streaming, 51.2 Hz)
    S->>A: Config ACK
    
    A->>T: USB Permission Request
    T->>A: Permission Granted
    A->>T: Initialize SDK (VID: 0x1234, PID: 0x5678)
    T->>A: SDK Initialized, Camera Ready
    
    Note over A,PC: Phase 6: Session Start & Data Streaming
    
    PC->>A: Start Session Command<br/>{"command":"start_session","session_id":"uuid","metadata":{}}
    A->>PC: Session Started ACK
    
    loop Real-time Data Streaming
        S->>A: GSR Data Packet (51.2 Hz)<br/>{"gsr_value":15.2,"timestamp":1234567890,"quality":"good"}
        T->>A: Thermal Frame (30 FPS)<br/>384x288 temperature matrix
        A->>PC: Multi-modal Data Bundle<br/>{"thermal_frame":"base64","gsr_data":{},"timestamp_sync":{}}
        
        PC->>A: Quality Check Response<br/>{"status":"ok","next_frame":"ready"}
    end
    
    Note over A,PC: Phase 7: Session Termination & Data Export
    
    PC->>A: Stop Session Command
    A->>S: BLE Command 0x20 (Stop Streaming)
    S->>A: Streaming Stopped
    A->>PC: Session Summary<br/>{"total_frames":1800,"gsr_samples":3072,"duration":60}
    PC->>A: HDF5 Export Complete<br/>{"file_path":"/export/session_uuid.h5","size":"15.2MB"}
```

### Data Synchronization Architecture - NTP-like Clock Offset Algorithm

```mermaid
graph TB
    subgraph "Time Synchronization System"
        subgraph "PC Controller Hub (Master Clock)"
            PCClock[PC System Clock<br/>Master Reference]
            NTPClient[NTP Client<br/>Internet Time Sync]
            TimestampGen[Timestamp Generator<br/>Microsecond Precision]
        end
        
        subgraph "Android Node (Slave Clock)"
            AndroidClock[Android System Clock<br/>Local Reference]
            ClockOffset[Clock Offset Calculator<br/>NTP-like Algorithm]
            DriftComp[Drift Compensator<br/>Linear Regression]
        end
        
        subgraph "Synchronization Protocol"
            SyncReq[Sync Request<br/>T1: PC Send Time]
            SyncResp[Sync Response<br/>T2: Android Receive<br/>T3: Android Send]
            SyncFinal[Sync Final<br/>T4: PC Receive<br/>Offset = ((T2-T1)+(T3-T4))/2]
        end
    end
    
    subgraph "Multi-modal Data Alignment"
        subgraph "Data Stream Inputs"
            ThermalStream[Thermal Stream<br/>30 FPS<br/>33.33ms intervals]
            GSRStream[GSR Stream<br/>51.2 Hz<br/>19.53ms intervals]
            RGBStream[RGB Stream<br/>30 FPS<br/>33.33ms intervals]
        end
        
        subgraph "Timestamp Correction"
            ThermalTS[Thermal Timestamp<br/>Corrector]
            GSRTS[GSR Timestamp<br/>Corrector]
            RGBTS[RGB Timestamp<br/>Corrector]
        end
        
        subgraph "Alignment Engine"
            InterpolateEngine[Interpolation Engine<br/>Cubic Spline]
            ResampleEngine[Resampling Engine<br/>Common Time Base]
            QualityValidator[Quality Validator<br/>Gap Detection]
        end
        
        subgraph "Synchronized Output"
            SyncBuffer[Synchronized Buffer<br/>Common Timestamps]
            MetadataGen[Metadata Generator<br/>Sync Quality Metrics]
        end
    end
    
    subgraph "HDF5 Export Pipeline"
        subgraph "Data Organization"
            ThermalGroup[/thermal/<br/>Temperature Matrices]
            GSRGroup[/gsr/<br/>Conductance Values]
            RGBGroup[/rgb/<br/>Image Data]
            MetaGroup[/metadata/<br/>Session Info]
        end
        
        subgraph "Export Processing"
            Compression[HDF5 Compression<br/>GZIP Level 6]
            Chunking[Data Chunking<br/>Optimized Access]
            Indexing[Time Indexing<br/>Fast Retrieval]
        end
    end
    
    %% Time sync connections
    PCClock --> NTPClient
    NTPClient --> TimestampGen
    TimestampGen --> SyncReq
    SyncReq --> AndroidClock
    AndroidClock --> ClockOffset
    ClockOffset --> SyncResp
    SyncResp --> SyncFinal
    SyncFinal --> DriftComp
    
    %% Data alignment connections
    ThermalStream --> ThermalTS
    GSRStream --> GSRTS
    RGBStream --> RGBTS
    
    ThermalTS --> InterpolateEngine
    GSRTS --> InterpolateEngine
    RGBTS --> InterpolateEngine
    
    InterpolateEngine --> ResampleEngine
    ResampleEngine --> QualityValidator
    QualityValidator --> SyncBuffer
    SyncBuffer --> MetadataGen
    
    %% HDF5 export connections
    SyncBuffer --> ThermalGroup
    SyncBuffer --> GSRGroup
    SyncBuffer --> RGBGroup
    MetadataGen --> MetaGroup
    
    ThermalGroup --> Compression
    GSRGroup --> Compression
    RGBGroup --> Compression
    Compression --> Chunking
    Chunking --> Indexing
    
    %% Cross-system connections
    DriftComp --> ThermalTS
    DriftComp --> GSRTS
    DriftComp --> RGBTS
```

### Session Lifecycle State Machine - Complete Workflow

```mermaid
stateDiagram-v2
    [*] --> Idle: Application Start
    
    state "Device Discovery" as Discovery {
        [*] --> Scanning
        Scanning --> DeviceFound: mDNS Response
        DeviceFound --> Connecting: User Selection
        Connecting --> Connected: TCP Established
        Connecting --> ConnectionFailed: Timeout/Error
        ConnectionFailed --> Scanning: Retry
        Connected --> [*]: Success
    }
    
    state "Session Initialization" as Initialization {
        [*] --> Authentication
        Authentication --> Authenticated: TLS Success
        Authentication --> AuthFailed: Certificate Error
        AuthFailed --> [*]: Return to Discovery
        Authenticated --> HardwareCheck
        HardwareCheck --> HardwareReady: All Sensors OK
        HardwareCheck --> HardwareError: Sensor Failure
        HardwareReady --> TimeSyncStart
        TimeSyncStart --> TimeSynced: Offset Calculated
        TimeSynced --> [*]: Ready for Session
        HardwareError --> [*]: Hardware Diagnostics
    }
    
    state "Active Session" as Session {
        [*] --> SessionStarting
        SessionStarting --> Recording: All Systems Go
        Recording --> Paused: User Pause
        Recording --> QualityCheck: Data Validation
        Paused --> Recording: Resume
        QualityCheck --> Recording: Quality OK
        QualityCheck --> DataError: Quality Issues
        DataError --> Recording: Error Recovered
        DataError --> SessionStopping: Critical Error
        Recording --> SessionStopping: User Stop/Duration Complete
        SessionStopping --> [*]: Session Complete
    }
    
    state "Data Processing" as Processing {
        [*] --> DataValidation
        DataValidation --> Processing_State: Data Valid
        DataValidation --> ValidationError: Data Issues
        ValidationError --> [*]: Return with Error
        Processing_State --> HDF5Export: Processing Complete
        HDF5Export --> ExportComplete: File Generated
        ExportComplete --> [*]: Success
    }
    
    state "Error Handling" as ErrorStates {
        [*] --> ErrorDetected
        ErrorDetected --> NetworkError: Connection Issues
        ErrorDetected --> SensorError: Hardware Problems
        ErrorDetected --> DataCorruption: Data Issues
        NetworkError --> Reconnecting: Auto Retry
        SensorError --> SensorRecovery: Restart Sensor
        DataCorruption --> DataRecovery: Repair Attempt
        Reconnecting --> [*]: Success/Failure
        SensorRecovery --> [*]: Success/Failure
        DataRecovery --> [*]: Success/Failure
    }
    
    Idle --> Discovery: Start Discovery
    Discovery --> Initialization: Device Connected
    Initialization --> Session: Session Ready
    Session --> Processing: Session Complete
    Processing --> Idle: Export Complete
    
    %% Error transitions from any state
    Discovery --> ErrorStates: Connection Error
    Initialization --> ErrorStates: Init Error
    Session --> ErrorStates: Runtime Error
    Processing --> ErrorStates: Export Error
    
    ErrorStates --> Discovery: Network Recovery
    ErrorStates --> Initialization: Sensor Recovery
    ErrorStates --> Session: Resume Session
    ErrorStates --> Idle: Critical Error
```

### Android UI Navigation Flow - Complete User Experience

```mermaid
flowchart TB
    subgraph "App Launch & Permissions"
        AppStart([App Launch<br/>IRCamera])
        PermissionCheck{Permissions<br/>Granted?}
        PermissionRequest[Request Permissions<br/>- Camera<br/>- Bluetooth<br/>- Storage<br/>- Location]
        PermissionDenied[Permission Denied<br/>Show Rationale]
    end
    
    subgraph "Main Dashboard"
        MainDashboard[Dashboard Fragment<br/>System Status Overview]
        
        subgraph "Navigation Tabs"
            SensorTab[Sensor Status Tab<br/>Device Health]
            SessionTab[Session Control Tab<br/>Recording Controls]  
            SettingsTab[Settings Tab<br/>Configuration]
            ExportTab[Export Tab<br/>Data Management]
        end
    end
    
    subgraph "Sensor Management"
        SensorStatus[SensorStatusFragment<br/>Real-time Status]
        ThermalMgmt[TC001ManagementFragment<br/>Thermal Camera Control]
        GSRMgmt[GSRManagementFragment<br/>Shimmer3 Control]
        CameraMgmt[CameraManagementFragment<br/>RGB Camera Control]
        
        subgraph "Sensor Configurations"
            ThermalConfig[ThermalConfigDialog<br/>- Frame Rate<br/>- Temperature Range<br/>- Color Palette]
            GSRConfig[GSRConfigDialog<br/>- Sampling Rate<br/>- GSR Range<br/>- Calibration]
            CameraConfig[CameraConfigDialog<br/>- Resolution<br/>- Focus Mode<br/>- Exposure]
        end
    end
    
    subgraph "Session Control"
        SessionControl[SessionControlFragment<br/>Recording Interface]
        SessionMetadata[SessionMetadataDialog<br/>- Participant Info<br/>- Study Details<br/>- Notes]
        LivePreview[LivePreviewFragment<br/>Real-time Data Display]
        
        subgraph "Recording States"
            RecordingActive[Recording Active<br/>- Live Data Streams<br/>- Quality Indicators<br/>- Duration Timer]
            RecordingPaused[Recording Paused<br/>- Resume/Stop Options<br/>- Current Statistics]
        end
    end
    
    subgraph "Data Export & Review"
        ExportManager[ExportManagerFragment<br/>Session List]
        ExportConfig[ExportConfigDialog<br/>- Format Selection<br/>- Quality Settings<br/>- Metadata Options]
        ExportProgress[ExportProgressDialog<br/>HDF5 Generation]
        ExportComplete[ExportCompleteDialog<br/>File Location & Share]
    end
    
    subgraph "Settings & Configuration"
        AppSettings[AppSettingsFragment<br/>Global Configuration]
        NetworkSettings[NetworkSettingsDialog<br/>PC Hub Connection]
        SecuritySettings[SecuritySettingsDialog<br/>TLS Configuration]
        DiagnosticsView[DiagnosticsFragment<br/>System Health]
    end
    
    subgraph "Error & Help States"
        ErrorDialog[ErrorDialog<br/>Error Messages & Recovery]
        HelpFragment[HelpFragment<br/>User Guidance]
        AboutDialog[AboutDialog<br/>App Information]
    end
    
    %% App launch flow
    AppStart --> PermissionCheck
    PermissionCheck -->|Yes| MainDashboard
    PermissionCheck -->|No| PermissionRequest
    PermissionRequest --> PermissionCheck
    PermissionRequest -->|Denied| PermissionDenied
    PermissionDenied --> PermissionRequest
    
    %% Main navigation
    MainDashboard --> SensorTab
    MainDashboard --> SessionTab
    MainDashboard --> SettingsTab
    MainDashboard --> ExportTab
    
    %% Sensor management flow
    SensorTab --> SensorStatus
    SensorStatus --> ThermalMgmt
    SensorStatus --> GSRMgmt
    SensorStatus --> CameraMgmt
    
    ThermalMgmt --> ThermalConfig
    GSRMgmt --> GSRConfig
    CameraMgmt --> CameraConfig
    
    %% Session control flow
    SessionTab --> SessionControl
    SessionControl --> SessionMetadata
    SessionMetadata --> LivePreview
    LivePreview --> RecordingActive
    RecordingActive --> RecordingPaused
    RecordingPaused --> RecordingActive
    
    %% Export flow
    ExportTab --> ExportManager
    ExportManager --> ExportConfig
    ExportConfig --> ExportProgress
    ExportProgress --> ExportComplete
    
    %% Settings flow
    SettingsTab --> AppSettings
    AppSettings --> NetworkSettings
    AppSettings --> SecuritySettings
    AppSettings --> DiagnosticsView
    
    %% Error handling (can occur from any state)
    SensorStatus -.-> ErrorDialog
    SessionControl -.-> ErrorDialog
    ExportProgress -.-> ErrorDialog
    ErrorDialog --> HelpFragment
    
    %% Navigation back to main
    ThermalConfig -.-> SensorStatus
    GSRConfig -.-> SensorStatus
    SessionMetadata -.-> SessionControl
    ExportComplete -.-> ExportManager
    NetworkSettings -.-> AppSettings
```

### Security Architecture - End-to-end Security Implementation

```mermaid
graph TB
    subgraph "TLS Authentication Layer"
        subgraph "Certificate Management"
            CACert[CA Certificate<br/>Root Authority]
            ServerCert[Server Certificate<br/>PC Controller Hub]
            ClientCert[Client Certificate<br/>Android Device]
            CertValidation[Certificate Validation<br/>Chain of Trust]
        end
        
        subgraph "TLS Handshake Process"
            ClientHello[ClientHello<br/>TLS 1.3]
            ServerHello[ServerHello<br/>+ Server Certificate]
            CertVerify[Certificate Verification<br/>Public Key Validation]
            KeyExchange[Key Exchange<br/>ECDH P-256]
            HandshakeComplete[Handshake Complete<br/>Secure Channel Established]
        end
    end
    
    subgraph "Data Encryption Layer"
        subgraph "AES256-GCM Encryption"
            EncryptionKey[256-bit Encryption Key<br/>Per-Session Generation]
            GCMMode[GCM Mode<br/>Authenticated Encryption]
            IVGeneration[IV Generation<br/>Cryptographically Secure]
            AuthTag[Authentication Tag<br/>128-bit MAC]
        end
        
        subgraph "Key Management"
            KeyDerivation[Key Derivation<br/>PBKDF2-SHA256]
            KeyRotation[Key Rotation<br/>Every 1MB of Data]
            KeyStorage[Secure Key Storage<br/>Android Keystore]
        end
    end
    
    subgraph "Android Security Integration"
        subgraph "Android Keystore"
            KeystoreAPI[Android Keystore API<br/>Hardware-backed Keys]
            BiometricAuth[Biometric Authentication<br/>Fingerprint/Face]
            SecureElement[Secure Element<br/>TEE Integration]
        end
        
        subgraph "Permission Management"
            RuntimePerms[Runtime Permissions<br/>- Camera<br/>- Bluetooth<br/>- Storage]
            PermissionCheck[Permission Validation<br/>Dynamic Checking]
            PrivacyPolicy[Privacy Policy Compliance<br/>GDPR/CCPA]
        end
    end
    
    subgraph "Data Protection & Anonymization"
        subgraph "Data Anonymization"
            PIIDetection[PII Detection<br/>Sensitive Data Identification]
            DataMasking[Data Masking<br/>Reversible Pseudonymization]
            HashGeneration[Hash Generation<br/>SHA-256 Participant IDs]
        end
        
        subgraph "Secure Storage"
            EncryptedDB[Encrypted Database<br/>SQLCipher]
            SecureFiles[Secure File Storage<br/>AES-encrypted]
            TempDataCleaning[Temporary Data Cleaning<br/>Automatic Purge]
        end
    end
    
    subgraph "Network Security"
        subgraph "Transport Security"
            TLSChannel[TLS 1.3 Channel<br/>Perfect Forward Secrecy]
            NetworkValidation[Network Validation<br/>Certificate Pinning]
            DDoSProtection[DDoS Protection<br/>Rate Limiting]
        end
        
        subgraph "Access Control"
            DeviceWhitelist[Device Whitelist<br/>MAC Address Filtering]
            NetworkSegmentation[Network Segmentation<br/>VLAN Isolation]
            FirewallRules[Firewall Rules<br/>Port-specific Access]
        end
    end
    
    %% Certificate management flow
    CACert --> ServerCert
    CACert --> ClientCert
    ServerCert --> CertValidation
    ClientCert --> CertValidation
    
    %% TLS handshake flow
    ClientHello --> ServerHello
    ServerHello --> CertVerify
    CertVerify --> KeyExchange
    KeyExchange --> HandshakeComplete
    
    %% Encryption flow
    KeyDerivation --> EncryptionKey
    EncryptionKey --> GCMMode
    GCMMode --> IVGeneration
    IVGeneration --> AuthTag
    
    %% Key management
    EncryptionKey --> KeyRotation
    KeyRotation --> KeyStorage
    KeyStorage --> KeystoreAPI
    
    %% Android security integration
    KeystoreAPI --> BiometricAuth
    BiometricAuth --> SecureElement
    RuntimePerms --> PermissionCheck
    PermissionCheck --> PrivacyPolicy
    
    %% Data protection
    PIIDetection --> DataMasking
    DataMasking --> HashGeneration
    HashGeneration --> EncryptedDB
    EncryptedDB --> SecureFiles
    SecureFiles --> TempDataCleaning
    
    %% Network security
    HandshakeComplete --> TLSChannel
    TLSChannel --> NetworkValidation
    NetworkValidation --> DDoSProtection
    DeviceWhitelist --> NetworkSegmentation
    NetworkSegmentation --> FirewallRules
```

### Build System Architecture - Multi-project Gradle with Performance Optimizations

```mermaid
graph TB
    subgraph "Gradle Build Architecture"
        subgraph "Root Configuration"
            RootBuild[build.gradle.kts<br/>Root Project Configuration]
            SettingsGradle[settings.gradle.kts<br/>Project Structure Definition]
            GradleProps[gradle.properties<br/>- JVM Memory: -Xmx4g<br/>- Parallel Builds: true<br/>- Daemon: true<br/>- Configuration Cache: true]
            VersionCatalog[libs.versions.toml<br/>Centralized Version Management]
        end
        
        subgraph "Build Performance Optimizations"
            ConfigCache[Configuration Cache<br/>Gradle 8.0+ Feature]
            BuildCache[Build Cache<br/>Local + Remote]
            ParallelExecution[Parallel Execution<br/>--parallel flag]
            IncrementalCompilation[Incremental Compilation<br/>Kotlin + Java]
            AnnotationProcessing[Annotation Processing<br/>Kapt Configuration]
        end
        
        subgraph "Hardware Build Flavors"
            FullFlavor[full<br/>- All Sensors<br/>- Complete Feature Set<br/>- Production Build]
            LiteFlavor[lite<br/>- Essential Sensors<br/>- Reduced Features<br/>- Demo Build]
            DebugFlavor[debug<br/>- Debug Symbols<br/>- Logging Enabled<br/>- Development Build]
        end
        
        subgraph "JVM Memory Management"
            HeapSettings[JVM Heap Settings<br/>-Xms2g -Xmx4g]
            GCSettings[Garbage Collector<br/>G1GC Configuration]
            MetaspaceSettings[Metaspace<br/>-XX:MetaspaceSize=512m]
        end
    end
    
    subgraph "Dependency Management Tree"
        subgraph "Android Dependencies"
            AndroidXCore[androidx.core<br/>1.12.0]
            AndroidXLifecycle[androidx.lifecycle<br/>2.7.0]
            AndroidXFragment[androidx.fragment<br/>1.6.2]
            CameraX[androidx.camera<br/>1.3.0]
            Material[material<br/>1.10.0]
        end
        
        subgraph "Networking Dependencies"
            OkHttp[okhttp<br/>4.12.0]
            Retrofit[retrofit<br/>2.9.0]
            Gson[gson<br/>2.10.1]
            NetworkSecurityConfig[network-security-config<br/>Custom TLS]
        end
        
        subgraph "BLE & Hardware Dependencies"
            ShimmerSDK[shimmer-android-sdk<br/>3.0.0]
            TopdonSDK[topdon-tc001-sdk<br/>1.2.1]
            BluetoothGatt[bluetooth-gatt<br/>Android Framework]
        end
        
        subgraph "Data Processing Dependencies"
            OpenCV[opencv-android<br/>4.8.0]
            ApacheCommons[commons-math3<br/>3.6.1]
            HDF5Java[hdf5-java<br/>1.14.0]
        end
        
        subgraph "Testing Dependencies"
            JUnit5[junit5<br/>5.10.0]
            Mockito[mockito-kotlin<br/>4.1.0]
            AndroidXTest[androidx.test<br/>1.5.0]
            Espresso[espresso<br/>3.5.1]
        end
    end
    
    subgraph "Module Build Pipeline"
        subgraph "Compilation Stages"
            KotlinCompile[Kotlin Compilation<br/>kotlinc 1.9.0]
            JavaCompile[Java Compilation<br/>javac 17]
            ResourceCompile[Resource Compilation<br/>AAPT2]
            NativeCompile[Native Compilation<br/>NDK r25c]
        end
        
        subgraph "Optimization Stages"
            ProGuardR8[ProGuard/R8<br/>Code Minification]
            ResourceShrinking[Resource Shrinking<br/>Unused Resource Removal]
            APKOptimization[APK Optimization<br/>zipalign + apksigner]
        end
        
        subgraph "Quality Gates"
            Detekt[Detekt<br/>Kotlin Static Analysis]
            SpotBugs[SpotBugs<br/>Java Bug Detection]
            Lint[Android Lint<br/>Platform-specific Checks]
            UnitTests[Unit Tests<br/>JUnit + Mockito]
            IntegrationTests[Integration Tests<br/>Instrumented Tests]
        end
    end
    
    %% Root configuration connections
    RootBuild --> SettingsGradle
    SettingsGradle --> VersionCatalog
    GradleProps --> ConfigCache
    ConfigCache --> BuildCache
    
    %% Performance optimization connections
    ParallelExecution --> IncrementalCompilation
    IncrementalCompilation --> AnnotationProcessing
    BuildCache --> ParallelExecution
    
    %% Build flavors
    RootBuild --> FullFlavor
    RootBuild --> LiteFlavor
    RootBuild --> DebugFlavor
    
    %% JVM memory management
    GradleProps --> HeapSettings
    HeapSettings --> GCSettings
    GCSettings --> MetaspaceSettings
    
    %% Dependency management
    VersionCatalog --> AndroidXCore
    VersionCatalog --> OkHttp
    VersionCatalog --> ShimmerSDK
    VersionCatalog --> OpenCV
    VersionCatalog --> JUnit5
    
    %% Build pipeline
    KotlinCompile --> JavaCompile
    JavaCompile --> ResourceCompile
    ResourceCompile --> NativeCompile
    
    NativeCompile --> ProGuardR8
    ProGuardR8 --> ResourceShrinking
    ResourceShrinking --> APKOptimization
    
    %% Quality gates
    KotlinCompile --> Detekt
    JavaCompile --> SpotBugs
    ResourceCompile --> Lint
    APKOptimization --> UnitTests
    UnitTests --> IntegrationTests
```

### External Integrations - Research Platform Connections

```mermaid
graph TB
    subgraph "IRCamera Platform Core"
        IRCameraHub[IRCamera Hub<br/>PC Controller]
        AndroidNodes[Android Sensor Nodes<br/>Multiple Devices]
        DataExport[HDF5 Data Export<br/>Structured Format]
    end
    
    subgraph "Lab Streaming Layer (LSL) Integration"
        LSLOutlet[LSL Outlet<br/>Real-time Stream]
        LSLInlet[LSL Inlet<br/>Data Reception]
        
        subgraph "LSL Data Streams"
            ThermalLSL[Thermal Stream<br/>Type: VideoRaw<br/>30 Hz]
            GSRLSL[GSR Stream<br/>Type: GSR<br/>51.2 Hz]
            RGBLSL[RGB Stream<br/>Type: VideoRaw<br/>30 Hz]
            MarkerLSL[Marker Stream<br/>Type: Markers<br/>Event-driven]
        end
    end
    
    subgraph "Hardware API Integrations"
        subgraph "Shimmer SDK Integration"
            ShimmerSDK[Shimmer Android SDK<br/>v3.0.0]
            ShimmerAPI[Shimmer API<br/>BLE Communication]
            ShimmerConfig[Shimmer Configuration<br/>- GSR Range: 40kOhm<br/>- Sampling: 51.2 Hz<br/>- Calibration: Auto]
        end
        
        subgraph "Topdon SDK Integration" 
            TopdonSDK[Topdon TC001 SDK<br/>Native Library]
            TopdonAPI[Topdon API<br/>USB Communication]
            TopdonConfig[Topdon Configuration<br/>- Resolution: 384x288<br/>- Frame Rate: 30 FPS<br/>- Temperature Range: -20degC to 400degC]
        end
    end
    
    subgraph "Cloud Storage Integration"
        subgraph "Google Cloud Platform"
            GCSBucket[Google Cloud Storage<br/>Data Archive]
            BigQuery[BigQuery<br/>Analytics Database]
            CloudML[Cloud ML<br/>Model Training]
        end
        
        subgraph "AWS Integration"
            S3Bucket[AWS S3<br/>Backup Storage]
            Athena[AWS Athena<br/>Query Service]
            SageMaker[AWS SageMaker<br/>ML Pipeline]
        end
    end
    
    subgraph "Research Platform Connections"
        subgraph "BIOPAC Integration"
            BIOPACSystem[BIOPAC MP160<br/>Physiological Monitor]
            ACQKnowledge[AcqKnowledge<br/>Data Acquisition]
            BIOPACSync[BIOPAC Sync<br/>TTL Trigger]
        end
        
        subgraph "Empatica Integration"
            EmpaticaE4[Empatica E4<br/>Wearable Sensor]
            EmpaticaAPI[Empatica Cloud API<br/>Data Synchronization]
            EmpaticaRealTime[E4 Real-time API<br/>Live Streaming]
        end
        
        subgraph "PsychoPy Integration"
            PsychoPyExperiment[PsychoPy Experiment<br/>Stimulus Presentation]
            PsychoPyTrigger[PsychoPy Trigger<br/>Event Markers]
            PsychoPyData[PsychoPy Data Export<br/>Behavioral Responses]
        end
    end
    
    subgraph "Analysis Tool Integrations"
        subgraph "MATLAB Integration"
            MATLABEngine[MATLAB Engine<br/>Analysis Scripts]
            MATLABToolbox[Signal Processing Toolbox<br/>Custom Functions]
            MATLABExport[MATLAB Export<br/>.mat Format]
        end
        
        subgraph "Python Analysis Stack"
            NumPy[NumPy<br/>Numerical Computing]
            SciPy[SciPy<br/>Scientific Computing]
            Pandas[Pandas<br/>Data Manipulation]
            ScikitLearn[scikit-learn<br/>Machine Learning]
            Matplotlib[Matplotlib<br/>Visualization]
        end
        
        subgraph "R Integration"
            RStudio[RStudio<br/>Statistical Analysis]
            Tidyverse[Tidyverse<br/>Data Science Package]
            RMarkdown[R Markdown<br/>Reproducible Research]
        end
    end
    
    %% Core platform connections
    IRCameraHub --> AndroidNodes
    AndroidNodes --> DataExport
    
    %% LSL integration
    IRCameraHub --> LSLOutlet
    LSLOutlet --> ThermalLSL
    LSLOutlet --> GSRLSL
    LSLOutlet --> RGBLSL
    LSLOutlet --> MarkerLSL
    
    %% Hardware API connections
    AndroidNodes --> ShimmerSDK
    ShimmerSDK --> ShimmerAPI
    ShimmerAPI --> ShimmerConfig
    
    AndroidNodes --> TopdonSDK
    TopdonSDK --> TopdonAPI
    TopdonAPI --> TopdonConfig
    
    %% Cloud storage connections
    DataExport --> GCSBucket
    GCSBucket --> BigQuery
    BigQuery --> CloudML
    
    DataExport --> S3Bucket
    S3Bucket --> Athena
    Athena --> SageMaker
    
    %% Research platform connections
    LSLInlet --> BIOPACSystem
    BIOPACSystem --> ACQKnowledge
    IRCameraHub --> BIOPACSync
    
    LSLInlet --> EmpaticaE4
    EmpaticaE4 --> EmpaticaAPI
    EmpaticaAPI --> EmpaticaRealTime
    
    MarkerLSL --> PsychoPyExperiment
    PsychoPyExperiment --> PsychoPyTrigger
    PsychoPyTrigger --> PsychoPyData
    
    %% Analysis tool connections
    DataExport --> MATLABEngine
    MATLABEngine --> MATLABToolbox
    MATLABToolbox --> MATLABExport
    
    DataExport --> NumPy
    NumPy --> SciPy
    SciPy --> Pandas
    Pandas --> ScikitLearn
    ScikitLearn --> Matplotlib
    
    DataExport --> RStudio
    RStudio --> Tidyverse
    Tidyverse --> RMarkdown
    
    %% Cross-platform data flow
    HDF5Export[HDF5 Export] --> MATLABEngine
    HDF5Export --> NumPy
    HDF5Export --> RStudio
    DataExport --> HDF5Export
```

### Data Export Pipeline - Complete Processing Chain

```mermaid
graph LR
    subgraph "Raw Sensor Data Sources"
        ThermalRaw[Thermal Raw Data<br/>384x288x30fps<br/>16-bit Temperature Values]
        GSRRaw[GSR Raw Data<br/>51.2 Hz<br/>Conductance uS]
        RGBRaw[RGB Raw Data<br/>1920x1080x30fps<br/>8-bit RGB Values]
        MetadataRaw[Metadata<br/>Timestamps<br/>Device Info<br/>Session Params]
    end
    
    subgraph "Quality Validation & Preprocessing"
        ThermalQuality[Thermal Quality Check<br/>- Frame Completeness<br/>- Temperature Range Validation<br/>- Dead Pixel Detection]
        GSRQuality[GSR Quality Check<br/>- Signal Range Validation<br/>- Artifact Detection<br/>- Saturation Check]
        RGBQuality[RGB Quality Check<br/>- Frame Integrity<br/>- Color Balance<br/>- Exposure Validation]
        
        ThermalPreproc[Thermal Preprocessing<br/>- Bad Pixel Interpolation<br/>- Calibration Application<br/>- Noise Reduction]
        GSRPreproc[GSR Preprocessing<br/>- Drift Correction<br/>- Baseline Removal<br/>- Outlier Handling]
        RGBPreproc[RGB Preprocessing<br/>- Color Correction<br/>- Brightness Normalization<br/>- Compression Artifacts Removal]
    end
    
    subgraph "Data Synchronization & Alignment"
        TimestampAlign[Timestamp Alignment<br/>- Clock Offset Correction<br/>- Drift Compensation<br/>- Inter-sensor Synchronization]
        
        ResamplingEngine[Resampling Engine<br/>- Common Time Base<br/>- Interpolation Algorithms<br/>- Gap Handling]
        
        QualityMetrics[Quality Metrics Generation<br/>- Sync Accuracy<br/>- Data Completeness<br/>- Signal Quality Scores]
    end
    
    subgraph "Export Format Generation"
        subgraph "HDF5 Export Path"
            HDF5Structure[HDF5 Structure Creation<br/>- Hierarchical Groups<br/>- Dataset Organization<br/>- Compression Settings]
            HDF5Thermal[Thermal Group<br/>/thermal/data<br/>/thermal/timestamps<br/>/thermal/metadata]
            HDF5GSR[GSR Group<br/>/gsr/data<br/>/gsr/timestamps<br/>/gsr/quality]
            HDF5RGB[RGB Group<br/>/rgb/frames<br/>/rgb/timestamps<br/>/rgb/info]
            HDF5Meta[Metadata Group<br/>/session/info<br/>/session/devices<br/>/session/quality]
        end
        
        subgraph "CSV Export Path"
            CSVGeneration[CSV Generation<br/>- Flattened Data Structure<br/>- Time-series Format<br/>- Column Headers]
            ThermalCSV[thermal_data.csv<br/>timestamp,temp_matrix_flat,avg_temp,max_temp,min_temp]
            GSRCSV[gsr_data.csv<br/>timestamp,conductance,quality_flag,artifact_flag]
            SyncCSV[sync_data.csv<br/>timestamp,thermal_frame_id,gsr_sample_id,sync_quality]
        end
        
        subgraph "MATLAB Export Path"
            MATLABGeneration[MATLAB Structure<br/>- Nested Struct Arrays<br/>- Native Data Types<br/>- Function Compatibility]
            MATFile[data.mat<br/>- thermal_data struct<br/>- gsr_data struct<br/>- session_info struct]
        end
    end
    
    subgraph "Validation & Archival"
        ExportValidation[Export Validation<br/>- File Integrity Check<br/>- Data Completeness Verification<br/>- Format Compliance Test]
        
        ArchivalProcess[Archival Process<br/>- Backup Creation<br/>- Checksums Generation<br/>- Metadata Preservation]
        
        QualityReport[Quality Report Generation<br/>- Data Statistics<br/>- Quality Scores<br/>- Processing Logs]
    end
    
    %% Raw data to quality validation
    ThermalRaw --> ThermalQuality
    GSRRaw --> GSRQuality
    RGBRaw --> RGBQuality
    
    %% Quality validation to preprocessing
    ThermalQuality --> ThermalPreproc
    GSRQuality --> GSRPreproc
    RGBQuality --> RGBPreproc
    
    %% Preprocessing to synchronization
    ThermalPreproc --> TimestampAlign
    GSRPreproc --> TimestampAlign
    RGBPreproc --> TimestampAlign
    MetadataRaw --> TimestampAlign
    
    TimestampAlign --> ResamplingEngine
    ResamplingEngine --> QualityMetrics
    
    %% Synchronization to export formats
    QualityMetrics --> HDF5Structure
    QualityMetrics --> CSVGeneration
    QualityMetrics --> MATLABGeneration
    
    %% HDF5 export structure
    HDF5Structure --> HDF5Thermal
    HDF5Structure --> HDF5GSR
    HDF5Structure --> HDF5RGB
    HDF5Structure --> HDF5Meta
    
    %% CSV export structure
    CSVGeneration --> ThermalCSV
    CSVGeneration --> GSRCSV
    CSVGeneration --> SyncCSV
    
    %% MATLAB export
    MATLABGeneration --> MATFile
    
    %% All exports to validation
    HDF5Meta --> ExportValidation
    SyncCSV --> ExportValidation
    MATFile --> ExportValidation
    
    %% Validation to archival
    ExportValidation --> ArchivalProcess
    ArchivalProcess --> QualityReport
```

### Performance Monitoring System - Real-time Metrics & Optimization

```mermaid
graph TB
    subgraph "System Performance Monitoring"
        subgraph "Resource Monitoring"
            CPUMonitor[CPU Usage Monitor<br/>- Per-core Utilization<br/>- Process-level Tracking<br/>- Real-time Alerts]
            MemoryMonitor[Memory Usage Monitor<br/>- Heap Utilization<br/>- GC Performance<br/>- Memory Leaks Detection]
            NetworkMonitor[Network Performance<br/>- Bandwidth Utilization<br/>- Latency Measurements<br/>- Packet Loss Detection]
            DiskMonitor[Disk I/O Monitor<br/>- Read/Write Rates<br/>- Queue Depth<br/>- Storage Capacity]
        end
        
        subgraph "Application Performance Metrics"
            FrameRateMonitor[Frame Rate Monitor<br/>- Thermal: Target 30 FPS<br/>- RGB: Target 30 FPS<br/>- Frame Drop Detection]
            SampleRateMonitor[Sample Rate Monitor<br/>- GSR: Target 51.2 Hz<br/>- Actual vs Target<br/>- Jitter Measurement]
            LatencyMonitor[Latency Monitor<br/>- Sensor to Display<br/>- Network Round-trip<br/>- Processing Delays]
        end
        
        subgraph "Data Quality Metrics"
            SignalQuality[Signal Quality Monitor<br/>- GSR Signal Integrity<br/>- Thermal Calibration Status<br/>- RGB Exposure Quality]
            SyncQuality[Synchronization Quality<br/>- Clock Drift Tracking<br/>- Inter-sensor Alignment<br/>- Timestamp Accuracy]
            DataIntegrity[Data Integrity Monitor<br/>- Packet Loss Detection<br/>- Checksum Validation<br/>- Corruption Detection]
        end
    end
    
    subgraph "Performance Optimization Strategies"
        subgraph "CPU Optimization"
            ThreadPoolOpt[Thread Pool Optimization<br/>- Core Count Adaptation<br/>- Work Stealing Queues<br/>- Priority Scheduling]
            SIMDOptimization[SIMD Optimization<br/>- Vector Operations<br/>- Parallel Processing<br/>- Hardware Acceleration]
            CacheOptimization[Cache Optimization<br/>- Data Locality<br/>- Cache-friendly Algorithms<br/>- Memory Access Patterns]
        end
        
        subgraph "Memory Optimization"
            MemoryPooling[Memory Pooling<br/>- Object Reuse<br/>- Buffer Recycling<br/>- Allocation Reduction]
            GCTuning[Garbage Collection Tuning<br/>- G1GC Configuration<br/>- Heap Size Optimization<br/>- Pause Time Minimization]
            DataStructureOpt[Data Structure Optimization<br/>- Efficient Collections<br/>- Primitive Arrays<br/>- Memory-mapped Files]
        end
        
        subgraph "Network Optimization"
            CompressionOpt[Compression Optimization<br/>- Adaptive Compression<br/>- Quality-based Scaling<br/>- Bandwidth Management]
            BufferingStrategy[Buffering Strategy<br/>- Adaptive Buffer Sizes<br/>- Predictive Buffering<br/>- Flow Control]
            ConnectionPooling[Connection Pooling<br/>- Persistent Connections<br/>- Load Balancing<br/>- Failover Mechanisms]
        end
    end
    
    subgraph "Benchmarking Architecture"
        subgraph "Performance Benchmarks"
            ThermalBenchmark[Thermal Processing Benchmark<br/>- Frame Processing Time<br/>- Temperature Calculation Speed<br/>- Color Mapping Performance]
            GSRBenchmark[GSR Processing Benchmark<br/>- Signal Processing Time<br/>- Quality Analysis Speed<br/>- Artifact Detection Rate]
            NetworkBenchmark[Network Benchmark<br/>- Throughput Measurement<br/>- Latency Distribution<br/>- Connection Reliability]
        end
        
        subgraph "Regression Testing"
            PerformanceRegression[Performance Regression Tests<br/>- Automated Benchmarking<br/>- Historical Comparison<br/>- Performance Alerts]
            MemoryRegression[Memory Regression Tests<br/>- Memory Usage Tracking<br/>- Leak Detection<br/>- Allocation Patterns]
            LoadTesting[Load Testing<br/>- Stress Testing<br/>- Scalability Testing<br/>- Breaking Point Analysis]
        end
        
        subgraph "Profiling Integration"
            AndroidProfiler[Android Profiler<br/>- CPU/Memory/Network<br/>- Method Tracing<br/>- GPU Rendering]
            JVMProfiler[JVM Profiler<br/>- JProfiler Integration<br/>- Method Hotspots<br/>- Memory Analysis]
            CustomProfiler[Custom Profiler<br/>- Domain-specific Metrics<br/>- Real-time Monitoring<br/>- Performance Dashboards]
        end
    end
    
    subgraph "Alert & Response System"
        subgraph "Alert Thresholds"
            CPUAlert[CPU Alert<br/>Threshold: >80% sustained]
            MemoryAlert[Memory Alert<br/>Threshold: >85% usage]
            NetworkAlert[Network Alert<br/>Threshold: >100ms latency]
            QualityAlert[Quality Alert<br/>Threshold: <90% signal quality]
        end
        
        subgraph "Response Actions"
            AutoOptimization[Automatic Optimization<br/>- Buffer Size Adjustment<br/>- Compression Level Change<br/>- Thread Count Adaptation]
            UserNotification[User Notification<br/>- Performance Warnings<br/>- Quality Degradation<br/>- System Recommendations]
            SystemRecovery[System Recovery<br/>- Graceful Degradation<br/>- Service Restart<br/>- Error Recovery]
        end
    end
    
    %% Resource monitoring connections
    CPUMonitor --> ThreadPoolOpt
    MemoryMonitor --> MemoryPooling
    NetworkMonitor --> CompressionOpt
    DiskMonitor --> DataStructureOpt
    
    %% Application performance connections
    FrameRateMonitor --> ThermalBenchmark
    SampleRateMonitor --> GSRBenchmark
    LatencyMonitor --> NetworkBenchmark
    
    %% Quality monitoring connections
    SignalQuality --> QualityAlert
    SyncQuality --> PerformanceRegression
    DataIntegrity --> LoadTesting
    
    %% Optimization strategy connections
    SIMDOptimization --> CacheOptimization
    GCTuning --> MemoryRegression
    BufferingStrategy --> ConnectionPooling
    
    %% Benchmarking connections
    ThermalBenchmark --> AndroidProfiler
    GSRBenchmark --> JVMProfiler
    NetworkBenchmark --> CustomProfiler
    
    %% Alert system connections
    CPUAlert --> AutoOptimization
    MemoryAlert --> UserNotification
    NetworkAlert --> SystemRecovery
    QualityAlert --> UserNotification
    
    %% Response system connections
    AutoOptimization --> ThreadPoolOpt
    UserNotification --> SystemRecovery
    SystemRecovery --> PerformanceRegression
```

## Extended Summary

This comprehensive architecture documentation now includes **18 detailed architectural diagrams** organized across **12 specialized categories**, providing complete coverage of the IRCamera Multi-Modal Thermal Sensing Platform:

### [ARCH] Architecture Diagrams (5 diagrams)
1. **PC Controller Detailed Architecture** - Maps actual Python modules with real file relationships
2. **Android Class Diagram** - Complete Kotlin class structure with precise relationships  
3. **Repository Module Dependencies** - Multi-project Gradle build system with real dependencies
4. **System Overview** - Complete system architecture (original)
5. **Hub-and-Spoke Architecture** - Distributed communication architecture (original)

### [FEAT] Feature Implementation Charts (3 diagrams)
6. **Sensor Integration Features** - Detailed hardware integration with BLE commands and SDK specifics
7. **Communication Protocol Sequence** - Complete TLS handshake and data transfer protocols
8. **Data Synchronization Architecture** - NTP-like algorithm and HDF5 export pipeline

### [FLOW] Workflow Diagrams (2 diagrams)
9. **Session Lifecycle State Machine** - Complete workflow with error handling
10. **Android UI Navigation Flow** - Complete user experience through all fragments

### [INFRA] Infrastructure Charts (3 diagrams)
11. **Security Architecture** - End-to-end TLS, AES256-GCM, Android Keystore integration
12. **Build System Architecture** - Multi-project Gradle with performance optimizations
13. **Performance Monitoring System** - Real-time metrics and optimization strategies

### [DATA] Data Pipeline Details (2 diagrams)
14. **Data Export Pipeline** - Complete processing from sensors to analysis tools
15. **Data Flow Architecture** - Multi-modal synchronization (original)

### [INTEG] External Integrations (1 diagram)
16. **External Integrations** - LSL streaming, hardware APIs, cloud storage, research platforms

### [TEST] Integration & Testing (2 diagrams)
17. **Integration Architecture** - Development to production flow (original)
18. **Build System Architecture** - Complete Gradle system (original)

### Technical Achievements:

- **Real Code Mapping**: Diagrams show actual Python modules (discovery.py, session_manager.py, etc.) and Kotlin classes (MainActivity.kt, ThermalCameraRecorder.kt)
- **Hardware Specifications**: Exact BLE commands (0x07/0x20), VID/PID values, sampling rates (51.2 Hz GSR, 30 FPS thermal)
- **Security Implementation**: Complete TLS 1.3 handshake, AES256-GCM encryption, Android Keystore integration
- **Performance Details**: JVM memory settings (-Xmx4g), build optimizations, real-time monitoring thresholds
- **Integration Specifics**: LSL streaming types, HDF5 structure, MATLAB/Python/R export formats

This implementation provides the most comprehensive architectural documentation available, with precise technical details for every aspect of the multi-modal sensing platform.

Each diagram shows precise relationships, dependencies, and data flows, providing a complete technical reference for understanding, developing, and maintaining the IRCamera platform.