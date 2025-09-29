#!/usr/bin/env python3
import re

def clean_app_manifest():
    """Clean up the app manifest by removing legacy activity references"""
    manifest_path = "app/src/main/AndroidManifest.xml"
    
    # Read the current manifest
    with open(manifest_path, 'r') as f:
        content = f.read()
    
    # Activities to remove (that have Compose equivalents and were moved to backup)
    legacy_activities_to_remove = [
        # Main legacy activities
        'mpdc4gsr.activities.MainActivity',
        'mpdc4gsr.activities.PolicyActivity', 
        'mpdc4gsr.activities.VersionActivity',
        'mpdc4gsr.activities.FaultTolerantRecordingActivity',
        
        # GSR legacy activities
        'mpdc4gsr.sensors.gsr.MultiModalRecordingActivity',
        'mpdc4gsr.sensors.gsr.GSRQuickRecordingActivity',
        'mpdc4gsr.sensors.gsr.SessionManagerActivity',
        'mpdc4gsr.sensors.gsr.ResearchTemplateActivity',
        'mpdc4gsr.sensors.gsr.SessionDetailActivity',
        'mpdc4gsr.sensors.gsr.SessionExportActivity',
        'mpdc4gsr.sensors.gsr.GSRVideoPlayerActivity',
        'mpdc4gsr.sensors.gsr.GSRPlotActivity',
        'mpdc4gsr.sensors.gsr.GSRDataViewActivity',
        'mpdc4gsr.sensors.gsr.GSRSettingsActivity',
        'mpdc4gsr.sensors.gsr.GSRRawImageViewActivity',
        'mpdc4gsr.sensors.gsr.GSRGalleryActivity',
        'mpdc4gsr.sensors.gsr.GSRDeviceManagementActivity',
        'mpdc4gsr.sensors.gsr.ShimmerConfigActivity',
        
        # Test activities
        'mpdc4gsr.sensors.gsr.GSRBenchTestActivity',
        'mpdc4gsr.sensors.gsr.GSRDataIntegrityTestActivity',
        'mpdc4gsr.sensors.gsr.GSRReconnectionTestActivity',
        'mpdc4gsr.sensors.gsr.ParallelRecordingTestActivity',
        'mpdc4gsr.sensors.gsr.SessionLifecycleTestActivity',
        'mpdc4gsr.sensors.gsr.CompleteSessionTrialActivity',
        'mpdc4gsr.sensors.gsr.CrossModalSyncTestActivity',
        'mpdc4gsr.sensors.gsr.SynchronizationTestActivity',
        'mpdc4gsr.sensors.gsr.TimeSynchronizationTestActivity',
        'mpdc4gsr.sensors.gsr.BLEIntegrationTestActivity',
        'mpdc4gsr.sensors.gsr.RawCaptureTestActivity',
        'mpdc4gsr.sensors.gsr.RgbCameraTestActivity',
        
        # Network and device activities
        'mpdc4gsr.network.DevicePairingActivity',
        'mpdc4gsr.permissions.PermissionRequestActivity',
        'mpdc4gsr.camera.integration.DualModeCameraActivity',
        'mpdc4gsr.sensors.HubSpokeIntegrationActivity',
    ]
    
    # Remove activity entries for legacy activities
    for activity_name in legacy_activities_to_remove:
        # Pattern to match the entire activity block
        pattern = r'<activity[^>]*android:name="' + re.escape(activity_name) + r'"[^>]*>.*?</activity>'
        content = re.sub(pattern, '', content, flags=re.DOTALL)
        
        # Also remove single-line activity entries
        pattern = r'<activity[^>]*android:name="' + re.escape(activity_name) + r'"[^>]*/>'
        content = re.sub(pattern, '', content)
    
    # Clean up duplicate entries and redundant whitespace
    content = re.sub(r'\n\s*\n\s*\n', '\n\n', content)
    
    return content

def clean_thermal_manifest():
    """Clean up the thermal unified manifest by removing legacy activity references"""
    manifest_path = "component/thermalunified/src/main/AndroidManifest.xml"
    
    # Read the current manifest
    with open(manifest_path, 'r') as f:
        content = f.read()
    
    # Thermal activities to remove (that have Compose equivalents and were moved to backup)
    legacy_activities_to_remove = [
        '.activity.ThermalActivity',
        '.activity.MonitorActivity', 
        '.activity.ConnectActivity',
        '.activity.GalleryActivity',
        '.activity.VideoActivity',
        '.activity.MonitorChartActivity',
        '.activity.LogMPChartActivity',
        '.activity.BaseIRActivity',
        '.activity.IRMainActivity',
        '.activity.IRThermalPlusActivity',
        '.activity.IRThermalNightActivity',
        '.activity.IRCameraSettingActivity',
        '.activity.IREmissivityActivity',
        '.activity.IRCorrectionActivity',
        '.activity.IRCorrectionTwoActivity',
        '.activity.IRCorrectionFourActivity',
        '.activity.AlgorithmImageActivity',
        '.activity.ManualStep1Activity',
        '.activity.ManualStep2Activity',
        '.activity.MonitoryHomeActivity',
        '.activity.ImagePickIRPlushActivity',
        '.activity.ImagePickIRActivity',
        '.activity.IRGalleryDetail01Activity',
        '.activity.IRGalleryDetail04Activity',
        '.activity.IRGalleryHomeActivity',
        '.activity.IRVideoGSYActivity',
        '.activity.ReportPickImgActivity',
        '.activity.IRMonitorChartActivity',
    ]
    
    # Remove activity entries for legacy activities
    for activity_name in legacy_activities_to_remove:
        # Pattern to match the entire activity block
        pattern = r'<activity[^>]*android:name="' + re.escape(activity_name) + r'"[^>]*>.*?</activity>'
        content = re.sub(pattern, '', content, flags=re.DOTALL)
        
        # Also remove single-line activity entries
        pattern = r'<activity[^>]*android:name="' + re.escape(activity_name) + r'"[^>]*/>'
        content = re.sub(pattern, '', content)
    
    # Clean up duplicate entries and redundant whitespace
    content = re.sub(r'\n\s*\n\s*\n', '\n\n', content)
    
    return content

if __name__ == "__main__":
    print("Cleaning up app manifest...")
    app_content = clean_app_manifest()
    with open("app/src/main/AndroidManifest.xml", 'w') as f:
        f.write(app_content)
    print("App manifest cleaned.")
    
    print("\nCleaning up thermal manifest...")
    thermal_content = clean_thermal_manifest()
    with open("component/thermalunified/src/main/AndroidManifest.xml", 'w') as f:
        f.write(thermal_content)
    print("Thermal manifest cleaned.")
    
    print("\nManifest cleanup complete!")