# Chapter 2: Summary of Hardware Components and Specifications

## Table 2.1: Summary of Hardware Components and Specifications

This table provides a quick reference to the capabilities and constraints of the chosen hardware, helping readers understand the technical context and component selection rationale.

| Component | Model | Sensor Type | Resolution/Range | Sampling Rate | Interface | Key Specifications | Rationale for Selection |
|-----------|-------|-------------|------------------|---------------|-----------|-------------------|------------------------|
| **GSR Sensor** | Shimmer3 GSR+ | Electrodermal Activity (EDA) | 10kΩ - 4.7MΩ<br/>16-bit ADC | 128 Hz (configurable 51.2-512 Hz) | Bluetooth LE 4.0 | - Measurement range: 0.01-100 μS<br/>- Resolution: 76 μΩ<br/>- Excitation voltage: 0.5V<br/>- Weight: 22g<br/>- Battery: 450mAh (14 hours) | - Research-grade accuracy<br/>- Multi-channel (GSR, PPG, IMU)<br/>- Open SDK (Shimmer API)<br/>- Validated in peer-reviewed studies<br/>- Wireless for participant mobility |
| **Thermal Camera** | Topdon TC001 | Thermal Infrared Imaging | 256×192 pixels<br/>-20°C to +550°C | 25 fps | USB-C (OTG) | - Spectral range: 8-14 μm<br/>- Thermal sensitivity: <50 mK<br/>- Temperature accuracy: ±2°C or ±2%<br/>- FOV: 56° × 42°<br/>- Radiometric output: Yes | - Smartphone integration via USB<br/>- Higher resolution than FLIR One (160×120)<br/>- Radiometric temperature data access<br/>- Affordable (~$300)<br/>- InfiSense SDK support<br/>- 25Hz permits smooth tracking |
| **RGB Camera** | Phone Built-in Camera | High-Resolution Video | 1920×1080 pixels (1080p)<br/>Up to 4K capable | 30 fps (configurable) | Internal (CameraX API) | - Lens: Wide-angle (78° FOV typical)<br/>- Encoding: H.264/AVC<br/>- Autofocus: PDAF/Laser AF<br/>- Exposure: Auto-HDR<br/>- Storage: Local (MP4 format) | - Already integrated in smartphone<br/>- CameraX provides stable API<br/>- Efficient H.264 compression<br/>- Synchronized with thermal camera<br/>- High resolution for facial analysis<br/>- No additional hardware cost |
| **Android Device** | Samsung Galaxy S22 / Google Pixel 7 | Computing Platform | N/A | N/A | Wi-Fi 6, USB-C, BLE 5.0 | - Processor: Snapdragon 8 Gen 1 / Tensor G2<br/>- RAM: 8GB<br/>- Storage: 128-256GB<br/>- Display: 6.1-6.3" AMOLED<br/>- OS: Android 12+ | - Powerful CPU/GPU for real-time processing<br/>- Multiple connectivity options<br/>- USB OTG for thermal camera<br/>- Large storage for recordings<br/>- NTP time sync capability |
| **PC Controller** | Standard Desktop/Laptop | Control Station | N/A | N/A | Wi-Fi, Ethernet | - OS: Windows 10/11, Linux, macOS<br/>- Python 3.8+<br/>- Network: TCP/IP server<br/>- Storage: ≥100GB free | - Session orchestration and coordination<br/>- Multi-device management<br/>- Data aggregation and sync<br/>- Real-time monitoring dashboard<br/>- NTP time source |
| **Network** | Wi-Fi Router | Communication Infrastructure | N/A | N/A | IEEE 802.11ac/ax | - Frequency: 2.4/5 GHz dual-band<br/>- Latency: <5ms local network<br/>- Bandwidth: ≥100 Mbps | - Low-latency command delivery<br/>- Reliable connection for heartbeat<br/>- Local network (no internet required)<br/>- Supports multiple Android devices |

## Detailed Specifications

### Shimmer3 GSR+ Sensor

**Electrodermal Activity Measurement:**
- Constant voltage method (0.5V across Ag/AgCl electrodes)
- Wide dynamic range suitable for all skin types
- Motion artifacts detectable via integrated 3-axis accelerometer
- Additional PPG channel for heart rate (128 Hz, red + infrared LEDs)
- Real-time data streaming via Bluetooth with <20ms latency

**API Integration:**
- Official Shimmer Android API (Java)
- PyShimmer library for PC-side control
- Binary protocol with packet acknowledgment
- Configurable sampling rates (51.2 Hz to 512 Hz)
- Multi-device support (up to 10 sensors via BLE)

### Topdon TC001 Thermal Camera

**Thermal Imaging Capabilities:**
- Uncooled microbolometer sensor (VOx)
- 256×192 pixel native resolution (no interpolation)
- Radiometric mode provides temperature matrix per frame
- Temperature measurement accuracy within ±2°C absolute
- Suitable for detecting 0.3-0.7°C nasal cooling (stress response)

**Software Integration:**
- InfiSense IRUVC SDK (Android native)
- USB Video Class (UVC) protocol support
- Frame callback interface (IFrameCallback)
- Configurable emissivity and temperature range
- Real-time temperature matrix extraction

### RGB Camera (Phone Built-in)

**Video Recording Specifications:**
- CameraX Jetpack library for consistent API across devices
- H.264 hardware encoding for efficient storage (~10 MB/min at 1080p30)
- Frame timestamps aligned with system monotonic clock
- Autofocus and auto-exposure for varying conditions
- Video + audio recording capability

**Analysis Readiness:**
- MediaPipe face landmark detection (468 points)
- Remote photoplethysmography (rPPG) extraction
- Facial expression classification via computer vision
- High spatial resolution for subtle feature detection

## Hardware Selection Rationale Summary

The hardware combination provides:

1. **Ground Truth GSR**: Shimmer3 research-grade sensor for supervised learning
2. **Contactless Thermal**: TC001 captures involuntary stress responses (vasoconstriction)
3. **Visual Context**: RGB camera for expressions, rPPG, and behavioral cues
4. **Computational Power**: Modern smartphone handles all processing
5. **Synchronization**: All devices share unified timestamp system
6. **Cost-Effective**: Total hardware cost ~$1,500 (Shimmer + Topdon + phone)
7. **Portable Setup**: Entire system fits in small carrying case
8. **Extensible**: Additional sensors can be added via BLE or USB

This specification table establishes the technical foundation for understanding system capabilities and constraints referenced throughout the thesis.
