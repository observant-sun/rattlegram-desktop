package com.github.observant_sun.rattlegram.model;

import com.github.observant_sun.rattlegram.audio.AudioInputHandler;
import com.github.observant_sun.rattlegram.audio.AudioOutputHandler;
import com.github.observant_sun.rattlegram.encoding.Decoder;
import com.github.observant_sun.rattlegram.encoding.Encoder;
import com.github.observant_sun.rattlegram.entity.*;
import com.github.observant_sun.rattlegram.prefs.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.WritableImage;
import lombok.extern.slf4j.Slf4j;

import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class Model {

    private static final class InstanceHolder {
        private static final Model instance = new Model();
    }

    public static Model get() {
        return InstanceHolder.instance;
    }

    private Encoder encoder;
    private Decoder decoder;

    private AudioOutputHandler audioOutputHandler;

    private ScheduledExecutorService encoderThread;

    private List<Message> messages = new ArrayList<>();
    private ObservableList<Message> incomingMessages = FXCollections.observableArrayList();
    private IncomingMessagesRepeatValidator incomingMessagesRepeatValidator;

    private List<Consumer<Message>> newMessageCallbacks = new ArrayList<>();
    private List<StatusUpdate> statusUpdates = new ArrayList<>();
    private List<Consumer<StatusUpdate>> statusUpdateCallbacks = new ArrayList<>();

    private List<Runnable> transmissionBeginCallbacks = new ArrayList<>();
    private List<Runnable> listeningBeginCallbacks = new ArrayList<>();

    private List<Consumer<SpectrumImages>> updateSpectrogramCallbacks = new ArrayList<>();

    public record SpectrumImages (
            Image spectrum,
            Image spectrogram
    ) {}

    private Model() {

    }

    public void initializeEncoders() {
        AppPreferences prefs = AppPreferences.get();

        final int debounceDurationMs = prefs.get(Pref.REPEATER_DEBOUNCE_TIME, Integer.class);
        final Duration debounceDuration = Duration.ofMillis(debounceDurationMs);
        incomingMessagesRepeatValidator = new IncomingMessagesRepeatValidator(incomingMessages, debounceDuration);

        final int outputSampleRate = prefs.get(Pref.OUTPUT_SAMPLE_RATE, SampleRate.class).getRateValue();
        final int outputChannelCount = prefs.get(Pref.OUTPUT_CHANNEL, OutputChannel.class).getChannelCount();

        this.encoder = new Encoder(outputSampleRate, outputChannelCount);
        this.audioOutputHandler = new AudioOutputHandler(outputSampleRate, outputChannelCount);

        this.encoderThread = Executors.newSingleThreadScheduledExecutor((runnable) -> {
            Thread thread = new Thread(runnable, "encoder-thread");
            thread.setDaemon(true);
            return thread;
        });

        final int inputSampleRate = prefs.get(Pref.INPUT_SAMPLE_RATE, SampleRate.class).getRateValue();
        final int inputChannel = prefs.get(Pref.INPUT_CHANNEL, InputChannel.class).getIntValue();
        final int inputChannelCount = prefs.get(Pref.INPUT_CHANNEL, InputChannel.class).getChannelCount();
        Consumer<Message> newMessageCallback = this::processNewIncomingMessage;
        Consumer<StatusUpdate> statusUpdateCallback = this::processStatusUpdate;
        Runnable spectrumUpdateCallback = this::updateSpectrogram;
        AudioInputHandler audioInputHandler = new AudioInputHandler(inputSampleRate, inputChannelCount);
        this.decoder = new Decoder(inputSampleRate, inputChannel, inputChannelCount, newMessageCallback, statusUpdateCallback, spectrumUpdateCallback, audioInputHandler);
        this.decoder.setUpdateSpectrum(showSpectrumAnalyzerProperty().get());
        this.decoder.start();
        processStatusUpdate(new StatusUpdate(StatusType.OK, "Listening"));
    }

    private void processNewIncomingMessage(Message incomingMessage) {
        log.debug("processNewIncomingMessage: {}", incomingMessage);
        incomingMessages.add(incomingMessage);
        if (repeaterModeEnabledProperty().getValue()) {
            IncomingMessagesRepeatValidator.ValidationResult validationResult = incomingMessagesRepeatValidator.validate(incomingMessage);
            switch (validationResult) {
                case DEBOUNCE_INVALID ->
                        statusUpdateCallbacks.add(statusUpdate -> new StatusUpdate(StatusType.IGNORED, "Ignoring repeated message"));
                case INVALID_MESSAGE ->
                        statusUpdateCallbacks.add(statusUpdate -> new StatusUpdate(StatusType.IGNORED, "Invalid message, will not repeat"));
                case FAILED_MESSAGE -> {} // no-op
                case VALID -> repeatMessage(incomingMessage);
                case IS_OUTGOING_MESSAGE ->
                        log.error("Somehow got an outgoing message in processNewIncomingMessage: {}", incomingMessage);
            }
        }
        for (Consumer<Message> callback : newMessageCallbacks) {
            callback.accept(incomingMessage);
        }
    }

    private void repeatMessage(Message incomingMessage) {
        AppPreferences prefs = AppPreferences.get();
        Integer delay = prefs.get(Pref.REPEATER_DELAY, Integer.class);
        if (delay <= 0) {
            delay = null;
        }
        log.debug("repeat delay: {} ms", delay);
        transmitNewMessage(incomingMessage.callsign(), incomingMessage.body(), delay);
    }

    private void processStatusUpdate(StatusUpdate statusUpdate) {
        statusUpdates.add(statusUpdate);
        for (Consumer<StatusUpdate> callback : statusUpdateCallbacks) {
            callback.accept(statusUpdate);
        }
    }

    public void addNewMessageCallback(Consumer<Message> newMessageCallback) {
        this.newMessageCallbacks.add(newMessageCallback);
    }

    public void addStatusUpdateCallback(Consumer<StatusUpdate> statusUpdateCallback) {
        this.statusUpdateCallbacks.add(statusUpdateCallback);
    }

    public void addTransmissionBeginCallback(Runnable transmissionBeginCallback) {
        this.transmissionBeginCallbacks.add(transmissionBeginCallback);
    }

    public void addListeningBeginCallback(Runnable listeningBeginCallback) {
        this.listeningBeginCallbacks.add(listeningBeginCallback);
    }

    public void transmitNewMessage(String callsign, String message) {
        transmitNewMessage(callsign, message, null);
    }

    public void transmitNewMessage(String callsign, String message, Integer delay) {
        if (callsign == null) callsign = "";
        if (message == null) message = "";

        byte[] payload = getPayload(message);
        byte[] callsignBytes = getCallsignBytes(callsign);

        AppPreferences prefs = AppPreferences.get();
        final int carrierFrequency = prefs.get(Pref.CARRIER_FREQUENCY, Integer.class);
        final int noiseSymbols = prefs.get(Pref.LEADING_NOISE, LeadingNoise.class).getNoiseSymbols();
        final boolean fancyHeader = prefs.get(Pref.FANCY_HEADER, Boolean.class);
        final int channelSelect = prefs.get(Pref.OUTPUT_CHANNEL, OutputChannel.class).getIntValue();
        TransmissionSettings transmissionSettings = new TransmissionSettings(carrierFrequency, noiseSymbols, fancyHeader, channelSelect);
        Runnable runnable = () -> {
            byte[] audioOutputBytes = produceAudioOutputBytes(payload, callsignBytes, transmissionSettings);
            playAudioOutputBytes(audioOutputBytes);
        };
        if (delay != null) {
            encoderThread.schedule(runnable, delay, TimeUnit.MILLISECONDS);
        } else {
            encoderThread.submit(runnable);
        }
    }

    private void closeResources() {
        encoder.close();
        decoder.close();
        encoderThread.shutdownNow();
    }

    public void reinitializeEncoders() {
        closeResources();
        initializeEncoders();
    }

    private static byte[] getCallsignBytes(String callsign) {
        return Arrays.copyOf(callsign.getBytes(StandardCharsets.US_ASCII), callsign.length() + 1);
    }

    private static byte[] getPayload(String message) {
        return Arrays.copyOf(message.getBytes(StandardCharsets.UTF_8), 170);
    }

    private byte[] produceAudioOutputBytes(byte[] payload, byte[] callsignBytes, TransmissionSettings transmissionSettings) {
        encoder.configure(payload, callsignBytes, transmissionSettings.carrierFrequency(), transmissionSettings.noiseSymbols(), transmissionSettings.fancyHeader());

        return encoder.produce(transmissionSettings.channelSelect());
    }

    private void playAudioOutputBytes(byte[] arr) {
        Boolean stopListeningWhenTransmitting = AppPreferences.get().get(Pref.STOP_LISTENING_WHEN_TRANSMITTING, Boolean.class);
        transmissionBeginCallbacks.forEach(Runnable::run);
        Platform.runLater(() -> processStatusUpdate(new StatusUpdate(StatusType.OK, "Transmitting")));

        if (stopListeningWhenTransmitting) {
            decoder.pause();
        }

        audioOutputHandler.play(arr);

        if (stopListeningWhenTransmitting) {
            decoder.resume();
        }

        listeningBeginCallbacks.forEach(Runnable::run);
    }

    public void pauseRecording() {
        decoder.pause();
    }

    public void addUpdateSpectrogramCallback(Consumer<SpectrumImages> updateSpectrogramCallback) {
        this.updateSpectrogramCallbacks.add(updateSpectrogramCallback);
    }

    private void updateSpectrogram() {
        Decoder.SpectrumDecoderResult spectrumDecoderResult = decoder.spectrumDecoder();
        PixelBuffer<IntBuffer> spectrumPixels = spectrumDecoderResult.spectrumPixels();
        Image spectrumImage = new WritableImage(spectrumPixels);
        PixelBuffer<IntBuffer> spectrogramPixels = spectrumDecoderResult.spectrogramPixels();
        Image spectrogramImage = new WritableImage(spectrogramPixels);
        for (Consumer<SpectrumImages> callback : updateSpectrogramCallbacks) {
            callback.accept(new SpectrumImages(spectrumImage, spectrogramImage));
        }
    }

    private volatile BooleanProperty showSpectrumAnalyzer;
    public BooleanProperty showSpectrumAnalyzerProperty() {
        if (showSpectrumAnalyzer == null) {
            synchronized (this) {
                if (showSpectrumAnalyzer == null) {
                    boolean initialValue = AppPreferences.get().get(Pref.SHOW_SPECTRUM_ANALYZER, Boolean.class);
                    showSpectrumAnalyzer = new SimpleBooleanProperty(this, "showSpectrogram", initialValue);
                    showSpectrumAnalyzer.addListener((observable, oldValue, newValue) -> {
                        decoder.setUpdateSpectrum(newValue);
                        AppPreferences.get().set(Pref.SHOW_SPECTRUM_ANALYZER, newValue);
                    });
                }
            }
        }
        return showSpectrumAnalyzer;
    }

    private volatile BooleanProperty showRepeaterWindow = new SimpleBooleanProperty(false);
    public BooleanProperty showRepeaterWindowProperty() {
        return showRepeaterWindow;
    }

    public void toggleRepeaterWindow() {
        showRepeaterWindow.set(!showRepeaterWindow.get());
    }

    private volatile BooleanProperty repeaterModeEnabled;
    public BooleanProperty repeaterModeEnabledProperty() {
        if (repeaterModeEnabled == null) {
            synchronized (this) {
                if (repeaterModeEnabled == null) {
                    boolean initialValue = AppPreferences.get().get(Pref.REPEATER_MODE_ENABLED, Boolean.class);
                    repeaterModeEnabled = new SimpleBooleanProperty(this, "repeaterModeEnabled", initialValue);
                    repeaterModeEnabled.addListener((observable, oldValue, newValue) -> {
                        AppPreferences.get().set(Pref.REPEATER_MODE_ENABLED, newValue);
                    });
                }
            }
        }
        return repeaterModeEnabled;
    }

    private volatile IntegerProperty repeaterDelay;
    public IntegerProperty repeaterDelayProperty() {
        if (repeaterDelay == null) {
            synchronized (this) {
                if (repeaterDelay == null) {
                    int initialValue = AppPreferences.get().get(Pref.REPEATER_DELAY, Integer.class);
                    repeaterDelay = new SimpleIntegerProperty(this, "repeaterDelay", initialValue);
                    repeaterDelay.addListener((observable, oldValue, newValue) -> {
                        AppPreferences.get().set(Pref.REPEATER_DELAY, newValue);
                    });
                }
            }
        }
        return repeaterDelay;
    }

    private volatile IntegerProperty repeaterDebounceTime;
    public IntegerProperty repeaterDebounceTimeProperty() {
        if (repeaterDebounceTime == null) {
            synchronized (this) {
                if (repeaterDebounceTime == null) {
                    int initialValue = AppPreferences.get().get(Pref.REPEATER_DEBOUNCE_TIME, Integer.class);
                    repeaterDebounceTime = new SimpleIntegerProperty(this, "repeaterDebounceTime", initialValue);
                    repeaterDebounceTime.addListener((observable, oldValue, newValue) -> {
                        AppPreferences.get().set(Pref.REPEATER_DEBOUNCE_TIME, newValue);
                    });
                }
            }
        }
        return repeaterDebounceTime;
    }

}
