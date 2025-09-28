"""
TC001 Thermal Camera Software Emulator

Generates realistic thermal imaging data for testing without requiring
physical TC001 hardware. Provides spatially and temporally accurate
thermal patterns based on physics simulation.
"""

import numpy as np
import time
import math
from typing import Dict, List, Tuple, Optional, Any
from dataclasses import dataclass
from enum import Enum


class ThermalScenario(Enum):
    """Predefined thermal imaging scenarios for testing"""
    HUMAN_SUBJECT = "human_subject"
    HOT_OBJECT = "hot_object"
    ENVIRONMENTAL_CHANGE = "environmental_change"
    CALIBRATION_TARGET = "calibration_target"
    THERMAL_EVENT = "thermal_event"


@dataclass
class ThermalFrame:
    """Single thermal frame with metadata"""
    timestamp: float
    temperature_matrix: np.ndarray  # 256x192 temperature values in Celsius
    frame_id: int
    ambient_temperature: float
    emissivity: float = 0.95
    metadata: Dict[str, Any] = None


class TC001ThermalEmulator:
    """
    Software emulator for TC001 Thermal Camera
    
    Generates realistic thermal imaging data with proper:
    - Spatial temperature gradients
    - Temporal dynamics (heating/cooling)
    - Hardware-accurate specifications
    - Configurable scenarios
    """
    
    # TC001 Hardware Specifications
    RESOLUTION = (256, 192)  # width x height
    TEMP_RANGE = (-20.0, 550.0)  # Celsius
    ACCURACY = 2.0  # ±2°C
    FRAME_RATE = 10.0  # Hz
    USB_VID = 0x0525
    USB_PID = 0xa4a2
    
    def __init__(self, seed: int = 42):
        """Initialize thermal emulator with reproducible random seed"""
        self.seed = seed
        np.random.seed(seed)
        
        # Current state
        self.frame_counter = 0
        self.start_time = None
        self.current_scenario = None
        self.ambient_temperature = 22.0  # Default room temperature
        
        # Physics simulation parameters
        self.thermal_diffusion_rate = 0.1
        self.cooling_coefficient = 0.05
        self.noise_level = 0.1
        
        # Scenario-specific state
        self.scenario_state = {}
        
    def start_capture(self, scenario: ThermalScenario = ThermalScenario.HUMAN_SUBJECT):
        """Start thermal capture with specified scenario"""
        self.start_time = time.time()
        self.frame_counter = 0
        self.current_scenario = scenario
        self.scenario_state = self._initialize_scenario(scenario)
        
    def generate_frame(self) -> ThermalFrame:
        """Generate single realistic thermal frame"""
        if self.start_time is None:
            raise RuntimeError("Must call start_capture() before generating frames")
            
        current_time = time.time()
        elapsed = current_time - self.start_time
        timestamp = self.start_time + (self.frame_counter / self.FRAME_RATE)
        
        # Generate temperature matrix based on scenario
        temp_matrix = self._generate_temperature_matrix(elapsed)
        
        # Add hardware-realistic noise
        temp_matrix = self._apply_hardware_noise(temp_matrix)
        
        # Ensure within hardware range
        temp_matrix = np.clip(temp_matrix, self.TEMP_RANGE[0], self.TEMP_RANGE[1])
        
        frame = ThermalFrame(
            timestamp=timestamp,
            temperature_matrix=temp_matrix,
            frame_id=self.frame_counter,
            ambient_temperature=self.ambient_temperature,
            metadata={
                'scenario': self.current_scenario.value,
                'elapsed_time': elapsed,
                'emulator_seed': self.seed
            }
        )
        
        self.frame_counter += 1
        return frame
        
    def generate_sequence(self, duration_seconds: float) -> List[ThermalFrame]:
        """Generate sequence of thermal frames over specified duration"""
        frames = []
        num_frames = int(duration_seconds * self.FRAME_RATE)
        
        for i in range(num_frames):
            frame = self.generate_frame()
            frames.append(frame)
            
        return frames
        
    def _initialize_scenario(self, scenario: ThermalScenario) -> Dict[str, Any]:
        """Initialize scenario-specific parameters"""
        if scenario == ThermalScenario.HUMAN_SUBJECT:
            return {
                'subject_temp': 37.2,  # Normal body temperature
                'subject_position': (128, 96),  # Center of frame
                'subject_size': 40,  # Thermal signature radius
                'movement_pattern': 'stationary'
            }
            
        elif scenario == ThermalScenario.HOT_OBJECT:
            return {
                'object_temp': 85.0,
                'object_position': (100, 80),
                'object_size': 20,
                'cooling_enabled': True
            }
            
        elif scenario == ThermalScenario.ENVIRONMENTAL_CHANGE:
            return {
                'initial_temp': self.ambient_temperature,
                'target_temp': self.ambient_temperature + 5.0,
                'change_rate': 0.1  # degrees/second
            }
            
        elif scenario == ThermalScenario.CALIBRATION_TARGET:
            return {
                'target_temp': 50.0,
                'target_position': (128, 96),
                'target_size': 15,
                'stability': 0.05  # temperature stability
            }
            
        elif scenario == ThermalScenario.THERMAL_EVENT:
            return {
                'event_temp': 120.0,
                'event_start_time': 10.0,  # seconds
                'event_duration': 5.0,
                'event_position': (150, 120)
            }
            
        return {}
        
    def _generate_temperature_matrix(self, elapsed_time: float) -> np.ndarray:
        """Generate realistic temperature matrix based on current scenario"""
        width, height = self.RESOLUTION
        temp_matrix = np.full((height, width), self.ambient_temperature, dtype=np.float32)
        
        if self.current_scenario == ThermalScenario.HUMAN_SUBJECT:
            temp_matrix = self._apply_human_subject_pattern(temp_matrix, elapsed_time)
            
        elif self.current_scenario == ThermalScenario.HOT_OBJECT:
            temp_matrix = self._apply_hot_object_pattern(temp_matrix, elapsed_time)
            
        elif self.current_scenario == ThermalScenario.ENVIRONMENTAL_CHANGE:
            temp_matrix = self._apply_environmental_change(temp_matrix, elapsed_time)
            
        elif self.current_scenario == ThermalScenario.CALIBRATION_TARGET:
            temp_matrix = self._apply_calibration_target(temp_matrix, elapsed_time)
            
        elif self.current_scenario == ThermalScenario.THERMAL_EVENT:
            temp_matrix = self._apply_thermal_event(temp_matrix, elapsed_time)
            
        return temp_matrix
        
    def _apply_human_subject_pattern(self, temp_matrix: np.ndarray, elapsed_time: float) -> np.ndarray:
        """Apply realistic human thermal signature"""
        state = self.scenario_state
        height, width = temp_matrix.shape
        
        # Get subject position (can include movement)
        if state['movement_pattern'] == 'hand_wave':
            # Simulate hand movement
            offset_x = int(20 * math.sin(elapsed_time * 2))
            pos_x = state['subject_position'][0] + offset_x
            pos_y = state['subject_position'][1]
        else:
            pos_x, pos_y = state['subject_position']
            
        # Create distance matrix from subject center
        y_coords, x_coords = np.ogrid[:height, :width]
        distances = np.sqrt((x_coords - pos_x)**2 + (y_coords - pos_y)**2)
        
        # Apply thermal gradient (body temperature decreasing with distance)
        subject_radius = state['subject_size']
        thermal_signature = np.where(
            distances < subject_radius,
            state['subject_temp'] - (distances / subject_radius) * 10.0,  # 10°C gradient
            temp_matrix
        )
        
        # Add face region (hotter spot)
        face_y = max(0, pos_y - 15)
        face_region = (distances < 12) & (y_coords < face_y + 20) & (y_coords > face_y - 5)
        thermal_signature = np.where(face_region, state['subject_temp'] + 0.8, thermal_signature)
        
        return thermal_signature
        
    def _apply_hot_object_pattern(self, temp_matrix: np.ndarray, elapsed_time: float) -> np.ndarray:
        """Apply hot object with cooling dynamics"""
        state = self.scenario_state
        height, width = temp_matrix.shape
        
        pos_x, pos_y = state['object_position']
        object_size = state['object_size']
        
        # Calculate current temperature with cooling
        if state['cooling_enabled']:
            current_temp = state['object_temp'] * math.exp(-self.cooling_coefficient * elapsed_time)
        else:
            current_temp = state['object_temp']
            
        # Create distance matrix
        y_coords, x_coords = np.ogrid[:height, :width]
        distances = np.sqrt((x_coords - pos_x)**2 + (y_coords - pos_y)**2)
        
        # Apply thermal diffusion pattern
        thermal_signature = np.where(
            distances < object_size,
            current_temp - (distances / object_size) * (current_temp - self.ambient_temperature),
            temp_matrix
        )
        
        return thermal_signature
        
    def _apply_environmental_change(self, temp_matrix: np.ndarray, elapsed_time: float) -> np.ndarray:
        """Apply gradual environmental temperature change"""
        state = self.scenario_state
        
        # Calculate current ambient temperature
        temp_change = state['change_rate'] * elapsed_time
        target_reached = abs(temp_change) >= abs(state['target_temp'] - state['initial_temp'])
        
        if target_reached:
            current_ambient = state['target_temp']
        else:
            current_ambient = state['initial_temp'] + temp_change
            
        # Update ambient temperature
        self.ambient_temperature = current_ambient
        
        # Fill entire matrix with new ambient temperature
        temp_matrix.fill(current_ambient)
        
        return temp_matrix
        
    def _apply_calibration_target(self, temp_matrix: np.ndarray, elapsed_time: float) -> np.ndarray:
        """Apply stable calibration target"""
        state = self.scenario_state
        height, width = temp_matrix.shape
        
        pos_x, pos_y = state['target_position']
        target_size = state['target_size']
        
        # Stable temperature with minimal variation
        target_temp = state['target_temp'] + np.random.normal(0, state['stability'])
        
        # Create circular target
        y_coords, x_coords = np.ogrid[:height, :width]
        distances = np.sqrt((x_coords - pos_x)**2 + (y_coords - pos_y)**2)
        
        target_mask = distances < target_size
        temp_matrix = np.where(target_mask, target_temp, temp_matrix)
        
        return temp_matrix
        
    def _apply_thermal_event(self, temp_matrix: np.ndarray, elapsed_time: float) -> np.ndarray:
        """Apply time-based thermal event"""
        state = self.scenario_state
        
        event_start = state['event_start_time']
        event_end = event_start + state['event_duration']
        
        if event_start <= elapsed_time <= event_end:
            # Event is active
            height, width = temp_matrix.shape
            pos_x, pos_y = state['event_position']
            
            y_coords, x_coords = np.ogrid[:height, :width]
            distances = np.sqrt((x_coords - pos_x)**2 + (y_coords - pos_y)**2)
            
            # Thermal event with rapid temperature rise
            event_radius = 25
            event_mask = distances < event_radius
            temp_matrix = np.where(event_mask, state['event_temp'], temp_matrix)
            
        return temp_matrix
        
    def _apply_hardware_noise(self, temp_matrix: np.ndarray) -> np.ndarray:
        """Apply realistic hardware noise and quantization"""
        # Thermal noise
        noise = np.random.normal(0, self.noise_level, temp_matrix.shape)
        temp_matrix += noise
        
        # Quantization (simulate ADC resolution)
        quantization_step = 0.1  # 0.1°C resolution
        temp_matrix = np.round(temp_matrix / quantization_step) * quantization_step
        
        # Add systematic accuracy error (±2°C specification)
        systematic_error = np.random.uniform(-self.ACCURACY, self.ACCURACY)
        temp_matrix += systematic_error
        
        return temp_matrix
        
    def get_hardware_info(self) -> Dict[str, Any]:
        """Return emulated hardware information"""
        return {
            'device_type': 'TC001 Thermal Camera (Emulated)',
            'resolution': self.RESOLUTION,
            'temperature_range': self.TEMP_RANGE,
            'accuracy': f'±{self.ACCURACY}°C',
            'frame_rate': f'{self.FRAME_RATE} Hz',
            'usb_vid': f'0x{self.USB_VID:04x}',
            'usb_pid': f'0x{self.USB_PID:04x}',
            'emulator_seed': self.seed
        }
        
    def generate_hand_clap_synchronization_test(self, duration: float = 30.0) -> List[ThermalFrame]:
        """
        Generate thermal frames for synchronization testing using hand clap event.
        Creates sharp thermal event at t=15s for cross-modal alignment validation.
        """
        self.start_capture(ThermalScenario.THERMAL_EVENT)
        
        # Configure for hand clap at 15 seconds
        self.scenario_state.update({
            'event_temp': 35.0,  # Hand temperature
            'event_start_time': 15.0,
            'event_duration': 0.5,  # Brief event
            'event_position': (128, 96)  # Center frame
        })
        
        frames = self.generate_sequence(duration)
        
        # Add synchronization metadata
        for frame in frames:
            if 14.9 <= frame.timestamp - self.start_time <= 15.1:
                frame.metadata['sync_event'] = 'hand_clap'
                frame.metadata['sync_confidence'] = 0.95
                
        return frames


# Example usage and testing functions
def demonstrate_tc001_emulator():
    """Demonstration of TC001 emulator capabilities"""
    emulator = TC001ThermalEmulator(seed=42)
    
    print("TC001 Thermal Camera Emulator Demo")
    print("=" * 40)
    
    # Hardware info
    hw_info = emulator.get_hardware_info()
    for key, value in hw_info.items():
        print(f"{key}: {value}")
    print()
    
    # Test different scenarios
    scenarios = [
        (ThermalScenario.HUMAN_SUBJECT, 5.0),
        (ThermalScenario.HOT_OBJECT, 10.0),
        (ThermalScenario.THERMAL_EVENT, 20.0)
    ]
    
    for scenario, duration in scenarios:
        print(f"Testing {scenario.value} scenario for {duration}s...")
        
        emulator.start_capture(scenario)
        frames = emulator.generate_sequence(duration)
        
        # Analysis
        temperatures = [frame.temperature_matrix.mean() for frame in frames]
        min_temp = min(temperatures)
        max_temp = max(temperatures)
        
        print(f"  Generated {len(frames)} frames")
        print(f"  Temperature range: {min_temp:.1f}°C to {max_temp:.1f}°C")
        print(f"  Frame rate: {len(frames)/duration:.1f} Hz")
        print()


if __name__ == "__main__":
    demonstrate_tc001_emulator()