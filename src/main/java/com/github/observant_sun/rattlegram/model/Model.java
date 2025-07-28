package com.github.observant_sun.rattlegram.model;

import com.github.observant_sun.rattlegram.audio.AudioInputHandler;
import com.github.observant_sun.rattlegram.audio.AudioOutputHandler;
import com.github.observant_sun.rattlegram.encoding.Decoder;
import com.github.observant_sun.rattlegram.encoding.Encoder;
import com.github.observant_sun.rattlegram.entity.Message;
import com.github.observant_sun.rattlegram.entity.StatusType;
import com.github.observant_sun.rattlegram.entity.StatusUpdate;
import com.github.observant_sun.rattlegram.entity.TransmissionSettings;
import com.github.observant_sun.rattlegram.prefs.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.WritableImage;
import lombok.extern.slf4j.Slf4j;

import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private ExecutorService encoderThread;

    private List<Message> messages = new ArrayList<>();
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
        final int outputSampleRate = prefs.get(Pref.OUTPUT_SAMPLE_RATE, SampleRate.class).getRateValue();
        final int outputChannelCount = prefs.get(Pref.OUTPUT_CHANNEL, OutputChannel.class).getChannelCount();

        this.encoder = new Encoder(outputSampleRate, outputChannelCount);
        this.audioOutputHandler = new AudioOutputHandler(outputSampleRate, outputChannelCount);

        this.encoderThread = Executors.newSingleThreadExecutor((runnable) -> {
            Thread thread = new Thread(runnable, "encoder-thread");
            thread.setDaemon(true);
            return thread;
        });

        final int inputSampleRate = prefs.get(Pref.INPUT_SAMPLE_RATE, SampleRate.class).getRateValue();
        final int inputChannel = prefs.get(Pref.INPUT_CHANNEL, InputChannel.class).getIntValue();
        final int inputChannelCount = prefs.get(Pref.INPUT_CHANNEL, InputChannel.class).getChannelCount();
        Consumer<Message> newMessageCallback = this::processNewMessage;
        Consumer<StatusUpdate> statusUpdateCallback = this::processStatusUpdate;
        Runnable spectrumUpdateCallback = this::updateSpectrogram;
        AudioInputHandler audioInputHandler = new AudioInputHandler(inputSampleRate, inputChannelCount);
        this.decoder = new Decoder(inputSampleRate, inputChannel, inputChannelCount, newMessageCallback, statusUpdateCallback, spectrumUpdateCallback, audioInputHandler);
        this.decoder.setUpdateSpectrum(showSpectrumAnalyzerProperty().get());
        this.decoder.start();
        processStatusUpdate(new StatusUpdate(StatusType.OK, "Listening"));
    }

    private void processNewMessage(Message message) {
        messages.add(message);
        for (Consumer<Message> callback : newMessageCallbacks) {
            callback.accept(message);
        }
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
        byte[] payload = getPayload(message);
        byte[] callsignBytes = getCallsignBytes(callsign);

        AppPreferences prefs = AppPreferences.get();
        final int carrierFrequency = prefs.get(Pref.CARRIER_FREQUENCY, Integer.class);
        final int noiseSymbols = prefs.get(Pref.LEADING_NOISE, LeadingNoise.class).getNoiseSymbols();
        final boolean fancyHeader = prefs.get(Pref.FANCY_HEADER, Boolean.class);
        final int channelSelect = prefs.get(Pref.OUTPUT_CHANNEL, OutputChannel.class).getIntValue();
        TransmissionSettings transmissionSettings = new TransmissionSettings(carrierFrequency, noiseSymbols, fancyHeader, channelSelect);
        encoderThread.submit(() -> {
            byte[] audioOutputBytes = produceAudioOutputBytes(payload, callsignBytes, transmissionSettings);
            playAudioOutputBytes(audioOutputBytes);
        });
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
        transmissionBeginCallbacks.forEach(Runnable::run);
        Platform.runLater(() -> processStatusUpdate(new StatusUpdate(StatusType.OK, "Transmitting")));

        decoder.pause();

        audioOutputHandler.play(arr);

        decoder.resume();

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

}
