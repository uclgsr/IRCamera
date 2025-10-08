# Phase 2: Import Update Guide

## Overview

Phase 1 created the new Clean Architecture structure and moved 190 files. Phase 2 requires updating all import statements in those files to reference the new package locations.

## Current Status

✅ **Completed (Phase 1)**:
- New directory structure created
- 190 files copied to new locations
- Package declarations updated in all files
- Comprehensive documentation created

⏳ **Remaining (Phase 2)**:
- Update import statements in 190 files
- Fix compilation errors
- Verify build succeeds

## The Challenge

All moved files still have imports referencing the old package locations:

```kotlin
// OLD imports (need updating)
import mpdc4gsr.core.data.model.DeviceInfo           // Should be: mpdc4gsr.domain.model
import mpdc4gsr.feature.gsr.ui.GSRMonitorScreen      // Should be: mpdc4gsr.presentation.screens.gsr
import mpdc4gsr.core.ui.theme.Theme                  // Should be: mpdc4gsr.ui.theme
```

## Import Mapping Guide

### Domain Layer Imports

**Old** → **New**:
```kotlin
mpdc4gsr.core.data.model.*          → mpdc4gsr.domain.model.*
mpdc4gsr.feature.*.domain.model.*   → mpdc4gsr.domain.model.*
mpdc4gsr.feature.*.domain.repository.* → mpdc4gsr.domain.repository.*
mpdc4gsr.feature.*.domain.usecase.* → mpdc4gsr.domain.usecase.*
```

### Data Layer Imports

**Old** → **New**:
```kotlin
mpdc4gsr.core.data.*Repository      → mpdc4gsr.data.repository.*
mpdc4gsr.feature.*.data.repository.* → mpdc4gsr.data.repository.*
mpdc4gsr.feature.*.data.source.*    → mpdc4gsr.data.source.*
```

### Presentation Layer Imports

**Old** → **New**:
```kotlin
mpdc4gsr.feature.camera.ui.*        → mpdc4gsr.presentation.screens.camera.*
mpdc4gsr.feature.camera.presentation.* → mpdc4gsr.presentation.screens.camera.*
mpdc4gsr.feature.gsr.ui.*           → mpdc4gsr.presentation.screens.gsr.*
mpdc4gsr.feature.gsr.presentation.* → mpdc4gsr.presentation.screens.gsr.*
mpdc4gsr.feature.thermal.ui.*       → mpdc4gsr.presentation.screens.thermal.*
mpdc4gsr.feature.thermal.presentation.* → mpdc4gsr.presentation.screens.thermal.*
mpdc4gsr.feature.network.ui.*       → mpdc4gsr.presentation.screens.network.*
mpdc4gsr.feature.settings.ui.*      → mpdc4gsr.presentation.screens.settings.*
mpdc4gsr.feature.main.ui.*          → mpdc4gsr.presentation.screens.main.*
mpdc4gsr.core.ui.navigation.*       → mpdc4gsr.presentation.navigation.*
mpdc4gsr.core.ui.BaseViewModel      → mpdc4gsr.presentation.common.BaseViewModel
```

### Infrastructure Layer Imports

**Old** → **New**:
```kotlin
mpdc4gsr.core.RecordingService      → mpdc4gsr.infrastructure.service.RecordingService
mpdc4gsr.core.SessionManager        → mpdc4gsr.infrastructure.service.SessionManager
mpdc4gsr.core.monitoring.*          → mpdc4gsr.infrastructure.monitoring.*
mpdc4gsr.core.data.SecurityMonitor  → mpdc4gsr.infrastructure.security.SecurityMonitor
mpdc4gsr.core.data.TimeSyncManager  → mpdc4gsr.infrastructure.sync.TimeSyncManager
mpdc4gsr.core.App                   → mpdc4gsr.infrastructure.platform.App
```

### UI Layer Imports

**Old** → **New**:
```kotlin
mpdc4gsr.core.ui.components.*       → mpdc4gsr.ui.components.*
mpdc4gsr.core.ui.theme.*            → mpdc4gsr.ui.theme.*
mpdc4gsr.core.ui.utils.*            → mpdc4gsr.ui.utils.*
```

### DI Layer Imports

**Old** → **New**:
```kotlin
mpdc4gsr.core.di.*                  → mpdc4gsr.di.*
mpdc4gsr.feature.*.di.*             → mpdc4gsr.di.*
```

## Approach Options

### Option 1: Manual Update (Time-consuming)

1. Open each file in the new structure
2. Find and replace imports according to mapping above
3. Verify each change
4. Test compilation

**Pros**: Full control, understand each change  
**Cons**: Very time-consuming (190 files)

### Option 2: Android Studio Refactoring (Recommended)

1. Open project in Android Studio
2. Use "Find and Replace in Files" (Ctrl+Shift+R / Cmd+Shift+R)
3. Search for old package patterns
4. Replace with new package patterns
5. Preview and apply changes

**Pros**: Faster, IDE helps validate  
**Cons**: Requires careful pattern matching

### Option 3: Script-based Update (Fastest)

Use bash/sed scripts to automate the updates:

```bash
#!/bin/bash
# Example script to update imports

# Update domain model imports
find /path/to/mpdc4gsr -name "*.kt" -type f -exec sed -i \
  's/import mpdc4gsr\.core\.data\.model\./import mpdc4gsr.domain.model./g' {} \;

# Update presentation imports  
find /path/to/mpdc4gsr -name "*.kt" -type f -exec sed -i \
  's/import mpdc4gsr\.feature\.gsr\.ui\./import mpdc4gsr.presentation.screens.gsr./g' {} \;

# Repeat for all patterns...
```

**Pros**: Fastest, consistent  
**Cons**: Need careful pattern matching, may miss edge cases

## Recommended Workflow

### Step 1: Backup
```bash
git checkout -b phase2-import-updates
git push -u origin phase2-import-updates
```

### Step 2: Start with Small Layer

Begin with **domain layer** (smallest, 13 files):

```bash
cd app/src/main/java/mpdc4gsr/domain
```

1. Update imports in all domain files
2. Verify compilation of domain layer
3. Commit: `git commit -m "Phase 2: Update domain layer imports"`

### Step 3: Move to Data Layer

Update **data layer** (11 files):
1. Update imports in data files
2. Verify compilation
3. Commit: `git commit -m "Phase 2: Update data layer imports"`

### Step 4: Infrastructure Layer

Update **infrastructure layer** (15 files):
1. Update imports
2. Verify services compile
3. Commit: `git commit -m "Phase 2: Update infrastructure imports"`

### Step 5: UI Layer

Update **UI layer** (21 files):
1. Update imports in components and theme
2. Verify compilation
3. Commit: `git commit -m "Phase 2: Update UI layer imports"`

### Step 6: DI Layer

Update **DI layer** (8 files):
1. Update imports in all DI modules
2. Verify DI configuration
3. Commit: `git commit -m "Phase 2: Update DI imports"`

### Step 7: Presentation Layer (Largest)

Update **presentation layer** (122 files) by feature:

1. **Camera** (14 files): Update and test
2. **GSR** (35 files): Update and test
3. **Thermal** (25 files): Update and test
4. **Network** (8 files): Update and test
5. **Settings** (23 files): Update and test
6. **Main** (9 files): Update and test
7. **Navigation** (4 files): Update and test
8. **Common** (3 files): Update and test

Commit after each feature group.

### Step 8: Full Build

```bash
./gradlew clean build
```

Fix any remaining compilation errors.

### Step 9: Update References in Old Files

Files in old `core/` and `feature/` directories that import the new locations need updating:

```bash
# Find files importing new packages
grep -r "import mpdc4gsr.domain" app/src/main/java/mpdc4gsr/core/
grep -r "import mpdc4gsr.domain" app/src/main/java/mpdc4gsr/feature/
```

These can be temporarily updated or left as-is until old structure is removed.

## Import Update Script Template

```bash
#!/bin/bash
# import-updater.sh - Automated import update script

BASE_DIR="app/src/main/java/mpdc4gsr"

echo "Updating imports in new architecture..."

# Domain layer
echo "Updating domain imports..."
find "$BASE_DIR/domain" -name "*.kt" -exec sed -i \
  -e 's/import mpdc4gsr\.core\.data\.model\./import mpdc4gsr.domain.model./g' \
  -e 's/import mpdc4gsr\.feature\.[^.]*\.domain\./import mpdc4gsr.domain./g' \
  {} \;

# Data layer
echo "Updating data imports..."
find "$BASE_DIR/data" -name "*.kt" -exec sed -i \
  -e 's/import mpdc4gsr\.core\.data\./import mpdc4gsr.data./g' \
  -e 's/import mpdc4gsr\.feature\.[^.]*\.data\./import mpdc4gsr.data./g' \
  {} \;

# Infrastructure layer
echo "Updating infrastructure imports..."
find "$BASE_DIR/infrastructure" -name "*.kt" -exec sed -i \
  -e 's/import mpdc4gsr\.core\./import mpdc4gsr.infrastructure./g' \
  {} \;

# UI layer
echo "Updating UI imports..."
find "$BASE_DIR/ui" -name "*.kt" -exec sed -i \
  -e 's/import mpdc4gsr\.core\.ui\./import mpdc4gsr.ui./g' \
  {} \;

# DI layer
echo "Updating DI imports..."
find "$BASE_DIR/di" -name "*.kt" -exec sed -i \
  -e 's/import mpdc4gsr\.core\.di\./import mpdc4gsr.di./g' \
  -e 's/import mpdc4gsr\.feature\.[^.]*\.di\./import mpdc4gsr.di./g' \
  {} \;

# Presentation layer
echo "Updating presentation imports..."
find "$BASE_DIR/presentation" -name "*.kt" -exec sed -i \
  -e 's/import mpdc4gsr\.feature\.camera\.\(ui\|presentation\)\./import mpdc4gsr.presentation.screens.camera./g' \
  -e 's/import mpdc4gsr\.feature\.gsr\.\(ui\|presentation\)\./import mpdc4gsr.presentation.screens.gsr./g' \
  -e 's/import mpdc4gsr\.feature\.thermal\.\(ui\|presentation\)\./import mpdc4gsr.presentation.screens.thermal./g' \
  -e 's/import mpdc4gsr\.feature\.network\.\(ui\|presentation\)\./import mpdc4gsr.presentation.screens.network./g' \
  -e 's/import mpdc4gsr\.feature\.settings\.\(ui\|presentation\)\./import mpdc4gsr.presentation.screens.settings./g' \
  -e 's/import mpdc4gsr\.feature\.main\.\(ui\|presentation\)\./import mpdc4gsr.presentation.screens.main./g' \
  -e 's/import mpdc4gsr\.core\.ui\.navigation\./import mpdc4gsr.presentation.navigation./g' \
  -e 's/import mpdc4gsr\.core\.ui\.BaseViewModel/import mpdc4gsr.presentation.common.BaseViewModel/g' \
  {} \;

echo "Import updates complete!"
echo "Run './gradlew build' to check for compilation errors"
```

## Testing After Import Updates

### 1. Verify Compilation
```bash
./gradlew assembleDebug
```

### 2. Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### 3. Run Lint
```bash
./gradlew lint
```

### 4. Check for Missing Imports
```bash
# Look for compilation errors
./gradlew assembleDebug 2>&1 | grep "unresolved reference"
```

## Common Issues and Solutions

### Issue 1: Unresolved Reference

**Problem**: `Unresolved reference: SomeClass`

**Solution**: 
1. Check if file was moved to new structure
2. Update import to new package location
3. Verify package declaration in source file

### Issue 2: Circular Dependencies

**Problem**: `Circular dependency between X and Y`

**Solution**:
1. Identify the cycle
2. Extract interface to domain layer
3. Implement in data layer
4. Use interface in presentation

### Issue 3: Duplicate Classes

**Problem**: `Class SomeClass is defined multiple times`

**Solution**:
1. Old structure still has the file
2. Either update old imports or delete old file
3. Phase 3 will remove old structure entirely

### Issue 4: Android Framework in Domain

**Problem**: Domain layer imports Android classes

**Solution**:
1. Remove Android dependency from domain
2. Create interface in domain
3. Implement in data or infrastructure layer

## Verification Checklist

After completing import updates:

- [ ] All domain layer files compile
- [ ] All data layer files compile
- [ ] All presentation layer files compile
- [ ] All infrastructure layer files compile
- [ ] All UI layer files compile
- [ ] All DI layer files compile
- [ ] Full project builds: `./gradlew build`
- [ ] Unit tests pass: `./gradlew test`
- [ ] No unresolved references
- [ ] No circular dependencies
- [ ] Lint passes: `./gradlew lint`

## Time Estimates

| Layer | Files | Estimated Time |
|-------|-------|----------------|
| Domain | 13 | 30 minutes |
| Data | 11 | 30 minutes |
| Infrastructure | 15 | 45 minutes |
| UI | 21 | 45 minutes |
| DI | 8 | 30 minutes |
| Presentation | 122 | 3-4 hours |
| Testing & Fixes | - | 2-3 hours |
| **Total** | **190** | **8-10 hours** |

With automation scripts: **4-6 hours**

## Phase 3 Preview

After Phase 2 completes successfully:

### Phase 3 Tasks:
1. Remove old `core/` directory
2. Remove old `feature/` directory
3. Update AndroidManifest.xml references
4. Update XML layouts importing old classes
5. Update any resource references
6. Final testing
7. Update README and documentation

## Support

If you encounter issues:

1. Check the mapping guide above
2. Review [NEW_ARCHITECTURE_GUIDE.md](NEW_ARCHITECTURE_GUIDE.md)
3. Look for similar patterns in completed files
4. Use IDE's "Find Usages" to track dependencies

## Success Criteria

Phase 2 is complete when:

✅ All 190 files have updated imports  
✅ Project builds successfully  
✅ All tests pass  
✅ No unresolved references  
✅ No circular dependencies  

---

**Ready to start? Begin with domain layer (smallest) and work your way up!**
