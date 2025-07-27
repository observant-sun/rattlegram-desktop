package com.github.observant_sun.rattlegram;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;

public class RattlegramLauncher extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = getParent();
        primaryStage.initStyle(StageStyle.DECORATED);
        int width = 800;
        int height = 600;
        Scene mainScene = new Scene(root, width, height);
        mainScene.setRoot(root);
        primaryStage.setResizable(true);
        primaryStage.setScene(mainScene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> Platform.exit());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("controller/fxml/spectrum.fxml"));
        Parent parent = loader.load();
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle("Spectrum");
        width = 800;
        height = 400;
        stage.setScene(new Scene(parent, width, height));
        stage.show();
    }

    private Parent getParent() throws IOException {
        String mainFXMLPath = "fxml/main.fxml";
        URL mainFXMLResource = getClass().getResource(mainFXMLPath);
        if (mainFXMLResource == null) {
            throw new MissingResourceException("Failed to load main.fxml",
                    RattlegramLauncher.class.getName(), mainFXMLPath);
        }
        return FXMLLoader.load(mainFXMLResource);
    }

    public static void main(String[] args) {
        launch(args);
    }

}