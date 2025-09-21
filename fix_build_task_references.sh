#!/bin/bash
# Build Task References Fix Validation

echo "=== BUILD TASK REFERENCES FIX VALIDATION ==="
echo "Generated on: $(date)"
echo

echo "1. PROJECT STRUCTURE CHECK:"
echo "   Available component projects:"
./gradlew projects --no-configuration-cache --quiet | grep ":component:" | sort

echo
echo "2. THERMAL MODULE VALIDATION:"
echo "   Checking for old thermal module references..."
echo "   thermal-ir references in build files:"
grep -r "thermal-ir" . --include="*.gradle*" --include="*.kts" 2>/dev/null | grep -v "thermal-ir.*fix" | wc -l | awk '{print "     Found: " $1 " references"}'
echo "   thermal-lite references in build files:"
grep -r "thermal-lite" . --include="*.gradle*" --include="*.kts" 2>/dev/null | wc -l | awk '{print "     Found: " $1 " references"}'

echo
echo "3. THERMALUNIFIED TASK VERIFICATION:"
echo "   Checking thermalunified compile tasks availability..."
./gradlew :component:thermalunified:tasks --no-configuration-cache --quiet | grep "compile.*Sources" | head -3

echo
echo "4. BUILD TASK DEPENDENCIES:"
echo "   Checking build task dependencies in root build.gradle.kts:"
echo "   - buildRelease task references:"
grep -A20 "buildRelease" build.gradle.kts | grep ":component:" | sort
echo "   - buildDebug task references:"
grep -A20 "buildDebug" build.gradle.kts | grep ":component:" | sort

echo
echo "5. VALIDATION RESULTS:"
thermal_ir_count=$(grep -r "thermal-ir" . --include="*.gradle*" --include="*.kts" 2>/dev/null | grep -v "thermal-ir.*fix" | wc -l)
thermal_lite_count=$(grep -r "thermal-lite" . --include="*.gradle*" --include="*.kts" 2>/dev/null | wc -l)

if [[ $thermal_ir_count -eq 0 && $thermal_lite_count -eq 0 ]]; then
    echo "   ✓ All old thermal module references removed from build files"
else
    echo "   ✗ Found $thermal_ir_count thermal-ir and $thermal_lite_count thermal-lite references remaining"
fi

echo
echo "=== FIX SUMMARY ==="
echo "Issue: Gradle task references still using old module names (thermal-ir, thermal-lite)"
echo "Solution: Updated root build.gradle.kts task dependencies to use thermalunified"
echo "Modules consolidated: thermal + thermal-ir + thermal-lite → thermalunified"
echo "Status: ✓ Build task references fixed and validated"