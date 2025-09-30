# IRCamera Scripts Directory

This directory contains all consolidated shell scripts for the IRCamera project.

## Quick Start

Use the master script for all operations:

```bash
./scripts/ircamera.sh <command> [options]
```

## Available Scripts

### Master Script: ircamera.sh
Main entry point that routes to other scripts.

**Usage:**
```bash
./scripts/ircamera.sh help
```

### Testing: test.sh
Consolidated testing suite combining accessibility, integration, performance, and validation testing.

**Replaces:**
- accessibility_test_suite.sh
- integration_test_suite.sh
- performance_benchmark.sh
- run_comprehensive_tests.sh
- validate_test_results.sh

**Usage:**
```bash
# Run all tests
./scripts/ircamera.sh test all

# Run specific test type
./scripts/ircamera.sh test accessibility
./scripts/ircamera.sh test integration
./scripts/ircamera.sh test performance
./scripts/ircamera.sh test comprehensive
./scripts/ircamera.sh test validate
```

**Test Types:**
- `accessibility` - Compose UI accessibility and semantic compliance
- `integration` - Sensor hardware and EventBus integration
- `performance` - Performance benchmarks and profiling
- `comprehensive` - Complete test suite (unit + instrumentation)
- `validate` - Validate existing test results
- `all` - Run all test types (default)

### Verification: verify.sh
Consolidated verification combining build fixes and Compose migration verification.

**Replaces:**
- verify_build_fixes.sh
- verify_compose_migration.sh

**Usage:**
```bash
# Verify everything
./scripts/ircamera.sh verify all

# Verify specific aspect
./scripts/ircamera.sh verify build
./scripts/ircamera.sh verify dependencies
./scripts/ircamera.sh verify migration
```

**Verify Types:**
- `build` - Verify build fixes (manifest duplicates, gradle)
- `dependencies` - Verify Compose dependencies
- `migration` - Verify Compose migration status
- `all` - Verify everything (default)

### Connection: connect.sh
Connect to Android device for live preview streaming.

**Replaces:**
- pc-controller/connect_to_android.sh

**Usage:**
```bash
./scripts/ircamera.sh connect 192.168.1.100
./scripts/ircamera.sh connect 192.168.1.100 --port 8080 --duration 60
```

## Direct Script Access

You can also call scripts directly:

```bash
# Testing
./scripts/test.sh [test-type]

# Verification
./scripts/verify.sh [verify-type]

# Connection
./scripts/connect.sh <android-ip> [--port PORT] [--duration SECONDS]
```

## Test Results

All test results are stored in the `test-results/` directory with timestamps:

```
test-results/
├── accessibility/
├── integration/
├── benchmark/
├── comprehensive/
└── validation/
```

## Scripts Not Included

The following files remain in the root directory:

- **gradlew** and **gradlew.bat** - Standard Gradle wrapper scripts (required by Gradle)

## Migration from Old Scripts

### Old Script Locations

Previously scripts were scattered in:
- Root directory: 8 shell scripts
- pc-controller/: 1 shell script
- Root directory: 1 batch file (gradlew.bat)

### New Consolidated Structure

All scripts now in `scripts/` directory with clear organization:
- **ircamera.sh** - Master entry point
- **test.sh** - All testing consolidated
- **verify.sh** - All verification consolidated
- **connect.sh** - Device connection

### Quick Migration Guide

| Old Command | New Command |
|-------------|-------------|
| `./accessibility_test_suite.sh` | `./scripts/ircamera.sh test accessibility` |
| `./integration_test_suite.sh` | `./scripts/ircamera.sh test integration` |
| `./performance_benchmark.sh` | `./scripts/ircamera.sh test performance` |
| `./run_comprehensive_tests.sh` | `./scripts/ircamera.sh test comprehensive` |
| `./validate_test_results.sh` | `./scripts/ircamera.sh test validate` |
| `./verify_build_fixes.sh` | `./scripts/ircamera.sh verify build` |
| `./verify_compose_migration.sh` | `./scripts/ircamera.sh verify migration` |
| `./pc-controller/connect_to_android.sh` | `./scripts/ircamera.sh connect` |

## Benefits

### Organization
- All scripts in one location
- Clear naming and structure
- Easy to find and use

### Consolidation
- 8 separate scripts → 4 consolidated scripts
- Reduced duplication of common code
- Single source of truth for each function

### Usability
- Master script provides unified interface
- Consistent command-line options
- Better help documentation

### Maintainability
- Easier to update (fewer files)
- Shared functions reduce code duplication
- Clear separation of concerns

## Development

### Adding New Functionality

To add new functionality:

1. Add to appropriate consolidated script (test.sh, verify.sh, etc.)
2. Update the master script (ircamera.sh) if needed
3. Update this README

### Testing Scripts

Test scripts locally before committing:

```bash
# Test individual scripts
./scripts/test.sh --help
./scripts/verify.sh --help

# Test master script
./scripts/ircamera.sh help
```

## Support

For issues or questions about scripts:
1. Check this README
2. Run `./scripts/ircamera.sh help` for usage
3. Check script source code for details
