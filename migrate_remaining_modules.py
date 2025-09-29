#!/usr/bin/env python3
import os
import shutil

def create_backup_dirs():
    """Create necessary backup directories for remaining modules"""
    dirs = [
        "backup/activities/component/thermalunified",
        "backup/activities/component/user", 
        "backup/activities/component/gsr-recording",
        "backup/activities/libunified",
        "backup/activities/BleModule",
        "backup/fragments/component/thermalunified",
        "backup/fragments/component/user",
        "backup/fragments/component/gsr-recording", 
        "backup/fragments/libunified",
        "backup/viewmodels/component/thermalunified",
        "backup/viewmodels/component/user",
        "backup/viewmodels/component/gsr-recording",
        "backup/viewmodels/libunified"
    ]
    for dir_path in dirs:
        os.makedirs(dir_path, exist_ok=True)
        print(f"Created directory: {dir_path}")

def move_thermal_legacy_activities():
    """Move thermal component legacy activities that have Compose equivalents"""
    # Only moving activities that have corresponding Compose versions
    thermal_legacy_mappings = [
        # Legacy activities with Compose equivalents
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/lite/activity/IRCorrectionLiteThreeActivity.kt", 
         "backup/activities/component/thermalunified/IRCorrectionLiteThreeActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/lite/activity/IRThermalLiteActivity.kt", 
         "backup/activities/component/thermalunified/IRThermalLiteActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/lite/activity/IRCorrectionLiteFourActivity.kt", 
         "backup/activities/component/thermalunified/IRCorrectionLiteFourActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/lite/activity/ImagePickIRLiteActivity.kt", 
         "backup/activities/component/thermalunified/ImagePickIRLiteActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/lite/activity/IRMonitorChartLiteActivity.kt", 
         "backup/activities/component/thermalunified/IRMonitorChartLiteActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/lite/activity/IRMonitorLiteActivity.kt", 
         "backup/activities/component/thermalunified/IRMonitorLiteActivity.kt"),
        
        # Report activities with Compose equivalents  
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/report/activity/ReportCreateSecondActivity.kt", 
         "backup/activities/component/thermalunified/ReportCreateSecondActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/report/activity/ReportCreateFirstActivity.kt", 
         "backup/activities/component/thermalunified/ReportCreateFirstActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/report/activity/ReportDetailActivity.kt", 
         "backup/activities/component/thermalunified/ReportDetailActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/report/activity/ReportPreviewFirstActivity.kt", 
         "backup/activities/component/thermalunified/ReportPreviewFirstActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/report/activity/ReportPreviewSecondActivity.kt", 
         "backup/activities/component/thermalunified/ReportPreviewSecondActivity.kt"),
        
        # Main activities with Compose equivalents
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/SimpleThermalConfigActivity.kt", 
         "backup/activities/component/thermalunified/SimpleThermalConfigActivity.kt"),
    ]
    
    moved_count = 0
    for legacy_path, backup_path in thermal_legacy_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total thermal legacy activities moved: {moved_count}")

def move_thermal_legacy_fragments():
    """Move thermal component legacy fragments that have Compose equivalents"""
    thermal_fragment_mappings = [
        # Legacy fragments with Compose equivalents
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/MonitorThermalFragment.kt", 
         "backup/fragments/component/thermalunified/MonitorThermalFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/AbilityFragment.kt", 
         "backup/fragments/component/thermalunified/AbilityFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/IRGalleryFragment.kt", 
         "backup/fragments/component/thermalunified/IRGalleryFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/GalleryPictureFragment.kt", 
         "backup/fragments/component/thermalunified/GalleryPictureFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/IRMonitorCaptureFragment.kt", 
         "backup/fragments/component/thermalunified/IRMonitorCaptureFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/IRMonitorHistoryFragment.kt", 
         "backup/fragments/component/thermalunified/IRMonitorHistoryFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/GalleryVideoFragment.kt", 
         "backup/fragments/component/thermalunified/GalleryVideoFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/PDFListFragment.kt", 
         "backup/fragments/component/thermalunified/PDFListFragment.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/fragment/IRPlushFragment.kt", 
         "backup/fragments/component/thermalunified/IRPlushFragment.kt"),
    ]
    
    moved_count = 0
    for legacy_path, backup_path in thermal_fragment_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total thermal legacy fragments moved: {moved_count}")

def move_user_legacy_fragments():
    """Move user component legacy fragments that have Compose equivalents"""
    user_fragment_mappings = [
        ("component/user/src/main/java/com/mpdc4gsr/module/user/fragment/MineFragment.kt", 
         "backup/fragments/component/user/MineFragment.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/fragment/MoreFragment.kt", 
         "backup/fragments/component/user/MoreFragment.kt"),
    ]
    
    moved_count = 0
    for legacy_path, backup_path in user_fragment_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total user legacy fragments moved: {moved_count}")

def move_legacy_viewmodels():
    """Move ViewModels that are associated with legacy activities/fragments being moved"""
    # Only moving ViewModels that are specifically tied to legacy components being migrated
    viewmodel_mappings = [
        # Thermal ViewModels for legacy components 
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/viewmodel/IRGalleryEditViewModel.kt", 
         "backup/viewmodels/component/thermalunified/IRGalleryEditViewModel.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/viewmodel/LogViewModel.kt", 
         "backup/viewmodels/component/thermalunified/LogViewModel.kt"),
        
        # User ViewModels for legacy components
        ("component/user/src/main/java/com/mpdc4gsr/module/user/viewmodel/MoreFragmentViewModel.kt", 
         "backup/viewmodels/component/user/MoreFragmentViewModel.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/viewmodel/MoreActivityViewModel.kt", 
         "backup/viewmodels/component/user/MoreActivityViewModel.kt"),
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
    
    print(f"Total legacy ViewModels moved: {moved_count}")

def move_ble_module_legacy_components():
    """Move BleModule legacy components if they exist"""
    # Check if BleModule has any legacy activities or fragments that need migration
    print("Checking BleModule for legacy components...")
    
    # Find any activities or fragments in BleModule
    ble_activities = []
    ble_fragments = []
    
    if os.path.exists("BleModule"):
        for root, dirs, files in os.walk("BleModule"):
            for file in files:
                if file.endswith(".kt"):
                    if "activity" in root.lower() or "Activity" in file:
                        ble_activities.append(os.path.join(root, file))
                    elif "fragment" in root.lower() or "Fragment" in file:
                        ble_fragments.append(os.path.join(root, file))
    
    print(f"Found {len(ble_activities)} BleModule activities")
    print(f"Found {len(ble_fragments)} BleModule fragments")
    
    # For now, just report what we found - we'll only migrate if we find Compose equivalents
    moved_count = 0
    print(f"Total BleModule components moved: {moved_count}")

if __name__ == "__main__":
    print("Creating backup directories for remaining modules...")
    create_backup_dirs()
    
    print("\nMoving thermal component legacy activities...")
    move_thermal_legacy_activities()
    
    print("\nMoving thermal component legacy fragments...")
    move_thermal_legacy_fragments()
    
    print("\nMoving user component legacy fragments...")
    move_user_legacy_fragments()
    
    print("\nMoving legacy ViewModels...")
    move_legacy_viewmodels()
    
    print("\nChecking BleModule components...")
    move_ble_module_legacy_components()
    
    print("\nRemaining modules migration complete!")