# Performance Metrics Charts

## Figure 5.3: System Performance Analysis

### Figure 5.3a: System Architecture with Performance Metrics

```mermaid
graph LR
    subgraph PC["PC Controller"]
        CMD[Command Generator<br/>69ms avg latency]
        MON[Monitor & Logger<br/>Real-time tracking]
    end
    
    subgraph Network["Network Layer"]
        TCP[TCP Connection<br/>2.1ms avg latency<br/>±0.3ms jitter]
    end
    
    subgraph Android["Android Application"]
        REC[Recording Controller<br/>120MB RAM<br/>15% CPU avg]
        TM[Time Manager<br/>±2.1ms precision]
        STOR[Storage Manager<br/>95MB/s write]
    end
    
    subgraph Sensors["Sensor Array"]
        TC[TC001 Thermal<br/>24.5fps<br/>195KB/s]
        GSR[Shimmer3 GSR<br/>127.2Hz<br/>2.5KB/s]
        RGB[RGB Camera<br/>29.8fps<br/>2.4MB/s]
    end
    
    subgraph Power["Power Consumption"]
        BAT[Battery Monitor<br/>3%/10min<br/>18%/hour]
    end
    
    CMD -->|2ms| TCP
    TCP -->|Command| REC
    REC -->|Get Time| TM
    REC -->|Start| TC
    REC -->|Start| GSR
    REC -->|Start| RGB
    TC -->|Data Stream| STOR
    GSR -->|Data Stream| STOR
    RGB -->|Data Stream| STOR
    STOR -->|Status| MON
    TC -.->|Power Draw| BAT
    GSR -.->|Power Draw| BAT
    RGB -.->|Power Draw| BAT
    REC -.->|Power Draw| BAT
    
    style TC fill:#ffcccc
    style GSR fill:#ccffcc
    style RGB fill:#ccccff
    style BAT fill:#fff9c4
    style TM fill:#f0e68c
```

### Figure 5.3b: Latency Distribution Analysis

```mermaid
pie title Command-to-Start Latency Breakdown (68.9ms total)
    "Network Transmission" : 2.1
    "Command Parsing" : 2.0
    "TimeManager Init" : 4.0
    "Sensor Broadcast" : 7.0
    "Thermal USB Init" : 3.2
    "GSR BLE Wake" : 3.3
    "RGB CameraX Start" : 3.8
    "Coordination Overhead" : 43.5
```

### Figure 5.3c: Sensor Throughput Performance

```mermaid
graph TB
    subgraph Thermal["TC001 Thermal Camera"]
        TT[Target: 25fps]
        TA[Actual: 24.5fps]
        TP[Performance: 98%]
        TD[Data Rate: 195KB/s]
        TT --> TA
        TA --> TP
        TP --> TD
    end
    
    subgraph RGB["RGB Camera"]
        RT[Target: 30fps]
        RA[Actual: 29.8fps]
        RP[Performance: 99%]
        RD[Data Rate: 2.4MB/s]
        RT --> RA
        RA --> RP
        RP --> RD
    end
    
    subgraph GSR["Shimmer3 GSR"]
        GT[Target: 128Hz]
        GA[Actual: 127.2Hz]
        GP[Performance: 99%]
        GD[Data Rate: 2.5KB/s]
        GT --> GA
        GA --> GP
        GP --> GD
    end
    
    style TP fill:#90ee90
    style RP fill:#90ee90
    style GP fill:#90ee90
```

### Figure 5.3d: Battery Consumption Timeline

```mermaid
gantt
    title Battery Level During 60-Minute Recording Session
    dateFormat X
    axisFormat %M min
    
    section Battery Level
    100% (Start)        :milestone, b0, 0, 0
    Normal Operation    :active, op1, 0, 600000
    97% (10 min)        :milestone, b1, 600000, 600000
    Stable Drain        :active, op2, 600000, 1200000
    94% (20 min)        :milestone, b2, 1200000, 1200000
    Continuous Record   :active, op3, 1200000, 1800000
    91% (30 min)        :milestone, b3, 1800000, 1800000
    Sustained Load      :active, op4, 1800000, 2400000
    88% (40 min)        :milestone, b4, 2400000, 2400000
    Extended Session    :active, op5, 2400000, 3000000
    85% (50 min)        :milestone, b5, 3000000, 3000000
    Final Phase         :active, op6, 3000000, 3600000
    82% (60 min)        :milestone, b6, 3600000, 3600000
```

### Performance Summary

- **Average command-to-start latency**: 68.9ms (Target: <100ms) ✓
- **Network latency**: 2.1ms average (±0.3ms jitter)
- **Total throughput**: 2.6MB/s (all sensors combined)
- **Battery efficiency**: 0.30% per minute (3% per 10min)
- **Memory footprint**: 120MB average (Android app)
- **CPU utilization**: 15% average (all sensors active)

### Individual Sensor Performance

| Sensor         | Target | Measured | Performance | Data Rate |
|----------------|--------|----------|-------------|-----------|
| Thermal Camera | 25 fps | 24.5 fps | 98%         | 195KB/s   |
| RGB Camera     | 30 fps | 29.8 fps | 99%         | 2400KB/s  |
| GSR Sensor     | 128 Hz | 127.2 Hz | 99%         | 2.5KB/s   |








