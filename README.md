# rattlegram-desktop

Based on https://github.com/aicodix/rattlegram.

Transceive UTF-8 text messages with up to 170 bytes over audio in about a second, on desktop, with a GUI.

Currently only works on Linux. Capable of sending and receiving.

## Running
1. Download `rattlegram-desktop-{version}-{platform}.zip` from [latest release](https://github.com/observant-sun/rattlegram-desktop/releases/latest)
2. Unzip the archive
3. Execute `bin/rattlegram-desktop`

## Building

### Prerequisites
* JDK version >= 17
* GCC (tested with version 15.1.1)

### Running from source code
```bash
./gradlew run
```

### Making a distributable
A file `build/distributions/rattlegram-desktop-{version}-{platform}.zip` should be created after you run the following command:
```bash
./gradlew clean jlinkZip
```

## TODO
* Add some tests
* Windows and Mac support
* Repeater mode
* Add localizations