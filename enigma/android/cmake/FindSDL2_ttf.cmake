find_package(SDL2_ttf CONFIG REQUIRED)

if(TARGET SDL2_ttf::SDL2_ttf)
    set(_enigma_sdl2_ttf_target SDL2_ttf::SDL2_ttf)
elseif(TARGET SDL2_ttf::SDL2_ttf-static)
    set(_enigma_sdl2_ttf_target SDL2_ttf::SDL2_ttf-static)
else()
    message(FATAL_ERROR "vcpkg SDL2_ttf package did not export a library target")
endif()

if(NOT TARGET SDL2::TTF)
    add_library(SDL2::TTF INTERFACE IMPORTED)
    set_property(TARGET SDL2::TTF PROPERTY INTERFACE_LINK_LIBRARIES "${_enigma_sdl2_ttf_target}")
endif()

set(SDL2_ttf_FOUND TRUE)
set(SDL2_TTF_FOUND TRUE)
set(SDL2_TTF_LIBRARIES SDL2::TTF)
