#!/usr/bin/env python3
"""
Data Export Pipeline for Multi-Modal Physiological Sensing Platform

This pipeline implements scientific data export in HDF5 format with:
1. Multi-modal sensor data fusion (thermal, RGB, GSR, PPG)
2. Temporal alignment across devices using calculated offsets
3. Metadata preservation for scientific analysis
4. Standardized format for machine learning pipelines
"""

import asyncio
import json
import os
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import cv2
import h5py
import numpy as np
import pandas as pd

# Import our core services
from src.ircamera_pc.core.config import config


class MultiModalDataExporter:
    """
    Scientific data export pipeline for multi-modal physiological sensing.

    Exports synchronized data from multiple Android sensor nodes into
    standardized HDF5 format suitable for scientific analysis and ML.
    """

    def __init__(self, output_dir: str = "data/exports"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        # Supported data types and their expected formats
        self.data_types = {
            "thermal": {"format": "matrix", "dtype": np.float32, "shape": (256, 192)},
            "rgb": {"format": "video", "dtype": np.uint8, "shape": (1920, 1080, 3)},
            "gsr": {
                "format": "timeseries",
                "dtype": np.float64,
                "columns": ["timestamp", "gsr_microsiemens"],
            },
            "ppg": {
                "format": "timeseries",
                "dtype": np.float64,
                "columns": ["timestamp", "ppg_raw"],
            },
            "imu": {
                "format": "timeseries",
                "dtype": np.float64,
                "columns": [
                    "timestamp",
                    "acc_x",
                    "acc_y",
                    "acc_z",
                    "gyro_x",
                    "gyro_y",
                    "gyro_z",
                ],
            },
            "audio": {"format": "audio", "dtype": np.float32, "sample_rate": 44100},
        }

    async def export_session(
        self,
        session_id: str,
        device_data: Dict[str, Dict[str, Any]],
        clock_offsets: Dict[str, float],
        session_metadata: Dict[str, Any],
    ) -> str:
        """
        Export complete multi-modal session to HDF5 format.

        Args:
            session_id: Unique session identifier
            device_data: Data from each device {device_id: {sensor_type: data}}
            clock_offsets: Clock offsets for each device in nanoseconds
            session_metadata: Session-level metadata

        Returns:
            Path to exported HDF5 file
        """
        output_file = self.output_dir / f"session_{session_id}.h5"

        print(f"📊 Exporting session {session_id} to {output_file}")

        with h5py.File(output_file, "w") as h5_file:
            # Store session metadata
            await self._store_session_metadata(h5_file, session_id, session_metadata)

            # Store device metadata and clock offsets
            await self._store_device_metadata(h5_file, device_data, clock_offsets)

            # Process and store sensor data with temporal alignment
            await self._store_aligned_sensor_data(h5_file, device_data, clock_offsets)

            # Create synchronized timeline for analysis
            await self._create_synchronized_timeline(
                h5_file, device_data, clock_offsets
            )

            # Store analysis metadata
            await self._store_analysis_metadata(h5_file, device_data)

        print(f"✅ Session exported successfully: {output_file}")
        return str(output_file)

    async def _store_session_metadata(
        self, h5_file: h5py.File, session_id: str, metadata: Dict[str, Any]
    ):
        """Store session-level metadata."""
        session_group = h5_file.create_group("session")

        # Basic session info
        session_group.attrs["session_id"] = session_id
        session_group.attrs["export_timestamp"] = datetime.now(timezone.utc).isoformat()
        session_group.attrs["platform_version"] = "1.0.0"

        # Session metadata
        for key, value in metadata.items():
            if isinstance(value, (str, int, float, bool)):
                session_group.attrs[key] = value
            else:
                # Store complex data as JSON string
                session_group.attrs[key] = json.dumps(value)

        print(f"  📋 Session metadata stored: {len(metadata)} fields")

    async def _store_device_metadata(
        self,
        h5_file: h5py.File,
        device_data: Dict[str, Dict[str, Any]],
        clock_offsets: Dict[str, float],
    ):
        """Store device metadata and clock synchronization info."""
        devices_group = h5_file.create_group("devices")

        for device_id, data in device_data.items():
            device_group = devices_group.create_group(device_id)

            # Store device info
            device_group.attrs["device_id"] = device_id
            device_group.attrs["clock_offset_ns"] = clock_offsets.get(device_id, 0)

            # Store available sensors
            sensors = list(data.keys())
            device_group.attrs["sensors"] = sensors
            device_group.attrs["sensor_count"] = len(sensors)

            print(
                f"  📱 Device {device_id}: {len(sensors)} sensors, offset: {clock_offsets.get(device_id, 0)/1e6:.2f}ms"
            )

    async def _store_aligned_sensor_data(
        self,
        h5_file: h5py.File,
        device_data: Dict[str, Dict[str, Any]],
        clock_offsets: Dict[str, float],
    ):
        """Store sensor data with temporal alignment applied."""
        data_group = h5_file.create_group("sensor_data")

        for device_id, device_sensors in device_data.items():
            device_group = data_group.create_group(device_id)
            clock_offset = clock_offsets.get(device_id, 0)

            for sensor_type, sensor_data in device_sensors.items():
                await self._store_sensor_data(
                    device_group, sensor_type, sensor_data, clock_offset
                )

    async def _store_sensor_data(
        self,
        parent_group: h5py.Group,
        sensor_type: str,
        sensor_data: Any,
        clock_offset: float,
    ):
        """Store individual sensor data with appropriate format."""
        sensor_group = parent_group.create_group(sensor_type)

        data_spec = self.data_types.get(sensor_type, {})
        data_format = data_spec.get("format", "unknown")

        if data_format == "timeseries":
            await self._store_timeseries_data(sensor_group, sensor_data, clock_offset)
        elif data_format == "matrix":
            await self._store_matrix_data(sensor_group, sensor_data, clock_offset)
        elif data_format == "video":
            await self._store_video_data(sensor_group, sensor_data, clock_offset)
        elif data_format == "audio":
            await self._store_audio_data(sensor_group, sensor_data, clock_offset)
        else:
            # Generic data storage
            await self._store_generic_data(sensor_group, sensor_data, clock_offset)

        # Store metadata
        sensor_group.attrs["sensor_type"] = sensor_type
        sensor_group.attrs["clock_offset_applied_ns"] = clock_offset
        sensor_group.attrs["data_format"] = data_format

        print(f"    💾 {sensor_type}: {data_format} format")

    async def _store_timeseries_data(
        self, sensor_group: h5py.Group, data: List[Dict], clock_offset: float
    ):
        """Store timeseries data (GSR, PPG, IMU)."""
        if not data:
            return

        # Convert to DataFrame for easier manipulation
        df = pd.DataFrame(data)

        # Apply clock offset to timestamps
        if "timestamp" in df.columns:
            df["timestamp_aligned"] = df["timestamp"] + clock_offset
            df["timestamp_original"] = df["timestamp"]

        # Store as HDF5 datasets
        for column in df.columns:
            if column == "timestamp" or column == "timestamp_aligned":
                # Store timestamps as int64 nanoseconds
                dataset = sensor_group.create_dataset(
                    column, data=df[column].astype(np.int64), compression="gzip"
                )
            else:
                # Store numeric data
                dataset = sensor_group.create_dataset(
                    column, data=df[column].astype(np.float64), compression="gzip"
                )

            dataset.attrs["unit"] = self._get_unit_for_column(column)

        # Store sampling statistics
        sensor_group.attrs["sample_count"] = len(df)
        if len(df) > 1:
            time_diff = df["timestamp"].iloc[-1] - df["timestamp"].iloc[0]
            sensor_group.attrs["duration_ns"] = int(time_diff)
            sensor_group.attrs["sample_rate_hz"] = len(df) / (time_diff / 1e9)

    async def _store_matrix_data(
        self, sensor_group: h5py.Group, data: List[Dict], clock_offset: float
    ):
        """Store matrix data (thermal images)."""
        if not data:
            return

        # Extract frames and timestamps
        frames = []
        timestamps = []

        for frame_data in data:
            if "matrix" in frame_data and "timestamp" in frame_data:
                frames.append(frame_data["matrix"])
                timestamps.append(frame_data["timestamp"] + clock_offset)

        if frames:
            # Store frame stack
            frames_array = np.array(frames, dtype=np.float32)
            frames_dataset = sensor_group.create_dataset(
                "frames", data=frames_array, compression="gzip", compression_opts=6
            )
            frames_dataset.attrs["shape"] = frames_array.shape
            frames_dataset.attrs["dtype"] = str(frames_array.dtype)

            # Store aligned timestamps
            timestamps_dataset = sensor_group.create_dataset(
                "timestamps_aligned",
                data=np.array(timestamps, dtype=np.int64),
                compression="gzip",
            )

            # Store metadata
            sensor_group.attrs["frame_count"] = len(frames)
            sensor_group.attrs["frame_shape"] = frames_array.shape[1:]
            if len(timestamps) > 1:
                duration_ns = timestamps[-1] - timestamps[0]
                sensor_group.attrs["duration_ns"] = int(duration_ns)
                sensor_group.attrs["fps"] = len(frames) / (duration_ns / 1e9)

    async def _store_video_data(
        self, sensor_group: h5py.Group, data: List[str], clock_offset: float
    ):
        """Store video data (RGB frames)."""
        # For video data, we typically store file paths and frame extraction info
        # In a real implementation, we might extract keyframes or store video metadata

        if data:
            # Store video file paths
            video_files = [str(path) for path in data if isinstance(path, (str, Path))]
            if video_files:
                dt = h5py.special_dtype(vlen=str)
                dataset = sensor_group.create_dataset(
                    "video_files", (len(video_files),), dtype=dt
                )
                dataset[:] = video_files

                sensor_group.attrs["video_count"] = len(video_files)
                sensor_group.attrs["clock_offset_applied_ns"] = clock_offset

    async def _store_audio_data(
        self, sensor_group: h5py.Group, data: Dict, clock_offset: float
    ):
        """Store audio data."""
        if "samples" in data and "sample_rate" in data:
            samples = np.array(data["samples"], dtype=np.float32)

            # Store audio samples
            audio_dataset = sensor_group.create_dataset(
                "samples", data=samples, compression="gzip"
            )

            # Apply timestamp offset
            if "start_timestamp" in data:
                aligned_start = data["start_timestamp"] + clock_offset
                sensor_group.attrs["start_timestamp_aligned"] = int(aligned_start)

            sensor_group.attrs["sample_rate"] = data["sample_rate"]
            sensor_group.attrs["duration_seconds"] = len(samples) / data["sample_rate"]

    async def _store_generic_data(
        self, sensor_group: h5py.Group, data: Any, clock_offset: float
    ):
        """Store generic data as JSON string."""
        json_data = json.dumps(data, default=str)

        dt = h5py.special_dtype(vlen=str)
        dataset = sensor_group.create_dataset("json_data", (1,), dtype=dt)
        dataset[0] = json_data

        sensor_group.attrs["data_type"] = "json"
        sensor_group.attrs["clock_offset_applied_ns"] = clock_offset

    async def _create_synchronized_timeline(
        self,
        h5_file: h5py.File,
        device_data: Dict[str, Dict[str, Any]],
        clock_offsets: Dict[str, float],
    ):
        """Create synchronized timeline for cross-modal analysis."""
        timeline_group = h5_file.create_group("synchronized_timeline")

        # Collect all timestamps from all sensors
        all_events = []

        for device_id, device_sensors in device_data.items():
            clock_offset = clock_offsets.get(device_id, 0)

            for sensor_type, sensor_data in device_sensors.items():
                events = self._extract_events(
                    sensor_data, device_id, sensor_type, clock_offset
                )
                all_events.extend(events)

        if all_events:
            # Sort by aligned timestamp
            all_events.sort(key=lambda x: x["timestamp_aligned"])

            # Create timeline dataset
            event_df = pd.DataFrame(all_events)

            for column in event_df.columns:
                if "timestamp" in column:
                    dataset = timeline_group.create_dataset(
                        column,
                        data=event_df[column].astype(np.int64),
                        compression="gzip",
                    )
                else:
                    dt = h5py.special_dtype(vlen=str)
                    dataset = timeline_group.create_dataset(
                        column, (len(event_df),), dtype=dt
                    )
                    dataset[:] = event_df[column].astype(str)

            timeline_group.attrs["event_count"] = len(all_events)
            if len(all_events) > 1:
                duration_ns = (
                    all_events[-1]["timestamp_aligned"]
                    - all_events[0]["timestamp_aligned"]
                )
                timeline_group.attrs["total_duration_ns"] = int(duration_ns)

            print(f"  🕐 Synchronized timeline: {len(all_events)} events")

    def _extract_events(
        self, sensor_data: Any, device_id: str, sensor_type: str, clock_offset: float
    ) -> List[Dict]:
        """Extract timestamp events from sensor data."""
        events = []

        if isinstance(sensor_data, list):
            for i, item in enumerate(sensor_data):
                if isinstance(item, dict) and "timestamp" in item:
                    events.append(
                        {
                            "timestamp_original": item["timestamp"],
                            "timestamp_aligned": item["timestamp"] + clock_offset,
                            "device_id": device_id,
                            "sensor_type": sensor_type,
                            "event_index": i,
                            "event_type": "data_point",
                        }
                    )

        return events

    async def _store_analysis_metadata(
        self, h5_file: h5py.File, device_data: Dict[str, Dict[str, Any]]
    ):
        """Store metadata useful for analysis."""
        analysis_group = h5_file.create_group("analysis")

        # Calculate data statistics
        stats = {
            "device_count": len(device_data),
            "total_sensors": sum(len(sensors) for sensors in device_data.values()),
            "sensor_types": list(
                set(
                    sensor_type
                    for sensors in device_data.values()
                    for sensor_type in sensors.keys()
                )
            ),
        }

        # Store statistics
        for key, value in stats.items():
            if isinstance(value, list):
                dt = h5py.special_dtype(vlen=str)
                dataset = analysis_group.create_dataset(key, (len(value),), dtype=dt)
                dataset[:] = [str(v) for v in value]
            else:
                analysis_group.attrs[key] = value

        # Store format version for future compatibility
        analysis_group.attrs["format_version"] = "1.0.0"
        analysis_group.attrs["created_by"] = "IRCamera Multi-Modal Platform"

        print(
            f"  📈 Analysis metadata: {stats['device_count']} devices, {stats['total_sensors']} sensors"
        )

    def _get_unit_for_column(self, column: str) -> str:
        """Get appropriate unit for data column."""
        unit_map = {
            "timestamp": "nanoseconds",
            "timestamp_aligned": "nanoseconds",
            "timestamp_original": "nanoseconds",
            "gsr_microsiemens": "µS",
            "ppg_raw": "ADC_counts",
            "acc_x": "m/s²",
            "acc_y": "m/s²",
            "acc_z": "m/s²",
            "gyro_x": "rad/s",
            "gyro_y": "rad/s",
            "gyro_z": "rad/s",
            "temperature": "°C",
            "heart_rate": "bpm",
        }
        return unit_map.get(column, "unknown")


async def test_data_export_pipeline():
    """Test the data export pipeline with simulated multi-modal data."""
    print("🧪 Testing Multi-Modal Data Export Pipeline")
    print("=" * 60)

    exporter = MultiModalDataExporter()

    # Create simulated multi-modal data
    session_id = f"test_session_{int(datetime.now().timestamp())}"

    # Simulate device data
    device_data = {
        "android_001": {
            "thermal": [
                {
                    "timestamp": 1700000000000000000,  # ns
                    "matrix": np.random.rand(256, 192).astype(np.float32) * 50
                    + 20,  # 20-70°C
                },
                {
                    "timestamp": 1700000000111111111,  # 111ms later
                    "matrix": np.random.rand(256, 192).astype(np.float32) * 50 + 20,
                },
            ],
            "gsr": [
                {"timestamp": 1700000000000000000, "gsr_microsiemens": 2.5},
                {
                    "timestamp": 1700000000020000000,
                    "gsr_microsiemens": 2.7,
                },  # 20ms later
                {
                    "timestamp": 1700000000040000000,
                    "gsr_microsiemens": 2.4,
                },  # 40ms later
            ],
        },
        "android_002": {
            "rgb": ["/path/to/video1.mp4", "/path/to/video2.mp4"],
            "ppg": [
                {"timestamp": 1700000000005000000, "ppg_raw": 1024},  # 5ms offset
                {"timestamp": 1700000000025000000, "ppg_raw": 1056},
                {"timestamp": 1700000000045000000, "ppg_raw": 1012},
            ],
        },
    }

    # Simulate clock offsets (in nanoseconds)
    clock_offsets = {
        "android_001": 0,  # Reference device
        "android_002": 2500000,  # 2.5ms fast
    }

    # Session metadata
    session_metadata = {
        "participant_id": "P001",
        "session_type": "baseline_measurement",
        "duration_minutes": 5,
        "researcher": "Dr. Smith",
        "location": "Lab A",
        "temperature_celsius": 22.5,
        "notes": "Normal session, no issues",
    }

    try:
        # Export the session
        output_file = await exporter.export_session(
            session_id, device_data, clock_offsets, session_metadata
        )

        print(f"\n✅ Export completed successfully!")
        print(f"📁 Output file: {output_file}")

        # Validate the exported file
        await validate_exported_file(output_file)

        return True

    except Exception as e:
        print(f"❌ Export failed: {e}")
        return False


async def validate_exported_file(file_path: str):
    """Validate the structure and content of exported HDF5 file."""
    print(f"\n🔍 Validating exported file: {file_path}")

    try:
        with h5py.File(file_path, "r") as h5_file:
            # Check main groups
            expected_groups = [
                "session",
                "devices",
                "sensor_data",
                "synchronized_timeline",
                "analysis",
            ]
            for group_name in expected_groups:
                if group_name in h5_file:
                    print(f"  ✅ {group_name}: Present")
                else:
                    print(f"  ❌ {group_name}: Missing")

            # Check session metadata
            if "session" in h5_file:
                session_attrs = dict(h5_file["session"].attrs)
                print(f"  📋 Session attributes: {len(session_attrs)} fields")

            # Check device data
            if "devices" in h5_file:
                device_count = len(h5_file["devices"])
                print(f"  📱 Devices: {device_count} found")

            # Check sensor data
            if "sensor_data" in h5_file:
                total_sensors = 0
                for device_id in h5_file["sensor_data"]:
                    sensor_count = len(h5_file["sensor_data"][device_id])
                    total_sensors += sensor_count
                    print(f"    {device_id}: {sensor_count} sensors")
                print(f"  💾 Total sensors: {total_sensors}")

            # Check timeline
            if "synchronized_timeline" in h5_file:
                timeline = h5_file["synchronized_timeline"]
                if "timestamp_aligned" in timeline:
                    event_count = len(timeline["timestamp_aligned"])
                    print(f"  🕐 Timeline events: {event_count}")

        print("  ✅ File validation completed successfully")

    except Exception as e:
        print(f"  ❌ File validation failed: {e}")


if __name__ == "__main__":
    print("📊 IRCamera Data Export Pipeline Test")
    print("Multi-Modal Physiological Sensing Platform")
    print("=" * 60)

    success = asyncio.run(test_data_export_pipeline())
    exit(0 if success else 1)
