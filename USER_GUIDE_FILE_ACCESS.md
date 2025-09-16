# Accessing Recorded Session Files

This guide explains how to access and retrieve recorded session files from the IRCamera app on Android devices.

## File Storage Location

The app stores all recorded sessions in the app-specific external storage directory:
```
/Android/data/com.csl.irCamera/files/sessions/
```

This location is chosen for Android 12/13 compatibility and doesn't require special storage permissions.

## Session Directory Structure

Each recording session creates a unique folder with the following structure:

```
sessions/
└── 20231201_143022_456_SM_G998B_a1b2c3d4/    # Session ID
    ├── session_metadata.json                   # Session information
    ├── sync_markers.csv                        # Synchronization events
    ├── RGB/
    │   └── rgb_video.mp4                       # Video recording
    ├── Thermal/
    │   ├── thermal_frames.csv                  # Frame-by-frame data
    │   ├── thermal_metadata_data.csv           # Summary statistics
    │   └── thermal_calibration.json            # Camera calibration
    └── Shimmer/
        └── shimmer_data.csv                    # GSR and PPG data
```

## Accessing Files from PC

### Method 1: USB Connection (MTP)
1. Connect your Android device to PC via USB
2. Enable "File Transfer" mode when prompted
3. Navigate to: `Internal Storage` → `Android` → `data` → `com.csl.irCamera` → `files` → `sessions`
4. Copy desired session folders to your PC

**Note**: On some devices, the `Android/data` folder may be hidden. Enable "Show hidden files" in your file manager.

### Method 2: Developer Options (ADB)
If the files are not visible via MTP:

1. Enable Developer Options on your Android device
2. Enable USB Debugging
3. Use ADB to pull files:
   ```bash
   adb pull /sdcard/Android/data/com.csl.irCamera/files/sessions/ ./sessions/
   ```

### Method 3: In-App Export (Recommended)
The app provides built-in export functionality:

1. Open IRCamera app
2. Go to "Session Manager" 
3. Select a session
4. Tap the "Export" button
5. Choose export method (email, Google Drive, etc.)
6. The session will be packaged as a ZIP file for easy sharing

## Understanding the Data Files

### Session Metadata (`session_metadata.json`)
Contains comprehensive information about the recording session:
- Session timing and duration
- Enabled sensors and their status
- Device information
- File sizes and locations
- Any errors that occurred

### RGB Video (`RGB/rgb_video.mp4`)
- Standard MP4 video file
- Can be played with any video player
- Typically 1080p at 30fps
- Includes audio if microphone was enabled

### Thermal Data
- **`thermal_metadata_data.csv`**: Summary statistics per frame (min/max/avg temperatures)
- **`thermal_frames.csv`**: Full temperature matrix for each frame (large file)
- **`thermal_calibration.json`**: Camera calibration parameters

### Shimmer GSR Data (`Shimmer/shimmer_data.csv`)
- High-frequency GSR (Galvanic Skin Response) measurements
- PPG (Photoplethysmography) data
- Timestamp aligned with other sensors
- Sampling rate typically 128Hz or 256Hz

### Synchronization Data (`sync_markers.csv`)
- Events used for cross-sensor synchronization
- Session start/stop markers
- User-triggered events
- Critical for aligning data streams in analysis

## File Sizes and Storage Requirements

Typical file sizes for a 10-minute recording session:

| Data Type | Approximate Size | Description |
|-----------|------------------|-------------|
| RGB Video | 50-100 MB | 1080p MP4, depends on scene complexity |
| Thermal Frames | 20-40 MB | Full temperature matrices |
| Thermal Metadata | 1-2 MB | Summary statistics |
| Shimmer GSR | 5-10 MB | High-frequency sensor data |
| Session Metadata | < 1 MB | JSON configuration and status |
| **Total per session** | **75-150 MB** | Varies by duration and sensors |

## Storage Management

### Automatic Cleanup
The app automatically:
- Removes failed sessions (empty or corrupted)
- Warns when storage is low (< 1GB available)
- Prevents recording when critically low (< 500MB available)

### Manual Management
In the Session Manager:
- View storage usage in the subtitle bar
- Delete old sessions to free space
- Export sessions before deletion
- Filter sessions by data type or status

## Data Analysis Tips

### Synchronization
- Use `sync_markers.csv` to align data streams
- All timestamps are in nanoseconds (Unix epoch)
- Session start/stop markers provide common reference points

### CSV Format
All CSV files use standard formatting:
- UTF-8 encoding
- Comma-separated values
- Headers in first row
- Consistent timestamp format across all sensors

### Video Analysis
- RGB video includes accurate timestamps
- Frame rate information in session metadata
- Can be synchronized with sensor data using session start time

## Troubleshooting File Access

### Files Not Visible on PC
1. Check if "Show hidden files" is enabled in file manager
2. Try different USB connection modes (MTP vs PTP)
3. Use the in-app export feature instead
4. Enable Developer Options and use ADB

### Corrupted or Missing Files
1. Check session metadata for error information
2. Verify recording completed successfully (status = "COMPLETED")
3. Look for partial data files that may still contain useful information
4. Contact support with session ID for investigation

### Large File Sizes
1. Consider recording shorter sessions
2. Disable sensors not needed for your study
3. Use the export feature to compress files
4. Analyze data on device before transferring

## Privacy and Security

- All data is stored locally on the device
- No automatic cloud uploads
- Session IDs don't contain personal information
- Use participant IDs instead of real names
- Delete sessions after analysis to protect privacy

## Support

If you encounter issues accessing recorded files:
1. Check the app logs in Session Manager
2. Verify session completed without errors
3. Try different file access methods
4. Contact technical support with:
   - Device model and Android version
   - Session ID and timestamp
   - Screenshots of any error messages

---

**Note**: This storage approach ensures compatibility with Android 12+ scoped storage requirements while providing reliable access to recorded data for research and analysis purposes.
