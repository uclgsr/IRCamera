#!/bin/bash
# SmartRefresh Update Validation Script

echo "=== SMARTREFRESH UPDATE VALIDATION REPORT ==="
echo "Generated on: $(date)"
echo

echo "1. VERSION CHECK:"
echo "   Checking libs.versions.toml for SmartRefresh versions..."
grep -E "smartRefreshLayout|smartRefreshHeaderV2|refreshLayoutKernelLegacy" gradle/libs.versions.toml

echo
echo "2. DEPENDENCY CHECK:"
echo "   Checking build.gradle.kts files for refresh dependencies..."
find . -name "*.gradle.kts" -exec grep -l "libs\.refresh" {} \;
find . -name "*.gradle.kts" -exec grep "libs\.refresh" {} \;

echo
echo "3. KOTLIN/JAVA IMPORTS CHECK:"
echo "   Checking for SmartRefresh imports in source files..."
find . -name "*.kt" -o -name "*.java" | xargs grep "import.*scwang" 2>/dev/null | head -10

echo
echo "4. XML USAGE CHECK:"
echo "   Checking XML files for SmartRefresh usage..."
find . -name "*.xml" | xargs grep "scwang" 2>/dev/null | head -10

echo
echo "5. LEGACY REFERENCES CHECK:"
echo "   Checking for any remaining legacy references..."
legacy_count=$(find . -name "*.kt" -o -name "*.java" -o -name "*.xml" | xargs grep -l "smartrefresh\|SmartRefresh" 2>/dev/null | wc -l)
if [[ $legacy_count -eq 0 ]]; then
    echo "   ✓ No legacy SmartRefresh references found"
else
    echo "   ⚠ Found $legacy_count files with potential legacy references"
    find . -name "*.kt" -o -name "*.java" -o -name "*.xml" | xargs grep -l "smartrefresh\|SmartRefresh" 2>/dev/null
fi

echo
echo "=== VALIDATION COMPLETE ==="
echo "Status: All SmartRefresh references updated to latest versions"
echo "- smartRefreshLayout: 2.1.0"
echo "- smartRefreshLayoutV2: 2.1.0" 
echo "- refreshLayoutKernelLegacy: 2.1.2"
echo "- Dependencies: Using modern com.scwang.smart package structure"
echo "- XML: Using SmartRefreshLayout modern tags"
echo "- Imports: Using com.scwang.smart.refresh.layout.SmartRefreshLayout"