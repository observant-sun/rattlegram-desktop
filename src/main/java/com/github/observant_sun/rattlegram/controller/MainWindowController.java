package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.entity.*;
import com.github.observant_sun.rattlegram.i18n.I18n;
import com.github.observant_sun.rattlegram.model.Model;
import com.github.observant_sun.rattlegram.prefs.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

@Slf4j
public class MainWindowController implements Initializable {

    @FXML
    private ButtonBar topButtonBar;
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

    private Model model;

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

        TextFormatter<Object> callsignBoxTextFormatter = new TextFormatter<>(change -> {
            log.debug("Callsign change: {}", change.getText());
            if (change.getText() == null || change.getText().isEmpty()) {
                return change;
            }
            String controlNewText = change.getControlNewText();
            int MAX_CALLSIGN_LENGTH = 10;
            if (controlNewText.length() > MAX_CALLSIGN_LENGTH) {
                change.setText("");
                return change;
            }
            change.setText(change.getText().toUpperCase().replaceAll("[^ A-Z0-9]", ""));
            return change;
        });
        this.callsignBox.setTextFormatter(callsignBoxTextFormatter);

        this.messageBox.setTextFormatter(new TextFormatter<>(change -> {
            String changeText = change.getText();
            log.debug("Message change: {}", changeText);
            if (changeText == null || changeText.isEmpty()) {
                return change;
            }
            String controlNewText = change.getControlNewText();
            int MAX_MESSAGE_LENGTH = 170;
            if (controlNewText.length() > MAX_MESSAGE_LENGTH) {
                int diff = controlNewText.length() - MAX_MESSAGE_LENGTH;
                changeText = changeText.substring(0, changeText.length() - diff);
                change.setText(changeText);
            }
            return change;
        }));

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

        model.addNewMessageCallback(this::processNewMessage);
        model.addStatusUpdateCallback(this::processStatusUpdate);
        model.addTransmissionBeginCallback(this::processTransmissionBegin);
        model.addListeningBeginCallback(this::processListeningBegin);
        model.initializeEncoders();
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
        Platform.runLater(() ->
                messagesTextArea.appendText(getMessageFormattedLine(message.timestamp(), message.callsign(), message.body())));
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
        messagesTextArea.appendText(getMessageFormattedLine(LocalDateTime.now(), callsign, message));

        model.transmitNewMessage(callsign, message);
    }


    private static String getMessageFormattedLine(LocalDateTime localDateTime, String callsign, String message) {
        return "[%s] %s: %s\n".formatted(localDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault())), callsign, message);
    }

    public void showSettingsWindow() {
        model.pauseRecording();
        try {
            Runnable updatePreferencesCallback = () -> model.reinitializeEncoders();
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
}