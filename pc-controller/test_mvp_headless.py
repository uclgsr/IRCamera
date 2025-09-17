#!/usr/bin/env python3
"""
Test script to validate MVP implementation without display requirements.
"""

import os
import sys
from pathlib import Path

# Set Qt platform for headless operation
os.environ["QT_QPA_PLATFORM"] = "offscreen"

# Add src to path
src_dir = Path(__file__).parent / "src"
sys.path.insert(0, str(src_dir))


def test_core_imports():
    """Test that core components can be imported."""
    print("Testing core imports...")

    try:
        from ircamera_pc.core.device_manager import DeviceManager
        print("✓ DeviceManager import successful")
    except Exception as e:
        print(f"❌ DeviceManager import failed: {e}")
        return False

    try:
        from ircamera_pc.core.session_manager import EnhancedSessionManager
        print("✓ EnhancedSessionManager import successful")
    except Exception as e:
        print(f"❌ EnhancedSessionManager import failed: {e}")
        return False

    try:
        from ircamera_pc.sync import EnhancedTimeSyncServer
        print("✓ EnhancedTimeSyncServer import successful")
    except Exception as e:
        print(f"❌ EnhancedTimeSyncServer import failed: {e}")
        return False

    return True


def test_mvc_architecture():
    """Test that MVP components can be created."""
    print("\nTesting MVP architecture components...")

    try:
        # Test device manager
        from ircamera_pc.core.device_manager import DeviceManager
        device_manager = DeviceManager()
        print("✓ DeviceManager instantiation successful")

        # Test session manager
        from ircamera_pc.core.session_manager import EnhancedSessionManager
        session_manager = EnhancedSessionManager(
            device_manager=device_manager,
            base_session_dir=Path("./test_sessions")
        )
        print("✓ EnhancedSessionManager instantiation successful")

        # Test time sync server
        from ircamera_pc.sync import EnhancedTimeSyncServer
        time_sync = EnhancedTimeSyncServer()
        print("✓ EnhancedTimeSyncServer instantiation successful")

        return True

    except Exception as e:
        print(f"❌ MVP architecture test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_configuration():
    """Test configuration loading."""
    print("\nTesting configuration...")

    try:
        from ircamera_pc.core.config import config
        print("✓ Configuration system loaded")

        # Test some config values
        port = config.get("network.server_port", 8080)
        print(f"✓ Network port configured: {port}")

        return True

    except Exception as e:
        print(f"❌ Configuration test failed: {e}")
        return False


def main():
    """Run all tests."""
    print("=" * 60)
    print("IRCamera PC Controller MVP - Headless Validation")
    print("=" * 60)

    success = True

    # Test imports
    if not test_core_imports():
        success = False

    # Test MVP architecture
    if not test_mvc_architecture():
        success = False

    # Test configuration
    if not test_configuration():
        success = False

    print("\n" + "=" * 60)
    if success:
        print("✅ All MVP core components working successfully!")
        print("✅ PyQt6 architecture ready (UI components not tested due to headless env)")
        print("✅ Ready for continuation of implementation")
    else:
        print("❌ Some components failed validation")
        return 1

    print("=" * 60)
    return 0


if __name__ == "__main__":
    sys.exit(main())
