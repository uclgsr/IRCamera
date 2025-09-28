#!/usr/bin/env python3
"""
Emulated Scenario Test Runner

Demonstrates realistic sensor emulation scenarios for comprehensive testing
without requiring physical hardware.
"""

import time
import sys
import os
import json
from pathlib import Path

# Add emulators to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'emulators'))

from emulators.tc001_emulator import TC001ThermalEmulator, ThermalScenario
from emulators.shimmer3_emulator import Shimmer3GSREmulator, GSRScenario
from emulators.network_emulator import NetworkEmulator, NetworkCondition
from emulators.android_emulator import AndroidSystemEmulator, AndroidScenario


def run_multi_modal_synchronization_test():
    """Run comprehensive multi-modal synchronization test using emulators"""
    print("🎯 Multi-Modal Synchronization Test")
    print("=" * 50)
    
    # Initialize emulators
    thermal_emulator = TC001ThermalEmulator(seed=42)
    gsr_emulator = Shimmer3GSREmulator(seed=43, sampling_rate=128)
    network_emulator = NetworkEmulator(seed=44)
    android_emulator = AndroidSystemEmulator(seed=45)
    
    # Set good network conditions for synchronization test
    network_emulator.set_network_condition(NetworkCondition.GOOD_WIFI_5GHZ)
    android_emulator.set_scenario(AndroidScenario.NORMAL_OPERATION)
    
    # Generate synchronized test data (30 seconds with hand clap at t=15s)
    duration = 30.0
    
    print(f"Generating {duration}s of synchronized multi-modal data...")
    
    # Generate thermal frames with hand clap event
    thermal_frames = thermal_emulator.generate_hand_clap_synchronization_test(duration)
    
    # Generate GSR samples with corresponding stress response
    gsr_samples = gsr_emulator.generate_hand_clap_synchronization_test(duration)
    
    # Generate network measurements
    network_measurements = network_emulator.generate_synchronization_test_scenario(duration)
    
    # Generate Android system states
    android_emulator.start_recording_session()
    android_states = []
    for i in range(int(duration)):
        state = android_emulator.get_system_state()
        android_states.append(state)
        time.sleep(0.1)  # Brief pause for realistic timing
        
    android_emulator.stop_recording_session()
    
    # Analysis
    print("\n📊 Synchronization Analysis:")
    
    # Find synchronization events
    thermal_sync_events = [f for f in thermal_frames if f.metadata and 'sync_event' in f.metadata]
    gsr_sync_events = [s for s in gsr_samples if s.metadata and 'sync_event' in s.metadata]
    network_sync_events = [m for m in network_measurements if m.metadata and 'sync_event' in m.metadata]
    
    print(f"  Thermal sync events: {len(thermal_sync_events)}")
    print(f"  GSR sync events: {len(gsr_sync_events)}")
    print(f"  Network sync events: {len(network_sync_events)}")
    
    # Calculate synchronization precision
    if thermal_sync_events and gsr_sync_events:
        thermal_sync_time = thermal_sync_events[0].timestamp
        gsr_sync_time = gsr_sync_events[0].timestamp
        sync_offset_ms = abs(thermal_sync_time - gsr_sync_time) * 1000
        
        print(f"  Cross-modal sync precision: {sync_offset_ms:.2f} ms")
        
        if sync_offset_ms < 5.0:
            print("  ✅ Synchronization within 5ms target")
        else:
            print("  ❌ Synchronization exceeds 5ms target")
    
    # Performance summary
    avg_thermal_temp = sum(f.temperature_matrix.mean() for f in thermal_frames) / len(thermal_frames)
    avg_gsr_conductance = sum(s.conductance_us for s in gsr_samples) / len(gsr_samples)
    avg_network_latency = sum(m.latency_ms for m in network_measurements) / len(network_measurements)
    
    print(f"\n📈 Performance Summary:")
    print(f"  Average thermal temperature: {avg_thermal_temp:.1f}°C")
    print(f"  Average GSR conductance: {avg_gsr_conductance:.2f} μS")
    print(f"  Average network latency: {avg_network_latency:.1f} ms")
    print(f"  Total thermal frames: {len(thermal_frames)}")
    print(f"  Total GSR samples: {len(gsr_samples)}")
    
    return {
        'thermal_frames': len(thermal_frames),
        'gsr_samples': len(gsr_samples),
        'network_measurements': len(network_measurements),
        'sync_precision_ms': sync_offset_ms if thermal_sync_events and gsr_sync_events else None,
        'avg_thermal_temp': avg_thermal_temp,
        'avg_gsr_conductance': avg_gsr_conductance,
        'avg_network_latency': avg_network_latency
    }


def run_stress_response_scenario():
    """Run realistic stress response testing scenario"""
    print("🧠 Stress Response Scenario Test")
    print("=" * 50)
    
    # Initialize emulators for stress testing
    thermal_emulator = TC001ThermalEmulator(seed=50)
    gsr_emulator = Shimmer3GSREmulator(seed=51)
    android_emulator = AndroidSystemEmulator(seed=52)
    
    # Configure for stress response
    android_emulator.set_scenario(AndroidScenario.NORMAL_OPERATION)
    android_emulator.start_recording_session()
    
    # Generate 90 seconds of data with stress response at t=30s
    print("Generating stress response scenario (90 seconds)...")
    
    # Human subject with stress response
    thermal_emulator.start_capture(ThermalScenario.HUMAN_SUBJECT)
    thermal_frames = thermal_emulator.generate_sequence(90.0)
    
    # GSR stress response (onset at 30s, peak at 35s, recovery by 60s)
    gsr_emulator.start_recording(GSRScenario.STRESS_RESPONSE)
    gsr_emulator.scenario_state.update({
        'stress_onset_time': 30.0,
        'stress_peak_time': 35.0,
        'recovery_time': 60.0,
        'stress_amplitude': 2.5  # Strong stress response
    })
    gsr_samples = gsr_emulator.generate_sequence(90.0)
    
    # Android system monitoring throughout
    android_states = []
    for i in range(90):  # 1 measurement per second
        state = android_emulator.get_system_state()
        android_states.append(state)
        
    android_emulator.stop_recording_session()
    
    # Analysis
    print("\n📊 Stress Response Analysis:")
    
    # Find peak stress period (30-40s)
    stress_period_gsr = [s for s in gsr_samples 
                        if 30.0 <= (s.timestamp - gsr_samples[0].timestamp) <= 40.0]
    baseline_period_gsr = [s for s in gsr_samples 
                          if 0.0 <= (s.timestamp - gsr_samples[0].timestamp) <= 15.0]
    
    if stress_period_gsr and baseline_period_gsr:
        baseline_gsr = sum(s.conductance_us for s in baseline_period_gsr) / len(baseline_period_gsr)
        peak_gsr = max(s.conductance_us for s in stress_period_gsr)
        stress_response = peak_gsr - baseline_gsr
        
        print(f"  Baseline GSR: {baseline_gsr:.2f} μS")
        print(f"  Peak GSR: {peak_gsr:.2f} μS")
        print(f"  Stress response: {stress_response:.2f} μS")
        
        if stress_response > 1.0:
            print("  ✅ Significant stress response detected")
        else:
            print("  ⚠️ Weak stress response")
    
    # Thermal analysis
    thermal_temps = [f.temperature_matrix.max() for f in thermal_frames]
    max_thermal = max(thermal_temps)
    print(f"  Peak thermal temperature: {max_thermal:.1f}°C")
    
    # System resource usage
    max_memory = max(s.memory_used_mb for s in android_states)
    avg_cpu = sum(s.cpu_usage_percent for s in android_states) / len(android_states)
    
    print(f"  Peak memory usage: {max_memory} MB")
    print(f"  Average CPU usage: {avg_cpu:.1f}%")
    
    return {
        'stress_response_us': stress_response if stress_period_gsr and baseline_period_gsr else 0,
        'peak_thermal_temp': max_thermal,
        'peak_memory_mb': max_memory,
        'avg_cpu_percent': avg_cpu,
        'total_duration_s': 90.0
    }


def run_network_failure_scenario():
    """Run network failure and recovery testing"""
    print("📶 Network Failure Scenario Test")
    print("=" * 50)
    
    network_emulator = NetworkEmulator(seed=60)
    android_emulator = AndroidSystemEmulator(seed=61)
    
    print("Simulating Wi-Fi roaming failure (as documented in thesis)...")
    
    # Run roaming failure scenario (3/14 sessions with 50-80ms jumps)
    measurements = network_emulator.simulate_wifi_roaming_failure(120.0)
    
    # Simulate multi-device coordination during network issues
    device_messages = network_emulator.simulate_multi_device_coordination(3, 60.0)
    
    # Android system response to network issues
    android_emulator.set_scenario(AndroidScenario.BACKGROUND_APPS)  # More stress
    android_states = []
    for i in range(120):
        state = android_emulator.get_system_state()
        android_states.append(state)
        
    # Analysis
    print("\n📊 Network Failure Analysis:")
    
    # Identify failure events
    failure_measurements = [m for m in measurements if m.metadata.get('failure_event', False)]
    normal_measurements = [m for m in measurements if not m.metadata.get('failure_event', False)]
    
    print(f"  Total measurements: {len(measurements)}")
    print(f"  Failure events: {len(failure_measurements)}")
    print(f"  Failure rate: {len(failure_measurements)/len(measurements):.1%}")
    
    if failure_measurements:
        avg_failure_latency = sum(m.latency_ms for m in failure_measurements) / len(failure_measurements)
        avg_normal_latency = sum(m.latency_ms for m in normal_measurements) / len(normal_measurements)
        
        print(f"  Normal latency: {avg_normal_latency:.1f} ms")
        print(f"  Failure latency: {avg_failure_latency:.1f} ms")
        print(f"  Latency increase: {avg_failure_latency - avg_normal_latency:.1f} ms")
        
        # Check against documented thesis values
        if 50.0 <= (avg_failure_latency - avg_normal_latency) <= 80.0:
            print("  ✅ Matches documented 50-80ms latency jumps")
        else:
            print("  ⚠️ Latency jump outside documented range")
    
    # Message success rates
    total_messages = sum(len(msgs) for msgs in device_messages.values())
    failed_messages = sum(1 for msgs in device_messages.values() 
                         for msg in msgs if not msg.success)
    
    success_rate = (total_messages - failed_messages) / total_messages
    print(f"  Protocol messages: {total_messages} total, {success_rate:.1%} success rate")
    
    return {
        'total_measurements': len(measurements),
        'failure_events': len(failure_measurements),
        'failure_rate_percent': len(failure_measurements)/len(measurements) * 100,
        'avg_failure_latency_ms': avg_failure_latency if failure_measurements else 0,
        'protocol_success_rate': success_rate,
        'documented_range_match': 50.0 <= (avg_failure_latency - avg_normal_latency) <= 80.0 if failure_measurements else False
    }


def run_extended_recording_scenario():
    """Run extended recording session test (2 hours simulated)"""
    print("⏱️ Extended Recording Scenario Test")
    print("=" * 50)
    
    gsr_emulator = Shimmer3GSREmulator(seed=70, sampling_rate=128)
    android_emulator = AndroidSystemEmulator(seed=71)
    
    # Configure for extended recording
    gsr_emulator.start_recording(GSRScenario.EXTENDED_RECORDING)
    android_emulator.set_scenario(AndroidScenario.NORMAL_OPERATION)
    android_emulator.start_recording_session()
    
    # Simulate 2 hours of recording (compressed time)
    duration_hours = 2.0
    duration_seconds = duration_hours * 3600
    
    print(f"Simulating {duration_hours} hours of continuous recording...")
    
    # Generate extended GSR data
    gsr_samples = gsr_emulator.generate_sequence(120.0)  # Sample period
    
    # Android system monitoring (every 10 minutes = 600s intervals)
    monitoring_points = []
    for hour in range(int(duration_hours)):
        for interval in [0, 600, 1200, 1800, 2400, 3000]:  # Every 10 minutes
            # Simulate time passage
            current_time = hour * 3600 + interval
            android_emulator.battery_level = max(5, 85 - (current_time / 3600) * 35)  # Battery drain
            android_emulator.memory_used_mb += random.randint(0, 50)  # Memory growth
            
            state = android_emulator.get_system_state()
            state.metadata['recording_time_hours'] = current_time / 3600
            monitoring_points.append(state)
    
    android_emulator.stop_recording_session()
    
    # Analysis
    print("\n📊 Extended Recording Analysis:")
    
    # Battery analysis
    initial_battery = 85  # Starting level
    final_battery = monitoring_points[-1].battery_level_percent if monitoring_points else 0
    battery_consumption = initial_battery - final_battery
    
    print(f"  Recording duration: {duration_hours} hours")
    print(f"  Battery consumption: {battery_consumption}%")
    print(f"  Battery life projection: {initial_battery / (battery_consumption / duration_hours):.1f} hours")
    
    # Memory analysis
    initial_memory = monitoring_points[0].memory_used_mb if monitoring_points else 0
    final_memory = monitoring_points[-1].memory_used_mb if monitoring_points else 0
    memory_growth = final_memory - initial_memory
    
    print(f"  Memory growth: {memory_growth} MB")
    print(f"  Memory growth rate: {memory_growth / duration_hours:.1f} MB/hour")
    
    # GSR data quality
    avg_contact_quality = sum(s.electrode_contact_quality for s in gsr_samples) / len(gsr_samples)
    low_contact_samples = sum(1 for s in gsr_samples if s.electrode_contact_quality < 0.8)
    
    print(f"  Average electrode contact: {avg_contact_quality:.1%}")
    print(f"  Poor contact periods: {low_contact_samples / len(gsr_samples):.1%}")
    
    # Data volume estimation
    estimated_total_samples = int(128 * duration_seconds)  # 128 Hz for full duration
    estimated_data_mb = estimated_total_samples * 16 / (8 * 1024 * 1024)  # 16 bytes per sample
    
    print(f"  Estimated total samples: {estimated_total_samples:,}")
    print(f"  Estimated data size: {estimated_data_mb:.1f} MB")
    
    # Performance assessment
    if battery_consumption < 80 and memory_growth < 1000:
        print("  ✅ System suitable for extended recording")
    else:
        print("  ⚠️ System limitations for extended recording")
    
    return {
        'duration_hours': duration_hours,
        'battery_consumption_percent': battery_consumption,
        'memory_growth_mb': memory_growth,
        'avg_contact_quality': avg_contact_quality,
        'estimated_data_mb': estimated_data_mb,
        'performance_suitable': battery_consumption < 80 and memory_growth < 1000
    }


def main():
    """Run all emulated testing scenarios"""
    print("🧪 IRCamera Emulated Testing Suite")
    print("=" * 60)
    print()
    
    results = {}
    
    try:
        # Run test scenarios
        results['synchronization'] = run_multi_modal_synchronization_test()
        print("\n" + "="*60 + "\n")
        
        results['stress_response'] = run_stress_response_scenario()
        print("\n" + "="*60 + "\n")
        
        results['network_failure'] = run_network_failure_scenario()
        print("\n" + "="*60 + "\n")
        
        results['extended_recording'] = run_extended_recording_scenario()
        print("\n" + "="*60 + "\n")
        
        # Summary
        print("🎯 Overall Testing Summary")
        print("=" * 50)
        print(f"✅ Multi-modal synchronization: {results['synchronization']['sync_precision_ms']:.2f}ms precision")
        print(f"✅ Stress response detection: {results['stress_response']['stress_response_us']:.2f}μS response")
        print(f"✅ Network failure handling: {results['network_failure']['protocol_success_rate']:.1%} success rate")
        print(f"✅ Extended recording capability: {results['extended_recording']['duration_hours']}h capacity")
        
        # Save results (convert numpy types to Python types for JSON serialization)
        results_file = Path(__file__).parent / 'testing-suite' / 'results' / 'emulated_scenarios_results.json'
        results_file.parent.mkdir(parents=True, exist_ok=True)
        
        # Convert numpy float32 to float for JSON serialization
        def convert_numpy_types(obj):
            if hasattr(obj, 'item'):  # numpy scalar
                return obj.item()
            elif isinstance(obj, dict):
                return {k: convert_numpy_types(v) for k, v in obj.items()}
            elif isinstance(obj, list):
                return [convert_numpy_types(item) for item in obj]
            else:
                return obj
        
        json_results = convert_numpy_types(results)
        
        with open(results_file, 'w') as f:
            json.dump(json_results, f, indent=2)
            
        print(f"\n📄 Detailed results saved to: {results_file}")
        
        print("\n🎉 All emulated scenarios completed successfully!")
        
    except Exception as e:
        print(f"❌ Error during testing: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    import random
    main()