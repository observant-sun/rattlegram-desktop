package com.github.observant_sun.rattlegram.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.IOException;

public class AudioInputHandler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AudioInputHandler.class);

    private TargetDataLine line;
    private AudioInputStream audioInputStream;

    private final int sampleRate;
    private final int channelCount;

    public AudioInputHandler(int sampleRate, int channelCount) {
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
    }

    public void start() throws LineUnavailableException {
        log.debug("Starting audio input handler");
        AudioFormat format = getAudioFormat();
        log.debug("Format: {}", format);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        log.debug("TargetDataLine: {}", info);

        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("Input audio line not supported");
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        log.debug("Opened audio input line");
        line.start();
        log.debug("Audio input line started");

        audioInputStream = new AudioInputStream(line);
        log.debug("Audio input stream opened");
    }

    public void pause() {
        log.debug("Pausing audio input stream");
        line.stop();
    }

    public void resume() {
        log.debug("Resuming audio input stream");
        line.start();
    }

    public int read(byte[] buffer) throws IOException {
        return audioInputStream.read(buffer);
    }

    private AudioFormat getAudioFormat() {
        int sampleSizeInBits = 16; // 2 bytes
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channelCount, signed, bigEndian);
    }

    @Override
    public void close() {
        try {
            line.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.debug("Closing audio input handler");
        try {
            audioInputStream.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
