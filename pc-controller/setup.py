"""Setup helpers for building the native backend."""
# coverage: ignore file

import os
import platform

import pybind11
from pybind11.setup_helpers import ParallelCompile, Pybind11Extension, build_ext
from setuptools import find_packages, setup

ParallelCompile("NPY_NUM_BUILD_JOBS").install()

extra_compile_args = []
extra_link_args = []

if platform.system() == "Windows":
    extra_compile_args = ["/std:c++17", "/O2"]
elif platform.system() in ["Linux", "Darwin"]:
    extra_compile_args = ["-std=c++17", "-O3", "-ffast-math"]

try:
    import cv2

    opencv_include_dirs = [cv2.includes()]
    opencv_libs = []
except ImportError:
    opencv_include_dirs = []
    opencv_libs = []

ext_modules = [
    Pybind11Extension(
        "native_backend",
        sources=[
            "native_backend/src/native_shimmer.cpp",
            "native_backend/src/native_webcam.cpp",
            "native_backend/src/pybind_module.cpp",
        ],
        include_dirs=[
                         "native_backend/include",
                         pybind11.get_cmake_dir() + "/../../../include",
                     ]
                     + opencv_include_dirs,
        libraries=opencv_libs,
        extra_compile_args=extra_compile_args,
        extra_link_args=extra_link_args,
        cxx_std=17,
    ),
]

setup(
    name="ircamera-pc-controller",
    version="1.0.0",
    author="IRCamera Team",
    author_email="contact@ircamera.org",
    url="https://github.com/buccancs/IRCamera",
    description="Multi-Modal Physiological Sensing Platform - PC Controller",
    long_description=open("README.md").read() if os.path.exists("README.md") else "",
    long_description_content_type="text/markdown",
    ext_modules=ext_modules,
    cmdclass={"build_ext": build_ext},
    python_requires=">=3.11",
    install_requires=[
        "PyQt6>=6.4.0",
        "pyqtgraph>=0.13.0",
        "numpy>=1.24.0",
        "pandas>=2.0.0",
        "opencv-python>=4.8.0",
        "h5py>=3.8.0",
        "zeroconf>=0.100.0",
        "loguru>=0.7.0",
        "websockets>=12.0",
        "asyncio-mqtt>=0.13.0",
        "pyyaml>=6.0",
    ],
    extras_require={
        "dev": [
            "pytest>=7.0.0",
            "pytest-asyncio>=0.21.0",
            "pytest-cov>=4.0.0",
            "black>=23.0.0",
            "mypy>=1.0.0",
        ],
        "test": [
            "pytest>=7.0.0",
            "pytest-asyncio>=0.21.0",
            "pytest-cov>=4.0.0",
        ],
    },
    py_modules=["pc_controller", "protocol_adapter", "sync_handler", "command_client"],
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Science/Research",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: C++",
        "Topic :: Scientific/Engineering",
        "Topic :: System :: Hardware",
        "Topic :: Multimedia :: Video :: Capture",
    ],
    include_package_data=True,
    zip_safe=False,
)
