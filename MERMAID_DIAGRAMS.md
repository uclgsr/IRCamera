# Mermaid Diagrams

## Code Quality Improvements Flow

```mermaid
graph TD
    A[Kotlin Compilation Warnings] --> B[Type Safety Issues]
    A --> C[Null Safety Issues] 
    A --> D[Experimental API Usage]
    
    B --> E[GuideInterface.kt<br/>String? -> String]
    C --> F[RingBuffer.kt<br/>ByteArray? null checks]
    C --> G[UsbBuffer.kt<br/>Remove redundant checks]
    C --> H[FileUtils.kt<br/>Array<File>? safety]
    D --> I[ByteUtils.kt<br/>@OptIn annotation]
    
    E --> J[Fixed with !!]
    F --> K[Added null guard]
    G --> L[Removed always true/false]
    H --> M[Added null check]
    I --> N[Added @OptIn]
    
    J --> O[Zero Warnings]
    K --> O
    L --> O
    M --> O
    N --> O
    
    O --> P[Successful Build]
```

## Architecture Overview

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
```