package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.entity.*;
import com.github.observant_sun.rattlegram.i18n.I18n;
import com.github.observant_sun.rattlegram.model.DecoderInteractor;
import com.github.observant_sun.rattlegram.model.EncoderInteractor;
import com.github.observant_sun.rattlegram.model.Model;
import com.github.observant_sun.rattlegram.prefs.*;
import com.github.observant_sun.rattlegram.util.Utils;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

@Slf4j
public class MainWindowController implements Initializable {

    private static final int MAX_MESSAGE_BYTE_LENGTH = 170;

    private static final TextFormatter<Object> callsignBoxTextFormatter = new TextFormatter<>(change -> {
        String text = change.getText();
        log.trace("Callsign change: {}", text);
        if (text == null || text.isEmpty()) {
            return change;
        }
        String controlNewText = change.getControlNewText();
        int MAX_CALLSIGN_LENGTH = 10;
        if (controlNewText.length() > MAX_CALLSIGN_LENGTH) {
            change.setText("");
            return change;
        }
        change.setText(text.toUpperCase().replaceAll("[^ A-Z0-9]", ""));
        return change;
    });

    // TODO too computationally expensive for every key press, creates input lag
    private static final TextFormatter<Object> messageBoxTextFormatter = new TextFormatter<>(change -> {
        String changeText = change.getText();
        log.trace("Message change: {}", changeText);
        if (changeText == null || changeText.isEmpty()) {
            return change;
        }
        int controlNewTextBytesLength = change.getControlNewText().getBytes(StandardCharsets.UTF_8).length;
        if (controlNewTextBytesLength <= MAX_MESSAGE_BYTE_LENGTH) {
            return change;
        }

        int diffBytes = controlNewTextBytesLength - MAX_MESSAGE_BYTE_LENGTH;
        int oldByteLength = changeText.getBytes(StandardCharsets.UTF_8).length;
        int newByteLength = oldByteLength - diffBytes;
        changeText = Utils.truncateStringToUtf8ByteLength(changeText, newByteLength);
        change.setText(changeText);
        return change;
    });

    private static final String IMAGE_PATH_STRONG_PROTECTION = "/images/indicator_green.png";
    private static final String IMAGE_PATH_MEDIUM_PROTECTION = "/images/indicator_yellow.png";
    private static final String IMAGE_PATH_NORMAL_PROTECTION = "/images/indicator_orange.png";
    private static final String IMAGE_PATH_MESSAGE_TOO_LONG = "/images/indicator_orange.png";

    private String getProtectionStrengthImagePath(int messageLength) {
        if (messageLength <= 85) {
            return IMAGE_PATH_STRONG_PROTECTION;
        } else if (messageLength <= 128) {
            return IMAGE_PATH_MEDIUM_PROTECTION;
        } else if (MAX_MESSAGE_BYTE_LENGTH <= 170) {
            return IMAGE_PATH_NORMAL_PROTECTION;
        } else {
            return IMAGE_PATH_MESSAGE_TOO_LONG;
        }
    }

    private void setEncodingProtectionStrengthImageView(int oldMessageLength, int newMessageLength) {
        String oldImagePath = getProtectionStrengthImagePath(oldMessageLength);
        String newImagePath = getProtectionStrengthImagePath(newMessageLength);
        if (oldImagePath.equals(newImagePath) && encodingProtectionStrengthImageView.getImage() != null) {
            return;
        }
        encodingProtectionStrengthImageView.setImage(new Image(getClass().getResourceAsStream(newImagePath)));
    }

    @FXML
    private ButtonBar topButtonBar;
    @FXML
    private Button repeaterSettingsButton;
    @FXML
    private Button showSpectrogramAnalyzerButton;
    @FXML
    private Button settingsButton;
    @FXML
    private HBox hBox;
    @FXML
    private Label statusLabel;
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private VBox vBox;
    @FXML
    private TextArea messagesTextArea;
    @FXML
    private TextField callsignBox;
    @FXML
    private TextField messageBox;
    @FXML
    private ImageView encodingProtectionStrengthImageView;
    @FXML
    private Label messageBoxSymbolsLeftLabel;

    private Model model;
    private DecoderInteractor decoderInteractor;
    private EncoderInteractor encoderInteractor;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String callsign = AppPreferences.get().get(Pref.CALLSIGN, String.class);
        this.callsignBox.setText(callsign);

        this.callsignBox.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                this.callsignBox.setText(this.callsignBox.getText().trim());
                saveCallsign();
            }
        });

        this.callsignBox.setTextFormatter(callsignBoxTextFormatter);
        this.messageBox.setTextFormatter(messageBoxTextFormatter);
        this.messageBox.textProperty().addListener((observable, oldValue, newValue) -> {
            int oldMessageLength = Optional.ofNullable(oldValue).map(string -> string.getBytes(StandardCharsets.UTF_8)).map(bytes -> bytes.length).orElse(0);
            int newMessageLength = Optional.ofNullable(newValue).map(string -> string.getBytes(StandardCharsets.UTF_8)).map(bytes -> bytes.length).orElse(0);
            setEncodingProtectionStrengthImageView(oldMessageLength, newMessageLength);
            this.messageBoxSymbolsLeftLabel.setText(String.valueOf(MAX_MESSAGE_BYTE_LENGTH - newMessageLength));
        });
        setEncodingProtectionStrengthImageView(0, 0);

        try {
            SpectrumAnalyzerWindowStarter.get().start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        model = Model.get();
        boolean showSpectrogram = model.showSpectrumAnalyzerProperty().get();
        setShowSpectrogramButtonText(showSpectrogram);
        model.showSpectrumAnalyzerProperty().subscribe(value -> {
            Platform.runLater(() -> setShowSpectrogramButtonText(value));
        });
        setRepeaterSettingsButtonStyle(model.repeaterModeEnabledProperty().get());
        model.repeaterModeEnabledProperty().addListener((observable, oldValue, newValue) -> {
            setRepeaterSettingsButtonStyle(newValue);
        });
        // TODO switch to on-change approach
        model.getMessages().addListener((InvalidationListener) observable -> {
            messagesTextArea.clear();
            model.getMessages().forEach(this::processNewMessage);
        });
        model.getStatusUpdatePublisher().subscribe(this::processStatusUpdate);
        model.getTransmissionBeginPublisher().subscribe(this::processTransmissionBegin);
        model.getListeningBeginPublisher().subscribe(this::processListeningBegin);
        decoderInteractor = new DecoderInteractor(model);
        encoderInteractor = new EncoderInteractor(model);
        initEncoders();
    }

    private void setRepeaterSettingsButtonStyle(boolean repeaterEnabled) {
        if (repeaterEnabled) {
            repeaterSettingsButton.setStyle("-fx-background-color: rgba(0,255,0,0.7)");
        } else {
            repeaterSettingsButton.setStyle("");
        }
    }

    private void setShowSpectrogramButtonText(boolean showSpectrogram) {
        String buttonText;
        if (showSpectrogram) {
            buttonText = I18n.get().getMessage(MainWindowController.class, "hideSpectrumAnalyzer");
        } else {
            buttonText = I18n.get().getMessage(MainWindowController.class, "showSpectrumAnalyzer");
        }
        showSpectrogramAnalyzerButton.setText(buttonText);
    }

    private void processNewMessage(Message message) {
        Platform.runLater(() -> {
            String messageFormattedLine = null;
            switch (message.type()) {
                case NORMAL_INCOMING -> {
                    messageFormattedLine = getMessageFormattedLine(message.timestamp(), message.callsign(), message.body(), ">>");
                }
                case ERROR_INCOMING -> {
                    messageFormattedLine = getMessageFormattedLine(message.timestamp(), message.callsign(), message.decoderResult(), "!>");
                }
                case PING_INCOMING -> {
                    messageFormattedLine = getMessageFormattedLine(message.timestamp(), message.callsign(), "<Received ping>", "P>");
                }
                case NORMAL_OUTGOING -> {
                    messageFormattedLine = getMessageFormattedLine(message.timestamp(), message.callsign(), message.body(), "<<");
                }
                case PING_OUTGOING -> {
                    messageFormattedLine = getMessageFormattedLine(message.timestamp(), message.callsign(), "<Sent ping>", "P<");
                }
            }
            if (messageFormattedLine != null) {
                messagesTextArea.appendText(messageFormattedLine);
            }
        });
    }

    private void processStatusUpdate(StatusUpdate status) {
        Platform.runLater(() ->
                statusLabel.setText(status.message()));
    }

    private void processTransmissionBegin() {
        Platform.runLater(() ->
        {
            String statusUpdateString = I18n.get().getMessage(MainWindowController.class, "transmissionBeginStatus");
            processStatusUpdate(new StatusUpdate(StatusType.OK, statusUpdateString));
        });
    }

    private void processListeningBegin() {
        Platform.runLater(() -> {
            String statusUpdateString = I18n.get().getMessage(MainWindowController.class, "listeningBeginStatus");
            processStatusUpdate(new StatusUpdate(StatusType.OK, statusUpdateString));
        });
    }

    public void sendMethod(KeyEvent keyEvent) {
        if (keyEvent.getCode() != KeyCode.ENTER) {
            return;
        }
        String callsign = callsignBox.getText();
        String message = messageBox.getText();

        messageBox.clear();
        model.getNewOutgoingMessagePublisher().submit(new OutgoingMessage(callsign, message, null));
    }


    private static String getMessageFormattedLine(LocalDateTime localDateTime, String callsign, String message, String directionString) {
        return "[%s] %s %s: %s\n".formatted(localDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault())), directionString, callsign, message);
    }

    public void showSettingsWindow() {
        decoderInteractor.pauseRecording();
        try {
            Runnable updatePreferencesCallback = this::reinitializeEncoders;
            SettingsWindowStarter.get().start(updatePreferencesCallback);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveCallsign() {
        String text = this.callsignBox.getText();
        AppPreferences.get().set(Pref.CALLSIGN, text);
    }

    public void toggleShowSpectrumAnalyzerWindowProperty() {
        model.showSpectrumAnalyzerProperty().set(!model.showSpectrumAnalyzerProperty().get());
    }

    public void toggleRepeaterWindow() throws IOException {
        RepeaterWindowStarter.get().start();
        model.toggleRepeaterWindow();
    }

    private void initEncoders() {
        encoderInteractor.init();
        decoderInteractor.init();
    }

    private void closeResources() {
        encoderInteractor.closeResources();
        decoderInteractor.closeResources();
    }

    public void reinitializeEncoders() {
        closeResources();
        initEncoders();
    }
}