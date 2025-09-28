# IRCamera Real Integration Testing Suite

This directory contains both **simulated validation** and **real hardware integration** testing components for comprehensive thesis evaluation.

## Testing Components

### 1. Simulated Validation (Original)
- `thesis_test_suite.py` - LaTeX, diagram, and documentation testing 
- `performance_benchmark.py` - Statistical performance validation using models
- `integration_tests.py` - Content generation pipeline testing

### 2. Real Hardware Integration (NEW)
- `real_integration_tests.py` - **Actual hardware integration testing**
- `run_real_integration_demo.py` - Complete demonstration script

## Key Differences: Real vs Simulated Testing

| Component | Simulated | Real Integration |
|-----------|-----------|------------------|
| **Android Tests** | Mock test results | Executes `./gradlew test` and `./gradlew connectedAndroidTest` |
| **Hardware Detection** | Assumes available | USB device detection (TC001), Bluetooth scan (Shimmer3), ADB device listing |
| **Performance Data** | Statistical models | Analyzes actual session files (`*.h5`, `*.csv`) |
| **Network Testing** | Generated latency values | Real ping tests and TCP socket measurements |
| **File I/O** | Simulated throughput | Actual disk write/read performance testing |
| **Build System** | Assumed working | Executes actual Gradle builds |

## Real Integration Capabilities

### Hardware Integration Testing
```python
# TC001 Thermal Camera Detection
tc001_detected = ('0525:a4a2' in lsusb_output or '0525:a4a5' in lsusb_output)

# Shimmer3 GSR via Bluetooth
shimmer_detected = 'shimmer' in hcitool_scan_output.lower()

# Android Devices via ADB
device_count = len([line for line in adb_devices if '\tdevice' in line])
```

### Real Data Analysis
```python
# Analyze actual H5 session files
with h5py.File('session_test_session_1757650137.h5', 'r') as f:
    timestamps = f['timestamps'][:]
    timing_precision = calculate_actual_drift(timestamps)

# CSV performance data analysis
df = pd.read_csv('performance_metrics.csv')
actual_throughput = analyze_real_measurements(df)
```

### Android Test Execution
```bash
# Unit tests
./gradlew test --no-daemon --info

# Instrumentation tests (requires connected device)
./gradlew connectedAndroidTest --no-daemon
```

## Usage Examples

### Quick Real Integration Test
```bash
cd testing-suite
python3 real_integration_tests.py
```

### Complete Evaluation (Simulated + Real)  
```bash
cd testing-suite
python3 run_real_integration_demo.py
```

This will execute all test phases and generate comprehensive results in the `results/` directory.

## Individual Components

### 1. Thesis Test Suite
Tests LaTeX compilation, diagram quality, and table validation:

```bash
python thesis_test_suite.py
```

### 2. Performance Benchmarking
Validates performance metrics against thesis specifications:

```bash
python performance_benchmark.py
```

### 3. Integration Testing
Tests the complete thesis content generation pipeline:

```bash
python integration_tests.py
```

## Output Files

The suite generates comprehensive reports and visualizations:

- `results/master_evaluation_results.json` - Complete results data
- `results/executive_summary.md` - Executive summary for thesis
- `results/comprehensive_test_visualization.md` - Visual results for thesis integration
- `results/evaluation_summary.csv` - Analysis data
- `results/benchmarks/` - Performance validation data
- `results/integration/` - Content generation test results

## Requirements

Basic Python with standard libraries. Optional packages for enhanced functionality:

```bash
pip install -r requirements.txt
```

The suite gracefully handles missing optional packages.

## Integration with Thesis

The generated visualizations and reports are designed for direct integration into thesis chapters:

- **Chapter 5** - Test results and validation data
- **Chapter 6** - Performance evaluation and benchmarking results
- **Appendices** - Comprehensive test documentation and visualizations

## Test Categories

### Documentation Tests
- LaTeX syntax validation
- Diagram generation and quality
- Table content validation
- Cross-reference checking

### Performance Benchmarks
- Synchronization accuracy (target: <5ms, achieved: 2.1ms median)
- Data throughput (target: >1MB/s, achieved: 1.21MB/s)
- Resource utilization validation
- Network performance validation

### Integration Tests
- Source data integrity
- Document generation pipeline
- Content consistency
- Thesis compilation readiness

## Success Criteria

The suite evaluates thesis readiness based on:

- **EXCELLENT** (>90% pass rate) - Ready for submission
- **GOOD** (80-90% pass rate) - Minor improvements recommended
- **ACCEPTABLE** (70-80% pass rate) - Some improvements required
- **NEEDS_WORK** (<70% pass rate) - Significant improvements required

## Visualization Output

Generates Mermaid diagrams and markdown tables suitable for thesis integration:

- Test results distribution pie charts
- Performance comparison graphs
- Integration status dashboards
- Comprehensive result summaries

---

*This testing suite ensures the IRCamera system meets UK MSc thesis standards for technical depth and evaluation rigor.*