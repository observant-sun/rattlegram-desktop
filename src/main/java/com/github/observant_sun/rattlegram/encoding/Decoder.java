package com.github.observant_sun.rattlegram.encoding;

import com.github.observant_sun.rattlegram.entity.*;
import com.github.observant_sun.rattlegram.audio.AudioInputHandler;
import com.github.observant_sun.rattlegram.i18n.I18n;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// TODO: refactor
public class Decoder implements AutoCloseable {

    private static final int spectrumWidth = 360, spectrumHeight = 128;
    private static final int spectrogramWidth = 360, spectrogramHeight = 128;

    private static final Logger log = LoggerFactory.getLogger(Decoder.class);

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
    private final Semaphore runSemaphore = new Semaphore(1);

    public Decoder(int sampleRate, int recordChannel, int channelCount,
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

    public void start() {
        try {
            audioInputHandler.start();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        Thread decoderThread = new Thread(this::run);
        decoderThread.setDaemon(true);
        decoderThread.start();
    }

    public void setUpdateSpectrum(boolean updateSpectrum) {
        this.updateSpectrum.set(updateSpectrum);
    }

    public record SpectrumDecoderResult (
            PixelBuffer<IntBuffer> spectrumPixels,
            PixelBuffer<IntBuffer> spectrogramPixels
    ) {
    }

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
        runSemaphore.acquireUninterruptibly();
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
        runSemaphore.release();
    }

    private void decodeNextBytes() {
        if (!feedDecoder(decoderHandle, transformedAudioInputBuffer, recordCount, recordChannel))
            return;
        int status = processDecoder(decoderHandle);
        if (updateSpectrum.get()) {
            spectrumUpdateCallback.run();
        }
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
                String preambleFailedMsg = I18n.get().getMessage(Decoder.class, "preambleFailed");
                statusUpdateCallback.accept(new StatusUpdate(StatusType.ERROR, preambleFailedMsg));
                break;
            case STATUS_NOPE:
                stagedDecoder(decoderHandle, stagedCFO, stagedMode, stagedCall);
                fromStatus();
                String modeUnsupportedMsg = I18n.get().getMessage(Decoder.class, "modeUnsupported");
                newMessageCallback.accept(new Message(getCallsign(), null, modeUnsupportedMsg.formatted(stagedMode[0]), LocalDateTime.now(), MessageType.ERROR_INCOMING));
                break;
            case STATUS_PING:
                stagedDecoder(decoderHandle, stagedCFO, stagedMode, stagedCall);
                fromStatus();
                String gotPingMsg = I18n.get().getMessage(Decoder.class, "gotPing");
                newMessageCallback.accept(new Message(getCallsign(), null, gotPingMsg, LocalDateTime.now(), MessageType.PING_INCOMING));
                break;
            case STATUS_HEAP:
                String notEnoughMemoryMsg = I18n.get().getMessage(Decoder.class, "notEnoughMemory");
                statusUpdateCallback.accept(new StatusUpdate(StatusType.ERROR, notEnoughMemoryMsg));
                break;
            case STATUS_SYNC:
                stagedDecoder(decoderHandle, stagedCFO, stagedMode, stagedCall);
                fromStatus();
                break;
            case STATUS_DONE:
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

    public void pause() {
        if (closed.get()) {
            return;
        }
        audioInputHandler.pause();
    }

    public void resume() {
        if (closed.get()) {
            return;
        }
        audioInputHandler.resume();
    }

    @Override
    public void close() {
        resume(); // needed to unlock audioInputHandler
        boolean alreadyClosed = closed.getAndSet(true);
        if (alreadyClosed) {
            log.warn("Attempted to close an already closed Decoder");
            return;
        }
        log.debug("Asking decoder to stop");
        try {
            boolean acquired = runSemaphore.tryAcquire(3, TimeUnit.SECONDS);
            if (!acquired) {
                log.error("Failed to acquire runSemaphore for closing, calling System.exit() so application doesn't hang");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        runSemaphore.release();
    }
}
