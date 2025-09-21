#!/bin/bash
# Package Header Validation Script

echo "=== PACKAGE HEADER VALIDATION REPORT ==="
echo "Generated on: $(date)"
echo

# Check thermalunified module packages
echo "1. THERMALUNIFIED MODULE PACKAGES:"
echo "   Expected: com.mpdc4gsr.module.thermalunified.*"
find component/thermalunified -name "*.java" -o -name "*.kt" | while read file; do
    package=$(grep "^package " "$file" 2>/dev/null | head -1)
    if [[ "$package" == *"com.mpdc4gsr.module.thermalunified"* ]]; then
        echo "   ✓ $file: $package"
    elif [[ "$package" == *"com.shuyu"* || "$package" == *"com.infisense"* ]]; then
        echo "   ○ $file: $package (external library)"
    else
        echo "   ✗ $file: $package (INCORRECT)"
    fi
done | head -20
echo "   ... (showing first 20 files)"
echo

# Check for old thermal module references
echo "2. OLD THERMAL MODULE REFERENCES CHECK:"
old_thermal_count=$(find . -name "*.java" -o -name "*.kt" | xargs grep "^package.*com\.mpdc4gsr\.module\.thermal\.[^i]" 2>/dev/null | wc -l)
old_lite_count=$(find . -name "*.java" -o -name "*.kt" | xargs grep "^package.*com\.example\.thermal_lite" 2>/dev/null | wc -l)
echo "   Old thermal references: $old_thermal_count"
echo "   Old thermal-lite references: $old_lite_count"
if [[ $old_thermal_count -eq 0 && $old_lite_count -eq 0 ]]; then
    echo "   ✓ No old thermal package references found"
else
    echo "   ✗ Found old thermal package references"
fi
echo

# Check app module packages  
echo "3. APP MODULE PACKAGES:"
echo "   Expected: mpdc4gsr.*"
inconsistent_util=$(find app/src -name "*.java" -o -name "*.kt" | xargs grep "^package.*mpdc4gsr\.util[^s]" 2>/dev/null | wc -l)
echo "   Inconsistent util packages: $inconsistent_util"
if [[ $inconsistent_util -eq 0 ]]; then
    echo "   ✓ App package consistency verified"
else
    echo "   ✗ Found inconsistent util packages"
fi
echo

# Check other components
echo "4. OTHER COMPONENT MODULES:"
for component in gsr-recording user; do
    if [[ -d "component/$component" ]]; then
        echo "   $component module:"
        sample_package=$(find "component/$component" -name "*.java" -o -name "*.kt" | head -1 | xargs grep "^package " 2>/dev/null)
        echo "     Sample: $sample_package"
    fi
done
echo

# Summary statistics
echo "5. SUMMARY STATISTICS:"
total_files=$(find . -name "*.java" -o -name "*.kt" | wc -l)
thermalunified_files=$(find component/thermalunified -name "*.java" -o -name "*.kt" | wc -l)
app_files=$(find app/src -name "*.java" -o -name "*.kt" | wc -l)

echo "   Total Java/Kotlin files: $total_files"
echo "   Thermalunified module files: $thermalunified_files" 
echo "   App module files: $app_files"
echo

echo "=== VALIDATION COMPLETE ==="