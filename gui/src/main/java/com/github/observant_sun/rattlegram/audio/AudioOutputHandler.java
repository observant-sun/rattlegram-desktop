package com.github.observant_sun.rattlegram.audio;

public interface AudioOutputHandler {
    void play(byte[] audioData);

    static AudioOutputHandler newAudioOutputHandler(int sampleRate, int channelCount) {
        return new AudioOutputHandlerImpl(sampleRate, channelCount);
    }
}
