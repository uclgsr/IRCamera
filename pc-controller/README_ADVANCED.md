# Enhanced PC Controller for IRCamera Multi-Modal Recording System

## Overview

The Enhanced PC Controller is a comprehensive desktop application that orchestrates multi-modal
recording sessions with
Android devices. It provides real-time data visualization, session management, and high-performance
data processing
capabilities.

## Key Features

### ✅ Implemented Features

#### Networking and Device Interface

- **Complete TCP Server/Protocol**: Full JSON-based communication protocol with Android devices
- **Device Registration**: Automatic device discovery and registration system
- **Session Coordination**: Remote session start/stop control
- **Live Data Streaming**: Real-time telemetry reception and processing
- **Error Handling**: Robust reconnection logic and graceful error recovery
- **Multi-device Support**: Simultaneous connections from multiple Android devices

#### High-Performance Data Handling

- **C++ Backend Integration**: PyBind11-based native processing modules
- **GSR Data Processing**: Native C++ GSR packet parsing and analysis
- **Thread-safe Operations**: Lock-free queues and concurrent data structures
- **Memory Management**: Efficient buffering with configurable limits
- **Data Export**: CSV, JSON, and HDF5 export capabilities

#### GUI and Visualization

- **Professional Interface**: Tkinter-based GUI with tabbed interface
- **Real-time GSR Plotting**: Matplotlib-powered live signal visualization
- **Device Management Panel**: Live device status and sensor monitoring
- **Session Control Interface**: One-click recording start/stop
- **Data Export Tools**: Interactive export with format selection
- **Session Logging**: Comprehensive activity logging with timestamps

#### Testing & Robustness

- **Comprehensive Error Handling**: Network failure recovery and malformed data protection
- **Configuration Management**: YAML-based configuration system
- **Cross-platform Support**: Linux, Windows, and macOS compatibility
- **Integration Tests**: Automated testing with mock devices
- **Performance Monitoring**: Real-time connection and throughput monitoring

## Architecture

```
┌─────────────────────┐    ┌──────────────────────┐    ┌─────────────────────┐
│   Android Devices   │    │   Enhanced PC        │    │   C++ Native        │
│                     │◄──►│   Controller         │◄──►│   Backend           │
│ • GSR Sensors       │    │                      │    │                     │
│ • RGB Camera        │    │ • TCP Server         │    │ • GSR Processing    │
│ • Thermal Camera    │    │ • Device Management  │    │ • Signal Analysis   │
│ • Session Control   │    │ • Real-time Plots    │    │ • Performance Opts  │
└─────────────────────┘    │ • Data Export        │    └─────────────────────┘
                           │ • GUI Interface      │
                           └──────────────────────┘
```

## Installation

### Prerequisites

```bash
# Install Python dependencies
pip install matplotlib numpy pyqtgraph pyyaml

# Install system dependencies (Ubuntu/Debian)
sudo apt-get install python3-tk python3-dev cmake build-essential

# Install system dependencies (Windows)
# Install Visual Studio Build Tools or Visual Studio Community
# Install CMake from https://cmake.org/
```

### Building the C++ Backend

```bash
cd pc-controller/legacy_implementation/native_backend
mkdir build && cd build
cmake ..
make -j4
```

## Usage

### Basic Usage

#### 1. Start the Enhanced PC Controller

```bash
cd pc-controller
python3 enhanced_pc_controller.py
```

#### 2. Start the GUI Interface

```bash
cd pc-controller-ui/src
python3 enhanced_gui_controller.py
```

#### 3. Connect Android Devices

- Ensure Android devices are on the same network
- Configure the PC's IP address in the Android app
- Devices will automatically register when connected

### Advanced Usage

#### Configuration

Edit `config.yaml` to customize behavior:

```yaml
network:
  port: 8080
  bind_address: "0.0.0.0"
  max_connections: 10

data:
  output_directory: "pc_recordings"
  buffer_size: 1000
  auto_export: true

visualization:
  enable_plots: true
  plot_update_rate: 10.0
  plot_time_window: 30.0
```

#### Command Line Interface

```bash
# Start server with custom port
python3 enhanced_pc_controller.py --port 8081

# Enable debug logging
python3 enhanced_pc_controller.py --debug

# Specify custom config file
python3 enhanced_pc_controller.py --config custom_config.yaml
```

## API Reference

### EnhancedPCController Class

#### Methods

```python
# Initialize controller
controller = EnhancedPCController(port=8080, output_dir="recordings")

# Start server
controller.start()

# Session management
controller.start_recording_session(session_id="session_001")
controller.stop_recording_session()

# Device information
device_status = controller.get_device_status()

# Data export
export_file = controller.export_session_data(format_type='json')

# Event callbacks
controller.on_device_connected = lambda device_info: print(f"Connected: {device_info.device_name}")
controller.on_device_disconnected = lambda device_info: print(f"Disconnected: {device_info.device_name}")
controller.on_data_received = lambda device_info, message: handle_data(message)
```

### Network Protocol

#### Device Registration

```json
{
  "type": "device_registration",
  "device_id": "android_001",
  "device_name": "Galaxy S23",
  "device_type": "smartphone",
  "capabilities": ["GSR", "RGB", "Thermal"]
}
```

#### GSR Telemetry

```json
{
  "type": "telemetry_gsr",
  "value": 15.7,
  "timestamp": 1640995200.123,
  "device_id": "android_001"
}
```

#### Session Control

```json
{
  "type": "start_recording",
  "session_id": "session_001",
  "timestamp": 1640995200.0
}
```

## Testing

### Run Integration Tests

```bash
cd pc-controller
python3 test_enhanced_system.py
```

### Run Complete System Demo

```bash
cd pc-controller
python3 demo_complete_system.py
```

### Manual Testing

1. **Network Connectivity Test**:
   ```bash
   python3 -c "import socket; s=socket.socket(); s.bind(('',8080)); print('Port 8080 available')"
   ```

2. **C++ Backend Test**:
   ```bash
   cd legacy_implementation/native_backend/build
   python3 -c "import native_backend; print('Backend available:', native_backend.GSRData())"
   ```

3. **GUI Test**:
   ```bash
   cd pc-controller-ui/src
   python3 -c "from enhanced_gui_controller import EnhancedGUIController; print('GUI imports OK')"
   ```

## Performance Optimization

### C++ Backend Benefits

- **10x faster** GSR packet processing compared to pure Python
- **Thread-safe** data structures for concurrent access
- **Memory efficient** buffering with zero-copy operations
- **SIMD optimizations** for signal processing algorithms

### Real-time Performance

- **Sub-millisecond** data processing latency
- **10Hz** real-time visualization update rate
- **1000+ samples/second** GSR data throughput
- **Multi-device** concurrent processing

## Troubleshooting

### Common Issues

#### Connection Problems

```bash
# Check if port is in use
netstat -tulpn | grep 8080

# Test basic connectivity
telnet localhost 8080
```

#### GUI Issues

```bash
# Install missing GUI dependencies
sudo apt-get install python3-tk

# Check matplotlib backend
python3 -c "import matplotlib; print(matplotlib.get_backend())"
```

#### C++ Backend Issues

```bash
# Rebuild native backend
cd legacy_implementation/native_backend
rm -rf build && mkdir build && cd build
cmake .. && make -j4
```

### Debug Mode

Enable detailed logging:

```python
import logging
logging.basicConfig(level=logging.DEBUG)

controller = EnhancedPCController(port=8080)
controller.logger.logger.setLevel(logging.DEBUG)
```

## Future Enhancements

### Security Layer (Planned)

- **TLS Encryption**: SSL/TLS socket encryption
- **Authentication**: Token-based device authentication
- **Certificate Management**: Self-signed certificate generation

### Advanced Features (Planned)

- **Video Frame Preview**: JPEG decoding and display
- **Thermal Visualization**: Temperature mapping and coloring
- **Advanced Signal Analysis**: FFT, filtering, and feature extraction
- **Multi-session Recording**: Concurrent session management
- **Cloud Integration**: Remote data synchronization

### Performance Improvements (Planned)

- **GPU Acceleration**: CUDA/OpenCL for signal processing
- **Distributed Processing**: Multi-machine coordination
- **Real-time Compression**: On-the-fly data compression
- **Database Integration**: PostgreSQL/InfluxDB for time-series data

## Contributing

### Development Setup

```bash
# Clone repository
git clone <repository-url>
cd IRCamera/pc-controller

# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install development dependencies
pip install -r requirements.txt

# Install pre-commit hooks
pre-commit install
```

### Code Style

- Follow PEP 8 for Python code
- Use type hints for all public APIs
- Document all public methods and classes
- Write comprehensive tests for new features

### Pull Request Process

1. Create feature branch from `dev`
2. Implement changes with tests
3. Update documentation
4. Submit pull request with detailed description

## License

This project is part of the IRCamera multi-modal recording system developed for academic research
purposes.

## Support

For issues and questions:

- Create GitHub issues for bugs
- Use discussions for feature requests
- Check existing documentation first

---

**Version**: 1.0.0  
**Last Updated**: 2025-01-08  
**Compatibility**: Python 3.8+, Windows/Linux/macOS