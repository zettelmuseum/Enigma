cmake_minimum_required(VERSION 3.18)
project(EnigmaHostTools C)

if(NOT DEFINED UPSTREAM_SOURCE OR NOT EXISTS "${UPSTREAM_SOURCE}/lib-src/lua/CMakeLists.txt")
    message(FATAL_ERROR "Set UPSTREAM_SOURCE to the checked-out Enigma source directory")
endif()

add_subdirectory("${UPSTREAM_SOURCE}/lib-src/lua" lua)
add_executable(tolua
    "${UPSTREAM_SOURCE}/tools/tolua.c"
    "${UPSTREAM_SOURCE}/tools/toluabind.c"
)
target_link_libraries(tolua PRIVATE lua m)
