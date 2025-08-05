# rattlegram-desktop

Desktop port of https://github.com/aicodix/rattlegram.

Transceive UTF-8 text messages with up to 170 bytes over audio in about a second, on desktop, with a GUI.

Currently only works on Linux. Capable of sending and receiving.

## Running
1. Have Java Runtime Environment version 17 or higher installed
2. Download `rattlegram-desktop-{version}-{platform}.zip` from [latest release](https://github.com/observant-sun/rattlegram-desktop/releases/latest)
3. Unzip the archive
4. Execute `bin/rattlegram-desktop`

## Building

### Prerequisites
* JDK version >= 17
* GCC

[//]: # (  * On Windows, use mingw-w64 &#40;[installation guide]&#40;https://code.visualstudio.com/docs/cpp/config-mingw&#41;&#41; )

### Running from source code
```bash
./gradlew run
```

### Making a distributable
A file `build/distributions/rattlegram-desktop-{version}-{platform}.zip` should be created after you run the following command:
```bash
./gradlew build
```

## TODO
* Add some tests
* Windows and Mac support
* Add localizations