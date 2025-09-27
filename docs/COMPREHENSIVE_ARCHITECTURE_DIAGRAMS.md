# IRCamera Comprehensive Architecture Diagrams

This document provides precise Mermaid diagrams for each feature, module, and architectural aspect of the IRCamera Multi-Modal Thermal Sensing Platform, created fresh based on actual codebase analysis.

## Table of Contents

1. [System Overview](#system-overview)
2. [Hub-and-Spoke Architecture](#hub-and-spoke-architecture)
3. [Android Module Architecture](#android-module-architecture)
4. [PC Controller Architecture](#pc-controller-architecture)
5. [Android Class Structure](#android-class-structure)
6. [Thermal Processing Pipeline](#thermal-processing-pipeline)
7. [GSR Recording System](#gsr-recording-system)
8. [Build System Architecture](#build-system-architecture)
9. [Data Flow Architecture](#data-flow-architecture)
10. [Network Communication](#network-communication)
11. [Security Architecture](#security-architecture)
12. [Testing Framework](#testing-framework)

---

## System Overview

### Complete Multi-Modal Sensing Platform

```mermaid
graph TB
    subgraph "Hardware Layer"
        TC001[Topdon TC001<br/>Thermal Camera<br/>USB-C Interface]
        Shimmer3[Shimmer3 GSR+<br/>Wireless GSR Sensor<br/>Bluetooth 5.0]
        AndroidCam[Android Camera<br/>RGB/Depth Sensors]
    end
    
    subgraph "Android Sensor Nodes"
        subgraph "Main Application"
            MainActivity[MainActivity.kt<br/>Entry Point & Coordinator]
            ThermalUI[Thermal Interface<br/>Real-time Display]
            GSRInterface[GSR Interface<br/>Data Monitoring]
        end
        
        subgraph "Feature Components"
            ThermalIR[thermal-ir<br/>Advanced Thermal Processing]
            ThermalBasic[thermal<br/>Basic Thermal Processing]
            ThermalLite[thermal-lite<br/>Lightweight Thermal]
            GSRRecording[gsr-recording<br/>GSR Data Collection]
            UserMgmt[user<br/>User Management]
        end
        
        subgraph "Core Libraries"
            LibApp[libapp<br/>Application Framework]
            LibIR[libir<br/>IR Processing Engine]
            LibUI[libui<br/>UI Components Library]
        end
        
        subgraph "External Modules"
            BleModule[BleModule<br/>Bluetooth LE Interface]
            RangeSeekBar[RangeSeekBar<br/>Custom UI Control]
        end
    end
    
    subgraph "PC Controller Hub"
        subgraph "Entry Points"
            MVPSimple[mvp_simple.py<br/>Minimal Implementation]
            MainController[Main Controller<br/>Full Application]
        end
        
        subgraph "Core Services"
            Discovery[Network Discovery<br/>mDNS Service]
            Server[TCP Server<br/>Multi-client Handler]
            DataProcessor[Data Processing<br/>Multi-modal Sync]
        end
        
        subgraph "GUI Interface"
            PyQt6GUI[PyQt6 Interface<br/>Real-time Dashboard]
            Visualization[Data Visualization<br/>Thermal & GSR Plots]
        end
    end
    
    subgraph "Data Storage"
        HDF5[HDF5 Files<br/>Structured Storage]
        CSV[CSV Export<br/>Analysis Ready]
        RealTime[Real-time Stream<br/>Research Tools]
    end
    
    %% Hardware connections
    TC001 --> LibIR
    Shimmer3 --> BleModule
    AndroidCam --> LibApp
    
    %% Android internal connections
    MainActivity --> ThermalIR
    MainActivity --> GSRRecording
    ThermalIR --> LibIR
    GSRRecording --> BleModule
    LibApp --> LibUI
    
    %% Network communication
    MainActivity -.->|TCP/JSON| Server
    Discovery -.->|mDNS| MainActivity
    
    %% Data flow
    Server --> DataProcessor
    DataProcessor --> HDF5
    DataProcessor --> CSV
    PyQt6GUI --> Visualization
    
    %% Real-time connections
    DataProcessor -.->|Live Stream| RealTime
```

## Hub-and-Spoke Architecture

### Distributed Multi-Device Communication

```mermaid
graph TB
    subgraph "Central Hub - PC Controller"
        subgraph "Hub Core Services"
            DeviceDiscovery[Device Discovery Service<br/>mDNS Broadcaster]
            SessionManager[Session Manager<br/>Lifecycle Control]
            DataAggregator[Data Aggregator<br/>Multi-modal Sync]
            NetworkHub[Network Hub<br/>TCP Server Multi-client]
        end
        
        subgraph "Hub Processing"
            TimeSync[Time Synchronization<br/>NTP-like Protocol]
            DataValidator[Data Validator<br/>Quality Control]
            ExportEngine[Export Engine<br/>HDF5/CSV/MATLAB]
        end
        
        subgraph "Hub Interface"
            WebInterface[Web Dashboard<br/>Remote Control]
            LocalGUI[Local GUI<br/>PyQt6 Interface]
            APIServer[REST API<br/>External Integration]
        end
    end
    
    subgraph "Spoke 1 - Android Node A"
        AndroidA[Android Application A]
        ThermalA[Thermal Camera A<br/>Topdon TC001]
        GSRA[GSR Sensor A<br/>Shimmer3]
        RGBA[RGB Camera A<br/>Android Camera]
    end
    
    subgraph "Spoke 2 - Android Node B"
        AndroidB[Android Application B]
        ThermalB[Thermal Camera B<br/>Topdon TC001]
        GSRB[GSR Sensor B<br/>Shimmer3]
        RGBB[RGB Camera B<br/>Android Camera]
    end
    
    subgraph "Spoke N - Android Node N"
        AndroidN[Android Application N]
        ThermalN[Thermal Camera N<br/>Topdon TC001]
        GSRN[GSR Sensor N<br/>Shimmer3]
        RGBN[RGB Camera N<br/>Android Camera]
    end
    
    subgraph "External Systems"
        CloudStorage[Cloud Storage<br/>Google Cloud/AWS]
        AnalysisTools[Analysis Tools<br/>MATLAB/Python/R]
        LSLStream[Lab Streaming Layer<br/>Real-time Research]
    end
    
    %% Hub internal connections
    DeviceDiscovery --> SessionManager
    SessionManager --> DataAggregator
    DataAggregator --> NetworkHub
    NetworkHub --> TimeSync
    TimeSync --> DataValidator
    DataValidator --> ExportEngine
    
    %% Hub interfaces
    SessionManager --> WebInterface
    DataAggregator --> LocalGUI
    NetworkHub --> APIServer
    
    %% Spoke connections to hub
    AndroidA <-->|JSON/TCP| NetworkHub
    AndroidB <-->|JSON/TCP| NetworkHub
    AndroidN <-->|JSON/TCP| NetworkHub
    
    %% Spoke internal connections
    AndroidA --> ThermalA
    AndroidA --> GSRA
    AndroidA --> RGBA
    
    AndroidB --> ThermalB
    AndroidB --> GSRB
    AndroidB --> RGBB
    
    AndroidN --> ThermalN
    AndroidN --> GSRN
    AndroidN --> RGBN
    
    %% External integrations
    ExportEngine --> CloudStorage
    ExportEngine --> AnalysisTools
    DataAggregator --> LSLStream
    
    %% Discovery broadcasts
    DeviceDiscovery -.->|mDNS Broadcast| AndroidA
    DeviceDiscovery -.->|mDNS Broadcast| AndroidB
    DeviceDiscovery -.->|mDNS Broadcast| AndroidN
```

## Android Module Architecture

### Complete Gradle Multi-Module System

```mermaid
graph TB
    subgraph "Root Project Configuration"
        RootBuild[build.gradle.kts<br/>Root Configuration]
        Settings[settings.gradle.kts<br/>Module Registry]
        VersionCatalog[gradle/libs.versions.toml<br/>Dependency Management]
    end
    
    subgraph "Application Module"
        AppModule[app<br/>Main Android Application<br/>com.mpdc4gsr]
    end
    
    subgraph "Feature Components"
        ThermalIRModule[thermal-ir<br/>Advanced Thermal Processing<br/>Topdon TC001 Integration]
        ThermalModule[thermal<br/>Basic Thermal Processing<br/>Core Functionality]
        ThermalLiteModule[thermal-lite<br/>Lightweight Thermal<br/>Reduced Features]
        GSRModule[gsr-recording<br/>GSR Data Collection<br/>Shimmer3 Integration]
        UserModule[user<br/>User Management<br/>Authentication & Profiles]
    end
    
    subgraph "Core Libraries"
        LibAppModule[libapp<br/>Application Framework<br/>Base Classes & Utilities]
        LibIRModule[libir<br/>IR Processing Engine<br/>Temperature Algorithms]
        LibUIModule[libui<br/>UI Component Library<br/>Custom Views & Widgets]
    end
    
    subgraph "External Dependencies"
        BleModuleExt[BleModule<br/>Bluetooth LE Interface<br/>Shimmer SDK Integration]
        RangeSeekBarExt[RangeSeekBar<br/>Custom Slider Control<br/>Temperature Range Selection]
    end
    
    subgraph "Consolidated Libraries"
        CommonComponent[CommonComponent<br/>Shared Components<br/>Cross-module Utilities]
    end
    
    %% Root configuration connections
    RootBuild --> Settings
    Settings --> VersionCatalog
    
    %% Application dependencies
    AppModule --> LibAppModule
    AppModule --> LibUIModule
    AppModule --> ThermalIRModule
    AppModule --> GSRModule
    AppModule --> UserModule
    AppModule --> BleModuleExt
    
    %% Feature component dependencies
    ThermalIRModule --> LibIRModule
    ThermalIRModule --> LibUIModule
    ThermalModule --> LibIRModule
    ThermalLiteModule --> LibIRModule
    GSRModule --> BleModuleExt
    GSRModule --> LibAppModule
    UserModule --> LibAppModule
    
    %% Core library dependencies
    LibAppModule --> CommonComponent
    LibUIModule --> CommonComponent
    LibUIModule --> RangeSeekBarExt
    
    %% Build system dependencies
    RootBuild --> AppModule
    RootBuild --> ThermalIRModule
    RootBuild --> ThermalModule
    RootBuild --> ThermalLiteModule
    RootBuild --> GSRModule
    RootBuild --> UserModule
    RootBuild --> LibAppModule
    RootBuild --> LibIRModule
    RootBuild --> LibUIModule
    RootBuild --> BleModuleExt
    RootBuild --> RangeSeekBarExt
    RootBuild --> CommonComponent
```

## PC Controller Architecture

### Python Implementation Structure

```mermaid
graph TB
    subgraph "Entry Points"
        MVPSimple[mvp_simple.py<br/>~250 lines<br/>Minimal Implementation]
        SetupPy[setup.py<br/>Package Configuration<br/>Dependencies & Installation]
    end
    
    subgraph "Legacy Implementation (ircamera_pc/)"
        subgraph "Core Services (core/)"
            SessionMgr[session_manager.py<br/>Session Lifecycle<br/>Device Coordination]
            DeviceMgr[device_manager.py<br/>Hardware Management<br/>Device Registry]
            ConfigMgr[config.py<br/>Configuration Management<br/>Settings & Preferences]
        end
        
        subgraph "Network Layer (network/)"
            Discovery[discovery.py<br/>mDNS Service Discovery<br/>Device Advertisement]
            Server[server.py<br/>TCP Server<br/>Multi-client Handler]
            Messaging[messaging.py<br/>Message Protocol<br/>JSON Communication]
            Security[security.py<br/>Encryption & Auth<br/>TLS Implementation]
            SecurityMgr[security_manager.py<br/>Security Coordinator<br/>Certificate Management]
            Protocol[protocol.py<br/>Communication Protocol<br/>Message Definitions]
            WebSocketSvr[websocket_server.py<br/>WebSocket Support<br/>Real-time Communication]
        end
        
        subgraph "Data Processing (data/)"
            Processing[processing.py<br/>Data Pipeline<br/>Multi-modal Processing]
            HDF5Export[hdf5_exporter.py<br/>HDF5 Storage<br/>Hierarchical Data Format]
        end
        
        subgraph "GUI Layer (gui/)"
            GUIApp[app.py<br/>Main GUI Application<br/>PyQt6 Interface]
            MainWindow[main_window.py<br/>Primary Window<br/>Dashboard Interface]
            Widgets[widgets.py<br/>Custom UI Components<br/>Thermal & GSR Displays]
            Utils[utils.py<br/>GUI Utilities<br/>Helper Functions]
        end
        
        subgraph "Synchronization (sync/)"
            TimeSyncSvc[timesync_service.py<br/>Time Synchronization<br/>NTP-like Protocol]
        end
        
        subgraph "Utilities (utils/)"
            SimpleLogger[simple_logger.py<br/>Logging System<br/>Debug & Info Logging]
        end
        
        subgraph "Examples (examples/)"
            HubSpokeDemo[hub_spoke_demo.py<br/>Hub-Spoke Demonstration<br/>Multi-device Example]
        end
    end
    
    %% Entry point connections
    MVPSimple --> Discovery
    MVPSimple --> Server
    SetupPy --> GUIApp
    
    %% Core service connections
    SessionMgr --> DeviceMgr
    SessionMgr --> ConfigMgr
    DeviceMgr --> Discovery
    
    %% Network layer connections
    Discovery --> Server
    Server --> Messaging
    Server --> Security
    Messaging --> Protocol
    Security --> SecurityMgr
    Server --> WebSocketSvr
    
    %% Data processing connections
    Server --> Processing
    Processing --> HDF5Export
    SessionMgr --> Processing
    
    %% GUI connections
    GUIApp --> MainWindow
    MainWindow --> Widgets
    Widgets --> Utils
    SessionMgr --> GUIApp
    Processing --> MainWindow
    
    %% Synchronization connections
    TimeSyncSvc --> Server
    Protocol --> TimeSyncSvc
    
    %% Utility connections
    SimpleLogger --> SessionMgr
    SimpleLogger --> Server
    SimpleLogger --> Processing
    
    %% Example connections
    HubSpokeDemo --> Discovery
    HubSpokeDemo --> SessionMgr
```

## Android Class Structure

### Key Application Classes

```mermaid
classDiagram
    class MainActivity {
        +onCreate() void
        +onDestroy() void
        +initializeComponents() void
        +setupPermissions() void
        +handleDeviceConnections() void
        -fragmentManager: FragmentManager
        -permissionHandler: PermissionHandler
        -deviceRegistry: DeviceRegistry
    }
    
    class ThermalCameraHandler {
        +connectCamera() boolean
        +startRecording() void
        +stopRecording() void
        +processFrame(frame: ByteArray) void
        +getTemperatureData() TemperatureData
        -ircamEngine: IrcamEngine
        -uvcCamera: UVCCamera
        -frameProcessor: FrameProcessor
    }
    
    class GSRDataCollector {
        +connectShimmer() boolean
        +startDataCollection() void
        +stopDataCollection() void
        +processGSRData(data: ByteArray) void
        +getSignalQuality() SignalQuality
        -shimmerDevice: ShimmerDevice
        -bleManager: BluetoothManager
        -dataBuffer: CircularBuffer
    }
    
    class NetworkClient {
        +connectToHub(address: String) boolean
        +sendData(data: SensorData) void
        +receiveCommands() Command
        +handleHeartbeat() void
        -tcpSocket: Socket
        -jsonProtocol: JSONProtocol
        -messageQueue: MessageQueue
    }
    
    class DeviceManager {
        +registerDevice(device: SensorDevice) void
        +getDeviceStatus(id: String) DeviceStatus
        +synchronizeDevices() void
        +handleDeviceError(error: DeviceError) void
        -connectedDevices: Map~String, SensorDevice~
        -deviceStatusMonitor: StatusMonitor
    }
    
    class DataSynchronizer {
        +alignTimestamps(data: List~SensorData~) List~SensorData~
        +synchronizeStreams() void
        +calculateClockOffset() long
        +validateDataIntegrity() boolean
        -timeOffset: long
        -syncAccuracy: double
        -dataValidator: DataValidator
    }
    
    class UIController {
        +updateThermalDisplay(frame: ThermalFrame) void
        +updateGSRPlot(data: GSRData) void
        +showDeviceStatus(status: DeviceStatus) void
        +handleUserInput(input: UserInput) void
        -thermalView: ThermalImageView
        -gsrPlotView: GSRPlotView
        -statusIndicator: StatusIndicator
    }
    
    MainActivity --> ThermalCameraHandler
    MainActivity --> GSRDataCollector
    MainActivity --> NetworkClient
    MainActivity --> DeviceManager
    
    ThermalCameraHandler --> DataSynchronizer
    GSRDataCollector --> DataSynchronizer
    NetworkClient --> DataSynchronizer
    
    DeviceManager --> UIController
    DataSynchronizer --> UIController
    
    NetworkClient --> DeviceManager
```

## Thermal Processing Pipeline

### Complete IR Processing Chain

```mermaid
graph TB
    subgraph "Hardware Interface"
        TC001Device[Topdon TC001<br/>Thermal Camera<br/>USB-C Connection]
        USBInterface[USB Interface<br/>UVC Driver<br/>Device Detection]
    end
    
    subgraph "Low-Level Processing (libir)"
        subgraph "Camera Control"
            CameraInit[Camera Initialization<br/>Device Enumeration<br/>Capability Detection]
            CameraConfig[Camera Configuration<br/>Resolution: 384x288<br/>Frame Rate: 30 FPS]
            FrameCapture[Frame Capture<br/>Raw Thermal Data<br/>14-bit Resolution]
        end
        
        subgraph "Temperature Processing"
            RawProcessor[Raw Data Processor<br/>Bit Depth Conversion<br/>Noise Reduction]
            TempCalculator[Temperature Calculator<br/>Radiometric Conversion<br/>Calibration Application]
            TempRange[Temperature Range<br/>-20°C to +400°C<br/>Accuracy: ±2°C]
        end
        
        subgraph "Image Processing"
            ImageEnhancer[Image Enhancement<br/>Contrast Adjustment<br/>Histogram Equalization]
            ColorMapper[Color Mapping<br/>Palette Application<br/>Rainbow/Iron/Gray]
            ImageScaler[Image Scaling<br/>Resolution Adjustment<br/>Interpolation]
        end
    end
    
    subgraph "Feature Components"
        subgraph "thermal-ir (Advanced)"
            AdvancedProcessor[Advanced Processing<br/>Multi-point Analysis<br/>Thermal Patterns]
            RegionAnalyzer[Region Analyzer<br/>Temperature Zones<br/>Statistical Analysis]
            AlertSystem[Alert System<br/>Temperature Thresholds<br/>Anomaly Detection]
        end
        
        subgraph "thermal (Basic)"
            BasicProcessor[Basic Processing<br/>Single Point Analysis<br/>Center Temperature]
            SimpleDisplay[Simple Display<br/>Basic Visualization<br/>Temperature Overlay]
        end
        
        subgraph "thermal-lite (Minimal)"
            LiteProcessor[Lite Processing<br/>Minimal Features<br/>Low Resource Usage]
            QuickDisplay[Quick Display<br/>Fast Rendering<br/>Reduced Quality]
        end
    end
    
    subgraph "UI Integration (libui)"
        ThermalImageView[Thermal Image View<br/>Custom Android View<br/>Touch Interaction]
        TemperatureDisplay[Temperature Display<br/>Numeric Values<br/>Real-time Updates]
        ControlPanel[Control Panel<br/>Settings Interface<br/>Calibration Controls]
    end
    
    subgraph "Data Output"
        NetworkStream[Network Stream<br/>TCP/JSON Protocol<br/>Real-time Transmission]
        LocalStorage[Local Storage<br/>CSV Files<br/>Image Sequences]
        AnalysisExport[Analysis Export<br/>Structured Data<br/>Research Format]
    end
    
    %% Hardware to low-level processing
    TC001Device --> USBInterface
    USBInterface --> CameraInit
    CameraInit --> CameraConfig
    CameraConfig --> FrameCapture
    
    %% Low-level processing chain
    FrameCapture --> RawProcessor
    RawProcessor --> TempCalculator
    TempCalculator --> TempRange
    TempRange --> ImageEnhancer
    ImageEnhancer --> ColorMapper
    ColorMapper --> ImageScaler
    
    %% Feature component processing
    ImageScaler --> AdvancedProcessor
    ImageScaler --> BasicProcessor
    ImageScaler --> LiteProcessor
    
    AdvancedProcessor --> RegionAnalyzer
    RegionAnalyzer --> AlertSystem
    
    %% UI integration
    AdvancedProcessor --> ThermalImageView
    BasicProcessor --> ThermalImageView
    LiteProcessor --> ThermalImageView
    
    TempCalculator --> TemperatureDisplay
    AlertSystem --> ControlPanel
    
    %% Data output
    AdvancedProcessor --> NetworkStream
    RegionAnalyzer --> LocalStorage
    TempCalculator --> AnalysisExport
```

## GSR Recording System

### Shimmer3 Integration Architecture

```mermaid
graph TB
    subgraph "Hardware Layer"
        Shimmer3HW[Shimmer3 GSR+<br/>Wireless Sensor<br/>Bluetooth 5.0 LE]
        GSRElectrodes[GSR Electrodes<br/>Skin Conductance<br/>Ag/AgCl Electrodes]
    end
    
    subgraph "Bluetooth Communication (BleModule)"
        subgraph "BLE Stack"
            AndroidBLE[Android BLE Stack<br/>BluetoothManager<br/>GATT Services]
            BLEScanner[BLE Scanner<br/>Device Discovery<br/>Service Advertisement]
            GATTClient[GATT Client<br/>Service Connection<br/>Characteristic Access]
        end
        
        subgraph "Shimmer Protocol"
            DevicePairing[Device Pairing<br/>Authentication<br/>Secure Connection]
            CommandInterface[Command Interface<br/>Control Messages<br/>Configuration]
            DataStream[Data Stream<br/>GSR Measurements<br/>Continuous Sampling]
        end
    end
    
    subgraph "GSR Data Processing (gsr-recording)"
        subgraph "Data Acquisition"
            SampleCollector[Sample Collector<br/>51.2 Hz Sampling<br/>Timestamp Synchronization]
            DataValidator[Data Validator<br/>Quality Assessment<br/>Artifact Detection]
            BufferManager[Buffer Manager<br/>Circular Buffer<br/>Overflow Protection]
        end
        
        subgraph "Signal Processing"
            SignalProcessor[Signal Processor<br/>Digital Filtering<br/>Noise Reduction]
            ArtifactRemover[Artifact Remover<br/>Motion Detection<br/>Signal Cleaning]
            FeatureExtractor[Feature Extractor<br/>SCL/SCR Analysis<br/>Peak Detection]
        end
        
        subgraph "Data Analysis"
            StatisticalAnalyzer[Statistical Analyzer<br/>Mean/Variance<br/>Trend Analysis]
            ResponseDetector[Response Detector<br/>GSR Events<br/>Amplitude Thresholds]
            QualityMetrics[Quality Metrics<br/>Signal-to-Noise<br/>Electrode Contact]
        end
    end
    
    subgraph "Application Integration"
        GSRInterface[GSR Interface<br/>Real-time Display<br/>Control Panel]
        DataLogger[Data Logger<br/>CSV Export<br/>Session Recording]
        NetworkTransmit[Network Transmit<br/>Hub Communication<br/>Real-time Stream]
    end
    
    subgraph "Synchronization & Storage"
        TimestampManager[Timestamp Manager<br/>Clock Synchronization<br/>Multi-device Alignment]
        LocalDatabase[Local Database<br/>SQLite Storage<br/>Session Management]
        CloudSync[Cloud Sync<br/>Remote Backup<br/>Analysis Ready]
    end
    
    %% Hardware connections
    Shimmer3HW --> GSRElectrodes
    Shimmer3HW --> AndroidBLE
    
    %% BLE communication flow
    AndroidBLE --> BLEScanner
    BLEScanner --> GATTClient
    GATTClient --> DevicePairing
    DevicePairing --> CommandInterface
    CommandInterface --> DataStream
    
    %% Data acquisition flow
    DataStream --> SampleCollector
    SampleCollector --> DataValidator
    DataValidator --> BufferManager
    
    %% Signal processing flow
    BufferManager --> SignalProcessor
    SignalProcessor --> ArtifactRemover
    ArtifactRemover --> FeatureExtractor
    
    %% Data analysis flow
    FeatureExtractor --> StatisticalAnalyzer
    StatisticalAnalyzer --> ResponseDetector
    ResponseDetector --> QualityMetrics
    
    %% Application integration
    QualityMetrics --> GSRInterface
    ResponseDetector --> DataLogger
    StatisticalAnalyzer --> NetworkTransmit
    
    %% Synchronization and storage
    SampleCollector --> TimestampManager
    DataLogger --> LocalDatabase
    NetworkTransmit --> CloudSync
    TimestampManager --> CloudSync
```

## Build System Architecture

### Gradle Multi-Module Configuration

```mermaid
graph TB
    subgraph "Root Project"
        subgraph "Build Configuration"
            RootGradle[build.gradle.kts<br/>Root Build Script<br/>Global Configuration]
            GradleProps[gradle.properties<br/>Project Properties<br/>Build Settings]
            VersionCatalog[gradle/libs.versions.toml<br/>Version Catalog<br/>Dependency Management]
        end
        
        subgraph "Project Structure"
            SettingsGradle[settings.gradle.kts<br/>Module Registry<br/>Include Declarations]
            GradleWrapper[gradlew / gradlew.bat<br/>Gradle Wrapper<br/>Version 8.4]
        end
    end
    
    subgraph "Application Modules"
        AppBuild[app/build.gradle.kts<br/>Main Application<br/>Android Config]
    end
    
    subgraph "Feature Component Builds"
        ThermalIRBuild[thermal-ir/build.gradle.kts<br/>Advanced Thermal<br/>Component Library]
        ThermalBuild[thermal/build.gradle.kts<br/>Basic Thermal<br/>Component Library]
        ThermalLiteBuild[thermal-lite/build.gradle.kts<br/>Lite Thermal<br/>Component Library]
        GSRBuild[gsr-recording/build.gradle.kts<br/>GSR Recording<br/>Component Library]
        UserBuild[user/build.gradle.kts<br/>User Management<br/>Component Library]
    end
    
    subgraph "Core Library Builds"
        LibAppBuild[libapp/build.gradle.kts<br/>App Framework<br/>Core Library]
        LibIRBuild[libir/build.gradle.kts<br/>IR Processing<br/>Core Library]
        LibUIBuild[libui/build.gradle.kts<br/>UI Components<br/>Core Library]
    end
    
    subgraph "External Module Builds"
        BleModuleBuild[BleModule/build.gradle.kts<br/>Bluetooth LE<br/>External Library]
        RangeSeekBarBuild[RangeSeekBar/build.gradle.kts<br/>Custom UI Control<br/>External Library]
        CommonComponentBuild[CommonComponent/build.gradle.kts<br/>Shared Components<br/>Consolidated Library]
    end
    
    subgraph "Build Tools & Scripts"
        DevScript[dev.sh<br/>Development Tools<br/>Lint/Build/Test]
        ProductionScript[build_production_apk.sh<br/>Production Build<br/>Release APK]
        GradleDaemon[Gradle Daemon<br/>Build Optimization<br/>Incremental Builds]
    end
    
    %% Root configuration relationships
    RootGradle --> SettingsGradle
    SettingsGradle --> VersionCatalog
    VersionCatalog --> GradleProps
    GradleWrapper --> RootGradle
    
    %% Module registration
    SettingsGradle --> AppBuild
    SettingsGradle --> ThermalIRBuild
    SettingsGradle --> ThermalBuild
    SettingsGradle --> ThermalLiteBuild
    SettingsGradle --> GSRBuild
    SettingsGradle --> UserBuild
    SettingsGradle --> LibAppBuild
    SettingsGradle --> LibIRBuild
    SettingsGradle --> LibUIBuild
    SettingsGradle --> BleModuleBuild
    SettingsGradle --> RangeSeekBarBuild
    SettingsGradle --> CommonComponentBuild
    
    %% Dependency relationships
    AppBuild --> LibAppBuild
    AppBuild --> LibUIBuild
    AppBuild --> ThermalIRBuild
    AppBuild --> GSRBuild
    AppBuild --> BleModuleBuild
    
    ThermalIRBuild --> LibIRBuild
    ThermalBuild --> LibIRBuild
    ThermalLiteBuild --> LibIRBuild
    GSRBuild --> BleModuleBuild
    LibUIBuild --> RangeSeekBarBuild
    LibAppBuild --> CommonComponentBuild
    
    %% Build tool integration
    DevScript --> RootGradle
    ProductionScript --> AppBuild
    GradleDaemon --> RootGradle
```

## Data Flow Architecture

### Multi-Modal Data Processing Pipeline

```mermaid
graph TB
    subgraph "Data Sources"
        subgraph "Thermal Data Stream"
            ThermalSensor[Topdon TC001<br/>Thermal Camera<br/>30 FPS @ 384x288]
            ThermalProcessor[Thermal Processor<br/>Temperature Conversion<br/>16-bit Values]
        end
        
        subgraph "GSR Data Stream"
            GSRSensor[Shimmer3 GSR+<br/>Galvanic Skin Response<br/>51.2 Hz Sampling]
            GSRProcessor[GSR Processor<br/>Signal Processing<br/>Artifact Removal]
        end
        
        subgraph "RGB Data Stream"
            RGBCamera[Android Camera<br/>RGB Image Capture<br/>Variable Resolution]
            RGBProcessor[RGB Processor<br/>Image Processing<br/>Synchronization Markers]
        end
    end
    
    subgraph "Data Synchronization Layer"
        subgraph "Timestamp Management"
            ClockSync[Clock Synchronization<br/>NTP-like Protocol<br/>Multi-device Alignment]
            TimestampAligner[Timestamp Aligner<br/>Offset Calculation<br/>Drift Compensation]
        end
        
        subgraph "Stream Coordination"
            DataBuffer[Data Buffer<br/>Circular Buffers<br/>Overflow Protection]
            StreamMerger[Stream Merger<br/>Multi-modal Alignment<br/>Temporal Synchronization]
            QualityController[Quality Controller<br/>Data Validation<br/>Missing Sample Handling]
        end
    end
    
    subgraph "Processing Pipeline"
        subgraph "Real-time Processing"
            FeatureExtractor[Feature Extractor<br/>Signal Features<br/>Statistical Measures]
            EventDetector[Event Detector<br/>Anomaly Detection<br/>Threshold Monitoring]
            PatternAnalyzer[Pattern Analyzer<br/>Cross-modal Correlation<br/>Behavioral Insights]
        end
        
        subgraph "Data Transformation"
            Normalizer[Data Normalizer<br/>Range Scaling<br/>Unit Conversion]
            Aggregator[Data Aggregator<br/>Temporal Averaging<br/>Statistical Summaries]
            Compressor[Data Compressor<br/>Lossless Compression<br/>Storage Optimization]
        end
    end
    
    subgraph "Storage & Export"
        subgraph "Local Storage"
            SQLiteDB[SQLite Database<br/>Session Metadata<br/>Index & Timestamps]
            RawFiles[Raw Data Files<br/>Binary Format<br/>High Performance]
            CSVExport[CSV Export<br/>Analysis Ready<br/>Human Readable]
        end
        
        subgraph "Structured Storage"
            HDF5Storage[HDF5 Storage<br/>Hierarchical Format<br/>Multi-dimensional Arrays]
            MetadataStore[Metadata Store<br/>Session Information<br/>Device Configuration]
            IndexService[Index Service<br/>Fast Queries<br/>Time-based Lookup]
        end
        
        subgraph "External Integration"
            CloudUpload[Cloud Upload<br/>Google Cloud Storage<br/>Automatic Backup]
            AnalysisExport[Analysis Export<br/>MATLAB/Python/R<br/>Research Integration]
            LSLStream[LSL Stream<br/>Lab Streaming Layer<br/>Real-time Research]
        end
    end
    
    subgraph "Network Distribution"
        NetworkStream[Network Stream<br/>TCP/JSON Protocol<br/>Real-time Transmission]
        HubCoordinator[Hub Coordinator<br/>Multi-device Management<br/>Load Balancing]
        QoSManager[QoS Manager<br/>Bandwidth Management<br/>Priority Queuing]
    end
    
    %% Data source processing
    ThermalSensor --> ThermalProcessor
    GSRSensor --> GSRProcessor
    RGBCamera --> RGBProcessor
    
    %% Synchronization layer
    ThermalProcessor --> ClockSync
    GSRProcessor --> ClockSync
    RGBProcessor --> ClockSync
    
    ClockSync --> TimestampAligner
    TimestampAligner --> DataBuffer
    DataBuffer --> StreamMerger
    StreamMerger --> QualityController
    
    %% Processing pipeline
    QualityController --> FeatureExtractor
    FeatureExtractor --> EventDetector
    EventDetector --> PatternAnalyzer
    
    PatternAnalyzer --> Normalizer
    Normalizer --> Aggregator
    Aggregator --> Compressor
    
    %% Storage and export
    Compressor --> SQLiteDB
    Compressor --> RawFiles
    Compressor --> CSVExport
    
    Aggregator --> HDF5Storage
    StreamMerger --> MetadataStore
    MetadataStore --> IndexService
    
    HDF5Storage --> CloudUpload
    CSVExport --> AnalysisExport
    FeatureExtractor --> LSLStream
    
    %% Network distribution
    EventDetector --> NetworkStream
    NetworkStream --> HubCoordinator
    HubCoordinator --> QoSManager
```

## Network Communication

### Hub-Spoke Communication Protocol

```mermaid
sequenceDiagram
    participant Android as Android Node
    participant Hub as PC Controller Hub
    participant Storage as Data Storage
    participant Analysis as Analysis Tools
    
    Note over Android, Hub: Device Discovery Phase
    Hub->>Android: mDNS Service Advertisement
    Android->>Hub: Service Discovery Response
    Hub->>Android: Connection Invitation
    Android->>Hub: Connection Request + Device Info
    
    Note over Android, Hub: Authentication & Setup
    Hub->>Android: TLS Handshake Initiation
    Android->>Hub: TLS Certificate Exchange
    Hub->>Android: Authentication Challenge
    Android->>Hub: Authentication Response
    Hub->>Android: Connection Established
    
    Note over Android, Hub: Time Synchronization
    Android->>Hub: Time Sync Request
    Hub->>Android: Server Timestamp T1
    Android->>Hub: Client Timestamp T2, T3
    Hub->>Android: Server Timestamp T4
    Note over Android, Hub: Calculate offset: ((T2-T1)+(T3-T4))/2
    
    Note over Android, Hub: Data Streaming Phase
    loop Continuous Data Stream
        Android->>Hub: Thermal Data Frame (30 FPS)
        Android->>Hub: GSR Data Sample (51.2 Hz)
        Android->>Hub: RGB Frame (Variable)
        Hub->>Android: Acknowledgment + QoS Metrics
    end
    
    Note over Hub, Storage: Data Processing
    Hub->>Storage: Synchronized Multi-modal Data
    Hub->>Storage: Session Metadata
    Storage->>Hub: Storage Confirmation
    
    Note over Android, Hub: Session Control
    Hub->>Android: Start Recording Command
    Android->>Hub: Recording Started Confirmation
    Hub->>Android: Stop Recording Command
    Android->>Hub: Recording Stopped + Statistics
    
    Note over Hub, Analysis: Data Export
    Hub->>Storage: Export Request (HDF5/CSV)
    Storage->>Hub: Export File Ready
    Hub->>Analysis: Data Transfer (MATLAB/Python)
    Analysis->>Hub: Analysis Complete
    
    Note over Android, Hub: Error Handling
    Android->>Hub: Device Error Report
    Hub->>Android: Error Recovery Instructions
    Android->>Hub: Recovery Status Update
    Hub->>Android: Configuration Update
    
    Note over Android, Hub: Session Termination
    Hub->>Android: Session End Command
    Android->>Hub: Cleanup Complete
    Hub->>Android: Connection Termination
```

## Security Architecture

### End-to-End Security Implementation

```mermaid
graph TB
    subgraph "Android Security Layer"
        subgraph "Authentication"
            DeviceAuth[Device Authentication<br/>Certificate-based<br/>X.509 Certificates]
            BiometricAuth[Biometric Authentication<br/>Fingerprint/Face ID<br/>Android BiometricPrompt]
            UserAuth[User Authentication<br/>PIN/Password<br/>Local Verification]
        end
        
        subgraph "Data Protection"
            AndroidKeystore[Android Keystore<br/>Hardware-backed Keys<br/>TEE/Secure Element]
            EncryptionAtRest[Encryption at Rest<br/>AES-256-GCM<br/>Local Data Protection]
            SecureStorage[Secure Storage<br/>EncryptedSharedPreferences<br/>Sensitive Data]
        end
        
        subgraph "Communication Security"
            TLSClient[TLS 1.3 Client<br/>Secure Channel<br/>Perfect Forward Secrecy]
            CertVerification[Certificate Verification<br/>Chain Validation<br/>Revocation Checking]
            PinningVerification[Certificate Pinning<br/>Public Key Pinning<br/>MITM Protection]
        end
    end
    
    subgraph "Network Security Layer"
        subgraph "Transport Security"
            TLSHandshake[TLS 1.3 Handshake<br/>ECDHE Key Exchange<br/>ChaCha20-Poly1305]
            SessionKeys[Session Keys<br/>Ephemeral Keys<br/>Key Rotation]
            DataEncryption[Data Encryption<br/>AES-256-GCM<br/>Authenticated Encryption]
        end
        
        subgraph "Network Protection"
            Firewall[Network Firewall<br/>Port Restrictions<br/>IP Filtering]
            IntrusionDetection[Intrusion Detection<br/>Anomaly Monitoring<br/>Attack Prevention]
            RateLimiting[Rate Limiting<br/>DDoS Protection<br/>Connection Throttling]
        end
    end
    
    subgraph "Hub Security Layer"
        subgraph "Server Security"
            ServerAuth[Server Authentication<br/>TLS Server Certificate<br/>Identity Verification]
            AccessControl[Access Control<br/>Role-based Permissions<br/>API Authorization]
            AuditLogging[Audit Logging<br/>Security Events<br/>Compliance Tracking]
        end
        
        subgraph "Data Security"
            DatabaseEncryption[Database Encryption<br/>Transparent Encryption<br/>Key Management]
            BackupSecurity[Backup Security<br/>Encrypted Backups<br/>Secure Storage]
            DataAnonymization[Data Anonymization<br/>PII Removal<br/>Privacy Protection]
        end
    end
    
    subgraph "Cloud Security Layer"
        subgraph "Cloud Storage Security"
            CloudEncryption[Cloud Encryption<br/>Customer-managed Keys<br/>Envelope Encryption]
            AccessPolicies[Access Policies<br/>IAM Roles<br/>Principle of Least Privilege]
            ComplianceFrameworks[Compliance Frameworks<br/>GDPR/HIPAA<br/>Data Governance]
        end
        
        subgraph "Monitoring & Response"
            SecurityMonitoring[Security Monitoring<br/>SIEM Integration<br/>Threat Detection]
            IncidentResponse[Incident Response<br/>Automated Response<br/>Forensic Capabilities]
            VulnerabilityManagement[Vulnerability Management<br/>Security Scanning<br/>Patch Management]
        end
    end
    
    %% Authentication flow
    DeviceAuth --> AndroidKeystore
    BiometricAuth --> UserAuth
    UserAuth --> TLSClient
    
    %% Data protection flow
    AndroidKeystore --> EncryptionAtRest
    EncryptionAtRest --> SecureStorage
    SecureStorage --> TLSClient
    
    %% Network security flow
    TLSClient --> TLSHandshake
    CertVerification --> PinningVerification
    TLSHandshake --> SessionKeys
    SessionKeys --> DataEncryption
    
    %% Network protection
    DataEncryption --> Firewall
    Firewall --> IntrusionDetection
    IntrusionDetection --> RateLimiting
    
    %% Hub security
    RateLimiting --> ServerAuth
    ServerAuth --> AccessControl
    AccessControl --> AuditLogging
    
    %% Data security at hub
    AuditLogging --> DatabaseEncryption
    DatabaseEncryption --> BackupSecurity
    BackupSecurity --> DataAnonymization
    
    %% Cloud security
    DataAnonymization --> CloudEncryption
    CloudEncryption --> AccessPolicies
    AccessPolicies --> ComplianceFrameworks
    
    %% Monitoring and response
    ComplianceFrameworks --> SecurityMonitoring
    SecurityMonitoring --> IncidentResponse
    IncidentResponse --> VulnerabilityManagement
```

## Testing Framework

### Comprehensive Testing Architecture

```mermaid
graph TB
    subgraph "Android Testing"
        subgraph "Unit Testing"
            UnitTests[JUnit Unit Tests<br/>Business Logic<br/>Isolated Components]
            MockingFramework[Mockito Framework<br/>Dependency Mocking<br/>Test Isolation]
            TestUtilities[Test Utilities<br/>Helper Functions<br/>Common Test Data]
        end
        
        subgraph "Integration Testing"
            AndroidTests[Android Instrumented Tests<br/>UI Testing<br/>Device Integration]
            EspressoTests[Espresso UI Tests<br/>User Interaction<br/>Automated Testing]
            DatabaseTests[Database Tests<br/>SQLite Integration<br/>Data Persistence]
        end
        
        subgraph "Hardware Testing"
            ThermalCameraTests[Thermal Camera Tests<br/>TC001 Integration<br/>Frame Processing]
            GSRSensorTests[GSR Sensor Tests<br/>Shimmer3 Integration<br/>Data Collection]
            BluetoothTests[Bluetooth Tests<br/>BLE Communication<br/>Device Pairing]
        end
    end
    
    subgraph "PC Controller Testing"
        subgraph "Python Unit Tests"
            PyTestFramework[pytest Framework<br/>Test Discovery<br/>Fixture Management]
            NetworkTests[Network Tests<br/>TCP/UDP Communication<br/>Protocol Testing]
            DataProcessingTests[Data Processing Tests<br/>Pipeline Validation<br/>Algorithm Testing]
        end
        
        subgraph "Integration Tests"
            HubIntegrationTests[Hub Integration Tests<br/>Multi-device Testing<br/>Session Management]
            GUITests[GUI Tests<br/>PyQt6 Interface<br/>User Interaction]
            DatabaseIntegrationTests[Database Integration Tests<br/>HDF5/SQLite<br/>Data Storage]
        end
        
        subgraph "Performance Tests"
            LoadTests[Load Tests<br/>Multiple Devices<br/>Concurrent Connections]
            StressTests[Stress Tests<br/>Resource Limits<br/>Error Conditions]
            BenchmarkTests[Benchmark Tests<br/>Performance Metrics<br/>Optimization Validation]
        end
    end
    
    subgraph "System Testing"
        subgraph "End-to-End Testing"
            E2EWorkflows[E2E Workflows<br/>Complete User Journeys<br/>Multi-component Testing]
            ScenarioTests[Scenario Tests<br/>Real-world Usage<br/>Edge Case Handling]
            RegressionTests[Regression Tests<br/>Feature Stability<br/>Bug Prevention]
        end
        
        subgraph "Hardware-in-Loop Testing"
            HardwareValidation[Hardware Validation<br/>Real Device Testing<br/>Physical Sensors]
            CalibrationTests[Calibration Tests<br/>Accuracy Validation<br/>Reference Standards]
            EnvironmentalTests[Environmental Tests<br/>Temperature Ranges<br/>Humidity Conditions]
        end
    end
    
    subgraph "Quality Assurance"
        subgraph "Code Quality"
            StaticAnalysis[Static Analysis<br/>Code Quality Metrics<br/>Security Scanning]
            CodeCoverage[Code Coverage<br/>Test Coverage Analysis<br/>Gap Identification]
            DocumentationTests[Documentation Tests<br/>API Documentation<br/>Example Validation]
        end
        
        subgraph "Security Testing"
            SecurityTests[Security Tests<br/>Vulnerability Assessment<br/>Penetration Testing]
            EncryptionTests[Encryption Tests<br/>Cryptographic Validation<br/>Key Management]
            AuthenticationTests[Authentication Tests<br/>Access Control<br/>Permission Validation]
        end
    end
    
    subgraph "CI/CD Testing Pipeline"
        subgraph "Automated Testing"
            ContinuousIntegration[Continuous Integration<br/>GitHub Actions<br/>Automated Builds]
            AutomatedTestExecution[Automated Test Execution<br/>Multi-platform Testing<br/>Parallel Execution]
            TestReporting[Test Reporting<br/>Results Dashboard<br/>Failure Analysis]
        end
        
        subgraph "Deployment Testing"
            StagingTests[Staging Tests<br/>Pre-production Validation<br/>Environment Testing]
            ProductionMonitoring[Production Monitoring<br/>Health Checks<br/>Performance Metrics]
            RollbackTesting[Rollback Testing<br/>Failure Recovery<br/>System Restoration]
        end
    end
    
    %% Android testing connections
    UnitTests --> MockingFramework
    MockingFramework --> TestUtilities
    AndroidTests --> EspressoTests
    EspressoTests --> DatabaseTests
    ThermalCameraTests --> GSRSensorTests
    GSRSensorTests --> BluetoothTests
    
    %% PC Controller testing connections
    PyTestFramework --> NetworkTests
    NetworkTests --> DataProcessingTests
    HubIntegrationTests --> GUITests
    GUITests --> DatabaseIntegrationTests
    LoadTests --> StressTests
    StressTests --> BenchmarkTests
    
    %% System testing connections
    E2EWorkflows --> ScenarioTests
    ScenarioTests --> RegressionTests
    HardwareValidation --> CalibrationTests
    CalibrationTests --> EnvironmentalTests
    
    %% Quality assurance connections
    StaticAnalysis --> CodeCoverage
    CodeCoverage --> DocumentationTests
    SecurityTests --> EncryptionTests
    EncryptionTests --> AuthenticationTests
    
    %% CI/CD pipeline connections
    ContinuousIntegration --> AutomatedTestExecution
    AutomatedTestExecution --> TestReporting
    StagingTests --> ProductionMonitoring
    ProductionMonitoring --> RollbackTesting
    
    %% Cross-layer integration
    UnitTests --> PyTestFramework
    AndroidTests --> HubIntegrationTests
    HardwareValidation --> LoadTests
    SecurityTests --> ContinuousIntegration
```

---

## Summary

This comprehensive architecture documentation provides **12 detailed Mermaid diagrams** covering every aspect of the IRCamera Multi-Modal Thermal Sensing Platform:

### Architecture Coverage:
- **System Overview**: Complete platform architecture with hardware, software, and data flow
- **Hub-and-Spoke**: Distributed communication model with PC controller hub and Android nodes
- **Android Architecture**: Complete module structure with Gradle build system
- **PC Controller**: Python implementation with network services and GUI
- **Class Structure**: Key Android application classes and relationships
- **Thermal Processing**: Complete IR processing pipeline from hardware to display
- **GSR Recording**: Shimmer3 integration with Bluetooth LE communication
- **Build System**: Gradle multi-module configuration and dependencies
- **Data Flow**: Multi-modal data processing and synchronization
- **Network Communication**: Protocol sequences and hub-spoke messaging
- **Security**: End-to-end security implementation with encryption and authentication
- **Testing**: Comprehensive testing framework covering all components

### Technical Specifications:
- **Real Implementation Mapping**: Diagrams reflect actual codebase structure
- **Hardware Details**: Specific device models (Topdon TC001, Shimmer3 GSR+)
- **Protocol Specifications**: TLS 1.3, TCP/JSON, mDNS, Bluetooth LE
- **Performance Metrics**: Frame rates (30 FPS thermal, 51.2 Hz GSR)
- **Security Standards**: AES-256-GCM encryption, Android Keystore, certificate pinning
- **Build Tools**: Gradle 8.4, multi-module architecture, version catalogs

This documentation serves as the definitive technical reference for developers, researchers, and system integrators working with the IRCamera platform.