find_package(SDL2_mixer CONFIG REQUIRED)

if(TARGET SDL2_mixer::SDL2_mixer)
    set(_enigma_sdl2_mixer_target SDL2_mixer::SDL2_mixer)
elseif(TARGET SDL2_mixer::SDL2_mixer-static)
    set(_enigma_sdl2_mixer_target SDL2_mixer::SDL2_mixer-static)
else()
    message(FATAL_ERROR "vcpkg SDL2_mixer package did not export a library target")
endif()

if(NOT TARGET SDL2::Mixer)
    add_library(SDL2::Mixer INTERFACE IMPORTED)
    set_property(TARGET SDL2::Mixer PROPERTY INTERFACE_LINK_LIBRARIES "${_enigma_sdl2_mixer_target}")
endif()

set(SDL2_mixer_FOUND TRUE)
set(SDL2_MIXER_FOUND TRUE)
set(SDL2_MIXER_LIBRARIES SDL2::Mixer)
