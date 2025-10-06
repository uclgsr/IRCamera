# Time Synchronization Implementation Guide

## Overview

Time synchronization is now active in the Android app. The app initiates time sync at session start and can trigger
periodic syncs during long recording sessions.

## Protocol Flow

### 1. Phone-Initiated Sync

When the phone needs to synchronize its clock with the PC:

```
Phone -> PC: SYNC_INIT
PC -> Phone: SYNC_REQUEST t_pc=<T1>
Phone -> PC: SYNC_RESPONSE t_pc=<T1> t_ph=<T2>
PC -> Phone: SYNC_RESULT t1=<T1> t2=<T2> t3=<T3> offset=<OFFSET> rtt=<RTT>
```

### 2. Message Definitions

#### SYNC_INIT (Phone -> PC)

- Format: `SYNC_INIT`
- Purpose: Phone requests PC to initiate a time synchronization exchange
- PC Action: Immediately send SYNC_REQUEST with current PC timestamp

#### SYNC_REQUEST (PC -> Phone)

- Format: `SYNC_REQUEST t_pc=<T1>`
- T1: PC timestamp in milliseconds when request is sent
- Purpose: PC initiates the NTP-style exchange

#### SYNC_RESPONSE (Phone -> PC)

- Format: `SYNC_RESPONSE t_pc=<T1> t_ph=<T2>`
- T1: Original PC timestamp from SYNC_REQUEST
- T2: Phone timestamp in milliseconds when request was received
- Purpose: Phone responds with receive timestamp

#### SYNC_RESULT (PC -> Phone)

- Format: `SYNC_RESULT t1=<T1> t2=<T2> t3=<T3> offset=<OFFSET> rtt=<RTT>`
- T1: PC send time
- T2: Phone receive time
- T3: PC receive time
- offset: Calculated clock offset in milliseconds (positive = phone ahead)
- rtt: Round-trip time in milliseconds
- Purpose: PC sends calculated offset for phone to apply

### 3. Offset Calculation

```
offset = ((T2 - T1) + (T2 - T3)) / 2
rtt = (T3 - T1) - (T2 - T2) = T3 - T1
```

Simplified:

```
offset = T2 - ((T1 + T3) / 2)
rtt = T3 - T1
```

The offset represents how much the phone clock is ahead of the PC clock.

### 4. PC Controller Implementation Requirements

The PC controller must:

1. **Handle SYNC_INIT messages**: When receiving SYNC_INIT, immediately send SYNC_REQUEST
   ```python
   async def handle_sync_init(self, client):
       t1 = int(time.time() * 1000)  # PC timestamp in ms
       await client.send(f"SYNC_REQUEST t_pc={t1}")
       # Store t1 for later calculation
       self.pending_sync[client.id] = {'t1': t1}
   ```

2. **Process SYNC_RESPONSE**: Calculate offset and RTT, send SYNC_RESULT
   ```python
   async def handle_sync_response(self, client, t_pc, t_ph):
       t1 = self.pending_sync[client.id]['t1']
       t2 = t_ph
       t3 = int(time.time() * 1000)  # PC timestamp when response received
       
       offset = t2 - ((t1 + t3) // 2)
       rtt = t3 - t1
       
       await client.send(f"SYNC_RESULT t1={t1} t2={t2} t3={t3} offset={offset} rtt={rtt}")
       
       # Store sync stats
       self.sync_stats[client.id] = {
           'offset_ms': offset,
           'rtt_ms': rtt,
           'last_sync': datetime.now()
       }
   ```

3. **Log sync events**: Track sync history for debugging
   ```python
   logger.info(f"Time sync completed: offset={offset}ms, rtt={rtt}ms")
   ```

### 5. When Sync is Triggered

The Android app triggers sync automatically at:

- **Session start**: When recording begins
- **Periodic intervals**: Every 5 minutes during long sessions (>10 minutes)
- **Manual request**: When user manually triggers sync (if implemented)

### 6. Sync Quality Metrics

The phone tracks sync quality based on RTT:

- **EXCELLENT**: RTT < 50ms
- **GOOD**: RTT 50-100ms
- **ACCEPTABLE**: RTT 100-200ms
- **POOR**: RTT > 200ms

The PC should log RTT values to help diagnose network issues.

### 7. Testing

#### Quick Start Testing

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

#### Running PC Tests

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

#### Protocol Verification - Manual Testing

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

#### Integration Testing - End-to-End

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

#### Verify Timestamps

Check sensor data files to confirm synchronized timestamps are being used:

```bash
adb pull /sdcard/Android/data/<package>/files/<session>/gsr_data.csv
```

Look for the `synchronizedTimestampMs` column with adjusted timestamps.

#### Troubleshooting

**SYNC_INIT not received by PC**

Symptoms: No sync messages in PC logs

Check:

1. Android NetworkServer is running: `adb logcat | grep NetworkServer`
2. PC and Android are connected: Check connection status
3. SYNC_INIT is being sent: `adb logcat | grep "SYNC_INIT message sent"`

Fix: Verify network connection, check firewall settings

**Offset seems incorrect**

Symptoms: Large or unexpected offset values

Check:

1. RTT value - should be <200ms for good quality
2. PC clock accuracy - sync PC with NTP
3. Network stability - check for packet loss

Fix:

- Improve network conditions
- Use wired connection if possible
- Sync PC with NTP: `sudo systemctl start systemd-timesyncd` or `sudo chronyc makestep`

**Periodic sync not working**

Symptoms: Only session start sync occurs

Check:

1. Session duration >10 minutes
2. `periodicSyncEnabled` is true
3. Callback is properly registered

Verify:

```bash
adb logcat | grep "Periodic sync triggered"
```

### 8. Integration with Existing Code

Update the ProtocolHandler or similar PC-side component:

```python
async def process_message(self, message):
    msg_type = message.get('type')
    
    if msg_type == 'SYNC_INIT':
        await self.handle_sync_init(message)
    elif msg_type == 'SYNC_RESPONSE':
        await self.handle_sync_response(
            message.get('t_pc'),
            message.get('t_ph')
        )
    # ... existing handlers
```

## Impact on Timestamps

After sync completes:

- All sensor data timestamps are adjusted by the calculated offset
- Phone timestamps become aligned with PC time
- Cross-device coordination becomes accurate
- Offset is preserved until next sync

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

## Notes

- The phone's clock is not physically adjusted (by design)
- An offset is calculated and applied to all timestamps
- This maintains phone stability while achieving sync
- Multiple devices can sync to the same PC master clock
