#!/usr/bin/env python3
"""
IRCamera PC Controller Launcher

Smart launcher that detects dependencies and provides guidance for running
the unified PC controller with optimal configuration.
"""

import os
import sys
import subprocess
from pathlib import Path

def check_python_version():
    """Check if Python version is compatible"""
    if sys.version_info < (3, 7):
        print("❌ Python 3.7 or higher is required")
        print(f"   Current version: {sys.version}")
        return False
    print(f"✅ Python {sys.version.split()[0]} (OK)")
    return True

def check_dependencies():
    """Check for required and optional dependencies"""
    required = ['PyQt6']
    optional = ['pyqtgraph', 'numpy', 'Pillow', 'cryptography']
    
    missing_required = []
    missing_optional = []
    
    for pkg in required:
        try:
            __import__(pkg)
            print(f"✅ {pkg} (required)")
        except ImportError:
            missing_required.append(pkg)
            print(f"❌ {pkg} (required) - MISSING")
    
    for pkg in optional:
        try:
            __import__(pkg)
            print(f"✅ {pkg} (optional)")
        except ImportError:
            missing_optional.append(pkg)
            print(f"⚠️  {pkg} (optional) - missing")
    
    return missing_required, missing_optional

def install_dependencies(packages):
    """Install missing packages"""
    if not packages:
        return True
    
    print(f"\nInstalling missing packages: {', '.join(packages)}")
    try:
        subprocess.check_call([sys.executable, '-m', 'pip', 'install'] + packages)
        print("✅ Installation successful")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ Installation failed: {e}")
        return False

def get_launch_mode():
    """Determine the best launch mode based on available components"""
    try:
        import PyQt6
        return "gui"
    except ImportError:
        return "cli"

def main():
    print("=" * 60)
    print("IRCamera PC Controller Launcher")
    print("=" * 60)
    
    # Check Python version
    if not check_python_version():
        sys.exit(1)
    
    print("\nChecking dependencies...")
    missing_required, missing_optional = check_dependencies()
    
    # Handle missing required dependencies
    if missing_required:
        print(f"\n❌ Missing required dependencies: {', '.join(missing_required)}")
        
        response = input("\nInstall missing dependencies? (y/n): ").lower().strip()
        if response == 'y':
            if not install_dependencies(missing_required):
                print("❌ Failed to install required dependencies")
                sys.exit(1)
        else:
            print("❌ Cannot run without required dependencies")
            sys.exit(1)
    
    # Recommend optional dependencies
    if missing_optional:
        print(f"\n⚠️  Optional dependencies missing: {', '.join(missing_optional)}")
        print("   These provide enhanced functionality:")
        print("   - pyqtgraph: High-performance real-time plotting")
        print("   - numpy: Numerical computations") 
        print("   - Pillow: Image processing and display")
        print("   - cryptography: SSL/TLS encryption support")
        
        response = input("\nInstall optional dependencies? (y/n): ").lower().strip()
        if response == 'y':
            install_dependencies(missing_optional)
    
    # Determine launch configuration
    mode = get_launch_mode()
    
    print(f"\n🚀 Launching PC Controller in {mode.upper()} mode...")
    print("=" * 60)
    
    # Build command line arguments
    pc_controller_path = Path(__file__).parent / "pc_controller.py"
    cmd = [sys.executable, str(pc_controller_path)]
    
    if mode == "cli":
        cmd.append("--cli")
    
    # Add any additional arguments passed to this script
    cmd.extend(sys.argv[1:])
    
    # Launch the controller
    try:
        os.execv(sys.executable, cmd)
    except Exception as e:
        print(f"❌ Failed to launch PC Controller: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()