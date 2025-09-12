#!/usr/bin/env python3
"""
Quick Hardware Setup Check for PC-to-Phone Communication

This script performs pre-flight checks to ensure the system is ready for hardware testing.
Run this before attempting to test with actual Android devices.
"""

import importlib
import os
import subprocess
import sys
from pathlib import Path


def check_python_environment():
    """Check Python version and required packages"""
    print("🐍 Checking Python Environment...")

    # Check Python version
    if sys.version_info < (3, 8):
        print(f"❌ Python {sys.version} is too old. Requires Python 3.8+")
        return False
    else:
        print(f"✅ Python {sys.version} is compatible")

    # Check required packages
    required_packages = ["socket", "json", "threading", "time", "argparse"]
    for package in required_packages:
        try:
            importlib.import_module(package)
            print(f"✅ {package} available")
        except ImportError:
            print(f"❌ {package} not available")
            return False

    return True


def check_pc_controller_scripts():
    """Check that all PC controller test scripts are present and functional"""
    print("\n🖥️ Checking PC Controller Scripts...")

    pc_controller_dir = Path("pc-controller")
    if not pc_controller_dir.exists():
        print("❌ pc-controller directory not found!")
        return False

    required_scripts = [
        "test_pc_to_phone.py",
        "comprehensive_validation.py",
        "test_android_server.py",
    ]

    for script in required_scripts:
        script_path = pc_controller_dir / script
        if not script_path.exists():
            print(f"❌ {script} not found!")
            return False
        else:
            print(f"✅ {script} found")

    # Test import of key modules
    try:
        sys.path.append(str(pc_controller_dir))
        from test_pc_to_phone import PCControllerTest

        from comprehensive_validation import ValidationReport

        print("✅ All test modules can be imported successfully")
        return True
    except ImportError as e:
        print(f"❌ Import error: {e}")
        return False


def check_android_build_system():
    """Check Android build system readiness"""
    print("\n🤖 Checking Android Build System...")

    # Check for Gradle wrapper
    gradlew_script = "gradlew" if os.name != "nt" else "gradlew.bat"
    if not Path(gradlew_script).exists():
        print(f"❌ {gradlew_script} not found!")
        return False
    else:
        print(f"✅ {gradlew_script} found")

    # Check for build script
    if not Path("build_for_testing.sh").exists():
        print("❌ build_for_testing.sh not found!")
        return False
    else:
        print("✅ build_for_testing.sh found")

    # Check for app module
    if not Path("app/build.gradle.kts").exists():
        print("❌ app/build.gradle.kts not found!")
        return False
    else:
        print("✅ Android app module found")

    return True


def check_network_tools():
    """Check network utilities availability"""
    print("\n🌐 Checking Network Tools...")

    # Check for adb (Android Debug Bridge)
    try:
        result = subprocess.run(
            ["adb", "version"], capture_output=True, text=True, timeout=5
        )
        if result.returncode == 0:
            print("✅ ADB is available")
            return True
        else:
            print("⚠️ ADB available but may have issues")
            return True
    except (subprocess.TimeoutExpired, FileNotFoundError):
        print("⚠️ ADB not found - install Android SDK Platform Tools for device testing")
        print(
            "   Download from: https://developer.android.com/studio/releases/platform-tools"
        )
        return True  # Not critical for setup check


def check_documentation():
    """Check that documentation files are present"""
    print("\n📚 Checking Documentation...")

    required_docs = ["HARDWARE_TESTING_GUIDE.md", "NEXT_STEPS_HARDWARE_TESTING.md"]

    all_found = True
    for doc in required_docs:
        if not Path(doc).exists():
            print(f"❌ {doc} not found!")
            all_found = False
        else:
            print(f"✅ {doc} found")

    return all_found


def generate_setup_summary():
    """Generate setup instructions summary"""
    print("\n📋 HARDWARE TESTING SETUP SUMMARY")
    print("=" * 50)
    print()
    print("To proceed with hardware testing:")
    print()
    print("1. 📱 ANDROID DEVICE SETUP:")
    print("   - Enable Developer Options")
    print("   - Enable USB Debugging")
    print("   - Connect to same WiFi as PC")
    print("   - Note device IP address")
    print()
    print("2. 🔨 BUILD APK:")
    print("   ./build_for_testing.sh")
    print("   # or")
    print("   ./gradlew app:assembleRelease")
    print()
    print("3. 📲 INSTALL APK:")
    print("   adb install app/build/outputs/apk/release/app-release.apk")
    print()
    print("4. 🧪 RUN TESTS:")
    print(
        "   python pc-controller/test_pc_to_phone.py --android-ip <DEVICE_IP> --test all"
    )
    print(
        "   python pc-controller/comprehensive_validation.py --android-ip <DEVICE_IP>"
    )
    print()
    print("5. 📊 CHECK RESULTS:")
    print("   - Connection within 5 seconds")
    print("   - Ping/pong latency < 100ms")
    print("   - All commands processed successfully")
    print()


def main():
    """Main setup check function"""
    print("🚀 IRCamera PC-to-Phone Hardware Setup Check")
    print("=" * 50)

    checks = [
        ("Python Environment", check_python_environment),
        ("PC Controller Scripts", check_pc_controller_scripts),
        ("Android Build System", check_android_build_system),
        ("Network Tools", check_network_tools),
        ("Documentation", check_documentation),
    ]

    all_passed = True
    for check_name, check_func in checks:
        if not check_func():
            all_passed = False

    print("\n" + "=" * 50)
    if all_passed:
        print("🎉 ALL CHECKS PASSED!")
        print("✅ System is ready for hardware testing")
        generate_setup_summary()
    else:
        print("⚠️ SOME CHECKS FAILED")
        print("❌ Please resolve issues before proceeding with hardware tests")
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
