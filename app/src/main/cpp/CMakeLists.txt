cmake_minimum_required(VERSION 3.22.1)
project("ggwavebridge")

add_library(ggwave SHARED
        ggwave/ggwave.cpp
        bridge.cpp
)

find_library(log-lib log)

target_link_libraries(ggwave ${log-lib})
