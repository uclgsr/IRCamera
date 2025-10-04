# Thesis Diagrams and Documentation

This directory contains comprehensive diagrams, tables, and code snippets for the thesis documenting the multi-sensor recording system implementation.

## Files Overview

### Chapter 4: Implementation and Development

- **[chapter4-implementation.md](./chapter4-implementation.md)** - Complete implementation documentation including:
  - Figure 4.1: Mobile App UI and Data Flow (Mermaid diagram)
  - Code Snippet 4.2: Bluetooth GSR Connection and Reading
  - Code Snippet 4.3: Thermal Camera Frame Capture (USB)
  - Code Snippet 4.4: Timestamp Synchronization Logic
  - Code Snippet 4.5: Remote Command Handling (TCP Server)

### System Architecture

- **[android-architecture-diagram.md](./android-architecture-diagram.md)** - Figure 4.8: Android application internal architecture
- **[session-sequence-diagram.md](./session-sequence-diagram.md)** - Figure 4.5: Protocol sequence and session control
- **[enhanced-data-flow.md](./enhanced-data-flow.md)** - Data flow architecture diagrams
- **[state-machine-diagram.md](./state-machine-diagram.md)** - System state transitions

### Configuration and Performance

- **[system-configuration-tables.md](./system-configuration-tables.md)** - System configuration specifications
- **[performance-test-tables.md](./performance-test-tables.md)** - Performance benchmarks and test results
- **[time-sync-timeline.md](./time-sync-timeline.md)** - Time synchronization sequence diagrams

## Viewing Mermaid Diagrams

All diagrams use Mermaid syntax for version control and easy editing. To view the diagrams:

### Option 1: GitHub (Recommended)
GitHub natively renders Mermaid diagrams in Markdown files. Simply view the files on GitHub.

### Option 2: VS Code
Install the "Markdown Preview Mermaid Support" extension to view diagrams in VS Code preview.

### Option 3: Online Viewer
Copy the Mermaid code blocks and paste into the [Mermaid Live Editor](https://mermaid.live/).

## Documentation Guidelines

When adding or modifying documentation:

1. **Use Mermaid for diagrams** - Prioritize Mermaid syntax for maintainability
2. **ASCII-safe characters only** - No emojis or special Unicode characters
3. **Code accuracy** - Verify code snippets match actual implementation files
4. **Consistent structure** - Follow existing section organization patterns
5. **Add file path comments** - Include source file paths in code snippet comments

## Code Snippet Sources

All code snippets are extracted from actual implementation files:

- **GSR/Bluetooth**: `app/src/main/java/mpdc4gsr/core/data/ShimmerDeviceManager.kt`
- **GSR Recording**: `app/src/main/java/mpdc4gsr/feature/gsr/data/GSRSensorRecorder.kt`
- **Thermal Camera**: `app/src/main/java/mpdc4gsr/feature/thermal/ui/ThermalCameraRecorder.kt`
- **Thermal USB**: `libunified/src/main/java/com/mpdc4gsr/libunified/ir/camera/IRUVCTC.java`
- **Time Sync**: `app/src/main/java/mpdc4gsr/core/data/TimeSyncManager.kt`
- **Network Server**: `app/src/main/java/mpdc4gsr/feature/network/data/CommandServer.kt`
- **Protocol Handler**: `app/src/main/java/mpdc4gsr/feature/network/data/ProtocolHandler.kt`

## Thesis Chapter Organization

```
Chapter 4: Implementation and Development
├── 4.1 System Architecture Overview (android-architecture-diagram.md)
├── 4.2 Sensor Integration
│   ├── 4.2.1 GSR Sensor Implementation (chapter4-implementation.md - Snippet 4.2)
│   └── 4.2.2 Thermal Camera Implementation (chapter4-implementation.md - Snippet 4.3)
├── 4.3 Time Synchronization (chapter4-implementation.md - Snippet 4.4)
├── 4.4 Network Protocol (session-sequence-diagram.md + chapter4-implementation.md - Snippet 4.5)
├── 4.5 Data Flow Architecture (chapter4-implementation.md - Figure 4.1)
└── 4.6 System Configuration (system-configuration-tables.md)
```

## Maintenance

This documentation is maintained alongside the source code. When making significant implementation changes:

1. Update relevant code snippets in documentation files
2. Regenerate diagrams if architecture changes
3. Update version numbers and timestamps
4. Verify all cross-references remain valid

## Contact

For questions about the documentation structure or content, refer to the main project README or contact the maintainers.
