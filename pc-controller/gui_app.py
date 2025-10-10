# coverage: ignore file

from __future__ import annotations

import base64
import shutil
import time
from collections import defaultdict, deque
from pathlib import Path
from typing import Any, Dict, Deque, Optional, Tuple

try:  # pragma: no cover - GUI availability depends on runtime environment
    from PyQt6.QtCore import QObject, Qt, QTimer, pyqtSignal
    from PyQt6.QtGui import QCloseEvent, QPixmap
    from PyQt6.QtWidgets import (
        QApplication,
        QFileDialog,
        QHBoxLayout,
        QInputDialog,
        QLabel,
        QMainWindow,
        QMessageBox,
        QPushButton,
        QSplitter,
        QTabWidget,
        QTextEdit,
        QTreeWidget,
        QTreeWidgetItem,
        QVBoxLayout,
        QWidget,
    )

    import pyqtgraph as pg

    GUI_AVAILABLE = True
except Exception:  # pragma: no cover
    GUI_AVAILABLE = False
    QApplication = None  # type: ignore[assignment]


class ControllerBridge(QObject):
    event_received = pyqtSignal(object)

    def __init__(self, controller: Any):
        super().__init__()
        controller.register_listener(self._forward)

    def _forward(self, event: Any) -> None:
        self.event_received.emit(event)


class MainWindow(QMainWindow):
    def __init__(self, controller: Any):
        super().__init__()
        self.controller = controller
        self.bridge = ControllerBridge(controller)
        self.bridge.event_received.connect(self._handle_event)

        self.device_items: Dict[str, QTreeWidgetItem] = {}
        self.gsr_history: Dict[str, Deque[Tuple[float, float]]] = defaultdict(lambda: deque(maxlen=600))
        self.gsr_curves: Dict[str, pg.PlotDataItem] = {}
        self._transfer_progress: Dict[Tuple[str, str], int] = {}
        self._rgb_pixmap: Optional[QPixmap] = None
        self._thermal_pixmap: Optional[QPixmap] = None

        self._setup_ui()

        self._plot_timer = QTimer(self)
        self._plot_timer.timeout.connect(self._refresh_plot)
        self._plot_timer.start(250)

    def _setup_ui(self) -> None:
        self.setWindowTitle("IRCamera PC Controller")
        self.resize(1200, 800)

        central = QWidget()
        self.setCentralWidget(central)
        root_layout = QVBoxLayout(central)

        controls = QHBoxLayout()
        root_layout.addLayout(controls)

        self.start_btn = QPushButton("Start Session")
        self.start_btn.clicked.connect(self._start_session)
        controls.addWidget(self.start_btn)

        self.stop_btn = QPushButton("Stop Session")
        self.stop_btn.clicked.connect(self._stop_session)
        self.stop_btn.setEnabled(False)
        controls.addWidget(self.stop_btn)

        self.sync_btn = QPushButton("Sync Devices")
        self.sync_btn.clicked.connect(self._sync_devices)
        controls.addWidget(self.sync_btn)

        self.export_btn = QPushButton("Export Data")
        self.export_btn.clicked.connect(self._export_data)
        controls.addWidget(self.export_btn)

        self.flash_btn = QPushButton("Flash Sync")
        self.flash_btn.clicked.connect(self._flash_sync)
        controls.addWidget(self.flash_btn)

        self.beep_btn = QPushButton("Audio Beep")
        self.beep_btn.clicked.connect(self._beep_sync)
        controls.addWidget(self.beep_btn)

        self.marker_btn = QPushButton("Add Marker")
        self.marker_btn.clicked.connect(self._add_marker)
        controls.addWidget(self.marker_btn)

        self.sim_btn = QPushButton()
        self.sim_btn.clicked.connect(self._toggle_simulation)
        controls.addWidget(self.sim_btn)
        self._update_sim_button()

        controls.addStretch(1)

        splitter = QSplitter()
        root_layout.addWidget(splitter, stretch=1)

        self.device_tree = QTreeWidget()
        self.device_tree.setHeaderLabels(
            ["Device", "Status", "Sensors", "Last Update", "Session", "Last Sync"]
        )
        self.device_tree.setColumnWidth(0, 160)
        self.device_tree.setColumnWidth(1, 90)
        self.device_tree.setColumnWidth(2, 140)
        self.device_tree.setColumnWidth(3, 120)
        self.device_tree.setColumnWidth(4, 80)
        self.device_tree.setColumnWidth(5, 180)
        splitter.addWidget(self.device_tree)

        self.tabs = QTabWidget()
        splitter.addWidget(self.tabs)

        telemetry_tab = QWidget()
        telemetry_layout = QVBoxLayout(telemetry_tab)
        self.gsr_plot = pg.PlotWidget(title="GSR (uS)")
        self.gsr_plot.setLabel("left", "Conductance", units="uS")
        self.gsr_plot.setLabel("bottom", "Seconds")
        self.gsr_plot.addLegend()
        telemetry_layout.addWidget(self.gsr_plot)
        self.tabs.addTab(telemetry_tab, "Telemetry")

        frames_tab = QWidget()
        frames_layout = QHBoxLayout(frames_tab)
        self.rgb_label = QLabel("RGB frame")
        self.rgb_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.rgb_label.setMinimumSize(320, 240)
        frames_layout.addWidget(self.rgb_label)
        self.thermal_label = QLabel("Thermal frame")
        self.thermal_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.thermal_label.setMinimumSize(320, 240)
        frames_layout.addWidget(self.thermal_label)
        self.tabs.addTab(frames_tab, "Frames")

        log_tab = QWidget()
        log_layout = QVBoxLayout(log_tab)
        self.log_text = QTextEdit()
        self.log_text.setReadOnly(True)
        log_layout.addWidget(self.log_text)
        self.tabs.addTab(log_tab, "Log")

    def _start_session(self) -> None:
        try:
            session_id = self.controller.start_recording()
            QMessageBox.information(self, "Session Started", f"Session {session_id} started.")
            self.start_btn.setEnabled(False)
            self.stop_btn.setEnabled(True)
        except Exception as exc:
            QMessageBox.critical(self, "Error", str(exc))

    def _stop_session(self) -> None:
        session_id = self.controller.stop_recording()
        if session_id:
            QMessageBox.information(self, "Session Stopped", f"Session {session_id} stopped.")
        self.start_btn.setEnabled(True)
        self.stop_btn.setEnabled(False)

    def _sync_devices(self) -> None:
        count = self.controller.broadcast_command("sync_request", server_time=time.time())
        self._append_log(f"Sync request sent to {count} device(s)")

    def _export_data(self) -> None:
        session = self.controller.get_session()
        storage_dir = self.controller.get_storage_dir()
        if session:
            session_id = session.session_id
        else:
            session_dirs = [p for p in storage_dir.iterdir() if p.is_dir()]
            if not session_dirs:
                QMessageBox.information(self, "Export", "No sessions available yet.")
                return
            session_dirs.sort(key=lambda p: p.stat().st_mtime, reverse=True)
            session_id = session_dirs[0].name

        source_dir = self.controller.get_session_dir(session_id)
        default_path = storage_dir / f"{session_id}.zip"
        save_path, _ = QFileDialog.getSaveFileName(
            self,
            "Export Session Archive",
            str(default_path),
            "Zip Archives (*.zip)",
        )
        if not save_path:
            return

        target = Path(save_path)
        base = target.with_suffix("")
        shutil.make_archive(str(base), "zip", root_dir=source_dir)
        QMessageBox.information(self, "Export Complete", f"Session archived to {base}.zip")

    def _flash_sync(self) -> None:
        event = self.controller.trigger_flash_sync()
        self._append_log(f"Flash sync triggered ({event.timestamp:.2f}s)")

    def _beep_sync(self) -> None:
        event = self.controller.trigger_audio_beep()
        self._append_log(f"Audio sync beep triggered ({event.timestamp:.2f}s)")

    def _add_marker(self) -> None:
        label, ok = QInputDialog.getText(self, "Add Marker", "Marker label:")
        if not ok or not label.strip():
            return
        event = self.controller.mark_event(label.strip())
        self._append_log(f"Marker '{label.strip()}' recorded ({event.timestamp:.2f}s)")

    def _toggle_simulation(self) -> None:
        enabled = self.controller.toggle_simulation_mode()
        self._update_sim_button()
        self._append_log(f"Simulation mode {'enabled' if enabled else 'disabled'}")

    def _update_sim_button(self) -> None:
        state = "On" if self.controller.is_simulation_enabled() else "Off"
        self.sim_btn.setText(f"Simulation: {state}")

    def _handle_event(self, event: Any) -> None:
        handlers = {
            "device_registered": self._on_device_registered,
            "status_update": self._on_status_update,
            "device_disconnected": self._on_device_disconnected,
            "telemetry_gsr": self._on_gsr_sample,
            "frame_rgb": lambda e: self._update_image(e, frame_type="rgb"),
            "frame_thermal": lambda e: self._update_image(e, frame_type="thermal"),
            "session_started": self._on_session_started,
            "session_stopped": self._on_session_stopped,
            "time_sync_started": self._on_time_sync_started,
            "time_sync_completed": self._on_time_sync_completed,
            "time_sync_failed": self._on_time_sync_failed,
            "stimulus_event": self._on_stimulus_event,
            "event_marker": self._on_event_marker_event,
            "file_transfer_begin": self._on_file_transfer_begin,
            "file_transfer_chunk": self._on_file_transfer_chunk,
            "file_transfer_end": self._on_file_transfer_end,
            "local_sensors_started": self._on_local_sensors_started,
            "simulation_mode_changed": self._on_simulation_changed,
            "command_replayed": self._on_command_replayed,
        }
        handler = handlers.get(getattr(event, "type", ""), self._on_generic_event)
        handler(event)

    def _on_device_registered(self, event: Any) -> None:
        payload = event.payload.get("record", {})
        self._update_device_row(payload)
        self._append_log(f"Device {event.device_id} registered.")

    def _on_status_update(self, event: Any) -> None:
        payload = event.payload
        device = getattr(event, "device_id", None)
        if device and device in self.controller.devices:
            snapshot = self.controller.devices[device].snapshot()
        else:
            snapshot = payload
        self._update_device_row(snapshot)

    def _on_device_disconnected(self, event: Any) -> None:
        record = self.controller.devices.get(event.device_id)
        if record:
            self._update_device_row(record.snapshot())
        self._append_log(f"Device {event.device_id} disconnected.")

    def _on_session_started(self, event: Any) -> None:
        self.start_btn.setEnabled(False)
        self.stop_btn.setEnabled(True)
        self._append_log(f"Session {event.payload.get('session_id')} started.")

    def _on_session_stopped(self, event: Any) -> None:
        self.start_btn.setEnabled(True)
        self.stop_btn.setEnabled(False)
        self._append_log(f"Session {event.payload.get('session_id')} stopped.")

    def _on_gsr_sample(self, event: Any) -> None:
        history = self.gsr_history[event.device_id]
        history.append((event.payload["timestamp"], event.payload["value"]))

    def _on_time_sync_started(self, event: Any) -> None:
        device = event.device_id or "unknown device"
        self._append_log(f"Time sync started for {device}.")

    def _on_time_sync_completed(self, event: Any) -> None:
        device = event.device_id or "unknown device"
        payload = event.payload or {}
        offset = payload.get("offset_ms")
        rtt = payload.get("rtt_ms")
        details = ""
        if offset is not None and rtt is not None:
            details = f" offset={offset} ms, RTT={rtt} ms"
        self._append_log(f"Time sync completed for {device}.{details}")
        if device in self.controller.devices:
            snapshot = self.controller.devices[device].snapshot()
            self._update_device_row(snapshot)

    def _on_time_sync_failed(self, event: Any) -> None:
        device = event.device_id or "unknown device"
        reason = ""
        if event.payload and event.payload.get("error"):
            reason = f": {event.payload.get('error')}"
        self._append_log(f"Time sync failed for {device}{reason}.")

    def _on_stimulus_event(self, event: Any) -> None:
        payload = event.payload or {}
        code = payload.get("code", "stimulus")
        timestamp = payload.get("timestamp")
        details = payload.get("details", {})
        detail_str = ", ".join(f"{k}={v}" for k, v in details.items()) if details else ""
        ts_str = f" ({timestamp:.2f}s)" if isinstance(timestamp, (int, float)) else ""
        message = f"Stimulus {code}{ts_str} {detail_str}".strip()
        self._append_log(message)

    def _on_event_marker_event(self, event: Any) -> None:
        payload = event.payload or {}
        code = payload.get("code", "marker")
        timestamp = payload.get("timestamp")
        origin = event.device_id or "local"
        details = payload.get("details", {})
        detail_str = ", ".join(f"{k}={v}" for k, v in details.items()) if details else ""
        ts_str = f" ({timestamp:.2f}s)" if isinstance(timestamp, (int, float)) else ""
        message = f"Marker '{code}' from {origin}{ts_str} {detail_str}".strip()
        self._append_log(message)

    def _on_local_sensors_started(self, event: Any) -> None:
        sensors = event.payload.get("sensors", []) if event.payload else []
        sensor_list = ", ".join(sensors) if sensors else "simulation only"
        self._append_log(f"Local sensors active: {sensor_list}")

    def _on_simulation_changed(self, event: Any) -> None:
        enabled = bool(event.payload.get("enabled")) if event.payload else False
        self._update_sim_button()
        self._append_log(f"Simulation mode {'enabled' if enabled else 'disabled'}")

    def _on_file_transfer_begin(self, event: Any) -> None:
        payload = event.payload or {}
        key = (event.device_id or "unknown", payload.get("filename", ""))
        self._transfer_progress[key] = 0
        session_id = payload.get("session_id")
        self._append_log(
            f"File transfer start from {event.device_id}: {payload.get('filename')} (session {session_id})"
        )

    def _on_file_transfer_chunk(self, event: Any) -> None:
        payload = event.payload or {}
        key = (event.device_id or "unknown", payload.get("filename", ""))
        size = int(payload.get("size", 0))
        self._transfer_progress[key] = self._transfer_progress.get(key, 0) + size

    def _on_file_transfer_end(self, event: Any) -> None:
        payload = event.payload or {}
        key = (event.device_id or "unknown", payload.get("filename", ""))
        total = self._transfer_progress.pop(key, 0)
        checksum = payload.get("sha256")
        self._append_log(
            f"File transfer complete from {event.device_id}: {payload.get('filename')} ({total} bytes, sha256={checksum})"
        )

    def _on_command_replayed(self, event: Any) -> None:
        payload = event.payload or {}
        self._append_log(
            f"Replayed command {payload.get('command')} to {event.device_id} (replay={payload.get('replay')})"
        )

    def _on_generic_event(self, event: Any) -> None:
        self._append_log(f"{getattr(event, 'type', 'unknown')}: {event.payload}")

    def _update_device_row(self, snapshot: Dict[str, Any]) -> None:
        device_id = snapshot.get("device_id")
        if not device_id:
            return
        item = self.device_items.get(device_id)
        if not item:
            item = QTreeWidgetItem([device_id, "", "", "", "", ""])
            self.device_tree.addTopLevelItem(item)
            self.device_items[device_id] = item
        item.setText(1, snapshot.get("status", "unknown"))
        sensors = ", ".join(snapshot.get("sensors", []))
        item.setText(2, sensors)
        last_seen = snapshot.get("last_seen")
        if last_seen:
            item.setText(3, time.strftime("%H:%M:%S", time.localtime(last_seen)))
        item.setText(4, "Yes" if snapshot.get("session_active") else "No")
        info = snapshot.get("info") or {}
        offset = info.get("last_sync_offset_ms")
        rtt = info.get("last_sync_rtt_ms")
        timestamp = info.get("last_sync_timestamp")
        details = []
        if offset is not None and rtt is not None:
            if timestamp:
                ts_str = time.strftime("%H:%M:%S", time.localtime(timestamp))
                details.append(f"sync {offset} ms/{rtt} ms ({ts_str})")
            else:
                details.append(f"sync {offset} ms/{rtt} ms")
        rate = info.get("instant_sample_rate_hz")
        if isinstance(rate, (int, float)):
            details.append(f"{rate:.1f} Hz")
        samples = info.get("samples_received")
        if samples:
            details.append(f"{samples} samples")
        item.setText(5, "; ".join(details) if details else "n/a")

    def _update_image(self, event: Any, frame_type: str) -> None:
        frame_b64 = event.payload.get("frame_b64")
        if not isinstance(frame_b64, str):
            return
        image_bytes = base64.b64decode(frame_b64)
        pixmap = QPixmap()
        if not pixmap.loadFromData(image_bytes):
            return

        if frame_type == "rgb":
            self._rgb_pixmap = pixmap
            self._apply_pixmap(self.rgb_label, pixmap)
        else:
            self._thermal_pixmap = pixmap
            self._apply_pixmap(self.thermal_label, pixmap)

    def _refresh_plot(self) -> None:
        for device_id, history in self.gsr_history.items():
            if device_id not in self.gsr_curves:
                color = pg.intColor(len(self.gsr_curves))
                curve = self.gsr_plot.plot(pen=pg.mkPen(color=color, width=2), name=device_id)
                self.gsr_curves[device_id] = curve

            if not history:
                continue

            base_ts = history[0][0]
            xs = [ts - base_ts for ts, _ in history]
            ys = [value for _, value in history]
            self.gsr_curves[device_id].setData(xs, ys)

    def _apply_pixmap(self, label: QLabel, pixmap: Optional[QPixmap]) -> None:
        if not pixmap:
            label.setText("No frame")
            label.setPixmap(QPixmap())
            return
        scaled = pixmap.scaled(
            label.size(),
            Qt.AspectRatioMode.KeepAspectRatio,
            Qt.TransformationMode.SmoothTransformation,
        )
        label.setPixmap(scaled)

    def resizeEvent(self, event) -> None:  # type: ignore[override]
        super().resizeEvent(event)
        self._apply_pixmap(self.rgb_label, self._rgb_pixmap)
        self._apply_pixmap(self.thermal_label, self._thermal_pixmap)

    def closeEvent(self, event: QCloseEvent) -> None:  # type: ignore[override]
        try:
            self.controller.stop_server()
        finally:
            super().closeEvent(event)

    def _append_log(self, message: str) -> None:
        timestamp = time.strftime("%H:%M:%S")
        self.log_text.append(f"[{timestamp}] {message}")


def run_gui(controller: Any) -> None:
    if not GUI_AVAILABLE:
        raise RuntimeError("PyQt6 + pyqtgraph are required for the GUI")
    app = QApplication.instance() or QApplication([])
    window = MainWindow(controller)
    window.show()
    app.exec()
