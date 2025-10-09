#!/usr/bin/env python3
"""Setup script for building the enhanced_native_backend PyBind11 extension."""

from pybind11.setup_helpers import Pybind11Extension, build_ext
from setuptools import setup
import pybind11

ext_modules = [
    Pybind11Extension(
        "enhanced_native_backend",
        [
            "src/data_processor.cpp",
            "src/pybind_module.cpp",
        ],
        include_dirs=[
            "include",
            pybind11.get_include(),
        ],
        language="c++",
        cxx_std=17,
    ),
]

setup(
    name="enhanced_native_backend",
    version="0.1.0",
    author="IRCamera Project",
    description="Minimal native backend providing a fast GSR packet parser and statistics utilities.",
    ext_modules=ext_modules,
    cmdclass={"build_ext": build_ext},
    zip_safe=False,
    python_requires=">=3.8",
)
