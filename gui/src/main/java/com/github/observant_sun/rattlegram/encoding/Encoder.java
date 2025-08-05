package com.github.observant_sun.rattlegram.encoding;

public interface Encoder extends AutoCloseable {

    void configure(byte[] payload, byte[] callSign, int carrierFrequency, int noiseSymbols, boolean fancyHeader);
    byte[] produce(int channelSelect);
    @Override
    void close();

    static Encoder newEncoder(int sampleRate, int channelCount) {
        return new EncoderImpl(sampleRate, channelCount);
    }
}
