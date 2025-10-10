# PC Controller Overview

The desktop controller located in `pc-controller/` orchestrates Android capture devices, provides live monitoring, and
stores long-running session artefacts.

## Major Components

- `pc_controller.py` – main entry point; starts the PyQt6 dashboard, CLI server, and background threads for networking
  and storage.
- `command_client.py` – convenience client for issuing commands to Android devices (used by scripts and tests).
- `protocol_adapter.py` – bridges legacy key/value payloads to the JSON message schema and negotiates protocol versions.
- `sync_handler.py` – performs time synchronisation and ensures consistent timestamps across devices.
- `native_backend/` – optional pybind11 modules for high-frequency packet parsing and statistics.
- `scripts/` – automation and integration helpers.
- `tests/` – unit tests verifying protocol handling, session storage, and recovery.

## Features

- Device discovery, session start/stop, sync-flash triggers, and metadata editing.
- Live dashboards with PyQtGraph plots for GSR, preview panes for RGB/Thermal frames, and a log console.
- Immediate storage of CSV/JSON artefacts, zipped exports, and resumable file uploads from Android.
- CLI mode (`python pc_controller.py --cli`) for automation pipelines and headless deployments.
- Optional TLS sockets when certificates are supplied in `pc-controller/certificates/`.

## Getting Started

```bash
cd pc-controller
python -m venv .venv
. .venv/bin/activate      # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python pc_controller.py   # GUI
python pc_controller.py --cli
```

Build the native backend to accelerate GSR packet parsing:

```bash
cd native_backend
python setup.py build_ext --inplace
```

## Testing

```bash
python -m pytest
# or
python -m unittest discover -s tests
```

Refer to [`pc-controller/docs/`](../pc-controller/docs/) for an exhaustive implementation guide, protocol specification,
and verification logs.
