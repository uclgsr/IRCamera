# PC Controller Test Suite

This directory contains test files for the PC Controller application.

## Test Files

- **test_protocol_compatibility.py** - Tests protocol adapter and message parsing
- **test_pc_controller_features.py** - Tests core controller features (native backend, networking, etc.)
- **test_protocol_verification.py** - Comprehensive protocol verification with mock Android device
- **test_comprehensive_integration.py** - Integration tests for PC-Android communication

## Running Tests

Note: Most tests are currently disabled (skipped) as they require specific dependencies or running services.

```bash
# Run specific test file
python -m unittest tests.test_protocol_compatibility

# Run all tests (if pytest is installed)
pytest tests/

# Run with verbose output
python -m unittest -v tests.test_protocol_compatibility
```

## Test Dependencies

Some tests require additional dependencies:
- Native backend tests require C++ backend to be built
- Integration tests may require running services or mock devices
