# Chapter 3: Sensor Integration Design Decisions

            ## Table 3.2: Critical Design Decisions and Alternatives

            | Design Aspect | Options Considered | Chosen Solution | Rationale | Trade-offs | Impact on Requirements |

| --- | --- | --- | --- | --- | --- |
| **Time Synchronization Approach** | 1. External GPS clock<br/>2. System clock modification<br/>3. Internal relative
sync | **Option 3:** Software-based NTP exchange with offsets | No hardware dependency<br/>Works across Android
versions<br/>Achieves <5ms accuracy | Slightly less precise than hardware PTP | Satisfies FR-003 (timestamp sync) |
| **Thermal Camera Interface** | 1. Vendor SDK<br/>2. Custom UVC driver<br/>3. Generic V4L2 | **Option 1:** Official
TC001 SDK (LibIRParse) | Reliable calibration<br/>Vendor support<br/>Radiometric data access | Vendor lock-in, binary
dependency | Delivers FR-001/FR-005 thermal requirements |
| **GSR Sensor Connection** | 1. PC direct<br/>2. Android BLE<br/>3. Dual-mode support | **Option 3:** Unified code
paths for both modes | Maximum deployment flexibility<br/>Unified CSV output | Higher complexity | Supports FR-001 &
FR-004 |
| **Video Encoding Strategy** | 1. Raw frames<br/>2. H.264 hardware<br/>3. H.265 | **Option 2:** H.264 hardware
encoding | Real-time performance<br/>Widely compatible<br/>Reasonable storage footprint | Larger files than H.265 |
Meets FR-012 (RGB recording) |
| **Network Protocol** | 1. WebSocket<br/>2. gRPC<br/>3. Custom TCP + JSON<br/>4. MQTT | **Option 3:** Lightweight TCP +
JSON framing | Deterministic control<br/>Simple integration<br/>Low overhead | Requires custom tooling | Aligned with
FR-002 (remote control) |
