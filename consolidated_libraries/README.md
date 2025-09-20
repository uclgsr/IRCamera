# Consolidated Libraries Directory

This directory contains support libraries that have been organized together for better project structure and
maintainability.

## Current Library Organization

### Support Libraries in this Directory

- **CommonComponent** (4 files) - Shared component utilities and common functionality

### Planned Libraries (Future Organization)

The following libraries are mentioned in documentation but not yet implemented in this consolidated structure:
- **libcom** - Communication utilities, dialogs, and helper functions (currently distributed)
- **libmatrix** - Matrix operations and mathematical computations (currently distributed) 
- **libmenu** - Menu components and UI widget utilities (currently distributed)

### Core Libraries (in root)

- **libapp/** - Main application framework library
- **libir/** - Infrared processing and thermal camera library
- **libui/** - User interface components library

## Purpose and Organization

This organizational structure:

- **Groups related libraries** for easier maintenance
- **Separates core from support** functionality for cleaner dependencies
- **Maintains clear boundaries** between different functional areas
- **Simplifies navigation** by grouping similar components

## Usage in Build System

Components reference these libraries using project dependencies:

```kotlin
// Currently available
implementation(project(":consolidated_libraries:CommonComponent"))

// Planned for future consolidation
// implementation(project(":consolidated_libraries:libcom"))
// implementation(project(":consolidated_libraries:libmatrix"))
// implementation(project(":consolidated_libraries:libmenu"))
```

## Development Notes

These libraries provide specialized functionality:

- **CommonComponent**: Shared utilities and common components used across multiple modules

### Future Consolidation Plans

The following functionality is currently distributed across modules and planned for future consolidation:

- **Communication utilities**: Network communication, file operations, utility dialogs
- **Mathematical operations**: Matrix operations for sensor data processing  
- **Menu components**: Reusable menu and UI components

The libraries maintain their original API interfaces to ensure compatibility with existing components.