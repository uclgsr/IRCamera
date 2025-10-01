# Layout Analysis Scripts

This directory contains scripts for analyzing XML layout files and their Compose equivalents.

## Scripts

### analyze_layouts.py

Basic layout inventory script that:

- Counts all XML layout files
- Lists layouts by category (Activity, Fragment, Dialog, etc.)
- Groups layouts by module
- Provides initial migration statistics

**Usage:**

```bash
python3 scripts/analysis/analyze_layouts.py
```

### detailed_layout_analysis.py

Comprehensive migration analysis script that:

- Maps XML layouts to Compose Activity files
- Uses name-based matching to find equivalents
- Categorizes layouts by type and module
- Identifies missing migrations
- Provides detailed migration status reports

**Usage:**

```bash
python3 scripts/analysis/detailed_layout_analysis.py
```

**Output includes:**

- Migration status by module and category
- List of layouts with Compose equivalents
- List of layouts without equivalents
- Coverage percentages
- Critical missing migrations

## Output

Both scripts output to stdout. You can redirect to a file:

```bash
python3 scripts/analysis/detailed_layout_analysis.py > migration_report.txt
```

## Documentation

Analysis results are documented in:

- `docs/LAYOUT_TO_COMPOSE_MIGRATION_STATUS.md` - Comprehensive analysis
- `docs/LAYOUT_MIGRATION_SUMMARY.md` - Quick reference
- `docs/consolidated/COMPOSE_MIGRATION.md` - Updated migration guide

## Notes

- Scripts analyze files in place, no modifications are made
- Analysis is based on file naming conventions
- Some Compose equivalents may exist as Composable functions rather than separate files
- Fragment migrations often result in Composable functions, not new files
- Dialog and list item layouts often consolidate into fewer Compose files

## Re-running Analysis

Re-run these scripts periodically to track migration progress:

```bash
# Quick check
python3 scripts/analysis/analyze_layouts.py | grep "Migration coverage"

# Full analysis
python3 scripts/analysis/detailed_layout_analysis.py | tee migration_report_$(date +%Y%m%d).txt
```

## Requirements

- Python 3.6+
- Standard library only (no external dependencies)

---

**Created:** 2024  
**Purpose:** Track and document Compose migration progress
