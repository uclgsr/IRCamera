# Android Platform Guide

This guide outlines the modern Android architecture in the MPDC4GSR project and points to the primary entry points for feature development.

## Application Lifecycle

- `App` (`core/App.kt`) is annotated with `@HiltAndroidApp`, configures StrictMode in debug builds, loads native libraries, registers telemetry, and always starts the `RecordingService`.
- Global exception handling suppresses known transient issues while forwarding fatal crashes.
- `RecordingService` (`core/RecordingService.kt`) runs in the foreground, publishes notifications, owns the command socket, and coordinates session start/stop, sync markers, and crash recovery flows.

## Recording and Session Management

- Session metadata is generated through `core/session/SessionManager.kt`, `SessionModels.kt`, and `core/data/utils/SessionDirectoryManager.kt`.
- `CrashRecoveryManager` and `CrashSafeSupervisor` monitor long running sessions and automatically resume incomplete recordings.
- `FeatureFlags` and `TimeSyncManager` manage protocol negotiation and synchronisation with the PC controller.

## Sensor Integrations

### RGB Camera

- `core/data/RgbCameraRecorder` encapsulates CameraX preview, video recording, adaptive frame capture, and metadata export.
- The recorder exposes `SensorRecorder` flows to the UI. `feature/camera/presentation/RGBCameraViewModel` and `feature/camera/ui/CameraDashboardScreen` provide compose-driven controls for mode switching, frame rates, focus, and diagnostics.
- Supporting utilities include `CameraConfigurationManager`, `CameraControlsManager`, `CameraPerformanceManager`, and Samsung-specific compatibility helpers.

### GSR

- GSR devices are handled by `core/sensors/gsr` and the feature layer in `feature/gsr`.
- View models such as `GSRSensorViewModel`, `MultiModalRecordingViewModel`, `SessionExportViewModel`, and `SessionManagerViewModel` manage live telemetry, recording orchestration, and export workflows.
- BLE connections are mediated through `BleModule`, with reconnect logic and packet parsing abstracted behind repository interfaces.

### Thermal

- Thermal capture flows through the `thermalunified` component and the feature module `feature/thermal`. View models coordinate device warm-up, calibration, and frame delivery into the session directory.

## Network Communication

- `PcControllerServer` listens for controller connections, performs mDNS registration, and delegates parsed commands.
- `feature/network/data` contains `NetworkController`, `RecordingController`, `NetworkClient`, `PreviewStreamer`, `UnifiedDataStreamingService`, and `FileUploadService`.
- Discovery utilities (`PcServerDiscovery`, `NetworkUtils`) allow Android to find controllers on the LAN, while `NetworkErrorRecoveryManager` handles reconnection.

## User Interface

- `feature/main/ui/MainComposeActivity` hosts a Compose `NavHost` and acts as the canonical launcher path.
- Feature screens live under `feature/*/ui`, each paired with a Hilt-backed view model in `feature/*/presentation`.
- Reusable components and theming live in `core/ui/`, and the shared `LibUnifiedTheme` is provided in `libunified`.

## Dependency Injection and Threading

- Hilt modules reside in `core/di` and feature-specific `di` packages.
- Background work should use injected coroutine scopes or the `serviceScope` provided by `RecordingService`. Existing code relies on `viewModelScope`, `lifecycleScope`, or custom supervisors; avoid global scopes.

## Storage and Export

- Session directories are allocated by `SessionDirectoryManager`, and exports are performed via `FileUploadService`, `SessionExportViewModel`, and sensor-specific repositories.
- CSV recording is buffered with `CSVBufferedWriter`; metadata is captured in JSON manifests for recovery.

## Developer Workflow

- Build: `./gradlew assembleDebug`
- Lint and Detekt: `./gradlew lint detekt`
- Unit tests: `./gradlew testDebugUnitTest`
- Instrumentation tests: `./gradlew connectedAndroidTest`

During manual testing, ensure the `RecordingService` notification appears (indicating the server is active) and that the PC controller can discover the device via mDNS. Use this guide along with the implementation packages to locate code when adding new features or debugging sensor pipelines.
