# rattlegram-desktop

Desktop port of https://github.com/aicodix/rattlegram.

Transceive UTF-8 text messages with up to 170 bytes over audio in about a second, on desktop, with a GUI.

Tested on Linux, distributed for Linux and MacOS. 

If you encounter a problem, please open a new issue and describe it in detail.

## Running
Two versions of distributables are released: JARs (that require a JRE version 17 or higher to run) and JLink images (that don't need a JRE, but are bigger).

### Running a JLink image version
1. Download `rattlegram-desktop-{version}-{platform}-jlink-image.zip` from [latest release](https://github.com/observant-sun/rattlegram-desktop/releases/latest)
2. Unzip the archive
3. Execute `image/bin/rattlegram-desktop`

### Running JARs version
1. Ensure you have JRE version >= 17 installed
2. Download `rattlegram-desktop-{version}-{platform}-jlink-image.zip` from [latest release](https://github.com/observant-sun/rattlegram-desktop/releases/latest)
3. Unzip the archive
4. Execute `bin/rattlegram-desktop`

## Building

### Prerequisites
* JDK version >= 17
* GCC
  * On Windows, use mingw-w64 ([installation guide](https://code.visualstudio.com/docs/cpp/config-mingw))

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
