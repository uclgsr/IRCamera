#!/usr/bin/env python3
"""
Setup Validation Script

Validates that the environment is correctly set up to run the thesis evaluation tests.
Checks Python version, dependencies, and file structure.
"""

import sys
import os
from pathlib import Path

def check_python_version():
    """Check if Python version is 3.7 or higher"""
    print("Checking Python version...")
    version = sys.version_info
    if version >= (3, 7):
        print(f"  ✓ Python {version.major}.{version.minor}.{version.micro} (OK)")
        return True
    else:
        print(f"  ✗ Python {version.major}.{version.minor}.{version.micro} (Need 3.7+)")
        return False

def check_command_client():
    """Check if CommandClient is available"""
    print("\nChecking CommandClient availability...")
    try:
        sys.path.insert(0, str(Path(__file__).parent.parent / 'pc-controller'))
        from command_client import CommandClient
        print("  ✓ CommandClient import successful")
        return True
    except ImportError as e:
        print(f"  ✗ CommandClient import failed: {e}")
        print("    Make sure pc-controller/command_client.py exists")
        return False

def check_test_files():
    """Check if all test files are present"""
    print("\nChecking test files...")
    test_dir = Path(__file__).parent / 'tests'
    
    required_files = [
        'test_1_remote_start_stop.py',
        'test_2_command_latency_throughput.py',
        'test_3_edge_case_commands.py',
        'test_4_multi_command_sequence.py',
        'run_all_tests.py'
    ]
    
    all_present = True
    for filename in required_files:
        filepath = test_dir / filename
        if filepath.exists():
            print(f"  ✓ tests/{filename}")
        else:
            print(f"  ✗ tests/{filename} (missing)")
            all_present = False
    
    return all_present

def check_permissions():
    """Check if test files are executable"""
    print("\nChecking file permissions...")
    test_dir = Path(__file__).parent / 'tests'
    
    test_files = [
        'test_1_remote_start_stop.py',
        'test_2_command_latency_throughput.py',
        'test_3_edge_case_commands.py',
        'test_4_multi_command_sequence.py',
        'run_all_tests.py'
    ]
    
    all_executable = True
    for filename in test_files:
        filepath = test_dir / filename
        if filepath.exists():
            if os.access(filepath, os.X_OK):
                print(f"  ✓ tests/{filename} (executable)")
            else:
                print(f"  ⚠ tests/{filename} (not executable, but can still run with python3)")
    
    return True  # Not critical, just informational

def check_documentation():
    """Check if documentation files exist"""
    print("\nChecking documentation...")
    test_dir = Path(__file__).parent
    
    doc_files = [
        'README.md',
        'QUICK_START.md',
        'IMPLEMENTATION_SUMMARY.md'
    ]
    
    for filename in doc_files:
        filepath = test_dir / filename
        if filepath.exists():
            print(f"  ✓ {filename}")
        else:
            print(f"  ✗ {filename} (missing)")
    
    return True  # Documentation is helpful but not critical

def check_dependencies():
    """Check optional dependencies"""
    print("\nChecking optional dependencies...")
    
    optional_deps = [
        ('typing_extensions', 'typing-extensions'),
        ('pandas', 'pandas'),
        ('numpy', 'numpy'),
        ('matplotlib', 'matplotlib')
    ]
    
    for module_name, package_name in optional_deps:
        try:
            __import__(module_name)
            print(f"  ✓ {package_name} (installed)")
        except ImportError:
            print(f"  - {package_name} (optional, not installed)")
    
    return True  # Optional dependencies

def main():
    """Run all validation checks"""
    print("=" * 70)
    print("Thesis Evaluation Tests - Setup Validation")
    print("=" * 70)
    
    checks = [
        ("Python version", check_python_version),
        ("CommandClient", check_command_client),
        ("Test files", check_test_files),
        ("Permissions", check_permissions),
        ("Documentation", check_documentation),
        ("Dependencies", check_dependencies)
    ]
    
    results = []
    for name, check_func in checks:
        results.append(check_func())
    
    print("\n" + "=" * 70)
    print("Validation Summary")
    print("=" * 70)
    
    critical_checks = results[:3]  # Python, CommandClient, Test files
    
    if all(critical_checks):
        print("✓ All critical checks passed!")
        print("\nYou can now run the tests:")
        print("  python3 tests/test_1_remote_start_stop.py --device-ip <ANDROID_IP>")
        print("  python3 tests/run_all_tests.py --device-ip <ANDROID_IP>")
        print("\nOr use the interactive menu:")
        print("  ./example_usage.sh <ANDROID_IP>")
        return 0
    else:
        print("✗ Some critical checks failed.")
        print("\nPlease fix the issues above before running tests.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
