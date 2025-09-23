"""
Android System Emulator

Simulates Android system behavior including resource usage, file system operations,
and application lifecycle for realistic testing.
"""

import os
import time
import random
import psutil
from typing import Dict, List, Tuple, Optional, Any
from dataclasses import dataclass
from enum import Enum


class AndroidScenario(Enum):
    """Android system scenarios for testing"""
    NORMAL_OPERATION = "normal"
    LOW_MEMORY = "low_memory"
    LOW_BATTERY = "low_battery"
    BACKGROUND_APPS = "background_apps"
    THERMAL_THROTTLING = "thermal_throttling"
    STORAGE_FULL = "storage_full"


@dataclass
class AndroidSystemState:
    """Android system state snapshot"""
    timestamp: float
    memory_used_mb: int
    memory_available_mb: int
    cpu_usage_percent: float
    battery_level_percent: int
    storage_used_gb: float
    storage_available_gb: float
    active_apps: int
    temperature_celsius: float
    metadata: Dict[str, Any] = None


@dataclass
class GradleTestResult:
    """Gradle test execution result"""
    timestamp: float
    test_type: str  # 'unit' or 'instrumentation'
    total_tests: int
    passed_tests: int
    failed_tests: int
    skipped_tests: int
    execution_time_seconds: float
    success: bool
    error_output: Optional[str] = None


class AndroidSystemEmulator:
    """
    Software emulator for Android system behavior
    
    Simulates realistic:
    - Memory and CPU usage patterns
    - Battery consumption
    - Storage operations
    - Test execution behavior
    - System performance characteristics
    """
    
    # Android system specifications (based on Galaxy S22)
    TOTAL_MEMORY_MB = 8192  # 8GB RAM
    TOTAL_STORAGE_GB = 256  # 256GB storage
    BATTERY_CAPACITY_MAH = 3700  # mAh
    CPU_CORES = 8
    
    def __init__(self, seed: int = 42):
        """Initialize Android system emulator"""
        self.seed = seed
        random.seed(seed)
        
        # Current system state
        self.memory_used_mb = 2048  # Base system usage
        self.battery_level = 85  # Start with good battery
        self.storage_used_gb = 64   # Some apps/data installed
        self.cpu_temperature = 32.0  # Normal temperature
        self.active_background_apps = 15
        
        # Recording session state
        self.recording_active = False
        self.recording_start_time = None
        self.recording_memory_baseline = 0
        
        # Performance characteristics
        self.system_load_factor = 1.0
        self.thermal_throttling_active = False
        
    def set_scenario(self, scenario: AndroidScenario):
        """Configure system for specific testing scenario"""
        if scenario == AndroidScenario.LOW_MEMORY:
            self.memory_used_mb = int(self.TOTAL_MEMORY_MB * 0.85)  # 85% memory used
            self.active_background_apps = 25
            
        elif scenario == AndroidScenario.LOW_BATTERY:
            self.battery_level = 15  # 15% battery
            
        elif scenario == AndroidScenario.BACKGROUND_APPS:
            self.active_background_apps = 35
            self.memory_used_mb = int(self.TOTAL_MEMORY_MB * 0.75)
            
        elif scenario == AndroidScenario.THERMAL_THROTTLING:
            self.cpu_temperature = 75.0  # Hot CPU
            self.thermal_throttling_active = True
            self.system_load_factor = 0.7  # Reduced performance
            
        elif scenario == AndroidScenario.STORAGE_FULL:
            self.storage_used_gb = self.TOTAL_STORAGE_GB - 2  # Only 2GB free
            
    def start_recording_session(self):
        """Start recording session and track resource usage"""
        self.recording_active = True
        self.recording_start_time = time.time()
        self.recording_memory_baseline = self.memory_used_mb
        
    def stop_recording_session(self):
        """Stop recording session"""
        self.recording_active = False
        self.recording_start_time = None
        
        # Free up some memory used by recording
        self.memory_used_mb = max(
            self.recording_memory_baseline,
            self.memory_used_mb - random.randint(200, 500)
        )
        
    def get_system_state(self) -> AndroidSystemState:
        """Get current Android system state"""
        timestamp = time.time()
        
        # Memory usage (varies based on activity)
        memory_variation = random.randint(-100, 200)
        if self.recording_active:
            # Recording uses additional memory
            recording_duration = time.time() - self.recording_start_time
            recording_memory_growth = int(recording_duration * 5)  # 5MB per second growth
            memory_variation += recording_memory_growth
            
        current_memory = min(
            self.TOTAL_MEMORY_MB - 512,  # Always leave some free
            max(512, self.memory_used_mb + memory_variation)
        )
        
        memory_available = self.TOTAL_MEMORY_MB - current_memory
        
        # CPU usage (higher during recording)
        base_cpu = 15.0  # Base system CPU usage
        if self.recording_active:
            recording_cpu = random.uniform(25.0, 45.0)
        else:
            recording_cpu = 0.0
            
        background_cpu = self.active_background_apps * 0.5
        total_cpu = min(100.0, base_cpu + recording_cpu + background_cpu)
        
        if self.thermal_throttling_active:
            total_cpu *= self.system_load_factor
            
        # Battery level (decreases over time, faster during recording)
        if self.recording_active:
            battery_drain_rate = 0.5  # %/minute during recording
        else:
            battery_drain_rate = 0.1  # %/minute idle
            
        # Simulate battery drain (simplified)
        self.battery_level = max(0, self.battery_level - random.uniform(0, battery_drain_rate))
        
        # Storage (increases during recording)
        storage_growth = 0.0
        if self.recording_active:
            recording_duration = time.time() - self.recording_start_time
            storage_growth = recording_duration * 0.02  # 20MB per second recording
            
        current_storage = min(
            self.TOTAL_STORAGE_GB - 1,  # Always leave some free
            self.storage_used_gb + storage_growth
        )
        
        storage_available = self.TOTAL_STORAGE_GB - current_storage
        
        # Temperature (increases with load)
        temp_increase = (total_cpu / 100.0) * 10.0  # Up to 10°C increase
        current_temperature = self.cpu_temperature + temp_increase + random.uniform(-2.0, 2.0)
        
        return AndroidSystemState(
            timestamp=timestamp,
            memory_used_mb=int(current_memory),
            memory_available_mb=int(memory_available),
            cpu_usage_percent=total_cpu,
            battery_level_percent=int(self.battery_level),
            storage_used_gb=round(current_storage, 2),
            storage_available_gb=round(storage_available, 2),
            active_apps=self.active_background_apps,
            temperature_celsius=current_temperature,
            metadata={
                'recording_active': self.recording_active,
                'thermal_throttling': self.thermal_throttling_active,
                'emulator_seed': self.seed
            }
        )
        
    def simulate_gradle_test_execution(self, test_type: str = 'unit') -> GradleTestResult:
        """Simulate Gradle test execution (unit or instrumentation)"""
        timestamp = time.time()
        
        # Test counts based on actual project
        if test_type == 'unit':
            total_tests = 42
            base_execution_time = 45.0  # seconds
        elif test_type == 'instrumentation':
            total_tests = 28
            base_execution_time = 120.0  # seconds (requires device)
        else:
            total_tests = 15
            base_execution_time = 30.0
            
        # Execution time varies with system performance
        execution_time = base_execution_time * (2.0 - self.system_load_factor)
        execution_time += random.uniform(-10.0, 20.0)  # Natural variation
        
        # Success rate depends on system conditions
        system_state = self.get_system_state()
        
        # Factors affecting test success
        memory_pressure = system_state.memory_used_mb / self.TOTAL_MEMORY_MB
        thermal_impact = 1.0 if system_state.temperature_celsius > 70 else 0.0
        battery_impact = 1.0 if system_state.battery_level_percent < 10 else 0.0
        
        # Calculate failure probability
        failure_probability = (memory_pressure * 0.1) + (thermal_impact * 0.15) + (battery_impact * 0.2)
        
        # Determine test results
        if test_type == 'instrumentation' and system_state.battery_level_percent < 5:
            # Device might disconnect during long instrumentation tests
            success = False
            passed_tests = random.randint(0, total_tests // 3)
            failed_tests = total_tests - passed_tests
            skipped_tests = 0
            error_output = "Device disconnected during test execution"
            
        elif random.random() < failure_probability:
            # Some tests fail due to system stress
            success = False
            failed_tests = random.randint(1, max(1, total_tests // 10))
            passed_tests = total_tests - failed_tests
            skipped_tests = 0
            
            error_output = self._generate_test_error_output(system_state)
            
        else:
            # Tests pass successfully
            success = True
            passed_tests = total_tests
            failed_tests = 0
            skipped_tests = 0
            error_output = None
            
        return GradleTestResult(
            timestamp=timestamp,
            test_type=test_type,
            total_tests=total_tests,
            passed_tests=passed_tests,
            failed_tests=failed_tests,
            skipped_tests=skipped_tests,
            execution_time_seconds=execution_time,
            success=success,
            error_output=error_output
        )
        
    def simulate_file_io_performance(self, operation: str, file_size_mb: int) -> Dict[str, Any]:
        """Simulate file I/O performance (read/write operations)"""
        timestamp = time.time()
        
        # Base I/O performance (MB/s)
        if operation == 'write':
            base_throughput = 45.0  # MB/s for flash storage write
        else:  # read
            base_throughput = 120.0  # MB/s for flash storage read
            
        # Performance factors
        system_state = self.get_system_state()
        
        # Memory pressure reduces I/O performance
        memory_factor = 1.0 - (system_state.memory_used_mb / self.TOTAL_MEMORY_MB) * 0.3
        
        # Thermal throttling reduces performance
        thermal_factor = self.system_load_factor
        
        # Storage nearly full reduces performance
        storage_factor = 1.0
        if system_state.storage_available_gb < 5.0:
            storage_factor = 0.6  # Significant slowdown when nearly full
            
        # Calculate actual throughput
        actual_throughput = base_throughput * memory_factor * thermal_factor * storage_factor
        actual_throughput += random.uniform(-5.0, 5.0)  # Natural variation
        actual_throughput = max(5.0, actual_throughput)  # Minimum performance
        
        # Calculate operation time
        operation_time = file_size_mb / actual_throughput
        
        # Success probability (failures rare but possible)
        if system_state.storage_available_gb < 1.0 and operation == 'write':
            success = False
            error = "Insufficient storage space"
        elif system_state.memory_available_mb < 100:
            success = random.random() > 0.1  # 10% failure chance with low memory
            error = "Out of memory" if not success else None
        else:
            success = random.random() > 0.001  # 0.1% baseline failure rate
            error = "I/O error" if not success else None
            
        return {
            'timestamp': timestamp,
            'operation': operation,
            'file_size_mb': file_size_mb,
            'throughput_mbps': actual_throughput,
            'operation_time_seconds': operation_time,
            'success': success,
            'error': error,
            'system_state': {
                'memory_available_mb': system_state.memory_available_mb,
                'storage_available_gb': system_state.storage_available_gb,
                'cpu_usage_percent': system_state.cpu_usage_percent
            }
        }
        
    def simulate_app_lifecycle_events(self, duration: float = 60.0) -> List[Dict[str, Any]]:
        """Simulate Android app lifecycle events during testing"""
        events = []
        
        # Background app events
        num_events = random.randint(5, 15)
        for i in range(num_events):
            event_time = random.uniform(0, duration)
            
            event_types = ['app_background', 'app_foreground', 'memory_pressure', 'gc_event']
            event_type = random.choice(event_types)
            
            if event_type == 'memory_pressure':
                # Trigger garbage collection
                memory_freed = random.randint(50, 200)
                self.memory_used_mb -= memory_freed
                
            elif event_type == 'app_background':
                self.active_background_apps += 1
                
            elif event_type == 'app_foreground':
                if self.active_background_apps > 0:
                    self.active_background_apps -= 1
                    
            event = {
                'timestamp': time.time() + event_time,
                'event_type': event_type,
                'memory_impact_mb': memory_freed if event_type == 'memory_pressure' else 0,
                'app_count': self.active_background_apps
            }
            
            events.append(event)
            
        return sorted(events, key=lambda x: x['timestamp'])
        
    def _generate_test_error_output(self, system_state: AndroidSystemState) -> str:
        """Generate realistic test error output based on system conditions"""
        if system_state.memory_available_mb < 200:
            return "java.lang.OutOfMemoryError: Failed to allocate memory for test execution"
            
        elif system_state.temperature_celsius > 70:
            return "Test execution slowed due to thermal throttling"
            
        elif system_state.storage_available_gb < 2:
            return "java.io.IOException: No space left on device"
            
        else:
            return "Test failed due to system resource constraints"
            
    def get_hardware_info(self) -> Dict[str, Any]:
        """Return emulated Android hardware information"""
        return {
            'device_model': 'Samsung Galaxy S22 (Emulated)',
            'android_version': 'Android 15',
            'api_level': 35,
            'total_memory_mb': self.TOTAL_MEMORY_MB,
            'total_storage_gb': self.TOTAL_STORAGE_GB,
            'cpu_cores': self.CPU_CORES,
            'battery_capacity_mah': self.BATTERY_CAPACITY_MAH,
            'emulator_seed': self.seed
        }
        
    def get_performance_profile(self) -> Dict[str, Any]:
        """Return current performance characteristics"""
        system_state = self.get_system_state()
        
        return {
            'memory_usage_percent': (system_state.memory_used_mb / self.TOTAL_MEMORY_MB) * 100,
            'storage_usage_percent': (system_state.storage_used_gb / self.TOTAL_STORAGE_GB) * 100,
            'cpu_usage_percent': system_state.cpu_usage_percent,
            'battery_level_percent': system_state.battery_level_percent,
            'temperature_celsius': system_state.temperature_celsius,
            'performance_factor': self.system_load_factor,
            'thermal_throttling_active': self.thermal_throttling_active
        }


# Example usage and testing functions
def demonstrate_android_emulator():
    """Demonstration of Android system emulator capabilities"""
    emulator = AndroidSystemEmulator(seed=42)
    
    print("Android System Emulator Demo")
    print("=" * 40)
    
    # Hardware info
    hw_info = emulator.get_hardware_info()
    for key, value in hw_info.items():
        print(f"{key}: {value}")
    print()
    
    # Test different scenarios
    scenarios = [
        AndroidScenario.NORMAL_OPERATION,
        AndroidScenario.LOW_MEMORY,
        AndroidScenario.THERMAL_THROTTLING
    ]
    
    for scenario in scenarios:
        print(f"Testing {scenario.value} scenario...")
        emulator.set_scenario(scenario)
        
        # System state
        state = emulator.get_system_state()
        print(f"  Memory: {state.memory_used_mb}/{emulator.TOTAL_MEMORY_MB} MB")
        print(f"  CPU: {state.cpu_usage_percent:.1f}%")
        print(f"  Battery: {state.battery_level_percent}%")
        print(f"  Temperature: {state.temperature_celsius:.1f}°C")
        
        # Gradle test simulation
        unit_result = emulator.simulate_gradle_test_execution('unit')
        print(f"  Unit tests: {unit_result.passed_tests}/{unit_result.total_tests} passed "
              f"in {unit_result.execution_time_seconds:.1f}s")
        
        # File I/O test
        io_result = emulator.simulate_file_io_performance('write', 100)
        print(f"  File I/O: {io_result['throughput_mbps']:.1f} MB/s write performance")
        print()
        
    # Recording session simulation
    print("Recording session simulation...")
    emulator.start_recording_session()
    
    initial_state = emulator.get_system_state()
    print(f"  Initial memory: {initial_state.memory_used_mb} MB")
    
    time.sleep(1)  # Simulate some recording time
    
    final_state = emulator.get_system_state()
    print(f"  After recording: {final_state.memory_used_mb} MB")
    print(f"  Memory growth: {final_state.memory_used_mb - initial_state.memory_used_mb} MB")
    
    emulator.stop_recording_session()


if __name__ == "__main__":
    demonstrate_android_emulator()