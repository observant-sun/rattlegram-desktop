package com.github.observant_sun.rattlegram.util;

import javafx.stage.Stage;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WindowPosition {
    private int x;
    private int y;
    private int width;
    private int height;

    public WindowPosition(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setOnStage(Stage stage) {
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(width);
        stage.setHeight(height);
    }

    public void addListeners(Stage stage) {
        stage.xProperty().addListener((observable, oldValue, newValue) ->
                this.setX(newValue.intValue()));
        stage.yProperty().addListener((observable, oldValue, newValue) ->
                this.setY(newValue.intValue()));
        stage.widthProperty().addListener((observable, oldValue, newValue) ->
                this.setWidth(newValue.intValue()));
        stage.heightProperty().addListener((observable, oldValue, newValue) ->
                this.setHeight(newValue.intValue()));
    }
}
