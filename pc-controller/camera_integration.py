#!/usr/bin/env python3
"""
Enhanced Camera Integration for IRCamera PC Controller

Provides webcam integration, thermal/RGB preview, and multi-modal data export.
Supports both native OpenCV and fallback implementations.
"""

import cv2
import numpy as np
import base64
import json
import time
import threading
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, List, Callable, Tuple
import io

try:
    from PIL import Image

    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False

import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class CameraManager:
    """Manages webcam capture and multi-modal camera data processing"""

    def __init__(self, preview_callback: Optional[Callable] = None):
        self.preview_callback = preview_callback
        self.camera = None
        self.camera_thread = None
        self.camera_running = False

        # Camera settings
        self.camera_index = 0
        self.resolution = (640, 480)
        self.fps = 30

        # Frame storage
        self.latest_frame = None
        self.frame_count = 0
        self.start_time = time.time()

        # Multi-modal frame storage
        self.thermal_frames = {}  # {device_id: latest_thermal_frame}
        self.rgb_frames = {}  # {device_id: latest_rgb_frame}

        logger.info(" Camera manager initialized")

    def start_webcam_capture(self) -> bool:
        """Start local webcam capture"""
        try:
            # Try to initialize camera
            self.camera = cv2.VideoCapture(self.camera_index)

            if not self.camera.isOpened():
                logger.error(f" Cannot open camera {self.camera_index}")
                return False

            # Set camera properties
            self.camera.set(cv2.CAP_PROP_FRAME_WIDTH, self.resolution[0])
            self.camera.set(cv2.CAP_PROP_FRAME_HEIGHT, self.resolution[1])
            self.camera.set(cv2.CAP_PROP_FPS, self.fps)

            # Start capture thread
            self.camera_running = True
            self.camera_thread = threading.Thread(target=self._camera_capture_loop, daemon=True)
            self.camera_thread.start()

            actual_width = int(self.camera.get(cv2.CAP_PROP_FRAME_WIDTH))
            actual_height = int(self.camera.get(cv2.CAP_PROP_FRAME_HEIGHT))
            actual_fps = self.camera.get(cv2.CAP_PROP_FPS)

            logger.info(f" Webcam started: {actual_width}x{actual_height} @ {actual_fps:.1f} FPS")
            return True

        except Exception as e:
            logger.error(f" Failed to start webcam: {e}")
            return False

    def stop_webcam_capture(self):
        """Stop local webcam capture"""
        self.camera_running = False

        if self.camera_thread and self.camera_thread.is_alive():
            self.camera_thread.join(timeout=2.0)

        if self.camera:
            self.camera.release()
            self.camera = None

        logger.info(" Webcam capture stopped")

    def _camera_capture_loop(self):
        """Camera capture loop running in background thread"""
        logger.info(" Camera capture loop started")

        while self.camera_running and self.camera:
            try:
                ret, frame = self.camera.read()

                if not ret or frame is None:
                    logger.warning(" Failed to capture frame from webcam")
                    time.sleep(0.1)
                    continue

                # Store latest frame
                self.latest_frame = frame.copy()
                self.frame_count += 1

                # Call preview callback if provided
                if self.preview_callback:
                    self.preview_callback('local_webcam', frame, 'rgb')

                # Control frame rate (additional throttling)
                time.sleep(1.0 / self.fps)

            except Exception as e:
                logger.error(f"Camera capture error: {e}")
                time.sleep(1.0)

    def process_thermal_frame(self, device_id: str, frame_data: bytes, format: str = 'jpeg') -> bool:
        """Process thermal camera frame from Android device"""
        try:
            # Decode frame data
            if format.lower() == 'jpeg':
                # Decode JPEG data
                np_arr = np.frombuffer(frame_data, np.uint8)
                frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
            elif format.lower() == 'base64':
                # Decode base64 encoded data
                decoded_data = base64.b64decode(frame_data)
                np_arr = np.frombuffer(decoded_data, np.uint8)
                frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
            else:
                logger.error(f" Unsupported thermal frame format: {format}")
                return False

            if frame is None:
                logger.error(f" Failed to decode thermal frame from {device_id}")
                return False

            # Store thermal frame
            self.thermal_frames[device_id] = {
                'frame': frame,
                'timestamp': datetime.now(),
                'device_id': device_id,
                'format': format
            }

            # Call preview callback
            if self.preview_callback:
                self.preview_callback(device_id, frame, 'thermal')

            logger.debug(f" Thermal frame processed from {device_id}: {frame.shape}")
            return True

        except Exception as e:
            logger.error(f" Error processing thermal frame from {device_id}: {e}")
            return False

    def process_rgb_frame(self, device_id: str, frame_data: bytes, format: str = 'jpeg') -> bool:
        """Process RGB camera frame from Android device"""
        try:
            # Decode frame data (similar to thermal)
            if format.lower() == 'jpeg':
                np_arr = np.frombuffer(frame_data, np.uint8)
                frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
            elif format.lower() == 'base64':
                decoded_data = base64.b64decode(frame_data)
                np_arr = np.frombuffer(decoded_data, np.uint8)
                frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
            else:
                logger.error(f" Unsupported RGB frame format: {format}")
                return False

            if frame is None:
                logger.error(f" Failed to decode RGB frame from {device_id}")
                return False

            # Store RGB frame
            self.rgb_frames[device_id] = {
                'frame': frame,
                'timestamp': datetime.now(),
                'device_id': device_id,
                'format': format
            }

            # Call preview callback
            if self.preview_callback:
                self.preview_callback(device_id, frame, 'rgb')

            logger.debug(f" RGB frame processed from {device_id}: {frame.shape}")
            return True

        except Exception as e:
            logger.error(f" Error processing RGB frame from {device_id}: {e}")
            return False

    def get_latest_webcam_frame(self) -> Optional[np.ndarray]:
        """Get latest webcam frame"""
        return self.latest_frame.copy() if self.latest_frame is not None else None

    def get_latest_thermal_frame(self, device_id: str) -> Optional[Dict]:
        """Get latest thermal frame from specific device"""
        return self.thermal_frames.get(device_id)

    def get_latest_rgb_frame(self, device_id: str) -> Optional[Dict]:
        """Get latest RGB frame from specific device"""
        return self.rgb_frames.get(device_id)

    def get_all_devices_with_cameras(self) -> List[str]:
        """Get list of devices that have provided camera data"""
        thermal_devices = set(self.thermal_frames.keys())
        rgb_devices = set(self.rgb_frames.keys())
        return list(thermal_devices | rgb_devices)

    def get_camera_statistics(self) -> Dict:
        """Get camera capture statistics"""
        uptime = time.time() - self.start_time
        fps = self.frame_count / uptime if uptime > 0 else 0

        return {
            'webcam_active': self.camera_running,
            'frames_captured': self.frame_count,
            'uptime_seconds': uptime,
            'average_fps': fps,
            'thermal_devices': len(self.thermal_frames),
            'rgb_devices': len(self.rgb_frames),
            'total_devices': len(self.get_all_devices_with_cameras())
        }

    def save_frame_to_file(self, frame: np.ndarray, filename: str, quality: int = 95) -> bool:
        """Save frame to file with specified quality"""
        try:
            # Determine file format from extension
            file_path = Path(filename)
            file_extension = file_path.suffix.lower()

            if file_extension in ['.jpg', '.jpeg']:
                cv2.imwrite(str(file_path), frame, [cv2.IMWRITE_JPEG_QUALITY, quality])
            elif file_extension == '.png':
                cv2.imwrite(str(file_path), frame, [cv2.IMWRITE_PNG_COMPRESSION, 9])
            elif file_extension == '.bmp':
                cv2.imwrite(str(file_path), frame)
            else:
                # Default to JPEG
                cv2.imwrite(str(file_path), frame, [cv2.IMWRITE_JPEG_QUALITY, quality])

            logger.info(f" Frame saved to: {filename}")
            return True

        except Exception as e:
            logger.error(f" Failed to save frame to {filename}: {e}")
            return False

    def cleanup(self):
        """Clean up camera resources"""
        self.stop_webcam_capture()
        self.thermal_frames.clear()
        self.rgb_frames.clear()


class DataExporter:
    """Handles export of multi-modal session data"""

    def __init__(self, export_base_dir: Optional[Path] = None):
        self.export_base_dir = export_base_dir or Path("./exports")
        self.export_base_dir.mkdir(exist_ok=True)

        logger.info(f" Data exporter initialized: {self.export_base_dir}")

    def export_session_data(self, session_id: str, gsr_data: Dict,
                            camera_manager: CameraManager,
                            device_info: Dict) -> str:
        """Export complete session data including GSR, images, and metadata"""
        try:
            # Create session export directory
            session_dir = self.export_base_dir / f"session_{session_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            session_dir.mkdir(exist_ok=True)

            logger.info(f" Exporting session data to: {session_dir}")

            # Export GSR data to CSV
            gsr_file = session_dir / "gsr_data.csv"
            self._export_gsr_to_csv(gsr_data, gsr_file)

            # Export camera frames
            camera_dir = session_dir / "cameras"
            camera_dir.mkdir(exist_ok=True)
            self._export_camera_frames(camera_manager, camera_dir)

            # Export device information
            devices_file = session_dir / "devices.json"
            self._export_device_info(device_info, devices_file)

            # Export session metadata
            metadata_file = session_dir / "session_metadata.json"
            self._export_session_metadata(session_id, gsr_data, camera_manager, metadata_file)

            # Create session summary
            summary_file = session_dir / "README.txt"
            self._create_session_summary(session_id, gsr_data, camera_manager, device_info, summary_file)

            logger.info(f" Session export completed: {session_dir}")
            return str(session_dir)

        except Exception as e:
            logger.error(f" Failed to export session data: {e}")
            raise

    def _export_gsr_to_csv(self, gsr_data: Dict, output_file: Path):
        """Export GSR data to CSV format"""
        try:
            with open(output_file, 'w') as f:
                f.write("timestamp,device_id,gsr_microsiemens,quality\n")

                for device_id, data_points in gsr_data.items():
                    for timestamp, gsr_value in data_points:
                        # Handle both tuple and dict formats
                        if isinstance(timestamp, str) and isinstance(gsr_value, (int, float)):
                            f.write(f"{timestamp},{device_id},{gsr_value},1.0\n")
                        elif hasattr(gsr_value, 'get'):  # Dictionary-like
                            value = gsr_value.get('value', 0.0)
                            quality = gsr_value.get('quality', 1.0)
                            f.write(f"{timestamp},{device_id},{value},{quality}\n")

            logger.info(f" GSR data exported to: {output_file}")

        except Exception as e:
            logger.error(f" Failed to export GSR data: {e}")

    def _export_camera_frames(self, camera_manager: CameraManager, camera_dir: Path):
        """Export latest camera frames from all devices"""
        try:
            frame_count = 0

            # Export local webcam frame
            webcam_frame = camera_manager.get_latest_webcam_frame()
            if webcam_frame is not None:
                webcam_file = camera_dir / "local_webcam.jpg"
                camera_manager.save_frame_to_file(webcam_frame, str(webcam_file))
                frame_count += 1

            # Export thermal frames
            thermal_dir = camera_dir / "thermal"
            thermal_dir.mkdir(exist_ok=True)

            for device_id, frame_info in camera_manager.thermal_frames.items():
                frame = frame_info['frame']
                thermal_file = thermal_dir / f"{device_id}_thermal.jpg"
                camera_manager.save_frame_to_file(frame, str(thermal_file))
                frame_count += 1

            # Export RGB frames
            rgb_dir = camera_dir / "rgb"
            rgb_dir.mkdir(exist_ok=True)

            for device_id, frame_info in camera_manager.rgb_frames.items():
                frame = frame_info['frame']
                rgb_file = rgb_dir / f"{device_id}_rgb.jpg"
                camera_manager.save_frame_to_file(frame, str(rgb_file))
                frame_count += 1

            logger.info(f" Exported {frame_count} camera frames to: {camera_dir}")

        except Exception as e:
            logger.error(f" Failed to export camera frames: {e}")

    def _export_device_info(self, device_info: Dict, output_file: Path):
        """Export device information to JSON"""
        try:
            with open(output_file, 'w') as f:
                json.dump(device_info, f, indent=2, default=str)

            logger.info(f" Device info exported to: {output_file}")

        except Exception as e:
            logger.error(f" Failed to export device info: {e}")

    def _export_session_metadata(self, session_id: str, gsr_data: Dict,
                                 camera_manager: CameraManager, output_file: Path):
        """Export session metadata"""
        try:
            # Calculate statistics
            total_gsr_samples = sum(len(points) for points in gsr_data.values())
            gsr_devices = len(gsr_data)
            camera_stats = camera_manager.get_camera_statistics()

            metadata = {
                'session_id': session_id,
                'export_timestamp': datetime.now().isoformat(),
                'data_summary': {
                    'gsr_devices': gsr_devices,
                    'total_gsr_samples': total_gsr_samples,
                    'camera_devices': camera_stats['total_devices'],
                    'webcam_active': camera_stats['webcam_active']
                },
                'camera_statistics': camera_stats,
                'export_format_version': '1.0'
            }

            with open(output_file, 'w') as f:
                json.dump(metadata, f, indent=2, default=str)

            logger.info(f" Session metadata exported to: {output_file}")

        except Exception as e:
            logger.error(f" Failed to export session metadata: {e}")

    def _create_session_summary(self, session_id: str, gsr_data: Dict,
                                camera_manager: CameraManager, device_info: Dict,
                                output_file: Path):
        """Create human-readable session summary"""
        try:
            with open(output_file, 'w') as f:
                f.write("IRCamera Multi-Modal Session Export\n")
                f.write("=" * 40 + "\n\n")

                f.write(f"Session ID: {session_id}\n")
                f.write(f"Export Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")

                # GSR Data Summary
                f.write("GSR Data Summary:\n")
                f.write("-" * 20 + "\n")
                total_samples = sum(len(points) for points in gsr_data.values())
                f.write(f"  • Devices with GSR data: {len(gsr_data)}\n")
                f.write(f"  • Total GSR samples: {total_samples}\n")

                for device_id, points in gsr_data.items():
                    f.write(f"  • {device_id}: {len(points)} samples\n")
                f.write("\n")

                # Camera Data Summary
                camera_stats = camera_manager.get_camera_statistics()
                f.write("Camera Data Summary:\n")
                f.write("-" * 20 + "\n")
                f.write(f"  • Webcam active: {'Yes' if camera_stats['webcam_active'] else 'No'}\n")
                f.write(f"  • Thermal camera devices: {camera_stats['thermal_devices']}\n")
                f.write(f"  • RGB camera devices: {camera_stats['rgb_devices']}\n")

                if camera_stats['webcam_active']:
                    f.write(f"  • Webcam frames captured: {camera_stats['frames_captured']}\n")
                    f.write(f"  • Average webcam FPS: {camera_stats['average_fps']:.1f}\n")
                f.write("\n")

                # Connected Devices
                f.write("Connected Devices:\n")
                f.write("-" * 20 + "\n")
                for device_id, info in device_info.items():
                    device_data = info.get('device_info', {})
                    device_type = device_data.get('device_type', 'unknown')
                    capabilities = device_data.get('capabilities', [])
                    f.write(f"  • {device_id} ({device_type})\n")
                    if capabilities:
                        f.write(f"    Capabilities: {', '.join(capabilities)}\n")
                f.write("\n")

                # File Structure
                f.write("Export File Structure:\n")
                f.write("-" * 20 + "\n")
                f.write("  • gsr_data.csv - GSR sensor data\n")
                f.write("  • devices.json - Device information\n")
                f.write("  • session_metadata.json - Session metadata\n")
                f.write("  • cameras/ - Camera frames\n")
                f.write("    • local_webcam.jpg - PC webcam frame\n")
                f.write("    • thermal/ - Thermal camera frames\n")
                f.write("    • rgb/ - RGB camera frames\n")

            logger.info(f" Session summary created: {output_file}")

        except Exception as e:
            logger.error(f" Failed to create session summary: {e}")


def main():
    """Test camera manager and data export functionality"""
    print(" IRCamera Camera Manager & Data Export Test")
    print("=" * 50)

    # Test camera manager
    def preview_callback(device_id: str, frame: np.ndarray, camera_type: str):
        print(f" Frame received from {device_id} ({camera_type}): {frame.shape}")

    camera_manager = CameraManager(preview_callback=preview_callback)

    # Try to start webcam
    print("Testing webcam capture...")
    if camera_manager.start_webcam_capture():
        print(" Webcam started successfully")
        time.sleep(2)  # Capture for 2 seconds

        # Get statistics
        stats = camera_manager.get_camera_statistics()
        print(f" Camera stats: {stats}")

        # Save a test frame
        frame = camera_manager.get_latest_webcam_frame()
        if frame is not None:
            test_file = "test_webcam_frame.jpg"
            if camera_manager.save_frame_to_file(frame, test_file):
                print(f" Test frame saved: {test_file}")

        camera_manager.stop_webcam_capture()
    else:
        print(" Webcam not available (expected in headless environment)")

    # Test data export
    print("\nTesting data export...")
    exporter = DataExporter()

    # Simulate session data
    fake_gsr_data = {
        'device_1': [
            ('2025-09-18T12:00:00', 15.5),
            ('2025-09-18T12:00:01', 16.2),
            ('2025-09-18T12:00:02', 14.8)
        ],
        'device_2': [
            ('2025-09-18T12:00:00', 12.1),
            ('2025-09-18T12:00:01', 13.4)
        ]
    }

    fake_device_info = {
        'device_1': {
            'device_info': {
                'device_type': 'android_phone',
                'capabilities': ['gsr', 'thermal', 'rgb'],
                'battery_level': 85
            }
        },
        'device_2': {
            'device_info': {
                'device_type': 'android_tablet',
                'capabilities': ['gsr'],
                'battery_level': 92
            }
        }
    }

    try:
        export_dir = exporter.export_session_data(
            'test_session_001',
            fake_gsr_data,
            camera_manager,
            fake_device_info
        )
        print(f" Test export completed: {export_dir}")
    except Exception as e:
        print(f" Export test failed: {e}")

    # Cleanup
    camera_manager.cleanup()
    print(" Cleanup completed")


if __name__ == "__main__":
    main()
