# TopInfrared Repository Overview (Mermaid Diagrams + Tables)

This document provides an at‑a‑glance overview of the TopInfrared (IRCamera) repository modules, relationships, and core feature flows using Mermaid diagrams, charts, and summary tables.

Note: Diagrams reflect the modules declared in `settings.gradle.kts` and common architectural patterns in this codebase.

---

## 1) Module Landscape — High-Level Dependency Graph

```mermaid
graph LR
  %% Categories
  subgraph Application
    APP[":app"]
  end

  subgraph Device and IO
    BLE[":BleModule"]
    LIBIR[":libir"]
    LIBMATRIX[":libmatrix"]
  end

  subgraph Core Libraries
    LIBAPP[":libapp"]
    LIBCOM[":libcom"]
    LIBUI[":libui"]
    LIBMENU[":libmenu"]
    LIBHIK[":libhik"]
    RANGE[":RangeSeekBar"]
    COMMONLIB[":commonlibrary"]
  end

  subgraph Feature Components
    CC[":component:CommonComponent"]
    EDIT3D[":component:edit3d"]
    HOUSE[":component:house"]
    PSEUDO[":component:pseudo"]
    THERMAL[":component:thermal"]
    THERMALIR[":component:thermal-ir"]
    THERMALLITE[":component:thermal-lite"]
    TRANSFER[":component:transfer"]
    USER[":component:user"]
    GSR[":component:gsr-recording"]
  end

  %% App depends on feature components and core libs
  APP --> CC
  APP --> EDIT3D
  APP --> HOUSE
  APP --> PSEUDO
  APP --> THERMAL
  APP --> THERMALIR
  APP --> THERMALLITE
  APP --> TRANSFER
  APP --> USER
  APP --> GSR

  APP --> LIBAPP
  APP --> LIBCOM
  APP --> LIBUI
  APP --> LIBMENU
  APP --> LIBHIK
  APP --> RANGE
  APP --> COMMONLIB

  %% Device and IO used by features
  THERMALIR --> LIBIR
  THERMAL --> LIBIR
  THERMALLITE --> LIBIR
  GSR --> BLE

  %% Math and Imaging helpers
  EDIT3D --> LIBMATRIX
  THERMALIR --> LIBMATRIX
```

---

## 2) Build and Packaging Pipeline (Gantt)

```mermaid
gantt
  title Android Build and Packaging Overview
  dateFormat  YYYY-MM-DD
  section Configure
  Gradle Configuration and Sync     :active, conf, 2025-09-01, 1d
  Dependency Resolution             :dep1, after conf, 1d
  section Compile and Link
  Compile Core Libraries            :lib, 1d
  Compile Feature Components        :feat, after lib, 1d
  Compile App                       :appc, after feat, 1d
  Resource Merge and Link - aapt2   :res, after appc, 1d
  section Test APKs
  androidTest Resource Link         :testres, after res, 1d
  Unit or Instrumentation Tests - if enabled :tests, after testres, 1d
  section Packaging
  Assemble APK or AAB - ProdDebug   :pkg, after tests, 1d
  Sign and Upload - as applicable   :sign, after pkg, 1d
```

---

## 3) Thermal Capture and Rendering — Flow Overview

```mermaid
flowchart LR
  A[IR Sensor / Camera] -->|Frames/Stream| B[libir]
  B --> C[component:thermal-ir]
  C --> D[libmatrix - image/transform helpers]
  D --> E[libui and libmenu - UI controls]
  E --> F[:app Activity/Fragment]
  F -->|User interaction| G[component:transfer - export/share]
```

---

## 4) GSR Synchronized Recording — Sequence Diagram

```mermaid
sequenceDiagram
  participant UI as :app UI
  participant GSR as component:gsr-recording
  participant BLE as :BleModule
  participant Core as :libapp
  participant Store as Storage/Export

  UI->>GSR: Start synchronized recording
  GSR->>BLE: Initialize BLE session and subscribe to sensors
  BLE-->>GSR: Stream GSR samples (timestamped)
  GSR->>Core: Buffer and align streams - timestamps and calibration
  Core-->>GSR: Confirm alignment window
  UI->>GSR: Stop recording
  GSR->>Store: Persist session - data and metadata
  UI-->>Store: Export/share via component:transfer
```

---

## 5) Module Categories — Pie Chart

```mermaid
pie title Modules by Category
  "Application" : 1
  "Device and IO" : 3
  "Core Libraries" : 7
  "Feature Components" : 10
```

Notes:
- Application: :app
- Device and IO: :BleModule, :libir, :libmatrix
- Core Libraries: :libapp, :libcom, :libui, :libmenu, :libhik, :RangeSeekBar, :commonlibrary
- Feature Components: :component:* (CommonComponent, edit3d, house, pseudo, thermal, thermal-ir, thermal-lite, transfer, user, gsr-recording)

---

## 6) Feature Map — Journey Chart (User Perspective)

```mermaid
journey
  title User Journey — Thermal Inspection
  section Setup
    Launch app: 4
    Connect device (USB/BLE): 3
  section Capture
    Live preview and palettes: 4
    Adjust emissivity/ROI: 3
    Take snapshot/record: 4
  section Analyze
    Temperature stats and overlays: 3
    Edit 3D/annotations: 2
  section Share
    Export image/video/report: 4
    Transfer to cloud/device: 3
```

---

## 7) Modules and Responsibilities — Summary Table

| Module | Path | Type | Primary Responsibilities |
|---|---|---|---|
| App | `:app` | Application | Entry point, DI/wiring, activities/fragments, packaging. |
| BleModule | `:BleModule` | Library | BLE communication and data streams for sensors (incl. GSR). |
| CommonLibrary | `:commonlibrary` | Library | Shared utility/resources across modules. |
| Core (libapp) | `:libapp` | Library | Core utilities, networking, Room, WorkManager, Glide/Lottie integration. |
| libcom | `:libcom` | Library | Common components/utilities for business logic. |
| libui | `:libui` | Library | UI widgets/styles/view utilities. |
| libmenu | `:libmenu` | Library | Menu/navigation UI elements and helpers. |
| libhik | `:libhik` | Library | Vendor-specific integrations (e.g., HIK). |
| libir | `:libir` | Library | IR capture/processing glue (native/FFmpeg/JavaCV integration). |
| libmatrix | `:libmatrix` | Library | Math/transform utilities for imaging/3D. |
| RangeSeekBar | `:RangeSeekBar` | Library | Slider/seekbar UI component. |
| component:CommonComponent | `:component:CommonComponent` | Feature | Shared feature scaffolding/common feature UIs. |
| component:edit3d | `:component:edit3d` | Feature | 3D editing/annotation for images/models. |
| component:house | `:component:house` | Feature | Domain feature (house inspection flows/UI). |
| component:pseudo | `:component:pseudo` | Feature | Pseudo-coloring/visualization helpers. |
| component:thermal | `:component:thermal` | Feature | Thermal feature set: palettes, measurement, overlays. |
| component:thermal-ir | `:component:thermal-ir` | Feature | IR device integration and thermal pipeline orchestration. |
| component:thermal-lite | `:component:thermal-lite` | Feature | Lightweight thermal feature subset for constrained devices. |
| component:transfer | `:component:transfer` | Feature | Export/sharing pipelines (files/cloud). |
| component:user | `:component:user` | Feature | User profile/settings/auth flows. |
| component:gsr-recording | `:component:gsr-recording` | Feature | Synchronized GSR recording and session management. |

---

## 8) Notes for Contributors
- Module list mirrors `settings.gradle.kts` and may evolve; update this document when modules are added/removed.
- For accurate dependency edges, review each module’s `build.gradle.kts` and reflect significant `implementation` relationships here.
- Mermaid rendering works in many tools (GitHub, JetBrains IDEs with Mermaid plugins). If diagrams don’t render, use a Mermaid viewer.

---

Last updated: 2025-09-08
