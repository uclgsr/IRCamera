#!/usr/bin/env python3
"""
Feature sampler for the IRCamera PC controller.

This script exercises three subsystems:
  1. The native PyBind11 backend used to parse compact Shimmer GSR packets.
  2. The protocol adapter that bridges legacy text frames and JSON payloads.
  3. Session storage helpers that persist telemetry and support exports.

It is intentionally lightweight so that it can run without an Android device
or the PyQt6 GUI stack.
"""

from __future__ import annotations

import shutil
import tempfile
import time
from pathlib import Path

import protocol_adapter
from pc_controller import PCControllerCore, SessionStorage


def _print_header(title: str) -> None:
    print("\n" + "=" * 70)
    print(title)
    print("=" * 70)


def demo_native_backend() -> bool:
    _print_header("1. Native backend (PyBind11) demonstration")
    try:
        import enhanced_native_backend as nb
    except ImportError as exc:
        print(f"[skip] Native backend not built: {exc}")
        print("       Run `python setup.py build_ext --inplace` inside native_backend/")
        return False

    version = getattr(nb, "__version__", "unknown")
    print(f"Module: {nb.__name__}, version: {version}")

    packet = bytes([0xAA, 0x55, 0x00, 0x10, 0x00, 0x00, 0x03, 0xE8, 0x01, 0xF4])
    decoded = nb.parse_gsr_packet(packet)
    print(f"Decoded packet -> sequence={decoded.sequence}, uS={decoded.conductance_us:.2f}")

    samples = [decoded.conductance_us + delta for delta in (-0.5, 0.0, 0.3, 0.7)]
    stats = nb.compute_gsr_statistics(samples)
    print(
        "Statistics: "
        f"mean={stats.mean_us:.2f} uS, stddev={stats.stddev_us:.2f}, "
        f"range=({stats.min_us:.2f}, {stats.max_us:.2f})"
    )
    return True


def demo_protocol_adapter() -> bool:
    _print_header("2. Protocol adapter demonstration")
    adapter = protocol_adapter.ProtocolAdapter()

    legacy = "HELLO device_id=mock_android sensors=[GSR,RGB]"
    converted = adapter.android_to_json(legacy)
    print(f"Legacy '{legacy}' -> JSON: {converted}")

    response = adapter.json_to_android({"type": "ACK", "command": "START_RECORD"})
    print(f"JSON -> legacy text: {response}")

    ack = adapter.create_ack("START_RECORD", session_id="session_demo")
    print(f"create_ack helper -> {ack}")
    return True


def demo_session_storage() -> bool:
    _print_header("3. Session storage + export demonstration")

    with tempfile.TemporaryDirectory() as tmpdir:
        root = Path(tmpdir) / "pc_data"
        storage = SessionStorage(root)
        session_id = "session_demo"
        storage.start_session(session_id, {"notes": "demo run"})

        timestamp = time.time()
        for value in (4.2, 4.4, 4.1, 4.6):
            storage.append_gsr_sample(session_id, "device_01", timestamp, value)
            timestamp += 0.5

        storage.finalize_file(session_id, "device_01", "summary.json")
        csv_path = storage.get_session_dir(session_id) / "device_01_gsr.csv"
        print(f"Data stored at: {csv_path}")

        controller = PCControllerCore(root)
        print(f"Controller storage dir: {controller.get_storage_dir()}")

        export_target = Path(tmpdir) / "session_demo.zip"
        export_base = export_target.with_suffix("")
        shutil.make_archive(str(export_base), "zip", storage.get_session_dir(session_id))
        print(f"Exported archive: {export_base.with_suffix('.zip')}")

    return True


def main() -> None:
    successes = [
        demo_native_backend(),
        demo_protocol_adapter(),
        demo_session_storage(),
    ]
    print("\n" + "=" * 70)
    passed = sum(1 for result in successes if result)
    print(f"Completed {passed}/3 demonstrations successfully.")


if __name__ == "__main__":
    main()
