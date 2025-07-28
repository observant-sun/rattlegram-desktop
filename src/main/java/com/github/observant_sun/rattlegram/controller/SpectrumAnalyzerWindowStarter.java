package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.i18n.I18n;
import com.github.observant_sun.rattlegram.model.Model;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SpectrumAnalyzerWindowStarter {

    private static final class InstanceHolder {
        private static final SpectrumAnalyzerWindowStarter instance = new SpectrumAnalyzerWindowStarter();
    }

    public static SpectrumAnalyzerWindowStarter get() {
        return InstanceHolder.instance;
    }

    private volatile boolean started = false;
    private volatile Stage stage;

    public synchronized void start() throws IOException {
        if (started) {
            return;
        }
        started = true;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/spectrum.fxml"), I18n.get().getResourceBundle());
        Parent parent = loader.load();
        stage = new Stage();
        stage.setResizable(false);
        String title = I18n.get().getMessage(SpectrumAnalyzerWindowStarter.class, "windowTitle");
        stage.setTitle(title);
        Scene spectrogramScene = new Scene(parent);
        stage.setScene(spectrogramScene);
        stage.setOnCloseRequest(event -> Model.get().showSpectrumAnalyzerProperty().set(false));
        if (Model.get().showSpectrumAnalyzerProperty().get()) {
            Platform.runLater(this::show);
        }
        Model.get().showSpectrumAnalyzerProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                show();
            } else {
                hide();
            }
        });
    }

    public synchronized void show() {
        if (!started) {
            try {
                start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        stage.show();
    }

    public synchronized void hide() {
        stage.hide();
    }
}
