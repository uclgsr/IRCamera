# Architecture Diagram Summary

This document provides a quick reference to the comprehensive architecture diagrams created for the IRCamera Multi-Modal Thermal Sensing Platform.

## Quick Access

### 📊 Main Documentation
- **[COMPREHENSIVE_ARCHITECTURE_DIAGRAMS.md](COMPREHENSIVE_ARCHITECTURE_DIAGRAMS.md)** - Complete technical documentation with all Mermaid diagrams
- **[architecture-interactive-viewer.html](architecture-interactive-viewer.html)** - Interactive web-based diagram browser

### 🏗️ Architecture Coverage

The comprehensive diagrams cover **9 major architectural aspects**:

#### 1. System Overview
- Complete system with all layers (Physical, Android, PC Hub, Build & CI/CD)
- 17 total modules mapped with precise relationships
- Hardware to software integration flow

#### 2. Hub-and-Spoke Architecture  
- Distributed communication between PC Controller Hub and multiple Android sensor nodes
- mDNS discovery, TCP/JSON communication, and device management
- Real-time data streaming and coordination

#### 3. Android Module Architecture
- Complete Android module dependencies (17 modules)
- Feature components: thermal-ir, thermal, thermal-lite, gsr-recording, user
- Core libraries: libapp, libir, libui, libcom, libmatrix, libmenu
- External modules: BleModule, RangeSeekBar

#### 4. PC Controller Hub Architecture
- 83% complete framework with MVP implementations
- Core services: Device Manager, Session Manager, Data Aggregator, Network Hub
- Data processing pipeline: ingestion, synchronization, storage
- PyQt6 GUI with monitoring and visualization

#### 5. Feature-Specific Diagrams
- **Thermal Processing**: Topdon TC001 integration, libir engine, thermal components
- **GSR & BLE Integration**: Shimmer3 integration, BleModule, gsr-recording component

#### 6. Data Flow Architecture
- Multi-modal data synchronization (thermal 30 FPS, GSR 51.2 Hz)
- Real-time streaming with quality monitoring
- HDF5 export with metadata preservation

#### 7. Build System Architecture
- Gradle multi-module build system (15 build files)
- Version catalog management
- Dependency relationships and build tools integration

#### 8. Integration Architecture
- Complete development to production flow
- Hardware integration, network infrastructure, testing, CI/CD
- Quality assurance and deployment processes

## Technical Specifications

### Diagram Types
- **System Architecture**: High-level overviews showing complete system
- **Component Diagrams**: Detailed internal structure of major components  
- **Sequence Diagrams**: Data flow and communication protocols
- **Dependency Graphs**: Module relationships and build dependencies

### Tools & Standards
- **Mermaid.js**: All diagrams use standard Mermaid syntax
- **Interactive Features**: Zoom, pan, section navigation
- **Responsive Design**: Mobile-friendly viewing
- **Export Ready**: Diagrams can be exported as SVG/PNG

### Coverage Statistics
- **Android Modules**: 10 modules with complete dependency mapping
- **PC Controller**: 15+ components with service relationships  
- **Build Files**: 15 Gradle files with dependency relationships
- **Hardware Integration**: 4 hardware components mapped to software layers

## Usage Guide

### For Developers
1. **Understanding Architecture**: Start with System Overview diagram
2. **Module Development**: Reference specific Android/PC Controller diagrams  
3. **Integration Work**: Use Data Flow and Integration diagrams
4. **Build Issues**: Check Build System Architecture

### For Documentation  
1. **Reference in PRs**: Link to specific diagram sections
2. **Architecture Discussions**: Use Interactive Viewer for presentations
3. **New Feature Planning**: Reference existing patterns in diagrams
4. **Onboarding**: Direct new developers to comprehensive diagrams

### For Maintenance
1. **Dependency Updates**: Check Build System diagrams before changes
2. **Refactoring**: Understand current relationships via Component diagrams
3. **Performance Analysis**: Use Data Flow diagrams to identify bottlenecks
4. **Integration Testing**: Reference Integration Architecture for test planning

## Maintenance Notes

### Keeping Diagrams Current
- Update diagrams when major architectural changes occur
- Verify diagram accuracy during code reviews
- Test Interactive Viewer after Mermaid.js updates
- Validate all links and references periodically

### Future Enhancements
- Add more detailed sequence diagrams for complex flows
- Create deployment-specific architecture diagrams
- Add performance and scalability architecture views
- Integrate with automated architecture validation tools

---

**Created**: December 2024  
**Covers**: IRCamera repository comprehensive architecture  
**Format**: Mermaid.js diagrams with interactive HTML viewer  
**Status**: Complete coverage of all major architectural aspects