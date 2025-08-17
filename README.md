# rattlegram-desktop

Desktop port of https://github.com/aicodix/rattlegram. 

Transceive UTF-8 text messages with up to 170 bytes over audio in about a second, on desktop, with a GUI. 
The app has all the functionality of the original Android app.
Tested on Linux and Windows, distributed for Linux, Windows and macOS.

## Running
Two versions of distributables are released: JARs (require a JRE version 17 or higher to run) and JLink images (don't need a JRE, but are larger in size).

### Running a JLink image version (recommended for inexperienced users and Windows users)
1. Download `rattlegram-desktop-{version}-{platform}-jlink-image.zip` from [latest release](https://github.com/observant-sun/rattlegram-desktop/releases/latest)
2. Unzip the archive
3. Execute `image/bin/rattlegram-desktop`

### Running JARs version
1. Ensure you have JRE version >= 17 installed
2. Download `rattlegram-desktop-{version}-{platform}.zip` from [latest release](https://github.com/observant-sun/rattlegram-desktop/releases/latest)
3. Unzip the archive
4. Execute `bin/rattlegram-desktop`

## Building

### Prerequisites
* JDK version >= 17
* GCC or Clang
  * On Windows, use GCC with mingw-w64 ([installation guide](https://code.visualstudio.com/docs/cpp/config-mingw))

### Running from source code
```bash
./gradlew run
```

### Making a distributable
A file `gui/build/distributions/rattlegram-desktop-{version}-{platform}.zip` should be created after you run the following command:
```bash
./gradlew build
```
