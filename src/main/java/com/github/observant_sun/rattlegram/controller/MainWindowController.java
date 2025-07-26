package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.entity.*;
import com.github.observant_sun.rattlegram.model.Model;
import com.github.observant_sun.rattlegram.prefs.*;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

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

    private Model model;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String callsign = AppPreferences.get().get(AppPreferences.Pref.CALLSIGN, String.class);
        this.callsignBox.setText(callsign);

        this.callsignBox.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                saveCallsign();
            }
        });

        model = Model.get();
        model.initializeEncoders();
        model.addNewMessageCallback(this::processNewMessage);
        model.addStatusUpdateCallback(this::processStatusUpdate);
        model.addTransmissionBeginCallback(this::processTransmissionBegin);
        model.addListeningBeginCallback(this::processListeningBegin);
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
                processStatusUpdate(new StatusUpdate(StatusType.OK, "Transmitting")));
    }

    private void processListeningBegin() {
        Platform.runLater(() -> processStatusUpdate(new StatusUpdate(StatusType.OK, "Listening")));
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
        // TODO: if settings are opened while transmitting, recording will be resumed
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
        AppPreferences.get().set(AppPreferences.Pref.CALLSIGN, text);
    }
}