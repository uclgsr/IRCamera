#!/usr/bin/env python3
"""
GSR Receiver for IRCamera PC Controller (Hub)

Handles real-time GSR data reception from Android Sensor Nodes (Spokes)
in the Multi-Modal Physiological Sensing Platform hub-and-spoke architecture.
"""

import asyncio
import json
import numpy as np
import pandas as pd
import sqlite3
import struct
import time
from collections import deque
from dataclasses import dataclass, field
from loguru import logger
from pathlib import Path
from typing import Any, Dict, List, Optional, Set

from .gsr_analytics import GSRAnalytics


@dataclass
class GSRSample:
    """Individual GSR sample from Android device"""

    timestamp: float
    gsr_value: float
    raw_value: int
    quality: int
    device_id: str
    received_time: float = field(default_factory=time.time)


@dataclass
class DeviceSession:
    """GSR session data for a specific Android device"""

    device_id: str
    session_id: str
    start_time: float
    samples: deque = field(default_factory=lambda: deque(maxlen=10000))
    sample_count: int = 0
    last_sample_time: float = 0
    quality_stats: Dict[str, float] = field(default_factory=dict)
    network_stats: Dict[str, int] = field(default_factory=dict)


class GSRReceiver:
    """
    GSR Data Receiver for PC Controller Hub

    Receives and processes real-time GSR data from multiple Android Sensor Nodes.
    Provides data aggregation, quality monitoring, and storage capabilities.
    """

    def __init__(self, config: Dict[str, Any]):
        """
        Initialize GSR Receiver

        Args:
            config: Configuration dictionary with GSR receiver settings
        """
        self.config = config.get("gsr_receiver", {})
        self.data_dir = Path(self.config.get("data_dir", "data/gsr"))
        self.data_dir.mkdir(parents=True, exist_ok=True)

        
        self.db_path = self.data_dir / "gsr_data.db"
        self.init_database()

        
        self.active_sessions: Dict[str, DeviceSession] = {}
        self.completed_sessions: Dict[str, DeviceSession] = {}

        
        self.sample_buffer = deque(maxlen=1000)
        self.quality_alerts: Set[str] = set()

        
        self.max_devices = self.config.get("max_devices", 10)
        self.buffer_flush_interval = self.config.get("buffer_flush_interval", 5.0)
        self.quality_threshold = self.config.get("quality_threshold", 50)

        
        self.time_offset_correction = self.config.get("time_offset_correction", True)
        self.sync_precision_threshold = self.config.get(
            "sync_precision_threshold", 1000000
        )  

        
        analytics_config = self.config.get("analytics", {})
        self.analytics = GSRAnalytics(
            window_size_seconds=analytics_config.get("window_size_seconds", 60),
            overlap_seconds=analytics_config.get("overlap_seconds", 30),
            sampling_rate=analytics_config.get("sampling_rate", 128.0),
        )

        
        self._running = False
        self._flush_task: Optional[asyncio.Task] = None
        self._quality_monitor_task: Optional[asyncio.Task] = None
        self._analytics_task: Optional[asyncio.Task] = None

        logger.info(
            f"GSR Receiver initialized with data directory: {self.data_dir} and advanced analytics"
        )

    def init_database(self) -> Any:
        """Initialize SQLite database for GSR data storage"""
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute(
                    """
                    CREATE TABLE IF NOT EXISTS gsr_samples (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        device_id TEXT NOT NULL,
                        session_id TEXT NOT NULL,
                        timestamp REAL NOT NULL,
                        gsr_value REAL NOT NULL,
                        raw_value INTEGER NOT NULL,
                        quality INTEGER NOT NULL,
                        received_time REAL NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """
                )

                conn.execute(
                    """
                    CREATE TABLE IF NOT EXISTS device_sessions (
                        device_id TEXT NOT NULL,
                        session_id TEXT NOT NULL,
                        start_time REAL NOT NULL,
                        end_time REAL,
                        sample_count INTEGER DEFAULT 0,
                        avg_quality REAL,
                        status TEXT DEFAULT 'active',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (device_id, session_id)
                    )
                """
                )

                
                conn.execute(
                    "CREATE INDEX IF NOT EXISTS idx_samples_device_session ON gsr_samples(device_id, "
                    "session_id)"
                )
                conn.execute(
                    "CREATE INDEX IF NOT EXISTS idx_samples_timestamp ON gsr_samples(timestamp)"
                )

                conn.commit()
                logger.info("GSR database initialized successfully")

        except Exception as e:
            logger.error(f"Failed to initialize GSR database: {e}")
            raise

    async def start(self) -> Any:
        """Start GSR receiver background tasks"""
        if self._running:
            logger.warning("GSR Receiver already running")
            return

        self._running = True

        
        self._flush_task = asyncio.create_task(self._periodic_flush())
        self._quality_monitor_task = asyncio.create_task(self._quality_monitor())

        logger.info("GSR Receiver started")

    async def stop(self) -> Any:
        """Stop GSR receiver and cleanup"""
        if not self._running:
            return

        self._running = False

        
        if self._flush_task:
            self._flush_task.cancel()
            try:
                await self._flush_task
            except asyncio.CancelledError:
                pass

        if self._quality_monitor_task:
            self._quality_monitor_task.cancel()
            try:
                await self._quality_monitor_task
            except asyncio.CancelledError:
                pass

        
        for session in list(self.active_sessions.values()):
            await self.end_session(session.device_id, session.session_id)

        
        await self._flush_to_database()

        logger.info("GSR Receiver stopped")

    async def register_device_session(self, device_id: str, session_id: str) -> bool:
        """
        Register a new GSR session from an Android device

        Args:
            device_id: Unique identifier for the Android device
            session_id: Session identifier

        Returns:
            True if session registered successfully
        """
        try:
            if len(self.active_sessions) >= self.max_devices:
                logger.error(f"Maximum device limit ({self.max_devices}) reached")
                return False

            session_key = f"{device_id}_{session_id}"

            if session_key in self.active_sessions:
                logger.warning(f"GSR session already exists: {session_key}")
                return False

            session = DeviceSession(
                device_id=device_id,
                session_id=session_id,
                start_time=time.time(),
                network_stats={
                    "packets_received": 0,
                    "bytes_received": 0,
                    "packet_loss": 0,
                    "last_heartbeat": time.time(),
                },
            )

            self.active_sessions[session_key] = session

            
            with sqlite3.connect(self.db_path) as conn:
                conn.execute(
                    """
                    INSERT INTO device_sessions 
                    (device_id, session_id, start_time, status)
                    VALUES (?, ?, ?, 'active')
                """,
                    (device_id, session_id, session.start_time),
                )
                conn.commit()

            logger.info(f"Registered GSR session: {session_key}")
            return True

        except Exception as e:
            logger.error(
                f"Failed to register GSR session {device_id}_{session_id}: {e}"
            )
            return False

    async def process_gsr_batch(
            self, device_id: str, session_id: str, samples_data: List[Dict]
    ) -> bool:
        """
        Process a batch of GSR samples from Android device

        Args:
            device_id: Device identifier
            session_id: Session identifier
            samples_data: List of sample dictionaries

        Returns:
            True if batch processed successfully
        """
        try:
            session_key = f"{device_id}_{session_id}"
            session = self.active_sessions.get(session_key)

            if not session:
                logger.warning(f"Received GSR batch for unknown session: {session_key}")
                return False

            
            processed_samples = []
            for sample_data in samples_data:
                sample = GSRSample(
                    timestamp=sample_data["timestamp"],
                    gsr_value=sample_data["gsr_value"],
                    raw_value=sample_data["raw_value"],
                    quality=sample_data["quality"],
                    device_id=device_id,
                )

                
                if self._validate_sample(sample):
                    processed_samples.append(sample)
                    session.samples.append(sample)
                    self.sample_buffer.append(sample)

            
            session.sample_count += len(processed_samples)
            session.last_sample_time = time.time()
            session.network_stats["packets_received"] += 1
            session.network_stats["bytes_received"] += len(str(samples_data))

            
            if processed_samples:
                qualities = [s.quality for s in processed_samples]
                session.quality_stats.update(
                    {
                        "min_quality": min(qualities),
                        "max_quality": max(qualities),
                        "avg_quality": sum(qualities) / len(qualities),
                        "quality_samples": len(qualities),
                    }
                )

                
                gsr_values = [s.gsr_value for s in processed_samples]
                timestamps = [s.timestamp for s in processed_samples]
                self.analytics.add_gsr_samples(
                    device_id, session_id, gsr_values, timestamps
                )

            logger.debug(
                f"Processed GSR batch: {len(processed_samples)} samples from {session_key}"
            )
            return True

        except Exception as e:
            logger.error(f"Failed to process GSR batch from {device_id}: {e}")
            return False

    async def handle_heartbeat(
            self, device_id: str, session_id: str, heartbeat_data: Dict
    ) -> bool:
        """
        Handle heartbeat message from Android device

        Args:
            device_id: Device identifier
            session_id: Session identifier
            heartbeat_data: Heartbeat data dictionary

        Returns:
            True if heartbeat processed successfully
        """
        try:
            session_key = f"{device_id}_{session_id}"
            session = self.active_sessions.get(session_key)

            if not session:
                logger.warning(f"Received heartbeat for unknown session: {session_key}")
                return False

            
            session.network_stats["last_heartbeat"] = time.time()

            
            buffer_size = heartbeat_data.get("buffer_size", 0)
            if buffer_size > 500:  
                logger.warning(
                    f"High buffer size ({buffer_size}) detected for {session_key}"
                )

            return True

        except Exception as e:
            logger.error(f"Failed to handle heartbeat from {device_id}: {e}")
            return False

    async def handle_quality_metrics(
            self, device_id: str, session_id: str, metrics_data: Dict
    ) -> bool:
        """
        Handle quality metrics from Android device

        Args:
            device_id: Device identifier
            session_id: Session identifier
            metrics_data: Quality metrics data

        Returns:
            True if metrics processed successfully
        """
        try:
            session_key = f"{device_id}_{session_id}"
            session = self.active_sessions.get(session_key)

            if not session:
                logger.warning(
                    f"Received quality metrics for unknown session: {session_key}"
                )
                return False

            
            session.network_stats.update(
                {
                    "samples_sent": metrics_data.get("samples_sent", 0),
                    "bytes_transmitted": metrics_data.get("bytes_transmitted", 0),
                    "network_errors": metrics_data.get("network_errors", 0),
                    "uptime_ms": metrics_data.get("uptime_ms", 0),
                }
            )

            
            error_count = metrics_data.get("network_errors", 0)
            if error_count > 10:
                self.quality_alerts.add(
                    f"High network errors for {session_key}: {error_count}"
                )

            return True

        except Exception as e:
            logger.error(f"Failed to handle quality metrics from {device_id}: {e}")
            return False

    async def end_session(self, device_id: str, session_id: str) -> bool:
        """
        End GSR session and finalize data

        Args:
            device_id: Device identifier
            session_id: Session identifier

        Returns:
            True if session ended successfully
        """
        try:
            session_key = f"{device_id}_{session_id}"
            session = self.active_sessions.get(session_key)

            if not session:
                logger.warning(f"Cannot end unknown session: {session_key}")
                return False

            
            await self._flush_session_to_database(session)

            
            end_time = time.time()
            avg_quality = session.quality_stats.get("avg_quality", 0)

            with sqlite3.connect(self.db_path) as conn:
                conn.execute(
                    """
                    UPDATE device_sessions 
                    SET end_time = ?, sample_count = ?, avg_quality = ?,
                        status = 'completed'
                    WHERE device_id = ? AND session_id = ?
                """,
                    (
                        end_time,
                        session.sample_count,
                        avg_quality,
                        device_id,
                        session_id,
                    ),
                )
                conn.commit()

            
            analysis_report = self.analytics.generate_session_report(
                device_id, session_id
            )
            if analysis_report:
                
                report_filename = (
                    f"gsr_analysis_{device_id}_{session_id}_{int(end_time)}.json"
                )
                report_path = self.data_dir / "analytics" / report_filename
                report_path.parent.mkdir(exist_ok=True)

                
                report_dict = {
                    "session_id": analysis_report.session_id,
                    "device_id": analysis_report.device_id,
                    "start_time": analysis_report.start_time.isoformat(),
                    "end_time": analysis_report.end_time.isoformat(),
                    "duration_minutes": analysis_report.duration_minutes,
                    "total_samples": analysis_report.total_samples,
                    "sampling_rate": analysis_report.sampling_rate,
                    "data_quality": analysis_report.data_quality,
                    "average_stress_score": analysis_report.average_stress_score,
                    "peak_stress_score": analysis_report.peak_stress_score,
                    "stress_distribution": analysis_report.stress_distribution,
                    "stress_trend": analysis_report.stress_trend,
                    "trend_confidence": analysis_report.trend_confidence,
                    "recommendations": analysis_report.recommendations,
                    "feature_count": len(analysis_report.features),
                }

                with open(report_path, "w") as f:
                    json.dump(report_dict, f, indent=2)

                
                features_filename = (
                    f"gsr_features_{device_id}_{session_id}_{int(end_time)}.csv"
                )
                features_path = self.data_dir / "analytics" / features_filename
                self.analytics.export_features_csv(
                    device_id, session_id, str(features_path)
                )

                logger.info(
                    f"Generated analytics report for session {session_key}: "
                    f"avg_stress={analysis_report.average_stress_score:.1f}, "
                    f"trend={analysis_report.stress_trend}"
                )

            
            self.analytics.cleanup_device_session(device_id, session_id)

            
            self.completed_sessions[session_key] = session
            del self.active_sessions[session_key]

            logger.info(
                f"Ended GSR session {session_key}: {session.sample_count} samples"
            )
            return True

        except Exception as e:
            logger.error(f"Failed to end GSR session {device_id}_{session_id}: {e}")
            return False

    def _validate_sample(self, sample: GSRSample) -> bool:
        """Validate GSR sample data"""
        try:
            
            current_time = time.time()
            if (
                    abs(sample.timestamp - current_time) > 3600
            ):  
                logger.warning(f"Sample timestamp seems invalid: {sample.timestamp}")
                return False

            
            if not (0.01 <= sample.gsr_value <= 1000):
                logger.warning(f"GSR value out of range: {sample.gsr_value}")
                return False

            
            if sample.quality < self.quality_threshold:
                logger.debug(f"Low quality sample: {sample.quality}")
                

            return True

        except Exception as e:
            logger.error(f"Error validating GSR sample: {e}")
            return False

    async def _periodic_flush(self):
        """Periodically flush buffered data to database"""
        while self._running:
            try:
                await asyncio.sleep(self.buffer_flush_interval)
                await self._flush_to_database()

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in periodic flush: {e}")

    async def _flush_to_database(self):
        """Flush all buffered samples to database"""
        if not self.sample_buffer:
            return

        try:
            samples_to_flush = list(self.sample_buffer)
            self.sample_buffer.clear()

            with sqlite3.connect(self.db_path) as conn:
                conn.executemany(
                    """
                    INSERT INTO gsr_samples 
                    (device_id, session_id, timestamp, gsr_value, raw_value, quality,
                        received_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                    [
                        (
                            sample.device_id,
                            "unknown",  
                            sample.timestamp,
                            sample.gsr_value,
                            sample.raw_value,
                            sample.quality,
                            sample.received_time,
                        )
                        for sample in samples_to_flush
                    ],
                )
                conn.commit()

            logger.debug(f"Flushed {len(samples_to_flush)} GSR samples to database")

        except Exception as e:
            logger.error(f"Failed to flush samples to database: {e}")

    async def _flush_session_to_database(self, session: DeviceSession):
        """Flush specific session samples to database"""
        if not session.samples:
            return

        try:
            samples_to_flush = list(session.samples)

            with sqlite3.connect(self.db_path) as conn:
                conn.executemany(
                    """
                    INSERT INTO gsr_samples 
                    (device_id, session_id, timestamp, gsr_value, raw_value, quality,
                        received_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                    [
                        (
                            session.device_id,
                            session.session_id,
                            sample.timestamp,
                            sample.gsr_value,
                            sample.raw_value,
                            sample.quality,
                            sample.received_time,
                        )
                        for sample in samples_to_flush
                    ],
                )
                conn.commit()

            logger.debug(
                f"Flushed {len(samples_to_flush)} samples for session {session.device_id}_{session.session_id}"
            )

        except Exception as e:
            logger.error(f"Failed to flush session samples to database: {e}")

    async def _quality_monitor(self):
        """Monitor data quality and generate alerts"""
        while self._running:
            try:
                await asyncio.sleep(10.0)  

                current_time = time.time()

                
                for session_key, session in self.active_sessions.items():
                    last_heartbeat = session.network_stats.get("last_heartbeat", 0)
                    if (
                            current_time - last_heartbeat > 30
                    ):  
                        self.quality_alerts.add(
                            f"Stale session detected: {session_key}"
                        )

                
                if self.quality_alerts:
                    for alert in self.quality_alerts:
                        logger.warning(f"Quality Alert: {alert}")
                    self.quality_alerts.clear()

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in quality monitor: {e}")

    def get_session_stats(
            self, device_id: str, session_id: str
    ) -> Optional[Dict[str, Any]]:
        """Get statistics for a specific session"""
        session_key = f"{device_id}_{session_id}"
        session = self.active_sessions.get(session_key)

        if not session:
            return None

        return {
            "device_id": session.device_id,
            "session_id": session.session_id,
            "start_time": session.start_time,
            "sample_count": session.sample_count,
            "last_sample_time": session.last_sample_time,
            "quality_stats": session.quality_stats,
            "network_stats": session.network_stats,
            "buffer_size": len(session.samples),
        }

    def get_all_session_stats(self) -> Dict[str, Dict[str, Any]]:
        """Get statistics for all active sessions"""
        return {
            session_key: self.get_session_stats(session.device_id, session.session_id)
            for session_key, session in self.active_sessions.items()
        }

    def get_real_time_analytics(
            self, device_id: str, session_id: str
    ) -> Optional[Dict[str, Any]]:
        """
        Get real-time analytics features for a session

        Args:
            device_id: Device identifier
            session_id: Session identifier

        Returns:
            Dictionary with latest analytics features or None
        """
        features = self.analytics.get_real_time_features(device_id, session_id)
        if not features:
            return None

        return {
            "timestamp": features.timestamp,
            "stress_score": features.stress_score,
            "stress_level": features.stress_level.value,
            "confidence": features.confidence,
            "mean_gsr": features.mean_gsr,
            "std_gsr": features.std_gsr,
            "peak_frequency": features.peak_frequency,
            "rising_time": features.rising_time,
            "rapid_changes": features.rapid_changes,
            "trend_slope": features.slope,
            "trend_significance": features.slope_significance,
        }

    def get_stress_summary(self) -> Dict[str, Any]:
        """Get stress level summary for all active sessions"""
        return self.analytics.get_stress_summary()

    def get_analytics_alerts(self) -> List[Dict[str, Any]]:
        """Get current analytics-based alerts"""
        alerts = []

        for session_key, session in self.active_sessions.items():
            features = self.analytics.get_real_time_features(
                session.device_id, session.session_id
            )
            if features:
                
                if features.stress_score > 80:
                    alerts.append(
                        {
                            "type": "high_stress",
                            "device_id": session.device_id,
                            "session_id": session.session_id,
                            "stress_score": features.stress_score,
                            "message": f"High stress detected: {features.stress_score:.1f}/100",
                            "timestamp": features.timestamp,
                        }
                    )

                
                if features.confidence < 50:
                    alerts.append(
                        {
                            "type": "low_confidence",
                            "device_id": session.device_id,
                            "session_id": session.session_id,
                            "confidence": features.confidence,
                            "message": f"Low analysis confidence: {features.confidence:.1f}%",
                            "timestamp": features.timestamp,
                        }
                    )

                
                if features.rapid_changes > 20:
                    alerts.append(
                        {
                            "type": "unstable_signal",
                            "device_id": session.device_id,
                            "session_id": session.session_id,
                            "rapid_changes": features.rapid_changes,
                            "message": f"Unstable GSR signal: {features.rapid_changes} rapid changes",
                            "timestamp": features.timestamp,
                        }
                    )

        return alerts

    async def export_session_data(
            self, device_id: str, session_id: str, format: str = "csv"
    ) -> Optional[Path]:
        """
        Export session data to file

        Args:
            device_id: Device identifier
            session_id: Session identifier
            format: Export format ('csv', 'json', 'hdf5')

        Returns:
            Path to exported file or None if failed
        """
        try:
            
            with sqlite3.connect(self.db_path) as conn:
                df = pd.read_sql_query(
                    """
                    SELECT * FROM gsr_samples 
                    WHERE device_id = ? AND session_id = ?
                    ORDER BY timestamp
                """,
                    conn,
                    params=(device_id, session_id),
                )

            if df.empty:
                logger.warning(f"No data found for session {device_id}_{session_id}")
                return None

            
            timestamp = int(time.time())
            filename = f"gsr_{device_id}_{session_id}_{timestamp}.{format}"
            export_path = self.data_dir / filename

            if format == "csv":
                df.to_csv(export_path, index=False)
            elif format == "json":
                df.to_json(export_path, orient="records", indent=2)
            elif format == "hdf5":
                df.to_hdf(export_path, key="gsr_data", mode="w")
            else:
                logger.error(f"Unsupported export format: {format}")
                return None

            logger.info(f"Exported GSR data to {export_path}")
            return export_path

        except Exception as e:
            logger.error(f"Failed to export session data: {e}")
            return None
