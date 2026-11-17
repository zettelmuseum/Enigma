find_package(SDL2_image CONFIG REQUIRED)

if(TARGET SDL2_image::SDL2_image)
    set(_enigma_sdl2_image_target SDL2_image::SDL2_image)
elseif(TARGET SDL2_image::SDL2_image-static)
    set(_enigma_sdl2_image_target SDL2_image::SDL2_image-static)
else()
    message(FATAL_ERROR "vcpkg SDL2_image package did not export a library target")
endif()

if(NOT TARGET SDL2::Image)
    add_library(SDL2::Image INTERFACE IMPORTED)
    set_property(TARGET SDL2::Image PROPERTY INTERFACE_LINK_LIBRARIES "${_enigma_sdl2_image_target}")
endif()

set(SDL2_image_FOUND TRUE)
set(SDL2_IMAGE_FOUND TRUE)
set(SDL2_IMAGE_LIBRARIES SDL2::Image)
