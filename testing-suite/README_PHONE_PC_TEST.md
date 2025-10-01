# Phone-PC Connection Test Suite

## Quick Start

Test that a simulated Android phone can connect to the PC controller.

### Option 1: Quick Test (Basic Handshake)

```bash
cd testing-suite
python3 test_phone_pc_connection.py --quick
```

This will automatically:

1. Start the PC controller server
2. Run the phone simulator (quick mode)
3. Verify basic connection and HELLO handshake
4. Report results

### Option 2: Full Test (All Protocol Commands)

```bash
cd testing-suite
python3 test_phone_pc_connection.py
```

This runs a complete protocol test including:

- Connection and HELLO handshake
- START_RECORD and STOP_RECORD commands
- GSR data streaming
- Graceful disconnect

### Option 3: Enhanced Test Suite (Comprehensive)

```bash
cd testing-suite
python3 test_enhanced_phone_pc_connection.py
```

Runs 6 comprehensive tests:

1. Basic connection and handshake
2. Full protocol exchange
3. Multiple simultaneous connections
4. Reconnection after disconnect
5. Connection timeout handling
6. Rapid sequential connections

### Option 4: Manual Test

**Terminal 1 - Start PC Controller:**

```bash
cd pc-controller
python3 unified_pc_controller_improved.py --cli --port 9090
```

**Terminal 2 - Run Phone Simulator:**

```bash
cd testing-suite
python3 simulate_android_phone.py --host localhost --port 9090 --quick
```

## Expected Output

### Quick Test Success

```
[INFO] Connecting to PC controller at localhost:9090...
[SUCCESS] Connected to localhost:9090
[SEND] HELLO device_name=android_sim_XXXXX sensors=[RGB,THERMAL,GSR]
[RECV] ACK cmd=HELLO device_id=device_127.0.0.1_XXXXX
[SUCCESS] HELLO handshake completed
```

### Full Test Success

```
[SUCCESS] HELLO handshake completed
[SUCCESS] Recording commands test passed
[SUCCESS] All tests passed!
```

### Enhanced Test Suite Success

```
Total Tests: 6
Passed: 6
Failed: 0

RESULT: ALL TESTS PASSED
```

### Server Log

```
2025-10-01 12:01:15,734 - __main__ - INFO - New connection from ('127.0.0.1', 44424)
2025-10-01 12:01:15,735 - __main__ - INFO - Device registered: android_sim_1759320075 (sensors: RGB, THERMAL, GSR)
```

## Troubleshooting

### "Connection refused"

- Make sure the PC controller is running first
- Check the port number matches (default: 9090 or 8080)

### "Address already in use"

- Another process is using the port
- Try a different port: `--port 9091`
- Or kill the existing process: `pkill -f unified_pc_controller`

### "Connection timeout"

- Check firewall settings
- Verify server is listening: `netstat -tuln | grep 9090`

### Tests failing

- Make sure you're in the testing-suite directory
- Check Python version: `python3 --version` (should be 3.7+)
- Verify all test scripts are executable

## Test Files

- `simulate_android_phone.py` - Android phone TCP client simulator
- `test_phone_pc_connection.py` - Basic automated test runner
- `test_enhanced_phone_pc_connection.py` - Enhanced test suite with 6 comprehensive tests
- `PHONE_PC_CONNECTION_VERIFICATION.md` - Detailed verification report
- This file: `README_PHONE_PC_TEST.md` - Quick start guide

## What's Tested

### Basic Tests

- TCP socket connection
- HELLO handshake protocol
- Message format compatibility
- Bidirectional communication
- Graceful disconnect

### Enhanced Tests

- Multiple simultaneous connections (3 clients)
- Reconnection after disconnect
- Connection timeout handling
- Rapid sequential connections (5 in quick succession)
- Full protocol exchange (START_RECORD, STOP_RECORD, GSR data)

## Results

All tests pass successfully. The networking layer is verified and production-ready.

See `PHONE_PC_CONNECTION_VERIFICATION.md` for detailed test results and verification status.
