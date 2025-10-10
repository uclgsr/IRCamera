# Thesis Diagrams and Documentation

This directory contains comprehensive diagrams, tables, and code snippets for the thesis documenting the multi-sensor
recording system implementation.

## Files Overview

### Chapter 4: Implementation and Development

- **[chapter4-implementation.md](./chapter4-implementation.md)** - Complete implementation documentation including:
    - Figure 4.1: Mobile App UI and Data Flow (Mermaid diagram)
    - Code Snippet 4.2: Bluetooth GSR Connection and Reading
    - Code Snippet 4.3: Thermal Camera Frame Capture (USB)
    - Code Snippet 4.4: Timestamp Synchronization Logic
    - Code Snippet 4.5: Remote Command Handling (TCP Server)

### System Architecture

- **[android-architecture-diagram.md](./android-architecture-diagram.md)** - Figure 4.8: Android application internal
  architecture
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

- **GSR/Bluetooth**: `app/src/main/java/mpdc4gsr/core/hardware/gsr/GsrDeviceManager.kt`
- **GSR Recording**: `app/src/main/java/mpdc4gsr/feature/capture/gsr/data/GSRSensorRecorder.kt`
- **Thermal Camera**: `app/src/main/java/mpdc4gsr/feature/capture/thermal/ui/ThermalCameraRecorder.kt`
- **Thermal USB**: `libunified/src/main/java/com/mpdc4gsr/libunified/ir/camera/IRUVCTC.java`
- **Time Sync**: `app/src/main/java/mpdc4gsr/core/data/TimeSyncManager.kt`
- **Network Server**: `app/src/main/java/mpdc4gsr/feature/connectivity/data/CommandServer.kt`
- **Protocol Handler**: `app/src/main/java/mpdc4gsr/feature/connectivity/data/ProtocolHandler.kt`

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

# Thesis Diagrams and Visual Content

This directory contains Mermaid diagrams, tables, and visual content for the Multi-Sensor Recording System thesis.

## Chapter 1: Introduction

### Figure 1.1: Multi-Sensor System Overview

- **File**: `chapter1-multisensor-overview.md`
- **Type**: Mermaid graph diagram
- **Description**: High-level architecture showing PC controller, Android sensor node with integrated thermal camera,
  GSR sensor, RGB camera, and Wi-Fi communication infrastructure
- **LaTeX Reference**: `\ref{fig:multisensor-overview}` in `docs/latex/1.tex`

### Figure 1.2: Example Use-Case Scenario Timeline

- **File**: `chapter1-usecase-timeline.md`
- **Type**: Mermaid sequence diagram
- **Description**: Typical recording session showing initialization, time synchronization, parallel sensor startup,
  continuous recording, and graceful shutdown
- **LaTeX Reference**: `\ref{fig:usecase-timeline}` in `docs/latex/1.tex`

## Chapter 2: Background and Literature Review

### Table 2.1: Summary of Hardware Components and Specifications

- **File**: `chapter2-hardware-specs-table.md`
- **Type**: Markdown table with detailed specifications
- **Description**: Comprehensive hardware specifications for Shimmer3 GSR+, Topdon TC001, RGB camera, Android device, PC
  controller, and network infrastructure
- **LaTeX Reference**: `\ref{tab:hardware-specs}` in `docs/latex/2.tex`

### Table 2.2: Related Systems and Methods Comparison

- **File**: `chapter2-related-systems-comparison.md`
- **Type**: Markdown comparison table
- **Description**: Comparative analysis of PhysioKit, iBVP Dataset, PsychoPy+LSL, FLIR studies, Empatica E4, smartphone
  rPPG, and this work
- **LaTeX Reference**: `\ref{tab:related-systems}` in `docs/latex/2.tex`

### Figure 2.1: Basic GSR and Thermal Data Examples

- **File**: `chapter2-gsr-thermal-examples.md`
- **Type**: Mermaid charts (xychart, graph, gantt)
- **Description**: Three-part visualization showing GSR signal characteristics, thermal image ROI analysis, and
  synchronized multi-modal timeline
- **LaTeX Reference**: `\ref{fig:gsr-thermal-examples}` in `docs/latex/2.tex`

## Rendering Mermaid Diagrams

### For LaTeX (PDF Output)

The LaTeX files reference these diagrams with placeholder text. To render Mermaid diagrams in LaTeX:

**Option 1: Mermaid CLI (Recommended)**

```bash
# Install mermaid-cli
npm install -g @mermaid-js/mermaid-cli

# Convert Mermaid to PNG/SVG
mmdc -i chapter1-multisensor-overview.md -o chapter1-multisensor-overview.png
mmdc -i chapter1-usecase-timeline.md -o chapter1-usecase-timeline.svg

# Update LaTeX to use generated images
\includegraphics[width=\textwidth]{../thesis-diagrams/chapter1-multisensor-overview.png}
```

**Option 2: Online Rendering**

1. Copy Mermaid code from `.md` files
2. Paste into https://mermaid.live/
3. Export as PNG/SVG
4. Place in `docs/latex/figures/` directory
5. Update LaTeX `\includegraphics` commands

**Option 3: Direct Mermaid in LaTeX**

```latex
% In preamble (requires mermaid.sty and Node.js)
\usepackage{mermaid}

% In document
\begin{mermaid}
    graph TB
    A[Node] --> B[Node]
\end{mermaid}
```

### For Web/Markdown Viewing

GitHub and most Markdown viewers render Mermaid automatically:

- View `.md` files directly on GitHub
- Use VS Code with Mermaid extension
- Use any Markdown viewer with Mermaid support

### For Documentation Sites

Use documentation generators that support Mermaid:

- MkDocs with `mkdocs-mermaid2-plugin`
- Sphinx with `sphinxcontrib-mermaid`
- Docusaurus with built-in Mermaid support

## File Organization

```
docs/thesis-diagrams/
├── README.md (this file)
├── chapter1-multisensor-overview.md       # Fig 1.1
├── chapter1-usecase-timeline.md           # Fig 1.2
├── chapter2-hardware-specs-table.md       # Table 2.1
├── chapter2-related-systems-comparison.md # Table 2.2
├── chapter2-gsr-thermal-examples.md       # Fig 2.1
├── enhanced-data-flow.md                  # Chapter 4 (existing)
├── session-sequence-diagram.md            # Chapter 4 (existing)
├── state-machine-diagram.md               # Chapter 4 (existing)
└── time-sync-timeline.md                  # Chapter 4 (existing)
```

## Integration with LaTeX Thesis

The diagrams are referenced in the LaTeX files:

- `docs/latex/1.tex`: Chapter 1 Introduction
- `docs/latex/2.tex`: Chapter 2 Background and Literature Review
- `docs/latex/appendix_Z.tex`: Consolidated Figures Appendix

Current status:

- ✅ Mermaid source files created
- ✅ LaTeX references added with placeholders
- ✅ Appendix Z updated with detailed descriptions
- ⏳ PNG/SVG rendering (requires mermaid-cli or manual export)
- ⏳ LaTeX figure replacement (requires rendered images)

## Contributing

When adding new diagrams:

1. Create `.md` file with Mermaid code
2. Add descriptive comments and context
3. Reference in appropriate LaTeX chapter file
4. Update `appendix_Z.tex` with detailed description
5. Update this README with file information

## Style Guidelines

### Mermaid Diagrams

- Use descriptive node labels with line breaks (`<br/>`)
- Apply consistent styling with `classDef` declarations
- Include comments for complex flows
- Limit graph complexity (aim for <20 nodes per diagram)

### Tables

- Use Markdown tables for readability
- Include header row with column descriptions
- Provide inline references to sources
- Add summary/key insights section

### Documentation

- Start with brief one-line description
- Explain context and purpose
- Note any assumptions or limitations
- Reference related diagrams/sections

## Quality Checklist

Before committing diagram files:

- [ ] Mermaid syntax validated (test on mermaid.live)
- [ ] ASCII-only characters used (no emojis)
- [ ] Descriptive labels and clear flow
- [ ] Consistent styling applied
- [ ] LaTeX reference added
- [ ] Appendix Z updated
- [ ] README updated

## Known Limitations

1. **LaTeX Rendering**: Mermaid requires Node.js and mermaid-cli for LaTeX compilation. Current placeholders need manual
   image generation.

2. **Complex Diagrams**: Very large diagrams may need to be split or simplified for print quality.

3. **Color Printing**: Diagrams use color for clarity; consider grayscale accessibility for B&W printing.

4. **Font Consistency**: Mermaid default fonts may differ from LaTeX document fonts; consider font matching in final
   rendering.

## License

These diagrams are part of the Multi-Sensor Recording System thesis and follow the same license as the main project.

## Contact

For questions about diagrams or rendering issues, refer to the main project documentation or open an issue in the
repository.








