# Legacy Implementation Files

This directory contains historical implementations that have been superseded by the unified controller approach.

## Archived MVP Implementations

The following MVP files have been moved here from the main directory:

- **mvp_simple.py** - Single-file MVP implementation (~250 lines)
- **run_mvp_app.py** - GUI application launcher
- **run_mvp_cli.py** - CLI MVP version
- **requirements_mvp.txt** - Minimal MVP dependencies
- **config_mvp.yaml** - Basic MVP configuration

**Why archived**: The unified controller (`run_unified_controller.py`) consolidates all MVP functionality with better
dependency management and automatic controller selection.

**For new development**: Use `run_unified_controller.py` in the main pc-controller directory.

## Over-engineered Implementation

- **src/** - 49 Python files, ~2000+ lines of complex architecture
- **native_backend/** - C++ PyBind11 complexity
- **Over-engineered features**: TLS/SSL, complex async patterns, advanced GUI components

Refer to OVER_ENGINEERED_ANALYSIS.md for detailed analysis.

## Historical Context

These files are preserved for:

1. Reference implementation patterns
2. Understanding evolution of the PC Controller
3. Educational purposes
4. Recovery if needed (available in git history)

**Do not use these files for new development** - they are maintained for historical reference only.
