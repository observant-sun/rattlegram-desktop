package com.github.observant_sun.rattlegram.audio;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

public interface AudioOutputHandler {
    void play(byte[] audioData) throws LineUnavailableException;

    static AudioOutputHandler newAudioOutputHandler(int sampleRate, int channelCount, boolean artificiallyBlockingPlay, Mixer.Info mixerInfo) {
        return new AudioOutputHandlerImpl(sampleRate, channelCount, artificiallyBlockingPlay, mixerInfo);
    }
}
