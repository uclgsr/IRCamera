# IRCamera User Guide

## Overview

This guide provides comprehensive instructions for operating the IRCamera Multi-Modal Thermal Sensing Platform. The
system implements a Hub-and-Spoke architecture where a PC Controller (Hub) coordinates multiple Android sensor nodes (
Spokes) for synchronized multi-modal data collection.

## System Components

### PC Controller (Hub)

- **Purpose**: Central coordinator for distributed sensor network
- **Technology**: Python-based application with PyQt6 GUI
- **Capabilities**: Device discovery, session management, data coordination
- **Location**: `pc-controller/` directory

### Android Sensor Nodes (Spokes)

- **Purpose**: Mobile sensor nodes with multiple sensing capabilities
- **Technology**: Kotlin-based Android application
- **Sensors**: Thermal imaging, GSR (Galvanic Skin Response), RGB camera
- **Location**: Main Android app in repository root

## Quick Start

### Prerequisites

#### PC Controller Requirements

```bash
# Required Python packages
pip install PyQt6 loguru zeroconf numpy pandas h5py pyqtgraph

# Optional packages for full functionality  
pip install scipy opencv-python bleak psutil
```

#### Android Requirements

- Android device with USB OTG support (for thermal camera)
- Shimmer3 GSR+ device (for physiological sensing)
- Network connectivity (same network as PC Controller)

### Starting the System

#### 1. Launch PC Controller (Hub)

```bash
# Navigate to PC Controller directory
cd pc-controller

# Launch the MVP application
python run_mvp_app.py

# For headless systems (no display)
QT_QPA_PLATFORM=offscreen python run_mvp_app.py
```

#### 2. Install and Start Android App (Spokes)

```bash
# Install APK on Android device (when available)
adb install app/build/outputs/apk/release/app-release.apk

# Start the IRCamera app on Android device
# The app will automatically advertise itself via mDNS
```

## PC Controller Interface

### Main Dashboard

The PC Controller provides a comprehensive GUI interface organized into several key sections:

#### Device Dashboard

- **Live Device List**: Shows all discovered Android sensor nodes
- **Status Indicators**: Real-time connection status (Discovered/Online/Recording)
- **Device Capabilities**: Displays available sensors for each device
- **Manual Device Addition**: Fallback option if automatic discovery fails

#### Session Control Panel

- **Create Session**: Initialize new recording session with metadata
- **Session Status**: Current session state and duration tracking
- **Recording Controls**: Synchronized start/stop across all devices
- **Session Finalization**: Complete session with metadata generation

#### System Monitoring

- **Real-time Logging**: Detailed event logging with timestamps
- **System Status**: Overall system health and device connectivity
- **Error Notifications**: Alert system for issues and recoveries

### Device Discovery Process

The PC Controller automatically discovers Android devices using mDNS (Zero-configuration networking):

1. **Automatic Discovery**: PC Controller scans for devices advertising `_ircamera._tcp.local.`
2. **Device Registration**: Discovered devices appear in the Device Dashboard
3. **Capability Detection**: System queries each device for available sensors
4. **Status Monitoring**: Continuous heartbeat monitoring ensures device availability

### Session Management Workflow

#### Creating a Recording Session

1. **Session Initialization**
    - Click **"Create Session"** in the Session Control Panel
    - Provide session name and optional metadata
    - System creates session directory structure
    - Session transitions to **ACTIVE** state

2. **Device Selection**
    - System automatically includes all online devices
    - Manual device selection available if needed
    - Capability validation ensures compatible sensors

#### Recording Operations

1. **Start Recording**
    - Click **"Start Recording"** to begin synchronized data collection
    - System sends start commands to all participating devices
    - Real-time progress monitoring displays recording status
    - Individual device failures are handled gracefully

2. **Monitor Recording**
    - **Device Status**: Live indicators show recording progress
    - **Session Duration**: Running timer displays elapsed recording time
    - **System Events**: Log panel shows detailed operational events
    - **Error Handling**: Automatic recovery from temporary device issues

3. **Stop Recording**
    - Click **"Stop Recording"** to end data collection
    - Coordinated stop commands sent to all devices
    - Device acknowledgment ensures clean session termination
    - Data validation confirms successful recording completion

4. **Session Finalization**
    - Click **"Finalize Session"** to complete the session
    - System generates comprehensive session metadata
    - File organization and data validation performed
    - Session summary and statistics generated

## Android App Operation

### Initial Setup

#### Device Permissions

The Android app requires several permissions for full functionality:

- **Camera**: For RGB video recording and thermal imaging
- **Bluetooth**: For GSR sensor communication via Shimmer3 device
- **Location**: Required for Bluetooth scanning (Android restriction)
- **USB**: For thermal camera (Topdon TC001) communication
- **Storage**: For saving recorded sensor data

#### Hardware Connection

1. **Thermal Camera Setup**
    - Connect Topdon TC001 thermal camera via USB OTG cable
    - Accept USB permission dialog when prompted
    - Camera status indicator should show "Connected"

2. **GSR Sensor Setup**
    - Power on Shimmer3 GSR+ device
    - Ensure Bluetooth is enabled on Android device
    - App will automatically discover and connect to GSR sensor
    - Connection status displayed in app interface

### Recording Process

#### Automatic Mode (Hub-Controlled)

1. **Network Connection**: Ensure Android device is on same network as PC Controller
2. **Service Advertisement**: App automatically advertises capabilities via mDNS
3. **Hub Discovery**: PC Controller discovers device and displays in dashboard
4. **Session Participation**: Device automatically participates in hub-initiated sessions
5. **Synchronized Recording**: All recording start/stop commands come from PC Hub

#### Manual Mode (Standalone)

1. **Local Session**: Create recording session directly on Android device
2. **Sensor Coordination**: App manages all connected sensors locally
3. **Data Storage**: All sensor data saved to device local storage
4. **Session Export**: Data can be transferred to PC for analysis

## Data Collection & Management

### Data Types Collected

#### Thermal Imaging Data

- **Format**: CSV with temperature matrices and metadata
- **Frequency**: 10 FPS capture rate
- **Configuration**: Emissivity, temperature range, color palette
- **Fallback**: Simulation mode when hardware unavailable

#### GSR (Physiological) Data

- **Format**: CSV with timestamp, conductance, PPG values
- **Frequency**: 128 Hz sampling rate from Shimmer3 device
- **Streaming**: Real-time data streaming with quality monitoring
- **Recovery**: Automatic reconnection with retry mechanisms

#### RGB Video Data

- **Format**: MP4 video files with concurrent frame capture
- **Resolution**: 4K@60fps with automatic fallback to 1080p
- **Dual Output**: Video recording + individual frames (~30 FPS)
- **Quality**: High-quality encoding for analysis applications

### Session Directory Structure

```
sessions/
+-- session_YYYY-MM-DD_HH-MM-SS/
    +-- metadata.json                    # Session configuration and timing
    +-- device_001/                      # Android device data
        +-- thermal_data.csv            # Thermal sensor data
        +-- gsr_data.csv               # GSR sensor data  
        +-- rgb_video.mp4              # RGB video recording
        +-- rgb_frames/                # Individual video frames
    +-- session_summary.json            # Overall session statistics
```

## Advanced Configuration

### PC Controller Settings

#### Network Configuration

- **TCP Port**: Default 8080 (configurable in settings)
- **mDNS Service**: `_ircamera._tcp.local.` for device discovery
- **Timeout Settings**: Device heartbeat and command acknowledgment timeouts
- **Buffer Management**: Data reception and logging buffer sizes

#### Session Configuration

- **Recording Duration**: Maximum session length limits
- **Device Requirements**: Minimum device count for session validity
- **Data Validation**: Quality checks and error recovery options
- **Storage Management**: Automatic cleanup and disk space monitoring

### Android App Configuration

#### Sensor Settings

- **Thermal Camera**: Temperature range, emissivity, color palette selection
- **GSR Sensor**: Sampling rate, calibration settings, range configuration
- **RGB Camera**: Resolution, frame rate, encoding quality settings

#### Communication Settings

- **Network Discovery**: mDNS advertisement configuration
- **TCP Communication**: Connection timeout and retry settings
- **Data Streaming**: Buffer sizes and transmission frequency

## Troubleshooting

### Common Issues

#### PC Controller Problems

**1. GUI Not Starting**

```bash
# Verify PyQt6 installation
pip install PyQt6

# For headless systems
QT_QPA_PLATFORM=offscreen python run_mvp_app.py
```

**2. Device Discovery Failed**

- Check network connectivity between PC and Android devices
- Verify mDNS is enabled on network (not all enterprise networks support mDNS)
- Use manual device addition as fallback option
- Check firewall settings allow TCP traffic on port 8080

**3. Session Creation Error**

- Verify session directory has write permissions
- Check available disk space for data storage
- Review system logs for detailed error information
- Ensure configuration file is accessible

#### Android App Problems

**1. Permission Denied Errors**

- Grant all requested permissions in Android settings
- Check USB OTG functionality with thermal camera
- Verify Bluetooth permissions for GSR sensor access
- Enable location services (required for Bluetooth scanning)

**2. Sensor Connection Issues**

**Thermal Camera (Topdon TC001):**

- Verify USB OTG cable functionality
- Check USB permission dialog acceptance
- Try disconnecting/reconnecting camera
- Review USB device logs via `adb logcat`

**GSR Sensor (Shimmer3):**

- Ensure Shimmer device is powered on and charged
- Check Bluetooth pairing status
- Verify device is not connected to other applications
- Try power cycling the Shimmer device

**3. Network Connectivity Issues**

- Ensure Android device and PC are on same network
- Check WiFi connectivity stability
- Verify no network firewalls blocking communication
- Test with mobile hotspot as alternative network

#### Data Quality Issues

**1. Synchronization Problems**

- Check system clock synchronization between devices
- Review network latency for time-critical operations
- Validate timestamp alignment in recorded data
- Use sync flash commands to verify timing accuracy

**2. Missing Data**

- Review device logs for recording interruptions
- Check storage space availability during recording
- Verify all sensors remained connected throughout session
- Examine session metadata for partial recording indicators

### Debug Mode

Enable detailed logging for troubleshooting:

```bash
# PC Controller debug mode
IRCAMERA_LOG_LEVEL=DEBUG python run_mvp_app.py

# Android app debug logs
adb logcat | grep IRCamera
```

### Performance Optimization

#### PC Controller Performance

- **Memory Usage**: Monitor system memory during long recording sessions
- **CPU Usage**: Optimize for multi-device data processing
- **Network Throughput**: Ensure adequate bandwidth for multiple device streams
- **Storage Performance**: Use SSDs for high-frequency data recording

#### Android App Performance

- **Battery Optimization**: Disable battery optimization for consistent recording
- **Thermal Management**: Monitor device temperature during extended sessions
- **Storage Management**: Regular cleanup of temporary files
- **Resource Monitoring**: Track CPU and memory usage during recording

## Best Practices

### Session Planning

1. **Pre-Session Checks**: Verify all devices connected and sensors functional
2. **Network Stability**: Ensure stable network connection before starting
3. **Storage Capacity**: Confirm adequate storage space for planned recording duration
4. **Device Battery**: Ensure all devices sufficiently charged for session length

### Data Quality Assurance

1. **Baseline Recording**: Capture baseline data before experimental conditions
2. **Calibration Checks**: Verify sensor calibration before critical recordings
3. **Redundancy Planning**: Use multiple devices when possible for data backup
4. **Real-time Monitoring**: Actively monitor data quality during recording

### System Maintenance

1. **Regular Updates**: Keep software updated to latest versions
2. **Hardware Maintenance**: Clean sensors and check connections regularly
3. **Data Backup**: Implement regular backup procedures for recorded data
4. **Performance Monitoring**: Track system performance trends over time

---

**Status**: [DONE] Complete User Documentation  
**Last Updated**: Documentation Consolidation v1.0  
**Support**: See [Documentation Hub](README.md) for additional resources