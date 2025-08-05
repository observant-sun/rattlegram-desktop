package com.github.observant_sun.rattlegram.encoding;

import com.github.observant_sun.rattlegram.audio.AudioInputHandler;
import com.github.observant_sun.rattlegram.entity.*;

import java.util.function.Consumer;

public interface Decoder extends AutoCloseable {

    void pause();
    void resume();
    SpectrumDecoderResult spectrumDecoder();
    void start();
    void setUpdateSpectrum(boolean updateSpectrum);
    @Override
    void close();

    static Decoder newDecoder(int sampleRate, int recordChannel, int channelCount,
                              Consumer<Message> newMessageCallback, Consumer<StatusUpdate> statusUpdateCallback,
                              Runnable spectrumUpdateCallback, AudioInputHandler audioInputHandler) {
        return new DecoderImpl(sampleRate, recordChannel, channelCount,
                newMessageCallback, statusUpdateCallback,
                spectrumUpdateCallback, audioInputHandler);
    }
}
