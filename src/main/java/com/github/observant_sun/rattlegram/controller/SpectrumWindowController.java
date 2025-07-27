package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.model.Model;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;

public class SpectrumWindowController implements Initializable {

    public AnchorPane rootAnchorPane;
    public HBox hBox;
    @FXML private ImageView spectrumImageView;

    private Model model;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Color blackColor = new Color(0, 0, 0, 1);
        hBox.setBackground(new Background(new BackgroundFill(blackColor, null, null)));
        spectrumImageView.setPreserveRatio(false);
        model = Model.get();
        model.addUpdateSpectrumCallback(image -> spectrumImageView.setImage(image));
    }

}
