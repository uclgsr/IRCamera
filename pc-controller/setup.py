from setuptools import setup, find_packages
from pybind11.setup_helpers import Pybind11Extension, build_ext
from pybind11.setup_helpers import ParallelCompile
import pybind11
import platform
import os

# Enable parallel compilation
ParallelCompile("NPY_NUM_BUILD_JOBS").install()

# Platform-specific OpenCV library configuration
def get_opencv_libs():
    """Get appropriate OpenCV library names for the current platform."""
    if platform.system() == "Windows":
        return ["opencv_world"]  # Windows often uses unified opencv_world
    else:
        # Linux/macOS - use individual libraries
        return ["opencv_core", "opencv_imgproc", "opencv_videoio", "opencv_imgcodecs"]

# Check if pybind11 directory exists
pybind11_include_dirs = [pybind11.get_cmake_dir() + "/../include"]

# Try to find additional OpenCV include directories
opencv_include_dirs = []
common_opencv_paths = [
    "/usr/include/opencv4",
    "/usr/local/include/opencv4", 
    "/opt/homebrew/include/opencv4",  # macOS Homebrew
    "C:/opencv/build/include"  # Windows common path
]
for path in common_opencv_paths:
    if os.path.exists(path):
        opencv_include_dirs.append(path)

# Define the C++ extension
ext_modules = [
    Pybind11Extension(
        "native_backend",
        [
            "native_backend/src/native_shimmer.cpp",
            "native_backend/src/native_webcam.cpp", 
            "native_backend/src/pybind_module.cpp",
        ],
        include_dirs=[
            "native_backend/include",
        ] + pybind11_include_dirs + opencv_include_dirs,
        libraries=get_opencv_libs(),
        cxx_std=17,
        define_macros=[("VERSION_INFO", '"1.0.0"')],
    ),
]

setup(
    name="ircamera-pc-controller",
    version="1.0.0",
    author="IRCamera Team",
    author_email="contact@ircamera.org",
    url="https://github.com/buccancs/IRCamera",
    description="Multi-Modal Physiological Sensing Platform - PC Controller",
    long_description="PC Controller for the IRCamera multi-modal physiological sensing platform with native C++ backend for high-performance sensor interfacing.",
    
    packages=find_packages(where="src"),
    package_dir={"": "src"},
    
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
        "pybind11>=2.10.0",
        "jsonschema>=4.17.0",
        "asyncio-mqtt>=0.13.0",
        "cryptography>=45.0.7",
        "psutil>=5.9.0",
        "bleak>=0.20.0",
    ],
    
    extras_require={
        "dev": [
            "pytest>=7.4.0",
            "pytest-qt>=4.2.0",
            "pytest-asyncio>=0.21.0",
            "black>=23.0.0",
            "flake8>=6.0.0",
            "mypy>=1.5.0",
        ],
        "test": [
            "pytest>=7.4.0",
            "pytest-qt>=4.2.0", 
            "pytest-asyncio>=0.21.0",
        ],
    },
    
    entry_points={
        "console_scripts": [
            "ircamera-pc=ircamera_pc.gui.main:main",
            "ircamera-demo=integration_example:main",
        ],
    },
    
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Science/Research",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Programming Language :: C++",
        "Topic :: Scientific/Engineering",
        "Topic :: System :: Hardware",
        "Topic :: Multimedia :: Video :: Capture",
    ],
    
    keywords="physiological-sensing gsr thermal-imaging multi-modal research shimmer",
    
    project_urls={
        "Bug Reports": "https://github.com/buccancs/IRCamera/issues",
        "Source": "https://github.com/buccancs/IRCamera",
        "Documentation": "https://github.com/buccancs/IRCamera/tree/main/docs",
    },
    
    zip_safe=False,
)