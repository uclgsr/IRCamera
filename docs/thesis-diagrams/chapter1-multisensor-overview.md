# Chapter 1: Multi-Sensor System Overview

## Figure 1.1: Multi-Sensor System Overview

A high-level diagram illustrating the smartphone-based system with attached sensors (Shimmer3 GSR via Bluetooth, Topdon TC001 thermal via USB, and an RGB camera via CameraX), plus the remote PC controller over Wi-Fi.

```mermaid
graph TB
    subgraph "PC Controller Hub"
        PC[PC Controller Application<br/>Python TCP Client<br/>Session Orchestration]
        SessMgr[Session Manager<br/>Recording Coordination<br/>Multi-Device Sync]
        DataAgg[Data Aggregator<br/>Multi-Modal Fusion<br/>Timestamp Alignment]
        NTP[NTP Time Sync<br/>Clock Synchronization]
    end
    
    subgraph "Android Sensor Node"
        Android[Android Device<br/>Samsung/Google Phone<br/>TCP Server :8080]
        RecCtrl[Recording Controller<br/>Sensor Coordination<br/>Lifecycle Management]
        TimeMgr[Time Manager<br/>Nanosecond Precision<br/>Unified Timestamp]
        
        subgraph "Sensor Interfaces"
            ThermalMgr[Thermal Camera Manager<br/>TC001 USB/OTG Driver]
            GSRMgr[GSR Sensor Service<br/>Shimmer3 BLE Handler]
            RGBMgr[RGB Camera Manager<br/>CameraX API]
        end
        
        DataStore[Local Data Storage<br/>CSV + H.264 Video<br/>Session Metadata]
    end
    
    subgraph "Hardware Sensors"
        TC001[Topdon TC001<br/>Thermal IR Camera<br/>256x192 @ 25Hz<br/>USB-C Connection]
        Shimmer[Shimmer3 GSR+<br/>Electrodermal Activity<br/>128Hz @ 16-bit<br/>Bluetooth LE]
        PhoneCam[Phone RGB Camera<br/>1920x1080 @ 30fps<br/>H.264 Encoding<br/>Built-in Camera]
    end
    
    subgraph "Network Communication"
        WiFi[Wi-Fi Network<br/>Local TCP/IP<br/>Port 8080]
    end
    
    %% PC to Android Communication
    PC <-->|START/STOP Commands<br/>SYNC Protocol| WiFi
    WiFi <-->|TCP Messages<br/>JSON Protocol| Android
    PC <-->|NTP Sync<br/>+/-20ms accuracy| NTP
    NTP -.->|Clock Adjustment| TimeMgr
    
    %% Android Internal Flow
    Android --> RecCtrl
    RecCtrl --> ThermalMgr
    RecCtrl --> GSRMgr
    RecCtrl --> RGBMgr
    RecCtrl --> TimeMgr
    
    %% Hardware Connections
    ThermalMgr <-->|USB OTG<br/>InfiSense SDK| TC001
    GSRMgr <-->|BLE GATT<br/>Shimmer API| Shimmer
    RGBMgr <-->|CameraX<br/>Android HAL| PhoneCam
    
    %% Data Flow
    ThermalMgr -->|Thermal Frames<br/>Temperature Matrix| DataStore
    GSRMgr -->|GSR Samples<br/>Microsiemens| DataStore
    RGBMgr -->|Video Frames<br/>H.264 Stream| DataStore
    TimeMgr -.->|Unified Timestamps<br/>Nanosecond Precision| DataStore
    
    %% Session Management
    SessMgr -->|Session Control<br/>Device Discovery| PC
    DataAgg -->|Data Retrieval<br/>Post-Recording| Android
    
    %% Styling
    classDef pcClass fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef androidClass fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef sensorClass fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
    classDef hwClass fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef netClass fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    
    class PC,SessMgr,DataAgg,NTP pcClass
    class Android,RecCtrl,TimeMgr,DataStore androidClass
    class ThermalMgr,GSRMgr,RGBMgr sensorClass
    class TC001,Shimmer,PhoneCam hwClass
    class WiFi netClass
```

## System Architecture Context

This diagram establishes the project's scope and architecture:

- **PC Controller Hub**: Centralized control station running Python application for multi-device orchestration
- **Android Sensor Node**: Smartphone acting as sensor hub with integrated thermal camera, built-in RGB camera, and BLE connection to GSR sensor
- **Hardware Sensors**: Three sensing modalities (thermal IR, electrodermal activity, RGB video)
- **Network Communication**: Wi-Fi-based TCP/IP protocol for command and control
- **Time Synchronization**: NTP-style clock alignment achieving sub-millisecond precision

## Key System Characteristics

- **Modular Architecture**: Each sensor managed by dedicated software component
- **Distributed Computing**: PC coordinates multiple Android devices simultaneously
- **Precise Synchronization**: Unified timestamp system across all modalities
- **Research-Grade Quality**: Hardware specifications suitable for scientific data collection
- **Real-Time Operation**: Live data streaming with minimal latency
