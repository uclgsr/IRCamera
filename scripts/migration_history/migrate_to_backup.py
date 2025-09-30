#!/usr/bin/env python3
import os
import shutil

def create_backup_dirs():
    """Create necessary backup directories"""
    dirs = [
        "backup/activities/app/sensors/gsr",
        "backup/activities/app/camera/integration", 
        "backup/activities/app/permissions",
        "backup/activities/app/activities",
        "backup/activities/app/test",
        "backup/activities/app/network",
        "backup/activities/component/thermalunified", 
        "backup/activities/component/user"
    ]
    for dir_path in dirs:
        os.makedirs(dir_path, exist_ok=True)
        print(f"Created directory: {dir_path}")

def move_app_activities():
    """Move app module legacy activities to backup"""
    app_mappings = [
        ("app/src/main/java/mpdc4gsr/network/DevicePairingActivity.kt", "backup/activities/app/network/DevicePairingActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRVideoPlayerActivity.kt", "backup/activities/app/sensors/gsr/GSRVideoPlayerActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRPlotActivity.kt", "backup/activities/app/sensors/gsr/GSRPlotActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/SessionDetailActivity.kt", "backup/activities/app/sensors/gsr/SessionDetailActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRDataViewActivity.kt", "backup/activities/app/sensors/gsr/GSRDataViewActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/SessionManagerActivity.kt", "backup/activities/app/sensors/gsr/SessionManagerActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/MultiModalRecordingActivity.kt", "backup/activities/app/sensors/gsr/MultiModalRecordingActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRSettingsActivity.kt", "backup/activities/app/sensors/gsr/GSRSettingsActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/ShimmerConfigActivity.kt", "backup/activities/app/sensors/gsr/ShimmerConfigActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/ResearchTemplateActivity.kt", "backup/activities/app/sensors/gsr/ResearchTemplateActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRDeviceManagementActivity.kt", "backup/activities/app/sensors/gsr/GSRDeviceManagementActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRQuickRecordingActivity.kt", "backup/activities/app/sensors/gsr/GSRQuickRecordingActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRGalleryActivity.kt", "backup/activities/app/sensors/gsr/GSRGalleryActivity.kt"),
        ("app/src/main/java/mpdc4gsr/camera/integration/DualModeCameraActivity.kt", "backup/activities/app/camera/integration/DualModeCameraActivity.kt"),
        ("app/src/main/java/mpdc4gsr/permissions/PermissionRequestActivity.kt", "backup/activities/app/permissions/PermissionRequestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/activities/PolicyActivity.kt", "backup/activities/app/activities/PolicyActivity.kt"),
        ("app/src/main/java/mpdc4gsr/activities/FaultTolerantRecordingActivity.kt", "backup/activities/app/activities/FaultTolerantRecordingActivity.kt"),
        ("app/src/main/java/mpdc4gsr/activities/MainActivity.kt", "backup/activities/app/activities/MainActivity.kt"),
        ("app/src/main/java/mpdc4gsr/activities/VersionActivity.kt", "backup/activities/app/activities/VersionActivity.kt"),
        ("app/src/main/java/mpdc4gsr/activities/WebViewActivity.kt", "backup/activities/app/activities/WebViewActivity.kt"),
    ]
    
    # Add test activities
    test_mappings = [
        ("app/src/main/java/mpdc4gsr/test/CrossModalSyncTestActivity.kt", "backup/activities/app/test/CrossModalSyncTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/RawCaptureTestActivity.kt", "backup/activities/app/test/RawCaptureTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/SessionLifecycleTestActivity.kt", "backup/activities/app/test/SessionLifecycleTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/BLEIntegrationTestActivity.kt", "backup/activities/app/test/BLEIntegrationTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/TimeSynchronizationTestActivity.kt", "backup/activities/app/test/TimeSynchronizationTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/CompleteSessionTrialActivity.kt", "backup/activities/app/test/CompleteSessionTrialActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/ParallelRecordingTestActivity.kt", "backup/activities/app/test/ParallelRecordingTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/GSRReconnectionTestActivity.kt", "backup/activities/app/test/GSRReconnectionTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/GSRDataIntegrityTestActivity.kt", "backup/activities/app/test/GSRDataIntegrityTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/GSRBenchTestActivity.kt", "backup/activities/app/test/GSRBenchTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/RgbCameraTestActivity.kt", "backup/activities/app/test/RgbCameraTestActivity.kt"),
    ]
    
    app_mappings.extend(test_mappings)
    
    moved_count = 0
    for legacy_path, backup_path in app_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total app activities moved: {moved_count}")

if __name__ == "__main__":
    print("Creating backup directories...")
    create_backup_dirs()
    print("\nMoving app module legacy activities...")
    move_app_activities()
    print("\nApp module migration complete!")