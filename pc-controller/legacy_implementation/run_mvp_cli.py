#!/usr/bin/env python3
"""
IRCamera PC Controller Hub - MVP CLI Launcher
Direct launcher for the CLI interface
"""

import sys
from pathlib import Path

# Add src directory to path
src_dir = Path(__file__).parent / "src"
sys.path.insert(0, str(src_dir))


def main():
    """Launch the MVP CLI interface directly"""
    try:
        from ircamera_pc.gui.cli_mvp import main as cli_main
        return cli_main()
    except ImportError as e:
        print(f"Failed to import CLI: {e}")
        print("Please ensure the MVP modules are properly installed")
        return 1
    except Exception as e:
        print(f"CLI launcher failed: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
