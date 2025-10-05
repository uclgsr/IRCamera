# Testing Time Synchronization - Summary

## Overview

This document provides a quick reference for testing the time synchronization feature. For complete implementation details, protocol specifications, and troubleshooting, see the comprehensive documentation in:

**[pc-controller/docs/time_sync_implementation.md](../../pc-controller/docs/time_sync_implementation.md)**

## Quick Test

1. **Start PC server:**
   ```bash
   cd pc-controller
   python3 example_sync_server.py
   ```

2. **Start Android app and connect to PC**

3. **Start recording session** - automatic sync will occur

4. **Verify in logs:**
   - PC: `INFO - Time sync completed with <device>: offset=Xms, rtt=Yms`
   - Android: `adb logcat | grep TimeSyncManager`

## Key Test Commands

```bash
# Run PC sync handler tests
cd pc-controller
python3 tests/test_sync_handler.py -v

# Check Android sync logs
adb logcat | grep TimeSyncManager

# Verify sync results in CSV
adb pull /sdcard/Android/data/<package>/files/<session>/timesync_log.csv
```

## Success Indicators

- SYNC_INIT sent at session start
- PC responds with SYNC_REQUEST
- Phone sends SYNC_RESPONSE
- PC sends SYNC_RESULT with offset
- Offset applied to all timestamps
- RTT <50ms on local network

## Common Issues

| Issue | Solution |
|-------|----------|
| SYNC_INIT not received | Check network connection and firewall |
| Large offset values | Check RTT, sync PC with NTP |
| Periodic sync not working | Verify session duration >10 min |

## Documentation Structure

- **[time_sync_implementation.md](../../pc-controller/docs/time_sync_implementation.md)** - Complete implementation guide
  - Protocol flow and message definitions
  - PC controller implementation requirements
  - Comprehensive testing procedures
  - Troubleshooting guide
  - Performance metrics and examples

- **[time-sync-implementation-summary.md](../maintenance/time-sync-implementation-summary.md)** - Android implementation changes
  - Root cause analysis
  - Changes implemented
  - Impact on codebase

- **[time-sync-timeline.md](../thesis/diagrams/time-sync-timeline.md)** - Thesis visualization
  - Mermaid diagrams for temporal alignment
  - Multi-sensor synchronization timeline
