import h5py
import json
import numpy as np
import pandas as pd
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from loguru import logger
from pathlib import Path
from typing import Dict, List, Optional, Tuple

@dataclass
class DataStreamInfo:
    stream_id: str
    device_id: str
    sensor_type: str
    sampling_rate_hz: float
    data_type: str
    units: str
    description: str
    start_timestamp: float
    end_timestamp: Optional[float] = None
    total_samples: int = 0
    quality_score: float = 100.0

@dataclass
class SyncMarkerInfo:
    timestamp: float
    marker_type: str
    description: str
    device_id: Optional[str] = None
    sequence_number: int = 0

@dataclass
class SessionMetadata:
    session_id: str
    participant_id: str
    start_time: datetime
    end_time: Optional[datetime] = None
    duration_seconds: float = 0.0

    experiment_name: str = ""
    condition: str = ""
    trial_number: int = 0
    researcher: str = ""
    notes: str = ""

    platform_version: str = "3.0.0"
    sync_accuracy_ms: float = 5.0
    total_devices: int = 0
    device_info: Dict[str, Dict] = None

    overall_quality_score: float = 100.0
    data_integrity_validated: bool = False

    def __post_init__(self):
        if self.device_info is None:
            self.device_info = {}

class MultiModalHDF5Exporter:

    def __init__(self, session_id: str, output_dir: Optional[Path] = None):

        self.session_id = session_id
        self.output_dir = Path(output_dir) if output_dir else Path("./data/exports/")
        self.output_dir.mkdir(parents=True, exist_ok=True)

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        self.output_file = self.output_dir / f"session_{session_id}_{timestamp}.h5"

        self.session_metadata: Optional[SessionMetadata] = None
        self.data_streams: Dict[str, DataStreamInfo] = {}
        self.sync_markers: List[SyncMarkerInfo] = []

        self.h5_file: Optional[h5py.File] = None
        self.is_open = False
        self.compression_level = 6

        self.streaming_datasets: Dict[str, h5py.Dataset] = {}
        self.streaming_positions: Dict[str, int] = {}

        logger.info(f"HDF5 Exporter initialized for session {session_id}")

    def __enter__(self):

        self.open()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):

        self.close()

    def open(self):

        try:
            self.h5_file = h5py.File(self.output_file, 'w')
            self.is_open = True

            self._create_file_structure()

            logger.info(f"HDF5 file opened: {self.output_file}")

        except Exception as e:
            logger.error(f"Failed to open HDF5 file: {e}")
            raise

    def close(self):

        if self.h5_file and self.is_open:
            try:

                self._finalize_metadata()

                self._validate_data_integrity()

                self.h5_file.close()
                self.is_open = False

                validation_report = self._generate_validation_report()

                logger.info(f"HDF5 export completed: {self.output_file}")
                logger.info(f"Data validation: {validation_report}")

                return self.output_file

            except Exception as e:
                logger.error(f"Error closing HDF5 file: {e}")
                raise

    def set_session_metadata(self, metadata: SessionMetadata):

        self.session_metadata = metadata

        if self.is_open:
            self._write_session_metadata()

    def register_data_stream(self, stream_info: DataStreamInfo):

        self.data_streams[stream_info.stream_id] = stream_info

        if self.is_open:
            self._create_dataset_for_stream(stream_info)

        logger.info(f"Data stream registered: {stream_info.stream_id} ({stream_info.sensor_type})")

    def add_timeseries_data(self, stream_id: str, timestamps: np.ndarray,
                            data: np.ndarray, metadata: Optional[Dict] = None):

        if not self.is_open:
            raise RuntimeError("HDF5 file not open")

        if stream_id not in self.data_streams:
            raise ValueError(f"Stream {stream_id} not registered")

        try:

            if stream_id in self.streaming_datasets:
                dataset = self.streaming_datasets[stream_id]
                timestamp_dataset = self.streaming_datasets[f"{stream_id}_timestamps"]

                current_pos = self.streaming_positions[stream_id]
                new_size = current_pos + len(data)

                if new_size > dataset.shape[0]:
                    dataset.resize((new_size,) + dataset.shape[1:])
                    timestamp_dataset.resize((new_size,))

                dataset[current_pos:new_size] = data
                timestamp_dataset[current_pos:new_size] = timestamps

                self.streaming_positions[stream_id] = new_size

            else:

                group = self.h5_file[f"data_streams/{stream_id}"]

                chunks = self._get_optimal_chunks(data.shape)

                dataset = group.create_dataset(
                    "data",
                    data=data,
                    chunks=chunks,
                    compression='gzip',
                    compression_opts=self.compression_level,
                    shuffle=True
                )

                timestamp_dataset = group.create_dataset(
                    "timestamps",
                    data=timestamps,
                    chunks=(chunks[0],),
                    compression='gzip',
                    compression_opts=self.compression_level
                )

                dataset.attrs['units'] = self.data_streams[stream_id].units
                dataset.attrs['sampling_rate_hz'] = self.data_streams[stream_id].sampling_rate_hz
                dataset.attrs['description'] = self.data_streams[stream_id].description

                if metadata:
                    for key, value in metadata.items():
                        dataset.attrs[key] = value

            self.data_streams[stream_id].total_samples += len(data)
            self.data_streams[stream_id].end_timestamp = float(timestamps[-1])

            logger.debug(f"Added {len(data)} samples to stream {stream_id}")

        except Exception as e:
            logger.error(f"Failed to add timeseries data to {stream_id}: {e}")
            raise

    def add_video_data(self, stream_id: str, timestamps: np.ndarray,
                       frames: np.ndarray, metadata: Optional[Dict] = None):

        if not self.is_open:
            raise RuntimeError("HDF5 file not open")

        try:
            group = self.h5_file[f"data_streams/{stream_id}"]

            chunks = (1, frames.shape[1], frames.shape[2], frames.shape[3])

            video_dataset = group.create_dataset(
                "frames",
                data=frames,
                chunks=chunks,
                compression='gzip',
                compression_opts=3,
                shuffle=True
            )

            timestamp_dataset = group.create_dataset(
                "timestamps",
                data=timestamps,
                compression='gzip',
                compression_opts=self.compression_level
            )

            video_dataset.attrs['frame_count'] = frames.shape[0]
            video_dataset.attrs['height'] = frames.shape[1]
            video_dataset.attrs['width'] = frames.shape[2]
            video_dataset.attrs['channels'] = frames.shape[3]
            video_dataset.attrs['format'] = metadata.get('format', 'RGB') if metadata else 'RGB'
            video_dataset.attrs['fps'] = self.data_streams[stream_id].sampling_rate_hz

            if metadata:
                for key, value in metadata.items():
                    video_dataset.attrs[key] = value

            self.data_streams[stream_id].total_samples = frames.shape[0]
            self.data_streams[stream_id].end_timestamp = float(timestamps[-1])

            logger.info(f"Added video data to stream {stream_id}: {frames.shape[0]} frames")

        except Exception as e:
            logger.error(f"Failed to add video data to {stream_id}: {e}")
            raise

    def add_sync_marker(self, marker: SyncMarkerInfo):

        self.sync_markers.append(marker)

        if self.is_open:
            self._write_sync_marker(marker)

        logger.debug(f"Sync marker added: {marker.marker_type} at {marker.timestamp}")

    def create_streaming_dataset(self, stream_id: str, data_shape: Tuple[int, ...],
                                 dtype: np.dtype = np.float64, initial_size: int = 1000):

        if not self.is_open:
            raise RuntimeError("HDF5 file not open")

        if stream_id not in self.data_streams:
            raise ValueError(f"Stream {stream_id} not registered")

        try:
            group = self.h5_file[f"data_streams/{stream_id}"]

            full_shape = (initial_size,) + data_shape
            max_shape = (None,) + data_shape
            chunks = self._get_optimal_chunks(full_shape)

            dataset = group.create_dataset(
                "data",
                shape=full_shape,
                maxshape=max_shape,
                dtype=dtype,
                chunks=chunks,
                compression='gzip',
                compression_opts=self.compression_level,
                shuffle=True
            )

            timestamp_dataset = group.create_dataset(
                "timestamps",
                shape=(initial_size,),
                maxshape=(None,),
                dtype=np.float64,
                chunks=(chunks[0],),
                compression='gzip',
                compression_opts=self.compression_level
            )

            self.streaming_datasets[stream_id] = dataset
            self.streaming_datasets[f"{stream_id}_timestamps"] = timestamp_dataset
            self.streaming_positions[stream_id] = 0

            logger.info(f"Streaming dataset created for {stream_id}")

        except Exception as e:
            logger.error(f"Failed to create streaming dataset for {stream_id}: {e}")
            raise

    def export_to_formats(self, formats: List[str] = None) -> Dict[str, Path]:

        if formats is None:
            formats = ['csv']

        if not self.is_open:

            temp_file = h5py.File(self.output_file, 'r')
        else:
            temp_file = self.h5_file

        exported_files = {}

        try:
            for format_type in formats:
                if format_type == 'csv':
                    exported_files['csv'] = self._export_to_csv(temp_file)
                elif format_type == 'mat':
                    exported_files['mat'] = self._export_to_matlab(temp_file)
                elif format_type == 'json':
                    exported_files['json'] = self._export_metadata_to_json(temp_file)

        finally:
            if not self.is_open:
                temp_file.close()

        return exported_files

    def _create_file_structure(self):

        self.h5_file.create_group("metadata")
        self.h5_file.create_group("data_streams")
        self.h5_file.create_group("sync_markers")
        self.h5_file.create_group("analysis")

        self.h5_file.attrs['created_at'] = datetime.now(timezone.utc).isoformat()
        self.h5_file.attrs['platform_version'] = "3.0.0"
        self.h5_file.attrs['file_format_version'] = "1.0"
        self.h5_file.attrs['session_id'] = self.session_id

    def _create_dataset_for_stream(self, stream_info: DataStreamInfo):

        group = self.h5_file.create_group(f"data_streams/{stream_info.stream_id}")

        group.attrs['device_id'] = stream_info.device_id
        group.attrs['sensor_type'] = stream_info.sensor_type
        group.attrs['sampling_rate_hz'] = stream_info.sampling_rate_hz
        group.attrs['data_type'] = stream_info.data_type
        group.attrs['units'] = stream_info.units
        group.attrs['description'] = stream_info.description
        group.attrs['start_timestamp'] = stream_info.start_timestamp

    def _write_session_metadata(self):

        if not self.session_metadata:
            return

        metadata_group = self.h5_file["metadata"]

        metadata_dict = asdict(self.session_metadata)

        if isinstance(metadata_dict['start_time'], datetime):
            metadata_dict['start_time'] = metadata_dict['start_time'].isoformat()
        if metadata_dict['end_time'] and isinstance(metadata_dict['end_time'], datetime):
            metadata_dict['end_time'] = metadata_dict['end_time'].isoformat()

        for key, value in metadata_dict.items():
            if value is not None:
                if isinstance(value, dict):

                    metadata_group.attrs[key] = json.dumps(value)
                else:
                    metadata_group.attrs[key] = value

    def _write_sync_marker(self, marker: SyncMarkerInfo):

        markers_group = self.h5_file["sync_markers"]

        if 'markers' not in markers_group:
            marker_dtype = np.dtype([
                ('timestamp', 'f8'),
                ('marker_type', 'S50'),
                ('description', 'S200'),
                ('device_id', 'S50'),
                ('sequence_number', 'i4')
            ])

            markers_group.create_dataset(
                'markers',
                shape=(0,),
                maxshape=(None,),
                dtype=marker_dtype,
                chunks=True,
                compression='gzip'
            )

        dataset = markers_group['markers']

        current_size = dataset.shape[0]
        dataset.resize((current_size + 1,))

        dataset[current_size] = (
            marker.timestamp,
            marker.marker_type.encode('utf-8'),
            marker.description.encode('utf-8'),
            (marker.device_id or '').encode('utf-8'),
            marker.sequence_number
        )

    def _finalize_metadata(self):

        if self.session_metadata:

            self.session_metadata.end_time = datetime.now(timezone.utc)
            if self.session_metadata.start_time:
                duration = self.session_metadata.end_time - self.session_metadata.start_time
                self.session_metadata.duration_seconds = duration.total_seconds()

            self.session_metadata.total_devices = len(self.data_streams)

            self._write_session_metadata()

        self._write_data_stream_summary()

    def _write_data_stream_summary(self):

        summary_group = self.h5_file.create_group("metadata/data_stream_summary")

        for stream_id, stream_info in self.data_streams.items():
            stream_group = summary_group.create_group(stream_id)

            for key, value in asdict(stream_info).items():
                if value is not None:
                    stream_group.attrs[key] = value

    def _validate_data_integrity(self):

        validation_results = {
            'total_streams': len(self.data_streams),
            'total_sync_markers': len(self.sync_markers),
            'data_integrity_checks': {},
            'temporal_alignment_check': True,
            'missing_data_check': True
        }

        for stream_id, stream_info in self.data_streams.items():
            if f"data_streams/{stream_id}" in self.h5_file:
                group = self.h5_file[f"data_streams/{stream_id}"]

                stream_validation = {
                    'data_present': 'data' in group,
                    'timestamps_present': 'timestamps' in group,
                    'sample_count': 0,
                    'temporal_consistency': True
                }

                if 'data' in group and 'timestamps' in group:
                    data_shape = group['data'].shape
                    timestamp_shape = group['timestamps'].shape

                    stream_validation['sample_count'] = data_shape[0]
                    stream_validation['temporal_consistency'] = data_shape[0] == timestamp_shape[0]

                    timestamps = group['timestamps'][:]
                    if len(timestamps) > 1:
                        stream_validation['temporal_ordering'] = np.all(np.diff(timestamps) >= 0)
                    else:
                        stream_validation['temporal_ordering'] = True

                validation_results['data_integrity_checks'][stream_id] = stream_validation

        validation_group = self.h5_file.create_group("metadata/validation")
        for key, value in validation_results.items():
            if isinstance(value, dict):
                validation_group.attrs[key] = json.dumps(value)
            else:
                validation_group.attrs[key] = value

        if self.session_metadata:
            all_checks_passed = all(
                check.get('temporal_consistency', False) and
                check.get('temporal_ordering', False)
                for check in validation_results['data_integrity_checks'].values()
            )
            self.session_metadata.data_integrity_validated = all_checks_passed

        return validation_results

    def _generate_validation_report(self) -> Dict:

        if not self.output_file.exists():
            return {"error": "Output file does not exist"}

        file_size_mb = self.output_file.stat().st_size / (1024 * 1024)

        return {
            'output_file': str(self.output_file),
            'file_size_mb': round(file_size_mb, 2),
            'total_streams': len(self.data_streams),
            'total_sync_markers': len(self.sync_markers),
            'export_completed': True,
            'data_integrity_validated': self.session_metadata.data_integrity_validated if self.session_metadata else False
        }

    def _get_optimal_chunks(self, shape: Tuple[int, ...]) -> Tuple[int, ...]:

        target_size = 1024 * 1024

        if len(shape) == 1:

            element_size = 8
            chunk_size = min(shape[0], target_size // element_size)
            return (max(1, chunk_size),)

        elif len(shape) == 2:

            element_size = 8 * shape[1]
            chunk_size = min(shape[0], target_size // element_size)
            return (max(1, chunk_size), shape[1])

        else:

            total_elements = np.prod(shape[1:])
            element_size = 8 * total_elements
            chunk_size = min(shape[0], max(1, target_size // element_size))
            return (chunk_size,) + shape[1:]

    def _export_to_csv(self, h5_file: h5py.File) -> Path:

        csv_file = self.output_file.with_suffix('.csv')

        all_data = []

        for stream_id in self.data_streams:
            if f"data_streams/{stream_id}" in h5_file:
                group = h5_file[f"data_streams/{stream_id}"]

                if 'data' in group and 'timestamps' in group:
                    timestamps = group['timestamps'][:]
                    data = group['data'][:]

                    if len(data.shape) == 1:
                        df = pd.DataFrame({
                            'timestamp': timestamps,
                            f'{stream_id}': data
                        })
                    else:

                        df_data = {'timestamp': timestamps}
                        for i in range(data.shape[1]):
                            df_data[f'{stream_id}_ch{i}'] = data[:, i]
                        df = pd.DataFrame(df_data)

                    all_data.append(df)

        if all_data:

            combined_df = all_data[0]
            for df in all_data[1:]:
                combined_df = pd.merge(combined_df, df, on='timestamp', how='outer')

            combined_df = combined_df.sort_values('timestamp')
            combined_df.to_csv(csv_file, index=False)

        return csv_file

    def _export_to_matlab(self, h5_file: h5py.File) -> Path:

        try:
            from scipy.io import savemat
        except ImportError:
            logger.warning("scipy not available, skipping MATLAB export")
            return None

        mat_file = self.output_file.with_suffix('.mat')

        matlab_data = {}

        for stream_id in self.data_streams:
            if f"data_streams/{stream_id}" in h5_file:
                group = h5_file[f"data_streams/{stream_id}"]

                if 'data' in group and 'timestamps' in group:
                    matlab_data[f"{stream_id}_data"] = group['data'][:]
                    matlab_data[f"{stream_id}_timestamps"] = group['timestamps'][:]

        if 'metadata' in h5_file:
            metadata_dict = {}
            for key in h5_file['metadata'].attrs:
                metadata_dict[key] = h5_file['metadata'].attrs[key]
            matlab_data['metadata'] = metadata_dict

        savemat(mat_file, matlab_data)
        return mat_file

    def _export_metadata_to_json(self, h5_file: h5py.File) -> Path:

        json_file = self.output_file.with_suffix('.json')

        export_metadata = {
            'session_metadata': {},
            'data_streams': {},
            'sync_markers': [],
            'file_info': {}
        }

        if 'metadata' in h5_file:
            for key in h5_file['metadata'].attrs:
                value = h5_file['metadata'].attrs[key]
                if isinstance(value, bytes):
                    value = value.decode('utf-8')
                export_metadata['session_metadata'][key] = value

        for stream_id in self.data_streams:
            if f"data_streams/{stream_id}" in h5_file:
                group = h5_file[f"data_streams/{stream_id}"]
                stream_meta = {}

                for key in group.attrs:
                    value = group.attrs[key]
                    if isinstance(value, bytes):
                        value = value.decode('utf-8')
                    stream_meta[key] = value

                export_metadata['data_streams'][stream_id] = stream_meta

        if 'sync_markers/markers' in h5_file:
            markers_dataset = h5_file['sync_markers/markers']
            for marker in markers_dataset:
                export_metadata['sync_markers'].append({
                    'timestamp': float(marker['timestamp']),
                    'marker_type': marker['marker_type'].decode('utf-8'),
                    'description': marker['description'].decode('utf-8'),
                    'device_id': marker['device_id'].decode('utf-8'),
                    'sequence_number': int(marker['sequence_number'])
                })

        export_metadata['file_info'] = {
            'hdf5_file': str(self.output_file),
            'created_at': datetime.now(timezone.utc).isoformat(),
            'total_streams': len(self.data_streams),
            'total_sync_markers': len(self.sync_markers)
        }

        with open(json_file, 'w') as f:
            json.dump(export_metadata, f, indent=2)

        return json_file

def create_session_exporter(participant_id: str, experiment_name: str = "",
                            output_dir: Optional[Path] = None) -> MultiModalHDF5Exporter:
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    session_id = f"{participant_id}_{timestamp}"

    exporter = MultiModalHDF5Exporter(session_id, output_dir)

    metadata = SessionMetadata(
        session_id=session_id,
        participant_id=participant_id,
        start_time=datetime.now(timezone.utc),
        experiment_name=experiment_name
    )

    exporter.set_session_metadata(metadata)

    return exporter
