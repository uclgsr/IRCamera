# Migration Scripts Archive

This directory contains historical migration scripts that were used during the Compose migration.

## Status: ARCHIVED

These scripts served their purpose during the migration from XML-based Android Views to Jetpack Compose. The migration is now **95% complete** and these scripts are no longer actively used.

## Scripts

1. **cleanup_component_manifests.py** - Cleaned up AndroidManifest.xml files in component modules
2. **cleanup_manifests.py** - Cleaned up main app AndroidManifest.xml
3. **migrate_fragments.py** - Migrated Fragment implementations to Compose
4. **migrate_remaining_modules.py** - Migrated remaining modules to Compose
5. **migrate_thermal_activities.py** - Migrated thermal camera activities to Compose
6. **migrate_to_backup.py** - Moved legacy files to backup directory
7. **migrate_viewmodels.py** - Migrated ViewModels to modern patterns

## Why Archived?

- Migration is 95% complete
- Scripts were one-time use tools
- No longer needed for ongoing development
- Kept for historical reference and documentation

## If You Need Them

These scripts are preserved here for:
- Understanding migration process
- Documentation purposes
- Historical reference
- Potential future similar migrations

## Current Migration Status

See `COMPOSE_MIGRATION_STATUS_REPORT.md` in the repository root for current status:
- 95% migration complete
- 100% user-facing activities migrated
- Code quality: 8.9/10
- Production ready

## Backup Files

The `backup/` directory was removed as all content is safely stored in git history. To access old implementations:

```bash
# View files from before backup removal
git show HEAD~1:backup/

# Restore a specific file
git show HEAD~1:backup/path/to/file.kt > restored_file.kt
```

---

*These scripts represent an important phase in the project's modernization but are no longer part of active development.*
