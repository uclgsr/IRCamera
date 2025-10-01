# Coding Standards for IRCamera Project

## File Naming Conventions

### General Guidelines
- Use PascalCase for all class and file names
- File name must match the primary class/interface name within it
- Keep names descriptive but concise

### Utility Classes
**Standard:** Use **Utils** (plural) suffix for utility classes
- Examples: `CommUtils.kt`, `FileUtils.kt`, `NetworkUtils.kt`
- Rationale: Plural form indicates a collection of utility functions
- Legacy files with `Util` (singular) may remain but new files should use `Utils`

### Manager Classes
**Standard:** Use **Manager** suffix for classes managing state or resources
- Examples: `FileSchemaManager.kt`, `SessionManager.kt`, `NetworkManager.kt`
- Use when the class maintains state and coordinates operations

### Helper Classes
**Standard:** Use **Helper** suffix for stateless assistance classes
- Examples: `TempDrawHelper.kt`, `BackgroundScanHelper.kt`
- Use for pure functional helpers without state

### Activity Classes
**Standard:** Use `Activity` or `ComposeActivity` suffix
- Fragment-based: `NetworkConfigActivity.kt`
- Compose-based: `NetworkConfigActivityCompose.kt` or `NetworkConfigComposeActivity.kt`
- Preference: `ComposeActivity` suffix for new Compose activities

### View Model Classes
**Standard:** Use `ViewModel` suffix
- Examples: `NetworkSettingsViewModel.kt`, `SessionExportViewModel.kt`

## Timestamp Format Standards

### File Names
**Standard:** Use `yyyyMMdd_HHmmss_SSS` format
```kotlin
val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
```
- Year: 4 digits (yyyy)
- Month: 2 digits (MM)
- Day: 2 digits (dd)
- Hour: 24-hour format, 2 digits (HH)
- Minute: 2 digits (mm)
- Second: 2 digits (ss)
- Millisecond: 3 digits (SSS)
- Separator: underscore (_)

### Display Timestamps
**Standard:** Use `yyyy-MM-dd HH:mm:ss` format for UI display
```kotlin
val displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
```

### Session IDs
**Standard:** Use `yyyyMMdd_HHmmss_SSS` to ensure uniqueness
- Defined in `FileSchemaManager.kt` and `SessionDirectoryManager.kt`

## Comment Standards

### File Header Comments
**Requirement:** Meaningful description of file purpose and functionality

Good example:
```kotlin
/**
 * File Schema Manager
 *
 * Enforces consistent file naming and schema conventions across all sensor modalities.
 * Addresses standardized session directory structure requirements and ensures
 * each sensor's CSV has a clear header and units.
 */
```

Avoid:
```java
/**
 * @ProjectName: ANDROID_IRUVC_SDK
 * @Package: com.infisense.iruvc.utils
 * @ClassName: FileUtil
 * @Description:
 * @Author: brilliantzhao
 * @CreateDate: 2021.11.11 13:56
 */
```

### Method Comments
**Standard:** Use meaningful Javadoc/KDoc only when necessary
- Document complex algorithms or non-obvious behavior
- Document public API methods in libraries
- Avoid redundant comments that simply restate the method name

Good example:
```kotlin
/**
 * Validates thermal data against expected schema including resolution bounds,
 * temperature range limits, and required metadata fields.
 */
fun validateThermalData(data: Map<String, Any>): ValidationResult
```

Avoid:
```java
/**
 * @param bytes
 * @param fileTitle
 */
public static void saveByteFile(byte[] bytes, String fileTitle)
```

### Inline Comments
**Standard:** Minimal, only when necessary
- Explain "why", not "what"
- Use for complex logic or workarounds
- Never use non-ASCII characters (Chinese, special symbols)

Good:
```kotlin
// Retry connection after 2 seconds to avoid overwhelming the sensor
delay(2000)
```

Avoid:
```java
// 
file.createNewFile()
```

### TODO Comments
**Standard:** Use structured format with context
```kotlin
// TODO: Requirement "One failing sensor should not derail entire session"
// Context: Implement graceful degradation when thermal camera disconnects
```

Format:
- Start with `TODO:` or `FIXME:`
- Include requirement reference or issue number if applicable
- Provide brief context
- Keep on single line if possible

### Language
**Requirement:** All comments MUST be in English
- No Chinese characters
- No special Unicode symbols that are not ASCII-safe
- Exception: User-facing strings can be localized

## Architecture Patterns

### MVVM Pattern
**Requirement:** Follow MVVM for all UI components
- Model: Data classes and repositories
- View: Composables or Activities/Fragments
- ViewModel: Business logic and state management

### Repository Pattern
**Requirement:** Use repositories for data access
- Separate data sources from business logic
- Abstract implementation details
- Example: `SessionRepository.kt`, `NetworkRepository.kt`

### Naming Convention for Repositories
```kotlin
interface SessionRepository { }
class SessionRepositoryImpl : SessionRepository { }
```

## Package Structure

### App Module
```
app/src/main/java/mpdc4gsr/
├── core/           # Core utilities, managers, base classes
├── data/           # Data models, repositories
├── feature/        # Feature modules (network, gsr, thermal, etc.)
├── compose/        # Compose-specific base classes
└── utils/          # General utility functions
```

### Library Module
```
libunified/src/main/java/com/mpdc4gsr/libunified/
├── app/            # App-level utilities
├── ir/             # Thermal camera specific code
└── ui/             # UI components and utilities
```

## Code Style

### Kotlin
- Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `kotlin.code.style=official` in gradle.properties
- 4 spaces for indentation
- No wildcard imports

### Java
- Follow Android Java style guide
- 4 spaces for indentation
- Maximum line length: 120 characters

### Android Specific
- Follow [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide)
- Use `@Parcelize` for Parcelable data classes
- Prefer `by lazy` over lateinit when appropriate

## Comments: When to Comment

### Always Comment
1. Complex algorithms or non-trivial logic
2. Workarounds for bugs or limitations
3. Public APIs in library modules
4. Architecture decisions that aren't obvious
5. Performance-critical sections

### Never Comment
1. Obvious code that is self-explanatory
2. Redundant information already in method name
3. Outdated information (remove or update instead)
4. Code that should be removed (delete it instead)

### Comment Maintenance
- Update comments when code changes
- Remove obsolete comments
- Prefer self-documenting code over comments

## Version Control

### Commit Messages
- Use present tense: "Add feature" not "Added feature"
- First line: concise summary (50 chars or less)
- Detailed description after blank line if needed
- Reference issue numbers when applicable

### Branch Naming
- Feature: `feature/description`
- Bugfix: `bugfix/description`
- Copilot: `copilot/fix-{hash}`

## Testing

### Test File Naming
- Unit tests: `{ClassName}Test.kt`
- Integration tests: `{Feature}IntegrationTest.kt`
- Instrumentation tests: `{Feature}InstrumentationTest.kt`

### Test Package Structure
- Mirror source package structure
- Keep tests close to code they test

## Review Checklist

Before submitting code:
- [ ] File names follow conventions
- [ ] All comments are in English (no Chinese characters)
- [ ] Timestamp formats are standardized
- [ ] No minimal/empty Javadoc comments
- [ ] TODO comments are properly formatted
- [ ] Code follows MVVM and Repository patterns
- [ ] No ASCII-unsafe characters anywhere
- [ ] Build passes (`./gradlew build`)
- [ ] Lint passes (`./gradlew lint`)

## References

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Android Java Style Guide](https://source.android.com/docs/setup/contribute/code-style)
- Project-specific: `FileSchemaManager.kt` for file naming patterns
