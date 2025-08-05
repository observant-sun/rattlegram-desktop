package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.i18n.I18n;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class SettingsWindowStarter {

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
        stage.initStyle(StageStyle.UTILITY);
        String title = I18n.get().getMessage(SettingsWindowStarter.class, "windowTitle");
        stage.setTitle(title);
        int width = 800;
        int height = 400;
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setScene(new Scene(parent, width, height));
        stage.setOnHidden(event -> {
            controller.saveSettings();
            updatePreferencesCallback.run();
        });
        stage.show();
    }
}
