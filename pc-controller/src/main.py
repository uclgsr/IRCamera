#!/usr/bin/env python3
"""
Main entry point for IRCamera PC Controller

This script starts the PC controller application with all required services.
"""

import sys
from pathlib import Path


src_dir = Path(__file__).parent
sys.path.insert(0, str(src_dir))

from ircamera_pc.gui import main  

if __name__ == "__main__":
    sys.exit(main())
