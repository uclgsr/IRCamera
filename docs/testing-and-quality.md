# Testing and Quality

This document captures the recommended checks for both sides of the MPDC4GSR platform.

## Android Application

### Automated Tasks

- Lint: `./gradlew lint`
- Detekt: `./gradlew detekt`
- Unit tests (JVM): `./gradlew testDebugUnitTest`
- Instrumentation tests: `./gradlew connectedAndroidTest`
- Assemble release artefacts: `./gradlew assembleRelease`

The Android build uses `detekt-config.yml` and `lint-baseline.xml`; update these when new rules are added. CI runs in
`.github/workflows/android-quality-gates.yml` and executes lint, detekt, unit tests, and assemble checks on pull
requests.

### Manual Verification

- Confirm the `RecordingService` foreground notification appears and remains responsive during long recordings.
- Inspect structured logs in the session storage directory (via `StructuredLogger`) for warnings during sensor
  start/stop.
- Validate camera preview and recording flows through `CameraDashboardScreen` and `RGBCameraScreen`.
- Pair with a Shimmer device and ensure reconnection works after toggling Bluetooth.
- Test multi-modal sessions end-to-end with the PC controller connected.

## PC Controller

### Automated Tests

- Run `python -m pytest` to execute the suite in `pc-controller/tests`.
- Build and import the native backend to ensure the pybind11 module loads correctly.

### Manual Checks

- Launch the GUI and confirm device discovery, live plots, and session export.
- Exercise the CLI (`python pc_controller.py --cli`) to verify headless workflows.
- Run the scripts in `pc-controller/scripts/` (for example `test_android_connection.py`) against a device.

## Observability

- `TelemetryManager` and `PerformanceMetrics` log metrics; monitor the output when introducing long-running tasks.
- Network health is surfaced via `NetworkController` logs; `PcControllerServer` reports client counts and failure
  causes.
- Crash recovery events emit notifications from `CrashRecoveryManager`. Always review these during QA cycles.

## Before Releasing

1. Run all automated tasks on a clean tree.
2. Verify Android and PC controller interoperability on the supported sensors (GSR, RGB, thermal).
3. Validate session export and crash recovery by simulating app restarts mid-session.
4. Update documentation if behaviour changes.
