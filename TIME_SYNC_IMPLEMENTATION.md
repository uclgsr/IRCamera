# Time Synchronization Implementation

This document describes the implementation of the Time Synchronization feature as specified in issue requirements.

## Overview

The time synchronization implementation follows the NTP-style protocol design specified in the issue. It includes:

1. **TimeSyncManager** - Dedicated component for handling sync operations
2. **Protocol Extensions** - Added SYNC_RESULT message support
3. **ProtocolHandler Integration** - Enhanced sync handling with TimeSyncManager
4. **RecordingService Integration** - Automatic sync at session start/stop
5. **Comprehensive Logging** - CSV-based sync result logging

## Architecture

### TimeSyncManager Class

Location: `app/src/main/java/mpdc4gsr/sync/TimeSyncManager.kt`

Key features:
- **NTP-style timestamp exchange**: Captures t2 timestamp immediately upon receiving PC's sync request
- **Dedicated CSV logging**: Creates `timesync_log.csv` with columns: `sync_index,timestamp_iso,phone_timestamp_t2,pc_send_time_t1,pc_recv_time_t3,offset_ms,rtt_ms,session_relative_time_ms`
- **Session lifecycle integration**: Automatically initializes/finalizes with recording sessions
- **Non-blocking execution**: Uses coroutines for async operations
- **Comprehensive sync statistics**: Provides runtime stats about sync operations

Key methods:
- `initializeSession(sessionDirectory)` - Sets up sync logging for a new session
- `performSyncResponse(t1PcSendTime)` - Handles incoming SYNC_REQUEST, captures t2
- `completeSyncCalculation(t1, t2, t3, offsetMs, rttMs, syncIndex)` - Processes PC's calculated results
- `performSessionStartSync()` - Logs session start marker
- `setPeriodicSyncEnabled(enabled)` - Enable/disable periodic sync for long sessions
- `triggerManualSync()` - Manually trigger sync operation during recording
- `setSyncTriggerCallback(callback)` - Set callback for handling manual sync requests
- `finalizeSession()` - Cleans up session resources

### Protocol Extensions

Location: `app/src/main/java/mpdc4gsr/network/Protocol.kt`

Added support for:
- `MSG_SYNC_RESULT` - New message type for PC->Android offset transmission
- `createSyncResultMessage()` - Creates SYNC_RESULT messages with t1, t2, t3, offset, and RTT

### Enhanced Features

**Periodic Sync Support**:
- Automatic periodic sync monitoring for long recording sessions (>10 minutes)
- Configurable sync intervals (default: 5 minutes)
- Can be enabled/disabled per session

**Manual Sync Triggers**:
- `triggerManualSync()` method for on-demand synchronization
- Callback interface for handling sync requests from PC or user actions
- Integration with RecordingService for seamless operation

### PC Controller Updates

Location: `pc-controller/standardized_controller.py`

Enhanced features:
- **MSG_SYNC_RESULT**: Added support for SYNC_RESULT message type
- **Complete NTP Calculation**: Proper t1, t2, t3 timestamp handling with correct offset formula
- **SYNC_RESULT Transmission**: Sends calculated offset and RTT back to Android for comprehensive logging
- **Improved Accuracy**: Uses standard NTP offset calculation: `θ = ((t2 - t1) + (t2 - t3)) / 2`

### ProtocolHandler Integration

Location: `app/src/main/java/mpdc4gsr/network/ProtocolHandler.kt`

Enhancements:
- `setTimeSyncManager()` - Associates TimeSyncManager with protocol handler
- Enhanced `handleSyncRequest()` - Uses TimeSyncManager when available, falls back to legacy behavior
- New `handleSyncResult()` - Processes SYNC_RESULT messages from PC

### RecordingService Integration

Location: `app/src/main/java/mpdc4gsr/core/RecordingService.kt`

Integration points:
- **Initialization**: TimeSyncManager created in `initializePhase0Baseline()`
- **Session Start**: `timeSyncManager.initializeSession()` and `performSessionStartSync()` called in `startRecordingSession()`
- **Session Stop**: `timeSyncManager.finalizeSession()` called in `stopRecordingSession()`
- **Cleanup**: `timeSyncManager.cleanup()` called in `onDestroy()`

## Protocol Flow

### Two-Way NTP-Style Handshake

1. **PC → Android**: `SYNC_REQUEST t_pc=T1`
2. **Android captures t2** immediately upon receiving request
3. **Android → PC**: `SYNC_RESPONSE t_pc=T1 t_ph=T2`
4. **PC captures t3** and calculates offset and RTT
5. **PC → Android** (optional): `SYNC_RESULT t1=T1 t2=T2 t3=T3 offset=OFFSET rtt=RTT`
6. **Android logs complete sync result**

### Session Integration

1. **Recording Start**: 
   - TimeSyncManager initialized for session
   - Session start marker logged (sync_index=0)
   - Ready to handle PC sync requests

2. **During Recording**:
   - Responds to SYNC_REQUEST messages immediately
   - Logs all sync results to session CSV file
   - Operates non-blocking parallel to sensor recording
   - **Periodic sync**: Automatically triggers sync every 5 minutes for sessions >10 minutes
   - **Manual sync**: Can be triggered on-demand by PC or user action

3. **Recording Stop**:
   - Periodic sync monitoring stopped
   - Session finalized
   - Sync log file completed
   - Resources cleaned up

## Logging Format

The sync log file (`timesync_log.csv`) contains:

```csv
sync_index,timestamp_iso,phone_timestamp_t2,pc_send_time_t1,pc_recv_time_t3,offset_ms,rtt_ms,session_relative_time_ms
0,2024-01-15T14:30:22.123Z,1640995200123,1640995200123,1640995200123,0,0,0
1,2024-01-15T14:30:25.456Z,1640995203456,1640995203400,1640995203500,50,100,3333
```

## Testing

Unit tests are provided in:
- `app/src/test/java/mpdc4gsr/sync/TimeSyncManagerTest.kt` - Tests TimeSyncManager functionality
- `app/src/test/java/mpdc4gsr/sync/TimeSyncManagerPeriodicTest.kt` - Tests periodic sync and manual triggers
- `app/src/test/java/mpdc4gsr/network/ProtocolTest.kt` - Tests Protocol message creation
- `app/src/test/java/mpdc4gsr/sync/TimeSyncIntegrationTest.kt` - Integration tests

## Key Design Decisions

1. **PC as Clock Master**: PC calculates offset and RTT, optionally sends results to Android
2. **Immediate t2 Capture**: Android captures timestamp immediately upon receiving sync request for accuracy
3. **CSV Logging**: Human-readable format for easy post-processing and analysis
4. **Session Integration**: Automatic sync logging tied to recording lifecycle
5. **Backward Compatibility**: Falls back to existing sync behavior if TimeSyncManager not available
6. **Non-blocking Design**: All sync operations run asynchronously to avoid disrupting sensor recording

## Benefits

1. **Accuracy**: NTP-style protocol with immediate timestamp capture
2. **Comprehensive Logging**: Detailed sync history for analysis and validation
3. **Automatic Operation**: No manual intervention required
4. **Fault Tolerance**: Graceful fallback and error handling
5. **Performance**: Non-blocking design doesn't impact sensor recording
6. **Flexibility**: Supports both one-way and two-way sync protocols

This implementation fully satisfies the requirements specified in the Time Synchronization Implementation Plan issue.