package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.i18n.I18n;
import com.github.observant_sun.rattlegram.model.Model;
import com.github.observant_sun.rattlegram.util.WindowPosition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class RepeaterWindowStarter {

    private static final int DEFAULT_WIDTH = 600;
    private static final int DEFAULT_HEIGHT = 400;

    private final WindowPosition windowPosition = new WindowPosition(DEFAULT_WIDTH, DEFAULT_HEIGHT);

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
        stage.setWidth(windowPosition.getWidth());
        stage.setHeight(windowPosition.getHeight());
        String title = I18n.get().getMessage(RepeaterWindowStarter.class, "windowTitle");
        stage.setTitle(title);
        Scene spectrogramScene = new Scene(parent, windowPosition.getWidth(), windowPosition.getHeight());
        stage.setScene(spectrogramScene);
        stage.setOnCloseRequest(event -> Model.get().showRepeaterWindowProperty().set(false));
        windowPosition.addListeners(stage);
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
        windowPosition.setOnStage(stage);
    }

    public synchronized void hide() {
        stage.hide();
    }
}
