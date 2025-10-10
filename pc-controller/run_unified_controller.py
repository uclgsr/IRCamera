#!/usr/bin/env python3
# coverage: ignore file
"""
IRCamera PC Controller Launcher

Simple launcher for the unified PC controller application.
This serves as the single entry point for all PC controller functionality.
"""

import subprocess
import sys
from pathlib import Path


def main():
    """Main launcher function"""
    print("=" * 60)
    print("IRCamera PC Controller")
    print("=" * 60)
    print()

    # Path to the main controller
    controller_path = Path(__file__).parent / "pc_controller.py"

    if not controller_path.exists():
        print("Error: pc_controller.py not found")
        print(f"Expected location: {controller_path}")
        sys.exit(1)

    print("Launching PC Controller...")
    print()

    try:
        # Forward all command-line arguments to pc_controller.py
        subprocess.run([sys.executable, str(controller_path)] + sys.argv[1:], check=True)

    except subprocess.CalledProcessError as e:
        print(f"Controller failed to start: {e}")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\nController stopped by user")
    except Exception as e:
        print(f"Unexpected error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
