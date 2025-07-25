package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.audio.AudioInputHandler;
import com.github.observant_sun.rattlegram.audio.AudioOutputHandler;
import com.github.observant_sun.rattlegram.encoding.Decoder;
import com.github.observant_sun.rattlegram.encoding.Encoder;
import com.github.observant_sun.rattlegram.entity.Message;
import com.github.observant_sun.rattlegram.entity.StatusType;
import com.github.observant_sun.rattlegram.entity.StatusUpdate;
import com.github.observant_sun.rattlegram.entity.TransmissionSettings;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    @FXML private ButtonBar topButtonBar;
    @FXML private Button settingsButton;
    @FXML private HBox hBox;
    @FXML private Label statusLabel;
    @FXML private AnchorPane anchorPane;
    @FXML private VBox vBox;
    @FXML private TextArea messagesTextArea;
    @FXML private TextField callsignBox;
    @FXML private TextField messageBox;

    private Encoder encoder;
    private Decoder decoder;
    private AudioOutputHandler audioOutputHandler;
    private ExecutorService encoderThread;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeResources();
    }

    private void initializeResources() {
        final int sampleRate = 48000;

        this.encoder = new Encoder(sampleRate);
        this.audioOutputHandler = new AudioOutputHandler(sampleRate);

        this.encoderThread = Executors.newSingleThreadExecutor((runnable) -> {
            Thread thread = new Thread(runnable, "encoder-thread");
            thread.setDaemon(true);
            return thread;
        });

        Consumer<Message> newMessageCallback = this::processNewMessage;
        Consumer<StatusUpdate> statusUpdateCallback = this::processStatusUpdate;
        AudioInputHandler audioInputHandler = new AudioInputHandler(sampleRate);
        this.decoder = new Decoder(sampleRate, newMessageCallback, statusUpdateCallback, audioInputHandler);
        processStatusUpdate(new StatusUpdate(StatusType.OK, "Listening"));
    }

    private void closeResources() {
        encoder.close();
        decoder.close();
        encoderThread.shutdownNow();
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

        final int carrierFrequency = 1500;
        final int noiseSymbols = 6;
        final boolean fancyHeader = false;
        final int channelSelect = 0;
        final int repeatCount = 15;
        TransmissionSettings transmissionSettings = new TransmissionSettings(carrierFrequency, noiseSymbols, fancyHeader, channelSelect, repeatCount);
        encoderThread.submit(() -> {
            byte[] audioOutputBytes = produceAudioOutputBytes(payload, callsignBytes, transmissionSettings);
            playAudioOutputBytes(audioOutputBytes);
        });
    }

    private byte[] produceAudioOutputBytes(byte[] payload, byte[] callsignBytes, TransmissionSettings transmissionSettings) {
        encoder.configure(payload, callsignBytes, transmissionSettings.carrierFrequency(), transmissionSettings.noiseSymbols(), transmissionSettings.fancyHeader());

        return encoder.produce(transmissionSettings.channelSelect(), transmissionSettings.repeatCount());
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

    public void showSettingsWindow() {
        try {
            SettingsWindowStarter.get().start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}