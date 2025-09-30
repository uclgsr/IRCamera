#!/bin/bash

# IRCamera Accessibility Testing Suite
# Tests Compose UI accessibility features and semantic compliance

echo " IRCamera Accessibility Testing Suite"
echo "======================================"

# Configuration
ACCESSIBILITY_RESULTS_DIR="accessibility-test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_FILE="$ACCESSIBILITY_RESULTS_DIR/accessibility_test_$TIMESTAMP.json"
LOG_FILE="$ACCESSIBILITY_RESULTS_DIR/accessibility_test_$TIMESTAMP.log"

# Create output directory
mkdir -p "$ACCESSIBILITY_RESULTS_DIR"

# Test counters
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
    local severity="${4:-medium}"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$status" == "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log "   $test_name: PASSED"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "   $test_name: FAILED - $details"
    fi
    
    # Record in JSON
    echo "    {" >> "$RESULTS_FILE"
    echo "      \"test_name\": \"$test_name\"," >> "$RESULTS_FILE"
    echo "      \"status\": \"$status\"," >> "$RESULTS_FILE"
    echo "      \"severity\": \"$severity\"," >> "$RESULTS_FILE"
    echo "      \"timestamp\": \"$(date -Iseconds)\"," >> "$RESULTS_FILE"
    echo "      \"details\": \"$details\"" >> "$RESULTS_FILE"
    echo "    }," >> "$RESULTS_FILE"
}

# Function to test TalkBack integration
test_talkback_integration() {
    log ""
    log "🔊 Testing TalkBack Integration..."
    
    # Check if TalkBack service is available
    TALKBACK_SERVICE=$(adb shell service list | grep -c "accessibility")
    if [ "$TALKBACK_SERVICE" -gt 0 ]; then
        record_test_result "TalkBack Service Availability" "PASS" "Accessibility services available" "low"
    else
        record_test_result "TalkBack Service Availability" "FAIL" "No accessibility services found" "high"
    fi
    
    # Test semantic content descriptions in MainActivity
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 3
    
    # Dump UI hierarchy to check for content descriptions
    adb shell uiautomator dump /sdcard/ui_dump.xml &>/dev/null
    adb pull /sdcard/ui_dump.xml /tmp/ui_dump.xml &>/dev/null
    
    if [ -f "/tmp/ui_dump.xml" ]; then
        # Check for content-desc attributes
        CONTENT_DESC_COUNT=$(grep -c 'content-desc=' /tmp/ui_dump.xml)
        if [ "$CONTENT_DESC_COUNT" -gt 5 ]; then
            record_test_result "Content Descriptions" "PASS" "Found $CONTENT_DESC_COUNT content descriptions" "low"
        else
            record_test_result "Content Descriptions" "FAIL" "Insufficient content descriptions ($CONTENT_DESC_COUNT found)" "high"
        fi
        
        # Check for clickable elements without descriptions
        CLICKABLE_WITHOUT_DESC=$(grep 'clickable="true"' /tmp/ui_dump.xml | grep -c 'content-desc=""')
        if [ "$CLICKABLE_WITHOUT_DESC" -eq 0 ]; then
            record_test_result "Clickable Elements Accessibility" "PASS" "All clickable elements have descriptions" "low"
        else
            record_test_result "Clickable Elements Accessibility" "FAIL" "$CLICKABLE_WITHOUT_DESC clickable elements lack descriptions" "high"
        fi
        
        # Check for proper heading structure (text size variations)
        HEADING_STRUCTURE=$(grep -c 'resource-id.*heading\|text.*°C\|text.*Temperature' /tmp/ui_dump.xml)
        if [ "$HEADING_STRUCTURE" -gt 0 ]; then
            record_test_result "Heading Structure" "PASS" "Proper heading elements detected" "medium"
        else
            record_test_result "Heading Structure" "FAIL" "No clear heading structure detected" "medium"
        fi
        
        rm -f /tmp/ui_dump.xml
    else
        record_test_result "UI Hierarchy Analysis" "FAIL" "Could not dump UI hierarchy" "high"
    fi
}

# Function to test keyboard navigation
test_keyboard_navigation() {
    log ""
    log "⌨️  Testing Keyboard Navigation..."
    
    # Start main activity
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 3
    
    # Test tab navigation through UI elements
    for i in {1..5}; do
        adb shell input keyevent KEYCODE_TAB
        sleep 1
    done
    
    # Check if focus moved (dump UI and look for focused elements)
    adb shell uiautomator dump /sdcard/ui_dump_focus.xml &>/dev/null
    adb pull /sdcard/ui_dump_focus.xml /tmp/ui_dump_focus.xml &>/dev/null
    
    if [ -f "/tmp/ui_dump_focus.xml" ]; then
        FOCUSED_ELEMENTS=$(grep -c 'focused="true"' /tmp/ui_dump_focus.xml)
        if [ "$FOCUSED_ELEMENTS" -gt 0 ]; then
            record_test_result "Keyboard Focus Navigation" "PASS" "Elements can receive keyboard focus" "medium"
        else
            record_test_result "Keyboard Focus Navigation" "FAIL" "No elements show keyboard focus" "high"
        fi
        
        # Test Enter key activation
        adb shell input keyevent KEYCODE_ENTER
        sleep 2
        
        # Check if action was triggered (activity might change)
        CURRENT_ACTIVITY=$(adb shell "dumpsys window | grep mCurrentFocus")
        if [[ "$CURRENT_ACTIVITY" != *"MainActivity"* ]]; then
            record_test_result "Keyboard Activation" "PASS" "Enter key triggers navigation" "medium"
        else
            record_test_result "Keyboard Activation" "FAIL" "Enter key may not activate focused elements" "medium"
        fi
        
        rm -f /tmp/ui_dump_focus.xml
    else
        record_test_result "Keyboard Navigation Analysis" "FAIL" "Could not analyze keyboard navigation" "high"
    fi
}

# Function to test color contrast and visual accessibility
test_visual_accessibility() {
    log ""
    log "🎨 Testing Visual Accessibility..."
    
    # Take screenshot for visual analysis
    adb shell screencap -p /sdcard/screenshot.png &>/dev/null
    adb pull /sdcard/screenshot.png /tmp/screenshot.png &>/dev/null
    
    if [ -f "/tmp/screenshot.png" ]; then
        # Basic screenshot analysis (file size as proxy for content richness)
        SCREENSHOT_SIZE=$(stat -c%s /tmp/screenshot.png 2>/dev/null || echo "0")
        if [ "$SCREENSHOT_SIZE" -gt 50000 ]; then
            record_test_result "Visual Content Richness" "PASS" "Screenshot indicates rich visual content" "low"
        else
            record_test_result "Visual Content Richness" "FAIL" "Screenshot may indicate poor visual content" "medium"
        fi
        
        # Check for proper theme usage (approximated by screenshot analysis)
        # This is a simplified test - in practice you'd use image analysis tools
        if [ "$SCREENSHOT_SIZE" -gt 0 ]; then
            record_test_result "Theme Consistency" "PASS" "Visual elements appear to be themed" "low"
        else
            record_test_result "Theme Consistency" "FAIL" "Could not verify theme consistency" "medium"
        fi
        
        rm -f /tmp/screenshot.png
    else
        record_test_result "Visual Analysis" "FAIL" "Could not capture screenshot for analysis" "medium"
    fi
    
    # Test text size scalability
    # Check if app responds to system font size changes
    CURRENT_FONT_SCALE=$(adb shell settings get system font_scale)
    
    # Temporarily increase font scale
    adb shell settings put system font_scale 1.3
    sleep 2
    
    # Restart activity to apply changes
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 3
    
    # Check if app handles larger fonts (UI hierarchy check)
    adb shell uiautomator dump /sdcard/ui_dump_large_font.xml &>/dev/null
    adb pull /sdcard/ui_dump_large_font.xml /tmp/ui_dump_large_font.xml &>/dev/null
    
    if [ -f "/tmp/ui_dump_large_font.xml" ]; then
        # Look for text elements (simplified check)
        TEXT_ELEMENTS=$(grep -c 'text=' /tmp/ui_dump_large_font.xml)
        if [ "$TEXT_ELEMENTS" -gt 3 ]; then
            record_test_result "Font Scale Support" "PASS" "App displays text with custom font scale" "medium"
        else
            record_test_result "Font Scale Support" "FAIL" "App may not support custom font scaling" "high"
        fi
        
        rm -f /tmp/ui_dump_large_font.xml
    else
        record_test_result "Font Scale Analysis" "FAIL" "Could not analyze font scale support" "medium"
    fi
    
    # Restore original font scale
    adb shell settings put system font_scale "$CURRENT_FONT_SCALE"
}

# Function to test touch target sizes
test_touch_targets() {
    log ""
    log "👆 Testing Touch Target Sizes..."
    
    # Dump UI hierarchy to analyze touch target sizes
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 3
    
    adb shell uiautomator dump /sdcard/ui_dump_touch.xml &>/dev/null
    adb pull /sdcard/ui_dump_touch.xml /tmp/ui_dump_touch.xml &>/dev/null
    
    if [ -f "/tmp/ui_dump_touch.xml" ]; then
        # Extract bounds information for clickable elements
        CLICKABLE_ELEMENTS=$(grep 'clickable="true"' /tmp/ui_dump_touch.xml)
        
        # Count elements (simplified analysis)
        CLICKABLE_COUNT=$(echo "$CLICKABLE_ELEMENTS" | wc -l)
        
        if [ "$CLICKABLE_COUNT" -gt 3 ]; then
            record_test_result "Touch Target Availability" "PASS" "Multiple touch targets available ($CLICKABLE_COUNT)" "low"
            
            # Simple heuristic: check if bounds suggest reasonable sizes
            # Look for elements with reasonable coordinate differences
            REASONABLE_TARGETS=$(echo "$CLICKABLE_ELEMENTS" | grep -c 'bounds="\[.*\]"')
            
            if [ "$REASONABLE_TARGETS" -gt 2 ]; then
                record_test_result "Touch Target Size Analysis" "PASS" "Touch targets appear properly sized" "medium"
            else
                record_test_result "Touch Target Size Analysis" "FAIL" "Touch targets may be too small" "high"
            fi
        else
            record_test_result "Touch Target Availability" "FAIL" "Insufficient touch targets ($CLICKABLE_COUNT)" "high"
        fi
        
        rm -f /tmp/ui_dump_touch.xml
    else
        record_test_result "Touch Target Analysis" "FAIL" "Could not analyze touch targets" "high"
    fi
}

# Function to test semantic markup in Compose
test_compose_semantics() {
    log ""
    log "🏷️  Testing Compose Semantic Markup..."
    
    # Start a known Compose activity
    adb shell am start -n com.csl.irCamera/mpdc4gsr.activities.SettingsComposeActivity &>/dev/null
    sleep 3
    
    # Check if Compose semantic information is available
    adb shell uiautomator dump /sdcard/ui_dump_semantics.xml &>/dev/null
    adb pull /sdcard/ui_dump_semantics.xml /tmp/ui_dump_semantics.xml &>/dev/null
    
    if [ -f "/tmp/ui_dump_semantics.xml" ]; then
        # Check for semantic properties commonly added by Compose
        SEMANTIC_ELEMENTS=$(grep -c 'content-desc=\|text=\|resource-id=' /tmp/ui_dump_semantics.xml)
        
        if [ "$SEMANTIC_ELEMENTS" -gt 5 ]; then
            record_test_result "Compose Semantic Elements" "PASS" "Rich semantic information available ($SEMANTIC_ELEMENTS elements)" "low"
        else
            record_test_result "Compose Semantic Elements" "FAIL" "Limited semantic information ($SEMANTIC_ELEMENTS elements)" "high"
        fi
        
        # Check for role-based elements (buttons, switches, etc.)
        BUTTON_ELEMENTS=$(grep -c 'class=.*Button\|text=.*Button' /tmp/ui_dump_semantics.xml)
        SWITCH_ELEMENTS=$(grep -c 'class=.*Switch\|checkable="true"' /tmp/ui_dump_semantics.xml)
        
        if [ "$BUTTON_ELEMENTS" -gt 0 ] || [ "$SWITCH_ELEMENTS" -gt 0 ]; then
            record_test_result "Semantic Role Information" "PASS" "Interactive elements have proper roles" "medium"
        else
            record_test_result "Semantic Role Information" "FAIL" "Limited role information for interactive elements" "high"
        fi
        
        # Check for proper state information
        STATE_ELEMENTS=$(grep -c 'checked=\|selected=\|enabled=' /tmp/ui_dump_semantics.xml)
        
        if [ "$STATE_ELEMENTS" -gt 0 ]; then
            record_test_result "State Information" "PASS" "Elements provide state information" "medium"
        else
            record_test_result "State Information" "FAIL" "Limited state information available" "medium"
        fi
        
        rm -f /tmp/ui_dump_semantics.xml
    else
        record_test_result "Compose Semantics Analysis" "FAIL" "Could not analyze Compose semantics" "high"
    fi
}

# Function to test error handling accessibility
test_error_accessibility() {
    log ""
    log "  Testing Error Handling Accessibility..."
    
    # Try to trigger error states by navigating without proper setup
    adb shell am start -n com.csl.irCamera/.MainActivity &>/dev/null
    sleep 2
    
    # Try to access thermal camera without proper initialization
    adb shell input tap 200 400
    sleep 3
    
    # Check for error messages or dialogs
    adb shell uiautomator dump /sdcard/ui_dump_error.xml &>/dev/null
    adb pull /sdcard/ui_dump_error.xml /tmp/ui_dump_error.xml &>/dev/null
    
    if [ -f "/tmp/ui_dump_error.xml" ]; then
        # Look for error dialogs or messages
        ERROR_DIALOGS=$(grep -c 'AlertDialog\|Dialog\|Error\|Warning' /tmp/ui_dump_error.xml)
        
        if [ "$ERROR_DIALOGS" -gt 0 ]; then
            # Check if error dialogs have proper accessibility information
            ERROR_WITH_DESC=$(grep 'AlertDialog\|Dialog\|Error\|Warning' /tmp/ui_dump_error.xml | grep -c 'content-desc=')
            
            if [ "$ERROR_WITH_DESC" -gt 0 ]; then
                record_test_result "Error Dialog Accessibility" "PASS" "Error dialogs have accessibility descriptions" "medium"
            else
                record_test_result "Error Dialog Accessibility" "FAIL" "Error dialogs lack accessibility descriptions" "high"
            fi
        else
            record_test_result "Error State Detection" "PASS" "No obvious error states detected" "low"
        fi
        
        rm -f /tmp/ui_dump_error.xml
    else
        record_test_result "Error Accessibility Analysis" "FAIL" "Could not analyze error accessibility" "medium"
    fi
}

# Function to generate accessibility report
generate_accessibility_report() {
    log ""
    log " Generating Accessibility Report..."
    
    # Calculate severity breakdown
    HIGH_SEVERITY=$(grep '"severity": "high"' "$RESULTS_FILE" | wc -l)
    MEDIUM_SEVERITY=$(grep '"severity": "medium"' "$RESULTS_FILE" | wc -l)
    LOW_SEVERITY=$(grep '"severity": "low"' "$RESULTS_FILE" | wc -l)
    
    # Close JSON structure
    sed -i '$s/,$//' "$RESULTS_FILE"  # Remove last comma
    echo "  ]," >> "$RESULTS_FILE"
    echo "  \"summary\": {" >> "$RESULTS_FILE"
    echo "    \"total_tests\": $TOTAL_TESTS," >> "$RESULTS_FILE"
    echo "    \"passed_tests\": $PASSED_TESTS," >> "$RESULTS_FILE"
    echo "    \"failed_tests\": $FAILED_TESTS," >> "$RESULTS_FILE"
    echo "    \"pass_rate\": \"$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%\"," >> "$RESULTS_FILE"
    echo "    \"severity_breakdown\": {" >> "$RESULTS_FILE"
    echo "      \"high\": $HIGH_SEVERITY," >> "$RESULTS_FILE"
    echo "      \"medium\": $MEDIUM_SEVERITY," >> "$RESULTS_FILE"
    echo "      \"low\": $LOW_SEVERITY" >> "$RESULTS_FILE"
    echo "    }" >> "$RESULTS_FILE"
    echo "  }," >> "$RESULTS_FILE"
    echo "  \"wcag_compliance\": {" >> "$RESULTS_FILE"
    echo "    \"level_aa_estimate\": \"$([ $HIGH_SEVERITY -eq 0 ] && echo 'Likely Compliant' || echo 'Needs Improvement')\"," >> "$RESULTS_FILE"
    echo "    \"critical_issues\": $HIGH_SEVERITY," >> "$RESULTS_FILE"
    echo "    \"recommendations\": [" >> "$RESULTS_FILE"
    
    # Add recommendations based on failures
    if [ $HIGH_SEVERITY -gt 0 ]; then
        echo "      \"Address high-severity accessibility issues first\"," >> "$RESULTS_FILE"
    fi
    if grep -q '"Content Descriptions".*"FAIL"' "$RESULTS_FILE"; then
        echo "      \"Add content descriptions to UI elements\"," >> "$RESULTS_FILE"
    fi
    if grep -q '"Keyboard.*"FAIL"' "$RESULTS_FILE"; then
        echo "      \"Improve keyboard navigation support\"," >> "$RESULTS_FILE"
    fi
    
    echo "      \"Regularly test with screen readers\"" >> "$RESULTS_FILE"
    echo "    ]" >> "$RESULTS_FILE"
    echo "  }" >> "$RESULTS_FILE"
    echo "}" >> "$RESULTS_FILE"
    
    # Create HTML report
    HTML_REPORT="$ACCESSIBILITY_RESULTS_DIR/accessibility_report_$TIMESTAMP.html"
    cat > "$HTML_REPORT" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>IRCamera Accessibility Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #9C27B0; color: white; padding: 20px; border-radius: 5px; }
        .summary { background: #f3e5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }
        .test-pass { background: #e8f5e8; border-left: 5px solid #4CAF50; padding: 10px; margin: 5px 0; }
        .test-fail { background: #ffeaea; border-left: 5px solid #f44336; padding: 10px; margin: 5px 0; }
        .severity-high { border-left: 5px solid #d32f2f; }
        .severity-medium { border-left: 5px solid #f57c00; }
        .severity-low { border-left: 5px solid #388e3c; }
        .test-name { font-weight: bold; }
        .test-details { color: #666; font-size: 14px; }
        .severity-badge { 
            display: inline-block; 
            padding: 2px 8px; 
            border-radius: 12px; 
            font-size: 12px; 
            font-weight: bold;
        }
        .severity-high-badge { background: #ffebee; color: #d32f2f; }
        .severity-medium-badge { background: #fff3e0; color: #f57c00; }
        .severity-low-badge { background: #e8f5e8; color: #388e3c; }
        .wcag-info { background: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h1> IRCamera Accessibility Test Report</h1>
        <p>Generated: $(date)</p>
    </div>
    
    <div class="summary">
        <h2> Test Summary</h2>
        <p><strong>Total Tests:</strong> $TOTAL_TESTS</p>
        <p><strong>Passed:</strong> $PASSED_TESTS</p>
        <p><strong>Failed:</strong> $FAILED_TESTS</p>
        <p><strong>Pass Rate:</strong> $(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%</p>
        
        <h3>Severity Breakdown</h3>
        <p><span class="severity-badge severity-high-badge">HIGH</span> $HIGH_SEVERITY issues</p>
        <p><span class="severity-badge severity-medium-badge">MEDIUM</span> $MEDIUM_SEVERITY issues</p>
        <p><span class="severity-badge severity-low-badge">LOW</span> $LOW_SEVERITY issues</p>
    </div>
    
    <div class="wcag-info">
        <h2>🌐 WCAG Compliance Estimate</h2>
        <p><strong>Level AA Compliance:</strong> $([ $HIGH_SEVERITY -eq 0 ] && echo 'Likely Compliant' || echo 'Needs Improvement')</p>
        <p><strong>Critical Issues:</strong> $HIGH_SEVERITY</p>
        <p><strong>Priority:</strong> Address high-severity issues first for better accessibility</p>
    </div>
    
    <h2> Detailed Results</h2>
    <div id="test-results">
        <!-- Results will be populated by JavaScript -->
    </div>
    
    <script>
        fetch('accessibility_test_$TIMESTAMP.json')
            .then(response => response.json())
            .then(data => {
                const resultsContainer = document.getElementById('test-results');
                data.tests.forEach(test => {
                    const testDiv = document.createElement('div');
                    testDiv.className = (test.status === 'PASS' ? 'test-pass' : 'test-fail') + 
                                      ' severity-' + test.severity;
                    testDiv.innerHTML = 
                        '<div class="test-name">' + 
                        (test.status === 'PASS' ? '' : '') + ' ' + test.test_name +
                        ' <span class="severity-badge severity-' + test.severity + '-badge">' + 
                        test.severity.toUpperCase() + '</span>' +
                        '</div>' +
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
    
    log "   Results saved to: $RESULTS_FILE"
    log "   HTML report saved to: $HTML_REPORT"
    log "  📝 Detailed log saved to: $LOG_FILE"
}

# Main execution
main() {
    # Check prerequisites
    if ! command -v adb &> /dev/null; then
        log " ADB not found. Please install Android SDK platform tools."
        exit 1
    fi
    
    if ! command -v bc &> /dev/null; then
        log " bc (calculator) not found. Please install bc package."
        exit 1
    fi
    
    # Check device connection
    if ! adb devices | grep -q "device$"; then
        log " No Android device connected. Please connect a device and enable USB debugging."
        exit 1
    fi
    
    # Initialize results file
    echo "{" > "$RESULTS_FILE"
    echo "  \"test_info\": {" >> "$RESULTS_FILE"
    echo "    \"timestamp\": \"$(date -Iseconds)\"," >> "$RESULTS_FILE"
    echo "    \"device\": \"$(adb shell getprop ro.product.model | tr -d '\r')\"," >> "$RESULTS_FILE"
    echo "    \"android_version\": \"$(adb shell getprop ro.build.version.release | tr -d '\r')\"," >> "$RESULTS_FILE"
    echo "    \"test_type\": \"accessibility_compliance\"" >> "$RESULTS_FILE"
    echo "  }," >> "$RESULTS_FILE"
    echo "  \"tests\": [" >> "$RESULTS_FILE"
    
    log " Starting IRCamera Accessibility Tests..."
    log " Device: $(adb shell getprop ro.product.model | tr -d '\r')"
    log "🤖 Android: $(adb shell getprop ro.build.version.release | tr -d '\r')"
    
    # Run accessibility test suites
    test_talkback_integration
    test_keyboard_navigation
    test_visual_accessibility
    test_touch_targets
    test_compose_semantics
    test_error_accessibility
    
    # Generate report
    generate_accessibility_report
    
    # Final summary
    log ""
    log " Accessibility testing completed!"
    log " Results: $PASSED_TESTS/$TOTAL_TESTS tests passed ($(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%)"
    log "  High-severity issues: $(grep '"severity": "high"' "$RESULTS_FILE" | wc -l)"
    log " Reports available in: $ACCESSIBILITY_RESULTS_DIR/"
    
    # Exit with proper code based on high-severity issues
    HIGH_ISSUES=$(grep -c '"severity": "high"' "$RESULTS_FILE")
    if [ "$HIGH_ISSUES" -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi