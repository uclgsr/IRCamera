# GSR Module Rewrite Architecture

This document captures the target architecture for the rewritten galvanic skin response (GSR) subsystem across both Android capture clients and the PC controller. The design directly addresses functional requirements FR1–FR10 and non-functional requirements NFR1–NFR8.

## 1. System Overview

The platform is split into two cooperating runtimes:

1. **PC Orchestrator**
   - Runs the command server, time synchronisation service, session manager, data ingestor, and file aggregation pipeline.
   - Hosts the experiment control UI, stimulus presentation tools, and live telemetry dashboards.
2. **Android Capture Node (per device)**
   - Manages physical sensors (Shimmer3 GSR, RGB camera, Topdon TC001 thermal camera, microphone).
   - Executes capture pipelines, maintains local buffering, exposes previews, and mirrors PC commands.

All communication occurs over an authenticated TLS WebSocket multiplexed protocol with binary sub-channels for live data and gRPC-over-HTTP/2 for post-session file transfers. Every control message uses a JSON schema with an authoritative session timeline anchored by the PC.

## 2. Core Services

### 2.1 Time Synchronisation (FR3, NFR2)

- PC hosts a high-resolution clock service backed by `chrony`-style filtering.
- Android nodes run a lightweight client that periodically:
  1. Exchanges ping/pong packets to derive round-trip time.
  2. Applies a Kalman-filter-based drift correction.
  3. Publishes skew estimates to the recorder pipelines.
- Sync tolerance goal: ≤ 5 ms average, 10 ms absolute.

### 2.2 Session Lifecycle (FR2, FR4, FR5, FR10, NFR3, NFR4)

- The PC `SessionManager` coordinates:
  - Session creation with UUID + human label.
  - Directory provisioning `sessions/<timestamp>-<label>/`.
  - Metadata manifest `session.json` with lifecycle, participants, devices, file manifests, and checksum table.
  - Broadcast `session_started`, `session_stopped`, and `session_closed` commands.
- Android `SessionClient` mirrors session state, creates local storage roots, and exposes local faults.
- File transfers are orchestrated by `TransferSupervisor` using resumable chunked uploads with per-file SHA-256 verification.

### 2.3 Device Discovery & Management (FR1, FR6, FR7, NFR5, NFR6, NFR7)

- **PC Device Registry**:
  - Tracks registered Android nodes (`HELLO` handshake) plus directly paired Shimmer3 devices.
  - Exposes gRPC service `ListDevices`, `GetDeviceStatus`, `SendCommand`.
  - Maintains per-device heartbeats, battery, RSSI, and connection quality.
- **Android Sensor Manager**:
  - Encapsulates Shimmer SDK usage via a `ShimmerDeviceController` which supports multi-device pairing.
  - Handles GATT scanning, bond recovery, and connection concurrency on dedicated threads.
  - Supports simulation sources with configurable waveforms when no hardware is attached.
  - Manages Topdon TC001 via the vendor SDK and publishes both RGB (CameraX) and IR streams.
- **Preview Bus**:
  - Down-samples and JPEG-compresses preview frames to 320×180 @ 5 FPS, sending via WebSocket binary messages.
  - Sensor telemetry (GSR µS, thermal stats, audio level) streamed as JSON every 250 ms.

### 2.4 Recording Pipelines (FR2, FR5, FR8, NFR1, NFR3, NFR7, NFR8)

Android nodes expose modular recorders:

- `GsrRecorder` (Shimmer):
  - Operates at 128 Hz, writes incremental CSV + binary ring buffer.
  - Validates sample ranges, auto-restarts on gaps, logs latency metrics.
- `VideoRecorder` (RGB):
  - Uses CameraX with HEVC + segmented MP4 (1 GB chunks) at 1080p30.
  - Simultaneously captures preview frames at reduced resolution.
- `ThermalRecorder`:
  - Pulls Topdon frames (256×192 @ 25 Hz), writes to raw `.tiff` stack and optional processed `.mp4`.
- `AudioRecorder`:
  - Captures PCM 44.1 kHz mono, chunked into FLAC segments for efficiency.
- `StimulusMarkerChannel`:
  - Applies PC-issued event markers to all local logs immediately.

Each recorder publishes state transitions to the `CaptureCoordinator`, which ensures synchronised start/stop triggered by PC commands. A per-session `TimelineClock` uses the sync client to convert local time to global timeline, guaranteeing consistent timestamps.

### 2.5 Fault Tolerance & Recovery (FR8, NFR3)

- Every recorder persists to disk incrementally (fsync at boundaries).
- If network connectivity fails:
  - Android continues recording locally.
  - PC flags device offline and keeps session active.
  - Upon reconnection, `CommandReconciler` replays missed commands and fetches backlog logs.
- PC keeps durable command history; Android acknowledges with monotonic counters. Missing acknowledgements trigger resend using exponential backoff.

### 2.6 Calibration Utilities (FR9)

- Android provides a guided calibration workflow:
  - Launch from PC or device UI.
  - Captures synchronous RGB + IR frames of calibration pattern.
  - Runs OpenCV-based solver (using `thermalunified` module) on-device or offloaded to PC.
  - Stores calibration JSON in shared config accessible to analytics pipeline.
- PC UI visualises calibration results and warns if reprojection error exceeds threshold.

### 2.7 Security (NFR5)

- Mutually authenticated TLS with certificate pinning.
- Device enrolment workflow issues pairing tokens stored in encrypted SharedPreferences and PC keystore.
- Command messages include per-device HMAC to prevent tampering.
- File transfer service enforces per-session root directories and verifies SHA-256 of each chunk.

## 3. Module Structure

### 3.1 Android (`app/src/main/java/mpdc4gsr/gsr`)

```
mpdc4gsr/gsr/
  capture/
    CaptureCoordinator.kt         // orchestrates recorder lifecycles
    RecorderFactory.kt
    SimulationSource.kt
  device/
    ShimmerDeviceController.kt
    ThermalCameraController.kt
    SensorRegistry.kt
  network/
    CommandClient.kt              // JSON command channel
    PreviewPublisher.kt
    TransferClient.kt
    TimeSyncClient.kt
  recording/
    GsrRecorder.kt
    VideoRecorder.kt
    ThermalRecorder.kt
    AudioRecorder.kt
    RecordingContext.kt
  session/
    SessionStateStore.kt
    TimelineClock.kt
  ui/
    SessionDashboardScreen.kt
    DeviceStatusPanel.kt
    CalibrationWorkflowScreen.kt
  di/
    GsrModule.kt
```

Dependencies include `external/ShimmerAndroidAPI`, `external/Shimmer-Java-Android-API`, CameraX, and the Topdon SDK via existing `thermalunified` module.

### 3.2 PC (`pc-controller/mpdc4gsr/gsr`)

```
mpdc4gsr/gsr/
  controller.py        // high-level facade
  session_manager.py
  time_sync.py
  device_registry.py
  command_server.py
  telemetry_stream.py
  file_transfer.py
  stimulus.py
  ui/
    main_window.py
    preview_panel.py
    session_controls.py
    device_table.py
```

Key technologies: `asyncio`, `quart` (for WebSocket/gRPC bridge), `pyntp`-inspired sync server, `PyQt6` for UI, `aiofiles` for disk operations.

## 4. Data Formats

- **Command Envelope**:
  ```json
  {
    "type": "start_recording",
    "session_id": "2025-10-10T21-00-00Z",
    "timeline": {"epoch_ms": 1696965600000, "drift_ppm": -3.2},
    "parameters": {
      "modalities": ["GSR", "RGB", "THERMAL", "AUDIO"],
      "stimulus_id": null
    }
  }
  ```
- **GSR Sample Packet** (`data_gsr_binary`):
  - Little-endian struct: `[uint64 global_timestamp_ns][float32 microsiemens][uint32 sequence][float32 temperature]`.
- **Session Metadata** (`session.json`):
  - Contains device manifests, configuration, calibration references, file manifests with size + sha256, event log.

## 5. Testing Strategy

- Unit tests for protocol parsing, time sync, recorder state machines.
- Integration tests using simulation mode to mimic multi-device sessions with deterministic timestamping.
- Performance benchmarks verifying sustained 128 Hz streaming + 30 FPS preview without drops.
- Fault-injection tests (disconnect/reconnect, transfer retry) automated via scripts under `pc-controller/tests`.

## 6. Migration Plan

1. Backup existing GSR code (completed).
2. Replace on-device module with new packages and DI entry points.
3. Introduce new PC controller package and UI.
4. Update build scripts and dependency wiring.
5. Port tests and ensure compatibility with simulation.
6. Validate end-to-end session run in lab scenario.

## 7. Outstanding Risks

- Thermal + RGB dual capture performance: may require device-specific tuning using CameraX extensions.
- TLS mutual auth certificate provisioning for Android clients in headless deployments.
- Handling of large file uploads over unstable Wi-Fi; chunk size adaptation and resume heuristics must be tested extensively.

This architecture will guide the implementation that follows in the rewrite tasks.
