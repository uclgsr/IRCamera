# Thermal Module Requirements Alignment (2025-10-11)

This document captures the state of the Android thermal capture stack relative
to the FR/NFR specification provided by the research team. It tracks progress
as of **2025-10-11** after introducing the new `ThermalCaptureCoordinator`
workflow, failover data sources, and session-aware recording.

## Scope

- Android `app` module (thermal capture UI + view models + repositories)
- `component/thermal` module (Topdon TC001 integration + legacy Compose UIs)
- Shared infrastructure (`core`, `feature/connectivity`, `DataManagementService`)

## Findings

### FR1 – Multi-Device Sensor Integration
- ✅   The app now boots with a **failover thermal data source** (`FailoverThermalDataSource`)
        that prefers physical Topdon hardware but automatically switches to the
        deterministic simulation source when hardware is absent. Simulation
        status is surfaced in the UI and status flows.
- ⚠️   RGB/thermal alignment still happens post-recording; the live pipeline has
        not yet fused RGB camera events with thermal sessions.

### FR2/FR3 – Synchronous Multi-Modal Recording & Time Sync
- ✅   `ThermalCaptureCoordinator` is invoked from the `RecordingService`
        whenever the PC (or UI) starts/stops a session, so thermal recording now
        participates in the single start/stop trigger alongside other sensors.
- ✅   Thermal frame timestamps originate from `TimeManager.getCurrentTimestampMs()`,
        keeping them aligned with the millisecond-offset maintained against the PC.
- ⚠️   Sync signals (stimulus markers, flash events) are still forwarded only to
        GSR/RGB components; thermal needs explicit hook-ups in the command layer.

### FR5 – Data Recording & Storage
- ✅   Thermal recordings are streamed to `.tcf` (hardware) / `.csv` (simulation)
        files, migrated into the session’s `Thermal/` directory, and accompanied
        by a JSON manifest capturing frame counts, min/max temperatures, and
        simulation flags.
- ✅   Each output file (data + manifest) is registered with
        `DataManagementService`, so the PC manifest now includes the thermal artefacts.
- ⚠️   Dual-stream capture with RGB is still handled independently; combined
        post-processing manifests remain a TODO.

### FR6 – Monitoring UI
- ✅   The Compose UI now reflects live coordinator status: connection mode,
        simulation flag, frame counters, recording duration, and last file path.
- ✅   Thermal telemetry is merged into the multi-modal preview pipeline via
        `ThermalCaptureCoordinator`, so PC dashboards receive live spot-temperature
        metrics alongside existing GSR/RGB feeds.
- ⚠️   Battery/health metrics and remote time-sync quality are still missing
        from the UI, and palette/AGC controls remain static placeholders.

### FR7 – Sync Signals & JSON Command Protocol
- ⚠️   Protocol handlers relay start/stop commands but do not yet emit or react
        to sync pulses (flash, buzzer) for the thermal camera. This remains open.

### FR8 – Fault Tolerance
- ✅   Streaming is supervised: reconnection attempts resume the flow, and
        `DataManagementService` captures recovered files during controlled stops.
- ⚠️   Automatic restart after USB hot-unplug still needs dedicated testing and
        explicit retry logic.

### FR9 – Calibration Utilities
- ✅   The calibration screen now captures deterministic RGB/IR frame batches,
        stores manifests, and exposes capture progress/error status to the user.
- ⚠️   Actual reprojection error analysis and matrix persistence remain TODOs.

### FR10 – Data Transfer & Aggregation
- ✅   Thermal artefacts are registered in the session manifest with per-file
        metadata, and hashes are computed via `DataManagementService`.
- ⚠️   Automatic post-session transfer to the PC controller is not yet wired; it
        still relies on the existing upload subsystem (future integration needed).

### NFR Highlights
- NFR1/NFR7: Streaming and disk IO are off the main thread via the coordinator,
  but preview throttling/compression still needs tuning.
- NFR2: Thermal timestamps now respect the shared offset, but periodic sync
  diagnostics for thermal specifically are pending.
- NFR3/NFR4: Recording manifests include frame counts/min/max temps, providing
  better integrity checks; additional validation hooks for temperature ranges are
  still desirable.
- NFR5: Security posture unchanged—TLS/MTLS handling still relies on the
  transport layer; device identity for the thermal pipeline should mirror the
  RBC/GSR components.
- NFR6/NFR8: Configuration has been centralised into the coordinator and
  DataManagementService, improving modularity, but a formal configuration file
  for palette/AGC defaults is still pending.

## Next Steps

1. Extend the command protocol to broadcast sync markers and stimulus events to
   the thermal coordinator, recording them alongside GSR/RGB.
2. Feed thermal health metrics (device info, ISP state, clock drift) into the UI
   and PC status dashboards.
3. Wire post-session transfers so thermal data is pushed back to the PC
   automatically, and finalise RGB/thermal calibration by persisting matrices and
   reprojection scores.
