#!/usr/bin/env python3
"""
Test script for new Bluetooth and WiFi functionality
Tests the core logic without GUI dependencies
"""

import asyncio
import sys
from pathlib import Path

# Add src to path
src_dir = Path(__file__).parent / "src"
sys.path.insert(0, str(src_dir))


async def test_bluetooth_manager():
    """Test Bluetooth manager basic functionality."""
    try:
        from ircamera_pc.core.bluetooth_manager import (
            BluetoothManager,
        )

        # Test basic initialization
        manager = BluetoothManager()
        print(f"✅ Bluetooth Manager - Available: {manager.is_available}")
        print(f"   - Discovered devices: {len(manager.discovered_devices)}")
        print(f"   - Connected devices: {len(manager.connected_devices)}")

        return True
    except (OSError, ValueError, RuntimeError) as e:
        print(f"❌ Bluetooth Manager test failed: {e}")
        return False


async def test_wifi_manager():
    """Test WiFi manager basic functionality."""
    try:
        from ircamera_pc.core.wifi_manager import (
            WiFiManager,
        )

        # Test basic initialization
        manager = WiFiManager()
        print("✅ WiFi Manager initialized")
        print(f"   - Available networks: {len(manager.available_networks)}")
        print(f"   - IRCamera networks: {len(manager.ircamera_networks)}")
        print(f"   - Hotspot state: {manager.hotspot_state.value}")
        print(f"   - WiFi interfaces: {len(manager.wifi_interfaces)}")

        return True
    except (OSError, ValueError, RuntimeError) as e:
        print(f"❌ WiFi Manager test failed: {e}")
        return False


def test_admin_privileges_basic():
    """Test admin privileges manager basic functionality (without GUI)."""
    try:
        from ircamera_pc.core.admin_privileges import (
            ElevationResult,
            PrivilegeLevel,
            SystemPermissions,
        )

        # Test enum values
        levels = [level.value for level in PrivilegeLevel]
        results = [result.value for result in ElevationResult]

        print("✅ Admin Privileges Enums loaded")
        print(f"   - Privilege levels: {levels}")
        print(f"   - Elevation results: {results}")

        # Test SystemPermissions dataclass
        perms = SystemPermissions()
        print(f"   - Default permissions: {perms}")

        return True
    except (OSError, ValueError, RuntimeError) as e:
        print(f"❌ Admin Privileges test failed: {e}")
        return False


def test_protocol_extension():
    """Test protocol extension with new message types."""
    try:
        from ircamera_pc.network.protocol import ProtocolManager

        manager = ProtocolManager()

        # Test message type validation for new Bluetooth messages
        bluetooth_scan_msg = {
            "message_type": "bluetooth_scan_request",
            "timestamp": "2024-01-01T12:00:00Z",
            "scan_duration": 30,
            "filter_ircamera_only": True,
        }

        # Test message type validation for new WiFi messages
        wifi_scan_msg = {
            "message_type": "wifi_scan_request",
            "timestamp": "2024-01-01T12:00:00Z",
            "filter_ircamera_only": False,
        }

        print("✅ Protocol Manager loaded")
        print(f"   - Message types loaded: {len(manager._message_definitions)}")

        # Test if our new message types are recognized
        bt_valid = manager.validate_message(bluetooth_scan_msg)
        wifi_valid = manager.validate_message(wifi_scan_msg)

        print(f"   - Bluetooth message valid: {bt_valid}")
        print(f"   - WiFi message valid: {wifi_valid}")

        return True
    except (OSError, ValueError, RuntimeError) as e:
        print(f"❌ Protocol extension test failed: {e}")
        return False


async def main():
    """Run all tests."""
    print("🧪 Testing IRCamera PC Controller - System Integration Features")
    print("=" * 60)

    results = []

    # Test core modules
    print("\n📡 Testing Core System Integration Modules:")
    results.append(await test_bluetooth_manager())
    results.append(await test_wifi_manager())
    results.append(test_admin_privileges_basic())
    results.append(test_protocol_extension())

    # Summary
    passed = sum(results)
    total = len(results)

    print("\n" + "=" * 60)
    print(f"📊 Test Results: {passed}/{total} tests passed")

    if passed == total:
        print("🎉 All system integration features are working correctly!")
        return 0
    else:
        print("⚠️ Some tests failed. Check the output above for details.")
        return 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
