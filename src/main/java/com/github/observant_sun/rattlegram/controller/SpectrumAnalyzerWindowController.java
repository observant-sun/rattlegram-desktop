package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.model.Model;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;

public class SpectrumAnalyzerWindowController implements Initializable {

    public AnchorPane rootAnchorPane;
    public VBox vBox;
    @FXML private ImageView spectrumImageView;
    public ImageView spectrogramImageView;

    private Model model;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Color blackColor = new Color(0, 0, 0, 1);
        vBox.setBackground(new Background(new BackgroundFill(blackColor, null, null)));
        spectrumImageView.setPreserveRatio(false);
        model = Model.get();
        model.getUpdateSpectrogramPublisher().subscribe(spectrumImages -> {
            spectrumImageView.setImage(spectrumImages.spectrum());
            spectrogramImageView.setImage(spectrumImages.spectrogram());
        });
    }

}
