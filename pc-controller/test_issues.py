#!/usr/bin/env python3
"""
Test script to isolate and validate the specific issues mentioned in the PR comments.
"""

import os
import sys
from pathlib import Path

# Set Qt platform for headless operation
os.environ["QT_QPA_PLATFORM"] = "offscreen"

# Add src to path
src_dir = Path(__file__).parent / "src"
sys.path.insert(0, str(src_dir))


def test_device_manager_connect_method():
    """Test if DeviceManager has connect_to_device method."""
    print("Testing DeviceManager.connect_to_device method...")

    try:
        from ircamera_pc.core.device_manager import DeviceManager

        # Check if method exists
        device_manager = DeviceManager()
        if hasattr(device_manager, 'connect_to_device'):
            print("✓ connect_to_device method exists")
            return True
        else:
            print("❌ connect_to_device method is missing")
            return False

    except Exception as e:
        print(f"❌ DeviceManager import failed: {e}")
        return False


def test_discovery_callback_events():
    """Test the discovery callback event handling."""
    print("\nTesting discovery callback events...")

    try:
        from ircamera_pc.core.device_manager import DeviceManager
        from ircamera_pc.network.discovery import NetworkDiscoveryService, DiscoveredDevice, \
            DeviceType
        from datetime import datetime

        device_manager = DeviceManager()

        # Check what callbacks are registered and what they expect
        discovery_service = device_manager.discovery_service

        # Create a mock discovered device
        mock_device = DiscoveredDevice(
            service_name="test_device",
            service_type="_test._tcp.local.",
            ip_address="192.168.1.100",
            port=8080,
            device_type=DeviceType.ANDROID_SENSOR_NODE,
            attributes={},
            discovered_at=datetime.now(),
            last_seen=datetime.now()
        )

        # Check callback method names
        callback_method = device_manager._on_device_discovered
        print(f"✓ Callback method exists: {callback_method.__name__}")

        # Test the callback signature
        try:
            # This should fail with current implementation
            callback_method("device_discovered", mock_device)
            print("✓ Callback accepts 'device_discovered' event")
        except Exception as e:
            print(f"❌ Callback fails with 'device_discovered': {e}")

        try:
            # This should work if fixed
            callback_method("discovered", mock_device)
            print("✓ Callback accepts 'discovered' event")
        except Exception as e:
            print(f"❌ Callback fails with 'discovered': {e}")

        return True

    except Exception as e:
        print(f"❌ Discovery callback test failed: {e}")
        return False


def test_register_device_signature():
    """Test the register_device method signature."""
    print("\nTesting register_device method signature...")

    try:
        from ircamera_pc.core.device_manager import DeviceRegistry, DeviceInfo, DeviceType, \
            DeviceConnectionState
        from ircamera_pc.network.discovery import DiscoveredDevice
        from datetime import datetime

        registry = DeviceRegistry()

        # Test with DiscoveredDevice (correct way)
        discovered_device = DiscoveredDevice(
            service_name="test_device",
            service_type="_test._tcp.local.",
            ip_address="192.168.1.100",
            port=8080,
            device_type=DeviceType.ANDROID_SENSOR_NODE,
            attributes={},
            discovered_at=datetime.now(),
            last_seen=datetime.now()
        )

        try:
            device_id = registry.register_device(discovered_device)
            print(f"✓ register_device works with DiscoveredDevice: {device_id}")
        except Exception as e:
            print(f"❌ register_device fails with DiscoveredDevice: {e}")

        # Test with wrong signature (should fail)
        device_info = DeviceInfo(
            device_id="test_manual_device",
            device_name="Test Manual Device",
            device_type=DeviceType.ANDROID_SENSOR_NODE,
            ip_address="192.168.1.100",
            port=8080,
            state=DeviceConnectionState.DISCOVERED,
            capabilities=["manual_device"]
        )

        try:
            registry.register_device("test_manual_device", device_info)
            print("❌ register_device incorrectly accepts two arguments")
        except Exception as e:
            print(f"✓ register_device correctly rejects two arguments: {e}")

        return True

    except Exception as e:
        print(f"❌ Register device test failed: {e}")
        return False


def test_attribute_naming():
    """Test the attribute naming consistency."""
    print("\nTesting attribute naming consistency...")

    try:
        from ircamera_pc.network.discovery import DiscoveredDevice, DeviceType
        from datetime import datetime

        # Create a DiscoveredDevice
        device = DiscoveredDevice(
            service_name="test_device",
            service_type="_test._tcp.local.",
            ip_address="192.168.1.100",  # This should be the correct attribute name
            port=8080,
            device_type=DeviceType.ANDROID_SENSOR_NODE,
            attributes={},
            discovered_at=datetime.now(),
            last_seen=datetime.now()
        )

        # Test correct attribute name
        try:
            ip = device.ip_address
            print(f"✓ ip_address attribute works: {ip}")
        except AttributeError:
            print("❌ ip_address attribute missing")

        # Test incorrect attribute name
        try:
            ip = device.ipAddress
            print("❌ ipAddress attribute incorrectly exists")
        except AttributeError:
            print("✓ ipAddress attribute correctly missing")

        return True

    except Exception as e:
        print(f"❌ Attribute naming test failed: {e}")
        return False


def main():
    """Run all issue tests."""
    print("=" * 60)
    print("IRCamera PC Controller - Issue Validation Test")
    print("=" * 60)

    success = True

    # Test each issue
    if not test_device_manager_connect_method():
        success = False

    if not test_discovery_callback_events():
        success = False

    if not test_register_device_signature():
        success = False

    if not test_attribute_naming():
        success = False

    print("\n" + "=" * 60)
    if success:
        print("✅ All issue tests completed")
    else:
        print("❌ Some issues detected - need to be fixed")

    print("=" * 60)
    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
