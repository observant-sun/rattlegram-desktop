package com.github.observant_sun.rattlegram.model;

import com.github.observant_sun.rattlegram.audio.AudioInputHandler;
import com.github.observant_sun.rattlegram.encoding.Decoder;
import com.github.observant_sun.rattlegram.entity.*;
import com.github.observant_sun.rattlegram.prefs.AppPreferences;
import com.github.observant_sun.rattlegram.prefs.InputChannel;
import com.github.observant_sun.rattlegram.prefs.Pref;
import com.github.observant_sun.rattlegram.prefs.SampleRate;
import javafx.beans.value.ChangeListener;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.WritableImage;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.Mixer;
import java.nio.IntBuffer;
import java.time.Duration;
import java.util.function.Consumer;

@Slf4j
public class DecoderInteractor {

    private final Model model;
    private IncomingMessagesRepeatValidator incomingMessagesRepeatValidator;

    private final ChangeListener<Boolean> showSpectrumAnalyzerPropertyChangeListener = (observable, oldValue, newValue) -> {
        getDecoder().setUpdateSpectrum(newValue);
    };

    public DecoderInteractor(Model model) {
        this.model = model;
    }

    public void init() {
        AppPreferences prefs = AppPreferences.get();

        final int debounceDurationMs = prefs.get(Pref.REPEATER_DEBOUNCE_TIME, Integer.class);
        final Duration debounceDuration = Duration.ofMillis(debounceDurationMs);
        incomingMessagesRepeatValidator = new IncomingMessagesRepeatValidator(model.getIncomingMessages(), debounceDuration);

        final int inputSampleRate = prefs.get(Pref.INPUT_SAMPLE_RATE, SampleRate.class).getRateValue();
        final int inputChannel = prefs.get(Pref.INPUT_CHANNEL, InputChannel.class).getIntValue();
        final int inputChannelCount = prefs.get(Pref.INPUT_CHANNEL, InputChannel.class).getChannelCount();
        Consumer<Message> newMessageCallback = this::processNewIncomingMessage;
        Consumer<StatusUpdate> statusUpdateCallback = model::processStatusUpdate;
        Runnable spectrumUpdateCallback = this::updateSpectrogram;
        Mixer.Info inputMixerInfo = model.inputMixerInfoProperty().get().mixerInfo();
        AudioInputHandler audioInputHandler = AudioInputHandler.newAudioInputHandler(inputSampleRate, inputChannelCount, inputMixerInfo);
        Decoder decoder = Decoder.newDecoder(inputSampleRate, inputChannel, inputChannelCount, newMessageCallback, statusUpdateCallback, spectrumUpdateCallback, audioInputHandler);
        model.setDecoder(decoder);
        decoder.setUpdateSpectrum(model.showSpectrumAnalyzerProperty().get());
        model.showSpectrumAnalyzerProperty().addListener(showSpectrumAnalyzerPropertyChangeListener);
        try {
            decoder.start();
        } catch (Exception e) {
            log.error("Error starting audio input system", e);
            model.getStatusUpdatePublisher().submit(new StatusUpdate(StatusType.ERROR, "Error starting audio input system"));
            return;
        }
        model.getStatusUpdatePublisher().submit(new StatusUpdate(StatusType.OK, "Listening"));
    }

    public void closeResources() {
        model.showSpectrumAnalyzerProperty().removeListener(showSpectrumAnalyzerPropertyChangeListener);
        getDecoder().close();
    }

    public void pauseRecording() {
        getDecoder().pause();
    }

    private void updateSpectrogram() {
        SpectrumDecoderResult spectrumDecoderResult = getDecoder().spectrumDecoder();
        PixelBuffer<IntBuffer> spectrumPixels = spectrumDecoderResult.spectrumPixels();
        Image spectrumImage = new WritableImage(spectrumPixels);
        PixelBuffer<IntBuffer> spectrogramPixels = spectrumDecoderResult.spectrogramPixels();
        Image spectrogramImage = new WritableImage(spectrogramPixels);
        model.getUpdateSpectrogramPublisher().submit(new SpectrumImages(spectrumImage, spectrogramImage));
    }

    private void processNewIncomingMessage(Message incomingMessage) {
        log.debug("processNewIncomingMessage: {}", incomingMessage);
        Boolean repeaterModeEnabled = model.repeaterModeEnabledProperty().getValue();
        if (repeaterModeEnabled) {
            IncomingMessagesRepeatValidator.ValidationResult validationResult = incomingMessagesRepeatValidator.validate(incomingMessage);
            switch (validationResult) {
                case DEBOUNCE_INVALID ->
                        model.getStatusUpdatePublisher().submit(new StatusUpdate(StatusType.IGNORED, "Ignoring repeated message"));
                case INVALID_MESSAGE ->
                        model.getStatusUpdatePublisher().submit(new StatusUpdate(StatusType.IGNORED, "Invalid message, will not repeat"));
                case FAILED_MESSAGE -> {} // no-op
                case VALID -> repeatMessage(incomingMessage);
                case IS_OUTGOING_MESSAGE ->
                        log.error("Somehow got an outgoing message in processNewIncomingMessage: {}", incomingMessage);
            }
        }
        model.getMessages().add(incomingMessage);
    }

    private void repeatMessage(Message message) {
        AppPreferences prefs = AppPreferences.get();
        Integer delay = prefs.get(Pref.REPEATER_DELAY, Integer.class);
        if (delay <= 0) {
            delay = null;
        }
        log.debug("repeat delay: {} ms", delay);
        OutgoingMessage outgoingMessage = new OutgoingMessage(message.callsign(), message.body(), delay);
        model.getNewOutgoingMessagePublisher().submit(outgoingMessage);
    }

    private Decoder getDecoder() {
        return model.getDecoder();
    }


}
