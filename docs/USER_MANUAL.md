# User Manual - MPDC4GSR Platform

Complete guide for using the Multi-Modal Physiological Sensing Platform.

## 📖 Table of Contents

1. [Getting Started](#getting-started)
2. [Hardware Setup](#hardware-setup)
3. [Software Installation](#software-installation)
4. [Device Pairing](#device-pairing)
5. [Recording Sessions](#recording-sessions)
6. [Data Management](#data-management)
7. [Troubleshooting](#troubleshooting)
8. [Advanced Features](#advanced-features)

## 🚀 Getting Started

### What is MPDC4GSR?

The Multi-Modal Physiological Data Collection for GSR (MPDC4GSR) platform is a research tool that simultaneously records:

- **RGB Video**: High-quality 4K60FPS video recording
- **RAW Images**: Professional-grade DNG captures for detailed analysis
- **Thermal Imagery**: Infrared temperature mapping
- **GSR Data**: Galvanic Skin Response measurements from Shimmer3 sensors

All data streams are synchronized with nanosecond precision for accurate multi-modal analysis.

### System Overview

```
┌─────────────────┐    WiFi    ┌──────────────────┐
│   PC Controller │◄─────────►│  Android Device  │
│     (Hub)       │            │     (Sensor)     │
└─────────────────┘            └──────────────────┘
         │                               │
    Data Storage              ┌──────────┴──────────┐
    & Analysis               │                      │
                     ┌───────▼────┐      ┌─────────▼──────┐
                     │ Shimmer3   │      │ Cameras        │
                     │ GSR Sensor │      │ RGB + Thermal  │
                     └────────────┘      └────────────────┘
```

## 🔧 Hardware Setup

### Required Equipment

#### Essential Components
- **Android Device**: API 21+ with Camera2 support (Samsung Galaxy S22 recommended)
- **PC/Laptop**: Windows/Linux/macOS with WiFi capability
- **Shimmer3 GSR Sensor**: For physiological data collection
- **WiFi Router**: For device communication

#### Optional Components
- **External Thermal Camera**: USB-connected FLIR/Topdon cameras
- **Power Banks**: For extended recording sessions
- **SD Cards**: Additional storage for large datasets

### Shimmer3 GSR Setup

#### 1. Sensor Preparation
1. **Insert electrodes** into Shimmer3 GSR sensor ports
2. **Attach finger electrodes** to index and middle finger
3. **Power on** the Shimmer3 device (LED should blink)
4. **Verify operation** - LED patterns indicate sensor status

#### 2. Electrode Placement
```
Correct Placement:
┌─────────────────┐
│ Index Finger    │ ← Electrode 1 (tip)
│                 │
│ Middle Finger   │ ← Electrode 2 (tip)  
│                 │
│ Other fingers   │ ← No electrodes
└─────────────────┘

Guidelines:
- Clean, dry skin contact
- Secure but comfortable fit
- Avoid excessive movement
- 5-10 minute stabilization period
```

#### 3. Device Verification
- **Green LED**: Normal operation
- **Red LED**: Low battery or error
- **Blinking pattern**: Data transmission active

### Thermal Camera Setup (Optional)

#### USB Thermal Camera
1. **Connect** thermal camera to Android device via USB-C/OTG adapter
2. **Grant permissions** when prompted by Android
3. **Verify detection** in MPDC4GSR app settings

#### Built-in Thermal (Samsung S22)
- No additional setup required
- Thermal sensors automatically detected

## 💾 Software Installation

### Android App Installation

#### Option 1: Pre-built APK
```bash
# Download latest release APK from GitHub
# Enable "Install from Unknown Sources" in Android settings
# Install APK file (tap to install)
```

#### Option 2: Build from Source
```bash
# Clone repository
git clone https://github.com/buccancs/IRCamera.git
cd IRCamera

# Build release APK
./gradlew clean :app:assembleRelease

# Install on device
adb install app/build/outputs/apk/release/app-release.apk
```

### PC Controller Installation

#### Windows
```bash
# Download Python 3.11+ from python.org
# Clone repository
git clone https://github.com/buccancs/IRCamera.git
cd IRCamera/pc-controller

# Create virtual environment
python -m venv venv
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Launch application
python src/main.py
```

#### macOS/Linux
```bash
# Ensure Python 3.11+ is installed
# Clone repository
git clone https://github.com/buccancs/IRCamera.git
cd IRCamera/pc-controller

# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Launch application
python src/main.py
```

## 📱 Device Pairing

### Step 1: Network Setup
1. **Connect both devices** to the same WiFi network
2. **Note network name** - both devices must be on identical network
3. **Check IP addresses** if automatic discovery fails

### Step 2: Shimmer3 Pairing
1. **Android Settings** → **Bluetooth**
2. **Scan for devices** → Look for "Shimmer3-XXXX"
3. **Pair device** → Enter PIN if prompted (usually 1234)
4. **Verify pairing** → Device shows as "Paired" in Bluetooth list

### Step 3: Application Pairing

#### On PC Controller:
1. **Launch** PC Controller application
2. **Click** "Discover Devices" button  
3. **Wait** for Android devices to appear in list
4. **Select** target Android device
5. **Click** "Connect" button

#### On Android Device:
1. **Launch** MPDC4GSR app
2. **Grant permissions** when prompted:
   - Camera access
   - Storage access  
   - Location access (for Bluetooth)
   - Bluetooth permissions
3. **Confirm pairing** when PC Controller requests connection
4. **Verify connection** - Green indicator shows connected status

### Step 4: Connection Verification
- **PC Controller**: Device appears in "Connected Devices" list
- **Android App**: "PC Connected" indicator shows green
- **Shimmer3**: "GSR Sensor" status shows "Connected"

## 🎬 Recording Sessions

### Quick Start Recording

#### Method 1: PC-Initiated Recording
1. **PC Controller** → Select connected device
2. **Click** "Start Recording" 
3. **Enter session details**:
   - Session Name: `Study_001_Participant_A`
   - Participant ID: `P001`
   - Study Protocol: `Stress_Test`
4. **Configure recording**:
   - Duration: 5 minutes
   - Video Quality: 4K60FPS
   - RAW Images: Enabled
   - GSR Sampling: 128Hz
5. **Click** "Begin Recording"

#### Method 2: Android-Initiated Recording  
1. **Android App** → Main screen
2. **Long-press** app title for quick menu
3. **Select** "Full Recording Session"
4. **Configure** session parameters
5. **Tap** "Start Recording"

### Recording Interface

#### PC Controller Interface
```
┌─────────────────────────────────────────────────┐
│ MPDC4GSR - PC Controller                        │
├─────────────────────────────────────────────────┤
│ Connected Devices:                              │
│ ☑ Android-Device-001 [192.168.1.105]          │
│ ☑ Shimmer3-ABC123 [Via Bluetooth]             │
├─────────────────────────────────────────────────┤
│ Session: Study_001_P001    Duration: 00:02:15  │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│ │ RGB Camera  │ │ GSR Data    │ │ Thermal     │ │
│ │ 4K60 Active │ │ 128Hz Live  │ │ 30FPS IR    │ │
│ │ [Live View] │ │ [Real Plot] │ │ [Live View] │ │
│ └─────────────┘ └─────────────┘ └─────────────┘ │
├─────────────────────────────────────────────────┤
│ [Sync Flash] [Stop Recording] [Emergency Stop] │
└─────────────────────────────────────────────────┘
```

#### Android Interface
```
┌─────────────────────────────────────────────────┐
│ MPDC4GSR Recording Session                      │
├─────────────────────────────────────────────────┤
│ Status: Recording  Duration: 00:02:15          │
│ PC Controller: ☑ Connected                     │
│ Shimmer3 GSR: ☑ Active (128Hz)                │
├─────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────┐ │
│ │           Camera Preview                    │ │
│ │                                             │ │
│ │         [Live RGB View]                     │ │
│ │                                             │ │
│ │                4K60FPS                      │ │
│ └─────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────┤
│ GSR: 12.5 µS  |  Samples: 1,920              │
│ Storage: 2.1 GB available                      │
├─────────────────────────────────────────────────┤
│ [Sync Event] [Pause] [Stop Recording]          │
└─────────────────────────────────────────────────┘
```

### Recording Controls

#### Sync Flash
- **Purpose**: Create visual markers for post-processing alignment
- **Usage**: Press during important events (stimulus presentation, etc.)
- **Effect**: All device screens flash white simultaneously
- **Data**: Sync events recorded in `sync_events.csv`

#### Emergency Stop
- **Purpose**: Immediate recording termination
- **Usage**: Critical situations or equipment malfunction
- **Effect**: All devices stop recording immediately
- **Data**: Partial session data preserved

### Recording Quality Settings

#### Video Configuration
```
Quality Presets:
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ Setting     │ Resolution  │ Frame Rate  │ File Size   │
├─────────────┼─────────────┼─────────────┼─────────────┤
│ Maximum     │ 4K (3840px) │ 60 FPS      │ ~60 MB/min  │
│ High        │ 4K (3840px) │ 30 FPS      │ ~30 MB/min  │
│ Standard    │ 1080p       │ 60 FPS      │ ~25 MB/min  │
│ Efficient   │ 1080p       │ 30 FPS      │ ~12 MB/min  │
└─────────────┴─────────────┴─────────────┴─────────────┘
```

#### RAW Image Settings
```
RAW Configuration:
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ Mode        │ Resolution  │ Frame Rate  │ File Size   │
├─────────────┼─────────────┼─────────────┼─────────────┤
│ Full RAW    │ Native      │ 30 FPS      │ ~6 GB/min   │
│ Compressed  │ Native      │ 15 FPS      │ ~2 GB/min   │
│ Burst       │ Native      │ 2 FPS       │ ~400 MB/min │
│ Disabled    │ N/A         │ N/A         │ 0 MB/min    │
└─────────────┴─────────────┴─────────────┴─────────────┘
```

## 📊 Data Management

### Session Data Structure

Each recording session creates a organized folder structure:

```
IRCamera_Sessions/
└── session_20240115_143022_Study001_P001/
    ├── rgb_video.mp4              # Main video recording
    ├── session_metadata.json      # Session configuration
    ├── gsr_data.csv              # Shimmer3 GSR measurements
    ├── sync_events.csv           # Synchronization markers
    ├── thermal_video.mp4         # Infrared video (if available)
    ├── raw_images/               # DNG raw captures
    │   ├── frame_000001.dng
    │   ├── frame_000002.dng
    │   └── ...
    └── analysis/                 # Post-processing outputs
        ├── aligned_timestamps.csv
        └── data_quality_report.json
```

### File Formats

#### GSR Data (gsr_data.csv)
```csv
timestamp_ms,utc_timestamp_ms,conductance_us,resistance_kohms,sample_index,session_id
1705328522000,1705328522000,12.347,80.923,1,session_20240115_143022
1705328522008,1705328522008,12.356,80.897,2,session_20240115_143022
1705328522016,1705328522016,12.341,80.945,3,session_20240115_143022
```

#### Sync Events (sync_events.csv)
```csv
timestamp_ms,utc_timestamp_ms,event_type,session_id,metadata
1705328525000,1705328525000,SYNC_FLASH,session_20240115_143022,manual_trigger
1705328530000,1705328530000,STIMULUS_START,session_20240115_143022,condition_A
1705328535000,1705328535000,USER_MARKER,session_20240115_143022,important_event
```

#### Session Metadata (session_metadata.json)
```json
{
    "session_id": "session_20240115_143022_Study001_P001",
    "participant_id": "P001",
    "study_protocol": "Study001",
    "start_time": "2024-01-15T14:30:22.000Z",
    "end_time": "2024-01-15T14:35:22.000Z",
    "duration_seconds": 300,
    "configuration": {
        "rgb_video": {
            "resolution": "4K",
            "frame_rate": 60,
            "codec": "H.264"
        },
        "raw_images": {
            "enabled": true,
            "frame_rate": 30,
            "format": "DNG"
        },
        "gsr_sampling": {
            "rate_hz": 128,
            "device": "Shimmer3-ABC123"
        }
    },
    "data_quality": {
        "gsr_samples_total": 38400,
        "sync_events_count": 5,
        "video_frames": 18000,
        "raw_images": 9000
    }
}
```

### Data Export Options

#### CSV Export
- **Format**: Comma-separated values for spreadsheet analysis
- **Usage**: Statistical analysis, basic plotting
- **Tools**: Excel, R, Python pandas, MATLAB

#### HDF5 Export
- **Format**: Hierarchical scientific data format
- **Usage**: Large dataset analysis, machine learning
- **Tools**: Python h5py, MATLAB, specialized research software

#### Research Formats
- **EDF/BDF**: European Data Format for physiological signals
- **BIDS**: Brain Imaging Data Structure for neuroimaging studies
- **Custom**: JSON-based metadata with raw data preservation

### Storage Management

#### Storage Requirements
```
Typical 5-minute session:
┌─────────────────┬─────────────┬─────────────┐
│ Data Type       │ File Size   │ Storage     │
├─────────────────┼─────────────┼─────────────┤
│ RGB Video (4K)  │ ~300 MB     │ Internal    │
│ RAW Images      │ ~1.8 GB     │ SD Card     │
│ GSR Data        │ ~2 MB       │ Internal    │
│ Thermal Video   │ ~150 MB     │ Internal    │
│ Metadata        │ ~50 KB      │ Internal    │
├─────────────────┼─────────────┼─────────────┤
│ Total           │ ~2.25 GB    │ Mixed       │
└─────────────────┴─────────────┴─────────────┘
```

#### Automatic Cleanup
- **Temporary files**: Automatically removed after successful session
- **Failed sessions**: Partial data retained for diagnostics
- **Storage monitoring**: Alerts when space below 5GB threshold

## 🔍 Troubleshooting

### Common Issues

#### ❌ "Shimmer3 sensor not detected"
**Symptoms**: GSR data shows as "Disconnected" or simulated
**Solutions**:
1. Verify Bluetooth pairing in Android settings
2. Restart Shimmer3 device (power cycle)
3. Grant Bluetooth permissions to MPDC4GSR app
4. Check Shimmer3 battery level
5. Move closer to Android device (within 3 meters)

#### ❌ "PC Controller not visible"
**Symptoms**: Android app shows "No PC Controllers found"
**Solutions**:
1. Ensure both devices on same WiFi network
2. Check PC firewall settings (allow port 8080)
3. Restart WiFi on both devices
4. Use manual IP connection option
5. Verify network subnet (both devices in 192.168.x.x range)

#### ❌ "Camera permission denied"
**Symptoms**: Black screen or "Camera not available" error
**Solutions**:
1. Android Settings → Apps → MPDC4GSR → Permissions
2. Grant Camera, Storage, and Microphone permissions
3. Restart MPDC4GSR app
4. Reboot Android device if permissions persist

#### ❌ "Insufficient storage space"
**Symptoms**: Recording stops early or fails to start
**Solutions**:
1. Free up space on Android device (minimum 5GB recommended)
2. Move existing sessions to PC or cloud storage
3. Reduce recording quality settings
4. Use external SD card for RAW images
5. Enable automatic cleanup in settings

#### ❌ "Time synchronization failed"
**Symptoms**: Data streams show large timestamp differences
**Solutions**:
1. Ensure stable WiFi connection
2. Restart both applications
3. Verify system clocks on both devices
4. Use manual time offset if automatic sync fails
5. Check for network interference

### Performance Issues

#### Slow Recording Performance
**Causes & Solutions**:
1. **High recording quality**: Reduce to 1080p or lower frame rate
2. **Low storage speed**: Use high-speed SD card (Class 10+)
3. **Background apps**: Close unnecessary apps on Android
4. **Thermal throttling**: Allow device cooling between sessions
5. **Network congestion**: Use dedicated WiFi network if possible

#### Bluetooth Connection Drops
**Causes & Solutions**:
1. **Distance**: Keep Shimmer3 within 2 meters of Android device
2. **Interference**: Avoid 2.4GHz WiFi congestion
3. **Power saving**: Disable battery optimization for MPDC4GSR
4. **Multiple devices**: Unpair unused Bluetooth devices
5. **Firmware**: Update Shimmer3 firmware if available

### Error Messages

#### "Session initialization failed"
```
Error: Unable to create session directory
Solution: Check storage permissions and available space
Command: Settings → Apps → MPDC4GSR → Permissions → Storage
```

#### "Network protocol mismatch"
```
Error: Version incompatibility between PC and Android
Solution: Update both applications to latest version
Download: GitHub releases page
```

#### "GSR calibration timeout"
```
Error: Shimmer3 device not responding to calibration
Solution: 
1. Power cycle Shimmer3 device
2. Re-pair Bluetooth connection
3. Check electrode connections
4. Try different Shimmer3 device
```

## 🔬 Advanced Features

### Custom Recording Protocols

#### Creating Study Protocols
```json
{
    "protocol_name": "Stress_Induction_Protocol",
    "version": "1.2",
    "description": "Standardized stress response measurement",
    "phases": [
        {
            "name": "baseline",
            "duration_seconds": 120,
            "instructions": "Remain calm and relaxed",
            "sync_markers": ["BASELINE_START", "BASELINE_END"]
        },
        {
            "name": "stressor",
            "duration_seconds": 180,
            "instructions": "Complete cognitive task",
            "sync_markers": ["TASK_START", "TASK_END"]
        },
        {
            "name": "recovery",
            "duration_seconds": 180,
            "instructions": "Return to baseline state",
            "sync_markers": ["RECOVERY_START", "RECOVERY_END"]
        }
    ],
    "data_quality_thresholds": {
        "gsr_sampling_rate_min": 120,
        "video_frame_drops_max": 5,
        "sync_accuracy_ms_max": 10
    }
}
```

### Real-time Data Analysis

#### Live GSR Monitoring
The PC Controller provides real-time GSR analysis:

```python
# Example: Real-time GSR feature extraction
def analyze_gsr_stream(gsr_samples):
    """Extract real-time GSR features"""
    if len(gsr_samples) >= 128:  # 1 second of data
        # Extract features
        mean_conductance = np.mean([s.conductance for s in gsr_samples[-128:]])
        std_conductance = np.std([s.conductance for s in gsr_samples[-128:]])
        
        # Detect skin conductance responses (SCRs)
        scr_count = detect_scrs(gsr_samples[-128:])
        
        return {
            'mean_conductance': mean_conductance,
            'conductance_variability': std_conductance,
            'scr_frequency': scr_count,
            'signal_quality': assess_signal_quality(gsr_samples[-128:])
        }
```

#### Data Quality Monitoring
Real-time assessment of data quality:
- **Signal continuity**: Gap detection in data streams
- **Synchronization accuracy**: Timestamp alignment verification  
- **Hardware status**: Sensor connectivity and battery monitoring
- **Storage monitoring**: Available space and write performance

### Integration with External Systems

#### MATLAB Integration
```matlab
% Load MPDC4GSR session data
session_path = 'IRCamera_Sessions/session_20240115_143022/';
gsr_data = readtable([session_path 'gsr_data.csv']);
sync_events = readtable([session_path 'sync_events.csv']);

% Analyze GSR responses
baseline_data = gsr_data(gsr_data.timestamp_ms < sync_events.timestamp_ms(1), :);
stress_data = gsr_data(gsr_data.timestamp_ms > sync_events.timestamp_ms(2) & ...
                      gsr_data.timestamp_ms < sync_events.timestamp_ms(3), :);

% Calculate stress response metrics
baseline_mean = mean(baseline_data.conductance_us);
stress_peak = max(stress_data.conductance_us);
stress_response = stress_peak - baseline_mean;

fprintf('Stress response: %.3f µS\n', stress_response);
```

#### Python Analysis Pipeline
```python
import pandas as pd
import numpy as np
from scipy import signal
import matplotlib.pyplot as plt

class MPDCDataAnalyzer:
    def __init__(self, session_path):
        self.session_path = session_path
        self.load_data()
    
    def load_data(self):
        """Load all session data"""
        self.gsr_data = pd.read_csv(f"{self.session_path}/gsr_data.csv")
        self.sync_events = pd.read_csv(f"{self.session_path}/sync_events.csv")
        
        # Convert timestamps to datetime
        self.gsr_data['timestamp'] = pd.to_datetime(
            self.gsr_data['timestamp_ms'], unit='ms'
        )
    
    def extract_scrs(self, window_size=5, threshold=0.1):
        """Extract skin conductance responses"""
        # Apply low-pass filter
        conductance = self.gsr_data['conductance_us'].values
        filtered = signal.butter(3, 0.5, 'low', fs=128, output='sos')
        conductance_filtered = signal.sosfilt(filtered, conductance)
        
        # Detect peaks
        peaks, _ = signal.find_peaks(
            conductance_filtered, 
            height=threshold,
            distance=int(128 * window_size)  # Minimum 5s between peaks
        )
        
        return peaks, conductance_filtered
    
    def plot_session_overview(self):
        """Create comprehensive session visualization"""
        fig, axes = plt.subplots(3, 1, figsize=(15, 10))
        
        # GSR trace
        axes[0].plot(self.gsr_data['timestamp'], self.gsr_data['conductance_us'])
        axes[0].set_ylabel('Conductance (µS)')
        axes[0].set_title('GSR Data Stream')
        
        # Mark sync events
        for _, event in self.sync_events.iterrows():
            event_time = pd.to_datetime(event['timestamp_ms'], unit='ms')
            axes[0].axvline(event_time, color='red', linestyle='--', alpha=0.7)
        
        # Heart rate (if available)
        if 'heart_rate' in self.gsr_data.columns:
            axes[1].plot(self.gsr_data['timestamp'], self.gsr_data['heart_rate'])
            axes[1].set_ylabel('Heart Rate (BPM)')
        
        # Data quality indicators
        axes[2].plot(self.gsr_data['timestamp'], self.gsr_data['sample_index'])
        axes[2].set_ylabel('Sample Index')
        axes[2].set_xlabel('Time')
        
        plt.tight_layout()
        plt.savefig(f"{self.session_path}/analysis/session_overview.png")
        plt.show()

# Usage example
analyzer = MPDCDataAnalyzer("IRCamera_Sessions/session_20240115_143022/")
scr_peaks, filtered_gsr = analyzer.extract_scrs()
analyzer.plot_session_overview()
```

### Custom Sensor Integration

#### Adding New Sensors
The platform supports custom sensor integration through the `SensorRecorder` interface:

```kotlin
class CustomSensorRecorder : SensorRecorder {
    override suspend fun initialize(): Boolean {
        // Initialize custom hardware
        return customHardware.connect()
    }
    
    override suspend fun startRecording(session: Session, syncTime: Long): Boolean {
        val outputFile = File(session.dataDirectory, "custom_sensor_data.csv")
        
        return withContext(Dispatchers.IO) {
            // Start data collection
            customHardware.startSampling { sample ->
                // Write sample to file with synchronized timestamp
                val timestamp = System.nanoTime() + syncTime
                outputFile.appendText("$timestamp,${sample.value}\n")
            }
            true
        }
    }
    
    override suspend fun stopRecording(): SessionData {
        customHardware.stopSampling()
        return SessionData(
            sensorType = "custom_sensor",
            sampleCount = customHardware.getSampleCount(),
            dataFiles = listOf("custom_sensor_data.csv")
        )
    }
}
```

## 📞 Support and Resources

### Documentation
- **Quick Start**: [QUICK_START.md](QUICK_START.md)
- **Developer Guide**: [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)
- **API Reference**: [API_REFERENCE.md](API_REFERENCE.md)
- **Troubleshooting**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

### Community
- **GitHub Issues**: Report bugs and request features
- **Discussions**: Ask questions and share experiences
- **Wiki**: Community-contributed guides and tips

### Professional Support
For research institutions and commercial applications:
- Technical consultation for study design
- Custom sensor integration development
- Data analysis pipeline setup
- Performance optimization services

---

**Thank you for using MPDC4GSR!** 🚀

*This platform is designed to support high-quality physiological research. For questions, suggestions, or support, please don't hesitate to reach out through our GitHub repository.*