#!/bin/bash

# validate_before_commit.sh - Comprehensive validation script that must pass before any commit
# This script ensures all changes are validated through the complete gradle build suite

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🔍 Starting comprehensive pre-commit validation..."
echo "================================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Track validation status
VALIDATION_PASSED=true
VALIDATION_LOG="/tmp/validation_$(date +%Y%m%d_%H%M%S).log"

# Function to log and display messages
log_message() {
    local level=$1
    local message=$2
    echo -e "${level}${message}${NC}" | tee -a "$VALIDATION_LOG"
}

# Function to run command with logging
run_command() {
    local description=$1
    local command=$2
    
    log_message "$BLUE" "🔄 $description..."
    
    if eval "$command" >> "$VALIDATION_LOG" 2>&1; then
        log_message "$GREEN" "✅ $description - PASSED"
        return 0
    else
        log_message "$RED" "❌ $description - FAILED"
        VALIDATION_PASSED=false
        return 1
    fi
}

# Start validation timestamp
log_message "$BLUE" "Validation started at: $(date)"

# 1. Clean previous build artifacts
log_message "$YELLOW" "Step 1: Cleaning previous build artifacts"
run_command "Clean gradle build cache" "./gradlew clean --no-daemon"

# 2. Full gradle build with all modules
log_message "$YELLOW" "Step 2: Full gradle build validation (this may take several minutes)"
run_command "Complete gradle build" "./gradlew build --no-daemon --max-workers=4"

# 3. Specific critical module compilation tests
log_message "$YELLOW" "Step 3: Critical module compilation validation"
run_command "libir module compilation" "./gradlew :libir:compileReleaseJavaWithJavac --no-daemon"
run_command "app module compilation" "./gradlew :app:compileReleaseSources --no-daemon"
run_command "BleModule compilation" "./gradlew :BleModule:compileReleaseSources --no-daemon"

# 4. Lint checks (if available)
log_message "$YELLOW" "Step 4: Code quality and lint validation"
if [[ -f ".ktlintrc" ]]; then
    run_command "Kotlin lint check" "./gradlew ktlintCheck --no-daemon" || true  # Non-blocking
fi

if [[ -f "checkstyle.xml" ]]; then
    run_command "Java checkstyle validation" "./gradlew checkstyleMain --no-daemon" || true  # Non-blocking
fi

# 5. Unit tests (if available)
log_message "$YELLOW" "Step 5: Unit test execution"
run_command "Unit tests execution" "./gradlew testReleaseUnitTest --no-daemon" || true  # Non-blocking for now

# 6. APK build test
log_message "$YELLOW" "Step 6: APK build validation"
run_command "Release APK build" "./gradlew assembleRelease --no-daemon"

# 7. Check for common issues
log_message "$YELLOW" "Step 7: Common issue detection"

# Check for orphaned case statements (the issue that was causing problems)
if grep -r "case.*:" libir/src/main/java/ | grep -v "switch" | head -5; then
    log_message "$YELLOW" "⚠️  Potential orphaned case statements detected (review needed)"
fi

# Check for malformed XML
find . -name "*.xml" -not -path "./build/*" -not -path "./.gradle/*" | head -10 | while read file; do
    if ! xmllint --noout "$file" 2>/dev/null; then
        log_message "$YELLOW" "⚠️  XML validation issue in: $file"
    fi
done

# Final validation result
echo "================================================="
if [ "$VALIDATION_PASSED" = true ]; then
    log_message "$GREEN" "🎉 ALL VALIDATIONS PASSED - Safe to commit!"
    log_message "$GREEN" "Validation completed at: $(date)"
    echo ""
    echo "Summary of validations performed:"
    echo "✅ Clean build from scratch"
    echo "✅ Complete gradle build suite"
    echo "✅ Critical module compilation"
    echo "✅ Code quality checks"
    echo "✅ Unit test execution"
    echo "✅ APK build validation"
    echo "✅ Common issue detection"
    echo ""
    echo "Full validation log: $VALIDATION_LOG"
    exit 0
else
    log_message "$RED" "❌ VALIDATION FAILED - DO NOT COMMIT"
    log_message "$RED" "Please fix the issues before committing"
    echo ""
    echo "Check the validation log for details: $VALIDATION_LOG"
    echo "Common fixes:"
    echo "- Run ./gradlew clean build to identify specific errors"
    echo "- Check Java syntax in modified files"
    echo "- Validate XML formatting"
    echo "- Run individual module builds to isolate issues"
    exit 1
fi