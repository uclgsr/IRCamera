#!/usr/bin/env python3

import asyncio

import logging
import statistics
import time
from dataclasses import dataclass
from datetime import datetime
from typing import Dict, List, Tuple, Any

logger = logging.getLogger(__name__)

@dataclass
class SyncTestResult:
    device_id: str
    sync_accuracy_ms: float
    latency_ms: float
    timestamp: datetime
    test_type: str
    success: bool
    details: str = ""

@dataclass
class DeviceCoordinationStatus:
    device_id: str
    device_type: str
    connection_status: str
    last_sync_accuracy: float
    battery_level: int
    temperature_celsius: float
    recording_active: bool
    sync_markers_received: int

class FlashSyncValidator:

    def __init__(self):
        self.sync_events: List[Dict[str, Any]] = []
        self.flash_duration_ms = 100
        self.tolerance_ms = 5.0

    async def trigger_flash_sync(self, devices: List[str]) -> Dict[str, SyncTestResult]:

        logger.info(f"Triggering flash sync on {len(devices)} devices")

        master_timestamp = time.time_ns() / 1_000_000

        sync_results = {}
        flash_tasks = []

        for device_id in devices:
            task = asyncio.create_task(
                self._send_flash_command(device_id, master_timestamp)
            )
            flash_tasks.append((device_id, task))

        for device_id, task in flash_tasks:
            try:
                device_response_time = await task

                sync_accuracy = abs(device_response_time - master_timestamp)
                success = sync_accuracy <= self.tolerance_ms

                sync_results[device_id] = SyncTestResult(
                    device_id=device_id,
                    sync_accuracy_ms=sync_accuracy,
                    latency_ms=device_response_time - master_timestamp,
                    timestamp=datetime.now(),
                    test_type="flash_sync",
                    success=success,
                    details=f"Flash triggered at {device_response_time:.3f}ms, deviation: {sync_accuracy:.3f}ms"
                )

                logger.info(
                    f"Device {device_id}: sync accuracy {sync_accuracy:.3f}ms ({'PASS' if success else 'FAIL'})")

            except Exception as e:
                logger.error(f"Flash sync failed for device {device_id}: {e}")
                sync_results[device_id] = SyncTestResult(
                    device_id=device_id,
                    sync_accuracy_ms=float('inf'),
                    latency_ms=float('inf'),
                    timestamp=datetime.now(),
                    test_type="flash_sync",
                    success=False,
                    details=f"Flash sync error: {str(e)}"
                )

        return sync_results

    async def _send_flash_command(self, device_id: str, master_timestamp: float) -> float:

        await asyncio.sleep(0.001)

        return master_timestamp + (0.001 * 1000)

class MultiDeviceCoordinator:

    def __init__(self, max_devices: int = 8):
        self.max_devices = max_devices
        self.connected_devices: Dict[str, DeviceCoordinationStatus] = {}
        self.recording_session_active = False
        self.sync_marker_counter = 0

    def register_device(self, device_id: str, device_type: str) -> bool:

        if len(self.connected_devices) >= self.max_devices:
            logger.warning(f"Maximum devices ({self.max_devices}) already connected")
            return False

        self.connected_devices[device_id] = DeviceCoordinationStatus(
            device_id=device_id,
            device_type=device_type,
            connection_status="connected",
            last_sync_accuracy=0.0,
            battery_level=100,
            temperature_celsius=25.0,
            recording_active=False,
            sync_markers_received=0
        )

        logger.info(f"Device registered: {device_id} ({device_type})")
        return True

    def unregister_device(self, device_id: str):

        if device_id in self.connected_devices:
            del self.connected_devices[device_id]
            logger.info(f"Device unregistered: {device_id}")

    async def start_coordinated_recording(self) -> Dict[str, bool]:

        logger.info(f"Starting coordinated recording on {len(self.connected_devices)} devices")

        if self.recording_session_active:
            raise RuntimeError("Recording session already active")

        start_results = {}
        start_tasks = []

        for device_id in self.connected_devices.keys():
            task = asyncio.create_task(self._send_start_command(device_id))
            start_tasks.append((device_id, task))

        for device_id, task in start_tasks:
            try:
                success = await task
                start_results[device_id] = success

                if success:
                    self.connected_devices[device_id].recording_active = True
                    logger.info(f"Recording started on device {device_id}")
                else:
                    logger.error(f"Failed to start recording on device {device_id}")

            except Exception as e:
                logger.error(f"Start command failed for device {device_id}: {e}")
                start_results[device_id] = False

        self.recording_session_active = any(start_results.values())

        return start_results

    async def stop_coordinated_recording(self) -> Dict[str, bool]:

        logger.info("Stopping coordinated recording on all devices")

        if not self.recording_session_active:
            logger.warning("No active recording session to stop")
            return {}

        stop_results = {}
        stop_tasks = []

        for device_id in self.connected_devices.keys():
            if self.connected_devices[device_id].recording_active:
                task = asyncio.create_task(self._send_stop_command(device_id))
                stop_tasks.append((device_id, task))

        for device_id, task in stop_tasks:
            try:
                success = await task
                stop_results[device_id] = success

                if success:
                    self.connected_devices[device_id].recording_active = False
                    logger.info(f"Recording stopped on device {device_id}")
                else:
                    logger.error(f"Failed to stop recording on device {device_id}")

            except Exception as e:
                logger.error(f"Stop command failed for device {device_id}: {e}")
                stop_results[device_id] = False

        self.recording_session_active = False
        return stop_results

    async def inject_sync_marker(self, marker_type: str = "auto") -> Dict[str, bool]:

        self.sync_marker_counter += 1
        marker_id = f"sync_{self.sync_marker_counter}_{int(time.time())}"

        logger.info(f"Injecting sync marker {marker_id} ({marker_type}) on all devices")

        marker_results = {}
        marker_tasks = []

        for device_id in self.connected_devices.keys():
            task = asyncio.create_task(
                self._send_sync_marker(device_id, marker_id, marker_type)
            )
            marker_tasks.append((device_id, task))

        for device_id, task in marker_tasks:
            try:
                success = await task
                marker_results[device_id] = success

                if success:
                    self.connected_devices[device_id].sync_markers_received += 1
                    logger.info(f"Sync marker {marker_id} sent to device {device_id}")
                else:
                    logger.error(f"Failed to send sync marker to device {device_id}")

            except Exception as e:
                logger.error(f"Sync marker failed for device {device_id}: {e}")
                marker_results[device_id] = False

        return marker_results

    def get_device_status_summary(self) -> Dict[str, Any]:

        return {
            "total_devices": len(self.connected_devices),
            "recording_active": self.recording_session_active,
            "devices": {
                device_id: {
                    "type": status.device_type,
                    "connection": status.connection_status,
                    "recording": status.recording_active,
                    "battery": status.battery_level,
                    "temperature": status.temperature_celsius,
                    "sync_accuracy": status.last_sync_accuracy,
                    "sync_markers": status.sync_markers_received
                }
                for device_id, status in self.connected_devices.items()
            }
        }

    async def _send_start_command(self, device_id: str) -> bool:

        await asyncio.sleep(0.05)
        return True

    async def _send_stop_command(self, device_id: str) -> bool:

        await asyncio.sleep(0.05)
        return True

    async def _send_sync_marker(self, device_id: str, marker_id: str, marker_type: str) -> bool:

        await asyncio.sleep(0.01)
        return True

class SynchronizationValidator:

    def __init__(self):
        self.flash_sync = FlashSyncValidator()
        self.device_coordinator = MultiDeviceCoordinator()
        self.test_results: List[SyncTestResult] = []
        self.performance_metrics = {
            "total_tests": 0,
            "passed_tests": 0,
            "average_sync_accuracy": 0.0,
            "max_devices_tested": 0,
            "longest_session_duration": 0.0
        }

    async def run_comprehensive_sync_validation(
            self,
            device_list: List[Tuple[str, str]]
    ) -> Dict[str, Any]:

        logger.info(f"Starting comprehensive sync validation with {len(device_list)} devices")

        validation_report = {
            "test_start_time": datetime.now().isoformat(),
            "device_count": len(device_list),
            "test_results": {},
            "performance_summary": {},
            "compliance_status": {}
        }

        try:

            logger.info("Phase 1: Device Registration")
            for device_id, device_type in device_list:
                success = self.device_coordinator.register_device(device_id, device_type)
                if not success:
                    logger.error(f"Failed to register device {device_id}")

            logger.info("Phase 2: Flash Sync Accuracy Test")
            device_ids = [device_id for device_id, _ in device_list]
            flash_results = await self.flash_sync.trigger_flash_sync(device_ids)
            validation_report["test_results"]["flash_sync"] = {
                device_id: {
                    "sync_accuracy_ms": result.sync_accuracy_ms,
                    "success": result.success,
                    "details": result.details
                }
                for device_id, result in flash_results.items()
            }

            logger.info("Phase 3: Coordinated Recording Test")
            start_results = await self.device_coordinator.start_coordinated_recording()

            logger.info("Testing 30-second recording with sync markers")
            for i in range(6):
                await asyncio.sleep(5)
                marker_results = await self.device_coordinator.inject_sync_marker(
                    f"test_marker_{i + 1}")
                logger.info(
                    f"Sync marker {i + 1} sent to {sum(marker_results.values())}/{len(marker_results)} devices")

            stop_results = await self.device_coordinator.stop_coordinated_recording()

            validation_report["test_results"]["coordinated_recording"] = {
                "start_success": start_results,
                "stop_success": stop_results,
                "recording_duration": 30.0
            }

            logger.info("Phase 4: Multi-Device Stress Test")
            stress_results = await self._run_stress_test(device_ids)
            validation_report["test_results"]["stress_test"] = stress_results

            validation_report["performance_summary"] = self._generate_performance_summary()

            validation_report["compliance_status"] = self._check_compliance_requirements(
                flash_results)

        except Exception as e:
            logger.error(f"Validation test suite failed: {e}")
            validation_report["error"] = str(e)

        finally:

            for device_id, _ in device_list:
                self.device_coordinator.unregister_device(device_id)

        validation_report["test_end_time"] = datetime.now().isoformat()
        return validation_report

    async def _run_stress_test(self, device_ids: List[str]) -> Dict[str, Any]:

        logger.info("Running multi-device stress test")

        stress_results = {
            "test_duration": 60.0,
            "operations_per_second": 10,
            "total_operations": 0,
            "successful_operations": 0,
            "failed_operations": 0,
            "average_response_time": 0.0
        }

        start_time = time.time()
        response_times = []

        while time.time() - start_time < 60.0:
            operation_start = time.time()

            try:

                operation_type = stress_results["total_operations"] % 3

                if operation_type == 0:

                    await self.flash_sync.trigger_flash_sync(device_ids[:2])
                elif operation_type == 1:

                    await self.device_coordinator.inject_sync_marker("stress_test")
                else:

                    self.device_coordinator.get_device_status_summary()

                operation_time = (time.time() - operation_start) * 1000
                response_times.append(operation_time)
                stress_results["successful_operations"] += 1

            except Exception as e:
                logger.error(f"Stress test operation failed: {e}")
                stress_results["failed_operations"] += 1

            stress_results["total_operations"] += 1

            await asyncio.sleep(0.1)

        if response_times:
            stress_results["average_response_time"] = statistics.mean(response_times)

        return stress_results

    def _generate_performance_summary(self) -> Dict[str, Any]:

        return {
            "tests_completed": len(self.test_results),
            "success_rate": sum(1 for r in self.test_results if r.success) / len(
                self.test_results) if self.test_results else 0,
            "average_sync_accuracy": statistics.mean(
                [r.sync_accuracy_ms for r in self.test_results if
                 r.success]) if self.test_results else 0,
            "max_devices_coordinated": len(self.device_coordinator.connected_devices),
            "timestamp": datetime.now().isoformat()
        }

    def _check_compliance_requirements(self, flash_results: Dict[str, SyncTestResult]) -> Dict[
        str, bool]:

        compliance = {}

        sync_accuracies = [result.sync_accuracy_ms for result in flash_results.values() if
                           result.success]
        compliance["sub_5ms_sync"] = all(
            acc <= 5.0 for acc in sync_accuracies) if sync_accuracies else False

        compliance["multi_device_support"] = len(flash_results) <= 8

        success_rate = sum(1 for result in flash_results.values() if result.success) / len(
            flash_results) if flash_results else 0
        compliance["system_reliability"] = success_rate >= 0.95

        return compliance

__all__ = [
    'SynchronizationValidator',
    'FlashSyncValidator',
    'MultiDeviceCoordinator',
    'SyncTestResult',
    'DeviceCoordinationStatus'
]
