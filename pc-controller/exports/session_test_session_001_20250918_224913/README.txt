IRCamera Multi-Modal Session Export
========================================

Session ID: test_session_001
Export Time: 2025-09-18 22:49:13

GSR Data Summary:
--------------------
  • Devices with GSR data: 2
  • Total GSR samples: 5
  • device_1: 3 samples
  • device_2: 2 samples

Camera Data Summary:
--------------------
  • Webcam active: No
  • Thermal camera devices: 0
  • RGB camera devices: 0

Connected Devices:
--------------------
  • device_1 (android_phone)
    Capabilities: gsr, thermal, rgb
  • device_2 (android_tablet)
    Capabilities: gsr

Export File Structure:
--------------------
  • gsr_data.csv - GSR sensor data
  • devices.json - Device information
  • session_metadata.json - Session metadata
  • cameras/ - Camera frames
    • local_webcam.jpg - PC webcam frame
    • thermal/ - Thermal camera frames
    • rgb/ - RGB camera frames
