package com.github.observant_sun.rattlegram.encoding;

import com.github.observant_sun.rattlegram.util.Utils;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public interface Encoder extends AutoCloseable {

    void configure(byte[] payload, byte[] callSign, int carrierFrequency, int noiseSymbols, boolean fancyHeader);
    byte[] produce(int channelSelect);
    @Override
    void close();

    static Encoder newEncoder(int sampleRate, int channelCount) {
        return new EncoderImpl(sampleRate, channelCount);
    }
}
