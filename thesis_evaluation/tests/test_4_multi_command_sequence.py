#!/usr/bin/env python3
"""
Test 4: Multi-command Sequence Automation Test

This test automates a full-session scenario controlled by the PC, including
a mix of commands (e.g., START -> several SYNC -> pause -> STOP). This
ensures the system can handle realistic usage patterns.

Output: A combined timeline of events (JSON array) showing PC commands and
phone's actions over the session. Can be visualized as a timeline.

Subsystem: End-to-end system orchestration (PC-to-phone)
Chapters: Chapter 5 (comprehensive results of system operation)
          Chapter 6 (meeting orchestration requirements)
"""

import json
import logging
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List

sys.path.insert(0, str(Path(__file__).parent.parent.parent / 'pc-controller'))

from command_client import CommandClient

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class MultiCommandSequenceTest:
    """Test multi-command sequence automation"""
    
    def __init__(self, device_ip: str, port: int = 8080):
        self.device_ip = device_ip
        self.port = port
        self.client = CommandClient(timeout=15)
        self.timeline: List[Dict] = []
        self.session_start_time = None
        
    def add_timeline_event(self, event_type: str, command: str = None, 
                          response: str = None, details: Dict = None):
        """Add event to timeline"""
        timestamp = datetime.now()
        
        if self.session_start_time is None:
            self.session_start_time = timestamp
            elapsed_seconds = 0.0
        else:
            elapsed_seconds = (timestamp - self.session_start_time).total_seconds()
        
        event = {
            'timestamp_iso': timestamp.isoformat(),
            'timestamp_ms': int(timestamp.timestamp() * 1000),
            'elapsed_seconds': elapsed_seconds,
            'event_type': event_type,
            'command': command,
            'response': response,
            'details': details or {}
        }
        
        self.timeline.append(event)
        
        log_msg = f"[T+{elapsed_seconds:7.2f}s] {event_type}"
        if command:
            log_msg += f" - Command: {command}"
        if response:
            log_msg += f" - Response: {response[:50]}"
        logger.info(log_msg)
        
        return event
    
    def send_command_with_timeline(self, device_id: str, command: str, 
                                   params: Dict = None) -> str:
        """Send command and record in timeline"""
        self.add_timeline_event('Command Sent', command=command, details=params)
        
        send_time = time.time_ns()
        response = self.client.send_command(device_id, command, params)
        receive_time = time.time_ns()
        
        latency_ms = (receive_time - send_time) / 1e6
        
        self.add_timeline_event(
            'Response Received',
            command=command,
            response=response,
            details={'latency_ms': latency_ms}
        )
        
        return response
    
    def run_scenario_1_basic(self, device_id: str, duration: int = 15) -> Dict:
        """
        Scenario 1: Basic recording session with periodic sync
        
        START -> SYNC (every 3s) -> STOP
        """
        logger.info("Scenario 1: Basic recording with periodic sync")
        
        session_id = f"scenario1_{int(time.time())}"
        
        # Start recording
        self.add_timeline_event('Phase', details={'phase': 'Session Start'})
        response = self.send_command_with_timeline(device_id, 'START', {
            'session_id': session_id
        })
        
        if not response or not response.startswith('START-ACK'):
            return {'success': False, 'error': 'Failed to start recording'}
        
        # Periodic sync during recording
        self.add_timeline_event('Phase', details={'phase': 'Recording with Sync'})
        sync_count = duration // 3
        
        for i in range(sync_count):
            time.sleep(3)
            self.send_command_with_timeline(device_id, 'SYNC', {
                't_pc': int(time.time() * 1000)
            })
            
            # Also check status periodically
            if i % 2 == 0:
                self.send_command_with_timeline(device_id, 'GET_STATUS')
        
        # Stop recording
        self.add_timeline_event('Phase', details={'phase': 'Session Stop'})
        self.send_command_with_timeline(device_id, 'STOP', {
            'session_id': session_id
        })
        
        return {'success': True, 'session_id': session_id}
    
    def run_scenario_2_with_pause(self, device_id: str) -> Dict:
        """
        Scenario 2: Recording session with pause simulation
        
        START -> SYNC -> STATUS -> (pause) -> SYNC -> STATUS -> STOP
        """
        logger.info("Scenario 2: Recording with pause")
        
        session_id = f"scenario2_{int(time.time())}"
        
        # Start recording
        self.add_timeline_event('Phase', details={'phase': 'Session Start'})
        response = self.send_command_with_timeline(device_id, 'START', {
            'session_id': session_id
        })
        
        if not response or 'ACK' not in response:
            return {'success': False, 'error': 'Failed to start recording'}
        
        # Active recording period
        self.add_timeline_event('Phase', details={'phase': 'Active Recording'})
        time.sleep(2)
        self.send_command_with_timeline(device_id, 'SYNC', {
            't_pc': int(time.time() * 1000)
        })
        time.sleep(2)
        self.send_command_with_timeline(device_id, 'GET_STATUS')
        
        # Pause simulation (no commands sent)
        self.add_timeline_event('Phase', details={'phase': 'Pause (no commands)', 'duration_s': 5})
        logger.info("Pausing command sending for 5 seconds...")
        time.sleep(5)
        
        # Resume activity
        self.add_timeline_event('Phase', details={'phase': 'Resume Activity'})
        self.send_command_with_timeline(device_id, 'SYNC', {
            't_pc': int(time.time() * 1000)
        })
        time.sleep(2)
        self.send_command_with_timeline(device_id, 'GET_STATUS')
        
        # Stop recording
        self.add_timeline_event('Phase', details={'phase': 'Session Stop'})
        time.sleep(1)
        self.send_command_with_timeline(device_id, 'STOP', {
            'session_id': session_id
        })
        
        return {'success': True, 'session_id': session_id}
    
    def run_scenario_3_intensive(self, device_id: str) -> Dict:
        """
        Scenario 3: Intensive command sequence
        
        Multiple syncs, status checks, and commands in rapid succession
        """
        logger.info("Scenario 3: Intensive command sequence")
        
        session_id = f"scenario3_{int(time.time())}"
        
        # Start recording
        self.add_timeline_event('Phase', details={'phase': 'Session Start'})
        response = self.send_command_with_timeline(device_id, 'START', {
            'session_id': session_id
        })
        
        if not response or 'ACK' not in response:
            return {'success': False, 'error': 'Failed to start recording'}
        
        # Intensive command period
        self.add_timeline_event('Phase', details={'phase': 'Intensive Commands'})
        
        for i in range(8):
            time.sleep(0.5)
            
            # Send PING
            self.send_command_with_timeline(device_id, 'PING')
            
            # Every other iteration, send SYNC
            if i % 2 == 0:
                time.sleep(0.3)
                self.send_command_with_timeline(device_id, 'SYNC', {
                    't_pc': int(time.time() * 1000)
                })
            
            # Every third iteration, check status
            if i % 3 == 0:
                time.sleep(0.3)
                self.send_command_with_timeline(device_id, 'GET_STATUS')
        
        # Calm period
        self.add_timeline_event('Phase', details={'phase': 'Calm Period'})
        time.sleep(3)
        
        # Final sync before stop
        self.send_command_with_timeline(device_id, 'SYNC', {
            't_pc': int(time.time() * 1000)
        })
        
        # Stop recording
        self.add_timeline_event('Phase', details={'phase': 'Session Stop'})
        time.sleep(1)
        self.send_command_with_timeline(device_id, 'STOP', {
            'session_id': session_id
        })
        
        return {'success': True, 'session_id': session_id}
    
    def run_scenario_4_full_workflow(self, device_id: str) -> Dict:
        """
        Scenario 4: Complete workflow simulation
        
        Connection -> Status check -> START -> Multiple operations -> STOP
        """
        logger.info("Scenario 4: Complete workflow simulation")
        
        # Initial status check
        self.add_timeline_event('Phase', details={'phase': 'Pre-flight Checks'})
        self.send_command_with_timeline(device_id, 'GET_STATUS')
        time.sleep(1)
        
        # Sync before starting
        self.send_command_with_timeline(device_id, 'SYNC', {
            't_pc': int(time.time() * 1000)
        })
        time.sleep(1)
        
        session_id = f"scenario4_{int(time.time())}"
        
        # Start recording
        self.add_timeline_event('Phase', details={'phase': 'Session Start'})
        response = self.send_command_with_timeline(device_id, 'START', {
            'session_id': session_id
        })
        
        if not response or 'ACK' not in response:
            return {'success': False, 'error': 'Failed to start recording'}
        
        # Early recording phase
        self.add_timeline_event('Phase', details={'phase': 'Early Recording'})
        time.sleep(2)
        self.send_command_with_timeline(device_id, 'GET_STATUS')
        time.sleep(2)
        self.send_command_with_timeline(device_id, 'SYNC', {
            't_pc': int(time.time() * 1000)
        })
        
        # Mid recording phase
        self.add_timeline_event('Phase', details={'phase': 'Mid Recording'})
        time.sleep(3)
        self.send_command_with_timeline(device_id, 'PING')
        time.sleep(2)
        self.send_command_with_timeline(device_id, 'GET_STATUS')
        time.sleep(2)
        self.send_command_with_timeline(device_id, 'SYNC', {
            't_pc': int(time.time() * 1000)
        })
        
        # Late recording phase
        self.add_timeline_event('Phase', details={'phase': 'Late Recording'})
        time.sleep(3)
        self.send_command_with_timeline(device_id, 'GET_STATUS')
        time.sleep(2)
        
        # Pre-stop sync
        self.add_timeline_event('Phase', details={'phase': 'Pre-Stop Sync'})
        self.send_command_with_timeline(device_id, 'SYNC', {
            't_pc': int(time.time() * 1000)
        })
        time.sleep(1)
        
        # Stop recording
        self.add_timeline_event('Phase', details={'phase': 'Session Stop'})
        self.send_command_with_timeline(device_id, 'STOP', {
            'session_id': session_id
        })
        
        # Post-stop status check
        self.add_timeline_event('Phase', details={'phase': 'Post-Stop Check'})
        time.sleep(1)
        self.send_command_with_timeline(device_id, 'GET_STATUS')
        
        return {'success': True, 'session_id': session_id}
    
    def run_test(self, scenario: str = 'all') -> Dict:
        """Execute multi-command sequence test"""
        logger.info("=" * 80)
        logger.info("TEST 4: Multi-command Sequence Automation Test")
        logger.info("=" * 80)
        
        test_start_time = datetime.now()
        self.session_start_time = test_start_time
        
        self.add_timeline_event('Test Started', details={
            'device_ip': self.device_ip,
            'port': self.port,
            'scenario': scenario
        })
        
        # Connect to device
        logger.info(f"\nConnecting to device {self.device_ip}:{self.port}")
        if not self.client.connect_to_device(self.device_ip, self.port):
            return self._generate_report(
                success=False,
                error=f"Could not connect to {self.device_ip}:{self.port}"
            )
        
        device_id = f"{self.device_ip}:{self.port}"
        self.add_timeline_event('Connected', details={'device_id': device_id})
        
        # Run scenarios
        scenario_results = []
        
        if scenario == 'all' or scenario == '1':
            logger.info("\n" + "=" * 80)
            result = self.run_scenario_1_basic(device_id, duration=15)
            scenario_results.append({
                'scenario': 'Scenario 1: Basic recording with periodic sync',
                'result': result
            })
            time.sleep(2)
        
        if scenario == 'all' or scenario == '2':
            logger.info("\n" + "=" * 80)
            result = self.run_scenario_2_with_pause(device_id)
            scenario_results.append({
                'scenario': 'Scenario 2: Recording with pause',
                'result': result
            })
            time.sleep(2)
        
        if scenario == 'all' or scenario == '3':
            logger.info("\n" + "=" * 80)
            result = self.run_scenario_3_intensive(device_id)
            scenario_results.append({
                'scenario': 'Scenario 3: Intensive command sequence',
                'result': result
            })
            time.sleep(2)
        
        if scenario == 'all' or scenario == '4':
            logger.info("\n" + "=" * 80)
            result = self.run_scenario_4_full_workflow(device_id)
            scenario_results.append({
                'scenario': 'Scenario 4: Complete workflow',
                'result': result
            })
        
        # Disconnect
        self.client.disconnect_device(device_id)
        self.add_timeline_event('Disconnected', details={'device_id': device_id})
        
        test_end_time = datetime.now()
        total_duration = (test_end_time - test_start_time).total_seconds()
        
        self.add_timeline_event('Test Completed', details={
            'total_duration_seconds': total_duration
        })
        
        return self._generate_report(
            success=True,
            scenario_results=scenario_results,
            total_duration=total_duration
        )
    
    def _generate_report(self, success: bool, error: str = None,
                        scenario_results: List[Dict] = None,
                        total_duration: float = None) -> Dict:
        """Generate test report"""
        report = {
            'test_name': 'Multi-command Sequence Automation Test',
            'test_id': 'test_4',
            'timestamp': datetime.now().isoformat(),
            'success': success,
            'timeline': self.timeline,
            'summary': self._generate_summary()
        }
        
        if error:
            report['error'] = error
        
        if scenario_results:
            report['scenario_results'] = scenario_results
        
        if total_duration:
            report['total_duration_seconds'] = total_duration
        
        return report
    
    def _generate_summary(self) -> Dict:
        """Generate summary from timeline"""
        summary = {
            'total_events': len(self.timeline),
            'events_by_type': {},
            'commands_by_type': {}
        }
        
        for event in self.timeline:
            event_type = event['event_type']
            summary['events_by_type'][event_type] = \
                summary['events_by_type'].get(event_type, 0) + 1
            
            if event['command']:
                cmd = event['command']
                summary['commands_by_type'][cmd] = \
                    summary['commands_by_type'].get(cmd, 0) + 1
        
        return summary
    
    def save_report(self, report: Dict, output_file: str = None):
        """Save test report to JSON file"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"test_4_multi_sequence_report_{timestamp}.json"
        
        output_path = Path(__file__).parent / output_file
        
        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        logger.info(f"\nReport saved to: {output_path}")
        
        # Create timeline visualization file
        timeline_file = output_path.with_name(
            output_path.stem + '_timeline.txt'
        )
        
        with open(timeline_file, 'w') as f:
            f.write("=" * 100 + "\n")
            f.write("TEST 4: Multi-command Sequence Automation - Timeline\n")
            f.write("=" * 100 + "\n\n")
            
            f.write(f"{'Time (s)':<10} {'Event Type':<25} {'Command':<15} {'Response':<30} {'Details'}\n")
            f.write("-" * 100 + "\n")
            
            for event in report['timeline']:
                f.write(f"{event['elapsed_seconds']:<10.2f} ")
                f.write(f"{event['event_type']:<25} ")
                f.write(f"{event['command'] or '-':<15} ")
                response = event['response'][:28] if event['response'] else '-'
                f.write(f"{response:<30} ")
                
                if event['details']:
                    details_str = json.dumps(event['details'])[:30]
                    f.write(details_str)
                
                f.write("\n")
            
            f.write("-" * 100 + "\n\n")
            
            # Summary
            f.write("Summary:\n")
            f.write(json.dumps(report['summary'], indent=2))
            f.write("\n\n")
            
            # Scenario results
            if 'scenario_results' in report:
                f.write("Scenario Results:\n")
                f.write("-" * 100 + "\n")
                for sr in report['scenario_results']:
                    f.write(f"\n{sr['scenario']}\n")
                    f.write(f"  Success: {sr['result'].get('success', False)}\n")
                    if 'session_id' in sr['result']:
                        f.write(f"  Session ID: {sr['result']['session_id']}\n")
                    if 'error' in sr['result']:
                        f.write(f"  Error: {sr['result']['error']}\n")
        
        logger.info(f"Timeline visualization saved to: {timeline_file}")


def main():
    """Main entry point for test"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Test 4: Multi-command Sequence Automation Test'
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
        '--scenario',
        choices=['all', '1', '2', '3', '4'],
        default='all',
        help='Which scenario to run (default: all)'
    )
    parser.add_argument(
        '--output',
        help='Output file name for report (optional)'
    )
    
    args = parser.parse_args()
    
    # Run test
    test = MultiCommandSequenceTest(args.device_ip, args.port)
    report = test.run_test(scenario=args.scenario)
    
    # Save report
    test.save_report(report, args.output)
    
    # Print summary
    logger.info("\n" + "=" * 80)
    logger.info("TEST RESULTS")
    logger.info("=" * 80)
    logger.info(f"Success: {report['success']}")
    
    if report['success']:
        summary = report['summary']
        logger.info(f"Total Timeline Events: {summary['total_events']}")
        logger.info(f"\nCommands Sent:")
        for cmd, count in summary['commands_by_type'].items():
            logger.info(f"  {cmd}: {count}")
        
        if 'scenario_results' in report:
            logger.info(f"\nScenarios Executed: {len(report['scenario_results'])}")
            for sr in report['scenario_results']:
                success = sr['result'].get('success', False)
                status = 'SUCCESS' if success else 'FAILED'
                logger.info(f"  {sr['scenario']}: {status}")
    else:
        logger.error(f"Error: {report.get('error', 'Unknown error')}")
        sys.exit(1)
    
    logger.info("\nTest completed successfully!")


if __name__ == "__main__":
    main()
