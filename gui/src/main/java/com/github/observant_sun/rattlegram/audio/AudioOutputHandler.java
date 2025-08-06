package com.github.observant_sun.rattlegram.audio;

import javax.sound.sampled.LineUnavailableException;

public interface AudioOutputHandler {
    void play(byte[] audioData) throws LineUnavailableException;

    static AudioOutputHandler newAudioOutputHandler(int sampleRate, int channelCount, boolean artificiallyBlockingPlay) {
        return new AudioOutputHandlerImpl(sampleRate, channelCount, artificiallyBlockingPlay);
    }
}
