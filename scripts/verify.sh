#!/bin/bash

# IRCamera Build and Migration Verification Script
# Consolidates build fixes verification and Compose migration verification

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse arguments
VERIFY_TYPE="${1:-all}"

usage() {
    echo "Usage: $0 [verify-type]"
    echo ""
    echo "Verify Types:"
    echo "  all              Verify everything (default)"
    echo "  build            Verify build fixes only"
    echo "  migration        Verify Compose migration only"
    echo "  dependencies     Verify dependencies only"
    echo ""
    exit 1
}

if [ "$VERIFY_TYPE" = "-h" ] || [ "$VERIFY_TYPE" = "--help" ]; then
    usage
fi

# Function: Verify Build Fixes
verify_build_fixes() {
    echo -e "${BLUE}Verifying Critical Build Fixes...${NC}"
    echo "=================================="
    
    # 1. Check AndroidManifest.xml duplicates
    echo "1. Checking AndroidManifest.xml duplicates..."
    
    local DEVICE_PAIRING_COUNT=$(grep -c 'android:name="mpdc4gsr.network.DevicePairingComposeActivity"' app/src/main/AndroidManifest.xml || echo "0")
    local PERMISSION_COUNT=$(grep -c 'android:name="mpdc4gsr.permissions.PermissionRequestComposeActivity"' app/src/main/AndroidManifest.xml || echo "0")
    local SETTINGS_COUNT=$(grep -c 'android:name="mpdc4gsr.activities.SettingsComposeActivity"' app/src/main/AndroidManifest.xml || echo "0")
    
    echo "   DevicePairingComposeActivity: $DEVICE_PAIRING_COUNT declarations"
    echo "   PermissionRequestComposeActivity: $PERMISSION_COUNT declarations"
    echo "   SettingsComposeActivity: $SETTINGS_COUNT declarations"
    
    local MANIFEST_OK=true
    if [ "$DEVICE_PAIRING_COUNT" -eq 1 ] && [ "$PERMISSION_COUNT" -eq 1 ] && [ "$SETTINGS_COUNT" -eq 1 ]; then
        echo -e "   ${GREEN}AndroidManifest.xml - No duplicates found${NC}"
    else
        echo -e "   ${RED}AndroidManifest.xml - Duplicate activity declarations found${NC}"
        MANIFEST_OK=false
    fi
    
    # 2. Test gradle clean
    echo ""
    echo "2. Testing gradle clean..."
    if ./gradlew clean --no-daemon -q > /dev/null 2>&1; then
        echo -e "   ${GREEN}Gradle clean successful${NC}"
        local CLEAN_OK=true
    else
        echo -e "   ${RED}Gradle clean failed${NC}"
        local CLEAN_OK=false
    fi
    
    # 3. Test manifest processing
    echo ""
    echo "3. Testing manifest processing..."
    if timeout 30 ./gradlew processDebugMainManifest --no-daemon -q > manifest_test.log 2>&1; then
        echo -e "   ${GREEN}AndroidManifest processing successful${NC}"
        local MANIFEST_PROCESS_OK=true
    else
        echo -e "   ${RED}AndroidManifest processing failed or timed out${NC}"
        echo "   Check manifest_test.log for details"
        local MANIFEST_PROCESS_OK=false
    fi
    
    # Summary
    if [ "$MANIFEST_OK" = true ] && [ "$CLEAN_OK" = true ] && [ "$MANIFEST_PROCESS_OK" = true ]; then
        echo -e "${GREEN}Build fixes verified successfully${NC}"
        return 0
    else
        echo -e "${RED}Some build issues remain${NC}"
        return 1
    fi
}

# Function: Verify Dependencies
verify_dependencies() {
    echo -e "${BLUE}Verifying Compose Dependencies...${NC}"
    echo "=================================="
    
    # Check Compose BOM
    if grep -q "compose-bom.*2025" gradle/libs.versions.toml; then
        echo -e "   ${GREEN}Compose BOM found (2025.01.01)${NC}"
        local BOM_OK=true
    else
        echo -e "   ${YELLOW}Compose BOM missing or wrong version${NC}"
        local BOM_OK=false
    fi
    
    # Check Material 3
    if grep -q "compose-material3" gradle/libs.versions.toml; then
        echo -e "   ${GREEN}Material 3 dependency found${NC}"
        local MATERIAL3_OK=true
    else
        echo -e "   ${YELLOW}Material 3 dependency missing${NC}"
        local MATERIAL3_OK=false
    fi
    
    # Check bundle configuration
    if grep -q "compose-core.*compose-material3" gradle/libs.versions.toml; then
        echo -e "   ${GREEN}Compose bundles properly configured${NC}"
        local BUNDLES_OK=true
    else
        echo -e "   ${YELLOW}Compose bundles not properly configured${NC}"
        local BUNDLES_OK=false
    fi
    
    if [ "$BOM_OK" = true ] && [ "$MATERIAL3_OK" = true ] && [ "$BUNDLES_OK" = true ]; then
        echo -e "${GREEN}Dependencies verified successfully${NC}"
        return 0
    else
        echo -e "${YELLOW}Some dependency issues detected${NC}"
        return 1
    fi
}

# Function: Verify Compose Migration
verify_compose_migration() {
    echo -e "${BLUE}Verifying Compose Migration...${NC}"
    echo "=================================="
    
    # 1. Check build status
    echo "1. Checking Gradle build status..."
    
    echo "  - Cleaning previous build..."
    ./gradlew clean --no-daemon -q > /dev/null 2>&1
    
    echo "  - Attempting Kotlin compilation..."
    if ./gradlew compileDebugKotlin --no-daemon -q --stacktrace > build_output.log 2>&1; then
        echo -e "   ${GREEN}Kotlin compilation successful${NC}"
        local KOTLIN_OK=true
    else
        echo -e "   ${RED}Kotlin compilation failed${NC}"
        echo "   Error details in build_output.log"
        local KOTLIN_OK=false
    fi
    
    # 2. Analyze Compose activities
    echo ""
    echo "2. Analyzing Compose activities..."
    
    local COMPOSE_COUNT=$(find . -name "*Compose*.kt" | grep -i activity | wc -l)
    local TOTAL_ACTIVITIES=$(find . -name "*Activity*.kt" | wc -l)
    
    if [ "$TOTAL_ACTIVITIES" -gt 0 ]; then
        local COMPOSE_PERCENTAGE=$((COMPOSE_COUNT * 100 / TOTAL_ACTIVITIES))
        echo "   Compose Activities: $COMPOSE_COUNT / $TOTAL_ACTIVITIES ($COMPOSE_PERCENTAGE%)"
    else
        echo "   Compose Activities: $COMPOSE_COUNT / $TOTAL_ACTIVITIES"
    fi
    
    # 3. Check base classes
    if [ -f "app/src/main/java/mpdc4gsr/compose/base/BaseComposeActivity.kt" ]; then
        echo -e "   ${GREEN}BaseComposeActivity found${NC}"
    else
        echo -e "   ${YELLOW}BaseComposeActivity missing${NC}"
    fi
    
    if [ -f "libunified/src/main/java/com/mpdc4gsr/libunified/app/ktbase/BaseViewModel.kt" ]; then
        echo -e "   ${GREEN}BaseViewModel found${NC}"
    else
        echo -e "   ${YELLOW}BaseViewModel missing${NC}"
    fi
    
    # Summary
    if [ "$KOTLIN_OK" = true ]; then
        echo -e "${GREEN}Compose migration verified successfully${NC}"
        return 0
    else
        echo -e "${RED}Compose migration has issues${NC}"
        return 1
    fi
}

# Main execution
main() {
    echo "=========================================="
    echo "  IRCamera Verification Suite"
    echo "=========================================="
    echo ""
    
    local BUILD_OK=0
    local DEPS_OK=0
    local MIGRATION_OK=0
    
    case "$VERIFY_TYPE" in
        all)
            verify_build_fixes && BUILD_OK=1 || BUILD_OK=0
            echo ""
            verify_dependencies && DEPS_OK=1 || DEPS_OK=0
            echo ""
            verify_compose_migration && MIGRATION_OK=1 || MIGRATION_OK=0
            ;;
        build)
            verify_build_fixes && BUILD_OK=1 || BUILD_OK=0
            ;;
        dependencies)
            verify_dependencies && DEPS_OK=1 || DEPS_OK=0
            ;;
        migration)
            verify_compose_migration && MIGRATION_OK=1 || MIGRATION_OK=0
            ;;
        *)
            echo -e "${RED}Unknown verify type: $VERIFY_TYPE${NC}"
            usage
            ;;
    esac
    
    echo ""
    echo "=========================================="
    echo "  Verification Summary"
    echo "=========================================="
    
    if [ "$VERIFY_TYPE" = "all" ]; then
        [ $BUILD_OK -eq 1 ] && echo -e "${GREEN}Build Fixes: PASSED${NC}" || echo -e "${RED}Build Fixes: FAILED${NC}"
        [ $DEPS_OK -eq 1 ] && echo -e "${GREEN}Dependencies: PASSED${NC}" || echo -e "${YELLOW}Dependencies: WARNINGS${NC}"
        [ $MIGRATION_OK -eq 1 ] && echo -e "${GREEN}Migration: PASSED${NC}" || echo -e "${RED}Migration: FAILED${NC}"
        
        if [ $BUILD_OK -eq 1 ] && [ $MIGRATION_OK -eq 1 ]; then
            echo ""
            echo -e "${GREEN}Overall: VERIFICATION PASSED${NC}"
            exit 0
        else
            echo ""
            echo -e "${RED}Overall: VERIFICATION FAILED${NC}"
            exit 1
        fi
    else
        echo -e "${GREEN}Verification completed${NC}"
    fi
}

# Run main
main
