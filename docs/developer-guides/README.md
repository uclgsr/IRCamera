# Developer Guides

Technical guides for developers working on the IRCamera codebase.

## Contents

### Core Systems

- [ui-components-guide.md](ui-components-guide.md) - Phase 1 permissions handling implementation
  - Permission controller architecture
  - Runtime permission management
  - USB device permissions
  - Battery optimization handling
  - Integration examples

- [logging-utilities-guide.md](logging-utilities-guide.md) - Centralized logging and error handling
  - AppLogger API and usage
  - ErrorHandler utilities
  - Structured logging
  - Migration from legacy logging

- [permission-handling-guide.md](permission-handling-guide.md) - Permission tools and utilities
  - PermissionTools API
  - Activity/Fragment integration
  - Permission callbacks
  - Best practices

## Usage

These guides document core utilities and systems used throughout the codebase. Refer to them when:

- Implementing new features that require permissions
- Adding logging to components
- Handling errors consistently
- Understanding the permission system architecture

## Source Locations

The actual implementation code is located in:

- **UI Components**: `app/src/main/java/mpdc4gsr/core/ui/`
- **Utilities**: `app/src/main/java/mpdc4gsr/core/utils/`
- **Permission Tools**: `libunified/`

## Related Documentation

- [../android/](../android/) - Android app networking documentation
- [../maintenance/](../maintenance/) - Maintenance and migration guides
- [../anti-patterns-checklist.md](../anti-patterns-checklist.md) - Code quality guidelines
