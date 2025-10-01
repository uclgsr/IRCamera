#!/usr/bin/env python3
"""
End-to-End Phone-PC Connection Test

This script tests the complete connection flow between a simulated Android phone
and the PC controller, verifying that they can successfully:
1. Establish TCP connection
2. Complete HELLO handshake
3. Exchange protocol messages
4. Send/receive data

This is a comprehensive integration test for the networking layer.
"""

import subprocess
import time
import sys
import os
import signal
from pathlib import Path


class PhonePCConnectionTester:
    """Manages the end-to-end connection test"""
    
    def __init__(self):
        self.repo_root = Path(__file__).parent.parent
        self.pc_controller_dir = self.repo_root / 'pc-controller'
        self.pc_controller_script = self.pc_controller_dir / 'unified_pc_controller_improved.py'
        self.phone_simulator_script = Path(__file__).parent / 'simulate_android_phone.py'
        self.pc_process = None
        self.test_port = 8080
        
    def check_dependencies(self) -> bool:
        """Check if required scripts exist"""
        if not self.pc_controller_script.exists():
            print(f"[ERROR] PC controller script not found: {self.pc_controller_script}")
            return False
        
        if not self.phone_simulator_script.exists():
            print(f"[ERROR] Phone simulator script not found: {self.phone_simulator_script}")
            return False
        
        print("[OK] All required scripts found")
        return True
    
    def start_pc_controller(self) -> bool:
        """Start the PC controller server in background"""
        try:
            print(f"\n[INFO] Starting PC controller on port {self.test_port}...")
            
            # Start PC controller in CLI mode (no GUI)
            self.pc_process = subprocess.Popen(
                [sys.executable, str(self.pc_controller_script), '--cli', '--port', str(self.test_port)],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                cwd=str(self.pc_controller_dir),
                text=True
            )
            
            # Wait for server to start
            print("[INFO] Waiting for PC controller to initialize...")
            time.sleep(3)
            
            # Check if process is still running
            if self.pc_process.poll() is not None:
                stdout, stderr = self.pc_process.communicate()
                print(f"[ERROR] PC controller failed to start")
                print(f"STDOUT: {stdout}")
                print(f"STDERR: {stderr}")
                return False
            
            print("[SUCCESS] PC controller started successfully")
            return True
            
        except Exception as e:
            print(f"[ERROR] Failed to start PC controller: {e}")
            return False
    
    def stop_pc_controller(self):
        """Stop the PC controller server"""
        if self.pc_process:
            try:
                print("\n[INFO] Stopping PC controller...")
                self.pc_process.terminate()
                
                # Wait for graceful shutdown
                try:
                    self.pc_process.wait(timeout=5)
                    print("[SUCCESS] PC controller stopped gracefully")
                except subprocess.TimeoutExpired:
                    print("[WARNING] PC controller did not stop gracefully, forcing...")
                    self.pc_process.kill()
                    self.pc_process.wait()
                    
            except Exception as e:
                print(f"[ERROR] Error stopping PC controller: {e}")
    
    def run_phone_simulator(self, quick: bool = False) -> bool:
        """Run the phone simulator and check results"""
        try:
            print(f"\n[INFO] Starting Android phone simulator...")
            
            # Build command
            cmd = [sys.executable, str(self.phone_simulator_script), '--host', 'localhost', '--port', str(self.test_port)]
            if quick:
                cmd.append('--quick')
            
            # Run phone simulator
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=30
            )
            
            # Print output
            print("\n" + "="*60)
            print("Phone Simulator Output:")
            print("="*60)
            print(result.stdout)
            
            if result.stderr:
                print("\nErrors/Warnings:")
                print(result.stderr)
            
            # Check result
            if result.returncode == 0:
                print("\n[SUCCESS] Phone simulator test PASSED")
                return True
            else:
                print(f"\n[FAILED] Phone simulator test FAILED (exit code: {result.returncode})")
                return False
                
        except subprocess.TimeoutExpired:
            print("[ERROR] Phone simulator test timed out")
            return False
        except Exception as e:
            print(f"[ERROR] Failed to run phone simulator: {e}")
            return False
    
    def run_full_test(self) -> bool:
        """Run the complete end-to-end test"""
        print("\n" + "="*70)
        print("PHONE-PC CONNECTION VERIFICATION TEST")
        print("="*70)
        print("\nThis test will:")
        print("1. Start the PC controller server")
        print("2. Simulate an Android phone connecting to it")
        print("3. Verify the connection and protocol exchange")
        print("="*70)
        
        success = False
        
        try:
            # Check dependencies
            if not self.check_dependencies():
                return False
            
            # Start PC controller
            if not self.start_pc_controller():
                return False
            
            # Run phone simulator
            success = self.run_phone_simulator(quick=False)
            
        except KeyboardInterrupt:
            print("\n[INFO] Test interrupted by user")
            success = False
        except Exception as e:
            print(f"\n[ERROR] Unexpected error: {e}")
            success = False
        finally:
            # Always stop the PC controller
            self.stop_pc_controller()
        
        # Print final result
        print("\n" + "="*70)
        if success:
            print("FINAL RESULT: TEST PASSED")
            print("The Android phone and PC controller can successfully connect!")
        else:
            print("FINAL RESULT: TEST FAILED")
            print("See errors above for details")
        print("="*70)
        
        return success
    
    def run_quick_test(self) -> bool:
        """Run a quick connection test (just HELLO handshake)"""
        print("\n" + "="*70)
        print("QUICK PHONE-PC CONNECTION TEST")
        print("="*70)
        
        success = False
        
        try:
            if not self.check_dependencies():
                return False
            
            if not self.start_pc_controller():
                return False
            
            success = self.run_phone_simulator(quick=True)
            
        except KeyboardInterrupt:
            print("\n[INFO] Test interrupted by user")
            success = False
        except Exception as e:
            print(f"\n[ERROR] Unexpected error: {e}")
            success = False
        finally:
            self.stop_pc_controller()
        
        print("\n" + "="*70)
        if success:
            print("QUICK TEST PASSED: Connection and handshake successful")
        else:
            print("QUICK TEST FAILED: See errors above")
        print("="*70)
        
        return success


def main():
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Test Android phone to PC controller connection'
    )
    parser.add_argument(
        '--quick',
        action='store_true',
        help='Run quick test (just connection and HELLO)'
    )
    
    args = parser.parse_args()
    
    tester = PhonePCConnectionTester()
    
    if args.quick:
        success = tester.run_quick_test()
    else:
        success = tester.run_full_test()
    
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
