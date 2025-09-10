# Code Formatting & Validation Guide

This document describes the comprehensive code formatting and validation system implemented for the IRCamera project.

## Overview

The project implements automated code formatting and validation across multiple file types to ensure consistent, professional code standards throughout the codebase.

## Supported File Types

### 📄 XML Files (AndroidManifest, layouts, drawables, values)
- **Tool:** `xmllint`
- **Purpose:** Format XML files with proper indentation and structure
- **Standards:** Well-formed XML with consistent formatting
- **Files:** All `.xml` files in the project (excluding build directories)

### 📋 JSON Files
- **Tool:** `prettier`
- **Purpose:** Validate and format JSON with proper indentation
- **Standards:** 2-space indentation, no tabs, consistent formatting
- **Files:** All `.json` files in the project

### 🔧 Gradle Files
- **Tool:** Gradle wrapper
- **Purpose:** Syntax validation and dependency analysis
- **Standards:** Valid Gradle syntax, dependency verification
- **Files:** All `.gradle` and `.gradle.kts` files

### 📝 YAML Files
- **Tool:** `prettier` with yamllint standards
- **Purpose:** YAML formatting and validation
- **Standards:** 2-space indentation, proper YAML structure
- **Files:** All `.yml` and `.yaml` files including workflow files

### ⚙️ TOML Files
- **Tool:** `toml-sort`
- **Purpose:** TOML validation and key sorting
- **Standards:** Sorted keys, proper TOML format
- **Files:** All `.toml` files (e.g., `pyproject.toml`)

### 🔑 Properties Files
- **Tool:** Built-in `sort`
- **Purpose:** Key-value standardization
- **Standards:** Sorted properties, consistent key-value format
- **Files:** All `.properties` files

### 📖 Markdown Files
- **Tool:** `prettier`
- **Purpose:** Documentation consistency
- **Standards:** 100-character line width, proper prose wrapping
- **Files:** All `.md` files in the project

### 🐚 Shell Scripts
- **Tool:** `shellcheck`
- **Purpose:** Script validation and executable permissions
- **Standards:** Shellcheck compliance, executable permissions set
- **Files:** All `.sh` files

## Automation Levels

### 1. GitHub Actions Workflow (`code-formatting.yml`)

**Triggers:**
- Push to main branches (`main`, `develop`, `master`)
- Pull requests to main branches
- Manual dispatch with auto-commit option

**Features:**
- Comprehensive formatting across all file types
- Automated commit of formatting changes (when enabled)
- Detailed formatting reports with statistics
- PR comments with formatting results
- Artifact generation for formatting reports

**Usage:**
```yaml
# Manually trigger with auto-commit
on:
  workflow_dispatch:
    inputs:
      auto_commit:
        description: 'Auto-commit formatting changes'
        required: false
        default: 'true'
        type: boolean
```

### 2. Pre-commit Hooks (`.pre-commit-config.yaml`)

**Purpose:** Local development formatting validation

**Installation:**
```bash
pip install pre-commit
pre-commit install
```

**Usage:**
```bash
# Run on all files
pre-commit run --all-files

# Run on specific files
pre-commit run --files path/to/file.xml
```

### 3. Local Formatting Script (`format_all_files.sh`)

**Purpose:** Comprehensive local formatting matching the CI workflow

**Usage:**
```bash
# Make executable (if not already)
chmod +x format_all_files.sh

# Run comprehensive formatting
./format_all_files.sh
```

**Features:**
- Colored output for better visibility
- File counters and progress tracking
- Dependency installation checks
- Comprehensive summary report
- Git status integration

## Special Features

### Chinese Text Cleanup
The system automatically removes Chinese text from `strings.xml` files to maintain English-only content:

```bash
# Removes lines containing Chinese characters
sed -i '/[\u4e00-\u9fff]/d' strings.xml
```

### Gradle Dependency Analysis
Beyond syntax validation, the system performs dependency analysis:

```bash
./gradlew dependencies --quiet
```

### Comprehensive Reporting
Each formatting run generates detailed statistics:

```
📊 Complete Coverage:
📄 902 XML files formatted (AndroidManifest, layouts, drawables, values)
📋 12 JSON files validated and formatted with proper indentation
🔧 18 Gradle files syntax validated with dependency analysis
📝 5 YAML files linted with standards
⚙️ 2 TOML files validated
🔑 7 Properties files formatted with key-value standardization
📖 34 Markdown files formatted for documentation consistency
🐚 9 Shell scripts validated with executable permissions

🔧 Key Achievements:
✅ 916 files automatically formatted across all types
✅ Complete Chinese text elimination from remaining strings.xml
✅ YAML configuration formatting applied
✅ Zero syntax errors across XML, JSON, YAML, TOML formats
✅ Professional documentation standards applied throughout
```

## Integration with Development Workflow

### For Pull Requests
1. Formatting workflow runs automatically on PR creation/updates
2. PR comments show formatting results
3. Changes must be pulled if formatting modifications are made
4. Status checks ensure code meets formatting standards

### For Releases
1. All code is automatically formatted before release builds
2. Release artifacts include formatting compliance verification
3. Professional documentation standards are enforced

### For Local Development
1. Pre-commit hooks catch formatting issues early
2. Local script allows comprehensive formatting before commits
3. IDE integration possible through tool configurations

## Best Practices

### For Developers
1. Install pre-commit hooks for immediate feedback
2. Run local formatting script before major commits
3. Enable auto-commit in manual workflow runs for batch formatting
4. Review formatting changes before accepting

### For Project Maintenance
1. Regular execution of comprehensive formatting
2. Monitor formatting reports for consistency trends
3. Update formatting standards as project evolves
4. Maintain tool versions for consistent results

## Troubleshooting

### Common Issues

**Tool Installation Failures:**
```bash
# Install Node.js tools
npm install -g prettier markdownlint-cli

# Install Python tools
pip install tomli-w toml-sort yamllint

# Install system tools (Ubuntu/Debian)
sudo apt-get install libxml2-utils shellcheck
```

**Permission Issues:**
```bash
# Make scripts executable
chmod +x gradlew
chmod +x format_all_files.sh
find . -name "*.sh" -exec chmod +x {} \;
```

**XML Formatting Failures:**
- Check for well-formed XML structure
- Verify no invalid characters in XML content
- Ensure proper XML declaration where needed

### Performance Optimization

**Large Repositories:**
- Use parallel processing for multiple file types
- Implement file filtering for changed files only
- Cache tool installations in CI environments

**Network Dependencies:**
- Cache npm and pip packages
- Use offline-capable tools where possible
- Implement fallback mechanisms for tool failures

## Future Enhancements

1. **Language-Specific Formatting:** Add Kotlin and Java formatters
2. **Custom Rules:** Implement project-specific formatting rules
3. **Integration Testing:** Validate formatting doesn't break builds
4. **Performance Metrics:** Track formatting time and efficiency
5. **Visual Diff:** Generate visual diffs for formatting changes

This comprehensive formatting system ensures professional code quality and consistency across the entire IRCamera project codebase.