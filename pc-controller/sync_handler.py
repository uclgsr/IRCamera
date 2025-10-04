#!/usr/bin/env python3
"""
Time Synchronization Handler for PC Controller

This module implements the SYNC_INIT -> SYNC_REQUEST -> SYNC_RESPONSE -> SYNC_RESULT
protocol flow as specified in docs/time_sync_implementation.md
"""

import time
import logging
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)


class SyncHandler:
    """Handles time synchronization protocol with Android devices"""
    
    def __init__(self):
        self.pending_syncs = {}
        self.sync_history = {}
        
    def handle_sync_init(self, device_id: str, socket: 'socket.socket') -> bool:
        """
        Handle SYNC_INIT message from Android device.
        
        When Android sends SYNC_INIT, immediately respond with SYNC_REQUEST
        containing the current PC timestamp.
        
        Args:
            device_id: Unique identifier for the device
            socket: Socket connection to the device
            
        Returns:
            True if SYNC_REQUEST was sent successfully
        """
        try:
            # Capture T1 (PC send time)
            t1 = int(time.time() * 1000)
            
            # Create SYNC_REQUEST message
            sync_request = f"SYNC_REQUEST t_pc={t1}\n"
            
            # Send to Android
            socket.send(sync_request.encode('utf-8'))
            
            # Store T1 for later calculation
            self.pending_syncs[device_id] = {
                't1': t1,
                'sent_at': time.time()
            }
            
            logger.info(f"SYNC_INIT received from {device_id}, sent SYNC_REQUEST with t1={t1}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to handle SYNC_INIT from {device_id}: {e}")
            return False
    
    def handle_sync_response(self, device_id: str, t_pc: int, t_ph: int, socket: 'socket.socket') -> Optional[Dict[str, Any]]:
        """
        Handle SYNC_RESPONSE message from Android device.
        
        Calculate offset and RTT, then send SYNC_RESULT back to Android.
        
        Args:
            device_id: Unique identifier for the device
            t_pc: PC timestamp (T1) from original SYNC_REQUEST
            t_ph: Phone timestamp (T2) when it received SYNC_REQUEST
            socket: Socket connection to the device
            
        Returns:
            Dictionary with sync results or None if failed
        """
        try:
            # Check if we have a pending sync for this device
            if device_id not in self.pending_syncs:
                logger.warning(f"Received SYNC_RESPONSE from {device_id} but no pending sync")
                return None
            
            pending = self.pending_syncs[device_id]
            t1 = pending['t1']
            
            # Verify t_pc matches our t1
            if t_pc != t1:
                logger.warning(f"SYNC_RESPONSE t_pc={t_pc} doesn't match our t1={t1}")
                # Continue anyway, use the t_pc from response
                t1 = t_pc
            
            t2 = t_ph
            
            # Capture T3 (PC receive time)
            t3 = int(time.time() * 1000)
            
            # Calculate offset and RTT
            # offset = T2 - ((T1 + T3) / 2)
            # Positive offset means phone is ahead of PC
            offset = int(t2 - ((t1 + t3) / 2))
            
            # rtt = T3 - T1
            rtt = t3 - t1
            
            # Create SYNC_RESULT message
            sync_result_msg = f"SYNC_RESULT t1={t1} t2={t2} t3={t3} offset={offset} rtt={rtt}\n"
            
            # Send to Android
            socket.send(sync_result_msg.encode('utf-8'))
            
            # Store sync results
            sync_data = {
                't1': t1,
                't2': t2,
                't3': t3,
                'offset_ms': offset,
                'rtt_ms': rtt,
                'timestamp': time.time(),
                'quality': self._calculate_quality(rtt)
            }
            
            self.sync_history[device_id] = sync_data
            del self.pending_syncs[device_id]
            
            logger.info(
                f"Time sync completed with {device_id}: "
                f"offset={offset}ms, rtt={rtt}ms, quality={sync_data['quality']}"
            )
            
            return sync_data
            
        except Exception as e:
            logger.error(f"Failed to handle SYNC_RESPONSE from {device_id}: {e}")
            return None
    
    def _calculate_quality(self, rtt_ms: int) -> str:
        """Calculate sync quality based on RTT"""
        if rtt_ms < 50:
            return "EXCELLENT"
        elif rtt_ms < 100:
            return "GOOD"
        elif rtt_ms < 200:
            return "ACCEPTABLE"
        else:
            return "POOR"
    
    def get_sync_stats(self, device_id: str) -> Optional[Dict[str, Any]]:
        """Get sync statistics for a device"""
        return self.sync_history.get(device_id)
    
    def get_all_sync_stats(self) -> Dict[str, Dict[str, Any]]:
        """Get sync statistics for all devices"""
        return self.sync_history.copy()
    
    def cleanup_expired_syncs(self, timeout_seconds: float = 30.0):
        """Remove pending syncs that haven't completed within timeout"""
        current_time = time.time()
        expired = []
        
        for device_id, pending in self.pending_syncs.items():
            if current_time - pending['sent_at'] > timeout_seconds:
                expired.append(device_id)
        
        for device_id in expired:
            logger.warning(f"Sync timeout for {device_id}, removing from pending")
            del self.pending_syncs[device_id]


# Example usage
if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    print("SyncHandler Example")
    print("=" * 60)
    print()
    print("This module handles the time synchronization protocol:")
    print()
    print("1. Android sends: SYNC_INIT")
    print("   -> PC responds with: SYNC_REQUEST t_pc=T1")
    print()
    print("2. Android sends: SYNC_RESPONSE t_pc=T1 t_ph=T2")
    print("   -> PC calculates offset and RTT")
    print("   -> PC responds with: SYNC_RESULT t1=T1 t2=T2 t3=T3 offset=X rtt=Y")
    print()
    print("Usage:")
    print("  handler = SyncHandler()")
    print("  handler.handle_sync_init(device_id, socket)")
    print("  handler.handle_sync_response(device_id, t_pc, t_ph, socket)")
    print()
