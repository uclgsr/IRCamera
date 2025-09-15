"""
Data Aggregation Engine for Multi-Modal Physiological Sensing Platform

This module implements the data aggregation engine that handles synchronized
data streams from multiple sensors according to the Hub-and-Spoke architecture.
"""

import asyncio
import h5py
import json
import numpy as np
import pandas as pd
import threading
import time
from collections import defaultdict, deque
from dataclasses import dataclass, field
from loguru import logger
from pathlib import Path
from queue import Empty, Queue
from typing import Any, Dict, List, Optional, Tuple, Union


@dataclass
class DataStream:
    """Represents a single data stream from a sensor."""

    device_id: str
    stream_type: str  # 'gsr', 'rgb_video', 'thermal_video', 'raw_images'
    sample_rate: float
    start_timestamp_ns: int
    data_buffer: deque = field(default_factory=lambda: deque(maxlen=100000))
    last_timestamp_ns: int = 0
    total_samples: int = 0
    dropped_samples: int = 0
    is_active: bool = True


@dataclass
class SyncEvent:
    """Represents a synchronization event across all streams."""

    timestamp_ns: int
    event_type: str  # 'flash', 'manual_mark', 'auto_sync'
    source_device: str
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class AggregationStats:
    """Statistics for data aggregation performance."""

    total_devices: int = 0
    active_streams: int = 0
    data_rate_mbps: float = 0.0
    sync_quality_percent: float = 0.0
    buffer_usage_percent: float = 0.0
    dropped_frames_total: int = 0
    last_sync_seconds_ago: float = 0.0
    session_duration_seconds: float = 0.0


class DataAggregationEngine:
    """
    Core data aggregation engine for synchronized multi-modal streams.

    Features:
    - Real-time data stream management
    - Temporal synchronization with sub-5ms accuracy
    - HDF5-based data export for analysis
    - Automatic buffer management
    - Sync event tracking and alignment
    - Performance monitoring and statistics
    """

    def __init__(self, session_directory: Path, buffer_size_mb: int = 1000):
        """
        Initialize data aggregation engine.

        Args:
            session_directory: Directory for session data storage
            buffer_size_mb: Maximum buffer size in megabytes
        """
        self.session_directory = Path(session_directory)
        self.session_directory.mkdir(parents=True, exist_ok=True)

        self.buffer_size_bytes = buffer_size_mb * 1024 * 1024

        # Data streams management
        self.streams: Dict[str, DataStream] = {}  # stream_id: DataStream
        self.sync_events = List[SyncEvent] = []

        # Threading and async management
        self.data_queue = Queue()
        self.sync_queue = Queue()
        self.aggregation_thread: Optional[threading.Thread] = None
        self.is_running = threading.Event()

        # Performance tracking
        self.stats = AggregationStats()
        self.start_time = time.time()
        self.last_stats_update = time.time()

        # HDF5 export configuration
        self.hdf5_file: Optional[h5py.File] = None
        self.export_enabled = True

        logger.info(
            f"Data aggregation engine initialized for session: {session_directory}"
        )

    def start(self) -> None:
        """Start the data aggregation engine."""
        if self.is_running.is_set():
            logger.warning("Data aggregation engine already running")
            return

        self.is_running.set()

        # Initialize HDF5 export file
        if self.export_enabled:
            self._initialize_hdf5_export()

        # Start aggregation thread
        self.aggregation_thread = threading.Thread(
            target=self._aggregation_loop, name="DataAggregationThread"
        )
        self.aggregation_thread.start()

        logger.info("Data aggregation engine started")

    def stop(self) -> None:
        """Stop the data aggregation engine and finalize exports."""
        if not self.is_running.is_set():
            return

        logger.info("Stopping data aggregation engine...")

        self.is_running.clear()

        # Wait for aggregation thread to finish
        if self.aggregation_thread and self.aggregation_thread.is_alive():
            self.aggregation_thread.join(timeout=5.0)

        # Finalize exports
        self._finalize_exports()

        logger.info("Data aggregation engine stopped")

    def add_stream(self, device_id: str, stream_type: str, sample_rate: float) -> str:
        """
        Add a new data stream.

        Args:
            device_id: Unique device identifier
            stream_type: Type of stream ('gsr', 'rgb_video', etc.)
            sample_rate: Expected sample rate in Hz

        Returns:
            Stream ID for referencing this stream
        """
        stream_id = f"{device_id}_{stream_type}"

        if stream_id in self.streams:
            logger.warning(f"Stream {stream_id} already exists, updating configuration")

        stream = DataStream(
            device_id=device_id,
            stream_type=stream_type,
            sample_rate=sample_rate,
            start_timestamp_ns=time.time_ns(),
        )

        self.streams[stream_id] = stream

        # Create HDF5 dataset if export is enabled
        if self.export_enabled and self.hdf5_file:
            self._create_hdf5_dataset(stream_id, stream)

        logger.info(f"Added data stream: {stream_id} ({stream_type}, {sample_rate}Hz)")
        return stream_id

    def remove_stream(self, stream_id: str) -> bool:
        """
        Remove a data stream.

        Args:
            stream_id: Stream identifier

        Returns:
            True if stream was removed, False if not found
        """
        if stream_id not in self.streams:
            return False

        # Mark stream as inactive
        self.streams[stream_id].is_active = False

        # Export remaining data
        if self.export_enabled:
            self._export_stream_data(stream_id)

        del self.streams[stream_id]

        logger.info(f"Removed data stream: {stream_id}")
        return True

    def add_data(self, stream_id: str, timestamp_ns: int, data: Any) -> bool:
        """
        Add data point to a stream.

        Args:
            stream_id: Stream identifier
            timestamp_ns: Data timestamp in nanoseconds
            data: Data payload (varies by stream type)

        Returns:
            True if data was added successfully
        """
        if not self.is_running.is_set():
            return False

        if stream_id not in self.streams:
            logger.warning(f"Attempted to add data to unknown stream: {stream_id}")
            return False

        # Queue data for processing
        self.data_queue.put(
            {
                "stream_id": stream_id,
                "timestamp_ns": timestamp_ns,
                "data": data,
                "received_at": time.time_ns(),
            }
        )

        return True

    def add_sync_event(
            self,
            event_type: str,
            source_device: str,
            timestamp_ns: Optional[int] = None,
            metadata: Optional[Dict] = None,
    ) -> None:
        """
        Add synchronization event.

        Args:
            event_type: Type of sync event
            source_device: Device that triggered the event
            timestamp_ns: Event timestamp (current time if None)
            metadata: Additional event metadata
        """
        if timestamp_ns is None:
            timestamp_ns = time.time_ns()

        if metadata is None:
            metadata = {}

        sync_event = SyncEvent(
            timestamp_ns=timestamp_ns,
            event_type=event_type,
            source_device=source_device,
            metadata=metadata,
        )

        self.sync_queue.put(sync_event)

        logger.info(f"Added sync event: {event_type} from {source_device}")

    def get_statistics(self) -> AggregationStats:
        """Get current aggregation statistics."""
        self._update_statistics()
        return self.stats

    def get_stream_data(
            self, stream_id: str, last_n: int = 1000
    ) -> List[Tuple[int, Any]]:
        """
        Get recent data from a stream.

        Args:
            stream_id: Stream identifier
            last_n: Number of most recent samples to return

        Returns:
            List of (timestamp_ns, data) tuples
        """
        if stream_id not in self.streams:
            return []

        stream = self.streams[stream_id]
        data_list = list(stream.data_buffer)

        if last_n < len(data_list):
            data_list = data_list[-last_n:]

        return data_list

    def export_session_data(self, export_path: Optional[Path] = None) -> Path:
        """
        Export all session data to various formats.

        Args:
            export_path: Custom export path (uses session directory if None)

        Returns:
            Path to exported data directory
        """
        if export_path is None:
            export_path = self.session_directory / "exports"

        export_path.mkdir(parents=True, exist_ok=True)

        # Export sync events
        sync_data = []
        for event in self.sync_events:
            sync_data.append(
                {
                    "timestamp_ns": event.timestamp_ns,
                    "event_type": event.event_type,
                    "source_device": event.source_device,
                    "metadata": event.metadata,
                }
            )

        sync_df = pd.DataFrame(sync_data)
        sync_df.to_csv(export_path / "sync_events.csv", index=False)

        # Export stream summaries
        stream_summary = []
        for stream_id, stream in self.streams.items():
            stream_summary.append(
                {
                    "stream_id": stream_id,
                    "device_id": stream.device_id,
                    "stream_type": stream.stream_type,
                    "sample_rate": stream.sample_rate,
                    "start_timestamp_ns": stream.start_timestamp_ns,
                    "total_samples": stream.total_samples,
                    "dropped_samples": stream.dropped_samples,
                    "is_active": stream.is_active,
                }
            )

        summary_df = pd.DataFrame(stream_summary)
        summary_df.to_csv(export_path / "stream_summary.csv", index=False)

        # Export session metadata
        session_metadata = {
            "session_directory": str(self.session_directory),
            "start_time": self.start_time,
            "duration_seconds": time.time() - self.start_time,
            "total_streams": len(self.streams),
            "total_sync_events": len(self.sync_events),
            "statistics": {
                "total_devices": self.stats.total_devices,
                "data_rate_mbps": self.stats.data_rate_mbps,
                "sync_quality_percent": self.stats.sync_quality_percent,
                "dropped_frames_total": self.stats.dropped_frames_total,
            },
        }

        with open(export_path / "session_metadata.json", "w") as f:
            json.dump(session_metadata, f, indent=2)

        logger.info(f"Session data exported to: {export_path}")
        return export_path

    def _aggregation_loop(self) -> None:
        """Main aggregation loop running in separate thread."""
        logger.info("Data aggregation loop started")

        while self.is_running.is_set():
            try:
                # Process data queue
                self._process_data_queue()

                # Process sync events
                self._process_sync_queue()

                # Update statistics
                self._update_statistics()

                # Export data periodically
                if self.export_enabled:
                    self._periodic_export()

                # Small delay to prevent CPU spinning
                time.sleep(0.001)  # 1ms

            except Exception as e:
                logger.error(f"Error in aggregation loop: {e}")
                time.sleep(0.1)  # Longer delay on error

        logger.info("Data aggregation loop stopped")

    def _process_data_queue(self) -> None:
        """Process queued data points."""
        processed_count = 0

        while processed_count < 1000:  # Limit per iteration
            try:
                data_item = self.data_queue.get_nowait()

                stream_id = data_item["stream_id"]
                timestamp_ns = data_item["timestamp_ns"]
                data = data_item["data"]

                if stream_id in self.streams:
                    stream = self.streams[stream_id]

                    # Add to buffer
                    stream.data_buffer.append((timestamp_ns, data))
                    stream.last_timestamp_ns = timestamp_ns
                    stream.total_samples += 1

                    # Check for dropped samples (basic heuristic)
                    if stream.total_samples > 1:
                        expected_interval = 1e9 / stream.sample_rate  # nanoseconds
                        time_diff = timestamp_ns - stream.last_timestamp_ns
                        if time_diff > expected_interval * 2:  # Significant gap
                            estimated_dropped = int(time_diff / expected_interval) - 1
                            stream.dropped_samples += max(0, estimated_dropped)

                processed_count += 1

            except Empty:
                break

    def _process_sync_queue(self) -> None:
        """Process queued sync events."""
        while True:
            try:
                sync_event = self.sync_queue.get_nowait()
                self.sync_events.append(sync_event)

                # Export sync event immediately if needed
                if self.export_enabled and self.hdf5_file:
                    self._export_sync_event(sync_event)

            except Empty:
                break

    def _update_statistics(self) -> None:
        """Update aggregation statistics."""
        current_time = time.time()

        # Update basic counts
        self.stats.total_devices = len(
            set(stream.device_id for stream in self.streams.values())
        )
        self.stats.active_streams = sum(
            1 for stream in self.streams.values() if stream.is_active
        )

        # Calculate data rate
        total_samples = sum(stream.total_samples for stream in self.streams.values())
        duration = current_time - self.start_time
        if duration > 0:
            # Rough estimate: assume 1KB per sample
            total_bytes = total_samples * 1024
            self.stats.data_rate_mbps = (total_bytes / duration) / (1024 * 1024)

        # Calculate sync quality (simplified metric)
        if self.sync_events:
            recent_syncs = [
                e for e in self.sync_events if (time.time_ns() - e.timestamp_ns) < 60e9
            ]  # Last minute
            sync_rate = len(recent_syncs) / 60.0  # Syncs per second
            self.stats.sync_quality_percent = min(100.0, sync_rate * 100)

            # Last sync timing
            if self.sync_events:
                last_sync_ns = self.sync_events[-1].timestamp_ns
                self.stats.last_sync_seconds_ago = (time.time_ns() - last_sync_ns) / 1e9

        # Buffer usage
        total_buffer_size = sum(
            len(stream.data_buffer) for stream in self.streams.values()
        )
        max_buffer_size = len(self.streams) * 100000  # Assuming max 100k per stream
        if max_buffer_size > 0:
            self.stats.buffer_usage_percent = (
                                                      total_buffer_size / max_buffer_size
                                              ) * 100

        # Dropped frames
        self.stats.dropped_frames_total = sum(
            stream.dropped_samples for stream in self.streams.values()
        )

        # Session duration
        self.stats.session_duration_seconds = duration

    def _initialize_hdf5_export(self) -> None:
        """Initialize HDF5 export file."""
        try:
            hdf5_path = self.session_directory / "session_data.h5"
            self.hdf5_file = h5py.File(hdf5_path, "w")

            # Create groups for different data types
            self.hdf5_file.create_group("streams")
            self.hdf5_file.create_group("sync_events")

            # Add session metadata
            self.hdf5_file.attrs["start_time"] = self.start_time
            self.hdf5_file.attrs["session_directory"] = str(self.session_directory)

            logger.info(f"HDF5 export initialized: {hdf5_path}")

        except Exception as e:
            logger.error(f"Failed to initialize HDF5 export: {e}")
            self.export_enabled = False

    def _create_hdf5_dataset(self, stream_id: str, stream: DataStream) -> None:
        """Create HDF5 dataset for a stream."""
        if not self.hdf5_file:
            return

        try:
            group = self.hdf5_file["streams"]

            # Create datasets based on stream type
            if stream.stream_type == "gsr":
                # GSR data: timestamp, raw_value, microsiemens, ppg
                dataset = group.create_dataset(
                    stream_id,
                    (0, 4),  # Start with 0 rows, 4 columns
                    maxshape=(None, 4),
                    dtype=np.float64,
                    chunks=True,
                    compression="gzip",
                )
                dataset.attrs["columns"] = [
                    "timestamp_ns",
                    "raw_gsr",
                    "gsr_microsiemens",
                    "raw_ppg",
                ]

            elif stream.stream_type in ["rgb_video", "thermal_video"]:
                # Video metadata: timestamp, frame_number, width, height
                dataset = group.create_dataset(
                    stream_id,
                    (0, 4),
                    maxshape=(None, 4),
                    dtype=np.int64,
                    chunks=True,
                    compression="gzip",
                )
                dataset.attrs["columns"] = [
                    "timestamp_ns",
                    "frame_number",
                    "width",
                    "height",
                ]

            dataset.attrs["device_id"] = stream.device_id
            dataset.attrs["stream_type"] = stream.stream_type
            dataset.attrs["sample_rate"] = stream.sample_rate

        except Exception as e:
            logger.error(f"Failed to create HDF5 dataset for {stream_id}: {e}")

    def _export_stream_data(self, stream_id: str) -> None:
        """Export stream data to HDF5."""
        if not self.hdf5_file or stream_id not in self.streams:
            return

        try:
            stream = self.streams[stream_id]
            if not stream.data_buffer:
                return

            group = self.hdf5_file["streams"]
            if stream_id not in group:
                return

            dataset = group[stream_id]
            data_list = list(stream.data_buffer)

            if stream.stream_type == "gsr":
                # Convert GSR data to structured format
                export_data = []
                for timestamp_ns, gsr_data in data_list:
                    if hasattr(gsr_data, "raw_gsr_value"):
                        row = [
                            timestamp_ns,
                            gsr_data.raw_gsr_value,
                            gsr_data.gsr_microsiemens,
                            gsr_data.raw_ppg_value,
                        ]
                        export_data.append(row)

                if export_data:
                    # Resize and append data
                    current_size = dataset.shape[0]
                    new_size = current_size + len(export_data)
                    dataset.resize((new_size, 4))
                    dataset[current_size:new_size] = export_data

            # Clear buffer after export
            stream.data_buffer.clear()

        except Exception as e:
            logger.error(f"Failed to export stream data for {stream_id}: {e}")

    def _export_sync_event(self, sync_event: SyncEvent) -> None:
        """Export sync event to HDF5."""
        if not self.hdf5_file:
            return

        try:
            group = self.hdf5_file["sync_events"]

            # Create or extend sync events dataset
            if "events" not in group:
                dataset = group.create_dataset(
                    "events",
                    (0, 3),  # timestamp, event_type, source_device
                    maxshape=(None, 3),
                    dtype=h5py.string_dtype(encoding="utf-8"),
                    chunks=True,
                )
                dataset.attrs["columns"] = [
                    "timestamp_ns",
                    "event_type",
                    "source_device",
                ]
            else:
                dataset = group["events"]

            # Add new event
            current_size = dataset.shape[0]
            dataset.resize((current_size + 1, 3))
            dataset[current_size] = [
                str(sync_event.timestamp_ns),
                sync_event.event_type,
                sync_event.source_device,
            ]

        except Exception as e:
            logger.error(f"Failed to export sync event: {e}")

    def _periodic_export(self) -> None:
        """Perform periodic data export."""
        current_time = time.time()

        # Export every 30 seconds
        if current_time - self.last_stats_update > 30:
            for stream_id in self.streams:
                self._export_stream_data(stream_id)

            # Flush HDF5 file
            if self.hdf5_file:
                self.hdf5_file.flush()

            self.last_stats_update = current_time

    def _finalize_exports(self) -> None:
        """Finalize all data exports."""
        try:
            # Export remaining data
            for stream_id in self.streams:
                self._export_stream_data(stream_id)

            # Close HDF5 file
            if self.hdf5_file:
                self.hdf5_file.close()
                self.hdf5_file = None

            # Create final CSV exports
            self.export_session_data()

            logger.info("Data export finalized")

        except Exception as e:
            logger.error(f"Error finalizing exports: {e}")


# Utility functions for data aggregation


def calculate_temporal_alignment(
        sync_events: List[SyncEvent], tolerance_ms: float = 5.0
) -> Dict[str, float]:
    """
    Calculate temporal alignment offsets for devices based on sync events.

    Args:
        sync_events: List of synchronization events
        tolerance_ms: Maximum acceptable offset in milliseconds

    Returns:
        Dictionary mapping device_id to offset in nanoseconds
    """
    device_offsets = {}

    if not sync_events:
        return device_offsets

    # Group sync events by type and timestamp
    flash_events = [e for e in sync_events if e.event_type == "flash"]

    if len(flash_events) < 2:
        return device_offsets

    # Use first flash event as reference
    reference_event = flash_events[0]
    reference_device = reference_event.source_device
    reference_timestamp = reference_event.timestamp_ns

    device_offsets[reference_device] = 0.0  # Reference device has zero offset

    # Calculate offsets for other devices
    for event in flash_events[1:]:
        if event.source_device != reference_device:
            offset_ns = event.timestamp_ns - reference_timestamp
            offset_ms = offset_ns / 1e6

            if abs(offset_ms) <= tolerance_ms:
                device_offsets[event.source_device] = offset_ns
            else:
                logger.warning(
                    f"Device {event.source_device} offset {offset_ms:.2f}ms exceeds tolerance"
                )

    return device_offsets


def validate_data_synchronization(
        streams: Dict[str, DataStream], tolerance_ms: float = 5.0
) -> Dict[str, Any]:
    """
    Validate synchronization quality across data streams.

    Args:
        streams: Dictionary of data streams
        tolerance_ms: Synchronization tolerance in milliseconds

    Returns:
        Synchronization quality report
    """
    report = {
        "total_streams": len(streams),
        "synchronized_streams": 0,
        "max_offset_ms": 0.0,
        "synchronization_rate": 0.0,
        "quality_issues": [],
    }

    if len(streams) < 2:
        return report

    # Get recent timestamps from each stream
    stream_timestamps = {}
    for stream_id, stream in streams.items():
        if stream.data_buffer and stream.is_active:
            recent_data = list(stream.data_buffer)[-10:]  # Last 10 samples
            if recent_data:
                stream_timestamps[stream_id] = [ts for ts, _ in recent_data]

    if len(stream_timestamps) < 2:
        return report

    # Calculate synchronization metrics
    all_timestamps = []
    for timestamps in stream_timestamps.values():
        all_timestamps.extend(timestamps)

    if all_timestamps:
        min_timestamp = min(all_timestamps)
        max_timestamp = max(all_timestamps)
        max_offset_ns = max_timestamp - min_timestamp
        max_offset_ms = max_offset_ns / 1e6

        report["max_offset_ms"] = max_offset_ms

        # Count streams within tolerance
        synchronized_count = 0
        for stream_id, timestamps in stream_timestamps.items():
            if timestamps:
                latest_timestamp = max(timestamps)
                offset_ms = abs(latest_timestamp - max_timestamp) / 1e6

                if offset_ms <= tolerance_ms:
                    synchronized_count += 1
                else:
                    report["quality_issues"].append(
                        f"Stream {stream_id} offset {offset_ms:.2f}ms exceeds tolerance"
                    )

        report["synchronized_streams"] = synchronized_count
        report["synchronization_rate"] = synchronized_count / len(stream_timestamps)

    return report
