#!/usr/bin/env python3
"""
Launch script for IRCamera PC Controller Hub MVP

This script launches the complete MVP application with all components.
Use this to start the Hub application for testing with Android devices.
"""

import os
import sys
from pathlib import Path


src_dir = Path(__file__).parent / "src"
sys.path.insert(0, str(src_dir))


def main():
    """Launch the IRCamera Hub MVP application."""

    print("=" * 70)
    print("IRCamera PC Controller Hub - MVP Application")
    print("=" * 70)
    print("Starting Hub-and-Spoke Architecture Implementation...")
    print()

    
    print("Environment Check:")
    try:
        import PyQt6
        print("✓ PyQt6 GUI framework available")
    except ImportError:
        print("✗ PyQt6 not available - GUI will run in headless mode")

    try:
        from zeroconf import Zeroconf
        print("✓ Zeroconf service discovery available")
    except ImportError:
        print("⚠ Zeroconf not available - using fallback discovery")

    print()

    
    if "DISPLAY" not in os.environ and "QT_QPA_PLATFORM" not in os.environ:
        os.environ["QT_QPA_PLATFORM"] = "offscreen"
        print("ℹ Running in headless mode (no display detected)")
        print()

    
    try:
        from ircamera_pc.gui.app_mvp import main as app_main

        print("🚀 Launching IRCamera Hub Application...")
        print("Features enabled:")
        print("  • Device discovery via mDNS")
        print("  • Session management")
        print("  • Multi-device coordination")
        print("  • Real-time monitoring")
        print("  • JSON protocol communication")
        print()
        print("Use Ctrl+C to stop the application")
        print("=" * 70)

        return app_main()

    except ImportError as e:
        print(f"❌ Failed to import application: {e}")
        print()
        print("Please ensure all dependencies are installed:")
        print("  pip install PyQt6 loguru zeroconf")
        return 1

    except KeyboardInterrupt:
        print("\n👋 Application stopped by user")
        return 0

    except Exception as e:
        print(f"❌ Application error: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
