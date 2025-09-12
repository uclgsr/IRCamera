#!/bin/bash

# IRCamera Security Scanner
# Comprehensive security vulnerability scanning and analysis

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

show_header() {
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║              🔒 IRCamera Security Scanner                     ║${NC}"
    echo -e "${BLUE}║        Vulnerability Detection • Dependency Analysis         ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

scan_dependencies() {
    echo -e "${YELLOW}🔍 Scanning Dependencies for Vulnerabilities${NC}"
    echo "──────────────────────────────────────────────"
    
    # Run OWASP dependency check if available
    if command -v dependency-check &> /dev/null; then
        echo "🛡️  Running OWASP Dependency Check..."
        dependency-check --project "IRCamera" --scan . --format JSON --format HTML --out reports/security/
        echo -e "${GREEN}✅ OWASP scan completed${NC}"
    else
        echo -e "${YELLOW}⚠️  OWASP Dependency Check not available${NC}"
    fi
    
    # Check gradle dependencies for known vulnerabilities
    echo "📦 Analyzing Gradle dependencies..."
    if ./gradlew dependencyCheckAnalyze &>/dev/null; then
        echo -e "${GREEN}✅ Gradle dependency analysis completed${NC}"
    else
        echo -e "${YELLOW}⚠️  Gradle dependency check plugin not configured${NC}"
    fi
    
    # Scan for common vulnerable patterns in dependencies
    local suspicious_deps=0
    if grep -r "http://" build.gradle.kts */build.gradle.kts 2>/dev/null | grep -v localhost; then
        echo -e "${RED}⚠️  HTTP URLs found in dependencies (should use HTTPS)${NC}"
        suspicious_deps=$((suspicious_deps + 1))
    fi
    
    echo -e "${CYAN}Dependency Security Status: $suspicious_deps issues found${NC}"
}

scan_code_vulnerabilities() {
    echo -e "${YELLOW}🔬 Scanning Code for Security Vulnerabilities${NC}"
    echo "───────────────────────────────────────────────────"
    
    local security_issues=0
    
    # Check for hardcoded credentials
    echo "🔑 Checking for hardcoded credentials..."
    if find . -name "*.kt" -o -name "*.java" | xargs grep -l -i "password\|secret\|api_key\|token" 2>/dev/null | head -5; then
        echo -e "${YELLOW}⚠️  Potential hardcoded credentials found${NC}"
        security_issues=$((security_issues + 1))
    else
        echo -e "${GREEN}✅ No obvious hardcoded credentials${NC}"
    fi
    
    # Check for SQL injection patterns
    echo "💉 Checking for SQL injection vulnerabilities..."
    if find . -name "*.kt" -o -name "*.java" | xargs grep -l "rawQuery\|execSQL" 2>/dev/null | head -3; then
        echo -e "${YELLOW}⚠️  Raw SQL queries found - review for injection risks${NC}"
        security_issues=$((security_issues + 1))
    else
        echo -e "${GREEN}✅ No raw SQL queries detected${NC}"
    fi
    
    # Check for insecure network configurations
    echo "🌐 Checking network security configurations..."
    if find . -name "*.xml" | xargs grep -l "usesCleartextTraffic.*true\|android:networkSecurityConfig" 2>/dev/null; then
        echo -e "${YELLOW}⚠️  Network security configurations found - review settings${NC}"
        security_issues=$((security_issues + 1))
    else
        echo -e "${GREEN}✅ No insecure network configs detected${NC}"
    fi
    
    # Check for debugging code in production
    echo "🐛 Checking for debug code..."
    if find . -name "*.kt" -o -name "*.java" | xargs grep -l "Log\.d\|System\.out\.print\|printStackTrace" 2>/dev/null | head -3; then
        echo -e "${YELLOW}⚠️  Debug/logging code found - review for production${NC}"
        security_issues=$((security_issues + 1))
    else
        echo -e "${GREEN}✅ No debug code in production files${NC}"
    fi
    
    echo -e "${CYAN}Code Security Status: $security_issues potential issues${NC}"
    return "$security_issues"
}

check_permissions() {
    echo -e "${YELLOW}🛡️  Analyzing Android Permissions${NC}"
    echo "──────────────────────────────────────"
    
    local dangerous_permissions=0
    
    # Find all AndroidManifest.xml files
    while IFS= read -r -d '' manifest; do
        echo "📄 Analyzing: $manifest"
        
        # Check for dangerous permissions
        if grep -q "android.permission.CAMERA\|android.permission.RECORD_AUDIO\|android.permission.ACCESS_FINE_LOCATION" "$manifest" 2>/dev/null; then
            echo -e "${YELLOW}  ⚠️  Sensitive permissions detected${NC}"
            dangerous_permissions=$((dangerous_permissions + 1))
        fi
        
        # Check for unnecessary permissions
        if grep -q "android.permission.WRITE_EXTERNAL_STORAGE" "$manifest" 2>/dev/null; then
            echo -e "${YELLOW}  💾 External storage write permission - ensure necessary${NC}"
        fi
        
        # Check for network permissions
        if grep -q "android.permission.INTERNET" "$manifest" 2>/dev/null; then
            echo -e "${CYAN}  🌐 Internet permission found${NC}"
        fi
        
    done < <(find . -name "AndroidManifest.xml" -print0)
    
    echo -e "${CYAN}Permission Analysis: $dangerous_permissions manifests with sensitive permissions${NC}"
}

analyze_proguard_configuration() {
    echo -e "${YELLOW}🔒 Analyzing ProGuard/R8 Configuration${NC}"
    echo "─────────────────────────────────────────"
    
    local proguard_files
    proguard_files=$(find . -name "proguard-*.pro" -o -name "consumer-rules.pro" | wc -l)
    
    if [ "$proguard_files" -gt 0 ]; then
        echo -e "${GREEN}✅ ProGuard configuration files found: $proguard_files${NC}"
        
        # Check for common security-related configurations
        while IFS= read -r -d '' config; do
            echo "📄 Checking: $config"
            
            if grep -q "minifyEnabled.*true" "$config" 2>/dev/null; then
                echo -e "${GREEN}  ✅ Minification enabled${NC}"
            fi
            
            if grep -q "shrinkResources.*true" "$config" 2>/dev/null; then
                echo -e "${GREEN}  ✅ Resource shrinking enabled${NC}"
            fi
            
        done < <(find . -name "*.pro" -print0)
    else
        echo -e "${YELLOW}⚠️  No ProGuard configuration files found${NC}"
    fi
}

generate_security_report() {
    echo -e "${YELLOW}📊 Generating Security Report${NC}"
    echo "────────────────────────────────"
    
    local report_dir="reports/security"
    mkdir -p "$report_dir"
    
    # Create comprehensive security report
    cat > "$report_dir/security-analysis.md" << EOF
# IRCamera Security Analysis Report

Generated: $(date '+%Y-%m-%d %H:%M:%S')

## Executive Summary

This report provides a comprehensive security analysis of the IRCamera Android application.

## Dependency Security
- OWASP Dependency Check: $([ -f "reports/security/dependency-check-report.html" ] && echo "✅ Completed" || echo "⚠️  Not run")
- Gradle Dependencies: Analyzed for known vulnerabilities
- Recommendation: Regular dependency updates and vulnerability monitoring

## Code Security Analysis
- Hardcoded Credentials: Scanned for sensitive information
- SQL Injection: Checked for unsafe query patterns
- Network Security: Analyzed configuration settings
- Debug Code: Reviewed for production readiness

## Android-Specific Security
- Permissions: Analyzed for minimal necessary permissions
- Manifest Security: Reviewed AndroidManifest.xml configurations
- ProGuard/R8: Code obfuscation and optimization settings

## Recommendations

### High Priority
1. Enable ProGuard/R8 minification for release builds
2. Review and minimize required permissions
3. Implement certificate pinning for network communications
4. Enable network security configuration

### Medium Priority
1. Regular dependency vulnerability scans
2. Static code analysis integration
3. Security-focused code reviews
4. Penetration testing for critical features

### Monitoring
1. Set up automated security scanning in CI/CD
2. Regular OWASP dependency check updates
3. Monitor security advisories for used libraries

## Security Score
Overall Security Posture: Review Required
- Dependencies: $([ -f "reports/security/dependency-check-report.html" ] && echo "Analyzed" || echo "Needs Analysis")
- Code Quality: Static analysis recommended
- Configuration: Review Android security settings

EOF

    echo -e "${GREEN}✅ Security report generated: $report_dir/security-analysis.md${NC}"
}

run_automated_security_tests() {
    echo -e "${YELLOW}🧪 Running Automated Security Tests${NC}"
    echo "─────────────────────────────────────"
    
    # Test for common Android security issues
    local security_test_results=0
    
    # Check if app uses HTTPS enforcement
    if find . -name "*.xml" | xargs grep -l "android:usesCleartextTraffic.*false" 2>/dev/null; then
        echo -e "${GREEN}✅ HTTPS enforcement configured${NC}"
    else
        echo -e "${YELLOW}⚠️  Consider enforcing HTTPS-only traffic${NC}"
        security_test_results=$((security_test_results + 1))
    fi
    
    # Check for backup allowance
    if find . -name "AndroidManifest.xml" | xargs grep -l "android:allowBackup.*false" 2>/dev/null; then
        echo -e "${GREEN}✅ App backup disabled for security${NC}"
    else
        echo -e "${YELLOW}⚠️  Consider disabling app backup for sensitive data${NC}"
        security_test_results=$((security_test_results + 1))
    fi
    
    # Check for debug mode
    if find . -name "AndroidManifest.xml" | xargs grep -l "android:debuggable.*true" 2>/dev/null; then
        echo -e "${RED}❌ Debug mode enabled - disable for production${NC}"
        security_test_results=$((security_test_results + 1))
    else
        echo -e "${GREEN}✅ Debug mode properly configured${NC}"
    fi
    
    echo -e "${CYAN}Security Test Results: $security_test_results issues to address${NC}"
    return "$security_test_results"
}

# Main execution
main() {
    show_header
    
    local start_time end_time total_duration
    start_time=$(date +%s)
    
    echo -e "${PURPLE}🔒 Starting Security Analysis${NC}"
    echo ""
    
    # Create reports directory
    mkdir -p reports/security
    
    # Run all security checks
    scan_dependencies
    echo ""
    
    scan_code_vulnerabilities
    echo ""
    
    check_permissions
    echo ""
    
    analyze_proguard_configuration
    echo ""
    
    run_automated_security_tests
    echo ""
    
    generate_security_report
    echo ""
    
    end_time=$(date +%s)
    total_duration=$((end_time - start_time))
    
    echo -e "${GREEN}🛡️  Security analysis completed in ${total_duration}s${NC}"
    echo ""
    echo -e "${CYAN}📚 Next Steps:${NC}"
    echo "  • Review security report: ./reports/security/security-analysis.md"
    echo "  • Address identified issues in order of priority"
    echo "  • Set up automated security scanning in CI/CD"
    echo "  • Consider penetration testing for critical features"
}

# Execute if run directly
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    main "$@"
fi