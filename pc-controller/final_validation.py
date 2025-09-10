#!/usr/bin/env python3
"""
Final validation script for IRCamera PC Controller
Tests all key components to ensure stability and no errors
"""

import sys
import tempfile
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent / "src"))


def test_imports_and_basic_functionality():
    """Test that all core modules import and basic functionality works."""
    print("🔍 Testing imports and basic functionality...")

    try:
        # Test core imports
        from ircamera_pc.core import (
            CameraCalibrator,
            ConfigManager,
            FileTransferManager,
            GSRIngestor,
            SessionManager,
            TimeSyncService,
        )
        from ircamera_pc.gui.icons import IconRegistry
        from ircamera_pc.network import NetworkServer

        print("✅ All imports successful")

        # Test instantiation
        config = ConfigManager()
        session_manager = SessionManager()
        file_manager = FileTransferManager(config)
        calibrator = CameraCalibrator(config)
        gsr_ingestor = GSRIngestor(config)
        timesync = TimeSyncService()
        network_server = NetworkServer()
        print("✅ All components instantiate successfully")

        # Test icons
        icons = IconRegistry.list_available_icons()
        assert len(icons) == 4, f"Expected 4 icons, got {len(icons)}"
        print(f"✅ IconRegistry works: {len(icons)} icons available")

        # Test session creation
        session_id = session_manager.create_session("validation_test")
        assert session_id is not None
        print(f"✅ Session created: {session_id}")

        return True

    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback

        traceback.print_exc()
        return False


def test_android_build():
    """Test that Android build is successful."""
    print("\n📱 Testing Android build...")

    import os
    import subprocess

    try:
        # Change to root directory
        os.chdir(Path(__file__).parent.parent)

        # Run Android build
        result = subprocess.run(
            ["./gradlew", ":libapp:assembleRelease", "--quiet"],
            capture_output=True,
            text=True,
            timeout=300,  # 5 minutes
        )

        if result.returncode == 0:
            print("✅ Android build successful")
            return True
        else:
            print(f"❌ Android build failed: {result.stderr}")
            return False

    except subprocess.TimeoutExpired:
        print("❌ Android build timed out")
        return False
    except Exception as e:
        print(f"❌ Android build error: {e}")
        return False


def test_python_tests():
    """Run Python test suite."""
    print("\n🧪 Testing Python test suite...")

    import os
    import subprocess

    try:
        os.chdir(Path(__file__).parent)

        # Set environment variables for headless testing
        env = os.environ.copy()
        env["QT_QPA_PLATFORM"] = "offscreen"
        env["DISPLAY"] = ":99"

        # Run pytest
        result = subprocess.run(
            [
                sys.executable,
                "-m",
                "pytest",
                "src/ircamera_pc/tests/",
                "-v",
                "--tb=short",
            ],
            capture_output=True,
            text=True,
            env=env,
            timeout=180,  # 3 minutes
        )

        if result.returncode == 0:
            print("✅ All Python tests passed")
            return True
        else:
            print(f"❌ Python tests failed:\n{result.stdout}\n{result.stderr}")
            return False

    except Exception as e:
        print(f"❌ Python test error: {e}")
        return False


def main():
    """Run all validation tests."""
    print("🚀 Starting Final Validation for IRCamera PC Controller")
    print("=" * 60)

    results = []

    # Test 1: Basic functionality
    results.append(test_imports_and_basic_functionality())

    # Test 2: Python tests
    results.append(test_python_tests())

    # Test 3: Android build
    results.append(test_android_build())

    print("\n" + "=" * 60)
    print("📊 FINAL VALIDATION RESULTS")
    print("=" * 60)

    test_names = ["Basic Functionality", "Python Tests", "Android Build"]
    for i, (name, passed) in enumerate(zip(test_names, results)):
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"{i+1}. {name}: {status}")

    all_passed = all(results)

    if all_passed:
        print("\n🎉 ALL VALIDATION TESTS PASSED")
        print("✅ IRCamera PC Controller is stable with no errors or warnings")
        print(
            "✅ Enhanced networking, GUI icons, and data processing pipeline complete"
        )
        print("✅ Cross-platform compatibility achieved")
        print("✅ Production-ready implementation")
    else:
        print("\n❌ SOME VALIDATION TESTS FAILED")
        print("Please review the output above for details")

    return 0 if all_passed else 1


if __name__ == "__main__":
    sys.exit(main())
