# Bridge the vcpkg SDL2 config package to Enigma's legacy FindSDL2 API.
find_package(SDL2 CONFIG REQUIRED)

if(TARGET SDL2::SDL2)
    set(_enigma_sdl2_target SDL2::SDL2)
elseif(TARGET SDL2::SDL2-static)
    set(_enigma_sdl2_target SDL2::SDL2-static)
else()
    message(FATAL_ERROR "vcpkg SDL2 package did not export an SDL2 library target")
endif()

if(NOT TARGET SDL2::Core)
    add_library(SDL2::Core INTERFACE IMPORTED)
    set_property(TARGET SDL2::Core PROPERTY INTERFACE_LINK_LIBRARIES "${_enigma_sdl2_target}")
endif()

set(SDL2_FOUND TRUE)
set(SDL2_LIBRARIES SDL2::Core)
set(SDL2_INCLUDE_DIRS "${VCPKG_INSTALLED_DIR}/${VCPKG_TARGET_TRIPLET}/include/SDL2")
