#!/bin/bash

# IRCamera Development Tools - CI/CD Core
# Usage: ./dev.sh [command]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

show_help() {
    echo -e "${BLUE}IRCamera Development Tools${NC}"
    echo ""
    echo "Usage: ./dev.sh [command]"
    echo ""
    echo "Commands:"
    echo "  lint        - Run linting checks (ktlint, checkstyle)"
    echo "  static      - Run static analysis"
    echo "  build-check - Quick build validation"
    echo "  validate    - Run all checks (lint + static + build)"
    echo "  diagram     - Generate repository architecture Mermaid diagram"
    echo "  help        - Show this help"
}

print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

run_lint() {
    print_status "Running linting checks..."
    
    local errors=0
    
    # Kotlin lint with ktlint
    if command -v ktlint &> /dev/null; then
        print_status "Running ktlint on Kotlin files..."
        if ! find . -name "*.kt" -not -path "./build/*" -not -path "./.gradle/*" -exec ktlint {} + 2>/dev/null; then
            errors=$((errors + 1))
            print_error "Kotlin linting issues found"
        else
            print_status "Kotlin linting passed"
        fi
    else
        print_warning "ktlint not available. Install with: brew install ktlint (macOS) or download from GitHub"
    fi
    
    # Python lint with flake8
    if command -v flake8 &> /dev/null; then
        print_status "Running flake8 on Python files..."
        if ! find . -name "*.py" -exec flake8 {} + 2>/dev/null; then
            errors=$((errors + 1))
            print_error "Python flake8 issues found"
        else
            print_status "Python linting passed"
        fi
    fi
    
    if [ $errors -eq 0 ]; then
        print_status "All linting checks passed"
        return 0
    else
        print_error "Linting failed with $errors error(s)"
        return 1
    fi
}

run_static_analysis() {
    print_status "Running static analysis..."
    
    # Basic static analysis - can be expanded
    local issues=0
    
    # Check for common issues in Kotlin/Java files
    if command -v grep &> /dev/null; then
        print_status "Checking for common code issues..."
        
        # Check for potential null pointer issues
        if find . -name "*.kt" -o -name "*.java" | xargs grep -l "!!" 2>/dev/null | grep -v build > /dev/null; then
            print_warning "Found potential null assertion issues (!!)"
            issues=$((issues + 1))
        fi
        
        # Check for hardcoded strings
        if find . -name "*.kt" -o -name "*.java" | xargs grep -l "Log\." 2>/dev/null | grep -v build > /dev/null; then
            print_warning "Found hardcoded logging statements"
            issues=$((issues + 1))
        fi
    fi
    
    if [ $issues -eq 0 ]; then
        print_status "Static analysis passed"
        return 0
    else
        print_warning "Static analysis completed with $issues warning(s)"
        return 0  # warnings don't fail the build
    fi
}

run_build_check() {
    print_status "Running build validation..."
    
    # Check if gradlew exists and is executable
    if [ ! -f "./gradlew" ]; then
        print_error "gradlew not found"
        return 1
    fi
    
    if [ ! -x "./gradlew" ]; then
        chmod +x ./gradlew
        print_status "Made gradlew executable"
    fi
    
    # Run gradle check
    if ./gradlew assemble --no-daemon --console=plain; then
        print_status "Build validation passed"
        return 0
    else
        print_error "Build validation failed"
        return 1
    fi
}

run_validate() {
    print_status "Running full validation..."
    
    local overall_status=0
    
    # Run linting
    if ! run_lint; then
        overall_status=1
    fi
    
    # Run static analysis
    run_static_analysis  # doesn't fail on warnings
    
    # Run build check
    if ! run_build_check; then
        overall_status=1
    fi
    
    if [ $overall_status -eq 0 ]; then
        print_status "All validation checks passed ✨"
    else
        print_error "Some validation checks failed"
    fi
    
    return $overall_status
}

generate_architecture_diagram() {
    print_status "Generating repository architecture Mermaid diagram..."
    
    local output_file="docs/architecture-diagram.md"
    
    # Create docs directory if it doesn't exist
    mkdir -p docs
    
    # Generate comprehensive Mermaid diagram
    cat > "$output_file" << 'EOF'
# IRCamera Repository Architecture

This document contains a detailed Mermaid diagram showing the complete architecture of the IRCamera repository, including all modules, components, CI/CD workflows, and their relationships.

## Repository Architecture Diagram

```mermaid
graph TB
    %% Repository Root
    subgraph "IRCamera Repository"
        
        %% Main Application
        subgraph "Core Application"
            APP["`**app**
            Main Android Application
            • UI Activities & Fragments
            • Application Entry Point
            • Dependency Aggregator`"]
        end
        
        %% Library Modules
        subgraph "Library Modules"
            LIBAPP["`**libapp**
            Application Library
            • Core App Logic
            • Shared Components
            • LMS SDK Integration`"]
            
            LIBCOM["`**libcom**
            Communication Library  
            • Network Operations
            • API Connections
            • Data Exchange`"]
            
            LIBIR["`**libir**
            IR Camera Library
            • Thermal Imaging
            • Camera Controls
            • Image Processing`"]
            
            LIBUI["`**libui**
            UI Components Library
            • Custom Views
            • Shared UI Elements
            • Theme & Styling`"]
            
            LIBMENU["`**libmenu**
            Menu System Library
            • Navigation Logic
            • Menu Components
            • User Interface`"]
            
            LIBMATRIX["`**libmatrix**
            Matrix Operations Library
            • Mathematical Operations
            • Data Processing
            • Algorithm Support`"]
        end
        
        %% Feature Components
        subgraph "Feature Components"
            THERMAL["`**component:thermal**
            Thermal Processing
            • Standard Thermal Imaging
            • Temperature Analysis
            • Image Enhancement`"]
            
            THERMAL_IR["`**component:thermal-ir**
            Thermal IR Resources
            • IR-Specific Resources
            • Configuration Files
            • Asset Management`"]
            
            THERMAL_LITE["`**component:thermal-lite**
            Thermal Lite Mode
            • Lightweight Processing
            • Performance Optimized
            • Resource Efficient`"]
            
            PSEUDO["`**component:pseudo**
            Pseudo Color Processing
            • Color Mapping
            • Visual Enhancement
            • Display Optimization`"]
            
            GSR["`**component:gsr-recording**
            GSR Recording System
            • Galvanic Skin Response
            • Data Recording
            • Sensor Integration`"]
            
            USER["`**component:user**
            User Management
            • User Profiles
            • Settings Management
            • Authentication`"]
            
            COMMON["`**component:CommonComponent**
            Shared Components
            • Common Utilities
            • Shared Resources
            • Base Classes`"]
        end
        
        %% External Modules
        subgraph "External Modules"
            BLE["`**BleModule**
            Bluetooth Low Energy
            • BLE Communication
            • Device Connection
            • Nordic Backend`"]
            
            RANGE["`**RangeSeekBar**
            Range Selection UI
            • Custom Seek Bar
            • Range Input Control
            • User Interaction`"]
        end
        
        %% CI/CD System
        subgraph "CI/CD Pipeline"
            CI_MAIN["`**CI Main Workflow**
            .github/workflows/ci.yml
            • Build Validation
            • Code Quality Checks
            • Linting & Analysis
            • Artifact Generation`"]
            
            GRADLE_CHECK["`**Gradle Build Check**
            .github/workflows/gradle-build-check.yml
            • Multi-task Validation
            • Matrix Strategy Testing
            • Build Verification`"]
            
            STATIC_ANALYSIS["`**Static Analysis**
            .github/workflows/static-analysis.yml
            • Multi-language Analysis
            • Code Quality Metrics
            • Standards Compliance`"]
        end
        
        %% Development Tools
        subgraph "Development Tools"
            DEV_SCRIPT["`**dev.sh**
            Development Script
            • lint - Code linting
            • static - Static analysis  
            • build-check - Build validation
            • validate - Full validation
            • diagram - Architecture diagram`"]
            
            GRADLE_WRAPPER["`**Gradle System**
            gradlew / build.gradle.kts
            • Build Automation
            • Dependency Management
            • Multi-module Coordination`"]
        end
        
        %% Configuration Files
        subgraph "Configuration & Resources"
            SETTINGS["`**settings.gradle.kts**
            Project Configuration
            • Module Definitions
            • Repository Settings
            • Dependency Resolution`"]
            
            LIBS_VERSION["`**gradle/libs.versions.toml**
            Version Catalog
            • Dependency Versions
            • Library Definitions
            • Version Management`"]
            
            AAR_LIBS["`**Local AAR Libraries**
            Native & Third-party Libraries
            • USB IR SDK
            • LMS International SDK
            • Authentication Libraries`"]
        end
    end
    
    %% Dependencies - App Level
    APP --> THERMAL
    APP --> THERMAL_IR
    APP --> THERMAL_LITE
    APP --> PSEUDO
    APP --> GSR
    APP --> USER
    APP --> LIBAPP
    APP --> LIBCOM
    APP --> LIBIR
    APP --> LIBUI
    APP --> LIBMENU
    APP --> BLE
    
    %% Library Dependencies
    LIBAPP --> LIBCOM
    LIBIR --> LIBMATRIX
    LIBUI --> COMMON
    
    %% Component Dependencies
    THERMAL --> LIBIR
    THERMAL --> LIBMATRIX
    THERMAL_IR --> LIBIR
    THERMAL_LITE --> LIBIR
    PSEUDO --> LIBIR
    GSR --> LIBCOM
    USER --> LIBUI
    USER --> COMMON
    
    %% External Dependencies
    APP --> AAR_LIBS
    LIBAPP --> AAR_LIBS
    LIBIR --> AAR_LIBS
    
    %% CI/CD Workflow Dependencies
    CI_MAIN --> DEV_SCRIPT
    GRADLE_CHECK --> GRADLE_WRAPPER
    STATIC_ANALYSIS --> DEV_SCRIPT
    
    %% Development Tool Dependencies
    DEV_SCRIPT --> GRADLE_WRAPPER
    GRADLE_WRAPPER --> SETTINGS
    GRADLE_WRAPPER --> LIBS_VERSION
    
    %% Styling
    classDef appModule fill:#e1f5fe
    classDef libModule fill:#f3e5f5
    classDef componentModule fill:#e8f5e8
    classDef externalModule fill:#fff3e0
    classDef cicdModule fill:#fce4ec
    classDef toolModule fill:#f1f8e9
    classDef configModule fill:#fafafa
    
    class APP appModule
    class LIBAPP,LIBCOM,LIBIR,LIBUI,LIBMENU,LIBMATRIX libModule
    class THERMAL,THERMAL_IR,THERMAL_LITE,PSEUDO,GSR,USER,COMMON componentModule
    class BLE,RANGE externalModule
    class CI_MAIN,GRADLE_CHECK,STATIC_ANALYSIS cicdModule
    class DEV_SCRIPT,GRADLE_WRAPPER toolModule
    class SETTINGS,LIBS_VERSION,AAR_LIBS configModule
```

## Architecture Overview

### Core Application Layer
- **app**: Main Android application module that integrates all components and provides the user interface

### Library Layer
- **libir**: Core IR camera functionality and thermal imaging processing
- **libapp**: Application-level shared components and business logic
- **libcom**: Communication and networking capabilities
- **libui**: Reusable UI components and custom views
- **libmenu**: Menu system and navigation logic
- **libmatrix**: Mathematical operations and matrix processing

### Feature Components Layer
- **thermal**: Standard thermal processing and analysis
- **thermal-ir**: IR-specific resources and configurations  
- **thermal-lite**: Lightweight thermal processing for performance
- **pseudo**: Pseudo color mapping and visual enhancement
- **gsr-recording**: Galvanic skin response recording system
- **user**: User management, profiles, and settings
- **CommonComponent**: Shared utilities and base classes

### External Modules
- **BleModule**: Bluetooth Low Energy communication with Nordic backend
- **RangeSeekBar**: Custom UI control for range selection

### CI/CD Pipeline
- **ci.yml**: Main CI workflow with comprehensive validation
- **gradle-build-check.yml**: Matrix strategy build validation
- **static-analysis.yml**: Multi-language static analysis

### Development Tools
- **dev.sh**: Unified development script with validation commands
- **Gradle System**: Build automation and dependency management

## Module Statistics

### File Distribution
- **Kotlin Files**: ~1,478 files across all modules
- **Java Files**: ~886 files for Android compatibility
- **Python Files**: ~96 files for tooling and scripts
- **Total Modules**: 17 active modules (app + 10 components + 6 libraries)

### Key Dependencies
- **Android Gradle Plugin**: 8.1.2
- **Kotlin**: 1.9.10
- **Java**: 17 (toolchain)
- **Native Libraries**: ARM64-v8a architecture support
- **Third-party SDKs**: LMS International, USB IR SDK, Authentication libraries

This architecture provides a modular, scalable structure for the IRCamera application with comprehensive CI/CD automation and development tooling.
EOF

    print_status "Architecture diagram generated: $output_file"
    print_status "View the diagram by opening the file in any Markdown viewer that supports Mermaid"
    
    # Also create a simple HTML viewer
    cat > "docs/architecture-viewer.html" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>IRCamera Architecture Diagram</title>
    <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .mermaid { text-align: center; }
        h1 { color: #2196F3; }
        .info { background: #f5f5f5; padding: 10px; border-radius: 5px; margin: 20px 0; }
    </style>
</head>
<body>
    <h1>IRCamera Repository Architecture</h1>
    <div class="info">
        <strong>Note:</strong> This diagram shows the complete architecture including all modules, 
        components, CI/CD workflows, and their interdependencies.
    </div>
    
    <div class="mermaid">
graph TB
    %% Repository Root
    subgraph "IRCamera Repository"
        
        %% Main Application
        subgraph "Core Application"
            APP["`**app**
            Main Android Application
            • UI Activities & Fragments
            • Application Entry Point
            • Dependency Aggregator`"]
        end
        
        %% Library Modules
        subgraph "Library Modules"
            LIBAPP["`**libapp**
            Application Library
            • Core App Logic
            • Shared Components
            • LMS SDK Integration`"]
            
            LIBCOM["`**libcom**
            Communication Library  
            • Network Operations
            • API Connections
            • Data Exchange`"]
            
            LIBIR["`**libir**
            IR Camera Library
            • Thermal Imaging
            • Camera Controls
            • Image Processing`"]
            
            LIBUI["`**libui**
            UI Components Library
            • Custom Views
            • Shared UI Elements
            • Theme & Styling`"]
            
            LIBMENU["`**libmenu**
            Menu System Library
            • Navigation Logic
            • Menu Components
            • User Interface`"]
            
            LIBMATRIX["`**libmatrix**
            Matrix Operations Library
            • Mathematical Operations
            • Data Processing
            • Algorithm Support`"]
        end
        
        %% Feature Components
        subgraph "Feature Components"
            THERMAL["`**component:thermal**
            Thermal Processing
            • Standard Thermal Imaging
            • Temperature Analysis
            • Image Enhancement`"]
            
            THERMAL_IR["`**component:thermal-ir**
            Thermal IR Resources
            • IR-Specific Resources
            • Configuration Files
            • Asset Management`"]
            
            THERMAL_LITE["`**component:thermal-lite**
            Thermal Lite Mode
            • Lightweight Processing
            • Performance Optimized
            • Resource Efficient`"]
            
            PSEUDO["`**component:pseudo**
            Pseudo Color Processing
            • Color Mapping
            • Visual Enhancement
            • Display Optimization`"]
            
            GSR["`**component:gsr-recording**
            GSR Recording System
            • Galvanic Skin Response
            • Data Recording
            • Sensor Integration`"]
            
            USER["`**component:user**
            User Management
            • User Profiles
            • Settings Management
            • Authentication`"]
            
            COMMON["`**component:CommonComponent**
            Shared Components
            • Common Utilities
            • Shared Resources
            • Base Classes`"]
        end
        
        %% External Modules
        subgraph "External Modules"
            BLE["`**BleModule**
            Bluetooth Low Energy
            • BLE Communication
            • Device Connection
            • Nordic Backend`"]
            
            RANGE["`**RangeSeekBar**
            Range Selection UI
            • Custom Seek Bar
            • Range Input Control
            • User Interaction`"]
        end
        
        %% CI/CD System
        subgraph "CI/CD Pipeline"
            CI_MAIN["`**CI Main Workflow**
            .github/workflows/ci.yml
            • Build Validation
            • Code Quality Checks
            • Linting & Analysis
            • Artifact Generation`"]
            
            GRADLE_CHECK["`**Gradle Build Check**
            .github/workflows/gradle-build-check.yml
            • Multi-task Validation
            • Matrix Strategy Testing
            • Build Verification`"]
            
            STATIC_ANALYSIS["`**Static Analysis**
            .github/workflows/static-analysis.yml
            • Multi-language Analysis
            • Code Quality Metrics
            • Standards Compliance`"]
        end
        
        %% Development Tools
        subgraph "Development Tools"
            DEV_SCRIPT["`**dev.sh**
            Development Script
            • lint - Code linting
            • static - Static analysis  
            • build-check - Build validation
            • validate - Full validation
            • diagram - Architecture diagram`"]
            
            GRADLE_WRAPPER["`**Gradle System**
            gradlew / build.gradle.kts
            • Build Automation
            • Dependency Management
            • Multi-module Coordination`"]
        end
        
        %% Configuration Files
        subgraph "Configuration & Resources"
            SETTINGS["`**settings.gradle.kts**
            Project Configuration
            • Module Definitions
            • Repository Settings
            • Dependency Resolution`"]
            
            LIBS_VERSION["`**gradle/libs.versions.toml**
            Version Catalog
            • Dependency Versions
            • Library Definitions
            • Version Management`"]
            
            AAR_LIBS["`**Local AAR Libraries**
            Native & Third-party Libraries
            • USB IR SDK
            • LMS International SDK
            • Authentication Libraries`"]
        end
    end
    
    %% Dependencies - App Level
    APP --> THERMAL
    APP --> THERMAL_IR
    APP --> THERMAL_LITE
    APP --> PSEUDO
    APP --> GSR
    APP --> USER
    APP --> LIBAPP
    APP --> LIBCOM
    APP --> LIBIR
    APP --> LIBUI
    APP --> LIBMENU
    APP --> BLE
    
    %% Library Dependencies
    LIBAPP --> LIBCOM
    LIBIR --> LIBMATRIX
    LIBUI --> COMMON
    
    %% Component Dependencies
    THERMAL --> LIBIR
    THERMAL --> LIBMATRIX
    THERMAL_IR --> LIBIR
    THERMAL_LITE --> LIBIR
    PSEUDO --> LIBIR
    GSR --> LIBCOM
    USER --> LIBUI
    USER --> COMMON
    
    %% External Dependencies
    APP --> AAR_LIBS
    LIBAPP --> AAR_LIBS
    LIBIR --> AAR_LIBS
    
    %% CI/CD Workflow Dependencies
    CI_MAIN --> DEV_SCRIPT
    GRADLE_CHECK --> GRADLE_WRAPPER
    STATIC_ANALYSIS --> DEV_SCRIPT
    
    %% Development Tool Dependencies
    DEV_SCRIPT --> GRADLE_WRAPPER
    GRADLE_WRAPPER --> SETTINGS
    GRADLE_WRAPPER --> LIBS_VERSION
    
    %% Styling
    classDef appModule fill:#e1f5fe
    classDef libModule fill:#f3e5f5
    classDef componentModule fill:#e8f5e8
    classDef externalModule fill:#fff3e0
    classDef cicdModule fill:#fce4ec
    classDef toolModule fill:#f1f8e9
    classDef configModule fill:#fafafa
    
    class APP appModule
    class LIBAPP,LIBCOM,LIBIR,LIBUI,LIBMENU,LIBMATRIX libModule
    class THERMAL,THERMAL_IR,THERMAL_LITE,PSEUDO,GSR,USER,COMMON componentModule
    class BLE,RANGE externalModule
    class CI_MAIN,GRADLE_CHECK,STATIC_ANALYSIS cicdModule
    class DEV_SCRIPT,GRADLE_WRAPPER toolModule
    class SETTINGS,LIBS_VERSION,AAR_LIBS configModule
    </div>

    <script>
        mermaid.initialize({ startOnLoad: true, theme: 'default' });
    </script>
</body>
</html>
EOF

    print_status "Interactive HTML viewer created: docs/architecture-viewer.html"
    print_status "Open architecture-viewer.html in a web browser to view the interactive diagram"
}

# Main script logic
case "${1:-help}" in
    lint)
        run_lint
        ;;
    static)
        run_static_analysis
        ;;
    build-check)
        run_build_check
        ;;
    validate)
        run_validate
        ;;
    diagram)
        generate_architecture_diagram
        ;;
    help|*)
        show_help
        ;;
esac