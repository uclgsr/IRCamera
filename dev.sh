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
    echo "  format    - Format all code files"
    echo "  lint      - Run linting checks"
    echo "  build     - Build the project"
    echo "  test      - Run tests"
    echo "  validate  - Run all checks (format + lint + build)"
    echo "  clean     - Clean build artifacts"
    echo "  setup     - Setup development environment"
    echo "  help      - Show this help"
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
    
    local files_formatted=0
    
    # Format Kotlin files
    if command -v ktlint &> /dev/null; then
        find . -name "*.kt" -not -path "./build/*" -not -path "./.gradle/*" | while read -r file; do
            if ktlint --format "$file" 2>/dev/null; then
                ((files_formatted++))
            fi
        done
        print_status "Kotlin files formatted"
    else
        print_warning "ktlint not available. Install for Kotlin formatting."
    fi
    
    # Format Java files
    if command -v google-java-format &> /dev/null; then
        find . -name "*.java" -not -path "./build/*" -not -path "./.gradle/*" | while read -r file; do
            google-java-format --replace "$file" 2>/dev/null && ((files_formatted++))
        done
        print_status "Java files formatted"
    else
        print_warning "google-java-format not available. Install for Java formatting."
    fi
    
    # Format Python files
    if command -v black &> /dev/null; then
        find . -name "*.py" | while read -r file; do
            black --quiet "$file" 2>/dev/null && ((files_formatted++))
        done
        print_status "Python files formatted"
    fi
    
    # Format XML files
    find . -name "*.xml" -not -path "./build/*" -not -path "./.gradle/*" | while read -r file; do
        if command -v xmllint &> /dev/null; then
            xmllint --format "$file" --output "$file" 2>/dev/null && ((files_formatted++))
        fi
    done
    
    print_status "Code formatting completed"
}

run_lint() {
    print_status "Running linting checks..."
    
    local errors=0
    
    # Kotlin lint
    if command -v ktlint &> /dev/null; then
        if ! find . -name "*.kt" -not -path "./build/*" | xargs ktlint 2>/dev/null; then
            ((errors++))
            print_error "Kotlin linting issues found"
        else
            print_status "Kotlin linting passed"
        fi
    fi
    
    # Python lint
    if command -v flake8 &> /dev/null; then
        if ! find . -name "*.py" | xargs flake8 2>/dev/null; then
            ((errors++))
            print_error "Python linting issues found"
        else
            print_status "Python linting passed"
        fi
    fi
    
    # Shell script lint
    if command -v shellcheck &> /dev/null; then
        if ! find . -name "*.sh" | xargs shellcheck 2>/dev/null; then
            ((errors++))
            print_error "Shell script linting issues found"
        else
            print_status "Shell script linting passed"
        fi
    fi
    
    # YAML lint
    if command -v yamllint &> /dev/null; then
        if ! find . -name "*.yml" -o -name "*.yaml" | xargs yamllint -d relaxed 2>/dev/null; then
            ((errors++))
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
    
    local start_time=$(date +%s)
    
    # Run all validation steps
    format_code
    run_lint
    build_project
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    print_status "All validation checks passed! (${duration}s)"
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
    "help"|*)
        show_help
        ;;
esac