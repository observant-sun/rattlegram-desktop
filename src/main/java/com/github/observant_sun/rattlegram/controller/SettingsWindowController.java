package com.github.observant_sun.rattlegram.controller;

import javafx.fxml.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsWindowController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SettingsWindowController.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void loadSettings() {

    }

    public void saveSettings() {
        log.debug("save settings");
    }

}
