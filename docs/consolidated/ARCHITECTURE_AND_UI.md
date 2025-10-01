# Architecture and UI Component Reference

## Overview

This document consolidates architecture diagrams, UI component mappings, layout information, and navigation structure
for the IRCamera application.

## System Architecture

### Hub-and-Spoke Design

The IRCamera platform implements a hub-and-spoke architecture:

- **Hub**: Desktop PC controller coordinates all sensor nodes
- **Spokes**: Android devices act as sensor nodes
- **Communication**: Network-based command and data exchange
- **Synchronization**: NTP-style time synchronization (sub-10ms accuracy)

### Module Structure

#### Core Modules

1. **app/** - Main application module
    - Activities: 210+ implementations
    - Layouts: 221 XML layouts (legacy) + Compose screens
    - Services: Background data collection and coordination

2. **libunified/** - Shared library module
    - BaseComposeActivity - Compose foundation
    - BaseViewModel - StateFlow-based state management
    - Common utilities and extensions

3. **component/thermalunified/** - Thermal camera integration
    - TC001 device communication
    - Image processing and display
    - Temperature data handling

4. **component/user/** - User management
    - Authentication
    - Profile management
    - Session handling

### MVVM Architecture

Modern MVVM implementation using:

- **StateFlow** instead of LiveData for reactive UI
- **Repository pattern** for data management
- **Coroutine-based** error handling
- **Lifecycle-aware** components
- **Type-safe state management** with sealed classes

## Navigation Architecture

### Compose Navigation System

**UnifiedNavigation.kt** provides centralized navigation with:

- **Sealed class routes** for type safety
- **Deep linking support**
- **Back stack management**
- **Navigation graphs** per feature module

### Navigation Flow

```
MainActivity (Launcher)
  -> Sensor Dashboard
  -> Thermal Camera
  -> GSR Monitoring
  -> Settings
  -> Help/About
```

### Key Navigation Components

1. **MainActivity** - Entry point with UnifiedNavHost
2. **UnifiedNavigation** - Route definitions and navigation logic
3. **NavController** - Handles navigation state
4. **Deep Links** - Support for external navigation triggers

## UI Component Structure

### Screen Hierarchy

#### Main Screens

1. **Dashboard**
    - Sensor status overview
    - Quick actions
    - System health indicators

2. **Thermal Camera**
    - Live thermal feed
    - Temperature controls
    - Capture and recording

3. **GSR Monitoring**
    - Real-time GSR data
    - Signal quality indicators
    - Calibration controls

4. **Settings**
    - Device configuration
    - Network settings
    - Data management

#### Supporting Screens

- Device Pairing
- Version Information
- Help and Documentation
- Policy and Terms
- Testing Suite

### Component Categories

#### Data Display Components

- **Sensor Cards** - Real-time data visualization
- **Charts and Graphs** - Historical data display
- **Status Indicators** - Connection and health status
- **Data Tables** - Structured information display

#### Input Components

- **Settings Controls** - Switches, sliders, text fields
- **Device Selection** - Bluetooth device pickers
- **Configuration Forms** - Multi-field data entry
- **Action Buttons** - Primary and secondary actions

#### Navigation Components

- **Top App Bar** - Title and navigation actions
- **Bottom Navigation** - Main section switching
- **Navigation Drawer** - Advanced navigation options
- **Floating Action Buttons** - Primary screen actions

#### Feedback Components

- **Loading States** - Progress indicators
- **Error Messages** - User-friendly error display
- **Success Confirmations** - Action feedback
- **Toast Messages** - Brief notifications

### Layout Patterns

#### Material Design 3

Consistent use of Material Design 3 components:

- Cards for content grouping
- Elevated buttons for primary actions
- Outlined buttons for secondary actions
- Text buttons for tertiary actions
- FABs for primary screen actions

#### Responsive Design

- Adaptive layouts for different screen sizes
- Portrait and landscape orientations
- Tablet-optimized layouts where applicable

## Activity and Fragment Structure

### Compose Activities (Primary)

Current implementation uses Compose for all new screens:

- Material 3 design system
- Declarative UI
- StateFlow-based state management
- Proper lifecycle handling

### Legacy XML Activities (Backed Up)

Traditional activities moved to backup/ directory:

- 74 activities backed up
- 9 fragments archived
- 51 XML layouts preserved
- Available for reference if needed

## Testing Architecture

### Test Activity Organization

Seven comprehensive test activities:

1. BLE Integration Testing
2. GSR Benchmarking
3. RGB Camera Testing
4. Sensor Integration Testing
5. Time Synchronization Testing
6. Thermal Camera Testing
7. Data Collection Testing

### Testing Patterns

- Compose UI testing with test tags
- ViewModel testing with coroutines
- Repository testing with mocks
- Integration testing across components

## Data Flow Architecture

### Sensor Data Pipeline

```
Sensor Device -> BLE/Network -> Android Service -> Repository -> ViewModel -> UI
```

### State Management

```
User Action -> ViewModel -> Repository -> Data Source
                  |
                  v
            StateFlow Update -> UI Recomposition
```

### Error Handling

Comprehensive error handling at each layer:

- Network errors with retry logic
- Device disconnection handling
- Data validation and sanitization
- User-friendly error messages

## Performance Considerations

### Optimization Strategies

1. **Background Processing** - Services for long-running tasks
2. **Efficient Rendering** - Compose recomposition optimization
3. **Data Batching** - Reduce network overhead
4. **Memory Management** - Proper lifecycle handling
5. **Battery Optimization** - Doze mode compatibility

### Monitoring

- Frame rate tracking
- Memory usage monitoring
- Network efficiency metrics
- Battery consumption analysis

## Security Architecture

### Data Protection

- Encrypted local storage
- Secure network communication
- User authentication
- Permission management

### Privacy Considerations

- Minimal data collection
- User consent handling
- Data retention policies
- Export and deletion capabilities

## Deployment Architecture

### Build Variants

- Debug - Development testing
- Release - Production deployment
- Staging - Pre-production validation

### Module Dependencies

```
app
  -> libunified (shared code)
  -> component/thermalunified (thermal camera)
  -> component/user (user management)
  -> BleModule (Bluetooth communication)
```

## Documentation References

For detailed information on specific topics:

- **COMPOSE_MIGRATION.md** - Compose migration details
- **MVVM_MODERNIZATION_GUIDE.md** - ViewModel patterns
- **COMPREHENSIVE_ARCHITECTURE_DIAGRAMS.md** - Detailed system diagrams
- **BACKGROUND_DEVICE_SCANNING.md** - BLE implementation details
- **COMPREHENSIVE_TESTING_GUIDE.md** - Testing methodology

## Maintenance Notes

### Code Organization

- Activities in appropriate module packages
- ViewModels alongside related activities
- Repositories in dedicated package
- UI components in compose/ directory

### Best Practices

- Follow Material Design 3 guidelines
- Use StateFlow for state management
- Implement proper error handling
- Write comprehensive tests
- Document complex logic
- Keep components focused and reusable

### Future Enhancements

- Continue Compose adoption
- Enhance testing coverage
- Optimize performance
- Improve documentation
- Refine user experience

## Conclusion

The IRCamera architecture provides a solid foundation for multi-modal physiological sensing with clear separation of
concerns, modern development practices, and comprehensive testing infrastructure. The modular design enables independent
development and maintenance of different system components.
