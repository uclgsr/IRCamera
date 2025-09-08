cmake_minimum_required(VERSION 3.18)
project(ircamera_native_backend)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(pybind11 REQUIRED)
find_package(OpenCV REQUIRED)
find_package(PkgConfig REQUIRED)

# Include directories
include_directories(${OpenCV_INCLUDE_DIRS})
include_directories(include)

# Source files
set(SOURCES
    src/native_shimmer.cpp
    src/native_webcam.cpp
    src/native_backend.cpp
    src/utils/thread_safe_queue.cpp
    src/utils/serial_communication.cpp
)

# Create pybind11 module
pybind11_add_module(ircamera_native_backend ${SOURCES})

# Link libraries
target_link_libraries(ircamera_native_backend PRIVATE ${OpenCV_LIBS})

# Platform-specific configurations
if(WIN32)
    # Windows specific libraries
    target_link_libraries(ircamera_native_backend PRIVATE ws2_32 setupapi)
elseif(UNIX AND NOT APPLE)
    # Linux specific libraries
    target_link_libraries(ircamera_native_backend PRIVATE pthread)
elseif(APPLE)
    # macOS specific libraries
    find_library(COREFOUNDATION_LIBRARY CoreFoundation)
    find_library(IOKIT_LIBRARY IOKit)
    target_link_libraries(ircamera_native_backend PRIVATE ${COREFOUNDATION_LIBRARY} ${IOKIT_LIBRARY})
endif()

# Compiler-specific options
target_compile_definitions(ircamera_native_backend PRIVATE VERSION_INFO=${EXAMPLE_VERSION_INFO})

# Set properties
set_target_properties(ircamera_native_backend PROPERTIES
    CXX_VISIBILITY_PRESET "hidden"
    INTERPROCEDURAL_OPTIMIZATION TRUE
)

# Installation
install(TARGETS ircamera_native_backend DESTINATION .)