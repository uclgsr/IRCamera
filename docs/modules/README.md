# IRCamera Module Documentation Index

## Overview

This directory contains comprehensive documentation for all components and modules of the IRCamera thermal imaging platform. Each document provides detailed technical specifications, API references, and implementation guidance.

## Feature Components

### Core Thermal Processing
- **[Thermal-IR Module](THERMAL_IR_MODULE.md)** - Primary thermal imaging component with real-time processing
- **[Thermal-Lite Module](THERMAL_LITE_MODULE.md)** - Lightweight thermal processing for lower-end devices *(Coming Soon)*
- **[Pseudo Module](PSEUDO_MODULE.md)** - Advanced pseudo-color mapping and visualization *(Coming Soon)*

### Data Collection & Analysis
- **[GSR Recording Module](GSR_RECORDING_MODULE.md)** - Shimmer3 GSR sensor integration and physiological analysis
- **[House Module](HOUSE_MODULE.md)** - Building thermal analysis and energy auditing *(Coming Soon)*
- **[Edit3D Module](EDIT3D_MODULE.md)** - 3D thermal reconstruction and editing *(Coming Soon)*
- **[Transfer Module](TRANSFER_MODULE.md)** - Data management and synchronization *(Coming Soon)*

### User Interface & Controls
- **[User Module](USER_MODULE.md)** - User management and settings *(Coming Soon)*
- **[CommonComponent Module](COMMONCOMPONENT_MODULE.md)** - Shared UI components *(Coming Soon)*

## Core Libraries

### Processing Libraries
- **[LibIR Library](LIBIR_LIBRARY.md)** - Core thermal image processing algorithms and analysis
- **[LibCom Library](LIBCOM_LIBRARY.md)** - Communication protocols and networking *(Coming Soon)*
- **[LibMatrix Library](LIBMATRIX_LIBRARY.md)** - Matrix operations for image processing *(Coming Soon)*

### UI & Integration Libraries
- **[LibUI Library](LIBUI_LIBRARY.md)** - User interface components and styling *(Coming Soon)*
- **[LibHIK Library](LIBHIK_LIBRARY.md)** - HIKVision camera integration *(Coming Soon)*
- **[LibApp Library](LIBAPP_LIBRARY.md)** - Application framework and utilities *(Coming Soon)*
- **[LibMenu Library](LIBMENU_LIBRARY.md)** - Application menu system *(Coming Soon)*

## Platform Components

### Controller Systems
- **[PC Controller](PC_CONTROLLER.md)** - Python-based central hub for advanced processing
- **[Android Application](ANDROID_APPLICATION.md)** - Mobile sensor node implementation *(Coming Soon)*

### Hardware Integration
- **[BLE Module](BLE_MODULE.md)** - Bluetooth Low Energy integration *(Coming Soon)*
- **[Range Seek Bar](RANGE_SEEK_BAR.md)** - Custom UI control component *(Coming Soon)*

## Documentation Structure

Each module document follows a consistent structure:

1. **Overview** - Component purpose and functionality
2. **Architecture** - Internal structure and dependencies  
3. **Key Components** - Major classes and interfaces
4. **API Reference** - Detailed method documentation
5. **Data Structures** - Key data types and models
6. **Configuration** - Setup and configuration options
7. **Performance** - Performance characteristics and optimization
8. **Integration Examples** - Code examples and usage patterns
9. **Testing** - Unit and integration test examples
10. **Troubleshooting** - Common issues and solutions
11. **Dependencies** - Required libraries and dependencies
12. **Future Enhancements** - Planned features and improvements

## Cross-References

### Related Documentation
- **[Technical Specifications](../TECHNICAL_SPECIFICATIONS.md)** - Complete platform specifications
- **[Advanced API Documentation](../ADVANCED_API_DOCUMENTATION.md)** - Comprehensive API examples
- **[Architecture Guide](../ARCHITECTURE.md)** - System-wide architecture documentation
- **[Developer Guide](../DEVELOPER_GUIDE.md)** - Development setup and procedures

### Quick Navigation

#### By Functionality
- **Thermal Processing**: [Thermal-IR](THERMAL_IR_MODULE.md), [LibIR](LIBIR_LIBRARY.md), [Pseudo](PSEUDO_MODULE.md)
- **Data Collection**: [GSR Recording](GSR_RECORDING_MODULE.md), [Transfer](TRANSFER_MODULE.md)
- **Analysis & Visualization**: [House](HOUSE_MODULE.md), [Edit3D](EDIT3D_MODULE.md)
- **Communication**: [LibCom](LIBCOM_LIBRARY.md), [BLE Module](BLE_MODULE.md)
- **User Interface**: [LibUI](LIBUI_LIBRARY.md), [User](USER_MODULE.md), [CommonComponent](COMMONCOMPONENT_MODULE.md)

#### By Platform
- **Android Components**: [Thermal-IR](THERMAL_IR_MODULE.md), [GSR Recording](GSR_RECORDING_MODULE.md), [Android Application](ANDROID_APPLICATION.md)
- **PC Components**: [PC Controller](PC_CONTROLLER.md), [LibIR](LIBIR_LIBRARY.md)
- **Cross-Platform**: [LibCom](LIBCOM_LIBRARY.md), [Transfer](TRANSFER_MODULE.md)

#### By Development Priority
- **Core Components** (Essential): [Thermal-IR](THERMAL_IR_MODULE.md), [GSR Recording](GSR_RECORDING_MODULE.md), [LibIR](LIBIR_LIBRARY.md), [PC Controller](PC_CONTROLLER.md)
- **Support Components** (Important): [LibCom](LIBCOM_LIBRARY.md), [LibUI](LIBUI_LIBRARY.md), [Transfer](TRANSFER_MODULE.md)
- **Enhancement Components** (Optional): [House](HOUSE_MODULE.md), [Edit3D](EDIT3D_MODULE.md), [Pseudo](PSEUDO_MODULE.md)

---

For questions about specific modules or to report documentation issues, please refer to the main [Contributing Guide](../CONTRIBUTING.md).