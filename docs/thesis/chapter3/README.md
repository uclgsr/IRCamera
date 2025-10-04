# Chapter 3: System Design and Architecture

This directory contains figures, tables, and diagrams for Chapter 3 of the thesis, focusing on System Design and
Architecture.

## Contents

### Figures

1. **[system-architecture.md](./system-architecture.md)** - Figure 3.1: System Architecture Diagram
    - Detailed block diagram showing PC orchestrator, Android sensor nodes, and hardware components
    - Illustrates communication links (TCP/IP, Bluetooth LE, USB/OTG)
    - Depicts data flow from sensors through processing to storage
    - Shows modular architecture with clear component boundaries

2. **[state-machine.md](./state-machine.md)** - Figure 3.2: Software State Machine
    - State diagram for Android recording application lifecycle
    - States: Disconnected, Connecting, Idle, Initializing, Ready, Recording, Syncing, Stopping, Finalizing,
      Transferring, Error, Recovering
    - Transition conditions and event triggers
    - Detailed state descriptions with entry/exit actions
    - Fault handling and error recovery strategies

3. **[communication-sequence.md](./communication-sequence.md)** - Figure 3.3: Communication Sequence Diagram
    - Step-by-step PC-Android interaction protocol
    - 10 phases: Connection, Time Sync, Capabilities, Session Init, Recording Start, Active Recording, Mid-Session Sync,
      Termination, File Transfer, Cleanup
    - Message specifications with JSON examples
    - Timing characteristics and latency requirements
    - Error handling scenarios

### Tables

4. **[requirements-design-table.md](./requirements-design-table.md)** - Table 3.1: Functional Requirements and Design
   Criteria
    - 15 functional requirements (FR-001 to FR-015)
    - Design constraints matrix (hardware, network, storage, performance, security, usability, reliability,
      maintainability, portability)
    - Design decisions rationale for key choices
    - Verification methods for each requirement
    - Priority levels (Essential, Important)

5. **[design-decisions-table.md](./design-decisions-table.md)** - Table 3.2: Sensor Integration Design Decisions
    - 20 critical design aspects with alternatives and rationale
    - Time synchronization, thermal camera interface, GSR connection, video encoding, network protocol, data storage, UI
      framework, etc.
    - Architectural patterns applied (Repository, Observer, Command, Singleton, Factory, State Machine, Facade, Adapter)
    - Technology stack justification
    - Design principles and future extensibility

## Mermaid Diagram Support

All diagrams in this chapter are created using **Mermaid** with advanced features:

**Enhanced Features Used:**

- **Flowchart**: Decision nodes, buffers, queues, icons (🖥️ 📱 🌡️ ⚡ 📹)
- **State Diagram**: Composite states, parallel states (fork/join), nested substates
- **Sequence Diagram**: autonumber, alt/opt/critical/par blocks, rect colored sections, activation boxes

**Rendering Support:**

- GitHub (native support with all advanced features)
- VS Code (with Mermaid extension v1.16+)
- Obsidian
- GitLab
- Mermaid Live Editor (https://mermaid.live/)
- Many other markdown viewers

### Rendering Diagrams

To render these diagrams:

1. **GitHub**: View files directly on GitHub - diagrams render automatically
2. **VS Code**: Install "Markdown Preview Mermaid Support" extension
3. **Command Line**: Use `mmdc` (mermaid-cli) to generate images:
   ```bash
   npm install -g @mermaid-js/mermaid-cli
   mmdc -i system-architecture.md -o system-architecture.png
   ```
4. **Online**: Copy mermaid code to https://mermaid.live/ for interactive editing

## Usage in Thesis

These diagrams and tables are designed to be included in the thesis LaTeX document. To use them:

### For LaTeX Integration

1. **Generate Images from Mermaid**:
   ```bash
   cd docs/chapter3
   mmdc -i system-architecture.md -o ../../latex/figures/fig_3_1_system_architecture.png -w 1200
   mmdc -i state-machine.md -o ../../latex/figures/fig_3_2_state_machine.png -w 1200
   mmdc -i communication-sequence.md -o ../../latex/figures/fig_3_3_communication_sequence.png -w 1200
   ```

2. **Include in LaTeX**:
   ```latex
   \begin{figure}[htbp]
       \centering
       \includegraphics[width=\textwidth]{figures/fig_3_1_system_architecture.png}
       \caption{System Architecture - Multi-Modal Physiological Monitoring Platform}
       \label{fig:system_architecture}
   \end{figure}
   ```

3. **Convert Tables to LaTeX**:
    - Use pandoc or manually format markdown tables as LaTeX tabular environments
    - Or reference the markdown files directly if using markdown-enabled LaTeX build

### For HTML/Web Thesis

If using HTML-based thesis:

- Include markdown files directly
- Mermaid.js will render diagrams client-side
- Style with CSS for consistency

## Document Structure

Each document follows this structure:

1. **Title and Figure/Table Number**: Clear identification
2. **Description**: Purpose and content overview
3. **Diagram/Table**: The actual content (Mermaid or Markdown table)
4. **Supporting Text**: Detailed explanations, specifications, notes

## Design Philosophy

These documents were created following these principles:

- **Clarity**: Diagrams are detailed but not cluttered
- **Completeness**: All major components and interactions are shown
- **Consistency**: Naming conventions and visual style are uniform
- **Traceability**: Requirements mapped to design decisions
- **Reproducibility**: Sufficient detail for implementation

## Key Architectural Insights

From these diagrams and tables, the thesis reader will understand:

1. **System Architecture**: Hub-and-spoke model with PC orchestrator and distributed Android sensor nodes
2. **Communication Protocol**: Custom TCP/JSON protocol with time synchronization, commands, and file transfer
3. **State Management**: Robust state machine with error recovery and fault tolerance
4. **Design Rationale**: Every major decision is justified with alternatives considered
5. **Requirements Traceability**: Clear mapping from requirements to design solutions

## Validation

These diagrams and tables have been validated against:

- Actual implementation in the repository (app/, pc-controller/, component/)
- Existing thesis content (docs/latex/3.tex, 4.tex)
- Related diagrams (docs/thesis-diagrams/, docs/chapter4/)
- Protocol documentation (pc-controller/docs/)

## Future Updates

When updating these documents:

1. Maintain consistency with actual implementation
2. Update version dates in commit messages
3. Regenerate LaTeX images if diagrams change
4. Update related documentation in other chapters
5. Cross-reference with code documentation

## Related Documentation

- **Chapter 4 Diagrams**: [docs/thesis-diagrams/](../thesis-diagrams/) - Implementation details
- **LaTeX Thesis**: [docs/latex/](../latex/) - Full thesis content
- **Protocol Documentation**: [pc-controller/docs/](../../pc-controller/docs/) - Network protocol specs
- **Code Documentation**: In-code comments and docstrings

## Contact

For questions about these diagrams or design decisions, refer to:

- Code comments in relevant modules
- Git commit history for rationale
- Protocol verification reports in pc-controller/docs/

---

**Document Creation Date**: 2024
**Mermaid Version**: Compatible with Mermaid 10+
**Status**: Complete - Ready for Thesis Integration
