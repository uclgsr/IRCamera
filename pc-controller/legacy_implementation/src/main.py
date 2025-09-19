#!/usr/bin/env python3


import sys
from pathlib import Path


src_dir = Path(__file__).parent
sys.path.insert(0, str(src_dir))

from ircamera_pc.gui import main

if __name__ == "__main__":
    sys.exit(main())
