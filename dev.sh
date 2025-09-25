#!/bin/bash




set -e


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

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

lint_code() {
    echo -e "${BLUE}Running linting checks...${NC}"
    
    
    echo "Checking Kotlin files..."
    if ./gradlew ktlintCheck; then
        print_status "Kotlin linting passed"
    else
        print_warning "Kotlin linting issues found"
    fi
    
    
    echo "Checking Java files..."
    if ./gradlew checkstyle; then
        print_status "Java checkstyle passed"
    else
        print_warning "Java checkstyle issues found"
    fi
    
    
    if find . -name "*.py" -type f | head -1 | grep -q .; then
        echo "Checking Python files..."
        if command -v flake8 >/dev/null 2>&1; then
            if find . -name "*.py" -exec flake8 {} +; then
                print_status "Python linting passed"
            else
                print_warning "Python linting issues found"
            fi
        else
            print_warning "flake8 not installed, skipping Python linting"
        fi
    fi
}

static_analysis() {
    echo -e "${BLUE}Running static analysis...${NC}"
    
    
    if find . -name "*.sh" -type f | head -1 | grep -q .; then
        if command -v shellcheck >/dev/null 2>&1; then
            echo "Checking shell scripts..."
            if find . -name "*.sh" -exec shellcheck {} +; then
                print_status "Shell script analysis passed"
            else
                print_warning "Shell script issues found"
            fi
        else
            print_warning "shellcheck not installed, skipping shell analysis"
        fi
    fi
    
    print_status "Static analysis completed"
}

build_check() {
    echo -e "${BLUE}Running build validation...${NC}"
    
    
    if ./gradlew assembleDebug; then
        print_status "Build check passed"
    else
        print_error "Build check failed"
        exit 1
    fi
}

validate_all() {
    echo -e "${BLUE}Running comprehensive validation...${NC}"
    
    lint_code
    static_analysis  
    build_check
    
    print_status "All validation checks completed!"
}

generate_diagram() {
    echo -e "${BLUE}Generating repository architecture diagram...${NC}"
    
    
    mkdir -p docs
    
    
    cat > docs/architecture-diagram.md << 'EOF'


```mermaid
graph TB
    %% Core Application Layer
    subgraph "Application Layer"
        App[APP: app<br/>Main Android Application]
        MainActivity[MainActivity.kt]
        App --> MainActivity
    end
    
    %% Library Layer
    subgraph "Library Layer"
        LibApp[LIB: libapp<br/>Application Library]
        LibCom[LIB: libcom<br/>Communication Library] 
        LibIR[LIB: libir<br/>Infrared Camera Library]
        LibMatrix[LIB: libmatrix<br/>Matrix Operations]
        LibMenu[LIB: libmenu<br/>Menu Components]
        LibUI[LIB: libui<br/>UI Components Library]
    end
    
    %% Component Layer  
    subgraph "Component Layer"
        ThermalIR[THERMAL: thermal-ir<br/>Thermal IR Component]
        Thermal[THERMAL: thermal<br/>Thermal Component]
        ThermalLite[THERMAL: thermal-lite<br/>Thermal Lite Component]
        GSRRecording[GSR: gsr-recording<br/>GSR Data Recording]
        UserComp[USER: user<br/>User Management Component]
        PseudoComp[UTIL: pseudo<br/>Pseudo Component]
        CommonComp[UTIL: CommonComponent<br/>Shared Components]
    end
    
    %% External Module Layer
    subgraph "External Modules"
        BleModule[📡 BleModule<br/>Bluetooth Low Energy]
    end
    
    %% CI/CD Pipeline
    subgraph "CI/CD Pipeline"
        DevScript[🛠️ dev.sh<br/>Development Tools]
        CIWorkflow[⚙️ CI Workflow<br/>Build & Quality]
        GradleBuild[⚙️ Gradle Build Check<br/>Matrix Testing]  
        StaticAnalysis[🔍 Static Analysis<br/>Multi-language Linting]
    end
    
    %% Build System
    subgraph "Build System"
        GradleBuild2[🏗️ build.gradle.kts<br/>Root Build Config]
        VersionCatalog[📋 gradle/libs.versions.toml<br/>Version Catalog]
        Settings[⚙️ settings.gradle.kts<br/>Project Settings]
    end
    
    %% Dependencies and Relationships
    App --> LibApp
    App --> LibUI
    App --> LibCom
    App --> LibIR
    App --> BleModule
    
    LibApp --> LibCom
    LibUI --> LibMatrix
    LibIR --> LibMatrix
    
    ThermalIR --> LibIR
    Thermal --> LibIR  
    ThermalLite --> LibIR
    GSRRecording --> LibCom
    UserComp --> LibApp
    
    BleModule --> GSRRecording
    
    %% CI/CD Integration
    DevScript --> CIWorkflow
    DevScript --> GradleBuild
    DevScript --> StaticAnalysis
    
    CIWorkflow --> GradleBuild2
    GradleBuild --> GradleBuild2
    StaticAnalysis --> DevScript
    
    %% Build Dependencies
    GradleBuild2 --> VersionCatalog
    GradleBuild2 --> Settings
    
    %% Styling
    classDef appLayer fill:
    classDef libLayer fill:
    classDef componentLayer fill:
    classDef externalLayer fill:
    classDef cicdLayer fill:
    classDef buildLayer fill:
    
    class App,MainActivity appLayer
    class LibApp,LibCom,LibIR,LibMatrix,LibMenu,LibUI libLayer
    class ThermalIR,Thermal,ThermalLite,GSRRecording,UserComp,PseudoComp,CommonComp componentLayer
    class BleModule externalLayer
    class DevScript,CIWorkflow,GradleBuild,StaticAnalysis cicdLayer
    class GradleBuild2,VersionCatalog,Settings buildLayer
```



This diagram shows the complete IRCamera repository architecture with:

- **Application Layer**: Main Android app and core activities
- **Library Layer**: Reusable libraries for different functionalities  
- **Component Layer**: Feature-specific components for thermal imaging, GSR recording, user management
- **External Modules**: Third-party and external components
- **CI/CD Pipeline**: Automated build validation, testing, and quality assurance
- **Build System**: Gradle configuration and dependency management



1. **App → Libraries**: Main app depends on libapp, libui, libcom, libir
2. **Components → Libraries**: Thermal components use libir, GSR uses libcom  
3. **BLE Integration**: BleModule connects to GSR recording functionality
4. **CI/CD Integration**: Development tools integrate with all validation workflows
5. **Build System**: Centralized version catalog and gradle configuration
EOF
    
    
    cat > docs/architecture-viewer.html << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>IRCamera Architecture Viewer</title>
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: 
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        h1 { color: 
        .diagram-container { width: 100%; height: 600px; border: 1px solid 
        .info { background: 
        .controls { margin-bottom: 20px; text-align: center; }
        button { background: 
        button:hover { background: 
    </style>
</head>
<body>
    <div class="container">
        <h1>🏗️ IRCamera Repository Architecture</h1>
        
        <div class="controls">
            <button onclick="zoomIn()">🔍 Zoom In</button>
            <button onclick="zoomOut()">🔍 Zoom Out</button>
            <button onclick="resetZoom()">↺ Reset</button>
        </div>
        
        <div class="diagram-container" id="mermaidDiagram">
            <!-- Mermaid diagram will be rendered here -->
        </div>
        
        <div class="info">
            <h3>📊 Architecture Statistics</h3>
            <ul>
                <li><strong>Total Modules:</strong> 17 (App: 1, Libraries: 6, Components: 7, External: 2, CI/CD: 1)</li>
                <li><strong>Core Dependencies:</strong> App → LibApp, LibUI, LibCom, LibIR</li>
                <li><strong>Build System:</strong> Gradle with centralized version catalog</li>
                <li><strong>CI/CD Pipeline:</strong> 3 workflows (CI, Build Check, Static Analysis)</li>
            </ul>
        </div>
    </div>
    
    <script>
        mermaid.initialize({ 
            startOnLoad: true, 
            theme: 'base',
            themeVariables: {
                primaryColor: '#f9f9f9',
                primaryTextColor: '#333',
                primaryBorderColor: '#ddd',
                lineColor: '#666'
            }
        });
        
        const diagramDefinition = `
        graph TB
            %% Core Application Layer
            subgraph "Application Layer"
                App[APP: app<br/>Main Android Application]
                MainActivity[MainActivity.kt]
                App --> MainActivity
            end
            
            %% Library Layer
            subgraph "Library Layer"
                LibApp[LIB: libapp<br/>Application Library]
                LibCom[LIB: libcom<br/>Communication Library] 
                LibIR[LIB: libir<br/>Infrared Camera Library]
                LibMatrix[LIB: libmatrix<br/>Matrix Operations]
                LibMenu[LIB: libmenu<br/>Menu Components]
                LibUI[LIB: libui<br/>UI Components Library]
            end
            
            %% Component Layer  
            subgraph "Component Layer"
                ThermalIR[THERMAL: thermal-ir<br/>Thermal IR Component]
                Thermal[THERMAL: thermal<br/>Thermal Component]
                ThermalLite[THERMAL: thermal-lite<br/>Thermal Lite Component]
                GSRRecording[GSR: gsr-recording<br/>GSR Data Recording]
                UserComp[USER: user<br/>User Management Component]
                PseudoComp[UTIL: pseudo<br/>Pseudo Component]
                CommonComp[UTIL: CommonComponent<br/>Shared Components]
            end
            
            %% External Module Layer
            subgraph "External Modules"
                BleModule[📡 BleModule<br/>Bluetooth Low Energy]
            end
            
            %% CI/CD Pipeline
            subgraph "CI/CD Pipeline"
                DevScript[🛠️ dev.sh<br/>Development Tools]
                CIWorkflow[⚙️ CI Workflow<br/>Build & Quality]
                GradleBuild[⚙️ Gradle Build Check<br/>Matrix Testing]  
                StaticAnalysis[🔍 Static Analysis<br/>Multi-language Linting]
            end
            
            %% Dependencies
            App --> LibApp
            App --> LibUI
            App --> LibCom
            App --> LibIR
            App --> BleModule
            
            LibApp --> LibCom
            LibUI --> LibMatrix
            LibIR --> LibMatrix
            
            ThermalIR --> LibIR
            Thermal --> LibIR  
            ThermalLite --> LibIR
            GSRRecording --> LibCom
            UserComp --> LibApp
            
            BleModule --> GSRRecording
            
            DevScript --> CIWorkflow
            DevScript --> GradleBuild
            DevScript --> StaticAnalysis
        `;
        
        mermaid.render('generatedDiagram', diagramDefinition).then((result) => {
            document.getElementById('mermaidDiagram').innerHTML = result.svg;
        });
        
        function zoomIn() {
            const diagram = document.querySelector('#mermaidDiagram svg');
            if (diagram) {
                const currentScale = diagram.style.transform.match(/scale\((.*?)\)/);
                const scale = currentScale ? parseFloat(currentScale[1]) * 1.2 : 1.2;
                diagram.style.transform = \`scale(\${scale})\`;
                diagram.style.transformOrigin = 'center';
            }
        }
        
        function zoomOut() {
            const diagram = document.querySelector('#mermaidDiagram svg');
            if (diagram) {
                const currentScale = diagram.style.transform.match(/scale\((.*?)\)/);
                const scale = currentScale ? parseFloat(currentScale[1]) / 1.2 : 0.8;
                diagram.style.transform = \`scale(\${scale})\`;
                diagram.style.transformOrigin = 'center';
            }
        }
        
        function resetZoom() {
            const diagram = document.querySelector('#mermaidDiagram svg');
            if (diagram) {
                diagram.style.transform = 'scale(1)';
            }
        }
    </script>
</body>
</html>
EOF
    
    print_status "Architecture diagram generated: docs/architecture-diagram.md"
    print_status "Interactive viewer created: docs/architecture-viewer.html"
}


case "${1:-help}" in
    lint)
        lint_code
        ;;
    static)
        static_analysis
        ;;
    build-check)
        build_check
        ;;
    validate)
        validate_all
        ;;
    diagram)
        generate_diagram
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${RED}Unknown command: $1${NC}"
        echo ""
        show_help
        exit 1
        ;;
esac