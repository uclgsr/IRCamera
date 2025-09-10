#!/bin/bash

# Advanced Quality Checking and Linting Script
# Performs comprehensive code quality analysis with rollback mechanisms

set -e

# Initialize timing
start_time=$(date +%s)

echo "🔍 Starting advanced quality checking and linting..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Quality metrics
declare -A quality_metrics
quality_metrics[kotlin_violations_before]=0
quality_metrics[kotlin_violations_after]=0
quality_metrics[java_violations_before]=0
quality_metrics[java_violations_after]=0
quality_metrics[python_violations_before]=0
quality_metrics[python_violations_after]=0
quality_metrics[compilation_errors_before]=0
quality_metrics[compilation_errors_after]=0

# Backup directory
backup_dir="quality_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$backup_dir"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to create backup
create_backup() {
    echo -e "${BLUE}📦 Creating backup before quality checks...${NC}"
    
    # Create Git stash for rollback capability
    git add . >/dev/null 2>&1 || true
    git stash push -m "Quality check backup $(date)" >/dev/null 2>&1 || true
    
    # Also create filesystem backup
    cp -r . "$backup_dir/" 2>/dev/null || true
    echo -e "${GREEN}✅ Backup created: $backup_dir${NC}"
}

# Function to rollback changes
rollback_changes() {
    echo -e "${RED}🔄 Rolling back changes due to compilation errors...${NC}"
    
    # Restore from Git stash
    git stash pop >/dev/null 2>&1 || true
    
    echo -e "${GREEN}✅ Rollback completed${NC}"
}

# Function to install linting tools
install_tools() {
    echo -e "${BLUE}📦 Installing advanced linting tools...${NC}"
    
    # Install ktlint
    if ! command_exists ktlint; then
        echo "Installing ktlint..."
        curl -sSLO https://github.com/pinterest/ktlint/releases/download/0.50.0/ktlint && chmod a+x ktlint && sudo mv ktlint /usr/local/bin/
    fi
    
    # Install checkstyle
    if ! command_exists checkstyle; then
        echo "Installing checkstyle..."
        wget -q https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.12.4/checkstyle-10.12.4-all.jar -O checkstyle.jar
        sudo mv checkstyle.jar /usr/local/bin/
        echo '#!/bin/bash' | sudo tee /usr/local/bin/checkstyle > /dev/null
        echo 'java -jar /usr/local/bin/checkstyle.jar "$@"' | sudo tee -a /usr/local/bin/checkstyle > /dev/null
        sudo chmod +x /usr/local/bin/checkstyle
    fi
    
    # Install Python tools
    if ! command_exists black; then
        echo "Installing Python formatting tools..."
        pip install black flake8 isort autopep8
    fi
    
    # Install ESLint (for future JS/TS files)
    if ! command_exists eslint && command_exists npm; then
        echo "Installing ESLint..."
        npm install -g eslint @typescript-eslint/parser @typescript-eslint/eslint-plugin
    fi
}

# Function to check compilation status
check_compilation() {
    local phase="$1"
    echo -e "${PURPLE}🔨 Checking compilation status ($phase)...${NC}"
    
    local errors=0
    
    # Check Kotlin/Java compilation (Android project)
    if [ -f "gradlew" ]; then
        echo "  Checking Android/Kotlin/Java compilation..."
        if ! ./gradlew compileDebugKotlin compileDebugJavaWithJavac >/dev/null 2>&1; then
            ((errors++))
            echo -e "${RED}  ❌ Kotlin/Java compilation failed${NC}"
        else
            echo -e "${GREEN}  ✅ Kotlin/Java compilation successful${NC}"
        fi
    fi
    
    # Check Python syntax
    echo "  Checking Python syntax..."
    python_errors=0
    for py_file in $(find . -name "*.py" -not -path "./build/*" -not -path "./.gradle/*" -not -path "./.*" | head -20); do
        if ! python -m py_compile "$py_file" >/dev/null 2>&1; then
            ((python_errors++))
        fi
    done
    
    if [ $python_errors -gt 0 ]; then
        ((errors++))
        echo -e "${RED}  ❌ Python syntax errors: $python_errors files${NC}"
    else
        echo -e "${GREEN}  ✅ Python syntax check passed${NC}"
    fi
    
    quality_metrics["compilation_errors_$phase"]=$errors
    return $errors
}

# Function to run Kotlin linting
lint_kotlin() {
    local phase="$1"
    echo -e "${PURPLE}🔍 Running Kotlin linting ($phase)...${NC}"
    
    local violations=0
    local temp_file=$(mktemp)
    
    # Find Kotlin files and run ktlint
    kotlin_files=$(find . -name "*.kt" -not -path "./build/*" -not -path "./.gradle/*" | head -100)
    
    if [ -n "$kotlin_files" ] && command_exists ktlint; then
        echo "$kotlin_files" | while read -r file; do
            if [ -f "$file" ]; then
                if ! ktlint "$file" >/dev/null 2>&1; then
                    echo "1" >> "$temp_file"
                fi
            fi
        done
        
        violations=$(wc -l < "$temp_file" 2>/dev/null || echo "0")
        rm -f "$temp_file"
        
        quality_metrics["kotlin_violations_$phase"]=$violations
        echo -e "${BLUE}  📊 Kotlin violations: $violations${NC}"
        
        # Auto-fix if requested and this is the "before" phase
        if [ "$phase" = "before" ] && [ $violations -gt 0 ]; then
            echo "  🔧 Auto-fixing Kotlin style issues..."
            echo "$kotlin_files" | while read -r file; do
                if [ -f "$file" ]; then
                    ktlint -F "$file" >/dev/null 2>&1 || true
                fi
            done
        fi
    else
        echo -e "${YELLOW}  ⚠️ No Kotlin files found or ktlint not available${NC}"
    fi
}

# Function to run Java linting
lint_java() {
    local phase="$1"
    echo -e "${PURPLE}🔍 Running Java linting ($phase)...${NC}"
    
    local violations=0
    local temp_file=$(mktemp)
    
    # Find Java files and run checkstyle
    java_files=$(find . -name "*.java" -not -path "./build/*" -not -path "./.gradle/*" | head -100)
    
    if [ -n "$java_files" ] && command_exists checkstyle; then
        echo "$java_files" | while read -r file; do
            if [ -f "$file" ]; then
                if ! checkstyle -c checkstyle.xml "$file" >/dev/null 2>&1; then
                    echo "1" >> "$temp_file"
                fi
            fi
        done
        
        violations=$(wc -l < "$temp_file" 2>/dev/null || echo "0")
        rm -f "$temp_file"
        
        quality_metrics["java_violations_$phase"]=$violations
        echo -e "${BLUE}  📊 Java violations: $violations${NC}"
    else
        echo -e "${YELLOW}  ⚠️ No Java files found or checkstyle not available${NC}"
    fi
}

# Function to run Python linting
lint_python() {
    local phase="$1"
    echo -e "${PURPLE}🔍 Running Python linting ($phase)...${NC}"
    
    local violations=0
    local temp_file=$(mktemp)
    
    # Find Python files and run linting
    python_files=$(find . -name "*.py" -not -path "./build/*" -not -path "./.gradle/*" -not -path "./.*" | head -50)
    
    if [ -n "$python_files" ] && command_exists flake8; then
        echo "$python_files" | while read -r file; do
            if [ -f "$file" ]; then
                if ! flake8 "$file" >/dev/null 2>&1; then
                    echo "1" >> "$temp_file"
                fi
            fi
        done
        
        violations=$(wc -l < "$temp_file" 2>/dev/null || echo "0")
        rm -f "$temp_file"
        
        quality_metrics["python_violations_$phase"]=$violations
        echo -e "${BLUE}  📊 Python violations: $violations${NC}"
        
        # Auto-fix if requested and this is the "before" phase
        if [ "$phase" = "before" ] && [ $violations -gt 0 ]; then
            echo "  🔧 Auto-fixing Python style issues..."
            echo "$python_files" | while read -r file; do
                if [ -f "$file" ]; then
                    black "$file" >/dev/null 2>&1 || true
                    isort "$file" >/dev/null 2>&1 || true
                fi
            done
        fi
    else
        echo -e "${YELLOW}  ⚠️ No Python files found or flake8 not available${NC}"
    fi
}

# Function to generate quality report
generate_quality_report() {
    echo -e "${GREEN}📊 Quality Metrics Report${NC}"
    echo "=================================="
    
    echo "🔍 Linting Results:"
    echo "  Kotlin violations: ${quality_metrics[kotlin_violations_before]} → ${quality_metrics[kotlin_violations_after]}"
    echo "  Java violations: ${quality_metrics[java_violations_before]} → ${quality_metrics[java_violations_after]}"
    echo "  Python violations: ${quality_metrics[python_violations_before]} → ${quality_metrics[python_violations_after]}"
    
    echo ""
    echo "🔨 Compilation Status:"
    echo "  Compilation errors: ${quality_metrics[compilation_errors_before]} → ${quality_metrics[compilation_errors_after]}"
    
    # Calculate improvements
    local kotlin_improvement=$((quality_metrics[kotlin_violations_before] - quality_metrics[kotlin_violations_after]))
    local java_improvement=$((quality_metrics[java_violations_before] - quality_metrics[java_violations_after]))
    local python_improvement=$((quality_metrics[python_violations_before] - quality_metrics[python_violations_after]))
    
    echo ""
    echo "📈 Improvements:"
    echo "  Kotlin: $kotlin_improvement violations fixed"
    echo "  Java: $java_improvement violations fixed" 
    echo "  Python: $python_improvement violations fixed"
    
    # Overall quality score
    local total_before=$((quality_metrics[kotlin_violations_before] + quality_metrics[java_violations_before] + quality_metrics[python_violations_before]))
    local total_after=$((quality_metrics[kotlin_violations_after] + quality_metrics[java_violations_after] + quality_metrics[python_violations_after]))
    local total_improvement=$((total_before - total_after))
    
    echo ""
    echo "🎯 Overall Quality Improvement: $total_improvement violations fixed"
    
    # Generate JSON report for CI/CD
    cat > quality_report.json << EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "metrics": {
    "kotlin": {
      "before": ${quality_metrics[kotlin_violations_before]},
      "after": ${quality_metrics[kotlin_violations_after]},
      "improvement": $kotlin_improvement
    },
    "java": {
      "before": ${quality_metrics[java_violations_before]},
      "after": ${quality_metrics[java_violations_after]},
      "improvement": $java_improvement
    },
    "python": {
      "before": ${quality_metrics[python_violations_before]},
      "after": ${quality_metrics[python_violations_after]},
      "improvement": $python_improvement
    },
    "compilation": {
      "errors_before": ${quality_metrics[compilation_errors_before]},
      "errors_after": ${quality_metrics[compilation_errors_after]}
    }
  },
  "summary": {
    "total_violations_before": $total_before,
    "total_violations_after": $total_after,
    "total_improvement": $total_improvement
  }
}
EOF
}

# Main execution
main() {
    echo -e "${GREEN}🚀 Advanced Code Quality Analysis Starting...${NC}"
    
    # Install tools
    install_tools
    
    # Create backup
    create_backup
    
    # Phase 1: Before formatting - collect baseline metrics
    echo -e "\n${BLUE}📊 Phase 1: Baseline Quality Metrics${NC}"
    check_compilation "before"
    lint_kotlin "before"
    lint_java "before"
    lint_python "before"
    
    # Phase 2: After formatting - collect improved metrics
    echo -e "\n${BLUE}📊 Phase 2: Post-Formatting Quality Metrics${NC}"
    check_compilation "after"
    lint_kotlin "after"
    lint_java "after"
    lint_python "after"
    
    # Check if compilation is broken after formatting
    if [ ${quality_metrics[compilation_errors_after]} -gt ${quality_metrics[compilation_errors_before]} ]; then
        echo -e "${RED}💥 Compilation errors increased after formatting! Rolling back...${NC}"
        rollback_changes
        exit 1
    fi
    
    # Generate report
    generate_quality_report
    
    # Cleanup
    rm -rf "$backup_dir" 2>/dev/null || true
    
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    
    echo -e "\n${GREEN}✅ Quality analysis completed in ${duration}s${NC}"
    echo -e "${GREEN}📊 Report saved to quality_report.json${NC}"
}

# Run main function
main "$@"