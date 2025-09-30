#!/bin/bash

# IRCamera Performance Benchmarking Suite
# Validates performance claims for Compose migration

echo "🏃 IRCamera Performance Benchmarking Suite"
echo "=========================================="

# Configuration
BENCHMARK_DURATION=30
OUTPUT_DIR="benchmark-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_FILE="$OUTPUT_DIR/benchmark_$TIMESTAMP.json"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Function to check if app is installed
check_app_installed() {
    echo " Checking IRCamera app installation..."
    if adb shell pm list packages | grep -q "com.csl.irCamera"; then
        echo "   IRCamera app found"
        return 0
    else
        echo "   IRCamera app not found. Please install the app first."
        return 1
    fi
}

# Function to benchmark FPS during thermal data display
benchmark_thermal_fps() {
    echo ""
    echo " Benchmarking Thermal Data Display FPS..."
    
    # Start thermal activity and monitor FPS
    adb shell am start -n com.csl.irCamera/.MainActivity
    sleep 3
    
    # Navigate to thermal camera
    adb shell input tap 200 400  # Approximate thermal camera button location
    sleep 2
    
    # Monitor FPS for the benchmark duration
    echo "   Monitoring FPS for ${BENCHMARK_DURATION} seconds..."
    adb shell "dumpsys gfxinfo com.csl.irCamera reset" > /dev/null 2>&1
    sleep "$BENCHMARK_DURATION"
    
    # Get FPS data
    FPS_DATA=$(adb shell "dumpsys gfxinfo com.csl.irCamera" | grep -A 128 "Profile data in ms")
    
    # Calculate average FPS (simplified)
    FRAME_COUNT=$(echo "$FPS_DATA" | wc -l)
    AVERAGE_FPS=$((FRAME_COUNT * 60 / BENCHMARK_DURATION))
    
    echo "   Average FPS: $AVERAGE_FPS frames/second"
    
    # Record result
    echo "    \"thermal_fps\": {" >> "$RESULTS_FILE"
    echo "      \"average_fps\": $AVERAGE_FPS," >> "$RESULTS_FILE"
    echo "      \"target_fps\": 60," >> "$RESULTS_FILE"
    echo "      \"status\": \"$([ $AVERAGE_FPS -ge 30 ] && echo 'PASS' || echo 'FAIL')\"" >> "$RESULTS_FILE"
    echo "    }," >> "$RESULTS_FILE"
}

# Function to benchmark memory usage
benchmark_memory_usage() {
    echo ""
    echo "🧠 Benchmarking Memory Usage..."
    
    # Get baseline memory usage
    BASELINE_MEMORY=$(adb shell "dumpsys meminfo com.csl.irCamera | grep 'TOTAL PSS' | awk '{print \$3}'" | head -1)
    echo "   Baseline memory usage: ${BASELINE_MEMORY}KB"
    
    # Start thermal camera activity
    adb shell am start -n com.csl.irCamera/.MainActivity
    sleep 5
    
    # Navigate to thermal camera and start recording
    adb shell input tap 200 400  # Thermal camera
    sleep 3
    adb shell input tap 400 600  # Start recording (approximate)
    sleep 10
    
    # Measure peak memory usage
    PEAK_MEMORY=$(adb shell "dumpsys meminfo com.csl.irCamera | grep 'TOTAL PSS' | awk '{print \$3}'" | head -1)
    echo "   Peak memory usage: ${PEAK_MEMORY}KB"
    
    # Calculate memory increase
    MEMORY_INCREASE=$((PEAK_MEMORY - BASELINE_MEMORY))
    echo "   Memory increase: ${MEMORY_INCREASE}KB"
    
    # Record result
    echo "    \"memory_usage\": {" >> "$RESULTS_FILE"
    echo "      \"baseline_kb\": $BASELINE_MEMORY," >> "$RESULTS_FILE"
    echo "      \"peak_kb\": $PEAK_MEMORY," >> "$RESULTS_FILE"
    echo "      \"increase_kb\": $MEMORY_INCREASE," >> "$RESULTS_FILE"
    echo "      \"status\": \"$([ $MEMORY_INCREASE -le 100000 ] && echo 'PASS' || echo 'FAIL')\"" >> "$RESULTS_FILE"
    echo "    }," >> "$RESULTS_FILE"
}

# Function to benchmark battery usage
benchmark_battery_usage() {
    echo ""
    echo "🔋 Benchmarking Battery Usage..."
    
    # Reset battery stats
    adb shell "dumpsys batterystats --reset" > /dev/null 2>&1
    sleep 2
    
    # Get initial battery level
    INITIAL_BATTERY=$(adb shell "dumpsys battery | grep level | awk -F: '{print \$2}' | tr -d ' '")
    echo "   Initial battery level: ${INITIAL_BATTERY}%"
    
    # Start intensive thermal recording session
    adb shell am start -n com.csl.irCamera/.MainActivity
    sleep 3
    adb shell input tap 200 400  # Thermal camera
    sleep 3
    adb shell input tap 400 600  # Start recording
    
    echo "  ⏱️ Running battery test for ${BENCHMARK_DURATION} seconds..."
    sleep "$BENCHMARK_DURATION"
    
    # Stop recording
    adb shell input tap 400 600  # Stop recording
    sleep 2
    
    # Get final battery level
    FINAL_BATTERY=$(adb shell "dumpsys battery | grep level | awk -F: '{print \$2}' | tr -d ' '")
    echo "   Final battery level: ${FINAL_BATTERY}%"
    
    # Calculate battery drain rate
    BATTERY_DRAIN=$((INITIAL_BATTERY - FINAL_BATTERY))
    DRAIN_RATE_PER_HOUR=$((BATTERY_DRAIN * 3600 / BENCHMARK_DURATION))
    
    echo "   Battery drain rate: ${DRAIN_RATE_PER_HOUR}%/hour"
    
    # Record result
    echo "    \"battery_usage\": {" >> "$RESULTS_FILE"
    echo "      \"initial_percent\": $INITIAL_BATTERY," >> "$RESULTS_FILE"
    echo "      \"final_percent\": $FINAL_BATTERY," >> "$RESULTS_FILE"
    echo "      \"drain_percent_per_hour\": $DRAIN_RATE_PER_HOUR," >> "$RESULTS_FILE"
    echo "      \"status\": \"$([ $DRAIN_RATE_PER_HOUR -le 20 ] && echo 'PASS' || echo 'FAIL')\"" >> "$RESULTS_FILE"
    echo "    }," >> "$RESULTS_FILE"
}

# Function to benchmark startup time
benchmark_startup_time() {
    echo ""
    echo " Benchmarking App Startup Time..."
    
    # Force stop the app
    adb shell am force-stop com.csl.irCamera
    sleep 2
    
    # Measure cold start time
    echo "  ⏱️ Measuring cold start time..."
    START_TIME=$(date +%s%3N)
    adb shell am start -W -n com.csl.irCamera/.MainActivity | grep "TotalTime" | awk '{print $2}' > /tmp/startup_time.txt
    STARTUP_TIME=$(cat /tmp/startup_time.txt 2>/dev/null || echo "0")
    
    echo "   Cold start time: ${STARTUP_TIME}ms"
    
    # Record result
    echo "    \"startup_time\": {" >> "$RESULTS_FILE"
    echo "      \"cold_start_ms\": $STARTUP_TIME," >> "$RESULTS_FILE"
    echo "      \"target_ms\": 3000," >> "$RESULTS_FILE"
    echo "      \"status\": \"$([ $STARTUP_TIME -le 3000 ] && echo 'PASS' || echo 'FAIL')\"" >> "$RESULTS_FILE"
    echo "    }" >> "$RESULTS_FILE"
}

# Function to generate performance report
generate_report() {
    echo ""
    echo " Generating Performance Report..."
    
    # Close JSON structure
    sed -i '$s/,$//' "$RESULTS_FILE"  # Remove last comma
    echo "  }" >> "$RESULTS_FILE"
    echo "}" >> "$RESULTS_FILE"
    
    # Create HTML report
    HTML_REPORT="$OUTPUT_DIR/performance_report_$TIMESTAMP.html"
    cat > "$HTML_REPORT" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>IRCamera Performance Benchmark Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #2196F3; color: white; padding: 20px; border-radius: 5px; }
        .metric { background: #f5f5f5; margin: 10px 0; padding: 15px; border-radius: 5px; }
        .pass { border-left: 5px solid #4CAF50; }
        .fail { border-left: 5px solid #f44336; }
        .benchmark-date { color: #666; font-size: 14px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🏃 IRCamera Performance Benchmark Report</h1>
        <p class="benchmark-date">Generated: $(date)</p>
    </div>
    
    <h2> Performance Metrics</h2>
    <div id="metrics">
        <!-- Metrics will be populated by JavaScript -->
    </div>
    
    <script>
        // Load and display benchmark results
        fetch('benchmark_$TIMESTAMP.json')
            .then(response => response.json())
            .then(data => {
                const metricsContainer = document.getElementById('metrics');
                
                // Thermal FPS
                if (data.thermal_fps) {
                    const fps = data.thermal_fps;
                    metricsContainer.innerHTML += 
                        '<div class="metric ' + (fps.status === 'PASS' ? 'pass' : 'fail') + '">' +
                        '<h3> Thermal Display FPS</h3>' +
                        '<p>Average FPS: ' + fps.average_fps + ' (Target: ' + fps.target_fps + ')</p>' +
                        '<p>Status: ' + fps.status + '</p>' +
                        '</div>';
                }
                
                // Memory Usage
                if (data.memory_usage) {
                    const mem = data.memory_usage;
                    metricsContainer.innerHTML += 
                        '<div class="metric ' + (mem.status === 'PASS' ? 'pass' : 'fail') + '">' +
                        '<h3>🧠 Memory Usage</h3>' +
                        '<p>Peak Memory: ' + (mem.peak_kb / 1024).toFixed(1) + ' MB</p>' +
                        '<p>Memory Increase: ' + (mem.increase_kb / 1024).toFixed(1) + ' MB</p>' +
                        '<p>Status: ' + mem.status + '</p>' +
                        '</div>';
                }
                
                // Battery Usage
                if (data.battery_usage) {
                    const bat = data.battery_usage;
                    metricsContainer.innerHTML += 
                        '<div class="metric ' + (bat.status === 'PASS' ? 'pass' : 'fail') + '">' +
                        '<h3>🔋 Battery Usage</h3>' +
                        '<p>Drain Rate: ' + bat.drain_percent_per_hour + '%/hour</p>' +
                        '<p>Status: ' + bat.status + '</p>' +
                        '</div>';
                }
                
                // Startup Time
                if (data.startup_time) {
                    const start = data.startup_time;
                    metricsContainer.innerHTML += 
                        '<div class="metric ' + (start.status === 'PASS' ? 'pass' : 'fail') + '">' +
                        '<h3> Startup Time</h3>' +
                        '<p>Cold Start: ' + start.cold_start_ms + 'ms (Target: <' + start.target_ms + 'ms)</p>' +
                        '<p>Status: ' + start.status + '</p>' +
                        '</div>';
                }
            })
            .catch(error => {
                document.getElementById('metrics').innerHTML = 
                    '<p>Error loading benchmark data: ' + error + '</p>';
            });
    </script>
</body>
</html>
EOF
    
    echo "   Results saved to: $RESULTS_FILE"
    echo "   HTML report saved to: $HTML_REPORT"
    echo ""
    echo " Performance Summary:"
    cat "$RESULTS_FILE" | grep -E "(average_fps|peak_kb|drain_percent_per_hour|cold_start_ms)" | sed 's/^/  /'
}

# Main execution
main() {
    # Check prerequisites
    if ! command -v adb &> /dev/null; then
        echo " ADB not found. Please install Android SDK platform tools."
        exit 1
    fi
    
    # Check device connection
    if ! adb devices | grep -q "device$"; then
        echo " No Android device connected. Please connect a device and enable USB debugging."
        exit 1
    fi
    
    # Check app installation
    if ! check_app_installed; then
        exit 1
    fi
    
    # Initialize results file
    echo "{" > "$RESULTS_FILE"
    echo "  \"benchmark_info\": {" >> "$RESULTS_FILE"
    echo "    \"timestamp\": \"$(date -Iseconds)\"," >> "$RESULTS_FILE"
    echo "    \"duration_seconds\": $BENCHMARK_DURATION," >> "$RESULTS_FILE"
    echo "    \"device\": \"$(adb shell getprop ro.product.model | tr -d '\r')\"" >> "$RESULTS_FILE"
    echo "  }," >> "$RESULTS_FILE"
    echo "  \"results\": {" >> "$RESULTS_FILE"
    
    # Run benchmarks
    benchmark_startup_time
    benchmark_thermal_fps
    benchmark_memory_usage
    benchmark_battery_usage
    
    # Generate report
    generate_report
    
    echo " Performance benchmarking completed!"
    echo " Results available in: $OUTPUT_DIR/"
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi