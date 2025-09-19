#!/usr/bin/env python3


import asyncio
import sys
import tempfile
from pathlib import Path


sys.path.insert(0, str(Path(__file__).parent / "src"))

from loguru import logger
from ircamera_pc.core.device_manager import DeviceManager, DeviceConnectionState
from ircamera_pc.core.session_manager import AdvancedSessionManager, SessionConfiguration, \
    SessionState
from ircamera_pc.network.discovery import DeviceType, DiscoveredDevice


async def test_device_manager():
    
    print("\n=== Testing Device Manager ===")

    device_manager = DeviceManager()

    
    print("Starting device manager...")
    success = await device_manager.start()
    if success:
        print("✓ Device manager started successfully")
    else:
        print("✗ Failed to start device manager")
        return False

    
    print("Adding mock devices...")
    device_id1 = device_manager.add_device_manually("192.168.1.100", 8080, "TestDevice1")
    device_id2 = device_manager.add_device_manually("192.168.1.101", 8080, "TestDevice2")

    if device_id1 and device_id2:
        print(f"✓ Added devices: {device_id1}, {device_id2}")
    else:
        print("✗ Failed to add mock devices")
        return False

    
    registry = device_manager.get_registry()

    print("Testing device state updates...")
    success1 = registry.update_device_state(device_id1, DeviceConnectionState.ONLINE)
    success2 = registry.update_device_state(device_id2, DeviceConnectionState.ONLINE)

    if success1 and success2:
        print("✓ Device states updated successfully")
    else:
        print("✗ Failed to update device states")
        return False

    
    online_devices = registry.get_online_devices()
    print(f"✓ Found {len(online_devices)} online devices")

    
    registry.update_heartbeat(device_id1)
    registry.update_heartbeat(device_id2)
    print("✓ Heartbeats updated")

    
    await device_manager.stop()
    print("✓ Device manager stopped")

    return True


async def test_session_manager():
    
    print("\n=== Testing Session Manager ===")

    
    with tempfile.TemporaryDirectory() as temp_dir:
        session_dir = Path(temp_dir) / "sessions"

        
        device_manager = DeviceManager()
        await device_manager.start()

        
        device_id1 = device_manager.add_device_manually("192.168.1.100", 8080, "TestDevice1")
        device_id2 = device_manager.add_device_manually("192.168.1.101", 8080, "TestDevice2")

        
        registry = device_manager.get_registry()
        registry.update_device_state(device_id1, DeviceConnectionState.ONLINE)
        registry.update_device_state(device_id2, DeviceConnectionState.ONLINE)

        
        session_manager = AdvancedSessionManager(device_manager, session_dir)

        
        print("Creating test session...")
        config = SessionConfiguration(
            session_name="Test Session",
            modalities=["rgb", "gsr"],
            auto_start_recording=False
        )

        session_id = session_manager.create_session("Test Session MVP", config)
        if session_id:
            print(f"✓ Session created: {session_id}")
        else:
            print("✗ Failed to create session")
            return False

        
        session = session_manager.get_current_session()
        if session and session.state == SessionState.ACTIVE:
            print("✓ Session is in ACTIVE state")
        else:
            print("✗ Session not in expected state")
            return False

        
        print("Starting recording...")
        success = await session_manager.start_recording()
        if success:
            print("✓ Recording started successfully")
        else:
            print("✗ Failed to start recording")
            return False

        
        if session_manager.is_recording():
            print("✓ Session is in RECORDING state")
        else:
            print("✗ Session not in recording state")
            return False

        
        await asyncio.sleep(1)

        
        print("Stopping recording...")
        success = await session_manager.stop_recording()
        if success:
            print("✓ Recording stopped successfully")
        else:
            print("✗ Failed to stop recording")
            return False

        
        print("Finalizing session...")
        success = session_manager.finalize_session()
        if success:
            print("✓ Session finalized successfully")
        else:
            print("✗ Failed to finalize session")
            return False

        
        final_session = session_manager.get_current_session()
        if final_session and final_session.state == SessionState.COMPLETE:
            print("✓ Session is in COMPLETE state")
            print(f"  Duration: {final_session.duration_seconds:.1f} seconds")
            print(f"  Devices: {len(final_session.participating_devices)}")
        else:
            print("✗ Session not in expected final state")
            return False

        
        session_path = session_dir / session_id
        metadata_file = session_path / "session_metadata.json"

        if session_path.exists() and metadata_file.exists():
            print("✓ Session directory and metadata file created")
        else:
            print("✗ Session files not created properly")
            return False

        
        session_manager.reset_for_next_session()
        if not session_manager.is_session_active():
            print("✓ Session manager reset successfully")
        else:
            print("✗ Session manager not reset properly")
            return False

        
        await device_manager.stop()

        return True


def test_discovery_service():
    
    print("\n=== Testing Discovery Service ===")

    try:
        from ircamera_pc.network.discovery import NetworkDiscoveryService

        discovery = NetworkDiscoveryService()
        print("✓ Discovery service created")

        
        test_attributes = {"capabilities": "rgb_camera,gsr_sensor", "device_type": "ANDROID_NODE"}

        
        mock_device = DiscoveredDevice(
            service_name="TestDevice",
            service_type="_ircamera._tcp.local.",
            ip_address="192.168.1.100",
            port=8080,
            device_type=DeviceType.ANDROID_NODE,
            attributes=test_attributes
        )

        print(f"✓ Mock device created: {mock_device.service_name}")
        print(f"  Type: {mock_device.device_type.name}")
        print(f"  Address: {mock_device.ip_address}:{mock_device.port}")

        return True

    except Exception as e:
        print(f"✗ Discovery service test failed: {e}")
        return False


async def run_all_tests():
    
    print("IRCamera PC Controller Hub - MVP Test Suite")
    print("=" * 50)

    tests = [
        ("Discovery Service", test_discovery_service),
        ("Device Manager", test_device_manager),
        ("Session Manager", test_session_manager),
    ]

    results = []

    for test_name, test_func in tests:
        print(f"\nRunning {test_name} tests...")
        try:
            if asyncio.iscoroutinefunction(test_func):
                result = await test_func()
            else:
                result = test_func()

            results.append((test_name, result))

            if result:
                print(f"✅ {test_name} tests PASSED")
            else:
                print(f"❌ {test_name} tests FAILED")

        except Exception as e:
            print(f"❌ {test_name} tests FAILED with exception: {e}")
            results.append((test_name, False))

    
    print("\n" + "=" * 50)
    print("TEST SUMMARY")
    print("=" * 50)

    passed = 0
    total = len(results)

    for test_name, result in results:
        status = "PASSED" if result else "FAILED"
        icon = "✅" if result else "❌"
        print(f"{icon} {test_name}: {status}")

        if result:
            passed += 1

    print(f"\nOverall: {passed}/{total} tests passed")

    if passed == total:
        print("🎉 All tests passed! MVP implementation is working correctly.")
        return True
    else:
        print("⚠️  Some tests failed. Please review the implementation.")
        return False


if __name__ == "__main__":
    
    logger.remove()
    logger.add(sys.stderr, level="WARNING")  

    
    try:
        success = asyncio.run(run_all_tests())
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\nTests interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\nTest suite failed with error: {e}")
        sys.exit(1)
