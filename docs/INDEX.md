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
4. Read [ANTI_PATTERNS_CHECKLIST.md](ANTI_PATTERNS_CHECKLIST.md) for coding standards

### For Thesis Writing
1. Navigate to [thesis/](thesis/) directory
2. Review chapter-specific content
3. Check [thesis/diagrams/INDEX.md](thesis/diagrams/INDEX.md) for figures
4. See [thesis/evaluation/](thesis/evaluation/) for test results

### For Code Review
1. [ANTI_PATTERNS_CHECKLIST.md](ANTI_PATTERNS_CHECKLIST.md) - Quick checklist
2. [ANTI_PATTERNS_ANALYSIS.md](ANTI_PATTERNS_ANALYSIS.md) - Detailed analysis
3. [ANR_PREVENTION_GUIDE.md](ANR_PREVENTION_GUIDE.md) - Performance guidelines
4. [maintenance/](maintenance/) - Previous fixes and patterns

### For System Integration
1. [android/pc-networking-guide.md](android/pc-networking-guide.md) - Networking setup
2. [../pc-controller/docs/PROTOCOL_BRIDGE_GUIDE.md](../pc-controller/docs/PROTOCOL_BRIDGE_GUIDE.md) - Protocol details
3. [../pc-controller/docs/INTEGRATION_READY.md](../pc-controller/docs/INTEGRATION_READY.md) - Integration status

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
├── ANTI_PATTERNS_README.md       # Anti-patterns overview
├── ANTI_PATTERNS_ANALYSIS.md     # Detailed analysis
├── ANTI_PATTERNS_CHECKLIST.md    # Developer checklist
├── ANTI_PATTERNS_ACTION_PLAN.md  # Remediation plan
├── ANR_PREVENTION_GUIDE.md       # ANR prevention
├── ANR_FIX_SUMMARY.md            # ANR fixes
├── THIRD_PARTY_MIGRATION_STATUS.md
├── ANDROIDX_ALTERNATIVES_TO_UTILCODE.md
├── UTILCODE_LIBRARY_ANALYSIS.md
├── UTILCODE_NEXT_STEPS.md
├── UTILCODE_PROGRESS_TRACKER.md
├── UIConsistencyReview.md
├── MaterialIconsGuide.md
├── MaterialIconsExamples.md
├── IconMigrationSummary.md
└── IconConversionComplete.md
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
