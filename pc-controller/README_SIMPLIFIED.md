# IRCamera PC Controller - Simplified Structure

## Overview
This directory contains the simplified MVP (Minimum Viable Product) implementation of the PC Controller Hub, reduced from ~2000+ lines across multiple files to ~250 lines in a single focused implementation.

## Quick Start

### Basic MVP Server
```bash
python3 mvp_simple.py
```

### Full GUI Application (if dependencies available)
```bash
python3 run_mvp_app.py
```

## Files

### Simplified Structure (MVP)
- **`mvp_simple.py`** - Single-file MVP implementation (~250 lines)
- **`config_mvp.yaml`** - Simple configuration
- **`requirements_mvp.txt`** - Minimal dependencies

### Enhanced Implementation (Working)
- **`run_mvp_app.py`** - Full GUI application
- **`demo_mvp_components.py`** - Component demonstration
- **`test_mvp_simple.py`** - Basic functionality tests

### Legacy Structure (To be Removed)
- **`src/`** - Over-engineered implementation (~50 files, 2000+ lines)
- **`native_backend/`** - Unnecessary C++ complexity
- **Complex security, visualization, and networking components**

## Simplification Benefits
- **Reduced complexity**: 2000+ lines -> 250 lines
- **Eliminated dependencies**: No loguru, cryptography, asyncio complexity
- **Easier maintenance**: Single file vs distributed architecture
- **Faster development**: Clear, focused functionality
- **Better testing**: Simple to understand and modify

## Features Preserved
- Device registration and management
- Session management
- TCP communication with Android devices
- JSON protocol handling
- Basic logging and status monitoring

## Architecture
The simplified version implements the same Hub-and-Spoke pattern but focuses on core functionality:

```
PC Controller (Hub)
+-- Device Registry - Track connected Android devices
+-- Session Manager - Handle recording sessions
+-- TCP Server - Communicate with devices
+-- Simple Logging - Basic status and debugging
```

## Migration Strategy
1. Test MVP implementation with existing Android devices
2. Gradually remove over-engineered components from src/
3. Keep working GUI components that add value
4. Maintain compatibility with Android protocol