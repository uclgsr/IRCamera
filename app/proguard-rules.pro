# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ====================================================
# Shimmer Android SDK ProGuard Rules for Production
# ====================================================

# Keep all Shimmer classes and interfaces
-keep class com.shimmerresearch.** { *; }
-keep interface com.shimmerresearch.** { *; }
-keep enum com.shimmerresearch.** { *; }

# Keep Shimmer driver classes
-keep class com.shimmerresearch.driver.** { *; }
-keep class com.shimmerresearch.android.** { *; }
-keep class com.shimmerresearch.bluetooth.** { *; }

# Keep Shimmer data model classes (important for serialization)
-keep class com.shimmerresearch.driver.ObjectCluster { *; }
-keep class com.shimmerresearch.driver.FormatCluster { *; }
-keep class com.shimmerresearch.driver.Configuration { *; }

# Keep Shimmer BLE related classes
-keep class com.shimmerresearch.bluetooth.ShimmerBluetooth { *; }
-keep class com.shimmerresearch.bluetooth.ShimmerBLE { *; }

# Keep Parcelable implementations for Shimmer objects
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serialization attributes for Shimmer data
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SerializedName

# GSR-specific ProGuard rules
-keep class com.mpdc4gsr.sensors.gsr.** { *; }
-keep class com.mpdc4gsr.ble.** { *; }

# BLE and Bluetooth classes
-keep class android.bluetooth.** { *; }
-keep class no.nordicsemi.android.ble.** { *; }

# Keep native methods for JNI integration
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep reflection-based classes
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Prevent obfuscation of critical sensor data classes
-keep class com.mpdc4gsr.sensors.** { *; }

# Keep gson serialization classes
-keep class com.google.gson.** { *; }
-keep class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# CameraX classes for RGB recording
-keep class androidx.camera.** { *; }

# Coroutines support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# EventBus reflection-based event handlers
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}

# MethodHandle API compatibility rules (Phase 0-4 implementation)
-keep class java.lang.invoke.** { *; }
-dontwarn java.lang.invoke.**
-keep class kotlin.jvm.internal.** { *; }

# Enhanced Kotlin compatibility
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Network client compatibility (Phase 0-4 implementation)
-keep class com.mpdc4gsr.network.** { *; }

# ====================================================
# Performance optimization rules
# ====================================================

# Enable aggressive optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Remove debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Remove verbose GSR logging in release builds
-assumenosideeffects class com.mpdc4gsr.sensors.gsr.GSRSensorRecorder {
    private static void logVerbose(...);
}

# ====================================================
# ANR Prevention and Monitoring (Added for ANR fix)
# ====================================================

# Keep ANR prevention monitoring classes for debugging
-keep class mpdc4gsr.core.threading.MonitoredMainThreadPoster { *; }
-keep class mpdc4gsr.core.threading.MonitoredMainThreadPoster$* { *; }
-keep class mpdc4gsr.core.ui.SafeMainThreadHandler { *; }
-keep class mpdc4gsr.core.ui.SafeMainThreadHandler$* { *; }

# Keep performance manager for monitoring
-keep class mpdc4gsr.feature.camera.data.CameraPerformanceManager { *; }
-keep class mpdc4gsr.feature.camera.data.CameraPerformanceManager$* { *; }
