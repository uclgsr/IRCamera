# GitHub Actions CI/CD Pipeline

This directory contains the CI/CD workflows for the IRCamera Android project.

## Workflows Overview

### 1. `ci.yml` - Main CI/CD Pipeline
**Trigger:** Push to main branches, Pull Requests
**Purpose:** Comprehensive build, test, and security scanning

**Jobs:**
- **Test**: Runs on multiple API levels (28, 30, 34)
  - Lint checks
  - Unit tests
  - Test report generation
- **Build**: Builds debug and release APKs
  - APK artifact storage
- **Security Scan**: CodeQL analysis for security vulnerabilities

### 2. `release.yml` - Release Build
**Trigger:** Git tags (v*), Manual dispatch
**Purpose:** Production release builds

**Features:**
- Release APK generation
- Automatic release notes
- GitHub release creation
- Signed APK support (with secrets)

### 3. `pr-validation.yml` - Pull Request Validation
**Trigger:** Pull Request events
**Purpose:** Quick validation of PRs

**Features:**
- PR title validation (conventional commits)
- Changed file analysis
- Module-specific testing
- APK size impact analysis
- Automated PR comments

### 4. `dependency-check.yml` - Security & Dependencies
**Trigger:** Schedule (weekly), Build file changes, Manual
**Purpose:** Dependency security and update monitoring

**Features:**
- OWASP Dependency Check
- Vulnerability scanning
- Dependency update reports
- Security report artifacts

### 5. `build-variants.yml` - Build Variants
**Trigger:** Manual dispatch
**Purpose:** Build specific variants (Google/Topdon)

**Features:**
- Multiple build variants
- Debug/Release builds
- Integration with existing build scripts
- Comprehensive build outputs

### 6. `code-formatting.yml` - Code Formatting & Validation
**Trigger:** Push to main branches, Pull Requests, Manual dispatch
**Purpose:** Comprehensive code formatting and validation across all file types

**Features:**
- XML file formatting (AndroidManifest, layouts, drawables, values)
- JSON file validation and formatting with proper indentation
- Gradle file syntax validation with dependency analysis
- YAML file linting with standards compliance
- TOML file validation and formatting
- Properties file standardization with key-value formatting
- Markdown file formatting for documentation consistency
- Shell script validation with executable permissions
- Chinese text cleanup from strings.xml files
- Automated commit functionality for formatting changes
- Comprehensive formatting reports with statistics

## Secrets Configuration

For full functionality, configure these secrets in your repository:

### Required for Release Signing:
- `SIGNING_KEY_ALIAS`: Keystore alias
- `SIGNING_KEY_PASSWORD`: Key password
- `SIGNING_STORE_PASSWORD`: Keystore password

### Optional for Enhanced Features:
- `GITHUB_TOKEN`: Automatically available for GitHub Actions

## Caching Strategy

All workflows use Gradle caching to improve build performance:
- Gradle packages cache
- Gradle wrapper cache
- Build cache for faster incremental builds

## Artifacts

Each workflow generates relevant artifacts:
- **APK files**: Debug and release builds
- **Test reports**: JUnit and lint results
- **Security reports**: Dependency and vulnerability scans
- **Build summaries**: Detailed build information

## Usage Examples

### Manual Release Build
1. Go to Actions tab
2. Select "Release Build"
3. Click "Run workflow"
4. Specify version and build type
5. Download artifacts from the workflow run

### Custom Variant Build
1. Go to Actions tab
2. Select "Build Variants"
3. Choose variant (google/topdon) and build type
4. Monitor build progress and download results

### Comprehensive Code Formatting
1. Go to Actions tab
2. Select "Code Formatting & Validation"
3. Click "Run workflow"
4. Enable "Auto-commit formatting changes" if desired
5. Review the formatting report in artifacts

**Local Formatting:**
Run the local formatting script:
```bash
./format_all_files.sh
```

Or use pre-commit hooks:
```bash
pip install pre-commit
pre-commit install
pre-commit run --all-files
```

## Integration with Existing Scripts

The CI/CD pipeline integrates with existing build scripts:
- `build_production_apk.sh`
- `enhanced_build.sh`
- Individual variant build scripts

## Monitoring and Notifications

- Build status is visible on the repository main page
- PR checks prevent merging broken code
- Security scans run automatically
- Dependency updates are monitored weekly

## Troubleshooting

### Common Issues:
1. **Build Failures**: Check the logs for specific error messages
2. **Cache Issues**: Clear cache by re-running the workflow
3. **Signing Issues**: Verify secrets are correctly configured
4. **Test Failures**: Review test reports in artifacts

### Performance Optimization:
- Workflows use caching to reduce build times
- Matrix builds run in parallel
- Module-specific testing for PRs reduces unnecessary work

## Contributing

When adding new workflows:
1. Follow the existing naming convention
2. Include proper caching strategies
3. Generate relevant artifacts
4. Update this documentation
5. Test thoroughly before merging