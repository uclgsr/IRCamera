# Contributing to IRCamera

Thank you for your interest in contributing to IRCamera! This guide will help you get started.

## 🔧 Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/buccancs/IRCamera.git
   cd IRCamera
   ```

2. **Setup development environment**
   ```bash
   ./dev.sh setup
   ```

3. **Install required tools** (optional but recommended)
   ```bash
   # For code formatting and linting
   pip install pre-commit black flake8
   
   # For Kotlin formatting
   # Download ktlint from https://github.com/pinterest/ktlint
   
   # For Java formatting  
   # Download google-java-format from https://github.com/google/google-java-format
   ```

## 📝 Development Workflow

### Before Making Changes

1. **Run validation** to ensure everything works:
   ```bash
   ./dev.sh validate
   ```

2. **Create a feature branch**:
   ```bash
   git checkout -b feature/amazing-feature
   ```

### Making Changes

1. **Format your code**:
   ```bash
   ./dev.sh format
   ```

2. **Run linting**:
   ```bash
   ./dev.sh lint
   ```

3. **Build and test**:
   ```bash
   ./dev.sh build
   ./dev.sh test
   ```

### Submitting Changes

1. **Run full validation**:
   ```bash
   ./dev.sh validate
   ```

2. **Commit your changes**:
   ```bash
   git add .
   git commit -m "feat: add amazing feature"
   ```

3. **Push and create PR**:
   ```bash
   git push origin feature/amazing-feature
   ```

## 🔍 Code Quality Standards

### Automated Checks
- **Pre-commit hooks**: Automatically format and validate code before commits
- **CI Pipeline**: All PRs are automatically validated
- **Code formatting**: Consistent formatting across all languages

### Manual Verification
- Run `./dev.sh validate` before submitting any PR
- Ensure all tests pass
- Check that build completes successfully

## 🏗️ Project Structure

```
├── app/                    # Main Android application
├── component/              # Feature modules
│   ├── thermal-ir/        # Advanced thermal processing
│   ├── thermal-lite/      # Lightweight thermal features
│   └── ...
├── lib*/                  # Core libraries
├── pc-controller/         # Python PC application (WIP)
├── .github/workflows/     # CI/CD workflows
├── dev.sh                 # Development tools
└── CONTRIBUTING.md        # This file
```

## 📋 Development Commands

| Command | Description |
|---------|-------------|
| `./dev.sh setup` | Setup development environment |
| `./dev.sh format` | Format all code files |
| `./dev.sh lint` | Run linting checks |
| `./dev.sh build` | Build the project |
| `./dev.sh test` | Run tests |
| `./dev.sh validate` | Run all checks |
| `./dev.sh clean` | Clean build artifacts |

## 🐛 Reporting Issues

1. **Check existing issues** before creating new ones
2. **Use the issue templates** when available
3. **Provide detailed information**:
   - Android version
   - Device model
   - Steps to reproduce
   - Expected vs actual behavior

## 💡 Feature Requests

1. **Search existing feature requests** first
2. **Describe the use case** clearly
3. **Explain the benefit** to users
4. **Consider implementation complexity**

## 📱 Testing

### Unit Tests
- Located in `*/src/test/`
- Run with `./dev.sh test`

### Manual Testing
- Test on different Android versions
- Verify thermal camera functionality
- Check performance impact

## 🔐 Security

- **Never commit sensitive data** (API keys, passwords, etc.)
- **Use Android Keystore** for secure storage
- **Follow security best practices** for thermal data handling

## 📄 License

By contributing, you agree that your contributions will be licensed under the same license as the project.

## ❓ Questions?

- **Check the documentation** first
- **Search existing issues and discussions**
- **Ask in GitHub Discussions** for general questions
- **Create an issue** for bugs or feature requests