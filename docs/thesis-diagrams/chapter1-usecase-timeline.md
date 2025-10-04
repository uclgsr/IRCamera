# Chapter 1: Example Use-Case Scenario Timeline

## Figure 1.2: Example Use-Case Scenario Timeline

A simplified timeline diagram showing a typical recording session (PC sends "START", sensors begin logging synchronously, PC sends "STOP").

```mermaid
sequenceDiagram
    participant Researcher as Researcher<br/>(Operator)
    participant PC as PC Controller<br/>(Python App)
    participant Android as Android Device<br/>(Sensor Node)
    participant Sensors as Hardware Sensors<br/>(TC001, Shimmer3, RGB)
    participant Data as Local Storage<br/>(CSV + Video)
    
    Note over Researcher,Data: Pre-Session Setup Phase
    Researcher->>PC: Launch Controller Application
    PC->>PC: Initialize Network Server
    Researcher->>Android: Launch Sensor App
    Android->>PC: Auto-connect via TCP (port 8080)
    PC->>Android: HELLO / Device Discovery
    Android->>PC: Device Capabilities Report
    
    Note over Researcher,Data: Time Synchronization Phase
    PC->>Android: SYNC_REQUEST (t1 = PC_time)
    Android->>Android: Capture t2 = Android_time
    Android->>PC: SYNC_RESPONSE (t1, t2)
    PC->>PC: Calculate offset and RTT
    PC->>Android: SYNC_RESULT (offset = +2.3ms)
    Android->>Android: Apply time offset to TimeManager
    
    Note over Researcher,Data: Session Initialization
    Researcher->>PC: Click "Start Recording"<br/>Session ID: session_20241215_1430
    PC->>Android: START_RECORD command
    
    activate Android
    Android->>Sensors: Initialize All Sensors
    activate Sensors
    
    par Thermal Camera Startup
        Sensors->>Sensors: TC001 USB Init (50ms)
        Sensors->>Data: Create thermal_frames.csv
    and GSR Sensor Startup
        Sensors->>Sensors: Shimmer3 BLE Connect (80ms)
        Sensors->>Sensors: Send 0x07 Start Streaming
        Sensors->>Data: Create gsr_samples.csv
    and RGB Camera Startup
        Sensors->>Sensors: CameraX Init (100ms)
        Sensors->>Data: Create video_recording.mp4
    end
    
    Sensors->>Android: All Sensors Ready
    Android->>PC: ACK (status=recording)
    deactivate Sensors
    
    Note over Researcher,Data: Active Recording Phase (Continuous Data Flow)
    
    loop Every Sample Period (128Hz GSR, 25Hz Thermal, 30fps RGB)
        activate Sensors
        Sensors->>Data: Write GSR sample with timestamp
        Sensors->>Data: Write thermal frame with timestamp
        Sensors->>Data: Write RGB frame to H.264 stream
        deactivate Sensors
        
        Android->>PC: Heartbeat (every 2 seconds)<br/>Status Update
    end
    
    Note over Researcher,Data: Session Duration Example: 5 minutes
    
    Note over Researcher,Data: Session Termination Phase
    Researcher->>PC: Click "Stop Recording"
    PC->>Android: STOP_RECORD command
    
    activate Android
    Android->>Sensors: Stop All Sensors
    activate Sensors
    
    par Graceful Shutdown
        Sensors->>Sensors: TC001 Stop Stream
        Sensors->>Sensors: Shimmer3 Send 0x20 (Stop)
        Sensors->>Sensors: CameraX Stop Recording
    end
    
    Sensors->>Data: Flush buffers and close files
    deactivate Sensors
    Android->>Data: Write metadata.json
    Android->>PC: ACK (files_count=5, total_size=1.8GB)
    deactivate Android
    
    PC->>Researcher: Recording Complete<br/>Display Statistics
    
    Note over Researcher,Data: Post-Session Data Transfer
    Researcher->>Android: USB Connect / Network Transfer
    Android->>Researcher: Transfer Session Files<br/>thermal_frames.csv<br/>gsr_samples.csv<br/>video_recording.mp4<br/>metadata.json<br/>timesync_log.csv
```

## Use-Case Context

This timeline demonstrates:

- **Simple Operation**: Researcher clicks "Start" on PC, all sensors begin recording automatically
- **Automatic Synchronization**: Clock alignment happens transparently before recording
- **Parallel Initialization**: All sensors start simultaneously within ~100ms window
- **Continuous Recording**: Data flows to local storage throughout session
- **Clean Shutdown**: Graceful termination ensures all data saved properly
- **Data Accessibility**: Files ready for analysis immediately after session

## Typical Session Characteristics

- **Setup Time**: ~10-15 seconds (connection + sync)
- **Recording Duration**: 1-30 minutes typical
- **Data Volume**: ~360MB per 5-minute session
- **Synchronization Accuracy**: <5ms timestamp alignment
- **Sensors Active**: 3 modalities recording simultaneously
- **Heartbeat Interval**: 2 seconds for status monitoring

## Motivations Addressed

This simple workflow maps the problem context to the solution:

1. **Researcher Need**: Simple interface for multi-modal data collection
2. **Technical Challenge**: Synchronizing diverse sensor streams
3. **Solution**: Automated coordination via PC controller
4. **Outcome**: Time-aligned multi-modal dataset for analysis
