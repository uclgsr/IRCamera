cmake_minimum_required(VERSION 3.18)
project(ircamera_native_backend)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(Python3 COMPONENTS Interpreter Development REQUIRED)

# Find OpenCV (make it optional for now)
find_package(OpenCV QUIET)
if (OpenCV_FOUND)
    message(STATUS "OpenCV found: ${OpenCV_VERSION}")
    add_definitions(-DHAVE_OPENCV)
else ()
    message(WARNING "OpenCV not found - webcam functionality will be limited")
endif ()

# Add pybind11 - handle both system installation and bundled version
find_package(pybind11 QUIET)
if (NOT pybind11_FOUND)
    # Download pybind11 if not found
    include(FetchContent)
    FetchContent_Declare(pybind11 GIT_REPOSITORY https://github.com/pybind/pybind11 GIT_TAG v2.10.4)
    FetchContent_MakeAvailable(pybind11)
endif ()

# Include directories
include_directories(include)
if (OpenCV_FOUND)
    include_directories(${OpenCV_INCLUDE_DIRS})
endif ()

# Source files
set(SOURCES
        src/native_shimmer.cpp
        src/simple_pybind_module.cpp
)

# Create the pybind11 module
pybind11_add_module(native_backend ${SOURCES})

# Link libraries
if (OpenCV_FOUND)
    target_link_libraries(native_backend PRIVATE ${OpenCV_LIBS})
endif ()

# Set properties
set_target_properties(native_backend PROPERTIES
        CXX_VISIBILITY_PRESET "hidden"
        INTERPROCEDURAL_OPTIMIZATION TRUE
)

# Compiler-specific options
target_compile_definitions(native_backend PRIVATE VERSION_INFO=${EXAMPLE_VERSION_INFO})

# Installation
install(TARGETS native_backend DESTINATION .)
