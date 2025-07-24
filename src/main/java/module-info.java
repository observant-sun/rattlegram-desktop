module rattlegram {
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires org.slf4j;
    requires java.desktop;

    opens com.github.observant_sun.rattlegram.controller to javafx.fxml;
    exports com.github.observant_sun.rattlegram;
}