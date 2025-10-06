# Android Application Documentation

Documentation for the Android application component of IRCamera, including development best practices, code quality guidelines, and implementation guides.

## Quick Start

### For New Developers

1. **Start Here:** [android-core-priorities.md](android-core-priorities.md) - Comprehensive overview of Android development priorities
2. **Read Next:** [android-code-quality-guide.md](android-code-quality-guide.md) - Critical issues and fixes
3. **Before Committing:** Review the code quality checklist

### For Experienced Developers

- **Performance:** [performance-optimization-guide.md](performance-optimization-guide.md)
- **Accessibility:** [accessibility-guidelines.md](accessibility-guidelines.md)
- **Implementation Summary:** [core-priorities-implementation-summary.md](core-priorities-implementation-summary.md)

## Core Documentation

### Essential Guides

| Document | Purpose | Priority |
|----------|---------|----------|
| [android-core-priorities.md](android-core-priorities.md) | Comprehensive guide covering all Android development priorities (correctness, performance, reliability, security, architecture, testing, accessibility, observability) | HIGH |
| [android-code-quality-guide.md](android-code-quality-guide.md) | Specific code quality issues with immediate fixes - Critical issues that need attention | CRITICAL |
| [performance-optimization-guide.md](performance-optimization-guide.md) | Strategies for optimizing cold start, rendering, memory, battery, and I/O performance | HIGH |
| [accessibility-guidelines.md](accessibility-guidelines.md) | Complete accessibility implementation guide with TalkBack, contrast, touch targets | MEDIUM |
| [core-priorities-implementation-summary.md](core-priorities-implementation-summary.md) | Summary of implemented framework and next steps | INFO |

### Specialized Topics

| Document | Purpose |
|----------|---------|
| [time-sync-flow-diagram.txt](time-sync-flow-diagram.txt) | Time synchronization protocol flow diagram |

## Code Quality Standards

### Critical Rules (Must Follow)

- ✗ No GlobalScope usage (use viewModelScope or lifecycleScope)
- ✗ No runBlocking in lifecycle methods
- ✓ Always clean up resources (ExecutorService, sockets, callbacks)
- ✓ All I/O on background threads (enforced by StrictMode in debug)

See [android-code-quality-guide.md](android-code-quality-guide.md) for complete checklist.

## CI/CD Pipeline

**Location:** `.github/workflows/android-quality-gates.yml`

**Automated Checks:**
1. Android Lint
2. Detekt (Kotlin static analysis)
3. Unit Tests
4. Build Verification

Run locally: `./gradlew lint detekt testDebugUnitTest assembleDebug`

## Performance Monitoring

### Frameworks Implemented

- **PerformanceMetrics:** Track cold start, frame rendering, operation timing
- **TelemetryManager:** Event tracking, error reporting, feature usage
- **StrictMode:** Automatic detection of performance issues in debug builds

**Usage:**
```kotlin
// Initialize in App.onCreate()
PerformanceMetrics.initialize()
TelemetryManager.initialize(context)

// Track operations
measureTime("database_query") {
    database.query(...)
}

// Track events
TelemetryManager.trackEvent("recording_started")
```

See [performance-optimization-guide.md](performance-optimization-guide.md) for details.

## Architecture

- **Pattern:** MVVM with Repository pattern
- **UI:** Hybrid XML/Compose (migrating to Compose-first)
- **Concurrency:** Kotlin Coroutines with lifecycle-aware scopes
- **DI:** Manual (migration to Hilt planned)

See [android-core-priorities.md](android-core-priorities.md) Section 7 for architecture details.

## Related Documentation

### PC Networking
- [../summaries/pc-networking-summary.md](../summaries/pc-networking-summary.md) - PC networking implementation

### PC Controller
- [../../pc-controller/docs/](../../pc-controller/docs/) - Protocol bridge, integration guides

### Maintenance
- [../maintenance/](../maintenance/) - Migration and fix documentation

### Implementation Summaries
- [../summaries/](../summaries/) - Feature implementation summaries

## Common Tasks

### Setting Up Development Environment

```bash
# Clone and build
git clone https://github.com/uclgsr/IRCamera.git
cd IRCamera
./gradlew build

# Run tests
./gradlew testDebugUnitTest

# Run quality checks
./gradlew lint detekt
```

### Before Committing

```bash
# Full quality check
./gradlew lint detekt testDebugUnitTest assembleDebug

# Review checklist in android-code-quality-guide.md
```

### Debugging Performance Issues

1. StrictMode automatically enabled in debug builds
2. Use Android Profiler in Android Studio
3. Check PerformanceMetrics logs
4. Review [performance-optimization-guide.md](performance-optimization-guide.md)

## Key Performance Indicators

**Targets:**
- Cold start time: < 2 seconds
- Crash-free sessions: > 99.5%
- ANR rate: < 0.1%
- Janky frames: < 5%
- Battery impact: < 5%/hour during recording

See [android-core-priorities.md](android-core-priorities.md) Section 10 for full KPI list.

## Getting Help

1. Check relevant guide in this directory
2. Review [android-code-quality-guide.md](android-code-quality-guide.md) for common issues
3. Review [android-core-priorities.md](android-core-priorities.md) for best practices
4. File an issue on GitHub

## Version History

- **2024:** Comprehensive framework implementation
  - Core priorities documentation
  - Performance monitoring framework
  - Accessibility guidelines
  - CI/CD pipeline
  - Telemetry and observability

---

**Last Updated:** 2024  
**Maintained By:** Android Development Team
