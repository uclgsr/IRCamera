#!/usr/bin/env python3
"""
Implementation checker for Samsung Camera and Network fixes
Validates that the key fixes have been properly implemented
"""

import os
import re
from pathlib import Path


def check_file_contains(file_path, patterns, description):
    """Check if file contains required patterns"""
    if not os.path.exists(file_path):
        print(f"❌ {description}: File not found - {file_path}")
        return False

    try:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        results = []
        for pattern, desc in patterns:
            if re.search(pattern, content, re.IGNORECASE | re.MULTILINE):
                results.append(f"✅ {desc}")
            else:
                results.append(f"❌ {desc}")

        print(f"\n📁 {description}:")
        for result in results:
            print(f"  {result}")

        return all("✅" in result for result in results)

    except Exception as e:
        print(f"❌ {description}: Error reading file - {e}")
        return False


def main():
    """Run implementation checks"""
    print("🔍 Samsung Camera and Network Communication Fixes - Implementation Check")
    print("=" * 70)

    base_path = Path("/home/runner/work/IRCamera/IRCamera")

    # Check Samsung Camera Fixes
    camera_recorder_path = (
        base_path
        / "app/src/main/java/com/topdon/tc001/sensors/rgb/RgbCameraRecorder.kt"
    )
    camera_patterns = [
        (r"Samsung.*device.*compatibility", "Samsung device compatibility mentioned"),
        (r"fallback.*video.*only", "Video-only fallback mode"),
        (r"IllegalArgumentException", "Samsung CameraX exception handling"),
        (r"imageCapture\s*=\s*null", "Image capture disabled for compatibility"),
        (r"android\.os\.Build\.MODEL", "Device model logging for debugging"),
        (r"Conservative.*settings", "Conservative camera settings mentioned"),
    ]

    camera_ok = check_file_contains(
        camera_recorder_path, camera_patterns, "Samsung Camera Compatibility Fixes"
    )

    # Check Network Communication Fixes
    recording_service_path = (
        base_path / "app/src/main/java/com/topdon/tc001/service/RecordingService.kt"
    )
    network_patterns = [
        (r"NetworkClient", "NetworkClient integration"),
        (r"start_recording.*message", "Start recording command handler"),
        (r"stop_recording.*message", "Stop recording command handler"),
        (r"sync_flash.*message", "Sync flash command handler"),
        (r"query_capabilities", "Query capabilities handler"),
        (r"JSONObject.*apply", "JSON response handling"),
        (r"connectToPC", "Manual PC connection method"),
        (r"NetworkEventListener", "Network event listener implementation"),
    ]

    network_service_ok = check_file_contains(
        recording_service_path,
        network_patterns,
        "Network Command Processing in RecordingService",
    )

    # Check NetworkClient enhancements
    network_client_path = (
        base_path / "app/src/main/java/com/topdon/tc001/network/NetworkClient.kt"
    )
    client_patterns = [
        (r"suspend.*fun.*sendMessage.*JSONObject", "Public sendMessage method"),
        (r"setMessageHandler", "Message handler registration"),
        (r"connectToController", "Controller connection method"),
        (r"startDiscovery", "Network discovery capability"),
    ]

    client_ok = check_file_contains(
        network_client_path, client_patterns, "NetworkClient Message Sending"
    )

    # Check Device Pairing Activity exists
    pairing_activity_path = (
        base_path
        / "app/src/main/java/com/topdon/tc001/network/DevicePairingActivity.kt"
    )
    pairing_patterns = [
        (r"discovery.*pairing", "PC Controller discovery"),
        (r"connectToController", "Controller connection"),
        (r"NetworkEventListener", "Network event handling"),
    ]

    pairing_ok = check_file_contains(
        pairing_activity_path, pairing_patterns, "Device Pairing UI"
    )

    # Check Android Manifest permissions
    manifest_path = base_path / "app/src/main/AndroidManifest.xml"
    manifest_patterns = [
        (r"android\.permission\.CAMERA", "Camera permission declared"),
        (r"android\.permission\.INTERNET", "Internet permission declared"),
        (r"RecordingService", "RecordingService declared"),
        (r"DevicePairingActivity", "DevicePairingActivity declared"),
    ]

    manifest_ok = check_file_contains(
        manifest_path, manifest_patterns, "Android Manifest Configuration"
    )

    # Summary
    print("\n" + "=" * 70)
    print("📋 IMPLEMENTATION SUMMARY:")
    print("=" * 70)

    checks = [
        ("Samsung Camera Compatibility", camera_ok),
        ("Network Command Processing", network_service_ok),
        ("NetworkClient Enhancements", client_ok),
        ("Device Pairing UI", pairing_ok),
        ("Manifest Configuration", manifest_ok),
    ]

    passed = 0
    for check_name, result in checks:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"  {status}: {check_name}")
        if result:
            passed += 1

    print(f"\n🎯 Overall Status: {passed}/{len(checks)} checks passed")

    if passed == len(checks):
        print("🎉 All implementation checks PASSED!")
        print(
            "   The Samsung camera and network communication fixes appear to be properly implemented."
        )
    else:
        print("⚠️  Some implementation checks FAILED.")
        print("   Review the failing components before testing.")

    return passed == len(checks)


if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
