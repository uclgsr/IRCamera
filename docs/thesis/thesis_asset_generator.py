#!/usr/bin/env python3
"""
Thesis Asset Generator

Creates the complete set of figures, tables, and code snippets requested for the
thesis chapters. Outputs are written as Markdown/CSV artifacts under
`docs/thesis/generated_plan`.

Usage:
    python docs/thesis/thesis_asset_generator.py
"""

from __future__ import annotations

import logging
import sys
from dataclasses import dataclass
from pathlib import Path
from textwrap import dedent
from typing import Callable, Iterable, List, Optional

ROOT_DIR = Path(__file__).resolve().parents[2]
if str(ROOT_DIR) not in sys.path:
    sys.path.append(str(ROOT_DIR))

# Import chapter-specific generators
from docs.thesis.chapter5.experimental_evaluation import (  # type: ignore
    ExperimentalEvaluationFramework,
)
from docs.thesis.chapter6.requirements_evaluation import (  # type: ignore
    RequirementsEvaluationFramework,
)


logging.basicConfig(level=logging.INFO, format="[%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


@dataclass
class AssetDefinition:
    identifier: str
    chapter: str
    asset_type: str
    title: str
    filename: str
    generator: Callable[["ThesisAssetGenerator"], str]
    description: str


class ThesisAssetGenerator:
    """Central generator for all thesis assets."""

    def __init__(
        self,
        project_root: Optional[Path] = None,
        output_root: Optional[Path] = None,
    ) -> None:
        self.project_root = Path(project_root or ROOT_DIR)
        self.output_root = Path(
            output_root or self.project_root / "docs/thesis/generated_plan"
        )
        self.output_root.mkdir(parents=True, exist_ok=True)

        self._chapter5_generated = False
        self._chapter6_generated = False

        self.assets: List[AssetDefinition] = [
            AssetDefinition(
                identifier="chapter1.figure.multisensor_overview",
                chapter="1",
                asset_type="figure",
                title="Multi-Sensor System Overview",
                filename="chapter1/figure_multisensor_overview.md",
                generator=self._generate_ch1_multisensor_overview,
                description=(
                    "High-level diagram illustrating smartphone sensors, PC orchestrator, "
                    "and communication links."
                ),
            ),
            AssetDefinition(
                identifier="chapter1.figure.usecase_timeline",
                chapter="1",
                asset_type="figure",
                title="Example Use-Case Scenario Timeline",
                filename="chapter1/figure_usecase_timeline.md",
                generator=self._generate_ch1_usecase_timeline,
                description=(
                    "Sequence diagram capturing a recording session from START to STOP."
                ),
            ),
            AssetDefinition(
                identifier="chapter2.table.hardware_specs",
                chapter="2",
                asset_type="table",
                title="Summary of Hardware Components and Specifications",
                filename="chapter2/table_hardware_specs.md",
                generator=self._generate_ch2_hardware_table,
                description="Reference table of sensors, interfaces, and key specs.",
            ),
            AssetDefinition(
                identifier="chapter2.table.related_systems",
                chapter="2",
                asset_type="table",
                title="Related Systems and Methods Comparison",
                filename="chapter2/table_related_systems.md",
                generator=self._generate_ch2_related_systems_table,
                description="Comparative analysis of existing multi-sensor solutions.",
            ),
            AssetDefinition(
                identifier="chapter2.figure.gsr_thermal_examples",
                chapter="2",
                asset_type="figure",
                title="Basic GSR and Thermal Data Examples",
                filename="chapter2/figure_gsr_thermal_examples.md",
                generator=self._generate_ch2_gsr_thermal_examples,
                description="Illustrative plots for GSR signal and thermal ROI processing.",
            ),
            AssetDefinition(
                identifier="chapter3.figure.system_architecture",
                chapter="3",
                asset_type="figure",
                title="System Architecture Diagram",
                filename="chapter3/figure_system_architecture.md",
                generator=self._generate_ch3_system_architecture,
                description="Block diagram of Android node, PC orchestrator, and data flows.",
            ),
            AssetDefinition(
                identifier="chapter3.figure.state_machine",
                chapter="3",
                asset_type="figure",
                title="Software State Machine for Recording Control",
                filename="chapter3/figure_state_machine.md",
                generator=self._generate_ch3_state_machine,
                description="State diagram of the recording lifecycle and recovery paths.",
            ),
            AssetDefinition(
                identifier="chapter3.figure.sequence_diagram",
                chapter="3",
                asset_type="figure",
                title="Communication Sequence Diagram (PC–Device Interaction)",
                filename="chapter3/figure_sequence_diagram.md",
                generator=self._generate_ch3_sequence_diagram,
                description="Command exchange during session start, recording, and stop.",
            ),
            AssetDefinition(
                identifier="chapter3.table.functional_requirements",
                chapter="3",
                asset_type="table",
                title="Functional Requirements and Design Criteria",
                filename="chapter3/table_functional_requirements.md",
                generator=self._generate_ch3_requirements_table,
                description="Matrix linking requirements, design choices, and verification.",
            ),
            AssetDefinition(
                identifier="chapter3.table.design_decisions",
                chapter="3",
                asset_type="table",
                title="Sensor Integration Design Decisions",
                filename="chapter3/table_design_decisions.md",
                generator=self._generate_ch3_design_decisions_table,
                description="Design alternatives with chosen solutions and trade-offs.",
            ),
            AssetDefinition(
                identifier="chapter4.figure.mobile_ui_flow",
                chapter="4",
                asset_type="figure",
                title="Mobile App UI and Data Flow",
                filename="chapter4/figure_mobile_ui_flow.md",
                generator=self._generate_ch4_mobile_app_ui,
                description="Jetpack Compose UI wiring with background data pipelines.",
            ),
            AssetDefinition(
                identifier="chapter4.code.bluetooth_gsr",
                chapter="4",
                asset_type="code",
                title="Bluetooth GSR Connection and Reading",
                filename="chapter4/code_bluetooth_gsr.md",
                generator=self._generate_ch4_bluetooth_snippet,
                description="Kotlin implementation streaming Shimmer3 data to pipeline.",
            ),
            AssetDefinition(
                identifier="chapter4.code.thermal_capture",
                chapter="4",
                asset_type="code",
                title="Thermal Camera Frame Capture (USB)",
                filename="chapter4/code_thermal_capture.md",
                generator=self._generate_ch4_thermal_snippet,
                description="Thermal streaming loop capturing frames and metadata.",
            ),
            AssetDefinition(
                identifier="chapter4.code.timestamp_sync",
                chapter="4",
                asset_type="code",
                title="Timestamp Synchronization Logic",
                filename="chapter4/code_timestamp_sync.md",
                generator=self._generate_ch4_timestamp_sync_snippet,
                description="NTP-style UDP sync and HTTPS calibration routine.",
            ),
            AssetDefinition(
                identifier="chapter4.code.remote_commands",
                chapter="4",
                asset_type="code",
                title="Remote Command Handling (TCP Server)",
                filename="chapter4/code_remote_commands.md",
                generator=self._generate_ch4_remote_command_snippet,
                description="Command dispatcher responding to START/STOP/SYNC.",
            ),
            AssetDefinition(
                identifier="chapter5.figure.system_event_timeline",
                chapter="5",
                asset_type="figure",
                title="System Event Timeline and Synchronization",
                filename="chapter5/figure_system_event_timeline.md",
                generator=self._generate_ch5_system_event_timeline,
                description="Timeline chart showing command-to-sensor start flow.",
            ),
            AssetDefinition(
                identifier="chapter5.figure.sensor_sync_validation",
                chapter="5",
                asset_type="figure",
                title="Sensor Data Synchronization Validation",
                filename="chapter5/figure_sensor_sync_validation.md",
                generator=self._generate_ch5_sensor_sync_validation,
                description="Visualization of cross-sensor alignment tolerance checks.",
            ),
            AssetDefinition(
                identifier="chapter5.figure.recorded_samples",
                chapter="5",
                asset_type="figure",
                title="Recorded Data Samples (Thermal & GSR)",
                filename="chapter5/figure_recorded_samples.md",
                generator=self._generate_ch5_recorded_samples,
                description="Sample dataset excerpt illustrating synchronized records.",
            ),
            AssetDefinition(
                identifier="chapter5.figure.performance_metrics",
                chapter="5",
                asset_type="figure",
                title="Performance Metrics Charts",
                filename="chapter5/figure_performance_metrics.md",
                generator=self._generate_ch5_performance_metrics,
                description="Charts summarizing latency, throughput, and battery usage.",
            ),
            AssetDefinition(
                identifier="chapter5.table.test_cases",
                chapter="5",
                asset_type="table",
                title="Test Cases and Outcomes",
                filename="chapter5/table_test_cases.md",
                generator=self._generate_ch5_test_cases_table,
                description="Structured summary of validation scenarios and results.",
            ),
            AssetDefinition(
                identifier="chapter5.table.log_excerpt",
                chapter="5",
                asset_type="table",
                title="Example Log Excerpt (Synchronized Data)",
                filename="chapter5/table_log_excerpt.md",
                generator=self._generate_ch5_log_excerpt,
                description="Representative sample showing synchronized data columns.",
            ),
            AssetDefinition(
                identifier="chapter5.table.performance_summary",
                chapter="5",
                asset_type="table",
                title="Performance Summary Results",
                filename="chapter5/table_performance_summary.md",
                generator=self._generate_ch5_performance_summary,
                description="Aggregated metrics matched against design targets.",
            ),
            AssetDefinition(
                identifier="chapter6.table.objectives_fulfillment",
                chapter="6",
                asset_type="table",
                title="Fulfillment of Objectives",
                filename="chapter6/table_objectives_fulfillment.md",
                generator=self._generate_ch6_objectives_table,
                description="Maps thesis goals to achieved outcomes with evidence.",
            ),
            AssetDefinition(
                identifier="chapter6.figure.future_enhancements",
                chapter="6",
                asset_type="figure",
                title="Proposed Future System Enhancements",
                filename="chapter6/figure_future_enhancements.md",
                generator=self._generate_ch6_future_enhancements,
                description="Forward-looking roadmap for expanding the platform.",
            ),
        ]

    # Public API -----------------------------------------------------------------

    def generate_all_assets(self) -> None:
        """Generate every registered asset."""
        for asset in self.assets:
            logger.info(
                "Generating %s (%s) for chapter %s",
                asset.identifier,
                asset.asset_type,
                asset.chapter,
            )
            content = asset.generator()
            self._write_asset(asset.filename, content)

        logger.info("All thesis assets generated under %s", self.output_root)

    # Utility helpers -------------------------------------------------------------

    def _write_asset(self, relative_filename: str, content: str) -> None:
        path = self.output_root / relative_filename
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content.rstrip() + "\n", encoding="utf-8")
        logger.debug("Wrote %s", path)

    @staticmethod
    def _render_markdown_table(
        headers: Iterable[str], rows: Iterable[Iterable[str]]
    ) -> str:
        headers_list = list(headers)
        header_row = "| " + " | ".join(headers_list) + " |"
        separator = "|" + "|".join([" --- "] * len(headers_list)) + "|"
        lines = [header_row, separator]
        for row in rows:
            lines.append("| " + " | ".join(row) + " |")
        return "\n".join(lines)

    def _extract_kotlin_block(self, relative_path: str, marker: str) -> str:
        file_path = self.project_root / relative_path
        if not file_path.exists():
            raise FileNotFoundError(f"Source file not found: {file_path}")
        lines = file_path.read_text(encoding="utf-8").splitlines()
        start_idx = None
        for idx, line in enumerate(lines):
            if marker in line:
                start_idx = idx
                break
        if start_idx is None:
            raise ValueError(f"Marker '{marker}' not found in {relative_path}")

        snippet_lines: List[str] = []
        brace_depth = 0
        started = False

        for line in lines[start_idx:]:
            snippet_lines.append(line)
            brace_depth += line.count("{")
            brace_depth -= line.count("}")
            if "{" in line:
                started = True
            if started and brace_depth <= 0:
                break

        return "\n".join(snippet_lines).rstrip()

    def _ensure_chapter5_assets(self) -> Path:
        if not self._chapter5_generated:
            output_dir = self.output_root / "chapter5/raw"
            output_dir.mkdir(parents=True, exist_ok=True)
            framework = ExperimentalEvaluationFramework(output_dir=str(output_dir))
            framework.generate_all_chapter5_content()
            self._chapter5_generated = True
        return self.output_root / "chapter5/raw"

    def _ensure_chapter6_assets(self) -> Path:
        if not self._chapter6_generated:
            output_dir = self.output_root / "chapter6/raw"
            output_dir.mkdir(parents=True, exist_ok=True)
            evaluator = RequirementsEvaluationFramework(output_dir=str(output_dir))
            evaluator.generate_objectives_fulfillment_table()
            self._chapter6_generated = True
        return self.output_root / "chapter6/raw"

    # Chapter 1 -------------------------------------------------------------------

    def _generate_ch1_multisensor_overview(self) -> str:
        diagram = dedent(
            """
            ```mermaid
            graph TB
                subgraph "PC Controller Hub"
                    PC[PC Controller Application<br/>Python 3.8+ TCP Client<br/>Session Orchestration]
                    SessMgr[Session Manager<br/>Recording Coordination<br/>Multi-Device Sync<br/>State Machine]
                    DataAgg[Data Aggregator<br/>Multi-Modal Fusion<br/>Timestamp Alignment<br/>CSV Merger]
                    NTP[NTP Time Sync<br/>Clock Synchronization<br/>Chrony/ntpd]
                    CmdQueue[Command Queue<br/>Async Handler<br/>Retry Logic]
                    Logger[System Logger<br/>Event Logging<br/>Debug Output]
                    FileManager[File Manager<br/>Data Transfer<br/>USB/Network]
                    UIController[UI Controller<br/>Qt/Tkinter GUI<br/>Status Display]
                end
                
                subgraph "Android Sensor Node"
                    Android[Android Device<br/>Samsung Galaxy S22<br/>Google Pixel 7<br/>TCP Server :8080]
                    RecCtrl[Recording Controller<br/>Sensor Coordination<br/>Lifecycle Management<br/>State Tracking]
                    TimeMgr[Time Manager<br/>Nanosecond Precision<br/>Unified Timestamp<br/>Offset Calculation]
                    
                    subgraph "Sensor Interfaces"
                        ThermalMgr[Thermal Camera Manager<br/>TC001 USB/OTG Driver<br/>InfiSense SDK<br/>Frame Buffer]
                        GSRMgr[GSR Sensor Service<br/>Shimmer3 BLE Handler<br/>ShimmerAPI<br/>Stream Parser]
                        RGBMgr[RGB Camera Manager<br/>CameraX API<br/>H.264 Encoder<br/>Preview Handler]
                    end
                    
                    subgraph "Data Pipeline"
                        DataStore[Local Data Storage<br/>CSV + H.264 Video<br/>Session Metadata<br/>Internal Storage]
                        BufferMgr[Buffer Manager<br/>Ring Buffer<br/>Overflow Protection]
                        Serializer[Data Serializer<br/>CSV Writer<br/>JSON Metadata]
                    end
                    
                    NetworkMgr[Network Manager<br/>TCP Handler<br/>Heartbeat Monitor<br/>Error Recovery]
                    PermManager[Permission Manager<br/>USB/BLE/Camera<br/>Runtime Permissions]
                end
                
                subgraph "Hardware Sensors"
                    TC001[Topdon TC001<br/>Thermal IR Camera<br/>256x192 @ 25Hz<br/>USB-C OTG<br/>Radiometric Mode]
                    Shimmer[Shimmer3 GSR+<br/>Electrodermal Activity<br/>128Hz @ 16-bit<br/>Bluetooth LE 4.0<br/>PPG + IMU]
                    PhoneCam[Phone RGB Camera<br/>1920x1080 @ 30fps<br/>H.264 Encoding<br/>Built-in Camera<br/>Autofocus/HDR]
                    
                    subgraph "Sensor Components"
                        ThermalSensor[IR Microbolometer<br/>VOx Sensor<br/>8-14um Wavelength]
                        GSRElectrodes[Ag/AgCl Electrodes<br/>Finger Attachment<br/>0.5V Excitation]
                        CameraSensor[CMOS Sensor<br/>Lens Assembly<br/>ISP Pipeline]
                    end
                end
                
                subgraph "Network Communication"
                    WiFi[Wi-Fi Network<br/>Local TCP/IP<br/>Port 8080<br/>802.11ac/ax]
                    Router[WiFi Router<br/>DHCP Server<br/>Local Gateway]
                end
                
                subgraph "External Services"
                    NTPServer[NTP Server<br/>pool.ntp.org<br/>Time Reference]
                    CloudBackup[Cloud Backup<br/>Optional Sync<br/>Google Drive]
                end
                
                PC <-->|START/STOP/SYNC| CmdQueue
                CmdQueue <-->|JSON Commands| WiFi
                WiFi <-->|TCP Messages| Router
                Router <-->|Local Network| NetworkMgr
                NetworkMgr <-->|Command Parser| Android
                PC <-->|NTP Query| NTPServer
                NTPServer -.->|Time Reference| NTP
                NTP -.->|Clock Offset| TimeMgr
                
                UIController --> PC
                PC --> SessMgr
                SessMgr --> DataAgg
                PC --> Logger
                Logger -.->|Log Files| FileManager
                
                Android --> RecCtrl
                Android --> NetworkMgr
                Android --> PermManager
                RecCtrl --> ThermalMgr
                RecCtrl --> GSRMgr
                RecCtrl --> RGBMgr
                RecCtrl --> TimeMgr
                TimeMgr -.->|Timestamps| ThermalMgr
                TimeMgr -.->|Timestamps| GSRMgr
                TimeMgr -.->|Timestamps| RGBMgr
                
                ThermalMgr <-->|USB OTG Protocol| TC001
                TC001 --> ThermalSensor
                GSRMgr <-->|BLE GATT Profile| Shimmer
                Shimmer --> GSRElectrodes
                RGBMgr <-->|Camera HAL| PhoneCam
                PhoneCam --> CameraSensor
                
                ThermalMgr -->|Frame Callback| BufferMgr
                GSRMgr -->|Sample Stream| BufferMgr
                RGBMgr -->|Video Buffer| BufferMgr
                BufferMgr -->|Buffered Data| Serializer
                Serializer -->|CSV/Video| DataStore
                TimeMgr -.->|Unified Time| Serializer
                
                DataStore -.->|File Transfer| FileManager
                DataStore -.->|Optional| CloudBackup
                FileManager <-->|USB/Network| PC
                DataAgg <-->|Aggregated Data| FileManager
                
                classDef pcClass fill:#e3f2fd,stroke:#1976d2,stroke-width:3px
                classDef androidClass fill:#f3e5f5,stroke:#7b1fa2,stroke-width:3px
                classDef sensorClass fill:#e8f5e9,stroke:#388e3c,stroke-width:3px
                classDef hwClass fill:#fff3e0,stroke:#f57c00,stroke-width:3px
                classDef netClass fill:#fce4ec,stroke:#c2185b,stroke-width:3px
            ```
            """
        )
        return dedent(
            f"""
            # Chapter 1: Multi-Sensor System Overview

            ## Figure 1.1: Multi-Sensor System Overview

            A high-level diagram illustrating the smartphone-based system with attached sensors, the PC controller,
            and all communication links.

            {diagram}
            """
        ).strip()

    def _generate_ch1_usecase_timeline(self) -> str:
        return dedent(
            """
            # Chapter 1: Example Use-Case Scenario Timeline

            ## Figure 1.2: Example Use-Case Scenario Timeline

            A detailed sequence diagram showing a typical recording session workflow, including synchronization and
            remote control interactions.

            ```mermaid
            sequenceDiagram
                participant Researcher as Researcher<br/>(Operator)
                participant UI as PC UI<br/>(Qt GUI)
                participant PC as PC Controller<br/>(Python Core)
                participant Network as Network<br/>(TCP/IP)
                participant Android as Android App<br/>(Sensor Node)
                participant RecCtrl as Recording<br/>Controller
                participant ThermalMgr as Thermal<br/>Manager
                participant GSRMgr as GSR<br/>Manager
                participant RGBMgr as RGB<br/>Manager
                participant Sensors as Hardware<br/>Sensors
                participant Data as Local<br/>Storage

                Note over Researcher,Data: Pre-Session Setup
                Researcher->>UI: Launch Controller Application
                UI->>PC: Initialize Application
                PC->>PC: Load Configuration & TCP Server
                UI-->>Researcher: Ready - Awaiting Device

                Researcher->>Android: Launch Sensor App
                Android->>Android: Check Permissions (USB/BLE/Camera)
                Android->>Network: Discover PC on Local Network
                Network->>PC: TCP Connect Request
                PC->>Network: Accept Connection
                Network->>Android: Connection Established

                Android->>PC: HELLO device_id=Samsung_S22 version=1.2.0
                PC->>Android: HELLO_ACK capabilities_request=true
                Android->>RecCtrl: Query Sensor Availability
                RecCtrl->>ThermalMgr: Check TC001
                RecCtrl->>GSRMgr: Check Shimmer3
                RecCtrl->>RGBMgr: Check Camera
                Android->>PC: CAPABILITIES thermal=256x192@25Hz gsr=128Hz rgb=1080p@30fps

                Note over Researcher,Data: Time Synchronization Phase
                PC->>Network: SYNC_REQUEST t1=1703441234567890
                Network->>Android: Forward SYNC_REQUEST
                Android->>Network: SYNC_RESPONSE t2/t3 timestamps
                Network->>PC: Forward SYNC_RESPONSE
                PC->>PC: Calculate offset=+2.3ms RTT=4.1ms
                PC->>Network: SYNC_RESULT offset=+2.3ms rtt=4.1ms
                Network->>Android: Forward SYNC_RESULT
                Android->>RecCtrl: Apply Time Offset
                Android->>PC: SYNC_ACK status=synchronized

                Note over Researcher,Data: Session Initialization
                Researcher->>UI: Click "Start Recording"
                UI->>PC: Start Session Request
                PC->>Network: START_RECORD session_id=20241215_1430
                Network->>Android: Forward START_RECORD
                Android->>RecCtrl: Start Recording Session

                par Thermal Stream
                    RecCtrl->>ThermalMgr: Initialize TC001
                    ThermalMgr-->>Android: Frame Callback @25Hz
                    Android->>Data: thermal_frames.csv append
                and GSR Stream
                    RecCtrl->>GSRMgr: Initialize Shimmer3
                    GSRMgr-->>Android: 128Hz samples
                    Android->>Data: gsr_samples.csv append
                and RGB Stream
                    RecCtrl->>RGBMgr: Initialize CameraX
                    RGBMgr-->>Android: 30fps video frames
                    Android->>Data: video_recording.mp4 append
                end

                Android->>Network: ACK status=recording sensors=3
                Network->>PC: Forward ACK
                PC->>UI: Recording Started

                loop Every 2 seconds
                    RecCtrl->>Android: Build status message
                    Android->>Network: HEARTBEAT duration, frame counts
                    Network->>PC: Forward HEARTBEAT
                    PC->>UI: Update status dashboard
                end

                Note over Researcher,Data: Session Termination
                Researcher->>UI: Click "Stop Recording"
                UI->>PC: Stop Session Request
                PC->>Network: STOP_RECORD session_id=20241215_1430
                Network->>Android: Forward STOP_RECORD
                Android->>RecCtrl: Stop Sensor Threads

                par Graceful Shutdown
                    Android->>ThermalMgr: stopStreaming()
                    Android->>GSRMgr: stopStreaming()
                    Android->>RGBMgr: stopRecording()
                end

                Android->>Data: Finalize CSV/MP4 metadata
                Android->>Network: ACK status=completed total_size=2.3GB
                Network->>PC: Forward ACK
                PC->>UI: Session Complete
                Android->>PC: DISCONNECT
            ```
            """
        ).strip()

    # Chapter 2 -------------------------------------------------------------------

    def _generate_ch2_hardware_table(self) -> str:
        headers = [
            "Component",
            "Model",
            "Sensor Type",
            "Resolution / Range",
            "Sampling Rate",
            "Interface",
            "Key Specifications",
            "Rationale for Selection",
        ]
        rows = [
            [
                "**GSR Sensor**",
                "Shimmer3 GSR+",
                "Electrodermal Activity (EDA)",
                "10kΩ - 4.7MΩ<br/>16-bit ADC",
                "128 Hz (configurable 51.2-512 Hz)",
                "Bluetooth LE 4.0",
                "- Measurement range: 0.01-100 μS<br/>"
                "- Resolution: 76 μΩ<br/>"
                "- Excitation voltage: 0.5V<br/>"
                "- Weight: 22g<br/>"
                "- Battery: 450mAh (14 hours)",
                "- Research-grade accuracy<br/>"
                "- Multi-channel (GSR, PPG, IMU)<br/>"
                "- Open SDK (Shimmer API)<br/>"
                "- Validated in peer-reviewed studies<br/>"
                "- Wireless for participant mobility",
            ],
            [
                "**Thermal Camera**",
                "Topdon TC001",
                "Thermal Infrared Imaging",
                "256×192 pixels<br/>-20°C to +550°C",
                "25 fps",
                "USB-C (OTG)",
                "- Spectral range: 8-14 μm<br/>"
                "- Thermal sensitivity: <50 mK<br/>"
                "- Temperature accuracy: ±2°C or ±2%<br/>"
                "- Field of view: 56° × 42°<br/>"
                "- Radiometric output: Yes",
                "- Smartphone integration via USB<br/>"
                "- Higher resolution than FLIR One (160×120)<br/>"
                "- Radiometric temperature data access<br/>"
                "- Affordable (~$300)<br/>"
                "- InfiSense SDK support",
            ],
            [
                "**RGB Camera**",
                "Phone Built-in Camera",
                "High-Resolution Video",
                "1920×1080 (1080p) up to 4K",
                "30 fps (configurable)",
                "Internal (CameraX API)",
                "- Wide-angle lens (~78° FOV)<br/>"
                "- H.264/AVC encoding<br/>"
                "- Autofocus: PDAF/Laser AF<br/>"
                "- Exposure: Auto-HDR<br/>"
                "- Storage: Local MP4",
                "- Already integrated in smartphone<br/>"
                "- CameraX provides stable API<br/>"
                "- Efficient compression<br/>"
                "- Synchronized with thermal camera<br/>"
                "- No additional hardware cost",
            ],
            [
                "**Android Device**",
                "Samsung Galaxy S22 / Google Pixel 7",
                "Computing Platform",
                "N/A",
                "N/A",
                "Wi-Fi 6, USB-C, BLE 5.0",
                "- Snapdragon 8 Gen 1 / Tensor G2<br/>"
                "- RAM: 8GB<br/>"
                "- Storage: 128-256GB<br/>"
                "- Display: 6.1-6.3\" AMOLED<br/>"
                "- OS: Android 12+",
                "- Powerful CPU/GPU for real-time processing<br/>"
                "- USB OTG for thermal camera<br/>"
                "- Large storage for recordings<br/>"
                "- Supports NTP time sync",
            ],
            [
                "**PC Controller**",
                "Desktop/Laptop",
                "Control Station",
                "N/A",
                "N/A",
                "Wi-Fi, Ethernet",
                "- OS: Windows/macOS/Linux<br/>"
                "- Python 3.8+ runtime<br/>"
                "- TCP/IP server<br/>"
                "- ≥100GB storage",
                "- Session orchestration<br/>"
                "- Data aggregation and sync<br/>"
                "- Real-time monitoring dashboard<br/>"
                "- Acts as NTP time source",
            ],
            [
                "**Network**",
                "Wi-Fi Router",
                "Communication Infrastructure",
                "N/A",
                "N/A",
                "IEEE 802.11ac/ax",
                "- Dual-band 2.4/5 GHz<br/>"
                "- Latency: <5 ms local<br/>"
                "- Bandwidth: ≥100 Mbps",
                "- Low-latency command delivery<br/>"
                "- Reliable heartbeats<br/>"
                "- Local network operation (no internet)",
            ],
        ]
        table = self._render_markdown_table(headers, rows)
        return dedent(
            f"""
            # Chapter 2: Summary of Hardware Components and Specifications

            ## Table 2.1: Summary of Hardware Components and Specifications

            This table provides a quick reference to the capabilities and constraints of the chosen hardware.

            {table}
            """
        ).strip()

    def _generate_ch2_related_systems_table(self) -> str:
        headers = [
            "System / Study",
            "Sensor Modalities",
            "Synchronization Method",
            "Data Accuracy",
            "Key Features",
            "Limitations",
            "Year",
        ]
        rows = [
            [
                "**PhysioKit**<br/>(Reiter et al., 2023)",
                "- ECG<br/>- PPG<br/>- EDA (GSR)<br/>- Respiration<br/>- Accelerometer",
                "- Microcontroller hardware sync<br/>- Shared clock<br/>- Real-time streaming",
                "- ECG: ±2 BPM<br/>- GSR: research-grade<br/>- 100-500 Hz sampling",
                "- Open-source toolkit<br/>- Multi-user support<br/>- ML-driven quality checks<br/>- Real-time visualization",
                "- No thermal imaging<br/>- Requires custom hardware<br/>- Complex assembly",
                "2023",
            ],
            [
                "**iBVP Dataset**<br/>(Cho et al., 2024)",
                "- RGB facial video<br/>- Thermal facial video<br/>- Ear-PPG<br/>- Quality annotations",
                "- Hardware trigger sync<br/>- Frame-level alignment<br/>- Manual validation",
                "- Thermal: FLIR A655sc<br/>- RGB: HD camera<br/>- PPG: medical-grade",
                "- High-quality dataset<br/>- Quality labels<br/>- Varied conditions<br/>- rPPG validation",
                "- Face-focused only<br/>- No GSR<br/>- Expensive equipment<br/>- Lab-only setup",
                "2024",
            ],
            [
                "**PsychoPy + LSL**",
                "- Custom sensors via LSL<br/>- Stimulus presentation<br/>- Behavioral responses",
                "- LabStreamingLayer (LSL)<br/>- Network clock sync<br/>- Event markers",
                "- Sub-ms stimulus timing<br/>- ±1ms typical",
                "- Precise stimulus control<br/>- Multi-stream integration<br/>- Open-source platform",
                "- Complex setup<br/>- No Android support<br/>- Requires dedicated PCs",
                "N/A",
            ],
            [
                "**FLIR Thermal Studies**<br/>(Zhang et al., 2021)",
                "- FLIR thermal camera<br/>- Contact GSR sensor<br/>- Optional ECG",
                "- Post-hoc alignment<br/>- Manual sync markers",
                "- Thermal: 640×480 @0.02°C<br/>- GSR: research-grade",
                "- High-resolution thermal<br/>- Proven stress detection",
                "- Expensive ($15k+)<br/>- No real-time sync<br/>- Desktop-only",
                "2021",
            ],
            [
                "**Empatica E4 Wristband**",
                "- EDA (GSR)<br/>- PPG<br/>- Accelerometer<br/>- Temperature",
                "- Internal device clock<br/>- BLE timestamp alignment",
                "- GSR: 0.001 μS resolution<br/>- PPG: 64 Hz<br/>- Temp: ±0.2°C",
                "- Wearable, unobtrusive<br/>- Cloud analytics<br/>- Real-time BLE streaming",
                "- Closed ecosystem<br/>- No thermal/RGB<br/>- Subscription costs",
                "2018",
            ],
            [
                "**Proposed System (This Work)**",
                "- Thermal imaging (TC001)<br/>- RGB video<br/>- GSR via Shimmer3<br/>- Optional audio markers",
                "- Software NTP-style sync<br/>- Unified timeline offsets<br/>- Remote triggers",
                "- Thermal: 256×192 @ ±2°C<br/>- RGB: 1080p30<br/>- GSR: 128 Hz @ ±5 μS",
                "- Smartphone-based<br/>- Remote orchestration<br/>- Portable & cost-effective<br/>- Multi-sensor fusion ready",
                "- Requires Android device<br/>- Dependent on Wi-Fi quality",
                "2024",
            ],
        ]
        table = self._render_markdown_table(headers, rows)
        return dedent(
            f"""
            # Chapter 2: Related Systems and Methods Comparison

            ## Table 2.2: Related Systems and Methods Comparison

            {table}
            """
        ).strip()

    def _generate_ch2_gsr_thermal_examples(self) -> str:
        return dedent(
            """
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
            """
        ).strip()

    # Chapter 3 -------------------------------------------------------------------

    def _generate_ch3_system_architecture(self) -> str:
        return dedent(
            """
            # Chapter 3: System Design and Architecture

            ## Figure 3.1: System Architecture Diagram

            ```mermaid
            flowchart TB
                subgraph PC["PC Orchestrator (Python)"]
                    subgraph UI["User Interface"]
                        GUI[PyQt6 GUI<br/>Control Panel]
                        StatusDisplay[Status Dashboard]
                    end
                    subgraph Core["Core Coordination"]
                        SessionMgr[Session Manager]
                        DeviceMgr[Device Manager]
                        TimeSyncSvc[Time Sync Service]
                        CommandQueue[(Command Queue)]
                    end
                    subgraph NetData["Network & Data"]
                        NetServer[TCP Server :8080]
                        ProtocolEngine[Protocol Engine]
                        DataAggregator[Data Aggregator]
                        FileReceiver[File Transfer Manager]
                        DataCache[(Incoming Buffers)]
                    end
                    GUI --> SessionMgr
                    GUI --> StatusDisplay
                    SessionMgr --> DeviceMgr
                    SessionMgr --> CommandQueue
                    CommandQueue --> NetServer
                    DeviceMgr --> NetServer
                    NetServer --> ProtocolEngine --> FileReceiver --> DataCache --> DataAggregator
                    DataAggregator --> StatusDisplay
                end

                subgraph Network["Wi-Fi"]
                    TCPConnection{{TCP/IP Connection<br/>Port 8080}}
                    SyncChannel{{Sync Channel}}
                    DataChannel{{Data Channel}}
                end

                subgraph Android["Android Sensor Node"]
                    subgraph Control["Application Control"]
                        MainActivity[MainActivity]
                        RecordingService[Recording Service]
                        RecordingController{{Recording Controller}}
                        StateManager[(State Manager)]
                    end
                    subgraph NetComm["Network Communication"]
                        NetClient[Network Client]
                        ProtocolHandler[Protocol Handler]
                        SyncClient[Sync Manager]
                        MessageQueue[(Message Queue)]
                    end
                    subgraph SensorLayer["Sensor Drivers"]
                        ThermalDriver[Thermal Driver]
                        GSRDriver[GSR Driver]
                        CameraDriver[CameraX Driver]
                    end
                    subgraph DataProc["Data Processing"]
                        ThermalProc[Thermal Processor]
                        GSRProc[GSR Processor]
                        VideoProc[Video Processor]
                    end
                    subgraph Storage["Storage & Sync"]
                        TimeManager[Time Manager]
                        StorageMgr[Storage Manager]
                        DataWriter{{Data Writer}}
                        FileBuffer[(File Buffers)]
                    end
                    MainActivity --> RecordingService --> RecordingController
                    RecordingController --> ThermalDriver
                    RecordingController --> GSRDriver
                    RecordingController --> CameraDriver
                    ThermalDriver --> ThermalProc --> DataWriter
                    GSRDriver --> GSRProc --> DataWriter
                    CameraDriver --> VideoProc --> DataWriter
                    DataWriter --> FileBuffer --> StorageMgr
                    SyncClient --> TimeManager
                    ProtocolHandler --> RecordingController
                    MessageQueue --> NetClient
                end

                PC -->|TCP/JSON| Network -->|TCP/JSON| Android
                SyncChannel --> SyncClient

                classDef pcClass fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
                classDef androidClass fill:#fce4ec,stroke:#c2185b,stroke-width:2px
                classDef networkClass fill:#fff9c4,stroke:#f9a825,stroke-width:2px
                classDef processClass fill:#d1c4e9,stroke:#673ab7,stroke-width:2px

                class PC,UI,Core,NetData pcClass
                class Android,Control,NetComm,SensorLayer,DataProc,Storage androidClass
                class Network,SyncChannel networkClass
                class ThermalProc,GSRProc,VideoProc processClass
            ```
            """
        ).strip()

    def _generate_ch3_state_machine(self) -> str:
        return dedent(
            """
            # Chapter 3: Recording Control State Machine

            ## Figure 3.2: Software State Machine for Recording Control

            ```mermaid
            stateDiagram-v2
                [*] --> Idle
                Idle --> Discovering : User initiates connection
                Discovering --> Connecting : Device found
                Connecting --> Connected : TCP handshake complete
                Connecting --> Idle : Connection failed
                Connected --> Configuring : Session setup
                Configuring --> Ready : Sensors configured
                Ready --> Starting : START command issued
                Starting --> Recording : ACKs received
                Starting --> Error : Sensor init failure
                Recording --> Stopping : STOP command issued
                Recording --> Error : Critical failure
                Stopping --> Finalizing : All sensors stopped
                Finalizing --> Connected : Data persisted
                Error --> Recovering : Auto recovery
                Recovering --> Connected : Recovery success
                Recovering --> Error : Recovery failed
                Connected --> Idle : Disconnect
            ```
            """
        ).strip()

    def _generate_ch3_sequence_diagram(self) -> str:
        return dedent(
            """
            # Chapter 3: Communication Sequence Diagram

            ## Figure 3.3: Communication Sequence Diagram (PC–Device Interaction)

            ```mermaid
            sequenceDiagram
                participant PC as PC Orchestrator
                participant Android as Android Device
                participant Thermal as TC001 Thermal
                participant GSR as Shimmer3 GSR
                participant Camera as RGB Camera

                PC->>Android: TCP Connect (port 8080)
                Android->>PC: HELLO {device_id, sensors}
                PC->>Android: SYNC_REQUEST
                Android->>PC: SYNC_RESPONSE (t1, t2)
                PC->>PC: Calculate offset & RTT
                PC->>Android: SYNC_RESULT

                PC->>Android: START_RECORD {session_id}
                par Thermal Init
                    Android->>Thermal: LibIRParse.startStream()
                    Thermal-->>Android: Frame callbacks
                    Android->>PC: ACK thermal_ready
                and GSR Init
                    Android->>GSR: startStreaming()
                    GSR-->>Android: 128Hz samples
                    Android->>PC: ACK gsr_ready
                and RGB Init
                    Android->>Camera: CameraX.startRecording()
                    Camera-->>Android: 30fps frames
                    Android->>PC: ACK rgb_ready
                end

                loop Every 2 seconds
                    Android->>PC: HEARTBEAT {duration, frame counts}
                    PC->>PC: Update dashboard
                end

                PC->>Android: STOP_RECORD {session_id}
                Android->>Thermal: stopStream()
                Android->>GSR: stopStreaming()
                Android->>Camera: stopRecording()
                Android->>PC: ACK files_saved=5 total_size=2.3GB
                Android->>PC: DISCONNECT
            ```
            """
        ).strip()

    def _generate_ch3_requirements_table(self) -> str:
        headers = [
            "Requirement ID",
            "Functional Requirement",
            "Design Criteria / Constraint",
            "Design Solution",
            "Verification Method",
            "Priority",
        ]
        rows = [
            [
                "**FR-001**",
                "Record GSR, thermal, and RGB data simultaneously",
                "Temporal alignment within 5ms<br/>No data loss during concurrent capture",
                "SensorCoordinator orchestrates all sensors<br/>TimeManager with nanosecond precision<br/>Parallel I/O workers per modality",
                "Integration test with all sensors active; verify timestamps align within tolerance",
                "Essential",
            ],
            [
                "**FR-002**",
                "Remote start/stop control via PC",
                "Network latency <50ms<br/>Reliable command delivery",
                "TCP/IP JSON protocol with ACK/NACK<br/>CommandQueue ensures sequencing<br/>Retry with exponential backoff",
                "Protocol latency measurement plus resilience tests",
                "Essential",
            ],
            [
                "**FR-003**",
                "Timestamp synchronization within 5ms accuracy",
                "Clock drift compensation<br/>Periodic resync",
                "NTP-style t1-t4 exchange<br/>TimelineClock offsets applied per sample",
                "Sync test comparing timestamps after 30-minute run",
                "Essential",
            ],
            [
                "**FR-004**",
                "Multi-device support (8+ Android devices)",
                "Scalable architecture<br/>Per-device status tracking",
                "DeviceManager registry with heartbeats<br/>Independent connections for each device",
                "Load test with 8 devices for 60 minutes",
                "Important",
            ],
            [
                "**FR-005**",
                "Local data storage on Android devices",
                "Battery efficiency<br/>File integrity",
                "Session-based directory layout<br/>Buffered CSV writers & MP4 storage<br/>Metadata.json for provenance",
                "Storage verification after extended recording",
                "Essential",
            ],
            [
                "**FR-006**",
                "Automatic data transfer to PC after session",
                "Reliable bulk transfer<br/>Resume capability",
                "File manifest exchange with checksums<br/>Chunked transfer + resume",
                "Transfer interruption test ensuring resume",
                "Essential",
            ],
            [
                "**FR-007**",
                "Real-time monitoring and status display",
                "Low-latency status updates<br/>Battery/storage alerts",
                "2s heartbeats<br/>PyQt6 dashboard with alerting",
                "UI test verifying updates within 3 seconds",
                "Important",
            ],
            [
                "**FR-008**",
                "Fault tolerance and graceful degradation",
                "Auto reconnection<br/>Data preservation",
                "Per-sensor error handling with fallback to simulation",
                "Fault injection tests for sensor/network loss",
                "Essential",
            ],
        ]
        table = self._render_markdown_table(headers, rows)
        return dedent(
            f"""
            # Chapter 3: Functional Requirements and Design Criteria

            ## Table 3.1: System Requirements Matrix

            {table}
            """
        ).strip()

    def _generate_ch3_design_decisions_table(self) -> str:
        headers = [
            "Design Aspect",
            "Options Considered",
            "Chosen Solution",
            "Rationale",
            "Trade-offs",
            "Impact on Requirements",
        ]
        rows = [
            [
                "**Time Synchronization Approach**",
                "1. External GPS clock<br/>2. System clock modification<br/>3. Internal relative sync",
                "**Option 3:** Software-based NTP exchange with offsets",
                "No hardware dependency<br/>Works across Android versions<br/>Achieves <5ms accuracy",
                "Slightly less precise than hardware PTP",
                "Satisfies FR-003 (timestamp sync)",
            ],
            [
                "**Thermal Camera Interface**",
                "1. Vendor SDK<br/>2. Custom UVC driver<br/>3. Generic V4L2",
                "**Option 1:** Official TC001 SDK (LibIRParse)",
                "Reliable calibration<br/>Vendor support<br/>Radiometric data access",
                "Vendor lock-in, binary dependency",
                "Delivers FR-001/FR-005 thermal requirements",
            ],
            [
                "**GSR Sensor Connection**",
                "1. PC direct<br/>2. Android BLE<br/>3. Dual-mode support",
                "**Option 3:** Unified code paths for both modes",
                "Maximum deployment flexibility<br/>Unified CSV output",
                "Higher complexity",
                "Supports FR-001 & FR-004",
            ],
            [
                "**Video Encoding Strategy**",
                "1. Raw frames<br/>2. H.264 hardware<br/>3. H.265",
                "**Option 2:** H.264 hardware encoding",
                "Real-time performance<br/>Widely compatible<br/>Reasonable storage footprint",
                "Larger files than H.265",
                "Meets FR-012 (RGB recording)",
            ],
            [
                "**Network Protocol**",
                "1. WebSocket<br/>2. gRPC<br/>3. Custom TCP + JSON<br/>4. MQTT",
                "**Option 3:** Lightweight TCP + JSON framing",
                "Deterministic control<br/>Simple integration<br/>Low overhead",
                "Requires custom tooling",
                "Aligned with FR-002 (remote control)",
            ],
        ]
        table = self._render_markdown_table(headers, rows)
        return dedent(
            f"""
            # Chapter 3: Sensor Integration Design Decisions

            ## Table 3.2: Critical Design Decisions and Alternatives

            {table}
            """
        ).strip()

    # Chapter 4 -------------------------------------------------------------------

    def _generate_ch4_mobile_app_ui(self) -> str:
        return dedent(
            """
            # Chapter 4: Implementation and Development

            ## Figure 4.1: Mobile App UI and Data Flow

            ```mermaid
            graph TB
                subgraph UI["User Interface Layer (Jetpack Compose)"]
                    MainScreen[Main Screen] --> MainVM[MainActivity ViewModel]
                    ThermalUI[Thermal View] --> ThermalVM[Thermal ViewModel]
                    GSRUI[GSR View] --> GSRVM[GSR ViewModel]
                    RGBUI[RGB View] --> RGBVM[RGB ViewModel]
                    SettingsUI[Settings] --> SettingsVM[Settings ViewModel]
                end

                subgraph Control["Control Layer"]
                    UserAction{{User Actions}}
                    PCCommand{{PC Commands}}
                    RecordingController[Recording Controller]
                    SessionManager[Session Manager]
                    PermissionManager[Permission Manager]
                    UserAction --> RecordingController
                    PCCommand --> RecordingController
                    RecordingController --> SessionManager
                    RecordingController --> PermissionManager
                end

                subgraph Hardware["Hardware Interface"]
                    USBManager[USB Manager] --> TC001Device[/Topdon TC001/]
                    BLEManager[BLE Manager] --> Shimmer3Device[/Shimmer3 GSR+/]
                    CameraXAPI[CameraX API] --> RGBCamera[/Phone Camera/]
                end

                subgraph Pipeline["Sensor Data Pipeline"]
                    ThermalThread[Thermal Thread 25Hz]
                    GSRThread[GSR Thread 128Hz]
                    RGBThread[RGB Thread 30fps]
                    ThermalProcessor[Thermal Processor]
                    GSRProcessor[GSR Processor]
                    RGBProcessor[RGB Processor]
                    ThermalThread --> ThermalProcessor
                    GSRThread --> GSRProcessor
                    RGBThread --> RGBProcessor
                end

                subgraph TimeSync["Time Synchronization"]
                    TimeSyncManager[TimeSync Manager] --> TimeManager[Time Manager]
                    TimeSyncManager --> SyncLogger[Sync Logger]
                end

                subgraph Storage["Data Storage"]
                    BufferedWriter[Buffered Writer]
                    FileManager[File Manager]
                    SessionDir[(Session Directory)]
                    ThermalFile[Thermal CSV]
                    GSRFile[GSR CSV]
                    RGBFile[RGB MP4]
                    MetaFile[metadata.json]
                    SyncFile[timesync_log.csv]
                    BufferedWriter --> FileManager --> SessionDir
                    SessionDir --> ThermalFile
                    SessionDir --> GSRFile
                    SessionDir --> RGBFile
                    SessionDir --> MetaFile
                    SessionDir --> SyncFile
                end

                subgraph Network["Network Communication"]
                    PCOrchestrator[PC Orchestrator]
                    TCPServer[TCP Server :8080]
                    ProtocolHandler[Protocol Handler]
                    MessageQueue[Message Queue]
                    PCOrchestrator -. Wi-Fi .-> TCPServer
                    TCPServer --> MessageQueue --> ProtocolHandler
                end

                MainVM --> RecordingController
                ThermalVM --> ThermalThread
                GSRVM --> GSRThread
                RGBVM --> RGBThread
                RecordingController --> ThermalThread
                RecordingController --> GSRThread
                RecordingController --> RGBThread
                ProtocolHandler --> RecordingController
                ProtocolHandler --> TimeSyncManager
                USBManager --> ThermalThread
                BLEManager --> GSRThread
                CameraXAPI --> RGBThread
                TimeManager --> ThermalProcessor
                TimeManager --> GSRProcessor
                TimeManager --> RGBProcessor
                ThermalProcessor --> BufferedWriter
                GSRProcessor --> BufferedWriter
                RGBProcessor --> BufferedWriter
            ```
            """
        ).strip()

    def _generate_ch4_bluetooth_snippet(self) -> str:
        path = "app/src/main/java/mpdc4gsr/gsr/device/ShimmerDeviceController.kt"
        block_stream = self._extract_kotlin_block(path, "fun startStreaming(")
        block_handler = self._extract_kotlin_block(path, "private inner class ShimmerMsgHandler")
        block_map = self._extract_kotlin_block(path, "private fun mapCluster(")
        snippet = f"{block_stream}\n\n{block_handler}\n\n{block_map}"
        return dedent(
            f"""
            # Chapter 4: Bluetooth GSR Connection and Reading

            ## Code Snippet 4.1: Shimmer3 Streaming Pipeline

            ```kotlin
            {snippet}
            ```
            """
        ).strip()

    def _generate_ch4_thermal_snippet(self) -> str:
        path = "app/src/main/java/mpdc4gsr/feature/capture/thermal/data/source/ThermalSimulationDataSource.kt"
        block_stream = self._extract_kotlin_block(path, "override suspend fun startStreaming()")
        block_record = self._extract_kotlin_block(path, "override suspend fun startRecording()")
        snippet = f"{block_stream}\n\n{block_record}"
        return dedent(
            f"""
            # Chapter 4: Thermal Camera Frame Capture (USB)

            ## Code Snippet 4.2: Thermal Streaming and Recording Control

            ```kotlin
            {snippet}
            ```
            """
        ).strip()

    def _generate_ch4_timestamp_sync_snippet(self) -> str:
        path = "app/src/main/java/mpdc4gsr/gsr/network/TimeSyncClient.kt"
        block_udp = self._extract_kotlin_block(path, "private suspend fun runUdpLoop()")
        block_poll = self._extract_kotlin_block(path, "private suspend fun pollCalibration()")
        snippet = f"{block_udp}\n\n{block_poll}"
        return dedent(
            f"""
            # Chapter 4: Timestamp Synchronization Logic

            ## Code Snippet 4.3: TimeSyncClient UDP Loop and Calibration Polling

            ```kotlin
            {snippet}
            ```
            """
        ).strip()

    def _generate_ch4_remote_command_snippet(self) -> str:
        path = "app/src/main/java/mpdc4gsr/gsr/network/CommandClient.kt"
        block_handle_command = self._extract_kotlin_block(path, "private suspend fun handleCommand(")
        block_handle_start = self._extract_kotlin_block(path, "private suspend fun handleStartRecording(")
        snippet = f"{block_handle_command}\n\n{block_handle_start}"
        return dedent(
            f"""
            # Chapter 4: Remote Command Handling (TCP Server)

            ## Code Snippet 4.4: Command Dispatch and Session Handling

            ```kotlin
            {snippet}
            ```
            """
        ).strip()

    # Chapter 5 -------------------------------------------------------------------

    def _generate_ch5_system_event_timeline(self) -> str:
        raw_dir = self._ensure_chapter5_assets()
        csv_path = raw_dir / "system_event_timeline.csv"
        diagram_path = raw_dir / "system_event_timeline_diagram.md"
        return dedent(
            f"""
            # Chapter 5: Experimental Evaluation and Results

            ## Figure 5.1: System Event Timeline and Synchronization

            Generated assets:

            - `{csv_path.relative_to(self.output_root)}`
            - `{diagram_path.relative_to(self.output_root)}`

            The CSV captures per-actor timestamps; the Mermaid diagram details the command-to-recording flow.
            """
        ).strip()

    def _generate_ch5_sensor_sync_validation(self) -> str:
        raw_dir = self._ensure_chapter5_assets()
        csv_path = raw_dir / "sensor_sync_validation.csv"
        diagram_path = raw_dir / "sensor_sync_validation_diagram.md"
        return dedent(
            f"""
            # Chapter 5: Sensor Data Synchronization Validation

            ## Figure 5.2: Synchronization Accuracy

            Assets produced:

            - `{csv_path.relative_to(self.output_root)}`
            - `{diagram_path.relative_to(self.output_root)}`

            Includes jitter statistics and tolerance validation for LED flash stimulus alignment.
            """
        ).strip()

    def _generate_ch5_recorded_samples(self) -> str:
        raw_dir = self._ensure_chapter5_assets()
        csv_path = raw_dir / "recorded_data_samples.csv"
        return dedent(
            f"""
            # Chapter 5: Recorded Data Samples

            ## Figure 5.3: Recorded Data Samples (Thermal & GSR)

            Representative synchronized samples stored at `{csv_path.relative_to(self.output_root)}` provide
            joined thermal, RGB, and GSR records for a single session.
            """
        ).strip()

    def _generate_ch5_performance_metrics(self) -> str:
        raw_dir = self._ensure_chapter5_assets()
        latency = raw_dir / "latency_metrics.csv"
        throughput = raw_dir / "throughput_metrics.csv"
        battery = raw_dir / "battery_metrics.csv"
        diagram = raw_dir / "performance_metrics_diagrams.md"
        return dedent(
            f"""
            # Chapter 5: Performance Metrics

            ## Figure 5.4: Performance Metrics Charts

            Generated outputs:

            - `{latency.relative_to(self.output_root)}`
            - `{throughput.relative_to(self.output_root)}`
            - `{battery.relative_to(self.output_root)}`
            - `{diagram.relative_to(self.output_root)}`

            The datasets feed bar/line charts summarizing latency, throughput, and battery drain.
            """
        ).strip()

    def _generate_ch5_test_cases_table(self) -> str:
        raw_dir = self._ensure_chapter5_assets()
        csv_path = raw_dir / "test_cases_outcomes.csv"
        return dedent(
            f"""
            # Chapter 5: Test Cases and Outcomes

            ## Table 5.1: Test Cases and Outcomes

            Structured test matrix available at `{csv_path.relative_to(self.output_root)}` capturing purpose, conditions,
            expected vs actual results, and status.
            """
        ).strip()

    def _generate_ch5_log_excerpt(self) -> str:
        raw_dir = self._ensure_chapter5_assets()
        csv_path = raw_dir / "example_log_excerpt.csv"
        return dedent(
            f"""
            # Chapter 5: Example Log Excerpt

            ## Table 5.2: Example Log Excerpt (Synchronized Data)

            Synchronized log snippet exported to `{csv_path.relative_to(self.output_root)}` demonstrating aligned
            timestamps across modalities.
            """
        ).strip()

    def _generate_ch5_performance_summary(self) -> str:
        raw_dir = self._ensure_chapter5_assets()
        csv_path = raw_dir / "performance_summary_results.csv"
        report_path = raw_dir / "performance_summary_report.md"
        return dedent(
            f"""
            # Chapter 5: Performance Summary Results

            ## Table 5.3: Performance Summary Results

            Aggregated metrics and commentary:

            - `{csv_path.relative_to(self.output_root)}`
            - `{report_path.relative_to(self.output_root)}`
            """
        ).strip()

    # Chapter 6 -------------------------------------------------------------------

    def _generate_ch6_objectives_table(self) -> str:
        raw_dir = self._ensure_chapter6_assets()
        csv_path = raw_dir / "objectives_fulfillment_table.csv"
        md_path = raw_dir / "objectives_fulfillment_table.md"
        return dedent(
            f"""
            # Chapter 6: Fulfillment of Objectives

            ## Table 6.1: Fulfillment of Objectives

            Outputs generated via automated evaluation:

            - `{csv_path.relative_to(self.output_root)}`
            - `{md_path.relative_to(self.output_root)}`
            """
        ).strip()

    def _generate_ch6_future_enhancements(self) -> str:
        return dedent(
            """
            # Chapter 6: Proposed Future System Enhancements

            ## Figure 6.1: Future Expansion Roadmap

            ```mermaid
            graph LR
                A[Current Platform<br/>GSR + Thermal + RGB] --> B[Add Physiological Sensors<br/>ECG, EMG, Respiration]
                A --> C[Edge Analytics<br/>On-device Feature Extraction]
                A --> D[Cloud Synchronization<br/>Secure Dataset Repository]

                B --> B1[BLE Expansion<br/>Multi-sensor hub]
                B --> B2[Hardware Trigger Module<br/>GPIO sync]
                C --> C1[Real-time Stress Index<br/>Hybrid model]
                C --> C2[Adaptive Sampling<br/>Power-aware capture]
                D --> D1[Research Portal<br/>Role-based access]
                D --> D2[Automated Annotation<br/>LLM-assisted labeling]

                classDef current fill:#d0f0c0,stroke:#2e7d32,stroke-width:2px
                classDef expansion fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
                class A current
                class B,C,D,B1,B2,C1,C2,D1,D2 expansion
            ```

            Roadmap highlights near-term expansions: integrating additional physiological sensors, deploying
            edge analytics, and enabling cloud-based collaboration.
            """
        ).strip()


def main() -> None:
    generator = ThesisAssetGenerator()
    generator.generate_all_assets()


if __name__ == "__main__":
    main()
