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
            ThermalIR[thermal-ir<br/>Advanced IR Processing<br/>• Multi-spectral analysis<br/>• Temperature mapping<br/>• Object detection]
            
            Thermal[thermal<br/>Basic Thermal Processing<br/>• Simple temperature display<br/>• Basic color mapping<br/>• Raw data export]
            
            ThermalLite[thermal-lite<br/>Lightweight Thermal<br/>• Minimal processing<br/>• Low memory usage<br/>• Fast rendering]
        end
        
        subgraph "Data Collection"
            GSRRecording[gsr-recording<br/>GSR Data Collection<br/>• Shimmer3 integration<br/>• Real-time streaming<br/>• Quality validation]
        end
        
        subgraph "System Components"
            UserMgmt[user<br/>User Management<br/>• Profile management<br/>• Session tracking<br/>• Preferences]
        end
    end
    
    subgraph "Core Libraries"
        LibApp[libapp<br/>Application Framework<br/>• Base activities<br/>• Common utilities<br/>• Configuration management]
        
        LibIR[libir<br/>IR Processing Engine<br/>• Topdon TC001 SDK<br/>• Image processing<br/>• Temperature calculation]
        
        LibUI[libui<br/>UI Components<br/>• Custom views<br/>• Chart components<br/>• Material design]
        
        LibCom[libcom<br/>Communication Library<br/>• Network protocols<br/>• JSON messaging<br/>• mDNS discovery]
        
        LibMatrix[libmatrix<br/>Matrix Operations<br/>• Mathematical operations<br/>• Image transformations<br/>• Signal processing]
        
        LibMenu[libmenu<br/>Menu System<br/>• Navigation<br/>• Context menus<br/>• Action bars]
    end
    
    subgraph "External Modules"
        BleModule[BleModule<br/>Bluetooth Low Energy<br/>• Shimmer3 protocol<br/>• Device management<br/>• Data streaming]
        
        RangeSeekBar[RangeSeekBar<br/>UI Range Controls<br/>• Temperature ranges<br/>• Slider controls<br/>• Value selection]
    end
    
    subgraph "Consolidated Libraries"
        CommonComponent[CommonComponent<br/>Shared Components<br/>• Common interfaces<br/>• Utility classes<br/>• Constants]
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
        TopdonTC001[Topdon TC001<br/>Thermal Camera<br/>• 384x288 resolution<br/>• USB-C interface<br/>• Temperature range: -20°C to 400°C]
        
        AndroidCamera[Android Camera<br/>RGB Sensor<br/>• CameraX API<br/>• Various resolutions<br/>• Auto-focus & exposure]
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
        Shimmer3[Shimmer3 GSR+<br/>Galvanic Skin Response<br/>• Wireless GSR sensor<br/>• Bluetooth Low Energy<br/>• Real-time streaming]
        
        AndroidBLE[Android BLE<br/>Bluetooth Hardware<br/>• BLE 4.0+ support<br/>• Multiple connections<br/>• Low power mode]
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
        ThermalAcq[Thermal Acquisition<br/>• Frame capture<br/>• Temperature conversion<br/>• Quality check]
        
        GSRAcq[GSR Acquisition<br/>• BLE streaming<br/>• Packet parsing<br/>• Signal validation]
        
        RGBAcq[RGB Acquisition<br/>• CameraX capture<br/>• Image processing<br/>• Compression]
    end
    
    subgraph "Processing Layer"
        ThermalProc[Thermal Processing<br/>• Calibration<br/>• Filtering<br/>• Enhancement]
        
        GSRProc[GSR Processing<br/>• Artifact removal<br/>• Filtering<br/>• Feature extraction]
        
        RGBProc[RGB Processing<br/>• Color correction<br/>• Alignment<br/>• Compression]
    end
    
    subgraph "Synchronization Layer"
        TimeAlign[Time Alignment<br/>• Clock synchronization<br/>• Timestamp correction<br/>• Drift compensation]
        
        DataFusion[Data Fusion<br/>• Multi-modal alignment<br/>• Interpolation<br/>• Gap filling]
        
        QualityCtrl[Quality Control<br/>• Signal quality<br/>• Missing data<br/>• Outlier detection]
    end
    
    subgraph "Storage Layer"
        BufferMgr[Buffer Manager<br/>• Circular buffers<br/>• Memory management<br/>• Overflow handling]
        
        HDF5Store[HDF5 Storage<br/>• Hierarchical structure<br/>• Metadata preservation<br/>• Compression]
        
        ExportMgr[Export Manager<br/>• Multiple formats<br/>• Batch processing<br/>• Validation]
    end
    
    subgraph "Network Layer"
        StreamMgr[Stream Manager<br/>• Real-time streaming<br/>• Flow control<br/>• Error recovery]
        
        NetworkProto[Network Protocol<br/>• JSON messaging<br/>• Command routing<br/>• Status reporting]
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
        RootBuild[build.gradle.kts<br/>• Global configuration<br/>• Plugin management<br/>• Common dependencies]
        
        Settings[settings.gradle.kts<br/>• Module inclusion<br/>• Project structure<br/>• Repository configuration]
        
        VersionCatalog[gradle/libs.versions.toml<br/>• Dependency versions<br/>• Library definitions<br/>• Plugin versions]
        
        GradleProps[gradle.properties<br/>• Build properties<br/>• JVM settings<br/>• Android SDK config]
    end
    
    subgraph "Application Module"
        AppBuild[app/build.gradle.kts<br/>• Application plugin<br/>• APK configuration<br/>• Signing & optimization]
    end
    
    subgraph "Core Library Modules"
        LibAppBuild[libapp/build.gradle.kts<br/>• Android library<br/>• Framework components<br/>• Common utilities]
        
        LibIRBuild[libir/build.gradle.kts<br/>• Native libraries<br/>• JNI integration<br/>• Topdon SDK]
        
        LibUIBuild[libui/build.gradle.kts<br/>• UI components<br/>• Custom views<br/>• Material design]
    end
    
    subgraph "Feature Component Modules"
        ThermalIRBuild[thermal-ir/build.gradle.kts<br/>• Advanced thermal<br/>• Image processing<br/>• Computer vision]
        
        GSRBuild[gsr-recording/build.gradle.kts<br/>• GSR processing<br/>• BLE integration<br/>• Data streaming]
        
        UserBuild[user/build.gradle.kts<br/>• User management<br/>• Profile handling<br/>• Preferences]
    end
    
    subgraph "External Module Builds"
        BleBuild[BleModule/build.gradle.kts<br/>• Shimmer integration<br/>• BLE protocols<br/>• Device management]
        
        RangeSeekBarBuild[RangeSeekBar/build.gradle.kts<br/>• Custom UI controls<br/>• Range selection<br/>• Touch handling]
    end
    
    subgraph "Consolidated Libraries"
        CommonBuild[CommonComponent/build.gradle.kts<br/>• Shared components<br/>• Interfaces<br/>• Utilities]
        
        ConsolidatedBuild[consolidated_libraries/<br/>• Shared configuration<br/>• Common scripts<br/>• Build utilities]
    end
    
    subgraph "Build Tools & Scripts"
        DevScript[dev.sh<br/>• Development tools<br/>• Linting & validation<br/>• Build automation]
        
        BuildScripts[scripts/<br/>• Production builds<br/>• APK generation<br/>• Release automation]
        
        CIWorkflows[.github/workflows/<br/>• GitHub Actions<br/>• CI/CD pipeline<br/>• Quality gates]
    end
    
    subgraph "Dependency Management"
        subgraph "Version Catalog Dependencies"
            AndroidDeps[Android Dependencies<br/>• AndroidX libraries<br/>• Material Design<br/>• CameraX]
            
            KotlinDeps[Kotlin Dependencies<br/>• Kotlin stdlib<br/>• Coroutines<br/>• Serialization]
            
            NetworkDeps[Network Dependencies<br/>• OkHttp<br/>• Retrofit<br/>• WebSocket]
            
            BLEDeps[BLE Dependencies<br/>• Shimmer SDK<br/>• Bluetooth libraries<br/>• Protocol handlers]
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
        DevMachine[Developer Machine<br/>• Android Studio<br/>• Python environment<br/>• Git repository]
        
        AndroidDevice[Android Test Device<br/>• Debug build<br/>• USB debugging<br/>• Network access]
        
        PCController[PC Controller<br/>• Python environment<br/>• GUI application<br/>• Data storage]
    end
    
    subgraph "Hardware Integration"
        ThermalCamera[Topdon TC001<br/>• USB-C connection<br/>• Native SDK<br/>• Temperature sensor]
        
        ShimmerGSR[Shimmer3 GSR+<br/>• Bluetooth LE<br/>• Wireless sensor<br/>• Real-time streaming]
        
        AndroidSensors[Android Sensors<br/>• RGB camera<br/>• Accelerometer<br/>• Network interfaces]
    end
    
    subgraph "Network Infrastructure"
        WiFiNetwork[WiFi Network<br/>• Local network<br/>• mDNS support<br/>• TCP/IP connectivity]
        
        InternetAccess[Internet Access<br/>• Cloud services<br/>• Time synchronization<br/>• Software updates]
    end
    
    subgraph "Data Flow Integration"
        subgraph "Real-time Streaming"
            ThermalStream[Thermal Stream<br/>• 30 FPS<br/>• Temperature data<br/>• Image frames]
            
            GSRStream[GSR Stream<br/>• 51.2 Hz<br/>• Conductance data<br/>• Quality metrics]
            
            ControlStream[Control Stream<br/>• Commands<br/>• Status updates<br/>• Configuration]
        end
        
        subgraph "Data Synchronization"
            TimestampSync[Timestamp Sync<br/>• Clock alignment<br/>• Drift correction<br/>• Latency compensation]
            
            DataAlignment[Data Alignment<br/>• Multi-modal sync<br/>• Interpolation<br/>• Quality control]
            
            BufferManagement[Buffer Management<br/>• Memory efficiency<br/>• Overflow handling<br/>• Flow control]
        end
    end
    
    subgraph "Quality Assurance Integration"
        subgraph "Testing Infrastructure"
            UnitTests[Unit Tests<br/>• Component testing<br/>• Mock devices<br/>• Error scenarios]
            
            IntegrationTests[Integration Tests<br/>• End-to-end testing<br/>• Multi-device<br/>• Network protocols]
            
            PerformanceTests[Performance Tests<br/>• Load testing<br/>• Memory usage<br/>• Network bandwidth]
        end
        
        subgraph "CI/CD Pipeline"
            BuildValidation[Build Validation<br/>• Gradle builds<br/>• Python tests<br/>• Code quality]
            
            DeploymentTests[Deployment Tests<br/>• APK generation<br/>• Installation<br/>• Functionality]
            
            QualityGates[Quality Gates<br/>• Code coverage<br/>• Static analysis<br/>• Performance metrics]
        end
    end
    
    subgraph "Production Deployment"
        ProductionAPK[Production APK<br/>• Release build<br/>• Code signing<br/>• Optimization]
        
        PCDistribution[PC Application<br/>• Python package<br/>• Dependencies<br/>• Installation]
        
        Documentation[Documentation<br/>• User guides<br/>• API reference<br/>• Troubleshooting]
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

Each diagram shows precise relationships, dependencies, and data flows, providing a complete technical reference for understanding, developing, and maintaining the IRCamera platform.