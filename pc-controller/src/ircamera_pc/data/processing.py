#!/usr/bin/env python3


import asyncio
import json
import numpy as np
import time
from dataclasses import asdict, dataclass
from loguru import logger
from pathlib import Path
from typing import Any, Dict, List, Optional, Union

try:
    import pandas as pd
except ImportError:
    pd = None
    logger.warning(
        "pandas not available - some data processing features will be limited"
    )

try:
    import h5py
except ImportError:
    h5py = None
    logger.warning("h5py not available - HDF5 export will not be available")


@dataclass
class GSRDataPoint:
    

    timestamp: float
    gsr_value: float  
    raw_value: int  
    device_id: str
    session_id: str
    quality: str = "good"  


@dataclass
class ThermalDataPoint:
    

    timestamp: float
    temperature_data: List[List[float]]  
    min_temp: float
    max_temp: float
    avg_temp: float
    device_id: str
    session_id: str
    frame_number: int


@dataclass
class RGBDataPoint:
    

    timestamp: float
    image_path: str
    frame_number: int
    device_id: str
    session_id: str
    image_width: int
    image_height: int


class GSRIngestor:
    

    def __init__(self, session_manager=None):
        self.session_manager = session_manager
        self.active_sessions: Dict[str, Dict] = {}
        self.data_buffer: List[GSRDataPoint] = []
        self.buffer_size = 1000  
        self.processing_queue = asyncio.Queue()

        logger.info("GSRIngestor initialized")

    async def process_gsr_batch(
            self, device_id: str, session_id: str, gsr_data: List[Dict[str, Any]]
    ) -> bool:
        
        try:
            logger.debug(
                f"Processing GSR batch: {len(gsr_data)} points from {device_id}"
            )

            
            processed_points = []
            for data_point in gsr_data:
                
                raw_value = data_point.get("raw_value", 0)

                
                
                gsr_value = self._convert_raw_to_gsr(raw_value)

                
                point = GSRDataPoint(
                    timestamp=data_point.get("timestamp", time.time()),
                    gsr_value=gsr_value,
                    raw_value=raw_value,
                    device_id=device_id,
                    session_id=session_id,
                    quality=self._assess_signal_quality(raw_value),
                )

                processed_points.append(point)

            
            self.data_buffer.extend(processed_points)

            
            if len(self.data_buffer) > self.buffer_size:
                self.data_buffer = self.data_buffer[-self.buffer_size:]

            
            await self.processing_queue.put(
                {
                    "type": "gsr_batch",
                    "device_id": device_id,
                    "session_id": session_id,
                    "points": processed_points,
                }
            )

            logger.debug(f"Successfully processed {len(processed_points)} GSR points")
            return True

        except Exception as e:
            logger.error(f"Error processing GSR batch: {e}")
            return False

    def _convert_raw_to_gsr(self, raw_value: int) -> float:
        
        
        voltage = (raw_value / 4095.0) * 3.3

        
        
        if voltage == 0:
            return 0.0  

        resistance = (40200.0 * voltage) / (3.3 - voltage)

        
        if resistance <= 0:
            return 0.0

        gsr_microsiemens = 1000000.0 / resistance

        return max(0.0, min(gsr_microsiemens, 1000.0))  

    def _assess_signal_quality(self, raw_value: int) -> str:
        
        if raw_value < 100 or raw_value > 4000:
            return "poor"
        elif raw_value < 200 or raw_value > 3800:
            return "fair"
        else:
            return "good"

    def get_recent_data(self, session_id: str, seconds: int = 30) -> List[GSRDataPoint]:
        
        cutoff_time = time.time() - seconds
        return [
            point
            for point in self.data_buffer
            if point.session_id == session_id and point.timestamp >= cutoff_time
        ]


class DataProcessor:
    

    def __init__(self, output_dir: str = "data_output"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)

        self.gsr_ingestor = GSRIngestor()
        self.active_sessions: Dict[str, Dict] = {}
        self.data_streams: Dict[str, List] = {"gsr": [], "thermal": [], "rgb": []}

        logger.info(
            f"DataProcessor initialized with output directory: {self.output_dir}"
        )

    async def start_session(self, session_id: str, devices: List[str]) -> bool:
        
        try:
            self.active_sessions[session_id] = {
                "start_time": time.time(),
                "devices": devices,
                "data_points": {"gsr": [], "thermal": [], "rgb": []},
            }

            logger.info(
                f"Started data processing session {session_id} for devices: {devices}"
            )
            return True

        except Exception as e:
            logger.error(f"Error starting session {session_id}: {e}")
            return False

    async def process_gsr_data(
            self, device_id: str, session_id: str, data: List[Dict]
    ) -> bool:
        
        return await self.gsr_ingestor.process_gsr_batch(device_id, session_id, data)

    async def process_thermal_data(
            self, device_id: str, session_id: str, thermal_frame: Dict
    ) -> bool:
        
        try:
            point = ThermalDataPoint(
                timestamp=thermal_frame.get("timestamp", time.time()),
                temperature_data=thermal_frame.get("temperature_matrix", []),
                min_temp=thermal_frame.get("min_temp", 0.0),
                max_temp=thermal_frame.get("max_temp", 100.0),
                avg_temp=thermal_frame.get("avg_temp", 25.0),
                device_id=device_id,
                session_id=session_id,
                frame_number=thermal_frame.get("frame_number", 0),
            )

            if session_id in self.active_sessions:
                self.active_sessions[session_id]["data_points"]["thermal"].append(point)

            logger.debug(f"Processed thermal frame from {device_id}")
            return True

        except Exception as e:
            logger.error(f"Error processing thermal data: {e}")
            return False

    async def process_rgb_data(
            self, device_id: str, session_id: str, rgb_frame: Dict
    ) -> bool:
        
        try:
            point = RGBDataPoint(
                timestamp=rgb_frame.get("timestamp", time.time()),
                image_path=rgb_frame.get("image_path", ""),
                frame_number=rgb_frame.get("frame_number", 0),
                device_id=device_id,
                session_id=session_id,
                image_width=rgb_frame.get("width", 1920),
                image_height=rgb_frame.get("height", 1080),
            )

            if session_id in self.active_sessions:
                self.active_sessions[session_id]["data_points"]["rgb"].append(point)

            logger.debug(f"Processed RGB frame from {device_id}")
            return True

        except Exception as e:
            logger.error(f"Error processing RGB data: {e}")
            return False

    async def export_session_data(
            self, session_id: str, format: str = "json"
    ) -> Optional[str]:
        
        try:
            if session_id not in self.active_sessions:
                logger.error(f"Session {session_id} not found")
                return None

            session_data = self.active_sessions[session_id]
            timestamp = int(time.time())

            if format.lower() == "json":
                output_file = self.output_dir / f"session_{session_id}_{timestamp}.json"

                
                export_data = {
                    "session_info": {
                        "session_id": session_id,
                        "start_time": session_data["start_time"],
                        "devices": session_data["devices"],
                        "export_time": time.time(),
                    },
                    "gsr_data": [
                        asdict(point) for point in session_data["data_points"]["gsr"]
                    ],
                    "thermal_data": [
                        asdict(point)
                        for point in session_data["data_points"]["thermal"]
                    ],
                    "rgb_data": [
                        asdict(point) for point in session_data["data_points"]["rgb"]
                    ],
                }

                with open(output_file, "w") as f:
                    json.dump(export_data, f, indent=2, default=str)

                logger.info(f"Exported session {session_id} to {output_file}")
                return str(output_file)

            elif format.lower() == "hdf5" and h5py:
                output_file = self.output_dir / f"session_{session_id}_{timestamp}.h5"

                with h5py.File(output_file, "w") as f:
                    
                    f.attrs["session_id"] = session_id
                    f.attrs["start_time"] = session_data["start_time"]
                    f.attrs["export_time"] = time.time()

                    
                    if session_data["data_points"]["gsr"]:
                        gsr_group = f.create_group("gsr_data")
                        gsr_points = session_data["data_points"]["gsr"]

                        gsr_group.create_dataset(
                            "timestamps", data=[p.timestamp for p in gsr_points]
                        )
                        gsr_group.create_dataset(
                            "gsr_values", data=[p.gsr_value for p in gsr_points]
                        )
                        gsr_group.create_dataset(
                            "raw_values", data=[p.raw_value for p in gsr_points]
                        )

                    
                    if session_data["data_points"]["thermal"]:
                        thermal_group = f.create_group("thermal_data")
                        thermal_points = session_data["data_points"]["thermal"]

                        thermal_group.create_dataset(
                            "timestamps", data=[p.timestamp for p in thermal_points]
                        )
                        thermal_group.create_dataset(
                            "min_temps", data=[p.min_temp for p in thermal_points]
                        )
                        thermal_group.create_dataset(
                            "max_temps", data=[p.max_temp for p in thermal_points]
                        )
                        thermal_group.create_dataset(
                            "avg_temps", data=[p.avg_temp for p in thermal_points]
                        )

                logger.info(f"Exported session {session_id} to HDF5: {output_file}")
                return str(output_file)

            else:
                logger.error(f"Unsupported export format: {format}")
                return None

        except Exception as e:
            logger.error(f"Error exporting session data: {e}")
            return None

    def get_session_stats(self, session_id: str) -> Optional[Dict[str, Any]]:
        
        if session_id not in self.active_sessions:
            return None

        session_data = self.active_sessions[session_id]
        data_points = session_data["data_points"]

        return {
            "session_id": session_id,
            "duration": time.time() - session_data["start_time"],
            "device_count": len(session_data["devices"]),
            "data_counts": {
                "gsr": len(data_points["gsr"]),
                "thermal": len(data_points["thermal"]),
                "rgb": len(data_points["rgb"]),
            },
            "latest_gsr": data_points["gsr"][-1] if data_points["gsr"] else None,
            "data_rate": {
                "gsr": len(data_points["gsr"])
                       / max(1, time.time() - session_data["start_time"]),
                "thermal": len(data_points["thermal"])
                           / max(1, time.time() - session_data["start_time"]),
                "rgb": len(data_points["rgb"])
                       / max(1, time.time() - session_data["start_time"]),
            },
        }



__all__ = [
    "DataProcessor",
    "GSRIngestor",
    "GSRDataPoint",
    "ThermalDataPoint",
    "RGBDataPoint",
]
