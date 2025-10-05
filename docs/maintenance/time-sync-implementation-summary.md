# Time Synchronization Implementation Summary

> **Note:** For complete implementation details, protocol specifications, and testing procedures, see the comprehensive guide: **[pc-controller/docs/time_sync_implementation.md](../../pc-controller/docs/time_sync_implementation.md)**

## Issue Description

The issue stated that time synchronization was completely dormant - no actual NTP-style protocol exchange was happening, and all sensor data was timestamped using only the Android device's clock without any alignment to the PC master clock.

## Root Cause Analysis

The time synchronization infrastructure was already implemented but never triggered:
- `TimeSyncManager` class existed with full NTP-style protocol implementation
- `ProtocolHandler` could process SYNC_REQUEST, SYNC_RESPONSE, and SYNC_RESULT messages
- `TimeSyncManager` was initialized in RecordingService lifecycle
- **BUT**: The sync was never initiated because:
  1. PC controller did not automatically send SYNC_REQUEST
  2. Phone had no way to request PC to initiate sync
  3. Session start sync only logged a marker, didn't trigger actual exchange

## Changes Implemented

### 1. Protocol Enhancement (Protocol.kt)
- Added `MSG_SYNC_INIT` constant
- Added `createSyncInitMessage()` function
- Phone can now request PC to initiate time synchronization

### 2. RecordingService Wiring (RecordingService.kt)
- Updated `setSyncTriggerCallback` to actually send SYNC_INIT message to PC
- Changed from stub implementation to functional network message sending
- Now properly uses `networkServer.sendMessage()` to communicate with PC

### 3. Session Start Sync (TimeSyncManager.kt)
- Updated `performSessionStartSync()` to trigger actual sync with PC
- Now calls the sync trigger callback after logging session marker
- Ensures sync happens automatically when recording starts

### 4. Documentation Updates
- Updated `PROTOCOL_FLOW.txt` to show SYNC_INIT in the protocol flow
- Created `TIME_SYNC_IMPLEMENTATION.md` with complete PC controller guide
- Documented the full NTP-style protocol exchange

### 5. Tests Added
- Created `ProtocolTest.kt` with unit tests for SYNC_INIT message
- Updated `ProtocolIntegrationTest.kt` to include SYNC_INIT in test cases
- Verified protocol message creation and parsing

## How It Works Now

### Automatic Sync at Session Start
1. User starts recording
2. `RecordingService.startRecordingSession()` is called
3. `TimeSyncManager.performSessionStartSync()` is triggered
4. Phone sends `SYNC_INIT` message to PC
5. PC responds with `SYNC_REQUEST t_pc=<T1>`
6. Phone sends `SYNC_RESPONSE t_pc=<T1> t_ph=<T2>`
7. PC calculates offset and RTT, sends `SYNC_RESULT t1=<T1> t2=<T2> t3=<T3> offset=<OFFSET> rtt=<RTT>`
8. Phone applies offset via `TimeSyncManager.completeSyncCalculation()`
9. All subsequent timestamps are adjusted by the calculated offset

### Periodic Sync During Long Sessions
- For sessions longer than 10 minutes (configurable)
- Automatic re-sync every 5 minutes (configurable)
- Same SYNC_INIT -> SYNC_REQUEST -> SYNC_RESPONSE -> SYNC_RESULT flow
- Handles clock drift over time

### Manual Sync
- Can be triggered via `TimeSyncManager.triggerManualSync()`
- Same protocol flow as automatic sync

## Related Documentation

### Complete Implementation Guide
See **[pc-controller/docs/time_sync_implementation.md](../../pc-controller/docs/time_sync_implementation.md)** for:
- Full protocol specifications and message definitions
- PC controller implementation requirements
- Offset calculation details
- Comprehensive testing procedures
- Troubleshooting guide
- Performance metrics and examples

### Testing Guide
See **[docs/summaries/testing-time-sync.md](../summaries/testing-time-sync.md)** for:
- Quick test procedures
- Success indicators
- Common issues and solutions

### Thesis Documentation
See **[docs/thesis/diagrams/time-sync-timeline.md](../thesis/diagrams/time-sync-timeline.md)** for:
- Mermaid diagrams of temporal alignment
- Multi-sensor synchronization timeline visualization
- Verify offset calculation is correct
- Test with multiple devices syncing to same PC
- Verify periodic sync works during long sessions
- Test network failure scenarios

## Technical Details

### Offset Calculation
```
offset = T2 - ((T1 + T3) / 2)
rtt = T3 - T1
```
Where:
- T1 = PC send time (when PC sends SYNC_REQUEST)
- T2 = Phone receive time (when phone receives SYNC_REQUEST)
- T3 = PC receive time (when PC receives SYNC_RESPONSE)

Positive offset means phone clock is ahead of PC clock.

### Timestamp Adjustment
After sync completes:
- `TimestampManager.setClockOffset(offsetMs)` is called
- All subsequent calls to `getSynchronizedTimestampMs()` return `deviceTime + offset`
- Sensor data uses synchronized timestamps
- Original device timestamps are preserved for reference

### Sync Quality
Based on RTT:
- EXCELLENT: < 50ms
- GOOD: 50-100ms
- ACCEPTABLE: 100-200ms
- POOR: > 200ms

### Logging
All sync events are logged to `timesync_log.csv` in session directory:
```
sync_index,timestamp_iso,phone_timestamp_t2,pc_send_time_t1,pc_recv_time_t3,offset_ms,rtt_ms,session_relative_time_ms,sync_quality,retry_count
```

## Verification Steps

To verify the implementation works:

1. **Check Phone Logs**
   ```
   adb logcat | grep TimeSyncManager
   ```
   Should see:
   - "Manual sync requested - sending SYNC_INIT to PC"
   - "Performing session start sync"
   - "Session start sync initiated with PC"

2. **Check Network Traffic**
   - Phone should send `SYNC_INIT` at session start
   - Verify message is sent via NetworkServer

3. **Check Sync Log File**
   - After session completes, check `<session_dir>/timesync_log.csv`
   - Should contain sync results with offset and RTT values

4. **Verify Timestamps**
   - Sensor data timestamps should reflect applied offset
   - Compare `deviceTimestampMs` vs `synchronizedTimestampMs` in data files

## Impact

This implementation transforms the time sync from dormant to active:

### Before
- Phone clock ran independently
- No offset calculation
- No PC coordination
- Manual post-processing required to align data

### After
- Automatic sync at session start
- Periodic re-sync during long sessions
- PC-calculated offset applied to all timestamps
- Multiple devices can sync to same PC master clock
- Cross-device coordination is now accurate

## Configuration

Sync behavior can be configured via `TimeSyncManager.SyncConfiguration`:
- `periodicSyncIntervalMs`: How often to re-sync (default: 5 minutes)
- `longSessionThresholdMs`: When to start periodic sync (default: 10 minutes)
- `maxSyncRetries`: Retry attempts for failed sync (default: 3)
- `syncTimeoutMs`: Timeout for sync operations (default: 5 seconds)
- `maxTimestampDriftMs`: Max acceptable clock drift (default: 24 hours)

## Dependencies

The implementation relies on:
- `NetworkServer` being connected to PC
- `ProtocolHandler` processing incoming messages correctly
- `TimeManager` and `TimestampManager` for timestamp management
- PC controller implementing the protocol correctly

## Security Considerations

- Timestamp validation prevents clock attacks (max drift check)
- RTT validation ensures reasonable network conditions
- Failed sync attempts are logged but don't crash the app
- Original device timestamps always preserved

## Performance

- Minimal overhead: sync happens ~1 second at session start
- Periodic sync is non-blocking (uses coroutines)
- Sync quality tracking helps diagnose network issues
- Failed syncs don't block recording

## Future Enhancements

Potential improvements:
1. Use median of multiple sync exchanges for better accuracy
2. Kalman filter for smoother offset estimation
3. Automatic retry with exponential backoff
4. Sync health monitoring dashboard
5. Support for multiple PC controllers (multi-master)
6. IEEE 1588 PTP protocol for microsecond precision
