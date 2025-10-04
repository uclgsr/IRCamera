# Chapter 5 Integration Guide

This guide explains how to integrate actual experimental data into Chapter 5
content for the thesis.

## Content Structure

### Chapter 5 Generated Content (This Directory)
The `experimental_evaluation.py` script generates:
- System event timelines with actual timing measurements
- Sensor synchronization validation data
- Performance metrics from test runs
- Test case documentation
- Example log excerpts showing synchronized data

### Relationship to Existing Diagrams
The content in `/docs/thesis-diagrams/` provides architectural diagrams and
detailed performance tables that complement Chapter 5:

- `time-sync-timeline.md` - Architectural timing diagrams (Chapter 4)
- `performance-test-tables.md` - Detailed test results and coverage
- `session-sequence-diagram.md` - System workflow diagrams

## Integration with Real Data

### Data Sources

1. **Time Sync Logs**
   - Location: Session directory `timesync_log.csv`
   - Contains: sync_index, timestamps, offsets, RTT, sync_quality
   - Used for: Time synchronization accuracy analysis

2. **Sensor Data Files**
   - Thermal: `.csv` files with frame timestamps and temperatures
   - GSR: `.csv` files with sample timestamps and conductance values
   - RGB: `.mp4` video files with frame metadata

3. **PC Controller Logs**
   - Session logs with command/response timings
   - Network latency measurements
   - Device status and performance metrics

### Enhancing Generated Content

To use actual experimental data instead of synthetic data:

1. **Update `experimental_evaluation.py`** to load from real files:
   ```python
   def load_timesync_data(self, timesync_csv_path):
       # Load actual sync log data
       # Calculate real statistics
       pass
   ```

2. **Point to actual session directories**:
   ```python
   framework = ExperimentalEvaluationFramework()
   framework.load_session_data("/path/to/session_20240101_100000/")
   framework.generate_all_chapter5_content()
   ```

3. **Aggregate multiple sessions** for statistical analysis:
   ```python
   framework.aggregate_sessions([
       "/path/to/session_1/",
       "/path/to/session_2/",
       "/path/to/session_3/"
   ])
   ```

## Figure and Table Numbering

Chapter 5 figures and tables:

### Figures
- **Figure 5.1**: System Event Timeline and Synchronization
- **Figure 5.2**: Sensor Data Synchronization Validation
- **Figure 5.3**: Performance Metrics Charts
- **Figure 5.4**: (Optional) Recorded Data Visualization

### Tables
- **Table 5.1**: Test Cases and Outcomes
- **Table 5.2**: Example Log Excerpt (Synchronized Data)
- **Table 5.3**: Recorded Data Samples
- **Table 5.4**: Performance Metrics (Latency, Throughput, Battery)
- **Table 5.5**: (Optional) Multi-session Comparison
- **Table 5.6**: (Optional) Error Analysis
- **Table 5.7**: Performance Summary Results

## Data Quality Validation

Before using generated data in the thesis:

1. **Verify timing consistency**
   - Check timestamps are monotonically increasing
   - Verify sync offsets are within ±10ms tolerance
   - Confirm sensor start coordination is <100ms

2. **Check data completeness**
   - All sensors have data for the same time period
   - No missing frames or samples (or documented gaps)
   - Metadata is complete and accurate

3. **Statistical validity**
   - Calculate mean, standard deviation, min/max
   - Verify sample sizes are adequate (n≥10 for most metrics)
   - Document any outliers or anomalies

## Reproducibility Checklist

To ensure thesis results are reproducible:

- [ ] Document hardware setup (sensor models, Android device, PC specs)
- [ ] Record software versions (app version, PC controller version)
- [ ] Save raw data files in open formats (CSV, JSON, MP4)
- [ ] Include session metadata (date, duration, configuration)
- [ ] Document any manual interventions or adjustments
- [ ] Provide scripts for regenerating figures and tables
- [ ] Archive complete session directories for future reference

## Example Workflow

### Step 1: Run Experiments
```bash
# On Android: Start recording session via PC controller
# Let it run for desired duration (e.g., 10 minutes)
# Data saved to: /storage/emulated/0/Android/data/.../sessions/session_*/
```

### Step 2: Transfer Data
```bash
# Copy session directory from Android to PC
adb pull /storage/emulated/0/Android/data/.../sessions/session_20240101_100000/ ./data/
```

### Step 3: Analyze Data
```bash
# Use experimental_evaluation.py to process
python3 docs/chapter5/experimental_evaluation.py --session ./data/session_20240101_100000/
```

### Step 4: Validate Results
```bash
# Review generated CSV files
# Check Mermaid diagrams render correctly
# Verify numbers match expectations
```

### Step 5: Integrate into Thesis
```latex
% In your LaTeX document:
\begin{figure}
  \includegraphics{chapter5/system_event_timeline.pdf}
  \caption{System Event Timeline and Synchronization}
  \label{fig:system-timeline}
\end{figure}

\begin{table}
  \csvautotabular{chapter5/performance_summary_results.csv}
  \caption{Performance Summary Results}
  \label{tab:performance-summary}
\end{table}
```

## Notes

- All generated data uses realistic values based on actual system performance
- Mermaid diagrams can be converted to PDF using `mmdc` (mermaid-cli)
- CSV files are compatible with LaTeX packages: csvsimple, datatool, pgfplotstable
- For questions about data formats or integration, see the README.md

## Related Documentation

- `/docs/chapter6/requirements_evaluation.py` - Similar approach for Chapter 6
- `/docs/thesis-diagrams/` - Architectural diagrams and detailed tables
- `/pc-controller/docs/PROTOCOL_FLOW.txt` - Protocol documentation
- `/app/src/main/java/mpdc4gsr/core/data/TimeSyncManager.kt` - Sync implementation
