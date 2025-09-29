# Network & Device Management - Testing Guide

## Overview

This guide provides comprehensive testing procedures for the newly migrated Network & Device Management Compose
activities to ensure functionality, performance, and user experience quality.

## Pre-Testing Setup

### Build Verification

```bash
# Clean build to ensure no cached issues
./gradlew clean

# Compile debug variant
./gradlew :app:compileDebugKotlin

# Build APK for testing
./gradlew :app:assembleDebug
```

### Device Requirements

- **Android Version**: API 21+ (as per project requirements)
- **Permissions**: Camera, Bluetooth, Location access for full testing
- **Hardware**: Device with Bluetooth capability preferred
- **Network**: WiFi access for device discovery testing

## Testing Scenarios

### 1. DevicePairing Activity Testing

#### 1.1 Navigation Testing

```kotlin
// Test navigation from unified system
NavigationHelper.navigateToDevicePairing(navController)

// Expected: DevicePairingComposeActivity launches successfully
// Fallback: Legacy DevicePairingActivity if Compose fails
```

**Verification Steps:**

1. Launch from navigation system
2. Verify modern Material3 UI loads
3. Check proper toolbar and back navigation
4. Validate fallback mechanism if needed

#### 1.2 Device Discovery Testing

**Test Cases:**

- [ ] **Scan Initiation**: Tap "Scan Devices" button
    - Expected: Button text changes to "Scanning..."
    - Expected: Progress indicator appears
    - Expected: Button becomes disabled during scan

- [ ] **Device Discovery**: Allow scan to complete
    - Expected: Device count updates in real-time
    - Expected: Device list populates with found controllers
    - Expected: Status card shows discovery progress

- [ ] **Empty Results**: Test in environment with no devices
    - Expected: "No controllers found" message displays
    - Expected: Helper text about network requirements shown

#### 1.3 Connection Testing

**Test Cases:**

- [ ] **Device Selection**: Tap on discovered device
    - Expected: Connection attempt initiated
    - Expected: Status changes to "Connecting..."
    - Expected: Connection state updates in real-time

- [ ] **Connection Success**: When device connects successfully
    - Expected: Status card shows "Connected" state
    - Expected: Device card shows "Connected" badge
    - Expected: Disconnect button becomes enabled

- [ ] **Connection Failure**: When connection fails
    - Expected: Error status displayed
    - Expected: Retry options available
    - Expected: Proper error messaging

#### 1.4 UI State Testing

**Verification Points:**

- [ ] Loading states during scanning
- [ ] Connection status color coding
- [ ] Button enablement/disablement
- [ ] Progress indicators
- [ ] Error state displays

### 2. PermissionRequest Activity Testing

#### 2.1 Navigation Testing

```kotlin
// Test navigation launch
NavigationHelper.navigateToPermissionRequest(navController)

// Expected: PermissionRequestComposeActivity launches
// Fallback: Legacy PermissionRequestActivity if needed
```

#### 2.2 Permission Status Testing

**Initial State Testing:**

- [ ] **Permission Overview**: Check status overview card
    - Expected: Shows current permission count (X/4)
    - Expected: Color coding based on permission state
    - Expected: Quick status chips for each permission

- [ ] **Individual Cards**: Verify each permission card
    - [ ] Camera permission card state
    - [ ] Bluetooth permission card state
    - [ ] Location permission card state
    - [ ] Storage permission card state
    - Expected: Proper icons and status text

#### 2.3 Permission Request Testing

**Individual Requests:**

- [ ] **Camera Permission**:
    - Tap Camera card → System permission dialog
    - Grant → Card updates to "Granted" state
    - Deny → Card shows "Request" state

- [ ] **Bluetooth Permission**:
    - Tap Bluetooth card → System dialog
    - Test grant/deny scenarios
    - Verify state updates

- [ ] **Location Permission**:
    - Test location permission flow
    - Verify precise vs approximate options
    - Check state reflection

- [ ] **Storage Permission**:
    - Test storage permission request
    - Verify API level handling (SDK 33+)
    - Check state updates

**Batch Requests:**

- [ ] **Request All**: Tap "Request All" button
    - Expected: Sequential permission dialogs
    - Expected: Progress indication during requests
    - Expected: Final state summary

#### 2.4 Activity Logging Testing

**Log Functionality:**

- [ ] **Auto-scroll**: Verify log auto-scrolls to latest
- [ ] **Timestamp**: Check timestamp format and accuracy
- [ ] **Message Content**: Verify meaningful log messages
- [ ] **Scroll Performance**: Test with many log entries

#### 2.5 Action Button Testing

**Button States:**

- [ ] **Request All**: Proper enablement logic
- [ ] **Test Capabilities**: Functionality verification
- [ ] **Start Recording**: Enablement based on permissions
- [ ] **Loading States**: Proper indication during operations

### 3. Integration Testing

#### 3.1 Navigation Flow Testing

**Full Navigation Chain:**

1. Start from main navigation
2. Navigate to DevicePairing
3. Return to navigation
4. Navigate to PermissionRequest
5. Return to navigation
6. Verify no memory leaks or crashes

#### 3.2 State Persistence Testing

**Configuration Changes:**

- [ ] Rotate device during device scanning
- [ ] Rotate during permission requests
- [ ] Background/foreground app during operations
- [ ] Verify state restoration

#### 3.3 Error Handling Testing

**Error Scenarios:**

- [ ] Network disconnection during device scan
- [ ] Permission denial handling
- [ ] Activity launch failures
- [ ] ViewModel initialization errors

### 4. Performance Testing

#### 4.1 Memory Usage

```bash
# Monitor memory during testing
adb shell dumpsys meminfo [package_name]
```

**Monitor For:**

- [ ] Memory leaks during navigation
- [ ] StateFlow collection cleanup
- [ ] Compose recomposition efficiency

#### 4.2 UI Performance

**Metrics to Track:**

- [ ] Activity launch time
- [ ] UI responsiveness during scanning
- [ ] Log scroll performance
- [ ] Animation smoothness

#### 4.3 State Management Performance

**StateFlow Efficiency:**

- [ ] Unnecessary recompositions
- [ ] State update frequency
- [ ] Event handling performance

## Automated Testing

### Unit Tests

```kotlin
// ViewModel testing examples
@Test
fun `permission request updates state correctly`() {
    // Test permission state management
}

@Test
fun `device discovery handles errors properly`() {
    // Test error handling in scanning
}
```

### UI Tests

```kotlin
// Compose testing examples
@Test
fun `device pairing screen displays correctly`() {
    // Test UI component rendering
}

@Test
fun `permission cards respond to clicks`() {
    // Test interaction handling
}
```

## Bug Report Template

### Issue Description

- **Activity**: DevicePairing / PermissionRequest
- **Action**: What user was doing
- **Expected**: What should happen
- **Actual**: What actually happened
- **Reproducible**: Steps to reproduce

### Environment

- **Device**: Model and OS version
- **App Version**: Build details
- **Network**: WiFi/Mobile/Offline
- **Permissions**: Current permission states

### Logs

```
// Include relevant logs from activity logging or system logs
```

## Success Criteria

### Functional Requirements

- [ ] All navigation routes work correctly
- [ ] Device discovery functions properly
- [ ] Permission requests work on all API levels
- [ ] Fallback mechanisms activate when needed
- [ ] All UI states display correctly

### Performance Requirements

- [ ] Activity launch < 500ms
- [ ] UI remains responsive during operations
- [ ] No memory leaks detected
- [ ] Smooth animations and transitions

### User Experience Requirements

- [ ] Intuitive UI with clear feedback
- [ ] Proper error messaging
- [ ] Consistent Material3 design
- [ ] Accessibility compliance

## Testing Completion Checklist

- [ ] All test scenarios executed
- [ ] Performance metrics within acceptable ranges
- [ ] No critical bugs identified
- [ ] Documentation updated with any findings
- [ ] User acceptance criteria met
- [ ] Ready for production deployment

This comprehensive testing ensures the Network & Device Management Compose migration meets quality standards and
provides excellent user experience.