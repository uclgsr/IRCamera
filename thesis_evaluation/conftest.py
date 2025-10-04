"""
Pytest configuration for thesis evaluation tests
"""

import pytest
import os
from pathlib import Path


def pytest_configure(config):
    """Configure pytest for thesis evaluation tests"""
    config.addinivalue_line(
        "markers",
        "synthetic: mark test as synthetic event injection test"
    )
    config.addinivalue_line(
        "markers",
        "realrun: mark test as real hardware recording test"
    )
    config.addinivalue_line(
        "markers",
        "duration: mark test as session duration consistency test"
    )

    output_dir = Path("thesis_evaluation/outputs")
    output_dir.mkdir(parents=True, exist_ok=True)

    reports_dir = Path("thesis_evaluation/reports")
    reports_dir.mkdir(parents=True, exist_ok=True)


@pytest.fixture(scope="session")
def output_base_dir():
    """Base directory for test outputs"""
    return Path("thesis_evaluation/outputs")


@pytest.fixture(scope="session")
def reports_base_dir():
    """Base directory for test reports"""
    return Path("thesis_evaluation/reports")


@pytest.fixture(autouse=True)
def test_environment(monkeypatch):
    """Set up test environment variables"""
    monkeypatch.setenv("THESIS_EVAL_MODE", "1")
    monkeypatch.setenv("TEST_OUTPUT_DIR", "thesis_evaluation/outputs")
