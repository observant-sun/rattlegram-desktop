package com.github.observant_sun.rattlegram.encoding;

import com.github.observant_sun.rattlegram.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Encoder implements AutoCloseable {

    private static final int REPEAT_COUNT = 50;

    private static final Logger log = LoggerFactory.getLogger(Encoder.class);

    static {
        System.loadLibrary("rattlegram");
    }


    private final int sampleRate;
    private final int symbolLength;
    private final int guardLength;
    private final int extendedLength;

    private short[] outputBuffer;

    private long encoderHandle;

    private native long createNewEncoder(int sampleRate);

    private native void configureEncoder(long encoderHandle, byte[] payload, byte[] callSign, int carrierFrequency, int noiseSymbols, boolean fancyHeader);

    private native boolean produceEncoder(long encoderHandle, short[] audioBuffer, int channelSelect);

    private native void destroyEncoder(long encoderHandle);

    public Encoder(int sampleRate, int channelCount) {
        this.sampleRate = sampleRate;
        encoderHandle = createNewEncoder(sampleRate);
        symbolLength = (1280 * sampleRate) / 8000;
        guardLength = symbolLength / 8;
        extendedLength = symbolLength + guardLength;
        outputBuffer = new short[extendedLength * channelCount];
    }

    public void configure(byte[] payload, byte[] callSign, int carrierFrequency, int noiseSymbols, boolean fancyHeader) {
        this.configureEncoder(this.encoderHandle, payload, callSign, carrierFrequency, noiseSymbols, fancyHeader);
    }

    public byte[] produce(int channelSelect) {
        log.debug("produce(channelSelect={}), encoderHandle={}", channelSelect, encoderHandle);
        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < REPEAT_COUNT; i++) {
            boolean okay = this.produceEncoder(this.encoderHandle, outputBuffer, channelSelect);
            if (!okay) {
                break;
            }
            byte[] bytes = Utils.shortArrayToNewByteArray(outputBuffer);
            list.add(bytes);
        }
        int length = 0;
        for (byte[] byteArray : list) {
            length += byteArray.length;
        }
        byte[] arr = new byte[length];
        int offset = 0;
        for (byte[] bytes : list) {
            System.arraycopy(bytes, 0, arr, offset, bytes.length);
            offset += bytes.length;
        }
        return arr;
    }

    @Override
    public void close() {
        if (this.encoderHandle != 0) {
            destroyEncoder(this.encoderHandle);
            log.debug("Encoder {} destroyed", this.encoderHandle);
            this.encoderHandle = 0;
        }
    }


}
