# Android Platform Guide

This guide outlines the modern Android architecture in the MPDC4GSR project and points to the primary entry points for
feature development.

## Application Lifecycle

- `App` (`app/src/main/java/mpdc4gsr/app/App.kt`) is annotated with `@HiltAndroidApp`, configures StrictMode in debug
  builds, loads native
  libraries, registers telemetry, and always starts the `RecordingService`.
- Global exception handling suppresses known transient issues while forwarding fatal crashes.
- `RecordingService` (`app/src/main/java/mpdc4gsr/app/runtime/RecordingService.kt`) runs in the foreground, publishes
  notifications, owns the command
  socket, and coordinates session start/stop, sync markers, and crash recovery flows.

## Recording and Session Management

- Session metadata is generated through `core/recording/session/SessionManager.kt`, `SessionModels.kt`, and
  `core/data/utils/SessionDirectoryManager.kt`.
- `CrashRecoveryManager` and `CrashSafeSupervisor` monitor long running sessions and automatically resume incomplete
  recordings.
- `FeatureFlags` negotiate protocol compatibility, while `gsr/network/TimeSyncClient.kt` pairs with `TimelineClock` to
  keep device timestamps aligned with the PC reference.

## Sensor Integrations

### RGB Camera

- `core/data/RgbCameraRecorder` encapsulates CameraX preview, video recording, adaptive frame capture, and metadata
  export.
- The recorder exposes `SensorRecorder` flows to the UI. `feature/capture/camera/presentation/RGBCameraViewModel` and
  `feature/capture/camera/ui/CameraDashboardScreen` provide compose-driven controls for mode switching, frame rates,
  focus, and
  diagnostics.
- Supporting utilities include `CameraConfigurationManager`, `CameraControlsManager`, `CameraPerformanceManager`, and
  Samsung-specific compatibility helpers.

### GSR

- GSR devices are handled by `core/hardware/gsr` and the feature layer in `feature/capture/gsr`.
- View models such as `GSRSensorViewModel`, `MultiModalRecordingViewModel`, `SessionExportViewModel`, and
  `SessionManagerViewModel` manage live telemetry, recording orchestration, and export workflows.
- BLE connections are mediated through `BleModule`, with reconnect logic and packet parsing abstracted behind repository
  interfaces.

### Thermal

- Thermal capture flows through the `component/thermal` module and the feature module `feature/capture/thermal`. View
  models
  coordinate device warm-up, calibration, and frame delivery into the session directory.

## Network Communication

- `PcControllerServer` listens for controller connections, performs mDNS registration, and delegates parsed commands.
- `feature/connectivity/data` contains `NetworkController`, `RecordingController`, `NetworkClient`, `PreviewStreamer`,
  `UnifiedDataStreamingService`, and `FileUploadService`.
- Continuous clock alignment <15 ms is achieved via `gsr/network/TimeSyncClient.kt` probing the PC controller's
  `time_sync_service.py`, combining UDP round trips with shared calibration snapshots.
- Discovery utilities (`PcServerDiscovery`, `NetworkUtils`) allow Android to find controllers on the LAN, while
  `NetworkErrorRecoveryManager` handles reconnection.

## User Interface

- `feature/dashboard/ui/MainComposeActivity` hosts a Compose `NavHost` and acts as the canonical launcher path.
- Feature screens live under `feature/*/ui`, each paired with a Hilt-backed view model in `feature/*/presentation`.
- Reusable components and theming live in `core/designsystem/`, and the shared `LibSharedTheme` is provided in
  `component/shared`.

## Dependency Injection and Threading

- Hilt modules reside in `app/di` and feature-specific `di` packages.
- Background work should use injected coroutine scopes or the `serviceScope` provided by `RecordingService`. Existing
  code relies on `viewModelScope`, `lifecycleScope`, or custom supervisors; avoid global scopes.

## Storage and Export

- Session directories are allocated by `SessionDirectoryManager`, and exports are performed via `FileUploadService`,
  `SessionExportViewModel`, and sensor-specific repositories.
- CSV recording is buffered with `CSVBufferedWriter`; metadata is captured in JSON manifests for recovery.

## Developer Workflow

- Build: `./gradlew assembleDebug`
- Lint and Detekt: `./gradlew lint detekt`
- Unit tests: `./gradlew testDebugUnitTest`
- Instrumentation tests: `./gradlew connectedAndroidTest`

During manual testing, ensure the `RecordingService` notification appears (indicating the server is active) and that the
PC controller can discover the device via mDNS. Use this guide along with the implementation packages to locate code
when adding new features or debugging sensor pipelines.








