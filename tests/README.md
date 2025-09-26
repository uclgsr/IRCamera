# IRCamera Test Scripts

This directory contains test scripts for validating various components of the IRCamera platform.

## Available Tests

### Network and Protocol Tests

- **test_networking_client.py** - Network client functionality tests
- **test_pc_server.py** - PC server functionality tests
- **test_protocol.py** - Protocol implementation validation tests

## Usage

Run tests individually:

```bash
python3 test_networking_client.py
python3 test_pc_server.py  
python3 test_protocol.py
```

## Requirements

Test scripts require the same dependencies as the main application:

```bash
pip install -r ../pc-controller/requirements.txt
```

## Note

These are integration and functionality tests. For unit tests, see the individual module test suites in their respective
directories (e.g., `pc-controller/test_mvp*.py`).