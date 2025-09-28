"""
Shimmer3 GSR Sensor Software Emulator

Generates physiologically-realistic GSR (Galvanic Skin Response) data
for testing without requiring physical Shimmer3 hardware.
"""

import numpy as np
import time
import math
from typing import Dict, List, Tuple, Optional, Any
from dataclasses import dataclass
from enum import Enum


class GSRScenario(Enum):
    """Predefined GSR recording scenarios for testing"""
    BASELINE_RECORDING = "baseline"
    STRESS_RESPONSE = "stress_response"
    ELECTRODE_CONTACT_VARIATION = "contact_variation"
    CALIBRATION = "calibration"
    EXTENDED_RECORDING = "extended"


@dataclass
class GSRSample:
    """Single GSR sample with metadata"""
    timestamp: float
    conductance_us: float  # microsiemens (μS)
    raw_adc: int  # 12-bit ADC value
    sample_id: int
    electrode_contact_quality: float  # 0.0-1.0
    battery_level: float  # 0.0-1.0
    metadata: Dict[str, Any] = None


class Shimmer3GSREmulator:
    """
    Software emulator for Shimmer3 GSR sensor
    
    Generates physiologically-realistic GSR data with proper:
    - Frequency characteristics
    - Stress response patterns
    - Electrode contact variations
    - Hardware-accurate specifications
    """
    
    # Shimmer3 Hardware Specifications
    SAMPLING_RATE_HZ = 128  # Default sampling rate
    ADC_RESOLUTION = 12  # bits
    ADC_MAX_VALUE = 4095  # 2^12 - 1
    GSR_RANGE = (0.0, 4.0)  # microsiemens (μS)
    BATTERY_LIFE_HOURS = 12  # Continuous operation
    BLE_MAC_PREFIX = "00:06:66"  # Shimmer MAC prefix
    
    def __init__(self, seed: int = 42, sampling_rate: int = 128):
        """Initialize GSR emulator with reproducible random seed"""
        self.seed = seed
        self.sampling_rate = sampling_rate
        np.random.seed(seed)
        
        # Current state
        self.sample_counter = 0
        self.start_time = None
        self.current_scenario = None
        
        # Physiological parameters
        self.baseline_conductance = 1.2  # μS (typical resting value)
        self.current_conductance = self.baseline_conductance
        self.electrode_contact_quality = 1.0
        self.battery_level = 1.0
        
        # Scenario-specific state
        self.scenario_state = {}
        
        # Physiological signal generation
        self.noise_amplitude = 0.02  # μS
        self.drift_rate = 0.001  # μS per second
        
    def start_recording(self, scenario: GSRScenario = GSRScenario.BASELINE_RECORDING):
        """Start GSR recording with specified scenario"""
        self.start_time = time.time()
        self.sample_counter = 0
        self.current_scenario = scenario
        self.scenario_state = self._initialize_scenario(scenario)
        
    def generate_sample(self) -> GSRSample:
        """Generate single realistic GSR sample"""
        if self.start_time is None:
            raise RuntimeError("Must call start_recording() before generating samples")
            
        current_time = time.time()
        elapsed = current_time - self.start_time
        timestamp = self.start_time + (self.sample_counter / self.sampling_rate)
        
        # Generate conductance value based on scenario
        conductance = self._generate_conductance_value(elapsed)
        
        # Convert to ADC value
        raw_adc = self._conductance_to_adc(conductance)
        
        # Update battery level (discharge over time)
        self.battery_level = max(0.0, 1.0 - (elapsed / (self.BATTERY_LIFE_HOURS * 3600)))
        
        sample = GSRSample(
            timestamp=timestamp,
            conductance_us=conductance,
            raw_adc=raw_adc,
            sample_id=self.sample_counter,
            electrode_contact_quality=self.electrode_contact_quality,
            battery_level=self.battery_level,
            metadata={
                'scenario': self.current_scenario.value,
                'elapsed_time': elapsed,
                'sampling_rate': self.sampling_rate,
                'emulator_seed': self.seed
            }
        )
        
        self.sample_counter += 1
        return sample
        
    def generate_sequence(self, duration_seconds: float) -> List[GSRSample]:
        """Generate sequence of GSR samples over specified duration"""
        samples = []
        num_samples = int(duration_seconds * self.sampling_rate)
        
        for i in range(num_samples):
            sample = self.generate_sample()
            samples.append(sample)
            
        return samples
        
    def _initialize_scenario(self, scenario: GSRScenario) -> Dict[str, Any]:
        """Initialize scenario-specific parameters"""
        if scenario == GSRScenario.BASELINE_RECORDING:
            return {
                'target_conductance': self.baseline_conductance,
                'stability': 0.05,  # μS variation
                'drift_enabled': True
            }
            
        elif scenario == GSRScenario.STRESS_RESPONSE:
            return {
                'stress_onset_time': 15.0,  # seconds
                'stress_peak_time': 20.0,
                'recovery_time': 45.0,
                'stress_amplitude': 2.0,  # μS increase
                'recovery_tau': 25.0  # recovery time constant
            }
            
        elif scenario == GSRScenario.ELECTRODE_CONTACT_VARIATION:
            return {
                'good_contact_periods': [(0, 30), (60, 90)],
                'poor_contact_periods': [(30, 45), (90, 120)],
                'contact_quality_variation': 0.3
            }
            
        elif scenario == GSRScenario.CALIBRATION:
            return {
                'calibration_levels': [0.5, 1.0, 2.0, 3.0],  # μS
                'level_duration': 30.0,  # seconds per level
                'settling_time': 5.0  # seconds for stabilization
            }
            
        elif scenario == GSRScenario.EXTENDED_RECORDING:
            return {
                'duration_hours': 2.0,
                'stress_events': [
                    {'time': 1800, 'amplitude': 1.5},  # 30 min
                    {'time': 3600, 'amplitude': 2.2},  # 60 min
                    {'time': 5400, 'amplitude': 1.8}   # 90 min
                ],
                'drift_rate': 0.002  # Increased drift for long recording
            }
            
        return {}
        
    def _generate_conductance_value(self, elapsed_time: float) -> float:
        """Generate realistic conductance value based on current scenario"""
        base_conductance = self.baseline_conductance
        
        if self.current_scenario == GSRScenario.BASELINE_RECORDING:
            conductance = self._apply_baseline_pattern(base_conductance, elapsed_time)
            
        elif self.current_scenario == GSRScenario.STRESS_RESPONSE:
            conductance = self._apply_stress_response_pattern(base_conductance, elapsed_time)
            
        elif self.current_scenario == GSRScenario.ELECTRODE_CONTACT_VARIATION:
            conductance = self._apply_contact_variation_pattern(base_conductance, elapsed_time)
            
        elif self.current_scenario == GSRScenario.CALIBRATION:
            conductance = self._apply_calibration_pattern(base_conductance, elapsed_time)
            
        elif self.current_scenario == GSRScenario.EXTENDED_RECORDING:
            conductance = self._apply_extended_recording_pattern(base_conductance, elapsed_time)
            
        else:
            conductance = base_conductance
            
        # Add physiological noise
        conductance = self._apply_physiological_noise(conductance, elapsed_time)
        
        # Apply electrode contact quality effects
        conductance = self._apply_electrode_effects(conductance)
        
        # Ensure within valid range
        conductance = np.clip(conductance, self.GSR_RANGE[0], self.GSR_RANGE[1])
        
        return conductance
        
    def _apply_baseline_pattern(self, base_conductance: float, elapsed_time: float) -> float:
        """Apply baseline recording pattern with natural drift"""
        state = self.scenario_state
        
        # Natural baseline drift
        if state['drift_enabled']:
            drift = self.drift_rate * elapsed_time
        else:
            drift = 0.0
            
        # Small random variations
        variation = np.random.normal(0, state['stability'])
        
        return base_conductance + drift + variation
        
    def _apply_stress_response_pattern(self, base_conductance: float, elapsed_time: float) -> float:
        """Apply realistic stress response pattern"""
        state = self.scenario_state
        
        stress_onset = state['stress_onset_time']
        stress_peak = state['stress_peak_time']
        recovery_time = state['recovery_time']
        amplitude = state['stress_amplitude']
        tau = state['recovery_tau']
        
        if elapsed_time < stress_onset:
            # Pre-stress baseline
            stress_component = 0.0
            
        elif elapsed_time < stress_peak:
            # Rapid rise phase
            rise_progress = (elapsed_time - stress_onset) / (stress_peak - stress_onset)
            stress_component = amplitude * rise_progress
            
        elif elapsed_time < recovery_time:
            # Recovery phase (exponential decay)
            recovery_elapsed = elapsed_time - stress_peak
            stress_component = amplitude * math.exp(-recovery_elapsed / tau)
            
        else:
            # Back to baseline
            stress_component = 0.0
            
        return base_conductance + stress_component
        
    def _apply_contact_variation_pattern(self, base_conductance: float, elapsed_time: float) -> float:
        """Apply electrode contact quality variations"""
        state = self.scenario_state
        
        # Determine current contact quality
        contact_quality = 1.0  # Default good contact
        
        for start, end in state['poor_contact_periods']:
            if start <= elapsed_time <= end:
                contact_quality = 0.3  # Poor contact
                break
                
        # Update electrode contact quality
        self.electrode_contact_quality = contact_quality + np.random.normal(0, 0.1)
        self.electrode_contact_quality = np.clip(self.electrode_contact_quality, 0.0, 1.0)
        
        # Poor contact causes signal attenuation and noise
        if contact_quality < 0.5:
            attenuation = 0.7
            extra_noise = 0.1 * (1.0 - contact_quality)
        else:
            attenuation = 1.0
            extra_noise = 0.0
            
        noise = np.random.normal(0, extra_noise)
        
        return (base_conductance * attenuation) + noise
        
    def _apply_calibration_pattern(self, base_conductance: float, elapsed_time: float) -> float:
        """Apply step calibration pattern"""
        state = self.scenario_state
        
        levels = state['calibration_levels']
        level_duration = state['level_duration']
        settling_time = state['settling_time']
        
        # Determine current calibration level
        level_index = int(elapsed_time / level_duration)
        if level_index >= len(levels):
            level_index = len(levels) - 1
            
        target_level = levels[level_index]
        
        # Time within current level
        time_in_level = elapsed_time % level_duration
        
        if time_in_level < settling_time:
            # Settling to new level (exponential approach)
            if level_index > 0:
                previous_level = levels[level_index - 1]
                settling_progress = 1.0 - math.exp(-time_in_level / (settling_time / 3))
                current_level = previous_level + (target_level - previous_level) * settling_progress
            else:
                current_level = target_level
        else:
            # Stable at target level
            current_level = target_level
            
        return current_level
        
    def _apply_extended_recording_pattern(self, base_conductance: float, elapsed_time: float) -> float:
        """Apply extended recording with multiple stress events"""
        state = self.scenario_state
        
        # Apply gradual drift over time
        drift = state['drift_rate'] * elapsed_time
        
        # Check for stress events
        stress_component = 0.0
        for event in state['stress_events']:
            event_time = event['time']
            event_amplitude = event['amplitude']
            
            if abs(elapsed_time - event_time) < 60:  # Within 1 minute of event
                # Stress response with decay
                time_since_event = elapsed_time - event_time
                if time_since_event >= 0:
                    stress_component += event_amplitude * math.exp(-time_since_event / 30.0)
                    
        return base_conductance + drift + stress_component
        
    def _apply_physiological_noise(self, conductance: float, elapsed_time: float) -> float:
        """Add realistic physiological noise"""
        # High-frequency cardiac influence (~1 Hz)
        cardiac_influence = 0.01 * math.sin(2 * math.pi * elapsed_time)
        
        # Respiratory influence (~0.25 Hz)  
        respiratory_influence = 0.02 * math.sin(2 * math.pi * elapsed_time * 0.25)
        
        # Random thermal noise
        thermal_noise = np.random.normal(0, self.noise_amplitude)
        
        return conductance + cardiac_influence + respiratory_influence + thermal_noise
        
    def _apply_electrode_effects(self, conductance: float) -> float:
        """Apply electrode contact quality effects"""
        if self.electrode_contact_quality < 0.8:
            # Poor contact causes signal distortion
            distortion = np.random.uniform(0.9, 1.1)  # ±10% variation
            conductance *= distortion
            
        return conductance
        
    def _conductance_to_adc(self, conductance: float) -> int:
        """Convert conductance to 12-bit ADC value"""
        # Normalize to ADC range
        normalized = (conductance - self.GSR_RANGE[0]) / (self.GSR_RANGE[1] - self.GSR_RANGE[0])
        adc_value = int(normalized * self.ADC_MAX_VALUE)
        
        # Add ADC quantization noise
        adc_noise = np.random.randint(-2, 3)  # ±2 LSB noise
        adc_value = np.clip(adc_value + adc_noise, 0, self.ADC_MAX_VALUE)
        
        return adc_value
        
    def get_hardware_info(self) -> Dict[str, Any]:
        """Return emulated hardware information"""
        mac_address = f"{self.BLE_MAC_PREFIX}:{self.seed:02d}:{self.seed+1:02d}:{self.seed+2:02d}"
        
        return {
            'device_type': 'Shimmer3 GSR+ (Emulated)',
            'sampling_rate': f'{self.sampling_rate} Hz',
            'gsr_range': f'{self.GSR_RANGE[0]}-{self.GSR_RANGE[1]} μS',
            'adc_resolution': f'{self.ADC_RESOLUTION} bits',
            'battery_life': f'{self.BATTERY_LIFE_HOURS} hours',
            'ble_mac_address': mac_address,
            'firmware_version': 'BoilerPlate 0.1.0 (Emulated)',
            'emulator_seed': self.seed
        }
        
    def get_current_status(self) -> Dict[str, Any]:
        """Return current device status"""
        return {
            'battery_level': f'{self.battery_level:.1%}',
            'electrode_contact_quality': f'{self.electrode_contact_quality:.1%}',
            'current_conductance': f'{self.current_conductance:.2f} μS',
            'samples_generated': self.sample_counter,
            'recording_active': self.start_time is not None
        }
        
    def generate_hand_clap_synchronization_test(self, duration: float = 30.0) -> List[GSRSample]:
        """
        Generate GSR samples for synchronization testing using hand clap event.
        Creates sharp conductance spike at t=15s for cross-modal alignment validation.
        """
        # Custom scenario for synchronization testing
        self.start_recording(GSRScenario.STRESS_RESPONSE)
        
        # Configure for hand clap at 15 seconds
        self.scenario_state.update({
            'stress_onset_time': 14.9,
            'stress_peak_time': 15.1,
            'recovery_time': 20.0,
            'stress_amplitude': 0.8,  # Sharp but brief response
            'recovery_tau': 5.0
        })
        
        samples = self.generate_sequence(duration)
        
        # Add synchronization metadata
        for sample in samples:
            elapsed = sample.timestamp - self.start_time
            if 14.8 <= elapsed <= 15.2:
                sample.metadata['sync_event'] = 'hand_clap'
                sample.metadata['sync_confidence'] = 0.92


        return samples


# Example usage and testing functions
def demonstrate_shimmer3_emulator():
    """Demonstration of Shimmer3 emulator capabilities"""
    emulator = Shimmer3GSREmulator(seed=42, sampling_rate=128)
    
    print("Shimmer3 GSR Sensor Emulator Demo")
    print("=" * 40)
    
    # Hardware info
    hw_info = emulator.get_hardware_info()
    for key, value in hw_info.items():
        print(f"{key}: {value}")
    print()
    
    # Test different scenarios
    scenarios = [
        (GSRScenario.BASELINE_RECORDING, 10.0),
        (GSRScenario.STRESS_RESPONSE, 60.0),
        (GSRScenario.ELECTRODE_CONTACT_VARIATION, 30.0)
    ]
    
    for scenario, duration in scenarios:
        print(f"Testing {scenario.value} scenario for {duration}s...")
        
        emulator.start_recording(scenario)
        samples = emulator.generate_sequence(duration)
        
        # Analysis
        conductances = [sample.conductance_us for sample in samples]
        contact_qualities = [sample.electrode_contact_quality for sample in samples]
        
        print(f"  Generated {len(samples)} samples")
        print(f"  Conductance range: {min(conductances):.2f} to {max(conductances):.2f} μS")
        print(f"  Average contact quality: {np.mean(contact_qualities):.1%}")
        print(f"  Sampling rate: {len(samples)/duration:.1f} Hz")
        
        # Status check
        status = emulator.get_current_status()
        print(f"  Final battery level: {status['battery_level']}")
        print()


if __name__ == "__main__":
    demonstrate_shimmer3_emulator()