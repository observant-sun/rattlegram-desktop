package com.github.observant_sun.rattlegram.audio;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class AudioInputHandler implements AutoCloseable {

    private TargetDataLine line;
    private AudioInputStream audioInputStream;

    private final ReentrantLock reentrantLock = new ReentrantLock();

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
        reentrantLock.lock();
    }

    public void resume() {
        log.debug("Resuming audio input stream");
        line.start();
        reentrantLock.unlock();
    }

    public int read(byte[] buffer) throws IOException {
        // AudioInputStream.read(byte[]) does not block if line is stopped
        reentrantLock.lock();
        int read = audioInputStream.read(buffer);
        reentrantLock.unlock();
        return read;
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
