#!/usr/bin/env python3
import os
import shutil

def create_fragment_backup_dirs():
    """Create necessary backup directories for fragments"""
    dirs = [
        "backup/fragments/app/ui_components",
        "backup/fragments/component/thermalunified",
        "backup/fragments/component/user"
    ]
    for dir_path in dirs:
        os.makedirs(dir_path, exist_ok=True)
        print(f"Created directory: {dir_path}")

def move_fragments():
    """Move legacy fragments to backup"""
    fragment_mappings = [
        # App fragments
        ("app/src/main/java/mpdc4gsr/ui_components/MainFragment.kt", "backup/fragments/app/ui_components/MainFragment.kt"),
        ("app/src/main/java/mpdc4gsr/ui_components/SensorDashboardFragment.kt", "backup/fragments/app/ui_components/SensorDashboardFragment.kt"),
        
        # Thermal fragments
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/lite/fragment/IRMonitorLiteFragment.kt", "backup/fragments/component/thermalunified/IRMonitorLiteFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/IRMonitorThermalFragment.kt", "backup/fragments/component/thermalunified/IRMonitorThermalFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/ThermalFragment.kt", "backup/fragments/component/thermalunified/ThermalFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/IRCorrectionFragment.kt", "backup/fragments/component/thermalunified/IRCorrectionFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/IRThermalFragment.kt", "backup/fragments/component/thermalunified/IRThermalFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/GalleryFragment.kt", "backup/fragments/component/thermalunified/GalleryFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/IRGalleryTabFragment.kt", "backup/fragments/component/thermalunified/IRGalleryTabFragment.kt"),
    ]
    
    moved_count = 0
    for legacy_path, backup_path in fragment_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total fragments moved: {moved_count}")

def move_additional_activities():
    """Move additional activities that weren't covered in previous migrations"""
    additional_mappings = [
        # Additional activities found to have Compose variants
        ("app/src/main/java/mpdc4gsr/sensors/gsr/SessionExportActivity.kt", "backup/activities/app/sensors/gsr/SessionExportActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRRawImageViewActivity.kt", "backup/activities/app/sensors/gsr/GSRRawImageViewActivity.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/HubSpokeIntegrationActivity.kt", "backup/activities/app/sensors/HubSpokeIntegrationActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/SynchronizationTestActivity.kt", "backup/activities/app/test/SynchronizationTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/TimestampUnificationTestActivity.kt", "backup/activities/app/test/TimestampUnificationTestActivity.kt"),
        ("app/src/main/java/mpdc4gsr/test/TimestampSyncVerificationActivity.kt", "backup/activities/app/test/TimestampSyncVerificationActivity.kt"),
    ]
    
    moved_count = 0
    for legacy_path, backup_path in additional_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total additional activities moved: {moved_count}")

if __name__ == "__main__":
    print("Creating fragment backup directories...")
    create_fragment_backup_dirs()
    print("\nMoving legacy fragments...")
    move_fragments()
    print("\nMoving additional activities...")
    move_additional_activities()
    print("\nFragment and additional activity migration complete!")