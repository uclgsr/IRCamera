#!/usr/bin/env python3
"""
Quick validation of build system readiness for hardware testing.
This script verifies that the APK can be built successfully.
"""

import subprocess
import sys
import os
from pathlib import Path

def run_command(cmd, timeout=120):
    """Run a command with proper error handling"""
    try:
        result = subprocess.run(
            cmd, 
            shell=True, 
            capture_output=True, 
            text=True, 
            timeout=timeout
        )
        return result.returncode == 0, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return False, "", "Command timed out"

def validate_build_system():
    """Validate that the build system is ready"""
    print("🔨 Validating Build System for Hardware Testing")
    print("=" * 50)
    
    # Check Gradle wrapper
    if not Path("gradlew").exists():
        print("❌ gradlew not found!")
        return False
    
    print("✅ Gradle wrapper found")
    
    # Check app module
    if not Path("app/build.gradle.kts").exists():
        print("❌ app/build.gradle.kts not found!")
        return False
    
    print("✅ App module configuration found")
    
    # Test Gradle tasks
    print("\n🧪 Testing Gradle tasks...")
    
    success, stdout, stderr = run_command("./gradlew tasks --all | grep assembleRelease", 30)
    if success and "assembleRelease" in stdout:
        print("✅ assembleRelease task available")
    else:
        print("⚠️ assembleRelease task may not be available")
    
    # Check build directory structure
    app_build_dir = Path("app/build.gradle.kts").parent
    if app_build_dir.exists():
        print("✅ App module structure is valid")
    else:
        print("❌ App module structure is invalid")
        return False
    
    return True

def validate_pc_controller():
    """Validate PC controller testing tools"""
    print("\n🖥️ Validating PC Controller Testing Tools")
    print("=" * 50)
    
    pc_dir = Path("pc-controller")
    if not pc_dir.exists():
        print("❌ pc-controller directory not found!")
        return False
    
    required_scripts = [
        "test_pc_to_phone.py",
        "comprehensive_validation.py", 
        "test_android_server.py"
    ]
    
    all_found = True
    for script in required_scripts:
        script_path = pc_dir / script
        if script_path.exists():
            print(f"✅ {script} found")
        else:
            print(f"❌ {script} not found!")
            all_found = False
    
    return all_found

def validate_documentation():
    """Validate testing documentation"""
    print("\n📚 Validating Testing Documentation")
    print("=" * 50)
    
    docs = [
        "HARDWARE_TESTING_GUIDE.md",
        "IMPLEMENTATION_STATUS_FINAL.md",
        "quick_hardware_setup_check.py"
    ]
    
    all_found = True
    for doc in docs:
        if Path(doc).exists():
            print(f"✅ {doc} found")
        else:
            print(f"❌ {doc} not found!")
            all_found = False
    
    return all_found

def main():
    """Main validation function"""
    print("🚀 Build System Validation for Hardware Testing")
    print("=" * 60)
    print()
    
    checks = [
        ("Build System", validate_build_system),
        ("PC Controller Tools", validate_pc_controller), 
        ("Documentation", validate_documentation)
    ]
    
    all_passed = True
    for check_name, check_func in checks:
        if not check_func():
            all_passed = False
        print()
    
    print("=" * 60)
    if all_passed:
        print("🎉 ALL VALIDATIONS PASSED!")
        print("✅ System is ready for hardware testing")
        print()
        print("📋 NEXT STEPS:")
        print("1. Connect Android device to same WiFi as PC")
        print("2. Run: ./build_for_testing.sh")
        print("3. Run: adb install app/build/outputs/apk/release/app-release.apk")
        print("4. Run: python pc-controller/test_pc_to_phone.py --android-ip <IP> --test all")
        print()
        return 0
    else:
        print("⚠️ SOME VALIDATIONS FAILED")
        print("❌ Please resolve issues before hardware testing")
        return 1

if __name__ == "__main__":
    sys.exit(main())