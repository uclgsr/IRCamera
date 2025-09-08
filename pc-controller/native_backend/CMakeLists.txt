cmake_minimum_required(VERSION 3.16)
project(ircamera_native_backend)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(Python3 COMPONENTS Interpreter Development REQUIRED)
find_package(OpenCV REQUIRED)

# Add pybind11
add_subdirectory(pybind11)

# Include directories
include_directories(include)
include_directories(${OpenCV_INCLUDE_DIRS})

# Source files
set(SOURCES
    src/native_shimmer.cpp
    src/native_webcam.cpp
    src/pybind_module.cpp
)

# Create the pybind11 module
pybind11_add_module(native_backend ${SOURCES})

# Link libraries
target_link_libraries(native_backend PRIVATE ${OpenCV_LIBS})

# Set properties
set_target_properties(native_backend PROPERTIES
    CXX_VISIBILITY_PRESET "hidden"
    INTERPROCEDURAL_OPTIMIZATION TRUE
)

# Compiler-specific options
target_compile_definitions(native_backend PRIVATE VERSION_INFO=${EXAMPLE_VERSION_INFO})