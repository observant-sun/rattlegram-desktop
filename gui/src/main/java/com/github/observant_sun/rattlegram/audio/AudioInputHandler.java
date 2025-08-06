package com.github.observant_sun.rattlegram.audio;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import java.io.IOException;

public interface AudioInputHandler extends AutoCloseable {
    void start() throws LineUnavailableException;
    void pause();
    void resume();
    int read(byte[] buffer) throws IOException;

    static AudioInputHandler newAudioInputHandler(int sampleRate, int channelCount, Mixer.Info inputMixerInfo) {
        return new AudioInputHandlerImpl(sampleRate, channelCount, inputMixerInfo);
    }
}
