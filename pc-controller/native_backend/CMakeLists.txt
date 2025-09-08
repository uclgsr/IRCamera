cmake_minimum_required(VERSION 3.16)
project(ircamera_native_backend)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(Python3 COMPONENTS Interpreter Development REQUIRED)
find_package(OpenCV REQUIRED)

# Add pybind11 - handle both system installation and bundled version
find_package(pybind11 QUIET)
if(NOT pybind11_FOUND)
    # If pybind11 is not found system-wide, add the bundled version
    if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/pybind11/CMakeLists.txt")
        add_subdirectory(pybind11)
    else()
        message(FATAL_ERROR "pybind11 not found. Please install pybind11 or add the pybind11 submodule.")
    endif()
endif()

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