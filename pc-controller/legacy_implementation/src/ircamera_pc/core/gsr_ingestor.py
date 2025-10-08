#!/usr/bin/env python3

import json
import struct
import time
from dataclasses import asdict, dataclass
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional


class GSRMode(Enum):
    LOCAL = "local"
    BRIDGED = "bridged"


@dataclass
class GSRSample:
    timestamp: float
    value: float
    quality: int
    device_id: str

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class GSRDataSet:
    session_id: str
    device_id: str
    mode: GSRMode
    start_time: float
    end_time: float
    samples: List[GSRSample]
    sample_rate: float
    quality_stats: Dict[str, float]

    def to_dict(self) -> Dict[str, Any]:
        return {
            "session_id": self.session_id,
            "device_id": self.device_id,
            "mode": self.mode.value,
            "start_time": self.start_time,
            "end_time": self.end_time,
            "samples": [sample.to_dict() for sample in self.samples],
            "sample_rate": self.sample_rate,
            "quality_stats": self.quality_stats,
        }


class GSRIngestor:

    def __init__(self, config: Dict[str, Any]):

        self.config = config.get("gsr", {})
        self.data_dir = Path(self.config.get("data_dir", "data/gsr"))
        self.data_dir.mkdir(parents=True, exist_ok=True)

        self.min_sample_rate = self.config.get("min_sample_rate", 10.0)
        self.max_sample_rate = self.config.get("max_sample_rate", 1000.0)
        self.quality_threshold = self.config.get("quality_threshold", 50)
        self.max_gap_duration = self.config.get("max_gap_duration", 5.0)

        self.active_sessions: Dict[str, GSRDataSet] = {}
        self.completed_sessions: Dict[str, GSRDataSet] = {}

    async def start_session(
            self, session_id: str, device_id: str, mode: GSRMode
    ) -> bool:

        if session_id in self.active_sessions:
            return False

        dataset = GSRDataSet(
            session_id=session_id,
            device_id=device_id,
            mode=mode,
            start_time=time.time(),
            end_time=0.0,
            samples=[],
            sample_rate=0.0,
            quality_stats={"min": 100, "max": 0, "mean": 0},
        )

        self.active_sessions[session_id] = dataset
        return True

    async def ingest_sample(self, session_id: str, sample_data: bytes) -> bool:

        if session_id not in self.active_sessions:
            return False

        dataset = self.active_sessions[session_id]

        if len(sample_data) < 16:
            return False

        timestamp, value, quality = struct.unpack("<dfi", sample_data[:16])

        sample = GSRSample(
            timestamp=timestamp,
            value=value,
            quality=quality,
            device_id=dataset.device_id,
        )

        if not self._validate_sample(sample, dataset):
            return False

        dataset.samples.append(sample)
        self._update_quality_stats(dataset, sample)

        return True

    async def end_session(self, session_id: str) -> Optional[GSRDataSet]:

        if session_id not in self.active_sessions:
            return None

        dataset = self.active_sessions[session_id]
        dataset.end_time = time.time()

        if len(dataset.samples) > 1:
            duration = dataset.end_time - dataset.start_time
            dataset.sample_rate = (
                len(dataset.samples) / duration if duration > 0 else 0.0
            )

        if dataset.samples:
            qualities = [sample.quality for sample in dataset.samples]
            dataset.quality_stats = {
                "min": min(qualities),
                "max": max(qualities),
                "mean": sum(qualities) / len(qualities),
            }

        self.completed_sessions[session_id] = dataset
        del self.active_sessions[session_id]

        await self._save_dataset(dataset)

        return dataset

    def _validate_sample(self, sample: GSRSample, dataset: GSRDataSet) -> bool:

        if sample.quality < self.quality_threshold:
            return False

        if not (10.0 <= sample.value <= 1000000.0):
            return False

        if dataset.samples and sample.timestamp <= dataset.samples[-1].timestamp:
            return False

        return True

    def _update_quality_stats(self, dataset: GSRDataSet, sample: GSRSample):

        stats = dataset.quality_stats
        stats["min"] = min(stats["min"], sample.quality)
        stats["max"] = max(stats["max"], sample.quality)

        n = len(dataset.samples)
        if n == 1:
            stats["mean"] = sample.quality
        else:
            stats["mean"] = ((stats["mean"] * (n - 1)) + sample.quality) / n

    async def _save_dataset(self, dataset: GSRDataSet):

        filename = f"gsr_{dataset.session_id}_{dataset.device_id}.json"
        filepath = self.data_dir / filename

        with open(filepath, "w") as f:
            json.dump(dataset.to_dict(), f, indent=2)

    async def load_dataset(
            self, session_id: str, device_id: str
    ) -> Optional[GSRDataSet]:

        filename = f"gsr_{session_id}_{device_id}.json"
        filepath = self.data_dir / filename

        if not filepath.exists():
            return None

        with open(filepath, "r") as f:
            data = json.load(f)

        samples = [GSRSample(**sample_data) for sample_data in data["samples"]]

        dataset = GSRDataSet(
            session_id=data["session_id"],
            device_id=data["device_id"],
            mode=GSRMode(data["mode"]),
            start_time=data["start_time"],
            end_time=data["end_time"],
            samples=samples,
            sample_rate=data["sample_rate"],
            quality_stats=data["quality_stats"],
        )

        return dataset

    def get_session_status(self, session_id: str) -> Optional[Dict[str, Any]]:

        if session_id in self.active_sessions:
            dataset = self.active_sessions[session_id]
            return {
                "status": "active",
                "device_id": dataset.device_id,
                "mode": dataset.mode.value,
                "samples_collected": len(dataset.samples),
                "duration": time.time() - dataset.start_time,
                "current_quality": dataset.quality_stats["mean"],
            }
        elif session_id in self.completed_sessions:
            dataset = self.completed_sessions[session_id]
            return {
                "status": "completed",
                "device_id": dataset.device_id,
                "mode": dataset.mode.value,
                "samples_collected": len(dataset.samples),
                "duration": dataset.end_time - dataset.start_time,
                "sample_rate": dataset.sample_rate,
                "quality_stats": dataset.quality_stats,
            }
        else:
            return None

    def get_active_sessions(self) -> List[str]:

        return list(self.active_sessions.keys())

    def get_completed_sessions(self) -> List[str]:

        return list(self.completed_sessions.keys())
