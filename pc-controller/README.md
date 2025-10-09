# IRCamera PC Controller

The PC controller is the desktop companion to the IRCamera Android capture
application. It receives live telemetry streams (GSR, RGB, thermal), manages
recording sessions, and provides a PyQt6 dashboard for visualising sensor data
in real time. The controller can also run in pure CLI mode for headless
deployments or automated testing.

## Highlights

- **Network server** – newline delimited JSON protocol with graceful handling
  of legacy key/value messages; device registration, session events, telemetry,
  and file transfer messages are supported out of the box.
- **Real-time dashboard** – PyQt6 + PyQtGraph UI shows connected devices,
  streaming GSR plots, and the latest RGB/thermal frames. Controls are provided
  for starting/stopping sessions, triggering synchronisation, and exporting
  received data.
- **Session storage & export** – session assets (telemetry CSVs, uploaded
  artefacts) are written to disk immediately. A one-click export produces a zip
  archive for analysis.
- **C++ native backend** – a lightweight PyBind11 module parses Shimmer GSR
  packets and computes descriptive statistics with minimal overhead.
- **Robustness** – networking code tolerates malformed packets, disconnects,
  and devices rejoining a session without crashing the GUI thread.

## Getting Started

### 1. Install Python requirements

```bash
python -m venv .venv
. .venv/bin/activate                      # Windows: .venv\Scripts\activate
pip install -r requirements.txt
pip install pyqtgraph pyqt6               # GUI extras
```

### 2. Build the native backend (optional but recommended)

```bash
cd native_backend
python -m pip install pybind11 setuptools wheel
python setup.py build_ext --inplace
```

The controller falls back to a pure-Python parser if the extension is missing,
but the native module is significantly faster for high-rate GSR streams.

### 3. Launch

```bash
python pc_controller.py            # GUI (PyQt6 required)
python pc_controller.py --cli      # CLI fallback
```

Arguments:

| Option              | Description                                                 |
|---------------------|-------------------------------------------------------------|
| `--host` / `--port` | Bind address/port (default `0.0.0.0:8080`)                  |
| `--ssl`             | Enable TLS using `certificates/server.crt` and `server.key` |
| `--storage-dir`     | Destination for session artefacts (default `pc_data/`)      |
| `--cli`             | Force CLI mode even if PyQt6 is installed                   |

## Protocol Snapshot

Messages are UTF-8 JSON terminated by `\n`. Legacy key/value lines from older
firmware are converted internally via `ProtocolAdapter`.

### Device handshake

```json
{"type": "hello", "device_id": "android-01", "sensors": ["GSR", "RGB"]}
```

The controller replies with:

```json
{"type": "hello_ack", "server_time": 1736087504.1023, "session_id": null}
```

### GSR telemetry

Binary packets are base64-encoded and decoded via the native backend:

```json
{
  "type": "telemetry_gsr",
  "device_id": "android-01",
  "packet_b64": "qlUBAAABAAAP..."
}
```

### File transfer (post-session upload)

```
{"type": "file_begin", "session_id": "session_20250101_101000", "filename": "android-01_summary.json"}
{"type": "file_chunk", "session_id": "...", "filename": "...", "data": "<base64>"}
{"type": "file_end", "session_id": "...", "filename": "..."}
```

## PyQt6 Dashboard Tour

- **Device list** – shows connection status, sensors, last heartbeat, and
  whether the device is participating in the active session.
- **Telemetry tab** – rolling plot of GSR micro-siemens values for each device
  (colour-coded, auto-updated 4 Hz).
- **Frames tab** – latest RGB and thermal previews (JPEG-encoded) scaled to fit
  the panel. The display updates incrementally without blocking the UI.
- **Log tab** – timestamped event feed for quick debugging (session transitions,
  file transfers, protocol issues).
- **Controls** – start/stop session, broadcast sync requests, and export the
  latest session as a zip archive.

## Session Storage Layout

Each session is written to `<storage-dir>/<session-id>/`:

- `session.json` – metadata recorded when the session starts.
- `summary.json` – snapshot captured when the session ends.
- `<device>_gsr.csv` – rolling GSR samples (timestamp, µS).
- `<device>/...` – any uploaded files (images, summaries, etc.).

Pressing **Export Data** in the GUI creates `<session-id>.zip` alongside the
storage directory.

## Testing

The lightweight unit tests in `tests/test_controller_core.py` validate the
handshake path, GSR packet parsing (native fallback included), and session CSV
generation. Run with:

```bash
python -m unittest discover -s tests
```

## Future Work

- **End-to-end TLS** – the networking layer can wrap sockets with TLS when
  `--ssl` is used, but certificate provisioning & client authentication still
  need to be integrated on the Android side.
- **Device discovery** – mDNS/ZeroConf integration is planned to eliminate the
  need to manually enter IP addresses during pairing.
- **Additional sensors** – the GUI layout leaves room for extra plots (PPG,
  accelerometer) once the Android spokes surface them.
- **Automated session exports** – wiring the CLI to trigger exports on session
  stop would streamline scripted data collection workflows.

## Troubleshooting

- **“PyQt6 not available”** – install PyQt6/pyqtgraph or use `--cli`.
- **Native backend import error** – double-check you ran the `build_ext`
  command from `native_backend/` and that Python is loading from the same
  directory.
- **Firewall prompts** – Windows may prompt the first time the server listens
  on `0.0.0.0:8080`. Allow access on the private network used for testing.
