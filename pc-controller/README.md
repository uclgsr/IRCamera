# IRCamera PC Controller

The PC controller is the desktop orchestrator for the IRCamera research platform. It discovers Shimmer3 GSR sensors, coordinates Android capture devices, keeps everybody on the same clock and stores every modality in a manifest-backed session folder. A PyQt6 dashboard gives researchers a live view; a rich CLI is available for headless or scripted deployments.

## Highlights
- **Multi-device sensor hub**  manage Shimmer3 GSR sensors directly over BLE (via `bleak`), Android spokes, or deterministic simulation feeds when hardware is offline.
- **Time synchronisation service** - built-in UDP time server plus the JSON SYNC protocol keep device clocks within the +/-5 ms requirement (FR3, NFR2).
- **Session engine with manifest**  per-session metadata, events, rolling CSVs, file uploads with SHA-256 integrity and resumable queues (FR4, FR5, FR10).
- **Stimulus & marker coordination**  trigger flash/audio sync cues, log experiment markers, and replay missed commands when devices reconnect (FR2, FR7, FR8).
- **PyQt6 dashboard**  live device list, GSR plots, RGB/thermal previews, transfer logs, simulation toggle and instant stimulus controls (FR6, NFR6).
- **Extensible CLI**  single entry point for discovery, start/stop, sync flashes, markers, and simulation control; ideal for automation.

## Getting Started
1. **Install dependencies**
   ```bash
   python -m venv .venv
   . .venv/bin/activate              # Windows: .venv\Scripts\activate
   pip install -r requirements.txt
   ```
   PyQt6/pyqtgraph are optional unless you need the GUI.

2. **(Optional) Build the native Shimmer parser**
   ```bash
   cd native_backend
   python -m pip install pybind11 setuptools wheel
   python setup.py build_ext --inplace
   ```
   The controller transparently falls back to the pure-Python parser when the extension is missing.

3. **Launch**
   ```bash
   python pc_controller.py --config config/config.yaml
   ```
   Useful flags:
   | Option | Description |
   |--------|-------------|
   | `--host`, `--port` | Bind address/port for the controller server. |
   | `--ssl` | Wrap TCP connections with TLS using `certificates/server.crt` + `server.key`. |
   | `--storage-dir` | Root directory for session data (defaults to `pc_data/`). |
   | `--config` | Load YAML/JSON configuration (sensors, transfer, security, etc.). |
   | `--simulate-sensors` | Force local simulation streams regardless of config. |
   | `--cli` | Skip the GUI and enter the interactive CLI. |

## CLI Quick Reference
Within the CLI (`python pc_controller.py --cli`) the following commands are available:

```
help                         Show command list
devices                      Print registered device snapshots
status                       Display current session + simulation state
start / stop                 Start or stop a recording session
flash                        Trigger a flash synchronisation stimulus
beep                         Trigger an audio synchronisation stimulus
marker <label>               Record a custom experiment marker
simulate [on|off|toggle]     Control local sensor simulation mode
quit / exit                  Terminate the controller
```

All CLI commands are idempotent and queued for offline devices where appropriate.

## GUI Quick Tour
- **Controls row**  Start/Stop session, Sync devices, Export data, Flash sync, Audio beep, Add marker, Simulation toggle.
- **Device list**  Status, advertised sensors, last heartbeat, session participation, last sync offset, instantaneous GSR rate and sample counts.
- **Telemetry tab**  Rolling GSR plot per device (colour-coded).
- **Frames tab**  Latest RGB and thermal preview frames at a lightweight resolution.
- **Log tab**  Timestamped event feed including stimuli, markers, file transfers, reconnects and command replays.

## Command Protocol
Commands emitted to Android devices now carry a richer JSON envelope with unique IDs for acknowledgement.
```json
{
  "type": "command",
  "command": "flash_sync",
  "commandId": "<uuid>",
  "requiresAck": true,
  "timestamp": 1696965600.0,
  "payload": {
    "intensity": 0.8,
    "duration_s": 0.25
  }
}
```
Android clients reply with `command_ack` messages that echo the `commandId`, include a status (`"ok"` or `"error"`), and an optional message. Offline devices receive the same envelope on reconnect, so acknowledgements remain idempotent. A legacy `parameters` field is still included for backward compatibility.

## Session Storage & Manifest
Each recording lives under `<storage-dir>/<session-id>/`:

- `session.json`  creation metadata (start time, configuration snapshot).
- `events.jsonl`  chronological stream of session markers and stimuli.
- <device>_gsr.csv - incremental GSR samples (timestamp, value).
- <device>/... - uploaded artefacts from Android peers.
- `manifest.json`  SHA-256, chunk counts, sizes and event summaries, referenced from `summary.json`.
- `summary.json`  closure record (duration, devices, manifest, events).

The storage layer writes to disk on receipt (no in-memory buffering) to satisfy NFR3/NFR4.

## Configuration Pointers
See `config/config.yaml` for editable defaults:
- `sensors`  BLE addresses/UUIDs for each Shimmer3 device.
- `simulation`  defaults for dummy streams when hardware is absent.
- `time_sync_service`  UDP host/port settings.
- `transfer`  chunk size, retry count, checksum algorithm.
- `session`  data root, metadata format, single-session guard.

## Troubleshooting
- **BLE discovery fails**  ensure the host Bluetooth stack is enabled and run with the correct permissions; use `--simulate-sensors` to fall back to deterministic data.
- **Time sync drifts**  confirm no firewall blocks UDP on the time sync port (default 47017) and that devices see the SYNC command log.
- **TLS handshake issues**  regenerate the self-signed pair in `certificates/` or disable TLS with `--ssl` omitted during local development.
- **Large uploads stall**  check `transfer.max_retries` and consult the manifest entry in `summary.json` for partial sizes/checksums.

Run the automated suite with:
```bash
python -m unittest discover -s tests
```
The tests cover protocol handling, storage integrity, the time sync service and sensor simulation behaviour.



