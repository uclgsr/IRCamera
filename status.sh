#!/bin/bash

# IRCamera Project Status
# Shows quick overview of project health

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}IRCamera Project Status${NC}"
echo "========================"
echo ""

# Project info
echo -e "${GREEN}📱 Project Overview${NC}"
echo "Type: Android Thermal Imaging Platform"
echo "Main language: Kotlin/Java"
echo "Build system: Gradle"
echo ""

# File counts
echo -e "${GREEN}📊 Project Statistics${NC}"
echo "Kotlin files: $(find . -name "*.kt" | wc -l)"
echo "Java files: $(find . -name "*.java" | wc -l)"
echo "XML files: $(find . -name "*.xml" | wc -l)"
echo "Python files: $(find . -name "*.py" | wc -l)"
echo ""

# Build status
echo -e "${GREEN}🔧 Development Tools${NC}"
echo "Gradle: $([ -f "./gradlew" ] && echo "✅ Available" || echo "❌ Missing")"
echo "Dev script: $([ -f "./dev.sh" ] && echo "✅ Available" || echo "❌ Missing")"
echo "Pre-commit: $([ -f ".pre-commit-config.yaml" ] && echo "✅ Configured" || echo "❌ Not configured")"
echo ""

# Recent activity
echo -e "${GREEN}📝 Recent Activity${NC}"
echo "Last commit: $(git log -1 --format='%h - %s (%cr)' 2>/dev/null || echo 'No git history')"
echo "Branch: $(git branch --show-current 2>/dev/null || echo 'Not a git repository')"
echo ""

# Quick health check
echo -e "${GREEN}🏥 Quick Health Check${NC}"
if ./gradlew help --quiet &>/dev/null; then
    echo "Gradle: ✅ Working"
else
    echo "Gradle: ⚠️  Issues detected"
fi

if [ -x "./dev.sh" ]; then
    echo "Dev tools: ✅ Ready"
else
    echo "Dev tools: ⚠️  Not executable"
fi

echo ""
echo -e "${YELLOW}💡 Quick Start:${NC}"
echo "  ./dev.sh validate  # Run all checks"
echo "  ./dev.sh build     # Build the project"
echo "  make help          # Show Makefile commands"
