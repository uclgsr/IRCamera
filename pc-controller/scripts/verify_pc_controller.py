#!/usr/bin/env python3
"""
Sanity checks for the IRCamera PC controller.

The goal of this script is to validate the refactored architecture without
requiring the full Android stack.  It verifies three pillars:
  * the Python controller core can be instantiated and the TCP server started;
  * the protocol adapter correctly round-trips legacy messages;
  * the native backend (if built) decodes GSR packets as expected.
"""

from __future__ import annotations

import socket
import tempfile
from contextlib import closing
from pathlib import Path
from typing import Callable, List, Tuple

from pc_controller import NetworkEvent, PCControllerCore
from protocol_adapter import ProtocolAdapter


def _print_result(ok: bool, message: str) -> None:
    status = "PASS" if ok else "FAIL"
    print(f"[{status}] {message}")


def check_controller_start_stop() -> bool:
    with tempfile.TemporaryDirectory() as tmpdir:
        storage = Path(tmpdir) / "pc_data"
        controller = PCControllerCore(storage)

        # Bind to an ephemeral port to avoid conflicts.
        with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as probe:
            probe.bind(("127.0.0.1", 0))
            _, port = probe.getsockname()

        controller.start_server(host="127.0.0.1", port=port, use_ssl=False)
        controller.stop_server()
    return True


def check_protocol_adapter() -> bool:
    adapter = ProtocolAdapter()
    legacy = "DATA_GSR timestamp=1234 value=2.5"
    json_msg = adapter.android_to_json(legacy)
    if not json_msg or json_msg.get("type") != "DATA_GSR":
        return False
    round_trip = adapter.json_to_android(json_msg)
    return "DATA_GSR" in round_trip


def check_native_backend() -> bool:
    try:
        import enhanced_native_backend as nb
    except ImportError:
        print("[WARN] Native backend not built; skipping check.")
        return True

    packet = bytes([0xAA, 0x55, 0x00, 0x01, 0x00, 0x00, 0x00, 0x64, 0x01, 0xF4])
    decoded = nb.parse_gsr_packet(packet)
    return decoded.valid and abs(decoded.conductance_us - 5.00) < 1e-3


def main() -> int:
    checks: List[Tuple[str, Callable[[], bool]]] = [
        ("Controller start/stop", check_controller_start_stop),
        ("Protocol adapter round-trip", check_protocol_adapter),
        ("Native backend packet parsing", check_native_backend),
    ]

    print("IRCamera PC Controller verification\n" + "-" * 40)
    passed = 0
    for label, func in checks:
        ok = False
        try:
            ok = func()
        except Exception as exc:  # pragma: no cover - defensive output
            print(f"[FAIL] {label} raised {exc}")
        else:
            _print_result(ok, label)
        if ok:
            passed += 1

    total = len(checks)
    print(f"\nSummary: {passed}/{total} checks passed.")
    return 0 if passed == total else 1


if __name__ == "__main__":
    raise SystemExit(main())
