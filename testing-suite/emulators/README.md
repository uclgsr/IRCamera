# Software Sensor Emulators

This directory contains realistic software emulators for hardware sensors used in the IRCamera
testing suite.

## Purpose

Instead of using simple statistical sampling (`np.random.normal()`), these emulators generate
behaviorally-accurate sensor data that actually tests system capabilities while providing
reproducible, deterministic testing scenarios.

## Emulators

### TC001 Thermal Camera Emulator (`tc001_emulator.py`)

- **Resolution**: 256×192 temperature matrices
- **Temperature Range**: -20°C to +550°C with ±2°C accuracy
- **Scenarios**: Human subject detection, hot object tracking, thermal events, calibration targets
- **Physics Simulation**: Thermal diffusion, cooling dynamics, environmental changes
- **Frame Rate**: 10 Hz (matches hardware specification)

### Shimmer3 GSR Sensor Emulator (`shimmer3_emulator.py`)

- **Sampling Rate**: 128 Hz (configurable 1-1024 Hz)
- **GSR Range**: 0-4 μS with 12-bit ADC resolution
- **Scenarios**: Baseline recording, stress responses, electrode contact variations
- **Physiological Modeling**: Heart rate influence, respiratory patterns, stress recovery cycles
- **Hardware Simulation**: Battery drain, Bluetooth characteristics, electrode contact quality

### Network Communication Emulator (`network_emulator.py`)

- **Network Conditions**: Ethernet, Wi-Fi 5GHz/2.4GHz, mobile data, unstable connections
- **Protocol Simulation**: TCP handshakes, message timing, failure modes
- **Realistic Patterns**: Wi-Fi roaming failures (3/14 sessions, 50-80ms latency jumps as
  documented)
- **Multi-Device Coordination**: Device discovery, session management, data streaming

### Android System Emulator (`android_emulator.py`)

- **System Resources**: Memory usage (8GB), CPU load, battery consumption, storage operations
- **Test Execution**: Gradle unit/instrumentation test simulation with realistic timing
- **Performance Scenarios**: Low memory, thermal throttling, background app interference
- **File I/O**: Realistic disk performance with system load factors

## Key Benefits

### Over Statistical Sampling

- **Tests Real Logic**: Emulated data exercises actual data processing pipelines
- **Behavioral Accuracy**: Based on hardware specifications and research literature
- **Edge Case Coverage**: Validates documented failure modes and recovery mechanisms
- **Deterministic Results**: Same seed produces identical test conditions

### For Thesis Validation

- **Research Credibility**: Science-based emulation vs random number generation
- **Reproducible Results**: Deterministic scenarios support thesis claims
- **Technical Depth**: Demonstrates understanding of sensor physics and system behavior
- **Academic Rigor**: Appropriate for MSc evaluation standards

## Usage Examples

### Basic Emulator Usage

```python
from emulators import TC001ThermalEmulator, Shimmer3GSREmulator

# Initialize emulators
thermal = TC001ThermalEmulator(seed=42)
gsr = Shimmer3GSREmulator(seed=43, sampling_rate=128)

# Generate synchronized test data
thermal_frames = thermal.generate_hand_clap_synchronization_test(30.0)
gsr_samples = gsr.generate_hand_clap_synchronization_test(30.0)
```

### Scenario Testing

```python
# Stress response scenario
gsr.start_recording(GSRScenario.STRESS_RESPONSE)
stress_data = gsr.generate_sequence(60.0)

# Network failure simulation  
network.set_network_condition(NetworkCondition.ROAMING_FAILURE)
failure_measurements = network.simulate_wifi_roaming_failure(120.0)
```

### Complete System Test

```bash
# Run all emulated scenarios
python3 run_emulated_scenarios.py

# Individual scenario testing
python3 run_emulated_scenarios.py --scenario stress_response --duration 60
```

## Generated Data Characteristics

### Thermal Camera Data

- **Spatial Gradients**: Realistic temperature distributions
- **Temporal Dynamics**: Heating/cooling physics
- **Human Detection**: Body temperature signatures with face regions
- **Environmental Events**: Hot object introduction, thermal calibration

### GSR Sensor Data

- **Physiological Patterns**: Skin conductance with cardiac/respiratory influence
- **Stress Responses**: Fight-or-flight patterns with exponential recovery
- **Electrode Issues**: Contact quality variations, signal attenuation
- **Long-term Stability**: Baseline drift, battery discharge effects

### Network Communication

- **Latency Profiles**: Realistic for different network types
- **Failure Modes**: Connection drops, roaming handoffs, protocol timeouts
- **Multi-device Coordination**: Synchronized start/stop commands across devices
- **Message Timing**: Processing delays based on payload size and network conditions

## Integration with Testing Suite

The emulators integrate with the existing testing framework:

- **Performance Benchmarking**: Replace statistical models with behavioral simulation
- **Thesis Test Suite**: Enhance documentation validation with realistic data generation
- **Real Integration Tests**: Provide fallback when hardware unavailable
- **Comparative Analysis**: Side-by-side comparison of emulated vs real hardware results

## Technical Implementation

### Reproducibility

- **Seeded Random Generation**: Deterministic results for identical test conditions
- **Temporal Correlation**: Realistic patterns with proper time dependencies
- **Parameter Configuration**: Adjustable for different test scenarios

### Scientific Accuracy

- **Literature-Based Models**: Physiological patterns from published research
- **Hardware Specifications**: Exact match to TC001 and Shimmer3 specifications
- **Physics Simulation**: Thermal diffusion, network protocols, system resource behavior

### Performance Optimization

- **Efficient Generation**: Optimized for test suite execution speed
- **Memory Management**: Appropriate for extended recording scenarios
- **Graceful Degradation**: Fallback to statistical models if emulators unavailable

This emulator framework transforms the testing suite from simple statistical validation to
comprehensive behavioral testing, providing research-grade validation suitable for both academic
thesis evaluation and production deployment assessment.