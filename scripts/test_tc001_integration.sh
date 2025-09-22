#!/bin/bash
# TC001 Testing Automation Script
# 
# This script helps automate the testing of TC001 thermal camera integration
# covering all four requested scenarios.

set -e

echo "===================="
echo "TC001 Integration Testing Suite"
echo "===================="

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Please install Android SDK platform-tools."
    exit 1
fi

# Function to check if device is connected
check_device() {
    devices=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    if [ "$devices" -eq 0 ]; then
        echo "❌ No Android device connected. Please connect device and enable USB debugging."
        exit 1
    fi
    echo "✅ Android device connected"
}

# Function to check if TC001 is connected
check_tc001() {
    echo "🔍 Checking for TC001 device..."
    
    # List USB devices on Android device
    usb_devices=$(adb shell "ls /dev/bus/usb/*/001 2>/dev/null || echo 'no_usb'")
    if [ "$usb_devices" = "no_usb" ]; then
        echo "⚠️  TC001 device not detected. Tests will run in simulation mode."
        return 1
    else
        echo "✅ USB devices detected. TC001 may be connected."
        return 0
    fi
}

# Function to run unit tests
run_unit_tests() {
    echo ""
    echo "📋 Running Unit Tests..."
    echo "========================"
    
    echo "Running TC001IntegrationTest..."
    ./gradlew :app:testDebugUnitTest --tests="mpdc4gsr.sensors.thermal.TC001IntegrationTest" --info
    
    echo "✅ Unit tests completed"
}

# Function to run instrumentation tests
run_instrumentation_tests() {
    echo ""
    echo "📱 Running Instrumentation Tests..."
    echo "==================================="
    
    echo "Installing debug APK..."
    ./gradlew :app:installDebug
    
    echo "Running hardware integration tests..."
    ./gradlew :app:connectedDebugAndroidTest --tests="mpdc4gsr.sensors.thermal.TC001HardwareIntegrationTest" --info
    
    echo "✅ Instrumentation tests completed"
}

# Function to run manual testing guidance
manual_testing_guidance() {
    echo ""
    echo "🎯 Manual Testing Checklist"
    echo "============================"
    echo ""
    echo "Please perform the following manual tests with TC001 hardware:"
    echo ""
    echo "1. 📱 USB Permission Flow Test"
    echo "   - Connect TC001 to Android device"
    echo "   - Launch IRCamera app"
    echo "   - Verify USB permission dialog appears"
    echo "   - Grant permission and check thermal camera initializes"
    echo ""
    echo "2. ⏱️  Frame Rate Verification Test"
    echo "   - Start thermal recording"
    echo "   - Monitor logcat for frame rate messages:"
    echo "     adb logcat -s ThermalCameraRecorder:I"
    echo "   - Verify ~10Hz frame capture rate is achieved"
    echo "   - Check thermal_images/ directory for PNG files"
    echo ""
    echo "3. 🔌 Disconnect/Reconnect Test"
    echo "   - Start multi-sensor recording (GSR + Thermal + RGB)"
    echo "   - Unplug TC001 during recording"
    echo "   - Verify recording continues (switches to simulation)"
    echo "   - Reconnect TC001"
    echo "   - Verify recording continues without issues"
    echo ""
    echo "4. 🛡️  Sensor Independence Test"
    echo "   - Start recording with all sensors"
    echo "   - Force thermal camera error (unplug/power off TC001)"
    echo "   - Verify GSR and RGB sensors continue recording"
    echo "   - Check log files show thermal in simulation mode"
    echo "   - Verify other sensor data files are complete"
    echo ""
    echo "Expected outcomes:"
    echo "✅ No app crashes during any test"
    echo "✅ Thermal switches to simulation mode on failure"
    echo "✅ Other sensors unaffected by thermal issues"
    echo "✅ 10Hz frame rate achieved with real hardware"
    echo "✅ PNG files saved to thermal_images/ directory"
    echo "✅ CSV files contain temperature data"
}

# Function to collect test results
collect_results() {
    echo ""
    echo "📊 Collecting Test Results..."
    echo "============================="
    
    # Create results directory
    results_dir="tc001_test_results_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$results_dir"
    
    # Collect test reports
    if [ -d "app/build/reports/tests" ]; then
        cp -r app/build/reports/tests "$results_dir/"
        echo "✅ Test reports copied to $results_dir"
    fi
    
    # Collect test outputs
    if [ -d "app/build/outputs/androidTest-results" ]; then
        cp -r app/build/outputs/androidTest-results "$results_dir/"
        echo "✅ Android test results copied to $results_dir"
    fi
    
    # Collect logs
    if command -v adb &> /dev/null; then
        echo "Collecting device logs..."
        adb logcat -d -s ThermalCameraRecorder:* > "$results_dir/thermal_logcat.txt" 2>/dev/null || true
        echo "✅ Device logs collected"
    fi
    
    echo "📁 Results available in: $results_dir"
}

# Main execution
main() {
    echo "Starting TC001 integration testing..."
    
    # Check prerequisites
    check_device
    tc001_connected=$(check_tc001 && echo "yes" || echo "no")
    
    # Run tests based on user choice
    echo ""
    echo "Test Options:"
    echo "1) Run unit tests only"
    echo "2) Run instrumentation tests only" 
    echo "3) Run all automated tests"
    echo "4) Show manual testing guidance"
    echo "5) Run full test suite"
    echo ""
    read -p "Select option (1-5): " choice
    
    case $choice in
        1)
            run_unit_tests
            ;;
        2)
            run_instrumentation_tests
            ;;
        3)
            run_unit_tests
            run_instrumentation_tests
            ;;
        4)
            manual_testing_guidance
            ;;
        5)
            run_unit_tests
            run_instrumentation_tests
            manual_testing_guidance
            collect_results
            ;;
        *)
            echo "Invalid option. Exiting."
            exit 1
            ;;
    esac
    
    echo ""
    echo "🎉 TC001 testing completed!"
    
    if [ "$tc001_connected" = "no" ]; then
        echo ""
        echo "⚠️  Note: TC001 hardware not detected during testing."
        echo "   For complete validation, please:"
        echo "   1. Connect TC001 device"
        echo "   2. Re-run tests with option 2 or 5"
        echo "   3. Perform manual testing checklist"
    fi
}

# Execute main function
main "$@"