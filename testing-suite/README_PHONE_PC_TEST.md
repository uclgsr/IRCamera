# Phone-PC Connection Test Suite

## Quick Start

Test that a simulated Android phone can connect to the PC controller.

### Option 1: Automated Test (Recommended)

```bash
cd testing-suite
python3 test_phone_pc_connection.py --quick
```

This will automatically:
1. Start the PC controller server
2. Run the phone simulator
3. Verify the connection
4. Report results

### Option 2: Manual Test

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

### Success
```
[INFO] Connecting to PC controller at localhost:9090...
[SUCCESS] Connected to localhost:9090
[SEND] HELLO device_name=android_sim_XXXXX sensors=[RGB,THERMAL,GSR]
[RECV] ACK cmd=HELLO device_id=device_127.0.0.1_XXXXX
[SUCCESS] HELLO handshake completed
```

### Server Log
```
2025-10-01 12:01:15,734 - __main__ - INFO - New connection from ('127.0.0.1', 44424)
2025-10-01 12:01:15,735 - __main__ - INFO - Device registered: android_sim_1759320075 (sensors: RGB, THERMAL, GSR)
```

## Troubleshooting

### "Connection refused"
- Make sure the PC controller is running first
- Check the port number matches (default: 9090)

### "Address already in use"
- Another process is using the port
- Try a different port: `--port 9091`
- Or kill the existing process: `pkill -f unified_pc_controller`

### "Connection timeout"
- Check firewall settings
- Verify server is listening: `netstat -tuln | grep 9090`

## Files

- `simulate_android_phone.py` - Android phone TCP client simulator
- `test_phone_pc_connection.py` - Automated test runner
- `PHONE_PC_CONNECTION_VERIFICATION.md` - Detailed verification report

## What's Tested

- TCP socket connection
- HELLO handshake protocol
- Message format compatibility
- Bidirectional communication
- Graceful disconnect

## Results

See `PHONE_PC_CONNECTION_VERIFICATION.md` for detailed test results and verification status.
