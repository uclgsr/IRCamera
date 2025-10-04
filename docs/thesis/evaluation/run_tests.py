#!/usr/bin/env python3
"""
Test runner for thesis evaluation tests
Runs all multi-sensor data consistency tests and generates reports
"""

import sys
import os
from pathlib import Path

try:
    import pytest
except ImportError:
    print("Error: pytest is not installed")
    print("Install with: pip install -r requirements_thesis.txt")
    sys.exit(1)


def main():
    """Run thesis evaluation tests"""

    thesis_eval_dir = Path(__file__).parent
    os.chdir(thesis_eval_dir.parent.parent)

    print("=" * 70)
    print("Thesis Evaluation Tests - Multi-Sensor Data Consistency")
    print("=" * 70)
    print()

    args = [
        "docs/thesis/evaluation/",
        "-v",
        "--tb=short",
        "-s",
        "--color=yes"
    ]

    if "--html" in sys.argv or "-html" in sys.argv:
        args.extend([
            "--html=docs/thesis/evaluation/reports/test_report.html",
            "--self-contained-html"
        ])

    if "--cov" in sys.argv:
        args.extend([
            "--cov=docs/thesis_evaluation",
            "--cov-report=html:docs/thesis/evaluation/reports/coverage",
            "--cov-report=term"
        ])

    if len(sys.argv) > 1 and sys.argv[1] not in ["--html", "-html", "--cov"]:
        args = sys.argv[1:]

    print(f"Running pytest with args: {' '.join(args)}")
    print()

    exit_code = pytest.main(args)

    print()
    print("=" * 70)
    if exit_code == 0:
        print("All tests passed!")
    else:
        print(f"Tests completed with exit code: {exit_code}")
    print("=" * 70)
    print()
    print("Test outputs saved to: docs/thesis/evaluation/outputs/")
    print("Test reports saved to: docs/thesis/evaluation/reports/")

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
