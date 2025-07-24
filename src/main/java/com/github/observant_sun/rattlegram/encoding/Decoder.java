package com.github.observant_sun.rattlegram.encoding;

import com.github.observant_sun.rattlegram.entity.Message;
import com.github.observant_sun.rattlegram.entity.MessageType;
import com.github.observant_sun.rattlegram.entity.StatusType;
import com.github.observant_sun.rattlegram.entity.StatusUpdate;
import com.github.observant_sun.rattlegram.audio.AudioInputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Consumer;

// TODO: refactor
public class Decoder implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Decoder.class);

    static {
        System.loadLibrary("rattlegram");
    }

    private final int sampleRate;

    private final AudioInputHandler audioInputHandler;

    private byte[] audioInputBuffer;
    private short[] transformedAudioInputBuffer;

    private long decoderHandle;

    private int recordCount;

    private final Consumer<Message> newMessageCallback;
    private final Consumer<StatusUpdate> statusUpdateCallback;

    private final float[] stagedCFO = new float[1];
    private final int[] stagedMode = new int[1];
    private final byte[] stagedCall = new byte[10];
    private final byte[] payload = new byte[170];

    public Decoder(int sampleRate, Consumer<Message> newMessageCallback, Consumer<StatusUpdate> statusUpdateCallback,
                   AudioInputHandler audioInputHandler) {
        this.sampleRate = sampleRate;
        this.newMessageCallback = newMessageCallback;
        this.statusUpdateCallback = statusUpdateCallback;
        this.audioInputHandler = audioInputHandler;
        try {
            init();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }


    private native boolean feedDecoder(long decoderHandle, short[] audioBuffer, int sampleCount, int channelSelect);

    private native int processDecoder(long decoderHandle);

    private native void spectrumDecoder(long decoderHandle, int[] spectrumPixels, int[] spectrogramPixels, int spectrumTint);

    private native void stagedDecoder(long decoderHandle, float[] carrierFrequencyOffset, int[] operationMode, byte[] callSign);

    private native int fetchDecoder(long decoderHandle, byte[] payload);

    private native long createNewDecoder(int sampleRate);

    private native void destroyDecoder(long decoderHandle);

    private void init() throws LineUnavailableException {
        final int channelCount = 1;
        final int sampleSize = 2; //
        final int frameSize = sampleSize * channelCount;
        final int bufferSize = 2 * Integer.highestOneBit(3 * sampleRate) * frameSize;
        decoderHandle = createNewDecoder(sampleRate);
        if (decoderHandle == 0) {
            throw new RuntimeException("Failed to create decoder");
        }
        recordCount = sampleRate / 50;
        int transformedAudioInputBufferLength = channelCount * recordCount;
        audioInputBuffer = new byte[transformedAudioInputBufferLength * 2];
        transformedAudioInputBuffer = new short[transformedAudioInputBufferLength];
        audioInputHandler.start();
        Thread thread = new Thread(this::run);
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        while (true) {
            int read;
            try {
                read = audioInputHandler.read(audioInputBuffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (read == -1) {
                log.warn("Got EOF from audio input stream, stopping");
                break;
            }
            copyToTransformedBuffer();
            decodeNextBytes();
        }
    }

    private void decodeNextBytes() {
        final int recordChannel = 0;
        if (!feedDecoder(decoderHandle, transformedAudioInputBuffer, recordCount, recordChannel))
            return;
        int status = processDecoder(decoderHandle);

        final int STATUS_OKAY = 0;
        final int STATUS_FAIL = 1;
        final int STATUS_SYNC = 2;
        final int STATUS_DONE = 3;
        final int STATUS_HEAP = 4;
        final int STATUS_NOPE = 5;
        final int STATUS_PING = 6;
        switch (status) {
            case STATUS_OKAY:
                break;
            case STATUS_FAIL:
                statusUpdateCallback.accept(new StatusUpdate(StatusType.ERROR, "Preamble failed"));
                break;
            case STATUS_NOPE:
                stagedDecoder(decoderHandle, stagedCFO, stagedMode, stagedCall);
                fromStatus();
                newMessageCallback.accept(new Message(getCallsign(), "Mode %s unsupported".formatted(stagedMode[0]), LocalDateTime.now(), MessageType.ERROR));
                break;
            case STATUS_PING:
                stagedDecoder(decoderHandle, stagedCFO, stagedMode, stagedCall);
                fromStatus();
                newMessageCallback.accept(new Message(getCallsign(), "Got ping", LocalDateTime.now(), MessageType.OK));
                break;
            case STATUS_HEAP:
                statusUpdateCallback.accept(new StatusUpdate(StatusType.ERROR, "Not enough memory"));
                break;
            case STATUS_SYNC:
                stagedDecoder(decoderHandle, stagedCFO, stagedMode, stagedCall);
                fromStatus();
                break;
            case STATUS_DONE:
                int result = fetchDecoder(decoderHandle, payload);
                if (result < 0) {
                    newMessageCallback.accept(new Message(getCallsign(), "Decoding failed", LocalDateTime.now(), MessageType.ERROR));
                } else {
                    statusUpdateCallback.accept(new StatusUpdate(StatusType.OK, "%d bit flips corrected".formatted(result)));
                    newMessageCallback.accept(new Message(getCallsign(), new String(payload).trim(), LocalDateTime.now(), MessageType.OK));
                }
                break;
        }
    }

    private String getCallsign() {
        return new String(stagedCall).trim();
    }

    private void copyToTransformedBuffer() {
        for (int i = 0; i < transformedAudioInputBuffer.length; i++) {
            transformedAudioInputBuffer[i] = (short) ((audioInputBuffer[i * 2 + 1] << 8) + audioInputBuffer[i * 2]);
        }
    }

    private void fromStatus() {
        statusUpdateCallback.accept(
                new StatusUpdate(StatusType.OK, "From %1$s - Mode %2$d - CFO %3$.2f Hz".formatted(new String(stagedCall).trim(), stagedMode[0], stagedCFO[0]))
        );
    }

    public void pause() {
        audioInputHandler.pause();
    }

    public void resume() {
        audioInputHandler.resume();
    }

    @Override
    public void close() throws Exception {
        try {
            audioInputHandler.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        try {
            destroyDecoder(decoderHandle);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
