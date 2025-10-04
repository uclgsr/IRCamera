# PC Controller Utility Scripts

This directory contains utility scripts for testing, validation, and demonstration of the PC Controller functionality.

## Available Scripts

### Verification and Testing

#### `verify_installation.py`
Verifies that all required dependencies and components are properly installed.

```bash
python scripts/verify_installation.py
```

Checks:
- Python dependencies
- PyQt6 GUI framework
- Native backend availability
- SSL certificate generation
- Network capabilities

#### `verify_pc_controller.py`
Comprehensive verification of all PC Controller features and integrations.

```bash
python scripts/verify_pc_controller.py
```

Verifies:
- File structure and key files
- Module imports
- Protocol adapter functionality
- Native backend integration
- SSL/TLS certificates
- Protocol implementation
- Network threading
- Data processing capabilities
- Test suite execution

#### `test_android_connection.py`
Tests connectivity between PC Controller and Android devices.

```bash
python scripts/test_android_connection.py
```

Features:
- Device discovery testing
- TCP connection establishment
- Protocol message exchange
- Response validation

### Demonstration

#### `demo_features.py`
Demonstrates key features of the PC Controller framework.

```bash
python scripts/demo_features.py
```

Demonstrates:
- Protocol message handling
- Data processing capabilities
- Native backend integration
- Network operations
- Session management

### Examples

#### `example_sync_server.py`
Example implementation of a time synchronization server.

```bash
python scripts/example_sync_server.py
```

Shows:
- NTP-style time synchronization protocol
- Server implementation patterns
- Timestamp handling
- Network time protocol basics

## Usage Tips

1. **Start with verification**: Run `verify_installation.py` first to ensure your environment is properly set up.

2. **Check comprehensive functionality**: Use `verify_pc_controller.py` to validate all features before development.

3. **Test Android connectivity**: Use `test_android_connection.py` when debugging connection issues with Android devices.

4. **Learn by example**: Review `demo_features.py` and `example_sync_server.py` to understand implementation patterns.

## Integration with Main Application

These scripts are separate from the main application (`pc_controller.py`) but use the same modules and dependencies. They can be used during development, testing, and troubleshooting without affecting the production application.
