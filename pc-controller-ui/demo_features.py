#!/usr/bin/env python3
"""
Demo script showcasing Enhanced PC Controller features
This can be run in headless environments for demonstration
"""

import sys
import time
import json
import threading
from pathlib import Path

# Add C++ backend to path
sys.path.insert(0, str(Path(__file__).parent.parent / 'pc-controller/enhanced_native_backend'))

def demo_cpp_backend():
    """Demonstrate C++ backend capabilities"""
    print("🔧 C++ Backend Demo")
    print("=" * 40)
    
    try:
        import enhanced_native_backend as backend
        
        print(f"✅ Backend Version: {backend.__version__}")
        print(f"✅ OpenCV Available: {backend.opencv_available}")
        
        # Demo GSR processing
        print("\n📊 GSR Data Processing:")
        shimmer = backend.EnhancedShimmer()
        print(f"  Device ID: {shimmer.get_device_id()}")
        print(f"  Firmware: {shimmer.get_firmware_version()}")
        print(f"  Sampling Rate: {shimmer.get_sampling_rate()} Hz")
        
        # Demo data processor
        print("\n📈 Data Processor:")
        processor = backend.DataProcessor()
        
        # Add sample data
        current_time = time.time()
        for i in range(50):
            value = 500 + 50 * (0.5 + 0.5 * (i % 10) / 10)  # Simulated GSR
            processor.add_sample(current_time + i * 0.1, value)
        
        print(f"  Buffer Size: {processor.get_buffer_size()} samples")
        print(f"  Statistics: mean={processor.get_mean():.1f}, std={processor.get_std_deviation():.1f}")
        
        # Demo signal processing
        print("\n🎛️  Signal Processing:")
        test_signal = [500 + 20 * (i % 5) + 5 * (i % 2) for i in range(20)]
        
        filtered = backend.processing.apply_lowpass_filter(test_signal, 1.0, 10.0)
        print(f"  Original signal length: {len(test_signal)}")
        print(f"  Filtered signal length: {len(filtered)}")
        print(f"  Original mean: {backend.processing.calculate_mean(test_signal):.1f}")
        print(f"  Filtered mean: {backend.processing.calculate_mean(filtered):.1f}")
        
        # Demo artifact detection
        artifacts = backend.processing.detect_motion_artifacts(test_signal, 2.0)
        print(f"  Motion artifacts detected: {sum(artifacts)}/{len(artifacts)}")
        
        signal_quality = backend.processing.calculate_signal_quality(test_signal)
        print(f"  Signal quality: {signal_quality:.2f}")
        
    except ImportError:
        print("⚠️  C++ backend not available - run installation script first")

def demo_thermal_processing():
    """Demonstrate thermal processing capabilities"""
    print("\n🌡️  Thermal Processing Demo")
    print("=" * 40)
    
    try:
        import enhanced_native_backend as backend
        
        thermal_proc = backend.ThermalProcessor()
        
        # Generate simulated thermal data
        import random
        raw_temps = [random.randint(800, 1200) for _ in range(64)]  # 8x8 thermal array
        
        # Convert to Celsius
        celsius_temps = thermal_proc.raw_to_celsius(raw_temps, 0.95)
        
        print(f"  Raw temperature range: {min(raw_temps)}-{max(raw_temps)}")
        print(f"  Celsius range: {min(celsius_temps):.1f}°C - {max(celsius_temps):.1f}°C")
        print(f"  Average temperature: {thermal_proc.calculate_average_temperature(celsius_temps):.1f}°C")
        
        # Find hotspot
        hotspot = thermal_proc.find_hotspot_location(celsius_temps, 8, 8)
        print(f"  Hotspot location: ({hotspot[0]}, {hotspot[1]})")
        
        # Apply colormap
        colormap_data = thermal_proc.apply_colormap(celsius_temps, "jet")
        print(f"  Colormap data size: {len(colormap_data)} bytes (RGB)")
        
    except ImportError:
        print("⚠️  C++ backend not available")

def demo_network_protocol():
    """Demonstrate network protocol handling"""
    print("\n🌐 Network Protocol Demo")
    print("=" * 40)
    
    # Simulate device messages
    messages = [
        {
            "type": "HELLO",
            "device_id": "demo_device_1",
            "device_name": "Demo Android Device",
            "sensors": ["GSR", "RGB", "Thermal"],
            "firmware_version": "1.0.0"
        },
        {
            "type": "telemetry_gsr",
            "value": 523.4,
            "timestamp": time.time()
        },
        {
            "type": "thermal_frame",
            "frame_number": 1,
            "min_temp_c": 20.5,
            "max_temp_c": 35.2,
            "avg_temp_c": 27.8,
            "center_temp_c": 28.1
        },
        {
            "type": "status_update",
            "status": "Recording",
            "sensors": {
                "GSR": {"status": "Connected", "message": ""},
                "RGB": {"status": "Connected", "message": ""},
                "Thermal": {"status": "Connected", "message": ""}
            }
        }
    ]
    
    try:
        import enhanced_native_backend as backend
        msg_proc = backend.MessageProcessor()
        
        for msg in messages:
            json_str = json.dumps(msg)
            print(f"  📤 {msg['type']}: {len(json_str)} bytes")
            
            # Validate message format
            if msg_proc.validate_message_format(json_str):
                print(f"     ✅ Valid format")
            else:
                print(f"     ❌ Invalid format")
        
        # Demo message creation
        hello = msg_proc.create_hello_message("demo_device", ["GSR", "RGB"])
        print(f"  📝 Generated HELLO: {len(hello)} bytes")
        
        ack = msg_proc.create_ack_message("start_recording")
        print(f"  📝 Generated ACK: {len(ack)} bytes")
        
    except ImportError:
        # Fallback to basic JSON handling
        for msg in messages:
            json_str = json.dumps(msg)
            parsed = json.loads(json_str)
            print(f"  📤 {msg['type']}: {len(json_str)} bytes ✅")

def demo_real_time_simulation():
    """Simulate real-time data processing"""
    print("\n⏱️  Real-Time Processing Demo")
    print("=" * 40)
    
    print("  Simulating 5 seconds of real-time GSR data...")
    
    try:
        import enhanced_native_backend as backend
        processor = backend.DataProcessor()
        
        start_time = time.time()
        sample_count = 0
        
        # Simulate 128 Hz sampling for 5 seconds
        for i in range(640):  # 128 * 5
            current_time = start_time + i / 128.0
            
            # Generate realistic GSR signal
            baseline = 500
            slow_wave = 50 * (i % 128) / 128.0
            noise = (i % 7) * 2 - 7
            gsr_value = baseline + slow_wave + noise
            
            processor.add_sample(current_time, gsr_value)
            sample_count += 1
            
            # Update statistics every 32 samples (4 times per second)
            if i % 32 == 0:
                processor.update_statistics(gsr_value)
        
        print(f"  ✅ Processed {sample_count} samples")
        print(f"  📊 Buffer size: {processor.get_buffer_size()}")
        print(f"  📈 Statistics: mean={processor.get_mean():.1f}, std={processor.get_std_deviation():.1f}")
        
        # Get recent data
        recent = processor.get_recent_samples(2.0)  # Last 2 seconds
        print(f"  🕒 Recent samples (2s): {len(recent)}")
        
    except ImportError:
        # Fallback simulation
        for i in range(50):
            gsr_value = 500 + 50 * (i % 10) / 10
            print(f"  📊 Sample {i+1}: GSR = {gsr_value:.1f} μS", end='\r')
            time.sleep(0.1)
        print()

def main():
    """Run all demos"""
    print("🚀 Enhanced PC Controller Feature Demo")
    print("=" * 50)
    print("This demo showcases the enhanced features without requiring")
    print("a GUI or connected devices.")
    print()
    
    # Run demos
    demo_cpp_backend()
    demo_thermal_processing()
    demo_network_protocol()
    demo_real_time_simulation()
    
    print("\n🎉 Demo Complete!")
    print("\nTo run the full enhanced controller:")
    print("  cd src/")
    print("  python3 enhanced_pc_controller.py")
    print("\nFor more information, see README_ENHANCED.md")

if __name__ == "__main__":
    main()
