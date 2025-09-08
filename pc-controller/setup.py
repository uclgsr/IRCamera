"""
Setup script for IRCamera PC Controller with native backend
"""

from pybind11.setup_helpers import Pybind11Extension, build_ext
from setuptools import setup, Extension
import pybind11
import platform
import os

# Define the extension module
ext_modules = [
    Pybind11Extension(
        "ircamera_native_backend",
        [
            "native_backend/src/native_backend.cpp",
            "native_backend/src/native_shimmer.cpp", 
            "native_backend/src/native_webcam.cpp",
            "native_backend/src/utils/thread_safe_queue.cpp",
            "native_backend/src/utils/serial_communication.cpp",
        ],
        include_dirs=[
            "native_backend/include",
            pybind11.get_cmake_dir() + "/../include",
        ],
        language='c++',
        cxx_std=17,
    ),
]

# Platform-specific configuration
if platform.system() == "Windows":
    ext_modules[0].libraries.extend(["ws2_32", "setupapi"])
elif platform.system() == "Linux":
    ext_modules[0].libraries.extend(["pthread"])
elif platform.system() == "Darwin":  # macOS
    ext_modules[0].extra_link_args.extend([
        "-framework", "CoreFoundation",
        "-framework", "IOKit"
    ])

# Add OpenCV
try:
    import cv2
    opencv_path = cv2.__path__[0]
    ext_modules[0].include_dirs.append(os.path.join(opencv_path, "headers"))
    
    # Try to find OpenCV libraries
    if platform.system() == "Windows":
        # Windows OpenCV library detection
        ext_modules[0].libraries.extend(["opencv_world"])
    else:
        # Linux/macOS OpenCV library detection
        ext_modules[0].libraries.extend(["opencv_core", "opencv_imgproc", "opencv_imgcodecs", "opencv_videoio"])
        
except ImportError:
    print("Warning: OpenCV not found. Native webcam functionality may not work.")

setup(
    name="ircamera-pc-controller",
    version="1.0.0",
    author="IRCamera Team",
    author_email="support@ircamera.com",
    url="https://github.com/buccancs/IRCamera",
    description="Multi-Modal Physiological Sensing Platform - PC Controller",
    long_description=open("README.md").read() if os.path.exists("README.md") else "",
    long_description_content_type="text/markdown",
    ext_modules=ext_modules,
    cmdclass={"build_ext": build_ext},
    zip_safe=False,
    python_requires=">=3.11",
    install_requires=[
        "PyQt6>=6.4.0",
        "pyqtgraph>=0.13.0",
        "numpy>=1.24.0",
        "pandas>=2.0.0",
        "opencv-python>=4.8.0",
        "websockets>=12.0",
        "asyncio-mqtt>=0.13.0",
        "pyyaml>=6.0",
        "loguru>=0.7.0",
        "jsonschema>=4.17.0",
        "ntplib>=0.4.0",
        "aiofiles>=23.1.0",
        "cryptography>=45.0.7",
        "typing-extensions>=4.8.0",
        "urllib3>=2.5.0",
        "requests>=2.32.3",
        "jinja2>=3.1.6",
        "certifi>=2025.1.1",
        "twisted>=24.7.0",
        "pyjwt>=2.10.1",
        "bleak>=0.20.0",
        "psutil>=5.9.0",
        "elevate>=0.1.3",
        "pybind11>=2.10.0",
    ],
    extras_require={
        "dev": [
            "pytest>=7.4.0",
            "pytest-asyncio>=0.21.0", 
            "pytest-qt>=4.2.0",
            "flake8>=6.0.0",
            "black>=23.0.0",
            "mypy>=1.5.0",
            "bandit>=1.7.0",
            "safety>=2.3.0",
            "autoflake>=2.0.0",
        ],
        "windows": [
            "pywin32>=306",
            "comtypes>=1.1.14",
            "pywifi>=1.1.12",
        ]
    },
    entry_points={
        "console_scripts": [
            "ircamera-pc=src.main:main",
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
    ],
    package_dir={"": "src"},
    packages=[
        "ircamera_pc",
        "ircamera_pc.core",
        "ircamera_pc.gui", 
        "ircamera_pc.network",
        "ircamera_pc.utils",
        "ircamera_pc.tests",
    ],
    package_data={
        "ircamera_pc": ["*.yaml", "*.json"],
    },
    include_package_data=True,
)