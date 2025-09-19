# Library Consolidation Plan

## Current Structure (Small Libraries)
- **libcom** (16 files) - Communication utilities, dialogs, PDF/Excel helpers
- **libmenu** (23 files) - Menu components and UI widgets  
- **libmatrix** (16 files) - Matrix operations and USB buffer handling

## Consolidation Strategy

### Target: Merge small libraries into libapp
libapp already contains 160 files and serves as the main application utility library.

### Step 1: Move libcom -> libapp/comm
```
libcom/src/main/java/com/topdon/libcom/* 
-> libapp/src/main/java/com/topdon/lib/core/comm/
```

### Step 2: Move libmenu -> libapp/menu  
```
libmenu/src/main/java/com/topdon/menu/*
-> libapp/src/main/java/com/topdon/lib/core/menu/
```

### Step 3: Move libmatrix -> libapp/matrix
```
libmatrix/src/main/java/com/guide/zm04c/matrix/*
-> libapp/src/main/java/com/topdon/lib/core/matrix/
```

### Step 4: Update gradle dependencies
Remove libcom, libmenu, libmatrix from settings.gradle.kts
Update all references in component build.gradle.kts files

### Benefits
- Reduces module count from 6 libraries to 3 libraries  
- Simplifies build system (3 fewer gradle files)
- Reduces dependency complexity
- Maintains logical grouping within libapp structure

### Impact Analysis
- Low risk: Small libraries with clear functionality
- Build time: Minimal impact (small modules)  
- Maintenance: Easier with fewer modules to track