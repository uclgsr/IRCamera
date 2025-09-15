# Troubleshooting Guide - MPDC4GSR Platform

Comprehensive troubleshooting guide for common issues and their solutions.

## 🚨 Common Issues

### Android App Issues

#### ❌ App crashes on startup

**Symptoms**: App closes immediately after launch
**Possible Causes**:

- Missing permissions
- Corrupted installation
- Incompatible Android version

**Solutions**:

```bash
# Check device compatibility
adb shell getprop ro.build.version.sdk  # Should be ≥21

# Clear app data
adb shell pm clear com.csl.irCamera

# Reinstall app
adb uninstall com.csl.irCamera
adb install app-release.apk

# Check logs
adb logcat | grep MPDC4GSR
```

#### ❌ Shimmer3 GSR sensor not detected

**Symptoms**: "GSR Disconnected" or simulated data only
**Diagnostic Steps**:

1. **Check Bluetooth pairing**:
   ```
   Android Settings → Bluetooth → Paired Devices
   Look for: "Shimmer3-XXXX" or "RN42-XXXX"
   Status should show: "Paired"
   ```

2. **Verify permissions**:
   ```
   Settings → Apps → MPDC4GSR → Permissions
   Required: ✅ Location, ✅ Bluetooth, ✅ Nearby devices
   ```

3. **Test Shimmer3 device**:
   ```
   - LED should blink when powered on
   - Try pairing with different Android device
   - Check battery level (should be >20%)
   ```

**Solutions**:

```kotlin

1. Unpair Shimmer3 in Android Bluetooth settings
2. Power cycle Shimmer3 device (off/on)
3. Clear Bluetooth cache:
   Settings → Apps → Bluetooth → Storage → Clear Cache
4. Re-pair device and restart MPDC4GSR app
```

#### ❌ Camera permission denied

**Symptoms**: Black screen or "Camera unavailable" message

**Solutions**:

```bash
# Grant permissions via ADB
adb shell pm grant com.csl.irCamera android.permission.CAMERA
adb shell pm grant com.csl.irCamera android.permission.RECORD_AUDIO
adb shell pm grant com.csl.irCamera android.permission.WRITE_EXTERNAL_STORAGE

# Manual permission grant
Settings → Apps → MPDC4GSR → Permissions → Enable all
```

#### ❌ Recording stops unexpectedly

**Symptoms**: Recording terminates before intended duration

**Diagnostic Commands**:

```bash
# Check available storage
adb shell df /sdcard

# Monitor memory usage
adb shell dumpsys meminfo com.csl.irCamera

# Check for thermal throttling
adb shell cat /sys/class/thermal/thermal_zone*/temp
```

**Solutions**:

- Free up storage space (minimum 5GB recommended)
- Close background apps to free memory
- Allow device to cool down if overheating
- Reduce recording quality settings

### PC Controller Issues

#### ❌ "Failed to import PyQt6"

**Symptoms**: PC Controller won't start, import error

**Solutions**:

```bash
# Windows
pip uninstall PyQt6
pip install PyQt6

# macOS with brew
brew install qt6
pip install PyQt6

# Linux (Ubuntu/Debian)
sudo apt-get install python3-pyqt6
pip install PyQt6

# Verify installation
python -c "import PyQt6; print('PyQt6 version:', PyQt6.QtCore.qVersion())"
```

#### ❌ No Android devices discovered

**Symptoms**: Device list remains empty

**Diagnostic Steps**:

```bash
# Check network connectivity
ping [android-device-ip]

# Test port availability
netstat -an | grep 8080

# Check firewall settings (Windows)
netsh advfirewall firewall show rule name="MPDC4GSR"

# Check firewall settings (Linux)
sudo ufw status | grep 8080
```

**Solutions**:

```bash
# Windows Firewall
netsh advfirewall firewall add rule name="MPDC4GSR" dir=in action=allow protocol=TCP localport=8080

# Linux UFW
sudo ufw allow 8080

# macOS
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /path/to/python
```

#### ❌ Time synchronization failed

**Symptoms**: Large timestamp differences between devices

**Diagnostic Code**:

```python
# Test time sync manually
import asyncio
import time

async def test_time_sync(device_ip):
    t1 = time.time_ns()
    # Send sync request to device
    response = await send_sync_request(device_ip, t1)
    t4 = time.time_ns()
    
    offset = calculate_offset(t1, response.t2, response.t3, t4)
    delay = calculate_delay(t1, response.t2, response.t3, t4)
    
    print(f"Offset: {offset/1_000_000:.2f}ms, Delay: {delay/1_000_000:.2f}ms")
    
    if abs(offset) > 5_000_000:  # 5ms
        print("❌ Sync failed - offset too large")
    else:
        print("✅ Sync successful")
```

**Solutions**:

- Ensure stable WiFi connection
- Use 5GHz WiFi for better performance
- Restart both applications
- Check system clocks are correct on both devices

### Network Issues

#### ❌ Connection timeouts

**Symptoms**: "Connection timeout" or "Device unreachable"

**Network Diagnostics**:

```bash
# Test basic connectivity
ping [android-device-ip]

# Test specific port
telnet [android-device-ip] 8080

# Check network route
traceroute [android-device-ip]

# Monitor network traffic
wireshark -i [interface] -f "port 8080"
```

**Solutions**:

1. **Check network configuration**:
    - Both devices on same subnet (192.168.x.x)
    - No VPN interference
    - Router not blocking inter-device communication

2. **Reset network settings**:
   ```bash
   # Android
   Settings → Network → Reset Network Settings
   
   # PC - Reset TCP/IP stack
   netsh int ip reset
   netsh winsock reset
   ```

#### ❌ SSL/TLS handshake failed

**Symptoms**: "SSL handshake failed" or certificate errors

**Certificate Diagnostics**:

```bash
# Check certificate validity
openssl x509 -in certificate.crt -text -noout

# Test SSL connection
openssl s_client -connect [device-ip]:8080 -verify_return_error
```

**Solutions**:

```bash
# Regenerate certificates
./scripts/generate_certificates.sh

# Update certificate store
cp ca.crt /usr/local/share/ca-certificates/
update-ca-certificates
```

### Hardware Issues

#### ❌ Shimmer3 battery drain

**Symptoms**: Device disconnects frequently, low battery warnings

**Battery Diagnostics**:

```kotlin

val batteryLevel = shimmerDevice.getBatteryLevel()
Log.i("MPDC4GSR", "Shimmer3 battery: ${batteryLevel}%")

if (batteryLevel < 20) {
    Log.w("MPDC4GSR", "Low battery warning")
}
```

**Solutions**:

- Replace batteries (2x AA recommended)
- Use rechargeable batteries for extended sessions
- Enable power saving mode during long recordings
- Check for firmware updates

#### ❌ Thermal camera not detected

**Symptoms**: Thermal recording unavailable

**USB Diagnostics**:

```bash
# Check USB devices (Android)
adb shell lsusb

# Check camera permissions
adb shell pm list permissions | grep camera
```

**Solutions**:

- Use USB-C to USB-A adapter if needed
- Grant camera permissions for external devices
- Try different USB port
- Check camera compatibility list

### Performance Issues

#### ❌ High memory usage

**Symptoms**: App becomes slow, system warnings

**Memory Monitoring**:

```bash
# Android memory usage
adb shell dumpsys meminfo com.csl.irCamera

# PC memory usage
python -c "
import psutil
process = psutil.Process()
print(f'Memory: {process.memory_info().rss / 1024 / 1024:.1f} MB')
"
```

**Solutions**:

```kotlin

class MemoryOptimizedRecorder {
    private val memoryCache = LruCache<String, Bitmap>(50)
    
    fun optimizeMemory() {

        memoryCache.evictAll()

        System.gc()

        val bufferSize = min(DEFAULT_BUFFER_SIZE, availableMemory() / 4)
    }
}
```

#### ❌ Frame drops in video recording

**Symptoms**: Choppy video, missing frames

**Performance Monitoring**:

```kotlin
class PerformanceMonitor {
    fun checkVideoPerformance() {
        val frameRate = videoRecorder.getCurrentFrameRate()
        val targetFrameRate = sessionConfig.targetFrameRate
        
        val dropPercentage = (targetFrameRate - frameRate) / targetFrameRate * 100
        
        if (dropPercentage > 5) {
            Log.w("MPDC4GSR", "Frame drop detected: ${dropPercentage}%")

            adjustVideoQuality()
        }
    }
}
```

**Solutions**:

- Reduce video resolution (4K → 1080p)
- Lower frame rate (60fps → 30fps)
- Close background applications
- Use faster storage (internal vs SD card)

## 🔧 Diagnostic Tools

### Android Debugging

#### ADB Commands

```bash
# Complete device info
adb shell getprop

# Running processes
adb shell ps | grep irCamera

# Network connections
adb shell netstat -an

# Storage usage
adb shell du -sh /sdcard/IRCamera_Sessions/

# Real-time logs
adb logcat -s MPDC4GSR:V
```

#### Performance Profiling

```bash
# CPU usage
adb shell top -n 1 | grep com.csl.irCamera

# GPU usage (if available)
adb shell dumpsys gfxinfo com.csl.irCamera

# Battery usage
adb shell dumpsys batterystats | grep com.csl.irCamera
```

### PC Controller Debugging

#### Python Debugging

```python
import logging

# Enable debug logging
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger('mpdc4gsr')

# Memory profiling
import tracemalloc
tracemalloc.start()

# At end of session
current, peak = tracemalloc.get_traced_memory()
logger.info(f"Memory usage: {current / 1024 / 1024:.1f} MB (peak: {peak / 1024 / 1024:.1f} MB)")
```

#### Network Debugging

```python
import socket
import time

def test_network_connectivity(host: str, port: int = 8080) -> bool:
    """Test network connectivity to Android device"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5.0)
        
        start_time = time.time()
        result = sock.connect_ex((host, port))
        end_time = time.time()
        
        sock.close()
        
        if result == 0:
            print(f"✅ Connection successful ({(end_time - start_time) * 1000:.1f}ms)")
            return True
        else:
            print(f"❌ Connection failed: {result}")
            return False
            
    except Exception as e:
        print(f"❌ Network error: {e}")
        return False
```

### Data Validation

#### Session Data Integrity

```python
def validate_session_data(session_path: str) -> Dict[str, bool]:
    """Validate session data integrity"""
    results = {}
    
    # Check required files
    required_files = [
        'session_metadata.json',
        'gsr_data.csv',
        'sync_events.csv'
    ]
    
    for file in required_files:
        file_path = Path(session_path) / file
        results[f"file_{file}"] = file_path.exists()
    
    # Validate GSR data
    if results.get("file_gsr_data.csv"):
        gsr_data = pd.read_csv(Path(session_path) / 'gsr_data.csv')
        
        # Check sample rate
        time_diffs = gsr_data['timestamp_ms'].diff().dropna()
        avg_interval = time_diffs.mean()
        expected_interval = 1000 / 128  # 128 Hz
        
        results["gsr_sample_rate"] = abs(avg_interval - expected_interval) < 1.0
        
        # Check for gaps
        max_gap = time_diffs.max()
        results["gsr_no_gaps"] = max_gap < 50  # Less than 50ms gaps
    
    # Validate sync events
    if results.get("file_sync_events.csv"):
        sync_data = pd.read_csv(Path(session_path) / 'sync_events.csv')
        results["sync_events_present"] = len(sync_data) > 0
    
    return results
```

## 📋 Error Codes Reference

### Android Error Codes

| Code          | Description                  | Solution                   |
|---------------|------------------------------|----------------------------|
| `CAMERA_001`  | Camera permission denied     | Grant camera permissions   |
| `CAMERA_002`  | Camera in use by another app | Close other camera apps    |
| `CAMERA_003`  | Camera hardware failure      | Restart device             |
| `GSR_001`     | Shimmer3 not paired          | Pair Bluetooth device      |
| `GSR_002`     | Shimmer3 connection lost     | Check battery and range    |
| `GSR_003`     | GSR calibration failed       | Check electrode connection |
| `STORAGE_001` | Insufficient storage         | Free up space              |
| `STORAGE_002` | Write permission denied      | Grant storage permissions  |
| `NETWORK_001` | PC Controller not found      | Check WiFi connection      |
| `NETWORK_002` | TLS handshake failed         | Update certificates        |

### PC Controller Error Codes

| Code     | Description             | Solution                 |
|----------|-------------------------|--------------------------|
| `PC_001` | PyQt6 import failed     | Install PyQt6            |
| `PC_002` | Port 8080 in use        | Kill existing process    |
| `PC_003` | Certificate not found   | Generate certificates    |
| `PC_004` | Device timeout          | Check network connection |
| `PC_005` | Data aggregation failed | Check available memory   |

## 🆘 Emergency Procedures

### Emergency Stop

If recording must be stopped immediately:

**Android Device**:

1. Press volume down + power button simultaneously
2. Or force close app: Settings → Apps → MPDC4GSR → Force Stop

**PC Controller**:

1. Click "Emergency Stop" button
2. Or press Ctrl+C in terminal
3. Or kill process: `pkill -f mpdc4gsr`

### Data Recovery

If recording was interrupted:

```bash
# Check for partial session data
ls -la IRCamera_Sessions/session_*/

# Validate what was saved
python scripts/validate_session.py [session_path]

# Attempt data recovery
python scripts/recover_session.py [session_path]
```

## 📞 Getting Help

### Self-Diagnosis Checklist

Before reporting issues, try this checklist:

- [ ] Both devices on same WiFi network
- [ ] All permissions granted to MPDC4GSR app
- [ ] Shimmer3 device powered on and paired
- [ ] PC Controller firewall allows port 8080
- [ ] Sufficient storage space (>5GB)
- [ ] Latest version of software installed
- [ ] Device compatibility confirmed

### Log Collection

When reporting issues, collect these logs:

```bash
# Android logs
adb logcat -s MPDC4GSR:V > android_logs.txt

# PC Controller logs  
python src/main.py --debug > pc_logs.txt 2>&1

# Network diagnostics
ping [device-ip] > network_test.txt
```

### Support Channels

- **GitHub Issues**: For bug reports with logs
- **Discussions**: For usage questions
- **Documentation**: Check all guides first
- **Community Wiki**: User-contributed solutions

---

**Remember**: Most issues are resolved by checking network connectivity, permissions, and device
pairing. When in doubt, restart both applications and devices.**
