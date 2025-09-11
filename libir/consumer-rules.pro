# Consumer ProGuard rules for libir module
# These rules will be applied to the consumer of this library

# Keep IR-related classes and methods
-keep class com.infisense.usbir.** { *; }
-keep class com.topdon.libir.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep thermal camera related classes
-keep class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}