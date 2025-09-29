#!/usr/bin/env python3
import os
import shutil

def move_thermal_activities():
    """Move thermal component legacy activities to backup"""
    thermal_mappings = [
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/GalleryActivity.kt", "backup/activities/component/thermalunified/GalleryActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ThermalVideoActivity.kt", "backup/activities/component/thermalunified/ThermalVideoActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRCorrectionLiteThreeActivity.kt", "backup/activities/component/thermalunified/IRCorrectionLiteThreeActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/LogMPChartActivity.kt", "backup/activities/component/thermalunified/LogMPChartActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ThermalGalleryActivity.kt", "backup/activities/component/thermalunified/ThermalGalleryActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/MonitoryHomeActivity.kt", "backup/activities/component/thermalunified/MonitoryHomeActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ThermalCameraActivity.kt", "backup/activities/component/thermalunified/ThermalCameraActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ThermalMonitoringActivity.kt", "backup/activities/component/thermalunified/ThermalMonitoringActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ReportPickImgActivity.kt", "backup/activities/component/thermalunified/ReportPickImgActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRVideoGSYActivity.kt", "backup/activities/component/thermalunified/IRVideoGSYActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRMonitorChartActivity.kt", "backup/activities/component/thermalunified/IRMonitorChartActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ManualStep1Activity.kt", "backup/activities/component/thermalunified/ManualStep1Activity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRGalleryDetail04Activity.kt", "backup/activities/component/thermalunified/IRGalleryDetail04Activity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRThermalPlusActivity.kt", "backup/activities/component/thermalunified/IRThermalPlusActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRGalleryHomeActivity.kt", "backup/activities/component/thermalunified/IRGalleryHomeActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ConnectActivity.kt", "backup/activities/component/thermalunified/ConnectActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/BaseIRActivity.kt", "backup/activities/component/thermalunified/BaseIRActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRCorrectionFourActivity.kt", "backup/activities/component/thermalunified/IRCorrectionFourActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ImagePickIRActivity.kt", "backup/activities/component/thermalunified/ImagePickIRActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRCorrectionTwoActivity.kt", "backup/activities/component/thermalunified/IRCorrectionTwoActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ImagePickIRPlushActivity.kt", "backup/activities/component/thermalunified/ImagePickIRPlushActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/AlgorithmImageActivity.kt", "backup/activities/component/thermalunified/AlgorithmImageActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/MonitorActivity.kt", "backup/activities/component/thermalunified/MonitorActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRCameraSettingActivity.kt", "backup/activities/component/thermalunified/IRCameraSettingActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IREmissivityActivity.kt", "backup/activities/component/thermalunified/IREmissivityActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRCorrectionActivity.kt", "backup/activities/component/thermalunified/IRCorrectionActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ThermalActivity.kt", "backup/activities/component/thermalunified/ThermalActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRMainActivity.kt", "backup/activities/component/thermalunified/IRMainActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/ManualStep2Activity.kt", "backup/activities/component/thermalunified/ManualStep2Activity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/VideoActivity.kt", "backup/activities/component/thermalunified/VideoActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRThermalNightActivity.kt", "backup/activities/component/thermalunified/IRThermalNightActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/MonitorChartActivity.kt", "backup/activities/component/thermalunified/MonitorChartActivity.kt"),
        ("component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/activity/IRGalleryDetail01Activity.kt", "backup/activities/component/thermalunified/IRGalleryDetail01Activity.kt"),
    ]
    
    moved_count = 0
    for legacy_path, backup_path in thermal_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total thermal activities moved: {moved_count}")

def move_user_activities():
    """Move user component legacy activities to backup"""
    user_mappings = [
        ("component/user/src/main/java/com/mpdc4gsr/module/user/activity/AutoSaveActivity.kt", "backup/activities/component/user/AutoSaveActivity.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/activity/QuestionDetailsActivity.kt", "backup/activities/component/user/QuestionDetailsActivity.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/activity/UnitActivity.kt", "backup/activities/component/user/UnitActivity.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/activity/DeviceDetailsActivity.kt", "backup/activities/component/user/DeviceDetailsActivity.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/activity/QuestionActivity.kt", "backup/activities/component/user/QuestionActivity.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/activity/ElectronicManualActivity.kt", "backup/activities/component/user/ElectronicManualActivity.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/activity/MoreActivity.kt", "backup/activities/component/user/MoreActivity.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/activity/TISRActivity.kt", "backup/activities/component/user/TISRActivity.kt"),
        ("component/user/src/main/java/com/mpdc4gsr/module/user/activity/StorageSpaceActivity.kt", "backup/activities/component/user/StorageSpaceActivity.kt"),
    ]
    
    moved_count = 0
    for legacy_path, backup_path in user_mappings:
        if os.path.exists(legacy_path):
            # Create parent directory if needed
            os.makedirs(os.path.dirname(backup_path), exist_ok=True)
            
            # Move file
            shutil.move(legacy_path, backup_path)
            print(f"Moved: {legacy_path} -> {backup_path}")
            moved_count += 1
        else:
            print(f"File not found: {legacy_path}")
    
    print(f"Total user activities moved: {moved_count}")

if __name__ == "__main__":
    print("Moving thermal component legacy activities...")
    move_thermal_activities()
    print("\nMoving user component legacy activities...")
    move_user_activities()
    print("\nComponent module migration complete!")