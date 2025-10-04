# Quick Navigation Guide

Fast reference for finding documentation in the IRCamera repository.

## Start Here

- **New to IRCamera?** → [/README.md](../README.md)
- **Looking for specific docs?** → [INDEX.md](INDEX.md)
- **Want an overview?** → [README.md](README.md)

## By Role

### Android Developer
1. [developer-guides/ui-components-guide.md](developer-guides/ui-components-guide.md) - Permissions system
2. [developer-guides/logging-utilities-guide.md](developer-guides/logging-utilities-guide.md) - Logging and error handling
3. [android/pc-networking-guide.md](android/pc-networking-guide.md) - PC-Android communication
4. [anti-patterns-checklist.md](anti-patterns-checklist.md) - Code quality checklist
5. [anr-prevention-guide.md](anr-prevention-guide.md) - Performance guidelines

### PC Controller Developer
1. [../pc-controller/docs/quick-start.md](../pc-controller/docs/quick-start.md) - Getting started
2. [../pc-controller/docs/protocol-bridge-guide.md](../pc-controller/docs/protocol-bridge-guide.md) - Protocol details
3. [../pc-controller/docs/implementation-summary.md](../pc-controller/docs/implementation-summary.md) - Complete implementation

### Thesis Writer
1. [thesis/README.md](thesis/README.md) - Thesis content overview
2. [thesis/diagrams/index.md](thesis/diagrams/index.md) - All figures
3. [thesis/evaluation/README.md](thesis/evaluation/README.md) - Test suite

### Code Reviewer
1. [anti-patterns-checklist.md](anti-patterns-checklist.md) - Quick checklist
2. [anti-patterns-analysis.md](anti-patterns-analysis.md) - Detailed analysis
3. [maintenance/](maintenance/) - Previous fixes and patterns

### Project Manager
1. [summaries/next-steps.md](summaries/next-steps.md) - Planned work
2. [reorganization-summary.md](reorganization-summary.md) - Recent reorganization
3. [maintenance/](maintenance/) - Maintenance history

## By Topic

### Networking
- [android/pc-networking-guide.md](android/pc-networking-guide.md)
- [android/pc-networking-changes.md](android/pc-networking-changes.md)
- [android/pc-networking-verification.md](android/pc-networking-verification.md)
- [../pc-controller/docs/protocol-bridge-guide.md](../pc-controller/docs/protocol-bridge-guide.md)

### Time Synchronization
- [android/time-sync-flow-diagram.txt](android/time-sync-flow-diagram.txt)
- [maintenance/time-sync-implementation-summary.md](maintenance/time-sync-implementation-summary.md)
- [summaries/testing-time-sync.md](summaries/testing-time-sync.md)

### Developer Guides
- [developer-guides/ui-components-guide.md](developer-guides/ui-components-guide.md) - Permissions handling system
- [developer-guides/logging-utilities-guide.md](developer-guides/logging-utilities-guide.md) - Logging and error handling
- [developer-guides/permission-handling-guide.md](developer-guides/permission-handling-guide.md) - Permission tools API

### Code Quality
- [anti-patterns-readme.md](anti-patterns-readme.md) - Overview
- [anti-patterns-analysis.md](anti-patterns-analysis.md) - Detailed analysis
- [anti-patterns-checklist.md](anti-patterns-checklist.md) - Developer checklist
- [anti-patterns-action-plan.md](anti-patterns-action-plan.md) - Remediation plan

### Performance
- [anr-prevention-guide.md](anr-prevention-guide.md) - ANR prevention
- [anr-fix-summary.md](anr-fix-summary.md) - Previous ANR fixes

### Migration & Maintenance
- [maintenance/migration-complete-summary.md](maintenance/migration-complete-summary.md) - AndroidX migration
- [third-party-migration-status.md](third-party-migration-status.md) - Library status
- [maintenance/deprecated-java-patterns-analysis.md](maintenance/deprecated-java-patterns-analysis.md) - Legacy code

### UI/UX
- [ui-consistency-review.md](ui-consistency-review.md)
- [material-icons-guide.md](material-icons-guide.md)
- [icon-migration-summary.md](icon-migration-summary.md)

### Testing
- [thesis/evaluation/README.md](thesis/evaluation/README.md) - Test suite overview
- [thesis/evaluation/quick-start.md](thesis/evaluation/quick-start.md) - Quick start
- [../pc-controller/tests/README.md](../pc-controller/tests/README.md) - PC controller tests

### Thesis Content
- [thesis/chapter3/](thesis/chapter3/) - System design
- [thesis/chapter5/](thesis/chapter5/) - Experimental evaluation
- [thesis/chapter6/](thesis/chapter6/) - Requirements evaluation
- [thesis/diagrams/](thesis/diagrams/) - All figures

## By File Type

### Getting Started Guides
- [/README.md](../README.md) - Repository overview
- [../pc-controller/docs/quick-start.md](../pc-controller/docs/quick-start.md) - PC controller quickstart
- [thesis/evaluation/quick-start.md](thesis/evaluation/quick-start.md) - Test suite quickstart

### Implementation Details
- [summaries/pc-networking-implementation-summary.md](summaries/pc-networking-implementation-summary.md)
- [summaries/android-analysis-summary.md](summaries/android-analysis-summary.md)
- [../pc-controller/docs/implementation-summary.md](../pc-controller/docs/implementation-summary.md)

### Architecture & Design
- [thesis/chapter3/system-architecture.md](thesis/chapter3/system-architecture.md)
- [thesis/chapter3/state-machine.md](thesis/chapter3/state-machine.md)
- [thesis/diagrams/android-architecture-diagram.md](thesis/diagrams/android-architecture-diagram.md)

### API & Protocols
- [../pc-controller/docs/protocol-bridge-guide.md](../pc-controller/docs/protocol-bridge-guide.md)
- [../pc-controller/docs/protocol-flow.txt](../pc-controller/docs/protocol-flow.txt)
- [android/time-sync-flow-diagram.txt](android/time-sync-flow-diagram.txt)

### Troubleshooting
- [anr-prevention-guide.md](anr-prevention-guide.md)
- [anti-patterns-checklist.md](anti-patterns-checklist.md)
- [../pc-controller/docs/verification-report.md](../pc-controller/docs/verification-report.md)

## Common Tasks

### "I need to understand the system architecture"
1. [/README.md](../README.md) - High-level overview
2. [thesis/chapter3/system-architecture.md](thesis/chapter3/system-architecture.md) - Detailed architecture
3. [thesis/diagrams/](thesis/diagrams/) - Visual diagrams

### "I need to set up PC-Android communication"
1. [android/pc-networking-guide.md](android/pc-networking-guide.md) - Android side
2. [../pc-controller/docs/quick-start.md](../pc-controller/docs/quick-start.md) - PC side
3. [../pc-controller/docs/protocol-bridge-guide.md](../pc-controller/docs/protocol-bridge-guide.md) - Protocol details

### "I need to run tests"
1. [thesis/evaluation/README.md](thesis/evaluation/README.md) - Thesis evaluation tests
2. [../pc-controller/tests/README.md](../pc-controller/tests/README.md) - PC controller tests

### "I need to understand a bug fix"
1. [maintenance/](maintenance/) - Browse maintenance records
2. [summaries/resolution-summary.md](summaries/resolution-summary.md) - Resolution summary

### "I need thesis content"
1. [thesis/README.md](thesis/README.md) - Start here
2. [thesis/diagrams/index.md](thesis/diagrams/index.md) - All figures
3. [thesis/evaluation/](thesis/evaluation/) - Test results

## Recent Changes

- **2024-10-04**: Complete documentation reorganization
  - See [reorganization-summary.md](reorganization-summary.md) for details
  - All files now use lowercase-with-hyphens naming
  - Structured hierarchy with comprehensive indices

## Need Help?

- **Can't find something?** → Check [INDEX.md](INDEX.md)
- **Want an overview of a section?** → Look for README.md in that directory
- **Need to understand the structure?** → See [reorganization-summary.md](reorganization-summary.md)

---

*Quick tip: Most directories have a README.md that explains their contents. Start there if you're not sure where to look.*
