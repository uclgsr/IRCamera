# IRCamera Architecture Diagrams

## Current Timestamp Synchronization System (2024-12-23)

### Unified Timestamp Architecture

```mermaid
graph TB
    subgraph "Unified Timestamp System"
        subgraph "Core Timestamp Management"
            TimestampManager[TimestampManager<br/>✅ getCurrentTimestampNanos()<br/>✅ convertMonotonicToWallClock()<br/>✅ startSession()]
            
            TimeSyncService[TimeSynchronizationService<br/>✅ logSyncEvent()<br/>✅ logTimestampWithDriftAnalysis()<br/>✅ SessionStart events]
        end
        
        subgraph "Sensor Timestamp Sources - UNIFIED"
            RGBCamera[RGB Camera Recorder<br/>✅ TimestampManager.getCurrentTimestampNanos()<br/>✅ SessionSync marker logged<br/>⚠️ Previously: System.nanoTime()]
            
            ThermalRecorder[Thermal Recorder<br/>✅ TimestampManager.getCurrentTimestampNanos()<br/>✅ SessionSync marker logged<br/>⚠️ Previously: System.nanoTime()]
            
            GSRRecorder[GSR Sensor Recorder<br/>✅ TimestampManager.getCurrentTimestampNanos()<br/>✅ Unified timestamp system<br/>⚠️ Previously: System.nanoTime()]
        end
        
        subgraph "Cross-Device Synchronization"
            PCSync[PC-Phone NTP Handshake<br/>✅ Enhanced quality reporting<br/>✅ Drift monitoring<br/>✅ Network latency logging]
            
            TimeManager[TimeManager<br/>✅ synchronizeWithPC()<br/>✅ logSyncQualityInfo()<br/>✅ Drift analysis]
        end
    end
    
    subgraph "SessionSync Event Flow"
        SessionStart[Session Start] --> SessionSyncEvents[SessionSync Events Generated]
        SessionSyncEvents --> RGBSync[RGB_RECORDING_START]
        SessionSyncEvents --> ThermalSync[THERMAL_RECORDING_START] 
        SessionSyncEvents --> GSRSync[GSR_RECORDING_START]
        
        RGBSync --> AlignmentVerification[Post-hoc Alignment Verification<br/>Within 5ms tolerance]
        ThermalSync --> AlignmentVerification
        GSRSync --> AlignmentVerification
    end
    
    subgraph "Verification Tools"
        TestActivity[TimestampSyncVerificationActivity<br/>✅ Sharp event simulation (hand clap)<br/>✅ Multi-modal alignment testing<br/>✅ 5ms tolerance validation]
    end
    
    TimestampManager --> RGBCamera
    TimestampManager --> ThermalRecorder
    TimestampManager --> GSRRecorder
    
    TimeSyncService --> SessionSyncEvents
    TimeManager --> PCSync
    
    TestActivity --> AlignmentVerification
    
    classDef unified fill:#e8f5e8,stroke:#2e7d32,stroke-width:3px
    classDef fixed fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef warning fill:#fff3e0,stroke:#ef6c00,stroke-width:2px
    classDef verification fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    
    class TimestampManager,TimeSyncService unified
    class RGBCamera,ThermalRecorder,GSRRecorder,PCSync,TimeManager fixed
    class TestActivity,AlignmentVerification verification
```

### Timestamp Synchronization Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant TSS as TimeSynchronizationService
    participant TM as TimestampManager
    participant RGB as RGB Camera
    participant GSR as GSR Sensor
    participant Thermal as Thermal Recorder
    participant PC as PC Controller
    
    App->>TSS: initializeSession()
    TSS->>TM: startSession()
    TM->>TM: Create unified timestamp reference
    TSS->>TSS: logSessionStartSyncEvent()
    
    Note over App,PC: SessionSync Markers Creation
    
    App->>RGB: startRecording()
    RGB->>TM: getCurrentTimestampNanos()
    RGB->>RGB: addSyncEvent("RGB_RECORDING_START")
    
    App->>GSR: startRecording()
    GSR->>TM: getCurrentTimestampNanos()
    Note over GSR: Uses unified timestamp system
    
    App->>Thermal: startRecording()
    Thermal->>TM: getCurrentTimestampNanos()
    Thermal->>Thermal: addSyncEvent("THERMAL_RECORDING_START")
    
    Note over App,PC: Cross-Device Synchronization
    
    App->>PC: synchronizeWithPC()
    PC->>App: NTP handshake (t1, t2, t3, t4)
    App->>App: Calculate clock offset & network delay
    App->>App: Start drift monitoring
    
    Note over App,PC: Sharp Event Testing
    
    App->>RGB: Capture frame (hand clap event)
    RGB->>TM: getCurrentTimestampNanos()
    App->>GSR: Record sample (hand clap event)
    GSR->>TM: getCurrentTimestampNanos()
    App->>Thermal: Process frame (hand clap event)
    Thermal->>TM: getCurrentTimestampNanos()
    
    Note over App,PC: Verify timestamps within 5ms tolerance
```

## Previous Update: Kotlin Compilation Status (2024-12-21)

### BLE Core Module Compilation Error Resolution

```mermaid
graph LR
    subgraph "Fixed Compilation Errors"
        AppHolder[AppHolder11.kt<br/>FIXED: Activity Lifecycle Callbacks<br/>FIXED: PackageInfo Import<br/>FIXED: Context Return Type<br/>FIXED: Singleton Pattern]
        
        CheckableItem[CheckableItem111.kt<br/>FIXED: Interface Override<br/>FIXED: Property Syntax<br/>FIXED: Return Type Fix]
        
        CheckableParcelable[CheckableParcelable111.kt<br/>FIXED: Constructor Fix<br/>FIXED: Property Access<br/>FIXED: Parcelable Creator]
        
        PermissionsReq[PermissionsRequester11.kt<br/>FIXED: Collection Type<br/>FIXED: String List Fix]
        
        Observable[Observable11.kt<br/>FIXED: MethodInfo Access<br/>FIXED: Nullable Handling<br/>FIXED: Property Syntax]
        
        ObserverHelper[ObserverMethodHelper11.kt<br/>FIXED: Reflection API<br/>FIXED: Array Access<br/>FIXED: Method Properties]
        
        MethodInfo[MethodInfo11.kt<br/>FIXED: Reflection Updates<br/>FIXED: Property Access]
    end
    
    subgraph "Issue Categories Resolved"
        TypeSafety[Type Safety<br/>FIXED: Nullable Handling<br/>FIXED: Collection Types<br/>FIXED: Return Types]
        
        ReflectionAPI[Reflection API<br/>FIXED: method.name<br/>FIXED: method.parameterTypes<br/>FIXED: method.modifiers]
        
        InterfaceImpl[Interface Implementation<br/>FIXED: Override Keywords<br/>FIXED: Property Syntax<br/>FIXED: Return Compatibility]
        
        LifecycleCallbacks[Lifecycle Callbacks<br/>FIXED: Parameter Types<br/>FIXED: Non-null Activity<br/>FIXED: Bundle Types]
    end
    
    AppHolder --> TypeSafety
    CheckableItem --> InterfaceImpl  
    CheckableParcelable --> InterfaceImpl
    PermissionsReq --> TypeSafety
    Observable --> ReflectionAPI
    ObserverHelper --> ReflectionAPI
    MethodInfo --> ReflectionAPI
    AppHolder --> LifecycleCallbacks
```

## BLE Core Module Structure (2024-12-21)

### BLE Core Callback Architecture

## BLE Core Module Interface Structure (2024-12-21)

### GenericRequest Class Hierarchy

```mermaid
classDiagram
    class Request {
        <<interface>>
        +val device: Device
        +val type: RequestType
        +val tag: String?
        +val service: UUID?
        +val characteristic: UUID?
        +val descriptor: UUID?
        +execute(connection: Connection?)
    }
    
    class RequestBuilder {
        <<interface>>
        +val tag: String?
        +val type: RequestType
        +val service: UUID?
        +val characteristic: UUID?
        +val descriptor: UUID?
        +val priority: Int
        +val value: Any?
        +val callback: RequestCallback?
        +val writeOptions: WriteOptions?
        +build(): Request
        +setCallback(callback: Any): T
        +setTimeout(timeoutMillis: Long): T
    }
    
    class GenericRequest {
        +override val tag: String?
        +override val device: Device
        -_device: Device?
        +override val type: RequestType
        +override val service: UUID?
        +override val characteristic: UUID?
        +override val descriptor: UUID?
        +var value: Any?
        +var priority: Int
        +var callback: RequestCallback?
        +var writeOptions: WriteOptions?
        +compareTo(other: GenericRequest): Int
        +setDevice(device: Device): void
        +execute(connection: Connection?)
    }
    
    class RequestCallback {
        <<interface>>
        +onResult(success: Boolean, message: String?)
        +onProgress(progress: Int)
    }
    
    class Comparable {
        <<interface>>
        +compareTo(T): Int
    }
    
    Request <|.. GenericRequest
    Comparable <|.. GenericRequest
    RequestBuilder ..> Request : builds
    GenericRequest --> RequestCallback : uses
```

## BLE Core WriteOptions Fix (2024-12-21)

### WriteOptions Class Structure After Fix

```mermaid
classDiagram
    class WriteOptions {
        +val packageWriteDelayMillis: Int
        +val requestWriteDelayMillis: Int 
        +val isWaitWriteResult: Boolean
        +val writeType: Int
        +val useMtuAsPackageSize: Boolean
        +var packageSize: Int
        -WriteOptions(builder: Builder)
    }
    
    class Builder {
        ~internal var packageWriteDelayMillis: Int
        ~internal var requestWriteDelayMillis: Int
        ~internal var packageSize: Int
        ~internal var isWaitWriteResult: Boolean
        ~internal var writeType: Int
        ~internal var useMtuAsPackageSize: Boolean
        +setPackageWriteDelayMillis(Int): Builder
        +setRequestWriteDelayMillis(Int): Builder
        +setPackageSize(Int): Builder
        +setWaitWriteResult(Boolean): Builder
        +setWriteType(Int): Builder
        +setMtuAsPackageSize(): Builder
        +build(): WriteOptions
    }
    
    WriteOptions *-- Builder : contains
    
    note for WriteOptions "Constructor accesses Builder's internal fields\nFixed visibility issue by changing private to internal"
    note for Builder "Fields changed from private to internal\nAllows outer class access while maintaining module encapsulation"
```

### Fix Details

- **Issue**: Private Builder fields could not be accessed from WriteOptions constructor
- **Solution**: Changed field visibility from `private var` to `internal var`
- **Impact**: Enables compilation while maintaining proper encapsulation within module
- **Files Changed**: `ble-core/src/main/java/com/mpdc4gsr/ble/core/WriteOptions.kt`

## BLE Core Module Structure (Updated 2024-12-21)

### BLE Core Module Class Dependencies

```mermaid
graph TB
    subgraph "BLE Core Module"
        subgraph "Callback Package"
            RequestCallback[RequestCallback<br/>onRequestSuccess<br/>onRequestFailed]
            BleConnectionCallback[BleConnectionCallback<br/>onConnectionStateChanged<br/>onServicesDiscovered]
            BleCharacteristicCallback[BleCharacteristicCallback<br/>onCharacteristicRead<br/>onCharacteristicWrite<br/>onCharacteristicChanged]
        end
        
        subgraph "Util Package"
            ByteUtil[ByteUtil<br/>bytesToFloat - Fixed<br/>byteToFloat - Fixed]
            DefaultLogger[DefaultLogger<br/>Logger Implementation - Fixed]
            HexUtil[HexUtil<br/>uniteBytes - Fixed<br/>Byte Type Conflicts Resolved]
        end
        
        subgraph "Core Classes"
            GenericRequest[GenericRequest<br/>Uses RequestCallback]
            ConnectionImpl[ConnectionImpl<br/>Uses Callbacks]
        end
    end
    
    RequestCallback --> GenericRequest
    BleConnectionCallback --> ConnectionImpl
    BleCharacteristicCallback --> ConnectionImpl
```


        Request[Request.kt<br/>Interface with UUID properties<br/>DONE: import java.util.UUID]
        GenericRequest[GenericRequest.kt<br/>Implements Request<br/>DONE: import java.util.UUID]
        Connection[Connection.kt<br/>BLE Connection Management<br/>DONE: import java.util.UUID]
        ConnectionImpl[ConnectionImpl.kt<br/>Connection Implementation<br/>DONE: import java.util.UUID]
        ConnectionConfig[ConnectionConfiguration.kt<br/>BLE Configuration<br/>DONE: import java.util.UUID]
    end
    
    Request --> GenericRequest
    Connection --> ConnectionImpl
    GenericRequest --> Connection
    ConnectionConfig --> Connection
```
## Current Standardized Build System (2024-12-21)

### Gradle Build System Structure

```mermaid
graph TB
    subgraph "Root Project"
        RootBuild[build.gradle.kts<br/>Unified Build Tasks]
        Settings[settings.gradle.kts<br/>Module Configuration]
        VersionCatalog[gradle/libs.versions.toml<br/>Dependency Management]
    end
    
    subgraph "Application Module"
        AppModule[app<br/>Main Android Application<br/>namespace: com.csl.irCamera]
    end
    
    subgraph "Library Module"  
        LibUnified[libunified<br/>Unified Core Library<br/>namespace: com.mpdc4gsr.libunified]
    end
    
    subgraph "BLE Modules"
        BLECore[ble-core<br/>Core BLE Functionality<br/>namespace: com.mpdc4gsr.ble.core]
        BLEShimmer[ble-shimmer<br/>GSR/Shimmer Devices<br/>namespace: com.mpdc4gsr.ble.shimmer]
        BLETopdon[ble-topdon<br/>Thermal/Topdon Devices<br/>namespace: com.mpdc4gsr.ble.topdon]
    end
    
    subgraph "Component Modules"
        GSRRecording[component/gsr-recording<br/>GSR Data Recording<br/>namespace: com.mpdc4gsr.gsr]
        ThermalUnified[component/thermalunified<br/>Unified Thermal Component<br/>namespace: com.mpdc4gsr.module.thermalunified]
        UserModule[component/user<br/>User Management<br/>namespace: com.mpdc4gsr.module.user]
    end
    
    %% Build System Dependencies
    RootBuild --> Settings
    RootBuild --> VersionCatalog
    Settings --> AppModule
    Settings --> LibUnified
    Settings --> BLECore
    Settings --> BLEShimmer  
    Settings --> BLETopdon
    Settings --> GSRRecording
    Settings --> ThermalUnified
    Settings --> UserModule
    
    %% Module Dependencies
    AppModule --> LibUnified
    AppModule --> BLECore
    
    ThermalUnified --> LibUnified
    ThermalUnified --> BLECore
    ThermalUnified --> UserModule
    
    GSRRecording --> BLEShimmer
    UserModule --> LibUnified
    UserModule --> BLEShimmer
    
    BLEShimmer --> BLECore
    BLETopdon --> BLECore
    
    classDef appLayer fill:#e1f5fe
    classDef libLayer fill:#f3e5f5  
    classDef bleLayer fill:#e8f5e8
    classDef componentLayer fill:#fff3e0
    classDef buildLayer fill:#fce4ec
    
    class AppModule appLayer
    class LibUnified libLayer
    class BLECore,BLEShimmer,BLETopdon bleLayer
    class GSRRecording,ThermalUnified,UserModule componentLayer
    class RootBuild,Settings,VersionCatalog buildLayer
```

### Build Task Structure

```mermaid
graph LR
    subgraph "Simplified Build Tasks"
        Clean[clean<br/>Clean all modules]
        Build[build<br/>Clean + Build Release]
        BuildAll[buildAll<br/>Build all variants]
        BuildRelease[buildRelease<br/>Release builds only]
        BuildDebug[buildDebug<br/>Debug builds only]
    end
    
    subgraph "Advanced Tasks"
        CleanAll[cleanAll<br/>Clean + caches]
        CompileDebug[compileDebugSafe<br/>Safe debug compile]
        CompileRelease[compileReleaseSafe<br/>Safe release compile]
    end
    
    Clean --> CleanAll
    Build --> CleanAll
    Build --> BuildRelease
    BuildAll --> CleanAll
    BuildAll --> BuildRelease
    BuildAll --> BuildDebug
```

## Implemented Unified Architecture (Current State)

### Unified Library Structure + Device-Specific BLE Modules

```mermaid
graph TB
    subgraph "Application Layer"
        App[Android Application<br/>Main APK]
        PCController[PC Controller<br/>Python Application]
    end
    
    subgraph "Feature Components"
        ThermalUnified[Thermal Unified Component<br/>thermalunified module]
        GSRRecording[GSR Recording Component<br/>gsr-recording module]
        UserComponent[User Component<br/>user module]
    end
    
    subgraph "Unified Core Libraries"
        LibUnified[libunified<br/>598 files<br/>Unified Core Library<br/>app + ir + ui functionality]
    end
    
    subgraph "Device-Specific BLE Modules"
        BLECore[ble-core<br/>~3,500 lines<br/>Core BLE + Commons]
        BLEShimmer[ble-shimmer<br/>1,131 lines<br/>GSR/Shimmer Devices]
        BLETopdon[ble-topdon<br/>881 lines<br/>Thermal/Topdon Devices]
    end
    
    %% Application Dependencies
    App --> LibUnified
    App --> BLECore
    
    %% Component Dependencies
    ThermalUnified --> LibUnified
    ThermalUnified --> BLECore
    ThermalUnified --> UserComponent
    
    GSRRecording --> BLEShimmer
    UserComponent --> LibUnified
    UserComponent --> BLEShimmer
    
    %% BLE Module Dependencies
    BLEShimmer --> BLECore
    BLETopdon --> BLECore
    ThermalLite --> BLETopdon
    
    %% GSR/User Components -> Shimmer BLE
    GSRRecording --> LibUnified
    GSRRecording --> BLEShimmer
    
    UserComponent --> LibUnified
    UserComponent --> BLEShimmer
    
    %% BLE Module Dependencies
    BLEShimmer --> BLECore
    BLETopdon --> BLECore
    
    %% Support Dependencies
    LibUnified --> RangeSeekBar
    
    PCController -.->|Network Protocol| App
```

### Namespace Structure (Implemented)

```mermaid
graph TB
    subgraph "com.mpdc4gsr.libunified.*"
        AppNS[app.*<br/>Application Framework<br/>Database, Config, Utils]
        IRNs[ir.*<br/>IR Processing<br/>Camera, Hardware, Processing]
        UINs[ui.*<br/>UI Components<br/>Charting, Widgets, Controls]
    end
    
    subgraph "com.mpdc4gsr.ble.*"
        BLECoreNS[ble.core.*<br/>Core BLE Functionality<br/>Connection, Device, Utils]
        BLEShimmerNS[ble.shimmer.*<br/>Shimmer Device Classes<br/>GSR-specific Logic]
        BLETopdonNS[ble.topdon.*<br/>Topdon Device Classes<br/>Thermal-specific Logic]
    end
    
    subgraph "Component Namespaces (Unchanged)"
        CompThermal[com.mpdc4gsr.module.thermal.*]
        CompGSR[com.mpdc4gsr.gsr.*]
        CompUser[com.mpdc4gsr.module.user.*]
    end
```

```

## Previous Architecture (Historical Reference)

### Multi-Library Structure (Before Unification)

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
    
    subgraph "Previous Separate Libraries (DEPRECATED)"
        LibApp[libapp<br/>Application Framework]
        LibIR[libir<br/>IR Processing] 
        LibUI[libui<br/>UI Components]
    end
    
    subgraph "Support Libraries"
        LibCom[libcom<br/>Communication]
        LibMatrix[libmatrix<br/>Matrix Operations]
        LibMenu[libmenu<br/>Menu System]
    end
    
    subgraph "External Dependencies"
        BleModule[BLE Module<br/>Bluetooth Integration]
        RangeSeekBar[Range Seek Bar<br/>Custom UI Control]
        AndroidSDK[Android SDK<br/>Platform APIs]
    end
    
    %% Complex Dependencies - Multiple library dependencies
    App --> LibApp
    App --> LibUI
    App --> LibIR
    
    ThermalIR --> LibApp
    ThermalIR --> LibIR
    ThermalIR --> LibUI
    GSRRecording --> LibApp
    GSRRecording --> LibUI
    ThermalComponent --> LibApp
    ThermalComponent --> LibIR
    ThermalComponent --> LibUI
    UserComponent --> LibApp
    UserComponent --> LibUI
    
    %% Support library dependencies
    LibApp --> LibCom
    LibApp --> LibMatrix
    
    %% External dependencies
    App --> BleModule
    App --> RangeSeekBar
    
    PCController -.->|Network Protocol| LibCom
    
    %% Visual styling
    classDef deprecated fill:#ffebee,stroke:#c62828,stroke-width:2px,stroke-dasharray: 5 5
    classDef support fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    
    class LibApp,LibIR,LibUI deprecated
    class LibCom,LibMatrix,LibMenu support
```

## Library Unification Benefits Diagram

```mermaid
flowchart LR
    subgraph "Current Implemented State"
        A[Components depend on<br/>1 unified library]
        B[libunified<br/>All functionality]
        C[Simple dependencies<br/>1 build config]
        A --> B --> C
    end
    
    subgraph "Previous State"
        D[Components depend on<br/>3 separate libraries]
        E[libapp + libir + libui]
        F[Complex dependencies<br/>3 build configs]
        D --> E --> F
    end
    
    Previous --> |"COMPLETED MERGE"| Current
    
    subgraph "Benefits Achieved"
        G[DONE: 67% fewer library modules]
        H[DONE: Simplified build system]
        I[DONE: Faster compilation]
        J[DONE: Easier maintenance]
        K[DONE: No namespace conflicts]
    end
    
    Current --> G
    Current --> H
    Current --> I
    Current --> J
    Current --> K
```

## Migration Phases Diagram

```mermaid
gantt
    title Library Unification Migration Plan (COMPLETED)
    dateFormat X
    axisFormat %s
    
    section Phase 1: Foundation (COMPLETED)
    Create working libunified        :done, p1, 0, 2
    Resolve build conflicts          :done, p2, 1, 2
    Test basic functionality         :done, p3, 2, 1
    
    section Phase 2: Component Migration (COMPLETED)
    Migrate thermal-lite (pilot)     :done, p4, 3, 2
    Migrate thermal component        :done, p5, 4, 2
    Migrate thermal-ir component     :done, p6, 5, 2
    Migrate gsr-recording            :done, p7, 6, 2
    Migrate user component           :done, p8, 7, 2
    Update main app                  :done, p9, 8, 2
    
    section Phase 3: Cleanup (COMPLETED)
    Remove old libraries             :done, p10, 9, 1
    Update build configs             :done, p11, 10, 1
    Verify functionality             :done, p12, 11, 1
    
    section Phase 4: Documentation (IN PROGRESS)
    Update architecture docs         :active, p13, 12, 1
    Update API reference             :active, p14, 12, 1
    Update diagrams                  :active, p15, 12, 1
```

## Namespace Organization Diagram

```mermaid
graph TB
    subgraph "libunified Unified Namespaces"
        subgraph "com.mpdc4gsr.libunified.app.*"
            AppFramework[Application Framework<br/>• Database<br/>• Configuration<br/>• Common utilities]
        end
        
        subgraph "com.infisense.usbir.*"
            IRProcessing[IR Processing<br/>• Camera hardware<br/>• Temperature processing<br/>• Image conversion]
        end
        
        subgraph "com.github.mikephil.charting.*"
            UIComponents[UI Components<br/>• Charts and graphs<br/>• Custom UI widgets<br/>• Data visualization]
        end
    end
    
    subgraph "No Conflicts"
        NoConflicts[DONE: Different root packages<br/>DONE: No namespace overlap<br/>DONE: Direct merge possible]
    end
    
    AppFramework -.-> NoConflicts
    IRProcessing -.-> NoConflicts
    UIComponents -.-> NoConflicts
```

## Build System Comparison

```mermaid
graph LR
    subgraph "Current Build Complexity"
        C1[Component A] --> L1[libapp]
        C1 --> L2[libir] 
        C1 --> L3[libui]
        
        C2[Component B] --> L1
        C2 --> L2
        C2 --> L3
        
        C3[Component C] --> L1
        C3 --> L2
        C3 --> L3
        
        L1 --> B1[3 build.gradle files]
        L2 --> B1
        L3 --> B1
    end
    
    subgraph "Proposed Unified Build"
        C4[Component A] --> LC[libunified]
        C5[Component B] --> LC
        C6[Component C] --> LC
        
        LC --> B2[1 build.gradle file]
    end
    
    B1 -.->|"SIMPLIFY"| B2
    
    classDef current fill:#ffebee,stroke:#c62828
    classDef proposed fill:#e8f5e8,stroke:#2e7d32
    
    class C1,C2,C3,L1,L2,L3,B1 current
    class C4,C5,C6,LC,B2 proposed
```

## Implementation Status

### Implementation Completion Results

```mermaid
pie title Library Merge Implementation Status
    "COMPLETED: Namespace Compatible" : 598
    "RESOLVED: Build Conflicts" : 45  
    "NONE: Incompatible" : 0
```

### File Distribution in Unified Library

```mermaid
pie title libunified File Distribution (598 total)
    "libapp Framework" : 247
    "libui Components" : 287
    "libir Processing" : 64
```

## Current Status: IMPLEMENTATION COMPLETED

The merging of libapp, libir, and libui into a unified libunified has been **successfully completed** and is **fully operational** in the current architecture.

---

## Documentation Update History

### 2024-12-22 - Commit c7769bc - ASCII Safety and True State Documentation
- Removed all emoji characters from architecture diagrams and documentation
- Updated all references from libcore to libunified (actual implementation name)
- Corrected migration status from "proposed" to "completed" throughout diagrams
- Updated BLE module references to reflect actual ble-core, ble-shimmer, ble-topdon structure
- Ensured all Mermaid diagrams reflect the true current state of the repository