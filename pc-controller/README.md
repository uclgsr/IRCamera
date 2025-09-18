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
- **Native Backend**: Optional C++ backend for high-performance GSR processing
- **Simple Architecture**: Single file implementation focused on core functionality

### Architecture

The MVP PC Controller implements a simplified Hub-and-Spoke architecture:
- **Hub**: PC Controller manages sessions and receives data from devices  
- **Spokes**: Android devices provide sensor data streams
- **Communication**: JSON protocol over basic TCP connections
- **Processing**: Python-based data handling with optional native C++ backend for performance
- **GSR Processing**: Advanced GSR sensor data processing using native C++ when available

### Native C++ Backend

The implementation includes an optional high-performance C++ backend:

**Build native backend:**
```bash
cd native_backend
mkdir -p build && cd build
cmake .. && make -j2
```

**Features:**
- High-performance GSR data processing
- Native Shimmer device support  
- Real-time sensor data handling
- Automatic Python fallback if native backend unavailable

### Dependencies

**Core (required):**
- Python 3.12+ (standard library only)

**Native Backend (optional):**
- CMake 3.18+
- C++17 compatible compiler
- PyBind11 (auto-downloaded if not found)
- OpenCV (optional, for webcam support)

### Removed Over-engineered Components

The following were removed to focus on MVP functionality:
- Complex TCP server with TLS/SSL
- Real-time visualization with PyQtGraph
- Native C++ backend for performance
- Advanced GUI components
- Certificate management and security layers
- Complex error handling and async patterns

The MVP implementation is ~300 lines in a single file vs 2000+ lines across multiple files.