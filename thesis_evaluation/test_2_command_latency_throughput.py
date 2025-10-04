#!/usr/bin/env python3
"""
Test 2: Command Latency and Throughput Metric Test

This test measures how quickly and consistently the system processes
incoming commands under normal conditions. It sends a series of commands
and timestamps both send and receive actions.

Output: A table/JSON log with PC send time, Phone receive time, and
Action completed time for each command. Derives round-trip latency.

Subsystem: Network/Control performance
Chapters: Chapter 5 (quantitative results on command handling speed)
          Chapter 6 (performance evaluation conclusions)
"""

import json
import logging
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List

sys.path.insert(0, str(Path(__file__).parent.parent / 'pc-controller'))

from command_client import CommandClient

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class CommandLatencyThroughputTest:
    """Test command latency and throughput metrics"""
    
    def __init__(self, device_ip: str, port: int = 8080):
        self.device_ip = device_ip
        self.port = port
        self.client = CommandClient(timeout=15)
        self.measurements: List[Dict] = []
        
    def measure_command(self, device_id: str, command: str, params: Dict = None) -> Dict:
        """
        Send a command and measure timing
        
        Returns:
            Dictionary with timing measurements
        """
        # Record PC send time
        pc_send_time_ns = time.time_ns()
        pc_send_time_iso = datetime.now().isoformat()
        
        # Send command
        response = self.client.send_command(device_id, command, params)
        
        # Record PC receive time
        pc_receive_time_ns = time.time_ns()
        pc_receive_time_iso = datetime.now().isoformat()
        
        # Calculate latency
        round_trip_latency_ns = pc_receive_time_ns - pc_send_time_ns
        round_trip_latency_ms = round_trip_latency_ns / 1e6
        
        measurement = {
            'command': command,
            'params': params or {},
            'pc_send_time_iso': pc_send_time_iso,
            'pc_send_time_ns': pc_send_time_ns,
            'pc_receive_time_iso': pc_receive_time_iso,
            'pc_receive_time_ns': pc_receive_time_ns,
            'round_trip_latency_ns': round_trip_latency_ns,
            'round_trip_latency_ms': round_trip_latency_ms,
            'response': response,
            'success': response is not None
        }
        
        self.measurements.append(measurement)
        
        logger.info(f"Command: {command}, Latency: {round_trip_latency_ms:.2f} ms, Response: {response}")
        
        return measurement
    
    def run_test(self, num_iterations: int = 10) -> Dict:
        """
        Execute the latency and throughput test
        
        Args:
            num_iterations: Number of command iterations to test
            
        Returns:
            Dictionary containing test results
        """
        logger.info("=" * 80)
        logger.info("TEST 2: Command Latency and Throughput Metric Test")
        logger.info("=" * 80)
        
        test_start_time = datetime.now()
        
        # Connect to device
        logger.info(f"\nConnecting to device {self.device_ip}:{self.port}")
        if not self.client.connect_to_device(self.device_ip, self.port):
            return self._generate_report(
                success=False,
                error=f"Could not connect to {self.device_ip}:{self.port}"
            )
        
        device_id = f"{self.device_ip}:{self.port}"
        logger.info("Connected successfully\n")
        
        # Test 1: PING commands for baseline latency
        logger.info(f"Test 1: Measuring baseline latency with {num_iterations} PING commands")
        for i in range(num_iterations):
            self.measure_command(device_id, 'PING')
            time.sleep(0.1)  # Small delay between commands
        
        time.sleep(1)  # Pause between test phases
        
        # Test 2: SYNC commands
        logger.info(f"\nTest 2: Measuring SYNC command latency ({num_iterations} iterations)")
        for i in range(num_iterations):
            self.measure_command(device_id, 'SYNC', {
                't_pc': int(time.time() * 1000)
            })
            time.sleep(0.1)
        
        time.sleep(1)
        
        # Test 3: STATUS commands
        logger.info(f"\nTest 3: Measuring GET_STATUS command latency ({num_iterations} iterations)")
        for i in range(num_iterations):
            self.measure_command(device_id, 'GET_STATUS')
            time.sleep(0.1)
        
        time.sleep(1)
        
        # Test 4: START/STOP sequence with timing
        logger.info(f"\nTest 4: Measuring START/STOP command sequence")
        session_id = f"latency_test_{int(time.time())}"
        
        self.measure_command(device_id, 'START', {'session_id': session_id})
        time.sleep(2)  # Let recording run briefly
        self.measure_command(device_id, 'STOP', {'session_id': session_id})
        
        time.sleep(1)
        
        # Test 5: Rapid command burst (throughput test)
        logger.info(f"\nTest 5: Throughput test - rapid command burst (10 PING commands)")
        burst_start = time.time_ns()
        for i in range(10):
            self.measure_command(device_id, 'PING')
            # No delay - test throughput
        burst_end = time.time_ns()
        burst_duration_ms = (burst_end - burst_start) / 1e6
        commands_per_second = 10 / (burst_duration_ms / 1000)
        
        logger.info(f"Burst completed in {burst_duration_ms:.2f} ms")
        logger.info(f"Throughput: {commands_per_second:.2f} commands/second")
        
        # Disconnect
        self.client.disconnect_device(device_id)
        
        test_end_time = datetime.now()
        total_duration = (test_end_time - test_start_time).total_seconds()
        
        return self._generate_report(
            success=True,
            total_duration=total_duration,
            burst_duration_ms=burst_duration_ms,
            throughput_cmds_per_sec=commands_per_second
        )
    
    def _generate_report(self, success: bool, error: str = None, 
                        total_duration: float = None,
                        burst_duration_ms: float = None,
                        throughput_cmds_per_sec: float = None) -> Dict:
        """Generate test report"""
        report = {
            'test_name': 'Command Latency and Throughput Metric Test',
            'test_id': 'test_2',
            'timestamp': datetime.now().isoformat(),
            'success': success,
            'measurements': self.measurements,
            'statistics': self._calculate_statistics()
        }
        
        if error:
            report['error'] = error
        
        if total_duration:
            report['total_duration_seconds'] = total_duration
        
        if burst_duration_ms:
            report['burst_duration_ms'] = burst_duration_ms
            report['throughput_cmds_per_sec'] = throughput_cmds_per_sec
        
        return report
    
    def _calculate_statistics(self) -> Dict:
        """Calculate statistics from measurements"""
        if not self.measurements:
            return {}
        
        # Group by command type
        by_command = {}
        for m in self.measurements:
            cmd = m['command']
            if cmd not in by_command:
                by_command[cmd] = []
            by_command[cmd].append(m['round_trip_latency_ms'])
        
        statistics = {
            'total_commands': len(self.measurements),
            'successful_commands': sum(1 for m in self.measurements if m['success']),
            'failed_commands': sum(1 for m in self.measurements if not m['success']),
            'by_command_type': {}
        }
        
        # Calculate stats for each command type
        for cmd, latencies in by_command.items():
            if latencies:
                statistics['by_command_type'][cmd] = {
                    'count': len(latencies),
                    'min_latency_ms': min(latencies),
                    'max_latency_ms': max(latencies),
                    'avg_latency_ms': sum(latencies) / len(latencies),
                    'median_latency_ms': sorted(latencies)[len(latencies) // 2]
                }
        
        # Overall statistics
        all_latencies = [m['round_trip_latency_ms'] for m in self.measurements if m['success']]
        if all_latencies:
            statistics['overall'] = {
                'min_latency_ms': min(all_latencies),
                'max_latency_ms': max(all_latencies),
                'avg_latency_ms': sum(all_latencies) / len(all_latencies),
                'median_latency_ms': sorted(all_latencies)[len(all_latencies) // 2]
            }
        
        return statistics
    
    def save_report(self, report: Dict, output_file: str = None):
        """Save test report to JSON file"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"test_2_latency_throughput_report_{timestamp}.json"
        
        output_path = Path(__file__).parent / output_file
        
        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        logger.info(f"\nReport saved to: {output_path}")
        
        # Create a readable table format
        table_file = output_path.with_suffix('.txt')
        with open(table_file, 'w') as f:
            f.write("=" * 100 + "\n")
            f.write("TEST 2: Command Latency and Throughput Metric Test - Results\n")
            f.write("=" * 100 + "\n\n")
            
            # Measurements table
            f.write("Command Measurements:\n")
            f.write("-" * 100 + "\n")
            f.write(f"{'Command':<15} {'PC Send Time':<26} {'PC Receive Time':<26} {'Latency (ms)':<15} {'Response':<20}\n")
            f.write("-" * 100 + "\n")
            
            for m in report['measurements']:
                f.write(f"{m['command']:<15} ")
                f.write(f"{m['pc_send_time_iso']:<26} ")
                f.write(f"{m['pc_receive_time_iso']:<26} ")
                f.write(f"{m['round_trip_latency_ms']:<15.2f} ")
                response_str = str(m['response'])[:18] if m['response'] else 'None'
                f.write(f"{response_str:<20}\n")
            
            f.write("-" * 100 + "\n\n")
            
            # Statistics
            f.write("Statistics:\n")
            f.write("-" * 100 + "\n")
            stats = report['statistics']
            f.write(f"Total Commands: {stats['total_commands']}\n")
            f.write(f"Successful: {stats['successful_commands']}\n")
            f.write(f"Failed: {stats['failed_commands']}\n\n")
            
            if 'overall' in stats:
                f.write("Overall Latency:\n")
                f.write(f"  Min: {stats['overall']['min_latency_ms']:.2f} ms\n")
                f.write(f"  Max: {stats['overall']['max_latency_ms']:.2f} ms\n")
                f.write(f"  Avg: {stats['overall']['avg_latency_ms']:.2f} ms\n")
                f.write(f"  Median: {stats['overall']['median_latency_ms']:.2f} ms\n\n")
            
            if 'by_command_type' in stats:
                f.write("By Command Type:\n")
                for cmd, cmd_stats in stats['by_command_type'].items():
                    f.write(f"\n  {cmd}:\n")
                    f.write(f"    Count: {cmd_stats['count']}\n")
                    f.write(f"    Min: {cmd_stats['min_latency_ms']:.2f} ms\n")
                    f.write(f"    Max: {cmd_stats['max_latency_ms']:.2f} ms\n")
                    f.write(f"    Avg: {cmd_stats['avg_latency_ms']:.2f} ms\n")
                    f.write(f"    Median: {cmd_stats['median_latency_ms']:.2f} ms\n")
            
            if 'throughput_cmds_per_sec' in report:
                f.write(f"\nThroughput Test:\n")
                f.write(f"  Burst Duration: {report['burst_duration_ms']:.2f} ms\n")
                f.write(f"  Throughput: {report['throughput_cmds_per_sec']:.2f} commands/second\n")
            
            f.write("\n" + "=" * 100 + "\n")
        
        logger.info(f"Readable report saved to: {table_file}")


def main():
    """Main entry point for test"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Test 2: Command Latency and Throughput Metric Test'
    )
    parser.add_argument(
        '--device-ip',
        required=True,
        help='IP address of Android device'
    )
    parser.add_argument(
        '--port',
        type=int,
        default=8080,
        help='Port number (default: 8080)'
    )
    parser.add_argument(
        '--iterations',
        type=int,
        default=10,
        help='Number of iterations per command type (default: 10)'
    )
    parser.add_argument(
        '--output',
        help='Output file name for report (optional)'
    )
    
    args = parser.parse_args()
    
    # Run test
    test = CommandLatencyThroughputTest(args.device_ip, args.port)
    report = test.run_test(num_iterations=args.iterations)
    
    # Save report
    test.save_report(report, args.output)
    
    # Print summary
    logger.info("\n" + "=" * 80)
    logger.info("TEST RESULTS")
    logger.info("=" * 80)
    logger.info(f"Success: {report['success']}")
    
    if report['success']:
        stats = report['statistics']
        logger.info(f"Total Commands: {stats['total_commands']}")
        logger.info(f"Successful: {stats['successful_commands']}")
        logger.info(f"Failed: {stats['failed_commands']}")
        
        if 'overall' in stats:
            logger.info(f"\nOverall Latency:")
            logger.info(f"  Min: {stats['overall']['min_latency_ms']:.2f} ms")
            logger.info(f"  Max: {stats['overall']['max_latency_ms']:.2f} ms")
            logger.info(f"  Avg: {stats['overall']['avg_latency_ms']:.2f} ms")
            logger.info(f"  Median: {stats['overall']['median_latency_ms']:.2f} ms")
        
        if 'throughput_cmds_per_sec' in report:
            logger.info(f"\nThroughput: {report['throughput_cmds_per_sec']:.2f} commands/second")
    else:
        logger.error(f"Error: {report.get('error', 'Unknown error')}")
        sys.exit(1)
    
    logger.info("\nTest completed successfully!")


if __name__ == "__main__":
    main()
