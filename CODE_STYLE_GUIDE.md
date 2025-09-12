# IRCamera Code Style Guide

## Kotlin/Java Style Guidelines

### 1. Naming Conventions
```kotlin
// Classes: PascalCase
class ThermalImageProcessor

// Functions: camelCase
fun processThermalData()

// Constants: SCREAMING_SNAKE_CASE
const val MAX_TEMPERATURE = 100.0

// Variables: camelCase
val thermalSensor = ThermalSensor()
```

### 2. Documentation Standards
```kotlin
/**
 * Processes thermal imaging data with advanced analytics.
 * 
 * @param thermalData Raw thermal sensor data
 * @param analysisMode Type of analysis to perform
 * @return Processed thermal analysis result
 * @throws ThermalProcessingException If processing fails
 */
fun processThermalData(
    thermalData: ByteArray,
    analysisMode: AnalysisMode
): ThermalAnalysisResult
```

### 3. Code Organization
```kotlin
class ThermalImageProcessor {
    // Constants first
    companion object {
        private const val DEFAULT_THRESHOLD = 25.0
    }
    
    // Properties
    private val sensorManager: SensorManager
    
    // Constructor
    constructor(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    
    // Public methods
    fun startProcessing() { }
    
    // Private methods
    private fun processInternal() { }
}
```

### 4. Error Handling
```kotlin
try {
    val result = processThermalData(data)
    handleSuccess(result)
} catch (e: ThermalProcessingException) {
    Log.e(TAG, "Thermal processing failed", e)
    handleError(e)
}
```

## Python Style Guidelines

### 1. Type Hints
```python
def process_thermal_data(
    thermal_data: bytes,
    analysis_mode: AnalysisMode
) -> ThermalAnalysisResult:
    '''Process thermal imaging data with advanced analytics.'''
    pass
```

### 2. Docstring Format
```python
def process_thermal_data(thermal_data: bytes) -> dict:
    '''
    Process thermal imaging data.
    
    Args:
        thermal_data: Raw thermal sensor data
        
    Returns:
        Dictionary containing processed thermal analysis
        
    Raises:
        ThermalProcessingError: If processing fails
    '''
    pass
```

## XML Style Guidelines

### 1. Layout Structure
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    
    <!-- Thermal image display -->
    <ImageView
        android:id="@+id/thermal_image"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:contentDescription="@string/thermal_image_description" />
        
</LinearLayout>
```

### 2. Resource Naming
```xml
<!-- Colors -->
<color name="thermal_primary">#FF5722</color>
<color name="thermal_secondary">#FFC107</color>

<!-- Strings -->
<string name="thermal_mode_title">Thermal Mode</string>
<string name="temperature_measurement">Temperature Measurement</string>

<!-- Dimensions -->
<dimen name="thermal_menu_height">48dp</dimen>
<dimen name="thermal_button_margin">16dp</dimen>
```

## Build Configuration

### 1. Gradle Best Practices
```kotlin
android {
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
        targetSdk = 34
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
```

## Quality Assurance

### 1. Code Review Checklist
- [ ] All public APIs have documentation
- [ ] Error handling is comprehensive
- [ ] Resource management follows best practices
- [ ] Performance considerations addressed
- [ ] Security implications reviewed
- [ ] Testing coverage adequate

### 2. Automated Quality Tools
- **ktlint**: Kotlin style checking
- **detekt**: Static code analysis
- **jacoco**: Code coverage
- **gradle-versions-plugin**: Dependency updates

This guide ensures consistent, maintainable, and high-quality code across the IRCamera thermal imaging platform.
