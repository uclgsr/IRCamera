#!/bin/bash

# IRCamera Development Tools - Simplified and Efficient
# Usage: ./dev.sh [command]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

show_help() {
    echo -e "${BLUE}IRCamera Development Tools${NC}"
    echo ""
    echo "Usage: ./dev.sh [command]"
    echo ""
    echo "Commands:"
    echo "  format      - Format all code files"
    echo "  lint        - Run linting checks"
    echo "  build       - Build the project"
    echo "  test        - Run tests"
    echo "  validate    - Run all checks (format + lint + build)"
    echo "  clean       - Clean build artifacts"
    echo "  setup       - Setup development environment"
    echo "  monitor     - Launch quality monitor"
    echo "  analyze     - Run performance analysis"
    echo "  health      - Quick health check"
    echo "  security    - Run security analysis"
    echo "  docs        - Generate documentation"
    echo "  test-suite  - Run advanced testing suite"
    echo "  release     - Manage releases and versioning"
    echo "  help        - Show this help"
    echo ""
    echo -e "${CYAN}Advanced Tools:${NC}"
    echo "  ./tools/quality-monitor.sh     - Real-time quality monitoring"
    echo "  ./tools/performance-analyzer.sh - Performance and optimization analysis"
    echo "  ./tools/security-scanner.sh    - Comprehensive security analysis"
    echo "  ./tools/advanced-testing.sh    - Advanced testing with coverage"
    echo "  ./tools/doc-generator.sh       - Automated documentation generation"
    echo "  ./tools/release-manager.sh     - Release management and versioning"
    echo "  ./status.sh                    - Project status overview"
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

setup_dev_environment() {
    print_status "Setting up development environment..."
    
    # Install pre-commit hooks
    if command -v pre-commit &> /dev/null; then
        pre-commit install
        print_status "Pre-commit hooks installed"
    else
        print_warning "pre-commit not found. Install with: pip install pre-commit"
    fi
    
    # Setup IDE configurations
    if [ ! -f ".editorconfig" ]; then
        print_warning "No .editorconfig found. Consider adding IDE configuration."
    fi
    
    print_status "Development environment setup completed"
}

format_code() {
    print_status "Formatting code..."
    
    local temp_file
    temp_file=$(mktemp)
    echo "0" > "$temp_file"
    
    # Format Kotlin files
    if command -v ktlint &> /dev/null; then
        find . -name "*.kt" -not -path "./build/*" -not -path "./.gradle/*" -print0 | while IFS= read -r -d '' file; do
            if ktlint --format "$file" 2>/dev/null; then
                local count
                count=$(cat "$temp_file")
                echo $((count + 1)) > "$temp_file"
            fi
        done
        print_status "Kotlin files formatted"
    else
        print_warning "ktlint not available. Install for Kotlin formatting."
    fi
    
    # Format Java files
    if command -v google-java-format &> /dev/null; then
        find . -name "*.java" -not -path "./build/*" -not -path "./.gradle/*" -print0 | while IFS= read -r -d '' file; do
            if google-java-format --replace "$file" 2>/dev/null; then
                local count
                count=$(cat "$temp_file")
                echo $((count + 1)) > "$temp_file"
            fi
        done
        print_status "Java files formatted"
    else
        print_warning "google-java-format not available. Install for Java formatting."
    fi
    
    # Format Python files
    if command -v black &> /dev/null; then
        find . -name "*.py" -print0 | while IFS= read -r -d '' file; do
            if black --quiet "$file" 2>/dev/null; then
                local count
                count=$(cat "$temp_file")
                echo $((count + 1)) > "$temp_file"
            fi
        done
        print_status "Python files formatted"
    fi
    
    # Format XML files
    if command -v xmllint &> /dev/null; then
        find . -name "*.xml" -not -path "./build/*" -not -path "./.gradle/*" -print0 | while IFS= read -r -d '' file; do
            if xmllint --format "$file" --output "$file" 2>/dev/null; then
                local count
                count=$(cat "$temp_file")
                echo $((count + 1)) > "$temp_file"
            fi
        done
    fi
    
    local files_formatted
    files_formatted=$(cat "$temp_file")
    rm -f "$temp_file"
    
    print_status "Code formatting completed ($files_formatted files processed)"
}

run_lint() {
    print_status "Running linting checks..."
    
    local errors=0
    
    # Kotlin lint
    if command -v ktlint &> /dev/null; then
        if ! find . -name "*.kt" -not -path "./build/*" -print0 | xargs -0 ktlint 2>/dev/null; then
            errors=$((errors + 1))
            print_error "Kotlin linting issues found"
        else
            print_status "Kotlin linting passed"
        fi
    fi
    
    # Python lint
    if command -v flake8 &> /dev/null; then
        if ! find . -name "*.py" -print0 | xargs -0 flake8 2>/dev/null; then
            errors=$((errors + 1))
            print_error "Python linting issues found"
        else
            print_status "Python linting passed"
        fi
    fi
    
    # Shell script lint
    if command -v shellcheck &> /dev/null; then
        if ! find . -name "*.sh" -print0 | xargs -0 shellcheck 2>/dev/null; then
            errors=$((errors + 1))
            print_error "Shell script linting issues found"
        else
            print_status "Shell script linting passed"
        fi
    fi
    
    # YAML lint
    if command -v yamllint &> /dev/null; then
        if ! find . \( -name "*.yml" -o -name "*.yaml" \) -print0 | xargs -0 yamllint -d relaxed 2>/dev/null; then
            errors=$((errors + 1))
            print_error "YAML linting issues found"
        else
            print_status "YAML linting passed"
        fi
    fi
    
    if [ $errors -eq 0 ]; then
        print_status "All linting checks passed"
    else
        print_error "Found $errors linting issues"
        return 1
    fi
}

build_project() {
    print_status "Building project..."
    
    if [ -f "./gradlew" ]; then
        if ./gradlew :app:assemble --quiet; then
            print_status "Build completed successfully"
        else
            print_error "Build failed"
            return 1
        fi
    else
        print_error "No gradlew found"
        return 1
    fi
}

run_tests() {
    print_status "Running tests..."
    
    if [ -f "./gradlew" ]; then
        if ./gradlew :app:testDebugUnitTest --quiet; then
            print_status "Tests completed successfully"
        else
            print_error "Tests failed"
            return 1
        fi
    else
        print_error "No gradlew found"
        return 1
    fi
}

validate_all() {
    print_status "Running full validation..."
    
    local start_time
    start_time=$(date +%s)
    
    # Run all validation steps
    format_code
    run_lint
    build_project
    
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    print_status "All validation checks passed! (${duration}s)"
}

quick_health_check() {
    print_status "Running quick health check..."
    
    local issues=0
    
    # Check gradle
    if ./gradlew help --quiet &>/dev/null; then
        print_status "Gradle: Working"
    else
        print_error "Gradle: Issues detected"
        issues=$((issues + 1))
    fi
    
    # Check dev tools
    if [ -x "./dev.sh" ]; then
        print_status "Dev Tools: Ready"
    else
        print_warning "Dev Tools: Not executable"
        issues=$((issues + 1))
    fi
    
    # Check pre-commit
    if [ -f ".pre-commit-config.yaml" ]; then
        print_status "Pre-commit: Configured"
    else
        print_warning "Pre-commit: Not configured"
    fi
    
    # Quick file count
    local kotlin_files java_files
    kotlin_files=$(find . -name "*.kt" -not -path "./build/*" | wc -l)
    java_files=$(find . -name "*.java" -not -path "./build/*" | wc -l)
    
    print_status "Code Files: $kotlin_files Kotlin, $java_files Java"
    
    if [ "$issues" -eq 0 ]; then
        print_status "All systems healthy!"
    else
        print_warning "Found $issues issues"
    fi
}

clean_artifacts() {
    print_status "Cleaning build artifacts..."
    
    if [ -f "./gradlew" ]; then
        ./gradlew clean --quiet
    fi
    
    # Remove common build artifacts
    find . -name "build" -type d -not -path "./.gradle/*" -exec rm -rf {} + 2>/dev/null || true
    find . -name "*.log" -delete 2>/dev/null || true
    find . -name "*.tmp" -delete 2>/dev/null || true
    find . -name ".kotlin" -type d -exec rm -rf {} + 2>/dev/null || true
    
    print_status "Clean completed"
}

# Main command handler
case "${1:-help}" in
    "format")
        format_code
        ;;
    "lint")
        run_lint
        ;;
    "build")
        build_project
        ;;
    "test")
        run_tests
        ;;
    "validate")
        validate_all
        ;;
    "clean")
        clean_artifacts
        ;;
    "setup")
        setup_dev_environment
        ;;
    "monitor")
        if [ -x "./tools/quality-monitor.sh" ]; then
            ./tools/quality-monitor.sh
        else
            print_error "Quality monitor not found or not executable"
        fi
        ;;
    "analyze")
        if [ -x "./tools/performance-analyzer.sh" ]; then
            ./tools/performance-analyzer.sh
        else
            print_error "Performance analyzer not found or not executable"
        fi
        ;;
    "health")
        quick_health_check
        ;;
    "security")
        if [ -x "./tools/security-scanner.sh" ]; then
            ./tools/security-scanner.sh
        else
            print_error "Security scanner not found or not executable"
        fi
        ;;
    "docs")
        if [ -x "./tools/doc-generator.sh" ]; then
            ./tools/doc-generator.sh
        else
            print_error "Documentation generator not found or not executable"
        fi
        ;;
    "test-suite")
        if [ -x "./tools/advanced-testing.sh" ]; then
            ./tools/advanced-testing.sh
        else
            print_error "Advanced testing suite not found or not executable"
        fi
        ;;
    "release")
        if [ -x "./tools/release-manager.sh" ]; then
            ./tools/release-manager.sh "${@:2}"
        else
            print_error "Release manager not found or not executable"
        fi
        ;;
    "help"|*)
        show_help
        ;;
esac