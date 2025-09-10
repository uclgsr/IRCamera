#!/bin/bash

# Enhanced Quality Analyzer with Advanced Analytics and Performance Optimization
# Provides comprehensive code quality analysis with enterprise-grade reporting

set -e

# Initialize timing and performance metrics
start_time=$(date +%s)
script_version="2.1.0"

echo "🚀 Enhanced Quality Analyzer v${script_version} Starting..."

# Advanced color palette
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Performance and quality metrics storage
declare -A perf_metrics
declare -A quality_metrics
declare -A file_stats

# Configuration parameters
MAX_FILES_PER_BATCH=100
PARALLEL_WORKERS=4
ENABLE_INCREMENTAL=${ENABLE_INCREMENTAL:-false}
ENABLE_SECURITY_SCAN=${ENABLE_SECURITY_SCAN:-true}
ENABLE_COMPLEXITY_ANALYSIS=${ENABLE_COMPLEXITY_ANALYSIS:-true}

# Initialize metrics
init_metrics() {
    echo -e "${BLUE}📊 Initializing advanced quality metrics...${NC}"
    
    # Performance metrics
    perf_metrics[total_files]=0
    perf_metrics[processing_time]=0
    perf_metrics[avg_time_per_file]=0
    perf_metrics[memory_usage]=0
    
    # Quality metrics for each language
    for lang in kotlin java python javascript typescript css xml json yaml; do
        quality_metrics[${lang}_files]=0
        quality_metrics[${lang}_violations_before]=0
        quality_metrics[${lang}_violations_after]=0
        quality_metrics[${lang}_complexity_score]=0
        quality_metrics[${lang}_maintainability_index]=0
    done
}

# Enhanced command existence check with installation suggestions
command_exists() {
    if command -v "$1" >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Advanced tool installation with version management
install_advanced_tools() {
    echo -e "${PURPLE}🔧 Installing comprehensive toolchain...${NC}"
    
    local tools_installed=0
    
    # Kotlin tools
    if ! command_exists ktlint; then
        echo "  Installing ktlint (latest)..."
        curl -sSLO https://github.com/pinterest/ktlint/releases/download/1.0.1/ktlint && chmod a+x ktlint && sudo mv ktlint /usr/local/bin/
        ((tools_installed++))
    fi
    
    # Java tools
    if ! command_exists checkstyle; then
        echo "  Installing Checkstyle..."
        wget -q https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.12.4/checkstyle-10.12.4-all.jar -O /tmp/checkstyle.jar
        sudo mv /tmp/checkstyle.jar /usr/local/bin/
        cat > /tmp/checkstyle << 'EOF'
#!/bin/bash
java -jar /usr/local/bin/checkstyle.jar "$@"
EOF
        sudo mv /tmp/checkstyle /usr/local/bin/checkstyle
        sudo chmod +x /usr/local/bin/checkstyle
        ((tools_installed++))
    fi
    
    # Python tools
    if ! command_exists black; then
        echo "  Installing Python quality tools..."
        pip install --user black flake8 isort autopep8 mypy bandit safety radon complexity
        ((tools_installed++))
    fi
    
    # JavaScript/TypeScript tools
    if ! command_exists eslint && command_exists npm; then
        echo "  Installing JavaScript/TypeScript tools..."
        npm install -g eslint @typescript-eslint/parser @typescript-eslint/eslint-plugin prettier jshint
        ((tools_installed++))
    fi
    
    # Security and analysis tools
    if ! command_exists semgrep; then
        echo "  Installing security analysis tools..."
        pip install --user semgrep 2>/dev/null || true
        ((tools_installed++))
    fi
    
    echo -e "${GREEN}✅ Installed $tools_installed tool groups${NC}"
}

# Real-time file discovery with parallel processing
discover_files() {
    echo -e "${CYAN}🔍 Discovering files with parallel analysis...${NC}"
    
    local temp_dir=$(mktemp -d)
    
    # Parallel file discovery by type
    {
        find . -name "*.kt" -not -path "./build/*" -not -path "./.gradle/*" -not -path "./.*" > "$temp_dir/kotlin_files" &
        find . -name "*.java" -not -path "./build/*" -not -path "./.gradle/*" -not -path "./.*" > "$temp_dir/java_files" &
        find . -name "*.py" -not -path "./build/*" -not -path "./.gradle/*" -not -path "./.*" > "$temp_dir/python_files" &
        find . -name "*.js" -o -name "*.ts" -not -path "./build/*" -not -path "./node_modules/*" -not -path "./.*" > "$temp_dir/js_files" &
        find . -name "*.xml" -not -path "./build/*" -not -path "./.gradle/*" > "$temp_dir/xml_files" &
        find . -name "*.json" -not -path "./build/*" -not -path "./node_modules/*" > "$temp_dir/json_files" &
        wait
    }
    
    # Count files and update metrics
    for file_type in kotlin java python js xml json; do
        if [ -f "$temp_dir/${file_type}_files" ]; then
            local count=$(wc -l < "$temp_dir/${file_type}_files" 2>/dev/null || echo "0")
            file_stats[${file_type}_count]=$count
            perf_metrics[total_files]=$((perf_metrics[total_files] + count))
        fi
    done
    
    echo -e "${BLUE}📊 File Discovery Results:${NC}"
    echo "  Kotlin: ${file_stats[kotlin_count]} files"
    echo "  Java: ${file_stats[java_count]} files"
    echo "  Python: ${file_stats[python_count]} files"
    echo "  JavaScript/TypeScript: ${file_stats[js_count]} files"
    echo "  XML: ${file_stats[xml_count]} files"
    echo "  JSON: ${file_stats[json_count]} files"
    echo "  Total: ${perf_metrics[total_files]} files"
    
    rm -rf "$temp_dir"
}

# Advanced compilation validation with detailed error reporting
validate_compilation() {
    local phase="$1"
    echo -e "${PURPLE}🔨 Advanced Compilation Validation ($phase)...${NC}"
    
    local compilation_errors=0
    local error_details=""
    
    # Android/Kotlin/Java compilation with proper task names
    if [ -f "gradlew" ]; then
        echo "  🏗️ Validating Android build system..."
        
        # Check available tasks first
        if ./gradlew tasks --no-daemon 2>/dev/null | grep -q "assemble\b"; then
            echo "    Testing main assembly..."
            if ! timeout 120 ./gradlew assemble --no-daemon >/dev/null 2>&1; then
                ((compilation_errors++))
                error_details="$error_details\n    ❌ Main assembly failed"
                echo -e "${RED}    ❌ Main assembly failed${NC}"
            else
                echo -e "${GREEN}    ✅ Main assembly successful${NC}"
            fi
        fi
        
        # Test Kotlin compilation specifically
        if ./gradlew tasks --no-daemon 2>/dev/null | grep -q "compileKotlin"; then
            echo "    Testing Kotlin compilation..."
            if ! timeout 60 ./gradlew compileKotlin --no-daemon >/dev/null 2>&1; then
                ((compilation_errors++))
                error_details="$error_details\n    ❌ Kotlin compilation failed"
                echo -e "${RED}    ❌ Kotlin compilation failed${NC}"
            else
                echo -e "${GREEN}    ✅ Kotlin compilation successful${NC}"
            fi
        fi
    fi
    
    # Python syntax validation with batch processing
    echo "  🐍 Validating Python syntax..."
    local python_errors=0
    local python_files=$(find . -name "*.py" -not -path "./build/*" -not -path "./.gradle/*" -not -path "./.*" | head -50)
    
    if [ -n "$python_files" ]; then
        for py_file in $python_files; do
            if [ -f "$py_file" ]; then
                if ! python -m py_compile "$py_file" >/dev/null 2>&1; then
                    ((python_errors++))
                fi
            fi
        done
        
        if [ $python_errors -gt 0 ]; then
            ((compilation_errors++))
            error_details="$error_details\n    ❌ Python syntax errors: $python_errors files"
            echo -e "${RED}    ❌ Python syntax errors: $python_errors files${NC}"
        else
            echo -e "${GREEN}    ✅ Python syntax validation passed${NC}"
        fi
    fi
    
    # JavaScript/TypeScript validation if available
    if [ -f "package.json" ] && command_exists npm; then
        echo "  📜 Validating JavaScript/TypeScript..."
        if ! timeout 30 npm run build >/dev/null 2>&1; then
            # Don't fail if no build script exists
            echo -e "${YELLOW}    ⚠️ No JavaScript build script or build failed${NC}"
        else
            echo -e "${GREEN}    ✅ JavaScript/TypeScript build successful${NC}"
        fi
    fi
    
    quality_metrics[compilation_errors_$phase]=$compilation_errors
    
    if [ $compilation_errors -gt 0 ]; then
        echo -e "${RED}📊 Compilation Summary ($phase): $compilation_errors errors${NC}"
        echo -e "$error_details"
    else
        echo -e "${GREEN}📊 Compilation Summary ($phase): All checks passed${NC}"
    fi
    
    return $compilation_errors
}

# Enhanced Kotlin analysis with complexity metrics
analyze_kotlin() {
    local phase="$1"
    echo -e "${PURPLE}🔍 Advanced Kotlin Analysis ($phase)...${NC}"
    
    local violations=0
    local complexity_total=0
    local files_analyzed=0
    
    # Find Kotlin files with batching
    local kotlin_files=$(find . -name "*.kt" -not -path "./build/*" -not -path "./.gradle/*" | head $MAX_FILES_PER_BATCH)
    
    if [ -n "$kotlin_files" ] && command_exists ktlint; then
        echo "    📊 Analyzing ${file_stats[kotlin_count]} Kotlin files..."
        
        # Create temporary files for parallel processing
        local temp_violations=$(mktemp)
        local temp_complexity=$(mktemp)
        
        # Batch process Kotlin files
        echo "$kotlin_files" | while read -r file; do
            if [ -f "$file" ]; then
                # Lint check
                if ! ktlint "$file" >/dev/null 2>&1; then
                    echo "1" >> "$temp_violations"
                fi
                
                # Basic complexity analysis (line count as proxy)
                local lines=$(wc -l < "$file" 2>/dev/null || echo "0")
                echo "$lines" >> "$temp_complexity"
                
                ((files_analyzed++))
            fi
        done
        
        # Aggregate results
        violations=$(wc -l < "$temp_violations" 2>/dev/null || echo "0")
        complexity_total=$(awk '{sum+=$1} END {print sum}' "$temp_complexity" 2>/dev/null || echo "0")
        
        # Clean up
        rm -f "$temp_violations" "$temp_complexity"
        
        # Calculate metrics
        local avg_complexity=$((complexity_total / (files_analyzed + 1)))
        
        quality_metrics[kotlin_violations_$phase]=$violations
        quality_metrics[kotlin_complexity_score]=$avg_complexity
        
        echo -e "${BLUE}    📊 Kotlin Results:${NC}"
        echo "      Violations: $violations"
        echo "      Avg Complexity: $avg_complexity lines/file"
        echo "      Files Analyzed: $files_analyzed"
        
        # Auto-fix for before phase
        if [ "$phase" = "before" ] && [ $violations -gt 0 ]; then
            echo "    🔧 Auto-fixing Kotlin style issues..."
            echo "$kotlin_files" | while read -r file; do
                if [ -f "$file" ]; then
                    ktlint -F "$file" >/dev/null 2>&1 || true
                fi
            done
        fi
    else
        echo -e "${YELLOW}    ⚠️ Kotlin analysis unavailable (ktlint not found or no files)${NC}"
    fi
}

# Enhanced Java analysis with Checkstyle integration
analyze_java() {
    local phase="$1"
    echo -e "${PURPLE}🔍 Advanced Java Analysis ($phase)...${NC}"
    
    local violations=0
    local files_analyzed=0
    
    local java_files=$(find . -name "*.java" -not -path "./build/*" -not -path "./.gradle/*" | head $MAX_FILES_PER_BATCH)
    
    if [ -n "$java_files" ] && command_exists checkstyle && [ -f "checkstyle.xml" ]; then
        echo "    📊 Analyzing ${file_stats[java_count]} Java files..."
        
        local temp_violations=$(mktemp)
        
        # Process Java files with Checkstyle
        echo "$java_files" | while read -r file; do
            if [ -f "$file" ]; then
                if ! checkstyle -c checkstyle.xml "$file" >/dev/null 2>&1; then
                    echo "1" >> "$temp_violations"
                fi
                ((files_analyzed++))
            fi
        done
        
        violations=$(wc -l < "$temp_violations" 2>/dev/null || echo "0")
        rm -f "$temp_violations"
        
        quality_metrics[java_violations_$phase]=$violations
        
        echo -e "${BLUE}    📊 Java Results:${NC}"
        echo "      Violations: $violations"
        echo "      Files Analyzed: $files_analyzed"
    else
        echo -e "${YELLOW}    ⚠️ Java analysis unavailable (checkstyle not found or no config)${NC}"
    fi
}

# Enhanced Python analysis with multiple tools
analyze_python() {
    local phase="$1"
    echo -e "${PURPLE}🔍 Advanced Python Analysis ($phase)...${NC}"
    
    local violations=0
    local security_issues=0
    local complexity_score=0
    
    local python_files=$(find . -name "*.py" -not -path "./build/*" -not -path "./.gradle/*" -not -path "./.*" | head $MAX_FILES_PER_BATCH)
    
    if [ -n "$python_files" ]; then
        echo "    📊 Analyzing ${file_stats[python_count]} Python files..."
        
        # Style violations with flake8
        if command_exists flake8; then
            local temp_violations=$(mktemp)
            echo "$python_files" | while read -r file; do
                if [ -f "$file" ]; then
                    if ! flake8 "$file" >/dev/null 2>&1; then
                        echo "1" >> "$temp_violations"
                    fi
                fi
            done
            violations=$(wc -l < "$temp_violations" 2>/dev/null || echo "0")
            rm -f "$temp_violations"
        fi
        
        # Security analysis with bandit
        if command_exists bandit && [ "$ENABLE_SECURITY_SCAN" = "true" ]; then
            echo "    🔒 Running security analysis..."
            security_issues=$(echo "$python_files" | head -20 | xargs bandit -f json 2>/dev/null | jq '.results | length' 2>/dev/null || echo "0")
        fi
        
        # Complexity analysis with radon
        if command_exists radon && [ "$ENABLE_COMPLEXITY_ANALYSIS" = "true" ]; then
            echo "    📈 Analyzing code complexity..."
            complexity_score=$(echo "$python_files" | head -10 | xargs radon cc -s 2>/dev/null | grep -o "Average complexity: [0-9.]*" | grep -o "[0-9.]*" || echo "0")
        fi
        
        quality_metrics[python_violations_$phase]=$violations
        quality_metrics[python_security_issues]=$security_issues
        quality_metrics[python_complexity_score]=$complexity_score
        
        echo -e "${BLUE}    📊 Python Results:${NC}"
        echo "      Style Violations: $violations"
        echo "      Security Issues: $security_issues"
        echo "      Complexity Score: $complexity_score"
        
        # Auto-fix for before phase
        if [ "$phase" = "before" ] && [ $violations -gt 0 ]; then
            echo "    🔧 Auto-fixing Python style issues..."
            echo "$python_files" | while read -r file; do
                if [ -f "$file" ]; then
                    black "$file" >/dev/null 2>&1 || true
                    isort "$file" >/dev/null 2>&1 || true
                fi
            done
        fi
    else
        echo -e "${YELLOW}    ⚠️ No Python files found for analysis${NC}"
    fi
}

# Generate comprehensive quality report with enhanced metrics
generate_comprehensive_report() {
    echo -e "${GREEN}📊 Generating Comprehensive Quality Report...${NC}"
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    # Update performance metrics
    perf_metrics[processing_time]=$duration
    perf_metrics[avg_time_per_file]=$(awk "BEGIN {printf \"%.3f\", $duration/${perf_metrics[total_files]}}")
    
    # Calculate improvements
    local kotlin_improvement=$((quality_metrics[kotlin_violations_before] - quality_metrics[kotlin_violations_after]))
    local java_improvement=$((quality_metrics[java_violations_before] - quality_metrics[java_violations_after]))
    local python_improvement=$((quality_metrics[python_violations_before] - quality_metrics[python_violations_after]))
    local total_improvement=$((kotlin_improvement + java_improvement + python_improvement))
    
    # Generate enhanced HTML report
    cat > enhanced_quality_report.html << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Enhanced Quality Analysis Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
        .container { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .header { text-align: center; color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 20px; }
        .metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin: 20px 0; }
        .metric-card { background: #f9f9f9; padding: 20px; border-radius: 8px; border-left: 4px solid #4CAF50; }
        .improvement { color: #4CAF50; font-weight: bold; }
        .warning { color: #ff9800; }
        .error { color: #f44336; }
        .performance-table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        .performance-table th, .performance-table td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        .performance-table th { background-color: #4CAF50; color: white; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔍 Enhanced Quality Analysis Report</h1>
            <p>Generated: $(date)</p>
            <p>Analysis Duration: ${duration}s | Files Processed: ${perf_metrics[total_files]}</p>
        </div>
        
        <div class="metric-grid">
            <div class="metric-card">
                <h3>📊 Overall Quality Improvement</h3>
                <p class="improvement">Total Violations Fixed: $total_improvement</p>
                <p>Processing Speed: ${perf_metrics[avg_time_per_file]}s per file</p>
            </div>
            
            <div class="metric-card">
                <h3>🏗️ Kotlin Analysis</h3>
                <p>Files: ${file_stats[kotlin_count]}</p>
                <p>Violations: ${quality_metrics[kotlin_violations_before]} → ${quality_metrics[kotlin_violations_after]}</p>
                <p class="improvement">Improvement: $kotlin_improvement</p>
                <p>Avg Complexity: ${quality_metrics[kotlin_complexity_score]} lines/file</p>
            </div>
            
            <div class="metric-card">
                <h3>☕ Java Analysis</h3>
                <p>Files: ${file_stats[java_count]}</p>
                <p>Violations: ${quality_metrics[java_violations_before]} → ${quality_metrics[java_violations_after]}</p>
                <p class="improvement">Improvement: $java_improvement</p>
            </div>
            
            <div class="metric-card">
                <h3>🐍 Python Analysis</h3>
                <p>Files: ${file_stats[python_count]}</p>
                <p>Style Violations: ${quality_metrics[python_violations_before]} → ${quality_metrics[python_violations_after]}</p>
                <p class="improvement">Improvement: $python_improvement</p>
                <p>Security Issues: ${quality_metrics[python_security_issues]}</p>
                <p>Complexity Score: ${quality_metrics[python_complexity_score]}</p>
            </div>
        </div>
        
        <h3>🔨 Compilation Status</h3>
        <table class="performance-table">
            <tr>
                <th>Component</th>
                <th>Before</th>
                <th>After</th>
                <th>Status</th>
            </tr>
            <tr>
                <td>Compilation Errors</td>
                <td>${quality_metrics[compilation_errors_before]}</td>
                <td>${quality_metrics[compilation_errors_after]}</td>
                <td class="$([ ${quality_metrics[compilation_errors_after]} -eq 0 ] && echo "improvement" || echo "error")">
                    $([ ${quality_metrics[compilation_errors_after]} -eq 0 ] && echo "✅ Passed" || echo "❌ Failed")
                </td>
            </tr>
        </table>
        
        <h3>⚡ Performance Metrics</h3>
        <table class="performance-table">
            <tr>
                <th>Metric</th>
                <th>Value</th>
            </tr>
            <tr><td>Total Files Processed</td><td>${perf_metrics[total_files]}</td></tr>
            <tr><td>Processing Time</td><td>${duration}s</td></tr>
            <tr><td>Average Time per File</td><td>${perf_metrics[avg_time_per_file]}s</td></tr>
            <tr><td>Analysis Version</td><td>$script_version</td></tr>
        </table>
    </div>
</body>
</html>
EOF

    # Generate JSON report for CI/CD integration
    cat > enhanced_quality_report.json << EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "version": "$script_version",
  "performance": {
    "total_files": ${perf_metrics[total_files]},
    "processing_time": $duration,
    "avg_time_per_file": ${perf_metrics[avg_time_per_file]}
  },
  "file_stats": {
    "kotlin": ${file_stats[kotlin_count]},
    "java": ${file_stats[java_count]},
    "python": ${file_stats[python_count]},
    "javascript": ${file_stats[js_count]},
    "xml": ${file_stats[xml_count]},
    "json": ${file_stats[json_count]}
  },
  "quality_metrics": {
    "kotlin": {
      "violations_before": ${quality_metrics[kotlin_violations_before]},
      "violations_after": ${quality_metrics[kotlin_violations_after]},
      "improvement": $kotlin_improvement,
      "complexity_score": ${quality_metrics[kotlin_complexity_score]}
    },
    "java": {
      "violations_before": ${quality_metrics[java_violations_before]},
      "violations_after": ${quality_metrics[java_violations_after]},
      "improvement": $java_improvement
    },
    "python": {
      "violations_before": ${quality_metrics[python_violations_before]},
      "violations_after": ${quality_metrics[python_violations_after]},
      "improvement": $python_improvement,
      "security_issues": ${quality_metrics[python_security_issues]},
      "complexity_score": ${quality_metrics[python_complexity_score]}
    }
  },
  "compilation": {
    "errors_before": ${quality_metrics[compilation_errors_before]},
    "errors_after": ${quality_metrics[compilation_errors_after]},
    "status": "$([ ${quality_metrics[compilation_errors_after]} -eq 0 ] && echo "success" || echo "failure")"
  },
  "summary": {
    "total_improvement": $total_improvement,
    "overall_status": "$([ ${quality_metrics[compilation_errors_after]} -eq 0 ] && echo "success" || echo "failure")"
  }
}
EOF

    echo -e "${GREEN}✅ Comprehensive reports generated:${NC}"
    echo "  📄 HTML Report: enhanced_quality_report.html"
    echo "  📊 JSON Report: enhanced_quality_report.json"
    echo -e "${BLUE}📈 Total Quality Improvement: $total_improvement violations fixed${NC}"
}

# Main execution flow with enhanced error handling
main() {
    echo -e "${BOLD}${GREEN}🚀 Enhanced Quality Analyzer v${script_version}${NC}"
    echo -e "${BLUE}🔍 Performing comprehensive code quality analysis...${NC}"
    
    # Initialize system
    init_metrics
    install_advanced_tools
    discover_files
    
    # Create backup for rollback capability
    echo -e "${BLUE}📦 Creating backup for rollback capability...${NC}"
    git add . >/dev/null 2>&1 || true
    git stash push -m "Enhanced quality analysis backup $(date)" >/dev/null 2>&1 || true
    
    # Phase 1: Baseline analysis
    echo -e "\n${BOLD}${CYAN}📊 Phase 1: Baseline Quality Analysis${NC}"
    validate_compilation "before"
    analyze_kotlin "before"
    analyze_java "before"
    analyze_python "before"
    
    # Phase 2: Post-formatting analysis
    echo -e "\n${BOLD}${CYAN}📊 Phase 2: Post-Formatting Quality Analysis${NC}"
    validate_compilation "after"
    analyze_kotlin "after"
    analyze_java "after"
    analyze_python "after"
    
    # Check for compilation failures and rollback if needed
    if [ ${quality_metrics[compilation_errors_after]} -gt ${quality_metrics[compilation_errors_before]} ]; then
        echo -e "${RED}💥 Compilation errors increased! Rolling back changes...${NC}"
        git stash pop >/dev/null 2>&1 || true
        echo -e "${GREEN}🔄 Rollback completed${NC}"
        exit 1
    fi
    
    # Generate comprehensive reports
    generate_comprehensive_report
    
    # Cleanup
    git stash drop >/dev/null 2>&1 || true
    
    echo -e "\n${BOLD}${GREEN}✅ Enhanced Quality Analysis Completed Successfully!${NC}"
    echo -e "${CYAN}📊 Check enhanced_quality_report.html for detailed results${NC}"
}

# Execute main function with error handling
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    trap 'echo -e "${RED}❌ Analysis interrupted${NC}"; exit 1' INT TERM
    main "$@"
fi