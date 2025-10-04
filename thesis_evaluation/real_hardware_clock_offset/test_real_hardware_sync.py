#!/usr/bin/env python3
"""
Real Hardware Clock Offset Measurement Test

This test performs actual sync protocol with the PC and Android phone after hardware
integration. It triggers SYNC commands during a live session and logs timestamps from
both PC and phone to determine actual clock offset and drift over time.

Test Type: Real Hardware Integration
Output: Timestamp log (CSV and JSON) of PC vs phone times for each sync event
Subsystem: Time sync/clock alignment
Thesis Chapters: Chapter 5 (real-world sync precision), Chapter 6 (temporal accuracy)
"""

import sys
import os
import time
import csv
import json
import socket
import threading
from datetime import datetime
from typing import List, Dict, Optional

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', 'pc-controller'))

from sync_handler import SyncHandler
from protocol_adapter import ProtocolAdapter


class RealHardwareSyncTest:
    """Test harness for real hardware clock offset measurement"""
    
    def __init__(self, output_dir: str = ".", port: int = 8080):
        self.output_dir = output_dir
        self.port = port
        self.sync_handler = SyncHandler()
        self.protocol = ProtocolAdapter()
        self.results: List[Dict] = []
        self.server_socket: Optional[socket.socket] = None
        self.running = False
        self.client_socket: Optional[socket.socket] = None
        self.client_address = None
        
    def start_server(self):
        """Start PC server to accept Android connection"""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind(('0.0.0.0', self.port))
        self.server_socket.listen(1)
        self.running = True
        
        print(f"Real hardware sync test server started on port {self.port}")
        print("Waiting for Android device connection...")
        print("To connect from Android:")
        print(f"  1. Open the IRCamera app")
        print(f"  2. Go to Network settings")
        print(f"  3. Enter PC IP address and port {self.port}")
        print(f"  4. Connect to PC")
        print("-" * 60)
        
        try:
            self.client_socket, self.client_address = self.server_socket.accept()
            print(f"Android device connected from {self.client_address}")
            return True
        except Exception as e:
            print(f"Error accepting connection: {e}")
            return False
    
    def stop_server(self):
        """Stop the server and close connections"""
        self.running = False
        
        if self.client_socket:
            try:
                self.client_socket.close()
            except:
                pass
        
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
        
        print("\nServer stopped")
    
    def send_sync_init_trigger(self) -> bool:
        """
        Trigger a SYNC_INIT from PC side.
        
        Note: In the actual protocol, SYNC_INIT comes from Android.
        This is for testing purposes to manually trigger sync.
        """
        if not self.client_socket:
            print("No client connected")
            return False
        
        try:
            message = "TRIGGER_SYNC\n"
            self.client_socket.send(message.encode('utf-8'))
            print(f"Sent TRIGGER_SYNC to Android device")
            return True
        except Exception as e:
            print(f"Error sending trigger: {e}")
            return False
    
    def handle_client_messages(self, timeout: float = 60.0):
        """
        Handle messages from connected Android device.
        
        Args:
            timeout: Timeout in seconds for receiving messages
        """
        if not self.client_socket:
            print("No client connected")
            return
        
        device_id = f"{self.client_address[0]}:{self.client_address[1]}"
        
        try:
            self.client_socket.settimeout(timeout)
            
            while self.running:
                data = self.client_socket.recv(4096)
                if not data:
                    print("Client disconnected")
                    break
                
                message = data.decode('utf-8').strip()
                print(f"Received: {message}")
                
                parsed = self.protocol.parse_android_message(message)
                if not parsed:
                    print(f"Failed to parse message: {message}")
                    continue
                
                self._handle_sync_message(device_id, parsed)
                
        except socket.timeout:
            print("Client timeout")
        except Exception as e:
            print(f"Error handling messages: {e}")
    
    def _handle_sync_message(self, device_id: str, message):
        """Handle sync-related messages and log results"""
        msg_type = message.type
        params = message.parameters
        
        if msg_type == 'SYNC_INIT':
            print(f"SYNC_INIT from {device_id}")
            self.sync_handler.handle_sync_init(device_id, self.client_socket)
            
        elif msg_type == 'SYNC_RESPONSE':
            try:
                t_pc = int(params.get('t_pc', 0))
                t_ph = int(params.get('t_ph', 0))
                
                print(f"SYNC_RESPONSE: t_pc={t_pc}, t_ph={t_ph}")
                
                sync_result = self.sync_handler.handle_sync_response(
                    device_id, t_pc, t_ph, self.client_socket
                )
                
                if sync_result:
                    self._log_sync_result(sync_result, device_id)
                    
            except ValueError as e:
                print(f"Invalid timestamp format: {e}")
    
    def _log_sync_result(self, sync_result: Dict, device_id: str):
        """Log sync result to results list"""
        result = {
            'sync_index': len(self.results) + 1,
            'timestamp_iso': datetime.now().isoformat(),
            'device_id': device_id,
            't1_pc_send': sync_result['t1'],
            't2_phone_recv': sync_result['t2'],
            't3_pc_recv': sync_result['t3'],
            'offset_ms': sync_result['offset_ms'],
            'rtt_ms': sync_result['rtt_ms'],
            'quality': sync_result['quality']
        }
        
        self.results.append(result)
        
        print(f"Sync completed: offset={result['offset_ms']}ms, "
              f"rtt={result['rtt_ms']}ms, quality={result['quality']}")
    
    def run_periodic_sync_test(
        self,
        num_syncs: int = 10,
        interval_seconds: float = 30.0
    ):
        """
        Run periodic sync tests with real hardware.
        
        Args:
            num_syncs: Number of sync attempts
            interval_seconds: Time between syncs
        """
        print(f"\nStarting periodic sync test with {num_syncs} syncs")
        print(f"Interval: {interval_seconds} seconds")
        print("-" * 60)
        
        if not self.start_server():
            return False
        
        # Start message handler in background thread
        handler_thread = threading.Thread(
            target=self.handle_client_messages,
            args=(interval_seconds * num_syncs + 60,),
            daemon=True
        )
        handler_thread.start()
        
        # Wait for initial handshake
        time.sleep(2)
        
        # Trigger periodic syncs
        for i in range(num_syncs):
            print(f"\nTriggering sync {i+1}/{num_syncs}")
            
            # Wait for Android to send SYNC_INIT
            # In real scenario, Android sends SYNC_INIT at session start
            # and periodically during recording
            
            if i < num_syncs - 1:
                time.sleep(interval_seconds)
        
        # Wait for final messages
        time.sleep(5)
        
        return True
    
    def save_results_to_csv(self, filename: str = "real_hardware_sync_results.csv"):
        """Save test results to CSV file"""
        filepath = os.path.join(self.output_dir, filename)
        
        if not self.results:
            print("No results to save")
            return None
        
        fieldnames = [
            'sync_index', 'timestamp_iso', 'device_id',
            't1_pc_send', 't2_phone_recv', 't3_pc_recv',
            'offset_ms', 'rtt_ms', 'quality'
        ]
        
        with open(filepath, 'w', newline='') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(self.results)
        
        print(f"\nResults saved to CSV: {filepath}")
        return filepath
    
    def save_results_to_json(self, filename: str = "real_hardware_sync_results.json"):
        """Save test results to JSON file"""
        filepath = os.path.join(self.output_dir, filename)
        
        if not self.results:
            print("No results to save")
            return None
        
        with open(filepath, 'w') as jsonfile:
            json.dump({
                'test_type': 'real_hardware_clock_offset',
                'timestamp': datetime.now().isoformat(),
                'num_syncs': len(self.results),
                'results': self.results
            }, jsonfile, indent=2)
        
        print(f"Results saved to JSON: {filepath}")
        return filepath
    
    def generate_summary_report(self):
        """Generate statistical summary of real hardware sync test"""
        if not self.results:
            print("No results to analyze")
            return
        
        import statistics
        
        offsets = [r['offset_ms'] for r in self.results]
        rtts = [r['rtt_ms'] for r in self.results]
        
        print("\n" + "=" * 60)
        print("REAL HARDWARE SYNC TEST SUMMARY")
        print("=" * 60)
        print(f"Total syncs completed: {len(self.results)}")
        print()
        
        print("Clock Offset Statistics:")
        print(f"  Mean: {statistics.mean(offsets):.2f}ms")
        print(f"  Median: {statistics.median(offsets):.2f}ms")
        print(f"  Std Dev: {statistics.stdev(offsets) if len(offsets) > 1 else 0:.2f}ms")
        print(f"  Min: {min(offsets):.2f}ms")
        print(f"  Max: {max(offsets):.2f}ms")
        print(f"  Range: {max(offsets) - min(offsets):.2f}ms")
        print()
        
        print("Round-Trip Time (RTT) Statistics:")
        print(f"  Mean: {statistics.mean(rtts):.2f}ms")
        print(f"  Median: {statistics.median(rtts):.2f}ms")
        print(f"  Std Dev: {statistics.stdev(rtts) if len(rtts) > 1 else 0:.2f}ms")
        print(f"  Min: {min(rtts):.2f}ms")
        print(f"  Max: {max(rtts):.2f}ms")
        print()
        
        # Analyze drift over time
        if len(self.results) > 1:
            first_offset = offsets[0]
            last_offset = offsets[-1]
            drift = last_offset - first_offset
            
            print("Clock Drift Analysis:")
            print(f"  Initial offset: {first_offset}ms")
            print(f"  Final offset: {last_offset}ms")
            print(f"  Total drift: {drift}ms")
            
            if len(self.results) > 2:
                first_time = datetime.fromisoformat(self.results[0]['timestamp_iso'])
                last_time = datetime.fromisoformat(self.results[-1]['timestamp_iso'])
                duration = (last_time - first_time).total_seconds()
                drift_rate = (drift / duration) if duration > 0 else 0
                print(f"  Test duration: {duration:.1f}s")
                print(f"  Drift rate: {drift_rate:.3f}ms/s")
            print()
        
        quality_counts = {}
        for r in self.results:
            quality = r['quality']
            quality_counts[quality] = quality_counts.get(quality, 0) + 1
        
        print("Sync Quality Distribution:")
        for quality, count in sorted(quality_counts.items()):
            percentage = (count / len(self.results)) * 100
            print(f"  {quality}: {count} ({percentage:.1f}%)")
        print("=" * 60)


def main():
    """Main entry point for real hardware sync test"""
    print("Real Hardware Clock Offset Measurement Test")
    print("=" * 60)
    print()
    print("IMPORTANT: This test requires a real Android device running IRCamera app")
    print()
    print("Setup Instructions:")
    print("1. Ensure PC and Android are on the same network")
    print("2. Note the PC IP address (use `ip addr` or `ifconfig`)")
    print("3. Start the IRCamera app on Android")
    print("4. Configure network settings in app to connect to this PC")
    print()
    print("The test will:")
    print("- Accept connection from Android device")
    print("- Wait for SYNC_INIT messages from Android")
    print("- Perform time synchronization protocol")
    print("- Log all sync results with timestamps")
    print("- Measure clock offset and drift over time")
    print()
    
    output_dir = os.path.dirname(__file__)
    if not output_dir:
        output_dir = "."
    
    port = 8080
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print(f"Invalid port: {sys.argv[1]}, using default 8080")
    
    test = RealHardwareSyncTest(output_dir, port)
    
    try:
        # Run the test
        print(f"Starting server on port {port}...")
        print("Press Ctrl+C to stop the test and save results")
        print()
        
        if not test.start_server():
            print("Failed to start server")
            return
        
        # Keep handling messages until interrupted
        test.handle_client_messages(timeout=300.0)
        
    except KeyboardInterrupt:
        print("\n\nTest interrupted by user")
    finally:
        test.stop_server()
        
        # Save results if any were collected
        if test.results:
            test.save_results_to_csv()
            test.save_results_to_json()
            test.generate_summary_report()
        else:
            print("\nNo sync results collected")
            print("Make sure Android device sent SYNC_INIT messages")


if __name__ == "__main__":
    main()
