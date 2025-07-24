package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.audio.AudioInputHandler;
import com.github.observant_sun.rattlegram.audio.AudioOutputHandler;
import com.github.observant_sun.rattlegram.encoding.Decoder;
import com.github.observant_sun.rattlegram.encoding.Encoder;
import com.github.observant_sun.rattlegram.entity.Message;
import com.github.observant_sun.rattlegram.entity.StatusType;
import com.github.observant_sun.rattlegram.entity.StatusUpdate;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MainWindowController implements Initializable {


    private static final Logger log = LoggerFactory.getLogger(MainWindowController.class);
    @FXML public HBox hBox;
    @FXML public Label statusLabel;
    @FXML private AnchorPane anchorPane;
    @FXML private VBox vBox;
    @FXML private TextArea messagesTextArea;
    @FXML private TextField callsignBox;
    @FXML private TextField messageBox;

    private Encoder encoder;
    private Decoder decoder;
    private AudioOutputHandler audioOutputHandler;
    private final ExecutorService encoderThread = Executors.newSingleThreadExecutor((runnable) -> {
        Thread thread = new Thread(runnable, "encoder-thread");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        final int sampleRate = 48000;
        encoder = new Encoder(sampleRate);

        Consumer<Message> newMessageCallback = this::processNewMessage;
        Consumer<StatusUpdate> statusUpdateCallback = this::processStatusUpdate;
        AudioInputHandler audioInputHandler = new AudioInputHandler(sampleRate);
        decoder = new Decoder(sampleRate, newMessageCallback, statusUpdateCallback, audioInputHandler);
        audioOutputHandler = new AudioOutputHandler(sampleRate);
        processStatusUpdate(new StatusUpdate(StatusType.OK, "Listening"));
    }

    private void processNewMessage(Message message) {
        Platform.runLater(() -> {
            messagesTextArea.appendText(getMessageFormattedLine(message.timestamp(), message.callsign(), message.body()));
        });
    }

    private void processStatusUpdate(StatusUpdate status) {
        Platform.runLater(() -> {
            statusLabel.setText(status.message());
        });
    }

    public void sendMethod(KeyEvent keyEvent) {
        if (keyEvent.getCode() != KeyCode.ENTER) {
            return;
        }
        String callsign = callsignBox.getText();
        String message = messageBox.getText();
        byte[] payload = getPayload(message);
        byte[] callsignBytes = getCallsignBytes(callsign);

        messageBox.clear();
        messagesTextArea.appendText(getMessageFormattedLine(LocalDateTime.now(), callsign, message));

        encoderThread.submit(() -> {
            byte[] audioOutputBytes = produceAudioOutputBytes(payload, callsignBytes);
            playAudioOutputBytes(audioOutputBytes);
        });
    }

    private byte[] produceAudioOutputBytes(byte[] payload, byte[] callsignBytes) {
        final int carrierFrequency = 1500;
        final int noiseSymbols = 6;
        final boolean fancyHeader = false;
        encoder.configure(payload, callsignBytes, carrierFrequency, noiseSymbols, fancyHeader);

        int channelSelect = 0;
        int repeatCount = 15;
        return encoder.produce(channelSelect, repeatCount);
    }

    private void playAudioOutputBytes(byte[] arr) {
        Platform.runLater(() -> processStatusUpdate(new StatusUpdate(StatusType.OK, "Transmitting")));

        decoder.pause();

        audioOutputHandler.play(arr);

        decoder.resume();

        Platform.runLater(() -> processStatusUpdate(new StatusUpdate(StatusType.OK, "Listening")));
    }

    private static byte[] getCallsignBytes(String callsign) {
        return Arrays.copyOf(callsign.getBytes(StandardCharsets.US_ASCII), callsign.length() + 1);
    }

    private static String getMessageFormattedLine(LocalDateTime localDateTime, String callsign, String message) {
        return "[%s] %s: %s\n".formatted(localDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault())), callsign, message);
    }

    private static byte[] getPayload(String message) {
        return Arrays.copyOf(message.getBytes(StandardCharsets.UTF_8), 170);
    }
}