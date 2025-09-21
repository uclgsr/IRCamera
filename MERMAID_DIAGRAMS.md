# Mermaid Diagrams - IRCamera Architecture

## Current Thermal Module Architecture

```mermaid
graph TB
    subgraph "IRCamera Project"
        subgraph "Core Libraries"
            LA[libapp]
            LI[libir] 
            LU[libui]
        end
        
        subgraph "Thermal Modules"
            T[thermal<br/>38 files<br/>Basic Interface]
            TIR[thermal-ir<br/>152 files<br/>Advanced Features]
            TL[thermal-lite<br/>33 files<br/>Lightweight]
        end
        
        subgraph "Hardware Support"
            GEN[Generic Thermal<br/>Cameras]
            TC001[Topdon TC001<br/>Dual Camera]
            AC020[AC020 USB<br/>IR Camera]
        end
        
        T --> LA
        T --> LI
        T --> LU
        TIR --> LA
        TIR --> LI
        TIR --> LU
        TIR --> T
        TL --> LA
        TL --> LI
        TL --> LU
        TL --> TIR
        
        T -.-> GEN
        TIR -.-> TC001
        TL -.-> AC020
    end
```

## Proposed Thermal Module Consolidation

```mermaid
graph TB
    subgraph "Recommended Architecture"
        subgraph "Core Libraries" 
            LA[libapp]
            LI[libir]
            LU[libui]
        end
        
        subgraph "Thermal Framework"
            TC[thermal-common<br/>Shared Components]
            
            subgraph "Specialized Modules"
                T[thermal<br/>Basic Interface]
                TIR[thermal-ir<br/>Advanced Features]
                TL[thermal-lite<br/>Direct Hardware]
            end
        end
        
        subgraph "Hardware Abstraction"
            HAL[Hardware Abstraction Layer]
            
            subgraph "Camera Adapters"
                GA[Generic Adapter]
                TCA[TC001 Adapter]  
                ACA[AC020 Adapter]
            end
        end
        
        TC --> LA
        TC --> LI
        TC --> LU
        
        T --> TC
        TIR --> TC
        TL --> TC
        
        T --> HAL
        TIR --> HAL
        TL --> HAL
        
        HAL --> GA
        HAL --> TCA
        HAL --> ACA
    end
```

## Thermal Module Functionality Comparison

```mermaid
graph LR
    subgraph "thermal (Basic)"
        T1[Menu Interface]
        T2[Gallery View]
        T3[Basic Monitoring]
        T4[Chart Display]
    end
    
    subgraph "thermal-ir (Advanced)"
        IR1[Dual Camera Fusion]
        IR2[Advanced Processing]
        IR3[Report Generation]
        IR4[Database Integration]
        IR5[Video Recording]
        IR6[Algorithm Processing]
        IR7[WebSocket Support]
    end
    
    subgraph "thermal-lite (Hardware)"
        L1[USB Camera Control]
        L2[Direct Hardware Access]
        L3[Real-time Processing]
        L4[Sensor Management]
    end
    
    subgraph "Common Features"
        C1[Temperature Reading]
        C2[Image Capture]
        C3[Color Mapping]
        C4[Image Processing]
    end
    
    T1 --> C1
    T2 --> C2
    T3 --> C1
    T4 --> C4
    
    IR1 --> C2
    IR2 --> C4
    IR3 --> C2
    IR4 --> C1
    IR5 --> C2
    IR6 --> C4
    
    L1 --> C2
    L2 --> C1
    L3 --> C4
    L4 --> C1
```

## Merger Feasibility Assessment

```mermaid
flowchart TD
    A[Thermal Module Analysis] --> B{Merger Feasible?}
    
    B -->|Partial| C[Recommended Approach]
    B -->|No| D[Keep Separate]
    
    C --> E[Extract Common Components]
    C --> F[Create Hardware Abstraction]
    C --> G[Maintain Specialized Modules]
    
    E --> E1[Shared Utilities]
    E --> E2[Common UI Components]
    E --> E3[Temperature Processing]
    
    F --> F1[Camera Interface]
    F --> F2[Hardware Adapters]
    F --> F3[Sensor Abstraction]
    
    G --> G1[thermal: Basic Interface]
    G --> G2[thermal-ir: Advanced Features]
    G --> G3[thermal-lite: Direct Hardware]
    
    D --> D1[Maintain Current Structure]
    D --> D2[Address Code Duplication]
    D --> D3[Improve Documentation]
```

## Component Dependency Flow

## Code Quality Improvements Flow

```mermaid
graph TD
    A[Kotlin Compilation Warnings] --> B[Type Safety Issues]
    A --> C[Null Safety Issues] 
    A --> D[Experimental API Usage]
    subgraph "Application Layer"
        APP[Main Application]
    end
    
    subgraph "Module Layer"
        T[thermal]
        TIR[thermal-ir]  
        TL[thermal-lite]
    end
    
    subgraph "Common Layer" 
        TC[thermal-common<br/>Proposed]
        
        subgraph "Shared Components"
            AU[ArrayUtils]
            TP[Temperature Processing]  
            CM[Color Mapping]
            IP[Image Processing]
        end
    end
    
    subgraph "Hardware Layer"
        HAL[Hardware Abstraction Layer<br/>Proposed]
        
        subgraph "Device Drivers"
            GD[Generic Driver]
            TD[TC001 Driver]
            AD[AC020 Driver]
        end
    end
    
    subgraph "Core Infrastructure"
        LA[libapp]
        LI[libir]
        LU[libui]
    end
    
    APP --> T
    APP --> TIR
    APP --> TL
    
    B --> E[GuideInterface.kt<br/>String? -> String]
    C --> F[RingBuffer.kt<br/>ByteArray? null checks]
    C --> G[UsbBuffer.kt<br/>Remove redundant checks]
    C --> H[FileUtils.kt<br/>Array<File>? safety]
    D --> I[ByteUtils.kt<br/>@OptIn annotation]
    T --> TC
    TIR --> TC
    TL --> TC
    
    E --> J[Fixed with !!]
    F --> K[Added null guard]
    G --> L[Removed always true/false]
    H --> M[Added null check]
    I --> N[Added @OptIn]
    TC --> AU
    TC --> TP
    TC --> CM
    TC --> IP
    
    J --> O[Zero Warnings]
    K --> O
    L --> O
    M --> O
    N --> O
    T --> HAL
    TIR --> HAL
    TL --> HAL
    
    O --> P[Successful Build]
    HAL --> GD
    HAL --> TD
    HAL --> AD
    
    TC --> LA
    TC --> LI
    TC --> LU
```

## Architecture Overview & Risk Assessment Matrix

```mermaid
graph LR
    A[IRCamera Platform] --> B[libapp Module]
    B --> C[com.matrix Package]
    B --> D[com.mpdc4gsr Package]
    
    C --> E[GuideInterface]
    C --> F[RingBuffer] 
    C --> G[UsbBuffer]
    C --> H[ByteUtils]
    C --> I[FileUtils]
    
    D --> J[GuideInterface Copy]
    D --> K[RingBuffer Copy]
    D --> L[UsbBuffer Copy]
    D --> M[ByteUtils Copy]
    D --> N[FileUtils Copy]
    
    style E fill:#90EE90
    style F fill:#90EE90
    style G fill:#90EE90
    style H fill:#90EE90
    style I fill:#90EE90
    style J fill:#90EE90
    style K fill:#90EE90
    style L fill:#90EE90
    style M fill:#90EE90
    style N fill:#90EE90
    subgraph "Risk vs Impact Assessment"
        A[Low Risk<br/>High Impact] --> A1[Extract Common Utils]
        A[Low Risk<br/>High Impact] --> A2[Shared UI Components]
        
        B[Medium Risk<br/>Medium Impact] --> B1[Hardware Abstraction]
        B[Medium Risk<br/>Medium Impact] --> B2[Interface Standardization]
        
        C[High Risk<br/>Low Impact] --> C1[Complete Merger]
        C[High Risk<br/>Low Impact] --> C2[Architecture Unification]
        
        D[Low Risk<br/>Low Impact] --> D1[Documentation Updates]
        D[Low Risk<br/>Low Impact] --> D2[Code Formatting]
    end
    
    style A fill:#90EE90
    style B fill:#FFD700
    style C fill:#FFB6C1
    style D fill:#E6E6FA
```