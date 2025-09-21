# Changelog

All notable changes to the IRCamera project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive feasibility analysis for thermal modules merger
- THERMAL_MODULES_ANALYSIS.md documenting merger strategy
- Documentation for thermal, thermal-ir, and thermal-lite functionality comparison

### Analysis
- thermal module: Basic thermal imaging (38 files, simple menu interface)
- thermal-ir module: Advanced IR with dual-camera fusion (152 files, comprehensive features)  
- thermal-lite module: Lightweight USB camera control (33 files, direct hardware access)

### Recommendations
- Partial merger feasible through shared components extraction
- Hardware abstraction layer implementation recommended
- Maintain separate modules for hardware-specific functionality
- Create thermal-common module for shared utilities

## [Previous]
- Existing thermal imaging capabilities across three specialized modules
- Hub-and-Spoke architecture with PC Controller and Android nodes
- Multi-modal sensor integration (thermal, GSR, RGB)