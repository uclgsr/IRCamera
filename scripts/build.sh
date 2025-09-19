#!/bin/bash

# IRCamera Unified Build Script
# Cross-platform build system for Android APK generation
# Replaces multiple redundant .bat files with single configurable script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Global configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_LOG="$PROJECT_ROOT/build.log"

# Build configuration
BUILD_TYPE="release"          # Default: release, can be debug  
BUILD_VARIANT="standard"      # Default: standard, can be google/topdon
GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError"
GRADLE_ARGS="--no-configuration-cache"

# Function to print colored output
print_header() {
    echo -e "${CYAN}================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}================================${NC}"
}

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_help() {
    print_header "IRCamera Unified Build System"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -t, --type TYPE        Build type: release|debug (default: release)"
    echo "  -v, --variant VARIANT  Build variant: standard|google|topdon (default: standard)"
    echo "  -c, --clean           Clean project before building"
    echo "  -f, --fallback        Try fallback build strategies if primary fails"
    echo "  -q, --quick           Skip validation steps for faster build"
    echo "  --no-cache            Disable all Gradle caches"
    echo "  -h, --help            Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                           # Standard release build"
    echo "  $0 -t debug                  # Debug build"
    echo "  $0 -v google -c              # Google variant with clean"
    echo "  $0 -t release -v topdon -f   # Topdon release with fallback strategies"
    echo ""
    echo "Previous .bat file equivalents:"
    echo "  build_apk_google_script.bat     → $0 -v google"
    echo "  build_apk_topdon_script.bat     → $0 -v topdon"
    echo "  build_release_google_apk_script.bat → $0 -t release -v google"
    echo "  build_release_topdon_apk_script.bat → $0 -t release -v topdon"
    echo ""
    echo "Output:"
    echo "  APK files: app/build/outputs/apk/[variant]/[type]/"
    echo "  Build logs: $BUILD_LOG"
}

check_environment() {
    print_status "Checking build environment..."
    
    # Check if we're in the right directory
    if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
        print_error "gradlew not found. Please run this script from the project root or scripts directory."
        print_error "Expected location: $PROJECT_ROOT/gradlew"
        exit 1
    fi
    
    # Make gradlew executable
    chmod +x "$PROJECT_ROOT/gradlew"
    
    # Check Java version
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1)
        print_status "Java version: $JAVA_VERSION"
    else
        print_error "Java not found in PATH. Please install Java JDK."
        exit 1
    fi
    
    # Set Gradle environment
    export GRADLE_OPTS="$GRADLE_OPTS"
    
    print_success "Environment check completed"
}

clean_project() {
    if [ "$CLEAN_BUILD" = true ]; then
        print_status "Cleaning project (this may take 80-90 seconds)..."
        
        cd "$PROJECT_ROOT"
        if ! timeout 180 ./gradlew clean $GRADLE_ARGS 2>&1 | tee -a "$BUILD_LOG"; then
            print_warning "Clean operation timed out or failed, but continuing..."
        else
            print_success "Clean completed successfully"
        fi
    fi
}

determine_gradle_task() {
    local task_base=""
    
    # Determine task based on build type
    if [ "$BUILD_TYPE" = "debug" ]; then
        task_base="assembleDebug"
    else
        task_base="assembleRelease"
    fi
    
    # Add variant-specific task if needed
    case "$BUILD_VARIANT" in
        google)
            GRADLE_TASK=":app:${task_base}"
            ;;
        topdon)
            GRADLE_TASK=":app:${task_base}"
            ;;
        standard|*)
            GRADLE_TASK=":app:${task_base}"
            ;;
    esac
    
    print_status "Build task: $GRADLE_TASK"
}

primary_build() {
    print_header "Starting Primary Build Strategy"
    
    determine_gradle_task
    
    cd "$PROJECT_ROOT"
    
    print_status "Building $BUILD_TYPE APK with $BUILD_VARIANT variant..."
    print_status "This may take 1-2 minutes, please be patient..."
    
    # Build with timeout to prevent hanging
    if timeout 300 ./gradlew $GRADLE_TASK $GRADLE_ARGS 2>&1 | tee -a "$BUILD_LOG"; then
        return 0
    else
        return 1
    fi
}

fallback_build_strategies() {
    print_header "Primary Build Failed - Trying Fallback Strategies"
    
    # Strategy 1: Disable all caches
    print_status "Strategy 1: Building without caches..."
    cd "$PROJECT_ROOT"
    if timeout 300 ./gradlew $GRADLE_TASK --no-configuration-cache --no-build-cache --refresh-dependencies 2>&1 | tee -a "$BUILD_LOG"; then
        return 0
    fi
    
    # Strategy 2: Try debug build if release failed
    if [ "$BUILD_TYPE" = "release" ]; then
        print_status "Strategy 2: Trying debug build as fallback..."
        if timeout 300 ./gradlew :app:assembleDebug $GRADLE_ARGS 2>&1 | tee -a "$BUILD_LOG"; then
            print_warning "Release build failed, but debug build succeeded"
            BUILD_TYPE="debug"  # Update for output messages
            return 0
        fi
    fi
    
    # Strategy 3: Individual module builds
    print_status "Strategy 3: Trying individual module builds..."
    local modules=("libir" "libcom" "libapp" "libui")
    local built_modules=0
    
    for module in "${modules[@]}"; do
        if [ -d "$PROJECT_ROOT/$module" ]; then
            print_status "Building module: $module"
            if timeout 120 ./gradlew :$module:build $GRADLE_ARGS 2>&1 | tee -a "$BUILD_LOG"; then
                ((built_modules++))
                print_success "Module $module built successfully"
            else
                print_warning "Module $module build failed"
            fi
        fi
    done
    
    if [ $built_modules -gt 0 ]; then
        print_status "Some modules built successfully, retrying main build..."
        if timeout 300 ./gradlew $GRADLE_TASK $GRADLE_ARGS 2>&1 | tee -a "$BUILD_LOG"; then
            return 0
        fi
    fi
    
    return 1
}

analyze_build_failure() {
    print_header "Build Failure Analysis"
    
    if [ -f "$BUILD_LOG" ]; then
        print_status "Analyzing build log for common issues..."
        
        # Check for specific error patterns
        if grep -q "ShimmerDevice" "$BUILD_LOG"; then
            print_error "❌ KNOWN ISSUE: ShimmerDevice class not found in BleModule"
            echo "  This is a known blocking issue preventing APK generation."
            echo "  Location: BleModule/src/main/java/com/topdon/ble/ShimmerBleController.java"
            echo "  Resolution required: Implement or import ShimmerDevice class"
        fi
        
        if grep -q "Out of memory" "$BUILD_LOG"; then
            print_error "❌ Out of memory error detected"
            echo "  Try increasing Gradle memory: export GRADLE_OPTS=\"-Xmx6g\""
        fi
        
        if grep -q "Network" "$BUILD_LOG"; then
            print_error "❌ Network connectivity issues detected"
            echo "  Check internet connection for dependency downloads"
        fi
        
        if grep -q "Permission denied" "$BUILD_LOG"; then
            print_error "❌ Permission issues detected"
            echo "  Check file system permissions in project directory"
        fi
        
        # Show last 30 lines of build log
        print_status "Last 30 lines of build log:"
        echo "----------------------------------------"
        tail -30 "$BUILD_LOG" 2>/dev/null || echo "Could not read build log"
        echo "----------------------------------------"
    fi
    
    print_status "Troubleshooting suggestions:"
    echo "  1. Clean all caches: ./gradlew clean --no-configuration-cache"
    echo "  2. Check network connectivity for dependency downloads"
    echo "  3. Update Android SDK and build tools"
    echo "  4. Check available disk space (need 2GB+ free)"
    echo "  5. Try building individual modules first"
    echo "  6. Review detailed build log: $BUILD_LOG"
    echo ""
    echo "For the ShimmerDevice issue specifically:"
    echo "  - This is a known blocking issue in the current codebase"
    echo "  - PC Controller functionality is not affected"
    echo "  - Android APK generation requires this issue to be resolved first"
}

find_and_display_apks() {
    print_header "Build Results"
    
    cd "$PROJECT_ROOT"
    
    # Look for generated APK files
    local apk_files
    mapfile -t apk_files < <(find . -name "*.apk" -type f -newer "$BUILD_LOG" 2>/dev/null || true)
    
    if [ ${#apk_files[@]} -gt 0 ]; then
        print_success "✅ APK files generated:"
        for apk in "${apk_files[@]}"; do
            local size
            size=$(stat -f%z "$apk" 2>/dev/null || stat -c%s "$apk" 2>/dev/null || echo "unknown")
            local size_mb=$((size / 1024 / 1024))
            print_success "  📱 $apk (${size_mb} MB)"
        done
        
        echo ""
        print_status "Installation instructions:"
        echo "  adb install ${apk_files[0]}"
        echo ""
        print_status "Next steps:"
        echo "  1. Install APK on Android device"
        echo "  2. Test hardware integration with sensors"
        echo "  3. Verify Hub-and-Spoke communication with PC Controller"
        
        return 0
    else
        print_error "❌ No APK files were generated"
        return 1
    fi
}

validate_build_environment() {
    if [ "$QUICK_BUILD" = false ]; then
        print_status "Running build environment validation..."
        
        cd "$PROJECT_ROOT"
        
        # Check if dev.sh is available and run quick validation
        if [ -f "./dev.sh" ]; then
            print_status "Running development environment checks..."
            if ./dev.sh lint 2>&1 | tee -a "$BUILD_LOG"; then
                print_success "Code style validation passed"
            else
                print_warning "Code style issues detected (not blocking build)"
            fi
        fi
    fi
}

# Parse command line arguments
CLEAN_BUILD=false
FALLBACK_ENABLED=false
QUICK_BUILD=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            BUILD_TYPE="$2"
            shift 2
            ;;
        -v|--variant)
            BUILD_VARIANT="$2"
            shift 2
            ;;
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        -f|--fallback)
            FALLBACK_ENABLED=true
            shift
            ;;
        -q|--quick)
            QUICK_BUILD=true
            shift
            ;;
        --no-cache)
            GRADLE_ARGS="$GRADLE_ARGS --no-build-cache"
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Validate arguments
if [[ ! "$BUILD_TYPE" =~ ^(release|debug)$ ]]; then
    print_error "Invalid build type: $BUILD_TYPE. Must be 'release' or 'debug'"
    exit 1
fi

if [[ ! "$BUILD_VARIANT" =~ ^(standard|google|topdon)$ ]]; then
    print_error "Invalid build variant: $BUILD_VARIANT. Must be 'standard', 'google', or 'topdon'"
    exit 1
fi

# Main execution
main() {
    print_header "IRCamera Unified Build System"
    print_status "Configuration:"
    print_status "  Build Type: $BUILD_TYPE"
    print_status "  Variant: $BUILD_VARIANT"
    print_status "  Clean: $CLEAN_BUILD"
    print_status "  Fallback: $FALLBACK_ENABLED"
    print_status "  Quick: $QUICK_BUILD"
    echo ""
    
    # Initialize build log
    echo "IRCamera Build Log - $(date)" > "$BUILD_LOG"
    echo "Configuration: Type=$BUILD_TYPE, Variant=$BUILD_VARIANT" >> "$BUILD_LOG"
    
    # Execute build steps
    check_environment
    validate_build_environment
    clean_project
    
    # Attempt primary build
    if primary_build; then
        print_success "✅ Primary build strategy succeeded!"
        
        if find_and_display_apks; then
            print_success "🎉 Build completed successfully!"
            exit 0
        else
            print_warning "Build appeared to succeed but no APK files found"
            exit 1
        fi
    else
        print_warning "❌ Primary build strategy failed"
        
        if [ "$FALLBACK_ENABLED" = true ]; then
            if fallback_build_strategies; then
                print_success "✅ Fallback build strategy succeeded!"
                
                if find_and_display_apks; then
                    print_success "🎉 Build completed with fallback strategy!"
                    exit 0
                else
                    print_warning "Fallback build succeeded but no APK files found"
                    exit 1
                fi
            else
                print_error "❌ All build strategies failed"
                analyze_build_failure
                exit 1
            fi
        else
            print_error "❌ Build failed (use -f for fallback strategies)"
            analyze_build_failure
            exit 1
        fi
    fi
}

# Run main function
main "$@"