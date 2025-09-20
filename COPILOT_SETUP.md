# GitHub Copilot Setup for IRCamera Project

## Overview

This document describes the GitHub Copilot configuration for the IRCamera Multi-Modal Thermal Sensing Platform. The
setup is optimized for the project's multi-language architecture including Kotlin/Java for Android development and
Python for the PC controller.

## Quick Setup

### Prerequisites

1. **GitHub Copilot Subscription**: Ensure you have access to GitHub Copilot
2. **VS Code**: Install Visual Studio Code with the recommended extensions
3. **Development Environment**: Follow the main project setup instructions

### Automated Setup

The repository includes automated Copilot configuration:

```bash
# Clone the repository
git clone https://github.com/uclgsr/IRCamera.git
cd IRCamera

# VS Code will automatically prompt to install recommended extensions
code .
```

## Configuration Files

### Core Copilot Files

- **`.github/copilot-instructions.md`**: Comprehensive project context and instructions for Copilot
- **`.github/workflows/copilot.yml`**: GitHub Actions workflow for Copilot validation
- **`.vscode/settings.json`**: VS Code settings optimized for Copilot
- **`.vscode/extensions.json`**: Recommended extensions including Copilot
- **`.vscode/launch.json`**: Debug configurations for different project components
- **`.vscode/tasks.json`**: Common development tasks

### Key Features

- **Multi-language Support**: Optimized for Kotlin, Java, Python, and shell scripts
- **Project-aware Context**: Copilot understands the Hub-and-Spoke architecture
- **Build System Integration**: Configured for Gradle and Python build tools
- **Debug Support**: Ready-to-use debug configurations

## Using Copilot with This Project

### Project Structure Awareness

Copilot is configured with comprehensive knowledge of:

- **Android App Structure**: Main app, BLE module, component libraries
- **PC Controller**: Python-based hub with PyQt6 GUI
- **Build Systems**: Gradle for Android, Python setup tools
- **Known Issues**: Build failures, workarounds, and current limitations

### Common Tasks

#### Android Development

```kotlin
// Copilot understands the project's GSR and thermal sensor integration
class GSRDataProcessor {
    // Copilot will suggest context-aware implementations
}
```

#### PC Controller Development

```python
# Copilot knows about the Hub-and-Spoke architecture
class DeviceManager:
# Suggestions will align with existing patterns
```

#### Build and Testing

```bash
# Copilot understands the project's build constraints
./dev.sh lint  # Known to work
./gradlew clean  # Takes 90s, configured timeouts
```

### Best Practices

1. **Use Descriptive Comments**: Help Copilot understand your intent
2. **Reference Existing Patterns**: Copilot learns from the existing codebase
3. **Follow Project Conventions**: Leverage the established architecture

## Troubleshooting

### Common Issues

#### Copilot Not Working

1. Verify your GitHub Copilot subscription is active
2. Check that the GitHub Copilot extension is installed and enabled
3. Restart VS Code if suggestions aren't appearing

#### Limited Context

If Copilot suggestions seem generic:

1. Ensure you're working within the project directory
2. Reference the `.github/copilot-instructions.md` file
3. Use more specific variable names and comments

#### Build System Confusion

If Copilot suggests incorrect build commands:

1. Refer to the known working commands in `copilot-instructions.md`
2. Use the VS Code tasks (Ctrl+Shift+P -> "Tasks: Run Task")
3. Follow the documented workarounds for build issues

### Getting Help

- **Project Documentation**: See `docs/` directory for detailed information
- **Development Tools**: Use `./dev.sh help` for available commands
- **PC Controller**: Check `pc-controller/README_MVP.md` for specific guidance

## Validation

The repository includes automated validation:

```bash
# GitHub Actions workflow validates:
# - Copilot instructions are present and valid
# - Project structure is correctly configured
# - Build tools are accessible
# - Development environment is ready
```

## Advanced Configuration

### Custom Instructions

To add project-specific context for Copilot:

1. Edit `.github/copilot-instructions.md`
2. Add relevant patterns, constraints, or examples
3. Test with the validation workflow

### Language-Specific Settings

The VS Code configuration includes optimized settings for:

- **Kotlin**: Language server and syntax highlighting
- **Java**: Integrated with Gradle build system
- **Python**: Configured for the pc-controller virtual environment
- **Shell**: Optimized for the dev.sh script

## Contributing

When contributing to the project:

1. Follow the existing code patterns that Copilot has learned
2. Update documentation if you add new components
3. Test your changes with the existing validation scripts
4. Consider updating Copilot instructions for significant architectural changes

---

For more information, see the main [README.md](README.md) and [project documentation](docs/).