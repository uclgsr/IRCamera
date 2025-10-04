# IRCamera Documentation Index

Complete index of all documentation in the IRCamera repository.

## Overview

This repository contains documentation for the IRCamera multi-sensor recording system, including Android application, PC controller, thesis content, and maintenance records.

## Documentation Categories

### 1. Android Application ([android/](android/))
Documentation for the Android application component.
- PC-Android networking protocols
- Time synchronization
- Implementation details

### 2. PC Controller ([../pc-controller/docs/](../pc-controller/docs/))
Documentation for the PC controller application.
- Protocol bridge implementation
- Integration guides
- Code reviews and verification

### 3. Thesis Content ([thesis/](thesis/))
All thesis-related content including chapters, diagrams, and evaluation tests.
- Chapter 3: System Design and Architecture
- Chapter 4: Implementation
- Chapter 5: Experimental Evaluation  
- Chapter 6: Requirements Evaluation
- Thesis diagrams and figures
- Evaluation test suite

### 4. Implementation Summaries ([summaries/](summaries/))
High-level summaries of major implementations and analyses.
- PC networking implementation
- Android system analysis
- Testing procedures
- Planning documents

### 5. Maintenance Records ([maintenance/](maintenance/))
Documentation of migrations, fixes, and maintenance activities.
- AndroidX migration
- Bug fix summaries
- Feature implementations
- Code pattern analyses

### 6. Code Quality (Root Level)
Anti-patterns analysis and code quality guidelines.
- Anti-patterns analysis and checklist
- ANR prevention guide
- Migration status tracking
- UI/UX documentation

## Quick Access

### For New Developers
1. Start with [README.md](README.md) for overview
2. Review [android/README.md](android/README.md) for Android app
3. Check [../pc-controller/README.md](../pc-controller/README.md) for PC controller
4. Read [anti-patterns-checklist.md](anti-patterns-checklist.md) for coding standards

### For Thesis Writing
1. Navigate to [thesis/](thesis/) directory
2. Review chapter-specific content
3. Check [thesis/diagrams/index.md](thesis/diagrams/index.md) for figures
4. See [thesis/evaluation/](thesis/evaluation/) for test results

### For Code Review
1. [anti-patterns-checklist.md](anti-patterns-checklist.md) - Quick checklist
2. [anti-patterns-analysis.md](anti-patterns-analysis.md) - Detailed analysis
3. [anr-prevention-guide.md](anr-prevention-guide.md) - Performance guidelines
4. [maintenance/](maintenance/) - Previous fixes and patterns

### For System Integration
1. [android/pc-networking-guide.md](android/pc-networking-guide.md) - Networking setup
2. [../pc-controller/docs/protocol-bridge-guide.md](../pc-controller/docs/protocol-bridge-guide.md) - Protocol details
3. [../pc-controller/docs/integration-ready.md](../pc-controller/docs/integration-ready.md) - Integration status

## Directory Tree

```
docs/
├── INDEX.md                      # This file
├── README.md                     # Main documentation overview
│
├── android/                      # Android application docs
│   ├── README.md
│   ├── pc-networking-guide.md
│   ├── pc-networking-changes.md
│   ├── pc-networking-verification.md
│   └── time-sync-flow-diagram.txt
│
├── summaries/                    # Implementation summaries
│   ├── README.md
│   ├── pc-networking-implementation-summary.md
│   ├── android-analysis-summary.md
│   ├── testing-time-sync.md
│   ├── resolution-summary.md
│   └── next-steps.md
│
├── maintenance/                  # Maintenance and fixes
│   ├── README.md
│   ├── migration-complete-summary.md
│   ├── code-review-fixes.md
│   ├── rgb-camera-fixes-summary.md
│   ├── ripple-fix-summary.md
│   ├── time-sync-implementation-summary.md
│   └── deprecated-java-patterns-analysis.md
│
├── thesis/                       # Thesis content
│   ├── README.md
│   ├── requirements.txt
│   ├── chapter3/                 # System design
│   ├── chapter4/                 # Implementation
│   ├── chapter5/                 # Experimental evaluation
│   ├── chapter6/                 # Requirements evaluation
│   ├── diagrams/                 # All figures
│   │   ├── INDEX.md
│   │   └── [various .md files]
│   ├── evaluation/               # Test suite
│   │   ├── README.md
│   │   ├── tests/
│   │   └── [test files]
│   └── chapter4-documentation-summary.md
│
├── navigation/                   # Navigation component docs
│   └── README.md
│
├── latex/                        # LaTeX source files
│   └── [thesis .tex files]
│
├── anti-patterns-readme.md       # Anti-patterns overview
├── anti-patterns-analysis.md     # Detailed analysis
├── anti-patterns-checklist.md    # Developer checklist
├── anti-patterns-action-plan.md  # Remediation plan
├── anr-prevention-guide.md       # ANR prevention
├── anr-fix-summary.md            # ANR fixes
├── third-party-migration-status.md
├── androidx-alternatives-to-utilcode.md
├── utilcode-library-analysis.md
├── utilcode-next-steps.md
├── utilcode-progress-tracker.md
├── ui-consistency-review.md
├── material-icons-guide.md
├── material-icons-examples.md
├── icon-migration-summary.md
└── icon-conversion-complete.md
```

## File Count

- Total directories: 13
- Android docs: 4 files
- Summaries: 5 files  
- Maintenance: 6 files
- Thesis chapters: 4 chapter directories
- Thesis diagrams: 15+ diagram files
- Thesis evaluation: 50+ test files
- Code quality: 15+ documentation files

## Maintenance

When adding new documentation:
1. Place in appropriate category directory
2. Add entry to category README.md
3. Update this INDEX.md if adding new categories
4. Follow naming conventions (lowercase with hyphens)
5. Update cross-references as needed

## Related Documentation

### PC Controller
See [../pc-controller/docs/](../pc-controller/docs/) for comprehensive PC controller documentation.

### Source Code
- Android: [../app/src/main/java/mpdc4gsr/](../app/src/main/java/mpdc4gsr/)
- Components: [../component/](../component/)
- Libraries: [../libunified/](../libunified/)

---

Last Updated: 2024-10-04
Organization: University College London (UCL)
Project: IRCamera Multi-Sensor Recording System
