#!/usr/bin/env python3
"""
Enhanced Phone-PC Connection Test Suite

Comprehensive test suite with multiple scenarios:
- Basic connection and handshake
- Multiple simultaneous connections
- Protocol message exchange
- Error handling and recovery
- Performance testing
"""

import subprocess
import time
import sys
import os
import socket
import threading
from pathlib import Path
from typing import List, Tuple


class EnhancedConnectionTester:
    """Enhanced test suite for phone-PC connection"""
    
    def __init__(self):
        self.repo_root = Path(__file__).parent.parent
        self.pc_controller_dir = self.repo_root / 'pc-controller'
        self.pc_controller_script = self.pc_controller_dir / 'unified_pc_controller_improved.py'
        self.phone_simulator_script = Path(__file__).parent / 'simulate_android_phone.py'
        self.pc_process = None
        self.test_port = 8888
        self.results = []
        
    def start_pc_controller(self, port: int = None) -> bool:
        """Start PC controller server"""
        port = port or self.test_port
        try:
            print(f"\n[Starting PC controller on port {port}...]")
            self.pc_process = subprocess.Popen(
                [sys.executable, str(self.pc_controller_script), '--cli', '--port', str(port)],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                cwd=str(self.pc_controller_dir),
                text=True
            )
            time.sleep(3)
            
            if self.pc_process.poll() is not None:
                stdout, stderr = self.pc_process.communicate()
                print(f"[FAILED] PC controller failed to start")
                print(f"Error: {stderr}")
                return False
            
            print(f"[OK] PC controller started")
            return True
        except Exception as e:
            print(f"[ERROR] Failed to start PC controller: {e}")
            return False
    
    def stop_pc_controller(self):
        """Stop PC controller server"""
        if self.pc_process:
            try:
                self.pc_process.terminate()
                try:
                    self.pc_process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    self.pc_process.kill()
                    self.pc_process.wait()
            except Exception as e:
                print(f"[WARNING] Error stopping PC controller: {e}")
    
    def run_phone_simulator(self, args: List[str] = None) -> Tuple[bool, str]:
        """Run phone simulator with given arguments"""
        try:
            cmd = [sys.executable, str(self.phone_simulator_script)]
            if args:
                cmd.extend(args)
            
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=30
            )
            
            return (result.returncode == 0, result.stdout + result.stderr)
        except subprocess.TimeoutExpired:
            return (False, "Test timed out")
        except Exception as e:
            return (False, f"Error: {e}")
    
    def test_basic_connection(self) -> bool:
        """Test 1: Basic connection and handshake"""
        print("\n" + "="*70)
        print("TEST 1: Basic Connection and HELLO Handshake")
        print("="*70)
        
        success, output = self.run_phone_simulator([
            '--host', 'localhost',
            '--port', str(self.test_port),
            '--quick'
        ])
        
        if success:
            print("[PASS] Basic connection test")
            self.results.append(("Basic Connection", True, ""))
            return True
        else:
            print("[FAIL] Basic connection test")
            print(output)
            self.results.append(("Basic Connection", False, output))
            return False
    
    def test_full_protocol(self) -> bool:
        """Test 2: Full protocol exchange"""
        print("\n" + "="*70)
        print("TEST 2: Full Protocol Exchange (with recording commands)")
        print("="*70)
        
        success, output = self.run_phone_simulator([
            '--host', 'localhost',
            '--port', str(self.test_port)
        ])
        
        if success:
            print("[PASS] Full protocol test")
            self.results.append(("Full Protocol", True, ""))
            return True
        else:
            print("[FAIL] Full protocol test")
            print(output)
            self.results.append(("Full Protocol", False, output))
            return False
    
    def test_multiple_connections(self) -> bool:
        """Test 3: Multiple simultaneous connections"""
        print("\n" + "="*70)
        print("TEST 3: Multiple Simultaneous Connections")
        print("="*70)
        
        num_connections = 3
        threads = []
        results = []
        
        def run_client(client_id):
            success, output = self.run_phone_simulator([
                '--host', 'localhost',
                '--port', str(self.test_port),
                '--device-id', f'test_device_{client_id}',
                '--quick'
            ])
            results.append((client_id, success, output))
        
        # Start multiple clients
        print(f"[INFO] Starting {num_connections} concurrent connections...")
        for i in range(num_connections):
            t = threading.Thread(target=run_client, args=(i,))
            t.start()
            threads.append(t)
            time.sleep(0.5)  # Stagger connections slightly
        
        # Wait for all to complete
        for t in threads:
            t.join(timeout=30)
        
        # Check results
        all_success = all(success for _, success, _ in results)
        
        if all_success:
            print(f"[PASS] All {num_connections} connections succeeded")
            self.results.append(("Multiple Connections", True, ""))
            return True
        else:
            failed = [cid for cid, success, _ in results if not success]
            print(f"[FAIL] Some connections failed: {failed}")
            self.results.append(("Multiple Connections", False, f"Failed: {failed}"))
            return False
    
    def test_reconnection(self) -> bool:
        """Test 4: Reconnection after disconnect"""
        print("\n" + "="*70)
        print("TEST 4: Reconnection After Disconnect")
        print("="*70)
        
        # First connection
        print("[INFO] First connection...")
        success1, _ = self.run_phone_simulator([
            '--host', 'localhost',
            '--port', str(self.test_port),
            '--device-id', 'reconnect_test',
            '--quick'
        ])
        
        if not success1:
            print("[FAIL] First connection failed")
            self.results.append(("Reconnection", False, "First connection failed"))
            return False
        
        time.sleep(1)
        
        # Second connection (reconnect)
        print("[INFO] Reconnecting...")
        success2, _ = self.run_phone_simulator([
            '--host', 'localhost',
            '--port', str(self.test_port),
            '--device-id', 'reconnect_test',
            '--quick'
        ])
        
        if success2:
            print("[PASS] Reconnection successful")
            self.results.append(("Reconnection", True, ""))
            return True
        else:
            print("[FAIL] Reconnection failed")
            self.results.append(("Reconnection", False, "Second connection failed"))
            return False
    
    def test_connection_timeout(self) -> bool:
        """Test 5: Connection timeout handling"""
        print("\n" + "="*70)
        print("TEST 5: Connection Timeout (No Server)")
        print("="*70)
        
        # Stop server temporarily
        self.stop_pc_controller()
        time.sleep(1)
        
        # Try to connect (should fail)
        print("[INFO] Attempting connection to non-existent server...")
        success, output = self.run_phone_simulator([
            '--host', 'localhost',
            '--port', str(self.test_port),
            '--quick'
        ])
        
        # Restart server
        print("[INFO] Restarting server...")
        self.start_pc_controller(self.test_port)
        
        if not success:
            print("[PASS] Correctly handled connection failure")
            self.results.append(("Connection Timeout", True, ""))
            return True
        else:
            print("[FAIL] Should have failed to connect")
            self.results.append(("Connection Timeout", False, "Connected when shouldn't"))
            return False
    
    def test_rapid_connections(self) -> bool:
        """Test 6: Rapid sequential connections"""
        print("\n" + "="*70)
        print("TEST 6: Rapid Sequential Connections")
        print("="*70)
        
        num_rapid = 5
        print(f"[INFO] Running {num_rapid} rapid sequential connections...")
        
        all_success = True
        for i in range(num_rapid):
            success, _ = self.run_phone_simulator([
                '--host', 'localhost',
                '--port', str(self.test_port),
                '--device-id', f'rapid_{i}',
                '--quick'
            ])
            if not success:
                all_success = False
                print(f"[FAIL] Connection {i+1} failed")
                break
            time.sleep(0.1)  # Very short delay
        
        if all_success:
            print(f"[PASS] All {num_rapid} rapid connections succeeded")
            self.results.append(("Rapid Connections", True, ""))
            return True
        else:
            print("[FAIL] Some rapid connections failed")
            self.results.append(("Rapid Connections", False, ""))
            return False
    
    def print_summary(self):
        """Print test summary"""
        print("\n" + "="*70)
        print("TEST SUMMARY")
        print("="*70)
        
        total = len(self.results)
        passed = sum(1 for _, success, _ in self.results if success)
        failed = total - passed
        
        print(f"\nTotal Tests: {total}")
        print(f"Passed: {passed}")
        print(f"Failed: {failed}")
        print()
        
        for test_name, success, error in self.results:
            status = "[PASS]" if success else "[FAIL]"
            print(f"{status} {test_name}")
            if error:
                print(f"      Error: {error[:100]}")
        
        print("\n" + "="*70)
        if failed == 0:
            print("RESULT: ALL TESTS PASSED")
        else:
            print(f"RESULT: {failed} TEST(S) FAILED")
        print("="*70)
        
        return failed == 0
    
    def run_all_tests(self) -> bool:
        """Run all tests"""
        print("\n" + "="*70)
        print("ENHANCED PHONE-PC CONNECTION TEST SUITE")
        print("="*70)
        
        # Start server
        if not self.start_pc_controller(self.test_port):
            print("[FATAL] Could not start PC controller")
            return False
        
        try:
            # Run all tests
            self.test_basic_connection()
            self.test_full_protocol()
            self.test_multiple_connections()
            self.test_reconnection()
            self.test_connection_timeout()
            self.test_rapid_connections()
            
        except KeyboardInterrupt:
            print("\n[INFO] Tests interrupted by user")
        except Exception as e:
            print(f"\n[ERROR] Unexpected error: {e}")
        finally:
            self.stop_pc_controller()
        
        # Print summary
        return self.print_summary()


def main():
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Enhanced phone-PC connection test suite'
    )
    parser.add_argument(
        '--test',
        type=int,
        help='Run specific test number (1-6)'
    )
    
    args = parser.parse_args()
    
    tester = EnhancedConnectionTester()
    
    if args.test:
        # Run specific test
        tester.start_pc_controller(tester.test_port)
        try:
            if args.test == 1:
                success = tester.test_basic_connection()
            elif args.test == 2:
                success = tester.test_full_protocol()
            elif args.test == 3:
                success = tester.test_multiple_connections()
            elif args.test == 4:
                success = tester.test_reconnection()
            elif args.test == 5:
                success = tester.test_connection_timeout()
            elif args.test == 6:
                success = tester.test_rapid_connections()
            else:
                print(f"[ERROR] Invalid test number: {args.test}")
                success = False
        finally:
            tester.stop_pc_controller()
        
        tester.print_summary()
    else:
        # Run all tests
        success = tester.run_all_tests()
    
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
