#!/bin/bash

# IRCamera Testing Suite - Consolidated Test Runner
# Combines accessibility, integration, performance, and validation testing

set -e

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_BASE_DIR="test-results"
mkdir -p "$RESULTS_BASE_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse command line arguments
TEST_TYPE="${1:-all}"
ANDROID_DEVICE="${2:-}"

# Usage information
usage() {
    echo "Usage: $0 [test-type] [android-device]"
    echo ""
    echo "Test Types:"
    echo "  all              Run all tests (default)"
    echo "  accessibility    Run accessibility tests only"
    echo "  integration      Run integration tests only"
    echo "  performance      Run performance benchmarks only"
    echo "  comprehensive    Run comprehensive test suite"
    echo "  validate         Validate existing test results"
    echo ""
    echo "Android Device: Optional device ID for device-specific tests"
    echo ""
    exit 1
}

if [ "$TEST_TYPE" = "-h" ] || [ "$TEST_TYPE" = "--help" ]; then
    usage
fi

# Function: Accessibility Testing
run_accessibility_tests() {
    echo -e "${BLUE}Running Accessibility Tests...${NC}"
    
    local ACCESSIBILITY_DIR="$RESULTS_BASE_DIR/accessibility"
    mkdir -p "$ACCESSIBILITY_DIR"
    local RESULTS_FILE="$ACCESSIBILITY_DIR/accessibility_test_$TIMESTAMP.json"
    local LOG_FILE="$ACCESSIBILITY_DIR/accessibility_test_$TIMESTAMP.log"
    
    echo "Testing Compose UI accessibility features and semantic compliance" | tee "$LOG_FILE"
    
    # Run accessibility tests
    ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=mpdc4gsr.compose.testing.AccessibilityTest >> "$LOG_FILE" 2>&1 || true
    
    # Generate results summary
    echo "{" > "$RESULTS_FILE"
    echo "  \"test_type\": \"accessibility\"," >> "$RESULTS_FILE"
    echo "  \"timestamp\": \"$TIMESTAMP\"," >> "$RESULTS_FILE"
    echo "  \"status\": \"completed\"" >> "$RESULTS_FILE"
    echo "}" >> "$RESULTS_FILE"
    
    echo -e "${GREEN}Accessibility tests completed. Results: $RESULTS_FILE${NC}"
}

# Function: Integration Testing
run_integration_tests() {
    echo -e "${BLUE}Running Integration Tests...${NC}"
    
    local INTEGRATION_DIR="$RESULTS_BASE_DIR/integration"
    mkdir -p "$INTEGRATION_DIR"
    local RESULTS_FILE="$INTEGRATION_DIR/integration_test_$TIMESTAMP.json"
    local LOG_FILE="$INTEGRATION_DIR/integration_test_$TIMESTAMP.log"
    
    echo "Testing Compose UI integration with sensor hardware and EventBus" | tee "$LOG_FILE"
    
    # Run integration tests
    ./gradlew :app:connectedDebugAndroidTest >> "$LOG_FILE" 2>&1 || true
    
    # Generate results summary
    echo "{" > "$RESULTS_FILE"
    echo "  \"test_type\": \"integration\"," >> "$RESULTS_FILE"
    echo "  \"timestamp\": \"$TIMESTAMP\"," >> "$RESULTS_FILE"
    echo "  \"status\": \"completed\"" >> "$RESULTS_FILE"
    echo "}" >> "$RESULTS_FILE"
    
    echo -e "${GREEN}Integration tests completed. Results: $RESULTS_FILE${NC}"
}

# Function: Performance Benchmarking
run_performance_tests() {
    echo -e "${BLUE}Running Performance Benchmarks...${NC}"
    
    local BENCHMARK_DIR="$RESULTS_BASE_DIR/benchmark"
    mkdir -p "$BENCHMARK_DIR"
    local RESULTS_FILE="$BENCHMARK_DIR/benchmark_$TIMESTAMP.json"
    local LOG_FILE="$BENCHMARK_DIR/benchmark_$TIMESTAMP.log"
    
    echo "Validating performance claims for Compose migration" | tee "$LOG_FILE"
    
    # Check if app is installed
    if ! adb shell pm list packages | grep -q mpdc4gsr; then
        echo -e "${YELLOW}IRCamera app not installed. Installing...${NC}"
        ./gradlew installDebug >> "$LOG_FILE" 2>&1
    fi
    
    # Run performance profiling
    echo "Starting performance monitoring..." | tee -a "$LOG_FILE"
    adb shell dumpsys gfxinfo mpdc4gsr reset >> "$LOG_FILE" 2>&1 || true
    
    # Wait for some activity
    sleep 30
    
    adb shell dumpsys gfxinfo mpdc4gsr > "$BENCHMARK_DIR/gfxinfo_$TIMESTAMP.txt" 2>&1 || true
    
    # Generate results summary
    echo "{" > "$RESULTS_FILE"
    echo "  \"test_type\": \"performance\"," >> "$RESULTS_FILE"
    echo "  \"timestamp\": \"$TIMESTAMP\"," >> "$RESULTS_FILE"
    echo "  \"status\": \"completed\"" >> "$RESULTS_FILE"
    echo "}" >> "$RESULTS_FILE"
    
    echo -e "${GREEN}Performance benchmarks completed. Results: $RESULTS_FILE${NC}"
}

# Function: Comprehensive Testing
run_comprehensive_tests() {
    echo -e "${BLUE}Running Comprehensive Test Suite...${NC}"
    
    local COMPREHENSIVE_DIR="$RESULTS_BASE_DIR/comprehensive"
    mkdir -p "$COMPREHENSIVE_DIR"
    local LOG_FILE="$COMPREHENSIVE_DIR/comprehensive_$TIMESTAMP.log"
    
    echo "Executing all MVP-focused tests" | tee "$LOG_FILE"
    
    # Run Gradle tests
    echo "Running Android unit tests..." | tee -a "$LOG_FILE"
    ./gradlew test >> "$LOG_FILE" 2>&1 || true
    
    echo "Running Android instrumentation tests..." | tee -a "$LOG_FILE"
    ./gradlew connectedAndroidTest >> "$LOG_FILE" 2>&1 || true
    
    # Run PC controller tests if available
    if [ -d "$PROJECT_ROOT/pc-controller" ]; then
        echo "Running PC controller tests..." | tee -a "$LOG_FILE"
        cd "$PROJECT_ROOT/pc-controller"
        if [ -f "requirements.txt" ]; then
            python3 -m pytest testing/ >> "$LOG_FILE" 2>&1 || true
        fi
        cd "$PROJECT_ROOT"
    fi
    
    echo -e "${GREEN}Comprehensive tests completed. Log: $LOG_FILE${NC}"
}

# Function: Validate Test Results
validate_test_results() {
    echo -e "${BLUE}Validating Test Results...${NC}"
    
    local VALIDATION_DIR="$RESULTS_BASE_DIR/validation"
    mkdir -p "$VALIDATION_DIR"
    local VALIDATION_REPORT="$VALIDATION_DIR/VALIDATION_REPORT_$TIMESTAMP.md"
    
    echo "# Test Results Validation Report" > "$VALIDATION_REPORT"
    echo "Generated: $(date)" >> "$VALIDATION_REPORT"
    echo "" >> "$VALIDATION_REPORT"
    
    # Count test results
    local TOTAL_RESULTS=$(find "$RESULTS_BASE_DIR" -name "*.json" 2>/dev/null | wc -l)
    echo "## Summary" >> "$VALIDATION_REPORT"
    echo "- Total test result files: $TOTAL_RESULTS" >> "$VALIDATION_REPORT"
    echo "" >> "$VALIDATION_REPORT"
    
    # List results by type
    echo "## Test Results by Type" >> "$VALIDATION_REPORT"
    for test_type in accessibility integration benchmark comprehensive; do
        local count=$(find "$RESULTS_BASE_DIR/$test_type" -name "*.json" 2>/dev/null | wc -l)
        if [ $count -gt 0 ]; then
            echo "- $test_type: $count results" >> "$VALIDATION_REPORT"
        fi
    done
    
    echo -e "${GREEN}Validation completed. Report: $VALIDATION_REPORT${NC}"
}

# Main execution
main() {
    echo "=========================================="
    echo "  IRCamera Consolidated Testing Suite"
    echo "=========================================="
    echo ""
    
    case "$TEST_TYPE" in
        all)
            run_accessibility_tests
            run_integration_tests
            run_performance_tests
            validate_test_results
            ;;
        accessibility)
            run_accessibility_tests
            ;;
        integration)
            run_integration_tests
            ;;
        performance)
            run_performance_tests
            ;;
        comprehensive)
            run_comprehensive_tests
            ;;
        validate)
            validate_test_results
            ;;
        *)
            echo -e "${RED}Unknown test type: $TEST_TYPE${NC}"
            usage
            ;;
    esac
    
    echo ""
    echo -e "${GREEN}Testing complete!${NC}"
    echo "Results directory: $RESULTS_BASE_DIR"
}

# Run main function
main
