# Session Sequence Diagram (PC-Android Interaction)

## Figure 4.5: Protocol Sequence and Session Control

```mermaid
sequenceDiagram
    participant PC as PC Orchestrator<br/>(Python Controller)
    participant Android as Android Device<br/>(Sensor Node)
    participant Thermal as Topdon TC001<br/>(USB/OTG)
    participant GSR as Shimmer3 GSR<br/>(BLE)
    participant Camera as RGB Camera<br/>(CameraX)

    Note over PC, Camera: Session Initialization Phase
    PC->>Android: TCP Connection (Port 8080)
    Android->>PC: HELLO device_name=Samsung_S22 sensors=[thermal,gsr,rgb]
    PC->>Android: SYNC_REQUEST ts=1703441234567
    Android->>PC: SYNC_RESPONSE ts=1703441234569 offset=+2ms
    
    Note over PC, Camera: Device Discovery and Configuration
    PC->>Android: GET_CAPABILITIES
    Android->>Thermal: Initialize TC001 SDK (IRCMD.init())
    Android->>GSR: BLE Discovery and Pairing
    Android->>Camera: CameraX Setup (1920x1080@30fps)
    Android->>PC: CAPABILITIES thermal=256x192@25Hz gsr=128Hz rgb=1080p@30fps

    Note over PC, Camera: Recording Session Start
    PC->>Android: START_RECORD session_id=session_20241215_1430
    
    par Thermal Stream Initialization
        Android->>Thermal: Start capture (LibIRParse.startStream())
        Thermal-->>Android: Frame callback (256x192 thermal data)
        Android->>Android: Temperature calibration (+/-2 degrees C accuracy)
        Android->>PC: ACK cmd=START_RECORD status=thermal_ready
    and GSR Stream Initialization  
        Android->>GSR: Send 0x07 (start streaming command)
        GSR-->>Android: 12-bit ADC data @128Hz
        Android->>Android: Convert to microsiemens
        Android->>PC: ACK cmd=START_RECORD status=gsr_ready
    and RGB Stream Initialization
        Android->>Camera: Start recording (H.264 encoding)
        Camera-->>Android: Frame callbacks @30fps
        Android->>Android: Local storage (session directory)
        Android->>PC: ACK cmd=START_RECORD status=rgb_ready
    end

    Note over PC, Camera: Active Recording Phase (Continuous Data Flow)
    loop Every 128Hz (GSR), 25Hz (Thermal), 30fps (RGB)
        GSR-->>Android: Raw ADC sample
        Android->>Android: Timestamp with TimeManager.getCurrentTimestampNanos()
        Android->>PC: DATA_GSR ts=timestamp value=microsiemens
        
        Thermal-->>Android: Temperature matrix
        Android->>Android: CSV logging with nanosecond precision
        Android->>PC: STATUS_UPDATE thermal_frames=count
        
        Camera-->>Android: Video frame
        Android->>Android: H.264 encoding and local storage
        Android->>PC: HEARTBEAT status=recording duration=elapsed
    end

    Note over PC, Camera: Session Termination
    PC->>Android: STOP_RECORD session_id=session_20241215_1430
    
    par Graceful Shutdown Sequence
        Android->>Thermal: LibIRProcess.stopStream()
        Android->>GSR: Send 0x20 (stop streaming command)  
        Android->>Camera: CameraX.stopRecording()
    end
    
    Android->>Android: Finalize CSV files and metadata.json
    Android->>PC: ACK cmd=STOP_RECORD files_saved=5 total_size=2.3GB
    PC->>Android: SESSION_COMPLETE acknowledgment
    
    Note over PC, Camera: Connection Cleanup
    Android->>PC: DISCONNECT device_name=Samsung_S22
    PC->>Android: TCP Connection Close
```

## Technical Details

### Message Protocol

- **Transport**: TCP/IP over local network (port 8080)
- **Format**: JSON-based structured messages
- **Timing**: 2-second heartbeat intervals during recording
- **Error Handling**: Exponential backoff retry (500ms to 8s max)

### Synchronization Precision

- **Initial Sync**: SYNC_REQUEST/SYNC_RESPONSE exchange
- **Timestamp Source**: TimeManager.getCurrentTimestampNanos()
- **Accuracy**: Sub-5ms alignment (2.7ms median measured)
- **Clock Drift**: Chrony NTP compensation

### Hardware Integration Points

- **TC001**: Production SDK integration with VID/PID detection
- **Shimmer3**: Official ShimmerAndroidAPI with 12-bit ADC precision
- **CameraX**: Android Jetpack camera library for efficient capture







