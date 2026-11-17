# Enigma Android Port – Complete Change Report Compared with Upstream

**Comparison date:** 17 November 2026  
**Local repository:** [zettelmuseum/Enigma](https://github.com/zettelmuseum/Enigma), branch `main`  
**Local revision before this document was created:** `3e7d40220817a040550412988e8d1f52d2bc1c54`  
**Reference:** [Enigma-Game/Enigma](https://github.com/Enigma-Game/Enigma), branch `master`  
**Compared upstream revision:** `6f0c51b26cc013cb6c935046d7d1e6a20bea4179`  
**Source provenance:** The contents of `enigma/source/SOURCE_REVISION` exactly match `6f0c51b26cc013cb6c935046d7d1e6a20bea4179`.

This document describes the state actually present in the repository. It distinguishes changes to the Enigma C++/Lua source code, additional Android files, build changes, and upstream components intentionally not included. File comparisons were performed using the recursive Git trees and Git object IDs. For each path, `enigma/source/<upstream-path>` was compared with `<upstream-path>` in upstream.

---

## 1. Results of the complete file comparison

| Comparison | Result |
|---|---:|
| Entries in the embedded local source tree `enigma/source/` | 5,421 |
| Entries in the official upstream tree | 6,585 |
| Paths present in both trees | 5,420 |
| Shared entries with identical Git object IDs | 5,390 |
| Entries with different Git object IDs | 30 |
| Of these, source files actually modified | **24** |
| Of these, directory trees changed as a consequence of file changes | 6 |
| Additional local file | `SOURCE_REVISION` |
| Entries present only upstream | 1,165 |

The 1,165 entries present only upstream consist of **1,146 file objects and 19 directory entries**. They are entirely located in these two directories:

- `attic/`: archived or non-runtime materials, including historical graphics, XCF/SVG source files, older assets, flags, and templates.
- `doc/`: upstream documentation.

No other missing active source paths outside these two directories were found in the comparison. The actual Enigma source base is therefore largely complete in the embedded tree; it is simply located under `enigma/source/` rather than directly in the repository root.

### Level and music data

- The `data/levels/` directory tree has the same Git object ID on both sides.
- Both sides contain exactly **1,718 level files**.
- All 16 files under `data/music/` are still present in the local source tree and are identical in content to upstream. However, they are neither installed through the adapted data CMake file nor included in the Android data archive.
- No other active data assets were identified as changed files. The change to the data directory concerns its CMake integration, not the level files.

---

## 2. Overall design of the port

Upstream is a general-purpose Enigma project. The local repository adds a standalone Android build for **ARM64 / `arm64-v8a`** and reduces the application to an offline, single-player port.

Goals and consequences:

- The game and level data run locally on the device.
- No Android Internet permission.
- No CURL-based file downloads.
- No ENet multiplayer or network menus.
- No Level Pack Composer in the Android application.
- No music playback and no music files in the APK data archive.
- WAV sound effects are retained.
- Android uses SDL2 and loads the native Enigma library `libmain.so`.
- Native libraries are built and checked with at least 16 KB `PT_LOAD` alignment.
- Level data is extracted into the app's internal data directory; the preferences path is located under `Android/media/Enigma`.

The changes were made directly in `enigma/source/`. There is no patch series applied at runtime or by the workflow, and no automatic synchronization with an external Enigma source directory.

---

## 3. All 24 modified source files

### 3.1 CMake and native build structure

| File | Change compared with upstream |
|---|---|
| `CMakeLists.txt` | The mandatory CURL lookup (`find_package(CURL REQUIRED)`) was removed. The `tools/` subdirectory is now included only for non-Android builds. |
| `data/CMakeLists.txt` | `add_subdirectory(music)` was removed. Music data is therefore no longer treated as an installed runtime asset. |
| `lib-src/CMakeLists.txt` | `add_subdirectory(enet)` was removed. ENet is no longer built for this port. |
| `lib-src/lua/CMakeLists.txt` | `-U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=0` was added to the Lua target. This is the FORTIFY workaround used for the Lua library during Android debugging. **Important:** The option is not currently inside an `if(ANDROID)` block, so it also applies generally to non-Android builds that use this CMake file. |
| `lib-src/oxydlib/CMakeLists.txt` | The helper programs `extractbitmaps` and `printstats` are not created as executable host programs for Android builds. The library itself remains part of the build. |
| `src/CMakeLists.txt` | On Android, the target is built as a shared library rather than a regular executable. Its output name is set to `main`, producing `libmain.so`. Android linker options set `-Wl,-z,max-page-size=16384` and `-Wl,-z,common-page-size=16384`. Level Pack Composer files and multiplayer source modules are removed from the target's source list. CURL and ENet link dependencies are dropped. For the Lua bindings, cross-compilation uses an explicit host executable through `TOLUA_EXECUTABLE`. |

Using a host-built `tolua` is important for cross-compilation: the generator must run on the Linux build machine and must not be executed as an ARM64 Android program. The new CMake logic therefore requires an explicit host path for Android and uses it in both `COMMAND` and `DEPENDS`.

### 3.2 Music, networking, and file access

| File | Change compared with upstream |
|---|---|
| `src/MusicManager.cc` | The central control functions for menu, loading, and in-game music, as well as the music tick and initialization functions, have been turned into no-ops. The interfaces remain available, but music playback is no longer started. |
| `src/client.cc` | ENet client code was removed. Network startup reports success for the offline port but does not start a transport. The file also contains Android touch and window-event handling (see Section 4). |
| `src/client_internal.hh` | ENet headers, network start/stop methods, and the fields for the ENet host and server peer were removed. |
| `src/file.cc` | CURL includes, global CURL initialization, the download callback, and CURL cleanup code were removed. `Downloadfile()` no longer downloads anything from the Internet; instead, it explicitly throws `XLevelLoading` with a message that Internet access is disabled. |
| `src/file.hh` | The declarations `InitCurl()` and `ShutdownCurl()` were removed. |
| `src/server.cc` | The ENet include, global network address and host variable, and destruction of the ENet host during shutdown were removed. |

This removes both the previous CURL file-download mechanism and the ENet network transport from the Android build. This goes beyond hiding menu entries: the corresponding initialization, shutdown, and linking paths are also removed.

**Limitation:** The `Ratings update` menu item is still present in `src/gui/OptionsMenu.cc`. Its behavior when invoked offline has not been verified by the build test. Since `Downloadfile()` now throws an exception, this menu item should be considered an outstanding runtime test rather than a working online feature.

### 3.3 User interface and Composer

| File | Change compared with upstream |
|---|---|
| `src/gui/LevelMenu.cc` | The Composer integration and the F6 function for transferring all levels from the index into the Composer were removed. |
| `src/gui/LevelPackConfig.cc` | The `Compose Pack` button and its action path were removed. The former button variable is retained as an empty `Label`; the Composer is no longer opened from this screen. |
| `src/gui/MainMenu.cc` | The entire Network menu, including start/join actions, was removed. The Network Game button is no longer present. The Homepage and Documentation buttons were removed from the help area. The menu layout was adjusted to account for the smaller number of entries. |
| `src/gui/MainMenu.hh` | The variables for Homepage, Documentation, and Network Game, as well as the `NetworkMenu` class and `ShowNetworkMenu()`, were removed. |
| `src/gui/OptionsMenu.cc` | Music volume, menu music, and in-game music were removed from the options. Soundset, sound volume, and other sound options remain available. |

Composer implementation files may still exist in the source tree; however, they are no longer included in the Android Enigma target, and the corresponding menu entries/actions have been removed. This is a build/UI deactivation, not a blanket deletion of all Composer source files from the repository.

### 3.4 Android input and app resume handling

The main changes are in `src/client.cc`:

1. **Touch-generated mouse clicks are filtered out.** `SDL_MOUSEBUTTONDOWN` and `SDL_MOUSEBUTTONUP` events with `SDL_TOUCH_MOUSEID` are not passed to normal mouse-click handling. Conversely, synthetic finger events with `SDL_MOUSE_TOUCHID` are ignored.
2. **Native SDL finger events are processed.** One active finger is tracked using its touch and finger IDs. During an active touch, automatic inventory pickup is disabled through `player::InhibitPickup(true)` and re-enabled on release.
3. **Multi-taps are distinguished by timing.** The sequence uses a 300 ms time window. A triple tap triggers the inventory action `server::Msg_ActivateItem()` after the window expires. If a fourth tap begins within the window, the triple-tap action is not triggered yet. When the fourth tap is released, `rotate_inventory(+1)` is executed instead.
4. **Window focus loss resets touch state.** An incomplete tap is discarded and any active pickup inhibition is lifted.
5. **The screen is redrawn when the app resumes.** `display::RedrawAll()` is called for `FOCUS_GAINED`, `EXPOSED`, `RESTORED`, `SIZE_CHANGED`, and `RESIZED`. This addresses black or stale screens after returning to the app and after SDL window-surface changes.
6. **Relative mouse movement is preserved.** `SDL_MOUSEMOTION` is still translated through `xrel`/`yrel` into `server::Msg_MouseForce()`. Normal physical mouse handling through `on_mousebutton()` is also preserved.

The current mapping is also documented in the local README: **triple tap = use inventory item; quadruple tap = rotate inventory.** The code therefore explicitly does not use a double-tap mapping.

### 3.5 Lua diagnostic instrumentation

The following Lua files were modified only to add Android diagnostic output. The output is guarded by `#ifdef __ANDROID__`:

| File | Diagnostic points |
|---|---|
| `lib-src/lua/lapi.c` | Output before and after the garbage-collection step and after closure allocation in `lua_pushcclosure()`. |
| `lib-src/lua/lauxlib.c` | Detailed markers around `luaI_openlib()`: library tables, stack operations, upvalues, closure creation, function registration, and before/after the `strchr()` call in `luaL_findtable()`. |
| `lib-src/lua/lgc.c` | Markers immediately before and after the two `strchr()` calls used to detect weak-key and weak-value modes in `traversetable()`. |
| `lib-src/lua/linit.c` | Output at the beginning and end of registration of each Lua standard library. |
| `lib-src/lua/loslib.c` | Markers before and after registration of the OS library in `luaopen_os()`. |
| `src/lua.cc` | Markers around `lua_open()`, `luaL_openlibs()`, `compat.lua`, `tolua_open()`, `tolua_enigma_open()`, and `RegisterFuncs()`. |

These markers are diagnostic code; they do not change the intended Lua API or game rules. The additional FORTIFY compiler option for the Lua target is separate and is listed in Section 3.1.

### 3.6 Android-specific startup, logs, and crash diagnostics

| File | Change compared with upstream |
|---|---|
| `src/main.cc` | Android-specific signal handler for `SIGABRT`, `SIGSEGV`, `SIGBUS`, `SIGILL`, and `SIGFPE`. It collects native program counters and writes them directly to stderr before the process terminates. Android stdout is redirected to `Output.log`, stderr to `Error.log`; buffering is configured accordingly. Additional `ENIGMA_DIAG` markers log Xerces, Lua, startup-script, options, user-path, and world initialization. Sound initialization is called with music disabled. CURL and ENet initialization/cleanup, as well as music initialization, were removed. |

These changes complement the Java-side setup of the app paths. The signal handler provides program counters, not an already symbolicated stack trace; full symbolication requires matching native symbols and a separate symbolication step.

---

## 4. Newly added Android files

The following **16 files** under `enigma/android/` do not exist in official upstream. They make up the Android app and its build configuration.

| File | Purpose |
|---|---|
| `enigma/android/app/build.gradle` | Android application module: namespace and application ID `org.enigma.android`, compile/target SDK 35, min SDK 28, NDK 29.0.14206865, version `1.3-android`, ABI filter limited to `arm64-v8a`, Java 17, and JNI/asset packaging. |
| `enigma/android/app/src/main/AndroidManifest.xml` | Launcher activity, landscape orientation, fullscreen theme, and handling of relevant configuration changes. No `INTERNET` permission is declared. |
| `enigma/android/app/src/main/java/org/enigma/android/EnigmaActivity.java` | SDL activity with Android-specific adaptations (details below). |
| `enigma/android/app/src/main/res/values/styles.xml` | Fullscreen theme without an ActionBar/title bar. |
| `enigma/android/build.gradle` | Pins the Android Gradle Plugin to 8.7.3. |
| `enigma/android/settings.gradle` | Gradle repositories and inclusion of the `:app` module. |
| `enigma/android/gradle.properties` | Gradle memory, parallelism, cache, and AndroidX settings. |
| `enigma/android/host-tools/CMakeLists.txt` | Builds `tolua` and the required Lua library natively on the Linux build host. |
| `enigma/android/triplets/arm64-android.cmake` | Defines the vcpkg triplet for Android ARM64, Android NDK toolchain, ARM64 architecture, and dynamic CRT/library linking. |
| `enigma/android/vcpkg.json` | Declares SDL2, SDL2_image with libjpeg-turbo, SDL2_mixer, SDL2_ttf, Xerces-C, and zlib. |
| `enigma/android/toolchain.lock` | Pins the vcpkg commit, SDL version, Android SDK/Build Tools, and NDK version. |
| `enigma/android/cmake/FindSDL2.cmake` | Adapts the vcpkg SDL2 config package to the older FindSDL2 interface expected by Enigma. |
| `enigma/android/cmake/FindSDL2_image.cmake` | Equivalent bridge for SDL2_image. |
| `enigma/android/cmake/FindSDL2_mixer.cmake` | Equivalent bridge for SDL2_mixer. |
| `enigma/android/cmake/FindSDL2_ttf.cmake` | Equivalent bridge for SDL2_ttf. |
| `enigma/android/.gitignore` | Excludes local Gradle output, vcpkg downloads/installations, generated data archives, and native build files. |

### 4.1 Behavior of `EnigmaActivity.java`

The new activity extends `org.libsdl.app.SDLActivity` and performs the following tasks:

- **Android Back key → Escape:** `dispatchKeyEvent()` translates DOWN/UP events for the Android Back key into SDL `KEYCODE_ESCAPE`. `onBackPressed()` serves as a fallback for Back gestures that bypass the normal dispatch path.
- **Landscape:** `setOrientationBis()` sets `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`. The manifest additionally sets `screenOrientation="landscape"`.
- **Offline data installation:** The `enigma-data.zip` archive generated during the build is extracted on first launch into the internal app directory `files/enigma-data`. A marker, `.install-complete`, prevents extraction on every subsequent launch.
- **Protection against ZIP path manipulation:** Before writing, the canonical output path is checked; entries outside the temporary target directory are rejected.
- **Temporary installation:** Data is first extracted to `enigma-data.tmp`. Only after successful extraction is the completion marker created and the directory moved to its final name.
- **Preferences path:** The preferences path is set up under the external app media directory `Android/media/.../Enigma`.
- **Startup arguments:** Enigma receives `--data`, `--pref`, `--robinson`, `--log`, and `--redirect`.

The SDL-specific Java classes under `org.libsdl.app` are obtained by the workflow from the pinned SDL version and copied into the app directory during the build. They are not checked into this repository as a separate SDL source tree.

---

## 5. New repository files outside the Android module

| File | Contents |
|---|---|
| `.github/workflows/build-enigma-android.yml` | GitHub Actions workflow for the complete ARM64 Android build, validation, APK artifact, and GitHub Release. |
| `README.md` | Short port description, offline/feature scope, known limitation with some tutorials, warning about deeper options menus, and tap controls. |
| `enigma/PORTING.md` | Technical porting notes, source policy, build structure, and validation information. |
| `enigma/source/SOURCE_REVISION` | Provenance record containing the exact upstream commit. |

The upstream source files are embedded under `enigma/source/`. The repository has no dependency on a private source repository and no longer needs a bootstrap token for the Enigma source code.

---

## 6. GitHub Actions build changes

The workflow `.github/workflows/build-enigma-android.yml` is entirely new compared with official upstream. It builds the checked-in source tree and does not import or synchronize it from another Enigma repository.

### 6.1 Triggers and permissions

- Manual start through `workflow_dispatch`.
- Automatic start when files under `enigma/**` or the workflow file change.
- GitHub Actions permission `contents: write`, allowing the build to publish the APK in GitHub Releases.
- Ubuntu 24.04 runner, maximum job time limit of 150 minutes.
- Java 17, Gradle 8.9, and Android Gradle Plugin 8.7.3.

### 6.2 Pinned toolchain

| Component | Pinned version |
|---|---|
| Android Compile SDK | 35 |
| Android Build Tools | 35.0.0 |
| Android NDK | 29.0.14206865 |
| Android minimum platform | API 28 |
| ABI | `arm64-v8a` |
| SDL | `release-2.32.10` |
| vcpkg | Commit `4df3365fcbb4795a44795883f8719bbb3b3bcd77` |
| Gradle | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| CMake generator | Ninja |
| Enigma configuration | `MinSizeRel` |

### 6.3 Build sequence

1. Check out the repository.
2. Set up Java, Android SDK, NDK, Build Tools, and Gradle.
3. Read the pinned toolchain and dependency versions.
4. Validate the embedded source tree: CMake files and `SOURCE_REVISION` must exist; the number of level files must be 1,718. Remaining music, CURL, and ENet build paths/symbols are also checked at the inspected locations.
5. Obtain SDL 2.32.10 and copy its Android Java shell into the app module.
6. Build `tolua` natively as a host program.
7. Set up vcpkg at the pinned commit and build the Android dependencies for `arm64-android`, or restore them from cache.
8. Cross-compile Enigma as an Android shared library. The target is `libmain.so`.
9. Create the `enigma-data.zip` data archive. The `music/` directory is explicitly excluded; the archive is then checked for exactly 1,718 level files.
10. Copy license, authorship, and acknowledgements files into the app assets.
11. Collect ARM64 dependencies, `libmain.so`, and `libc++_shared.so` under `jniLibs/arm64-v8a`.
12. Check every ELF `PT_LOAD` segment in every included native library. No segment may have an alignment smaller than `0x4000` (16 KB).
13. Build the APK with `gradle assembleDebug` and validate it using `zipalign -c -P 16 -v 4`.
14. Upload the APK as a GitHub Actions artifact.
15. Publish the APK in GitHub Releases under tag `android-v1`. If the release already exists, replace the asset using `--clobber`.

### 6.4 Caches

The workflow uses separate caches for:

- Android SDK, Build Tools, and NDK,
- vcpkg binary packages,
- vcpkg source downloads,
- installed Android dependencies,
- vcpkg checkout and bootstrap,
- SDL Android Java sources,
- host `tolua` build,
- native ARM64 CMake build,
- Gradle (through the Gradle setup action).

The keys for installed libraries and native build directories take relevant manifests, triplets, CMake files, source files, and workflow files into account, so stale results are not silently reused for an incompatible revision.

### 6.5 No bootstrap token required

An earlier version of the workflow contained a bootstrap step to retrieve private source-code contents. This step has been removed completely. The current workflow:

- reads Enigma code exclusively from `enigma/source/`,
- does not use `ENIGMA_SOURCE_TOKEN`,
- no longer accesses `zettelmuseum/test`,
- does not download Enigma source code through an API archive or Git,
- does not require a repository secret for the Enigma source code.

SDL and vcpkg continue to be obtained as external build dependencies from their pinned public sources.

**Minor naming discrepancy:** The workflow display name is still `Bootstrap and build Enigma Android ARM64`, even though the bootstrap step has been removed. This is only an outdated label; the actual job builds the vendored source.

---

## 7. Runtime scope: what remains and what is excluded

### Included or retained

- The Enigma game engine and unchanged upstream source files.
- The complete active level data set with 1,718 files.
- The regular level packs and their associated active game data.
- WAV-based sound effects and the sound libraries required for them.
- Relative mouse movement controls through SDL.
- Normal physical mouse-click handling.
- Save games/preferences through the configured preferences path.
- Upstream license, authorship, and acknowledgements files in the APK.

### Disabled or excluded in the Android port

- Music playback and music options.
- The music data directory in the generated APK data archive.
- CURL downloads and remotely loaded files.
- ENet multiplayer and its network transport.
- Network Game menus and actions.
- Homepage and documentation links from the relevant help menu.
- Level Pack Composer actions and their integration into the Android Enigma target.
- Developer/conversion utilities as Android build targets.
- The upstream `attic/` and `doc/` directories.

This does not mean that every related historical file has been deleted from Git history. In particular, the 16 music files remain in the vendored source tree; they are simply not included in the Android data archive.

---

## 8. Build and release validation

The current token-free build run completed successfully:

- **Workflow run:** [44105521656](https://github.com/zettelmuseum/Enigma/actions/runs/44105521656)
- **Build commit:** `62cfc799a303faaa98386b7ce2722cef5aedda82`
- **Overall result:** `success`
- **Successful main steps:** source-tree validation, SDL setup, host `tolua`, ARM64 dependencies, Enigma cross-compilation, offline data package, ELF 16 KB validation, APK `zipalign`, artifact upload, and release publication.

The release is available:

- **Tag:** `android-v1`
- **Release:** [Enigma Android ARM64 – first release](https://github.com/zettelmuseum/Enigma/releases/tag/android-v1)
- **Asset:** [Enigma-Android-arm64-v8a.apk](https://github.com/zettelmuseum/Enigma/releases/download/android-v1/Enigma-Android-arm64-v8a.apk)
- **File size at the verified release:** 44,734,562 bytes

The published APK is generated by the workflow using `assembleDebug`. It is therefore the debug variant, not a release variant specifically configured for the Play Store.

### What the successful workflow proves

The successful run confirms that the current checked-in source revision was cross-compiled for ARM64, the required libraries and offline data were included in the APK, all 1,718 level files were present in the data archive, the included ELF `PT_LOAD` segments meet the required 16 KB alignment, the APK passes the 16 KB `zipalign` check, and the APK was published in the release.

### What this does not automatically prove

The workflow is not an automated gameplay test on an Android device. In particular, it does not confirm that every individual level, every options menu, every touch sequence, and every background/resume cycle works flawlessly on all devices. The local README itself notes that some tutorials may be exceptions and that navigating deeper into the options may trigger crashes. The offline behavior of the remaining `Ratings update` menu item is also a separate test item.

---

## 9. Summary

The local repository version is not a complete redevelopment of Enigma. It is based on official upstream revision `6f0c51b26cc013cb6c935046d7d1e6a20bea4179`. Of the 5,420 source paths shared by both trees, 5,390 have the same Git object ID; the source-code changes are concentrated in 24 files.

The main differences are:

1. A new, pinned Android build using Gradle, Android NDK, vcpkg, SDL2, and a native ARM64 library.
2. An Android SDL activity with Back-key-to-Escape mapping, forced landscape orientation, offline data installation, and a separate preferences path.
3. An offline single-player configuration without CURL downloads or ENet multiplayer.
4. Disabled music while retaining sound effects.
5. Removed Composer and network menu paths.
6. Android-specific touch/multi-tap handling and window/app-resume redraw handling.
7. Lua, startup, and crash diagnostics for Android.
8. Build validation for 1,718 level files, 16 KB ELF alignment, and APK `zipalign`.
9. A token-free workflow that automatically publishes the APK in the `android-v1` release.

The current repository state is therefore documented as a standalone, reproducibly configured Android port based on the specified official Enigma revision.
