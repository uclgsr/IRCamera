# Testing Time Synchronization

## Quick Start

The time synchronization implementation is now complete on both Android and PC sides.

### Testing with Example Server

1. **Start the PC server:**
   ```bash
   cd pc-controller
   python3 example_sync_server.py
   ```
   Default port: 8080

2. **Start Android app and connect to PC**

3. **Start a recording session**
   - Android will automatically send SYNC_INIT
   - PC will respond with SYNC_REQUEST
   - Full sync protocol will complete

4. **Check logs:**
   
   PC side:
   ```
   INFO - SYNC_INIT from <device> - initiating time sync
   INFO - Time sync completed with <device>: offset=Xms, rtt=Yms, quality=EXCELLENT
   ```
   
   Android side:
   ```
   adb logcat | grep TimeSyncManager
   ```
   Should see:
   ```
   TimeSyncManager: Session start sync initiated with PC
   TimeSyncManager: Clock offset applied: Xms (RTT: Yms)
   ```

### Running PC Tests

```bash
cd pc-controller
python3 tests/test_sync_handler.py -v
```

Expected output:
```
test_handle_sync_init ... ok
test_handle_sync_response ... ok
test_offset_calculation ... ok
test_quality_calculation ... ok
...
Ran 10 tests in 0.002s
OK
```

## Protocol Verification

### Manual Testing

You can also test manually with netcat:

1. Start example server: `python3 example_sync_server.py`

2. Connect with netcat: `nc localhost 8080`

3. Send SYNC_INIT:
   ```
   SYNC_INIT
   ```

4. Server should respond immediately:
   ```
   SYNC_REQUEST t_pc=1234567890
   ```

5. Send SYNC_RESPONSE:
   ```
   SYNC_RESPONSE t_pc=1234567890 t_ph=1234567895
   ```

6. Server should respond:
   ```
   SYNC_RESULT t1=1234567890 t2=1234567895 t3=1234567900 offset=5 rtt=10
   ```

## Integration Testing

### End-to-End Test

1. Start PC server
2. Start Android app
3. Connect Android to PC
4. Start recording session
5. Check sync log: `<session_dir>/timesync_log.csv`

Expected CSV content:
```csv
sync_index,timestamp_iso,phone_timestamp_t2,pc_send_time_t1,pc_recv_time_t3,offset_ms,rtt_ms,session_relative_time_ms,sync_quality,retry_count
1,2024-...,1234567895,1234567890,1234567900,5,10,0,EXCELLENT,0
```

### Verify Timestamps

Check sensor data files to confirm synchronized timestamps are being used:
```bash
adb pull /sdcard/Android/data/<package>/files/<session>/gsr_data.csv
```

Look for the `synchronizedTimestampMs` column with adjusted timestamps.

## Troubleshooting

### SYNC_INIT not received by PC

**Symptoms:** No sync messages in PC logs

**Check:**
1. Android NetworkServer is running: `adb logcat | grep NetworkServer`
2. PC and Android are connected: Check connection status
3. SYNC_INIT is being sent: `adb logcat | grep "SYNC_INIT message sent"`

**Fix:** Verify network connection, check firewall settings

### Offset seems incorrect

**Symptoms:** Large or unexpected offset values

**Check:**
1. RTT value - should be <200ms for good quality
2. PC clock accuracy - sync PC with NTP
3. Network stability - check for packet loss

**Fix:** 
- Improve network conditions
- Use wired connection if possible
- Sync PC with NTP: `sudo ntpdate pool.ntp.org`

### Periodic sync not working

**Symptoms:** Only session start sync occurs

**Check:**
1. Session duration >10 minutes
2. `periodicSyncEnabled` is true
3. Callback is properly registered

**Verify:**
```bash
adb logcat | grep "Triggering periodic sync"
```

## Performance Metrics

Expected performance:
- RTT: 10-50ms on local network
- Offset accuracy: ±5ms
- Sync quality: EXCELLENT (<50ms RTT)
- Completion time: <1 second

## Success Criteria

✅ SYNC_INIT sent at session start
✅ PC responds with SYNC_REQUEST
✅ Phone sends SYNC_RESPONSE
✅ PC calculates and sends SYNC_RESULT
✅ Offset applied to TimestampManager
✅ Sync logged to CSV file
✅ Periodic sync works for long sessions
✅ All tests pass

## Example Output

### Successful Sync

PC terminal:
```
INFO - Android device connected from ('192.168.1.100', 54321)
INFO - Received from 192.168.1.100:54321: SYNC_INIT
INFO - SYNC_INIT from 192.168.1.100:54321 - initiating time sync
INFO - Sending to 192.168.1.100:54321: SYNC_REQUEST t_pc=1234567890
INFO - Received from 192.168.1.100:54321: SYNC_RESPONSE t_pc=1234567890 t_ph=1234567895
INFO - Time sync completed with 192.168.1.100:54321: offset=5ms, rtt=10ms, quality=EXCELLENT
INFO - Sending to 192.168.1.100:54321: SYNC_RESULT t1=1234567890 t2=1234567895 t3=1234567900 offset=5 rtt=10
```

Android logcat:
```
I TimeSyncManager: Performing session start sync
I TimeSyncManager: Session start sync initiated with PC
I RecordingService: SYNC_INIT message sent to PC successfully
I ProtocolHandler: Processing protocol message: SYNC_REQUEST
I TimeSyncManager: Clock offset applied: 5ms (RTT: 10ms)
I TimeSyncManager: Sync calculation completed successfully (quality: EXCELLENT, attempt: 1)
```

## Next Steps

Once testing is complete:
1. Verify offset accuracy with reference clock
2. Test with multiple devices
3. Test network failure scenarios
4. Benchmark performance under load
5. Document production deployment
