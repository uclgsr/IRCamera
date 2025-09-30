# Compose Activity Naming Convention Inconsistencies

## Overview
The codebase has **two different naming conventions** for Compose activities, creating inconsistency and potential confusion:

1. **OLD Pattern**: `*ActivityCompose.kt` (24 files)
2. **NEW/CORRECT Pattern**: `*ComposeActivity.kt` (38 files)

## Identified Duplicates

### Same Package Duplicates (activities/)
These have BOTH naming patterns in the same directory:

1. **BlankDev**
   - OLD: `BlankDevActivityCompose.kt`
   - NEW: `BlankDevComposeActivity.kt` ✓

2. **SimplifiedMain** (Already addressed in consolidation)
   - OLD: `SimplifiedMainActivityCompose.kt` (deprecated)
   - NEW: `SimplifiedMainComposeActivity.kt` ✓

3. **NetworkClientTest** (Already addressed in consolidation)
   - OLD: `NetworkClientTestActivityCompose.kt` (deprecated)
   - NEW: `NetworkClientTestComposeActivity.kt` ✓

### Cross-Package Duplicates
These have the same base name but in different packages with different naming patterns:

4. **DevicePairing**
   - OLD: `activities/DevicePairingActivityCompose.kt`
   - NEW: `network/DevicePairingComposeActivity.kt` ✓

5. **DualModeCamera**
   - OLD: `activities/DualModeCameraActivityCompose.kt`
   - NEW: `camera/integration/DualModeCameraComposeActivity.kt` ✓

6. **SessionManager**
   - OLD: `activities/SessionManagerActivityCompose.kt`
   - NEW: `sensors/gsr/SessionManagerComposeActivity.kt` ✓

7. **MultiModalRecording**
   - OLD: `activities/MultiModalRecordingActivityCompose.kt`
   - NEW: `sensors/gsr/MultiModalRecordingComposeActivity.kt` ✓

## Activities Using OLD Pattern (24 total)

In `app/src/main/java/mpdc4gsr/activities/`:
1. BlankDevActivityCompose.kt (has duplicate)
2. ClauseActivityCompose.kt
3. DevicePairingActivityCompose.kt (duplicate in network/)
4. DeviceTypeActivityCompose.kt
5. DualModeCameraActivityCompose.kt (duplicate in camera/integration/)
6. FaultTolerantRecordingActivityCompose.kt
7. GSRDeviceManagementActivityCompose.kt (deprecated - GSR in sensors/gsr/)
8. GSRGalleryActivityCompose.kt (deprecated - GSR in sensors/gsr/)
9. GSRQuickRecordingActivityCompose.kt (deprecated - GSR in sensors/gsr/)
10. IRGalleryEditActivityCompose.kt
11. MoreHelpActivityCompose.kt
12. MultiModalRecordingActivityCompose.kt (duplicate in sensors/gsr/)
13. NetworkClientTestActivityCompose.kt (deprecated - duplicate exists)
14. NetworkConfigActivityCompose.kt
15. PdfActivityCompose.kt
16. PolicyActivityCompose.kt
17. SensorDashboardTestActivityCompose.kt (duplicate in compose/testing/)
18. SessionManagerActivityCompose.kt (duplicate in sensors/gsr/)
19. ShimmerMvpActivityCompose.kt
20. SimpleNetworkTestActivityCompose.kt (duplicate in compose/testing/)
21. SimplifiedMainActivityCompose.kt (deprecated - duplicate exists)
22. UnifiedSensorActivityCompose.kt
23. VersionActivityCompose.kt
24. WebViewActivityCompose.kt

## Recommended Standard

The **correct naming convention** should be: `*ComposeActivity.kt`

This follows Android conventions where the suffix describes the type:
- `*Activity.kt` - Traditional activity
- `*ComposeActivity.kt` - Compose-based activity
- `*Fragment.kt` - Fragment
- `*ViewModel.kt` - ViewModel

The pattern `*ActivityCompose.kt` is non-standard and should be phased out.

## Consolidation Status

### Already Addressed in Current PR
- ✅ SimplifiedMainActivityCompose → SimplifiedMainComposeActivity
- ✅ NetworkClientTestActivityCompose → NetworkClientTestComposeActivity
- ✅ GSR activities in activities/ → sensors/gsr/ (ComposeActivity pattern)

### Remaining Work
- [ ] BlankDev naming variants
- [ ] Cross-package duplicates (DevicePairing, DualModeCamera, SessionManager, MultiModalRecording)
- [ ] Rename remaining 20 *ActivityCompose files to *ComposeActivity pattern
- [ ] Update all references to use consistent naming
- [ ] Update AndroidManifest.xml entries

## Impact

**Before**: 24 files with old pattern, 38 with new pattern, 7 clear duplicates
**After full consolidation**: All files would use `*ComposeActivity.kt` pattern

This would provide:
- Consistent naming across the codebase
- Clearer Android conventions
- Reduced confusion about which file to use
- Easier maintenance and navigation
