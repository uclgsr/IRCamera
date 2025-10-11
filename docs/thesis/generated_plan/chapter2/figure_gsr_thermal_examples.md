# Chapter 2: Basic GSR and Thermal Data Examples

## Figure 2.1: Basic GSR and Thermal Data Examples

Illustrative diagrams showing representative GSR traces and thermal ROI processing.

### Part A: Sample GSR (Electrodermal Activity) Signal

```mermaid
---
config:
  themeVariables:
    xyChart:
      backgroundColor: "#ffffff"
      titleColor: "#000000"
---
xychart-beta
    title "GSR Signal During Stroop Task (3-minute recording)"
    x-axis "Time (seconds)" [0, 20, 40, 60, 80, 100, 120, 140, 160, 180]
    y-axis "Skin Conductance (microsiemens)" 3.0 --> 9.0
    line "Tonic SCL Baseline" [3.8, 3.85, 3.9, 3.95, 4.0, 4.05, 4.1, 4.15, 4.2, 4.25]
    line "Measured GSR with SCRs" [3.8, 3.85, 3.9, 4.1, 5.8, 7.2, 7.8, 7.1, 6.2, 5.3]
    line "Stimulus Markers" [3.8, 3.8, 3.8, 8.5, 8.5, 3.8, 8.5, 8.5, 3.8, 3.8]
```

### Part B: Thermal Image Analysis and ROI Extraction

```mermaid
graph TB
    subgraph "Thermal Frame 256×192 @ 25Hz"
        ThermalImg[Raw Thermal Frame<br/>TC001 Capture<br/>Radiometric Data]

        subgraph "Preprocessing"
            Calib[Radiometric Calibration<br/>Emissivity 0.98<br/>Distance 0.8m]
            FaceDetect[Face Detection<br/>MediaPipe/OpenCV]
            Registration[Image Registration<br/>Align with RGB]
        end

        subgraph "ROI Segmentation"
            ROI1[Forehead ROI<br/>80×40 pixels]
            ROI2[Nose Tip ROI<br/>20×30 pixels]
            ROI3[Left Cheek ROI<br/>60×50 pixels]
            ROI4[Right Cheek ROI<br/>60×50 pixels]
            ROI5[Periorbital ROI<br/>40×30 pixels]
            ROI6[Maxillary ROI<br/>40×25 pixels]
            ROI7[Nostril ROI<br/>15×15 pixels<br/>Breathing detection]
        end

        subgraph "Temperature Statistics"
            Stats1[Mean/Std/Min/Max<br/>per ROI]
            Stats2[Temporal Trends<br/>Frame-to-frame Δ]
        end
    end

    ThermalImg --> Calib --> FaceDetect --> Registration
    Registration --> ROI1 --> Stats1
    Registration --> ROI2 --> Stats1
    Registration --> ROI3 --> Stats1
    Registration --> ROI4 --> Stats1
    Registration --> ROI5 --> Stats1
    Registration --> ROI6 --> Stats1
    Registration --> ROI7 --> Stats2

    style ThermalImg fill:#ff6b6b,stroke:#c92a2a,stroke-width:3px
    style Calib fill:#ffe066,stroke:#f59f00,stroke-width:2px
    style ROI1 fill:#ffa94d,stroke:#fd7e14,stroke-width:2px
    style Stats1 fill:#d1c4e9,stroke:#512da8,stroke-width:2px
```
