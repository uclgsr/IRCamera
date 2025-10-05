# PC Controller - Quick Start Guide

## Overview

The PC Controller is a desktop application that serves as the central Hub for the IRCamera Multi-Modal Thermal Sensing
Platform. It coordinates multiple Android sensor nodes and provides real-time visualization and data management.

## Prerequisites

### System Requirements

- **OS**: Windows 10+, Ubuntu 20.04+, or macOS 11+
- **Python**: 3.8 or higher
- **RAM**: 4GB minimum (8GB recommended)
- **Storage**: 1GB for installation
- **Network**: Ethernet or Wi-Fi for device communication

### Required Software

- Python 3.8+
- pip (Python package manager)
- C++ compiler (for native backend):
    - Linux: g++ or clang
    - Windows: MSVC or MinGW
    - macOS: Xcode Command Line Tools

## Installation

### 1. Install Python Dependencies

```bash
cd pc-controller
pip install -r requirements.txt
```

Key dependencies:

- PyQt6: GUI framework
- pyqtgraph: High-performance plotting
- numpy: Numerical computing
- opencv-python: Webcam capture
- cryptography: SSL/TLS security
- pybind11: C++ Python bindings

### 2. Build Native Backend (Optional but Recommended)

The C++ native backend provides 10-100x performance improvement for data processing.

```bash
cd native_backend
python3 setup.py build_ext --inplace
cd ..
```

This creates `enhanced_native_backend.so` (Linux/Mac) or `enhanced_native_backend.pyd` (Windows).

**Note**: If native backend build fails, the controller will automatically fall back to Python processing.

### 3. Verify Installation

Run the test suite:

```bash
python3 test_pc_controller_features.py
```

Expected output: 13/13 tests passing

Run the feature demonstration:

```bash
python3 demo_features.py
```

Expected output: 5/5 features demonstrated successfully

## Running the PC Controller

### GUI Mode (Recommended)

```bash
python3 pc_controller.py
```

This launches the full PyQt6 GUI with:

- Real-time GSR plotting
- Camera preview (RGB and Thermal)
- Device management panel
- Session control
- Data export

### CLI Mode

```bash
python3 pc_controller.py --port 8080
```

Options:

- `--port PORT`: Server port (default: 8080)
- `--ssl`: Enable SSL/TLS encryption
- `--output-dir DIR`: Data output directory
- `--no-gui`: Run without GUI (headless mode)

### TLS Server Mode

For secure connections with Android devices, enable SSL/TLS in the GUI:

1. Start the PC Controller GUI: `python3 pc_controller.py`
2. Check the "Enable SSL/TLS" checkbox
3. Set port to 8443 (or desired secure port)
4. Click "Start Server"

Alternatively, use CLI mode with SSL:

```bash
python3 pc_controller.py --port 8443 --ssl
```

This will:

1. Generate self-signed certificates (if not present in `certificates/` directory)
2. Start secure TLS server on specified port
3. Accept encrypted connections from Android devices

## Configuration

### Network Configuration

Edit `config.yaml`:

```yaml
network:
  port: 8080
  use_ssl: false
  bind_address: "0.0.0.0"  # Listen on all interfaces
```

Or configure via GUI:

1. Click "Network Settings" in the device panel
2. Set port (1024-65535)
3. Check "Enable SSL/TLS" for secure connections
4. Click "Restart Server"

### Visualization Settings

```yaml
visualization:
  gsr_time_window: 60      # seconds
  update_rate: 10          # Hz
  max_samples: 1000        # buffer size per device
```

Or adjust in GUI:

- Use "Time Window" slider in GSR plot tab
- Toggle "Auto Scale" for automatic Y-axis scaling

## Basic Usage

### 1. Start the PC Controller

```bash
python3 pc_controller.py
```

The GUI will launch. Click "Start Server" to begin listening for device connections on port 8080.

### 2. Connect Android Devices

On the Android app:

1. Go to Settings → Network
2. Enter PC IP address (e.g., 192.168.1.100)
3. Enter port (8080)
4. Tap "Connect"

The device should appear in the "Connected Devices" panel.

### 3. Start Recording Session

1. Verify devices are connected (green status)
2. Click "Start All Recording"
3. Watch real-time GSR data in the plot
4. Camera frames appear in the preview tabs

### 4. Monitor Session

- **GSR Plot**: Shows real-time galvanic skin response
- **Device Tree**: Shows status, battery, frame counts
- **Session Log**: Records all events with timestamps

### 5. Stop Recording

1. Click "Stop All Recording"
2. Recording ends on all devices
3. Click "Export Session Data"
4. Choose export directory
5. Data is saved as CSV (GSR) and JSON (status)

## Features

### 1. Real-Time Visualization

- High-performance GSR plotting (PyQtGraph)
- Update rate: 10 Hz
- Multi-device support with color-coded curves
- Camera preview for RGB and thermal images

### 2. Data Processing

- C++ native backend for high-performance processing
- Digital filtering (lowpass, highpass, notch)
- Statistical analysis (mean, std, RMS)
- Artifact detection

### 3. Network Communication

- JSON-based protocol
- SSL/TLS encryption support
- Multi-device concurrent connections
- Time synchronization
- Heartbeat monitoring

### 4. Data Export

- CSV format for GSR data
- JSON format for device status
- Session logs as text files
- Organized export directories

### 5. Security

- Self-signed certificate generation
- TLS 1.2+ encryption
- Secure message handling
- Certificate validation

## Troubleshooting

### Native Backend Build Fails

**Symptoms**: Error during `python3 setup.py build_ext`

**Solutions**:

1. Install C++ compiler:
    - Linux: `sudo apt-get install build-essential`
    - Windows: Install Visual Studio with C++ tools
    - macOS: `xcode-select --install`

2. Install pybind11: `pip install pybind11`

3. If build still fails, controller works without native backend (Python fallback)

### PyQt6 Import Error

**Symptoms**: `ImportError: libEGL.so.1: cannot open shared object file`

**Solutions** (Linux):

```bash
sudo apt-get install libegl1 libgl1 libxkbcommon-x11-0 libxcb-cursor0
```

For headless servers:

```bash
xvfb-run python3 pc_controller.py
# Or use CLI mode
python3 pc_controller.py --no-gui
```

### No Devices Connecting

**Symptoms**: Android devices cannot connect to PC

**Checklist**:

1. PC and Android on same network
2. Firewall allows incoming connections on port 8080
3. Correct IP address entered on Android
4. PC Controller server is running (check status bar)

**Find PC IP Address**:

- Linux/Mac: `ip addr` or `ifconfig`
- Windows: `ipconfig`
- Look for address like 192.168.1.x

**Test Connection**:

```bash
# On PC
python3 pc_controller.py --port 8080

# On Android or another PC
telnet <PC_IP> 8080
# Should connect if server is accessible
```

### SSL Certificate Issues

**Symptoms**: SSL handshake failures

**Solutions**:

1. Delete old certificates:
   ```bash
   rm -rf certificates/
   ```

2. Restart PC Controller to generate new certificates

3. On Android, trust the new certificate (settings may vary)

### Performance Issues

**Symptoms**: Slow GUI, dropped frames, high CPU

**Solutions**:

1. Reduce time window: Settings → GSR time window → 30s
2. Lower update rate: Edit config.yaml → visualization.update_rate: 5
3. Build native backend for better performance
4. Close other applications
5. Reduce number of connected devices

## Advanced Usage

### Custom Message Handler

```python
from pc_controller import PCController

controller = PCController(port=8080)

def custom_gsr_handler(device_id, value, timestamp):
    print(f"Custom handler: {device_id} = {value} μS")
    # Your custom processing here

controller.on_gsr_data = custom_gsr_handler
controller.start_server()
```

### Using Native Backend Directly

```python
import sys
sys.path.insert(0, 'native_backend')
import enhanced_native_backend

# Create GSR data
gsr_data = enhanced_native_backend.GSRData()
gsr_data.gsr_microsiemens = 5.5

# Apply filtering
data = [1.0, 2.0, 3.0, 4.0, 5.0]
filtered = enhanced_native_backend.processing.apply_lowpass_filter(
    data, cutoff_hz=5.0, sample_rate=128.0
)
```

### Multi-PC Setup

Run multiple PC controllers for redundancy:

**PC 1 (Primary)**:

```bash
python3 pc_controller.py --port 8080
```

**PC 2 (Backup)**:

```bash
python3 pc_controller.py --port 8081
```

Configure Android devices to connect to both PCs.

## Testing

### Run All Tests

```bash
python3 test_pc_controller_features.py
```

Tests:

- Native backend integration
- Network protocol handling
- Data export functionality
- Webcam integration
- Security features

### Run Demonstrations

```bash
python3 demo_features.py
```

Demonstrates all implemented features with sample data.

### Manual Testing

1. **Network Test**:
   ```bash
   # Terminal 1: Start server
   python3 pc_controller.py --port 8080
   
   # Terminal 2: Send test message
   echo '{"type":"HELLO","device_id":"test"}' | nc localhost 8080
   ```

2. **Data Export Test**:
    - Start GUI
    - Wait for some data (or use demo data)
    - Click "Export Session Data"
    - Verify files in export directory

3. **Performance Test**:
    - Connect 2-4 devices
    - Start recording
    - Monitor CPU usage (`top` or Task Manager)
    - Check GUI responsiveness

## Documentation

- **Implementation Guide**: `implementation.md` - Complete feature documentation
- **README**: `README.md` - Project overview and architecture
- **API Reference**: See Python docstrings in source files
- **Network Protocol**: See `legacy_implementation/src/ircamera_pc/network/protocol.py`

## Support

### Common Issues

| Issue              | Solution                                 |
|--------------------|------------------------------------------|
| Build errors       | Check C++ compiler installation          |
| Import errors      | Install missing dependencies with pip    |
| Connection issues  | Check firewall and network configuration |
| Performance issues | Build native backend, reduce time window |
| SSL errors         | Delete and regenerate certificates       |

### Logs

Check logs for detailed error information:

- GUI session log (bottom panel)
- Terminal output (if running from command line)
- System logs: `/var/log/` (Linux) or Event Viewer (Windows)

### Getting Help

1. Check this Quick Start Guide
2. Review `implementation.md`
3. Run diagnostics: `python3 demo_features.py`
4. Check test results: `python3 test_pc_controller_features.py`

## Next Steps

1. **For Thesis**: See `implementation.md` for complete feature documentation
2. **For Development**: See `README.md` for architecture and design decisions
3. **For Testing**: Run `test_pc_controller_features.py` and `demo_features.py`
4. **For Deployment**: Configure `config.yaml` for production settings

## Summary

The PC Controller provides:
Real-time multi-modal data visualization  
High-performance C++ backend  
Secure SSL/TLS communication  
Multi-device session management  
Data export and aggregation  
Cross-platform support  
Comprehensive testing

Minimum setup:

```bash
pip install -r requirements.txt
python3 pc_controller.py
```

Full setup (recommended):

```bash
pip install -r requirements.txt
cd native_backend && python3 setup.py build_ext --inplace && cd ..
python3 tests/test_pc_controller_features.py
python3 pc_controller.py
```
