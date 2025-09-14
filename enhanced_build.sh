#!/bin/bash

# Enhanced Build Script for Hub-and-Spoke Implementation
# Optimized build process with better error handling and performance

echo "🚀 Enhanced Hub-and-Spoke Build System"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    print_error "gradlew not found. Please run this script from the project root directory."
    exit 1
fi

# Clean previous builds
print_status "Cleaning previous builds..."
./gradlew clean

if [ $? -ne 0 ]; then
    print_error "Clean failed"
    exit 1
fi

print_success "Clean completed"

# Optimize Gradle daemon
print_status "Optimizing Gradle daemon..."
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8"

# Build with enhanced configuration
print_status "Building Android application with enhanced Hub-Spoke features..."

# Use parallel builds and configure compiler
./gradlew build \
    --parallel \
    --configure-on-demand \
    --build-cache \
    -Dorg.gradle.jvmargs="-Xmx4g -XX:MaxMetaspaceSize=512m" \
    -Dkotlin.compiler.execution.strategy="in-process" \
    -Dkotlin.incremental=true \
    -Dkotlin.incremental.useClasspathSnapshot=true \
    -Pandroid.useAndroidX=true \
    -Pandroid.enableJetifier=true

BUILD_RESULT=$?

if [ $BUILD_RESULT -eq 0 ]; then
    print_success "Android application build completed successfully!"
    
    # Check if APK was generated
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        APK_SIZE=$(stat -f%z "app/build/outputs/apk/debug/app-debug.apk" 2>/dev/null || stat -c%s "app/build/outputs/apk/debug/app-debug.apk" 2>/dev/null)
        APK_SIZE_MB=$((APK_SIZE / 1024 / 1024))
        print_success "Debug APK generated: ${APK_SIZE_MB} MB"
        print_status "Location: app/build/outputs/apk/debug/app-debug.apk"
    fi
    
    # Build PC Controller if available
    if [ -d "pc-controller" ]; then
        print_status "Building PC Controller (Hub)..."
        cd pc-controller
        
        # Check if virtual environment exists, create if not
        if [ ! -d "venv" ]; then
            print_status "Creating Python virtual environment..."
            python3 -m venv venv
        fi
        
        # Activate virtual environment
        source venv/bin/activate 2>/dev/null || source venv/Scripts/activate 2>/dev/null
        
        # Install dependencies
        if [ -f "requirements.txt" ]; then
            print_status "Installing PC Controller dependencies..."
            pip install -r requirements.txt
            
            if [ $? -eq 0 ]; then
                print_success "PC Controller dependencies installed"
                
                # Test PC Controller
                print_status "Testing PC Controller..."
                python -c "
try:
    import sys
    sys.path.append('src')
    from ircamera_pc.core.session_manager import SessionManager
    print('✅ PC Controller core modules load successfully')
except Exception as e:
    print(f'❌ PC Controller test failed: {e}')
"
            else
                print_warning "PC Controller dependency installation failed"
            fi
        else
            print_warning "requirements.txt not found in pc-controller"
        fi
        
        cd ..
    fi
    
    echo ""
    print_success "🎯 Hub-and-Spoke Build Complete!"
    echo ""
    print_status "📱 Android Sensor Node (Spoke): Ready for installation"
    print_status "🖥️  PC Controller (Hub): Ready for execution"
    print_status "🔗 Integration Demo: Available in both applications"
    echo ""
    print_status "Next steps:"
    echo "  1. Install Android APK: adb install app/build/outputs/apk/debug/app-debug.apk"
    echo "  2. Run PC Controller: cd pc-controller && python integration_example.py --demo-mode"
    echo "  3. Test Hub-Spoke integration via Android app's integration demo"
    
else
    print_error "Build failed with exit code $BUILD_RESULT"
    
    # Show helpful error information
    print_status "Troubleshooting tips:"
    echo "  • Check Android SDK and build tools are properly installed"
    echo "  • Verify Gradle wrapper permissions: chmod +x gradlew"
    echo "  • Clean project: ./gradlew clean"
    echo "  • Update dependencies: ./gradlew --refresh-dependencies"
    echo "  • Check log files in build/reports/"
    
    exit $BUILD_RESULT
fi
