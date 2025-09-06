# Common Build Configuration
# This file centralizes common build logic and reduces duplication

# Common Plugin Configurations
# All library modules should use these standard plugins:
# - com.android.library
# - kotlin-android
# - kotlin-kapt (only if needed for annotation processing)
# - kotlin-parcelize (for Parcelable support)

# Common Android Configuration for Libraries
COMPILE_SDK=35
MIN_SDK=24
TARGET_JVM_VERSION=17
NDK_VERSION=21.3.6528147

# Build Performance Optimizations
# - Use gradle.properties for JVM and daemon configuration
# - Enable parallel builds and caching
# - Disable KAPT K2 for stability (causes warnings)

# Dependency Consolidation Guidelines:
# 1. Use version catalog bundles (libs.bundles.ui-common, libs.bundles.thermal-common)
# 2. Avoid duplicate implementation of same libraries across modules
# 3. Prefer 'api' for dependencies that need to be exposed to consuming modules
# 4. Use 'implementation' for internal dependencies

# KAPT Configuration Standards:
# - Only configure room-related arguments in modules that use Room
# - Remove AROUTER_MODULE_NAME to eliminate unrecognized processor warnings
# - Set correctErrorTypes=true and useBuildCache=true for performance

# Test Configuration:
# - Use explicit imports instead of wildcard imports in test files
# - Ensure all test modules have proper JUnit and Espresso dependencies
# - Keep test files minimal and focused

# Anti-Pattern Prevention:
# 1. NO wildcard imports (import package.*)
# 2. NO duplicate plugin applications
# 3. NO conflicting dependency versions
# 4. NO unnecessary KAPT processors
# 5. NO excessive build configuration duplication