# IRCamera Architecture Guide

## System Overview

The IRCamera Multi-Modal Thermal Sensing Platform implements a sophisticated **Hub-and-Spoke architecture** designed for distributed multi-modal physiological sensing. This document provides comprehensive technical details about the system design, component interactions, and architectural decisions.

## High-Level Architecture

### Hub-and-Spoke Model

```mermaid
graph TB
    subgraph "Hub (PC Controller)"
        PCHub[PC Controller Application<br/>Python + PyQt6]
        DevMgr[Device Manager<br/>mDNS Discovery]
        SessMgr[Session Manager<br/>Lifecycle Control]
        DataAgg[Data Aggregator<br/>Multi-modal Sync]
        Network[Network Hub<br/>TCP/JSON Protocol]
    end
    
    subgraph "Spoke 1 (Android Device)"
        Android1[Android Sensor Node]
        Thermal1[Thermal Camera<br/>Topdon TC001]
        GSR1[GSR Sensor<br/>Shimmer3 BLE]
        RGB1[RGB Camera<br/>CameraX API]
    end
    
    subgraph "Spoke 2 (Android Device)"
        Android2[Android Sensor Node]
        Thermal2[Thermal Camera<br/>Topdon TC001]
        GSR2[GSR Sensor<br/>Shimmer3 BLE]
        RGB2[RGB Camera<br/>CameraX API]
    end
    
    subgraph "Spoke N (Android Device)"
        AndroidN[Android Sensor Node]
        ThermalN[Thermal Camera<br/>Topdon TC001]
        GSRN[GSR Sensor<br/>Shimmer3 BLE]
        RGBN[RGB Camera<br/>CameraX API]
    end
    
    PCHub <-->|JSON Commands<br/>TCP/IP| Android1
    PCHub <-->|JSON Commands<br/>TCP/IP| Android2
    PCHub <-->|JSON Commands<br/>TCP/IP| AndroidN
    
    DevMgr --> PCHub
    SessMgr --> PCHub
    DataAgg --> PCHub
    Network --> PCHub
    
    Android1 --> Thermal1
    Android1 --> GSR1
    Android1 --> RGB1
    
    Android2 --> Thermal2
    Android2 --> GSR2
    Android2 --> RGB2
    
    AndroidN --> ThermalN
    AndroidN --> GSRN
    AndroidN --> RGBN
```

### Design Principles

1. **Distributed Processing**: Multiple Android devices operate as autonomous sensor nodes
2. **Centralized Coordination**: PC Controller manages session lifecycle and synchronization
3. **Fault Tolerance**: System continues operation if individual devices fail
4. **Scalability**: Architecture supports dynamic addition/removal of sensor nodes
5. **Modularity**: Clear separation of concerns across components
6. **Real-time Communication**: Low-latency command/response protocol

## PC Controller Hub Architecture

### Component Structure

```mermaid
graph TB
    subgraph "Presentation Layer"
        MainGUI[Main GUI Window<br/>PyQt6 Interface]
        DeviceView[Device Dashboard<br/>Live Status Display]
        SessionView[Session Control Panel<br/>Recording Management]
        LogView[System Log Viewer<br/>Event Monitoring]
    end
    
    subgraph "Application Layer"
        AppCtrl[Application Controller<br/>Main Event Loop]
        DeviceManager[Device Manager<br/>Registry + Status]
        SessionManager[Session Manager<br/>Lifecycle Control]
        ConfigManager[Configuration Manager<br/>Settings + Persistence]
    end
    
    subgraph "Service Layer"
        DiscoveryService[Device Discovery Service<br/>mDNS Integration]
        CommunicationService[Communication Service<br/>TCP Server]
        DataService[Data Aggregation Service<br/>Multi-modal Sync]
        StorageService[Storage Service<br/>Session Persistence]
    end
    
    subgraph "Network Layer"
        TCPServer[TCP Server<br/>Port 8080]
        mDNSClient[mDNS Client<br/>Service Discovery]
        JSONProtocol[JSON Protocol Handler<br/>Message Serialization]
        HeartbeatMonitor[Heartbeat Monitor<br/>Connection Health]
    end
    
    MainGUI --> AppCtrl
    DeviceView --> DeviceManager
    SessionView --> SessionManager
    LogView --> AppCtrl
    
    AppCtrl --> DeviceManager
    AppCtrl --> SessionManager
    AppCtrl --> ConfigManager
    
    DeviceManager --> DiscoveryService
    SessionManager --> CommunicationService
    SessionManager --> DataService
    SessionManager --> StorageService
    
    DiscoveryService --> mDNSClient
    CommunicationService --> TCPServer
    CommunicationService --> JSONProtocol
    CommunicationService --> HeartbeatMonitor
```

### Key Components

#### Device Manager
- **Purpose**: Central registry for all discovered and connected devices
- **Responsibilities**: 
  - Device discovery coordination
  - Status monitoring and health checks
  - Capability tracking and validation
  - Connection lifecycle management

#### Session Manager
- **Purpose**: Complete session lifecycle control from creation to finalization
- **Responsibilities**:
  - Session creation and metadata management
  - Synchronized recording start/stop coordination
  - Device acknowledgment and error handling
  - Session finalization and data validation

#### Communication Service
- **Purpose**: Reliable message exchange with Android sensor nodes
- **Responsibilities**:
  - TCP server management
  - JSON message serialization/deserialization
  - Command routing and response handling
  - Connection health monitoring

## Android Sensor Node Architecture

### Application Structure

```mermaid
graph TB
    subgraph "User Interface Layer"
        MainActivity[MainActivity<br/>Primary UI Controller]
        SensorDashboard[Sensor Dashboard<br/>Status Display]
        RecordingControls[Recording Controls<br/>Manual Operations]
        SettingsActivity[Settings Activity<br/>Configuration]
    end
    
    subgraph "Application Logic Layer"
        RecordingController[Recording Controller<br/>Multi-sensor Coordination]
        NetworkManager[Network Manager<br/>Hub Communication]
        PermissionManager[Permission Manager<br/>Runtime Permissions]
        ServiceDiscovery[Service Discovery<br/>mDNS Advertisement]
    end
    
    subgraph "Sensor Integration Layer"
        ThermalController[Thermal Controller<br/>Topdon TC001 Integration]
        GSRController[GSR Controller<br/>Shimmer3 BLE Integration] 
        RGBController[RGB Controller<br/>CameraX Integration]
        SensorCoordinator[Sensor Coordinator<br/>Synchronization]
    end
    
    subgraph "Hardware Abstraction Layer"
        USBManager[USB Manager<br/>Thermal Camera Interface]
        BLEManager[BLE Manager<br/>Shimmer3 Communication]
        CameraManager[Camera Manager<br/>CameraX Framework]
        StorageManager[Storage Manager<br/>Data Persistence]
    end
    
    subgraph "Core Libraries"
        LibIR[libir<br/>Thermal Processing]
        LibCom[libcom<br/>Network Communication]
        LibApp[libapp<br/>Application Framework]
        LibUI[libui<br/>UI Components]
    end
    
    MainActivity --> RecordingController
    MainActivity --> NetworkManager
    MainActivity --> PermissionManager
    
    RecordingController --> ThermalController
    RecordingController --> GSRController
    RecordingController --> RGBController
    RecordingController --> SensorCoordinator
    
    NetworkManager --> ServiceDiscovery
    
    ThermalController --> USBManager
    GSRController --> BLEManager
    RGBController --> CameraManager
    SensorCoordinator --> StorageManager
    
    ThermalController --> LibIR
    NetworkManager --> LibCom
    RecordingController --> LibApp
    MainActivity --> LibUI
```

### Sensor Integration Details

#### Thermal Camera Integration
- **Hardware**: Topdon TC001 USB thermal camera
- **Interface**: USB OTG connection with custom driver
- **Capabilities**: 256x192 resolution, 10 FPS capture rate
- **Processing**: Real-time temperature matrix generation
- **Fallback**: Simulation mode when hardware unavailable

#### GSR Sensor Integration  
- **Hardware**: Shimmer3 GSR+ device via Bluetooth Low Energy
- **Protocol**: Custom BLE communication protocol
- **Sampling**: 128 Hz continuous data streaming
- **Features**: Automatic reconnection, data quality monitoring
- **Fallback**: Simulation mode with synthetic data

#### RGB Camera Integration
- **Framework**: Android CameraX API for modern camera handling
- **Capabilities**: 4K@60fps recording with concurrent frame capture
- **Dual Output**: Video files (.mp4) + individual frames for analysis
- **Quality**: High-quality encoding optimized for analysis

## Communication Protocol Architecture

### Protocol Stack

```mermaid
graph TB
    subgraph "Application Protocol"
        Commands[Command Messages<br/>start_recording, stop_recording, sync_flash]
        Responses[Response Messages<br/>ack, error, status, data]
        Heartbeat[Heartbeat Messages<br/>Connection Health]
        SessionData[Session Data Messages<br/>Metadata Exchange]
    end
    
    subgraph "Message Format Layer"
        JSON[JSON Serialization<br/>Human-readable + Structured]
        Validation[Message Validation<br/>Schema Enforcement]
        Routing[Message Routing<br/>Target + Response Handling]
        Acknowledgment[Acknowledgment System<br/>Reliable Delivery]
    end
    
    subgraph "Transport Layer"
        TCP[TCP/IP Protocol<br/>Reliable Stream Transport]
        PortManagement[Port Management<br/>Dynamic Port Assignment]
        ConnectionPool[Connection Pool<br/>Multi-device Support]
        ErrorRecovery[Error Recovery<br/>Automatic Reconnection]
    end
    
    subgraph "Discovery Layer"
        mDNS[mDNS Service Discovery<br/>Zero-configuration Networking]
        ServiceAdvertisement[Service Advertisement<br/>Device Capability Broadcast]
        NetworkScanning[Network Scanning<br/>Active Device Detection]
        ManualFallback[Manual Configuration<br/>Fallback Option]
    end
    
    Commands --> JSON
    Responses --> JSON
    Heartbeat --> Validation
    SessionData --> Validation
    
    JSON --> TCP
    Validation --> PortManagement
    Routing --> ConnectionPool
    Acknowledgment --> ErrorRecovery
    
    TCP --> mDNS
    PortManagement --> ServiceAdvertisement
    ConnectionPool --> NetworkScanning
    ErrorRecovery --> ManualFallback
```

### Message Flow Examples

#### Session Start Sequence
```mermaid
sequenceDiagram
    participant Hub as PC Hub
    participant Node1 as Android Node 1
    participant Node2 as Android Node 2
    
    Note over Hub: User clicks "Start Recording"
    
    Hub->>Node1: start_recording command
    Hub->>Node2: start_recording command
    
    Note over Node1: Initialize sensors
    Node1->>Hub: ack (device_ready: true)
    
    Note over Node2: Initialize sensors  
    Node2->>Hub: ack (device_ready: true)
    
    Note over Hub: All devices ready
    
    Hub->>Node1: sync_flash command
    Hub->>Node2: sync_flash command
    
    Node1->>Hub: ack (sync_complete: true)
    Node2->>Hub: ack (sync_complete: true)
    
    Note over Hub,Node2: Recording starts synchronously
```

## Data Architecture

### Data Flow Pipeline

```mermaid
graph TB
    subgraph "Android Sensors"
        ThermalSensor[Thermal Camera<br/>10 FPS, Temperature Matrix]
        GSRSensor[GSR Sensor<br/>128 Hz, Conductance Values]
        RGBSensor[RGB Camera<br/>30 FPS, Video + Frames]
    end
    
    subgraph "Local Processing"
        ThermalProcessor[Thermal Processor<br/>Temperature Calibration]
        GSRProcessor[GSR Processor<br/>Signal Filtering]
        RGBProcessor[RGB Processor<br/>Frame Extraction]
    end
    
    subgraph "Data Synchronization"
        TimestampSync[Timestamp Synchronization<br/>±2.1ms Precision]
        DataBuffer[Data Buffer<br/>Temporary Storage]
        QualityCheck[Quality Validation<br/>Signal Integrity]
    end
    
    subgraph "Storage Layer"
        LocalStorage[Local Storage<br/>CSV Files + Video]
        SessionMetadata[Session Metadata<br/>JSON Configuration]
        DataValidation[Data Validation<br/>Integrity Checks]
    end
    
    subgraph "Hub Aggregation"
        DataAggregator[Data Aggregator<br/>Multi-device Sync]
        SessionManager[Session Manager<br/>Metadata Coordination]
        ExportEngine[Export Engine<br/>Analysis Ready Format]
    end
    
    ThermalSensor --> ThermalProcessor
    GSRSensor --> GSRProcessor
    RGBSensor --> RGBProcessor
    
    ThermalProcessor --> TimestampSync
    GSRProcessor --> TimestampSync
    RGBProcessor --> TimestampSync
    
    TimestampSync --> DataBuffer
    DataBuffer --> QualityCheck
    QualityCheck --> LocalStorage
    
    LocalStorage --> SessionMetadata
    SessionMetadata --> DataValidation
    DataValidation --> DataAggregator
    
    DataAggregator --> SessionManager
    SessionManager --> ExportEngine
```

### Data Format Specifications

#### Session Directory Structure
```
sessions/
+-- session_YYYY-MM-DD_HH-MM-SS/           # Session timestamp
    +-- metadata.json                       # Session configuration
    +-- session_summary.json                # Session statistics
    +-- device_001/                         # First Android device
        +-- thermal_data.csv               # Temperature matrices
        +-- gsr_data.csv                   # GSR measurements
        +-- rgb_video.mp4                  # RGB video recording
        +-- rgb_frames/                     # Individual video frames
            +-- frame_000001.jpg
            +-- ...
        +-- device_metadata.json           # Device-specific info
    +-- device_002/                         # Second Android device
        +-- ...                             # Same structure
    +-- synchronization.json                # Cross-device sync data
```

#### Data Format Examples

**Thermal Data CSV**:
```csv
timestamp,frame_id,width,height,min_temp,max_temp,avg_temp,temperature_matrix
1641234567123,1,256,192,18.5,37.2,24.8,"[[18.5,18.7,...],[19.2,19.4,...]]"
1641234567223,2,256,192,18.4,37.3,24.9,"[[18.4,18.6,...],[19.1,19.3,...]]"
```

**GSR Data CSV**:
```csv
timestamp,conductance,resistance,ppg_value,signal_quality
1641234567123,2.45,0.408,1024.3,good
1641234567131,2.47,0.405,1026.1,good
```

## Component Library Architecture

### Library Dependency Graph

```mermaid
graph TB
    subgraph "Application Layer"
        App[Android Application<br/>Main APK]
        PCController[PC Controller<br/>Python Application]
    end
    
    subgraph "Feature Components"
        ThermalIR[Thermal-IR Component<br/>thermal-ir module]
        GSRRecording[GSR Recording Component<br/>gsr-recording module]
        PseudoComponent[Pseudo Component<br/>pseudo module]
        ThermalComponent[Thermal Component<br/>thermal module]
        UserComponent[User Component<br/>user module]
    end
    
    subgraph "Core Libraries"
        LibApp[libapp<br/>Application Framework]
        LibCom[libcom<br/>Communication]
        LibIR[libir<br/>IR Processing]
        LibUI[libui<br/>UI Components]
        LibMatrix[libmatrix<br/>Matrix Operations]
        LibMenu[libmenu<br/>Menu System]
    end
    
    subgraph "External Dependencies"
        BleModule[BLE Module<br/>Bluetooth Integration]
        RangeSeekBar[Range Seek Bar<br/>Custom UI Control]
        AndroidSDK[Android SDK<br/>Platform APIs]
        OpenCV[OpenCV<br/>Computer Vision]
    end
    
    App --> LibApp
    App --> LibUI
    App --> LibCom
    App --> LibIR
    App --> BleModule
    
    ThermalIR --> LibIR
    ThermalIR --> LibMatrix
    ThermalIR --> LibUI
    
    GSRRecording --> LibCom
    GSRRecording --> BleModule
    GSRRecording --> LibApp
    
    LibApp --> LibCom
    LibUI --> LibMatrix
    LibIR --> LibMatrix
    LibIR --> OpenCV
    
    PCController -.->|Network Protocol| LibCom
```

### Library Responsibilities

#### Core Libraries

**libir - Infrared Processing Library**
- Thermal camera hardware abstraction
- Temperature data processing and calibration
- Image format conversion and enhancement
- Native code integration for performance

**libcom - Communication Library** (`consolidated_libraries/libcom/`)
- Cross-platform networking implementation
- JSON protocol handling
- mDNS service discovery
- TCP connection management

**libapp - Application Framework**
- Android application lifecycle management
- Configuration and settings persistence
- Resource management and optimization
- Cross-component integration

**libui - User Interface Library**
- Custom Android UI components
- Material Design implementation
- Real-time data visualization widgets
- Responsive layout management

**libmatrix - Matrix Operations Library** (`consolidated_libraries/libmatrix/`)
- High-performance matrix operations
- Image processing algorithms
- Mathematical computation utilities
- Memory-optimized data structures

## Security Architecture

### Security Layers

```mermaid
graph TB
    subgraph "Application Security"
        PermissionModel[Android Permission Model<br/>Runtime Permissions]
        DataEncryption[Data Encryption<br/>AES-256 for Sensitive Data]
        SecureStorage[Secure Storage<br/>Android Keystore]
        InputValidation[Input Validation<br/>Message Sanitization]
    end
    
    subgraph "Network Security"
        NetworkIsolation[Network Isolation<br/>Local Network Only]
        MessageValidation[Message Validation<br/>Schema Enforcement]
        ConnectionAuth[Connection Authentication<br/>Device Verification]
        HeartbeatSecurity[Heartbeat Security<br/>Liveness Verification]
    end
    
    subgraph "Data Security"
        AccessControl[Access Control<br/>File Permissions]
        DataIntegrity[Data Integrity<br/>Checksums + Validation]
        PrivacyProtection[Privacy Protection<br/>Data Anonymization]
        SecureDeletion[Secure Deletion<br/>Data Cleanup]
    end
    
    subgraph "Hardware Security"
        USBSecurity[USB Security<br/>Device Validation]
        BLESecurity[BLE Security<br/>Pairing Protection]
        SensorValidation[Sensor Validation<br/>Hardware Authentication]
        TamperDetection[Tamper Detection<br/>Integrity Monitoring]
    end
    
    PermissionModel --> NetworkIsolation
    DataEncryption --> MessageValidation
    SecureStorage --> ConnectionAuth
    InputValidation --> HeartbeatSecurity
    
    NetworkIsolation --> AccessControl
    MessageValidation --> DataIntegrity
    ConnectionAuth --> PrivacyProtection
    HeartbeatSecurity --> SecureDeletion
    
    AccessControl --> USBSecurity
    DataIntegrity --> BLESecurity
    PrivacyProtection --> SensorValidation
    SecureDeletion --> TamperDetection
```

## Performance Architecture

### Performance Optimization Strategy

#### Multi-threading Model
```mermaid
graph TB
    subgraph "PC Controller Threading"
        MainThread[Main GUI Thread<br/>PyQt6 Event Loop]
        NetworkThread[Network Thread<br/>TCP Server + Discovery]
        DataThread[Data Processing Thread<br/>Aggregation + Analysis]
        StorageThread[Storage Thread<br/>File I/O Operations]
    end
    
    subgraph "Android Threading"
        UIThread[UI Thread<br/>Android Main Thread]
        SensorThread[Sensor Thread<br/>Data Collection Loop]
        NetworkThread2[Network Thread<br/>Hub Communication]
        ProcessingThread[Processing Thread<br/>Data Processing]
    end
    
    MainThread <-.->|Event Bus| NetworkThread
    MainThread <-.->|Event Bus| DataThread
    DataThread <-.->|Queue| StorageThread
    
    UIThread <-.->|Handler| SensorThread
    UIThread <-.->|Handler| NetworkThread2
    SensorThread <-.->|Buffer| ProcessingThread
    ProcessingThread <-.->|Queue| NetworkThread2
```

#### Memory Management
- **Object Pooling**: Reuse of frequently allocated objects
- **Streaming Processing**: Process data in chunks to minimize memory footprint
- **Garbage Collection Optimization**: Minimize allocation in performance-critical paths
- **Native Code Integration**: Use native code for computationally intensive operations

#### Network Optimization
- **Connection Pooling**: Reuse TCP connections for multiple messages
- **Message Batching**: Combine multiple small messages into larger packets
- **Compression**: Optional data compression for large transfers
- **Priority Queues**: Prioritize time-critical messages

## Build System Architecture

### Gradle Build Structure

```mermaid
graph TB
    subgraph "Root Project"
        RootBuild[Root build.gradle.kts<br/>Global Configuration]
        Settings[settings.gradle.kts<br/>Module Configuration]
        VersionCatalog[gradle/libs.versions.toml<br/>Dependency Management]
    end
    
    subgraph "Application Modules"
        AppModule[app module<br/>Main Android Application]
        ComponentModules[Component Modules<br/>thermal-ir, gsr-recording, etc.]
    end
    
    subgraph "Library Modules"
        CoreLibraries[Core Libraries<br/>libir, libcom, libapp, etc.]
        ExternalModules[External Modules<br/>BLE, RangeSeekBar]
    end
    
    subgraph "Build Tools"
        DevScript[dev.sh<br/>Development Tools]
        BuildScript[scripts/build.sh<br/>Unified Build System]
        CIWorkflow[GitHub Actions<br/>CI/CD Pipeline]
    end
    
    RootBuild --> AppModule
    RootBuild --> ComponentModules
    RootBuild --> CoreLibraries
    Settings --> RootBuild
    VersionCatalog --> RootBuild
    
    AppModule --> CoreLibraries
    ComponentModules --> CoreLibraries
    ComponentModules --> ExternalModules
    
    DevScript --> RootBuild
    BuildScript --> RootBuild
    CIWorkflow --> DevScript
```

### Build Process Flow

1. **Configuration Phase**: Gradle resolves dependencies and configures modules
2. **Compilation Phase**: Kotlin/Java source code compilation
3. **Resource Processing**: Android resources and assets processing
4. **Library Integration**: Native library integration and packaging
5. **APK Assembly**: Final APK generation and signing
6. **Validation Phase**: Quality checks and testing

## Monitoring and Observability

### System Monitoring Architecture

```mermaid
graph TB
    subgraph "Application Monitoring"
        PerformanceMetrics[Performance Metrics<br/>CPU, Memory, Network]
        ErrorTracking[Error Tracking<br/>Exception Logging]
        UserInteraction[User Interaction<br/>Usage Analytics]
        SystemHealth[System Health<br/>Component Status]
    end
    
    subgraph "Network Monitoring"
        ConnectionHealth[Connection Health<br/>Latency, Packet Loss]
        ThroughputMetrics[Throughput Metrics<br/>Data Transfer Rates]
        DeviceStatus[Device Status<br/>Online/Offline State]
        ProtocolMetrics[Protocol Metrics<br/>Message Success Rate]
    end
    
    subgraph "Data Quality Monitoring"
        SensorQuality[Sensor Quality<br/>Signal Integrity]
        SyncAccuracy[Synchronization Accuracy<br/>Timing Precision]
        DataIntegrity[Data Integrity<br/>Validation Results]
        StorageHealth[Storage Health<br/>Disk Usage, I/O Performance]
    end
    
    subgraph "Logging and Alerting"
        CentralizedLogging[Centralized Logging<br/>Structured Log Aggregation]
        RealTimeAlerts[Real-time Alerts<br/>Critical Issue Notification]
        LogAnalysis[Log Analysis<br/>Pattern Detection]
        ReportGeneration[Report Generation<br/>System Status Reports]
    end
    
    PerformanceMetrics --> CentralizedLogging
    ErrorTracking --> RealTimeAlerts
    ConnectionHealth --> LogAnalysis
    SensorQuality --> ReportGeneration
```

## Future Architecture Considerations

### Scalability Enhancements
- **Cloud Integration**: AWS/Azure integration for large-scale data processing
- **Container Deployment**: Docker containerization for easy deployment
- **Microservices**: Breaking monolithic components into microservices
- **Load Balancing**: Support for multiple PC Controller hubs

### Technology Evolution
- **5G Connectivity**: Enhanced mobile connectivity for real-time streaming
- **Edge Computing**: On-device AI processing for real-time analysis
- **WebRTC**: Browser-based device connectivity
- **GraphQL**: More flexible API layer for data queries

### Security Enhancements
- **Zero Trust Architecture**: Comprehensive security model
- **End-to-End Encryption**: Full data encryption pipeline
- **Identity Management**: Centralized authentication system
- **Audit Logging**: Comprehensive activity logging

---

**Status**: [DONE] Complete Architecture Documentation  
**Last Updated**: Documentation Consolidation v1.0  
**Scope**: System-wide architecture coverage  
**Maintenance**: Update when making architectural changes or adding new components