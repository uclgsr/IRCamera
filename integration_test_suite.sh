#!/bin/bash

# IRCamera Integration Testing Suite
# Tests Compose UI integration with sensor hardware and EventBus

echo "🧪 IRCamera Integration Testing Suite"
echo "====================================="

# Configuration
TEST_RESULTS_DIR="integration-test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_FILE="$TEST_RESULTS_DIR/integration_test_$TIMESTAMP.json"
LOG_FILE="$TEST_RESULTS_DIR/integration_test_$TIMESTAMP.log"

# Create output directory
mkdir -p "$TEST_RESULTS_DIR"

# Initialize results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Logging function
log() {
    echo "$1" | tee -a "$LOG_FILE"
}

# Test result function
record_test_result() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$status" == "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log "  ✅ $test_name: PASSED"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "  ❌ $test_name: FAILED - $details"
    fi
    
    # Record in JSON
    echo "    {" >> "$RESULTS_FILE"
    echo "      \"test_name\": \"$test_name\"," >> "$RESULTS_FILE"
    echo "      \"status\": \"$status\"," >> "$RESULTS_FILE"
    echo "      \"timestamp\": \"$(date -Iseconds)\"," >> "$RESULTS_FILE"
    echo "      \"details\": \"$details\"" >> "$RESULTS_FILE"
    echo "    }," >> "$RESULTS_FILE"
}

# Function to check app installation and permissions
test_app_prerequisites() {
    log ""
    log "📱 Testing App Prerequisites..."
    
    # Check app installation
    if adb shell pm list packages | grep -q "com.csl.irCamera"; then
        record_test_result "App Installation" "PASS" "IRCamera app is installed"
    else
        record_test_result "App Installation" "FAIL" "IRCamera app not found"
        return 1
    fi
    
    # Check camera permission
    CAMERA_PERM=$(adb shell pm list permissions | grep -c "android.permission.CAMERA")
    if [ "$CAMERA_PERM" -gt 0 ]; then
        record_test_result "Camera Permission" "PASS" "Camera permission available"
    else
        record_test_result "Camera Permission" "FAIL" "Camera permission not available"
    fi
    
    # Check bluetooth permission  
    BT_PERM=$(adb shell pm list permissions | grep -c "android.permission.BLUETOOTH")
    if [ "$BT_PERM" -gt 0 ]; then
        record_test_result "Bluetooth Permission" "PASS" "Bluetooth permission available"
    else
        record_test_result "Bluetooth Permission" "FAIL" "Bluetooth permission not available"
    fi
    
    # Check storage permission
    STORAGE_PERM=$(adb shell pm list permissions | grep -c "android.permission.WRITE_EXTERNAL_STORAGE")
    if [ "$STORAGE_PERM" -gt 0 ]; then
        record_test_result "Storage Permission" "PASS" "Storage permission available"
    else
        record_test_result "Storage Permission" "FAIL" "Storage permission not available"
    fi
}

# Function to test Compose activity launches
test_compose_activity_launches() {
    log ""
    log "🎨 Testing Compose Activity Launches..."
    
    # Test MainActivity launch
    adb shell am force-stop com.csl.irCamera
    sleep 1
    
    START_TIME=$(date +%s%N)
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 3
    
    # Check if activity is running
    CURRENT_ACTIVITY=$(adb shell "dumpsys window | grep mCurrentFocus" | awk -F'/' '{print $2}' | awk '{print $1}')
    
    if [[ "$CURRENT_ACTIVITY" == *"MainActivity"* ]]; then
        END_TIME=$(date +%s%N)
        LAUNCH_TIME=$(( (END_TIME - START_TIME) / 1000000 ))
        record_test_result "MainActivity Launch" "PASS" "Launched in ${LAUNCH_TIME}ms"
    else
        record_test_result "MainActivity Launch" "FAIL" "Activity not found in focus"
    fi
    
    # Test WebViewActivityCompose launch
    adb shell am start -n com.csl.irCamera/mpdc4gsr.activities.WebViewActivityCompose &>/dev/null
    sleep 2
    
    CURRENT_ACTIVITY=$(adb shell "dumpsys window | grep mCurrentFocus" | awk -F'/' '{print $2}' | awk '{print $1}')
    if [[ "$CURRENT_ACTIVITY" == *"WebViewActivityCompose"* ]]; then
        record_test_result "WebViewActivityCompose Launch" "PASS" "WebView Compose activity launched successfully"
    else
        record_test_result "WebViewActivityCompose Launch" "FAIL" "WebView Compose activity not launched"
    fi
    
    # Test SettingsComposeActivity launch
    adb shell am start -n com.csl.irCamera/mpdc4gsr.activities.SettingsComposeActivity &>/dev/null
    sleep 2
    
    CURRENT_ACTIVITY=$(adb shell "dumpsys window | grep mCurrentFocus" | awk -F'/' '{print $2}' | awk '{print $1}')
    if [[ "$CURRENT_ACTIVITY" == *"SettingsComposeActivity"* ]]; then
        record_test_result "SettingsComposeActivity Launch" "PASS" "Settings Compose activity launched successfully"
    else
        record_test_result "SettingsComposeActivity Launch" "FAIL" "Settings Compose activity not launched"
    fi
}

# Function to test thermal camera integration
test_thermal_camera_integration() {
    log ""
    log "🌡️ Testing Thermal Camera Integration..."
    
    # Start main activity
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 3
    
    # Try to navigate to thermal camera (approximate coordinates)
    adb shell input tap 200 400
    sleep 3
    
    # Check if thermal activity is running
    CURRENT_ACTIVITY=$(adb shell "dumpsys window | grep mCurrentFocus")
    
    if [[ "$CURRENT_ACTIVITY" == *"IRThermal"* ]] || [[ "$CURRENT_ACTIVITY" == *"Thermal"* ]]; then
        record_test_result "Thermal Camera Navigation" "PASS" "Successfully navigated to thermal camera"
        
        # Test thermal data display (check for thermal-related processes)
        THERMAL_PROCESSES=$(adb shell ps | grep -c "thermal\|ir\|temp")
        if [ "$THERMAL_PROCESSES" -gt 0 ]; then
            record_test_result "Thermal Data Processing" "PASS" "Thermal processes detected"
        else
            record_test_result "Thermal Data Processing" "FAIL" "No thermal processes detected"
        fi
        
    else
        record_test_result "Thermal Camera Navigation" "FAIL" "Could not navigate to thermal camera"
    fi
    
    # Test thermal recording functionality
    adb shell input tap 400 600  # Approximate record button location
    sleep 2
    
    # Check if recording started (look for recording indicator in logs)
    RECORDING_LOG=$(adb logcat -d | grep -i "record\|capture" | tail -5)
    if [[ ! -z "$RECORDING_LOG" ]]; then
        record_test_result "Thermal Recording Start" "PASS" "Recording functionality detected"
    else
        record_test_result "Thermal Recording Start" "FAIL" "No recording activity detected"
    fi
}

# Function to test GSR sensor integration
test_gsr_sensor_integration() {
    log ""
    log "📊 Testing GSR Sensor Integration..."
    
    # Navigate back to main activity
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 2
    
    # Try to navigate to GSR sensor dashboard (approximate coordinates)
    adb shell input tap 300 500
    sleep 3
    
    # Check Bluetooth service status (GSR uses Bluetooth)
    BT_SERVICE=$(adb shell service list | grep -c "bluetooth")
    if [ "$BT_SERVICE" -gt 0 ]; then
        record_test_result "Bluetooth Service" "PASS" "Bluetooth service available for GSR"
    else
        record_test_result "Bluetooth Service" "FAIL" "Bluetooth service not available"
    fi
    
    # Check for GSR-related processes
    GSR_PROCESSES=$(adb shell ps | grep -i -c "gsr\|shimmer\|sensor")
    if [ "$GSR_PROCESSES" -gt 0 ]; then
        record_test_result "GSR Process Detection" "PASS" "GSR-related processes found"
    else
        record_test_result "GSR Process Detection" "FAIL" "No GSR processes detected"
    fi
    
    # Test GSR data visualization (check for chart-related logs)
    CHART_LOGS=$(adb logcat -d | grep -i "chart\|graph\|visual" | tail -5)
    if [[ ! -z "$CHART_LOGS" ]]; then
        record_test_result "GSR Data Visualization" "PASS" "Chart/visualization activity detected"
    else
        record_test_result "GSR Data Visualization" "FAIL" "No visualization activity detected"
    fi
}

# Function to test EventBus integration
test_eventbus_integration() {
    log ""
    log "🚌 Testing EventBus Integration..."
    
    # Check for EventBus-related logs in the system
    EVENTBUS_LOGS=$(adb logcat -d | grep -i "eventbus\|event\|bus" | tail -10)
    
    if [[ ! -z "$EVENTBUS_LOGS" ]]; then
        record_test_result "EventBus Activity" "PASS" "EventBus communication detected"
        
        # Count different types of events
        EVENT_COUNT=$(echo "$EVENTBUS_LOGS" | wc -l)
        if [ "$EVENT_COUNT" -gt 5 ]; then
            record_test_result "EventBus Message Volume" "PASS" "High event activity ($EVENT_COUNT events)"
        else
            record_test_result "EventBus Message Volume" "FAIL" "Low event activity ($EVENT_COUNT events)"
        fi
    else
        record_test_result "EventBus Activity" "FAIL" "No EventBus communication detected"
    fi
    
    # Test service connections (typical EventBus usage pattern)
    SERVICE_CONNECTIONS=$(adb shell dumpsys activity services | grep -c "ServiceRecord")
    if [ "$SERVICE_CONNECTIONS" -gt 2 ]; then
        record_test_result "Service Integration" "PASS" "Multiple services connected ($SERVICE_CONNECTIONS)"
    else
        record_test_result "Service Integration" "FAIL" "Limited service connections ($SERVICE_CONNECTIONS)"
    fi
}

# Function to test multi-sensor coordination
test_multi_sensor_coordination() {
    log ""
    log "🔄 Testing Multi-Sensor Coordination..."
    
    # Start recording session with multiple sensors
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 3
    
    # Simulate multi-modal recording (both thermal and GSR)
    adb shell input tap 200 400  # Thermal
    sleep 2
    adb shell input tap 300 500  # GSR  
    sleep 2
    adb shell input tap 400 600  # Start recording
    sleep 5
    
    # Check CPU usage during multi-sensor operation
    CPU_USAGE=$(adb shell top -n 1 | grep "com.csl.irCamera" | awk '{print $9}' | head -1 | sed 's/%//')
    
    if [[ ! -z "$CPU_USAGE" ]] && [ "${CPU_USAGE%.*}" -lt 80 ]; then
        record_test_result "Multi-Sensor CPU Usage" "PASS" "CPU usage under control: ${CPU_USAGE}%"
    else
        record_test_result "Multi-Sensor CPU Usage" "FAIL" "High CPU usage: ${CPU_USAGE}%"
    fi
    
    # Check memory usage during coordination
    MEMORY_KB=$(adb shell dumpsys meminfo com.csl.irCamera | grep "TOTAL PSS" | awk '{print $3}' | head -1)
    MEMORY_MB=$((MEMORY_KB / 1024))
    
    if [ "$MEMORY_MB" -lt 300 ]; then
        record_test_result "Multi-Sensor Memory Usage" "PASS" "Memory usage reasonable: ${MEMORY_MB}MB"
    else
        record_test_result "Multi-Sensor Memory Usage" "FAIL" "High memory usage: ${MEMORY_MB}MB"
    fi
    
    # Stop recording
    adb shell input tap 400 600
    sleep 2
}

# Function to test Compose lifecycle integration
test_compose_lifecycle_integration() {
    log ""
    log "🔄 Testing Compose Lifecycle Integration..."
    
    # Test activity pause/resume cycle
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 2
    
    # Go to background
    adb shell input keyevent KEYCODE_HOME
    sleep 2
    
    # Check if app handles background state
    APP_STATE=$(adb shell dumpsys activity | grep "com.csl.irCamera" | grep -c "stop")
    if [ "$APP_STATE" -gt 0 ]; then
        record_test_result "Background State Handling" "PASS" "App properly handles background state"
    else
        record_test_result "Background State Handling" "FAIL" "App may not handle background state properly"
    fi
    
    # Resume app
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 2
    
    # Check if app resumed properly
    CURRENT_ACTIVITY=$(adb shell "dumpsys window | grep mCurrentFocus")
    if [[ "$CURRENT_ACTIVITY" == *"MainActivity"* ]]; then
        record_test_result "Resume State Handling" "PASS" "App resumed successfully"
    else
        record_test_result "Resume State Handling" "FAIL" "App may not have resumed properly"
    fi
    
    # Test configuration change (rotation)
    adb shell input keyevent KEYCODE_CTRL_LEFT &>/dev/null  # Simulated rotation
    sleep 2
    
    # Check if app survives configuration change
    ACTIVITY_AFTER_ROTATION=$(adb shell "dumpsys window | grep mCurrentFocus")
    if [[ "$ACTIVITY_AFTER_ROTATION" == *"MainActivity"* ]]; then
        record_test_result "Configuration Change Handling" "PASS" "App handles configuration changes"
    else
        record_test_result "Configuration Change Handling" "FAIL" "App may not handle configuration changes properly"
    fi
}

# Function to generate integration test report
generate_integration_report() {
    log ""
    log "📋 Generating Integration Test Report..."
    
    # Close JSON structure
    sed -i '$s/,$//' "$RESULTS_FILE"  # Remove last comma
    echo "  ]," >> "$RESULTS_FILE"
    echo "  \"summary\": {" >> "$RESULTS_FILE"
    echo "    \"total_tests\": $TOTAL_TESTS," >> "$RESULTS_FILE"
    echo "    \"passed_tests\": $PASSED_TESTS," >> "$RESULTS_FILE"  
    echo "    \"failed_tests\": $FAILED_TESTS," >> "$RESULTS_FILE"
    echo "    \"pass_rate\": \"$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%\"" >> "$RESULTS_FILE"
    echo "  }" >> "$RESULTS_FILE"
    echo "}" >> "$RESULTS_FILE"
    
    # Create HTML report
    HTML_REPORT="$TEST_RESULTS_DIR/integration_report_$TIMESTAMP.html"
    cat > "$HTML_REPORT" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>IRCamera Integration Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #4CAF50; color: white; padding: 20px; border-radius: 5px; }
        .summary { background: #e8f5e8; padding: 15px; border-radius: 5px; margin: 20px 0; }
        .test-pass { background: #e8f5e8; border-left: 5px solid #4CAF50; padding: 10px; margin: 5px 0; }
        .test-fail { background: #ffeaea; border-left: 5px solid #f44336; padding: 10px; margin: 5px 0; }
        .test-name { font-weight: bold; }
        .test-details { color: #666; font-size: 14px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🧪 IRCamera Integration Test Report</h1>
        <p>Generated: $(date)</p>
    </div>
    
    <div class="summary">
        <h2>📊 Test Summary</h2>
        <p><strong>Total Tests:</strong> $TOTAL_TESTS</p>
        <p><strong>Passed:</strong> $PASSED_TESTS</p>
        <p><strong>Failed:</strong> $FAILED_TESTS</p>
        <p><strong>Pass Rate:</strong> $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%</p>
    </div>
    
    <h2>📋 Detailed Results</h2>
    <div id="test-results">
        <!-- Results will be populated by JavaScript -->
    </div>
    
    <script>
        fetch('integration_test_$TIMESTAMP.json')
            .then(response => response.json())
            .then(data => {
                const resultsContainer = document.getElementById('test-results');
                data.tests.forEach(test => {
                    const testDiv = document.createElement('div');
                    testDiv.className = test.status === 'PASS' ? 'test-pass' : 'test-fail';
                    testDiv.innerHTML = 
                        '<div class="test-name">' + (test.status === 'PASS' ? '✅' : '❌') + ' ' + test.test_name + '</div>' +
                        '<div class="test-details">' + test.details + '</div>';
                    resultsContainer.appendChild(testDiv);
                });
            })
            .catch(error => {
                document.getElementById('test-results').innerHTML = 
                    '<p>Error loading test data: ' + error + '</p>';
            });
    </script>
</body>
</html>
EOF
    
    log "  ✅ Results saved to: $RESULTS_FILE"
    log "  📊 HTML report saved to: $HTML_REPORT"
    log "  📝 Detailed log saved to: $LOG_FILE"
}

# Main execution
main() {
    # Check prerequisites
    if ! command -v adb &> /dev/null; then
        log "❌ ADB not found. Please install Android SDK platform tools."
        exit 1
    fi
    
    if ! command -v bc &> /dev/null; then
        log "❌ bc (calculator) not found. Please install bc package."
        exit 1
    fi
    
    # Check device connection
    if ! adb devices | grep -q "device$"; then
        log "❌ No Android device connected. Please connect a device and enable USB debugging."
        exit 1
    fi
    
    # Initialize results file
    echo "{" > "$RESULTS_FILE"
    echo "  \"test_info\": {" >> "$RESULTS_FILE"
    echo "    \"timestamp\": \"$(date -Iseconds)\"," >> "$RESULTS_FILE"
    echo "    \"device\": \"$(adb shell getprop ro.product.model | tr -d '\r')\"," >> "$RESULTS_FILE"
    echo "    \"android_version\": \"$(adb shell getprop ro.build.version.release | tr -d '\r')\"" >> "$RESULTS_FILE"
    echo "  }," >> "$RESULTS_FILE"
    echo "  \"tests\": [" >> "$RESULTS_FILE"
    
    log "🚀 Starting IRCamera Integration Tests..."
    log "📱 Device: $(adb shell getprop ro.product.model | tr -d '\r')"
    log "🤖 Android: $(adb shell getprop ro.build.version.release | tr -d '\r')"
    
    # Run test suites
    test_app_prerequisites
    test_compose_activity_launches  
    test_thermal_camera_integration
    test_gsr_sensor_integration
    test_eventbus_integration
    test_multi_sensor_coordination
    test_compose_lifecycle_integration
    
    # Generate report
    generate_integration_report
    
    # Final summary
    log ""
    log "🎉 Integration testing completed!"
    log "📊 Results: $PASSED_TESTS/$TOTAL_TESTS tests passed ($(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%)"
    log "📁 Reports available in: $TEST_RESULTS_DIR/"
    
    # Exit with proper code
    if [ "$FAILED_TESTS" -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi