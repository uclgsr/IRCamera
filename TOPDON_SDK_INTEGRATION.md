# Topdon SDK Integration in ThermalFragmentViewModel

## Summary

Successfully integrated the Topdon TC001/TC007 thermal camera SDK into the `ThermalFragmentViewModel` class, replacing simulated implementations with real SDK functionality.

## Changes Made

### File Modified
- `component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/viewmodel/ThermalFragmentViewModel.kt`

### Key Integrations

#### 1. SDK Imports Added
```kotlin
import com.mpdc4gsr.libunified.ir.camera.IRUVCTC
import com.mpdc4gsr.libunified.ir.extension.setMirror
import com.mpdc4gsr.libunified.ir.extension.setAutoShutter
import com.mpdc4gsr.libunified.ir.extension.setPropDdeLevel
import com.mpdc4gsr.libunified.ir.extension.setContrast
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.uvc.UVCCamera
import com.mpdc4gsr.libunified.ir.utils.USBMonitorCallback
```

#### 2. Constructor Parameter
- Added optional `Context` parameter for SDK initialization
- Maintains backward compatibility with default `null` value

#### 3. SDK Instance Fields
```kotlin
private var iruvctc: IRUVCTC? = null
private var syncBitmap: SynchronizedBitmap? = null
private var ircmd: IRCMD? = null
```

#### 4. initializeThermalCamera Method
Replaced simulated initialization with real SDK integration:
- Creates `SynchronizedBitmap` for frame synchronization
- Implements `ConnectCallback` for camera lifecycle events
- Implements `USBMonitorCallback` for USB device management
- Initializes `IRUVCTC` with proper camera dimensions (256x192)
- Configures camera settings using SDK extension methods:
  - Mirror mode: disabled
  - Auto shutter: enabled
  - DDE level: 128
  - Contrast: 128
- Registers USB monitor for device detection

#### 5. capturePhoto Method
- Updated to include SDK status in photo metadata
- Tracks device connection and SDK initialization state

#### 6. disconnectCamera Method
- Added proper SDK cleanup:
  - Unregisters USB monitor
  - Stops camera preview
  - Releases SDK resources

#### 7. onCleared Override
- Added ViewModel lifecycle handling
- Ensures camera disconnection on cleanup

## SDK Components Used

### IRUVCTC
Main thermal camera interface from libunified module that wraps Topdon SDK functionality:
- Camera initialization with USB device
- Frame streaming via SynchronizedBitmap
- Preview control
- Device lifecycle management

### Extension Methods
Custom Kotlin extensions for IRCMD configuration:
- `setMirror(Boolean)` - Mirror/flip image control
- `setAutoShutter(Boolean)` - Auto shutter toggle
- `setPropDdeLevel(Int)` - Digital Detail Enhancement level
- `setContrast(Int)` - Image contrast adjustment

### Data Flow Mode
Uses `CommonParams.DataFlowMode.TEMP_OUTPUT` for temperature data streaming

## Architecture

The integration follows the existing architecture pattern:
- ViewModel depends on libunified module (already configured)
- libunified provides IRUVCTC wrapper for Topdon SDK
- Topdon SDK (com.energy.iruvc) accessed through libunified abstractions
- Maintains separation of concerns and module boundaries

## Testing

- Compilation verified - no errors in ThermalFragmentViewModel
- Pre-existing build errors in IRMainComposeActivity are unrelated to these changes
- Manual testing required with actual Topdon TC001/TC007 hardware

## Usage

### With SDK Integration
```kotlin
val context = requireContext()
val viewModel = ThermalFragmentViewModel(context)
viewModel.initializeThermalCamera(surfaceView)
```

### Backward Compatibility
```kotlin
// Still works for cases without SDK
val viewModel = ThermalFragmentViewModel()
```

## Notes

- SDK initialization requires Android Context
- USB permissions must be granted by user
- Camera hardware must be connected via USB
- Falls back to simulated data if Context not provided
- Proper cleanup handled automatically in ViewModel lifecycle

## References

- Topdon SDK integration: `libunified/src/main/java/com/mpdc4gsr/libunified/ir/`
- Extension methods: `libunified/src/main/java/com/mpdc4gsr/libunified/ir/extension/IRCMDExtensions.kt`
- Ground truth implementation: https://github.com/CoderCaiSL/IRCamera/tree/github-main_ircamera
