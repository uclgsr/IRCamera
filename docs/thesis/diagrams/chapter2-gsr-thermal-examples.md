# Chapter 2: Basic GSR and Thermal Data Examples

## Figure 2.1: Basic GSR and Thermal Data Examples

Illustrative examples showing sample GSR signal and thermal image data to help readers unfamiliar with these modalities
understand the raw data characteristics.

### Part A: Sample GSR (Electrodermal Activity) Signal

#### A1: GSR Signal with Multiple SCR Events

```mermaid
---
config:
  themeVariables:
    xyChart:
      backgroundColor: "#ffffff"
      titleColor: "#000000"
---
xychart-beta
    title "GSR Signal During Stroop Task with Multiple Stimuli (3-minute recording)"
    x-axis "Time (seconds)" [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180]
    y-axis "Skin Conductance (microsiemens)" 3.0 --> 9.0
    line "Tonic SCL Baseline" [3.8, 3.85, 3.9, 3.95, 4.0, 4.05, 4.1, 4.15, 4.2, 4.25, 4.3, 4.35, 4.4, 4.45, 4.5, 4.55, 4.6, 4.65, 4.7]
    line "Measured GSR with SCRs" [3.8, 3.85, 3.9, 4.1, 5.8, 7.2, 7.8, 7.1, 6.2, 5.3, 4.8, 4.6, 4.5, 4.7, 6.3, 7.5, 8.2, 7.3, 6.5, 5.8, 5.2, 4.9, 4.8]
    line "Event Markers" [3.8, 3.8, 3.8, 8.5, 8.5, 3.8, 3.8, 3.8, 3.8, 3.8, 3.8, 3.8, 3.8, 8.5, 8.5, 3.8, 3.8, 3.8, 3.8]
```

#### A2: GSR Component Decomposition

```mermaid
flowchart TB
    subgraph "Raw GSR Signal Acquisition"
        Raw[Raw GSR Signal<br/>Shimmer3 ADC<br/>128 Hz Sampling<br/>16-bit Resolution]
        Resistance[Skin Resistance<br/>10 kOhm - 4.7 MOhm<br/>Measured across<br/>finger electrodes]
    end
    
    subgraph "Signal Processing Pipeline"
        Convert[Resistance to<br/>Conductance Conversion<br/>G = 1/R<br/>Units: microsiemens]
        Filter[Low-Pass Filter<br/>Butterworth 0.5 Hz<br/>Remove High-Freq Noise]
        Decompose[Signal Decomposition<br/>Separate Tonic/Phasic]
    end
    
    subgraph "Tonic Component SCL"
        Tonic[Tonic SCL<br/>Slow-varying baseline<br/>Changes over minutes<br/>Reflects overall arousal]
        TrendRemoval[Detrending<br/>Moving Average<br/>Window: 10 seconds]
    end
    
    subgraph "Phasic Component SCR"
        Phasic[Phasic SCR<br/>Rapid response events<br/>1-3s rise time<br/>5-15s recovery]
        PeakDetect[Peak Detection<br/>Amplitude threshold<br/>0.05 microS minimum]
        Latency[Response Latency<br/>1-3s after stimulus<br/>Peak at 3-5s]
    end
    
    subgraph "Extracted Features"
        Features[Feature Vector<br/>- SCR amplitude<br/>- SCR count<br/>- SCL mean<br/>- Rise/Recovery time<br/>- Peak latency]
    end
    
    Raw --> Resistance
    Resistance --> Convert
    Convert --> Filter
    Filter --> Decompose
    Decompose --> Tonic
    Decompose --> Phasic
    Tonic --> TrendRemoval
    Phasic --> PeakDetect
    PeakDetect --> Latency
    TrendRemoval --> Features
    Latency --> Features
    
    style Raw fill:#ffccbc
    style Convert fill:#fff9c4
    style Decompose fill:#c5e1a5
    style Tonic fill:#b3e5fc
    style Phasic fill:#f8bbd0
    style Features fill:#d1c4e9
```

**GSR Signal Characteristics:**

1. **Tonic Component (SCL - Skin Conductance Level)**
    - Slow-varying baseline: 3.8-5.0 μS range
    - Gradual drift over time (baseline shift)
    - Reflects overall arousal state
    - Changes over minutes to hours

2. **Phasic Component (SCR - Skin Conductance Response)**
    - Rapid events: spike at t=30s-50s
    - Peak amplitude: ~3.4 μS above baseline
    - Rise time: 1-3 seconds
    - Recovery time: 5-15 seconds
    - Triggered by stimulus or stress event

3. **Typical Response Pattern**
    - Latency: 1-3 seconds after stimulus
    - Peak: 3-5 seconds after onset
    - Amplitude: 0.1-0.8 μS typical stress response
    - Larger responses: 2-5 μS during intense stress
    - Non-responders: <0.05 μS (5-10% of population)

### Part B: Thermal Image Analysis and ROI Extraction

#### B1: Facial Thermal ROI Structure

```mermaid
graph TB
    subgraph "Thermal Image Frame 256×192 pixels @ 25fps"
        ThermalImg[Raw Thermal Frame<br/>TC001 Capture<br/>8-14 micron IR<br/>Radiometric Data]
        
        subgraph "Preprocessing"
            Calib[Radiometric Calibration<br/>Emissivity: 0.98<br/>Reflected Temp: 20°C<br/>Distance: 0.8m]
            FaceDetect[Face Detection<br/>MediaPipe/OpenCV<br/>Bounding Box Extraction]
            Registration[Image Registration<br/>Align with RGB frame<br/>Geometric Transform]
        end
        
        subgraph "ROI Segmentation"
            ROI1[Forehead Region<br/>Coordinates: 60,30 - 140,70<br/>Area: 80×40 pixels<br/>3200 temperature points]
            ROI2[Nose Tip Region<br/>Coordinates: 95,90 - 115,120<br/>Area: 20×30 pixels<br/>600 temperature points]
            ROI3[Left Cheek Region<br/>Coordinates: 50,80 - 110,130<br/>Area: 60×50 pixels<br/>3000 temperature points]
            ROI4[Right Cheek Region<br/>Coordinates: 145,80 - 205,130<br/>Area: 60×50 pixels<br/>3000 temperature points]
            ROI5[Periorbital Region<br/>Coordinates: 75,45 - 115,75<br/>Area: 40×30 pixels<br/>1200 temperature points]
            ROI6[Maxillary Region<br/>Coordinates: 80,120 - 120,145<br/>Area: 40×25 pixels<br/>1000 temperature points]
            ROI7[Nostril Region<br/>Coordinates: 100,110 - 115,125<br/>Area: 15×15 pixels<br/>225 temperature points<br/>Breathing Detection]
        end
        
        subgraph "Temperature Statistics Per ROI"
            Stats1[Mean Temperature<br/>Std Deviation<br/>Min/Max Values<br/>Temperature Gradient]
            Stats2[Temporal Changes<br/>Frame-to-frame Delta<br/>Trend Analysis<br/>Oscillation Detection]
        end
    end
    
    ThermalImg --> Calib
    Calib --> FaceDetect
    FaceDetect --> Registration
    
    Registration --> ROI1
    Registration --> ROI2
    Registration --> ROI3
    Registration --> ROI4
    Registration --> ROI5
    Registration --> ROI6
    Registration --> ROI7
    
    ROI1 --> Stats1
    ROI2 --> Stats1
    ROI3 --> Stats1
    ROI4 --> Stats1
    ROI5 --> Stats1
    ROI6 --> Stats1
    ROI7 --> Stats2
    
    style ThermalImg fill:#ff6b6b,stroke:#c92a2a,stroke-width:3px
    style Calib fill:#ffe066,stroke:#f59f00,stroke-width:2px
    style FaceDetect fill:#a5d8ff,stroke:#1c7ed6,stroke-width:2px
    style Registration fill:#b2f2bb,stroke:#2f9e44,stroke-width:2px
    style ROI1 fill:#ffe066,stroke:#f59f00,stroke-width:2px
    style ROI2 fill:#74c0fc,stroke:#1c7ed6,stroke-width:2px
    style ROI3 fill:#ffa94d,stroke:#fd7e14,stroke-width:2px
    style ROI4 fill:#ffa94d,stroke:#fd7e14,stroke-width:2px
    style ROI5 fill:#ffd43b,stroke:#fab005,stroke-width:2px
    style ROI6 fill:#d0bfff,stroke:#7950f2,stroke-width:2px
    style ROI7 fill:#99e9f2,stroke:#0c8599,stroke-width:2px
    style Stats1 fill:#e3fafc,stroke:#0b7285,stroke-width:2px
    style Stats2 fill:#d0ebff,stroke:#1971c2,stroke-width:2px
```

#### B2: Stress Response Thermal Patterns

```mermaid
flowchart LR
    subgraph Baseline["Baseline State (Resting)"]
        direction TB
        B1["Forehead<br/>34.2°C ± 0.3°C<br/>Stable"]
        B2["Nose Tip<br/>33.1°C ± 0.4°C<br/>Stable"]
        B3["Cheeks<br/>33.8°C ± 0.2°C<br/>Stable"]
        B4["Periorbital<br/>34.0°C ± 0.3°C<br/>Stable"]
    end
    
    subgraph Stressor["Stress Stimulus Applied"]
        direction TB
        S1["Stroop Task<br/>Color-Word Conflict"]
        S2["Cognitive Load<br/>Math Problems"]
        S3["Social Stress<br/>TSST Protocol"]
    end
    
    subgraph Response["Acute Stress Response (2-10s)"]
        direction TB
        R1["Forehead Warming<br/>34.8°C ± 0.5°C<br/>+0.6°C Delta<br/>Sympathetic Activation"]
        R2["Nose Cooling<br/>32.3°C ± 0.6°C<br/>-0.8°C Delta<br/>Vasoconstriction"]
        R3["Cheek Temperature<br/>33.5°C ± 0.4°C<br/>-0.3°C Delta<br/>Blood Redistribution"]
        R4["Breathing Change<br/>16-20 cycles/min<br/>+4 cycles/min<br/>Increased Arousal"]
    end
    
    subgraph Features["Extracted Features"]
        direction TB
        F1["Temperature Deltas<br/>ROI-specific changes<br/>Forehead/Nose ratio"]
        F2["Gradient Analysis<br/>Spatial patterns<br/>Temporal dynamics"]
        F3["Respiratory Rate<br/>Nostril oscillations<br/>Breathing frequency"]
        F4["Recovery Time<br/>Return to baseline<br/>15-60 seconds"]
    end
    
    Baseline --> Stressor
    Stressor --> Response
    Response --> Features
    
    B1 -.->|+0.6°C| R1
    B2 -.->|-0.8°C| R2
    B3 -.->|-0.3°C| R3
    B4 -.->|Monitor| R4
    
    style Baseline fill:#d0ebff,stroke:#1971c2
    style Stressor fill:#ffe3e3,stroke:#c92a2a
    style Response fill:#ffe066,stroke:#f59f00
    style Features fill:#d1c4e9,stroke:#7950f2
```

### Part C: Synchronized Multi-Modal Response Analysis

#### C1: Temporal Correlation Timeline

```mermaid
gantt
    title Synchronized Multi-Modal Stress Response Example (Stroop Task with Multiple Trials)
    dateFormat X
    axisFormat %S s
    
    section Stimulus Events
    Trial 1 Onset            :milestone, stim1, 0, 0s
    Stroop Display (5s)      :active, task1, 0, 5000ms
    Response Period          :active, task2, 5000, 7000ms
    Inter-Trial Interval     :crit, iti1, 7000, 12000ms
    Trial 2 Onset            :milestone, stim2, 12000, 12000ms
    Stroop Display (5s)      :active, task3, 12000, 17000ms
    Response Period          :active, task4, 17000, 19000ms
    Inter-Trial Interval     :crit, iti2, 19000, 24000ms
    Trial 3 Onset            :milestone, stim3, 24000, 24000ms
    
    section GSR Response (128 Hz)
    Baseline SCL 4.2μS       :crit, gsr1, 0, 1500ms
    SCR 1 Onset              :milestone, gsr2, 1500, 1500ms
    SCR 1 Rise               :active, gsr3, 1500, 3500ms
    SCR 1 Peak 7.1μS         :milestone, gsr4, 3500, 3500ms
    SCR 1 Recovery           :active, gsr5, 3500, 10000ms
    Partial Recovery 4.8μS   :milestone, gsr6, 10000, 10000ms
    SCR 2 Onset              :milestone, gsr7, 13500, 13500ms
    SCR 2 Rise               :active, gsr8, 13500, 15500ms
    SCR 2 Peak 7.8μS         :milestone, gsr9, 15500, 15500ms
    SCR 2 Recovery           :active, gsr10, 15500, 22000ms
    
    section Thermal Response (25 Hz)
    Baseline Nose 33.1°C     :crit, therm1, 0, 2000ms
    Vasoconstriction 1       :milestone, therm2, 2000, 2000ms
    Cooling Phase 1          :active, therm3, 2000, 6000ms
    Min Temp 32.3°C          :milestone, therm4, 6000, 6000ms
    Recovery Phase 1         :active, therm5, 6000, 13000ms
    Partial Recovery 32.8°C  :milestone, therm6, 13000, 13000ms
    Vasoconstriction 2       :milestone, therm7, 14000, 14000ms
    Cooling Phase 2          :active, therm8, 14000, 18000ms
    Min Temp 32.1°C          :milestone, therm9, 18000, 18000ms
    
    section RGB Indicators (30 fps)
    Neutral Expression       :crit, rgb1, 0, 2000ms
    Expression Change 1      :milestone, rgb2, 2000, 2000ms
    Facial Tension 1         :active, rgb3, 2000, 7000ms
    Relaxation               :active, rgb4, 7000, 13000ms
    Expression Change 2      :milestone, rgb5, 14000, 14000ms
    Facial Tension 2         :active, rgb6, 14000, 19000ms
    
    section rPPG Heart Rate (from RGB)
    Baseline 72 BPM          :crit, hr1, 0, 3000ms
    HR Increase              :active, hr2, 3000, 8000ms
    Peak 88 BPM              :milestone, hr3, 8000, 8000ms
    HR Decrease              :active, hr4, 8000, 13000ms
    Baseline Return 74 BPM   :milestone, hr5, 13000, 13000ms
    HR Increase 2            :active, hr6, 13000, 18000ms
    
    section Data Synchronization
    Timestamp Sync Point     :milestone, sync1, 0, 0ms
    128 GSR samples          :active, data1, 0, 1000ms
    25 Thermal frames        :active, data2, 0, 1000ms
    30 RGB frames            :active, data3, 0, 1000ms
```

#### C2: Cross-Modal Feature Correlation

```mermaid
flowchart TB
    subgraph Input["Raw Sensor Data @ t=0 to t=30s"]
        direction LR
        GSRData["GSR Time Series<br/>3,840 samples<br/>128 Hz × 30s<br/>16-bit resolution"]
        ThermalData["Thermal Frame Series<br/>750 frames<br/>25 Hz × 30s<br/>256×192 per frame"]
        RGBData["RGB Frame Series<br/>900 frames<br/>30 Hz × 30s<br/>1920×1080 per frame"]
    end
    
    subgraph GSRFeatures["GSR Feature Extraction"]
        direction TB
        GSRF1["SCR Count: 3 events<br/>SCR Amplitudes<br/>7.1, 7.8, 6.9 μS"]
        GSRF2["SCR Latencies<br/>1.5s, 1.5s, 1.3s"]
        GSRF3["Rise Times<br/>2.0s, 2.0s, 1.8s"]
        GSRF4["Recovery Times<br/>8.5s, 9.0s, 7.5s"]
        GSRF5["SCL Baseline<br/>Mean: 4.6 μS<br/>Trend: +0.1 μS/min"]
    end
    
    subgraph ThermalFeatures["Thermal Feature Extraction"]
        direction TB
        TF1["Nose Temperature<br/>Cooling Events: 3<br/>Deltas: -0.8, -1.0, -0.7°C"]
        TF2["Forehead Temperature<br/>Warming Events: 3<br/>Deltas: +0.6, +0.7, +0.5°C"]
        TF3["Breathing Rate<br/>Mean: 18 cycles/min<br/>Increase: +4 cycles/min"]
        TF4["Thermal Gradient<br/>Forehead-Nose ratio<br/>Increased during stress"]
    end
    
    subgraph RGBFeatures["RGB Feature Extraction"]
        direction TB
        RF1["Facial Action Units<br/>AU4 (Brow Lower): Active<br/>AU7 (Lid Tightener): Active"]
        RF2["rPPG Heart Rate<br/>Baseline: 72 BPM<br/>Peaks: 88, 90, 86 BPM"]
        RF3["Head Motion<br/>Tracking landmarks<br/>Stability metric"]
        RF4["Gaze Direction<br/>Eye tracking<br/>Attention metric"]
    end
    
    subgraph Correlation["Cross-Modal Correlation Analysis"]
        direction TB
        C1["GSR-Thermal Correlation<br/>SCR amplitude vs Nose cooling<br/>r = 0.78, p < 0.001"]
        C2["GSR-rPPG Correlation<br/>SCR amplitude vs HR increase<br/>r = 0.65, p < 0.01"]
        C3["Thermal-RGB Correlation<br/>Nose temp vs AU activation<br/>r = -0.72, p < 0.001"]
        C4["Temporal Alignment<br/>GSR leads thermal by 0.5-1.0s<br/>GSR leads rPPG by 1.0-1.5s"]
    end
    
    subgraph ML["Machine Learning Features"]
        direction TB
        MLF["Feature Vector (45D)<br/>- GSR: 10 features<br/>- Thermal: 15 features<br/>- RGB: 20 features<br/>For GSR prediction model"]
    end
    
    GSRData --> GSRF1 & GSRF2 & GSRF3 & GSRF4 & GSRF5
    ThermalData --> TF1 & TF2 & TF3 & TF4
    RGBData --> RF1 & RF2 & RF3 & RF4
    
    GSRF1 & GSRF2 & GSRF3 & GSRF4 & GSRF5 --> C1 & C2 & C4
    TF1 & TF2 & TF3 & TF4 --> C1 & C3 & C4
    RF1 & RF2 & RF3 & RF4 --> C2 & C3 & C4
    
    C1 & C2 & C3 & C4 --> MLF
    
    style Input fill:#e3f2fd,stroke:#1976d2,stroke-width:3px
    style GSRFeatures fill:#c5e1a5,stroke:#388e3c,stroke-width:2px
    style ThermalFeatures fill:#ffccbc,stroke:#f57c00,stroke-width:2px
    style RGBFeatures fill:#b3e5fc,stroke:#0277bd,stroke-width:2px
    style Correlation fill:#f8bbd0,stroke:#c2185b,stroke-width:3px
    style ML fill:#d1c4e9,stroke:#7950f2,stroke-width:3px
```

## Data Characteristics Summary

### GSR (Shimmer3 @ 128 Hz)

- **Format**: CSV with timestamp, resistance (kΩ), conductance (μS)
- **Sample Rate**: 128 samples/second
- **Resolution**: 16-bit (76 μΩ resolution)
- **Dynamic Range**: 0.01 - 100 μS
- **Typical File Size**: ~0.1 MB per minute
- **Key Features**: Tonic SCL, phasic SCRs, event markers

### Thermal Data (TC001 @ 25 Hz)

- **Format**: CSV with timestamp, temperature matrix (256×192 values)
- **Frame Rate**: 25 frames/second
- **Temperature Range**: Typically 28-40°C for facial imaging
- **Accuracy**: ±2°C absolute, ±0.1°C differential
- **Typical File Size**: ~30 MB per minute (raw temperature data)
- **Key Features**: ROI temperatures, spatial gradients, breathing cycles

### RGB Video (Phone Camera @ 30 fps)

- **Format**: MP4 (H.264 encoded)
- **Resolution**: 1920×1080 pixels
- **Frame Rate**: 30 frames/second
- **Typical File Size**: ~10 MB per minute (compressed)
- **Key Features**: Facial landmarks, expressions, rPPG, motion

## Why These Data Types Support GSR Prediction Research

### GSR as Ground Truth

- Provides validated measure of sympathetic arousal
- Fast response time (1-3s latency) enables precise correlation
- Continuous signal allows regression models (not just classification)
- Research-grade accuracy from validated Shimmer3 sensor

### Thermal as Contactless Predictor

- Captures involuntary physiological responses (vasoconstriction)
- Cannot be voluntarily controlled (unlike facial expressions)
- Temperature changes (0.3-0.7°C) correlate with GSR peaks
- Works in varying lighting conditions (infrared sensing)
- Provides spatial information (nose vs. forehead patterns)

### RGB as Complementary Context

- Adds behavioral indicators (expressions, movement)
- Remote photoplethysmography (rPPG) for heart rate
- Facial action units for emotion classification
- High spatial resolution for subtle features
- Validates attention and engagement during tasks

## Expected Correlations for ML Training

When synchronized properly, the platform enables training models to predict GSR from thermal/RGB:

1. **GSR SCR Peak** (t=3.5s) → **Nose Cooling** (t=6s, 3-4s lag)
2. **GSR Amplitude** (ΔμS) → **Cooling Magnitude** (Δ°C)
3. **GSR Baseline Drift** → **Mean Facial Temperature Increase**
4. **SCR Frequency** → **Breathing Rate Changes** (thermal nostril cycles)
5. **Combined RGB+Thermal** → **Improved GSR Prediction** over single modality

This figure establishes what raw sensor data looks like, justifying the design choices for sampling rates, data formats,
and expected signal characteristics needed for successful multi-modal analysis.








