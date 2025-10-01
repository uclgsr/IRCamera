# Legacy Migration Scripts

This directory contains historical migration scripts that were used during the transition from legacy architecture to Clean Architecture with Compose.

## Status: ARCHIVED

These scripts have completed their purpose and are kept for historical reference only.

## Migration Scripts

### Fragment Migration
- **migrate_fragments.py** - Moved legacy fragments to backup/fragments
- **migrate_viewmodels.py** - Reorganized ViewModels by feature

### Activity Migration
- **migrate_thermal_activities.py** - Migrated thermal activities to Compose
- **migrate_remaining_modules.py** - Migrated remaining module activities

### Cleanup Scripts
- **cleanup_manifests.py** - Cleaned up Android manifest files
- **cleanup_component_manifests.py** - Cleaned up component manifest files
- **migrate_to_backup.py** - General backup utility

## Why Archived?

These scripts are no longer needed because:
1. All migrations are complete
2. The repository is now fully organized
3. Clean Architecture is implemented
4. 100% Compose migration is done

## Historical Context

These scripts were part of the major architectural transformation:
- **Before**: Monolithic structure, mixed XML/Compose, direct SDK coupling
- **After**: Clean Architecture, 100% Compose, proper abstraction layers

## Do Not Use

These scripts should not be executed as they were designed for one-time migrations that have already been completed. Running them again could cause issues.

For current development workflows, see the main scripts directory:
- `../README.md` - Current script documentation
- `../ircamera.sh` - Master script entry point
