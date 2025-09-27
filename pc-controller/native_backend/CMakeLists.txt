cmake_minimum_required(VERSION 3.18)
project(enhanced_native_backend)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(Python3 COMPONENTS Interpreter Development REQUIRED)

# Find OpenCV (optional for webcam functionality)
find_package(OpenCV QUIET)
if (OpenCV_FOUND)
    message(STATUS "OpenCV found: ${OpenCV_VERSION}")
    add_definitions(-DHAVE_OPENCV)
else ()
    message(WARNING "OpenCV not found - webcam functionality will be limited")
endif ()

# Add pybind11
find_package(pybind11 QUIET)
if (NOT pybind11_FOUND)
    # Download pybind11 if not found
    include(FetchContent)
    FetchContent_Declare(pybind11
            GIT_REPOSITORY https://github.com/pybind/pybind11
            GIT_TAG v2.11.1
    )
    FetchContent_MakeAvailable(pybind11)
endif ()

# Include directories
include_directories(include)
if (OpenCV_FOUND)
    include_directories(${OpenCV_INCLUDE_DIRS})
endif ()

# Source files
set(SOURCES
        src/enhanced_shimmer.cpp
        src/data_processor.cpp
        src/enhanced_pybind_module.cpp
)

# Add optional webcam source if OpenCV is available
if (OpenCV_FOUND)
    list(APPEND SOURCES src/native_webcam.cpp)
endif ()

# Create the pybind11 module
pybind11_add_module(enhanced_native_backend ${SOURCES})

# Link libraries
if (OpenCV_FOUND)
    target_link_libraries(enhanced_native_backend PRIVATE ${OpenCV_LIBS})
endif ()

# Compiler-specific options for performance
target_compile_options(enhanced_native_backend PRIVATE
        $<$<CXX_COMPILER_ID:GNU>:-O3 -march=native -ffast-math>
        $<$<CXX_COMPILER_ID:Clang>:-O3 -march=native -ffast-math>
        $<$<CXX_COMPILER_ID:MSVC>:/O2>
)

# Set properties
set_target_properties(enhanced_native_backend PROPERTIES
        CXX_VISIBILITY_PRESET "hidden"
        INTERPROCEDURAL_OPTIMIZATION TRUE
)

# Installation
install(TARGETS enhanced_native_backend DESTINATION .)
