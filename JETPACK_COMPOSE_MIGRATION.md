# Jetpack Compose Migration Guide

This document outlines the strategy and patterns for migrating the IRCamera project from traditional Android Views to Jetpack Compose.

## Overview

The project has been configured with Jetpack Compose infrastructure while maintaining backward compatibility with the existing View system. This allows for gradual migration without disrupting functionality.

## Infrastructure Setup ✓

- **Compose BOM**: 2024.12.01
- **Compiler Extension**: 1.6.0
- **Activity Compose**: 1.9.3
- **Modules Enabled**: app, libunified

## Base Classes

### Traditional View System
```kotlin
abstract class BaseBindingActivity<B : ViewDataBinding> : AppCompatActivity()
```

### Compose System
```kotlin
abstract class BaseComposeActivity : ComponentActivity() {
    @Composable
    protected abstract fun Content()
}
```

## Migration Pattern

### Step 1: Create Compose Version
Create a new Activity alongside the existing one:
- `VersionActivity.kt` → `VersionComposeActivity.kt`
- Implement `BaseComposeActivity`
- Define UI in `Content()` composable

### Step 2: Maintain Feature Parity
Ensure the Compose version has:
- Same functionality as original
- Same navigation patterns
- Same business logic
- EventBus integration (inherited from BaseComposeActivity)

### Step 3: Gradual Replacement
- Update navigation to point to Compose version
- Test thoroughly
- Remove old Activity when confident

## Example Migration

### Before (Traditional Views)
```kotlin
class VersionActivity : BaseBindingActivity<ActivityVersionBinding>() {
    override fun initContentLayoutId(): Int = R.layout.activity_version
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.versionCodeText.text = "Version info"
    }
}
```

### After (Compose)
```kotlin
class VersionComposeActivity : BaseComposeActivity() {
    @Composable
    override fun Content() {
        IRCameraTheme {
            VersionScreen()
        }
    }
    
    @Composable
    private fun VersionScreen() {
        Text("Version info")
    }
}
```

## Theme System

### IRCameraTheme
- Material 3 design system
- Dark/Light theme support
- Consistent with existing app colors
- Located in `mpdc4gsr.ui.theme`

### Color Scheme
- Primary: Based on existing accent colors
- Background: Matches current `#16131E`
- Surface: Consistent with current design

## Migration Priority

### High Priority (Simple Activities)
1. **VersionActivity** ✓ (Example completed)
2. **PolicyActivity** ✓ (Example completed)
3. **MoreHelpActivity** ✓ (Example completed)
4. **WebViewActivity** ✓ (Example completed)

### Medium Priority (Complex UI)
1. **MainActivity** (Complex with services/networking)
2. **DeviceTypeActivity** ✓ (Example completed)
3. **IRGalleryEditActivity**

### Low Priority (Legacy/Specialized)
1. Activities with heavy native integration
2. Camera-specific activities
3. Thermal processing activities

## Best Practices

### State Management
```kotlin
@Composable
fun MyScreen() {
    var state by remember { mutableStateOf(initialValue) }
    
    LaunchedEffect(key1) {
        // Side effects
    }
}
```

### Navigation
```kotlin
// Continue using existing NavigationManager for consistency
NavigationManager.build(RouterConfig.SOME_ROUTE)
    .navigation(context)
```

### EventBus Integration
```kotlin
// BaseComposeActivity handles EventBus registration
// Override methods as needed
override fun connected() {
    // Handle device connection
}
```

### Performance
- Use `remember` for expensive computations
- Use `LaunchedEffect` for side effects
- Prefer `derivedStateOf` for computed values

## Testing Strategy

### Each Migration Should:
1. Build successfully
2. Launch without crashes
3. Maintain original functionality
4. Handle device events (USB, Bluetooth)
5. Integrate with existing services

### Testing Checklist
- [ ] Activity launches
- [ ] UI displays correctly
- [ ] Navigation works
- [ ] EventBus events handled
- [ ] Service integration maintained
- [ ] Performance acceptable

## Current Status

### Completed ✓
- Compose infrastructure setup
- BaseComposeActivity created
- IRCameraTheme established
- VersionComposeActivity as simple UI example
- PolicyComposeActivity as WebView integration example
- WebViewComposeActivity as advanced WebView with error handling
- MoreHelpComposeActivity as conditional UI and system integration example
- DeviceTypeComposeActivity as complex list UI with device selection
- ClauseComposeActivity as user agreement and app initialization
- PdfComposeActivity as file handling and placeholder content
- NetworkConfigComposeActivity as permissions and Bluetooth integration
- SimpleNetworkTestComposeActivity as network testing and TCP/UDP connections
- FaultTolerantRecordingComposeActivity as recording state management and fault tolerance
- NetworkClientTestComposeActivity as service binding and bidirectional networking

### Next Steps
1. Migrate simple activities (MoreHelp, WebView)
2. Update navigation routing
3. Create Compose UI components for common patterns
4. Migrate complex activities gradually

## Rollback Strategy

Since both systems coexist:
- Original Activities remain functional
- Switch navigation back to old activities if issues
- Remove Compose code if needed
- No impact on production builds

## Dependencies

All Compose dependencies are contained in:
- `gradle/libs.versions.toml`
- Individual module `build.gradle.kts` files
- Can be disabled by removing `compose = true` from buildFeatures

## Common Patterns

### Loading States
```kotlin
var isLoading by remember { mutableStateOf(false) }

if (isLoading) {
    CircularProgressIndicator()
} else {
    MainContent()
}
```

### Lists
```kotlin
LazyColumn {
    items(itemList) { item ->
        ItemCard(item = item)
    }
}
```

### Forms
```kotlin
var text by remember { mutableStateOf("") }

OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Label") }
)
```

## Migration Utilities

### ComposeMigrationHelper
Utility class providing:
- EventBus integration for Compose
- Common conversion patterns
- Migration tracking
- Permission handling helpers

This migration approach ensures minimal disruption while modernizing the UI layer gradually.