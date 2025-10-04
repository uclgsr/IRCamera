# Test Manifest - Robustness Tests

Quick reference for all thesis evaluation robustness tests.

## Test Summary Table

| Test ID | Test Name | Type | Duration | Hardware Required | Expected Result |
|---------|-----------|------|----------|-------------------|-----------------|
| RT-001 | GSR Reconnection (Simulated) | Automated | 60s | None | Pass - Auto reconnect after 20s gap |
| RT-002 | GSR Reconnection (Real) | Manual | Variable | Shimmer3 GSR | Pass - Detect disconnect, auto reconnect |
| RT-003 | Thermal Disconnect | Manual | Variable | USB Thermal Camera | Pass - Graceful handling, no crash |
| RT-004 | Network Drop | Manual | Variable | PC Controller | Pass - Recording continues |
| RT-005 | Sensor Isolation | Automated | 15s | None | Pass - Failure contained |

## Test Execution Checklist

### Pre-Test Requirements
- [ ] IRCamera app installed on device
- [ ] Required hardware connected (for hardware tests)
- [ ] Storage permissions granted
- [ ] Testing Suite Hub accessible
- [ ] Previous test logs backed up (if needed)

### Post-Test Verification
- [ ] Log file generated
- [ ] Metrics calculated correctly
- [ ] Expected events logged
- [ ] No system crashes
- [ ] Data integrity maintained

## Test Result Classification

### Pass Criteria
- **GSR Reconnection (Simulated)**
  - [x] Disconnect detected at 20s
  - [x] Reconnection attempts logged
  - [x] Reconnect successful at 40s
  - [x] Data gap measured
  - [x] No data corruption

- **GSR Reconnection (Real)**
  - [x] Real disconnect detected
  - [x] Automatic reconnection attempted
  - [x] Reconnection successful or failure documented
  - [x] Data gap recorded in CSV
  - [x] Events logged with timestamps

- **Thermal Disconnect**
  - [x] USB disconnect detected
  - [x] Error logged properly
  - [x] App did not crash
  - [x] Other sensors continued
  - [x] Switched to simulation mode

- **Network Drop**
  - [x] Network loss detected
  - [x] Recording continued
  - [x] No data loss
  - [x] Reconnection attempts logged
  - [x] Duration after drop measured

- **Sensor Isolation**
  - [x] Selected sensor failed
  - [x] Failure logged
  - [x] Other sensors unaffected
  - [x] Sample counts correct
  - [x] No cascade failures

## Expected Metrics

### RT-001: GSR Reconnection (Simulated)
```
Total Duration: 60s
Disconnect Duration: 20s
Data Gap: 20000ms
Reconnection Attempts: 4
Test Result: PASSED
```

### RT-002: GSR Reconnection (Real)
```
Time to Disconnect: 15-30s (variable)
Disconnect Duration: 5-60s (variable)
Data Gap: Measured in ms
Samples Before: ~1920-3840 (128 Hz * 15-30s)
Samples After: Continues until stop
Test Result: PASSED if auto-reconnect works
```

### RT-003: Thermal Disconnect
```
Time to Disconnect: 15-30s (manual trigger)
Frames Before: ~150-300 (10 fps * 15-30s)
System Crashed: false
Graceful Handling: true
Other Sensors OK: true
Test Result: PASSED
```

### RT-004: Network Drop
```
Time to Drop: 15-30s (manual trigger)
Recording After Drop: Continues
Reconnection Attempts: Multiple (every 10s)
Data Loss: false
Recording Continued: true
Test Result: PASSED
```

### RT-005: Sensor Isolation
```
Failure Induced: 5s after start
Failure Contained: true
Other Sensors Continued: true
GSR Samples Before/After: 640/1280
Thermal Frames Before/After: 50/100
Test Result: PASSED
```

## Log File Analysis

### Expected Log Events

#### GSR Tests
```
TEST_START - Test initialization
GSR_DISCONNECTED - Disconnect detected
RECONNECTION_ATTEMPT - Each attempt
GSR_RECONNECTED - Successful reconnect
TEST_COMPLETE - Test finished
```

#### Thermal Test
```
RECORDING_START - Recording begins
THERMAL_DISCONNECTED - USB unplugged
SIMULATION_MODE - Switch to simulation
RECORDING_STOP - Test complete
```

#### Network Test
```
RECORDING_START - With network
NETWORK_LOST - Connection dropped
RECONNECTION_ATTEMPT - Recovery attempts
NETWORK_RECONNECTED - If reconnects
RECORDING_STOP - Manual stop
```

#### Sensor Isolation Test
```
TEST_START - All sensors init
ALL_SENSORS_STARTED - Recording begins
FAILURE_INDUCED - Selected sensor fails
SENSOR_STOPPED - Failed sensor stops
OTHER_SENSORS_CONTINUE - Others OK
TEST_COMPLETE - Test finished
```

## Thesis Documentation Requirements

### For Each Test, Document:

1. **Test Setup**
   - Hardware configuration
   - Software version
   - Initial conditions

2. **Test Execution**
   - Steps performed
   - Timing of events
   - Manual interventions

3. **Test Results**
   - All metrics collected
   - Pass/fail status
   - Unexpected behaviors

4. **Log Files**
   - Location and filename
   - Key events and timestamps
   - Data gaps or anomalies

5. **Analysis**
   - System behavior interpretation
   - Comparison to expected results
   - Robustness assessment

## Common Test Scenarios

### Scenario 1: Complete Success Path
All tests pass with expected metrics, demonstrating full system robustness.

**Documentation Focus:**
- Highlight successful reconnection mechanisms
- Emphasize graceful degradation
- Show no data loss

### Scenario 2: Partial Failure
Some tests reveal issues (e.g., no auto-reconnect for GSR real hardware).

**Documentation Focus:**
- Document current limitations
- Explain manual intervention needed
- Propose improvements

### Scenario 3: Edge Cases
Tests reveal unexpected behaviors under specific conditions.

**Documentation Focus:**
- Describe edge case conditions
- Document workarounds
- Suggest future enhancements

## Data Collection Template

For each test run:

```
Test ID: RT-XXX
Test Name: [Name]
Date/Time: [Timestamp]
Device: [Device Model]
OS Version: [Android Version]
App Version: [Version]

Hardware:
- GSR Device: [Model/Serial] or N/A
- Thermal Camera: [Model] or N/A
- PC Controller: [Connected/Not Connected]

Test Results:
- Duration: [Time]
- Key Metrics: [List]
- Pass/Fail: [Status]
- Notes: [Observations]

Log File: [Filename]
```

## Quality Assurance

### Before Submitting Results:
- [ ] All required tests executed
- [ ] Multiple runs completed for consistency
- [ ] Log files preserved and backed up
- [ ] Metrics extracted and tabulated
- [ ] Screenshots captured (if relevant)
- [ ] Unusual behaviors documented
- [ ] Results cross-referenced with requirements

### Data Integrity Checks:
- [ ] Timestamps are sequential and reasonable
- [ ] Sample counts match expected rates
- [ ] No duplicate events in logs
- [ ] File sizes are appropriate
- [ ] All metric fields populated

## Thesis Chapter Mapping

### Chapter 5: Implementation and Testing

**Include:**
- Description of each test
- Test setup and methodology
- Implementation details
- Code snippets from test files

**Evidence:**
- Test execution procedures
- System behavior descriptions
- Error handling demonstrations

### Chapter 6: Evaluation and Results

**Include:**
- Test metrics and results
- Pass/fail analysis
- Robustness assessment
- Comparison with requirements

**Evidence:**
- Metric tables
- Log file excerpts
- Performance graphs
- Gap duration analysis

### Appendices

**Include:**
- Complete test code listings
- Full log file examples
- Test execution screenshots
- Raw data tables

## Revision History

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2024-10-04 | 1.0 | Initial test manifest | Copilot |

## Notes

- Tests are designed to be non-destructive
- Multiple runs recommended for statistical significance
- Real hardware tests may have variable results
- Document all deviations from expected behavior
- Keep original log files for verification
