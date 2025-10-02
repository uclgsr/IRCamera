#!/usr/bin/env python3

import asyncio
import hashlib
import json
import time
from dataclasses import asdict, dataclass
from enum import Enum
from loguru import logger
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional

class TransferStatus(Enum):
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    PAUSED = "paused"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"

class FileType(Enum):
    THERMAL_VIDEO = "thermal_video"
    VISUAL_VIDEO = "visual_video"
    GSR_DATA = "gsr_data"
    IMU_DATA = "imu_data"
    AUDIO = "audio"
    METADATA = "metadata"
    CALIBRATION = "calibration"

@dataclass
class FileManifest:
    file_id: str
    filename: str
    file_type: FileType
    size_bytes: int
    checksum: str
    device_id: str
    session_id: str
    timestamp: float
    compression: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        data = asdict(self)
        data["file_type"] = self.file_type.value
        return data

@dataclass
class TransferJob:
    job_id: str
    manifest: FileManifest
    local_path: Path
    status: TransferStatus
    bytes_transferred: int
    start_time: float
    end_time: Optional[float]
    resume_offset: int
    retry_count: int
    error_message: Optional[str]
    device_connection: Optional[Any] = None

    @property
    def progress_percent(self) -> float:

        if self.manifest.size_bytes == 0:
            return 100.0
        return (self.bytes_transferred / self.manifest.size_bytes) * 100.0

    @property
    def transfer_rate(self) -> float:

        if self.status != TransferStatus.IN_PROGRESS or self.start_time == 0:
            return 0.0
        elapsed = time.time() - self.start_time
        if elapsed == 0:
            return 0.0
        return self.bytes_transferred / elapsed

    def to_dict(self) -> Dict[str, Any]:

        data = asdict(self)
        data["status"] = self.status.value
        data["manifest"] = self.manifest.to_dict()
        data["local_path"] = str(self.local_path)
        return data

class FileTransferManager:

    def __init__(self, config: Dict[str, Any]):

        self.config = config.get("file_transfer", {})
        self.data_dir = Path(self.config.get("data_dir", "data/transfers"))
        self.data_dir.mkdir(parents=True, exist_ok=True)

        self.chunk_size = self.config.get("chunk_size", 1024 * 1024)
        self.max_concurrent = self.config.get("max_concurrent_transfers", 4)
        self.retry_limit = self.config.get("retry_limit", 3)
        self.timeout = self.config.get("timeout_seconds", 300)
        self.verify_checksums = self.config.get("verify_checksums", True)

        self.active_jobs: Dict[str, TransferJob] = {}
        self.completed_jobs: Dict[str, TransferJob] = {}
        self.transfer_queue: List[str] = []
        self.concurrent_transfers = 0

        self.progress_callbacks: List[Callable[[str, float, float], None]] = []

        logger.info(
            f"File Transfer Manager initialized withdata directory: {self.data_dir}"
        )
        logger.info(
            f"Chunk size: {self.chunk_size} bytes, Maxconcurrent: {self.max_concurrent}"
        )

    def add_progress_callback(
            self, callback: None = Callable[[str, float, float], None]
    ) -> None:

        self.progress_callbacks.append(callback)

    async def queue_transfer(self, manifest: FileManifest, device_conn: Any) -> str:

        try:

            job_id = f"transfer_{manifest.device_id}_{manifest.session_id}_{int(time.time())}"

            session_dir = self.data_dir / manifest.session_id
            device_dir = session_dir / manifest.device_id
            device_dir.mkdir(parents=True, exist_ok=True)

            local_path = device_dir / manifest.filename

            if local_path.exists():
                if await self._verify_existing_file(local_path, manifest):
                    logger.info(f"File already exists and verified:{manifest.filename}")
                    return job_id

            job = TransferJob(
                job_id=job_id,
                manifest=manifest,
                local_path=local_path,
                status=TransferStatus.PENDING,
                bytes_transferred=0,
                start_time=0.0,
                end_time=None,
                resume_offset=0,
                retry_count=0,
                error_message=None,
                device_connection=device_conn,

            )

            if local_path.exists():
                job.resume_offset = local_path.stat().st_size
                job.bytes_transferred = job.resume_offset
                logger.info(
                    f"Found partial file, will resume fromoffset: {job.resume_offset}"
                )

            self.active_jobs[job_id] = job
            self.transfer_queue.append(job_id)

            logger.info(
                f"Queued transfer: {manifest.filename}({manifest.size_bytes} bytes)"
            )

            if self.concurrent_transfers < self.max_concurrent:
                await self._start_next_transfer()

            return job_id

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to queue transfer for{manifest.filename}: {e}")
            raise

    async def cancel_transfer(self, job_id: str) -> bool:

        try:
            if job_id in self.active_jobs:
                job = self.active_jobs[job_id]
                job.status = TransferStatus.CANCELLED
                job.end_time = time.time()

                if job_id in self.transfer_queue:
                    self.transfer_queue.remove(job_id)

                logger.info(f"Cancelled transfer: {job.manifest.filename}")
                return True

            return False

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to cancel transfer {job_id}: {e}")
            return False

    async def pause_transfer(self, job_id: str) -> bool:

        try:
            if job_id in self.active_jobs:
                job = self.active_jobs[job_id]
                if job.status == TransferStatus.IN_PROGRESS:
                    job.status = TransferStatus.PAUSED
                    logger.info(f"Paused transfer: {job.manifest.filename}")
                    return True
            return False
        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to pause transfer {job_id}: {e}")
            return False

    async def resume_transfer(self, job_id: str) -> bool:

        try:
            if job_id in self.active_jobs:
                job = self.active_jobs[job_id]
                if job.status == TransferStatus.PAUSED:
                    job.status = TransferStatus.PENDING
                    if job_id not in self.transfer_queue:
                        self.transfer_queue.append(job_id)

                    if self.concurrent_transfers < self.max_concurrent:
                        await self._start_next_transfer()

                    logger.info(f"Resumed transfer: {job.manifest.filename}")
                    return True
            return False
        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to resume transfer {job_id}: {e}")
            return False

    async def _start_next_transfer(self):

        if not self.transfer_queue or self.concurrent_transfers >= self.max_concurrent:
            return

        job_id = self.transfer_queue.pop(0)
        if job_id not in self.active_jobs:
            return

        job = self.active_jobs[job_id]
        if job.status != TransferStatus.PENDING:
            return

        self.concurrent_transfers += 1
        asyncio.create_task(self._execute_transfer(job))

    async def _execute_transfer(self, job: TransferJob):

        try:
            job.status = TransferStatus.IN_PROGRESS
            job.start_time = time.time()

            logger.info(f"Starting transfer: {job.manifest.filename}")

            await self._transfer_file_chunks(job)

            if self.verify_checksums:
                if not await self._verify_file_integrity(job):
                    raise Exception("File integrity verification failed")

            job.status = TransferStatus.COMPLETED
            job.end_time = time.time()
            job.bytes_transferred = job.manifest.size_bytes

            self.completed_jobs[job.job_id] = job
            del self.active_jobs[job.job_id]

            duration = job.end_time - job.start_time
            rate = job.manifest.size_bytes / duration if duration > 0 else 0

            logger.info(f"Transfer completed: {job.manifest.filename}")
            logger.info(
                f"Size: {job.manifest.size_bytes} bytes, "
                f"Duration: {duration:.2f}s, "
                f"Rate: {rate / 1024 / 1024:.2f} MB/s"
            )

        except (OSError, ValueError, RuntimeError) as e:
            job.status = TransferStatus.FAILED
            job.end_time = time.time()
            job.error_message = str(e)
            job.retry_count += 1

            logger.error(f"Transfer failed: {job.manifest.filename} - {e}")

            if job.retry_count <= self.retry_limit:
                logger.info(
                    f"Retrying transfer (attempt {job.retry_count}/{self.retry_limit})"
                )
                job.status = TransferStatus.PENDING
                job.error_message = None
                self.transfer_queue.append(job.job_id)
            else:
                logger.error(
                    f"Transfer permanently failed after{job.retry_count} attempts"
                )

        finally:
            self.concurrent_transfers -= 1

            if self.transfer_queue:
                await self._start_next_transfer()

    async def _transfer_file_chunks(self, job: TransferJob):

        try:

            mode = "ab" if job.resume_offset > 0 else "wb"

            with open(job.local_path, mode) as f:
                bytes_remaining = job.manifest.size_bytes - job.resume_offset
                bytes_transferred = job.resume_offset

                while bytes_remaining > 0:
                    if job.status != TransferStatus.IN_PROGRESS:
                        break

                    chunk_size = min(self.chunk_size, bytes_remaining)

                    chunk_data = await self._read_chunk_from_device(
                        job, bytes_transferred, chunk_size
                    )

                    f.write(chunk_data)
                    f.flush()

                    bytes_transferred += len(chunk_data)
                    bytes_remaining -= len(chunk_data)
                    job.bytes_transferred = bytes_transferred

                    await self._update_progress(job)

                    await asyncio.sleep(0.001)

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error during chunk transfer: {e}")
            raise

    async def _read_chunk_from_device(
            self, job: TransferJob, offset: int, size: int
    ) -> bytes:

        try:

            device_conn = job.device_connection

            if hasattr(device_conn, "read_file_chunk"):

                return await device_conn.read_file_chunk(
                    job.manifest.file_path, offset, size
                )
            elif hasattr(device_conn, "websocket"):

                request_data = {
                    "type": "file_chunk_request",
                    "job_id": job.job_id,
                    "file_path": job.manifest.file_path,
                    "offset": offset,
                    "size": size,
                    "session_id": job.manifest.session_id,
                }

                response = await device_conn.send_and_wait(request_data, timeout=30.0)

                if response and response.get("type") == "file_chunk_response":
                    if response.get("status") == "success":

                        chunk_data = response.get("chunk_data", "")
                        if isinstance(chunk_data, str):
                            import base64

                            return base64.b64decode(chunk_data)
                        return chunk_data
                    else:
                        raise Exception(
                            f"Chunk read failed: {response.get('error',
                                                               'Unknown error')}"
                        )
                else:
                    raise Exception("Invalid or timeout response from device")
            else:

                request_data = {
                    "type": "read_file_chunk",
                    "file_path": job.manifest.file_path,
                    "offset": offset,
                    "size": size,
                    "session_id": job.manifest.session_id,
                }

                response = await self._send_device_request(device_conn, request_data)

                if response and response.get("status") == "success":
                    chunk_data = response.get("data", b"")
                    if isinstance(chunk_data, str):
                        import base64

                        chunk_data = base64.b64decode(chunk_data)
                    return chunk_data
                else:
                    raise Exception(
                        f"Device read failed: {response.get('error', 'Unknown error')}"
                    )

        except Exception as e:
            logger.error(f"Failed to read chunk from device: {e}")
            raise

    async def _send_device_request(self, device_conn: Any, request_data: dict) -> dict:

        try:
            import json

            request_json = json.dumps(request_data)

            if hasattr(device_conn, "send_json"):
                return await device_conn.send_json(request_data)
            elif hasattr(device_conn, "writer"):

                device_conn.writer.write(request_json.encode("utf-8"))
                await device_conn.writer.drain()

                response_data = await device_conn.reader.read(65536)
                response_json = response_data.decode("utf-8")
                return json.loads(response_json)
            else:

                raise Exception("No valid device communication method available")

        except Exception as e:
            logger.error(f"Device request failed: {e}")
            return {"status": "error", "error": str(e)}

    async def _update_progress(self, job: TransferJob):

        progress = job.progress_percent
        rate = job.transfer_rate

        for callback in self.progress_callbacks:
            try:
                callback(job.job_id, progress, rate)
            except (OSError, ValueError, RuntimeError) as e:
                logger.error(f"Error in progress callback: {e}")

    async def _verify_file_integrity(self, job: TransferJob) -> bool:

        try:
            logger.info(f"Verifying file integrity: {job.manifest.filename}")

            hash_sha256 = hashlib.sha256()
            with open(job.local_path, "rb") as f:
                for chunk in iter(lambda: f.read(self.chunk_size), b""):
                    hash_sha256.update(chunk)

            local_checksum = hash_sha256.hexdigest()
            expected_checksum = job.manifest.checksum

            if local_checksum == expected_checksum:
                logger.info(f"File integrity verified:{job.manifest.filename}")
                return True
            else:
                logger.error(f"Checksum mismatch for {job.manifest.filename}")
                logger.error(f"Expected: {expected_checksum}")
                logger.error(f"Actual:   {local_checksum}")
                return False

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error verifying file integrity: {e}")
            return False

    async def _verify_existing_file(
            self, filepath: Path, manifest: FileManifest
    ) -> bool:

        try:
            if not filepath.exists():
                return False

            file_size = filepath.stat().st_size
            if file_size != manifest.size_bytes:
                return False

            if self.verify_checksums:
                hash_sha256 = hashlib.sha256()
                with open(filepath, "rb") as f:
                    for chunk in iter(lambda: f.read(self.chunk_size), b""):
                        hash_sha256.update(chunk)

                local_checksum = hash_sha256.hexdigest()
                return local_checksum == manifest.checksum

            return True

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Error verifying existing file: {e}")
            return False

    def get_transfer_status(self, job_id: str) -> Optional[Dict[str, Any]]:

        if job_id in self.active_jobs:
            job = self.active_jobs[job_id]
            return {
                "job_id": job_id,
                "filename": job.manifest.filename,
                "status": job.status.value,
                "progress_percent": job.progress_percent,
                "bytes_transferred": job.bytes_transferred,
                "total_bytes": job.manifest.size_bytes,
                "transfer_rate": job.transfer_rate,
                "retry_count": job.retry_count,
                "error_message": job.error_message,
            }
        elif job_id in self.completed_jobs:
            job = self.completed_jobs[job_id]
            return {
                "job_id": job_id,
                "filename": job.manifest.filename,
                "status": job.status.value,
                "progress_percent": 100.0,
                "bytes_transferred": job.bytes_transferred,
                "total_bytes": job.manifest.size_bytes,
                "duration": (job.end_time - job.start_time if job.end_time else 0),
                "error_message": job.error_message,
            }
        else:
            return None

    def get_active_transfers(self) -> List[Dict[str, Any]]:

        return [self.get_transfer_status(job_id) for job_id in self.active_jobs.keys()]

    def get_transfer_summary(self) -> Dict[str, Any]:

        return {
            "active_transfers": len(self.active_jobs),
            "queued_transfers": len(self.transfer_queue),
            "completed_transfers": len(self.completed_jobs),
            "concurrent_capacity": f"{self.concurrent_transfers}/{self.max_concurrent}",
            "data_directory": str(self.data_dir),
        }

    async def save_job_state(self) -> Any:

        try:
            state_file = self.data_dir / "transfer_state.json"

            state = {
                "active_jobs": {
                    job_id: job.to_dict() for job_id, job in self.active_jobs.items()
                },
                "completed_jobs": {
                    job_id: job.to_dict() for job_id, job in self.completed_jobs.items()
                },
                "transfer_queue": self.transfer_queue,
            }

            with open(state_file, "w") as f:
                json.dump(state, f, indent=2)

            logger.info("Transfer job state saved")

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to save transfer state: {e}")

    async def load_job_state(self) -> Any:

        try:
            state_file = self.data_dir / "transfer_state.json"

            if not state_file.exists():
                logger.info("No transfer state file found")
                return

            with open(state_file, "r") as f:
                state_data = json.load(f)

            reconstructed_jobs = 0
            for job_id, job_data in state_data.get("active_jobs", {}).items():
                try:

                    manifest_data = job_data.get("manifest", {})
                    manifest = FileManifest(
                        file_id=manifest_data.get("file_id", ""),
                        filename=manifest_data.get("filename", ""),
                        size_bytes=manifest_data.get("size_bytes", 0),
                        checksum=manifest_data.get("checksum", ""),
                        file_type=FileType(manifest_data.get("file_type", "metadata")),
                        device_id=manifest_data.get("device_id", ""),
                        timestamp=manifest_data.get("timestamp", 0.0),
                    )

                    job = TransferJob(
                        job_id=job_id,
                        manifest=manifest,
                        local_path=Path(job_data.get("local_path", "")),
                        status=TransferStatus(job_data.get("status", "pending")),
                        bytes_transferred=job_data.get("bytes_transferred", 0),
                        start_time=job_data.get("start_time", 0.0),
                        end_time=job_data.get("end_time"),
                        resume_offset=job_data.get("resume_offset", 0),
                        retry_count=job_data.get("retry_count", 0),
                        error_message=job_data.get("error_message"),
                    )

                    if job.status in [
                        TransferStatus.PENDING,
                        TransferStatus.IN_PROGRESS,
                        TransferStatus.PAUSED,
                    ]:

                        if job.local_path.exists():
                            actual_size = job.local_path.stat().st_size
                            job.bytes_transferred = actual_size
                            job.resume_offset = actual_size

                            if actual_size >= job.manifest.size_bytes:
                                job.status = TransferStatus.COMPLETED
                                logger.info(
                                    f"Restored completed transfer: {job.manifest.filename}"
                                )
                            else:
                                job.status = TransferStatus.PAUSED
                                self.transfer_queue.append(job_id)
                                logger.info(
                                    f"Restored paused transfer: {job.manifest.filename} "
                                    f"({actual_size}/{job.manifest.size_bytes} bytes)"
                                )
                        else:

                            job.bytes_transferred = 0
                            job.resume_offset = 0
                            job.status = TransferStatus.PENDING
                            self.transfer_queue.append(job_id)
                            logger.info(
                                f"Restored pending transfer: {job.manifest.filename}"
                            )

                        self.active_jobs[job_id] = job
                        reconstructed_jobs += 1

                except Exception as e:
                    logger.warning(f"Failed to restore transfer job {job_id}: {e}")
                    continue

            saved_queue = state_data.get("transfer_queue", [])
            for job_id in saved_queue:
                if job_id in self.active_jobs and job_id not in self.transfer_queue:
                    self.transfer_queue.append(job_id)

            logger.info(
                f"Transfer state loaded: {reconstructed_jobs} jobs reconstructed, "
                f"{len(self.transfer_queue)} queued for processing"
            )

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to load transfer state: {e}")
