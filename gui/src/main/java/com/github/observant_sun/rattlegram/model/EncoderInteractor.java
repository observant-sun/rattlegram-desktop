package com.github.observant_sun.rattlegram.model;

import com.github.observant_sun.rattlegram.audio.AudioOutputHandler;
import com.github.observant_sun.rattlegram.encoding.Encoder;
import com.github.observant_sun.rattlegram.entity.Message;
import com.github.observant_sun.rattlegram.entity.MessageType;
import com.github.observant_sun.rattlegram.entity.OutgoingMessage;
import com.github.observant_sun.rattlegram.entity.TransmissionSettings;
import com.github.observant_sun.rattlegram.prefs.*;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class EncoderInteractor {

    private final Model model;

    @Getter
    private final AtomicReference<EncoderExecutor> encoderExecutorRef = new AtomicReference<>();

    public EncoderInteractor(Model model) {
        this.model = model;
        model.getNewOutgoingMessagePublisher().subscribe(this::transmitNewMessage);
    }

    public void init() {
        AppPreferences prefs = AppPreferences.get();
        final int outputSampleRate = prefs.get(Pref.OUTPUT_SAMPLE_RATE, SampleRate.class).getRateValue();
        final int outputChannelCount = prefs.get(Pref.OUTPUT_CHANNEL, OutputChannel.class).getChannelCount();
        Encoder encoder = Encoder.newEncoder(outputSampleRate, outputChannelCount);
        boolean artificiallyBlockingPlay = prefs.get(Pref.BLOCK_OUTPUT_DRAIN_WORKAROUND, Boolean.class);
        AudioOutputHandler audioOutputHandler = AudioOutputHandler.newAudioOutputHandler(outputSampleRate, outputChannelCount, artificiallyBlockingPlay);
        setEncoderExecutor(new EncoderExecutor(encoder, audioOutputHandler));
    }

    public void closeResources() {
        encoderExecutorRef.get().close();
    }

    public void transmitNewMessage(OutgoingMessage message) {
        String callsign = message.callsign();
        String body = message.body();
        Integer delay = message.delay();

        final String callsignFinal = callsign == null ? "" : callsign;
        final String bodyFinal = body == null ? "" : body;

        byte[] payload = getPayload(bodyFinal);
        byte[] callsignBytes = getCallsignBytes(callsignFinal);

        AppPreferences prefs = AppPreferences.get();
        final int carrierFrequency = prefs.get(Pref.CARRIER_FREQUENCY, Integer.class);
        final int noiseSymbols = prefs.get(Pref.LEADING_NOISE, LeadingNoise.class).getNoiseSymbols();
        final boolean fancyHeader = prefs.get(Pref.FANCY_HEADER, Boolean.class);
        final int channelSelect = prefs.get(Pref.OUTPUT_CHANNEL, OutputChannel.class).getIntValue();
        TransmissionSettings transmissionSettings = new TransmissionSettings(carrierFrequency, noiseSymbols, fancyHeader, channelSelect, delay);
        Runnable beforeTransmitRunnable = () -> {
            MessageType messageType = bodyFinal.isEmpty() ? MessageType.PING_OUTGOING : MessageType.NORMAL_OUTGOING;
            model.getMessages().add(new Message(callsignFinal, bodyFinal, null, LocalDateTime.now(), messageType));
        };
        getEncoderExecutor().transmit(payload, callsignBytes, transmissionSettings, beforeTransmitRunnable);
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

    private static byte[] getPayload(String body) {
        return Arrays.copyOf(body.getBytes(StandardCharsets.UTF_8), 170);
    }
}
