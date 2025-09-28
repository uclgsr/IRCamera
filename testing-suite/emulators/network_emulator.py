"""
Network and Communication Emulator

Generates realistic network conditions and communication patterns
for testing PC-Android coordination without requiring real network infrastructure.
"""

import time
import math
import random
from typing import Dict, List, Tuple, Optional, Any
from dataclasses import dataclass
from enum import Enum
import socket


class NetworkCondition(Enum):
    """Network condition scenarios for testing"""
    EXCELLENT_ETHERNET = "excellent_ethernet"
    GOOD_WIFI_5GHZ = "good_wifi_5ghz"
    MODERATE_WIFI_2GHZ = "moderate_wifi_2ghz"
    POOR_WIFI = "poor_wifi"
    MOBILE_DATA_4G = "mobile_4g"
    UNSTABLE_CONNECTION = "unstable"
    ROAMING_FAILURE = "roaming_failure"


@dataclass
class NetworkMeasurement:
    """Single network performance measurement"""
    timestamp: float
    latency_ms: float
    jitter_ms: float
    packet_loss_percent: float
    bandwidth_mbps: float
    connection_quality: float  # 0.0-1.0
    metadata: Dict[str, Any] = None


@dataclass
class ProtocolMessage:
    """Network protocol message with timing"""
    timestamp: float
    message_type: str
    payload_size: int
    processing_time_ms: float
    response_time_ms: float
    success: bool
    error_code: Optional[str] = None


class NetworkEmulator:
    """
    Software emulator for network conditions and protocol communication
    
    Generates realistic:
    - Network latency and jitter patterns
    - TCP connection behavior
    - Protocol message timing
    - Failure modes and recovery
    """
    
    # Network condition parameters
    NETWORK_PROFILES = {
        NetworkCondition.EXCELLENT_ETHERNET: {
            'base_latency': 1.5,      # ms
            'jitter_range': (0.1, 0.5),
            'packet_loss': 0.0,       # %
            'bandwidth': 1000.0,      # Mbps
            'stability': 0.98
        },
        NetworkCondition.GOOD_WIFI_5GHZ: {
            'base_latency': 8.0,
            'jitter_range': (0.5, 2.0),
            'packet_loss': 0.01,
            'bandwidth': 200.0,
            'stability': 0.95
        },
        NetworkCondition.MODERATE_WIFI_2GHZ: {
            'base_latency': 23.0,
            'jitter_range': (2.0, 8.0),
            'packet_loss': 0.05,
            'bandwidth': 50.0,
            'stability': 0.88
        },
        NetworkCondition.POOR_WIFI: {
            'base_latency': 150.0,
            'jitter_range': (20.0, 100.0),
            'packet_loss': 2.0,
            'bandwidth': 10.0,
            'stability': 0.70
        },
        NetworkCondition.MOBILE_DATA_4G: {
            'base_latency': 45.0,
            'jitter_range': (5.0, 25.0),
            'packet_loss': 0.1,
            'bandwidth': 25.0,
            'stability': 0.85
        },
        NetworkCondition.UNSTABLE_CONNECTION: {
            'base_latency': 80.0,
            'jitter_range': (10.0, 200.0),
            'packet_loss': 5.0,
            'bandwidth': 5.0,
            'stability': 0.50
        },
        NetworkCondition.ROAMING_FAILURE: {
            'base_latency': 187.0,     # Documented thesis value
            'jitter_range': (50.0, 80.0),  # 50-80ms jumps
            'packet_loss': 15.0,
            'bandwidth': 2.0,
            'stability': 0.25
        }
    }
    
    def __init__(self, seed: int = 42):
        """Initialize network emulator with reproducible random seed"""
        self.seed = seed
        random.seed(seed)
        
        # Current state
        self.start_time = None
        self.current_condition = NetworkCondition.GOOD_WIFI_5GHZ
        self.measurement_counter = 0
        self.message_counter = 0
        
        # Connection state
        self.connected_devices = {}
        self.active_sessions = {}
        
    def set_network_condition(self, condition: NetworkCondition):
        """Set current network condition for simulation"""
        self.current_condition = condition
        
    def start_monitoring(self):
        """Start network monitoring simulation"""
        self.start_time = time.time()
        self.measurement_counter = 0
        
    def generate_network_measurement(self) -> NetworkMeasurement:
        """Generate realistic network performance measurement"""
        if self.start_time is None:
            self.start_time = time.time()
            
        current_time = time.time()
        timestamp = current_time
        
        profile = self.NETWORK_PROFILES[self.current_condition]
        
        # Generate realistic latency with temporal correlation
        base_latency = profile['base_latency']
        jitter_min, jitter_max = profile['jitter_range']
        
        # Add temporal correlation (network conditions don't change instantly)
        if self.measurement_counter > 0:
            correlation_factor = 0.7  # Strong correlation with previous measurements
            jitter = random.uniform(jitter_min, jitter_max) * correlation_factor
        else:
            jitter = random.uniform(jitter_min, jitter_max)
            
        latency = max(0.1, base_latency + random.uniform(-jitter, jitter))
        
        # Packet loss simulation
        packet_loss = profile['packet_loss'] * random.uniform(0.5, 2.0)
        
        # Bandwidth variation
        bandwidth = profile['bandwidth'] * random.uniform(0.8, 1.2)
        
        # Connection quality based on stability
        stability = profile['stability']
        connection_quality = min(1.0, stability + random.uniform(-0.1, 0.1))
        
        measurement = NetworkMeasurement(
            timestamp=timestamp,
            latency_ms=latency,
            jitter_ms=jitter,
            packet_loss_percent=packet_loss,
            bandwidth_mbps=bandwidth,
            connection_quality=connection_quality,
            metadata={
                'network_condition': self.current_condition.value,
                'measurement_id': self.measurement_counter,
                'emulator_seed': self.seed
            }
        )
        
        self.measurement_counter += 1
        return measurement
        
    def simulate_tcp_connection(self, source: str, destination: str, port: int = 8080) -> Dict[str, Any]:
        """Simulate TCP connection establishment"""
        profile = self.NETWORK_PROFILES[self.current_condition]
        
        # Connection attempt timing
        base_latency = profile['base_latency']
        connection_time = base_latency * 3  # TCP handshake takes ~3x RTT
        
        # Success probability based on network stability
        success_probability = profile['stability']
        connection_success = random.random() < success_probability
        
        connection_info = {
            'source': source,
            'destination': destination,
            'port': port,
            'connection_time_ms': connection_time,
            'success': connection_success,
            'timestamp': time.time()
        }
        
        if connection_success:
            # Store active connection
            connection_key = f"{source}:{destination}:{port}"
            self.connected_devices[connection_key] = {
                'established': time.time(),
                'last_activity': time.time(),
                'bytes_sent': 0,
                'bytes_received': 0
            }
            connection_info['connection_id'] = connection_key
        else:
            # Connection failed
            if profile['stability'] < 0.5:
                connection_info['error'] = 'network_unreachable'
            else:
                connection_info['error'] = 'connection_timeout'
                
        return connection_info
        
    def simulate_protocol_message(self, message_type: str, payload_size: int) -> ProtocolMessage:
        """Simulate protocol message exchange timing"""
        timestamp = time.time()
        profile = self.NETWORK_PROFILES[self.current_condition]
        
        # Message processing time (varies by type)
        processing_times = {
            'HELLO': 2.0,           # ms
            'START_RECORD': 15.0,   # ms
            'STOP_RECORD': 8.0,     # ms
            'SYNC_REQUEST': 3.0,    # ms
            'SYNC_RESPONSE': 3.0,   # ms
            'DATA_GSR': 1.0,        # ms
            'ACK': 1.0,             # ms
            'ERROR': 2.0,           # ms
            'FRAME': 25.0           # ms (larger payload)
        }
        
        base_processing = processing_times.get(message_type, 5.0)
        processing_time = base_processing * random.uniform(0.8, 1.5)
        
        # Network transmission time
        bandwidth_mbps = profile['bandwidth'] * random.uniform(0.7, 1.3)
        transmission_time = (payload_size * 8) / (bandwidth_mbps * 1e6) * 1000  # ms
        
        # Total response time
        network_latency = profile['base_latency'] * random.uniform(0.5, 2.0)
        total_response_time = processing_time + transmission_time + network_latency
        
        # Success probability
        success_probability = profile['stability']
        message_success = random.random() < success_probability
        
        error_code = None
        if not message_success:
            error_codes = ['timeout', 'connection_lost', 'protocol_error', 'checksum_error']
            error_code = random.choice(error_codes)
            
        message = ProtocolMessage(
            timestamp=timestamp,
            message_type=message_type,
            payload_size=payload_size,
            processing_time_ms=processing_time,
            response_time_ms=total_response_time,
            success=message_success,
            error_code=error_code
        )
        
        self.message_counter += 1
        return message
        
    def simulate_device_discovery(self, scan_duration: float = 10.0) -> List[Dict[str, Any]]:
        """Simulate mDNS device discovery process"""
        discovered_devices = []
        
        # Number of devices found depends on network condition
        profile = self.NETWORK_PROFILES[self.current_condition]
        max_devices = 5
        discovery_probability = profile['stability']
        
        for device_id in range(1, max_devices + 1):
            if random.random() < discovery_probability:
                discovery_time = random.uniform(1.0, scan_duration)
                
                device_info = {
                    'device_id': f'android_sensor_{device_id}',
                    'device_name': f'Android Device {device_id}',
                    'ip_address': f'192.168.1.{100 + device_id}',
                    'port': 8080,
                    'services': ['gsr-recording', 'thermal-recording', 'rgb-recording'],
                    'discovery_time': discovery_time,
                    'signal_strength': random.uniform(0.5, 1.0) * profile['stability']
                }
                
                discovered_devices.append(device_info)
                
        return discovered_devices
        
    def simulate_wifi_roaming_failure(self, duration: float = 120.0) -> List[NetworkMeasurement]:
        """
        Simulate Wi-Fi roaming failure scenario documented in thesis
        (3/14 sessions with 50-80ms latency jumps)
        """
        measurements = []
        self.set_network_condition(NetworkCondition.ROAMING_FAILURE)
        
        num_measurements = int(duration)  # 1 measurement per second
        failure_probability = 3.0 / 14.0  # 3/14 sessions as documented
        
        base_latency = 23.0  # Normal Wi-Fi latency
        failure_latency_range = (50.0, 80.0)  # Documented jump range
        
        for i in range(num_measurements):
            timestamp = time.time() + i
            
            # Determine if this is a failure event
            is_failure = random.random() < failure_probability
            
            if is_failure:
                # Latency jump
                latency_jump = random.uniform(*failure_latency_range)
                latency = base_latency + latency_jump
                quality = 0.3  # Poor quality during failure
            else:
                # Normal operation
                latency = base_latency + random.uniform(-5.0, 10.0)
                quality = 0.9
                
            measurement = NetworkMeasurement(
                timestamp=timestamp,
                latency_ms=latency,
                jitter_ms=random.uniform(2.0, 15.0),
                packet_loss_percent=random.uniform(0.0, 5.0) if is_failure else 0.05,
                bandwidth_mbps=random.uniform(20.0, 60.0),
                connection_quality=quality,
                metadata={
                    'scenario': 'wifi_roaming_failure',
                    'failure_event': is_failure,
                    'measurement_sequence': i
                }
            )
            
            measurements.append(measurement)
            
        return measurements
        
    def simulate_multi_device_coordination(self, device_count: int = 3, duration: float = 60.0) -> Dict[str, List[ProtocolMessage]]:
        """Simulate multi-device coordination messaging"""
        device_messages = {}
        
        for device_id in range(device_count):
            device_name = f"android_device_{device_id + 1}"
            messages = []
            
            # Connection phase
            hello_msg = self.simulate_protocol_message('HELLO', 150)
            hello_msg.timestamp = time.time()
            messages.append(hello_msg)
            
            # Sync phase
            sync_req = self.simulate_protocol_message('SYNC_REQUEST', 50)
            sync_req.timestamp = time.time() + 1.0
            messages.append(sync_req)
            
            sync_resp = self.simulate_protocol_message('SYNC_RESPONSE', 75)
            sync_resp.timestamp = time.time() + 1.1
            messages.append(sync_resp)
            
            # Recording phase
            start_msg = self.simulate_protocol_message('START_RECORD', 200)
            start_msg.timestamp = time.time() + 5.0
            messages.append(start_msg)
            
            # Data streaming (periodic messages)
            stream_duration = duration - 15.0  # Leave time for cleanup
            num_data_messages = int(stream_duration * 2)  # 2 Hz data rate
            
            for i in range(num_data_messages):
                data_msg = self.simulate_protocol_message('DATA_GSR', 100)
                data_msg.timestamp = time.time() + 10.0 + (i * 0.5)
                messages.append(data_msg)
                
            # Stop phase
            stop_msg = self.simulate_protocol_message('STOP_RECORD', 100)
            stop_msg.timestamp = time.time() + duration - 2.0
            messages.append(stop_msg)
            
            device_messages[device_name] = messages
            
        return device_messages
        
    def get_network_statistics(self) -> Dict[str, Any]:
        """Return current network performance statistics"""
        profile = self.NETWORK_PROFILES[self.current_condition]
        
        return {
            'condition': self.current_condition.value,
            'expected_latency': f"{profile['base_latency']:.1f} ms",
            'expected_jitter': f"{profile['jitter_range'][0]:.1f}-{profile['jitter_range'][1]:.1f} ms",
            'packet_loss': f"{profile['packet_loss']:.2f}%",
            'bandwidth': f"{profile['bandwidth']:.0f} Mbps",
            'stability': f"{profile['stability']:.1%}",
            'measurements_generated': self.measurement_counter,
            'messages_simulated': self.message_counter
        }
        
    def generate_synchronization_test_scenario(self, duration: float = 30.0) -> List[NetworkMeasurement]:
        """
        Generate network measurements for synchronization testing.
        Tests network timing precision for cross-modal alignment validation.
        """
        measurements = []
        
        # Start with good conditions
        self.set_network_condition(NetworkCondition.GOOD_WIFI_5GHZ)
        
        num_measurements = int(duration * 10)  # 10 Hz measurement rate
        
        for i in range(num_measurements):
            timestamp = time.time() + (i * 0.1)
            
            # Generate measurement
            measurement = self.generate_network_measurement()
            measurement.timestamp = timestamp
            
            # Add sync event marker at t=15s
            elapsed = i * 0.1
            if 14.9 <= elapsed <= 15.1:
                measurement.metadata['sync_event'] = 'network_ping_reference'
                measurement.metadata['sync_confidence'] = 0.98
                
            measurements.append(measurement)
            
        return measurements


# Example usage and testing functions
def demonstrate_network_emulator():
    """Demonstration of network emulator capabilities"""
    emulator = NetworkEmulator(seed=42)
    
    print("Network Communication Emulator Demo")
    print("=" * 40)
    
    # Test different network conditions
    conditions = [
        NetworkCondition.EXCELLENT_ETHERNET,
        NetworkCondition.GOOD_WIFI_5GHZ,
        NetworkCondition.MODERATE_WIFI_2GHZ,
        NetworkCondition.POOR_WIFI
    ]
    
    for condition in conditions:
        print(f"Testing {condition.value}...")
        emulator.set_network_condition(condition)
        
        # Generate measurements
        measurements = []
        for i in range(10):
            measurement = emulator.generate_network_measurement()
            measurements.append(measurement)
            
        # Statistics
        latencies = [m.latency_ms for m in measurements]
        qualities = [m.connection_quality for m in measurements]
        
        print(f"  Average latency: {sum(latencies)/len(latencies):.1f} ms")
        print(f"  Latency range: {min(latencies):.1f} to {max(latencies):.1f} ms")
        print(f"  Average quality: {sum(qualities)/len(qualities):.1%}")
        
        # Protocol message test
        msg = emulator.simulate_protocol_message('START_RECORD', 200)
        print(f"  START_RECORD response time: {msg.response_time_ms:.1f} ms")
        print()
        
    # Multi-device coordination test
    print("Multi-device coordination test...")
    device_messages = emulator.simulate_multi_device_coordination(3, 30.0)
    
    total_messages = sum(len(msgs) for msgs in device_messages.values())
    print(f"Generated {total_messages} messages for {len(device_messages)} devices")
    
    # Network statistics
    print("\nNetwork Statistics:")
    stats = emulator.get_network_statistics()
    for key, value in stats.items():
        print(f"  {key}: {value}")


if __name__ == "__main__":
    demonstrate_network_emulator()