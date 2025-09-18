# IRCamera PC Controller

## Main Application

**`pc_controller.py`** - The unified PC Controller application for multi-modal physiological sensing.

### Usage

```bash
# Run headless demo
python3 pc_controller.py --headless --duration 30

# Run with GUI (requires PyQt6)
python3 pc_controller.py --gui

# Show help
python3 pc_controller.py --help
```

### Features

- **TCP Server**: Handles Android device connections on port 8080
- **Session Management**: Recording session lifecycle with metadata
- **Device Discovery**: Automatic detection of Android sensor nodes
- **Native Backend**: High-performance C++ processing for GSR data
- **GUI Interface**: Optional PyQt6 desktop interface
- **Real-time Processing**: Live data visualization and analysis

### Supporting Files

- `enhanced_tcp_server.py` - Advanced TCP server with TLS support
- `realtime_visualization.py` - PyQtGraph visualization components
- `security_manager.py` - TLS certificate and authentication
- `demo_mvp_components.py` - Component demonstration
- `headless_demo.py` - Alternative headless demo
- `native_backend/` - C++ native processing module

### Dependencies

Core (required):
- Python 3.12+
- loguru

Optional:
- PyQt6 (for GUI)
- numpy (for visualization)
- native_backend (for high-performance processing)

### Architecture

The PC Controller implements a Hub-and-Spoke architecture where:
- **Hub**: PC Controller manages sessions and coordinates devices
- **Spokes**: Android devices provide sensor data streams
- **Communication**: JSON protocol over TCP with optional TLS encryption
- **Processing**: Native C++ backend for performance-critical operations