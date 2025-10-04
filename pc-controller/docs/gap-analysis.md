# PC Controller Gap Analysis

## Executive Summary

This document identifies gaps between the existing PC Controller implementation and the Android app's networking
protocol. The analysis reveals **significant protocol incompatibilities** that prevent the PC and Android components
from communicating properly.

## Critical Gaps

### 1. Protocol Format Mismatch (CRITICAL)

**Gap**: Android uses **text-based protocol**, PC expects **JSON protocol**

**Android Protocol** (from `Protocol.kt`):

```
HELLO device_name=android_001 sensors=[GSR,RGB,THERMAL]
SYNC_REQUEST t_pc=1234567890
START_RECORD session_id=session_20240101_120000
DATA_GSR ts=1234567890 value=5.5
ACK cmd=START_RECORD session_id=session_20240101_120000
```

**PC Controller Protocol** (from `advanced_pc_controller.py`):

```json
{"type": "HELLO", "device_id": "android_001", "sensors": ["GSR", "RGB", "THERMAL"]}
{"type": "start_recording", "session_id": "session_20240101_120000"}
{"type": "telemetry_gsr", "value": 5.5, "timestamp": 1234567890}
```

**Impact**:

- PC cannot parse Android's text-based messages
- Android cannot parse PC's JSON messages
- No communication is possible in current state

**Evidence**:

- Android: `Protocol.kt` lines 40-110 - All messages are text-based with space-separated key=value pairs
- PC: `advanced_pc_controller.py` lines 257, 325, 785 - All message parsing expects JSON with `message.get('type')`

---

### 2. Message Type Name Mismatch (HIGH)

**Gap**: Different message type names between Android and PC

| Android (Protocol.kt) | PC (advanced_pc_controller.py) | Match?          |
|-----------------------|--------------------------------|-----------------|
| `HELLO`               | `HELLO`                        | Partial         |
| `SYNC_REQUEST`        | `sync_request`                 | Case mismatch   |
| `START_RECORD`        | `start_recording`              | Different name  |
| `STOP_RECORD`         | `stop_recording`               | Different name  |
| `DATA_GSR`            | `telemetry_gsr`                | Different name  |
| `FRAME`               | `thermal_frame`, `rgb_frame`   | Different names |
| `ACK`                 | (not handled)                  | Missing         |
| `ERROR`               | (not handled)                  | Missing         |

**Impact**:

- Even if protocol format is unified, message type routing will fail
- PC won't recognize Android's commands
- Android won't recognize PC's responses

---

### 3. Missing Message Handlers on PC (HIGH)

**Gap**: PC doesn't handle critical Android message types

**Missing on PC**:

1. **ACK messages** - Android sends acknowledgments, PC ignores them
2. **ERROR messages** - Android sends errors, PC doesn't process them
3. **SYNC_RESPONSE** - Android responds to sync requests, PC may not handle properly
4. **Device capability negotiation** - No handler for sensor availability checks

**Impact**:

- PC cannot detect command success/failure from Android
- Error recovery is impossible
- Time synchronization may fail
- Cannot verify sensor availability before starting session

---

### 4. Missing Command Support on PC (MEDIUM)

**Gap**: PC cannot send all commands Android expects

**Android Expects** (from `ProtocolHandler.kt`):

- `START_RECORD` with session_id parameter
- `STOP_RECORD` with session_id parameter
- `SYNC_REQUEST` with t_pc timestamp
- `SYNC_RESULT` with full timing data (t1, t2, t3, offset, rtt)

**PC Currently Sends**:

- `start_recording` (wrong name)
- `stop_recording` (wrong name)
- `sync_request` (case mismatch)
- Missing `SYNC_RESULT` entirely

**Impact**:

- Android will reject all PC commands due to name mismatch
- Time synchronization incomplete (missing SYNC_RESULT)
- Cannot remotely control Android recording

---

### 5. Data Streaming Format Incompatibility (HIGH)

**Gap**: GSR data format doesn't match between systems

**Android Sends** (Protocol.kt line 108):

```
DATA_GSR ts=1234567890 value=5.5
```

**PC Expects** (advanced_pc_controller.py line 803):

```json
{"type": "telemetry_gsr", "value": 5.5, "timestamp": 1234567890}
```

**Additional Issues**:

- Frame streaming format undefined on Android side
- No binary frame transfer support in Android Protocol.kt
- Android expects FRAME messages but PC sends `thermal_frame`/`rgb_frame`

**Impact**:

- Real-time GSR visualization on PC will not work
- Camera preview on PC will not work
- Live data streaming is completely broken

---

### 6. Connection Handshake Incomplete (MEDIUM)

**Gap**: Device registration flow not fully implemented

**Android Process** (NetworkClient.kt):

1. Connect to PC on port 8080
2. Send HELLO with device_name and sensors list
3. Wait for acknowledgment (not implemented)
4. Start heartbeat (implemented)

**PC Process** (advanced_pc_controller.py):

1. Accept connection
2. Receive HELLO message
3. Parse as JSON (fails - Android sends text)
4. Register device (never reaches this point)

**Impact**:

- Device never appears in PC's device list
- Session cannot be started
- Connection appears to succeed but is non-functional

---

### 7. Time Synchronization Protocol Incomplete (HIGH)

**Gap**: Full NTP-style sync not implemented on PC side

**Android Implementation** (ProtocolHandler.kt lines 81-120):

- Handles SYNC_REQUEST → sends SYNC_RESPONSE
- Handles SYNC_RESULT → applies clock offset
- Uses TimeSyncManager for offset calculation

**PC Implementation** (advanced_pc_controller.py):

- Sends sync_request (wrong name)
- Missing SYNC_RESULT message generation
- No RTT calculation
- No offset application
- Simple one-way timestamp exchange only

**Impact**:

- Multi-device time synchronization will fail
- Data from multiple devices cannot be aligned
- Cannot achieve <10ms synchronization accuracy required for multi-modal data

---

### 8. Session Management Protocol Gaps (MEDIUM)

**Gap**: Incomplete session lifecycle handling

**Missing on PC**:

- Session ID validation
- Session state tracking per device
- Session completion acknowledgment
- Session metadata exchange (duration, sensors, etc.)

**Missing on Android**:

- Session configuration from PC (sampling rates, compression, etc.)
- Pre-session capability check
- Session progress updates to PC

**Impact**:

- Cannot coordinate multi-device sessions
- PC doesn't know if session actually started
- No way to verify all devices are ready before recording

---

### 9. Error Recovery Mechanisms Missing (MEDIUM)

**Gap**: No error handling or recovery on PC side

**Android Has** (NetworkErrorRecoveryManager):

- Automatic reconnection on disconnect
- Exponential backoff
- Connection state machine
- Error categorization and logging

**PC Has**:

- Basic exception catching
- Log error messages
- No reconnection logic
- No error state tracking
- No retry mechanisms

**Impact**:

- Single network hiccup breaks entire session
- Must manually restart PC controller
- Lost data during brief disconnections

---

### 10. Protocol Version Negotiation Missing (LOW)

**Gap**: No version checking between PC and Android

**Android Defines** (Protocol.kt line 23):

```kotlin
const val PROTOCOL_VERSION = "1.0"
```

**PC Has**:

- No protocol version field
- No version checking
- No backward compatibility handling

**Impact**:

- Future protocol changes will break compatibility silently
- No way to detect version mismatches
- Cannot provide helpful error messages

---

## Secondary Gaps

### 11. SSL/TLS Certificate Handling

**Gap**: Certificate management inconsistency

**Android**: Uses CertificateManager with secure storage
**PC**: Generates self-signed certificates ad-hoc

**Issues**:

- No certificate exchange mechanism
- Android won't trust PC's self-signed cert by default
- No certificate pinning
- No certificate validation on Android

---

### 12. Data Export Protocol Missing

**Gap**: No file transfer protocol defined

**Requirement**: PC should download session data from Android after recording

**Current State**:

- No file transfer messages in Protocol.kt
- PC export assumes data is already local
- No chunked transfer support
- No integrity checking (checksums)

---

### 13. Discovery Service Integration

**Gap**: PC doesn't participate in mDNS discovery

**Android Has**: NetworkDiscoveryService broadcasting device info
**PC Has**: No discovery service, requires manual IP entry

**Impact**:

- User must manually enter PC IP on Android
- Cannot auto-discover PC on network
- No multi-PC support

---

### 14. Heartbeat Protocol Mismatch

**Gap**: Different heartbeat implementations

**Android**: Sends heartbeat every 5 seconds
**PC**: Expects JSON heartbeat messages (not defined in Android Protocol.kt)

---

### 15. Preview Streaming Not Implemented

**Gap**: Real-time frame preview protocol undefined

**Requirements** (from issue):

- Low-latency RGB/thermal preview on PC
- JPEG compression on Android
- Frame rate throttling (1-2 FPS for preview)

**Current State**:

- Android has PreviewStreamer.kt but no protocol integration
- PC expects frames in JSON messages
- No bandwidth management
- No frame dropping on overload

---

## Protocol Compatibility Matrix

| Feature       | Android Support | PC Support          | Compatible?  |
|---------------|-----------------|---------------------|--------------|
| Text Protocol |                 |                     |              |
| JSON Protocol |                 |                     |              |
| HELLO message |                 |                     | (format)     |
| START_RECORD  |                 | (wrong name)        |              |
| STOP_RECORD   |                 | (wrong name)        |              |
| SYNC_REQUEST  |                 | (case)              |              |
| SYNC_RESULT   |                 |                     |              |
| DATA_GSR      |                 | (wrong name/format) |              |
| ACK           |                 |                     |              |
| ERROR         |                 |                     |              |
| SSL/TLS       |                 |                     | ? (untested) |
| Heartbeat     |                 |                     |              |
| File Transfer |                 |                     | N/A          |
| Discovery     |                 |                     |              |

**Compatibility Score: 0/14 (0%)**

---

## Recommended Fixes (Priority Order)

### Priority 1 - Critical (Blocks all functionality)

1. **Unify Protocol Format**
    - Option A: Update PC to parse Android's text protocol
    - Option B: Update Android to send JSON protocol
    - Option C: Create protocol adapter layer on PC
    - **Recommendation**: Option A (minimal Android changes)

2. **Align Message Type Names**
    - Update PC message handlers to match Android names
    - Use uppercase conventions (HELLO, START_RECORD, etc.)
    - Map old names for backward compatibility

3. **Implement Missing PC Message Handlers**
    - Add ACK handler
    - Add ERROR handler
    - Add SYNC_RESPONSE handler
    - Complete SYNC_RESULT generation

### Priority 2 - High (Breaks key features)

4. **Fix Data Streaming**
    - Update PC to parse "DATA_GSR ts=X value=Y" format
    - Add FRAME message handler on PC
    - Implement frame decoding

5. **Complete Time Synchronization**
    - Implement full NTP-style sync on PC
    - Calculate and send SYNC_RESULT messages
    - Track offset per device

6. **Fix Connection Handshake**
    - Parse text-based HELLO correctly
    - Send proper ACK response
    - Update device tree UI

### Priority 3 - Medium (Quality of life)

7. **Add Session Management**
    - Session state tracking
    - Configuration exchange
    - Progress updates

8. **Implement Error Recovery**
    - Auto-reconnection on PC
    - Retry logic
    - State persistence

9. **Add Protocol Versioning**
    - Version field in all messages
    - Compatibility checking
    - Migration support

### Priority 4 - Low (Nice to have)

10. **Discovery Integration**
    - mDNS on PC
    - Auto-discovery UI

11. **File Transfer Protocol**
    - Define transfer messages
    - Chunked transfer
    - Progress tracking

12. **Preview Optimization**
    - Bandwidth management
    - Frame dropping
    - Quality adaptation

---

## Testing Requirements

To verify fixes, need integration tests for:

1. Connection establishment
2. HELLO message exchange
3. Time synchronization (full 4-step process)
4. Session start/stop commands
5. GSR data streaming
6. Frame streaming
7. Error handling and recovery
8. Multi-device coordination
9. SSL/TLS connection
10. Protocol version negotiation

**Current Test Coverage**: 0/10 integration tests exist

---

## Impact Assessment

### Current State

- **Communication**: 0% functional (protocol incompatible)
- **Time Sync**: 20% functional (basic timestamp exchange only)
- **Remote Control**: 0% functional (command names don't match)
- **Data Streaming**: 0% functional (format incompatible)
- **Error Handling**: 10% functional (basic logging only)

### After Priority 1 Fixes

- **Communication**: 90% functional
- **Time Sync**: 90% functional
- **Remote Control**: 90% functional
- **Data Streaming**: 80% functional
- **Error Handling**: 50% functional

### After All Fixes

- **Communication**: 100% functional
- **Time Sync**: 100% functional
- **Remote Control**: 100% functional
- **Data Streaming**: 100% functional
- **Error Handling**: 90% functional

---

## Effort Estimation

| Priority    | Tasks        | Estimated Effort | Risk   |
|-------------|--------------|------------------|--------|
| P1 Critical | 3 tasks      | 8-12 hours       | High   |
| P2 High     | 3 tasks      | 6-10 hours       | Medium |
| P3 Medium   | 3 tasks      | 4-8 hours        | Low    |
| P4 Low      | 3 tasks      | 4-6 hours        | Low    |
| **Total**   | **12 tasks** | **22-36 hours**  | -      |

---

## Conclusion

The existing PC Controller implementation is **well-architected** with excellent features:

- C++ native backend for performance
- SSL/TLS security layer
- Real-time PyQtGraph visualization
- Comprehensive documentation
- Cross-platform support

However, it has **zero compatibility** with the Android app due to fundamental protocol mismatches. The gaps are
systematic and affect every aspect of communication.

**Immediate Action Required**: Implement Priority 1 fixes to achieve basic communication. Without these, the PC
Controller cannot interact with Android devices at all.

**Recommended Approach**:

1. Create protocol adapter on PC to parse Android's text-based protocol
2. Update message type names to match Android
3. Add missing message handlers
4. Test end-to-end with real Android device
5. Iterate on remaining priorities

**Time to Basic Functionality**: ~8-12 hours of focused development
**Time to Full Feature Parity**: ~22-36 hours total
