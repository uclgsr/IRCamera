# Consolidated Libraries Directory

This directory contains support libraries that have been organized together for better project structure and maintainability.

## Current Library Organization

### Support Libraries in this Directory
- **libcom** (16 files) - Communication utilities, dialogs, and helper functions
- **libmatrix** (23 files) - Matrix operations and mathematical computations  
- **libmenu** (16 files) - Menu components and UI widget utilities
- **CommonComponent** (4 files) - Shared component utilities

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
implementation(project(":consolidated_libraries:libcom"))
implementation(project(":consolidated_libraries:libmatrix"))
implementation(project(":consolidated_libraries:libmenu"))
```

## Development Notes

These libraries provide specialized functionality:
- **libcom**: Network communication, file operations, utility dialogs
- **libmatrix**: Mathematical operations for sensor data processing
- **libmenu**: Reusable menu and UI components
- **CommonComponent**: Shared utilities used across multiple components

The libraries maintain their original API interfaces to ensure compatibility with existing components.