# Consolidated Libraries and Components

These small libraries and components have been consolidated to simplify the project structure:

## Phase 3: Library Consolidation

- **libcom** (16 files) -> libapp/src/main/java/com/topdon/lib/core/comm/
- **libmenu** (23 files) -> libapp/src/main/java/com/topdon/lib/core/menu/
- **libmatrix** (16 files) -> libapp/src/main/java/com/topdon/lib/core/matrix/

This reduces the module count from 6 libraries to 3 libraries and simplifies the build system.

## Phase 4: Component Consolidation  

- **CommonComponent** (4 files) -> component/thermal-lite/ (consolidated since only used by thermal-lite)

This reduces the component count from 7 to 6 components.

## Package Name Changes
- com.topdon.libcom.* -> com.topdon.lib.core.comm.*
- com.topdon.menu.* -> com.topdon.lib.core.menu.*
- com.guide.zm04c.matrix.* -> com.topdon.lib.core.matrix.*
- com.energy.commoncomponent.* -> (integrated into thermal-lite)