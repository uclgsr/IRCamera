# IRCamera PC Controller - MVP

## Main Application

**`pc_controller.py`** - The MVP PC Controller application for multi-modal physiological sensing.

### Usage

```bash
# Run MVP demo (default 30 seconds)
python3 pc_controller.py

# Run for specific duration
python3 pc_controller.py --duration 60

# Show help
python3 pc_controller.py --help
```

### Core MVP Features

- **TCP Server**: Handles Android device connections on port 8080
- **Device Registration**: Simple device discovery and registration
- **Session Management**: Basic recording session lifecycle
- **Data Logging**: GSR and sensor data reception with logging
- **Simple Architecture**: Single file implementation focused on core functionality

### Architecture

The MVP PC Controller implements a simplified Hub-and-Spoke architecture:
- **Hub**: PC Controller manages sessions and receives data from devices  
- **Spokes**: Android devices provide sensor data streams
- **Communication**: JSON protocol over basic TCP connections
- **Processing**: Simple Python-based data handling and logging

### Dependencies

Core (required):
- Python 3.12+ (standard library only)

Optional:
- Custom logging libraries (defaults to standard logging)

### Removed Over-engineered Components

The following were removed to focus on MVP functionality:
- Complex TCP server with TLS/SSL
- Real-time visualization with PyQtGraph
- Native C++ backend for performance
- Advanced GUI components
- Certificate management and security layers
- Complex error handling and async patterns

The MVP implementation is ~300 lines in a single file vs 2000+ lines across multiple files.