# Chapter 1: Multi-Sensor System Overview

            ## Figure 1.1: Multi-Sensor System Overview

            A high-level diagram illustrating the smartphone-based system with attached sensors, the PC controller,
            and all communication links.

```mermaid
graph TB
    subgraph "PC Controller Hub"
        PC[PC Controller Application<br/>Python 3.8+ TCP Client<br/>Session Orchestration]
        SessMgr[Session Manager<br/>Recording Coordination<br/>Multi-Device Sync<br/>State Machine]
        DataAgg[Data Aggregator<br/>Multi-Modal Fusion<br/>Timestamp Alignment<br/>CSV Merger]
        NTP[NTP Time Sync<br/>Clock Synchronization<br/>Chrony/ntpd]
        CmdQueue[Command Queue<br/>Async Handler<br/>Retry Logic]
        Logger[System Logger<br/>Event Logging<br/>Debug Output]
        FileManager[File Manager<br/>Data Transfer<br/>USB/Network]
        UIController[UI Controller<br/>Qt/Tkinter GUI<br/>Status Display]
    end

    subgraph "Android Sensor Node"
        Android[Android Device<br/>Samsung Galaxy S22<br/>Google Pixel 7<br/>TCP Server :8080]
        RecCtrl[Recording Controller<br/>Sensor Coordination<br/>Lifecycle Management<br/>State Tracking]
        TimeMgr[Time Manager<br/>Nanosecond Precision<br/>Unified Timestamp<br/>Offset Calculation]

        subgraph "Sensor Interfaces"
            ThermalMgr[Thermal Camera Manager<br/>TC001 USB/OTG Driver<br/>InfiSense SDK<br/>Frame Buffer]
            GSRMgr[GSR Sensor Service<br/>Shimmer3 BLE Handler<br/>ShimmerAPI<br/>Stream Parser]
            RGBMgr[RGB Camera Manager<br/>CameraX API<br/>H.264 Encoder<br/>Preview Handler]
        end

        subgraph "Data Pipeline"
            DataStore[Local Data Storage<br/>CSV + H.264 Video<br/>Session Metadata<br/>Internal Storage]
            BufferMgr[Buffer Manager<br/>Ring Buffer<br/>Overflow Protection]
            Serializer[Data Serializer<br/>CSV Writer<br/>JSON Metadata]
        end

        NetworkMgr[Network Manager<br/>TCP Handler<br/>Heartbeat Monitor<br/>Error Recovery]
        PermManager[Permission Manager<br/>USB/BLE/Camera<br/>Runtime Permissions]
    end

    subgraph "Hardware Sensors"
        TC001[Topdon TC001<br/>Thermal IR Camera<br/>256x192 @ 25Hz<br/>USB-C OTG<br/>Radiometric Mode]
        Shimmer[Shimmer3 GSR+<br/>Electrodermal Activity<br/>128Hz @ 16-bit<br/>Bluetooth LE 4.0<br/>PPG + IMU]
        PhoneCam[Phone RGB Camera<br/>1920x1080 @ 30fps<br/>H.264 Encoding<br/>Built-in Camera<br/>Autofocus/HDR]

        subgraph "Sensor Components"
            ThermalSensor[IR Microbolometer<br/>VOx Sensor<br/>8-14um Wavelength]
            GSRElectrodes[Ag/AgCl Electrodes<br/>Finger Attachment<br/>0.5V Excitation]
            CameraSensor[CMOS Sensor<br/>Lens Assembly<br/>ISP Pipeline]
        end
    end

    subgraph "Network Communication"
        WiFi[Wi-Fi Network<br/>Local TCP/IP<br/>Port 8080<br/>802.11ac/ax]
        Router[WiFi Router<br/>DHCP Server<br/>Local Gateway]
    end

    subgraph "External Services"
        NTPServer[NTP Server<br/>pool.ntp.org<br/>Time Reference]
        CloudBackup[Cloud Backup<br/>Optional Sync<br/>Google Drive]
    end

    PC <-->|START/STOP/SYNC| CmdQueue
    CmdQueue <-->|JSON Commands| WiFi
    WiFi <-->|TCP Messages| Router
    Router <-->|Local Network| NetworkMgr
    NetworkMgr <-->|Command Parser| Android
    PC <-->|NTP Query| NTPServer
    NTPServer -.->|Time Reference| NTP
    NTP -.->|Clock Offset| TimeMgr

    UIController --> PC
    PC --> SessMgr
    SessMgr --> DataAgg
    PC --> Logger
    Logger -.->|Log Files| FileManager

    Android --> RecCtrl
    Android --> NetworkMgr
    Android --> PermManager
    RecCtrl --> ThermalMgr
    RecCtrl --> GSRMgr
    RecCtrl --> RGBMgr
    RecCtrl --> TimeMgr
    TimeMgr -.->|Timestamps| ThermalMgr
    TimeMgr -.->|Timestamps| GSRMgr
    TimeMgr -.->|Timestamps| RGBMgr

    ThermalMgr <-->|USB OTG Protocol| TC001
    TC001 --> ThermalSensor
    GSRMgr <-->|BLE GATT Profile| Shimmer
    Shimmer --> GSRElectrodes
    RGBMgr <-->|Camera HAL| PhoneCam
    PhoneCam --> CameraSensor

    ThermalMgr -->|Frame Callback| BufferMgr
    GSRMgr -->|Sample Stream| BufferMgr
    RGBMgr -->|Video Buffer| BufferMgr
    BufferMgr -->|Buffered Data| Serializer
    Serializer -->|CSV/Video| DataStore
    TimeMgr -.->|Unified Time| Serializer

    DataStore -.->|File Transfer| FileManager
    DataStore -.->|Optional| CloudBackup
    FileManager <-->|USB/Network| PC
    DataAgg <-->|Aggregated Data| FileManager

    classDef pcClass fill:#e3f2fd,stroke:#1976d2,stroke-width:3px
    classDef androidClass fill:#f3e5f5,stroke:#7b1fa2,stroke-width:3px
    classDef sensorClass fill:#e8f5e9,stroke:#388e3c,stroke-width:3px
    classDef hwClass fill:#fff3e0,stroke:#f57c00,stroke-width:3px
    classDef netClass fill:#fce4ec,stroke:#c2185b,stroke-width:3px
```
