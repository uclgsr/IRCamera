#!/usr/bin/env python3
"""
Comprehensive Performance Benchmark Test

Test script for validating the complete performance evaluation system
including Android sensor performance and PC controller performance.

This script demonstrates the complete performance benchmarking workflow
as specified in issue #6.
"""

import asyncio
import json
import logging
import time
import sys
import os
from pathlib import Path

# Add the pc-controller directory to the path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from performance_evaluator import PCControllerPerformanceEvaluator

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class ComprehensiveBenchmarkTest:
    """
    Comprehensive performance benchmark test suite for the entire system
    """
    
    def __init__(self):
        self.pc_evaluator = PCControllerPerformanceEvaluator("benchmark_test_results")
        self.test_results = []
    
    async def run_complete_benchmark_suite(self):
        """Run the complete performance benchmark test suite"""
        logger.info("=== Starting Comprehensive Performance Benchmark Test Suite ===")
        
        # Test configurations
        test_scenarios = [
            {
                "name": "Quick Performance Validation",
                "description": "Fast validation of basic performance metrics",
                "duration": 10,
                "simulate_load": False
            },
            {
                "name": "Standard Multi-Modal Test",
                "description": "Standard test with multi-modal sensor simulation",
                "duration": 30,
                "simulate_load": True
            },
            {
                "name": "High Throughput Stress Test", 
                "description": "Stress test with high data throughput",
                "duration": 60,
                "simulate_load": True,
                "high_throughput": True
            },
            {
                "name": "Network Latency Assessment",
                "description": "Focused network performance evaluation",
                "duration": 15,
                "simulate_load": False,
                "network_focus": True
            }
        ]
        
        # Execute each test scenario
        for scenario in test_scenarios:
            try:
                logger.info(f"Starting scenario: {scenario['name']}")
                result = await self.run_single_test_scenario(scenario)
                self.test_results.append(result)
                
                logger.info(f"Scenario completed: {scenario['name']} - Score: {result.overall_score:.3f}")
                
                # Brief pause between tests
                await asyncio.sleep(2)
                
            except Exception as e:
                logger.error(f"Scenario failed: {scenario['name']}", exc_info=e)
        
        # Generate comprehensive report
        await self.generate_comprehensive_report()
        
        logger.info("=== Comprehensive Performance Benchmark Test Suite Completed ===")
        
        return self.test_results
    
    async def run_single_test_scenario(self, scenario):
        """Run a single test scenario"""
        logger.info(f"Executing: {scenario['description']}")
        
        # Start PC controller monitoring
        session_id = await self.pc_evaluator.start_performance_monitoring(
            f"test_{scenario['name'].lower().replace(' ', '_')}"
        )
        
        # Simulate sensor data and activities
        if scenario.get('simulate_load', False):
            await self.simulate_multi_modal_recording(
                duration=scenario['duration'],
                high_throughput=scenario.get('high_throughput', False),
                network_focus=scenario.get('network_focus', False)
            )
        else:
            await self.simulate_basic_activity(scenario['duration'])
        
        # Stop monitoring and get results
        benchmark_result = await self.pc_evaluator.stop_performance_monitoring()
        
        # Export results
        output_file = self.pc_evaluator.export_benchmark_results(benchmark_result)
        logger.info(f"Test results exported to: {output_file}")
        
        return benchmark_result
    
    async def simulate_multi_modal_recording(self, duration, high_throughput=False, network_focus=False):
        """Simulate multi-modal sensor recording with realistic data patterns"""
        logger.info(f"Simulating multi-modal recording for {duration} seconds")
        
        start_time = time.time()
        end_time = start_time + duration
        
        # Configuration based on test type
        if high_throughput:
            gsr_rate = 256  # Double rate
            rgb_rate = 60   # Double rate 
            thermal_rate = 20  # Double rate
            data_multiplier = 2.0
        else:
            gsr_rate = 128  # Standard rate
            rgb_rate = 30   # Standard rate
            thermal_rate = 10  # Standard rate
            data_multiplier = 1.0
        
        gsr_interval = 1.0 / gsr_rate
        rgb_interval = 1.0 / rgb_rate
        thermal_interval = 1.0 / thermal_rate
        
        last_gsr_time = 0
        last_rgb_time = 0
        last_thermal_time = 0
        
        sample_count = 0
        
        while time.time() < end_time:
            current_time = time.time()
            
            # Simulate GSR samples
            if current_time - last_gsr_time >= gsr_interval:
                # Record synchronization metric with slight drift
                self.pc_evaluator.record_synchronization_metric(
                    gsr_timestamp=current_time,
                    rgb_timestamp=current_time + 0.002,  # 2ms drift
                    thermal_timestamp=current_time + 0.001  # 1ms drift
                )
                last_gsr_time = current_time
                sample_count += 1
            
            # Simulate RGB frames
            if current_time - last_rgb_time >= rgb_interval:
                # Simulate network performance for RGB data
                latency = 15 + (10 * abs(hash(str(current_time)) % 100 - 50) / 50)  # 15-25ms variable
                throughput = (1000 + (500 * abs(hash(str(current_time + 1)) % 100 - 50) / 50)) * data_multiplier  # Variable throughput
                
                self.pc_evaluator.record_network_performance(
                    device_id="android_rgb_camera",
                    connection_latency_ms=latency,
                    throughput_kbps=throughput,
                    packet_loss_percent=0.1 if current_time % 30 < 1 else 0.0,  # Occasional packet loss
                    connection_stability=0.98,
                    data_integrity_score=0.99
                )
                last_rgb_time = current_time
            
            # Simulate thermal frames
            if current_time - last_thermal_time >= thermal_interval:
                # Record thermal performance metric
                self.pc_evaluator.record_performance_metric(
                    name="thermal_processing_time_ms",
                    value=3.5 + (2.0 * abs(hash(str(current_time + 2)) % 100 - 50) / 50),  # 3.5-5.5ms
                    unit="ms",
                    threshold=10.0,
                    category="thermal"
                )
                last_thermal_time = current_time
            
            # Add network focus testing if enabled
            if network_focus and sample_count % 10 == 0:
                # Simulate additional network devices
                for device_id in ["shimmer_gsr", "thermal_camera"]:
                    self.pc_evaluator.record_network_performance(
                        device_id=device_id,
                        connection_latency_ms=20 + (15 * abs(hash(device_id + str(current_time)) % 100 - 50) / 50),
                        throughput_kbps=200 + (100 * abs(hash(device_id + str(current_time + 1)) % 100 - 50) / 50),
                        connection_stability=0.95,
                        data_integrity_score=0.98
                    )
            
            # Record custom performance metrics periodically
            if sample_count % 100 == 0:
                self.pc_evaluator.record_performance_metric(
                    name="data_processing_rate_samples_per_sec",
                    value=sample_count / (current_time - start_time),
                    unit="samples/sec",
                    threshold=100.0,
                    category="processing"
                )
            
            # Small delay to prevent tight loop
            await asyncio.sleep(0.001)
    
    async def simulate_basic_activity(self, duration):
        """Simulate basic system activity for lighter testing"""
        logger.info(f"Simulating basic activity for {duration} seconds")
        
        start_time = time.time()
        end_time = start_time + duration
        
        while time.time() < end_time:
            current_time = time.time()
            
            # Simulate basic synchronization
            self.pc_evaluator.record_synchronization_metric(
                gsr_timestamp=current_time,
                rgb_timestamp=current_time + 0.001,  # 1ms drift
                thermal_timestamp=current_time + 0.0005  # 0.5ms drift
            )
            
            # Simulate basic network activity
            self.pc_evaluator.record_network_performance(
                device_id="test_device",
                connection_latency_ms=25.0,
                throughput_kbps=512.0,
                connection_stability=0.99,
                data_integrity_score=1.0
            )
            
            await asyncio.sleep(0.1)  # 10Hz sampling
    
    async def generate_comprehensive_report(self):
        """Generate comprehensive test suite report"""
        try:
            timestamp = time.strftime("%Y%m%d_%H%M%S")
            report_file = Path("benchmark_test_results") / f"comprehensive_benchmark_report_{timestamp}.txt"
            report_file.parent.mkdir(exist_ok=True)
            
            with open(report_file, 'w') as f:
                f.write("=" * 80 + "\n")
                f.write("COMPREHENSIVE PERFORMANCE BENCHMARK TEST REPORT\n")
                f.write("=" * 80 + "\n")
                f.write(f"Generated: {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write(f"Total Test Scenarios: {len(self.test_results)}\n\n")
                
                # Summary statistics
                if self.test_results:
                    passed_tests = sum(1 for r in self.test_results if r.success)
                    avg_score = sum(r.overall_score for r in self.test_results) / len(self.test_results)
                    success_rate = passed_tests / len(self.test_results) * 100
                    
                    f.write("SUMMARY\n")
                    f.write("-" * 40 + "\n")
                    f.write(f"Tests Passed: {passed_tests}/{len(self.test_results)}\n")
                    f.write(f"Success Rate: {success_rate:.1f}%\n")
                    f.write(f"Average Score: {avg_score:.3f}\n\n")
                
                # Individual test results
                f.write("INDIVIDUAL TEST RESULTS\n")
                f.write("-" * 40 + "\n")
                
                for result in self.test_results:
                    f.write(f"Test: {result.benchmark_id}\n")
                    f.write(f"Success: {'PASSED' if result.success else 'FAILED'}\n")
                    f.write(f"Score: {result.overall_score:.3f}\n")
                    f.write(f"Duration: {result.duration_seconds:.2f}s\n")
                    f.write(f"Summary: {result.summary}\n")
                    
                    if result.recommendations:
                        f.write("Recommendations:\n")
                        for i, rec in enumerate(result.recommendations, 1):
                            f.write(f"  {i}. {rec}\n")
                    f.write("\n")
                
                # Performance metrics analysis
                f.write("PERFORMANCE METRICS ANALYSIS\n")
                f.write("-" * 40 + "\n")
                
                all_sync_metrics = []
                all_resource_metrics = []
                all_network_metrics = []
                
                for result in self.test_results:
                    all_sync_metrics.extend(result.synchronization_metrics)
                    all_resource_metrics.extend(result.resource_metrics)
                    all_network_metrics.extend(result.network_metrics)
                
                if all_sync_metrics:
                    avg_drift = sum(m.max_drift_ms for m in all_sync_metrics) / len(all_sync_metrics)
                    avg_accuracy = sum(m.sync_accuracy_score for m in all_sync_metrics) / len(all_sync_metrics)
                    f.write(f"Synchronization - Avg Drift: {avg_drift:.2f}ms, Avg Accuracy: {avg_accuracy:.3f}\n")
                
                if all_resource_metrics:
                    avg_cpu = sum(m.cpu_percent for m in all_resource_metrics) / len(all_resource_metrics)
                    avg_memory = sum(m.memory_percent for m in all_resource_metrics) / len(all_resource_metrics)
                    f.write(f"Resources - Avg CPU: {avg_cpu:.1f}%, Avg Memory: {avg_memory:.1f}%\n")
                
                if all_network_metrics:
                    avg_latency = sum(m.connection_latency_ms for m in all_network_metrics) / len(all_network_metrics)
                    avg_throughput = sum(m.throughput_kbps for m in all_network_metrics) / len(all_network_metrics)
                    f.write(f"Network - Avg Latency: {avg_latency:.2f}ms, Avg Throughput: {avg_throughput:.1f} KB/s\n")
                
                f.write("\n")
                
                # Overall assessment
                f.write("OVERALL SYSTEM ASSESSMENT\n")
                f.write("-" * 40 + "\n")
                
                if self.test_results:
                    if success_rate >= 90:
                        f.write("🟢 EXCELLENT - System exceeds performance requirements\n")
                    elif success_rate >= 75:
                        f.write("🟡 GOOD - System meets performance requirements\n")
                    elif success_rate >= 50:
                        f.write("🟠 ACCEPTABLE - System performance needs improvement\n")
                    else:
                        f.write("🔴 POOR - System requires significant performance optimization\n")
                else:
                    f.write("❌ NO RESULTS - Unable to assess system performance\n")
                
                f.write("\nRecommendations for improvement:\n")
                all_recommendations = []
                for result in self.test_results:
                    all_recommendations.extend(result.recommendations)
                
                unique_recommendations = list(set(all_recommendations))
                for i, rec in enumerate(unique_recommendations, 1):
                    f.write(f"{i}. {rec}\n")
            
            logger.info(f"Comprehensive report generated: {report_file}")
            
        except Exception as e:
            logger.error(f"Error generating comprehensive report: {e}")


async def main():
    """Main function to run the comprehensive benchmark test"""
    print("Starting Comprehensive Performance Benchmark Test Suite...")
    print("This test validates all performance evaluation metrics specified in issue #6")
    print()
    
    test_suite = ComprehensiveBenchmarkTest()
    
    try:
        results = await test_suite.run_complete_benchmark_suite()
        
        print("\n" + "=" * 60)
        print("COMPREHENSIVE BENCHMARK TEST COMPLETED")
        print("=" * 60)
        
        if results:
            passed = sum(1 for r in results if r.success)
            total = len(results)
            avg_score = sum(r.overall_score for r in results) / total
            
            print(f"Tests Passed: {passed}/{total}")
            print(f"Success Rate: {passed/total*100:.1f}%")
            print(f"Average Score: {avg_score:.3f}")
            
            print("\nTest Results Summary:")
            for result in results:
                status = "✅ PASSED" if result.success else "❌ FAILED"
                print(f"  {result.benchmark_type}: {status} (Score: {result.overall_score:.3f})")
        
        print(f"\nDetailed results have been exported to the benchmark_test_results directory.")
        
    except Exception as e:
        print(f"❌ Test suite failed: {e}")
        return 1
    
    return 0


if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)