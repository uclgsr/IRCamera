#!/usr/bin/env python3
"""
Simple PC Controller Performance Monitor (MVP)

Focused MVP implementation for basic performance monitoring:
- Basic synchronization drift measurement
- Simple resource utilization tracking
- Essential network performance metrics
"""

import time
import json
import logging
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class SimplePerformanceResult:
    """Simple performance test result"""
    test_name: str
    success: bool
    value: float
    target: float
    unit: str
    summary: str


class SimplePerformanceMonitor:
    """Simple PC Controller Performance Monitor for MVP"""
    
    def __init__(self):
        self.results: List[SimplePerformanceResult] = []
        self.session_start = 0.0
        self.config = {
            "sync_drift_target_ms": 5.0,
            "sampling_rate_tolerance": 0.05,
            "network_latency_target_ms": 50.0,
        }
    
    def start_monitoring(self) -> str:
        """Start performance monitoring session"""
        self.session_start = time.time()
        self.results.clear()
        session_id = f"simple_session_{int(self.session_start)}"
        logger.info(f"Started simple performance monitoring: {session_id}")
        return session_id
    
    def check_sync_drift(self, gsr_time: float, rgb_time: float, thermal_time: Optional[float] = None) -> SimplePerformanceResult:
        """Check synchronization drift between sensor timestamps"""
        timestamps = [gsr_time, rgb_time]
        if thermal_time:
            timestamps.append(thermal_time)
        
        max_time = max(timestamps)
        min_time = min(timestamps)
        drift_ms = (max_time - min_time) * 1000
        
        target_ms = 5.0  # 5ms target
        success = drift_ms <= target_ms
        
        summary = f"{'✅' if success else '⚠️'} Sync drift: {drift_ms:.2f}ms"
        
        result = SimplePerformanceResult(
            test_name="sync_drift",
            success=success,
            value=drift_ms,
            target=target_ms,
            unit="ms",
            summary=summary
        )
        
        self.results.append(result)
        logger.info(summary)
        return result
    
    def check_sampling_rate(self, sample_count: int, duration_seconds: float, target_rate: float, sensor_type: str) -> SimplePerformanceResult:
        """Check if sampling rate meets target"""
        actual_rate = sample_count / duration_seconds if duration_seconds > 0 else 0.0
        rate_error = abs(actual_rate - target_rate) / target_rate
        success = rate_error <= 0.05  # 5% tolerance
        
        summary = f"{'✅' if success else '⚠️'} {sensor_type}: {actual_rate:.2f} Hz"
        
        result = SimplePerformanceResult(
            test_name=f"{sensor_type.lower()}_rate",
            success=success,
            value=actual_rate,
            target=target_rate,
            unit="Hz",
            summary=summary
        )
        
        self.results.append(result)
        logger.info(summary)
        return result
    
    def check_network_latency(self, latency_ms: float) -> SimplePerformanceResult:
        """Check network latency"""
        target_ms = 50.0  # 50ms target
        success = latency_ms <= target_ms
        
        summary = f"{'✅' if success else '⚠️'} Network latency: {latency_ms:.2f}ms"
        
        result = SimplePerformanceResult(
            test_name="network_latency",
            success=success,
            value=latency_ms,
            target=target_ms,
            unit="ms",
            summary=summary
        )
        
        self.results.append(result)
        logger.info(summary)
        return result
    
    def get_summary(self) -> Dict:
        """Get performance monitoring summary"""
        if not self.results:
            return {"status": "no_results", "message": "No performance data collected"}
        
        passed = sum(1 for r in self.results if r.success)
        total = len(self.results)
        success_rate = passed / total
        
        return {
            "total_tests": total,
            "passed_tests": passed,
            "success_rate": success_rate,
            "overall_success": success_rate >= 0.75,  # 75% threshold
            "session_duration": time.time() - self.session_start if self.session_start > 0 else 0,
            "results": [asdict(r) for r in self.results]
        }
    
    def export_results(self, output_dir: str = "simple_performance_results") -> Path:
        """Export results to JSON file"""
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"simple_performance_{timestamp}.json"
        filepath = output_path / filename
        
        summary = self.get_summary()
        
        with open(filepath, 'w') as f:
            json.dump(summary, f, indent=2)
        
        logger.info(f"Results exported to: {filepath}")
        return filepath
    
    def run_quick_test(self) -> Dict:
        """Run a quick performance validation test"""
        logger.info("=== Running Quick Performance Test ===")
        
        session_id = self.start_monitoring()
        
        # Test 1: Simulate GSR sampling rate check
        self.check_sampling_rate(sample_count=640, duration_seconds=5.0, target_rate=128.0, sensor_type="GSR")
        
        # Test 2: Simulate RGB frame rate check  
        self.check_sampling_rate(sample_count=150, duration_seconds=5.0, target_rate=30.0, sensor_type="RGB")
        
        # Test 3: Test sync drift
        base_time = time.time()
        self.check_sync_drift(
            gsr_time=base_time,
            rgb_time=base_time + 0.002,  # 2ms drift
            thermal_time=base_time + 0.001  # 1ms drift
        )
        
        # Test 4: Test network latency
        self.check_network_latency(25.0)  # 25ms simulated latency
        
        summary = self.get_summary()
        
        logger.info(f"=== Quick Test Complete: {summary['passed_tests']}/{summary['total_tests']} passed ===")
        for result in self.results:
            logger.info(result.summary)
        
        return summary


def main():
    """Run simple performance test demonstration"""
    print("Simple PC Controller Performance Monitor (MVP)")
    print("=" * 50)
    
    monitor = SimplePerformanceMonitor()
    
    try:
        results = monitor.run_quick_test()
        
        print(f"\nResults: {results['passed_tests']}/{results['total_tests']} tests passed")
        print(f"Success Rate: {results['success_rate']*100:.1f}%")
        print(f"Overall: {'✅ PASSED' if results['overall_success'] else '❌ FAILED'}")
        
        # Export results
        output_file = monitor.export_results()
        print(f"Results saved to: {output_file}")
        
        return 0 if results['overall_success'] else 1
        
    except Exception as e:
        print(f"❌ Test failed: {e}")
        logger.error("Test execution failed", exc_info=e)
        return 1


if __name__ == "__main__":
    import sys
    sys.exit(main())