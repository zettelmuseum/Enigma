# Enigma Android port

## Current target

- Offline single-player Enigma for Android, ARM64 only.
- Preserve the complete vendored game data tree, including all 1,718 level files.
- Keep WAV sound effects; disable music playback and omit music assets.
- Remove online services, multiplayer UI/transport, and the level-pack composer/editor.
- Use SDL2 Android activity and package native libraries with 16-KB ELF LOAD alignment.
- No INTERNET permission.

## Source policy

The Enigma source is vendored directly in `enigma/source/`. Its source snapshot is identified by `enigma/source/SOURCE_REVISION`. This is a fixed, standalone source tree. The Android build does not clone Enigma, compare it with another source tree, apply patch files, or synchronize it with a remote Enigma repository.

The old patch directory, patch-check workflow, and upstream lock file have been removed. Android toolchain and third-party dependency pins are kept separately in `enigma/android/toolchain.lock`.

## Integrated source changes

The following changes are already committed directly in `enigma/source/`:

- Android builds create the SDL-loaded shared library `libmain.so`; the Android link uses 16-KB maximum and common page sizes.
- The Lua binding generator is built for and executed on the Linux host during Android cross-compilation.
- Android source lists omit the Level Pack Composer and multiplayer transport; developer Oxyd command-line tools are not built for Android.
- CURL and ENet initialization, cleanup, includes, and link dependencies are removed. The remote-file compatibility entry point fails explicitly rather than accessing the network.
- Network and external homepage/documentation menu entries are removed.
- Music initialization is disabled while the sound-effects engine remains enabled. The music data directory is not installed or packaged.
- Single-finger relative touch movement is translated into the existing mouse-force control path.
- Android stdout/stderr redirection uses Bionic-compatible calls. Xerces startup checkpoints are written to the shared Error.log for diagnosis.

## Build

`.github/workflows/build-enigma-android.yml` builds the vendored source, builds host `tolua`, cross-compiles ARM64 dependencies and Enigma, packages the offline data and native libraries, verifies 16-KB ELF and APK alignment, and uploads the debug APK artifact.

Dependency caches are independent of Enigma source changes: vcpkg binary packages, installed Android dependencies, downloads, Android SDK, SDL Java sources, and vcpkg itself are cached separately. Host generator and CMake build caches include the vendored source in their keys.

## Validation status

The source snapshot import completed successfully. The direct source integration and workflow conversion are committed. The first complete build using only the vendored source has not yet been confirmed successful. Until that run completes, native compilation, APK packaging, installation, and on-device touch behavior remain unverified.
