# Enigma Android-Port – vollständiger Änderungsbericht gegenüber Upstream

**Vergleichsdatum:** 17. November 2026  
**Lokales Repository:** [zettelmuseum/Enigma](https://github.com/zettelmuseum/Enigma), Branch `main`  
**Lokaler Stand vor Anlage dieses Dokuments:** `3e7d40220817a040550412988e8d1f52d2bc1c54`  
**Referenz:** [Enigma-Game/Enigma](https://github.com/Enigma-Game/Enigma), Branch `master`  
**Verglichener Upstream-Stand:** `6f0c51b26cc013cb6c935046d7d1e6a20bea4179`  
**Source-Provenienz:** Der Inhalt von `enigma/source/SOURCE_REVISION` lautet exakt `6f0c51b26cc013cb6c935046d7d1e6a20bea4179`.

Dieses Dokument beschreibt den tatsächlich im Repository vorhandenen Stand. Es unterscheidet zwischen Änderungen am Enigma-C++-/Lua-Quellcode, zusätzlichen Android-Dateien, Änderungen am Build und bewusst nicht übernommenen Upstream-Bestandteilen. Die Dateivergleiche wurden anhand der rekursiven Git-Bäume und der Git-Objekt-IDs durchgeführt. Dabei wurde `enigma/source/<Upstream-Pfad>` jeweils mit `<Upstream-Pfad>` verglichen.

---

## 1. Ergebnis des vollständigen Dateivergleichs

| Vergleich | Ergebnis |
|---|---:|
| Einträge im eingebetteten lokalen Quellbaum `enigma/source/` | 5.421 |
| Einträge im offiziellen Upstream-Baum | 6.585 |
| Pfade, die in beiden Bäumen vorhanden sind | 5.420 |
| Gemeinsame Einträge mit identischer Git-Objekt-ID | 5.390 |
| Einträge mit geänderter Git-Objekt-ID | 30 |
| Davon tatsächlich geänderte Quelldateien | **24** |
| Davon geänderte Verzeichnisbäume als Folge der Dateiänderungen | 6 |
| Zusätzliche lokale Datei | `SOURCE_REVISION` |
| Nur im Upstream vorhandene Einträge | 1.165 |

Die 1.165 ausschließlich im Upstream vorhandenen Einträge bestehen aus **1.146 Dateiobjekten und 19 Verzeichniseinträgen**. Sie liegen vollständig in den beiden Verzeichnissen:

- `attic/`: archivierte bzw. nicht zum normalen Laufzeitbestand gehörende Materialien, darunter historische Grafiken, XCF-/SVG-Quelldateien, ältere Assets, Flaggen und Vorlagen.
- `doc/`: die Upstream-Dokumentation.

Es wurden beim Vergleich keine weiteren fehlenden aktiven Quellpfade außerhalb dieser beiden Verzeichnisse festgestellt. Die eigentliche Enigma-Quellbasis ist also weitgehend vollständig eingebettet; sie liegt lediglich unter `enigma/source/` statt direkt im Repository-Wurzelverzeichnis.

### Level- und Musikdaten

- Der Verzeichnisbaum `data/levels/` hat auf beiden Seiten dieselbe Git-Objekt-ID.
- Beide Seiten enthalten genau **1.718 Leveldateien**.
- Die 16 Dateien unter `data/music/` sind im lokalen Quellbaum weiterhin vorhanden und inhaltlich identisch zum Upstream. Sie werden jedoch weder über die angepasste Daten-CMake-Datei installiert noch in das Android-Datenarchiv aufgenommen.
- Die übrigen aktiven Datenbestände wurden nicht als geänderte Dateien festgestellt. Die Änderung am Datenverzeichnis betrifft insbesondere dessen CMake-Einbindung, nicht die Leveldateien.

---

## 2. Grundkonzept des Ports

Der Upstream ist ein allgemeines Enigma-Projekt. Das lokale Repository ergänzt es um einen eigenständigen Android-Build für **ARM64 / `arm64-v8a`** und reduziert die Anwendung auf einen Offline-Einzelspieler-Port.

Ziele und Konsequenzen:

- Spiel und Leveldaten laufen lokal auf dem Gerät.
- Keine Android-Internetberechtigung.
- Kein CURL-basierter Download von Dateien.
- Kein ENet-Multiplayer und keine Netzwerk-Menüs.
- Kein Level-Pack-Composer in der Android-Anwendung.
- Keine Musikwiedergabe und keine Musikdateien im APK-Datenarchiv.
- WAV-Soundeffekte bleiben erhalten.
- Android verwendet SDL2 und lädt die native Enigma-Bibliothek `libmain.so`.
- Die nativen Bibliotheken werden mit mindestens 16-KB-`PT_LOAD`-Alignment gebaut und geprüft.
- Die Leveldaten werden in das App-interne Datenverzeichnis entpackt; der Preferences-Pfad liegt unter `Android/media/Enigma`.

Die Anpassungen wurden direkt in `enigma/source/` vorgenommen. Es gibt keinen zur Laufzeit oder im Workflow angewendeten Patch-Satz und keinen automatischen Abgleich mit einem externen Enigma-Quellverzeichnis.

---

## 3. Alle 24 geänderten Quelldateien

### 3.1 CMake und native Buildstruktur

| Datei | Änderung gegenüber Upstream |
|---|---|
| `CMakeLists.txt` | Die verpflichtende CURL-Suche (`find_package(CURL REQUIRED)`) wurde entfernt. Das Unterverzeichnis `tools/` wird nur noch außerhalb von Android eingebunden. |
| `data/CMakeLists.txt` | `add_subdirectory(music)` wurde entfernt. Die Musikdaten werden dadurch nicht mehr als installierter Laufzeitbestand behandelt. |
| `lib-src/CMakeLists.txt` | `add_subdirectory(enet)` wurde entfernt. ENet wird für den Port nicht mehr gebaut. |
| `lib-src/lua/CMakeLists.txt` | Für das Lua-Ziel wurde `-U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=0` ergänzt. Das ist die im Android-Debugging verwendete FORTIFY-Umgehung für die Lua-Bibliothek. **Wichtig:** Die Option steht derzeit nicht in einem `if(ANDROID)`-Block und gilt deshalb für dieses Lua-Ziel grundsätzlich auch bei Nicht-Android-Builds, die diese CMake-Datei verwenden. |
| `lib-src/oxydlib/CMakeLists.txt` | Die Hilfsprogramme `extractbitmaps` und `printstats` werden bei Android-Builds nicht als ausführbare Host-Programme angelegt. Die Bibliothek selbst bleibt Teil des Builds. |
| `src/CMakeLists.txt` | Das Ziel wird auf Android als Shared Library statt als normales ausführbares Programm gebaut. Der Ausgabename wird auf `main` gesetzt, sodass `libmain.so` entsteht. Die Android-Linkeroptionen setzen `-Wl,-z,max-page-size=16384` und `-Wl,-z,common-page-size=16384`. Die Level-Pack-Composer-Dateien und die Multiplayer-Quellmodule werden aus der Zielliste entfernt. CURL- und ENet-Linkabhängigkeiten entfallen. Für die Lua-Bindings wird beim Cross-Compile ein explizites Host-Programm über `TOLUA_EXECUTABLE` verwendet. |
 
Die Umstellung auf ein Host-`tolua` ist für Cross-Compilation wichtig: Der Generator muss auf dem Linux-Buildrechner ausführbar sein und darf nicht als ARM64-Android-Programm ausgeführt werden. Die neue CMake-Logik verlangt deshalb für Android einen gesetzten Host-Pfad und verwendet diesen sowohl im `COMMAND` als auch in `DEPENDS`.

### 3.2 Musik, Netzwerk und Dateizugriff

| Datei | Änderung gegenüber Upstream |
|---|---|
| `src/MusicManager.cc` | Die zentralen Steuerfunktionen für Menü-, Lade- und Spielmusik sowie Musik-Tick und Initialisierung wurden zu No-op-Funktionen. Die Schnittstellen bleiben vorhanden, die Musikwiedergabe wird aber nicht mehr gestartet. |
| `src/client.cc` | Der ENet-Clientcode wurde entfernt. Der Netzwerkstart meldet für den Offline-Port Erfolg, startet aber keinen Transport. Außerdem enthält die Datei die Android-Touch- und Fensterereignisbehandlung (siehe Abschnitt 4). |
| `src/client_internal.hh` | ENet-Header, Netzwerkstart/-stopp-Methoden und die Felder für ENet-Host und Server-Peer wurden entfernt. |
| `src/file.cc` | CURL-Includes, globale CURL-Initialisierung, Download-Callback und CURL-Aufräumcode wurden entfernt. `Downloadfile()` lädt nichts mehr aus dem Internet, sondern wirft ausdrücklich `XLevelLoading` mit dem Hinweis, dass Internetzugriff deaktiviert ist. |
| `src/file.hh` | Die Deklarationen `InitCurl()` und `ShutdownCurl()` wurden entfernt. |
| `src/server.cc` | ENet-Include, globale Netzwerkadresse und Host-Variable sowie das Zerstören des ENet-Hosts beim Shutdown wurden entfernt. |

Damit sind sowohl der bisherige CURL-Dateidownload als auch der ENet-Netzwerktransport aus dem Android-Build entfernt. Das ist mehr als das Ausblenden von Menüeinträgen: Die zugehörigen Initialisierungs-, Shutdown- und Linkpfade entfallen ebenfalls.

**Einschränkung:** Der Menüpunkt `Ratings update` ist in `src/gui/OptionsMenu.cc` weiterhin vorhanden. Seine Reaktion auf einen Offline-Aufruf ist nicht durch den Buildtest geprüft. Da `Downloadfile()` jetzt eine Ausnahme wirft, sollte dieser Menüpunkt als offener Laufzeittest betrachtet werden und nicht als funktionierende Online-Funktion.

### 3.3 Menüoberfläche und Composer

| Datei | Änderung gegenüber Upstream |
|---|---|
| `src/gui/LevelMenu.cc` | Die Composer-Einbindung und die F6-Funktion zum Übernehmen aller Level aus dem Index in den Composer wurden entfernt. |
| `src/gui/LevelPackConfig.cc` | Der Button `Compose Pack` und sein Aktionspfad wurden entfernt. Die bisherige Button-Variable wird als leeres `Label` geführt; ein Composer wird von diesem Bildschirm aus nicht mehr geöffnet. |
| `src/gui/MainMenu.cc` | Das vollständige Network-Menü samt Start-/Join-Aktionen wurde entfernt. Die Network-Game-Schaltfläche ist nicht mehr vorhanden. Die Homepage- und Documentation-Schaltflächen des Hilfebereichs wurden entfernt. Die Menüanordnung wurde an die kleinere Zahl von Einträgen angepasst. |
| `src/gui/MainMenu.hh` | Die Variablen für Homepage, Documentation und Network Game sowie die Klasse `NetworkMenu` und `ShowNetworkMenu()` wurden entfernt. |
| `src/gui/OptionsMenu.cc` | Die Musiklautstärke, Menü-Musik und Ingame-Musik wurden aus den Optionen entfernt. Soundset, Soundlautstärke und weitere Soundoptionen bleiben vorhanden. |

Die Composer-Implementierungsdateien können weiterhin im Quellbaum liegen; sie werden jedoch nicht mehr in das Android-Enigma-Ziel eingebunden und die entsprechenden Menüpunkte/Aktionen sind entfernt. Das ist eine Build-/UI-Deaktivierung und keine pauschale Löschung sämtlicher Composer-Quelldateien aus dem Repository.

### 3.4 Android-Eingabe und Wiederaufnahme

Die wesentlichen Änderungen liegen in `src/client.cc`:

1. **Touch-generierte Mausklicks werden herausgefiltert.** `SDL_MOUSEBUTTONDOWN` und `SDL_MOUSEBUTTONUP` mit `SDL_TOUCH_MOUSEID` werden nicht an die normale Mausklickbehandlung weitergegeben. Umgekehrt werden synthetische Fingerereignisse mit `SDL_MOUSE_TOUCHID` ignoriert.
2. **Native SDL-Fingerereignisse werden ausgewertet.** Es wird jeweils ein aktiver Finger mit Touch- und Finger-ID verfolgt. Während eines aktiven Touchvorgangs wird die automatische Inventaraufnahme über `player::InhibitPickup(true)` unterbunden und beim Loslassen wieder freigegeben.
3. **Mehrfach-Taps werden zeitlich unterschieden.** Die Sequenz verwendet ein Zeitfenster von 300 ms. Ein Dreifach-Tap löst nach Ablauf des Fensters die Inventar-Aktion `server::Msg_ActivateItem()` aus. Beginnt innerhalb des Fensters ein vierter Tap, wird die Dreifach-Tap-Aktion noch nicht ausgelöst. Beim Loslassen des vierten Taps wird stattdessen `rotate_inventory(+1)` ausgeführt.
4. **Fensterfokusverlust setzt Touchzustände zurück.** Ein unvollständiger Tap wird verworfen und eine eventuell aktive Pickup-Sperre aufgehoben.
5. **Der Bildschirminhalt wird bei Wiederaufnahme neu gezeichnet.** Auf `FOCUS_GAINED`, `EXPOSED`, `RESTORED`, `SIZE_CHANGED` und `RESIZED` wird `display::RedrawAll()` aufgerufen. Das adressiert schwarze bzw. nicht neu gezeichnete Bildschirme nach dem Zurückwechseln zur App und nach Änderungen der SDL-Fensterfläche.
6. **Die relative Mausbewegung bleibt erhalten.** `SDL_MOUSEMOTION` wird weiterhin über `xrel`/`yrel` in `server::Msg_MouseForce()` übersetzt. Die normale physische Mausbedienung über `on_mousebutton()` bleibt ebenfalls erhalten.

Die derzeitige Zuordnung ist auch in der lokalen README festgehalten: **Dreifach-Tap = Inventarobjekt benutzen; Vierfach-Tap = Inventar drehen.** Es handelt sich im vorliegenden Code also ausdrücklich nicht um eine Zwei-Tap-Zuordnung.

### 3.5 Lua-Diagnoseinstrumentierung

Folgende Lua-Dateien wurden ausschließlich um Android-Diagnoseausgaben erweitert. Die Ausgaben sind mit `#ifdef __ANDROID__` umschlossen:

| Datei | Diagnosepunkte |
|---|---|
| `lib-src/lua/lapi.c` | Ausgabe vor und nach dem Garbage-Collection-Schritt und nach der Closure-Allokierung in `lua_pushcclosure()`. |
| `lib-src/lua/lauxlib.c` | Detaillierte Marker rund um `luaI_openlib()`: Bibliothekstabellen, Stackoperationen, Upvalues, Closure-Erzeugung, Funktionsregistrierung sowie vor/nach dem `strchr()`-Aufruf in `luaL_findtable()`. |
| `lib-src/lua/lgc.c` | Marker unmittelbar vor und nach den beiden `strchr()`-Aufrufen zur Erkennung von Weak-Key- und Weak-Value-Modi in `traversetable()`. |
| `lib-src/lua/linit.c` | Ausgabe beim Beginn und Ende der Registrierung jeder Lua-Standardbibliothek. |
| `lib-src/lua/loslib.c` | Marker vor und nach der Registrierung der OS-Bibliothek in `luaopen_os()`. |
| `src/lua.cc` | Marker rund um `lua_open()`, `luaL_openlibs()`, `compat.lua`, `tolua_open()`, `tolua_enigma_open()` und `RegisterFuncs()`. |

Die Markierungen sind Diagnosecode; sie ändern nicht die beabsichtigte Lua-API oder die Spielregeln. Die zusätzliche FORTIFY-Compileroption für das Lua-Ziel ist davon getrennt und ist in Abschnitt 3.1 aufgeführt.

### 3.6 Android-spezifischer Start, Logs und Absturzdiagnose

| Datei | Änderung gegenüber Upstream |
|---|---|
| `src/main.cc` | Android-spezifischer Signalhandler für `SIGABRT`, `SIGSEGV`, `SIGBUS`, `SIGILL` und `SIGFPE`. Er sammelt native Programmzähler und schreibt sie direkt nach stderr, bevor der Prozess beendet wird. Android-stdout wird nach `Output.log`, stderr nach `Error.log` umgeleitet; die Pufferung wird passend gesetzt. Zusätzliche `ENIGMA_DIAG`-Marker protokollieren Xerces-, Lua-, Startup-Script-, Options-, User-Path- und World-Initialisierung. Die Soundinitialisierung wird mit deaktivierter Musik aufgerufen. CURL- und ENet-Initialisierung/-Aufräumarbeiten sowie die Musikinitialisierung wurden entfernt. |

Diese Änderungen ergänzen die Java-seitige Einrichtung der App-Pfade. Der Signalhandler liefert Programmzähler, keine bereits symbolisierte Stacktrace-Ausgabe; zur vollständigen Symbolisierung wären passende native Symbole und ein separater Symbolisierungsschritt erforderlich.

---

## 4. Neu hinzugefügte Android-Dateien

Die folgenden **16 Dateien** unter `enigma/android/` gibt es im offiziellen Upstream nicht. Sie bilden die Android-App und deren Buildkonfiguration.

| Datei | Zweck |
|---|---|
| `enigma/android/app/build.gradle` | Android-Anwendungsmodul: Namespace und Application-ID `org.enigma.android`, Compile-/Target-SDK 35, Min-SDK 28, NDK 29.0.14206865, Version `1.3-android`, ABI-Filter ausschließlich `arm64-v8a`, Java 17 und JNI-/Asset-Packaging. |
| `enigma/android/app/src/main/AndroidManifest.xml` | Launcher-Aktivität, Landscape-Ausrichtung, Fullscreen-Theme und Behandlung relevanter Konfigurationsänderungen. Es ist keine `INTERNET`-Berechtigung eingetragen. |
| `enigma/android/app/src/main/java/org/enigma/android/EnigmaActivity.java` | SDL-Aktivität mit Android-spezifischen Anpassungen (Details unten). |
| `enigma/android/app/src/main/res/values/styles.xml` | Fullscreen-Theme ohne ActionBar/Titelzeile. |
| `enigma/android/build.gradle` | Pinnt das Android-Gradle-Plugin auf 8.7.3. |
| `enigma/android/settings.gradle` | Gradle-Repositories und Einbindung des Moduls `:app`. |
| `enigma/android/gradle.properties` | Gradle-Speicher-, Parallelisierungs-, Cache- und AndroidX-Einstellungen. |
| `enigma/android/host-tools/CMakeLists.txt` | Baut `tolua` und die benötigte Lua-Bibliothek nativ auf dem Linux-Buildhost. |
| `enigma/android/triplets/arm64-android.cmake` | Definiert das vcpkg-Triplet für Android ARM64, Android-NDK-Toolchain, ARM64-Architektur und dynamisches CRT-/Library-Linking. |
| `enigma/android/vcpkg.json` | Deklariert SDL2, SDL2_image mit libjpeg-turbo, SDL2_mixer, SDL2_ttf, Xerces-C und zlib. |
| `enigma/android/toolchain.lock` | Fixiert vcpkg-Commit, SDL-Version, Android-SDK-/Build-Tools- und NDK-Version. |
| `enigma/android/cmake/FindSDL2.cmake` | Übersetzt das vcpkg-SDL2-Config-Paket in die ältere von Enigma erwartete FindSDL2-Schnittstelle. |
| `enigma/android/cmake/FindSDL2_image.cmake` | Entsprechende Brücke für SDL2_image. |
| `enigma/android/cmake/FindSDL2_mixer.cmake` | Entsprechende Brücke für SDL2_mixer. |
| `enigma/android/cmake/FindSDL2_ttf.cmake` | Entsprechende Brücke für SDL2_ttf. |
| `enigma/android/.gitignore` | Schließt lokale Gradle-Ausgaben, vcpkg-Downloads/-Installationen, generierte Datenarchive und native Builddateien aus. |

### 4.1 Verhalten von `EnigmaActivity.java`

Die neue Activity erweitert `org.libsdl.app.SDLActivity` und übernimmt folgende Aufgaben:

- **Android-Zurück-Taste → Escape:** `dispatchKeyEvent()` übersetzt DOWN/UP der Android-Back-Taste in SDL-`KEYCODE_ESCAPE`. `onBackPressed()` dient als Fallback für Back-Gesten, die den normalen Dispatchweg umgehen.
- **Landscape:** `setOrientationBis()` setzt `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`. Das Manifest setzt zusätzlich `screenOrientation="landscape"`.
- **Offline-Dateninstallation:** Das beim Build erzeugte `enigma-data.zip` wird beim ersten Start in das interne App-Verzeichnis `files/enigma-data` entpackt. Ein Marker `.install-complete` verhindert die erneute Extraktion bei jedem Start.
- **Schutz gegen Zip-Pfadmanipulation:** Vor dem Schreiben wird der kanonische Ausgabepfad geprüft; Einträge außerhalb des temporären Zielverzeichnisses werden abgelehnt.
- **Temporäre Installation:** Die Daten werden zunächst nach `enigma-data.tmp` entpackt. Erst nach erfolgreichem Entpacken wird der Abschlussmarker angelegt und das Verzeichnis auf den endgültigen Namen verschoben.
- **Preference-Pfad:** Der Preferences-Pfad wird unter dem externen App-Medienverzeichnis `Android/media/.../Enigma` eingerichtet.
- **Startparameter:** Enigma erhält `--data`, `--pref`, `--robinson`, `--log` und `--redirect`.

Die SDL-eigenen Java-Klassen unter `org.libsdl.app` werden vom Workflow aus der gepinnten SDL-Version bezogen und beim Build in das App-Verzeichnis kopiert. Sie sind nicht als eigener SDL-Quellbaum in diesem Repository eingecheckt.

---

## 5. Neue Repository-Dateien außerhalb des Android-Moduls

| Datei | Inhalt |
|---|---|
| `.github/workflows/build-enigma-android.yml` | GitHub-Actions-Workflow für den vollständigen ARM64-Android-Build, Prüfungen, APK-Artefakt und GitHub-Release. |
| `README.md` | Kurze Portbeschreibung, Offline-/Featureumfang, bekannte Einschränkung bei einigen Tutorials, Warnung vor tieferen Optionsmenüs und Tap-Bedienung. |
| `enigma/PORTING.md` | Technische Portierungsnotizen, Source-Policy, Buildaufbau und Validierungsinformationen. |
| `enigma/source/SOURCE_REVISION` | Herkunftsnachweis mit dem exakten Upstream-Commit. |

Die Upstream-Quelldateien wurden in `enigma/source/` eingebettet. Das Repository enthält keine Abhängigkeit von einem privaten Quellrepository und benötigt für den Enigma-Quellcode keinen Bootstrap-Token mehr.

---

## 6. Änderungen am GitHub-Actions-Build

Der Workflow `.github/workflows/build-enigma-android.yml` ist vollständig neu gegenüber dem offiziellen Upstream. Er baut die eingecheckte Quellbasis und importiert oder synchronisiert sie nicht aus einem anderen Enigma-Repository.

### 6.1 Auslöser und Berechtigungen

- Start per `workflow_dispatch`.
- Automatischer Start bei Änderungen unter `enigma/**` oder an der Workflow-Datei.
- GitHub-Actions-Berechtigung `contents: write`, damit der Build das APK in GitHub Releases veröffentlichen kann.
- Ubuntu-24.04-Runner, maximales Job-Zeitlimit 150 Minuten.
- Java 17, Gradle 8.9 und Android-Gradle-Plugin 8.7.3.

### 6.2 Gepinnte Toolchain

| Komponente | Festgelegter Stand |
|---|---|
| Android Compile SDK | 35 |
| Android Build Tools | 35.0.0 |
| Android NDK | 29.0.14206865 |
| Android Mindestplattform | API 28 |
| ABI | `arm64-v8a` |
| SDL | `release-2.32.10` |
| vcpkg | Commit `4df3365fcbb4795a44795883f8719bbb3b3bcd77` |
| Gradle | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| CMake-Generator | Ninja |
| Enigma-Konfiguration | `MinSizeRel` |

### 6.3 Buildablauf

1. Repository auschecken.
2. Java, Android SDK, NDK, Build Tools und Gradle bereitstellen.
3. Gepinnte Toolchain- und Dependency-Stände einlesen.
4. Den eingebetteten Quellbaum prüfen: CMake-Dateien und `SOURCE_REVISION` müssen vorhanden sein; die Anzahl der Leveldateien muss 1.718 betragen. Außerdem werden verbliebene Musik-, CURL- und ENet-Buildpfade bzw. Symbole an den geprüften Stellen ausgeschlossen.
5. SDL 2.32.10 beziehen und dessen Android-Java-Shell in das App-Modul kopieren.
6. `tolua` nativ als Host-Programm bauen.
7. vcpkg auf dem festgelegten Commit bereitstellen und die Android-Abhängigkeiten für `arm64-android` bauen bzw. aus dem Cache wiederherstellen.
8. Enigma als Android-Shared-Library cross-kompilieren. Ziel ist `libmain.so`.
9. Das Datenarchiv `enigma-data.zip` erzeugen. Dabei wird `music/` ausdrücklich ausgeschlossen; anschließend wird geprüft, dass genau 1.718 Leveldateien im Archiv sind.
10. Lizenz-, Autoren- und Danksagungsdateien in die App-Assets kopieren.
11. Die ARM64-Abhängigkeiten, `libmain.so` und `libc++_shared.so` unter `jniLibs/arm64-v8a` sammeln.
12. Für jede eingebundene native Bibliothek alle ELF-`PT_LOAD`-Segmente prüfen. Kein Segment darf ein Alignment kleiner als `0x4000` (16 KB) haben.
13. Das APK mit `gradle assembleDebug` bauen und mit `zipalign -c -P 16 -v 4` prüfen.
14. Das APK als Actions-Artefakt hochladen.
15. Das APK in GitHub Releases unter dem Tag `android-v1` veröffentlichen. Existiert das Release bereits, wird das Asset mit `--clobber` ersetzt.

### 6.4 Caches

Der Workflow verwendet getrennte Caches für:

- Android SDK, Build Tools und NDK,
- vcpkg-Binärpakete,
- vcpkg-Quelldownloads,
- installierte Android-Abhängigkeiten,
- vcpkg-Checkout und Bootstrap,
- SDL-Android-Java-Quellen,
- Host-`tolua`-Build,
- nativen ARM64-CMake-Build,
- Gradle (über die Gradle-Setup-Action).

Die Schlüssel für installierte Bibliotheken und native Buildverzeichnisse berücksichtigen die relevanten Manifest-, Triplet-, CMake-, Quell- und Workflow-Dateien, damit veraltete Ergebnisse nicht unbemerkt für einen inkompatiblen Stand wiederverwendet werden.

### 6.5 Kein Bootstrap-Token mehr

Die frühere Workflow-Version hatte einen Bootstrap-Schritt zum Abruf privater Quellcodeinhalte. Dieser Schritt wurde vollständig entfernt. Der aktuelle Workflow:

- liest den Enigma-Code ausschließlich aus `enigma/source/`,
- verwendet kein `ENIGMA_SOURCE_TOKEN`,
- greift nicht mehr auf `zettelmuseum/test` zu,
- lädt den Enigma-Quellcode nicht per API-Archiv oder Git nach,
- benötigt für den Enigma-Quellcode kein Repository-Secret.

SDL und vcpkg werden weiterhin als externe Buildabhängigkeiten von ihren festgelegten öffentlichen Quellen bezogen.

**Kleine Namensabweichung:** Der Workflow-Anzeigename lautet noch `Bootstrap and build Enigma Android ARM64`, obwohl der Bootstrap-Schritt entfernt wurde. Das ist nur eine veraltete Bezeichnung; der tatsächliche Job baut die vendorte Quelle.

---

## 7. Laufzeitumfang: Was bleibt und was entfällt

### Enthalten bzw. beibehalten

- Der Enigma-Spielkern und die unveränderten Upstream-Quelldateien.
- Der vollständige aktive Leveldatenbestand mit 1.718 Dateien.
- Die regulären Levelpacks und die zugehörigen aktiven Spieldaten.
- WAV-basierte Soundeffekte und die dafür benötigten Soundbibliotheken.
- Relative Mausbewegungssteuerung über SDL.
- Die normale physische Mausklickbehandlung.
- Spielstände/Preferences über den konfigurierten Preference-Pfad.
- Die Upstream-Lizenz-, Autoren- und Danksagungsdateien im APK.

### Im Android-Port deaktiviert oder nicht enthalten

- Musikwiedergabe und Musikoptionen.
- Der Musikdatenordner im erzeugten APK-Datenarchiv.
- CURL-Downloads und remote geladene Dateien.
- ENet-Multiplayer und dessen Netzwerktransport.
- Network-Game-Menüs und -Aktionen.
- Homepage- und Dokumentationslinks aus dem betreffenden Hilfemenü.
- Level-Pack-Composer-Aktionen und deren Einbindung in das Android-Enigma-Ziel.
- Entwickler-/Konvertierungsprogramme als Android-Buildziele.
- Upstream-Verzeichnisse `attic/` und `doc/`.

Das bedeutet nicht, dass sämtliche dazugehörigen historischen Dateien aus dem Git-Verlauf gelöscht wurden. Insbesondere bleiben die 16 Musikdateien im vendorten Quellbaum erhalten; sie werden lediglich nicht in das Android-Datenarchiv aufgenommen.

---

## 8. Build- und Releaseprüfung

Die aktuelle tokenfreie Buildausführung wurde erfolgreich abgeschlossen:

- **Workflow-Run:** [44105521656](https://github.com/zettelmuseum/Enigma/actions/runs/44105521656)
- **Build-Commit:** `62cfc799a303faaa98386b7ce2722cef5aedda82`
- **Gesamtergebnis:** `success`
- **Erfolgreiche Hauptschritte:** Quellbaumprüfung, SDL-Bereitstellung, Host-`tolua`, ARM64-Abhängigkeiten, Enigma-Cross-Compile, Offline-Datenpaket, ELF-16-KB-Prüfung, APK-`zipalign`, Artefakt-Upload und Release-Veröffentlichung.

Das Release ist vorhanden:

- **Tag:** `android-v1`
- **Release:** [Enigma Android ARM64 – first release](https://github.com/zettelmuseum/Enigma/releases/tag/android-v1)
- **Asset:** [Enigma-Android-arm64-v8a.apk](https://github.com/zettelmuseum/Enigma/releases/download/android-v1/Enigma-Android-arm64-v8a.apk)
- **Dateigröße beim geprüften Release:** 44.734.562 Byte

Das veröffentlichte APK wird vom Workflow mit `assembleDebug` erzeugt. Es handelt sich daher um die Debug-Variante und nicht um eine eigens für den Play Store konfigurierte Release-Variante.

### Was der erfolgreiche Workflow belegt

Der erfolgreiche Lauf belegt, dass der aktuelle eingecheckte Quellstand für ARM64 cross-kompiliert wurde, die benötigten Bibliotheken und Offline-Daten in das APK gelangt sind, die 1.718 Leveldateien im Datenarchiv vorhanden sind, die eingebundenen ELF-`PT_LOAD`-Segmente das geforderte 16-KB-Alignment einhalten, das APK die `zipalign`-Prüfung mit 16 KB besteht und das APK im Release veröffentlicht wurde.

### Was damit nicht automatisch belegt ist

Der Workflow ist kein automatisierter Spieltest auf einem Android-Gerät. Er bestätigt insbesondere nicht, dass jeder einzelne Level, jedes Optionsmenü, jede Touch-Sequenz und jede Wiederaufnahme aus dem Hintergrund auf allen Geräten fehlerfrei funktioniert. Die lokale README weist selbst darauf hin, dass einige Tutorials Ausnahmen darstellen können und dass ein tieferes Navigieren in den Optionen Abstürze auslösen kann. Die Offline-Reaktion des verbliebenen Menüpunktes `Ratings update` ist ebenfalls ein gesonderter Testpunkt.

---

## 9. Zusammenfassung

Die lokale Repository-Version ist keine vollständige Neuentwicklung von Enigma. Sie basiert auf dem offiziellen Upstream-Stand `6f0c51b26cc013cb6c935046d7d1e6a20bea4179`. Von den 5.420 gemeinsam vorhandenen Quellpfaden haben 5.390 dieselbe Git-Objekt-ID; die inhaltlichen Quelländerungen konzentrieren sich auf 24 Dateien.

Die wesentlichen Abweichungen sind:

1. Ein neuer, gepinnter Android-Build mit Gradle, Android NDK, vcpkg, SDL2 und einer nativen ARM64-Bibliothek.
2. Eine Android-SDL-Activity mit Back-Taste-zu-Escape, Landscape-Vorgabe, Offline-Dateninstallation und getrenntem Preference-Pfad.
3. Ein Offline-Einzelspieler-Zuschnitt ohne CURL-Downloads und ENet-Multiplayer.
4. Deaktivierte Musik bei beibehaltenen Soundeffekten.
5. Entfernte Composer- und Netzwerk-Menüpfade.
6. Android-spezifische Touch-/Mehrfach-Tap- und Fenster-Wiederaufnahmebehandlung.
7. Lua-, Start- und Crashdiagnose für Android.
8. Eine Buildprüfung für 1.718 Leveldateien, 16-KB-ELF-Alignment und APK-`zipalign`.
9. Ein tokenfreier Workflow, der das APK automatisch im Release `android-v1` veröffentlicht.

Damit ist der aktuelle Repository-Stand als eigenständiger, reproduzierbar konfigurierter Android-Port auf Basis der angegebenen offiziellen Enigma-Revision dokumentiert.
