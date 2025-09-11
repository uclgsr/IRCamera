# IRCamera Development Makefile
# Provides convenient shortcuts for common development tasks

.PHONY: help setup format lint build test validate clean install

# Default target
help:
	@echo "IRCamera Development Commands"
	@echo ""
	@echo "Available targets:"
	@echo "  setup     - Setup development environment"
	@echo "  format    - Format all code files"
	@echo "  lint      - Run linting checks"
	@echo "  build     - Build the project"
	@echo "  test      - Run tests"
	@echo "  validate  - Run all checks (format + lint + build)"
	@echo "  clean     - Clean build artifacts"
	@echo "  install   - Install development tools"
	@echo ""
	@echo "Examples:"
	@echo "  make setup     # Setup development environment"
	@echo "  make validate  # Run all validation checks"
	@echo "  make build     # Build the Android app"

setup:
	./dev.sh setup

format:
	./dev.sh format

lint:
	./dev.sh lint

build:
	./dev.sh build

test:
	./dev.sh test

validate:
	./dev.sh validate

clean:
	./dev.sh clean

install:
	@echo "Installing development tools..."
	pip install pre-commit black flake8 yamllint
	@echo "✅ Development tools installed"
	@echo "Note: Install ktlint and google-java-format manually for optimal experience"