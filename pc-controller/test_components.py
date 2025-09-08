#!/usr/bin/env python3
"""
Test script for the new PC Controller components

This script tests the newly implemented components:
- Native backend functionality
- Real-time plotting widgets
- Data aggregation engine
- Enhanced GUI components
"""

import sys
import time
import asyncio
from pathlib import Path

# Add src directory to path
sys.path.insert(0, str(Path(__file__).parent / "src"))

def test_imports():
    """Test that all new components can be imported."""
    print("Testing imports...")
    
    try:
        # Test PyQtGraph plotting widgets
        from ircamera_pc.gui.plotting_widgets import (
            GSRPlotWidget, VideoPreviewWidget, MultiModalDashboard, DataAggregationWidget
        )
        print("✓ Plotting widgets imported successfully")
        
        # Test enhanced GUI widgets
        from ircamera_pc.gui.widgets import (
            DeviceListWidget, SessionControlWidget, StatusDisplayWidget,
            SystemIntegrationWidget, BluetoothControlWidget, WiFiControlWidget
        )
        print("✓ Enhanced GUI widgets imported successfully")
        
        # Test data aggregation engine
        from ircamera_pc.data import (
            DataAggregationEngine, DataStream, SyncEvent, AggregationStats,
            calculate_temporal_alignment, validate_data_synchronization
        )
        print("✓ Data aggregation engine imported successfully")
        
        return True
        
    except Exception as e:
        print(f"✗ Import failed: {e}")
        return False


def test_data_aggregation():
    """Test the data aggregation engine."""
    print("\nTesting data aggregation engine...")
    
    try:
        from ircamera_pc.data import DataAggregationEngine, DataStream, SyncEvent
        
        # Create test session directory
        test_dir = Path("/tmp/test_session")
        test_dir.mkdir(exist_ok=True)
        
        # Initialize aggregation engine
        engine = DataAggregationEngine(test_dir)
        engine.start()
        
        # Add test streams
        gsr_stream_id = engine.add_stream("device_1", "gsr", 128.0)
        video_stream_id = engine.add_stream("device_1", "rgb_video", 30.0)
        
        print(f"✓ Added streams: {gsr_stream_id}, {video_stream_id}")
        
        # Add test data
        timestamp_ns = time.time_ns()
        
        # Simulate GSR data
        class MockGSRData:
            def __init__(self):
                self.raw_gsr_value = 2048  # 12-bit value
                self.gsr_microsiemens = 10.5
                self.raw_ppg_value = 1500
                
        gsr_data = MockGSRData()
        engine.add_data(gsr_stream_id, timestamp_ns, gsr_data)
        
        # Simulate video frame data
        video_data = {"frame_number": 1, "width": 1920, "height": 1080}
        engine.add_data(video_stream_id, timestamp_ns, video_data)
        
        # Add sync event
        engine.add_sync_event("flash", "device_1", timestamp_ns)
        
        # Wait a bit for processing
        time.sleep(0.1)
        
        # Get statistics
        stats = engine.get_statistics()
        print(f"✓ Statistics: {stats.total_devices} devices, {stats.active_streams} streams")
        
        # Clean up
        engine.stop()
        
        print("✓ Data aggregation engine test passed")
        return True
        
    except Exception as e:
        print(f"✗ Data aggregation test failed: {e}")
        return False


def test_plotting_widgets():
    """Test the plotting widgets (without actually displaying)."""
    print("\nTesting plotting widgets...")
    
    try:
        from PyQt6.QtWidgets import QApplication
        from ircamera_pc.gui.plotting_widgets import GSRPlotWidget, MultiModalDashboard
        
        # Create minimal QApplication for testing
        app = QApplication.instance()
        if app is None:
            app = QApplication([])
        
        # Test GSR plot widget
        gsr_plot = GSRPlotWidget()
        gsr_plot.add_device("test_device", "cyan")
        gsr_plot.add_gsr_data("test_device", time.time_ns(), 15.5)
        print("✓ GSR plot widget created and tested")
        
        # Test multi-modal dashboard
        dashboard = MultiModalDashboard()
        dashboard.add_gsr_device("device_1")
        video_widget = dashboard.add_video_device("device_1", "RGB")
        print("✓ Multi-modal dashboard created and tested")
        
        print("✓ Plotting widgets test passed")
        return True
        
    except Exception as e:
        print(f"✗ Plotting widgets test failed: {e}")
        return False


def test_native_backend_structure():
    """Test that native backend structure is in place."""
    print("\nTesting native backend structure...")
    
    try:
        backend_dir = Path(__file__).parent / "native_backend"
        
        # Check directory structure
        if not backend_dir.exists():
            print("✗ Native backend directory missing")
            return False
            
        required_files = [
            "CMakeLists.txt",
            "include/native_shimmer.h",
            "include/native_webcam.h", 
            "src/native_shimmer.cpp",
            "src/native_webcam.cpp",
            "src/pybind_module.cpp"
        ]
        
        for file_path in required_files:
            full_path = backend_dir / file_path
            if not full_path.exists():
                print(f"✗ Missing file: {file_path}")
                return False
            print(f"✓ Found: {file_path}")
        
        print("✓ Native backend structure test passed")
        return True
        
    except Exception as e:
        print(f"✗ Native backend structure test failed: {e}")
        return False


def test_gui_widgets():
    """Test the enhanced GUI widgets."""
    print("\nTesting enhanced GUI widgets...")
    
    try:
        from PyQt6.QtWidgets import QApplication
        from ircamera_pc.gui.widgets import (
            DeviceListWidget, SessionControlWidget, StatusDisplayWidget
        )
        
        # Create minimal QApplication for testing
        app = QApplication.instance()
        if app is None:
            app = QApplication([])
        
        # Test device list widget
        device_list = DeviceListWidget()
        test_devices = [
            {"device_id": "device_1", "device_type": "GSR", "status": "connected"},
            {"device_id": "device_2", "device_type": "RGB", "status": "recording"}
        ]
        device_list.update_devices(test_devices)
        print("✓ Device list widget created and tested")
        
        # Test session control widget
        session_control = SessionControlWidget()
        print("✓ Session control widget created")
        
        # Test status display widget
        status_display = StatusDisplayWidget()
        test_stats = {"synchronization_rate": 0.95, "max_offset_ms": 2.5, "total_devices": 2}
        status_display.update_time_sync_stats(test_stats)
        print("✓ Status display widget created and tested")
        
        print("✓ Enhanced GUI widgets test passed")
        return True
        
    except Exception as e:
        print(f"✗ Enhanced GUI widgets test failed: {e}")
        return False


def main():
    """Run all tests."""
    print("=== PC Controller Components Test Suite ===\n")
    
    test_results = []
    
    # Run all tests
    test_results.append(("Imports", test_imports()))
    test_results.append(("Native Backend Structure", test_native_backend_structure()))
    test_results.append(("Data Aggregation", test_data_aggregation()))
    test_results.append(("Plotting Widgets", test_plotting_widgets()))
    test_results.append(("GUI Widgets", test_gui_widgets()))
    
    # Print summary
    print("\n=== Test Results Summary ===")
    passed = 0
    total = len(test_results)
    
    for test_name, result in test_results:
        status = "PASS" if result else "FAIL"
        print(f"{test_name:.<30} {status}")
        if result:
            passed += 1
    
    print(f"\nPassed: {passed}/{total}")
    
    if passed == total:
        print("🎉 All tests passed! PC Controller components are working correctly.")
        return 0
    else:
        print("❌ Some tests failed. Check the output above for details.")
        return 1


if __name__ == "__main__":
    sys.exit(main())