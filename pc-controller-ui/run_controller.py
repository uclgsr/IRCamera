#!/usr/bin/env python3
"""
Simple launcher for PC Session Controller
"""

import sys
import os

# Add src directory to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

try:
    from pc_session_controller import main
    main()
except ImportError as e:
    print(f"Import error: {e}")
    print("Please ensure you're running from the pc-controller-ui directory")
    sys.exit(1)
except Exception as e:
    print(f"Error: {e}")
    sys.exit(1)