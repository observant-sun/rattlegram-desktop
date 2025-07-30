package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.i18n.I18n;
import com.github.observant_sun.rattlegram.model.Model;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class RepeaterWindowStarter {

    private static final class InstanceHolder {
        private static final RepeaterWindowStarter instance = new RepeaterWindowStarter();
    }

    public static RepeaterWindowStarter get() {
        return InstanceHolder.instance;
    }

    private volatile boolean started = false;
    private volatile Stage stage;

    public synchronized void start() throws IOException {
        if (started) {
            return;
        }
        started = true;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/repeater.fxml"), I18n.get().getResourceBundle());
        Parent parent = loader.load();
        stage = new Stage();
        stage.setWidth(450);
        stage.setHeight(300);
        String title = I18n.get().getMessage(RepeaterWindowStarter.class, "windowTitle");
        stage.setTitle(title);
        Scene spectrogramScene = new Scene(parent, 450, 300);
        stage.setScene(spectrogramScene);
        stage.setOnCloseRequest(event -> Model.get().showRepeaterWindowProperty().set(false));
        if (Model.get().showRepeaterWindowProperty().get()) {
            Platform.runLater(this::show);
        }
        Model.get().showRepeaterWindowProperty().addListener((observable, oldValue, newValue) -> {
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
