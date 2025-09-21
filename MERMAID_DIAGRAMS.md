# IRCamera Architecture Diagrams

## Implemented Unified Architecture (Current State)

### Unified Library Structure + Device-Specific BLE Modules

```mermaid
graph TB
    subgraph "Application Layer"
        App[Android Application<br/>Main APK]
        PCController[PC Controller<br/>Python Application]
    end
    
    subgraph "Feature Components"
        ThermalIR[Thermal-IR Component<br/>thermal-ir module]
        GSRRecording[GSR Recording Component<br/>gsr-recording module]
        ThermalComponent[Thermal Component<br/>thermal module]
        ThermalLite[Thermal-Lite Component<br/>thermal-lite module]
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
    
    subgraph "Support Libraries"
        RangeSeekBar[RangeSeekBar<br/>Custom UI Control]
        ConsolidatedLibs[consolidated_libraries<br/>Shared Utilities]
    end
    
    %% Application Dependencies
    App --> LibUnified
    App --> BLECore
    App --> BLEShimmer
    App --> BLETopdon
    
    %% Thermal Components -> Topdon BLE
    ThermalIR --> LibUnified
    ThermalIR --> BLETopdon
    
    ThermalComponent --> LibUnified  
    ThermalComponent --> BLETopdon
    
    ThermalLite --> LibUnified
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

## Proposed Unified Architecture

### Single Unified Library Structure

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
    
    subgraph "Unified Core Library"
        LibCore[libcore<br/>598 files<br/>📦 All Core Functionality<br/>• Application Framework<br/>• IR Processing<br/>• UI Components]
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
    
    %% Simplified Dependencies - Single unified dependency
    App --> LibCore
    
    ThermalIR --> LibCore
    GSRRecording --> LibCore
    ThermalComponent --> LibCore
    UserComponent --> LibCore
    
    %% Support library dependencies
    LibCore --> LibCom
    LibCore --> LibMatrix
    
    %% External dependencies
    LibCore --> BleModule
    App --> RangeSeekBar
    
    PCController -.->|Network Protocol| LibCom
    
    %% Visual styling
    classDef unified fill:#e1f5fe,stroke:#01579b,stroke-width:3px
    classDef current fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef support fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    
    class LibCore unified
    class LibCom,LibMatrix,LibMenu support
```

## Library Unification Benefits Diagram

```mermaid
flowchart LR
    subgraph "Current State"
        A[Components depend on<br/>3 separate libraries]
        B[libapp + libir + libui]
        C[Complex dependencies<br/>3 build configs]
        A --> B --> C
    end
    
    subgraph "Proposed State"
        D[Components depend on<br/>1 unified library]
        E[libcore<br/>All functionality]
        F[Simple dependencies<br/>1 build config]
        D --> E --> F
    end
    
    Current --> |"MERGE"| Proposed
    
    subgraph "Benefits"
        G[✅ 67% fewer library modules]
        H[✅ Simplified build system]
        I[✅ Faster compilation]
        J[✅ Easier maintenance]
        K[✅ No namespace conflicts]
    end
    
    Proposed --> G
    Proposed --> H
    Proposed --> I
    Proposed --> J
    Proposed --> K
```

## Migration Phases Diagram

```mermaid
gantt
    title Library Unification Migration Plan
    dateFormat X
    axisFormat %s
    
    section Phase 1: Foundation
    Create working libcore           :p1, 0, 2
    Resolve build conflicts          :p2, 1, 2
    Test basic functionality         :p3, 2, 1
    
    section Phase 2: Component Migration
    Migrate thermal-lite (pilot)     :p4, 3, 2
    Migrate thermal component        :p5, 4, 2
    Migrate thermal-ir component     :p6, 5, 2
    Migrate gsr-recording            :p7, 6, 2
    Migrate user component           :p8, 7, 2
    Update main app                  :p9, 8, 2
    
    section Phase 3: Cleanup
    Remove old libraries             :p10, 9, 1
    Update build configs             :p11, 10, 1
    Verify functionality             :p12, 11, 1
    
    section Phase 4: Documentation
    Update architecture docs         :p13, 12, 1
    Update API reference             :p14, 12, 1
    Update diagrams                  :p15, 12, 1
```

## Namespace Organization Diagram

```mermaid
graph TB
    subgraph "libcore Unified Namespaces"
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
        NoConflicts[✅ Different root packages<br/>✅ No namespace overlap<br/>✅ Direct merge possible]
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
        C4[Component A] --> LC[libcore]
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

### Feasibility Analysis Results

```mermaid
pie title Library Merge Feasibility
    "✅ Namespace Compatible" : 598
    "⚠️ Build Conflicts" : 45  
    "❌ Incompatible" : 0
```

### File Distribution in Unified Library

```mermaid
pie title libcore File Distribution (598 total)
    "libapp Framework" : 247
    "libui Components" : 287
    "libir Processing" : 64
```

## Current Status: READY FOR IMPLEMENTATION

The analysis confirms that merging libapp, libir, and libui into a unified libcore is **technically feasible** and **highly beneficial** for the project architecture.