# IRCamera Architecture Diagrams - Complete Fresh Start

This document provides comprehensive Mermaid architecture diagrams for the IRCamera Multi-Modal
Thermal Sensing Platform, created from scratch based on current repository analysis.

** For detailed app navigation flow and UI structure,
see [APP_NAVIGATION_DIAGRAM.md](APP_NAVIGATION_DIAGRAM.md)**
**🎨 For complete layout architecture and UI components,
see [APP_LAYOUT_DIAGRAM.md](APP_LAYOUT_DIAGRAM.md)**

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Repository Structure](#2-repository-structure)
3. [Android Application Architecture](#3-android-application-architecture)
4. [PC Controller Architecture](#4-pc-controller-architecture)
5. [Component Module Structure](#5-component-module-structure)
6. [BLE Module Architecture](#6-ble-module-architecture)
7. [Data Flow Pipeline](#7-data-flow-pipeline)
8. [Build System Architecture](#8-build-system-architecture)
9. [Network Communication](#9-network-communication)
10. [Hardware Integration](#10-hardware-integration)

---

## 1. System Overview

```mermaid
graph TB
    subgraph "Physical Hardware"
        TC001[Topdon TC001 Thermal Camera<br/>USB Connection<br/>384x288 Resolution]
        Shimmer3[Shimmer3 GSR Sensor<br/>BLE Connection<br/>51.2Hz Sampling]
        AndroidDevice[Android Device<br/>USB Host + BLE]
    end
    
    subgraph "Android Application"
        MainApp[IRCamera Android App<br/>Main APK]
        BleModule[BLE Module<br/>Shimmer Integration]
        ThermalComponent[Thermal Unified Component<br/>TC001 Integration]
        GSRComponent[GSR Recording Component<br/>Data Collection]
        UserComponent[User Management Component<br/>Session Control]
    end
    
    subgraph "PC Controller Hub"
        PCAdvanced[Advanced Controller<br/>advanced_pc_controller.py]
        UnifiedController[Unified Controller<br/>run_unified_controller.py]
        LegacyFramework[Legacy Framework<br/>ircamera_pc/]
    end
    
    subgraph "Data Storage"
        LocalFiles[Local Data Files<br/>CSV, HDF5, JSON]
        RemoteStorage[Remote Storage<br/>Cloud/Network]
    end
    
    TC001 --> AndroidDevice
    Shimmer3 --> AndroidDevice
    AndroidDevice --> MainApp
    MainApp --> BleModule
    MainApp --> ThermalComponent
    MainApp --> GSRComponent
    MainApp --> UserComponent
    
    MainApp -->|Network/WiFi| PCAdvanced
    MainApp -->|Network/WiFi| UnifiedController
    
    PCAdvanced --> LocalFiles
    PCAdvanced --> LocalFiles
    UnifiedController --> LocalFiles
    PCAdvanced --> RemoteStorage
    UnifiedController --> RemoteStorage
```

---

## 2. Repository Structure

```mermaid
graph TB
    subgraph "Root Directory"
        RootBuild[build.gradle.kts<br/>Root Build Script]
        Settings[settings.gradle.kts<br/>Module Settings]
        GradleProps[gradle.properties<br/>Build Properties]
    end
    
    subgraph "Android Modules"
        AppModule[app/<br/>Main Android Application<br/>92 activities, 31 layouts]
        BleModuleDir[BleModule/<br/>Bluetooth Low Energy<br/>Legacy Integration]
        ComponentDir[component/<br/>Feature Components<br/>111 activities, 121 layouts total]
        LibUnified[libunified/<br/>Unified Library<br/>7 activities, 69 layouts]
    end
    
    subgraph "Component Modules"
        GSRRecording[component/gsr-recording/<br/>GSR Data Collection<br/>0 activities, 0 layouts]
        ThermalUnified[component/thermalunified/<br/>Thermal Processing<br/>93 activities, 103 layouts]
        UserModule[component/user/<br/>User Management<br/>18 activities, 18 layouts]
    end
    
    subgraph "PC Controller"
        PCRoot[pc-controller/<br/>Python Controllers]
        LegacyImpl[legacy_implementation/<br/>Framework Implementation]
        MVPFiles[MVP Implementation Files<br/>*.py]
    end
    
    subgraph "Documentation"
        DocsDir[docs/<br/>Documentation]
        ConfigDir[config/<br/>Configuration Files]
        TestingDir[testing/<br/>Test Suites]
    end
    
    RootBuild --> AppModule
    RootBuild --> BleModuleDir
    RootBuild --> ComponentDir
    RootBuild --> LibUnified
    
    Settings --> RootBuild
    GradleProps --> RootBuild
    
    ComponentDir --> GSRRecording
    ComponentDir --> ThermalUnified
    ComponentDir --> UserModule
    
    PCRoot --> LegacyImpl
    PCRoot --> MVPFiles
```

---

## 2.5. Module Statistics Overview

```mermaid
graph TB
    subgraph "IRCamera Repository Statistics"
        TotalStats[Total: 210 Activities, 221 Layouts<br/>4 Main Modules<br/>Multi-Modal Platform]
    end
    
    subgraph "App Module"
        AppStats[app/<br/>92 Activities (44%)<br/>31 Layouts (14%)<br/>Core Infrastructure]
    end
    
    subgraph "Component thermalunified"
        ThermalStats[thermalunified/<br/>93 Activities (44%)<br/>103 Layouts (47%)<br/>Thermal Imaging System]
    end
    
    subgraph "Component user"
        UserStats[user/<br/>18 Activities (9%)<br/>18 Layouts (8%)<br/>User Management]
    end
    
    subgraph "LibUnified"
        LibStats[libunified/<br/>7 Activities (3%)<br/>69 Layouts (31%)<br/>Shared Utilities]
    end
    
    TotalStats --> AppStats
    TotalStats --> ThermalStats
    TotalStats --> UserStats
    TotalStats --> LibStats
    
    %% Styling
    classDef totalBox fill:#ff6b6b,stroke:#333,stroke-width:3px,color:#fff
    classDef moduleBox fill:#4ecdc4,stroke:#333,stroke-width:2px,color:#fff
    
    class TotalStats totalBox
    class AppStats,ThermalStats,UserStats,LibStats moduleBox
```

---

## 3. Android Application Architecture

```mermaid
graph TB
    subgraph "Application Layer"
        MainActivity[MainActivity<br/>Main Entry Point]
        Application[IRCamera Application<br/>App Context]
    end
    
    subgraph "UI Layer"
        Fragments[UI Fragments<br/>Dashboard, Settings, Status]
        Activities[Activities<br/>Main, Configuration]
        Views[Custom Views<br/>Thermal Display, GSR Charts]
    end
    
    subgraph "Business Logic"
        ThermalManager[Thermal Camera Manager<br/>TC001 Control]
        GSRManager[GSR Data Manager<br/>Shimmer3 Control]
        NetworkManager[Network Manager<br/>PC Communication]
        DataManager[Data Manager<br/>Local Storage]
    end
    
    subgraph "Hardware Integration"
        USBHandler[USB Handler<br/>TC001 Communication]
        BLEHandler[BLE Handler<br/>Shimmer3 Communication]
        SensorController[Sensor Controller<br/>Data Collection]
    end
    
    subgraph "Data Layer"
        LocalDB[Local Database<br/>SQLite]
        FileStorage[File Storage<br/>CSV, JSON]
        NetworkClient[Network Client<br/>TCP/HTTP]
    end
    
    MainActivity --> Application
    MainActivity --> Fragments
    Fragments --> Activities
    Activities --> Views
    
    Views --> ThermalManager
    Views --> GSRManager
    Views --> NetworkManager
    
    ThermalManager --> USBHandler
    GSRManager --> BLEHandler
    NetworkManager --> NetworkClient
    
    USBHandler --> SensorController
    BLEHandler --> SensorController
    SensorController --> DataManager
    
    DataManager --> LocalDB
    DataManager --> FileStorage
    NetworkClient --> DataManager
```

---

## 4. PC Controller Architecture

```mermaid
graph TB
    subgraph "Advanced Controller"
        AdvancedMain[advanced_pc_controller.py<br/>Full Featured Controller]
        DeviceManager[Device Manager<br/>Multi-device Handling]
        SessionManager[Session Manager<br/>Recording Sessions]
        DataProcessor[Data Processor<br/>Real-time Processing]
    end
    
    subgraph "Unified Controller"
        UnifiedMain[run_unified_controller.py<br/>Unified Interface]
        ConfigManager[Configuration Manager<br/>Settings Management]
        PluginSystem[Plugin System<br/>Extensible Architecture]
    end
    
    subgraph "Legacy Framework"
        NetworkDiscovery[network/discovery.py<br/>mDNS Device Discovery]
        CoreFramework[core/<br/>Core Framework Classes]
        UtilsModule[utils/<br/>Utility Functions]
        ExamplesModule[examples/<br/>Usage Examples]
    end
    
    subgraph "Supporting Tools"
        SystemTest[test_comprehensive_integration.py<br/>System Testing]
        CommandClient[command_client.py<br/>CLI Interface]
        SetupScript[setup.py<br/>Installation Script]
    end
    
    AdvancedMain --> DeviceManager
    AdvancedMain --> SessionManager
    AdvancedMain --> DataProcessor
    
    UnifiedMain --> ConfigManager
    UnifiedMain --> PluginSystem
    UnifiedMain --> AdvancedMain
    
    AdvancedMain --> NetworkDiscovery
    AdvancedMain --> CoreFramework
    DeviceManager --> UtilsModule
    
    SystemTest --> AdvancedMain
    CommandClient --> UnifiedMain
```

---

## 5. Component Module Structure

```mermaid
graph TB
    subgraph "GSR Recording Component"
        GSRMain[GSR Recording Module<br/>component/gsr-recording/]
        GSRBuild[build.gradle.kts<br/>GSR Build Config]
        GSRSrc[src/main/<br/>GSR Source Code]
        GSRManifest[AndroidManifest.xml<br/>GSR Permissions]
    end
    
    subgraph "Thermal Unified Component"
        ThermalMain[Thermal Unified Module<br/>component/thermalunified/]
        ThermalBuild[build.gradle.kts<br/>Thermal Build Config]
        ThermalSrc[src/main/<br/>Thermal Source Code]
        ThermalManifest[AndroidManifest.xml<br/>Thermal Permissions]
    end
    
    subgraph "User Management Component"
        UserMain[User Module<br/>component/user/]
        UserBuild[build.gradle.kts<br/>User Build Config]
        UserSrc[src/main/<br/>User Source Code]
        UserManifest[AndroidManifest.xml<br/>User Permissions]
    end
    
    subgraph "Common Component Framework"
        CommonGradle[common-component.gradle.kts<br/>Shared Configuration]
        ComponentInterface[Component Interface<br/>Base Classes]
        SharedResources[Shared Resources<br/>Common Assets]
    end
    
    GSRMain --> GSRBuild
    GSRMain --> GSRSrc
    GSRMain --> GSRManifest
    
    ThermalMain --> ThermalBuild
    ThermalMain --> ThermalSrc
    ThermalMain --> ThermalManifest
    
    UserMain --> UserBuild
    UserMain --> UserSrc
    UserMain --> UserManifest
    
    CommonGradle --> GSRBuild
    CommonGradle --> ThermalBuild
    CommonGradle --> UserBuild
    
    ComponentInterface --> GSRSrc
    ComponentInterface --> ThermalSrc
    ComponentInterface --> UserSrc
```

---

## 6. BLE Module Architecture

```mermaid
graph TB
    subgraph "BLE Module Structure"
        BLERoot[BleModule/<br/>Root Directory]
        BLEBuild[build.gradle.kts<br/>BLE Build Configuration]
        BLEManifest[AndroidManifest.xml<br/>BLE Permissions]
    end
    
    subgraph "Topdon Commons"
        TopdonPoster[com/topdon/commons/poster/<br/>Event Posting System]
        AsyncPoster[AsyncPoster.java<br/>Async Event Handling]
        MainThreadPoster[MainThreadPoster.java<br/>UI Thread Events]
        BackgroundPoster[BackgroundPoster.java<br/>Background Events]
        UUIDManager[UUIDManager.java<br/>UUID Management]
    end
    
    subgraph "BLE Core Components"
        BLEScanner[BLE Scanner<br/>Device Discovery]
        BLEConnection[BLE Connection<br/>GATT Client]
        BLECharacteristics[BLE Characteristics<br/>Data Exchange]
        BLECallbacks[BLE Callbacks<br/>Event Handling]
    end
    
    subgraph "Shimmer Integration"
        ShimmerDevice[Shimmer Device<br/>Device Abstraction]
        ShimmerGSR[Shimmer GSR<br/>GSR Data Processing]
        ShimmerProtocol[Shimmer Protocol<br/>Command Interface]
        ShimmerCalibration[Shimmer Calibration<br/>Sensor Calibration]
    end
    
    BLERoot --> BLEBuild
    BLERoot --> BLEManifest
    BLERoot --> TopdonPoster
    
    TopdonPoster --> AsyncPoster
    TopdonPoster --> MainThreadPoster
    TopdonPoster --> BackgroundPoster
    TopdonPoster --> UUIDManager
    
    BLERoot --> BLEScanner
    BLERoot --> BLEConnection
    BLERoot --> BLECharacteristics
    BLERoot --> BLECallbacks
    
    BLEConnection --> ShimmerDevice
    BLECharacteristics --> ShimmerGSR
    BLECallbacks --> ShimmerProtocol
    ShimmerDevice --> ShimmerCalibration
```

---

## 7. Data Flow Pipeline

```mermaid
flowchart TD
    subgraph "Data Sources"
        TC001Sensor[TC001 Thermal Camera<br/>30 FPS @ 384x288<br/>-20 degrees C to +400 degrees C]
        Shimmer3Sensor[Shimmer3 GSR<br/>51.2 Hz Sampling<br/>Galvanic Skin Response]
    end
    
    subgraph "Android Data Collection"
        ThermalCapture[Thermal Data Capture<br/>USB Interface<br/>Raw Thermal Frames]
        GSRCapture[GSR Data Capture<br/>BLE Interface<br/>Resistance Values]
        TimestampSync[Timestamp Synchronization<br/>NTP-like Clock Sync<br/>Microsecond Precision]
    end
    
    subgraph "Data Processing"
        ThermalProcess[Thermal Processing<br/>Temperature Conversion<br/>Image Enhancement]
        GSRProcess[GSR Processing<br/>Signal Filtering<br/>Artifact Removal]
        DataFusion[Data Fusion<br/>Multi-modal Alignment<br/>Temporal Synchronization]
    end
    
    subgraph "Local Storage"
        RawFiles[Raw Data Files<br/>CSV Format<br/>Timestamped Records]
        ProcessedFiles[Processed Data<br/>HDF5 Format<br/>Hierarchical Structure]
        MetadataDB[Metadata Database<br/>SQLite<br/>Session Information]
    end
    
    subgraph "Network Transmission"
        NetworkStream[Network Streaming<br/>TCP/JSON Protocol<br/>Real-time Transmission]
        PCController[PC Controller<br/>Hub Processing<br/>Multi-device Coordination]
    end
    
    subgraph "PC Data Processing"
        DataAggregation[Data Aggregation<br/>Multi-device Fusion<br/>Session Management]
        AnalysisEngine[Analysis Engine<br/>Statistical Processing<br/>Feature Extraction]
        ExportFormats[Export Formats<br/>MATLAB, CSV, HDF5<br/>Research Tools]
    end
    
    TC001Sensor --> ThermalCapture
    Shimmer3Sensor --> GSRCapture
    
    ThermalCapture --> TimestampSync
    GSRCapture --> TimestampSync
    
    TimestampSync --> ThermalProcess
    TimestampSync --> GSRProcess
    
    ThermalProcess --> DataFusion
    GSRProcess --> DataFusion
    
    DataFusion --> RawFiles
    DataFusion --> ProcessedFiles
    DataFusion --> MetadataDB
    
    DataFusion --> NetworkStream
    NetworkStream --> PCController
    
    PCController --> DataAggregation
    DataAggregation --> AnalysisEngine
    AnalysisEngine --> ExportFormats
```

---

## 8. Build System Architecture

```mermaid
graph TB
    subgraph "Root Build Configuration"
        RootGradle[build.gradle.kts<br/>Root Project Build<br/>Plugin Management<br/>Global Settings]
        SettingsGradle[settings.gradle.kts<br/>Module Inclusion<br/>Repository Configuration<br/>Plugin Resolution Strategy]
        GradleProperties[gradle.properties<br/>Build Properties<br/>JVM Settings<br/>Android SDK Config]
    end
    
    subgraph "Version Management"
        VersionCatalog[gradle/libs.versions.toml<br/>Centralized Dependencies<br/>Version Definitions<br/>Library Bundles]
        SyntaxCheck[syntax_check.gradle<br/>Code Quality Checks<br/>Static Analysis]
    end
    
    subgraph "Android App Module"
        AppBuild[app/build.gradle.kts<br/>Application Configuration<br/>Dependencies<br/>Build Types]
        AppManifest[app/AndroidManifest.xml<br/>App Permissions<br/>Activity Declarations<br/>Hardware Features]
    end
    
    subgraph "BLE Module Build"
        BLEBuild[BleModule/build.gradle.kts<br/>Library Configuration<br/>JNI Integration<br/>Native Dependencies]
        BLEManifest[BleModule/AndroidManifest.xml<br/>BLE Permissions<br/>Service Declarations]
    end
    
    subgraph "Component Builds"
        GSRBuild[component/gsr-recording/build.gradle.kts<br/>GSR Component Build<br/>BLE Dependencies]
        ThermalBuild[component/thermalunified/build.gradle.kts<br/>Thermal Component Build<br/>USB Dependencies]
        UserBuild[component/user/build.gradle.kts<br/>User Component Build<br/>UI Dependencies]
        CommonComponent[component/common-component.gradle.kts<br/>Shared Component Config<br/>Common Dependencies]
    end
    
    subgraph "Library Builds"
        LibUnifiedBuild[libunified/build.gradle.kts<br/>Unified Library Build<br/>Core Dependencies]
    end
    
    subgraph "Build Tools"
        GradleWrapper[gradlew / gradlew.bat<br/>Gradle Wrapper<br/>Version 8.4<br/>Cross-platform Build]
        ThesisGenerator[generate_thesis_deliverables.py<br/>Documentation Generator<br/>Thesis Artifacts]
    end
    
    RootGradle --> SettingsGradle
    RootGradle --> GradleProperties
    RootGradle --> VersionCatalog
    RootGradle --> SyntaxCheck
    
    SettingsGradle --> AppBuild
    SettingsGradle --> BLEBuild
    SettingsGradle --> GSRBuild
    SettingsGradle --> ThermalBuild
    SettingsGradle --> UserBuild
    SettingsGradle --> LibUnifiedBuild
    
    VersionCatalog --> AppBuild
    VersionCatalog --> BLEBuild
    VersionCatalog --> GSRBuild
    VersionCatalog --> ThermalBuild
    VersionCatalog --> UserBuild
    VersionCatalog --> LibUnifiedBuild
    
    CommonComponent --> GSRBuild
    CommonComponent --> ThermalBuild
    CommonComponent --> UserBuild
    
    AppBuild --> AppManifest
    BLEBuild --> BLEManifest
    
    GradleWrapper --> RootGradle
    ThesisGenerator --> RootGradle
```

---

## 9. Network Communication

```mermaid
sequenceDiagram
    participant AndroidApp as Android App
    participant PCController as PC Controller
    participant NetworkStack as Network Stack
    participant DataStorage as Data Storage
    
    Note over AndroidApp,DataStorage: Device Discovery Phase
    AndroidApp->>NetworkStack: Broadcast mDNS Service
    PCController->>NetworkStack: Listen for mDNS Services
    NetworkStack->>PCController: Device Discovery Response
    PCController->>AndroidApp: Connection Request
    
    Note over AndroidApp,DataStorage: Authentication Phase
    AndroidApp->>PCController: Device Information
    PCController->>AndroidApp: Authentication Challenge
    AndroidApp->>PCController: Authentication Response
    PCController->>AndroidApp: Connection Established
    
    Note over AndroidApp,DataStorage: Data Streaming Phase
    loop Real-time Data Streaming
        AndroidApp->>AndroidApp: Collect Thermal Data (30 FPS)
        AndroidApp->>AndroidApp: Collect GSR Data (51.2 Hz)
        AndroidApp->>AndroidApp: Timestamp Synchronization
        AndroidApp->>PCController: Send Thermal Frame
        AndroidApp->>PCController: Send GSR Sample
        PCController->>DataStorage: Store Raw Data
        PCController->>AndroidApp: Acknowledgment
    end
    
    Note over AndroidApp,DataStorage: Session Management
    PCController->>AndroidApp: Start Recording Session
    AndroidApp->>PCController: Session Started
    AndroidApp->>PCController: Streaming Session Data
    PCController->>AndroidApp: Stop Recording Session
    AndroidApp->>PCController: Session Stopped
    PCController->>DataStorage: Finalize Session Data
    
    Note over AndroidApp,DataStorage: Error Handling
    AndroidApp->>PCController: Connection Error
    PCController->>AndroidApp: Retry Connection
    AndroidApp->>PCController: Data Transmission Error
    PCController->>AndroidApp: Request Retransmission
```

---

## 10. Hardware Integration

```mermaid
graph TB
    subgraph "Topdon TC001 Thermal Camera"
        TC001Hardware[TC001 Hardware<br/>Thermal Sensor Array<br/>384x288 Resolution<br/>-20 degrees C to +400 degrees C Range]
        TC001USB[USB Interface<br/>USB 2.0 Connection<br/>Hot-plug Support<br/>Power via USB]
        TC001Driver[TC001 Driver<br/>Native Library<br/>JNI Interface<br/>Frame Capture API]
        TC001SDK[TC001 SDK<br/>Temperature Conversion<br/>Image Processing<br/>Calibration Data]
    end
    
    subgraph "Shimmer3 GSR Sensor"
        Shimmer3Hardware[Shimmer3 Hardware<br/>GSR Electrodes<br/>16-bit ADC<br/>51.2 Hz Sampling]
        Shimmer3BLE[Bluetooth LE<br/>BLE 4.0+<br/>GATT Profile<br/>Low Power Mode]
        Shimmer3Protocol[Shimmer Protocol<br/>Command Interface<br/>Data Streaming<br/>Configuration]
        Shimmer3SDK[Shimmer SDK<br/>Signal Processing<br/>Calibration<br/>Artifact Removal]
    end
    
    subgraph "Android Hardware Interface"
        USBHost[USB Host Controller<br/>OTG Support<br/>Device Enumeration<br/>Permission Management]
        BLEAdapter[BLE Adapter<br/>Bluetooth Stack<br/>GATT Client<br/>Service Discovery]
        SensorHub[Sensor Hub<br/>Data Coordination<br/>Timestamp Sync<br/>Buffer Management]
    end
    
    subgraph "Android Application Layer"
        ThermalManager[Thermal Manager<br/>TC001 Control<br/>Frame Processing<br/>Temperature Mapping]
        GSRManager[GSR Manager<br/>Shimmer Control<br/>Signal Processing<br/>Data Validation]
        HardwareAbstraction[Hardware Abstraction<br/>Unified Interface<br/>Device Management<br/>Error Handling]
    end
    
    subgraph "Data Processing Pipeline"
        RealTimeProcessor[Real-time Processor<br/>Frame Rate Sync<br/>Data Fusion<br/>Quality Control]
        DataBuffer[Data Buffer<br/>Circular Buffers<br/>Memory Management<br/>Flow Control]
        NetworkTransmitter[Network Transmitter<br/>TCP Streaming<br/>Compression<br/>Error Recovery]
    end
    
    TC001Hardware --> TC001USB
    TC001USB --> TC001Driver
    TC001Driver --> TC001SDK
    
    Shimmer3Hardware --> Shimmer3BLE
    Shimmer3BLE --> Shimmer3Protocol
    Shimmer3Protocol --> Shimmer3SDK
    
    TC001SDK --> USBHost
    USBHost --> SensorHub
    
    Shimmer3SDK --> BLEAdapter
    BLEAdapter --> SensorHub
    
    SensorHub --> ThermalManager
    SensorHub --> GSRManager
    
    ThermalManager --> HardwareAbstraction
    GSRManager --> HardwareAbstraction
    
    HardwareAbstraction --> RealTimeProcessor
    RealTimeProcessor --> DataBuffer
    DataBuffer --> NetworkTransmitter
```

---

## Architecture Summary

This comprehensive architecture documentation covers:

- **System Overview**: Complete multi-modal sensing platform
- **Repository Structure**: Gradle multi-module project organization
- **Android Architecture**: Application, component, and hardware layers
- **PC Controller**: Multiple implementation approaches (MVP, Advanced, Unified)
- **Component Modules**: GSR recording, thermal processing, user management
- **BLE Integration**: Shimmer3 sensor communication and data processing
- **Data Pipeline**: Real-time streaming, synchronization, and storage
- **Build System**: Gradle configuration and dependency management
- **Network Communication**: Hub-spoke protocol and session management
- **Hardware Integration**: TC001 thermal camera and Shimmer3 GSR sensor

Each diagram provides implementation-level detail suitable for developers, researchers, and system
integrators working with the IRCamera Multi-Modal Thermal Sensing Platform.