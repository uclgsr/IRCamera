# System Overview

MPDC4GSR is a multi-modal sensing platform built around an Android capture device and a desktop PC controller. This
document summarises how the major components interact and where to find their implementations.

## Architecture Summary

### Android Device

- **Application bootstrap (`app/src/main/java/mpdc4gsr/app/App.kt`)** sets up StrictMode, telemetry, native libraries,
  and starts the `RecordingService`.
- **Recording service (`app/src/main/java/mpdc4gsr/app/runtime/RecordingService.kt`)** runs as a foreground service,
  manages session lifecycle,
  coordinates sensors, exposes a TCP server for the PC controller, and handles crash recovery via
  `CrashRecoveryManager`.
- **Structured logging and crash safety** are implemented in `core/common/logging/StructuredLogger.kt`,
  `core/common/crash/CrashSafeSupervisor.kt`, and
  `core/common/crash/CrashRecoveryManager.kt`.
- **Session management** is provided by `core/recording/session/SessionManager.kt`, `SessionModels.kt`, and
  `core/data/utils/SessionDirectoryManager.kt`.

### Sensors and Modalities

- **RGB camera**: `core/data/RgbCameraRecorder` drives CameraX with adaptive frame capture, exports video and frame
  metadata, and surfaces state via `feature/capture/camera/presentation/RGBCameraViewModel`.
- **GSR**: `core/hardware/gsr` and `feature/capture/gsr` integrate the Shimmer SDK, manage BLE connections, resampling,
  statistics, and exports.
- **Thermal**: `component/thermal` and `feature/capture/thermal` wrap the vendor SDK and align thermal frames with
  session metadata.
- **Multi-modal coordination** lives in `feature/capture/gsr/presentation/MultiModalRecordingViewModel` which
  orchestrates
  simultaneous captures and sync markers.

### Networking

- **PcControllerServer (`core/infrastructure/network/PcControllerServer.kt`)** hosts the socket server on port 8081,
  registers with
  mDNS, handles client command parsing, and delegates actions back to the service.
- **Feature network module (`feature/connectivity/data`)** provides `NetworkController`, `RecordingController`,
  streaming
  pipelines, TCP/WebSocket clients, file upload services, and error recovery utilities.
- **Time synchronisation** uses the `gsr/network/TimeSyncClient.kt` (Android) and
  `pc-controller/time_sync_service.py` (PC) pair to maintain <15 ms drift via UDP probes and HTTP calibration sharing.

### User Experience

- Compose navigation is centred in `core/designsystem/navigation/` with `IRCameraNavigation` and `UnifiedNavigation`.
- Dashboards such as `feature/capture/camera/ui/CameraDashboardScreen.kt` and
  `feature/capture/gsr/ui/GSRSensorScreen.kt` surface live
  state from their view models.
- Shared styling and widgets live under `core/designsystem/`.

### Desktop Controller

- The Python application in `pc-controller/` communicates over the same protocol, exposes a PyQt6 dashboard, optional
  CLI mode, and native packet parsing provided by `native_backend/`.
- Key modules: `pc_controller.py`, `protocol_adapter.py`, `sync_handler.py`, `command_client.py`, and `tests/`.

## Data Flow

1. The Android `RecordingService` initialises sensor recorders and registers itself with the PC controller using mDNS.
2. The PC controller connects, negotiates the protocol version, and can start or stop sessions and trigger sync markers.
3. Sensor recorders stream data locally (CSV, JSON, images) via `SessionDirectoryManager` while optionally streaming
   previews (`PreviewStreamer`, `UnifiedDataStreamingService`) to the controller.
4. When sessions end, the Android device finalises artefacts and can upload summaries to the controller using
   `FileUploadService`.
5. Crash recovery replays incomplete sessions by consulting persisted manifests, ensuring long recordings survive
   transient failures.

## Module Map

- `app/src/main/java/mpdc4gsr/app/` – Application bootstrap, runtime services, and global Hilt modules.
- `app/src/main/java/mpdc4gsr/core/` – Shared foundation (`common/`, `designsystem/`, `hardware/`, `infrastructure/`,
  `recording/`) providing utilities, UI building blocks, and sensor abstractions.
- `app/src/main/java/mpdc4gsr/feature/capture/camera/` – Camera repositories, configuration managers, view models, and
  Compose
  screens.
- `app/src/main/java/mpdc4gsr/feature/capture/gsr/` – GSR domain, repositories, export flow, and Compose UI.
- `app/src/main/java/mpdc4gsr/feature/capture/thermal/` – Thermal camera integration and UI.
- `app/src/main/java/mpdc4gsr/feature/connectivity/` – Command/control protocol, streaming, and discovery services.
- `app/src/main/java/mpdc4gsr/feature/control/` – Device diagnostics and configuration surfaces.
- `app/src/main/java/mpdc4gsr/feature/dashboard/` – Compose launch activity, dashboards, and navigation entry points.
- `app/src/main/java/mpdc4gsr/feature/recording/session/` – Session presentation layer and orchestration helpers.
- `BleModule/` – BLE helpers compiled as a separate module for Shimmer devices.
- `component/thermal/` – Vendor thermal libraries and glue code.
- `component/shared/` – Shared utilities (permissions, logging, resource helpers).
- `pc-controller/` – Desktop controller, scripts, docs, and tests.

## External Dependencies

- CameraX for RGB capture (`androidx.camera.core`, `androidx.camera.video`, `androidx.camera.view`).
- Shimmer SDK jars for GSR acquisition.
- Vendor AARs in `app/libs` and `component/shared/libs` for thermal camera support.
- Hilt for dependency injection, Kotlin coroutines for concurrency, and Jetpack Compose for UI.
- Python packages (PyQt6, pyqtgraph, numpy, OpenCV, pybind11) for the PC controller.

## Observability and Reliability

- Structured logs are persisted by `StructuredLogger`; metrics are exposed through `TelemetryManager` and
  `PerformanceMetrics`.
- `CrashSafeSupervisor` performs background health checks, and `CrashRecoveryManager` restores sessions after faults.
- Network health, reconnection, and backoff strategies live in
  `feature/connectivity/data/NetworkErrorRecoveryManager.kt`.

Consult the platform-specific guides for deeper dives into implementation details.









