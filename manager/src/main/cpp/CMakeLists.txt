cmake_minimum_required(VERSION 3.22.1)

project("nexussu")

# Create the shared library from your C++ file
add_library(
        nexus_bridge
        SHARED
        nexus_bridge.cpp)

# Link the Android logging library so we can see output in Logcat
find_library(
        log-lib
        log)

target_link_libraries(
        nexus_bridge
        ${log-lib})
        
