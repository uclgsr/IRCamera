#!/bin/bash

# Anti-Pattern Detection Script
# This script scans the codebase for common anti-patterns and generates a report

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_FILE="${PROJECT_ROOT}/antipattern_detection_report.txt"
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

echo "========================================" > "$REPORT_FILE"
echo "Anti-Pattern Detection Report" >> "$REPORT_FILE"
echo "Generated: $TIMESTAMP" >> "$REPORT_FILE"
echo "========================================" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

cd "$PROJECT_ROOT"

# Color codes for terminal output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Track total issues
CRITICAL_COUNT=0
HIGH_COUNT=0
MEDIUM_COUNT=0

echo -e "${YELLOW}Scanning for anti-patterns...${NC}"

# 1. GlobalScope Usage (CRITICAL)
echo "1. Checking for GlobalScope usage (CRITICAL)..." | tee -a "$REPORT_FILE"
GLOBALSCOPE_COUNT=$(grep -r "GlobalScope" app/src/main libunified/src/main --include="*.kt" 2>/dev/null | grep -v "// " | wc -l || echo "0")
if [ "$GLOBALSCOPE_COUNT" -gt 0 ]; then
    echo -e "${RED}   FOUND: $GLOBALSCOPE_COUNT instances of GlobalScope${NC}"
    echo "   Files:" | tee -a "$REPORT_FILE"
    grep -r "GlobalScope" app/src/main libunified/src/main --include="*.kt" 2>/dev/null | grep -v "// " | cut -d: -f1 | sort | uniq | sed 's/^/      /' | tee -a "$REPORT_FILE"
    CRITICAL_COUNT=$((CRITICAL_COUNT + GLOBALSCOPE_COUNT))
else
    echo -e "${GREEN}   OK: No GlobalScope usage found${NC}"
fi
echo "" | tee -a "$REPORT_FILE"

# 2. Generic Exception Catching (CRITICAL)
echo "2. Checking for generic exception catching (CRITICAL)..." | tee -a "$REPORT_FILE"
GENERIC_EXCEPTION_COUNT=$(grep -r "catch (e: Exception)" app/src/main libunified/src/main --include="*.kt" 2>/dev/null | wc -l || echo "0")
echo -e "${RED}   FOUND: $GENERIC_EXCEPTION_COUNT instances of catch (e: Exception)${NC}"
echo "   Total: $GENERIC_EXCEPTION_COUNT" >> "$REPORT_FILE"
if [ "$GENERIC_EXCEPTION_COUNT" -gt 100 ]; then
    echo "   Top 10 files with most occurrences:" | tee -a "$REPORT_FILE"
    grep -r "catch (e: Exception)" app/src/main libunified/src/main --include="*.kt" 2>/dev/null | cut -d: -f1 | sort | uniq -c | sort -rn | head -10 | sed 's/^/      /' | tee -a "$REPORT_FILE"
fi
CRITICAL_COUNT=$((CRITICAL_COUNT + GENERIC_EXCEPTION_COUNT))
echo "" | tee -a "$REPORT_FILE"

# 3. Context in ViewModels (CRITICAL)
echo "3. Checking for Context retention in ViewModels (CRITICAL)..." | tee -a "$REPORT_FILE"
CONTEXT_IN_VM_COUNT=$(find app/src/main/java -path "**/viewmodel/*.kt" -o -path "**/*ViewModel.kt" 2>/dev/null | xargs grep -l "private.*context.*Context\|lateinit.*context.*Context" 2>/dev/null | wc -l || echo "0")
if [ "$CONTEXT_IN_VM_COUNT" -gt 0 ]; then
    echo -e "${RED}   FOUND: $CONTEXT_IN_VM_COUNT ViewModels with Context${NC}"
    echo "   Files:" | tee -a "$REPORT_FILE"
    find app/src/main/java -path "**/viewmodel/*.kt" -o -path "**/*ViewModel.kt" 2>/dev/null | xargs grep -l "private.*context.*Context\|lateinit.*context.*Context" 2>/dev/null | sed 's/^/      /' | tee -a "$REPORT_FILE"
    CRITICAL_COUNT=$((CRITICAL_COUNT + CONTEXT_IN_VM_COUNT))
else
    echo -e "${GREEN}   OK: No Context in ViewModels${NC}"
fi
echo "" | tee -a "$REPORT_FILE"

# 4. Reflection Usage (HIGH)
echo "4. Checking for reflection usage (HIGH)..." | tee -a "$REPORT_FILE"
REFLECTION_COUNT=$(grep -r "Class.forName\|getDeclaredMethod\|getMethod\|setAccessible" app/src/main libunified/src/main BleModule/src/main --include="*.kt" --include="*.java" 2>/dev/null | wc -l || echo "0")
if [ "$REFLECTION_COUNT" -gt 10 ]; then
    echo -e "${RED}   FOUND: $REFLECTION_COUNT uses of reflection${NC}"
    echo "   Files:" | tee -a "$REPORT_FILE"
    grep -r "Class.forName\|getDeclaredMethod\|getMethod\|setAccessible" app/src/main libunified/src/main BleModule/src/main --include="*.kt" --include="*.java" 2>/dev/null | cut -d: -f1 | sort | uniq | sed 's/^/      /' | tee -a "$REPORT_FILE"
    HIGH_COUNT=$((HIGH_COUNT + REFLECTION_COUNT))
else
    echo -e "${GREEN}   OK: Minimal reflection usage${NC}"
fi
echo "" | tee -a "$REPORT_FILE"

# 5. God Classes (HIGH)
echo "5. Checking for God Classes (>1000 lines) (HIGH)..." | tee -a "$REPORT_FILE"
GOD_CLASSES=$(find app/src/main/java libunified/src/main/java -type f \( -name "*.kt" -o -name "*.java" \) 2>/dev/null | xargs wc -l 2>/dev/null | awk '$1 > 1000 {print}' | grep -v "total" | wc -l || echo "0")
if [ "$GOD_CLASSES" -gt 0 ]; then
    echo -e "${RED}   FOUND: $GOD_CLASSES classes with >1000 lines${NC}"
    echo "   Top God Classes:" | tee -a "$REPORT_FILE"
    find app/src/main/java libunified/src/main/java -type f \( -name "*.kt" -o -name "*.java" \) 2>/dev/null | xargs wc -l 2>/dev/null | sort -rn | head -15 | grep -v "total" | sed 's/^/      /' | tee -a "$REPORT_FILE"
    HIGH_COUNT=$((HIGH_COUNT + GOD_CLASSES))
else
    echo -e "${GREEN}   OK: No God Classes found${NC}"
fi
echo "" | tee -a "$REPORT_FILE"

# 6. MainActivity Variants (HIGH)
echo "6. Checking for MainActivity variants (HIGH)..." | tee -a "$REPORT_FILE"
MAIN_ACTIVITY_COUNT=$(find app/src/main/java -name "*MainActivity*.kt" 2>/dev/null | wc -l || echo "0")
if [ "$MAIN_ACTIVITY_COUNT" -gt 2 ]; then
    echo -e "${RED}   FOUND: $MAIN_ACTIVITY_COUNT MainActivity variants${NC}"
    echo "   Files:" | tee -a "$REPORT_FILE"
    find app/src/main/java -name "*MainActivity*.kt" 2>/dev/null | sed 's/^/      /' | tee -a "$REPORT_FILE"
    HIGH_COUNT=$((HIGH_COUNT + MAIN_ACTIVITY_COUNT - 2))
else
    echo -e "${GREEN}   OK: Acceptable number of MainActivity variants${NC}"
fi
echo "" | tee -a "$REPORT_FILE"

# 7. Test Activities in Production (MEDIUM)
echo "7. Checking for test activities in production source (MEDIUM)..." | tee -a "$REPORT_FILE"
TEST_ACTIVITIES=$(find app/src/main/java -name "*Test*Activity.kt" 2>/dev/null | wc -l || echo "0")
if [ "$TEST_ACTIVITIES" -gt 0 ]; then
    echo -e "${YELLOW}   FOUND: $TEST_ACTIVITIES test activities in main source${NC}"
    echo "   Files:" | tee -a "$REPORT_FILE"
    find app/src/main/java -name "*Test*Activity.kt" 2>/dev/null | sed 's/^/      /' | tee -a "$REPORT_FILE"
    MEDIUM_COUNT=$((MEDIUM_COUNT + TEST_ACTIVITIES))
else
    echo -e "${GREEN}   OK: No test activities in production${NC}"
fi
echo "" | tee -a "$REPORT_FILE"

# 8. Mutable Static State (MEDIUM)
echo "8. Checking for mutable static state (MEDIUM)..." | tee -a "$REPORT_FILE"
MUTABLE_STATIC_COUNT=$(grep -r "companion object" app/src/main/java --include="*.kt" -A 5 2>/dev/null | grep -E "var |mutableListOf|mutableMapOf" | wc -l || echo "0")
if [ "$MUTABLE_STATIC_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}   FOUND: $MUTABLE_STATIC_COUNT instances of mutable static state${NC}"
    echo "   Count: $MUTABLE_STATIC_COUNT" >> "$REPORT_FILE"
    MEDIUM_COUNT=$((MEDIUM_COUNT + MUTABLE_STATIC_COUNT))
else
    echo -e "${GREEN}   OK: No mutable static state found${NC}"
fi
echo "" | tee -a "$REPORT_FILE"

# 9. TODO/FIXME Comments (MEDIUM)
echo "9. Checking for TODO/FIXME comments (MEDIUM)..." | tee -a "$REPORT_FILE"
TODO_COUNT=$(grep -r "TODO\|FIXME\|HACK\|XXX" app/src/main/java --include="*.kt" 2>/dev/null | wc -l || echo "0")
if [ "$TODO_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}   FOUND: $TODO_COUNT TODO/FIXME comments${NC}"
    echo "   Count: $TODO_COUNT" >> "$REPORT_FILE"
    MEDIUM_COUNT=$((MEDIUM_COUNT + TODO_COUNT))
else
    echo -e "${GREEN}   OK: No TODO/FIXME comments${NC}"
fi
echo "" | tee -a "$REPORT_FILE"

# 10. Object Singletons (MEDIUM)
echo "10. Checking for object singletons (MEDIUM)..." | tee -a "$REPORT_FILE"
SINGLETON_COUNT=$(grep -r "^object " app/src/main/java libunified/src/main/java --include="*.kt" 2>/dev/null | grep -v "companion object" | wc -l || echo "0")
if [ "$SINGLETON_COUNT" -gt 5 ]; then
    echo -e "${YELLOW}   FOUND: $SINGLETON_COUNT object singletons${NC}"
    echo "   Files:" | tee -a "$REPORT_FILE"
    grep -r "^object " app/src/main/java libunified/src/main/java --include="*.kt" 2>/dev/null | grep -v "companion object" | cut -d: -f1 | sort | uniq | sed 's/^/      /' | head -10 | tee -a "$REPORT_FILE"
    MEDIUM_COUNT=$((MEDIUM_COUNT + SINGLETON_COUNT - 5))
else
    echo -e "${GREEN}   OK: Acceptable number of singletons${NC}"
fi
echo "" | tee -a "$REPORT_FILE"

# Summary
echo "========================================" | tee -a "$REPORT_FILE"
echo "SUMMARY" | tee -a "$REPORT_FILE"
echo "========================================" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

TOTAL_ISSUES=$((CRITICAL_COUNT + HIGH_COUNT + MEDIUM_COUNT))

echo "Critical Issues: $CRITICAL_COUNT" | tee -a "$REPORT_FILE"
echo "High Priority Issues: $HIGH_COUNT" | tee -a "$REPORT_FILE"
echo "Medium Priority Issues: $MEDIUM_COUNT" | tee -a "$REPORT_FILE"
echo "Total Issues: $TOTAL_ISSUES" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# Color-coded summary
if [ "$CRITICAL_COUNT" -gt 0 ]; then
    echo -e "${RED}STATUS: CRITICAL - Immediate action required${NC}"
    echo "STATUS: CRITICAL - Immediate action required" >> "$REPORT_FILE"
elif [ "$HIGH_COUNT" -gt 50 ]; then
    echo -e "${RED}STATUS: HIGH PRIORITY - Action required soon${NC}"
    echo "STATUS: HIGH PRIORITY - Action required soon" >> "$REPORT_FILE"
elif [ "$MEDIUM_COUNT" -gt 20 ]; then
    echo -e "${YELLOW}STATUS: MEDIUM PRIORITY - Plan refactoring${NC}"
    echo "STATUS: MEDIUM PRIORITY - Plan refactoring" >> "$REPORT_FILE"
else
    echo -e "${GREEN}STATUS: GOOD - Minor issues only${NC}"
    echo "STATUS: GOOD - Minor issues only" >> "$REPORT_FILE"
fi

echo "" | tee -a "$REPORT_FILE"
echo "Full report saved to: $REPORT_FILE"
echo ""

# Exit with error code if critical issues found
if [ "$CRITICAL_COUNT" -gt 0 ]; then
    exit 1
fi

exit 0
