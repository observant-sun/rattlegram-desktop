package com.github.observant_sun.rattlegram.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

public class AudioOutputHandler {

    private final int sampleRate;

    public AudioOutputHandler(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public synchronized void play(byte[] buffer) {
        Clip clip;
        try {
            clip = AudioSystem.getClip();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        try {
            AudioFormat format = getAudioFormat();
            clip.open(format, buffer, 0, buffer.length);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        clip.start();
        clip.drain();
    }

    private AudioFormat getAudioFormat() {
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        int sampleSizeInBits = 16;
        int channels = 1;
        int frameSize = 2;
        float frameRate = (float) (sampleRate / 2);
        boolean bigEndian = false;
        return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
    }
}
