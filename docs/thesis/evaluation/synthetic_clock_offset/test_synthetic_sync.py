#!/usr/bin/env python3
"""
Synthetic Clock Offset Measurement Test

This test simulates the time-sync handshake where the PC sends multiple SYNC
commands to a simulated Android app. It measures round-trip latency and computes
clock offset in milliseconds to evaluate timing precision and jitter.

Test Type: Simulated/Synthetic
Output: CSV log file with sync metrics
Subsystem: Time synchronization module
Thesis Chapters: Chapter 5 (sync accuracy results), Chapter 6 (timing alignment)
"""

import sys
import os
import time
import csv
import random
import statistics
from typing import List, Dict
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / 'pc-controller'))

from sync_handler import SyncHandler


class SimulatedAndroidDevice:
    """Simulates an Android device with configurable clock offset and network latency"""

    def __init__(self, clock_offset_ms: int = 0, network_latency_ms: float = 5.0):
        self.clock_offset_ms = clock_offset_ms
        self.network_latency_ms = network_latency_ms

    def get_current_time_ms(self) -> int:
        """Get simulated device time with clock offset applied"""
        return int(time.time() * 1000) + self.clock_offset_ms

    def simulate_network_delay(self):
        """Simulate network latency"""
        time.sleep(self.network_latency_ms / 1000.0)


class MockSocket:
    """Mock socket for testing without actual network connection"""

    def __init__(self):
        self.sent_messages = []

    def send(self, data: bytes):
        """Store sent messages for verification"""
        self.sent_messages.append(data.decode('utf-8'))

    def get_last_message(self) -> str:
        """Get the last sent message"""
        return self.sent_messages[-1] if self.sent_messages else ""


class SyntheticSyncTest:
    """Test harness for synthetic clock offset measurement"""

    def __init__(self, output_dir: str = "."):
        self.output_dir = output_dir
        self.results: List[Dict] = []

    def run_sync_test(
            self,
            num_syncs: int = 10,
            clock_offset_ms: int = 50,
            network_latency_ms: float = 10.0,
            jitter_ms: float = 2.0
    ) -> List[Dict]:
        """
        Run a synthetic sync test with specified parameters.
        
        Args:
            num_syncs: Number of sync attempts to perform
            clock_offset_ms: Simulated clock offset between PC and Android (ms)
            network_latency_ms: Base network latency (ms)
            jitter_ms: Network jitter range (ms)
            
        Returns:
            List of sync results with metrics
        """
        print(f"Starting synthetic sync test with {num_syncs} sync attempts")
        print(f"Simulated clock offset: {clock_offset_ms}ms")
        print(f"Network latency: {network_latency_ms}ms +/- {jitter_ms}ms")
        print("-" * 60)

        handler = SyncHandler()
        device = SimulatedAndroidDevice(clock_offset_ms, network_latency_ms)
        results = []

        for i in range(num_syncs):
            sync_result = self._perform_single_sync(
                handler, device, i + 1, jitter_ms
            )
            results.append(sync_result)

            print(f"Sync {i + 1}/{num_syncs}: "
                  f"offset={sync_result['measured_offset_ms']}ms, "
                  f"rtt={sync_result['rtt_ms']}ms, "
                  f"error={sync_result['offset_error_ms']}ms")

            time.sleep(0.1)

        self.results = results
        return results

    def _perform_single_sync(
            self,
            handler: SyncHandler,
            device: SimulatedAndroidDevice,
            sync_index: int,
            jitter_ms: float
    ) -> Dict:
        """Perform a single sync handshake and measure metrics"""

        device_id = "simulated_device"
        mock_socket = MockSocket()

        # Step 1: Simulate SYNC_INIT from Android
        t0 = time.time()
        handler.handle_sync_init(device_id, mock_socket)

        # Extract T1 from SYNC_REQUEST message
        sync_request = mock_socket.get_last_message()
        split_parts = sync_request.split('t_pc=')
        if len(split_parts) < 2 or not split_parts[1].strip().isdigit():
            return {
                'sync_index': sync_index,
                'success': False,
                'error': f"Invalid SYNC_REQUEST format: {sync_request}"
            }
        t1 = int(split_parts[1].strip())

        # Step 2: Simulate network delay with jitter
        actual_latency = device.network_latency_ms + random.uniform(-jitter_ms, jitter_ms)
        time.sleep(actual_latency / 1000.0)

        # Step 3: Android receives SYNC_REQUEST and gets T2
        t2 = device.get_current_time_ms()

        # Step 4: Simulate network delay for response
        time.sleep(actual_latency / 1000.0)

        # Step 5: PC receives SYNC_RESPONSE and calculates offset
        sync_data = handler.handle_sync_response(device_id, t1, t2, mock_socket)

        if not sync_data:
            return {
                'sync_index': sync_index,
                'success': False,
                'error': 'Sync failed'
            }

        # Calculate error metrics
        measured_offset = sync_data['offset_ms']
        actual_offset = device.clock_offset_ms
        offset_error = abs(measured_offset - actual_offset)

        result = {
            'sync_index': sync_index,
            'timestamp_iso': datetime.now().isoformat(),
            't1_pc_send': t1,
            't2_phone_recv': t2,
            't3_pc_recv': sync_data['t3'],
            'measured_offset_ms': measured_offset,
            'rtt_ms': sync_data['rtt_ms'],
            'actual_offset_ms': actual_offset,
            'offset_error_ms': offset_error,
            'quality': sync_data['quality'],
            'success': True
        }

        return result

    def save_results_to_csv(self, filename: str = "synthetic_sync_results.csv"):
        """Save test results to CSV file"""
        filepath = os.path.join(self.output_dir, filename)

        if not self.results:
            print("No results to save")
            return

        fieldnames = [
            'sync_index', 'timestamp_iso', 't1_pc_send', 't2_phone_recv', 't3_pc_recv',
            'measured_offset_ms', 'rtt_ms', 'actual_offset_ms', 'offset_error_ms', 'quality'
        ]

        with open(filepath, 'w', newline='') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()

            for result in self.results:
                if result.get('success'):
                    writer.writerow({k: result.get(k, '') for k in fieldnames})

        print(f"\nResults saved to: {filepath}")
        return filepath

    def generate_summary_report(self):
        """Generate statistical summary of sync test results"""
        if not self.results:
            print("No results to analyze")
            return

        successful_syncs = [r for r in self.results if r.get('success')]

        if not successful_syncs:
            print("No successful syncs to analyze")
            return

        offsets = [r['measured_offset_ms'] for r in successful_syncs]
        rtts = [r['rtt_ms'] for r in successful_syncs]
        errors = [r['offset_error_ms'] for r in successful_syncs]

        print("\n" + "=" * 60)
        print("SYNTHETIC SYNC TEST SUMMARY")
        print("=" * 60)
        print(f"Total sync attempts: {len(self.results)}")
        print(f"Successful syncs: {len(successful_syncs)}")
        print(f"Success rate: {len(successful_syncs) / len(self.results) * 100:.1f}%")
        print()

        print("Measured Offset Statistics:")
        print(f"  Mean: {statistics.mean(offsets):.2f}ms")
        print(f"  Median: {statistics.median(offsets):.2f}ms")
        print(f"  Std Dev: {statistics.stdev(offsets) if len(offsets) > 1 else 0:.2f}ms")
        print(f"  Min: {min(offsets):.2f}ms")
        print(f"  Max: {max(offsets):.2f}ms")
        print()

        print("Round-Trip Time (RTT) Statistics:")
        print(f"  Mean: {statistics.mean(rtts):.2f}ms")
        print(f"  Median: {statistics.median(rtts):.2f}ms")
        print(f"  Std Dev: {statistics.stdev(rtts) if len(rtts) > 1 else 0:.2f}ms")
        print(f"  Min: {min(rtts):.2f}ms")
        print(f"  Max: {max(rtts):.2f}ms")
        print()

        print("Offset Measurement Error:")
        print(f"  Mean: {statistics.mean(errors):.2f}ms")
        print(f"  Median: {statistics.median(errors):.2f}ms")
        print(f"  Max: {max(errors):.2f}ms")
        print()

        quality_counts = {}
        for r in successful_syncs:
            quality = r['quality']
            quality_counts[quality] = quality_counts.get(quality, 0) + 1

        print("Sync Quality Distribution:")
        for quality, count in sorted(quality_counts.items()):
            percentage = (count / len(successful_syncs)) * 100
            print(f"  {quality}: {count} ({percentage:.1f}%)")
        print("=" * 60)


def main():
    """Main entry point for synthetic sync test"""
    print("Synthetic Clock Offset Measurement Test")
    print("=" * 60)

    output_dir = os.path.dirname(__file__)
    if not output_dir:
        output_dir = "."

    test = SyntheticSyncTest(output_dir)

    # Test scenario 1: Low latency, small offset
    print("\nTest Scenario 1: Low latency network (10ms), small clock offset (50ms)")
    test.run_sync_test(
        num_syncs=20,
        clock_offset_ms=50,
        network_latency_ms=10.0,
        jitter_ms=2.0
    )
    test.save_results_to_csv("synthetic_sync_low_latency.csv")
    test.generate_summary_report()

    # Test scenario 2: Higher latency, larger offset
    print("\n" + "=" * 60)
    print("\nTest Scenario 2: Higher latency network (50ms), larger clock offset (200ms)")
    test2 = SyntheticSyncTest(output_dir)
    test2.run_sync_test(
        num_syncs=20,
        clock_offset_ms=200,
        network_latency_ms=50.0,
        jitter_ms=10.0
    )
    test2.save_results_to_csv("synthetic_sync_high_latency.csv")
    test2.generate_summary_report()

    # Test scenario 3: Variable jitter
    print("\n" + "=" * 60)
    print("\nTest Scenario 3: High jitter network (25ms), moderate offset (100ms)")
    test3 = SyntheticSyncTest(output_dir)
    test3.run_sync_test(
        num_syncs=20,
        clock_offset_ms=100,
        network_latency_ms=25.0,
        jitter_ms=15.0
    )
    test3.save_results_to_csv("synthetic_sync_high_jitter.csv")
    test3.generate_summary_report()

    print("\n" + "=" * 60)
    print("All synthetic sync tests completed successfully!")
    print(f"Results saved in: {output_dir}")
    print("=" * 60)


if __name__ == "__main__":
    main()
