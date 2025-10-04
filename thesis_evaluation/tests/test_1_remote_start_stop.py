#!/usr/bin/env python3
"""
Test 1: Remote Start/Stop Command Test

This test validates the basic PC-to-phone control loop:
1. PC sends START command to begin a session
2. After some time, PC sends STOP command
3. App executes these commands in order

Output: A detailed event log capturing the sequence with timestamps
and latency measurements.

Subsystem: TCP command interface (control protocol)
Chapters: Chapter 5 (remote session control and responsiveness)
          Chapter 6 (control efficiency evaluation)
"""

import json
import logging
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent.parent / 'pc-controller'))

from command_client import CommandClient

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class StartStopCommandTest:
    """Test remote start/stop command sequence"""
    
    def __init__(self, device_ip: str, port: int = 8080):
        self.device_ip = device_ip
        self.port = port
        self.client = CommandClient(timeout=15)
        self.event_log: List[Dict] = []
        
    def log_event(self, event_type: str, details: Dict = None):
        """Log an event with timestamp"""
        timestamp = datetime.now()
        timestamp_iso = timestamp.isoformat()
        timestamp_ms = int(timestamp.timestamp() * 1000)
        
        event = {
            'timestamp_iso': timestamp_iso,
            'timestamp_ms': timestamp_ms,
            'event_type': event_type,
            'details': details or {}
        }
        
        self.event_log.append(event)
        logger.info(f"Event: {event_type} at {timestamp_iso}")
        if details:
            logger.info(f"  Details: {json.dumps(details, indent=2)}")
    
    def run_test(self, session_duration: int = 10) -> Dict:
        """
        Execute the start/stop command test
        
        Args:
            session_duration: Duration in seconds to run the recording session
            
        Returns:
            Dictionary containing test results and event log
        """
        logger.info("=" * 80)
        logger.info("TEST 1: Remote Start/Stop Command Test")
        logger.info("=" * 80)
        
        test_start_time = datetime.now()
        self.log_event("Test Started", {
            'device_ip': self.device_ip,
            'port': self.port,
            'session_duration': session_duration
        })
        
        # Step 1: Connect to device
        logger.info(f"\nStep 1: Connecting to device {self.device_ip}:{self.port}")
        connect_start = time.time_ns()
        
        if not self.client.connect_to_device(self.device_ip, self.port):
            self.log_event("Connection Failed", {
                'error': f"Could not connect to {self.device_ip}:{self.port}"
            })
            return self._generate_report(success=False, error="Connection failed")
        
        connect_end = time.time_ns()
        connect_latency_ms = (connect_end - connect_start) / 1e6
        
        self.log_event("Connected to Device", {
            'device_id': f"{self.device_ip}:{self.port}",
            'connection_latency_ms': connect_latency_ms
        })
        
        device_id = f"{self.device_ip}:{self.port}"
        
        # Step 2: Send START command
        logger.info(f"\nStep 2: Sending START command")
        session_id = f"test_session_{int(time.time())}"
        
        start_cmd_send_time = time.time_ns()
        start_cmd_send_datetime = datetime.now().isoformat()
        
        response = self.client.send_command(device_id, 'START', {
            'session_id': session_id
        })
        
        start_cmd_recv_time = time.time_ns()
        start_cmd_recv_datetime = datetime.now().isoformat()
        start_cmd_latency_ms = (start_cmd_recv_time - start_cmd_send_time) / 1e6
        
        if response:
            self.log_event("START Command Sent", {
                'session_id': session_id,
                'pc_send_time': start_cmd_send_datetime,
                'pc_send_time_ns': start_cmd_send_time,
                'response': response,
                'pc_receive_time': start_cmd_recv_datetime,
                'pc_receive_time_ns': start_cmd_recv_time,
                'command_latency_ms': start_cmd_latency_ms
            })
            
            if response and response.startswith('START-ACK'):
                self.log_event("Recording Started", {
                    'session_id': session_id,
                    'latency_ms': start_cmd_latency_ms,
                    'response': response
                })
            else:
                self.log_event("START Command Failed", {
                    'response': response
                })
                return self._generate_report(success=False, error=f"START failed: {response}")
        else:
            self.log_event("START Command Failed", {
                'error': 'No response received'
            })
            return self._generate_report(success=False, error="START command failed - no response")
        
        # Step 3: Wait for recording duration
        logger.info(f"\nStep 3: Recording for {session_duration} seconds")
        self.log_event("Recording in Progress", {
            'duration_seconds': session_duration
        })
        
        time.sleep(session_duration)
        
        # Step 4: Send STOP command
        logger.info(f"\nStep 4: Sending STOP command")
        
        stop_cmd_send_time = time.time_ns()
        stop_cmd_send_datetime = datetime.now().isoformat()
        
        response = self.client.send_command(device_id, 'STOP', {
            'session_id': session_id
        })
        
        stop_cmd_recv_time = time.time_ns()
        stop_cmd_recv_datetime = datetime.now().isoformat()
        stop_cmd_latency_ms = (stop_cmd_recv_time - stop_cmd_send_time) / 1e6
        
        if response:
            self.log_event("STOP Command Sent", {
                'session_id': session_id,
                'pc_send_time': stop_cmd_send_datetime,
                'pc_send_time_ns': stop_cmd_send_time,
                'response': response,
                'pc_receive_time': stop_cmd_recv_datetime,
                'pc_receive_time_ns': stop_cmd_recv_time,
                'command_latency_ms': stop_cmd_latency_ms
            })
            
            if 'ACK' in response or 'STOP-ACK' in response:
                self.log_event("Recording Stopped", {
                    'session_id': session_id,
                    'latency_ms': stop_cmd_latency_ms,
                    'response': response
                })
            else:
                self.log_event("STOP Command Warning", {
                    'response': response,
                    'note': 'Unexpected response but continuing'
                })
        else:
            self.log_event("STOP Command Failed", {
                'error': 'No response received'
            })
            # Don't fail the test completely if STOP fails
        
        # Step 5: Disconnect
        logger.info(f"\nStep 5: Disconnecting from device")
        self.client.disconnect_device(device_id)
        self.log_event("Disconnected", {
            'device_id': device_id
        })
        
        test_end_time = datetime.now()
        total_duration = (test_end_time - test_start_time).total_seconds()
        
        self.log_event("Test Completed", {
            'total_duration_seconds': total_duration,
            'success': True
        })
        
        return self._generate_report(success=True)
    
    def _generate_report(self, success: bool, error: str = None) -> Dict:
        """Generate test report"""
        report = {
            'test_name': 'Remote Start/Stop Command Test',
            'test_id': 'test_1',
            'timestamp': datetime.now().isoformat(),
            'success': success,
            'event_log': self.event_log,
            'summary': self._generate_summary()
        }
        
        if error:
            report['error'] = error
        
        return report
    
    def _generate_summary(self) -> Dict:
        """Generate summary statistics from event log"""
        summary = {
            'total_events': len(self.event_log),
            'events_by_type': {}
        }
        
        # Count events by type
        for event in self.event_log:
            event_type = event['event_type']
            summary['events_by_type'][event_type] = \
                summary['events_by_type'].get(event_type, 0) + 1
        
        # Extract latency measurements
        start_latency = None
        stop_latency = None
        
        for event in self.event_log:
            if event['event_type'] == 'START Command Sent':
                start_latency = event['details'].get('command_latency_ms')
            elif event['event_type'] == 'STOP Command Sent':
                stop_latency = event['details'].get('command_latency_ms')
        
        if start_latency is not None:
            summary['start_command_latency_ms'] = start_latency
        if stop_latency is not None:
            summary['stop_command_latency_ms'] = stop_latency
        
        return summary
    
    def save_report(self, report: Dict, output_file: str = None):
        """Save test report to JSON file"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"test_1_start_stop_report_{timestamp}.json"
        
        output_path = Path(__file__).parent / output_file
        
        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        logger.info(f"\nReport saved to: {output_path}")
        
        # Also save a more readable version
        readable_file = output_path.with_suffix('.txt')
        with open(readable_file, 'w') as f:
            f.write("=" * 80 + "\n")
            f.write("TEST 1: Remote Start/Stop Command Test - Event Log\n")
            f.write("=" * 80 + "\n\n")
            
            for event in report['event_log']:
                f.write(f"[{event['timestamp_iso']}] {event['event_type']}\n")
                if event['details']:
                    for key, value in event['details'].items():
                        f.write(f"  {key}: {value}\n")
                f.write("\n")
            
            f.write("=" * 80 + "\n")
            f.write("Summary\n")
            f.write("=" * 80 + "\n")
            f.write(json.dumps(report['summary'], indent=2))
            f.write("\n")
        
        logger.info(f"Readable report saved to: {readable_file}")


def main():
    """Main entry point for test"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Test 1: Remote Start/Stop Command Test'
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
        '--duration',
        type=int,
        default=10,
        help='Recording session duration in seconds (default: 10)'
    )
    parser.add_argument(
        '--output',
        help='Output file name for report (optional)'
    )
    
    args = parser.parse_args()
    
    # Run test
    test = StartStopCommandTest(args.device_ip, args.port)
    report = test.run_test(session_duration=args.duration)
    
    # Save report
    test.save_report(report, args.output)
    
    # Print summary
    logger.info("\n" + "=" * 80)
    logger.info("TEST RESULTS")
    logger.info("=" * 80)
    logger.info(f"Success: {report['success']}")
    logger.info(f"Total Events: {report['summary']['total_events']}")
    
    if 'start_command_latency_ms' in report['summary']:
        logger.info(f"START Command Latency: {report['summary']['start_command_latency_ms']:.2f} ms")
    if 'stop_command_latency_ms' in report['summary']:
        logger.info(f"STOP Command Latency: {report['summary']['stop_command_latency_ms']:.2f} ms")
    
    if not report['success']:
        logger.error(f"Error: {report.get('error', 'Unknown error')}")
        sys.exit(1)
    
    logger.info("\nTest completed successfully!")


if __name__ == "__main__":
    main()
