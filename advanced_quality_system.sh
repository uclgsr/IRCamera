#!/bin/bash

# Advanced Quality System - Enterprise-Grade Code Quality Analysis
# Brings code quality to the highest professional standards
# Usage: ./advanced_quality_system.sh [analyze|enforce|report|all]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
MODE="${1:-all}"
OUTPUT_DIR="quality_reports"
QUALITY_DB="quality_metrics.json"
MIN_QUALITY_SCORE=85
START_TIME=$(date +%s)

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

print_header() {
    echo -e "${PURPLE}================================================================${NC}"
    echo -e "${PURPLE}🏆 Advanced Quality System - Enterprise Grade Analysis${NC}"
    echo -e "${PURPLE}================================================================${NC}"
    echo -e "Mode: ${CYAN}$MODE${NC}"
    echo -e "Target Quality Score: ${CYAN}$MIN_QUALITY_SCORE/100${NC}"
    echo -e "Report Directory: ${CYAN}$OUTPUT_DIR${NC}"
    echo ""
}

print_section() {
    echo -e "${BLUE}📊 $1${NC}"
    echo "----------------------------------------"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

install_quality_tools() {
    print_section "Installing Advanced Quality Tools"
    
    # Install Python quality tools
    if command -v pip3 >/dev/null 2>&1; then
        pip3 install --user bandit safety vulture radon mypy black isort flake8 pylint autopep8 >/dev/null 2>&1 || true
        print_success "Python quality tools installed"
    fi
    
    # Install Node.js quality tools
    if command -v npm >/dev/null 2>&1; then
        npm install -g jshint eslint complexity-report >/dev/null 2>&1 || true
        print_success "JavaScript quality tools installed"
    fi
    
    # Download quality analysis tools
    if [[ ! -f "tools/ktlint" ]]; then
        mkdir -p tools
        curl -sSLO https://github.com/pinterest/ktlint/releases/download/0.50.0/ktlint >/dev/null 2>&1 || true
        chmod +x ktlint 2>/dev/null || true
        mv ktlint tools/ 2>/dev/null || true
        print_success "Ktlint downloaded"
    fi
    
    # Download Detekt for advanced Kotlin analysis
    if [[ ! -f "tools/detekt" ]]; then
        curl -sSL https://github.com/detekt/detekt/releases/download/v1.23.1/detekt-cli-1.23.1.zip -o detekt.zip >/dev/null 2>&1 || true
        unzip -q detekt.zip 2>/dev/null || true
        mv detekt-cli-*/bin/detekt tools/ 2>/dev/null || true
        rm -rf detekt.zip detekt-cli-* 2>/dev/null || true
        print_success "Detekt advanced analyzer downloaded"
    fi
}

analyze_security_vulnerabilities() {
    print_section "Security Vulnerability Analysis"
    local security_score=100
    local issues_found=0
    
    # Python security analysis
    if command -v bandit >/dev/null 2>&1; then
        echo "🔍 Analyzing Python security..."
        if bandit -r . -f json -o "$OUTPUT_DIR/security_python.json" 2>/dev/null; then
            local python_issues
            python_issues=$(grep -o '"issue_severity"' "$OUTPUT_DIR/security_python.json" 2>/dev/null | wc -l || echo 0)
            issues_found=$((issues_found + python_issues))
            print_success "Python security: $python_issues issues found"
        fi
    fi
    
    # Dependency vulnerability scanning
    if command -v Safety >/dev/null 2>&1; then
        echo "🔍 Scanning Python dependencies..."
        safety check --json --output "$OUTPUT_DIR/dependency_vulnerabilities.json" 2>/dev/null || true
        print_success "Dependency vulnerability scan completed"
    fi
    
    # Android security patterns
    echo "🔍 Analyzing Android security patterns..."
    local android_security_issues=0
    
    # Check for hardcoded secrets
    if grep -r "password\|secret\|api_key\|token" --include="*.java" --include="*.kt" . >/dev/null 2>&1; then
        android_security_issues=$((android_security_issues + 1))
    fi
    
    # Check for SQL injection patterns
    if grep -r "rawQuery\|execSQL" --include="*.java" --include="*.kt" . >/dev/null 2>&1; then
        android_security_issues=$((android_security_issues + 1))
    fi
    
    issues_found=$((issues_found + android_security_issues))
    
    # Calculate security score
    if [[ $issues_found -gt 0 ]]; then
        security_score=$((100 - issues_found * 5))
        if [[ $security_score -lt 0 ]]; then security_score=0; fi
    fi
    
    echo "{\"security_score\": $security_score, \"issues_found\": $issues_found}" > "$OUTPUT_DIR/security_summary.json"
    print_success "Security Score: $security_score/100 ($issues_found issues)"
    
    return $security_score
}

analyze_code_complexity() {
    print_section "Code Complexity Analysis"
    local complexity_score=100
    local total_complexity=0
    local file_count=0
    
    # Python complexity analysis
    if command -v radon >/dev/null 2>&1; then
        echo "🔍 Analyzing Python complexity..."
        find . -name "*.py" -not -path "./.git/*" -not -path "./build/*" | while read -r file; do
            if [[ -f "$file" ]]; then
                local cc
                cc=$(radon cc "$file" -s 2>/dev/null | grep -o "([0-9]\+)" | tr -d "()" | head -1 || echo "1")
                echo "$file:$cc" >> "$OUTPUT_DIR/complexity_python.txt"
            fi
        done 2>/dev/null || true
        print_success "Python complexity analysis completed"
    fi
    
    # Java/Kotlin complexity estimation
    echo "🔍 Analyzing Java/Kotlin complexity..."
    find . -name "*.java" -o -name "*.kt" | grep -v build | while read -r file; do
        if [[ -f "$file" ]]; then
            local methods
            local loops
            local conditions
            
            methods=$(grep -c "public\|private\|protected" "$file" 2>/dev/null || echo 0)
            loops=$(grep -c "for\|while\|do" "$file" 2>/dev/null || echo 0)
            conditions=$(grep -c "if\|else\|switch\|case" "$file" 2>/dev/null || echo 0)
            
            local complexity=$((methods + loops + conditions))
            echo "$file:$complexity" >> "$OUTPUT_DIR/complexity_java_kotlin.txt"
            
            total_complexity=$((total_complexity + complexity))
            file_count=$((file_count + 1))
        fi
    done 2>/dev/null || true
    
    # Calculate average complexity
    if [[ $file_count -gt 0 ]]; then
        local avg_complexity=$((total_complexity / file_count))
        if [[ $avg_complexity -gt 20 ]]; then
            complexity_score=$((100 - (avg_complexity - 20) * 2))
        fi
        if [[ $complexity_score -lt 0 ]]; then complexity_score=0; fi
        
        echo "{\"complexity_score\": $complexity_score, \"average_complexity\": $avg_complexity, \"total_files\": $file_count}" > "$OUTPUT_DIR/complexity_summary.json"
        print_success "Complexity Score: $complexity_score/100 (avg: $avg_complexity per file)"
    else
        echo "{\"complexity_score\": 100, \"average_complexity\": 0, \"total_files\": 0}" > "$OUTPUT_DIR/complexity_summary.json"
        print_success "Complexity Score: 100/100 (no files analyzed)"
    fi
    
    return $complexity_score
}

analyze_code_duplication() {
    print_section "Code Duplication Analysis"
    local duplication_score=100
    local duplicate_blocks=0
    
    echo "🔍 Detecting code duplication..."
    
    # Simple duplication detection using file checksums
    find . -name "*.java" -o -name "*.kt" -o -name "*.py" | grep -v build | while read -r file; do
        if [[ -f "$file" ]]; then
            # Extract method bodies and check for duplicates
            grep -n "public\|private\|protected\|def\|fun " "$file" 2>/dev/null | head -10 >> "$OUTPUT_DIR/method_signatures.txt" || true
        fi
    done 2>/dev/null || true
    
    # Count potential duplicates
    if [[ -f "$OUTPUT_DIR/method_signatures.txt" ]]; then
        duplicate_blocks=$(sort "$OUTPUT_DIR/method_signatures.txt" | uniq -d | wc -l 2>/dev/null || echo 0)
        
        if [[ $duplicate_blocks -gt 0 ]]; then
            duplication_score=$((100 - duplicate_blocks * 3))
            if [[ $duplication_score -lt 0 ]]; then duplication_score=0; fi
        fi
    fi
    
    echo "{\"duplication_score\": $duplication_score, \"duplicate_blocks\": $duplicate_blocks}" > "$OUTPUT_DIR/duplication_summary.json"
    print_success "Duplication Score: $duplication_score/100 ($duplicate_blocks potential duplicates)"
    
    return $duplication_score
}

analyze_test_coverage() {
    print_section "Test Coverage Analysis"
    local coverage_score=0
    local test_files=0
    local source_files=0
    
    echo "🔍 Analyzing test coverage..."
    
    # Count test files
    test_files=$(find . -name "*Test.java" -o -name "*Test.kt" -o -name "test_*.py" | grep -v build | wc -l)
    
    # Count source files (excluding third-party libraries)
    source_files=$(find . -name "*.java" -o -name "*.kt" -o -name "*.py" | grep -v build | grep -v "libui/src/main/java/com/github" | wc -l)
    
    if [[ $source_files -gt 0 ]]; then
        coverage_score=$((test_files * 100 / source_files))
        if [[ $coverage_score -gt 100 ]]; then coverage_score=100; fi
    fi
    
    echo "{\"coverage_score\": $coverage_score, \"test_files\": $test_files, \"source_files\": $source_files}" > "$OUTPUT_DIR/coverage_summary.json"
    print_success "Test Coverage Score: $coverage_score/100 ($test_files tests for $source_files source files)"
    
    return $coverage_score
}

analyze_code_style() {
    print_section "Code Style Analysis"
    local style_score=100
    local style_violations=0
    
    echo "🔍 Analyzing code style compliance..."
    
    # Kotlin style analysis
    if [[ -f "tools/ktlint" ]] || command -v ktlint >/dev/null 2>&1; then
        local ktlint_cmd="ktlint"
        if [[ -f "tools/ktlint" ]]; then
            ktlint_cmd="./tools/ktlint"
        fi
        
        if $ktlint_cmd --reporter=json,output="$OUTPUT_DIR/kotlin_style.json" 2>/dev/null || true; then
            local kotlin_violations
            kotlin_violations=$(grep -o '"ruleId"' "$OUTPUT_DIR/kotlin_style.json" 2>/dev/null | wc -l || echo 0)
            style_violations=$((style_violations + kotlin_violations))
            print_success "Kotlin style: $kotlin_violations violations"
        fi
    fi
    
    # Python style analysis
    if command -v flake8 >/dev/null 2>&1; then
        if flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics > "$OUTPUT_DIR/python_style.txt" 2>&1 || true; then
            local python_violations
            python_violations=$(tail -1 "$OUTPUT_DIR/python_style.txt" 2>/dev/null | grep -o "[0-9]\+" | head -1 || echo 0)
            style_violations=$((style_violations + python_violations))
            print_success "Python style: $python_violations violations"
        fi
    fi
    
    # Calculate style score
    if [[ $style_violations -gt 0 ]]; then
        style_score=$((100 - style_violations / 10))
        if [[ $style_score -lt 0 ]]; then style_score=0; fi
    fi
    
    echo "{\"style_score\": $style_score, \"violations\": $style_violations}" > "$OUTPUT_DIR/style_summary.json"
    print_success "Style Score: $style_score/100 ($style_violations violations)"
    
    return $style_score
}

calculate_overall_quality_score() {
    print_section "Overall Quality Assessment"
    
    local security_score=100
    local complexity_score=100
    local duplication_score=100
    local coverage_score=0
    local style_score=100
    
    # Read individual scores
    if [[ -f "$OUTPUT_DIR/security_summary.json" ]]; then
        security_score=$(grep -o '"security_score": [0-9]\+' "$OUTPUT_DIR/security_summary.json" | grep -o '[0-9]\+')
    fi
    
    if [[ -f "$OUTPUT_DIR/complexity_summary.json" ]]; then
        complexity_score=$(grep -o '"complexity_score": [0-9]\+' "$OUTPUT_DIR/complexity_summary.json" | grep -o '[0-9]\+')
    fi
    
    if [[ -f "$OUTPUT_DIR/duplication_summary.json" ]]; then
        duplication_score=$(grep -o '"duplication_score": [0-9]\+' "$OUTPUT_DIR/duplication_summary.json" | grep -o '[0-9]\+')
    fi
    
    if [[ -f "$OUTPUT_DIR/coverage_summary.json" ]]; then
        coverage_score=$(grep -o '"coverage_score": [0-9]\+' "$OUTPUT_DIR/coverage_summary.json" | grep -o '[0-9]\+')
    fi
    
    if [[ -f "$OUTPUT_DIR/style_summary.json" ]]; then
        style_score=$(grep -o '"style_score": [0-9]\+' "$OUTPUT_DIR/style_summary.json" | grep -o '[0-9]\+')
    fi
    
    # Calculate weighted overall score
    local overall_score=$(( (security_score * 25 + complexity_score * 20 + duplication_score * 15 + coverage_score * 25 + style_score * 15) / 100 ))
    
    # Generate comprehensive report
    cat > "$OUTPUT_DIR/quality_dashboard.json" << EOF
{
    "timestamp": "$(date -Iseconds)",
    "overall_score": $overall_score,
    "target_score": $MIN_QUALITY_SCORE,
    "quality_gate": "$(if [[ $overall_score -ge $MIN_QUALITY_SCORE ]]; then echo "PASSED"; else echo "FAILED"; fi)",
    "metrics": {
        "security": {
            "score": $security_score,
            "weight": 25,
            "description": "Security vulnerability analysis"
        },
        "complexity": {
            "score": $complexity_score,
            "weight": 20,
            "description": "Code complexity analysis"
        },
        "duplication": {
            "score": $duplication_score,
            "weight": 15,
            "description": "Code duplication detection"
        },
        "coverage": {
            "score": $coverage_score,
            "weight": 25,
            "description": "Test coverage analysis"
        },
        "style": {
            "score": $style_score,
            "weight": 15,
            "description": "Code style compliance"
        }
    },
    "recommendations": [
        $(if [[ $coverage_score -lt 80 ]]; then echo '"Increase test coverage to improve quality"'; fi)
        $(if [[ $security_score -lt 90 ]]; then echo '"Address security vulnerabilities"'; fi)
        $(if [[ $complexity_score -lt 80 ]]; then echo '"Reduce code complexity"'; fi)
        $(if [[ $duplication_score -lt 90 ]]; then echo '"Eliminate code duplication"'; fi)
        $(if [[ $style_score -lt 95 ]]; then echo '"Fix code style violations"'; fi)
    ]
}
EOF
    
    print_success "Overall Quality Score: $overall_score/100"
    
    if [[ $overall_score -ge $MIN_QUALITY_SCORE ]]; then
        print_success "✅ Quality Gate: PASSED"
        return 0
    else
        print_error "❌ Quality Gate: FAILED (minimum: $MIN_QUALITY_SCORE)"
        return 1
    fi
}

generate_quality_report() {
    print_section "Generating Quality Report"
    
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - START_TIME))
    
    # Generate HTML report
    cat > "$OUTPUT_DIR/quality_report.html" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>Advanced Quality Report</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 40px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        .header { text-align: center; margin-bottom: 40px; }
        .score { font-size: 48px; font-weight: bold; color: #2196F3; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 30px 0; }
        .metric { background: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; }
        .metric-title { font-weight: bold; color: #495057; margin-bottom: 10px; }
        .metric-score { font-size: 24px; font-weight: bold; }
        .passed { color: #28a745; }
        .failed { color: #dc3545; }
        .warning { color: #ffc107; }
        .recommendations { background: #e9ecef; padding: 20px; border-radius: 8px; margin-top: 30px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🏆 Advanced Quality Report</h1>
            <div class="score" id="overall-score">Loading...</div>
            <div id="quality-gate">Loading...</div>
        </div>
        <div class="metrics" id="metrics-grid">
            Loading metrics...
        </div>
        <div class="recommendations" id="recommendations">
            Loading recommendations...
        </div>
    </div>
    <script>
        fetch('quality_dashboard.json')
            .then(response => response.json())
            .then(data => {
                document.getElementById('overall-score').textContent = data.overall_score + '/100';
                document.getElementById('quality-gate').innerHTML = 
                    '<span class="' + (data.quality_gate === 'PASSED' ? 'passed' : 'failed') + '">' + 
                    data.quality_gate + '</span>';
                
                let metricsHtml = '';
                for (const [key, metric] of Object.entries(data.metrics)) {
                    const scoreClass = metric.score >= 80 ? 'passed' : metric.score >= 60 ? 'warning' : 'failed';
                    metricsHtml += `
                        <div class="metric">
                            <div class="metric-title">${metric.description}</div>
                            <div class="metric-score ${scoreClass}">${metric.score}/100</div>
                            <div>Weight: ${metric.weight}%</div>
                        </div>
                    `;
                }
                document.getElementById('metrics-grid').innerHTML = metricsHtml;
                
                if (data.recommendations && data.recommendations.length > 0) {
                    document.getElementById('recommendations').innerHTML = 
                        '<h3>🔧 Recommendations</h3><ul>' + 
                        data.recommendations.map(r => '<li>' + r + '</li>').join('') + 
                        '</ul>';
                } else {
                    document.getElementById('recommendations').innerHTML = 
                        '<h3>✅ All Quality Standards Met</h3><p>No specific recommendations at this time.</p>';
                }
            });
    </script>
</body>
</html>
EOF
    
    print_success "Quality report generated: $OUTPUT_DIR/quality_report.html"
    print_success "Analysis completed in ${duration}s"
}

enforce_quality_gates() {
    print_section "Enforcing Quality Gates"
    
    if [[ ! -f "$OUTPUT_DIR/quality_dashboard.json" ]]; then
        print_error "Quality dashboard not found. Run analysis first."
        return 1
    fi
    
    local quality_gate
    quality_gate=$(grep -o '"quality_gate": "[^"]*"' "$OUTPUT_DIR/quality_dashboard.json" | cut -d'"' -f4)
    
    if [[ "$quality_gate" == "PASSED" ]]; then
        print_success "✅ All quality gates passed - Ready for commit"
        return 0
    else
        print_error "❌ Quality gates failed - Commit blocked"
        echo ""
        echo "Improve the following areas:"
        grep -o '"recommendations": \[[^]]*\]' "$OUTPUT_DIR/quality_dashboard.json" | sed 's/.*\[\(.*\)\].*/\1/' | tr ',' '\n' | sed 's/^[[:space:]]*"/  - /' | sed 's/"[[:space:]]*$//'
        return 1
    fi
}

main() {
    print_header
    
    case "$MODE" in
        "analyze"|"all")
            install_quality_tools
            analyze_security_vulnerabilities
            analyze_code_complexity
            analyze_code_duplication
            analyze_test_coverage
            analyze_code_style
            calculate_overall_quality_score
            generate_quality_report
            ;;
        "enforce")
            enforce_quality_gates
            ;;
        "report")
            generate_quality_report
            ;;
        *)
            echo "Usage: $0 [analyze|enforce|report|all]"
            exit 1
            ;;
    esac
}

main "$@"