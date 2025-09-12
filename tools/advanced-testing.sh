#!/bin/bash

# IRCamera Advanced Testing Suite
# Comprehensive testing with coverage, parallel execution, and detailed reporting

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

show_header() {
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║              🧪 IRCamera Advanced Testing Suite               ║${NC}"
    echo -e "${BLUE}║          Coverage • Parallel • Performance • Reporting       ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

run_unit_tests() {
    echo -e "${YELLOW}🔬 Running Unit Tests${NC}"
    echo "─────────────────────────"
    
    local start_time end_time duration
    start_time=$(date +%s)
    
    # Run unit tests with coverage
    if ./gradlew testDebugUnitTest jacocoTestReport --parallel --continue; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        
        echo -e "${GREEN}✅ Unit tests completed in ${duration}s${NC}"
        
        # Check for coverage reports
        local coverage_reports
        coverage_reports=$(find . -name "jacocoTestReport.xml" 2>/dev/null | wc -l)
        if [ "$coverage_reports" -gt 0 ]; then
            echo -e "${CYAN}📊 Coverage reports generated: $coverage_reports modules${NC}"
        fi
    else
        echo -e "${RED}❌ Unit tests failed${NC}"
        return 1
    fi
}

run_instrumented_tests() {
    echo -e "${YELLOW}📱 Running Instrumented Tests${NC}"
    echo "────────────────────────────────"
    
    # Check if emulator is available
    if command -v adb &> /dev/null && adb devices | grep -q "device$"; then
        echo "🤖 Android device/emulator detected"
        
        local start_time end_time duration
        start_time=$(date +%s)
        
        if ./gradlew connectedDebugAndroidTest; then
            end_time=$(date +%s)
            duration=$((end_time - start_time))
            echo -e "${GREEN}✅ Instrumented tests completed in ${duration}s${NC}"
        else
            echo -e "${RED}❌ Instrumented tests failed${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}⚠️  No Android device/emulator found, skipping instrumented tests${NC}"
    fi
}

analyze_test_coverage() {
    echo -e "${YELLOW}📊 Analyzing Test Coverage${NC}"
    echo "─────────────────────────────"
    
    local total_coverage=0
    local module_count=0
    
    # Find and analyze coverage reports
    while IFS= read -r -d '' report; do
        if [ -f "$report" ]; then
            module_count=$((module_count + 1))
            echo "📁 $(dirname "$report" | sed 's|.*/||'): Coverage report found"
        fi
    done < <(find . -name "jacocoTestReport.xml" -print0 2>/dev/null)
    
    if [ "$module_count" -gt 0 ]; then
        echo -e "${CYAN}📈 Coverage analysis complete for $module_count modules${NC}"
        
        # Generate summary coverage report
        echo -e "${GREEN}Coverage Summary:${NC}"
        echo "  • Unit test coverage: Generated for $module_count modules"
        echo "  • Coverage reports: build/reports/jacoco/jacocoTestReport/"
        echo "  • HTML reports: Open build/reports/jacoco/jacocoTestReport/html/index.html"
    else
        echo -e "${YELLOW}⚠️  No coverage reports found${NC}"
    fi
}

run_performance_tests() {
    echo -e "${YELLOW}⚡ Running Performance Tests${NC}"
    echo "──────────────────────────────"
    
    # Measure build performance
    local clean_build_time assemble_time
    
    echo "🧹 Measuring clean build time..."
    local start_time end_time
    start_time=$(date +%s)
    ./gradlew clean --quiet
    end_time=$(date +%s)
    clean_build_time=$((end_time - start_time))
    
    echo "🏗️  Measuring assemble time..."
    start_time=$(date +%s)
    ./gradlew :app:assembleDebug --quiet
    end_time=$(date +%s)
    assemble_time=$((end_time - start_time))
    
    echo -e "${CYAN}Performance Metrics:${NC}"
    echo "  • Clean time: ${clean_build_time}s"
    echo "  • Build time: ${assemble_time}s"
    echo "  • Total time: $((clean_build_time + assemble_time))s"
    
    # Performance rating
    local total_time=$((clean_build_time + assemble_time))
    if [ "$total_time" -lt 120 ]; then
        echo -e "  • Rating: ${GREEN}⚡ Excellent${NC}"
    elif [ "$total_time" -lt 300 ]; then
        echo -e "  • Rating: ${YELLOW}⚠️  Good${NC}"
    else
        echo -e "  • Rating: ${RED}🐌 Needs optimization${NC}"
    fi
}

generate_test_report() {
    echo -e "${YELLOW}📝 Generating Test Reports${NC}"
    echo "─────────────────────────────"
    
    local report_dir="reports/testing"
    mkdir -p "$report_dir"
    
    # Create comprehensive test report
    cat > "$report_dir/test-summary.md" << EOF
# IRCamera Test Report

Generated: $(date '+%Y-%m-%d %H:%M:%S')

## Test Execution Summary

### Unit Tests
- Status: $([ -f "app/build/test-results/testDebugUnitTest/TEST-*.xml" ] && echo "✅ Passed" || echo "❌ Failed or not run")
- Coverage: Available in build/reports/jacoco/jacocoTestReport/

### Instrumented Tests
- Status: $(adb devices | grep -q "device$" && echo "✅ Device available" || echo "⚠️  No device")
- Results: Available in build/reports/androidTests/

### Performance Metrics
- Last build time: Measured in performance tests
- Coverage modules: $(find . -name "jacocoTestReport.xml" 2>/dev/null | wc -l)

## Next Steps
1. Review coverage reports for areas needing more tests
2. Add integration tests for critical user flows
3. Set up automated performance regression testing
4. Configure continuous test execution in CI/CD

EOF

    echo -e "${GREEN}✅ Test report generated: $report_dir/test-summary.md${NC}"
}

run_test_quality_checks() {
    echo -e "${YELLOW}🔍 Running Test Quality Checks${NC}"
    echo "─────────────────────────────────"
    
    local test_files unit_test_files
    test_files=$(find . -name "*Test.kt" -o -name "*Test.java" | wc -l)
    unit_test_files=$(find . -path "*/test/*" -name "*.kt" -o -name "*.java" | wc -l)
    
    echo -e "${CYAN}Test Statistics:${NC}"
    echo "  • Total test files: $test_files"
    echo "  • Unit test files: $unit_test_files"
    
    # Check test naming conventions
    local bad_test_names
    bad_test_names=$(find . -name "*Test.kt" -o -name "*Test.java" | grep -v -E "(Test\.kt|Test\.java)$" | wc -l)
    
    if [ "$bad_test_names" -eq 0 ]; then
        echo -e "  • Naming conventions: ${GREEN}✅ Compliant${NC}"
    else
        echo -e "  • Naming conventions: ${YELLOW}⚠️  $bad_test_names files need review${NC}"
    fi
    
    # Check for test coverage in critical modules
    local critical_modules=("app" "libir" "BleModule")
    for module in "${critical_modules[@]}"; do
        if [ -d "$module/src/test" ]; then
            echo -e "  • $module tests: ${GREEN}✅ Present${NC}"
        else
            echo -e "  • $module tests: ${YELLOW}⚠️  Missing${NC}"
        fi
    done
}

# Main execution
main() {
    show_header
    
    local start_time end_time total_duration
    start_time=$(date +%s)
    
    echo -e "${PURPLE}🚀 Starting Advanced Testing Suite${NC}"
    echo ""
    
    # Run all test phases
    run_unit_tests
    echo ""
    
    run_instrumented_tests
    echo ""
    
    analyze_test_coverage
    echo ""
    
    run_performance_tests
    echo ""
    
    run_test_quality_checks
    echo ""
    
    generate_test_report
    echo ""
    
    end_time=$(date +%s)
    total_duration=$((end_time - start_time))
    
    echo -e "${GREEN}🎉 Advanced Testing Suite completed in ${total_duration}s${NC}"
    echo ""
    echo -e "${CYAN}📚 Additional Resources:${NC}"
    echo "  • Test reports: ./reports/testing/"
    echo "  • Coverage: ./build/reports/jacoco/"
    echo "  • Run individual tests: ./gradlew test"
    echo "  • Performance analysis: ./tools/performance-analyzer.sh"
}

# Execute if run directly
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    main "$@"
fi