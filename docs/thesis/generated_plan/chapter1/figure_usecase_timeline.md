# Chapter 1: Example Use-Case Scenario Timeline

## Figure 1.2: Example Use-Case Scenario Timeline

A detailed sequence diagram showing a typical recording session workflow, including synchronization and
remote control interactions.

```mermaid
sequenceDiagram
    participant Researcher as Researcher<br/>(Operator)
    participant UI as PC UI<br/>(Qt GUI)
    participant PC as PC Controller<br/>(Python Core)
    participant Network as Network<br/>(TCP/IP)
    participant Android as Android App<br/>(Sensor Node)
    participant RecCtrl as Recording<br/>Controller
    participant ThermalMgr as Thermal<br/>Manager
    participant GSRMgr as GSR<br/>Manager
    participant RGBMgr as RGB<br/>Manager
    participant Sensors as Hardware<br/>Sensors
    participant Data as Local<br/>Storage

    Note over Researcher,Data: Pre-Session Setup
    Researcher->>UI: Launch Controller Application
    UI->>PC: Initialize Application
    PC->>PC: Load Configuration & TCP Server
    UI-->>Researcher: Ready - Awaiting Device

    Researcher->>Android: Launch Sensor App
    Android->>Android: Check Permissions (USB/BLE/Camera)
    Android->>Network: Discover PC on Local Network
    Network->>PC: TCP Connect Request
    PC->>Network: Accept Connection
    Network->>Android: Connection Established

    Android->>PC: HELLO device_id=Samsung_S22 version=1.2.0
    PC->>Android: HELLO_ACK capabilities_request=true
    Android->>RecCtrl: Query Sensor Availability
    RecCtrl->>ThermalMgr: Check TC001
    RecCtrl->>GSRMgr: Check Shimmer3
    RecCtrl->>RGBMgr: Check Camera
    Android->>PC: CAPABILITIES thermal=256x192@25Hz gsr=128Hz rgb=1080p@30fps

    Note over Researcher,Data: Time Synchronization Phase
    PC->>Network: SYNC_REQUEST t1=1703441234567890
    Network->>Android: Forward SYNC_REQUEST
    Android->>Network: SYNC_RESPONSE t2/t3 timestamps
    Network->>PC: Forward SYNC_RESPONSE
    PC->>PC: Calculate offset=+2.3ms RTT=4.1ms
    PC->>Network: SYNC_RESULT offset=+2.3ms rtt=4.1ms
    Network->>Android: Forward SYNC_RESULT
    Android->>RecCtrl: Apply Time Offset
    Android->>PC: SYNC_ACK status=synchronized

    Note over Researcher,Data: Session Initialization
    Researcher->>UI: Click "Start Recording"
    UI->>PC: Start Session Request
    PC->>Network: START_RECORD session_id=20241215_1430
    Network->>Android: Forward START_RECORD
    Android->>RecCtrl: Start Recording Session

    par Thermal Stream
        RecCtrl->>ThermalMgr: Initialize TC001
        ThermalMgr-->>Android: Frame Callback @25Hz
        Android->>Data: thermal_frames.csv append
    and GSR Stream
        RecCtrl->>GSRMgr: Initialize Shimmer3
        GSRMgr-->>Android: 128Hz samples
        Android->>Data: gsr_samples.csv append
    and RGB Stream
        RecCtrl->>RGBMgr: Initialize CameraX
        RGBMgr-->>Android: 30fps video frames
        Android->>Data: video_recording.mp4 append
    end

    Android->>Network: ACK status=recording sensors=3
    Network->>PC: Forward ACK
    PC->>UI: Recording Started

    loop Every 2 seconds
        RecCtrl->>Android: Build status message
        Android->>Network: HEARTBEAT duration, frame counts
        Network->>PC: Forward HEARTBEAT
        PC->>UI: Update status dashboard
    end

    Note over Researcher,Data: Session Termination
    Researcher->>UI: Click "Stop Recording"
    UI->>PC: Stop Session Request
    PC->>Network: STOP_RECORD session_id=20241215_1430
    Network->>Android: Forward STOP_RECORD
    Android->>RecCtrl: Stop Sensor Threads

    par Graceful Shutdown
        Android->>ThermalMgr: stopStreaming()
        Android->>GSRMgr: stopStreaming()
        Android->>RGBMgr: stopRecording()
    end

    Android->>Data: Finalize CSV/MP4 metadata
    Android->>Network: ACK status=completed total_size=2.3GB
    Network->>PC: Forward ACK
    PC->>UI: Session Complete
    Android->>PC: DISCONNECT
```
