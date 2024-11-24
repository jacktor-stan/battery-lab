cmake_minimum_required(VERSION 3.4.1)

project(BatteryLab)

set(CMAKE_CXX_STANDARD 17)  # Gunakan C++17
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(
        native-jacktor
        SHARED
        root-checker.cpp)

find_library(
        log-jacktor
        log)

target_link_libraries(
        native-jacktor
        ${log-jacktor})