cmake_minimum_required(VERSION 3.18.1)

project(outguess)

set(CMAKE_VERBOSE_MAKEFILE ON)
set(CMAKE_CXX_STANDARD 17)

# Add our source files
file(GLOB LIBOUTGUESS_SOURCES CONFIGURE_DEPENDS
    "../../../cpp/outguess/*.cpp"
    "../../../cpp/outguess/*.c"
)

# Include directories
include_directories(
    ../../../cpp/outguess
)

# Create the library
add_library(
    outguess
    SHARED
    ${LIBOUTGUESS_SOURCES}
    outguess_jni.cpp
)

# Find required libraries
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

# Link libraries
target_link_libraries(
    outguess
    ${log-lib}
    ${jnigraphics-lib}
)

# Compiler-specific options
target_compile_options(outguess PRIVATE
    -Wall
    -Wextra
    -O2
    -fvisibility=hidden
)