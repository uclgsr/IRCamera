#!/usr/bin/env python3
"""
Setup script for enhanced_native_backend C++ module
"""

import pybind11
from pybind11 import get_cmake_dir
from pybind11.setup_helpers import Pybind11Extension, build_ext
from setuptools import setup, Extension

# Define the extension module
ext_modules = [
    Pybind11Extension(
        "enhanced_native_backend",
        [
            "src/shimmer.cpp",
            "src/data_processor.cpp",
            "src/pybind_module.cpp",
        ],
        include_dirs=[
            "include",
            pybind11.get_include()
        ],
        language='c++',
        cxx_std=17,
    ),
]

setup(
    name="enhanced_native_backend",
    version="2.0.0",
    author="IRCamera Project",
    author_email="dev@ircamera.project",
    description="High-performance native backend for IRCamera PC controller",
    long_description="Enhanced C++ backend providing high-performance sensor data processing, filtering, and protocol handling for the IRCamera multi-modal recording system.",
    ext_modules=ext_modules,
    cmdclass={"build_ext": build_ext},
    zip_safe=False,
    python_requires=">=3.7",
)
