package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.i18n.I18n;
import com.github.observant_sun.rattlegram.util.WindowPosition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class SettingsWindowStarter {

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 400;

    private final WindowPosition windowPosition = new WindowPosition(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    private static final class InstanceHolder {
        private static final SettingsWindowStarter instance = new SettingsWindowStarter();
    }

    public static SettingsWindowStarter get() {
        return InstanceHolder.instance;
    }

    public void start(Runnable updatePreferencesCallback) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/settings.fxml"), I18n.get().getResourceBundle());
        Parent parent = loader.load();
        SettingsWindowController controller = loader.getController();
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        String title = I18n.get().getMessage(SettingsWindowStarter.class, "windowTitle");
        stage.setTitle(title);
        stage.setWidth(windowPosition.getWidth());
        stage.setHeight(windowPosition.getHeight());
        stage.setScene(new Scene(parent, windowPosition.getWidth(), windowPosition.getHeight()));
        stage.setOnHidden(event -> {
            controller.saveSettings();
            updatePreferencesCallback.run();
        });
        windowPosition.addListeners(stage);
        stage.show();
    }
}
