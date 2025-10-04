# Time Synchronization Implementation Guide

## Overview

Time synchronization is now active in the Android app. The app initiates time sync at session start and can trigger periodic syncs during long recording sessions.

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

To test the sync implementation:

1. Start PC controller with sync handling enabled
2. Connect Android app to PC
3. Start a recording session
4. Verify SYNC_INIT, SYNC_REQUEST, SYNC_RESPONSE, SYNC_RESULT exchange
5. Check phone's timesync_log.csv for sync results
6. Verify subsequent sensor data uses synchronized timestamps

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

## Notes

- The phone's clock is not physically adjusted (by design)
- An offset is calculated and applied to all timestamps
- This maintains phone stability while achieving sync
- Multiple devices can sync to the same PC master clock
