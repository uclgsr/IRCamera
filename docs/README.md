# Documentation Guide

Use this index to navigate the active documentation for MPDC4GSR.

## Core References

- [system-overview.md](system-overview.md) – High-level architecture, core services, and module map.
- [android-platform.md](android-platform.md) – Android application subsystems, feature breakdown, and developer
  workflows.
- [pc-controller.md](pc-controller.md) – Desktop controller architecture and integration points.
- [testing-and-quality.md](testing-and-quality.md) – Testing strategy, automation, and quality gates.

## Developer Guides

The following focused guides live in `docs/developer-guides/`:

- [ui-components-guide.md](developer-guides/ui-components-guide.md) – Compose UI building blocks and permission-aware
  patterns.
- [logging-utilities-guide.md](developer-guides/logging-utilities-guide.md) – Structured logging, crash reporting, and
  diagnostics.
- [permission-handling-guide.md](developer-guides/permission-handling-guide.md) – Using `PermissionTools` and
  platform-specific permission flows.

Additional utility notes reside in `libunified/PERMISSION_HANDLING.md`.

## PC Controller Docs

Detailed documentation for the desktop controller is maintained alongside the code: see [
`pc-controller/docs/`](../pc-controller/docs/).

## Legacy Materials

Older planning documents, migration reports, and thesis chapters remain in the repository under `docs/maintenance/`,
`docs/thesis/`, and `docs/latex/`. They are preserved for historical context but are no longer updated; refer to the
core references above for the current state of the system.

## Contributing to Documentation

- Keep new documents in `docs/` and wire them into this index.
- Prefer ASCII-only markdown for compatibility.
- Update cross-references when directories move or files are removed.
