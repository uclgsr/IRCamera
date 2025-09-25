# Unified PC Session Controller

This document describes the **Unified PC Session Controller** - a single, comprehensive implementation that merges all previous PC controller implementations into one cohesive application.

## What Was Unified

The unified controller combines the best features from three separate implementations:

### 1. Original tkinter Controller (`pc_session_controller.py`)
- **Features Used**: Basic GUI layout, device status tracking, session management
- **GUI Framework**: tkinter (basic but functional)
- **Network**: Simple TCP server
- **Visualization**: matplotlib (slow but adequate)

### 2. Standardized Protocol Controller (`standardized_controller.py`) 
- **Features Used**: Robust protocol handling, message parsing, time synchronization
- **Network**: Advanced TCP with protocol definitions  
- **Protocol**: Support for both legacy and JSON message formats
- **Device Management**: Connection lifecycle management

### 3. Enhanced PyQt6 Controller (`enhanced_pc_controller.py`)
- **Features Used**: Modern GUI, high-performance plotting, C++ backend integration
- **GUI Framework**: PyQt6 (professional and responsive)
- **Visualization**: PyQtGraph (high-performance real-time plotting)
- **Security**: SSL/TLS encryption support
- **Backend**: C++ integration for intensive processing

## Unified Implementation Features

### 🔄 **Unified Protocol Support**
```python
class Protocol:
    """Supports both legacy text protocol and modern JSON format"""
    
    # Legacy format: "START_RECORD session_id=session_123"
    # JSON format: {"type": "start_recording", "session_id": "session_123"}
    
    @staticmethod
    def parse_message(message: str) -> Optional[Dict[str, Any]]:
        # Automatically detects and handles both formats
```

### 🖥️ **Modern PyQt6 GUI**
- **Real-time GSR plotting** with PyQtGraph (100ms updates)
- **RGB and thermal image preview** with ImageView widgets
- **Professional device management** with tree view and color coding
- **Tabbed visualization** for different data types
- **Session management** with timer and export functionality

### 🔒 **Security Layer**
- **SSL/TLS encryption** with automatic self-signed certificate generation
- **Secure device communication** for production environments
- **Certificate management** in `certificates/` directory

### ⚡ **High-Performance Backend**
- **C++ integration** via PyBind11 for intensive data processing
- **Real-time signal filtering** (lowpass, highpass, notch filters)
- **Statistical analysis** (mean, variance, RMS, signal quality)
- **Fallback to Python** when C++ backend unavailable

### 📊 **Comprehensive Data Management**
- **Multi-device support** (up to 10 simultaneous connections)
- **Real-time data buffering** with configurable time windows
- **Session export** to CSV, JSON, and log formats
- **Device status tracking** with protocol version detection

## Architecture

```
unified_pc_controller.py (1,200+ lines)
├── Protocol Class
│   ├── Legacy message parsing (START_RECORD, SYNC_REQUEST, etc.)
│   ├── JSON message parsing (start_recording, telemetry_gsr, etc.)
│   └── Unified message creation and validation
│
├── DeviceStatus Class  
│   ├── Enhanced sensor tracking (RGB, Thermal, GSR)
│   ├── Time synchronization management
│   ├── Capability and firmware tracking
│   └── Real-time data buffers
│
├── NetworkThread Class
│   ├── Multi-threaded TCP server
│   ├── SSL/TLS encryption support
│   ├── Protocol-agnostic message handling
│   └── PyQt6 signal emission for GUI updates
│
└── UnifiedPCController Class (Main GUI)
    ├── Modern PyQt6 interface
    ├── Real-time visualization with PyQtGraph
    ├── Device management and session control
    ├── C++ backend integration
    └── Data export and logging
```

## Installation & Usage

### Prerequisites
```bash
# Required dependencies
pip install PyQt6 pyqtgraph numpy matplotlib pillow

# Optional for C++ backend
pip install pybind11

# Optional for SSL certificate generation
pip install cryptography
```

### Running the Unified Controller
```bash
cd pc-controller-ui/src
python3 unified_pc_controller.py
```

### Features Available

**Device Connection:**
- Automatic device detection via HELLO messages
- Support for both legacy and JSON protocols
- Real-time device status with color coding
- Individual device control capabilities

**Session Management:**
- Start/stop recording on all devices simultaneously
- Individual device recording control
- Session timing and status tracking
- Comprehensive session logging

**Real-time Visualization:**
- High-performance GSR plotting (10Hz updates)
- RGB and thermal camera preview
- Configurable time windows (10-300 seconds)
- Auto-scaling and manual scale control

**Data Processing:**
- Optional C++ backend for signal processing
- Real-time filtering and artifact detection
- Statistical analysis and quality metrics
- Python fallback when C++ unavailable

**Network Security:**
- SSL/TLS encryption support
- Automatic self-signed certificate generation
- Secure multi-device communication
- Configurable port and security settings

**Data Export:**
- Session data to CSV format (GSR values with timestamps)
- Device status to JSON format
- Session logs to text format
- Organized export with metadata

## Protocol Compatibility

The unified controller supports multiple protocol versions:

### Legacy Text Protocol (from standardized_controller.py)
```
HELLO device_id="android_123" sensors="GSR,RGB,Thermal"
START_RECORD session_id="session_20240101_120000"
DATA_GSR value=523.4 ts=1640995201500
SYNC_REQUEST t_pc=1640995200000
```

### Modern JSON Protocol (from enhanced_pc_controller.py)
```json
{"type": "HELLO", "device_id": "android_123", "sensors": ["GSR", "RGB", "Thermal"]}
{"type": "start_recording", "session_id": "session_20240101_120000", "timestamp": 1640995200.0}
{"type": "telemetry_gsr", "value": 523.4, "timestamp": 1640995201.5}
{"type": "sync_request", "pc_timestamp": 1640995200000}
```

### Automatic Protocol Detection
The unified controller automatically detects which protocol format is being used and responds appropriately, ensuring compatibility with all existing Android app versions.

## Benefits of Unification

### 1. **Single Codebase**
- **Maintenance**: Only one PC controller to maintain and update
- **Features**: All features available in one application
- **Testing**: Single test suite covers all functionality

### 2. **Protocol Flexibility**
- **Backward Compatibility**: Works with existing Android app versions
- **Forward Compatibility**: Ready for future protocol enhancements
- **Graceful Degradation**: Falls back to simpler protocols when needed

### 3. **Performance Optimization**
- **Modern GUI**: Responsive PyQt6 interface with professional appearance
- **High-Performance Plotting**: PyQtGraph for smooth real-time visualization
- **C++ Acceleration**: Optional native backend for intensive computations
- **Efficient Networking**: Multi-threaded architecture with SSL support

### 4. **Comprehensive Functionality**
- **All Original Features**: Everything from previous implementations
- **Enhanced Capabilities**: Additional features like SSL, C++ processing
- **Better User Experience**: Professional GUI with improved usability
- **Data Export**: Comprehensive session data export capabilities

## Comparison with Previous Implementations

| Feature | Original tkinter | Standardized | Enhanced PyQt6 | **Unified** |
|---------|------------------|--------------|----------------|-------------|
| GUI Framework | tkinter | Console | PyQt6 | **PyQt6** |
| Protocol Support | Basic TCP | Legacy Protocol | JSON Messages | **Both Legacy + JSON** |
| Real-time Plotting | matplotlib (slow) | None | PyQtGraph (fast) | **PyQtGraph (fast)** |
| Multi-device | Limited | Full Support | Full Support | **Full Support** |
| SSL/TLS Security | No | No | Yes | **Yes** |
| C++ Backend | No | No | Yes | **Yes** |
| Session Export | Basic | None | Advanced | **Advanced** |
| Device Management | Basic | Good | Excellent | **Excellent** |
| Protocol Detection | No | No | No | **Yes (Automatic)** |

## Future Enhancements

The unified controller provides a solid foundation for additional features:

1. **Multi-PC Coordination**: Support for distributed recording across multiple PCs
2. **Advanced Analytics**: Real-time GSR analysis with event detection
3. **Cloud Integration**: Automatic session upload to cloud storage
4. **Plugin Architecture**: Extensible system for custom sensors and processing
5. **Mobile Companion**: Tablet/phone app for remote PC controller management
6. **Machine Learning**: Real-time anomaly detection and pattern recognition

## Technical Notes

### Protocol Unification Strategy
The unified controller uses a message parsing strategy that:
1. **Attempts JSON parsing first** (for modern messages)
2. **Falls back to legacy text parsing** (for older messages)
3. **Maintains separate response formats** based on detected input format
4. **Logs protocol version** for debugging and compatibility tracking

### GUI Threading Architecture
- **Main Thread**: PyQt6 GUI event handling and display updates
- **Network Thread**: TCP server and client connection management
- **Processing Thread**: Optional C++ backend processing (when available)
- **Timer Threads**: Periodic GUI updates and real-time plotting

### Error Handling Strategy
- **Network Errors**: Graceful connection handling with reconnection support
- **Protocol Errors**: Message validation with detailed error logging
- **GUI Errors**: Exception catching with user-friendly error dialogs
- **Backend Errors**: Automatic fallback from C++ to Python processing

---

**The Unified PC Session Controller represents the culmination of all PC controller development efforts, providing a single, comprehensive solution for IRCamera multi-modal recording control.**