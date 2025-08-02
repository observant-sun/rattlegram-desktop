package com.github.observant_sun.rattlegram.model;

import com.github.observant_sun.rattlegram.audio.AudioOutputHandler;
import com.github.observant_sun.rattlegram.encoding.Encoder;
import com.github.observant_sun.rattlegram.entity.TransmissionSettings;
import com.github.observant_sun.rattlegram.prefs.*;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class EncoderInteractor {

    private Model model;

    @Getter
    private final AtomicReference<EncoderExecutor> encoderExecutorRef = new AtomicReference<>();

    public EncoderInteractor(Model model) {
        this.model = model;
    }

    public void init() {
        AppPreferences prefs = AppPreferences.get();
        final int outputSampleRate = prefs.get(Pref.OUTPUT_SAMPLE_RATE, SampleRate.class).getRateValue();
        final int outputChannelCount = prefs.get(Pref.OUTPUT_CHANNEL, OutputChannel.class).getChannelCount();
        Encoder encoder = new Encoder(outputSampleRate, outputChannelCount);
        AudioOutputHandler audioOutputHandler = new AudioOutputHandler(outputSampleRate, outputChannelCount);
        setEncoderExecutor(new EncoderExecutor(encoder, audioOutputHandler));
    }

    public void closeResources() {
        encoderExecutorRef.get().close();
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
        TransmissionSettings transmissionSettings = new TransmissionSettings(carrierFrequency, noiseSymbols, fancyHeader, channelSelect, delay);

        getEncoderExecutor().transmit(payload, callsignBytes, transmissionSettings);
    }

    public EncoderExecutor getEncoderExecutor() {
        return encoderExecutorRef.get();
    }

    public void setEncoderExecutor(EncoderExecutor encoderThread) {
        this.encoderExecutorRef.set(encoderThread);
    }

    private static byte[] getCallsignBytes(String callsign) {
        return Arrays.copyOf(callsign.getBytes(StandardCharsets.US_ASCII), callsign.length() + 1);
    }

    private static byte[] getPayload(String message) {
        return Arrays.copyOf(message.getBytes(StandardCharsets.UTF_8), 170);
    }
}
