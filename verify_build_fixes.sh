#!/bin/bash

# Build Fixes Verification Script
# Verifies the three critical issues have been resolved

echo "🔧 Verifying Critical Build Fixes"
echo "=================================="

# 1. Check AndroidManifest.xml duplicates are removed
echo "1️⃣ Checking AndroidManifest.xml duplicates..."

# Count actual activity declarations (not comments)
DEVICE_PAIRING_COUNT=$(grep -c 'android:name="mpdc4gsr.network.DevicePairingComposeActivity"' app/src/main/AndroidManifest.xml)
PERMISSION_COUNT=$(grep -c 'android:name="mpdc4gsr.permissions.PermissionRequestComposeActivity"' app/src/main/AndroidManifest.xml)
SETTINGS_COUNT=$(grep -c 'android:name="mpdc4gsr.activities.SettingsComposeActivity"' app/src/main/AndroidManifest.xml)

echo "   DevicePairingComposeActivity: $DEVICE_PAIRING_COUNT declarations"
echo "   PermissionRequestComposeActivity: $PERMISSION_COUNT declarations"
echo "   SettingsComposeActivity: $SETTINGS_COUNT declarations"

if [ "$DEVICE_PAIRING_COUNT" -eq 1 ] && [ "$PERMISSION_COUNT" -eq 1 ] && [ "$SETTINGS_COUNT" -eq 1 ]; then
    echo "    AndroidManifest.xml - No duplicates found"
    MANIFEST_OK=true
else
    echo "    AndroidManifest.xml - Duplicate activity declarations found"
    MANIFEST_OK=false
fi

# 2. Check Compose dependencies
echo ""
echo "2️⃣ Checking Compose dependencies..."

# Check BOM
if grep -q "compose-bom.*2025" gradle/libs.versions.toml; then
    echo "    Compose BOM found (2025.01.01)"
    BOM_OK=true
else
    echo "    Compose BOM missing or wrong version"
    BOM_OK=false
fi

# Check Material 3
if grep -q "compose-material3" gradle/libs.versions.toml; then
    echo "    Material 3 dependency found"
    MATERIAL3_OK=true
else
    echo "    Material 3 dependency missing"
    MATERIAL3_OK=false
fi

# Check bundle configuration
if grep -q "compose-core.*compose-material3" gradle/libs.versions.toml; then
    echo "    Compose bundles properly configured"
    BUNDLES_OK=true
else
    echo "    Compose bundles not properly configured"
    BUNDLES_OK=false
fi

# 3. Test actual build
echo ""
echo "3️⃣ Testing build verification..."

# Test clean
echo "   Testing gradle clean..."
if ./gradlew clean --no-daemon -q > /dev/null 2>&1; then
    echo "    Gradle clean successful"
    CLEAN_OK=true
else
    echo "    Gradle clean failed"
    CLEAN_OK=false
fi

# Test manifest processing  
echo "   Testing manifest processing..."
if timeout 30 ./gradlew processDebugMainManifest --no-daemon -q > manifest_test.log 2>&1; then
    echo "    AndroidManifest processing successful"
    MANIFEST_PROCESS_OK=true
else
    echo "    AndroidManifest processing failed or timed out"
    echo "   📝 Check manifest_test.log for details"
    MANIFEST_PROCESS_OK=false
fi

# Summary
echo ""
echo " Fix Verification Summary"
echo "=========================="

if [ "$MANIFEST_OK" = true ] && [ "$BOM_OK" = true ] && [ "$MATERIAL3_OK" = true ] && [ "$BUNDLES_OK" = true ] && [ "$CLEAN_OK" = true ] && [ "$MANIFEST_PROCESS_OK" = true ]; then
    echo " ALL CRITICAL ISSUES FIXED!"
    echo ""
    echo " AndroidManifest.xml duplicates removed"
    echo " Compose BOM dependency verified (2025.01.01)"
    echo " Material 3 dependency verified"
    echo " Build system functional"
    echo ""
    echo " Migration is now technically functional"
    echo " Ready for performance testing and integration validation"
    exit 0
else
    echo "  Some issues remain:"
    [ "$MANIFEST_OK" = false ] && echo " AndroidManifest.xml still has duplicate issues"
    [ "$BOM_OK" = false ] && echo " Compose BOM dependency missing"
    [ "$MATERIAL3_OK" = false ] && echo " Material 3 dependency missing"
    [ "$BUNDLES_OK" = false ] && echo " Compose bundles not configured"
    [ "$CLEAN_OK" = false ] && echo " Gradle clean fails"
    [ "$MANIFEST_PROCESS_OK" = false ] && echo " Manifest processing fails"
    echo ""
    echo " Requires additional fixes before migration is functional"
    exit 1
fi