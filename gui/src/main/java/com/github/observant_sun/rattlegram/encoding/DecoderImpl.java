package com.github.observant_sun.rattlegram.encoding;

import com.github.observant_sun.rattlegram.audio.AudioInputHandler;
import com.github.observant_sun.rattlegram.entity.*;
import com.github.observant_sun.rattlegram.i18n.I18n;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
class DecoderImpl implements Decoder {

    private static final int spectrumWidth = 360, spectrumHeight = 128;
    private static final int spectrogramWidth = 360, spectrogramHeight = 128;

    static {
        System.loadLibrary("rattlegram");
    }

    private final int sampleRate;
    private final int recordChannel;
    private final int channelCount;

    private final AudioInputHandler audioInputHandler;

    private byte[] audioInputBuffer;
    private short[] transformedAudioInputBuffer;

    private long decoderHandle;
    private final AtomicBoolean updateSpectrum = new AtomicBoolean(true);

    private int recordCount;

    private final Consumer<Message> newMessageCallback;
    private final Consumer<StatusUpdate> statusUpdateCallback;
    private final Runnable spectrumUpdateCallback;

    private final float[] stagedCFO = new float[1];
    private final int[] stagedMode = new int[1];
    private final byte[] stagedCall = new byte[10];
    private final byte[] payload = new byte[170];

    private final int[] spectrumPixels = new int[spectrumWidth * spectrumHeight];
    private final int[] spectrogramPixels = new int[spectrogramHeight * spectrogramWidth];

    private final AtomicBoolean closed = new AtomicBoolean(false);

    public DecoderImpl(int sampleRate, int recordChannel, int channelCount,
                       Consumer<Message> newMessageCallback, Consumer<StatusUpdate> statusUpdateCallback,
                       Runnable spectrumUpdateCallback, AudioInputHandler audioInputHandler) {
        this.sampleRate = sampleRate;
        this.recordChannel = recordChannel;
        this.channelCount = channelCount;
        this.newMessageCallback = newMessageCallback;
        this.statusUpdateCallback = statusUpdateCallback;
        this.spectrumUpdateCallback = spectrumUpdateCallback;
        this.audioInputHandler = audioInputHandler;
        init();
    }

    @Override
    public void start() throws LineUnavailableException {
        audioInputHandler.start();
        Thread decoderThread = new Thread(this::run);
        decoderThread.setDaemon(true);
        decoderThread.start();
    }

    @Override
    public void setUpdateSpectrum(boolean updateSpectrum) {
        this.updateSpectrum.set(updateSpectrum);
    }

    @Override
    public SpectrumDecoderResult spectrumDecoder() {
        final int spectrumTint = 255;
        spectrumDecoder(this.decoderHandle, spectrumPixels, spectrogramPixels, spectrumTint);
        IntBuffer spectrumPixelsIntBuffer = IntBuffer.wrap(spectrumPixels);
        IntBuffer spectrogramPixelsIntBuffer = IntBuffer.wrap(spectrogramPixels);
        WritablePixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
        PixelBuffer<IntBuffer> spectrumPixels = new PixelBuffer<>(spectrumWidth, spectrumHeight, spectrumPixelsIntBuffer, pixelFormat);
        PixelBuffer<IntBuffer> spectrogramPixels = new PixelBuffer<>(spectrogramWidth, spectrogramHeight, spectrogramPixelsIntBuffer, pixelFormat);
        return new SpectrumDecoderResult(spectrumPixels, spectrogramPixels);
    }

    private native boolean feedDecoder(long decoderHandle, short[] audioBuffer, int sampleCount, int channelSelect);

    private native int processDecoder(long decoderHandle);

    private native void spectrumDecoder(long decoderHandle, int[] spectrumPixels, int[] spectrogramPixels, int spectrumTint);

    private native void stagedDecoder(long decoderHandle, float[] carrierFrequencyOffset, int[] operationMode, byte[] callSign);

    private native int fetchDecoder(long decoderHandle, byte[] payload);

    private native long createNewDecoder(int sampleRate);

    private native void destroyDecoder(long decoderHandle);

    private void init() {
        decoderHandle = createNewDecoder(sampleRate);
        if (decoderHandle == 0) {
            throw new RuntimeException("Failed to create decoder");
        }
        recordCount = sampleRate / 50;
        int transformedAudioInputBufferLength = channelCount * recordCount;
        audioInputBuffer = new byte[transformedAudioInputBufferLength * 2];
        transformedAudioInputBuffer = new short[transformedAudioInputBufferLength];
    }

    private void run() {
        while (!closed.get()) {
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

    @Getter
    @RequiredArgsConstructor
    private enum DecoderStatus {
        OKAY(0),
        FAIL(1),
        SYNC(2),
        DONE(3),
        HEAP(4),
        NOPE(5),
        PING(6),
        ;

        private final int statusCode;

        private static final Map<Integer, DecoderStatus> codeToStatusMap = new HashMap<>();

        static {
            for (DecoderStatus status : values()) {
                codeToStatusMap.put(status.statusCode, status);
            }
        }

        public static Optional<DecoderStatus> getByCode(int statusCode) {
            return Optional.ofNullable(codeToStatusMap.get(statusCode));
        }
    }

    private void decodeNextBytes() {
        if (!feedDecoder(decoderHandle, transformedAudioInputBuffer, recordCount, recordChannel))
            return;
        int statusCode = processDecoder(decoderHandle);
        DecoderStatus status = DecoderStatus.getByCode(statusCode)
                .orElseThrow(() -> new RuntimeException("Unknown decoder status: " + statusCode));
        if (updateSpectrum.get()) {
            spectrumUpdateCallback.run();
        }
        switch (status) {
            case OKAY:
                break;
            case FAIL:
                String preambleFailedMsg = I18n.get().getMessage(Decoder.class, "preambleFailed");
                statusUpdateCallback.accept(new StatusUpdate(StatusType.ERROR, preambleFailedMsg));
                break;
            case NOPE:
                stagedDecoder(decoderHandle, stagedCFO, stagedMode, stagedCall);
                fromStatus();
                String modeUnsupportedMsg = I18n.get().getMessage(Decoder.class, "modeUnsupported");
                newMessageCallback.accept(new Message(getCallsign(), null, modeUnsupportedMsg.formatted(stagedMode[0]), LocalDateTime.now(), MessageType.ERROR_INCOMING));
                break;
            case PING:
                stagedDecoder(decoderHandle, stagedCFO, stagedMode, stagedCall);
                fromStatus();
                String gotPingMsg = I18n.get().getMessage(Decoder.class, "gotPing");
                newMessageCallback.accept(new Message(getCallsign(), null, gotPingMsg, LocalDateTime.now(), MessageType.PING_INCOMING));
                break;
            case HEAP:
                String notEnoughMemoryMsg = I18n.get().getMessage(Decoder.class, "notEnoughMemory");
                statusUpdateCallback.accept(new StatusUpdate(StatusType.ERROR, notEnoughMemoryMsg));
                break;
            case SYNC:
                stagedDecoder(decoderHandle, stagedCFO, stagedMode, stagedCall);
                fromStatus();
                break;
            case DONE:
                int result = fetchDecoder(decoderHandle, payload);
                if (result < 0) {
                    String decodingFailedMsg = I18n.get().getMessage(Decoder.class, "decodingFailed");
                    newMessageCallback.accept(new Message(getCallsign(), null, decodingFailedMsg, LocalDateTime.now(), MessageType.ERROR_INCOMING));
                } else {
                    String bitFlipsCorrectedMsg = I18n.get().getMessage(Decoder.class, "bitFlipsCorrected");
                    statusUpdateCallback.accept(new StatusUpdate(StatusType.OK, bitFlipsCorrectedMsg.formatted(result)));
                    newMessageCallback.accept(new Message(getCallsign(), new String(payload).trim(), null, LocalDateTime.now(), MessageType.NORMAL_INCOMING));
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
        String fromMsg = I18n.get().getMessage(Decoder.class, "from");
        statusUpdateCallback.accept(
                new StatusUpdate(StatusType.OK, fromMsg.formatted(new String(stagedCall).trim(), stagedMode[0], stagedCFO[0]))
        );
    }

    @Override
    public void pause() {
        if (closed.get()) {
            return;
        }
        audioInputHandler.pause();
    }

    @Override
    public void resume() {
        if (closed.get()) {
            return;
        }
        audioInputHandler.resume();
    }

    @Override
    public void close() {
        resume();
        boolean alreadyClosed = closed.getAndSet(true);
        if (alreadyClosed) {
            log.warn("Attempted to close an already closed Decoder");
            return;
        }
        log.debug("Asking decoder to stop");
        try {
            audioInputHandler.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.debug("Closing decoder {}", decoderHandle);
        try {
            if (decoderHandle != 0) {
                destroyDecoder(decoderHandle);
                decoderHandle = 0;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
