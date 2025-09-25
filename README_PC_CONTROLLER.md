# IRCamera PC Controller - Unified Implementation

This is the **single, definitive PC controller** for the IRCamera multi-modal recording system. It consolidates all previous implementations into one comprehensive solution.

## Overview

The unified PC controller combines features from all previous implementations:
- Original tkinter GUI (`pc-controller-ui/src/pc_session_controller.py`)
- Standardized protocol handler (`pc-controller/standardized_controller.py`) 
- Enhanced PyQt6 GUI (`pc-controller-ui/src/enhanced_pc_controller.py`)

## Key Features

### 🔄 **Universal Protocol Support**
- **Legacy Text Protocol**: `START_RECORD session_id=abc123`
- **Modern JSON Protocol**: `{"type": "start_recording", "session_id": "abc123"}`
- **Automatic Detection**: Seamlessly handles both formats per device

### 🖥️ **Modern GUI (PyQt6)**
- Real-time GSR plotting with PyQtGraph (100ms updates)
- RGB and thermal camera preview
- Professional device management with color-coded status
- Session control and comprehensive data export
- Tabbed interface for different views

### 🔒 **Security & Networking**  
- SSL/TLS encryption with automatic self-signed certificates
- Multi-device support (up to 10 simultaneous connections)
- Robust error handling and automatic reconnection
- Time synchronization with quality metrics

### ⚡ **Performance**
- Multi-threaded architecture for responsive GUI
- Optional C++ backend integration for intensive processing
- Real-time visualization with configurable time windows
- Memory-efficient data buffering (1000+ samples per device)

### 📊 **Data Management**
- Real-time telemetry collection and visualization
- Session-based recording with unique identifiers
- Export to CSV (GSR data), JSON (device status), TXT (logs)
- Automatic export on session completion

## Installation

### Requirements

**Python 3.7+** with the following packages:

```bash
# Core dependencies
pip install PyQt6 pyqtgraph numpy

# Optional dependencies
pip install Pillow cryptography  # For image display and SSL
```

### Quick Start

```bash
# Clone and run
git clone <repository>
cd IRCamera
python3 pc_controller.py
```

## Usage

### GUI Mode (Default)

```bash
python3 pc_controller.py
```

Features:
- **Device Management**: View connected devices and sensor status
- **Real-time Visualization**: Live GSR plots and camera previews  
- **Session Control**: Start/stop recording across all devices
- **Time Synchronization**: Sync device clocks for accurate timestamps
- **Data Export**: Save session data in multiple formats

### CLI Mode

```bash
python3 pc_controller.py --cli
```

Available commands:
- `start` - Start recording session
- `stop` - Stop recording session  
- `sync` - Synchronize device time
- `quit` - Exit application

### Command Line Options

```bash
python3 pc_controller.py [OPTIONS]

Options:
  --port PORT     Server port (default: 8080)
  --ssl          Enable SSL/TLS encryption
  --cli          Force CLI mode  
  --verbose      Enable verbose logging
```

## Architecture

### Protocol Compatibility

The unified controller automatically detects and handles both protocol formats:

**Legacy Text Protocol** (from original implementations):
```
HELLO device_id=phone1 device_name="Samsung Galaxy"
START_RECORD session_id="session_20231225_120000"
DATA_GSR value=523.4 timestamp=1640995201.5
```

**Modern JSON Protocol** (from enhanced implementations):
```json
{"type": "hello", "device_id": "phone1", "device_name": "Samsung Galaxy"}
{"type": "start_recording", "session_id": "session_20231225_120000"}
{"type": "telemetry_gsr", "value": 523.4, "timestamp": 1640995201.5}
```

### Component Structure

```
pc_controller.py (1,500+ lines)
├── Protocol Class (universal message handling)
│   ├── Legacy text protocol parsing
│   ├── JSON protocol parsing
│   └── Automatic format detection
│
├── DeviceStatus Class (comprehensive device tracking)
│   ├── Multi-sensor status (RGB, Thermal, GSR)
│   ├── Time synchronization metrics
│   └── Real-time data buffering
│
├── NetworkThread Class (high-performance networking)
│   ├── Multi-threaded TCP server
│   ├── SSL/TLS encryption support
│   └── PyQt6 signal integration
│
└── PCController Class (main application)
    ├── PyQt6 GUI with real-time visualization
    ├── CLI mode for headless operation
    ├── Session management and data export
    └── Optional C++ backend integration
```

## Features Comparison

| Feature | Legacy tkinter | Standardized | Enhanced PyQt6 | **Unified** |
|---------|---------------|--------------|----------------|-------------|
| GUI Framework | tkinter | CLI only | PyQt6 | **PyQt6 + CLI** |
| Protocol Support | JSON only | Text only | JSON only | **Both Auto-detect** |
| Real-time Plotting | matplotlib | None | PyQtGraph | **PyQtGraph** |
| SSL/TLS Security | No | No | Yes | **Yes** |
| Multi-device | Limited | Yes | Yes | **Yes** |
| C++ Backend | No | No | Yes | **Optional** |
| Data Export | Basic | None | Advanced | **Advanced** |
| **Lines of Code** | 800 | 580 | 4,800 | **1,500** |

## Configuration

The controller can be configured through the GUI settings or by modifying the config dictionary:

```python
config = {
    'port': 8080,              # Server port
    'use_ssl': False,          # Enable SSL/TLS
    'gsr_plot_window': 30,     # Plot time window (seconds)
    'auto_export': True,       # Auto-export on session end
    'export_format': ['csv', 'json']  # Export formats
}
```

## Data Export

Session data is automatically exported to `exports/session_TIMESTAMP/`:

```
exports/session_20231225_120000/
├── device1_gsr.csv           # GSR telemetry data
├── device1_status.json       # Device status and sync info
├── device2_gsr.csv
├── device2_status.json
└── session_log.txt           # Complete session log
```

## Development

### Adding New Features

1. **Protocol Extensions**: Modify the `Protocol` class to handle new message types
2. **GUI Enhancements**: Add widgets to the appropriate tab in `_create_*_tab()` methods
3. **Data Processing**: Extend `DeviceStatus` for new sensor types
4. **Network Features**: Enhance `NetworkThread` for new connection types

### Testing

```bash
# Test basic functionality
python3 pc_controller.py --verbose

# Test SSL mode
python3 pc_controller.py --ssl --verbose

# Test CLI mode  
python3 pc_controller.py --cli --verbose
```

## Troubleshooting

### Common Issues

**PyQt6 not available**: 
- Install with `pip install PyQt6`
- Use `--cli` flag for command-line operation

**SSL certificate errors**:
- Certificates are auto-generated on first run
- Check `certificates/` directory for `server.crt` and `server.key`

**Connection refused**:
- Check firewall settings for the specified port
- Ensure Android devices are on the same network
- Verify port is not in use by another application

**Plot not updating**:
- Ensure `pyqtgraph` is installed: `pip install pyqtgraph`
- Check device is sending GSR telemetry data

## Future Enhancements

- **Native Webcam Integration**: OpenCV-based PC camera capture
- **Advanced Analytics**: Real-time signal processing and analysis
- **Multi-PC Orchestration**: Coordinate multiple PC controllers
- **Cloud Integration**: Remote session monitoring and data storage
- **Mobile App**: Companion mobile app for remote control

## License

This unified PC controller consolidates all previous implementations and serves as the single source of truth for IRCamera desktop control functionality.