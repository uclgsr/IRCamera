#!/bin/bash

# Unified Validation Script - Enterprise-Grade Quality Validation
# Usage: ./validate.sh [quick|core|full|enterprise] [--auto-fix]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Configuration
MODE="${1:-core}"
AUTO_FIX="${2:-}"
START_TIME=$(date +%s)
QUALITY_THRESHOLD=85

print_header() {
    echo -e "${PURPLE}================================================================${NC}"
    echo -e "${PURPLE}🏆 IRCamera Enterprise Validation System${NC}"
    echo -e "${PURPLE}================================================================${NC}"
    echo -e "Mode: ${YELLOW}$MODE${NC}"
    echo -e "Auto-fix: ${YELLOW}${AUTO_FIX:-disabled}${NC}"
    echo -e "Quality Threshold: ${YELLOW}$QUALITY_THRESHOLD/100${NC}"
    echo ""
}

print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

validate_syntax() {
    print_status "Validating syntax..."
    local errors=0

    # Kotlin files
    if command -v ktlint >/dev/null 2>&1; then
        if ! ktlint --format 2>/dev/null; then
            ((errors++))
        fi
    fi

    # Java files (basic syntax check)
    find . -name "*.java" -not -path "./build/*" -not -path "./.gradle/*" | while read -r file; do
        if ! javac -cp "$(find . -name "*.jar" | tr '\n' ':')" -d /tmp -Xlint:none "$file" 2>/dev/null; then
            print_warning "Java syntax issue in $file"
        fi
    done

    # Python files
    find . -name "*.py" | while read -r file; do
        if ! python3 -m py_compile "$file" 2>/dev/null; then
            print_warning "Python syntax issue in $file"
        fi
    done

    return $errors
}

validate_build() {
    print_status "Validating build..."
    case $MODE in
        "quick")
            ./gradlew assembleDebug --quiet || return 1
            ;;
        "core")
            ./gradlew compileReleaseSources --quiet || return 1
            ;;
        "full")
            ./gradlew build --quiet || return 1
            ;;
    esac
}

validate_tests() {
    if [[ "$MODE" == "full" ]]; then
        print_status "Running tests..."
        ./gradlew test --quiet || return 1
    fi
}

run_auto_fix() {
    if [[ "$AUTO_FIX" == "--auto-fix" ]]; then
        print_status "Running auto-fix..."

        # Format code
        if command -v ktlint >/dev/null 2>&1; then
            ktlint --format 2>/dev/null || true
        fi

        # Format XML
        find . -name "*.xml" -not -path "./build/*" -not -path "./.gradle/*" | while read -r file; do
            if command -v xmllint >/dev/null 2>&1; then
                if xmllint --format "$file" > "${file}.tmp" 2>/dev/null; then
                    mv "${file}.tmp" "$file"
                else
                    rm -f "${file}.tmp"
                fi
            fi
        done

        # Remove Chinese text from strings.xml
        find . -name "strings.xml" | while read -r file; do
            if grep -q '[^\x00-\x7F]' "$file"; then
                grep -v '[^\x00-\x7F]' "$file" > "${file}.tmp" && mv "${file}.tmp" "$file"
                print_status "Cleaned Chinese text from $file"
            fi
        done
    fi
}

main() {
    print_header

    local exit_code=0

    # Run validations based on mode
    case $MODE in
        "quick")
            validate_syntax || exit_code=1
            ;;
        "core")
            validate_syntax || exit_code=1
            validate_build || exit_code=1
            ;;
        "full")
            validate_syntax || exit_code=1
            validate_build || exit_code=1
            validate_tests || exit_code=1
            ;;
        "enterprise")
            print_status "🚀 Starting enterprise-grade quality analysis..."
            
            # Run comprehensive quality analysis
            if [[ -f "enterprise_quality_enforcer.py" ]]; then
                chmod +x enterprise_quality_enforcer.py
                
                if python3 enterprise_quality_enforcer.py --all; then
                    print_status "✅ Enterprise quality analysis completed successfully"
                    
                    # Check quality gates
                    if [[ -f "quality_reports/quality_dashboard.json" ]]; then
                        local quality_score
                        quality_score=$(grep -o '"overall_score": [0-9]\+' quality_reports/quality_dashboard.json | grep -o '[0-9]\+' || echo "0")
                        
                        if [[ $quality_score -ge $QUALITY_THRESHOLD ]]; then
                            print_status "🏆 Quality gates passed: $quality_score/100"
                        else
                            print_error "🚫 Quality gates failed: $quality_score/100 (required: $QUALITY_THRESHOLD)"
                            echo "💡 View detailed report: quality_reports/enterprise_quality_report.html"
                            exit_code=1
                        fi
                    fi
                else
                    print_error "❌ Enterprise quality analysis failed"
                    exit_code=1
                fi
            else
                print_warning "Enterprise quality enforcer not found, falling back to full validation"
                validate_syntax || exit_code=1
                validate_build || exit_code=1
                validate_tests || exit_code=1
            fi
            ;;
        *)
            print_error "Invalid mode: $MODE. Use: quick|core|full|enterprise"
            exit 1
            ;;
    esac

    # Auto-fix if requested
    run_auto_fix

    # Summary
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - START_TIME))

    echo ""
    if [[ $exit_code -eq 0 ]]; then
        print_status "Validation completed successfully in ${duration}s"
    else
        print_error "Validation failed in ${duration}s"
    fi

    exit $exit_code
}

# Show help
if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "Usage: $0 [MODE] [--auto-fix]"
    echo ""
    echo "Modes:"
    echo "  quick  - Syntax validation only (~15s)"
    echo "  core   - Syntax + build validation (~60s)"
    echo "  full   - Syntax + build + tests (~300s)"
    echo ""
    echo "Options:"
    echo "  --auto-fix  - Automatically fix formatting issues"
    echo ""
    exit 0
fi

main "$@"