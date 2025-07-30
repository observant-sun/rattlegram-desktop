package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.model.Model;
import com.github.observant_sun.rattlegram.util.Utils;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class RepeaterWindowController implements Initializable {


    public TilePane tilePane;
    public Label enableRepeaterModeCheckBoxLabel;
    public CheckBox enableRepeaterModeCheckBox;
    public Label repeaterDelayIntegerFieldLabel;
    public TextField repeaterDelayTextField;
    public Label repeaterDebounceTimeIntegerFieldLabel;
    public TextField repeaterDebounceTimeTextField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Model model = Model.get();
        enableRepeaterModeCheckBox.selectedProperty().bindBidirectional(model.repeaterModeEnabledProperty());
        repeaterDelayTextField.textProperty().set(String.valueOf(model.repeaterDelayProperty().get()));
        repeaterDelayTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                Optional<Integer> repeaterDelayOpt = Utils.stringToInteger(repeaterDelayTextField.getText());
                repeaterDelayOpt.ifPresentOrElse(
                        integer -> {
                            repeaterDelayTextField.styleProperty().set("");
                            model.repeaterDelayProperty().set(integer);
                        },
                        () -> repeaterDelayTextField.styleProperty().set("-fx-text-fill: red;")
                );
            }
        });
        repeaterDebounceTimeTextField.textProperty().set(String.valueOf(model.repeaterDebounceTimeProperty().get()));
        repeaterDebounceTimeTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                Optional<Integer> repeaterDebounceTimeOpt = Utils.stringToInteger(repeaterDebounceTimeTextField.getText());
                repeaterDebounceTimeOpt.ifPresentOrElse(
                        integer -> {
                            repeaterDebounceTimeTextField.styleProperty().set("");
                            model.repeaterDebounceTimeProperty().set(integer);
                        },
                        () -> repeaterDebounceTimeTextField.styleProperty().set("-fx-text-fill: red;")
                );
            }
        });
    }

}
