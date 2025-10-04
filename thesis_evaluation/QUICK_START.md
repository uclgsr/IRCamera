# Quick Start Guide - Thesis Evaluation Tests

## Prerequisites
1. Android device running IRCamera app
2. Device connected to same network as PC
3. Python 3.7+ installed
4. Network server enabled on Android (auto-starts with app)

## Installation
```bash
# Install dependencies
pip install -r requirements.txt

# Validate setup
python3 validate_setup.py
```

## Running Tests

### Option 1: Interactive Menu
```bash
./example_usage.sh 192.168.1.100
```

### Option 2: Individual Tests
```bash
# Test 1: Start/Stop (10 second session)
python3 tests/test_1_remote_start_stop.py --device-ip 192.168.1.100 --duration 10

# Test 2: Latency (10 iterations per command)
python3 tests/test_2_command_latency_throughput.py --device-ip 192.168.1.100 --iterations 10

# Test 3: Edge cases
python3 tests/test_3_edge_case_commands.py --device-ip 192.168.1.100

# Test 4: Multi-command sequences
python3 tests/test_4_multi_command_sequence.py --device-ip 192.168.1.100 --scenario all
```

### Option 3: Run All Tests
```bash
python3 tests/run_all_tests.py --device-ip 192.168.1.100
```

## Output Files
- `*.json` - Machine-readable data
- `*.txt` - Human-readable reports
- Generated in the `thesis_evaluation/` directory

## Common Issues

### Connection Failed
- Check device IP address
- Ensure both devices on same network
- Verify IRCamera app is running
- Check port 8080 is accessible

### Tests Timeout
- Increase duration: `--duration 20`
- Check network stability
- Restart Android app

## For Thesis
- Use JSON files for data analysis
- Use TXT files for report inclusion
- Master report combines all results
- Timeline visualizations in Test 4

## Help
```bash
python3 tests/test_1_remote_start_stop.py --help
python3 tests/run_all_tests.py --help
```
