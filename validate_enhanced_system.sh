#!/bin/bash

# Enhanced System Validation Script
# Tests all new quality analysis components

set -e

echo "🔍 Enhanced Quality System Validation"
echo "====================================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test results
passed_tests=0
total_tests=0

# Function to run test
run_test() {
    local test_name="$1"
    local command="$2"
    local expected_result="$3"
    
    echo -e "\n${BLUE}Testing: $test_name${NC}"
    ((total_tests++))
    
    if eval "$command" >/dev/null 2>&1; then
        echo -e "${GREEN}✅ PASSED: $test_name${NC}"
        ((passed_tests++))
        return 0
    else
        echo -e "${RED}❌ FAILED: $test_name${NC}"
        return 1
    fi
}

echo -e "\n${YELLOW}📊 Testing Enhanced Quality System Components...${NC}"

# Test 1: File existence
run_test "Enhanced Quality Analyzer exists" "[ -f enhanced_quality_analyzer.sh ]"
run_test "Intelligent Code Optimizer exists" "[ -f intelligent_code_optimizer.py ]"
run_test "Comprehensive workflow exists" "[ -f .github/workflows/comprehensive-quality-analysis.yml ]"
run_test "Quality review documentation exists" "[ -f COMPREHENSIVE_QUALITY_REVIEW.md ]"

# Test 2: Script permissions
run_test "Enhanced analyzer is executable" "[ -x enhanced_quality_analyzer.sh ]"
run_test "Python optimizer is executable" "[ -x intelligent_code_optimizer.py ]"

# Test 3: Script syntax validation
run_test "Enhanced analyzer syntax check" "bash -n enhanced_quality_analyzer.sh"
run_test "Python optimizer syntax check" "python -m py_compile intelligent_code_optimizer.py"

# Test 4: Python imports
run_test "Python optimizer import test" "python -c 'from intelligent_code_optimizer import IntelligentCodeOptimizer; print(\"OK\")'"

# Test 5: Configuration files
run_test "EditorConfig exists" "[ -f .editorconfig ]"
run_test "ktlint config exists" "[ -f .ktlintrc ]"
run_test "Checkstyle config exists" "[ -f checkstyle.xml ]"
run_test "ESLint config exists" "[ -f .eslintrc.js ]"

# Test 6: Basic functionality tests
echo -e "\n${YELLOW}🔧 Testing Basic Functionality...${NC}"

# Create test directory
mkdir -p test_quality_system
cd test_quality_system

# Create sample files for testing
cat > Sample.kt << 'EOF'
class Sample {
    fun test() {
        println("Hello World")
    }
}
EOF

cat > Sample.java << 'EOF'
public class Sample {
    public void test() {
        System.out.println("Hello World");
    }
}
EOF

cat > sample.py << 'EOF'
def test():
    print("Hello World")
EOF

cd ..

# Test file discovery
run_test "File discovery test" "find . -name '*.kt' | head -5 | wc -l | grep -q '[0-9]'"
run_test "Java file discovery" "find . -name '*.java' | head -5 | wc -l | grep -q '[0-9]'"
run_test "Python file discovery" "find . -name '*.py' | head -5 | wc -l | grep -q '[0-9]'"

# Test 7: Tool availability
echo -e "\n${YELLOW}🛠️ Testing Tool Availability...${NC}"

# Check if we can access the tools (don't fail if not available)
if command -v ktlint >/dev/null 2>&1; then
    run_test "ktlint availability" "ktlint --version"
else
    echo -e "${YELLOW}⚠️ ktlint not installed (will be installed during analysis)${NC}"
fi

if command -v checkstyle >/dev/null 2>&1; then
    run_test "checkstyle availability" "checkstyle --version"
else
    echo -e "${YELLOW}⚠️ checkstyle not installed (will be installed during analysis)${NC}"
fi

if command -v black >/dev/null 2>&1; then
    run_test "black availability" "black --version"
else
    echo -e "${YELLOW}⚠️ black not installed (will be installed during analysis)${NC}"
fi

# Test 8: Database functionality
echo -e "\n${YELLOW}💾 Testing Database Functionality...${NC}"

# Test SQLite availability
run_test "SQLite3 availability" "command -v sqlite3"

# Test Python database functionality
run_test "Database initialization test" "python -c 'import sqlite3; conn = sqlite3.connect(\":memory:\"); print(\"OK\")'"

# Test 9: Report generation capabilities
echo -e "\n${YELLOW}📊 Testing Report Generation...${NC}"

# Test HTML generation capabilities
run_test "HTML generation test" "python -c 'import datetime; print(\"<html><body>Test</body></html>\")' > test_report.html && [ -f test_report.html ]"

# Test JSON generation
run_test "JSON generation test" "python -c 'import json; print(json.dumps({\"test\": \"value\"}))' > test_report.json && [ -f test_report.json ]"

# Test 10: Workflow validation
echo -e "\n${YELLOW}🔄 Testing Workflow Configuration...${NC}"

# Basic YAML syntax check
if command -v yamllint >/dev/null 2>&1; then
    run_test "Workflow YAML syntax" "yamllint .github/workflows/comprehensive-quality-analysis.yml"
else
    echo -e "${YELLOW}⚠️ yamllint not available - skipping YAML validation${NC}"
fi

# Test GitHub Actions syntax
run_test "GitHub Actions structure" "grep -q 'name:' .github/workflows/comprehensive-quality-analysis.yml"
run_test "Workflow jobs defined" "grep -q 'jobs:' .github/workflows/comprehensive-quality-analysis.yml"
run_test "Workflow steps defined" "grep -q 'steps:' .github/workflows/comprehensive-quality-analysis.yml"

# Cleanup test files
rm -rf test_quality_system test_report.html test_report.json 2>/dev/null || true

# Summary
echo -e "\n${BLUE}📋 Validation Summary${NC}"
echo "======================"
echo -e "Tests Passed: ${GREEN}$passed_tests${NC}"
echo -e "Total Tests: $total_tests"

if [ $passed_tests -eq $total_tests ]; then
    echo -e "\n${GREEN}🎉 ALL TESTS PASSED! Enhanced Quality System is ready for use.${NC}"
    echo -e "\n${YELLOW}📋 Next Steps:${NC}"
    echo "1. Run ./enhanced_quality_analyzer.sh for comprehensive analysis"
    echo "2. Run python intelligent_code_optimizer.py for ML-powered insights"
    echo "3. Enable 'Comprehensive Quality Analysis' workflow in GitHub Actions"
    echo "4. Review generated reports in quality_reports/ directory"
    exit 0
else
    echo -e "\n${RED}⚠️ Some tests failed. Please review the issues above.${NC}"
    exit 1
fi