#!/usr/bin/env python3
import os
import shutil

def create_viewmodel_backup_dirs():
    """Create necessary backup directories for ViewModels"""
    dirs = [
        "backup/viewmodels/app/sensors/gsr",
        "backup/viewmodels/app/ui_components",
        "backup/viewmodels/app/viewmodel",
        "backup/viewmodels/component/thermalunified",
        "backup/viewmodels/libunified"
    ]
    for dir_path in dirs:
        os.makedirs(dir_path, exist_ok=True)
        print(f"Created directory: {dir_path}")

def move_viewmodels():
    """Move ViewModels related to backed up activities"""
    viewmodel_mappings = [
        # App GSR ViewModels
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRSettingsViewModel.kt", "backup/viewmodels/app/sensors/gsr/GSRSettingsViewModel.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRDataViewViewModel.kt", "backup/viewmodels/app/sensors/gsr/GSRDataViewViewModel.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/SessionManagerViewModel.kt", "backup/viewmodels/app/sensors/gsr/SessionManagerViewModel.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRPlotViewModel.kt", "backup/viewmodels/app/sensors/gsr/GSRPlotViewModel.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRVideoPlayerViewModel.kt", "backup/viewmodels/app/sensors/gsr/GSRVideoPlayerViewModel.kt"),
        
        # App UI Components ViewModels
        ("app/src/main/java/mpdc4gsr/ui_components/MainFragmentViewModel.kt", "backup/viewmodels/app/ui_components/MainFragmentViewModel.kt"),
        
        # App General ViewModels
        ("app/src/main/java/mpdc4gsr/viewmodel/PolicyViewModel.kt", "backup/viewmodels/app/viewmodel/PolicyViewModel.kt"),
        ("app/src/main/java/mpdc4gsr/viewmodel/MainActivityViewModel.kt", "backup/viewmodels/app/viewmodel/MainActivityViewModel.kt"),
        
        # LibUnified ViewModels
        ("libunified/src/main/java/com/mpdc4gsr/libunified/app/viewmodel/VersionViewModel.kt", "backup/viewmodels/libunified/VersionViewModel.kt"),
        
        # Thermal ViewModels
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/viewmodel/IRMainActivityViewModel.kt", "backup/viewmodels/component/thermalunified/IRMainActivityViewModel.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/viewmodel/IRThermalFragmentViewModel.kt", "backup/viewmodels/component/thermalunified/IRThermalFragmentViewModel.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/viewmodel/ThermalFragmentViewModel.kt", "backup/viewmodels/component/thermalunified/ThermalFragmentViewModel.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/viewmodel/ThermalViewModel.kt", "backup/viewmodels/component/thermalunified/ThermalViewModel.kt"),
    ]
    
    moved_count = 0
    for legacy_path, backup_path in viewmodel_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total ViewModels moved: {moved_count}")

def move_related_adapters():
    """Move adapters related to backed up activities"""
    adapter_mappings = [
        # GSR Adapters that are specifically for legacy activities
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRDataRowAdapter.kt", "backup/viewmodels/app/sensors/gsr/GSRDataRowAdapter.kt"),
        ("app/src/main/java/mpdc4gsr/sensors/gsr/GSRDeviceAdapter.kt", "backup/viewmodels/app/sensors/gsr/GSRDeviceAdapter.kt"),
    ]
    
    moved_count = 0
    for legacy_path, backup_path in adapter_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total Adapters moved: {moved_count}")

if __name__ == "__main__":
    print("Creating ViewModel backup directories...")
    create_viewmodel_backup_dirs()
    print("\nMoving legacy ViewModels...")
    move_viewmodels()
    print("\nMoving related adapters...")
    move_related_adapters()
    print("\nViewModel and adapter migration complete!")