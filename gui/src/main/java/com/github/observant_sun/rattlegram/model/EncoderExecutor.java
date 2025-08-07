package com.github.observant_sun.rattlegram.model;

import com.github.observant_sun.rattlegram.audio.AudioOutputHandler;
import com.github.observant_sun.rattlegram.encoding.Decoder;
import com.github.observant_sun.rattlegram.encoding.Encoder;
import com.github.observant_sun.rattlegram.entity.TransmissionSettings;
import com.github.observant_sun.rattlegram.prefs.AppPreferences;
import com.github.observant_sun.rattlegram.prefs.Pref;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.LineUnavailableException;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
public class EncoderExecutor implements AutoCloseable {

    @Getter
    private final Encoder encoder;
    @Getter
    private final AudioOutputHandler audioOutputHandler;
    @Getter
    private final Consumer<Exception> transmissionFailureCallback;

    private final ScheduledExecutorService executor;

    public EncoderExecutor(Encoder encoder, AudioOutputHandler audioOutputHandler, Consumer<Exception> transmissionFailureCallback) {
        this.encoder = encoder;
        this.audioOutputHandler = audioOutputHandler;
        this.transmissionFailureCallback = transmissionFailureCallback;
        this.executor = Executors.newSingleThreadScheduledExecutor((runnable) -> {
            Thread thread = new Thread(runnable, "encoder-thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void transmit(byte[] payload, byte[] callsignBytes, TransmissionSettings transmissionSettings, Runnable beforeTransmit) {
        Runnable runnable = () -> {
            try {
                log.debug("running transmit");
                beforeTransmit.run();
                log.debug("pre-transmit complete");
                byte[] audioOutputBytes = produceAudioOutputBytes(payload, callsignBytes, transmissionSettings);
                log.debug("bytes produced");
                playAudioOutputBytes(audioOutputBytes);
                log.debug("transmission complete");
            } catch (Exception e) {
                log.error("Error transmitting audio output", e);
                transmissionFailureCallback.accept(e);
            }
        };
        schedule(runnable, transmissionSettings.delay());
    }

    private void schedule(Runnable runnable, Integer delayMs) {
        if (delayMs != null) {
            executor.schedule(runnable, delayMs, TimeUnit.MILLISECONDS);
        } else {
            executor.submit(runnable);
        }
    }

    private byte[] produceAudioOutputBytes(byte[] payload, byte[] callsignBytes, TransmissionSettings transmissionSettings) {
        Encoder encoder = getEncoder();
        encoder.configure(payload, callsignBytes, transmissionSettings.carrierFrequency(), transmissionSettings.noiseSymbols(), transmissionSettings.fancyHeader());

        return encoder.produce(transmissionSettings.channelSelect());
    }

    private void playAudioOutputBytes(byte[] arr) throws LineUnavailableException {
        Model model = Model.get();
        Boolean stopListeningWhenTransmitting = AppPreferences.get().get(Pref.STOP_LISTENING_WHEN_TRANSMITTING, Boolean.class);
        model.getTransmissionBeginPublisher().publish();
        log.debug("published transmission begin");

        Decoder decoder = model.getDecoder();
        if (stopListeningWhenTransmitting) {
            decoder.pause();
            log.debug("decoder paused");
        }
        try {
            log.debug("playing audio output");
            getAudioOutputHandler().play(arr);
            log.debug("audio output played");
        } finally {
            if (stopListeningWhenTransmitting) {
                decoder.resume();
                log.debug("decoder resumed");
            }
        }

        model.getListeningBeginPublisher().publish();
        log.debug("published listening begin");
    }

    @Override
    public void close() {
        executor.shutdownNow();

        encoder.close();
    }
}
