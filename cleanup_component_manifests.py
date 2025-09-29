#!/usr/bin/env python3
import re

def clean_thermal_manifest():
    """Clean up the thermal unified manifest by removing migrated legacy activity references"""
    manifest_path = "component/thermalunified/src/main/AndroidManifest.xml"
    
    # Read the current manifest
    with open(manifest_path, 'r') as f:
        content = f.read()
    
    # Activities to remove (that have Compose equivalents and were moved to backup)
    legacy_activities_to_remove = [
        # Lite activities moved to backup
        '.lite.activity.IRCorrectionLiteThreeActivity',
        '.lite.activity.IRThermalLiteActivity',
        '.lite.activity.IRCorrectionLiteFourActivity',
        '.lite.activity.ImagePickIRLiteActivity',
        '.lite.activity.IRMonitorChartLiteActivity',
        '.lite.activity.IRMonitorLiteActivity',
        
        # Report activities moved to backup
        '.report.activity.ReportCreateSecondActivity',
        '.report.activity.ReportCreateFirstActivity',
        '.report.activity.ReportDetailActivity',
        '.report.activity.ReportPreviewFirstActivity',
        '.report.activity.ReportPreviewSecondActivity',
        
        # Main activities moved to backup
        '.activity.SimpleThermalConfigActivity',
    ]
    
    print(f"Cleaning thermal manifest: {manifest_path}")
    
    # Create a pattern that matches entire activity blocks
    for activity_name in legacy_activities_to_remove:
        # Pattern to match the entire activity block, including all attributes
        pattern = r'<activity[^>]*android:name="' + re.escape(activity_name) + r'"[^>]*(?:\s*android:[^>]*)*/>\s*'
        if re.search(pattern, content):
            content = re.sub(pattern, '', content, flags=re.MULTILINE | re.DOTALL)
            print(f"Removed activity: {activity_name}")
        else:
            # Try multi-line activity blocks
            pattern = r'<activity[^>]*android:name="' + re.escape(activity_name) + r'"[^>]*>.*?</activity>\s*'
            if re.search(pattern, content, re.MULTILINE | re.DOTALL):
                content = re.sub(pattern, '', content, flags=re.MULTILINE | re.DOTALL)
                print(f"Removed multi-line activity: {activity_name}")
            else:
                print(f"Activity not found in manifest: {activity_name}")
    
    # Clean up any extra empty lines
    content = re.sub(r'\n\s*\n\s*\n', '\n\n', content)
    
    return content

def clean_user_manifest():
    """Clean up the user manifest - Note: no user activities were migrated yet as they don't have Compose equivalents"""
    manifest_path = "component/user/src/main/AndroidManifest.xml"
    
    print(f"User manifest: {manifest_path}")
    print("No user activities migrated - they don't have Compose equivalents yet")
    
    # Read the current manifest and return unchanged for now
    with open(manifest_path, 'r') as f:
        content = f.read()
    
    return content

def clean_gsr_recording_manifest():
    """Clean up the GSR recording manifest if it exists"""
    manifest_path = "component/gsr-recording/src/main/AndroidManifest.xml"
    
    # Read the current manifest
    with open(manifest_path, 'r') as f:
        content = f.read()
    
    print(f"GSR recording manifest: {manifest_path}")
    print("No GSR recording activities migrated - they are infrastructure components")
    
    return content

if __name__ == "__main__":
    print("Cleaning up component manifests...")
    
    # Clean thermal manifest
    thermal_content = clean_thermal_manifest()
    with open("component/thermalunified/src/main/AndroidManifest.xml", 'w') as f:
        f.write(thermal_content)
    print("Thermal manifest cleaned.")
    
    # Clean user manifest (no changes for now)
    user_content = clean_user_manifest()
    with open("component/user/src/main/AndroidManifest.xml", 'w') as f:
        f.write(user_content)
    print("User manifest checked.")
    
    # Clean GSR recording manifest (no changes for now)
    gsr_content = clean_gsr_recording_manifest()
    with open("component/gsr-recording/src/main/AndroidManifest.xml", 'w') as f:
        f.write(gsr_content)
    print("GSR recording manifest checked.")
    
    print("\nComponent manifest cleanup complete!")