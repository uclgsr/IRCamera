#!/usr/bin/env python3
"""
PC Controller Performance Evaluation System

Implements comprehensive performance benchmarking and evaluation metrics
for the PC controller hub system as specified in issue #6.

Features:
- Multi-modal data synchronization accuracy measurement
- Network communication performance analysis
- Resource utilization monitoring
- Real-time performance visualization
- Automated benchmark test execution
- Comprehensive reporting and export functionality
"""

import asyncio
import json
import logging
import psutil
import time
import numpy as np
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
import csv

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class PerformanceMetric:
    """Individual performance metric measurement"""
    name: str
    value: float
    unit: str
    timestamp: float
    is_within_threshold: bool
    threshold: Optional[float] = None
    category: str = "general"


@dataclass
class SynchronizationMetric:
    """Multi-modal data synchronization measurement"""
    timestamp: float
    gsr_timestamp: Optional[float]
    rgb_timestamp: Optional[float]
    thermal_timestamp: Optional[float]
    max_drift_ms: float
    avg_drift_ms: float
    sync_accuracy_score: float
    modalities_count: int


@dataclass
class ResourceUtilizationMetric:
    """System resource utilization measurement"""
    timestamp: float
    cpu_percent: float
    memory_percent: float
    memory_mb: float
    disk_io_read_mb: float
    disk_io_write_mb: float
    network_bytes_sent: float
    network_bytes_recv: float
    gpu_percent: Optional[float] = None
    gpu_memory_mb: Optional[float] = None


@dataclass
class NetworkPerformanceMetric:
    """Network communication performance measurement"""
    timestamp: float
    device_id: str
    connection_latency_ms: float
    throughput_kbps: float
    packet_loss_percent: float
    connection_stability: float
    data_integrity_score: float


@dataclass
class BenchmarkResult:
    """Complete benchmark result with all metrics"""
    benchmark_id: str
    benchmark_type: str
    start_time: float
    end_time: float
    duration_seconds: float
    success: bool
    overall_score: float
    metrics: Dict[str, float]
    performance_metrics: List[PerformanceMetric]
    synchronization_metrics: List[SynchronizationMetric]
    resource_metrics: List[ResourceUtilizationMetric]
    network_metrics: List[NetworkPerformanceMetric]
    summary: str
    recommendations: List[str]


class PCControllerPerformanceEvaluator:
    """
    Main performance evaluation system for PC controller
    """
    
    def __init__(self, output_directory: str = "performance_data"):
        self.output_directory = Path(output_directory)
        self.output_directory.mkdir(exist_ok=True)
        
        # Performance tracking
        self.performance_metrics: List[PerformanceMetric] = []
        self.synchronization_metrics: List[SynchronizationMetric] = []
        self.resource_metrics: List[ResourceUtilizationMetric] = []
        self.network_metrics: List[NetworkPerformanceMetric] = []
        
        # Benchmark configuration
        self.benchmark_config = {
            "gsr_target_sampling_rate": 128.0,  # Hz
            "rgb_target_frame_rate": 30.0,      # fps
            "thermal_target_frame_rate": 10.0,  # fps
            "acceptable_sync_drift_ms": 5.0,    # ms
            "acceptable_latency_ms": 50.0,      # ms
            "acceptable_packet_loss_percent": 5.0,  # %
            "resource_warning_cpu_percent": 80.0,   # %
            "resource_warning_memory_percent": 80.0, # %
        }
        
        # Monitoring state
        self.is_monitoring = False
        self.monitoring_tasks: List[asyncio.Task] = []
        self.session_start_time: Optional[float] = None
        
        logger.info("PC Controller Performance Evaluator initialized")
    
    async def start_performance_monitoring(self, session_id: str = None) -> str:
        """Start comprehensive performance monitoring"""
        if self.is_monitoring:
            raise RuntimeError("Performance monitoring already active")
        
        if session_id is None:
            session_id = f"perf_session_{int(time.time())}"
        
        self.is_monitoring = True
        self.session_start_time = time.time()
        self.clear_metrics()
        
        logger.info(f"Starting performance monitoring session: {session_id}")
        
        # Start monitoring tasks
        tasks = [
            self._monitor_resource_utilization(),
            self._monitor_system_performance(),
        ]
        
        self.monitoring_tasks = [asyncio.create_task(task) for task in tasks]
        
        return session_id
    
    async def stop_performance_monitoring(self) -> BenchmarkResult:
        """Stop performance monitoring and generate results"""
        if not self.is_monitoring:
            raise RuntimeError("Performance monitoring not active")
        
        self.is_monitoring = False
        
        # Cancel monitoring tasks
        for task in self.monitoring_tasks:
            task.cancel()
        
        # Wait for tasks to complete
        await asyncio.gather(*self.monitoring_tasks, return_exceptions=True)
        self.monitoring_tasks.clear()
        
        end_time = time.time()
        duration = end_time - (self.session_start_time or end_time)
        
        logger.info(f"Performance monitoring stopped after {duration:.2f} seconds")
        
        # Generate comprehensive benchmark result
        result = self._generate_benchmark_result(
            benchmark_id=f"pc_controller_perf_{int(time.time())}",
            benchmark_type="comprehensive_performance",
            start_time=self.session_start_time or end_time,
            end_time=end_time
        )
        
        return result
    
    def record_synchronization_metric(
        self,
        gsr_timestamp: Optional[float] = None,
        rgb_timestamp: Optional[float] = None,
        thermal_timestamp: Optional[float] = None
    ):
        """Record multi-modal synchronization metric"""
        timestamps = [t for t in [gsr_timestamp, rgb_timestamp, thermal_timestamp] if t is not None]
        
        if len(timestamps) < 2:
            return  # Need at least 2 modalities for sync analysis
        
        # Convert to milliseconds and calculate drift
        timestamps_ms = [t * 1000 if t else 0 for t in timestamps]
        valid_timestamps = [t for t in timestamps_ms if t > 0]
        
        max_timestamp = max(valid_timestamps)
        min_timestamp = min(valid_timestamps)
        max_drift_ms = max_timestamp - min_timestamp
        avg_drift_ms = np.std(valid_timestamps) if len(valid_timestamps) > 1 else 0.0
        
        # Calculate sync accuracy score (1.0 = perfect, 0.0 = terrible)
        sync_accuracy_score = max(0.0, 1.0 - (max_drift_ms / 100.0))  # 100ms = 0 score
        
        metric = SynchronizationMetric(
            timestamp=time.time(),
            gsr_timestamp=gsr_timestamp,
            rgb_timestamp=rgb_timestamp,
            thermal_timestamp=thermal_timestamp,
            max_drift_ms=max_drift_ms,
            avg_drift_ms=avg_drift_ms,
            sync_accuracy_score=sync_accuracy_score,
            modalities_count=len(timestamps)
        )
        
        self.synchronization_metrics.append(metric)
        
        # Log significant sync issues
        if max_drift_ms > self.benchmark_config["acceptable_sync_drift_ms"]:
            logger.warning(f"Synchronization drift detected: {max_drift_ms:.2f}ms")
    
    def record_network_performance(
        self,
        device_id: str,
        connection_latency_ms: float,
        throughput_kbps: float,
        packet_loss_percent: float = 0.0,
        connection_stability: float = 1.0,
        data_integrity_score: float = 1.0
    ):
        """Record network performance metric"""
        metric = NetworkPerformanceMetric(
            timestamp=time.time(),
            device_id=device_id,
            connection_latency_ms=connection_latency_ms,
            throughput_kbps=throughput_kbps,
            packet_loss_percent=packet_loss_percent,
            connection_stability=connection_stability,
            data_integrity_score=data_integrity_score
        )
        
        self.network_metrics.append(metric)
        
        # Log performance issues
        if connection_latency_ms > self.benchmark_config["acceptable_latency_ms"]:
            logger.warning(f"High latency detected for {device_id}: {connection_latency_ms:.2f}ms")
        
        if packet_loss_percent > self.benchmark_config["acceptable_packet_loss_percent"]:
            logger.warning(f"Packet loss detected for {device_id}: {packet_loss_percent:.2f}%")
    
    def record_performance_metric(
        self,
        name: str,
        value: float,
        unit: str,
        threshold: Optional[float] = None,
        category: str = "general"
    ):
        """Record a custom performance metric"""
        is_within_threshold = True
        if threshold is not None:
            is_within_threshold = value <= threshold
        
        metric = PerformanceMetric(
            name=name,
            value=value,
            unit=unit,
            timestamp=time.time(),
            is_within_threshold=is_within_threshold,
            threshold=threshold,
            category=category
        )
        
        self.performance_metrics.append(metric)
        
        if not is_within_threshold:
            logger.warning(f"Performance threshold exceeded: {name} = {value} {unit} (threshold: {threshold})")
    
    async def _monitor_resource_utilization(self):
        """Monitor system resource utilization"""
        logger.info("Starting resource utilization monitoring")
        
        while self.is_monitoring:
            try:
                # Get system metrics
                cpu_percent = psutil.cpu_percent()
                memory = psutil.virtual_memory()
                memory_percent = memory.percent
                memory_mb = memory.used / 1024 / 1024
                
                # Get disk I/O
                disk_io = psutil.disk_io_counters()
                disk_read_mb = disk_io.read_bytes / 1024 / 1024 if disk_io else 0.0
                disk_write_mb = disk_io.write_bytes / 1024 / 1024 if disk_io else 0.0
                
                # Get network I/O
                network_io = psutil.net_io_counters()
                network_sent = network_io.bytes_sent / 1024 / 1024 if network_io else 0.0
                network_recv = network_io.bytes_recv / 1024 / 1024 if network_io else 0.0
                
                # Create resource metric
                metric = ResourceUtilizationMetric(
                    timestamp=time.time(),
                    cpu_percent=cpu_percent,
                    memory_percent=memory_percent,
                    memory_mb=memory_mb,
                    disk_io_read_mb=disk_read_mb,
                    disk_io_write_mb=disk_write_mb,
                    network_bytes_sent=network_sent,
                    network_bytes_recv=network_recv
                )
                
                self.resource_metrics.append(metric)
                
                # Check for resource warnings
                if cpu_percent > self.benchmark_config["resource_warning_cpu_percent"]:
                    logger.warning(f"High CPU usage: {cpu_percent:.1f}%")
                
                if memory_percent > self.benchmark_config["resource_warning_memory_percent"]:
                    logger.warning(f"High memory usage: {memory_percent:.1f}%")
                
                # Log periodic resource summary
                if len(self.resource_metrics) % 30 == 0:  # Every 30 seconds
                    logger.info(f"Resources - CPU: {cpu_percent:.1f}%, "
                              f"Memory: {memory_percent:.1f}% ({memory_mb:.1f}MB), "
                              f"Network: ↑{network_sent:.1f}MB ↓{network_recv:.1f}MB")
                
            except Exception as e:
                logger.error(f"Error monitoring resource utilization: {e}")
            
            await asyncio.sleep(1.0)  # Monitor every second
    
    async def _monitor_system_performance(self):
        """Monitor general system performance metrics"""
        logger.info("Starting system performance monitoring")
        
        while self.is_monitoring:
            try:
                # Monitor various system performance aspects
                current_time = time.time()
                
                # Example performance metrics (can be extended)
                process_count = len(psutil.pids())
                self.record_performance_metric(
                    "system_process_count", 
                    process_count, 
                    "processes", 
                    category="system"
                )
                
                # Monitor load average (Unix systems)
                try:
                    load_avg = psutil.getloadavg()[0] if hasattr(psutil, 'getloadavg') else 0.0
                    self.record_performance_metric(
                        "system_load_average", 
                        load_avg, 
                        "load", 
                        threshold=4.0,
                        category="system"
                    )
                except:
                    pass  # Not available on all systems
                
                # Monitor disk usage
                disk_usage = psutil.disk_usage('/')
                disk_usage_percent = (disk_usage.used / disk_usage.total) * 100
                self.record_performance_metric(
                    "disk_usage_percent", 
                    disk_usage_percent, 
                    "%", 
                    threshold=90.0,
                    category="storage"
                )
                
            except Exception as e:
                logger.error(f"Error monitoring system performance: {e}")
            
            await asyncio.sleep(5.0)  # Monitor every 5 seconds
    
    def _generate_benchmark_result(
        self,
        benchmark_id: str,
        benchmark_type: str,
        start_time: float,
        end_time: float
    ) -> BenchmarkResult:
        """Generate comprehensive benchmark result"""
        duration_seconds = end_time - start_time
        
        # Calculate overall metrics
        overall_metrics = {}
        recommendations = []
        
        # Analyze synchronization performance
        if self.synchronization_metrics:
            sync_scores = [m.sync_accuracy_score for m in self.synchronization_metrics]
            avg_sync_score = np.mean(sync_scores)
            max_drifts = [m.max_drift_ms for m in self.synchronization_metrics]
            avg_max_drift = np.mean(max_drifts)
            
            overall_metrics["avg_sync_accuracy_score"] = avg_sync_score
            overall_metrics["avg_max_drift_ms"] = avg_max_drift
            
            if avg_max_drift > self.benchmark_config["acceptable_sync_drift_ms"]:
                recommendations.append(f"Improve synchronization - average drift {avg_max_drift:.2f}ms exceeds {self.benchmark_config['acceptable_sync_drift_ms']}ms threshold")
        
        # Analyze network performance
        if self.network_metrics:
            latencies = [m.connection_latency_ms for m in self.network_metrics]
            throughputs = [m.throughput_kbps for m in self.network_metrics]
            packet_losses = [m.packet_loss_percent for m in self.network_metrics]
            
            overall_metrics["avg_network_latency_ms"] = np.mean(latencies)
            overall_metrics["avg_throughput_kbps"] = np.mean(throughputs)
            overall_metrics["avg_packet_loss_percent"] = np.mean(packet_losses)
            
            if np.mean(latencies) > self.benchmark_config["acceptable_latency_ms"]:
                recommendations.append(f"Optimize network performance - average latency {np.mean(latencies):.2f}ms")
            
            if np.mean(packet_losses) > self.benchmark_config["acceptable_packet_loss_percent"]:
                recommendations.append(f"Address packet loss - average {np.mean(packet_losses):.2f}%")
        
        # Analyze resource utilization
        if self.resource_metrics:
            cpu_usage = [m.cpu_percent for m in self.resource_metrics]
            memory_usage = [m.memory_percent for m in self.resource_metrics]
            
            overall_metrics["avg_cpu_percent"] = np.mean(cpu_usage)
            overall_metrics["max_cpu_percent"] = np.max(cpu_usage)
            overall_metrics["avg_memory_percent"] = np.mean(memory_usage)
            overall_metrics["max_memory_percent"] = np.max(memory_usage)
            
            if np.max(cpu_usage) > self.benchmark_config["resource_warning_cpu_percent"]:
                recommendations.append(f"High CPU usage detected - peak {np.max(cpu_usage):.1f}%")
            
            if np.max(memory_usage) > self.benchmark_config["resource_warning_memory_percent"]:
                recommendations.append(f"High memory usage detected - peak {np.max(memory_usage):.1f}%")
        
        # Calculate overall performance score (0.0 - 1.0)
        score_components = []
        
        # Synchronization score (30% weight)
        if "avg_sync_accuracy_score" in overall_metrics:
            score_components.append(overall_metrics["avg_sync_accuracy_score"] * 0.3)
        
        # Network performance score (30% weight)
        if "avg_network_latency_ms" in overall_metrics:
            latency_score = max(0.0, 1.0 - (overall_metrics["avg_network_latency_ms"] / 200.0))  # 200ms = 0 score
            score_components.append(latency_score * 0.3)
        
        # Resource efficiency score (40% weight)
        if "avg_cpu_percent" in overall_metrics and "avg_memory_percent" in overall_metrics:
            cpu_score = max(0.0, 1.0 - (overall_metrics["avg_cpu_percent"] / 100.0))
            memory_score = max(0.0, 1.0 - (overall_metrics["avg_memory_percent"] / 100.0))
            resource_score = (cpu_score + memory_score) / 2
            score_components.append(resource_score * 0.4)
        
        overall_score = sum(score_components) if score_components else 0.5
        
        # Determine success based on score and critical issues
        success = overall_score >= 0.7 and len([r for r in recommendations if "critical" in r.lower()]) == 0
        
        # Generate summary
        if success and overall_score >= 0.9:
            summary = f"🟢 EXCELLENT performance - Score: {overall_score:.3f}"
        elif success and overall_score >= 0.8:
            summary = f"🟡 GOOD performance - Score: {overall_score:.3f}"
        elif success:
            summary = f"🟠 ACCEPTABLE performance - Score: {overall_score:.3f}"
        else:
            summary = f"🔴 POOR performance - Score: {overall_score:.3f}"
        
        return BenchmarkResult(
            benchmark_id=benchmark_id,
            benchmark_type=benchmark_type,
            start_time=start_time,
            end_time=end_time,
            duration_seconds=duration_seconds,
            success=success,
            overall_score=overall_score,
            metrics=overall_metrics,
            performance_metrics=self.performance_metrics.copy(),
            synchronization_metrics=self.synchronization_metrics.copy(),
            resource_metrics=self.resource_metrics.copy(),
            network_metrics=self.network_metrics.copy(),
            summary=summary,
            recommendations=recommendations
        )
    
    def export_benchmark_results(self, benchmark_result: BenchmarkResult) -> Path:
        """Export benchmark results to files"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        base_filename = f"pc_performance_benchmark_{timestamp}"
        
        # Export main results as JSON
        json_file = self.output_directory / f"{base_filename}.json"
        with open(json_file, 'w') as f:
            # Convert dataclasses to dict for JSON serialization
            result_dict = asdict(benchmark_result)
            json.dump(result_dict, f, indent=2, default=str)
        
        # Export detailed metrics as CSV
        csv_file = self.output_directory / f"{base_filename}_detailed.csv"
        self._export_detailed_metrics_csv(benchmark_result, csv_file)
        
        # Export summary report
        summary_file = self.output_directory / f"{base_filename}_summary.txt"
        self._export_summary_report(benchmark_result, summary_file)
        
        logger.info(f"Benchmark results exported:")
        logger.info(f"  JSON: {json_file}")
        logger.info(f"  CSV:  {csv_file}")
        logger.info(f"  Summary: {summary_file}")
        
        return json_file
    
    def _export_detailed_metrics_csv(self, benchmark_result: BenchmarkResult, csv_file: Path):
        """Export detailed metrics to CSV"""
        with open(csv_file, 'w', newline='') as f:
            writer = csv.writer(f)
            
            # Write header
            writer.writerow(["# PC Controller Performance Benchmark - Detailed Metrics"])
            writer.writerow([f"# Benchmark ID: {benchmark_result.benchmark_id}"])
            writer.writerow([f"# Duration: {benchmark_result.duration_seconds:.2f}s"])
            writer.writerow([f"# Overall Score: {benchmark_result.overall_score:.3f}"])
            writer.writerow([f"# Success: {benchmark_result.success}"])
            writer.writerow([])
            
            # Export synchronization metrics
            if benchmark_result.synchronization_metrics:
                writer.writerow(["# Synchronization Metrics"])
                writer.writerow(["timestamp", "gsr_timestamp", "rgb_timestamp", "thermal_timestamp", 
                               "max_drift_ms", "avg_drift_ms", "sync_accuracy_score", "modalities_count"])
                
                for metric in benchmark_result.synchronization_metrics:
                    writer.writerow([
                        metric.timestamp,
                        metric.gsr_timestamp or "",
                        metric.rgb_timestamp or "",
                        metric.thermal_timestamp or "",
                        metric.max_drift_ms,
                        metric.avg_drift_ms,
                        metric.sync_accuracy_score,
                        metric.modalities_count
                    ])
                writer.writerow([])
            
            # Export resource metrics
            if benchmark_result.resource_metrics:
                writer.writerow(["# Resource Utilization Metrics"])
                writer.writerow(["timestamp", "cpu_percent", "memory_percent", "memory_mb", 
                               "disk_io_read_mb", "disk_io_write_mb", "network_bytes_sent", "network_bytes_recv"])
                
                for metric in benchmark_result.resource_metrics:
                    writer.writerow([
                        metric.timestamp,
                        metric.cpu_percent,
                        metric.memory_percent,
                        metric.memory_mb,
                        metric.disk_io_read_mb,
                        metric.disk_io_write_mb,
                        metric.network_bytes_sent,
                        metric.network_bytes_recv
                    ])
                writer.writerow([])
            
            # Export network metrics
            if benchmark_result.network_metrics:
                writer.writerow(["# Network Performance Metrics"])
                writer.writerow(["timestamp", "device_id", "connection_latency_ms", "throughput_kbps", 
                               "packet_loss_percent", "connection_stability", "data_integrity_score"])
                
                for metric in benchmark_result.network_metrics:
                    writer.writerow([
                        metric.timestamp,
                        metric.device_id,
                        metric.connection_latency_ms,
                        metric.throughput_kbps,
                        metric.packet_loss_percent,
                        metric.connection_stability,
                        metric.data_integrity_score
                    ])
    
    def _export_summary_report(self, benchmark_result: BenchmarkResult, summary_file: Path):
        """Export human-readable summary report"""
        with open(summary_file, 'w') as f:
            f.write("PC CONTROLLER PERFORMANCE BENCHMARK SUMMARY\n")
            f.write("=" * 50 + "\n\n")
            
            f.write(f"Benchmark ID: {benchmark_result.benchmark_id}\n")
            f.write(f"Benchmark Type: {benchmark_result.benchmark_type}\n")
            f.write(f"Start Time: {datetime.fromtimestamp(benchmark_result.start_time)}\n")
            f.write(f"Duration: {benchmark_result.duration_seconds:.2f} seconds\n")
            f.write(f"Overall Score: {benchmark_result.overall_score:.3f}/1.000\n")
            f.write(f"Success: {'PASSED' if benchmark_result.success else 'FAILED'}\n")
            f.write(f"Summary: {benchmark_result.summary}\n\n")
            
            # Overall metrics
            f.write("OVERALL METRICS\n")
            f.write("-" * 20 + "\n")
            for metric_name, metric_value in benchmark_result.metrics.items():
                f.write(f"{metric_name}: {metric_value:.3f}\n")
            f.write("\n")
            
            # Recommendations
            if benchmark_result.recommendations:
                f.write("RECOMMENDATIONS\n")
                f.write("-" * 20 + "\n")
                for i, recommendation in enumerate(benchmark_result.recommendations, 1):
                    f.write(f"{i}. {recommendation}\n")
                f.write("\n")
            
            # Performance category analysis
            sync_metrics = benchmark_result.synchronization_metrics
            resource_metrics = benchmark_result.resource_metrics
            network_metrics = benchmark_result.network_metrics
            
            if sync_metrics:
                avg_drift = np.mean([m.max_drift_ms for m in sync_metrics])
                avg_score = np.mean([m.sync_accuracy_score for m in sync_metrics])
                f.write("SYNCHRONIZATION ANALYSIS\n")
                f.write("-" * 25 + "\n")
                f.write(f"Average Max Drift: {avg_drift:.2f}ms\n")
                f.write(f"Average Accuracy Score: {avg_score:.3f}\n")
                f.write(f"Total Measurements: {len(sync_metrics)}\n\n")
            
            if resource_metrics:
                avg_cpu = np.mean([m.cpu_percent for m in resource_metrics])
                max_cpu = np.max([m.cpu_percent for m in resource_metrics])
                avg_memory = np.mean([m.memory_percent for m in resource_metrics])
                max_memory = np.max([m.memory_percent for m in resource_metrics])
                
                f.write("RESOURCE UTILIZATION ANALYSIS\n")
                f.write("-" * 30 + "\n")
                f.write(f"CPU Usage - Average: {avg_cpu:.1f}%, Peak: {max_cpu:.1f}%\n")
                f.write(f"Memory Usage - Average: {avg_memory:.1f}%, Peak: {max_memory:.1f}%\n")
                f.write(f"Total Measurements: {len(resource_metrics)}\n\n")
            
            if network_metrics:
                avg_latency = np.mean([m.connection_latency_ms for m in network_metrics])
                avg_throughput = np.mean([m.throughput_kbps for m in network_metrics])
                avg_stability = np.mean([m.connection_stability for m in network_metrics])
                
                f.write("NETWORK PERFORMANCE ANALYSIS\n")
                f.write("-" * 30 + "\n")
                f.write(f"Average Latency: {avg_latency:.2f}ms\n")
                f.write(f"Average Throughput: {avg_throughput:.1f} KB/s\n")
                f.write(f"Average Stability: {avg_stability:.3f}\n")
                f.write(f"Total Measurements: {len(network_metrics)}\n\n")
    
    def clear_metrics(self):
        """Clear all stored metrics"""
        self.performance_metrics.clear()
        self.synchronization_metrics.clear()
        self.resource_metrics.clear()
        self.network_metrics.clear()
        logger.info("Performance metrics cleared")
    
    def get_current_status(self) -> Dict[str, Any]:
        """Get current monitoring status"""
        return {
            "is_monitoring": self.is_monitoring,
            "session_start_time": self.session_start_time,
            "metrics_count": {
                "performance": len(self.performance_metrics),
                "synchronization": len(self.synchronization_metrics),
                "resource": len(self.resource_metrics),
                "network": len(self.network_metrics)
            },
            "monitoring_tasks": len(self.monitoring_tasks)
        }


# Example usage and testing functionality
async def run_performance_test():
    """Run a sample performance evaluation test"""
    evaluator = PCControllerPerformanceEvaluator()
    
    logger.info("Starting sample performance evaluation test...")
    
    # Start monitoring
    session_id = await evaluator.start_performance_monitoring("sample_test")
    
    # Simulate some performance data
    await asyncio.sleep(2)
    
    # Record some sample synchronization metrics
    evaluator.record_synchronization_metric(
        gsr_timestamp=time.time(),
        rgb_timestamp=time.time() + 0.002,  # 2ms drift
        thermal_timestamp=time.time() + 0.001  # 1ms drift
    )
    
    # Record some network performance metrics
    evaluator.record_network_performance(
        device_id="android_device_1",
        connection_latency_ms=25.5,
        throughput_kbps=512.0,
        packet_loss_percent=0.1,
        connection_stability=0.98
    )
    
    await asyncio.sleep(3)
    
    # Stop monitoring and get results
    result = await evaluator.stop_performance_monitoring()
    
    # Export results
    output_file = evaluator.export_benchmark_results(result)
    
    logger.info(f"Performance evaluation completed: {result.summary}")
    logger.info(f"Results exported to: {output_file}")
    
    return result


if __name__ == "__main__":
    # Run the test
    asyncio.run(run_performance_test())