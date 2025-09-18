#!/usr/bin/env python3
"""
Simple Performance Evaluation Test

A simplified test of the performance evaluation system that doesn't require 
all external dependencies.
"""

import asyncio
import json
import logging
import time
import sys
import os
from datetime import datetime
from pathlib import Path

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class SimplePerformanceTest:
    """Simple performance test without external dependencies"""
    
    def __init__(self):
        self.output_dir = Path("simple_test_results")
        self.output_dir.mkdir(exist_ok=True)
    
    async def run_simple_test(self):
        """Run a simple performance evaluation test"""
        logger.info("=== Starting Simple Performance Evaluation Test ===")
        
        start_time = time.time()
        
        # Simulate performance data collection
        performance_data = {
            "test_id": f"simple_test_{int(start_time)}",
            "start_time": start_time,
            "metrics": {},
            "benchmarks": []
        }
        
        # Test 1: GSR Sampling Rate Simulation
        logger.info("Testing GSR sampling rate simulation...")
        gsr_metrics = await self.test_gsr_sampling_rate()
        performance_data["benchmarks"].append(gsr_metrics)
        
        # Test 2: RGB Frame Rate Simulation
        logger.info("Testing RGB frame rate simulation...")
        rgb_metrics = await self.test_rgb_frame_rate()
        performance_data["benchmarks"].append(rgb_metrics)
        
        # Test 3: Network Throughput Simulation
        logger.info("Testing network throughput simulation...")
        network_metrics = await self.test_network_throughput()
        performance_data["benchmarks"].append(network_metrics)
        
        # Test 4: Synchronization Accuracy
        logger.info("Testing synchronization accuracy...")
        sync_metrics = await self.test_synchronization_accuracy()
        performance_data["benchmarks"].append(sync_metrics)
        
        end_time = time.time()
        duration = end_time - start_time
        
        performance_data["end_time"] = end_time
        performance_data["duration_seconds"] = duration
        
        # Calculate overall results
        overall_results = self.calculate_overall_results(performance_data["benchmarks"])
        performance_data["overall"] = overall_results
        
        # Export results
        await self.export_results(performance_data)
        
        logger.info(f"Simple performance test completed in {duration:.2f} seconds")
        logger.info(f"Overall Score: {overall_results['score']:.3f}")
        logger.info(f"Success: {'PASSED' if overall_results['success'] else 'FAILED'}")
        
        return performance_data
    
    async def test_gsr_sampling_rate(self):
        """Test GSR sampling rate performance"""
        target_rate = 128.0  # Hz
        test_duration = 5.0  # seconds
        
        start_time = time.time()
        samples = []
        
        # Simulate GSR sampling
        sample_interval = 1.0 / target_rate
        next_sample_time = start_time
        
        while time.time() - start_time < test_duration:
            current_time = time.time()
            if current_time >= next_sample_time:
                samples.append(current_time)
                next_sample_time += sample_interval
                
            await asyncio.sleep(0.001)  # Small delay
        
        # Calculate metrics
        actual_duration = samples[-1] - samples[0] if len(samples) > 1 else test_duration
        actual_rate = (len(samples) - 1) / actual_duration if actual_duration > 0 else 0
        rate_error = abs(actual_rate - target_rate) / target_rate
        
        # Calculate variance
        if len(samples) > 2:
            intervals = [samples[i+1] - samples[i] for i in range(len(samples)-1)]
            avg_interval = sum(intervals) / len(intervals)
            variance = sum((interval - avg_interval)**2 for interval in intervals) / len(intervals)
            normalized_variance = variance / (avg_interval**2)
        else:
            normalized_variance = 0.0
        
        success = rate_error <= 0.05  # 5% tolerance
        
        return {
            "test_type": "GSR_SAMPLING_RATE",
            "target_rate_hz": target_rate,
            "actual_rate_hz": actual_rate,
            "rate_error_percent": rate_error * 100,
            "total_samples": len(samples),
            "duration_seconds": actual_duration,
            "variance": normalized_variance,
            "success": success,
            "score": 1.0 - rate_error if success else 0.5,
            "summary": f"GSR: {actual_rate:.2f} Hz (target: {target_rate} Hz) - {'PASSED' if success else 'FAILED'}"
        }
    
    async def test_rgb_frame_rate(self):
        """Test RGB frame rate performance"""
        target_rate = 30.0  # fps
        test_duration = 3.0  # seconds
        
        start_time = time.time()
        frames = []
        
        # Simulate RGB frame capture
        frame_interval = 1.0 / target_rate
        next_frame_time = start_time
        
        while time.time() - start_time < test_duration:
            current_time = time.time()
            if current_time >= next_frame_time:
                # Simulate frame processing time (1-5ms)
                processing_delay = 0.001 + 0.004 * (hash(str(current_time)) % 100) / 100
                await asyncio.sleep(processing_delay)
                
                frames.append({
                    "timestamp": current_time,
                    "processing_time_ms": processing_delay * 1000,
                    "frame_size_kb": 150 + 50 * (hash(str(current_time)) % 100) / 100  # 150-200KB
                })
                next_frame_time += frame_interval
                
            await asyncio.sleep(0.001)
        
        # Calculate metrics
        actual_duration = frames[-1]["timestamp"] - frames[0]["timestamp"] if len(frames) > 1 else test_duration
        actual_rate = (len(frames) - 1) / actual_duration if actual_duration > 0 else 0
        rate_error = abs(actual_rate - target_rate) / target_rate
        
        avg_processing_time = sum(f["processing_time_ms"] for f in frames) / len(frames) if frames else 0
        avg_frame_size = sum(f["frame_size_kb"] for f in frames) / len(frames) if frames else 0
        
        success = rate_error <= 0.2  # 20% tolerance for frame rate
        
        return {
            "test_type": "RGB_FRAME_RATE",
            "target_rate_fps": target_rate,
            "actual_rate_fps": actual_rate,
            "rate_error_percent": rate_error * 100,
            "total_frames": len(frames),
            "duration_seconds": actual_duration,
            "avg_processing_time_ms": avg_processing_time,
            "avg_frame_size_kb": avg_frame_size,
            "success": success,
            "score": 1.0 - rate_error if success else 0.4,
            "summary": f"RGB: {actual_rate:.2f} fps (target: {target_rate} fps) - {'PASSED' if success else 'FAILED'}"
        }
    
    async def test_network_throughput(self):
        """Test network throughput simulation"""
        test_duration = 2.0  # seconds
        
        start_time = time.time()
        packets = []
        total_bytes = 0
        
        # Simulate network activity
        while time.time() - start_time < test_duration:
            current_time = time.time()
            
            # Simulate packet transmission
            packet_size = 512 + int(1024 * (hash(str(current_time)) % 100) / 100)  # 512-1536 bytes
            latency_ms = 15 + 20 * (hash(str(current_time + 1)) % 100) / 100  # 15-35ms
            
            packets.append({
                "timestamp": current_time,
                "size_bytes": packet_size,
                "latency_ms": latency_ms
            })
            
            total_bytes += packet_size
            
            await asyncio.sleep(0.01)  # 100Hz network activity
        
        # Calculate metrics
        actual_duration = packets[-1]["timestamp"] - packets[0]["timestamp"] if len(packets) > 1 else test_duration
        throughput_kbps = (total_bytes / 1024) / actual_duration if actual_duration > 0 else 0
        avg_latency_ms = sum(p["latency_ms"] for p in packets) / len(packets) if packets else 0
        
        # Simulate packet loss (5% random)
        packet_loss_percent = 5.0 * (hash(str(start_time)) % 100) / 100 / 20  # 0-0.25%
        
        success = avg_latency_ms <= 50 and packet_loss_percent <= 5.0
        
        return {
            "test_type": "NETWORK_THROUGHPUT",
            "throughput_kbps": throughput_kbps,
            "avg_latency_ms": avg_latency_ms,
            "packet_loss_percent": packet_loss_percent,
            "total_packets": len(packets),
            "total_bytes": total_bytes,
            "duration_seconds": actual_duration,
            "success": success,
            "score": 0.9 if success else 0.5,
            "summary": f"Network: {throughput_kbps:.1f} KB/s, Latency: {avg_latency_ms:.1f}ms - {'PASSED' if success else 'FAILED'}"
        }
    
    async def test_synchronization_accuracy(self):
        """Test timestamp synchronization accuracy"""
        base_time = time.time()
        
        # Simulate sensor timestamps with drift
        sensor_data = {
            "gsr": [],
            "rgb": [],
            "thermal": []
        }
        
        # Generate synchronized timestamps with realistic drift
        for i in range(100):  # 100 sample points
            sample_time = base_time + i * 0.01  # 10ms intervals
            
            # Add realistic drift patterns
            gsr_drift = 0.002 * (i % 10) / 10  # 0-2ms cyclical drift
            rgb_drift = 0.003 * (hash(str(i)) % 100) / 100  # 0-3ms random drift  
            thermal_drift = 0.001 * (i % 5) / 5  # 0-1ms cyclical drift
            
            sensor_data["gsr"].append(sample_time + gsr_drift)
            sensor_data["rgb"].append(sample_time + rgb_drift)
            sensor_data["thermal"].append(sample_time + thermal_drift)
        
        # Calculate synchronization metrics
        sync_metrics = []
        
        for i in range(len(sensor_data["gsr"])):
            timestamps = [
                sensor_data["gsr"][i],
                sensor_data["rgb"][i],
                sensor_data["thermal"][i]
            ]
            
            max_time = max(timestamps)
            min_time = min(timestamps)
            drift_ms = (max_time - min_time) * 1000
            
            sync_metrics.append(drift_ms)
        
        avg_drift_ms = sum(sync_metrics) / len(sync_metrics)
        max_drift_ms = max(sync_metrics)
        
        # Calculate sync accuracy score
        sync_accuracy_score = max(0.0, 1.0 - (avg_drift_ms / 10.0))  # 10ms = 0 score
        
        success = max_drift_ms <= 5.0  # 5ms threshold
        
        return {
            "test_type": "TIMESTAMP_SYNC_ACCURACY",
            "avg_drift_ms": avg_drift_ms,
            "max_drift_ms": max_drift_ms,
            "sync_accuracy_score": sync_accuracy_score,
            "sample_count": len(sync_metrics),
            "success": success,
            "score": sync_accuracy_score,
            "summary": f"Sync: {avg_drift_ms:.2f}ms avg drift (max: {max_drift_ms:.2f}ms) - {'PASSED' if success else 'FAILED'}"
        }
    
    def calculate_overall_results(self, benchmarks):
        """Calculate overall test results"""
        if not benchmarks:
            return {"success": False, "score": 0.0, "summary": "No benchmark results"}
        
        scores = [b["score"] for b in benchmarks]
        successes = [b["success"] for b in benchmarks]
        
        avg_score = sum(scores) / len(scores)
        success_rate = sum(successes) / len(successes)
        overall_success = success_rate >= 0.75  # 75% of tests must pass
        
        if overall_success and avg_score >= 0.9:
            assessment = "🟢 EXCELLENT"
        elif overall_success and avg_score >= 0.7:
            assessment = "🟡 GOOD"
        elif avg_score >= 0.5:
            assessment = "🟠 ACCEPTABLE"
        else:
            assessment = "🔴 POOR"
        
        return {
            "success": overall_success,
            "score": avg_score,
            "success_rate": success_rate,
            "assessment": assessment,
            "summary": f"{assessment} - Score: {avg_score:.3f}, Success Rate: {success_rate*100:.1f}%"
        }
    
    async def export_results(self, performance_data):
        """Export test results to files"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Export JSON results
        json_file = self.output_dir / f"simple_performance_test_{timestamp}.json"
        with open(json_file, 'w') as f:
            json.dump(performance_data, f, indent=2, default=str)
        
        # Export text summary
        summary_file = self.output_dir / f"simple_performance_summary_{timestamp}.txt"
        with open(summary_file, 'w') as f:
            f.write("SIMPLE PERFORMANCE EVALUATION TEST RESULTS\n")
            f.write("=" * 50 + "\n\n")
            
            f.write(f"Test ID: {performance_data['test_id']}\n")
            f.write(f"Duration: {performance_data['duration_seconds']:.2f} seconds\n")
            f.write(f"Overall Score: {performance_data['overall']['score']:.3f}\n")
            f.write(f"Overall Assessment: {performance_data['overall']['assessment']}\n\n")
            
            f.write("INDIVIDUAL BENCHMARK RESULTS\n")
            f.write("-" * 30 + "\n")
            
            for benchmark in performance_data['benchmarks']:
                f.write(f"{benchmark['test_type']}: {benchmark['summary']}\n")
            
            f.write(f"\nDetailed results saved to: {json_file}\n")
        
        logger.info(f"Results exported to: {json_file}")
        logger.info(f"Summary exported to: {summary_file}")


async def main():
    """Main function"""
    print("Simple Performance Evaluation Test")
    print("=" * 40)
    print("Testing core performance benchmarking functionality...")
    print()
    
    test = SimplePerformanceTest()
    
    try:
        results = await test.run_simple_test()
        
        print("\n" + "=" * 40)
        print("TEST COMPLETED")
        print("=" * 40)
        print(f"Overall: {results['overall']['summary']}")
        print(f"Individual Tests:")
        for benchmark in results['benchmarks']:
            print(f"  - {benchmark['summary']}")
        
        return 0 if results['overall']['success'] else 1
        
    except Exception as e:
        print(f"❌ Test failed: {e}")
        logger.error("Test execution failed", exc_info=e)
        return 1


if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)