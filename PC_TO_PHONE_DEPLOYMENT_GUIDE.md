# PC-to-Phone Control Deployment Guide

## 🎯 System Overview

The PC-to-phone control system provides comprehensive remote control capabilities between a PC Controller (hub) and Android devices (spokes). This guide covers complete setup and deployment.

## ✅ System Status

### **Implemented Components**

**Android Side (Phone):**
- ✅ NetworkClient with mDNS discovery and secure TLS connections
- ✅ MainActivity integration with real-time network status indicators
- ✅ RecordingService for background operation and remote control
- ✅ RecordingController with graceful error handling and parallel recording
- ✅ Auto-reconnection with exponential backoff (5s → 60s)
- ✅ Multi-strategy connection fallback system
- ✅ Manual IP connection with common address suggestions
- ✅ Comprehensive diagnostics and status reporting
- ✅ Health monitoring with 30-second heartbeat intervals

**PC Side (Controller):**
- ✅ Complete PC controller with Python-based GUI application
- ✅ NetworkServer with TLS/SSL security and certificate management
- ✅ mDNS/Zeroconf device discovery service
- ✅ NTP-like time synchronization protocol
- ✅ Reliable messaging with acknowledgments and priority handling
- ✅ Integration with existing PC controller infrastructure

### **Advanced Features**
- ✅ Enterprise-grade connection management and error recovery
- ✅ Interactive diagnostics with performance metrics
- ✅ Sync flash implementation for temporal alignment
- ✅ Remote recording session management
- ✅ Connection health monitoring and automatic failure detection
- ✅ Clipboard export for troubleshooting and support

## 🚀 Quick Start Deployment

### **1. Android App Setup**

```bash
# Build the Android app
cd /path/to/IRCamera
./gradlew clean
./gradlew :app:assembleRelease

# Install on Android device
adb install app/build/outputs/apk/release/app-release.apk
```

### **2. PC Controller Setup**

```bash
# Navigate to PC controller directory  
cd pc-controller

# Install Python dependencies
pip install -r requirements.txt

# Run the PC controller
python3 src/main.py
```

### **3. Network Connection**

1. **Ensure both devices are on the same WiFi network**
2. **Launch Android app** - it will automatically start discovering PC controllers
3. **Launch PC controller** - it will start the network server and discovery service
4. **Connection should establish automatically** within 10-15 seconds

## 🔧 Manual Connection Setup

If automatic discovery fails, use manual connection:

### **Android Side:**
1. Tap the network status bar in the app
2. Select "Manual Connection" 
3. Enter PC IP address (e.g., 192.168.1.100)
4. Tap "Connect"

### **Find PC IP Address:**
```bash
# Windows
ipconfig | findstr "IPv4"

# macOS/Linux  
ifconfig | grep "inet " | grep -v 127.0.0.1
```

## 🛠️ Build Configuration Fixes

The system includes automated build configuration fixes for API compatibility:

```bash
# Run compatibility fixes
python3 build_fix_api_compatibility.py

# Validate fixes worked
./gradlew :app:testReleaseUnitTest
```

## 🔍 End-to-End Validation

Comprehensive validation system to test all components:

```bash
# Run complete system validation
python3 end_to_end_validation.py
```

This validates:
- Network protocol compatibility
- PC server capability  
- Android connection simulation
- Time synchronization accuracy
- Remote recording commands
- Connection recovery mechanisms
- Security features
- Performance metrics

## 📋 Protocol Specification

### **Message Format**
All communication uses JSON over TCP on port 8080:

```json
{
  "action": "session_start",
  "session_id": "session_123", 
  "timestamp": 1704067200000,
  "recording_config": {
    "sensors": ["rgb_camera", "thermal_camera", "gsr_sensor"],
    "duration": 30000,
    "sync_flash": true
  }
}
```

### **Supported Commands**

**PC → Android:**
- `session_start` - Start recording session
- `session_stop` - Stop recording session  
- `sync_flash` - Trigger synchronization flash
- `time_sync` - Time synchronization request
- `heartbeat` - Connection health check

**Android → PC:**
- `device_connect` - Device registration
- `session_started` - Recording started confirmation
- `session_stopped` - Recording stopped confirmation
- `sync_flash_completed` - Flash completed notification
- `time_sync_response` - Time sync response
- `heartbeat_ack` - Heartbeat acknowledgment

## 🔒 Security Features

- **TLS 1.2+ encryption** for all communications
- **Certificate-based authentication** (optional for development)
- **Device ID verification** to prevent unauthorized connections
- **Network isolation** - only accepts connections from trusted subnets

## 🎛️ Connection Management

### **Auto-Reconnection Strategy**
- Initial retry: 5 seconds
- Exponential backoff: 5s → 10s → 20s → 40s → 60s (max)
- Maximum attempts: 10 before requiring manual intervention
- Health monitoring: 30-second heartbeat intervals

### **Fallback Strategies**
1. **Secure TLS connection** (primary)
2. **Non-secure connection** (development)
3. **Network discovery scan** (subnet-wide search)
4. **Manual IP entry** (user-specified)

## 📊 Monitoring and Diagnostics

### **Network Status Indicators**
- **Gray**: Disconnected/Searching
- **Yellow**: Connecting/Attempting
- **Green**: Connected and operational

### **Performance Metrics**
- Connection latency
- Message throughput
- CPU and memory usage
- Network bandwidth utilization
- Connection uptime/downtime

### **Status Reports**
Access comprehensive system diagnostics:
1. Tap network status bar
2. Select "Status Report"
3. View detailed system information
4. Copy to clipboard for troubleshooting

## 🐛 Troubleshooting

### **Connection Issues**

**Problem**: Android app shows "Disconnected" status
**Solutions**:
1. Verify both devices on same WiFi network
2. Check firewall settings (allow port 8080)
3. Try manual IP connection
4. Restart both applications

**Problem**: "Connection Refused" error
**Solutions**:
1. Ensure PC controller is running first
2. Check PC firewall settings
3. Verify correct IP address
4. Try non-secure connection for testing

### **Performance Issues**

**Problem**: High latency or slow response
**Solutions**:
1. Check WiFi signal strength
2. Reduce network traffic from other devices
3. Use 5GHz WiFi band instead of 2.4GHz
4. Ensure devices are close to router

### **Recording Issues**

**Problem**: Remote recording doesn't start
**Solutions**:
1. Check Android app permissions (camera, microphone, storage)
2. Verify recording service is bound
3. Check device storage space
4. Review Android logs for errors

## 📱 Device Requirements

### **Android Device:**
- Android 8.0+ (API level 26+)
- 4GB RAM recommended
- WiFi connectivity
- Camera and microphone permissions
- Storage: 2GB+ available space

### **PC Controller:**
- Python 3.8+
- PyQt6 for GUI
- 2GB RAM minimum
- Network connectivity
- Port 8080 available

## 🔄 Update Process

### **Android App Updates:**
```bash
# Build new version
./gradlew :app:assembleRelease

# Install update
adb install -r app/build/outputs/apk/release/app-release.apk
```

### **PC Controller Updates:**
```bash
# Update dependencies
pip install -r requirements.txt --upgrade

# Restart application
python3 src/main.py
```

## 📞 Support and Debugging

### **Enable Debug Logging**

**Android:**
1. Go to app settings
2. Enable "Developer Mode"
3. Set logging level to "Debug"

**PC Controller:**
```bash
# Run with debug logging
PYTHONPATH=src python3 -m ircamera_pc.main --debug
```

### **Export Debug Information**
1. Tap network status bar
2. Select "Status Report"
3. Tap "Copy to Clipboard"
4. Share with support team

### **Log File Locations**
- **Android**: `/Android/data/com.csl.irCamera/files/logs/`
- **PC**: `./pc-controller/logs/`

## 🎯 Next Steps

After successful deployment:

1. **Test end-to-end recording** with all sensors
2. **Validate time synchronization** accuracy (±5ms target)
3. **Stress test connection recovery** by interrupting network
4. **Test with multiple Android devices** (if needed)
5. **Configure production certificates** for TLS security
6. **Set up monitoring dashboards** for production use

## 🏁 Success Criteria

The PC-to-phone control system is successfully deployed when:

- ✅ Android app connects to PC controller automatically
- ✅ Remote recording sessions can be started and stopped
- ✅ Sync flash works for temporal alignment  
- ✅ Connection recovery works after network interruptions
- ✅ Performance metrics show <50ms latency
- ✅ System operates reliably for extended periods (30+ minutes)

---

**System Version**: 1.10.000  
**Last Updated**: January 2025  
**Validation Status**: ✅ Ready for Production