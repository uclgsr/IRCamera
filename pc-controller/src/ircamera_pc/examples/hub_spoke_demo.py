#!/usr/bin/env python3
"""
Hub-Spoke Architecture Integration Demo

Demonstrates the enhanced PC Controller (Hub) integration with
Android Sensor Nodes (Spokes) including NTP-like time synchronization.
"""

import asyncio
import json
import time
from datetime import datetime
from typing import Any, Dict

from ..core.hub_coordinator import HubCoordinator, SyncMarkerType
from ..core.config import config

# Configure logging
import logging
logging.basicConfig(level=logging.INFO)


async def main():
    """Main demo function."""
    print("=== Enhanced Hub-Spoke Architecture Demo ===")
    print("PC Controller (Hub) with Android Sensor Nodes (Spokes)")
    print("Features: NTP-like time sync, session coordination, device management\n")
    
    # Initialize hub coordinator
    hub = HubCoordinator()
    
    try:
        # Start the hub
        print("Starting Hub Coordinator...")
        if not await hub.start():
            print("Failed to start hub coordinator")
            return
        
        print("✓ Hub Coordinator started successfully")
        print(f"✓ Network server listening for Android devices")
        print(f"✓ Enhanced time synchronization service active")
        print(f"✓ Device discovery and management ready\n")
        
        # Monitor for device connections
        print("Waiting for Android devices to connect...")
        print("(Start Android app and ensure it's on same network)")
        
        # Demo session callbacks
        def on_session_started(session):
            print(f"📱 Session Started: '{session.session_name}' with {len(session.participating_devices)} devices")
        
        def on_device_sync_lost(data):
            print(f"⚠️  Device {data['device_id']} lost synchronization")
        
        def on_sync_marker_created(marker):
            print(f"🔄 Sync marker '{marker['marker_type']}' created at {marker['timestamp_ns']}")
        
        # Register callbacks
        hub.add_session_callback("session_started", on_session_started)
        hub.add_session_callback("device_sync_lost", on_device_sync_lost)
        hub.add_session_callback("sync_marker_created", on_sync_marker_created)
        
        # Main demo loop
        start_time = time.time()
        session_id = None
        
        while time.time() - start_time < 300:  # Run for 5 minutes
            await asyncio.sleep(5)
            
            # Check connected devices
            devices = hub.get_connected_devices()
            
            if devices:
                print(f"\n📱 Connected devices: {len(devices)}")
                
                # Show sync quality for each device
                for device_id, device_info in devices.items():
                    sync_stats = hub.get_device_sync_stats(device_id)
                    if sync_stats:
                        quality = sync_stats.get("sync_quality", "UNKNOWN")
                        offset = sync_stats.get("median_offset_ms", 0)
                        print(f"  • {device_id}: {quality} (offset: {offset:.1f}ms)")
                
                # Start demo session if we have synchronized devices and no active session
                if not session_id:
                    sync_ready = [
                        device_id for device_id in devices
                        if hub.is_device_synchronized(device_id)
                    ]
                    
                    if sync_ready:
                        print(f"\n🚀 Starting demo recording session with {len(sync_ready)} synchronized devices...")
                        session_id = await hub.start_recording_session(
                            session_name="Hub-Spoke Demo Session",
                            participant_id="demo_participant",
                            experiment_type="integration_demo",
                            notes="Demonstration of enhanced hub-spoke architecture"
                        )
                        
                        if session_id:
                            print(f"✓ Session started: {session_id}")
                            
                            # Demo sync markers
                            await asyncio.sleep(2)
                            await hub.create_sync_marker(session_id, SyncMarkerType.CUSTOM_EVENT, {
                                "event": "demo_marker_1",
                                "description": "First demo synchronization marker"
                            })
                            
                            await asyncio.sleep(3)
                            print("💡 Sending flash synchronization signal...")
                            await hub.send_flash_sync(session_id, duration_ms=200)
                            
                            await asyncio.sleep(5)
                            await hub.create_sync_marker(session_id, SyncMarkerType.CUSTOM_EVENT, {
                                "event": "demo_marker_2", 
                                "description": "Second demo synchronization marker"
                            })
                        else:
                            print("❌ Failed to start demo session")
                
                # Show session status
                if session_id:
                    session = hub.get_session(session_id)
                    if session:
                        duration = time.time() - session.start_time
                        print(f"📊 Active session: {duration:.1f}s elapsed, {len(session.sync_markers)} sync markers")
                        
                        # Stop session after 30 seconds
                        if duration > 30 and session.state.value == "recording":
                            print("\n🛑 Stopping demo session...")
                            if await hub.stop_recording_session(session_id):
                                print(f"✓ Session stopped successfully")
                                print(f"  Duration: {session.session_duration:.1f}s")
                                print(f"  Sync markers: {len(session.sync_markers)}")
                                print(f"  Devices: {list(session.participating_devices)}")
                            break
            else:
                print("⏳ No devices connected yet...")
        
        # Show final statistics
        print("\n=== Final Hub Statistics ===")
        sync_summary = hub.get_sync_quality_summary()
        print(f"Total devices handled: {sync_summary.get('total_devices', 0)}")
        print(f"Synchronized devices: {sync_summary.get('synchronized_devices', 0)}")
        print(f"Sync rate: {sync_summary.get('sync_rate', 0)*100:.1f}%")
        print(f"Overall median offset: {sync_summary.get('overall_median_offset_ms', 0):.1f}ms")
        print(f"Target compliance: {sync_summary.get('target_compliance', False)}")
        
        # Show quality distribution
        quality_dist = sync_summary.get('quality_distribution', {})
        if quality_dist:
            print("\nSync Quality Distribution:")
            for quality, count in quality_dist.items():
                if count > 0:
                    print(f"  {quality}: {count} devices")
        
    except KeyboardInterrupt:
        print("\n⏹️  Demo interrupted by user")
    except Exception as e:
        print(f"❌ Demo error: {e}")
    finally:
        # Clean shutdown
        print("\nShutting down hub coordinator...")
        await hub.stop()
        print("✓ Hub stopped successfully")


def run_demo():
    """Run the hub-spoke demo."""
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nDemo cancelled by user")
    except Exception as e:
        print(f"Demo failed: {e}")


if __name__ == "__main__":
    run_demo()