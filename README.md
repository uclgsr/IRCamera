# MPDC4GSR Multi-Sensor Recording Platform

MPDC4GSR combines an Android capture application and a desktop PC controller to record synchronised galvanic skin
response (GSR), thermal, and RGB data. The Android app manages sensor lifecycles, high throughput recording, and network
communication. The PC controller provides orchestration, real-time visualisation, and long-form session storage.

## Feature Highlights

- Android foreground recording service with crash recovery, session tracking, and a TCP server (`PcControllerServer`)
  that exposes remote controls and telemetry on port 8081 with optional mDNS discovery.
- Camera subsystem built on CameraX with the `RgbCameraRecorder`, dashboards, and Compose-based UI for manual controls,
  sensor diagnostics, and live preview.
- Modular `mpdc4gsr.gsr` package providing the `GsrOrchestrator`, Shimmer3 device manager, simulation sources, JSON
  command/time-sync clients, and a Compose session dashboard backed by Hilt.
- Thermal capture via the `thermalunified` component with Topdon TC001 bindings and coordinated multi-modal recording
  through shared recording/session infrastructure.
- Python PC controller (`pc_controller.py`) with the new `SensorManager`, `TimeSyncService`, `StimulusController`, PyQt6
  dashboard, CLI mode, native packet parsing, and end-to-end protocol compatibility with the Android service.
- Structured logging, telemetry, StrictMode policies, and crash-safe supervisors that keep long running sessions
  observable and recoverable.

## Repository Layout

```
finalgsr/
  app/
    src/main/java/mpdc4gsr/app/         Application bootstrap, runtime services, Hilt modules
    src/main/java/mpdc4gsr/core/        Shared foundations (`common/`, `designsystem/`, `hardware/`, `infrastructure/`, `recording/`)
    src/main/java/mpdc4gsr/feature/     Feature slices (`dashboard/`, `capture/`, `control/`, `connectivity/`, `recording/`)
  BleModule/           BLE utility module for Shimmer devices
  component/           Additional Android components such as thermal camera bindings
  libunified/          Shared Kotlin library (permission tools, logging, utilities)
  pc-controller/       Desktop controller application and documentation
  docs/                Project documentation (see docs/README.md)
  thermalunified/      Vendor thermal SDK wrapper
```

## Getting Started

### Android Application

1. Install Android Studio Jellyfish (or later) with JDK 17.
2. Open the project or run from the command line:

```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The application starts the `RecordingService` automatically which exposes the PC control server and prepares sensor
subsystems.

### PC Controller

```bash
cd pc-controller
python -m venv .venv
. .venv/Scripts/activate   # Windows
# or: source .venv/bin/activate
pip install -r requirements.txt
python pc_controller.py
# CLI mode
python pc_controller.py --cli
```

Optional: build the native analytics module in `pc-controller/native_backend` for high-rate GSR parsing.

## Testing & Quality

- Android unit tests: `./gradlew testDebugUnitTest`
- Android instrumentation tests: `./gradlew connectedAndroidTest`
- Static analysis: `./gradlew lint detekt`
- PC controller tests: `python -m pytest pc-controller/tests`

CI automation for the Android app lives in `.github/workflows/android-quality-gates.yml`.

## Documentation

Central documentation lives under `docs/`:

- [docs/system-overview.md](docs/system-overview.md) â€“ architecture, data flow, and module map
- [docs/android-platform.md](docs/android-platform.md) â€“ Android-specific subsystems and developer notes
- [docs/pc-controller.md](docs/pc-controller.md) â€“ desktop application overview
- [docs/testing-and-quality.md](docs/testing-and-quality.md) â€“ testing, analysis, and observability checklists
- [docs/developer-guides/](docs/developer-guides/) â€“ targeted deep dives (permissions, logging, UI utilities)
- [pc-controller/docs/](pc-controller/docs/) â€“ comprehensive PC controller manual

## Contributing

- Use Kotlin coroutines with lifecycle-aware scopes; see existing `AppBaseViewModel` patterns.
- Follow the Compose UI patterns established in `feature/**/ui` and keep view models in their sibling `presentation` packages.
- Record new sensors through the `SensorRecorder` interface and register them with the `RecordingService`.
- Run lint, detekt, unit tests, and relevant Python tests before opening a pull request.

## License

The repository currently has no published license. Add licensing information before producing releases.


