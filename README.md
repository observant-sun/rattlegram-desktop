# rattlegram-desktop

Based on https://github.com/aicodix/rattlegram.

Transceive UTF-8 text messages with up to 170 bytes over audio in about a second, on desktop, with a GUI.

Currently only works on Linux. Capable of sending and receiving, but settings are very limited.

## Building and running

### Building requirements
* JDK version >= 17
* GCC (tested with version 15.1.1)

### Building
```bash
./gradlew build
```

### Running
```bash
./gradlew run
```