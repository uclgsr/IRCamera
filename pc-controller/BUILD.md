# Building the Native Backend

## Prerequisites

### System Dependencies
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install cmake build-essential python3-dev

# Install OpenCV
sudo apt-get install libopencv-dev python3-opencv

# For serial communication (Shimmer sensors)
sudo apt-get install libudev-dev

# macOS (via Homebrew)
brew install cmake opencv python3

# Windows (via vcpkg or pre-built)
# Install Visual Studio 2019+ with C++ tools
# Install CMake and OpenCV
```

### Python Dependencies
```bash
cd pc-controller
pip install pybind11[global] cmake
```

## Build Instructions

### Step 1: Clone PyBind11 Submodule (if needed)
```bash
cd pc-controller/native_backend
git submodule add https://github.com/pybind/pybind11.git pybind11
# OR if already exists:
git submodule update --init --recursive
```

### Step 2: Build Native Backend
```bash
cd pc-controller/native_backend

# Create build directory
mkdir build && cd build

# Configure with CMake
cmake .. -DCMAKE_BUILD_TYPE=Release

# Build the native backend
make -j$(nproc)

# Install the Python module
make install
```

### Step 3: Alternative: Setup.py Build
```bash
cd pc-controller
python setup.py build_ext --inplace
```

## Testing Native Backend

```python
# Test imports
try:
    import native_backend
    print("Native backend loaded successfully!")
    
    # Test Shimmer detection
    shimmer_ports = native_backend.get_shimmer_ports()
    print(f"Available Shimmer ports: {shimmer_ports}")
    
    # Test camera detection  
    cameras = native_backend.get_available_cameras()
    print(f"Available cameras: {cameras}")
    
except ImportError as e:
    print(f"Failed to import native backend: {e}")
```

## Troubleshooting

### Common Issues

1. **OpenCV not found**
   ```bash
   # Set OpenCV_DIR if needed
   export OpenCV_DIR=/usr/local/lib/cmake/opencv4
   ```

2. **Serial port permissions**
   ```bash
   # Add user to dialout group (Linux)
   sudo usermod -a -G dialout $USER
   # Logout and login again
   ```

3. **PyBind11 compilation errors**
   ```bash
   # Ensure Python development headers
   sudo apt-get install python3-dev
   
   # Check Python version compatibility
   python3 -c "import pybind11; print(pybind11.__version__)"
   ```

4. **CMake configuration issues**
   ```bash
   # Clear build cache
   rm -rf build/
   mkdir build && cd build
   
   # Verbose configuration
   cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_VERBOSE_MAKEFILE=ON
   ```

## Build Configuration Options

```bash
# Debug build
cmake .. -DCMAKE_BUILD_TYPE=Debug

# Release with debug info
cmake .. -DCMAKE_BUILD_TYPE=RelWithDebInfo

# Specify Python executable
cmake .. -DPYTHON_EXECUTABLE=/usr/bin/python3.11

# Custom OpenCV installation
cmake .. -DOpenCV_DIR=/custom/opencv/path/lib/cmake/opencv4
```