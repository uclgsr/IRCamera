#!/usr/bin/env python3
"""
Unified PC Controller Launcher

This script launches the appropriate PC controller based on available dependencies.
It serves as the single entry point for all PC controller functionality.
"""

import sys
import subprocess
from pathlib import Path

def check_dependency(module_name, description=""):
    """Check if a Python module is available"""
    try:
        __import__(module_name)
        return True
    except ImportError:
        return False

def check_pyqt6_availability():
    """Check if PyQt6 is fully functional (including display)"""
    try:
        from PyQt6.QtWidgets import QApplication
        from PyQt6.QtCore import QTimer
        import pyqtgraph as pg
        
        # Try to create a minimal application to test display
        app = QApplication.instance()
        if app is None:
            app = QApplication([])
        
        # If we get this far, PyQt6 is working
        return True
    except Exception as e:
        print(f"PyQt6 not fully available: {e}")
        return False

def main():
    """Main launcher function"""
    print("=" * 60)
    print("IRCamera PC Controller Launcher")
    print("=" * 60)
    print()
    
    # Check available controllers
    src_dir = Path(__file__).parent / "src"
    
    controllers = []
    
    # Check for Unified Controller (preferred)
    if (src_dir / "unified_pc_controller.py").exists():
        if check_pyqt6_availability():
            controllers.append({
                'name': 'Unified Controller',
                'file': 'unified_pc_controller.py',
                'description': 'Modern PyQt6 GUI with unified protocol support',
                'priority': 1,
                'requirements': ['PyQt6', 'pyqtgraph', 'numpy']
            })
        else:
            print("  Unified Controller available but PyQt6 not functional")
    
    # Check for Enhanced Controller (fallback)
    if (src_dir / "enhanced_pc_controller.py").exists():
        if check_pyqt6_availability():
            controllers.append({
                'name': 'Enhanced Controller', 
                'file': 'enhanced_pc_controller.py',
                'description': 'PyQt6 GUI with C++ backend',
                'priority': 2,
                'requirements': ['PyQt6', 'pyqtgraph']
            })
    
    # Check for Original Controller (basic fallback)
    if (src_dir / "pc_session_controller.py").exists():
        if check_dependency('tkinter'):
            controllers.append({
                'name': 'Original Controller',
                'file': 'pc_session_controller.py', 
                'description': 'Basic tkinter GUI',
                'priority': 3,
                'requirements': ['tkinter', 'matplotlib']
            })
    
    if not controllers:
        print(" No PC controllers found or dependencies missing")
        print()
        print("Required files:")
        print(f"  - {src_dir / 'unified_pc_controller.py'}")
        print(f"  - {src_dir / 'enhanced_pc_controller.py'}")  
        print(f"  - {src_dir / 'pc_session_controller.py'}")
        print()
        print("Install dependencies:")
        print("  pip install PyQt6 pyqtgraph numpy matplotlib pillow")
        sys.exit(1)
    
    # Sort by priority (lower number = higher priority)
    controllers.sort(key=lambda x: x['priority'])
    
    # Show available controllers
    print("Available Controllers:")
    for i, controller in enumerate(controllers, 1):
        print(f"  {i}. {controller['name']}")
        print(f"     {controller['description']}")
        
        # Check requirements
        missing_reqs = []
        for req in controller['requirements']:
            if not check_dependency(req):
                missing_reqs.append(req)
        
        if missing_reqs:
            print(f"       Missing: {', '.join(missing_reqs)}")
        else:
            print(f"      All requirements available")
        print()
    
    # Auto-select best available controller or let user choose
    if len(sys.argv) > 1 and sys.argv[1] == '--auto':
        # Auto mode - use best available
        selected = controllers[0]
        print(f" Auto-launching: {selected['name']}")
    else:
        # Interactive mode
        if len(controllers) == 1:
            selected = controllers[0]
            print(f" Launching: {selected['name']}")
        else:
            print("Select controller to launch:")
            for i, controller in enumerate(controllers, 1):
                print(f"  {i}) {controller['name']}")
            print(f"  0) Exit")
            print()
            
            while True:
                try:
                    choice = input("Enter choice (1-{} or 0): ".format(len(controllers)))
                    choice = int(choice)
                    
                    if choice == 0:
                        print("Goodbye!")
                        sys.exit(0)
                    elif 1 <= choice <= len(controllers):
                        selected = controllers[choice - 1]
                        break
                    else:
                        print("Invalid choice. Please try again.")
                except (ValueError, KeyboardInterrupt):
                    print("\nGoodbye!")
                    sys.exit(0)
    
    # Launch selected controller
    controller_path = src_dir / selected['file']
    print()
    print(f"Launching {selected['name']}...")
    print(f"File: {controller_path}")
    print()
    
    try:
        # Change to src directory and run the controller
        import os
        os.chdir(src_dir)
        
        # Execute the selected controller
        subprocess.run([sys.executable, selected['file']], check=True)
        
    except subprocess.CalledProcessError as e:
        print(f" Controller failed to start: {e}")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\n Controller stopped by user")
    except Exception as e:
        print(f" Unexpected error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()