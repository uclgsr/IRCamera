#!/usr/bin/env python3
"""
Data Processing Package for IRCamera PC Controller

This package provides data aggregation, processing, and export capabilities
for the multi-modal physiological sensing platform.
"""

from .processing import DataProcessor, GSRIngestor

__all__ = ["DataProcessor", "GSRIngestor"]