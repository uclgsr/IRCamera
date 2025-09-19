#!/bin/bash



set -e

echo "🚀 IRCamera Build Script for Testing"
echo "====================================="


if [ ! -f "./gradlew" ]; then
    echo "❌ gradlew not found in current directory"
    exit 1
fi


chmod +x ./gradlew

echo "📋 Checking build environment..."
echo "Java version:"
java -version
echo ""

echo "🔧 Attempting standard build..."


if ./gradlew assembleRelease --no-configuration-cache 2>&1 | tee build.log; then
    echo "✅ Standard build successful!"
    
    
    echo "📱 APK files generated:"
    find . -name "*.apk" -type f -exec ls -lh {} \;
    
    echo ""
    echo "🎉 Build completed successfully!"
    echo "To install on Android device:"
    echo "  adb install app/build/outputs/apk/release/app-release.apk"
    echo ""
    echo "Next steps:"
    echo "1. Install APK on Android device"
    echo "2. Run hardware tests using HARDWARE_TESTING_GUIDE.md"
    echo "3. Use pc-controller/comprehensive_validation.py for automated testing"
    
    exit 0
else
    echo "❌ Standard build failed. Checking build log..."
    
    
    if grep -q "gsyVideoPlayer" build.log; then
        echo "🔍 Detected GSYVideoPlayer dependency issue"
        echo "💡 Attempting to fix GSYVideoPlayer dependency..."
        
        
        sed -i 's/gsyVideoPlayer-java:v11.1.0/gsyVideoPlayer-java:v8.6.0-release-jitpack/g' component/thermal-ir/build.gradle.kts
        
        echo "🔄 Retrying build with fixed dependency..."
        if ./gradlew assembleRelease --no-configuration-cache; then
            echo "✅ Build successful after dependency fix!"
            echo "📱 APK files generated:"
            find . -name "*.apk" -type f -exec ls -lh {} \;
            exit 0
        fi
    fi
    
    if grep -q "Configuration cache" build.log; then
        echo "🔍 Detected configuration cache issue"
        echo "🔄 Retrying without configuration cache..."
        if ./gradlew clean assembleRelease --no-configuration-cache --no-build-cache; then
            echo "✅ Build successful without caches!"
            echo "📱 APK files generated:"
            find . -name "*.apk" -type f -exec ls -lh {} \;
            exit 0
        fi
    fi
    
    echo "❌ Build failed with multiple strategies"
    echo "🔧 Attempting minimal build (debug variant)..."
    
    
    if ./gradlew assembleDebug --no-configuration-cache; then
        echo "✅ Debug build successful!"
        echo "📱 Debug APK files generated:"
        find . -name "*debug*.apk" -type f -exec ls -lh {} \;
        echo ""
        echo "⚠️  Note: Using debug APK for testing"
        echo "To install: adb install app/build/outputs/apk/debug/app-debug.apk"
        exit 0
    fi
    
    echo "❌ All build strategies failed"
    echo "📋 Build failure analysis:"
    echo "=========================================="
    tail -50 build.log
    echo "=========================================="
    echo ""
    echo "🔧 Troubleshooting suggestions:"
    echo "1. Clear all caches: ./gradlew clean --no-configuration-cache"
    echo "2. Check network connectivity for dependency downloads"
    echo "3. Update Android SDK and build tools"
    echo "4. Check available disk space"
    echo "5. Try building individual modules first"
    echo ""
    echo "🆘 If build continues to fail:"
    echo "- Review dependency versions in build.gradle.kts files"
    echo "- Check for Android SDK compatibility issues"
    echo "- Consider using Android Studio for more detailed error information"
    
    exit 1
fi
