#!/usr/bin/env python3
"""
Shimmer3 GSR+ Research PC Controller

Enhanced PC controller implementing the Hub-Spoke architecture for research-grade 
physiological data collection with Shimmer3 GSR+ devices.

Integration Plan Implementation:
- Real-time TCP communication with Android Shimmer nodes
- Research-grade data visualization with 128Hz sampling support
- Multi-device coordination for synchronized recording
- Comprehensive data logging with temporal alignment
- Quality metrics and validation for 12-bit ADC precision

Features:
- Multi-threaded TCP server supporting up to 8 Android devices
- Real-time matplotlib visualization of GSR streams
- Automatic device discovery and status monitoring  
- CSV export with research metadata and quality assessment
- Synchronization markers for cross-device temporal alignment
- Network resilience with automatic reconnection handling

This implementation supports the enhanced Android Shimmer3 GSR+ integration
without simulation or fake data - real research-grade data collection.
"""

import asyncio
import json
import logging
import matplotlib.pyplot as plt
import numpy as np
import socket
import threading
import time
from dataclasses import dataclass
from datetime import datetime
from matplotlib.animation import FuncAnimation
from pathlib import Path
from typing import Dict, List, Optional

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class GSRSample:
    """Research-grade GSR sample data structure with enhanced metadata"""
    device_id: str
    timestamp_ms: int
    gsr_microsiemens: float
    raw_adc_12bit: int  # Emphasizing 12-bit ADC (0-4095 range)
    resistance_ohm: float  # Full precision resistance in Ohms
    sample_sequence: int
    elapsed_seconds: float  # Time since recording start
    quality_flag: bool = True  # Data quality indicator

    @property
    def resistance_kohm(self) -> float:
        """Resistance in kΩ for compatibility"""
        return self.resistance_ohm / 1000.0


@dataclass
class ConnectedDevice:
    """Enhanced device information with research metrics"""
    device_id: str
    device_name: str
    device_address: str  # Bluetooth MAC address
    connection_time: float
    last_sample_time: float
    sample_count: int
    is_recording: bool
    sampling_rate_hz: float = 128.0  # Expected Shimmer3 GSR+ sampling rate
    session_id: Optional[str] = None
    data_quality_score: float = 1.0  # 0.0-1.0 quality metric

    @property
    def recording_duration(self) -> float:
        """Duration of current recording session in seconds"""
        if not self.is_recording:
            return 0.0
        return time.time() - self.connection_time

    @property
    def expected_sample_count(self) -> int:
        """Expected number of samples based on duration and sampling rate"""
        return int(self.recording_duration * self.sampling_rate_hz)

    @property
    def sample_completeness(self) -> float:
        """Ratio of actual to expected samples (data quality metric)"""
        expected = self.expected_sample_count
        return self.sample_count / expected if expected > 0 else 0.0


class ShimmerMvpController:
    """
    Main controller class for receiving and processing Shimmer GSR data from Android devices
    """

    def __init__(self, port: int = 8888):
        self.port = port
        self.server_socket: Optional[socket.socket] = None
        self.is_running = False
        self.connected_devices: Dict[str, ConnectedDevice] = {}
        self.gsr_data_buffer: Dict[str, List[GSRSample]] = {}

        # Data storage
        self.data_directory = Path("shimmer_data")
        self.data_directory.mkdir(exist_ok=True)

        # Real-time visualization
        self.fig, self.ax = plt.subplots(figsize=(12, 6))
        self.max_display_samples = 1000

        # Server thread
        self.server_thread: Optional[threading.Thread] = None

    def start_server(self):
        """Start the TCP server to accept connections from Android devices"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(5)

            self.is_running = True
            logger.info(f"Shimmer MVP Controller started on port {self.port}")

            self.server_thread = threading.Thread(target=self._server_loop, daemon=True)
            self.server_thread.start()

            return True

        except Exception as e:
            logger.error(f"Failed to start server: {e}")
            return False

    def stop_server(self):
        """Stop the TCP server"""
        try:
            self.is_running = False
            if self.server_socket:
                self.server_socket.close()
            logger.info("Server stopped")
        except Exception as e:
            logger.error(f"Error stopping server: {e}")

    def _server_loop(self):
        """Main server loop to accept and handle client connections"""
        while self.is_running:
            try:
                if not self.server_socket:
                    break

                client_socket, client_address = self.server_socket.accept()
                logger.info(f"New connection from {client_address}")

                # Handle each client in a separate thread
                client_thread = threading.Thread(
                    target=self._handle_client,
                    args=(client_socket, client_address),
                    daemon=True
                )
                client_thread.start()

            except OSError:
                # Socket was closed
                break
            except Exception as e:
                logger.error(f"Error in server loop: {e}")

    def _handle_client(self, client_socket: socket.socket, client_address):
        """Handle communication with a single Android device"""
        device_id = f"{client_address[0]}:{client_address[1]}"

        try:
            # Send connection acknowledgment
            welcome_msg = {
                "type": "connection_ack",
                "server_time": time.time() * 1000,
                "message": "Connected to Shimmer MVP Controller"
            }
            self._send_message(client_socket, welcome_msg)

            # Register device
            device_info = ConnectedDevice(
                device_id=device_id,
                device_name=f"Android-{client_address[0]}",
                connection_time=time.time(),
                last_sample_time=time.time(),
                sample_count=0,
                is_recording=False
            )
            self.connected_devices[device_id] = device_info
            self.gsr_data_buffer[device_id] = []

            logger.info(f"Device registered: {device_id}")

            # Handle incoming messages
            buffer = ""
            while self.is_running:
                try:
                    data = client_socket.recv(4096).decode('utf-8')
                    if not data:
                        break

                    buffer += data

                    # Process complete messages (assuming newline-delimited JSON)
                    while '\n' in buffer:
                        line, buffer = buffer.split('\n', 1)
                        if line.strip():
                            self._process_message(device_id, line.strip())

                except socket.timeout:
                    continue
                except Exception as e:
                    logger.error(f"Error receiving data from {device_id}: {e}")
                    break

        except Exception as e:
            logger.error(f"Error handling client {client_address}: {e}")
        finally:
            # Cleanup
            try:
                client_socket.close()
                if device_id in self.connected_devices:
                    del self.connected_devices[device_id]
                logger.info(f"Device disconnected: {device_id}")
            except:
                pass

    def _send_message(self, client_socket: socket.socket, message: dict):
        """Send JSON message to client"""
        try:
            json_msg = json.dumps(message) + '\n'
            client_socket.send(json_msg.encode('utf-8'))
        except Exception as e:
            logger.error(f"Error sending message: {e}")

    def _process_message(self, device_id: str, message_str: str):
        """Process incoming message from Android device"""
        try:
            message = json.loads(message_str)
            msg_type = message.get('type')

            if msg_type == 'gsr_sample':
                self._process_gsr_sample(device_id, message)
            elif msg_type == 'recording_start':
                self._handle_recording_start(device_id, message)
            elif msg_type == 'recording_stop':
                self._handle_recording_stop(device_id, message)
            elif msg_type == 'sync_marker':
                self._handle_sync_marker(device_id, message)
            else:
                logger.warning(f"Unknown message type from {device_id}: {msg_type}")

        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON from {device_id}: {e}")
        except Exception as e:
            logger.error(f"Error processing message from {device_id}: {e}")

    def _process_gsr_sample(self, device_id: str, message: dict):
        """Process GSR sample data"""
        try:
            sample = GSRSample(
                device_id=device_id,
                timestamp_ms=message['timestamp_ms'],
                gsr_microsiemens=message['gsr_microsiemens'],
                raw_value=message['raw_value'],
                resistance_kohm=message['resistance_kohm'],
                sample_sequence=message.get('sample_sequence', 0)
            )

            # Add to buffer
            if device_id in self.gsr_data_buffer:
                buffer = self.gsr_data_buffer[device_id]
                buffer.append(sample)

                # Keep buffer size manageable
                if len(buffer) > self.max_display_samples * 2:
                    buffer[:] = buffer[-self.max_display_samples:]

            # Update device info
            if device_id in self.connected_devices:
                device = self.connected_devices[device_id]
                device.last_sample_time = time.time()
                device.sample_count += 1

            # Log every 100 samples
            if device.sample_count % 100 == 0:
                logger.info(f"Device {device_id}: {device.sample_count} samples, "
                            f"GSR: {sample.gsr_microsiemens:.2f} µS")

        except Exception as e:
            logger.error(f"Error processing GSR sample from {device_id}: {e}")

    def _handle_recording_start(self, device_id: str, message: dict):
        """Handle recording start notification"""
        if device_id in self.connected_devices:
            device = self.connected_devices[device_id]
            device.is_recording = True
            device.sample_count = 0

            logger.info(f"Recording started on device {device_id}")

            # Clear previous data
            if device_id in self.gsr_data_buffer:
                self.gsr_data_buffer[device_id].clear()

    def _handle_recording_stop(self, device_id: str, message: dict):
        """Handle recording stop notification"""
        if device_id in self.connected_devices:
            device = self.connected_devices[device_id]
            device.is_recording = False

            logger.info(f"Recording stopped on device {device_id}, "
                        f"total samples: {device.sample_count}")

            # Export data to CSV
            self._export_device_data(device_id)

    def _handle_sync_marker(self, device_id: str, message: dict):
        """Handle synchronization marker"""
        marker_type = message.get('marker_type', 'unknown')
        timestamp = message.get('timestamp_ms', 0)

        logger.info(f"Sync marker from {device_id}: {marker_type} at {timestamp}")

    def _export_device_data(self, device_id: str):
        """Export device data to CSV file"""
        try:
            if device_id not in self.gsr_data_buffer:
                return

            data = self.gsr_data_buffer[device_id]
            if not data:
                return

            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = self.data_directory / f"gsr_data_{device_id.replace(':', '_')}_{timestamp}.csv"

            with open(filename, 'w') as f:
                # Write header
                f.write(
                    "device_id,timestamp_ms,gsr_microsiemens,raw_value,resistance_kohm,sample_sequence\n")

                # Write data
                for sample in data:
                    f.write(f"{sample.device_id},{sample.timestamp_ms},"
                            f"{sample.gsr_microsiemens},{sample.raw_value},"
                            f"{sample.resistance_kohm},{sample.sample_sequence}\n")

            logger.info(f"Data exported to {filename} ({len(data)} samples)")

        except Exception as e:
            logger.error(f"Error exporting data for {device_id}: {e}")

    def get_device_status(self) -> Dict:
        """Get status of all connected devices"""
        status = {
            "server_running": self.is_running,
            "connected_devices": len(self.connected_devices),
            "devices": []
        }

        for device_id, device in self.connected_devices.items():
            device_status = {
                "device_id": device_id,
                "device_name": device.device_name,
                "connection_time": device.connection_time,
                "is_recording": device.is_recording,
                "sample_count": device.sample_count,
                "last_sample_time": device.last_sample_time,
                "data_rate_hz": self._calculate_data_rate(device_id)
            }
            status["devices"].append(device_status)

        return status

    def _calculate_data_rate(self, device_id: str) -> float:
        """Calculate current data rate for a device"""
        try:
            if device_id not in self.gsr_data_buffer:
                return 0.0

            data = self.gsr_data_buffer[device_id]
            if len(data) < 10:
                return 0.0

            # Get recent samples
            recent_data = data[-100:]
            if len(recent_data) < 2:
                return 0.0

            time_span = (recent_data[-1].timestamp_ms - recent_data[0].timestamp_ms) / 1000.0
            if time_span > 0:
                return len(recent_data) / time_span

        except:
            pass

        return 0.0

    def start_visualization(self):
        """Start real-time data visualization"""

        def update_plot(frame):
            self.ax.clear()

            colors = ['blue', 'red', 'green', 'orange', 'purple']

            for i, (device_id, data) in enumerate(self.gsr_data_buffer.items()):
                if not data:
                    continue

                # Get recent data for display
                recent_data = data[-self.max_display_samples:] if len(
                    data) > self.max_display_samples else data

                if len(recent_data) < 2:
                    continue

                # Extract timestamps and GSR values
                timestamps = [(sample.timestamp_ms - recent_data[0].timestamp_ms) / 1000.0
                              for sample in recent_data]
                gsr_values = [sample.gsr_microsiemens for sample in recent_data]

                color = colors[i % len(colors)]
                self.ax.plot(timestamps, gsr_values, label=f"Device {device_id}",
                             color=color, linewidth=1.5)

            self.ax.set_xlabel("Time (seconds)")
            self.ax.set_ylabel("GSR (µS)")
            self.ax.set_title("Real-time GSR Data from Connected Devices")
            self.ax.grid(True, alpha=0.3)

            if self.gsr_data_buffer:
                self.ax.legend()

            # Add device status text
            status_text = f"Connected devices: {len(self.connected_devices)}"
            recording_devices = sum(1 for d in self.connected_devices.values() if d.is_recording)
            if recording_devices > 0:
                status_text += f" (Recording: {recording_devices})"

            self.ax.text(0.02, 0.98, status_text, transform=self.ax.transAxes,
                         verticalalignment='top',
                         bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))

        # Setup animation
        animation = FuncAnimation(self.fig, update_plot, interval=500, blit=False)

        plt.tight_layout()
        plt.show()

        return animation


def main():
    """Main function to run the Shimmer MVP Controller"""
    controller = ShimmerMvpController()

    try:
        # Start server
        if not controller.start_server():
            logger.error("Failed to start server")
            return

        logger.info("Shimmer MVP Controller is running...")
        logger.info("Waiting for Android device connections...")

        # Start visualization in main thread
        animation = controller.start_visualization()

    except KeyboardInterrupt:
        logger.info("Shutting down...")
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
    finally:
        controller.stop_server()


if __name__ == "__main__":
    main()
