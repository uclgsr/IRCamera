#!/bin/bash

# IRCamera Compose Migration Verification Script
# This script addresses the most critical missing element: build verification

echo "🔍 IRCamera Compose Migration Verification"
echo "=========================================="

# Function to check build status
check_build() {
    echo "📦 Checking Gradle Build Status..."
    
    # Clean build first
    echo "  - Cleaning previous build..."
    ./gradlew clean --no-daemon -q > /dev/null 2>&1
    
    # Try to compile debug variant
    echo "  - Attempting debug compilation..."
    if ./gradlew compileDebugKotlin --no-daemon -q --stacktrace > build_output.log 2>&1; then
        echo "  ✅ Kotlin compilation successful"
        KOTLIN_OK=true
    else
        echo "  ❌ Kotlin compilation failed"
        echo "  📝 Error details in build_output.log"
        KOTLIN_OK=false
    fi
    
    # Check manifest processing
    echo "  - Checking AndroidManifest processing..."
    if ./gradlew processDebugMainManifest --no-daemon -q > manifest_output.log 2>&1; then
        echo "  ✅ AndroidManifest processing successful"
        MANIFEST_OK=true
    else
        echo "  ❌ AndroidManifest processing failed"
        echo "  📝 Error details in manifest_output.log"
        MANIFEST_OK=false
    fi
}

# Function to analyze Compose activities
analyze_compose_activities() {
    echo ""
    echo "🎨 Analyzing Compose Activities..."
    
    # Count Compose activities
    COMPOSE_COUNT=$(find . -name "*Compose*.kt" | grep -i activity | wc -l)
    TOTAL_ACTIVITIES=$(find . -name "*Activity*.kt" | wc -l)
    if [ "$TOTAL_ACTIVITIES" -gt 0 ]; then
        COMPOSE_PERCENTAGE=$((COMPOSE_COUNT * 100 / TOTAL_ACTIVITIES))
        echo "  📊 Compose Activities: $COMPOSE_COUNT / $TOTAL_ACTIVITIES ($COMPOSE_PERCENTAGE%)"
    else
        COMPOSE_PERCENTAGE="N/A"
        echo "  📊 Compose Activities: $COMPOSE_COUNT / $TOTAL_ACTIVITIES ($COMPOSE_PERCENTAGE)"
        echo "  ⚠️  No Activity files found. Percentage calculation skipped."
    fi
    
    # Check base classes
    if [ -f "app/src/main/java/mpdc4gsr/compose/base/BaseComposeActivity.kt" ]; then
        echo "  ✅ BaseComposeActivity found"
    else
        echo "  ❌ BaseComposeActivity missing"
    fi
    
    if [ -f "libunified/src/main/java/com/mpdc4gsr/libunified/app/ktbase/BaseViewModel.kt" ]; then
        echo "  ✅ BaseViewModel found"
    else
        echo "  ❌ BaseViewModel missing"
    fi
}

# Function to check dependencies
check_dependencies() {
    echo ""
    echo "📚 Checking Compose Dependencies..."
    
    # Check for compose-bom in version catalog (correct location)
    if grep -r "compose-bom" gradle/libs.versions.toml > /dev/null 2>&1; then
        echo "  ✅ Compose BOM dependency found"
    else
        echo "  ❌ Compose BOM dependency missing"
    fi
    
    # Check for Material 3 in version catalog (correct location)
    if grep -r "material3" gradle/libs.versions.toml > /dev/null 2>&1; then
        echo "  ✅ Material 3 dependency found"
    else
        echo "  ❌ Material 3 dependency missing"
    fi
}

# Function to identify missing elements
identify_missing_elements() {
    echo ""
    echo "🚨 Critical Missing Elements:"
    echo ""
    
    if [ "$KOTLIN_OK" = false ]; then
        echo "  ❌ BUILD FAILURE: Kotlin compilation fails"
        echo "     Priority: CRITICAL - Must fix before migration can be considered complete"
    fi
    
    if [ "$MANIFEST_OK" = false ]; then
        echo "  ❌ MANIFEST ISSUES: AndroidManifest.xml processing fails"
        echo "     Priority: CRITICAL - Prevents app from running"
    fi
    
    # Check for performance testing
    if [ -f "performance_benchmark.sh" ]; then
        echo "  ✅ Performance benchmarking script found"
    else
        echo "  ❌ PERFORMANCE VALIDATION: No benchmarking scripts found"
        echo "     Priority: HIGH - Claims need verification"
    fi
    
    # Check for integration tests
    if [ -f "integration_test_suite.sh" ]; then
        echo "  ✅ Integration testing script found"
    else
        echo "  ❌ INTEGRATION TESTING: No Compose integration tests found"
        echo "     Priority: HIGH - Sensor integration untested"
    fi
    
    # Check for accessibility testing
    if [ -f "accessibility_test_suite.sh" ]; then
        echo "  ✅ Accessibility testing script found"
    else
        echo"  ❌ ACCESSIBILITY: No semantic testing found"
        echo "     Priority: MEDIUM - User experience validation missing"
    fi
}

# Function to provide recommendations
provide_recommendations() {
    echo ""
    echo "🎯 Immediate Action Items:"
    echo ""
    
    if [ "$KOTLIN_OK" = false ] || [ "$MANIFEST_OK" = false ]; then
        echo "1. FIX BUILD ISSUES (CRITICAL)"
        echo "   - Review build_output.log and manifest_output.log"
        echo "   - Fix AndroidManifest.xml duplicate activities"
        echo "   - Resolve dependency conflicts"
        echo ""
    fi
    
    echo "2. CREATE PERFORMANCE BENCHMARKS (HIGH)"
    echo "   - Measure FPS during thermal data display"
    echo "   - Profile memory usage with Android Studio"
    echo "   - Test battery usage during sensor operation"
    echo ""
    
    echo "3. ADD INTEGRATION TESTING (HIGH)"
    echo "   - Create Compose UI tests for sensor activities"
    echo "   - Test EventBus integration with Compose lifecycle"
    echo "   - Validate multi-sensor coordination"
    echo ""
    
    echo "4. DOCUMENT MIGRATION PATTERNS (MEDIUM)"
    echo "   - Create step-by-step migration cookbook"
    echo "   - Document error handling patterns"
    echo "   - Add accessibility testing guidelines"
}

# Main execution
main() {
    check_build
    analyze_compose_activities
    check_dependencies
    identify_missing_elements
    provide_recommendations
    
    echo ""
    echo "📝 Verification Summary:"
    echo "======================="
    
    if [ "$KOTLIN_OK" = true ] && [ "$MANIFEST_OK" = true ]; then
        echo "🎉 BUILD STATUS: PASSING - Migration technically functional"
        echo "📋 NEXT FOCUS: Performance validation and integration testing"
    else
        echo "⚠️  BUILD STATUS: FAILING - Migration requires immediate fixes"
        echo "📋 NEXT FOCUS: Resolve build issues before proceeding"
    fi
    
    echo ""
    echo "📊 Migration Status: $COMPOSE_PERCENTAGE% Compose coverage"
    echo "🔍 Detailed logs: build_output.log, manifest_output.log"
}

# Run the verification
main