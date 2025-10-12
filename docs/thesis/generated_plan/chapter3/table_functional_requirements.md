# Chapter 3: Functional Requirements and Design Criteria

            ## Table 3.1: System Requirements Matrix

            | Requirement ID | Functional Requirement | Design Criteria / Constraint | Design Solution | Verification Method | Priority |

| --- | --- | --- | --- | --- | --- |
| **FR-001** | Record GSR, thermal, and RGB data simultaneously | Temporal alignment within 5ms<br/>No data loss during
concurrent capture | SensorCoordinator orchestrates all sensors<br/>TimeManager with nanosecond precision<br/>Parallel
I/O workers per modality | Integration test with all sensors active; verify timestamps align within tolerance |
Essential |
| **FR-002** | Remote start/stop control via PC | Network latency <50ms<br/>Reliable command delivery | TCP/IP JSON
protocol with ACK/NACK<br/>CommandQueue ensures sequencing<br/>Retry with exponential backoff | Protocol latency
measurement plus resilience tests | Essential |
| **FR-003** | Timestamp synchronization within 5ms accuracy | Clock drift compensation<br/>Periodic resync | NTP-style
t1-t4 exchange<br/>TimelineClock offsets applied per sample | Sync test comparing timestamps after 30-minute run |
Essential |
| **FR-004** | Multi-device support (8+ Android devices) | Scalable architecture<br/>Per-device status tracking |
DeviceManager registry with heartbeats<br/>Independent connections for each device | Load test with 8 devices for 60
minutes | Important |
| **FR-005** | Local data storage on Android devices | Battery efficiency<br/>File integrity | Session-based directory
layout<br/>Buffered CSV writers & MP4 storage<br/>Metadata.json for provenance | Storage verification after extended
recording | Essential |
| **FR-006** | Automatic data transfer to PC after session | Reliable bulk transfer<br/>Resume capability | File
manifest exchange with checksums<br/>Chunked transfer + resume | Transfer interruption test ensuring resume |
Essential |
| **FR-007** | Real-time monitoring and status display | Low-latency status updates<br/>Battery/storage alerts | 2s
heartbeats<br/>PyQt6 dashboard with alerting | UI test verifying updates within 3 seconds | Important |
| **FR-008** | Fault tolerance and graceful degradation | Auto reconnection<br/>Data preservation | Per-sensor error
handling with fallback to simulation | Fault injection tests for sensor/network loss | Essential |
