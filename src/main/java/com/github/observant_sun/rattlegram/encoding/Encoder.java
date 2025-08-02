package com.github.observant_sun.rattlegram.encoding;

import com.github.observant_sun.rattlegram.util.Utils;
import lombok.Synchronized;
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

    private short[] outputBuffer;

    private volatile long encoderHandle;

    private native long createNewEncoder(int sampleRate);

    private native void configureEncoder(long encoderHandle, byte[] payload, byte[] callSign, int carrierFrequency, int noiseSymbols, boolean fancyHeader);

    private native boolean produceEncoder(long encoderHandle, short[] audioBuffer, int channelSelect);

    private native void destroyEncoder(long encoderHandle);

    public Encoder(int sampleRate, int channelCount) {
        this.encoderHandle = createNewEncoder(sampleRate);

        int symbolLength = (1280 * sampleRate) / 8000;
        int guardLength = symbolLength / 8;
        int extendedLength = symbolLength + guardLength;
        this.outputBuffer = new short[extendedLength * channelCount];
    }

    @Synchronized
    public void configure(byte[] payload, byte[] callSign, int carrierFrequency, int noiseSymbols, boolean fancyHeader) {
        this.configureEncoder(this.encoderHandle, payload, callSign, carrierFrequency, noiseSymbols, fancyHeader);
    }

    @Synchronized
    public byte[] produce(int channelSelect) {
        if (encoderHandle == 0) return null;
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
    @Synchronized
    public void close() {
        if (this.encoderHandle != 0) {
            destroyEncoder(this.encoderHandle);
            log.debug("Encoder {} destroyed", this.encoderHandle);
            this.encoderHandle = 0;
        }
    }


}
