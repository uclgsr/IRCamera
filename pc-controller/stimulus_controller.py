#!/usr/bin/env python3
"""
Stimulus orchestration utilities (FR7).

The controller needs a single coordination point for synchronisation signals
and external stimuli so that Android devices and the PC timeline share the same
event markers. The `StimulusController` encapsulates that behaviour and keeps a
log of emitted events which is later folded into the session metadata.
"""

from __future__ import annotations

import logging
import threading
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Dict, List, Optional

logger = logging.getLogger(__name__)

EventCallback = Callable[[str, Dict[str, object]], None]


@dataclass
class StimulusEvent:
    code: str
    timestamp: float
    payload: Dict[str, object] = field(default_factory=dict)


class StimulusController:
    """Keeps an ordered list of stimulus events and notifies listeners."""

    def __init__(self, notifier: EventCallback):
        self._notifier = notifier
        self._events: List[StimulusEvent] = []
        self._lock = threading.Lock()

    # ---------------------------------------------------------------- emitters
    def flash_sync(self, intensity: float = 1.0, duration_s: float = 0.25) -> StimulusEvent:
        event = StimulusEvent(
            code="flash_sync",
            timestamp=time.time(),
            payload={"intensity": float(intensity), "duration_s": float(duration_s)},
        )
        return self._record(event)

    def audio_beep(self, frequency_hz: float = 440.0, duration_s: float = 0.2) -> StimulusEvent:
        event = StimulusEvent(
            code="audio_beep",
            timestamp=time.time(),
            payload={"frequency_hz": float(frequency_hz), "duration_s": float(duration_s)},
        )
        return self._record(event)

    def stimulus_video(self, path: Path, autoplay: bool = True) -> StimulusEvent:
        event = StimulusEvent(
            code="stimulus_video",
            timestamp=time.time(),
            payload={"path": str(path), "autoplay": autoplay},
        )
        return self._record(event)

    def custom_marker(self, label: str, extra: Optional[Dict[str, object]] = None) -> StimulusEvent:
        event = StimulusEvent(
            code=str(label),
            timestamp=time.time(),
            payload=extra or {},
        )
        return self._record(event)

    # ---------------------------------------------------------------- storage/query
    def dump(self) -> List[StimulusEvent]:
        with self._lock:
            return list(self._events)

    def clear(self) -> None:
        with self._lock:
            self._events.clear()

    # ---------------------------------------------------------------- internals
    def _record(self, event: StimulusEvent) -> StimulusEvent:
        with self._lock:
            self._events.append(event)
        try:
            self._notifier(event.code, {"timestamp": event.timestamp, **event.payload})
        except Exception:  # pragma: no cover - defensive logging
            logger.exception("Stimulus notifier failed for %s", event.code)
        return event


__all__ = ["StimulusController", "StimulusEvent"]
