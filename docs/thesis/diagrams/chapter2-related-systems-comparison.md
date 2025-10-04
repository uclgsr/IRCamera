# Chapter 2: Related Systems and Methods Comparison

## Table 2.2: Related Systems and Methods Comparison

A comparative table of existing multi-sensor recording solutions and relevant literature, outlining features like sensor
types, synchronization methods, data accuracy, and limitations.

| System/Study                                             | Sensor Modalities                                                                                               | Synchronization Method                                                                                   | Data Accuracy                                                                                  | Key Features                                                                                                                                                                                                                           | Limitations                                                                                                                                           | Year  |
|----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|-------|
| **PhysioKit**<br/>(Reiter et al., 2023)                  | - ECG<br/>- PPG<br/>- EDA (GSR)<br/>- Respiration<br/>- Accelerometer                                           | - Microcontroller hardware sync<br/>- Shared clock base<br/>- Real-time streaming                        | - ECG: ±2 BPM<br/>- GSR: research-grade<br/>- Sampling: 100-500 Hz                             | - Open-source toolkit<br/>- Multi-user support<br/>- ML-driven quality checks<br/>- Real-time visualization<br/>- Modular architecture                                                                                                 | - No thermal imaging<br/>- Requires custom hardware<br/>- Limited to contact sensors<br/>- Complex assembly                                           | 2023  |
| **iBVP Dataset**<br/>(Cho et al., 2024)                  | - RGB facial video<br/>- Thermal facial video<br/>- Ear-PPG<br/>- Quality annotations                           | - Hardware trigger sync<br/>- Frame-level alignment<br/>- Manual validation                              | - Thermal: FLIR A655sc<br/>- RGB: HD camera<br/>- PPG: medical-grade                           | - High-quality dataset<br/>- Quality labels (manual + ML)<br/>- Varied conditions (rest, task, movement)<br/>- rPPG validation                                                                                                         | - Face-focused only<br/>- No GSR ground truth<br/>- Expensive equipment (FLIR A655sc)<br/>- Lab-only setting                                          | 2024  |
| **PsychoPy + LSL**<br/>(Standard Setup)                  | - Custom sensors via LSL<br/>- Stimulus presentation<br/>- Behavioral responses                                 | - LabStreamingLayer (LSL)<br/>- Network clock sync<br/>- Event markers                                   | - Sub-ms stimulus timing<br/>- LSL: ±1ms typical                                               | - Precise stimulus control<br/>- Multi-stream integration<br/>- Open-source<br/>- Wide sensor support                                                                                                                                  | - Complex LSL setup<br/>- No Android support (LSL NDK)<br/>- Requires PC per sensor<br/>- No built-in thermal                                         | N/A   |
| **FLIR Thermal Studies**<br/>(Zhang et al., 2021)        | - FLIR thermal camera<br/>- Contact GSR sensor<br/>- Optional ECG                                               | - Post-hoc timestamp alignment<br/>- Manual sync markers                                                 | - FLIR A655sc: 640×480, 0.02°C<br/>- GSR: research-grade                                       | - High-resolution thermal<br/>- Validated stress detection<br/>- 87.9% classification accuracy                                                                                                                                         | - Expensive equipment ($15k+ camera)<br/>- No real-time sync<br/>- Desktop-only setup<br/>- Limited portability                                       | 2021  |
| **Empatica E4 Wristband**                                | - EDA (GSR)<br/>- PPG<br/>- Accelerometer<br/>- Temperature                                                     | - Internal device clock<br/>- Cloud sync (delayed)<br/>- No multi-device sync                            | - GSR: 4 Hz only<br/>- PPG: 64 Hz<br/>- Commercial-grade                                       | - Wearable form factor<br/>- Easy deployment<br/>- Long battery life<br/>- Commercial support                                                                                                                                          | - Very low GSR sampling (4 Hz)<br/>- Cloud-only data access<br/>- Proprietary SDK<br/>- No camera integration<br/>- Cannot sync with external devices | 2015  |
| **Smartphone Camera rPPG**<br/>(Typical implementations) | - RGB camera only<br/>- rPPG (remote PPG)<br/>- Facial landmarks                                                | - N/A (single device)                                                                                    | - HR: ±2-5 BPM (controlled lighting)<br/>- Degrades outdoors                                   | - No additional hardware<br/>- Convenient<br/>- Real-time processing                                                                                                                                                                   | - Lighting sensitive<br/>- Voluntary control (expressions)<br/>- No thermal data<br/>- No ground-truth GSR<br/>- Motion artifacts                     | 2015+ |
| **Shimmer3 Stand-Alone**                                 | - GSR<br/>- PPG<br/>- IMU                                                                                       | - Internal clock<br/>- SD card logging<br/>- No external sync                                            | - GSR: 128 Hz, 16-bit<br/>- Research-grade accuracy                                            | - Wireless via BLE<br/>- Multi-channel recording<br/>- Open SDK<br/>- Validated hardware                                                                                                                                               | - No camera integration<br/>- Manual sync with other devices<br/>- PC required for real-time view<br/>- No stimulus control                           | 2014+ |
| **FLIR One Pro**<br/>(Consumer Thermal)                  | - Thermal camera<br/>- RGB camera (visual context)                                                              | - N/A (single device)                                                                                    | - Thermal: 160×120, 9 Hz<br/>- Temperature: ±3°C                                               | - Smartphone attachment<br/>- Affordable (~$400)<br/>- Portable<br/>- SDK available                                                                                                                                                    | - Low resolution (160×120)<br/>- Low frame rate (9 Hz)<br/>- No GSR integration<br/>- Limited accuracy (±3°C)<br/>- Restrictive SDK licensing         | 2017+ |
| **RTI Thermal Study**<br/>(2024 Report)                  | - FLIR Lepton thermal<br/>- Behavioral measures                                                                 | - Post-processing alignment                                                                              | - 160×120, 9 Hz<br/>- Stress: 0.3-0.7°C nasal cooling                                          | - Documented stress responses<br/>- Nasal ROI analysis<br/>- Correlation with subjective stress                                                                                                                                        | - Low thermal resolution<br/>- No real-time processing<br/>- No GSR validation<br/>- Manual analysis                                                  | 2024  |
| **This Work**<br/>(Multi-Modal Platform)                 | - Topdon TC001 thermal (256×192, 25Hz)<br/>- Shimmer3 GSR (128Hz, 16-bit)<br/>- Phone RGB camera (1080p, 30fps) | - NTP-style time sync<br/>- Unified timestamp system<br/>- TCP/IP coordination<br/>- Real-time alignment | - Thermal: ±2°C, 256×192, 25Hz<br/>- GSR: research-grade, 128Hz<br/>- Sync: <5ms median offset | - Integrated platform (3 modalities)<br/>- Smartphone-based portability<br/>- Real-time synchronization<br/>- Radiometric thermal data<br/>- Open-source architecture<br/>- Affordable (~$1,500 total)<br/>- Multi-device coordination | - Thermal resolution lower than FLIR A655sc<br/>- Requires PC controller<br/>- Wi-Fi network needed<br/>- No built-in quality annotation              | 2024  |

## Key Comparison Insights

### Synchronization Approaches

1. **Hardware Triggers** (iBVP): Most precise but requires specialized equipment
2. **LabStreamingLayer** (LSL): Excellent for PC-based multi-device, but no Android support
3. **Post-Hoc Alignment**: Simple but error-prone and prevents real-time use
4. **NTP-Style Sync** (This Work): Balances precision (<5ms) with practical implementation

### Sensor Coverage Comparison

- **Contact-Only** (PhysioKit, Shimmer): High accuracy but obtrusive
- **Thermal-Only** (FLIR studies): Contactless but lacks ground truth for validation
- **RGB-Only** (rPPG studies): Convenient but lighting-sensitive and voluntary control
- **Multi-Modal** (This Work): Combines ground truth GSR with contactless thermal/RGB

### Cost vs. Capability Trade-offs

| System            | Total Cost | Portability | Real-Time Sync | Multi-Modal | Open-Source |
|-------------------|------------|-------------|----------------|-------------|-------------|
| PhysioKit         | ~$800      | Medium      | ✓              | Limited     | ✓           |
| FLIR A655sc Setup | ~$18,000   | Low         | ✗              | ✓           | Partial     |
| Empatica E4       | ~$1,700    | High        | ✗              | Limited     | ✗           |
| FLIR One Pro      | ~$400      | High        | N/A            | Limited     | Partial     |
| This Work         | ~$1,500    | High        | ✓              | ✓           | ✓           |

## Novelty and Gap Addressed

This work addresses several gaps identified in existing systems:

1. **Integration Gap**: No existing system combines research-grade GSR with thermal imaging in a synchronized, portable
   setup
2. **Affordability Gap**: Scientific thermal cameras (FLIR A655sc) cost $15k+; this work uses $300 TC001 with acceptable
   specs
3. **Real-Time Sync Gap**: Most multi-modal studies use post-hoc alignment; this work synchronizes in real-time
4. **Platform Flexibility**: Single-device systems (E4, FLIR One) cannot coordinate with external sensors; this work
   orchestrates multiple devices
5. **Open Architecture**: Many commercial systems (E4, FLIR SDK) are proprietary; this work is open-source
6. **Mobile Platform**: Most research systems are PC-based; this work leverages smartphone computing power
7. **Validation Data**: This work provides ground-truth GSR for training contactless models, unlike thermal-only or
   RGB-only approaches

## Situating This Research

This thesis contributes a **middle-ground solution**:

- More capable than single-device consumer products (FLIR One, E4)
- More affordable and portable than high-end research setups (FLIR A655sc)
- More integrated than existing multi-tool approaches (PsychoPy + LSL + separate sensors)
- Specifically designed for GSR prediction research (combines ground truth + contactless modalities)

The platform enables future research to train supervised machine learning models using synchronized multi-modal data,
advancing the goal of contactless stress monitoring with scientific rigor.
