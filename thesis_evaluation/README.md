# Thesis Evaluation Tests

Multi-sensor data consistency tests for thesis evaluation (Chapters 5 and 6).

## Test Categories

### 1. Cross-Sensor Timeline Alignment (Synthetic Event)
Tests synchronized reference event injection across all data streams.

**Purpose**: Verify that all sensors record events simultaneously within acceptable tolerance.

**Output**: Combined log/chart showing GSR spike, thermal marker, and RGB timestamp at injected event time.

**Subsystem**: Multi-sensor integration (timing consistency)

**Chapters**: Chapter 5 (synchronized sensor behavior) and Chapter 6 (cross-modal synchronization)

### 2. Multi-Stream Sync Marker Test (Real Run)
Tests synchronization during real hardware recording with SYNC command.

**Purpose**: Verify sensor modalities remain time-aligned during actual recording session.

**Output**: Logs from each sensor stream around sync event with timestamped entries.

**Subsystem**: System synchronization (GSR, thermal, RGB combined)

**Chapters**: Chapter 5 (sensor data correlation) and Chapter 6 (timeline alignment effectiveness)

### 3. Session Duration Consistency Check
Tests recording duration consistency across all sensor streams.

**Purpose**: Verify all streams covered the same overall period.

**Output**: CSV/table with start/end timestamps from each sensor log.

**Subsystem**: Session controller (start/stop coordination)

**Chapters**: Chapter 5 (concurrent sensor operation) and Chapter 6 (sensor lag/lead analysis)

## Running Tests

```bash
# Run all thesis evaluation tests
python -m pytest thesis_evaluation/

# Run specific test module
python -m pytest thesis_evaluation/test_cross_sensor_alignment.py -v

# Run with detailed output
python -m pytest thesis_evaluation/ -v --tb=short

# Generate test report
python -m pytest thesis_evaluation/ --html=thesis_evaluation/report.html
```

## Test Requirements

- Python 3.8+
- pytest
- pandas (optional, for enhanced reporting)
- numpy (optional, for data analysis)

Install dependencies:
```bash
pip install -r requirements_thesis.txt
```

## Test Data

Tests use both simulated and real session data:
- Simulated data: Generated within tests for controlled scenarios
- Real data: Located in `pc-controller/exports/` directory

## Test Outputs

Test outputs are saved to:
- `thesis_evaluation/outputs/` - Generated test artifacts
- `thesis_evaluation/reports/` - Test result reports
- `thesis_evaluation/charts/` - Visualization outputs (if matplotlib available)
