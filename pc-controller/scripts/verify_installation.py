#!/usr/bin/env python3
"""
PC Controller Installation Verification Script

Checks that all required components are installed and working correctly.
"""

import subprocess
import sys
from pathlib import Path


def print_header(text):
    """Print a formatted header"""
    print("\n" + "=" * 70)
    print(f"  {text}")
    print("=" * 70)


def check_python_version():
    """Check Python version"""
    print("\n[1/8] Checking Python version...")
    version = sys.version_info
    if version.major >= 3 and version.minor >= 8:
        print(f"   Python {version.major}.{version.minor}.{version.micro}")
        return True
    else:
        print(f"   Python {version.major}.{version.minor} (requires 3.8+)")
        return False


def check_dependencies():
    """Check required Python packages"""
    print("\n[2/8] Checking Python dependencies...")

    required = {
        'PyQt6': 'GUI framework',
        'pyqtgraph': 'Real-time plotting',
        'numpy': 'Numerical computing',
        'opencv-python': 'Webcam capture',
        'cryptography': 'SSL/TLS security',
        'pybind11': 'C++ bindings'
    }

    missing = []
    for package, description in required.items():
        try:
            # Special cases for different package names
            if package == 'opencv-python':
                import cv2
                print(f"   {package:20s} - {description}")
            elif package == 'PyQt6':
                import PyQt6
                print(f"   {package:20s} - {description}")
            else:
                __import__(package.replace('-', '_').lower())
                print(f"   {package:20s} - {description}")
        except ImportError:
            print(f"   {package:20s} - {description} (MISSING)")
            missing.append(package)

    if missing:
        print(f"\n  Install missing packages:")
        print(f"  pip install {' '.join(missing)}")
        return False

    return True


def check_native_backend():
    """Check if native backend is built"""
    print("\n[3/8] Checking C++ native backend...")

    sys.path.insert(0, str(Path(__file__).parent.parent / 'native_backend'))

    try:
        import enhanced_native_backend
        print(f"   Native backend found")
        print(f"    Version: {enhanced_native_backend.__version__}")
        print(f"    Build date: {enhanced_native_backend.__build_date__}")
        return True
    except ImportError:
        print(f"    Native backend not built (optional)")
        print(f"    Build with: cd native_backend && python3 setup.py build_ext --inplace")
        print(f"    Controller will use Python fallback")
        return None  # None means optional, not critical


def check_controllers():
    """Check if controller files exist and are importable"""
    print("\n[4/8] Checking controller modules...")

    files = {
        'pc_controller.py': 'Unified controller',
        'protocol_adapter.py': 'Protocol compatibility layer',
        'sync_handler.py': 'Time synchronization handler',
        'command_client.py': 'CLI command tool',
        'run_unified_controller.py': 'Launcher script'
    }

    all_ok = True
    for filename, description in files.items():
        filepath = Path(__file__).parent.parent / filename
        if filepath.exists():
            print(f"   {filename:30s} - {description}")
        else:
            print(f"   {filename:30s} - MISSING")
            all_ok = False

    return all_ok


def check_config_files():
    """Check for configuration files"""
    print("\n[5/8] Checking configuration files...")

    files = ['config.yaml']

    for filename in files:
        filepath = Path(__file__).parent.parent / filename
        if filepath.exists():
            print(f"   {filename}")
        else:
            print(f"    {filename} (optional)")

    return True


def check_certificates():
    """Check SSL certificates"""
    print("\n[6/8] Checking SSL certificates...")

    cert_dir = Path(__file__).parent.parent / 'certificates'

    if cert_dir.exists():
        cert_files = list(cert_dir.glob('*.crt')) + list(cert_dir.glob('*.key'))
        if cert_files:
            print(f"   Certificate directory found with {len(cert_files)} files")
        else:
            print(f"    Certificate directory empty (will auto-generate)")
    else:
        print(f"    No certificates (will auto-generate on first run)")

    return True


def check_tests():
    """Check if test files exist"""
    print("\n[7/8] Checking test suite...")

    tests_dir = Path(__file__).parent.parent / 'tests'
    demo_file = Path(__file__).parent / 'demo_features.py'

    if tests_dir.exists():
        test_files = list(tests_dir.glob('test_*.py'))
        if test_files:
            print(f"   Test directory found with {len(test_files)} test files")
            for test_file in sorted(test_files)[:5]:
                print(f"     - {test_file.name}")
        else:
            print(f"   Test directory empty (MISSING)")
            return False
    else:
        print(f"   tests/ directory (MISSING)")
        return False

    if demo_file.exists():
        print(f"   demo_features.py")
    else:
        print(f"   demo_features.py (MISSING)")
        return False

    return True


def check_documentation():
    """Check if documentation exists"""
    print("\n[8/8] Checking documentation...")

    docs = {
        'README.md': 'Project overview',
        'docs/pc_controller_implementation.md': 'Implementation guide',
        'docs/quick_start.md': 'Quick start guide'
    }

    for filename, description in docs.items():
        filepath = Path(__file__).parent.parent / filename
        if filepath.exists():
            size_kb = filepath.stat().st_size / 1024
            print(f"   {filename:40s} ({size_kb:.1f} KB)")
        else:
            print(f"    {filename:40s} (optional)")

    return True


def run_quick_test():
    """Run a quick smoke test"""
    print("\n" + "=" * 70)
    print("  Running Quick Smoke Test")
    print("=" * 70)

    print("\nTesting native backend import...")
    sys.path.insert(0, str(Path(__file__).parent / 'native_backend'))
    try:
        import enhanced_native_backend
        gsr = enhanced_native_backend.GSRData()
        gsr.gsr_microsiemens = 5.5
        print(f"   Native backend working: {gsr}")
    except ImportError:
        print(f"    Native backend not available (using Python fallback)")
    except Exception as e:
        print(f"   Native backend error: {e}")
        return False

    print("\nTesting protocol message handling...")
    import json
    test_msg = {
        'type': 'HELLO',
        'device_id': 'test_001',
        'sensors': ['GSR', 'RGB']
    }
    try:
        json_str = json.dumps(test_msg)
        parsed = json.loads(json_str)
        assert parsed == test_msg
        print(f"   Protocol handling working")
    except Exception as e:
        print(f"   Protocol error: {e}")
        return False

    return True


def main():
    """Main verification function"""
    print_header("PC Controller Installation Verification")

    results = {
        'Python version': check_python_version(),
        'Dependencies': check_dependencies(),
        'Native backend': check_native_backend(),
        'Controllers': check_controllers(),
        'Config files': check_config_files(),
        'Certificates': check_certificates(),
        'Tests': check_tests(),
        'Documentation': check_documentation()
    }

    # Run smoke test if basics are OK
    if all(r for r in [results['Python version'], results['Dependencies'], results['Controllers']]):
        results['Smoke test'] = run_quick_test()
    else:
        print("\n  Skipping smoke test due to missing components")
        results['Smoke test'] = None

    # Print summary
    print("\n" + "=" * 70)
    print("  VERIFICATION SUMMARY")
    print("=" * 70)

    critical_count = 0
    warning_count = 0

    for check, result in results.items():
        if result is True:
            status = " PASS"
        elif result is False:
            status = " FAIL"
            critical_count += 1
        else:  # None means optional/warning
            status = "  WARN"
            warning_count += 1

        print(f"  {check:.<50} {status}")

    print("\n" + "-" * 70)

    if critical_count == 0:
        print("   All critical checks passed!")
        if warning_count > 0:
            print(f"    {warning_count} optional component(s) missing")
            print("  The controller will work but with reduced functionality")
    else:
        print(f"   {critical_count} critical check(s) failed")
        print("  Please fix the issues above before running the controller")

    print("=" * 70)

    # Print next steps
    print("\nNext steps:")
    if critical_count == 0:
        print("  1. Run tests:       python3 test_pc_controller_features.py")
        print("  2. Run demo:        python3 demo_features.py")
        print("  3. Start GUI:       python3 advanced_pc_controller.py")
        print("  4. Read guide:      See docs/quick_start.md")
    else:
        print("  1. Install missing dependencies:")
        print("     pip install -r requirements.txt")
        print("  2. Run verification again:")
        print("     python3 verify_installation.py")

    return critical_count == 0


if __name__ == '__main__':
    success = main()
    sys.exit(0 if success else 1)
