# System Event Timeline and Synchronization

## Figure 5.1: Multi-Sensor Recording Session Timeline

```mermaid
gantt
    title System Event Timeline - Recording Session Start
    dateFormat X
    axisFormat %L ms

    section PC Controller
    NTP Sync           :milestone, ntp, 0, 0ms
    Network Init       :active, netinit, 0, 50ms
    START Command      :milestone, start, 50, 50ms
    Wait for ACK       :active, wait, 50, 54ms
    Receive ACK        :milestone, ack, 54, 54ms
    Monitor Status     :active, monitor, 54, 200ms
    Sync Verification  :milestone, verify, 200, 200ms

    section Android Device
    Idle State         :done, idle, 0, 52ms
    Receive Command    :active, recv, 52, 54ms
    Send ACK           :milestone, sendack, 54, 54ms
    TimeManager Init   :active, init, 54, 58ms
    Get Timestamp      :crit, timestamp, 56, 57ms
    Sensor Broadcast   :active, coord, 58, 65ms
    Wait for Sensors   :active, waitsens, 65, 70ms

    section TC001 Thermal Camera
    Standby            :done, tstby, 0, 60ms
    USB Wake           :active, twake, 60, 63ms
    Init Sequence      :active, tinit, 63, 65ms
    First Frame        :milestone, therm, 65, 65ms
    Continuous Capture :active, tcap, 65, 200ms

    section Shimmer3 GSR
    Standby            :done, gstby, 0, 61ms
    BLE Conn Check     :active, gble, 61, 63ms
    Start Stream Cmd   :active, gstream, 63, 66ms
    First Sample       :milestone, gsr, 66, 66ms
    Continuous Sample  :active, gsamp, 66, 200ms

    section RGB Camera
    Standby            :done, rstby, 0, 62ms
    CameraX Start      :active, rstart, 62, 65ms
    Buffer Prep        :active, rbuf, 65, 68ms
    First Frame        :milestone, rgb, 68, 68ms
    Continuous Record  :active, rrec, 68, 200ms
```

### Synchronization Performance

- **Network Latency**: 2ms (PC to Android)
- **Sensor Coordination Window**: 13ms (from first to last sensor)
- **Total Start Latency**: 68ms (command to all sensors active)
- **Target Met**: All sensors started within <100ms requirement

## Figure 5.1b: Sequence Diagram - Recording Initiation Protocol

```mermaid
sequenceDiagram
    participant PC as PC Controller
    participant Net as Network Layer
    participant Android as Android App
    participant TM as TimeManager
    participant TC as TC001 Thermal
    participant GSR as Shimmer3 GSR
    participant RGB as RGB Camera

    PC->>Net: START_RECORD command
    Note over PC,Net: t=50ms
    Net->>Android: TCP packet (2ms latency)
    Note over Net,Android: t=52ms
    Android->>Android: Parse command
    Android->>Net: ACK response
    Net->>PC: Command acknowledged
    Note over PC,Net: t=54ms
    Android->>TM: getCurrentTimestampNanos()
    TM-->>Android: T_ref timestamp
    Note over Android,TM: t=56ms<br/>Reference time established
    par Sensor Initialization
        Android->>TC: Start capture with T_ref
        Note over TC: USB init (3ms)
        TC-->>Android: First frame ready
        Note over TC: t=65ms
    and
        Android->>GSR: BLE stream command (0x07)
        Note over GSR: BLE latency (3ms)
        GSR-->>Android: First sample ready
        Note over GSR: t=66ms
    and
        Android->>RGB: CameraX start recording
        Note over RGB: Buffer prep (3ms)
        RGB-->>Android: First frame ready
        Note over RGB: t=68ms
    end
    Note over PC,RGB: All sensors active<br/>Total: 68ms<br/>Coordination: 13ms
    Android->>PC: RECORDING_STARTED status
    PC->>PC: Verify sync window
    Note over PC: t=200ms<br/>Verification complete
```








